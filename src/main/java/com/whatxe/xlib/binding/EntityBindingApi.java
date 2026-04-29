package com.whatxe.xlib.binding;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.XLibRegistryGuard;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class EntityBindingApi {
    public static final ResourceLocation COMMAND_SOURCE =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "command_entity_binding");

    private static final Map<ResourceLocation, EntityBindingDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<UUID, EntityBindingState> RUNTIME_CACHE = new ConcurrentHashMap<>();

    private static MinecraftServer currentServer;

    private EntityBindingApi() {}

    public static void bootstrap() {}

    // --- Server lifecycle ---

    public static void onServerStart(MinecraftServer server) {
        currentServer = server;
    }

    public static void onServerStop() {
        currentServer = null;
        RUNTIME_CACHE.clear();
    }

    // --- Registration ---

    public static EntityBindingDefinition register(EntityBindingDefinition definition) {
        XLibRegistryGuard.ensureMutable("entity_bindings");
        EntityBindingDefinition previous = DEFINITIONS.putIfAbsent(definition.id(), definition);
        if (previous != null) {
            throw new IllegalStateException("Duplicate entity binding registration: " + definition.id());
        }
        return definition;
    }

    public static Optional<EntityBindingDefinition> unregister(ResourceLocation id) {
        XLibRegistryGuard.ensureMutable("entity_bindings");
        return Optional.ofNullable(DEFINITIONS.remove(id));
    }

    public static Optional<EntityBindingDefinition> findDefinition(ResourceLocation id) {
        return Optional.ofNullable(DEFINITIONS.get(id));
    }

    public static Collection<EntityBindingDefinition> all() {
        return List.copyOf(DEFINITIONS.values());
    }

    // --- Bind / Unbind ---

    public static UUID bind(LivingEntity primary, LivingEntity secondary, ResourceLocation bindingId, ResourceLocation sourceId) {
        EntityBindingDefinition def = DEFINITIONS.get(bindingId);
        if (def == null) {
            throw new IllegalArgumentException("Unknown entity binding definition: " + bindingId);
        }

        if (def.stackingPolicy() == EntityBindingStackingPolicy.REPLACE) {
            List<EntityBindingState> existing = bindings(primary).stream()
                    .filter(s -> s.bindingId().equals(bindingId) && s.secondaryEntityId().equals(secondary.getUUID()))
                    .toList();
            for (EntityBindingState s : existing) {
                removeBinding(primary, secondary, s.bindingInstanceId());
            }
        } else if (def.stackingPolicy() == EntityBindingStackingPolicy.SINGLE) {
            boolean alreadyExists = bindings(primary).stream()
                    .anyMatch(s -> s.bindingId().equals(bindingId) && s.secondaryEntityId().equals(secondary.getUUID()));
            if (alreadyExists) {
                throw new IllegalStateException("Binding already exists with SINGLE stacking policy: " + bindingId);
            }
        }

        UUID instanceId = UUID.randomUUID();
        long gameTime = primary.level().getGameTime();
        EntityBindingState state = new EntityBindingState(
                instanceId, bindingId,
                primary.getUUID(), secondary.getUUID(),
                sourceId, gameTime, def.durationTicks(),
                EntityBindingStatus.ACTIVE, new CompoundTag(), 0
        );

        primary.setData(ModAttachments.LIVING_ENTITY_BINDINGS, getData(primary).withBinding(state));
        secondary.setData(ModAttachments.LIVING_ENTITY_BINDINGS, getData(secondary).withSecondaryRef(instanceId));
        RUNTIME_CACHE.put(instanceId, state);
        return instanceId;
    }

    public static boolean unbind(UUID instanceId, EntityBindingEndReason reason) {
        EntityBindingState state = RUNTIME_CACHE.get(instanceId);
        if (state == null) return false;

        if (currentServer != null) {
            LivingEntity primary = resolveEntity(state.primaryEntityId());
            LivingEntity secondary = resolveEntity(state.secondaryEntityId());
            if (primary != null) {
                primary.setData(ModAttachments.LIVING_ENTITY_BINDINGS, getData(primary).withoutBinding(instanceId));
            }
            if (secondary != null) {
                secondary.setData(ModAttachments.LIVING_ENTITY_BINDINGS, getData(secondary).withoutSecondaryRef(instanceId));
            }
        }

        RUNTIME_CACHE.remove(instanceId);
        return true;
    }

    // --- Queries ---

    public static List<EntityBindingState> bindings(LivingEntity entity) {
        EntityBindingData data = getData(entity);
        List<EntityBindingState> result = new ArrayList<>(data.allPrimary());
        for (UUID refId : data.secondaryRefs()) {
            EntityBindingState cached = RUNTIME_CACHE.get(refId);
            if (cached != null) result.add(cached);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<EntityBindingState> bindings(LivingEntity entity, EntityBindingKind kind) {
        return bindings(entity).stream()
                .filter(s -> {
                    EntityBindingDefinition def = DEFINITIONS.get(s.bindingId());
                    return def != null && def.kind() == kind;
                })
                .toList();
    }

    public static Optional<EntityBindingState> find(UUID instanceId) {
        return Optional.ofNullable(RUNTIME_CACHE.get(instanceId));
    }

    public static List<EntityBindingState> between(LivingEntity a, LivingEntity b) {
        Set<UUID> aIds = new LinkedHashSet<>();
        for (EntityBindingState s : bindings(a)) aIds.add(s.bindingInstanceId());
        return bindings(b).stream()
                .filter(s -> aIds.contains(s.bindingInstanceId()))
                .toList();
    }

    // --- Break conditions ---

    public static void breakByCondition(LivingEntity entity, EntityBindingBreakCondition condition) {
        boolean isPrimaryCondition = condition == EntityBindingBreakCondition.PRIMARY_DIES
                || condition == EntityBindingBreakCondition.PRIMARY_DISCONNECTS;
        List<UUID> toBreak = new ArrayList<>();
        EntityBindingData data = getData(entity);

        if (isPrimaryCondition) {
            for (EntityBindingState state : data.allPrimary()) {
                EntityBindingDefinition def = DEFINITIONS.get(state.bindingId());
                if (def != null && def.breakConditions().contains(condition)) {
                    toBreak.add(state.bindingInstanceId());
                }
            }
        } else {
            for (UUID refId : data.secondaryRefs()) {
                EntityBindingState state = RUNTIME_CACHE.get(refId);
                if (state == null) continue;
                EntityBindingDefinition def = DEFINITIONS.get(state.bindingId());
                if (def != null && def.breakConditions().contains(condition)) {
                    toBreak.add(refId);
                }
            }
        }

        for (UUID instanceId : toBreak) {
            unbind(instanceId, EntityBindingEndReason.BROKEN_BY_CONDITION);
        }
    }

    // --- Cache management ---

    public static void onEntityLoad(LivingEntity entity) {
        for (EntityBindingState state : getData(entity).allPrimary()) {
            RUNTIME_CACHE.put(state.bindingInstanceId(), state);
        }
    }

    public static void onEntityUnload(LivingEntity entity) {
        for (UUID instanceId : getData(entity).primaryBindings().keySet()) {
            RUNTIME_CACHE.remove(instanceId);
        }
    }

    // --- Tick (duration countdown) ---

    public static void tick(LivingEntity entity) {
        EntityBindingData data = getData(entity);
        if (data.primaryBindings().isEmpty()) return;

        boolean changed = false;
        Map<UUID, EntityBindingState> updatedStates = new LinkedHashMap<>(data.primaryBindings());
        List<UUID> toExpire = new ArrayList<>();

        for (Map.Entry<UUID, EntityBindingState> entry : data.primaryBindings().entrySet()) {
            EntityBindingState state = entry.getValue();
            EntityBindingDefinition def = DEFINITIONS.get(state.bindingId());
            if (def == null || def.tickPolicy() != EntityBindingTickPolicy.TICK_DURATION) continue;
            if (state.remainingTicks().isEmpty()) continue;

            int remaining = state.remainingTicks().get() - 1;
            if (remaining <= 0) {
                toExpire.add(state.bindingInstanceId());
            } else {
                EntityBindingState updated = state.withRemainingTicks(remaining);
                updatedStates.put(state.bindingInstanceId(), updated);
                RUNTIME_CACHE.put(state.bindingInstanceId(), updated);
                changed = true;
            }
        }

        for (UUID expiredId : toExpire) {
            EntityBindingState expiredState = data.primaryBindings().get(expiredId);
            updatedStates.remove(expiredId);
            RUNTIME_CACHE.remove(expiredId);
            if (expiredState != null) {
                LivingEntity secondary = resolveEntity(expiredState.secondaryEntityId());
                if (secondary != null) {
                    secondary.setData(ModAttachments.LIVING_ENTITY_BINDINGS,
                            getData(secondary).withoutSecondaryRef(expiredId));
                }
            }
            changed = true;
        }

        if (changed) {
            entity.setData(ModAttachments.LIVING_ENTITY_BINDINGS,
                    new EntityBindingData(Collections.unmodifiableMap(updatedStates), data.secondaryRefs()));
        }
    }

    // --- Attachment access ---

    public static EntityBindingData getData(LivingEntity entity) {
        return entity.getData(ModAttachments.LIVING_ENTITY_BINDINGS);
    }

    // --- Helpers ---

    private static void removeBinding(LivingEntity primary, LivingEntity secondary, UUID instanceId) {
        primary.setData(ModAttachments.LIVING_ENTITY_BINDINGS, getData(primary).withoutBinding(instanceId));
        secondary.setData(ModAttachments.LIVING_ENTITY_BINDINGS, getData(secondary).withoutSecondaryRef(instanceId));
        RUNTIME_CACHE.remove(instanceId);
    }

    private static LivingEntity resolveEntity(UUID entityId) {
        if (currentServer == null) return null;
        for (ServerLevel level : currentServer.getAllLevels()) {
            Entity entity = level.getEntity(entityId);
            if (entity instanceof LivingEntity le) return le;
        }
        return null;
    }
}

package com.whatxe.xlib.body;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.XLibRegistryGuard;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.capability.CapabilityPolicyApi;
import com.whatxe.xlib.form.VisualFormApi;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class BodyTransitionApi {
    public static final ResourceLocation COMMAND_SOURCE =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "command_body_transition");

    private static final Map<ResourceLocation, BodyTransitionDefinition> DEFINITIONS = new LinkedHashMap<>();

    private BodyTransitionApi() {}

    public static void bootstrap() {}

    // --- Registration ---

    public static BodyTransitionDefinition register(BodyTransitionDefinition definition) {
        XLibRegistryGuard.ensureMutable("body_transitions");
        BodyTransitionDefinition previous = DEFINITIONS.putIfAbsent(definition.id(), definition);
        if (previous != null) {
            throw new IllegalStateException("Duplicate body transition registration: " + definition.id());
        }
        return definition;
    }

    public static Optional<BodyTransitionDefinition> unregister(ResourceLocation id) {
        XLibRegistryGuard.ensureMutable("body_transitions");
        return Optional.ofNullable(DEFINITIONS.remove(id));
    }

    public static Optional<BodyTransitionDefinition> findDefinition(ResourceLocation id) {
        return Optional.ofNullable(DEFINITIONS.get(id));
    }

    public static Collection<BodyTransitionDefinition> all() {
        return List.copyOf(DEFINITIONS.values());
    }

    // --- State queries ---

    public static Optional<BodyTransitionState> active(Player player) {
        return getData(player).activeTransition();
    }

    public static boolean isTransitioning(Player player) {
        return getData(player).hasTransition();
    }

    public static boolean isControlling(Player player, UUID targetEntityId) {
        return active(player)
                .map(s -> s.currentBodyEntityId().equals(targetEntityId))
                .orElse(false);
    }

    // --- Begin / Return / Clear ---

    public static boolean begin(ServerPlayer player, ResourceLocation transitionId, UUID targetEntityId, ResourceLocation sourceId) {
        BodyTransitionDefinition def = DEFINITIONS.get(transitionId);
        if (def == null) return false;
        if (isTransitioning(player)) return false;

        long gameTime = player.level().getGameTime();
        Optional<UUID> originBody = def.originBodyPolicy() != OriginBodyPolicy.DESTROY
                ? Optional.of(player.getUUID())
                : Optional.empty();

        BodyTransitionState state = new BodyTransitionState(
                player.getUUID(), targetEntityId, originBody,
                transitionId, sourceId, gameTime, BodyTransitionStatus.ACTIVE
        );

        setData(player, getData(player).withTransition(state));
        applyTemporaryEffects(player, def, sourceId);
        return true;
    }

    public static boolean returnToOrigin(ServerPlayer player, ResourceLocation sourceId) {
        Optional<BodyTransitionState> current = active(player);
        if (current.isEmpty()) return false;

        BodyTransitionDefinition def = DEFINITIONS.get(current.get().transitionId());
        if (def == null || !def.reversible()) return false;
        if (current.get().originBodyEntityId().isEmpty()) return false;

        clearTemporaryEffects(player, def, current.get().sourceId());
        setData(player, getData(player).withTransition(current.get().withStatus(BodyTransitionStatus.RETURNING)));
        setData(player, getData(player).withoutTransition());
        return true;
    }

    public static boolean clear(ServerPlayer player, ResourceLocation sourceId) {
        Optional<BodyTransitionState> current = active(player);
        if (current.isEmpty()) return false;

        BodyTransitionDefinition def = DEFINITIONS.get(current.get().transitionId());
        if (def != null) {
            clearTemporaryEffects(player, def, current.get().sourceId());
        }
        setData(player, getData(player).withoutTransition());
        return true;
    }

    // --- Temporary effect helpers ---

    private static void applyTemporaryEffects(ServerPlayer player, BodyTransitionDefinition def, ResourceLocation sourceId) {
        def.temporaryCapabilityPolicyId().ifPresent(policyId -> {
            try { CapabilityPolicyApi.apply(player, policyId, sourceId); } catch (Exception ignored) {}
        });
        def.temporaryVisualFormId().ifPresent(formId -> {
            try { VisualFormApi.apply(player, formId, sourceId); } catch (Exception ignored) {}
        });
    }

    private static void clearTemporaryEffects(ServerPlayer player, BodyTransitionDefinition def, ResourceLocation sourceId) {
        def.temporaryCapabilityPolicyId().ifPresent(policyId -> {
            try { CapabilityPolicyApi.revoke(player, policyId, sourceId); } catch (Exception ignored) {}
        });
        def.temporaryVisualFormId().ifPresent(formId -> {
            try { VisualFormApi.revoke(player, formId, sourceId); } catch (Exception ignored) {}
        });
    }

    // --- Sanitize ---

    public static BodyTransitionData sanitize(BodyTransitionData data) {
        if (data.activeTransition().isEmpty()) return data;
        ResourceLocation transitionId = data.activeTransition().get().transitionId();
        if (!DEFINITIONS.containsKey(transitionId)) return BodyTransitionData.empty();
        return data;
    }

    // --- Attachment access ---

    public static BodyTransitionData getData(Player player) {
        return player.getData(ModAttachments.PLAYER_BODY_TRANSITION);
    }

    public static void setData(ServerPlayer player, BodyTransitionData data) {
        player.setData(ModAttachments.PLAYER_BODY_TRANSITION, sanitize(data));
    }
}

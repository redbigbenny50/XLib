package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.XLibRegistryGuard;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class AbilitySlotContainerApi {
    public static final ResourceLocation PRIMARY_CONTAINER_ID = ResourceLocation.fromNamespaceAndPath(XLib.MODID, "primary");

    private static final Comparator<AbilitySlotContainerDefinition> DISPLAY_ORDER = Comparator
            .comparingInt(AbilitySlotContainerDefinition::displayOrder)
            .thenComparing(definition -> definition.id().toString());
    private static final Map<ResourceLocation, AbilitySlotContainerDefinition> CONTAINERS = new LinkedHashMap<>();

    private AbilitySlotContainerApi() {}

    public static void bootstrap() {
        if (CONTAINERS.containsKey(PRIMARY_CONTAINER_ID)) {
            return;
        }
        registerContainer(AbilitySlotContainerDefinition.builder(
                        PRIMARY_CONTAINER_ID,
                        Component.translatable("container.xlib.primary")
                )
                .owner(AbilitySlotContainerOwnerType.BASELINE, null)
                .slotsPerPage(AbilityData.SLOT_COUNT)
                .defaultPageCount(1)
                .displayOrder(0)
                .controlProfile(AbilityControlProfileApi.PRIMARY_NUMBER_ROW_PROFILE_ID)
                .build());
    }

    public static AbilitySlotContainerDefinition registerContainer(AbilitySlotContainerDefinition definition) {
        XLibRegistryGuard.ensureMutable("ability_slot_containers");
        AbilitySlotContainerDefinition resolvedDefinition = Objects.requireNonNull(definition, "definition");
        if (!PRIMARY_CONTAINER_ID.equals(resolvedDefinition.id())) {
            throw new IllegalStateException("Auxiliary slot containers are no longer supported: " + resolvedDefinition.id());
        }
        AbilitySlotContainerDefinition previous = CONTAINERS.putIfAbsent(resolvedDefinition.id(), resolvedDefinition);
        if (previous != null) {
            throw new IllegalStateException("Duplicate slot container registration: " + resolvedDefinition.id());
        }
        return resolvedDefinition;
    }

    public static Optional<AbilitySlotContainerDefinition> unregisterContainer(ResourceLocation containerId) {
        XLibRegistryGuard.ensureMutable("ability_slot_containers");
        if (PRIMARY_CONTAINER_ID.equals(containerId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(CONTAINERS.remove(containerId));
    }

    public static Collection<AbilitySlotContainerDefinition> allContainers() {
        return findContainer(PRIMARY_CONTAINER_ID)
                .map(List::of)
                .orElse(List.of());
    }

    public static Optional<AbilitySlotContainerDefinition> findContainer(ResourceLocation containerId) {
        if (!PRIMARY_CONTAINER_ID.equals(containerId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(CONTAINERS.get(PRIMARY_CONTAINER_ID));
    }

    public static List<AbilitySlotContainerDefinition> visibleContainers(@Nullable Player player, AbilityData data) {
        return findContainer(PRIMARY_CONTAINER_ID)
                .map(List::of)
                .orElse(List.of());
    }

    public static List<AbilitySlotContainerDefinition> editableContainers(@Nullable Player player, AbilityData data) {
        return visibleContainers(player, data);
    }

    public static int resolvedSlotsPerPage(AbilityData data, ResourceLocation containerId) {
        return PRIMARY_CONTAINER_ID.equals(containerId) ? AbilityData.SLOT_COUNT : 0;
    }

    public static int resolvedPageCount(AbilityData data, ResourceLocation containerId) {
        return PRIMARY_CONTAINER_ID.equals(containerId) ? 1 : 0;
    }

    public static @Nullable ResourceLocation controlProfileId(ResourceLocation containerId) {
        return PRIMARY_CONTAINER_ID.equals(containerId)
                ? findContainer(PRIMARY_CONTAINER_ID).map(AbilitySlotContainerDefinition::controlProfileId).orElse(null)
                : null;
    }

    public static boolean hasResolvedContainer(AbilityData data, ResourceLocation containerId) {
        return PRIMARY_CONTAINER_ID.equals(containerId);
    }

    public static boolean isPrimarySlotReference(AbilitySlotReference slotReference) {
        Objects.requireNonNull(slotReference, "slotReference");
        return PRIMARY_CONTAINER_ID.equals(slotReference.containerId())
                && slotReference.pageIndex() == 0
                && slotReference.slotIndex() >= 0
                && slotReference.slotIndex() < AbilityData.SLOT_COUNT;
    }

    public static boolean isValidSlotReference(AbilityData data, AbilitySlotReference slotReference) {
        return isPrimarySlotReference(slotReference);
    }
}

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

public final class AbilityControlProfileApi {
    public static final ResourceLocation PRIMARY_NUMBER_ROW_PROFILE_ID = ResourceLocation.fromNamespaceAndPath(XLib.MODID, "primary_number_row");

    private static final Comparator<AbilityControlProfile> PRIORITY_ORDER = Comparator
            .comparingInt(AbilityControlProfile::priority)
            .reversed()
            .thenComparing(profile -> profile.id().toString());
    private static final Map<ResourceLocation, AbilityControlProfile> PROFILES = new LinkedHashMap<>();

    private AbilityControlProfileApi() {}

    public static void bootstrap() {
        if (PROFILES.containsKey(PRIMARY_NUMBER_ROW_PROFILE_ID)) {
            return;
        }

        AbilityControlProfile.Builder primaryProfile = AbilityControlProfile.builder(
                PRIMARY_NUMBER_ROW_PROFILE_ID,
                Component.translatable("control_profile.xlib.primary_number_row")
        );
        for (int slot = 0; slot < AbilityData.SLOT_COUNT; slot++) {
            primaryProfile.bind(
                    AbilityControlTrigger.numberRow(slot),
                    AbilityControlAction.activateSlot(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID, slot)
            );
        }
        registerProfile(primaryProfile.build());
    }

    public static AbilityControlProfile registerProfile(AbilityControlProfile profile) {
        XLibRegistryGuard.ensureMutable("ability_control_profiles");
        AbilityControlProfile resolvedProfile = Objects.requireNonNull(profile, "profile");
        AbilityControlProfile previous = PROFILES.putIfAbsent(resolvedProfile.id(), resolvedProfile);
        if (previous != null) {
            throw new IllegalStateException("Duplicate control profile registration: " + resolvedProfile.id());
        }
        return resolvedProfile;
    }

    public static Optional<AbilityControlProfile> unregisterProfile(ResourceLocation profileId) {
        XLibRegistryGuard.ensureMutable("ability_control_profiles");
        if (PRIMARY_NUMBER_ROW_PROFILE_ID.equals(profileId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(PROFILES.remove(profileId));
    }

    public static Optional<AbilityControlProfile> findProfile(ResourceLocation profileId) {
        return Optional.ofNullable(PROFILES.get(profileId));
    }

    public static Collection<AbilityControlProfile> allProfiles() {
        return PROFILES.values().stream().sorted(PRIORITY_ORDER).toList();
    }

    public static List<AbilityControlBinding> activeBindings(@Nullable Player player, AbilityData data) {
        List<AbilityControlBinding> bindings = new java.util.ArrayList<>();
        List<AbilityControlProfile> profiles = AbilitySlotContainerApi.visibleContainers(player, data).stream()
                .map(AbilitySlotContainerDefinition::controlProfileId)
                .filter(Objects::nonNull)
                .map(AbilityControlProfileApi::findProfile)
                .flatMap(Optional::stream)
                .sorted(PRIORITY_ORDER)
                .toList();
        for (AbilityControlProfile profile : profiles) {
            bindings.addAll(profile.bindings());
        }
        return List.copyOf(bindings);
    }
}

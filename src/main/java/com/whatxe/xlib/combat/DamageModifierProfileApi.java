package com.whatxe.xlib.combat;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.XLibRegistryGuard;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

public final class DamageModifierProfileApi {
    public static final ResourceLocation COMMAND_SOURCE =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "command_damage_modifier_profile");

    private static final Map<ResourceLocation, DamageModifierProfileDefinition> PROFILES = new LinkedHashMap<>();

    private DamageModifierProfileApi() {}

    public static void bootstrap() {}

    public static DamageModifierProfileDefinition register(DamageModifierProfileDefinition profile) {
        XLibRegistryGuard.ensureMutable("damage_modifier_profiles");
        DamageModifierProfileDefinition previous = PROFILES.putIfAbsent(profile.id(), profile);
        if (previous != null) {
            throw new IllegalStateException("Duplicate damage modifier profile registration: " + profile.id());
        }
        return profile;
    }

    public static Optional<DamageModifierProfileDefinition> unregister(ResourceLocation profileId) {
        XLibRegistryGuard.ensureMutable("damage_modifier_profiles");
        return Optional.ofNullable(PROFILES.remove(profileId));
    }

    public static Optional<DamageModifierProfileDefinition> find(ResourceLocation profileId) {
        return Optional.ofNullable(resolvedProfiles().get(profileId));
    }

    public static Collection<DamageModifierProfileDefinition> all() {
        return List.copyOf(resolvedProfiles().values());
    }

    public static List<DamageModifierProfileDefinition> activeProfiles(Player player) {
        return activeProfiles(getData(player));
    }

    public static List<DamageModifierProfileDefinition> activeProfiles(DamageModifierProfileData data) {
        return data.activeProfiles().stream()
                .map(resolvedProfiles()::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public static Set<ResourceLocation> sourcesFor(Player player, ResourceLocation profileId) {
        return getData(player).sourcesFor(profileId);
    }

    public static double incomingMultiplier(Player player, DamageSource source) {
        return incomingMultiplier(getData(player), source);
    }

    public static double incomingMultiplier(DamageModifierProfileData data, DamageSource source) {
        return resolveMultiplier(activeProfiles(data), source, true);
    }

    public static double outgoingMultiplier(Player player, DamageSource source) {
        return outgoingMultiplier(getData(player), source);
    }

    public static double outgoingMultiplier(DamageModifierProfileData data, DamageSource source) {
        return resolveMultiplier(activeProfiles(data), source, false);
    }

    public static double incomingFlat(Player player, DamageSource source) {
        return incomingFlat(getData(player), source);
    }

    public static double incomingFlat(DamageModifierProfileData data, DamageSource source) {
        double total = 0.0D;
        for (DamageModifierProfileDefinition profile : activeProfiles(data)) {
            total += profile.incomingFlat(source);
        }
        return total;
    }

    public static double outgoingFlat(Player player, DamageSource source) {
        return outgoingFlat(getData(player), source);
    }

    public static double outgoingFlat(DamageModifierProfileData data, DamageSource source) {
        double total = 0.0D;
        for (DamageModifierProfileDefinition profile : activeProfiles(data)) {
            total += profile.outgoingFlat(source);
        }
        return total;
    }

    public static float applyIncoming(Player player, DamageSource source, float amount) {
        return clampAmount(amount * (float) incomingMultiplier(player, source));
    }

    /**
     * Applies incoming multiplier and then flat additions: {@code (amount * multiplier) + flat},
     * clamped to 0.
     */
    public static float applyIncomingWithFlat(Player player, DamageSource source, float amount) {
        DamageModifierProfileData data = getData(player);
        double multiplied = amount * incomingMultiplier(data, source);
        double withFlat = multiplied + incomingFlat(data, source);
        return clampAmount((float) withFlat);
    }

    public static float applyOutgoing(Player player, DamageSource source, float amount) {
        return clampAmount(amount * (float) outgoingMultiplier(player, source));
    }

    /**
     * Applies outgoing multiplier and then flat additions: {@code (amount * multiplier) + flat},
     * clamped to 0.
     */
    public static float applyOutgoingWithFlat(Player player, DamageSource source, float amount) {
        DamageModifierProfileData data = getData(player);
        double multiplied = amount * outgoingMultiplier(data, source);
        double withFlat = multiplied + outgoingFlat(data, source);
        return clampAmount((float) withFlat);
    }

    public static DamageModifierProfileData getData(Player player) {
        return player.getData(ModAttachments.PLAYER_DAMAGE_MODIFIER_PROFILE);
    }

    public static void setData(Player player, DamageModifierProfileData data) {
        player.setData(ModAttachments.PLAYER_DAMAGE_MODIFIER_PROFILE, sanitize(data));
    }

    public static DamageModifierProfileData sanitize(DamageModifierProfileData data) {
        return data.retainRegistered(resolvedProfiles().keySet());
    }

    public static void grant(Player player, ResourceLocation profileId) {
        grant(player, profileId, COMMAND_SOURCE);
    }

    public static void grant(Player player, ResourceLocation profileId, ResourceLocation sourceId) {
        update(player, getData(player).withProfileSource(profileId, sourceId, true));
    }

    public static void grant(Player player, Collection<ResourceLocation> profileIds, ResourceLocation sourceId) {
        DamageModifierProfileData data = getData(player);
        for (ResourceLocation profileId : new LinkedHashSet<>(profileIds)) {
            data = data.withProfileSource(profileId, sourceId, true);
        }
        update(player, data);
    }

    public static void revoke(Player player, ResourceLocation profileId) {
        revoke(player, profileId, COMMAND_SOURCE);
    }

    public static void revoke(Player player, ResourceLocation profileId, ResourceLocation sourceId) {
        update(player, getData(player).withProfileSource(profileId, sourceId, false));
    }

    public static void clearProfiles(Player player) {
        update(player, DamageModifierProfileData.empty());
    }

    public static void syncSourceProfiles(Player player, ResourceLocation sourceId, Collection<ResourceLocation> profileIds) {
        DamageModifierProfileData data = getData(player);
        Set<ResourceLocation> desiredProfiles = new LinkedHashSet<>(profileIds);
        for (ResourceLocation profileId : Set.copyOf(data.activeProfiles())) {
            if (data.sourcesFor(profileId).contains(sourceId) && !desiredProfiles.contains(profileId)) {
                data = data.withProfileSource(profileId, sourceId, false);
            }
        }
        for (ResourceLocation profileId : desiredProfiles) {
            data = data.withProfileSource(profileId, sourceId, true);
        }
        update(player, data);
    }

    public static DamageModifierProfileData revokeSourceProfiles(DamageModifierProfileData data, ResourceLocation sourceId) {
        return data.clearProfileSource(sourceId);
    }

    private static void update(Player player, DamageModifierProfileData data) {
        DamageModifierProfileData current = getData(player);
        if (!data.equals(current)) {
            setData(player, data);
        }
    }

    private static double resolveMultiplier(
            List<DamageModifierProfileDefinition> profiles,
            DamageSource source,
            boolean incoming
    ) {
        if (profiles.isEmpty()) {
            return 1.0D;
        }

        // Determine the dominant merge mode: prefer the first non-MULTIPLICATIVE mode found, or
        // pick the highest-priority profile's mode when OVERRIDE is involved.
        DamageModifierProfileMergeMode mode = DamageModifierProfileMergeMode.MULTIPLICATIVE;
        for (DamageModifierProfileDefinition profile : profiles) {
            if (profile.mergeMode() != DamageModifierProfileMergeMode.MULTIPLICATIVE) {
                mode = profile.mergeMode();
                break;
            }
        }

        switch (mode) {
            case ADDITIVE -> {
                double sum = 0.0D;
                for (DamageModifierProfileDefinition profile : profiles) {
                    double m = incoming ? profile.incomingMultiplier(source) : profile.outgoingMultiplier(source);
                    // Each profile contributes its deviation from 1.0 (the default)
                    sum += m;
                }
                return sum;
            }
            case OVERRIDE -> {
                DamageModifierProfileDefinition highest = null;
                for (DamageModifierProfileDefinition profile : profiles) {
                    if (highest == null || profile.priority() > highest.priority()) {
                        highest = profile;
                    }
                }
                return incoming ? highest.incomingMultiplier(source) : highest.outgoingMultiplier(source);
            }
            default -> {
                // MULTIPLICATIVE — original behavior
                double multiplier = 1.0D;
                for (DamageModifierProfileDefinition profile : profiles) {
                    multiplier *= incoming ? profile.incomingMultiplier(source) : profile.outgoingMultiplier(source);
                }
                return multiplier;
            }
        }
    }

    private static float clampAmount(float amount) {
        if (Float.isNaN(amount) || Float.isInfinite(amount) || amount < 0.0F) {
            return 0.0F;
        }
        return amount;
    }

    private static Map<ResourceLocation, DamageModifierProfileDefinition> resolvedProfiles() {
        LinkedHashMap<ResourceLocation, DamageModifierProfileDefinition> resolved = new LinkedHashMap<>(PROFILES);
        resolved.putAll(DataDrivenDamageModifierProfileApi.definitions());
        return Map.copyOf(resolved);
    }
}

package com.whatxe.xlib.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record ProfileSelectionData(
        Map<ResourceLocation, ProfileSelectionEntry> selections,
        Map<ResourceLocation, ProfilePendingSelection> pendingGroups,
        Map<ResourceLocation, Integer> resetCounts,
        Map<ResourceLocation, String> lastResetReasons,
        boolean firstLoginHandled
) {
    private static final Codec<Map<ResourceLocation, ProfileSelectionEntry>> SELECTION_MAP_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, ProfileSelectionEntry.CODEC);
    private static final Codec<Map<ResourceLocation, ProfilePendingSelection>> PENDING_MAP_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, ProfilePendingSelection.CODEC);
    private static final Codec<Map<ResourceLocation, Integer>> RESET_COUNT_MAP_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT);
    private static final Codec<Map<ResourceLocation, String>> STRING_MAP_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.STRING);

    public static final Codec<ProfileSelectionData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SELECTION_MAP_CODEC.optionalFieldOf("selections", Map.of()).forGetter(ProfileSelectionData::selections),
            PENDING_MAP_CODEC.optionalFieldOf("pending_groups", Map.of()).forGetter(ProfileSelectionData::pendingGroups),
            RESET_COUNT_MAP_CODEC.optionalFieldOf("reset_counts", Map.of()).forGetter(ProfileSelectionData::resetCounts),
            STRING_MAP_CODEC.optionalFieldOf("last_reset_reasons", Map.of()).forGetter(ProfileSelectionData::lastResetReasons),
            Codec.BOOL.optionalFieldOf("first_login_handled", false).forGetter(ProfileSelectionData::firstLoginHandled)
    ).apply(instance, ProfileSelectionData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProfileSelectionData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public ProfileSelectionData {
        selections = Map.copyOf(normalizeSelections(selections));
        pendingGroups = Map.copyOf(normalizePending(pendingGroups));
        resetCounts = Map.copyOf(normalizeResetCounts(resetCounts));
        lastResetReasons = Map.copyOf(normalizeReasons(lastResetReasons));
    }

    public static ProfileSelectionData empty() {
        return new ProfileSelectionData(Map.of(), Map.of(), Map.of(), Map.of(), false);
    }

    public Set<ResourceLocation> selectedProfileIds() {
        return Set.copyOf(this.selections.keySet());
    }

    public Set<ResourceLocation> pendingGroupIds() {
        return Set.copyOf(this.pendingGroups.keySet());
    }

    public Optional<ProfileSelectionEntry> selection(ResourceLocation profileId) {
        return Optional.ofNullable(this.selections.get(profileId));
    }

    public Optional<ProfilePendingSelection> pendingGroup(ResourceLocation groupId) {
        return Optional.ofNullable(this.pendingGroups.get(groupId));
    }

    public boolean hasPendingGroup(ResourceLocation groupId) {
        return this.pendingGroups.containsKey(groupId);
    }

    public int resetCount(ResourceLocation groupId) {
        return this.resetCounts.getOrDefault(groupId, 0);
    }

    public String lastResetReason(ResourceLocation groupId) {
        return this.lastResetReasons.getOrDefault(groupId, "");
    }

    public ProfileSelectionData withSelection(ResourceLocation profileId, ProfileSelectionEntry entry) {
        Map<ResourceLocation, ProfileSelectionEntry> updated = new LinkedHashMap<>(this.selections);
        if (profileId != null && entry != null) {
            updated.put(profileId, entry);
        } else if (profileId != null) {
            updated.remove(profileId);
        }
        return new ProfileSelectionData(updated, this.pendingGroups, this.resetCounts, this.lastResetReasons, this.firstLoginHandled);
    }

    public ProfileSelectionData withoutSelection(ResourceLocation profileId) {
        if (!this.selections.containsKey(profileId)) {
            return this;
        }
        Map<ResourceLocation, ProfileSelectionEntry> updated = new LinkedHashMap<>(this.selections);
        updated.remove(profileId);
        return new ProfileSelectionData(updated, this.pendingGroups, this.resetCounts, this.lastResetReasons, this.firstLoginHandled);
    }

    public ProfileSelectionData clearGroupSelections(ResourceLocation groupId) {
        Map<ResourceLocation, ProfileSelectionEntry> updated = new LinkedHashMap<>(this.selections);
        updated.entrySet().removeIf(entry -> entry.getValue().groupId().equals(groupId));
        if (updated.equals(this.selections)) {
            return this;
        }
        return new ProfileSelectionData(updated, this.pendingGroups, this.resetCounts, this.lastResetReasons, this.firstLoginHandled);
    }

    public ProfileSelectionData withPendingGroup(ResourceLocation groupId, ProfilePendingSelection pendingSelection) {
        Map<ResourceLocation, ProfilePendingSelection> updated = new LinkedHashMap<>(this.pendingGroups);
        if (groupId != null && pendingSelection != null) {
            updated.put(groupId, pendingSelection);
        } else if (groupId != null) {
            updated.remove(groupId);
        }
        return new ProfileSelectionData(this.selections, updated, this.resetCounts, this.lastResetReasons, this.firstLoginHandled);
    }

    public ProfileSelectionData clearPendingGroup(ResourceLocation groupId) {
        if (!this.pendingGroups.containsKey(groupId)) {
            return this;
        }
        Map<ResourceLocation, ProfilePendingSelection> updated = new LinkedHashMap<>(this.pendingGroups);
        updated.remove(groupId);
        return new ProfileSelectionData(this.selections, updated, this.resetCounts, this.lastResetReasons, this.firstLoginHandled);
    }

    public ProfileSelectionData withResetCount(ResourceLocation groupId, int count) {
        Map<ResourceLocation, Integer> updated = new LinkedHashMap<>(this.resetCounts);
        if (count > 0) {
            updated.put(groupId, count);
        } else {
            updated.remove(groupId);
        }
        return new ProfileSelectionData(this.selections, this.pendingGroups, updated, this.lastResetReasons, this.firstLoginHandled);
    }

    public ProfileSelectionData withLastResetReason(ResourceLocation groupId, String reason) {
        Map<ResourceLocation, String> updated = new LinkedHashMap<>(this.lastResetReasons);
        String normalized = reason == null ? "" : reason.trim();
        if (normalized.isEmpty()) {
            updated.remove(groupId);
        } else {
            updated.put(groupId, normalized);
        }
        return new ProfileSelectionData(this.selections, this.pendingGroups, this.resetCounts, updated, this.firstLoginHandled);
    }

    public ProfileSelectionData withFirstLoginHandled(boolean handled) {
        return handled == this.firstLoginHandled
                ? this
                : new ProfileSelectionData(this.selections, this.pendingGroups, this.resetCounts, this.lastResetReasons, handled);
    }

    private static Map<ResourceLocation, ProfileSelectionEntry> normalizeSelections(Map<ResourceLocation, ProfileSelectionEntry> values) {
        Map<ResourceLocation, ProfileSelectionEntry> normalized = new LinkedHashMap<>();
        if (values == null) {
            return normalized;
        }
        values.forEach((key, value) -> {
            if (key != null && value != null) {
                normalized.put(key, value);
            }
        });
        return normalized;
    }

    private static Map<ResourceLocation, ProfilePendingSelection> normalizePending(Map<ResourceLocation, ProfilePendingSelection> values) {
        Map<ResourceLocation, ProfilePendingSelection> normalized = new LinkedHashMap<>();
        if (values == null) {
            return normalized;
        }
        values.forEach((key, value) -> {
            if (key != null && value != null) {
                normalized.put(key, value);
            }
        });
        return normalized;
    }

    private static Map<ResourceLocation, Integer> normalizeResetCounts(Map<ResourceLocation, Integer> values) {
        Map<ResourceLocation, Integer> normalized = new LinkedHashMap<>();
        if (values == null) {
            return normalized;
        }
        values.forEach((key, value) -> {
            if (key != null && value != null && value > 0) {
                normalized.put(key, value);
            }
        });
        return normalized;
    }

    private static Map<ResourceLocation, String> normalizeReasons(Map<ResourceLocation, String> values) {
        Map<ResourceLocation, String> normalized = new LinkedHashMap<>();
        if (values == null) {
            return normalized;
        }
        values.forEach((key, value) -> {
            if (key != null && value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    normalized.put(key, trimmed);
                }
            }
        });
        return normalized;
    }
}

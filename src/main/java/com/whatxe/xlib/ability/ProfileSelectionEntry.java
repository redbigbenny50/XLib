package com.whatxe.xlib.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record ProfileSelectionEntry(
        ResourceLocation groupId,
        ProfileSelectionOrigin origin,
        boolean locked,
        String reason
) {
    public static final Codec<ProfileSelectionEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("group").forGetter(ProfileSelectionEntry::groupId),
            ProfileSelectionOrigin.CODEC.optionalFieldOf("origin", ProfileSelectionOrigin.PLAYER).forGetter(ProfileSelectionEntry::origin),
            Codec.BOOL.optionalFieldOf("locked", false).forGetter(ProfileSelectionEntry::locked),
            Codec.STRING.optionalFieldOf("reason", "").forGetter(ProfileSelectionEntry::reason)
    ).apply(instance, ProfileSelectionEntry::new));

    public ProfileSelectionEntry {
        Objects.requireNonNull(groupId, "groupId");
        origin = origin == null ? ProfileSelectionOrigin.PLAYER : origin;
        reason = reason == null ? "" : reason.trim();
    }

    public boolean adminSet() {
        return this.origin.isAdminSet();
    }

    public boolean delegated() {
        return this.origin.isDelegated();
    }

    public boolean temporary() {
        return this.origin.isTemporary();
    }
}

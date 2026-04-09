package com.whatxe.xlib.ability;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public record GrantSourceDescriptor(
        ResourceLocation sourceId,
        GrantSourceKind kind,
        Optional<ResourceLocation> primaryId,
        Optional<ResourceLocation> backingSourceId,
        Set<ResourceLocation> identities,
        Set<ResourceLocation> grantBundles,
        Optional<UUID> grantorId,
        boolean managed,
        String reason,
        String disappearsWhen
) {
    public GrantSourceDescriptor {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(kind, "kind");
        primaryId = primaryId == null ? Optional.empty() : primaryId;
        backingSourceId = backingSourceId == null ? Optional.empty() : backingSourceId;
        identities = Set.copyOf(identities);
        grantBundles = Set.copyOf(grantBundles);
        grantorId = grantorId == null ? Optional.empty() : grantorId;
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(disappearsWhen, "disappearsWhen");
    }

    public static GrantSourceDescriptor of(
            ResourceLocation sourceId,
            GrantSourceKind kind,
            ResourceLocation primaryId,
            ResourceLocation backingSourceId,
            Set<ResourceLocation> identities,
            Set<ResourceLocation> grantBundles,
            UUID grantorId,
            boolean managed,
            String reason,
            String disappearsWhen
    ) {
        return new GrantSourceDescriptor(
                sourceId,
                kind,
                Optional.ofNullable(primaryId),
                Optional.ofNullable(backingSourceId),
                identities,
                grantBundles,
                Optional.ofNullable(grantorId),
                managed,
                reason,
                disappearsWhen
        );
    }
}

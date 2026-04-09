package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLib;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class DelegatedGrantApi {
    private static final String DELEGATION_PATH_PREFIX = "delegation/";

    private DelegatedGrantApi() {}

    public static ResourceLocation sourceIdFor(UUID grantorId, ResourceLocation bundleId) {
        java.util.Objects.requireNonNull(grantorId, "grantorId");
        java.util.Objects.requireNonNull(bundleId, "bundleId");
        return ResourceLocation.fromNamespaceAndPath(
                XLib.MODID,
                DELEGATION_PATH_PREFIX + grantorId + "/" + bundleId.getNamespace() + "/" + bundleId.getPath()
        );
    }

    public static ResourceLocation sourceIdFor(ServerPlayer grantor, ResourceLocation bundleId) {
        return sourceIdFor(grantor.getUUID(), bundleId);
    }

    public static void grantBundle(ServerPlayer grantor, Player target, ResourceLocation bundleId) {
        GrantBundleApi.syncSourceBundles(target, sourceIdFor(grantor, bundleId), java.util.List.of(bundleId));
    }

    public static void revokeBundle(ServerPlayer grantor, Player target, ResourceLocation bundleId) {
        GrantBundleApi.clearManagedSourceBundles(target, sourceIdFor(grantor, bundleId));
    }

    public static Optional<DelegatedSource> parseSource(ResourceLocation sourceId) {
        if (!XLib.MODID.equals(sourceId.getNamespace()) || !sourceId.getPath().startsWith(DELEGATION_PATH_PREFIX)) {
            return Optional.empty();
        }
        String suffix = sourceId.getPath().substring(DELEGATION_PATH_PREFIX.length());
        String[] parts = suffix.split("/", 3);
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(new DelegatedSource(
                    UUID.fromString(parts[0]),
                    ResourceLocation.fromNamespaceAndPath(parts[1], parts[2])
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public record DelegatedSource(UUID grantorId, ResourceLocation bundleId) {}
}

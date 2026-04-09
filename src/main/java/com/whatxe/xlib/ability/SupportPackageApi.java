package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.XLibRegistryGuard;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class SupportPackageApi {
    private static final String SUPPORT_PACKAGE_PATH_PREFIX = "support_package/";

    private static final Map<ResourceLocation, SupportPackageDefinition> SUPPORT_PACKAGES = new LinkedHashMap<>();

    private SupportPackageApi() {}

    public static void bootstrap() {}

    public static SupportPackageDefinition registerSupportPackage(SupportPackageDefinition supportPackage) {
        XLibRegistryGuard.ensureMutable("support_packages");
        SupportPackageDefinition previous = SUPPORT_PACKAGES.putIfAbsent(supportPackage.id(), supportPackage);
        if (previous != null) {
            throw new IllegalStateException("Duplicate support package registration: " + supportPackage.id());
        }
        return supportPackage;
    }

    public static Optional<SupportPackageDefinition> unregisterSupportPackage(ResourceLocation supportPackageId) {
        XLibRegistryGuard.ensureMutable("support_packages");
        return Optional.ofNullable(SUPPORT_PACKAGES.remove(supportPackageId));
    }

    public static Optional<SupportPackageDefinition> findSupportPackage(ResourceLocation supportPackageId) {
        return Optional.ofNullable(SUPPORT_PACKAGES.get(supportPackageId));
    }

    public static Collection<SupportPackageDefinition> allSupportPackages() {
        return List.copyOf(SUPPORT_PACKAGES.values());
    }

    public static ResourceLocation sourceIdFor(UUID supporterId, ResourceLocation supportPackageId) {
        return ResourceLocation.fromNamespaceAndPath(
                XLib.MODID,
                SUPPORT_PACKAGE_PATH_PREFIX + supporterId + "/" + supportPackageId.getNamespace() + "/" + supportPackageId.getPath()
        );
    }

    public static ResourceLocation sourceIdFor(ServerPlayer supporter, ResourceLocation supportPackageId) {
        return sourceIdFor(supporter.getUUID(), supportPackageId);
    }

    public static boolean canApply(Entity supporter, Entity target, ResourceLocation supportPackageId) {
        SupportPackageDefinition supportPackage = SUPPORT_PACKAGES.get(supportPackageId);
        return supportPackage != null && canApply(supporter, target, supportPackage);
    }

    public static boolean canApply(Entity supporter, Entity target, SupportPackageDefinition supportPackage) {
        if (!supportPackage.allowSelf() && supporter.getUUID().equals(target.getUUID())) {
            return false;
        }
        return supportPackage.requiredRelationships().isEmpty()
                || EntityRelationshipApi.matchesAny(target, supporter, supportPackage.requiredRelationships());
    }

    public static boolean apply(ServerPlayer supporter, Player target, ResourceLocation supportPackageId) {
        SupportPackageDefinition supportPackage = SUPPORT_PACKAGES.get(supportPackageId);
        if (supportPackage == null || !canApply(supporter, target, supportPackage)) {
            return false;
        }
        GrantBundleApi.syncSourceBundles(target, sourceIdFor(supporter, supportPackageId), supportPackage.grantBundles());
        return true;
    }

    public static void revoke(ServerPlayer supporter, Player target, ResourceLocation supportPackageId) {
        GrantBundleApi.clearManagedSourceBundles(target, sourceIdFor(supporter, supportPackageId));
    }

    public static Optional<SupportSource> parseSource(ResourceLocation sourceId) {
        if (!XLib.MODID.equals(sourceId.getNamespace()) || !sourceId.getPath().startsWith(SUPPORT_PACKAGE_PATH_PREFIX)) {
            return Optional.empty();
        }
        String suffix = sourceId.getPath().substring(SUPPORT_PACKAGE_PATH_PREFIX.length());
        String[] parts = suffix.split("/", 3);
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(new SupportSource(
                    UUID.fromString(parts[0]),
                    ResourceLocation.fromNamespaceAndPath(parts[1], parts[2])
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public record SupportSource(UUID supporterId, ResourceLocation supportPackageId) {}
}

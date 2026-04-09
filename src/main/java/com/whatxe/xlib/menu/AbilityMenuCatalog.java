package com.whatxe.xlib.menu;

import com.whatxe.xlib.ability.AbilityDefinition;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class AbilityMenuCatalog {
    private AbilityMenuCatalog() {}

    public static List<ResourceLocation> availablePages(Collection<AbilityDefinition> abilities) {
        return Objects.requireNonNull(abilities, "abilities").stream()
                .map(AbilityDefinition::pageId)
                .flatMap(java.util.Optional::stream)
                .distinct()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }

    public static List<ResourceLocation> availableGroups(Collection<AbilityDefinition> abilities, @Nullable ResourceLocation pageId) {
        return Objects.requireNonNull(abilities, "abilities").stream()
                .filter(ability -> matches(ability.pageId(), pageId))
                .map(AbilityDefinition::groupId)
                .flatMap(java.util.Optional::stream)
                .distinct()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }

    public static List<ResourceLocation> availableFamilies(
            Collection<AbilityDefinition> abilities,
            @Nullable ResourceLocation pageId,
            @Nullable ResourceLocation groupId
    ) {
        return Objects.requireNonNull(abilities, "abilities").stream()
                .filter(ability -> matches(ability.pageId(), pageId))
                .filter(ability -> matches(ability.groupId(), groupId))
                .map(AbilityDefinition::familyId)
                .flatMap(java.util.Optional::stream)
                .distinct()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }

    public static Scope sanitizeScope(Collection<AbilityDefinition> abilities, Scope requestedScope) {
        Objects.requireNonNull(abilities, "abilities");
        Scope resolvedScope = Objects.requireNonNull(requestedScope, "requestedScope");
        ResourceLocation pageId = resolvedScope.pageId() != null && availablePages(abilities).contains(resolvedScope.pageId())
                ? resolvedScope.pageId()
                : null;
        ResourceLocation groupId = resolvedScope.groupId() != null && availableGroups(abilities, pageId).contains(resolvedScope.groupId())
                ? resolvedScope.groupId()
                : null;
        ResourceLocation familyId = resolvedScope.familyId() != null
                && availableFamilies(abilities, pageId, groupId).contains(resolvedScope.familyId())
                ? resolvedScope.familyId()
                : null;
        return new Scope(pageId, groupId, familyId);
    }

    public static List<AbilityDefinition> filter(Collection<AbilityDefinition> abilities, Scope scope) {
        Scope resolvedScope = Objects.requireNonNull(scope, "scope");
        return Objects.requireNonNull(abilities, "abilities").stream()
                .filter(ability -> matches(ability.pageId(), resolvedScope.pageId()))
                .filter(ability -> matches(ability.groupId(), resolvedScope.groupId()))
                .filter(ability -> matches(ability.familyId(), resolvedScope.familyId()))
                .toList();
    }

    public static Comparator<AbilityDefinition> catalogComparator() {
        return Comparator.comparing(AbilityMenuCatalog::pageSortKey)
                .thenComparing(AbilityMenuCatalog::groupSortKey)
                .thenComparing(AbilityMenuCatalog::familySortKey)
                .thenComparing((AbilityDefinition ability) -> ability.displayName().getString().toLowerCase(java.util.Locale.ROOT))
                .thenComparing(ability -> ability.id().toString());
    }

    private static boolean matches(java.util.Optional<ResourceLocation> maybeId, @Nullable ResourceLocation selectedId) {
        return selectedId == null || maybeId.filter(selectedId::equals).isPresent();
    }

    private static String pageSortKey(AbilityDefinition ability) {
        return sortKey(ability.pageId());
    }

    private static String groupSortKey(AbilityDefinition ability) {
        return sortKey(ability.groupId());
    }

    private static String familySortKey(AbilityDefinition ability) {
        return sortKey(ability.familyId());
    }

    private static String sortKey(java.util.Optional<ResourceLocation> maybeId) {
        return maybeId.map(ResourceLocation::toString).orElse("~");
    }

    public record Scope(
            @Nullable ResourceLocation pageId,
            @Nullable ResourceLocation groupId,
            @Nullable ResourceLocation familyId
    ) {
        public static final Scope ALL = new Scope(null, null, null);
    }
}

package com.whatxe.xlib.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.AbilityIcon;
import com.whatxe.xlib.ability.AbilityUseResult;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class AbilityMenuCatalogTest {
    private static final ResourceLocation ALPHA_STRIKE_ID = id("alpha_strike");
    private static final ResourceLocation ALPHA_SUPPORT_ID = id("alpha_support");
    private static final ResourceLocation BETA_GUARD_ID = id("beta_guard");
    private static final ResourceLocation UNGROUPED_ID = id("ungrouped");
    private static final ResourceLocation PAGE_ALPHA = id("page/alpha");
    private static final ResourceLocation PAGE_BETA = id("page/beta");
    private static final ResourceLocation GROUP_OFFENSE = id("group/offense");
    private static final ResourceLocation GROUP_SUPPORT = id("group/support");
    private static final ResourceLocation GROUP_DEFENSE = id("group/defense");
    private static final ResourceLocation FAMILY_STRIKER = id("family/striker");
    private static final ResourceLocation FAMILY_SUPPORT = id("family/support");
    private static final ResourceLocation FAMILY_GUARDIAN = id("family/guardian");

    @Test
    void availableScopesRespectPageGroupAndFamilyHierarchy() {
        List<AbilityDefinition> abilities = List.of(
                ability(ALPHA_STRIKE_ID, PAGE_ALPHA, GROUP_OFFENSE, FAMILY_STRIKER),
                ability(ALPHA_SUPPORT_ID, PAGE_ALPHA, GROUP_SUPPORT, FAMILY_SUPPORT),
                ability(BETA_GUARD_ID, PAGE_BETA, GROUP_DEFENSE, FAMILY_GUARDIAN)
        );

        assertEquals(List.of(PAGE_ALPHA, PAGE_BETA), AbilityMenuCatalog.availablePages(abilities));
        assertEquals(
                List.of(GROUP_OFFENSE, GROUP_SUPPORT),
                AbilityMenuCatalog.availableGroups(abilities, PAGE_ALPHA)
        );
        assertEquals(
                List.of(FAMILY_STRIKER),
                AbilityMenuCatalog.availableFamilies(abilities, PAGE_ALPHA, GROUP_OFFENSE)
        );
        assertEquals(
                new AbilityMenuCatalog.Scope(PAGE_ALPHA, null, null),
                AbilityMenuCatalog.sanitizeScope(
                        abilities,
                        new AbilityMenuCatalog.Scope(PAGE_ALPHA, GROUP_DEFENSE, FAMILY_GUARDIAN)
                )
        );
    }

    @Test
    void catalogComparatorOrdersByMetadataBeforeNameAndLeavesUngroupedEntriesLast() {
        List<AbilityDefinition> abilities = List.of(
                ability(BETA_GUARD_ID, PAGE_BETA, GROUP_DEFENSE, FAMILY_GUARDIAN),
                ability(ALPHA_STRIKE_ID, PAGE_ALPHA, GROUP_OFFENSE, FAMILY_STRIKER),
                ability(UNGROUPED_ID, null, null, null)
        );

        List<ResourceLocation> orderedIds = AbilityMenuCatalog.filter(abilities, AbilityMenuCatalog.Scope.ALL).stream()
                .sorted(AbilityMenuCatalog.catalogComparator())
                .map(AbilityDefinition::id)
                .toList();

        assertEquals(List.of(ALPHA_STRIKE_ID, BETA_GUARD_ID, UNGROUPED_ID), orderedIds);
    }

    private static AbilityDefinition ability(
            ResourceLocation abilityId,
            ResourceLocation pageId,
            ResourceLocation groupId,
            ResourceLocation familyId
    ) {
        AbilityDefinition.Builder builder = AbilityDefinition.builder(abilityId, AbilityIcon.ofTexture(id("icon/" + abilityId.getPath())))
                .action((player, data) -> AbilityUseResult.success(data));
        if (pageId != null) {
            builder.page(pageId);
        }
        if (groupId != null) {
            builder.group(groupId);
        }
        if (familyId != null) {
            builder.family(familyId);
        }
        return builder.build();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}

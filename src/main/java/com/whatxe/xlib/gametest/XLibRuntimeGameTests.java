package com.whatxe.xlib.gametest;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.AbilityCombatTracker;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.AbilityEndReason;
import com.whatxe.xlib.ability.AbilityIcon;
import com.whatxe.xlib.ability.AbilityGrantApi;
import com.whatxe.xlib.ability.AbilityLoadoutApi;
import com.whatxe.xlib.ability.AbilityResourceDefinition;
import com.whatxe.xlib.ability.AbilityRequirements;
import com.whatxe.xlib.ability.AbilityRuntime;
import com.whatxe.xlib.ability.AbilityUseResult;
import com.whatxe.xlib.ability.ComboChainApi;
import com.whatxe.xlib.ability.ComboChainDefinition;
import com.whatxe.xlib.ability.ContextGrantApi;
import com.whatxe.xlib.ability.GrantedItemApi;
import com.whatxe.xlib.ability.GrantedItemDefinition;
import com.whatxe.xlib.ability.GrantedItemGrantApi;
import com.whatxe.xlib.ability.GrantedItemRuntime;
import com.whatxe.xlib.ability.GrantBundleApi;
import com.whatxe.xlib.ability.GrantBundleDefinition;
import com.whatxe.xlib.ability.ControlledEntityApi;
import com.whatxe.xlib.ability.EntityRelationshipApi;
import com.whatxe.xlib.ability.IdentityApi;
import com.whatxe.xlib.ability.IdentityDefinition;
import com.whatxe.xlib.ability.ModeApi;
import com.whatxe.xlib.ability.ModeDefinition;
import com.whatxe.xlib.ability.PassiveApi;
import com.whatxe.xlib.ability.PassiveDefinition;
import com.whatxe.xlib.ability.PassiveGrantApi;
import com.whatxe.xlib.ability.RecipePermissionApi;
import com.whatxe.xlib.ability.RestrictedRecipeDefinition;
import com.whatxe.xlib.ability.RestrictedRecipeRule;
import com.whatxe.xlib.ability.SimpleContextGrantProvider;
import com.whatxe.xlib.ability.SupportPackageApi;
import com.whatxe.xlib.ability.SupportPackageDefinition;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.event.AbilityGameplayHooks;
import com.whatxe.xlib.event.AbilityItemHooks;
import com.whatxe.xlib.event.GrantedItemHooks;
import com.whatxe.xlib.progression.UpgradeApi;
import com.whatxe.xlib.progression.UpgradeConsumeRule;
import com.whatxe.xlib.progression.UpgradeKillRule;
import com.whatxe.xlib.progression.UpgradeNodeDefinition;
import com.whatxe.xlib.progression.UpgradePointType;
import com.whatxe.xlib.progression.UpgradeRewardBundle;
import com.whatxe.xlib.progression.UpgradeRequirements;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@GameTestHolder(XLib.MODID)
@PrefixGameTestTemplate(false)
public final class XLibRuntimeGameTests {
    private XLibRuntimeGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void modAttachmentsSetSanitizesRemovedAbility(GameTestHelper helper) {
        ResourceLocation abilityId = id("attachment_sanitize");
        AbilityApi.unregisterAbility(abilityId);

        AbilityDefinition definition = AbilityDefinition.builder(abilityId, AbilityIcon.ofTexture(id("attachment_icon")))
                .action((player, data) -> AbilityUseResult.success(data))
                .build();
        AbilityApi.registerAbility(definition);

        ServerPlayer player = GameTestPlayerFactory.create(helper);
        AbilityData dirtyData = AbilityData.empty()
                .withAbilityInSlot(0, abilityId)
                .withCooldown(abilityId, 20);

        AbilityApi.unregisterAbility(abilityId);
        ModAttachments.set(player, dirtyData);

        AbilityData sanitized = ModAttachments.get(player);
        helper.assertTrue(sanitized.abilityInSlot(0).isEmpty(), "Removed abilities should be cleared from player slots");
        helper.assertTrue(sanitized.cooldowns().isEmpty(), "Removed abilities should be cleared from player cooldowns");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void grantedItemHooksPreventDroppingManagedItems(GameTestHelper helper) {
        ResourceLocation grantedItemId = id("undroppable_item");
        GrantedItemApi.unregisterGrantedItem(grantedItemId);

        GrantedItemApi.registerGrantedItem(GrantedItemDefinition.builder(grantedItemId, Items.STICK)
                .undroppable()
                .build());

        ServerPlayer player = GameTestPlayerFactory.create(helper);
        GrantedItemGrantApi.grant(player, grantedItemId);

        int slot = findGrantedItemSlot(player, grantedItemId);
        helper.assertTrue(slot >= 0, "Granted item should be inserted into the player inventory");

        ItemStack droppedStack = player.getInventory().items.get(slot).copy();
        player.getInventory().items.set(slot, ItemStack.EMPTY);

        ItemEntity itemEntity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), droppedStack);
        ItemTossEvent event = new ItemTossEvent(itemEntity, player);
        GrantedItemHooks.onItemToss(event);

        helper.assertTrue(event.isCanceled(), "Undroppable managed items should cancel toss events");
        helper.assertTrue(findGrantedItemSlot(player, grantedItemId) >= 0, "Canceled tosses should restore the managed item to inventory");

        GrantedItemGrantApi.revoke(player, grantedItemId);
        helper.assertTrue(findGrantedItemSlot(player, grantedItemId) < 0, "Revoked managed items should be removed from inventory");

        GrantedItemApi.unregisterGrantedItem(grantedItemId);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void regrantingOwnedUndroppableItemsRestoresMissingPhysicalCopy(GameTestHelper helper) {
        ResourceLocation grantedItemId = id("regrant_restore_item");
        GrantedItemApi.unregisterGrantedItem(grantedItemId);

        try {
            GrantedItemApi.registerGrantedItem(GrantedItemDefinition.builder(grantedItemId, Items.SHIELD)
                    .undroppable()
                    .build());

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            GrantedItemGrantApi.grant(player, grantedItemId);

            int slot = findGrantedItemSlot(player, grantedItemId);
            helper.assertTrue(slot >= 0, "Granted undroppable item should be present before re-grant recovery");
            player.getInventory().items.set(slot, ItemStack.EMPTY);

            GrantedItemGrantApi.grant(player, grantedItemId);

            helper.assertTrue(
                    findGrantedItemSlot(player, grantedItemId) >= 0,
                    "Granting an already-owned undroppable item should restore its missing physical stack"
            );
        } finally {
            GrantedItemApi.unregisterGrantedItem(grantedItemId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void undroppableGrantedItemsAreReclaimedFromOpenContainers(GameTestHelper helper) {
        ResourceLocation grantedItemId = id("container_reclaim_item");
        GrantedItemApi.unregisterGrantedItem(grantedItemId);

        try {
            GrantedItemApi.registerGrantedItem(GrantedItemDefinition.builder(grantedItemId, Items.TOTEM_OF_UNDYING)
                    .undroppable()
                    .build());

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            GrantedItemGrantApi.grant(player, grantedItemId);

            int slot = findGrantedItemSlot(player, grantedItemId);
            helper.assertTrue(slot >= 0, "Granted undroppable item should start in the player inventory");

            ItemStack managedStack = player.getInventory().items.get(slot).copy();
            player.getInventory().items.set(slot, ItemStack.EMPTY);

            SimpleContainer chest = new SimpleContainer(1);
            chest.setItem(0, managedStack);
            player.containerMenu = new TestExternalStorageMenu(chest);

            GrantedItemRuntime.tick(player, ModAttachments.get(player));

            helper.assertTrue(chest.getItem(0).isEmpty(), "Undroppable managed items should be removed from open external containers");
            helper.assertTrue(
                    findGrantedItemSlot(player, grantedItemId) >= 0,
                    "Undroppable managed items reclaimed from containers should return to the player inventory"
            );
        } finally {
            GrantedItemApi.unregisterGrantedItem(grantedItemId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void blockExternalStoragePreventsInsertionIntoGuardedSlots(GameTestHelper helper) {
        ResourceLocation grantedItemId = id("container_block_item");
        GrantedItemApi.unregisterGrantedItem(grantedItemId);

        try {
            GrantedItemApi.registerGrantedItem(GrantedItemDefinition.builder(grantedItemId, Items.NETHER_STAR)
                    .blockExternalStorage()
                    .build());

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            GrantedItemGrantApi.grant(player, grantedItemId);

            int slot = findGrantedItemSlot(player, grantedItemId);
            helper.assertTrue(slot >= 0, "Blocked-storage managed item should start in the player inventory");

            ItemStack managedStack = player.getInventory().items.get(slot).copy();
            SimpleContainer chest = new SimpleContainer(1);
            TestExternalStorageMenu menu = new TestExternalStorageMenu(chest);
            player.containerMenu = menu;

            GrantedItemRuntime.guardStorageMenu(player, menu);

            ItemStack remainder = menu.getSlot(0).safeInsert(managedStack.copy());

            helper.assertTrue(!menu.getSlot(0).mayPlace(managedStack), "Guarded external slots should reject blockExternalStorage managed items");
            helper.assertTrue(chest.getItem(0).isEmpty(), "Blocked managed items should not insert into guarded external storage slots");
            helper.assertTrue(
                    ItemStack.isSameItemSameComponents(remainder, managedStack) && remainder.getCount() == managedStack.getCount(),
                    "Blocked external storage inserts should leave the carried stack unchanged"
            );
        } finally {
            GrantedItemApi.unregisterGrantedItem(grantedItemId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void blockExternalStorageCancelsStackedOnOtherInteractions(GameTestHelper helper) {
        ResourceLocation grantedItemId = id("container_stack_block_item");
        GrantedItemApi.unregisterGrantedItem(grantedItemId);

        try {
            GrantedItemApi.registerGrantedItem(GrantedItemDefinition.builder(grantedItemId, Items.AMETHYST_SHARD)
                    .blockExternalStorage()
                    .build());

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            GrantedItemGrantApi.grant(player, grantedItemId);

            int slot = findGrantedItemSlot(player, grantedItemId);
            helper.assertTrue(slot >= 0, "Blocked-storage managed item should start in the player inventory");

            ItemStack managedStack = player.getInventory().items.get(slot).copy();
            SimpleContainer chest = new SimpleContainer(1);
            TestExternalStorageMenu menu = new TestExternalStorageMenu(chest);
            player.containerMenu = menu;

            ItemStackedOnOtherEvent event = new ItemStackedOnOtherEvent(
                    managedStack,
                    ItemStack.EMPTY,
                    menu.getSlot(0),
                    ClickAction.PRIMARY,
                    player,
                    SlotAccess.NULL
            );
            GrantedItemHooks.onItemStackedOnOther(event);

            helper.assertTrue(event.isCanceled(), "Block-external-storage managed items should cancel stacked-on-other insertion attempts");
        } finally {
            GrantedItemApi.unregisterGrantedItem(grantedItemId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void revokedManagedItemsAreRemovedFromOpenContainers(GameTestHelper helper) {
        ResourceLocation grantedItemId = id("container_revoke_item");
        GrantedItemApi.unregisterGrantedItem(grantedItemId);

        try {
            GrantedItemApi.registerGrantedItem(GrantedItemDefinition.builder(grantedItemId, Items.IRON_SWORD).build());

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            GrantedItemGrantApi.grant(player, grantedItemId);

            int slot = findGrantedItemSlot(player, grantedItemId);
            helper.assertTrue(slot >= 0, "Granted managed item should start in the player inventory");

            ItemStack managedStack = player.getInventory().items.get(slot).copy();
            player.getInventory().items.set(slot, ItemStack.EMPTY);

            SimpleContainer chest = new SimpleContainer(1);
            chest.setItem(0, managedStack);
            player.containerMenu = new TestExternalStorageMenu(chest);

            GrantedItemGrantApi.revoke(player, grantedItemId);

            helper.assertTrue(chest.getItem(0).isEmpty(), "Revoked managed items should be removed from open external containers");
            helper.assertTrue(
                    findGrantedItemSlot(player, grantedItemId) < 0,
                    "Revoked managed items should not remain in the player inventory after open-container cleanup"
            );
        } finally {
            GrantedItemApi.unregisterGrantedItem(grantedItemId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void ownerBoundManagedItemsIgnoreAnotherPlayersOpenContainer(GameTestHelper helper) {
        ResourceLocation grantedItemId = id("container_owner_bound_item");
        GrantedItemApi.unregisterGrantedItem(grantedItemId);

        try {
            GrantedItemApi.registerGrantedItem(GrantedItemDefinition.builder(grantedItemId, Items.COMPASS)
                    .undroppable()
                    .build());

            ServerPlayer owner = GameTestPlayerFactory.create(helper);
            ServerPlayer otherPlayer = GameTestPlayerFactory.create(helper);
            GrantedItemGrantApi.grant(owner, grantedItemId);
            GrantedItemGrantApi.grant(otherPlayer, grantedItemId);

            int ownerSlot = findGrantedItemSlot(owner, grantedItemId);
            helper.assertTrue(ownerSlot >= 0, "Owner should receive the managed item before the shared-container check");

            ItemStack ownerStack = owner.getInventory().items.get(ownerSlot).copy();
            owner.getInventory().items.set(ownerSlot, ItemStack.EMPTY);

            SimpleContainer chest = new SimpleContainer(1);
            chest.setItem(0, ownerStack);

            otherPlayer.containerMenu = new TestExternalStorageMenu(chest);
            GrantedItemRuntime.tick(otherPlayer, ModAttachments.get(otherPlayer));

            helper.assertTrue(
                    !chest.getItem(0).isEmpty(),
                    "Another player with the same granted item id should not reclaim the owner's marked stack from shared storage"
            );

            owner.containerMenu = new TestExternalStorageMenu(chest);
            GrantedItemRuntime.tick(owner, ModAttachments.get(owner));

            helper.assertTrue(chest.getItem(0).isEmpty(), "The owning player should reclaim their marked managed stack from shared storage");
            helper.assertTrue(
                    findGrantedItemSlot(owner, grantedItemId) >= 0,
                    "Owner-bound managed items reclaimed from shared storage should return to the owner's inventory"
            );
        } finally {
            GrantedItemApi.unregisterGrantedItem(grantedItemId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void recipeRestrictionsClearResultSlot(GameTestHelper helper) {
        ResourceLocation recipeId = ResourceLocation.withDefaultNamespace("oak_planks");
        RecipePermissionApi.registerRestrictedRecipe(recipeId);

        ServerPlayer player = GameTestPlayerFactory.create(helper);
        RecipeHolder<?> recipe = helper.getLevel().getServer().getRecipeManager().byKey(recipeId).orElseThrow();
        TestRecipeMenu menu = new TestRecipeMenu(recipe, new ItemStack(Items.OAK_PLANKS, 4));
        player.containerMenu = menu;

        RecipePermissionApi.enforceOpenCraftingResult(player, ModAttachments.get(player));

        helper.assertTrue(menu.resultContainer.getItem(0).isEmpty(), "Restricted recipe outputs should be cleared from the result slot");
        helper.assertTrue(menu.resultContainer.getRecipeUsed() == null, "Restricted recipe outputs should clear the tracked recipe");

        RecipePermissionApi.unregisterRestrictedRecipe(recipeId);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void restrictedCraftingResultSlotsDenyPickup(GameTestHelper helper) {
        ResourceLocation recipeId = ResourceLocation.withDefaultNamespace("golden_apple");
        RecipePermissionApi.unregisterRestrictedRecipe(recipeId);

        try {
            RecipePermissionApi.registerRestrictedRecipe(recipeId);

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            CraftingMenu menu = new CraftingMenu(1, player.getInventory());
            player.containerMenu = menu;
            RecipePermissionApi.guardCraftingMenu(player, menu);

            RecipeHolder<?> recipe = helper.getLevel().getServer().getRecipeManager().byKey(recipeId).orElseThrow();
            ResultContainer resultContainer = (ResultContainer) menu.getSlot(0).container;
            resultContainer.setRecipeUsed(recipe);
            resultContainer.setItem(0, new ItemStack(Items.GOLDEN_APPLE));

            menu.clicked(0, 0, ClickType.PICKUP, player);

            helper.assertTrue(menu.getCarried().isEmpty(), "Locked crafting result slots should not place restricted outputs on the cursor");
            helper.assertTrue(resultContainer.getItem(0).isEmpty(), "Locked crafting result slots should clear the restricted output");
            helper.assertTrue(resultContainer.getRecipeUsed() == null, "Locked crafting result slots should clear the tracked denied recipe");
        } finally {
            RecipePermissionApi.unregisterRestrictedRecipe(recipeId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void runtimeRecipeRestrictionSyncGuardsAlreadyOpenMenus(GameTestHelper helper) {
        ResourceLocation recipeId = ResourceLocation.withDefaultNamespace("golden_apple");
        RecipePermissionApi.unregisterRestrictedRecipe(recipeId);

        ServerPlayer player = GameTestPlayerFactory.create(helper);
        CraftingMenu menu = new CraftingMenu(1, player.getInventory());
        player.containerMenu = menu;

        try {
            RecipePermissionApi.registerRestrictedRecipe(recipeId);
            RecipePermissionApi.syncOnlinePlayers();

            RecipeHolder<?> recipe = helper.getLevel().getServer().getRecipeManager().byKey(recipeId).orElseThrow();
            ResultContainer resultContainer = (ResultContainer) menu.getSlot(0).container;
            resultContainer.setRecipeUsed(recipe);
            resultContainer.setItem(0, new ItemStack(Items.GOLDEN_APPLE));

            menu.clicked(0, 0, ClickType.PICKUP, player);

            helper.assertTrue(
                    menu.getCarried().isEmpty(),
                    "Runtime recipe restriction sync should guard already-open crafting menus before restricted results are picked up"
            );
            helper.assertTrue(
                    resultContainer.getItem(0).isEmpty(),
                    "Runtime recipe restriction sync should clear denied results from already-open crafting menus"
            );
            helper.assertTrue(
                    resultContainer.getRecipeUsed() == null,
                    "Runtime recipe restriction sync should clear the tracked denied recipe from already-open crafting menus"
            );
        } finally {
            RecipePermissionApi.unregisterRestrictedRecipe(recipeId);
            RecipePermissionApi.syncOnlinePlayers();
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void selectorRestrictedRecipesPopulateResolvedMetadataCache(GameTestHelper helper) {
        ResourceLocation recipeId = ResourceLocation.withDefaultNamespace("oak_planks");
        ResourceLocation outputId = ResourceLocation.withDefaultNamespace("oak_planks");
        ResourceLocation ruleId = id("resolved_rule_cache");
        RecipePermissionApi.unregisterRestrictedRule(ruleId);

        try {
            RecipePermissionApi.registerRestrictedRule(RestrictedRecipeRule.builder(ruleId)
                    .output(outputId)
                    .unlockSource(id("resolved_rule_source"))
                    .unlockHint(Component.literal("Resolved from selector rule"))
                    .hiddenWhenLocked(false)
                    .build());

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            RestrictedRecipeDefinition definition = RecipePermissionApi.findRestrictedRecipe(player, recipeId).orElseThrow();

            helper.assertTrue(
                    RecipePermissionApi.restrictedRecipes(player).contains(recipeId),
                    "Selector-matched recipes should appear in the resolved restricted recipe set"
            );
            helper.assertTrue(
                    RecipePermissionApi.recipesForOutput(outputId).contains(recipeId),
                    "Selector-matched recipes should populate the cached output index used by metadata queries"
            );
            helper.assertTrue(
                    definition.outputs().contains(outputId),
                    "Selector-matched recipes should expose their resolved output metadata through the cached definition"
            );
            helper.assertTrue(
                    "Resolved from selector rule".equals(definition.unlockHint().getString()),
                    "Selector-matched recipes should preserve cached rule metadata such as unlock hints"
            );
            helper.assertTrue(!definition.hiddenWhenLocked(), "Selector-matched recipes should preserve cached hidden-when-locked metadata");
        } finally {
            RecipePermissionApi.unregisterRestrictedRule(ruleId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void recipeCommandsRestrictGrantRevokeAndUnrestrict(GameTestHelper helper) {
        ResourceLocation recipeId = ResourceLocation.withDefaultNamespace("oak_planks");
        RecipePermissionApi.unregisterRestrictedRecipe(recipeId);

        try {
            ServerPlayer player = GameTestPlayerFactory.create(helper);

            executeAsPlayerCommand(player, "xlib recipes restrict minecraft:oak_planks false");
            helper.assertTrue(RecipePermissionApi.isRestricted(recipeId), "Recipe restrict command should register the recipe as restricted");
            helper.assertTrue(
                    RecipePermissionApi.accessState(player, recipeId) == RecipePermissionApi.RecipeAccessState.LOCKED,
                    "Recipe restrict command should lock the recipe for players without a permission source"
            );
            helper.assertTrue(
                    RecipePermissionApi.findRestrictedRecipe(recipeId).orElseThrow().hiddenWhenLocked() == false,
                    "Recipe restrict command should keep the requested hidden_when_locked metadata"
            );

            executeAsPlayerCommand(player, "xlib recipes grant @s minecraft:oak_planks");
            helper.assertTrue(
                    RecipePermissionApi.accessState(player, recipeId) == RecipePermissionApi.RecipeAccessState.ALLOWED,
                    "Recipe grant command should allow the targeted player to craft the restricted recipe"
            );
            helper.assertTrue(
                    RecipePermissionApi.findRestrictedRecipe(recipeId).orElseThrow().hiddenWhenLocked() == false,
                    "Recipe grant command should not overwrite existing restriction metadata"
            );

            executeAsPlayerCommand(player, "xlib recipes revoke @s minecraft:oak_planks");
            helper.assertTrue(
                    RecipePermissionApi.accessState(player, recipeId) == RecipePermissionApi.RecipeAccessState.LOCKED,
                    "Recipe revoke command should remove the targeted player's permission source"
            );
            helper.assertTrue(
                    RecipePermissionApi.findRestrictedRecipe(recipeId).orElseThrow().hiddenWhenLocked() == false,
                    "Recipe revoke flows should preserve the original restriction metadata after access is removed"
            );

            executeAsPlayerCommand(player, "xlib recipes unrestrict minecraft:oak_planks");
            helper.assertTrue(
                    RecipePermissionApi.accessState(player, recipeId) == RecipePermissionApi.RecipeAccessState.UNRESTRICTED,
                    "Recipe unrestrict command should remove the runtime restriction entirely"
            );
        } finally {
            RecipePermissionApi.unregisterRestrictedRecipe(recipeId);
            RecipePermissionApi.syncOnlinePlayers();
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void jumpHookAppliesPassiveEffects(GameTestHelper helper) {
        ResourceLocation passiveId = id("jump_passive");
        PassiveApi.unregisterPassive(passiveId);

        PassiveApi.registerPassive(PassiveDefinition.builder(passiveId, AbilityIcon.ofTexture(id("jump_icon")))
                .onJump((player, data) -> data.withAbilityAccessRestricted(true))
                .build());

        ServerPlayer player = GameTestPlayerFactory.create(helper);
        PassiveGrantApi.grant(player, passiveId);
        AbilityGameplayHooks.onJump(new LivingEvent.LivingJumpEvent(player));

        helper.assertTrue(ModAttachments.get(player).abilityAccessRestricted(), "Passive jump hooks should update synced player state");

        PassiveGrantApi.revoke(player, passiveId);
        PassiveApi.unregisterPassive(passiveId);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void contextualProvidersSyncAndPruneManagedSources(GameTestHelper helper) {
        ResourceLocation grantedAbilityId = id("contextual_grant");
        ResourceLocation blockedAbilityId = id("contextual_block");
        ResourceLocation providerId = id("contextual_provider");
        ResourceLocation sourceId = id("contextual_source");

        AbilityApi.unregisterAbility(grantedAbilityId);
        AbilityApi.unregisterAbility(blockedAbilityId);
        ContextGrantApi.unregisterProvider(providerId);

        AbilityApi.registerAbility(AbilityDefinition.builder(grantedAbilityId, AbilityIcon.ofTexture(id("contextual_grant_icon")))
                .action((player, data) -> AbilityUseResult.success(data))
                .build());
        AbilityApi.registerAbility(AbilityDefinition.builder(blockedAbilityId, AbilityIcon.ofTexture(id("contextual_block_icon")))
                .action((player, data) -> AbilityUseResult.success(data))
                .build());
        ContextGrantApi.registerProvider(SimpleContextGrantProvider.builder(providerId, sourceId)
                .grantAbility(grantedAbilityId)
                .blockAbility(blockedAbilityId)
                .build());

        ServerPlayer player = GameTestPlayerFactory.create(helper);
        AbilityItemHooks.onPlayerTick(new PlayerTickEvent.Post(player));

        AbilityData grantedData = ModAttachments.get(player);
        helper.assertTrue(grantedData.grantedAbilities().contains(grantedAbilityId), "Context providers should grant abilities while active");
        helper.assertTrue(grantedData.activationBlockSourcesFor(blockedAbilityId).contains(sourceId), "Context providers should sync activation blocks");
        helper.assertTrue(grantedData.managedGrantSources().contains(sourceId), "Context providers should register their managed source");
        helper.assertTrue(AbilityGrantApi.grantSources(player, grantedAbilityId).contains(sourceId), "Granted abilities should retain their contextual source");

        ContextGrantApi.unregisterProvider(providerId);
        AbilityItemHooks.onPlayerTick(new PlayerTickEvent.Post(player));

        AbilityData prunedData = ModAttachments.get(player);
        helper.assertTrue(!prunedData.grantedAbilities().contains(grantedAbilityId), "Removing a context provider should prune its ability grant");
        helper.assertTrue(!prunedData.isAbilityActivationBlocked(blockedAbilityId), "Removing a context provider should prune its activation block");
        helper.assertTrue(!prunedData.managedGrantSources().contains(sourceId), "Removing a context provider should prune the managed source");

        AbilityApi.unregisterAbility(grantedAbilityId);
        AbilityApi.unregisterAbility(blockedAbilityId);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void transformingModesEndParentAndStartChild(GameTestHelper helper) {
        ResourceLocation parentModeId = id("mode_parent");
        ResourceLocation childModeId = id("mode_child");

        AbilityApi.unregisterAbility(parentModeId);
        AbilityApi.unregisterAbility(childModeId);
        ModeApi.unregisterMode(parentModeId);
        ModeApi.unregisterMode(childModeId);

        try {
            AbilityDefinition parentMode = AbilityDefinition.builder(parentModeId, AbilityIcon.ofTexture(id("mode_parent_icon")))
                    .toggleAbility()
                    .durationTicks(80)
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build();
            AbilityDefinition childMode = AbilityDefinition.builder(childModeId, AbilityIcon.ofTexture(id("mode_child_icon")))
                    .toggleAbility()
                    .durationTicks(80)
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build();
            AbilityApi.registerAbility(parentMode);
            AbilityApi.registerAbility(childMode);

            ModeApi.registerMode(ModeDefinition.builder(parentModeId).build());
            ModeApi.registerMode(ModeDefinition.builder(childModeId)
                    .transformsFrom(parentModeId)
                    .build());

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            AbilityUseResult parentResult = AbilityRuntime.activate(player, AbilityData.empty(), parentMode, 0);
            helper.assertTrue(parentResult.data().isModeActive(parentModeId), "Parent mode should activate successfully");

            AbilityUseResult childResult = AbilityRuntime.activate(player, parentResult.data(), childMode, 0);
            helper.assertTrue(childResult.data().isModeActive(childModeId), "Child transform mode should activate successfully");
            helper.assertTrue(!childResult.data().isModeActive(parentModeId), "Transforming into a child mode should end the parent mode");
            helper.assertTrue(
                    childResult.data().activeDurationFor(childModeId) == childMode.durationTicks(),
                    "Transform child mode should receive its configured duration"
            );
        } finally {
            ModeApi.unregisterMode(parentModeId);
            ModeApi.unregisterMode(childModeId);
            AbilityApi.unregisterAbility(parentModeId);
            AbilityApi.unregisterAbility(childModeId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void exclusiveModesReportReplacementEndReason(GameTestHelper helper) {
        ResourceLocation parentModeId = id("exclusive_parent");
        ResourceLocation childModeId = id("exclusive_child");
        AbilityEndReason[] capturedReason = new AbilityEndReason[1];

        AbilityApi.unregisterAbility(parentModeId);
        AbilityApi.unregisterAbility(childModeId);
        ModeApi.unregisterMode(parentModeId);
        ModeApi.unregisterMode(childModeId);

        try {
            AbilityDefinition parentMode = AbilityDefinition.builder(parentModeId, AbilityIcon.ofTexture(id("exclusive_parent_icon")))
                    .toggleAbility()
                    .durationTicks(80)
                    .action((player, data) -> AbilityUseResult.success(data))
                    .ender((player, data, reason) -> {
                        capturedReason[0] = reason;
                        return AbilityUseResult.success(data);
                    })
                    .build();
            AbilityDefinition childMode = AbilityDefinition.builder(childModeId, AbilityIcon.ofTexture(id("exclusive_child_icon")))
                    .toggleAbility()
                    .durationTicks(80)
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build();
            AbilityApi.registerAbility(parentMode);
            AbilityApi.registerAbility(childMode);

            ModeApi.registerMode(ModeDefinition.builder(parentModeId).build());
            ModeApi.registerMode(ModeDefinition.builder(childModeId)
                    .exclusiveWith(parentModeId)
                    .build());

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            AbilityUseResult parentResult = AbilityRuntime.activate(player, AbilityData.empty(), parentMode, 0);
            AbilityUseResult childResult = AbilityRuntime.activate(player, parentResult.data(), childMode, 0);

            helper.assertTrue(
                    capturedReason[0] == AbilityEndReason.REPLACED_BY_EXCLUSIVE,
                    "Exclusive mode replacements should expose REPLACED_BY_EXCLUSIVE to the ended mode"
            );
            helper.assertTrue(childResult.data().isModeActive(childModeId), "Exclusive replacement child mode should activate successfully");
            helper.assertTrue(!childResult.data().isModeActive(parentModeId), "Exclusive replacement should end the previous mode");
        } finally {
            ModeApi.unregisterMode(parentModeId);
            ModeApi.unregisterMode(childModeId);
            AbilityApi.unregisterAbility(parentModeId);
            AbilityApi.unregisterAbility(childModeId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void modeCycleGroupsRequireResetBeforeReuse(GameTestHelper helper) {
        ResourceLocation cycleGroupId = id("stance_cycle_group");
        ResourceLocation cycleModeId = id("cycle_mode");
        ResourceLocation resetModeId = id("cycle_reset_mode");

        AbilityApi.unregisterAbility(cycleModeId);
        AbilityApi.unregisterAbility(resetModeId);
        ModeApi.unregisterMode(cycleModeId);
        ModeApi.unregisterMode(resetModeId);

        try {
            AbilityDefinition cycleMode = AbilityDefinition.builder(cycleModeId, AbilityIcon.ofTexture(id("cycle_mode_icon")))
                    .toggleAbility()
                    .durationTicks(40)
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build();
            AbilityDefinition resetMode = AbilityDefinition.builder(resetModeId, AbilityIcon.ofTexture(id("cycle_reset_icon")))
                    .toggleAbility()
                    .durationTicks(40)
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build();
            AbilityApi.registerAbility(cycleMode);
            AbilityApi.registerAbility(resetMode);

            ModeApi.registerMode(ModeDefinition.builder(cycleModeId)
                    .cycleGroup(cycleGroupId)
                    .stackable()
                    .build());
            ModeApi.registerMode(ModeDefinition.builder(resetModeId)
                    .stackable()
                    .resetCycleGroupOnActivate(cycleGroupId)
                    .build());

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            AbilityUseResult firstUse = AbilityRuntime.activate(player, AbilityData.empty(), cycleMode, 0);
            AbilityUseResult toggledOff = AbilityRuntime.activate(player, firstUse.data(), cycleMode, 0);
            AbilityUseResult blockedReuse = AbilityRuntime.activate(player, toggledOff.data(), cycleMode, 0);

            helper.assertTrue(
                    toggledOff.data().modeCycleHistoryFor(cycleGroupId).contains(cycleModeId),
                    "Mode cycle groups should record which stance abilities have already been used in the current cycle"
            );
            helper.assertTrue(!blockedReuse.consumed(), "Used cycle-group modes should stay blocked until their cycle is reset");

            AbilityUseResult resetResult = AbilityRuntime.activate(player, toggledOff.data(), resetMode, 0);
            helper.assertTrue(
                    resetResult.data().modeCycleHistoryFor(cycleGroupId).isEmpty(),
                    "Reset-trigger modes should clear the configured cycle group on activation"
            );

            AbilityUseResult secondUse = AbilityRuntime.activate(player, resetResult.data(), cycleMode, 0);
            helper.assertTrue(secondUse.consumed(), "Reset cycle groups should allow the spent mode to activate again");
        } finally {
            ModeApi.unregisterMode(cycleModeId);
            ModeApi.unregisterMode(resetModeId);
            AbilityApi.unregisterAbility(cycleModeId);
            AbilityApi.unregisterAbility(resetModeId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void activeModesUsePlayerPresetLoadouts(GameTestHelper helper) {
        ResourceLocation baseAbilityId = id("preset_base");
        ResourceLocation modeAbilityId = id("preset_mode");
        ResourceLocation modePresetAbilityId = id("preset_mode_slot");
        ResourceLocation overlayAbilityId = id("preset_overlay_slot");

        AbilityApi.unregisterAbility(baseAbilityId);
        AbilityApi.unregisterAbility(modeAbilityId);
        AbilityApi.unregisterAbility(modePresetAbilityId);
        AbilityApi.unregisterAbility(overlayAbilityId);
        ModeApi.unregisterMode(modeAbilityId);

        try {
            AbilityApi.registerAbility(AbilityDefinition.builder(baseAbilityId, AbilityIcon.ofTexture(id("preset_base_icon")))
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build());
            AbilityApi.registerAbility(AbilityDefinition.builder(modeAbilityId, AbilityIcon.ofTexture(id("preset_mode_icon")))
                    .toggleAbility()
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build());
            AbilityApi.registerAbility(AbilityDefinition.builder(modePresetAbilityId, AbilityIcon.ofTexture(id("preset_mode_slot_icon")))
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build());
            AbilityApi.registerAbility(AbilityDefinition.builder(overlayAbilityId, AbilityIcon.ofTexture(id("preset_overlay_slot_icon")))
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build());

            ModeApi.registerMode(ModeDefinition.builder(modeAbilityId)
                    .overlayAbility(1, overlayAbilityId)
                    .build());

            AbilityData data = AbilityData.empty()
                    .withAbilityInSlot(0, baseAbilityId)
                    .withModeAbilityInSlot(modeAbilityId, 0, modePresetAbilityId)
                    .withMode(modeAbilityId, true);

            helper.assertTrue(
                    AbilityLoadoutApi.resolvedAbilityId(data, 0).filter(modePresetAbilityId::equals).isPresent(),
                    "Active modes should resolve player-authored preset abilities before falling back to the base loadout"
            );
            helper.assertTrue(
                    AbilityLoadoutApi.resolvedAbilityId(data, 1).filter(overlayAbilityId::equals).isPresent(),
                    "Static mode overlays should still win on slots explicitly owned by the mode definition"
            );
        } finally {
            ModeApi.unregisterMode(modeAbilityId);
            AbilityApi.unregisterAbility(baseAbilityId);
            AbilityApi.unregisterAbility(modeAbilityId);
            AbilityApi.unregisterAbility(modePresetAbilityId);
            AbilityApi.unregisterAbility(overlayAbilityId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void cooldownScalingSupportsPassivesAndStackableModes(GameTestHelper helper) {
        ResourceLocation cooldownAbilityId = id("scaled_cooldown_ability");
        ResourceLocation chargeAbilityId = id("scaled_charge_ability");
        ResourceLocation passiveId = id("cooldown_speed_passive");
        ResourceLocation modeAbilityId = id("cooldown_speed_mode");
        ResourceLocation passiveSourceId = id("cooldown_speed_passive_source");

        AbilityApi.unregisterAbility(cooldownAbilityId);
        AbilityApi.unregisterAbility(chargeAbilityId);
        AbilityApi.unregisterAbility(modeAbilityId);
        PassiveApi.unregisterPassive(passiveId);
        ModeApi.unregisterMode(modeAbilityId);

        try {
            AbilityDefinition cooldownAbility = AbilityDefinition.builder(cooldownAbilityId, AbilityIcon.ofTexture(id("scaled_cooldown_icon")))
                    .cooldownTicks(10)
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build();
            AbilityDefinition chargeAbility = AbilityDefinition.builder(chargeAbilityId, AbilityIcon.ofTexture(id("scaled_charge_icon")))
                    .charges(2, 10)
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build();
            AbilityDefinition modeAbility = AbilityDefinition.builder(modeAbilityId, AbilityIcon.ofTexture(id("cooldown_speed_mode_icon")))
                    .toggleAbility()
                    .durationTicks(80)
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build();
            PassiveDefinition passive = PassiveDefinition.builder(passiveId, AbilityIcon.ofTexture(id("cooldown_speed_passive_icon")))
                    .cooldownTickRateMultiplier(1.5D)
                    .build();

            AbilityApi.registerAbility(cooldownAbility);
            AbilityApi.registerAbility(chargeAbility);
            AbilityApi.registerAbility(modeAbility);
            PassiveApi.registerPassive(passive);
            ModeApi.registerMode(ModeDefinition.builder(modeAbilityId)
                    .stackable()
                    .cooldownTickRateMultiplier(2.0D)
                    .build());

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            AbilityData passiveScaledData = AbilityData.empty()
                    .withCooldown(cooldownAbilityId, 10)
                    .withChargeCount(chargeAbilityId, 0)
                    .withChargeRecharge(chargeAbilityId, 10)
                    .withPassiveGrantSource(passiveId, passiveSourceId, true);

            passiveScaledData = AbilityRuntime.tick(player, passiveScaledData);
            passiveScaledData = AbilityRuntime.tick(player, passiveScaledData);
            helper.assertTrue(
                    passiveScaledData.cooldownFor(cooldownAbilityId) == 7,
                    "Passive cooldown multipliers should preserve fractional progress across ticks"
            );
            helper.assertTrue(
                    passiveScaledData.chargeRechargeFor(chargeAbilityId) == 7,
                    "Passive cooldown multipliers should also speed up charge recharges"
            );

            AbilityData stackedData = AbilityData.empty()
                    .withCooldown(cooldownAbilityId, 10)
                    .withChargeCount(chargeAbilityId, 0)
                    .withChargeRecharge(chargeAbilityId, 10)
                    .withPassiveGrantSource(passiveId, passiveSourceId, true)
                    .withMode(modeAbilityId, true);
            stackedData = AbilityRuntime.tick(player, stackedData);

            helper.assertTrue(
                    ModeApi.findMode(modeAbilityId).map(ModeDefinition::stackable).orElse(false),
                    "Stackable modes should expose explicit overlay intent in their definition"
            );
            helper.assertTrue(stackedData.cooldownFor(cooldownAbilityId) == 7, "Stacked mode and passive cooldown multipliers should combine multiplicatively");
            helper.assertTrue(stackedData.chargeRechargeFor(chargeAbilityId) == 7, "Combined cooldown multipliers should apply to charge recharges too");
        } finally {
            ModeApi.unregisterMode(modeAbilityId);
            PassiveApi.unregisterPassive(passiveId);
            AbilityApi.unregisterAbility(cooldownAbilityId);
            AbilityApi.unregisterAbility(chargeAbilityId);
            AbilityApi.unregisterAbility(modeAbilityId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void chargeReleaseAbilitiesReportHeldTicksAndEndReason(GameTestHelper helper) {
        ResourceLocation abilityId = id("charge_release_ability");
        int[] toggledChargeTicks = {-1};
        AbilityEndReason[] toggledReason = new AbilityEndReason[1];
        int[] expiredChargeTicks = {-1};
        AbilityEndReason[] expiredReason = new AbilityEndReason[1];

        AbilityApi.unregisterAbility(abilityId);

        try {
            AbilityDefinition ability = AbilityDefinition.builder(abilityId, AbilityIcon.ofTexture(id("charge_release_icon")))
                    .chargeRelease(5, (player, data, reason, chargedTicks, maxChargeTicks) -> {
                        if (reason == AbilityEndReason.PLAYER_TOGGLED) {
                            toggledReason[0] = reason;
                            toggledChargeTicks[0] = chargedTicks;
                        } else if (reason == AbilityEndReason.DURATION_EXPIRED) {
                            expiredReason[0] = reason;
                            expiredChargeTicks[0] = chargedTicks;
                        }
                        return AbilityUseResult.success(data);
                    })
                    .build();
            AbilityApi.registerAbility(ability);

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            AbilityUseResult started = AbilityRuntime.activate(player, AbilityData.empty(), ability, 0);
            AbilityData manuallyReleasedData = started.data();
            manuallyReleasedData = AbilityRuntime.tick(player, manuallyReleasedData);
            manuallyReleasedData = AbilityRuntime.tick(player, manuallyReleasedData);
            AbilityRuntime.activate(player, manuallyReleasedData, ability, 0);

            helper.assertTrue(
                    toggledReason[0] == AbilityEndReason.PLAYER_TOGGLED && toggledChargeTicks[0] == 2,
                    "chargeRelease shortcuts should report held ticks when the player toggles the ability off"
            );

            AbilityUseResult startedAgain = AbilityRuntime.activate(player, AbilityData.empty(), ability, 0);
            AbilityData expiredData = startedAgain.data();
            for (int tick = 0; tick < 5; tick++) {
                expiredData = AbilityRuntime.tick(player, expiredData);
            }

            helper.assertTrue(
                    expiredReason[0] == AbilityEndReason.DURATION_EXPIRED && expiredChargeTicks[0] == 5,
                    "chargeRelease shortcuts should report a full charge when the duration expires naturally"
            );
        } finally {
            AbilityApi.unregisterAbility(abilityId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void comboActivationsOpenAndConsumeFollowupWindows(GameTestHelper helper) {
        ResourceLocation triggerAbilityId = id("combo_trigger");
        ResourceLocation comboAbilityId = id("combo_followup");
        ResourceLocation chainId = id("combo_chain");

        AbilityApi.unregisterAbility(triggerAbilityId);
        AbilityApi.unregisterAbility(comboAbilityId);
        ComboChainApi.unregisterChain(chainId);

        try {
            AbilityDefinition triggerAbility = AbilityDefinition.builder(triggerAbilityId, AbilityIcon.ofTexture(id("combo_trigger_icon")))
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build();
            AbilityDefinition comboAbility = AbilityDefinition.builder(comboAbilityId, AbilityIcon.ofTexture(id("combo_followup_icon")))
                    .activateRequirement(AbilityRequirements.comboWindowActive(comboAbilityId))
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build();
            AbilityApi.registerAbility(triggerAbility);
            AbilityApi.registerAbility(comboAbility);

            ComboChainApi.registerChain(ComboChainDefinition.builder(chainId, triggerAbilityId, comboAbilityId)
                    .windowTicks(10)
                    .transformTriggeredSlot()
                    .build());

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            AbilityData startingData = AbilityData.empty().withAbilityInSlot(0, triggerAbilityId);

            AbilityUseResult triggerResult = AbilityRuntime.activate(player, startingData, triggerAbility, 0);
            helper.assertTrue(
                    triggerResult.data().comboWindowFor(comboAbilityId) == 10,
                    "Trigger abilities should open the configured combo window"
            );
            helper.assertTrue(
                    triggerResult.data().comboOverrideInSlot(0).filter(comboAbilityId::equals).isPresent(),
                    "Trigger abilities should override the activated slot with the combo follow-up"
            );

            AbilityUseResult comboResult = AbilityRuntime.activate(player, triggerResult.data(), comboAbility, 0);
            helper.assertTrue(comboResult.data().comboWindowFor(comboAbilityId) == 0, "Using the follow-up should consume its combo window");
            helper.assertTrue(comboResult.data().comboOverrideInSlot(0).isEmpty(), "Using the follow-up should clear its temporary slot override");
        } finally {
            ComboChainApi.unregisterChain(chainId);
            AbilityApi.unregisterAbility(triggerAbilityId);
            AbilityApi.unregisterAbility(comboAbilityId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void branchingComboChainsChooseTheMatchingFollowup(GameTestHelper helper) {
        ResourceLocation triggerAbilityId = id("branch_combo_trigger");
        ResourceLocation defaultComboId = id("branch_combo_default");
        ResourceLocation alternateComboId = id("branch_combo_alternate");
        ResourceLocation branchModeId = id("branch_combo_mode");
        ResourceLocation chainId = id("branch_combo_chain");

        AbilityApi.unregisterAbility(triggerAbilityId);
        AbilityApi.unregisterAbility(defaultComboId);
        AbilityApi.unregisterAbility(alternateComboId);
        AbilityApi.unregisterAbility(branchModeId);
        ComboChainApi.unregisterChain(chainId);

        try {
            AbilityDefinition triggerAbility = AbilityDefinition.builder(triggerAbilityId, AbilityIcon.ofTexture(id("branch_combo_trigger_icon")))
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build();
            AbilityDefinition defaultCombo = AbilityDefinition.builder(defaultComboId, AbilityIcon.ofTexture(id("branch_combo_default_icon")))
                    .activateRequirement(AbilityRequirements.comboWindowActive(defaultComboId))
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build();
            AbilityDefinition alternateCombo = AbilityDefinition.builder(alternateComboId, AbilityIcon.ofTexture(id("branch_combo_alternate_icon")))
                    .activateRequirement(AbilityRequirements.comboWindowActive(alternateComboId))
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build();
            AbilityDefinition branchMode = AbilityDefinition.builder(branchModeId, AbilityIcon.ofTexture(id("branch_combo_mode_icon")))
                    .toggleAbility()
                    .durationTicks(80)
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build();
            AbilityApi.registerAbility(triggerAbility);
            AbilityApi.registerAbility(defaultCombo);
            AbilityApi.registerAbility(alternateCombo);
            AbilityApi.registerAbility(branchMode);

            ComboChainApi.registerChain(ComboChainDefinition.builder(chainId, triggerAbilityId, defaultComboId)
                    .windowTicks(10)
                    .transformTriggeredSlot()
                    .branch(alternateComboId, (player, data) -> data.isModeActive(branchModeId))
                    .build());

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            AbilityUseResult defaultResult = AbilityRuntime.activate(
                    player,
                    AbilityData.empty().withAbilityInSlot(0, triggerAbilityId),
                    triggerAbility,
                    0
            );
            helper.assertTrue(defaultResult.data().comboWindowFor(defaultComboId) == 10, "Branchless combo activations should use the default follow-up");
            helper.assertTrue(
                    defaultResult.data().comboOverrideInSlot(0).filter(defaultComboId::equals).isPresent(),
                    "Default combo branches should still override the triggered slot"
            );

            AbilityUseResult branchedResult = AbilityRuntime.activate(
                    player,
                    AbilityData.empty().withAbilityInSlot(0, triggerAbilityId).withMode(branchModeId, true),
                    triggerAbility,
                    0
            );
            helper.assertTrue(branchedResult.data().comboWindowFor(alternateComboId) == 10, "Matching combo branches should open their alternate follow-up window");
            helper.assertTrue(
                    branchedResult.data().comboOverrideInSlot(0).filter(alternateComboId::equals).isPresent(),
                    "Matching combo branches should override the triggered slot with the branch follow-up"
            );
        } finally {
            ComboChainApi.unregisterChain(chainId);
            AbilityApi.unregisterAbility(triggerAbilityId);
            AbilityApi.unregisterAbility(defaultComboId);
            AbilityApi.unregisterAbility(alternateComboId);
            AbilityApi.unregisterAbility(branchModeId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void resourceCostsAreCheckedAndSpentAtomically(GameTestHelper helper) {
        ResourceLocation resourceId = id("resource_cost_meter");
        ResourceLocation abilityId = id("resource_cost_ability");

        AbilityApi.unregisterResource(resourceId);
        AbilityApi.unregisterAbility(abilityId);

        try {
            AbilityApi.registerResource(AbilityResourceDefinition.builder(resourceId)
                    .maxAmount(10)
                    .startingAmount(0)
                    .build());
            AbilityDefinition ability = AbilityDefinition.builder(abilityId, AbilityIcon.ofTexture(id("resource_cost_icon")))
                    .resourceCost(resourceId, 3)
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build();
            AbilityApi.registerAbility(ability);

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            AbilityUseResult failedResult = AbilityRuntime.activate(player, AbilityData.empty().withResourceAmount(resourceId, 2), ability, 0);
            helper.assertTrue(!failedResult.consumed(), "Abilities with first-class resource costs should fail before activating when the resource is missing");

            AbilityUseResult successResult = AbilityRuntime.activate(player, AbilityData.empty().withResourceAmount(resourceId, 5), ability, 0);
            helper.assertTrue(successResult.consumed(), "Abilities with first-class resource costs should activate when enough resource is available");
            helper.assertTrue(successResult.data().resourceAmount(resourceId) == 2, "Successful activations should spend the configured resource amount automatically");
        } finally {
            AbilityApi.unregisterAbility(abilityId);
            AbilityApi.unregisterResource(resourceId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void restoreMissingUndroppableItemsRecreatesOwnedManagedItems(GameTestHelper helper) {
        ResourceLocation grantedItemId = id("restore_undroppable_item");
        GrantedItemApi.unregisterGrantedItem(grantedItemId);

        try {
            GrantedItemApi.registerGrantedItem(GrantedItemDefinition.builder(grantedItemId, Items.BLAZE_ROD)
                    .undroppable()
                    .build());

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            GrantedItemGrantApi.grant(player, grantedItemId);

            helper.assertTrue(
                    !GrantedItemRuntime.restoreMissingUndroppableItems(player),
                    "Restoring while the managed item is still present should not duplicate it"
            );

            int slot = findGrantedItemSlot(player, grantedItemId);
            helper.assertTrue(slot >= 0, "Granted undroppable item should be present before recovery testing");
            player.getInventory().items.set(slot, ItemStack.EMPTY);

            helper.assertTrue(
                    GrantedItemRuntime.restoreMissingUndroppableItems(player),
                    "Missing undroppable managed items should be recreated from grant ownership"
            );
            helper.assertTrue(findGrantedItemSlot(player, grantedItemId) >= 0, "Recovered undroppable item should be returned to the inventory");
        } finally {
            GrantedItemApi.unregisterGrantedItem(grantedItemId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void grantedItemsCanPersistAfterRevokeWhenConfigured(GameTestHelper helper) {
        ResourceLocation grantedItemId = id("keep_when_revoked_item");
        GrantedItemApi.unregisterGrantedItem(grantedItemId);

        try {
            GrantedItemApi.registerGrantedItem(GrantedItemDefinition.builder(grantedItemId, Items.ECHO_SHARD)
                    .keepWhenRevoked()
                    .build());

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            GrantedItemGrantApi.grant(player, grantedItemId);

            int slot = findGrantedItemSlot(player, grantedItemId);
            helper.assertTrue(slot >= 0, "Granted keep-when-revoked items should be inserted into inventory");

            GrantedItemGrantApi.revoke(player, grantedItemId);

            helper.assertTrue(
                    !GrantedItemGrantApi.grantedItems(player).contains(grantedItemId),
                    "Revoking a keep-when-revoked item should still clear ownership state"
            );
            helper.assertTrue(
                    findGrantedItemSlot(player, grantedItemId) >= 0,
                    "keepWhenRevoked items should remain in inventory after ownership is removed"
            );
        } finally {
            GrantedItemApi.unregisterGrantedItem(grantedItemId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void recipePermissionGrantAndRevokeUpdatesAccessState(GameTestHelper helper) {
        ResourceLocation recipeId = ResourceLocation.withDefaultNamespace("golden_apple");
        RecipePermissionApi.unregisterRestrictedRecipe(recipeId);

        try {
            RecipePermissionApi.registerRestrictedRecipe(RestrictedRecipeDefinition.builder(recipeId)
                    .hiddenWhenLocked(false)
                    .build());

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            helper.assertTrue(
                    RecipePermissionApi.accessState(player, recipeId) == RecipePermissionApi.RecipeAccessState.LOCKED,
                    "Restricted recipes should begin locked without a permission source"
            );
            helper.assertTrue(
                    RecipePermissionApi.visibleRestrictedRecipes(player).contains(recipeId),
                    "Visible restricted recipes should appear in the visible restricted set while locked"
            );

            RecipePermissionApi.grant(player, recipeId);
            helper.assertTrue(
                    RecipePermissionApi.accessState(player, recipeId) == RecipePermissionApi.RecipeAccessState.ALLOWED,
                    "Granting recipe permission should move the recipe into the allowed state"
            );
            helper.assertTrue(
                    RecipePermissionApi.permissions(player).contains(recipeId),
                    "Granted recipe permissions should be tracked on the player attachment"
            );

            RecipePermissionApi.revoke(player, recipeId);
            helper.assertTrue(
                    RecipePermissionApi.accessState(player, recipeId) == RecipePermissionApi.RecipeAccessState.LOCKED,
                    "Revoking recipe permission should move the recipe back into the locked state"
            );
            helper.assertTrue(
                    !RecipePermissionApi.permissions(player).contains(recipeId),
                    "Revoking recipe permission should clear the live permission source"
            );
        } finally {
            RecipePermissionApi.unregisterRestrictedRecipe(recipeId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void upgradeConsumeRulesAwardPointsAndUnlockNodes(GameTestHelper helper) {
        ResourceLocation pointTypeId = id("meat_points");
        ResourceLocation counterId = id("meat_eaten");
        ResourceLocation ruleId = id("eat_meat");
        ResourceLocation nodeId = id("meat_mastery");
        ResourceLocation rewardedPassiveId = id("meat_training");
        ResourceLocation rewardedAbilityId = id("meat_burst");

        UpgradeApi.unregisterPointType(pointTypeId);
        UpgradeApi.unregisterConsumeRule(ruleId);
        UpgradeApi.unregisterNode(nodeId);
        PassiveApi.unregisterPassive(rewardedPassiveId);
        AbilityApi.unregisterAbility(rewardedAbilityId);

        try {
            UpgradeApi.registerPointType(UpgradePointType.of(pointTypeId));
            UpgradeApi.registerConsumeRule(UpgradeConsumeRule.builder(ruleId)
                    .item(Items.COOKED_BEEF)
                    .awardPoints(pointTypeId, 1)
                    .incrementCounter(counterId, 1)
                    .build());
            PassiveApi.registerPassive(PassiveDefinition.builder(rewardedPassiveId, AbilityIcon.ofTexture(id("meat_training_icon"))).build());
            AbilityDefinition rewardedAbility = AbilityDefinition.builder(rewardedAbilityId, AbilityIcon.ofTexture(id("meat_burst_icon")))
                    .renderRequirement(AbilityRequirements.hasPassive(rewardedPassiveId))
                    .activateRequirement(AbilityRequirements.hasPassive(rewardedPassiveId))
                    .action((player, data) -> AbilityUseResult.success(data))
                    .build();
            AbilityApi.registerAbility(rewardedAbility);
            UpgradeApi.registerNode(UpgradeNodeDefinition.builder(nodeId)
                    .pointCost(pointTypeId, 1)
                    .rewards(UpgradeRewardBundle.builder()
                            .grantAbility(rewardedAbilityId)
                            .grantPassive(rewardedPassiveId)
                            .build())
                    .build());

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            helper.assertTrue(!AbilityGrantApi.canView(player, rewardedAbility), "Locked progression abilities should stay hidden before their reward node unlocks");
            UpgradeApi.onItemConsumed(player, new ItemStack(Items.COOKED_BEEF));

            helper.assertTrue(UpgradeApi.points(player, pointTypeId) == 1, "Eating meat should award the configured upgrade point");
            helper.assertTrue(UpgradeApi.counter(player, counterId) == 1, "Eating meat should increment the configured upgrade counter");
            helper.assertTrue(UpgradeApi.unlockNode(player, nodeId), "Players should be able to spend earned points on an upgrade node");
            helper.assertTrue(AbilityGrantApi.grantedAbilities(player).contains(rewardedAbilityId), "Unlocked nodes should project their rewards into XLib grants");
            helper.assertTrue(ModAttachments.get(player).grantedPassives().contains(rewardedPassiveId), "Unlocked nodes should also project their passive rewards");
            helper.assertTrue(UpgradeApi.points(player, pointTypeId) == 0, "Unlocking a node should spend its configured point cost");

            AbilityItemHooks.onPlayerTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(
                    AbilityGrantApi.grantedAbilities(player).contains(rewardedAbilityId),
                    "Active progression reward sources should survive normal dynamic-source pruning ticks"
            );
            helper.assertTrue(
                    ModAttachments.get(player).grantedPassives().contains(rewardedPassiveId),
                    "Progression reward passives should survive normal dynamic-source pruning ticks"
            );
            helper.assertTrue(
                    AbilityGrantApi.canView(player, rewardedAbility),
                    "Unlocked progression abilities should become visible once their reward passive is active"
            );
            helper.assertTrue(
                    AbilityGrantApi.canAssign(player, rewardedAbility),
                    "Unlocked progression abilities should stay assignable after the next dynamic-source sync tick"
            );

            helper.assertTrue(UpgradeApi.revokeNode(player, nodeId), "Nodes should be revokable for respec-style flows");
            AbilityItemHooks.onPlayerTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(!AbilityGrantApi.grantedAbilities(player).contains(rewardedAbilityId), "Revoked nodes should remove their granted ability rewards");
            helper.assertTrue(!ModAttachments.get(player).grantedPassives().contains(rewardedPassiveId), "Revoked nodes should remove their passive rewards");
            helper.assertTrue(!AbilityGrantApi.canView(player, rewardedAbility), "Revoked progression abilities should disappear from the ability menu again");
        } finally {
            UpgradeApi.unregisterConsumeRule(ruleId);
            UpgradeApi.unregisterNode(nodeId);
            UpgradeApi.unregisterPointType(pointTypeId);
            PassiveApi.unregisterPassive(rewardedPassiveId);
            AbilityApi.unregisterAbility(rewardedAbilityId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void upgradeKillRulesCanRequireAbilityAttribution(GameTestHelper helper) {
        ResourceLocation pointTypeId = id("vampiric_points");
        ResourceLocation counterId = id("villager_kills");
        ResourceLocation requiredAbilityId = id("vampiric_drain");
        ResourceLocation wrongAbilityId = id("other_ability");
        ResourceLocation ruleId = id("villager_harvest");

        UpgradeApi.unregisterPointType(pointTypeId);
        UpgradeApi.unregisterKillRule(ruleId);

        try {
            UpgradeApi.registerPointType(UpgradePointType.of(pointTypeId));
            UpgradeApi.registerKillRule(UpgradeKillRule.builder(ruleId)
                    .target(EntityType.VILLAGER)
                    .requiredAbility(requiredAbilityId)
                    .awardPoints(pointTypeId, 2)
                    .incrementCounter(counterId, 1)
                    .build());

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            Villager wrongKillTarget = EntityType.VILLAGER.create(helper.getLevel());
            helper.assertTrue(wrongKillTarget != null, "GameTest should be able to create a villager target");
            AbilityCombatTracker.recordAbilityHit(player, wrongKillTarget, wrongAbilityId);
            UpgradeApi.onKill(player, wrongKillTarget);
            helper.assertTrue(UpgradeApi.points(player, pointTypeId) == 0, "Wrong ability attributions should not satisfy kill-based point rules");

            Villager correctKillTarget = EntityType.VILLAGER.create(helper.getLevel());
            helper.assertTrue(correctKillTarget != null, "GameTest should be able to create a villager target");
            AbilityCombatTracker.recordAbilityHit(player, correctKillTarget, requiredAbilityId);
            UpgradeApi.onKill(player, correctKillTarget);
            helper.assertTrue(UpgradeApi.points(player, pointTypeId) == 2, "Matching ability-attributed kills should award upgrade points");
            helper.assertTrue(UpgradeApi.counter(player, counterId) == 1, "Matching ability-attributed kills should increment counters");
        } finally {
            UpgradeApi.unregisterKillRule(ruleId);
            UpgradeApi.unregisterPointType(pointTypeId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void upgradeChoiceNodesProjectIdentitiesAndGateFollowups(GameTestHelper helper) {
        ResourceLocation pointTypeId = id("specialization_points");
        ResourceLocation choiceGroupId = id("origin_choice");
        ResourceLocation originNodeId = id("sun_origin");
        ResourceLocation alternateNodeId = id("moon_origin");
        ResourceLocation masteryNodeId = id("sun_mastery");
        ResourceLocation identityId = id("identity/sunlineage");

        UpgradeApi.unregisterPointType(pointTypeId);
        UpgradeApi.unregisterNode(originNodeId);
        UpgradeApi.unregisterNode(alternateNodeId);
        UpgradeApi.unregisterNode(masteryNodeId);
        IdentityApi.unregisterIdentity(identityId);

        try {
            UpgradeApi.registerPointType(UpgradePointType.of(pointTypeId));
            IdentityApi.registerIdentity(IdentityDefinition.builder(identityId).build());
            UpgradeApi.registerNode(UpgradeNodeDefinition.builder(originNodeId)
                    .choiceGroup(choiceGroupId)
                    .pointCost(pointTypeId, 1)
                    .rewards(UpgradeRewardBundle.builder()
                            .grantIdentity(identityId)
                            .build())
                    .build());
            UpgradeApi.registerNode(UpgradeNodeDefinition.builder(alternateNodeId)
                    .choiceGroup(choiceGroupId)
                    .pointCost(pointTypeId, 1)
                    .build());
            UpgradeApi.registerNode(UpgradeNodeDefinition.builder(masteryNodeId)
                    .requiredNode(originNodeId)
                    .pointCost(pointTypeId, 1)
                    .requirement(UpgradeRequirements.identityActive(identityId))
                    .build());

            ServerPlayer player = GameTestPlayerFactory.create(helper);
            UpgradeApi.addPoints(player, pointTypeId, 2);

            helper.assertTrue(UpgradeApi.unlockNode(player, originNodeId), "Players should be able to commit to the first choice node");
            helper.assertTrue(IdentityApi.hasIdentity(player, identityId), "Choice nodes should project configured identity rewards");
            helper.assertTrue(
                    UpgradeApi.firstUnlockFailure(player, UpgradeApi.get(player), UpgradeApi.findNode(masteryNodeId).orElseThrow()).isEmpty(),
                    "Projected identities should satisfy later progression requirements"
            );
            helper.assertTrue(UpgradeApi.unlockNode(player, masteryNodeId), "Identity-gated follow-up nodes should unlock after their origin node");
            helper.assertTrue(
                    UpgradeApi.firstStructuralUnlockFailure(UpgradeApi.get(player), UpgradeApi.findNode(alternateNodeId).orElseThrow()).isPresent(),
                    "Alternative choice nodes should remain locked after the branch is committed"
            );
        } finally {
            UpgradeApi.unregisterNode(originNodeId);
            UpgradeApi.unregisterNode(alternateNodeId);
            UpgradeApi.unregisterNode(masteryNodeId);
            UpgradeApi.unregisterPointType(pointTypeId);
            IdentityApi.unregisterIdentity(identityId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void supportPackagesGrantBundlesToLinkedAllies(GameTestHelper helper) {
        ResourceLocation supportPackageId = id("support/guardian");
        ResourceLocation relationshipId = id("relationship/ally");
        ResourceLocation bundleId = id("bundle/support");
        ResourceLocation passiveId = id("support/passive");

        SupportPackageApi.unregisterSupportPackage(supportPackageId);
        GrantBundleApi.unregisterBundle(bundleId);
        PassiveApi.unregisterPassive(passiveId);

        try {
            PassiveApi.registerPassive(PassiveDefinition.builder(passiveId, AbilityIcon.ofTexture(id("support/passive_icon"))).build());
            GrantBundleApi.registerBundle(GrantBundleDefinition.builder(bundleId)
                    .grantPassive(passiveId)
                    .build());
            SupportPackageApi.registerSupportPackage(SupportPackageDefinition.builder(supportPackageId)
                    .grantBundle(bundleId)
                    .relationship(relationshipId)
                    .build());

            ServerPlayer supporter = GameTestPlayerFactory.create(helper);
            ServerPlayer ally = GameTestPlayerFactory.create(helper);

            helper.assertTrue(
                    !SupportPackageApi.apply(supporter, ally, supportPackageId),
                    "Support packages should reject unrelated targets when a relationship is required"
            );

            EntityRelationshipApi.setOwner(ally, relationshipId, supporter);
            helper.assertTrue(
                    SupportPackageApi.apply(supporter, ally, supportPackageId),
                    "Support packages should apply once the target is linked through the required relationship"
            );
            helper.assertTrue(
                    ModAttachments.get(ally).grantedPassives().contains(passiveId),
                    "Applied support packages should project their bundle passives onto the ally"
            );

            SupportPackageApi.revoke(supporter, ally, supportPackageId);
            helper.assertTrue(
                    !ModAttachments.get(ally).grantedPassives().contains(passiveId),
                    "Revoked support packages should remove their projected bundle passives"
            );
        } finally {
            SupportPackageApi.unregisterSupportPackage(supportPackageId);
            GrantBundleApi.unregisterBundle(bundleId);
            PassiveApi.unregisterPassive(passiveId);
        }

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void controlledEntitiesTrackOwnersAndCommands(GameTestHelper helper) {
        ResourceLocation relationshipId = id("relationship/summon");
        ResourceLocation commandId = id("command/follow");

        ServerPlayer controller = GameTestPlayerFactory.create(helper);
        Villager summoned = EntityType.VILLAGER.create(helper.getLevel());
        helper.assertTrue(summoned != null, "GameTest should be able to create a controlled villager");
        summoned.moveTo(controller.getX() + 1.0D, controller.getY(), controller.getZ(), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(summoned);

        ControlledEntityApi.bind(summoned, relationshipId, controller);
        ControlledEntityApi.setCommand(summoned, commandId);

        helper.assertTrue(
                ControlledEntityApi.isControlledBy(summoned, controller, relationshipId),
                "Bound controlled entities should report their controller through the relationship layer"
        );
        helper.assertTrue(
                ControlledEntityApi.controllerId(summoned, relationshipId).filter(controller.getUUID()::equals).isPresent(),
                "Controlled entities should persist their controller UUID"
        );
        helper.assertTrue(
                ControlledEntityApi.currentCommand(summoned).filter(commandId::equals).isPresent(),
                "Controlled entities should persist their active command id"
        );
        helper.assertTrue(
                ControlledEntityApi.controlledEntities(helper.getLevel(), controller, relationshipId).stream()
                        .anyMatch(entity -> entity.getUUID().equals(summoned.getUUID())),
                "Controllers should be able to resolve their currently bound living entities"
        );

        ControlledEntityApi.release(summoned, relationshipId);
        ControlledEntityApi.clearCommand(summoned);
        helper.assertTrue(
                ControlledEntityApi.controllerId(summoned, relationshipId).isEmpty(),
                "Released controlled entities should clear their controller relationship"
        );
        helper.assertTrue(
                ControlledEntityApi.currentCommand(summoned).isEmpty(),
                "Released controlled entities should allow their command state to be cleared"
        );

        helper.succeed();
    }

    private static int findGrantedItemSlot(ServerPlayer player, ResourceLocation grantedItemId) {
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack stack = player.getInventory().items.get(slot);
            if (GrantedItemRuntime.grantedItemId(stack).filter(grantedItemId::equals).isPresent()) {
                return slot;
            }
        }
        return -1;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(XLib.MODID, "gametest/" + path);
    }

    private static void executeAsPlayerCommand(ServerPlayer player, String command) {
        player.getServer().getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                command
        );
    }

    private static final class TestRecipeMenu extends AbstractContainerMenu {
        private final TestResultContainer resultContainer;

        private TestRecipeMenu(RecipeHolder<?> recipe, ItemStack result) {
            super(null, 0);
            this.resultContainer = new TestResultContainer(recipe, result);
            this.addSlot(new Slot(this.resultContainer, 0, 0, 0));
        }

        @Override
        public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(net.minecraft.world.entity.player.Player player) {
            return true;
        }
    }

    private static final class TestExternalStorageMenu extends AbstractContainerMenu {
        private TestExternalStorageMenu(SimpleContainer container) {
            super(null, 0);
            this.addSlot(new Slot(container, 0, 0, 0));
        }

        @Override
        public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(net.minecraft.world.entity.player.Player player) {
            return true;
        }
    }

    private static final class TestResultContainer extends SimpleContainer implements RecipeCraftingHolder {
        private RecipeHolder<?> recipeUsed;

        private TestResultContainer(RecipeHolder<?> recipe, ItemStack result) {
            super(1);
            this.recipeUsed = recipe;
            this.setItem(0, result);
        }

        @Override
        public void setRecipeUsed(RecipeHolder<?> recipe) {
            this.recipeUsed = recipe;
        }

        @Override
        public RecipeHolder<?> getRecipeUsed() {
            return this.recipeUsed;
        }
    }
}

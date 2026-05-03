package com.whatxe.xlib.ability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.whatxe.xlib.XLib;
import com.whatxe.xlib.api.event.XLibRecipePermissionEvent;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

public final class RecipePermissionApi {
    public static final ResourceLocation COMMAND_SOURCE =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "command_recipe_permission");
    private static final Gson RESTRICTION_GSON = new GsonBuilder().create();
    private static final String RESTRICTION_DIRECTORY = XLib.MODID + "/restricted_recipes";
    private static final String ADVANCEMENT_SOURCE_PATH_PREFIX = "recipe_advancement/";

    private static final Map<ResourceLocation, RestrictedRecipeDefinition> CODE_RESTRICTED_RECIPES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, RestrictedRecipeRule> CODE_RESTRICTED_RULES = new LinkedHashMap<>();
    private static volatile LoadedRestrictionState datapackRestrictions = LoadedRestrictionState.empty();
    private static volatile ResolvedRestrictionCache resolvedRestrictionCache = ResolvedRestrictionCache.empty();
    private static volatile boolean restrictionCacheDirty = true;

    private RecipePermissionApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new RestrictedRecipeReloadListener());
    }

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(RecipePermissionApi::syncServerState);
    }

    public static void registerRestrictedRecipe(ResourceLocation recipeId) {
        registerRestrictedRecipe(RestrictedRecipeDefinition.builder(recipeId).build());
    }

    public static void registerRestrictedRecipe(RestrictedRecipeDefinition definition) {
        CODE_RESTRICTED_RECIPES.put(definition.recipeId(), definition);
        markRestrictionCacheDirty();
    }

    public static void registerRestrictedRule(RestrictedRecipeRule rule) {
        CODE_RESTRICTED_RULES.put(rule.id(), rule);
        markRestrictionCacheDirty();
    }

    public static boolean unregisterRestrictedRecipe(ResourceLocation recipeId) {
        boolean removed = CODE_RESTRICTED_RECIPES.remove(recipeId) != null;
        if (removed) {
            markRestrictionCacheDirty();
        }
        return removed;
    }

    public static boolean unregisterRestrictedRule(ResourceLocation ruleId) {
        boolean removed = CODE_RESTRICTED_RULES.remove(ruleId) != null;
        if (removed) {
            markRestrictionCacheDirty();
        }
        return removed;
    }

    public static void clearRestrictedRecipes() {
        CODE_RESTRICTED_RECIPES.clear();
        markRestrictionCacheDirty();
    }

    public static void clearRestrictedRules() {
        CODE_RESTRICTED_RULES.clear();
        markRestrictionCacheDirty();
    }

    public static Set<ResourceLocation> restrictedRecipes() {
        return restrictedRecipes(null);
    }

    public static Set<ResourceLocation> restrictedRecipes(@Nullable Player player) {
        return resolvedRestrictionCache(recipeContext(player)).recipeIds();
    }

    public static boolean isRestricted(ResourceLocation recipeId) {
        return findRestrictedRecipe(recipeId).isPresent();
    }

    public static Collection<RestrictedRecipeDefinition> allRestrictedRecipes() {
        return List.copyOf(resolvedRestrictionCache(null).definitions().values());
    }

    public static Collection<RestrictedRecipeDefinition> restrictedRecipesInFamily(ResourceLocation familyId) {
        ResourceLocation resolvedFamilyId = java.util.Objects.requireNonNull(familyId, "familyId");
        return filterRestrictedDefinitions(definition -> definition.familyId().filter(resolvedFamilyId::equals).isPresent());
    }

    public static Collection<RestrictedRecipeDefinition> restrictedRecipesInGroup(ResourceLocation groupId) {
        ResourceLocation resolvedGroupId = java.util.Objects.requireNonNull(groupId, "groupId");
        return filterRestrictedDefinitions(definition -> definition.groupId().filter(resolvedGroupId::equals).isPresent());
    }

    public static Collection<RestrictedRecipeDefinition> restrictedRecipesOnPage(ResourceLocation pageId) {
        ResourceLocation resolvedPageId = java.util.Objects.requireNonNull(pageId, "pageId");
        return filterRestrictedDefinitions(definition -> definition.pageId().filter(resolvedPageId::equals).isPresent());
    }

    public static Collection<RestrictedRecipeDefinition> restrictedRecipesWithTag(ResourceLocation tagId) {
        ResourceLocation resolvedTagId = java.util.Objects.requireNonNull(tagId, "tagId");
        return filterRestrictedDefinitions(definition -> definition.hasTag(resolvedTagId));
    }

    public static Optional<RestrictedRecipeDefinition> findRestrictedRecipe(ResourceLocation recipeId) {
        return findRestrictedRecipe(recipeContext(null), recipeId);
    }

    public static Optional<RestrictedRecipeDefinition> findRestrictedRecipe(Player player, ResourceLocation recipeId) {
        return findRestrictedRecipe(recipeContext(player), recipeId);
    }

    public static boolean hasPermission(Player player, ResourceLocation recipeId) {
        return ModAttachments.get(player).hasRecipePermission(recipeId);
    }

    public static boolean canCraft(Player player, ResourceLocation recipeId) {
        Optional<RestrictedRecipeDefinition> definition = findRestrictedRecipe(player, recipeId);
        return definition.isEmpty() || definition.get().exempted() || hasPermission(player, recipeId);
    }

    public static boolean canCraft(AbilityData data, ResourceLocation recipeId) {
        Optional<RestrictedRecipeDefinition> definition = findRestrictedRecipe(recipeId);
        return definition.isEmpty() || definition.get().exempted() || data.hasRecipePermission(recipeId);
    }

    public static RecipeAccessState accessState(Player player, ResourceLocation recipeId) {
        Optional<RestrictedRecipeDefinition> definition = findRestrictedRecipe(player, recipeId);
        if (definition.isEmpty() || definition.get().exempted()) {
            return RecipeAccessState.UNRESTRICTED;
        }
        return hasPermission(player, recipeId) ? RecipeAccessState.ALLOWED : RecipeAccessState.LOCKED;
    }

    public static boolean shouldHideLockedRecipe(Player player, ResourceLocation recipeId) {
        return accessState(player, recipeId) == RecipeAccessState.LOCKED
                && findRestrictedRecipe(player, recipeId).map(RestrictedRecipeDefinition::hiddenWhenLocked).orElse(true);
    }

    public static boolean shouldHideLockedRecipe(Player player, RecipeHolder<?> recipe) {
        return shouldHideLockedRecipe(player, recipe.id());
    }

    public static Set<ResourceLocation> lockedRecipes(Player player) {
        AbilityData data = ModAttachments.get(player);
        LinkedHashSet<ResourceLocation> lockedRecipes = new LinkedHashSet<>();
        for (ResourceLocation recipeId : restrictedRecipes(player)) {
            if (!canCraft(data, recipeId)) {
                lockedRecipes.add(recipeId);
            }
        }
        return Set.copyOf(lockedRecipes);
    }

    public static Set<ResourceLocation> visibleRestrictedRecipes(Player player) {
        LinkedHashSet<ResourceLocation> visibleRecipes = new LinkedHashSet<>();
        for (ResourceLocation recipeId : restrictedRecipes(player)) {
            if (!shouldHideLockedRecipe(player, recipeId)) {
                visibleRecipes.add(recipeId);
            }
        }
        return Set.copyOf(visibleRecipes);
    }

    public static Set<ResourceLocation> permissions(Player player) {
        return Set.copyOf(ModAttachments.get(player).recipePermissions());
    }

    public static Set<ResourceLocation> recipesInCategory(ResourceLocation categoryId) {
        return resolvedRestrictionCache(null).recipesInCategory(categoryId);
    }

    public static Set<ResourceLocation> recipesInNamespace(String namespace) {
        return resolvedRestrictionCache(null).recipesInNamespace(namespace);
    }

    public static Set<ResourceLocation> recipesForOutput(ResourceLocation outputItemId) {
        return resolvedRestrictionCache(null).recipesForOutput(outputItemId);
    }

    public static Set<ResourceLocation> recipesForOutputTag(ResourceLocation outputItemTagId) {
        return resolvedRestrictionCache(null).recipesForOutputTag(outputItemTagId);
    }

    public static Set<ResourceLocation> unlockSources(ResourceLocation recipeId) {
        return findRestrictedRecipe(recipeId)
                .map(RestrictedRecipeDefinition::unlockSources)
                .orElse(Set.of());
    }

    public static Set<ResourceLocation> unlockSources(Player player, ResourceLocation recipeId) {
        return findRestrictedRecipe(player, recipeId)
                .map(RestrictedRecipeDefinition::unlockSources)
                .orElse(Set.of());
    }

    public static Optional<Component> unlockHint(ResourceLocation recipeId) {
        return findRestrictedRecipe(recipeId).map(RestrictedRecipeDefinition::unlockHint);
    }

    public static Optional<Component> unlockHint(Player player, ResourceLocation recipeId) {
        return findRestrictedRecipe(player, recipeId).map(RestrictedRecipeDefinition::unlockHint);
    }

    public static Set<ResourceLocation> permissionSources(Player player, ResourceLocation recipeId) {
        return Set.copyOf(ModAttachments.get(player).recipePermissionSourcesFor(recipeId));
    }

    public static void grant(Player player, ResourceLocation recipeId) {
        grant(player, recipeId, COMMAND_SOURCE);
    }

    public static void grant(Player player, ResourceLocation recipeId, ResourceLocation sourceId) {
        ensureRestrictedRecipe(recipeId);
        update(player, ModAttachments.get(player).withRecipePermissionSource(recipeId, sourceId, true));
    }

    public static void grant(Player player, Collection<ResourceLocation> recipeIds, ResourceLocation sourceId) {
        AbilityData updatedData = ModAttachments.get(player);
        for (ResourceLocation recipeId : new LinkedHashSet<>(recipeIds)) {
            ensureRestrictedRecipe(recipeId);
            updatedData = updatedData.withRecipePermissionSource(recipeId, sourceId, true);
        }
        update(player, updatedData);
    }

    public static void revoke(Player player, ResourceLocation recipeId) {
        revoke(player, recipeId, COMMAND_SOURCE);
    }

    public static void revoke(Player player, ResourceLocation recipeId, ResourceLocation sourceId) {
        update(player, ModAttachments.get(player).withRecipePermissionSource(recipeId, sourceId, false));
    }

    public static void clearPermissions(Player player) {
        update(player, ModAttachments.get(player).clearRecipePermissionSources());
    }

    public static void syncSourcePermissions(Player player, ResourceLocation sourceId, Collection<ResourceLocation> recipeIds) {
        AbilityData currentData = ModAttachments.get(player).withManagedGrantSource(sourceId, true);
        AbilityData updatedData = syncSourcePermissions(player instanceof ServerPlayer server ? server : null, currentData, sourceId, recipeIds);
        update(player, updatedData);
    }

    public static void syncServerState(ServerPlayer player) {
        AbilityData previousData = ModAttachments.get(player);
        AbilityData updatedData = syncAutomaticPermissions(player, previousData);
        applyData(player, previousData, updatedData);
        syncRecipeBook(player, updatedData);
        enforceOpenCraftingResult(player, updatedData);
    }

    public static void syncOnlinePlayers() {
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            return;
        }
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            installCraftingGuards(player);
            syncServerState(player);
        }
    }

    public static void installCraftingGuards(ServerPlayer player) {
        guardCraftingMenu(player, player.inventoryMenu);
        guardCraftingMenu(player, player.containerMenu);
    }

    public static void guardCraftingMenu(ServerPlayer player, @Nullable AbstractContainerMenu menu) {
        if (menu == null || menu.slots.size() < 2 || menu.getSlot(0) instanceof RestrictedResultSlot) {
            return;
        }
        if (!(menu instanceof CraftingMenu) && !(menu instanceof InventoryMenu)) {
            return;
        }

        Slot resultSlot = menu.getSlot(0);
        if (!(resultSlot.container instanceof RecipeCraftingHolder)) {
            return;
        }
        if (!(menu.getSlot(1).container instanceof CraftingContainer craftSlots)) {
            return;
        }

        RestrictedResultSlot guardedSlot = new RestrictedResultSlot(
                player,
                craftSlots,
                resultSlot.container,
                resultSlot.getContainerSlot(),
                resultSlot.x,
                resultSlot.y
        );
        guardedSlot.index = resultSlot.index;
        menu.slots.set(0, guardedSlot);
    }

    public static void enforceOpenCraftingResult(ServerPlayer player, AbilityData data) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || menu.slots.isEmpty()) {
            return;
        }

        Slot resultSlot = menu.getSlot(0);
        if (!(resultSlot.container instanceof RecipeCraftingHolder recipeHolder)) {
            return;
        }

        RecipeHolder<?> recipe = recipeHolder.getRecipeUsed();
        if (recipe == null || canCraft(player, recipe.id())) {
            return;
        }

        recipeHolder.setRecipeUsed(null);
        resultSlot.container.setItem(resultSlot.getContainerSlot(), ItemStack.EMPTY);
        menu.setRemoteSlot(0, ItemStack.EMPTY);
        player.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, ItemStack.EMPTY));
    }

    static AbilityData syncSourcePermissions(
            @Nullable ServerPlayer player,
            AbilityData currentData,
            ResourceLocation sourceId,
            Collection<ResourceLocation> recipeIds
    ) {
        AbilityData updatedData = currentData.withManagedGrantSource(sourceId, true);
        Set<ResourceLocation> desiredRecipes = new LinkedHashSet<>(recipeIds);
        for (ResourceLocation recipeId : desiredRecipes) {
            ensureRestrictedRecipe(recipeId);
        }
        for (ResourceLocation recipeId : Set.copyOf(updatedData.recipePermissions())) {
            if (updatedData.recipePermissionSourcesFor(recipeId).contains(sourceId) && !desiredRecipes.contains(recipeId)) {
                updatedData = updatedData.withRecipePermissionSource(recipeId, sourceId, false);
            }
        }
        for (ResourceLocation recipeId : desiredRecipes) {
            updatedData = updatedData.withRecipePermissionSource(recipeId, sourceId, true);
        }
        return updatedData;
    }

    private static void ensureRestrictedRecipe(ResourceLocation recipeId) {
        if (!isRestricted(recipeId)) {
            registerRestrictedRecipe(recipeId);
        }
    }

    static AbilityData revokeSourcePermissions(
            @Nullable ServerPlayer player,
            AbilityData currentData,
            ResourceLocation sourceId
    ) {
        AbilityData updatedData = currentData;
        for (ResourceLocation recipeId : Set.copyOf(updatedData.recipePermissions())) {
            if (updatedData.recipePermissionSourcesFor(recipeId).contains(sourceId)) {
                updatedData = updatedData.withRecipePermissionSource(recipeId, sourceId, false);
            }
        }
        return updatedData;
    }

    private static void syncRecipeBook(ServerPlayer player, AbilityData data) {
        List<RecipeHolder<?>> toAdd = new ArrayList<>();
        List<RecipeHolder<?>> toRemove = new ArrayList<>();
        for (ResourceLocation recipeId : restrictedRecipes(player)) {
            RecipeHolder<?> recipe = player.server.getRecipeManager().byKey(recipeId).orElse(null);
            if (recipe == null) {
                continue;
            }

            boolean allowed = data.hasRecipePermission(recipeId);
            boolean known = player.getRecipeBook().contains(recipeId);
            if (allowed && !known) {
                toAdd.add(recipe);
            } else if (!allowed && known) {
                toRemove.add(recipe);
            }
        }

        if (!toAdd.isEmpty()) {
            player.awardRecipes(toAdd);
        }
        if (!toRemove.isEmpty()) {
            player.resetRecipes(toRemove);
        }
    }

    private static void update(Player player, AbilityData updatedData) {
        if (player instanceof ServerPlayer serverPlayer) {
            AbilityData previousData = ModAttachments.get(serverPlayer);
            AbilityData syncedData = syncAutomaticPermissions(serverPlayer, updatedData);
            applyData(serverPlayer, previousData, syncedData);
            installCraftingGuards(serverPlayer);
            syncRecipeBook(serverPlayer, syncedData);
            enforceOpenCraftingResult(serverPlayer, syncedData);
            return;
        }
        if (!updatedData.equals(ModAttachments.get(player))) {
            ModAttachments.set(player, updatedData);
        }
    }

    private static void setDatapackRestrictions(
            Map<ResourceLocation, RestrictedRecipeDefinition> restrictedRecipes,
            Map<ResourceLocation, RestrictedRecipeRule> restrictedRules
    ) {
        datapackRestrictions = new LoadedRestrictionState(Map.copyOf(restrictedRecipes), Map.copyOf(restrictedRules));
        markRestrictionCacheDirty();
    }

    static void setDatapackRestrictionsForTesting(
            Map<ResourceLocation, RestrictedRecipeDefinition> restrictedRecipes,
            Map<ResourceLocation, RestrictedRecipeRule> restrictedRules
    ) {
        setDatapackRestrictions(restrictedRecipes, restrictedRules);
    }

    private static void readRestrictionElement(
            ResourceLocation fileId,
            JsonElement element,
            Map<ResourceLocation, RestrictedRecipeDefinition> exactOutput,
            Map<ResourceLocation, RestrictedRecipeRule> ruleOutput
    ) {
        if (element.isJsonArray()) {
            for (JsonElement recipeElement : element.getAsJsonArray()) {
                ResourceLocation recipeId = ResourceLocation.parse(recipeElement.getAsString());
                exactOutput.put(recipeId, RestrictedRecipeDefinition.builder(recipeId).build());
            }
            return;
        }

        if (element instanceof JsonPrimitive primitive && primitive.isString()) {
            ResourceLocation recipeId = ResourceLocation.parse(primitive.getAsString());
            exactOutput.put(recipeId, RestrictedRecipeDefinition.builder(recipeId).build());
            return;
        }

        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("Expected an array or object");
        }

        JsonObject object = element.getAsJsonObject();
        List<ResourceLocation> recipeIds = new ArrayList<>();
        if (object.has("recipe")) {
            recipeIds.add(ResourceLocation.parse(GsonHelper.getAsString(object, "recipe")));
        }
        if (object.has("recipes")) {
            for (JsonElement recipeElement : GsonHelper.getAsJsonArray(object, "recipes")) {
                recipeIds.add(ResourceLocation.parse(recipeElement.getAsString()));
            }
        }

        List<ResourceLocation> recipeTags = readLocations(object, "recipe_tag", "recipe_tags");
        List<String> recipeNamespaces = readStrings(object, "recipe_namespace", "recipe_namespaces");
        List<ResourceLocation> categories = readLocations(object, "category", "categories");
        List<ResourceLocation> outputs = readLocations(object, "output", "outputs");
        List<ResourceLocation> outputItemTags = readLocations(object, "output_item_tag", "output_item_tags");
        int priority = GsonHelper.getAsInt(object, "priority", 0);
        RecipeRuleMode mode = RecipeRuleMode.RESTRICT;
        if (object.has("mode")) {
            String modeStr = GsonHelper.getAsString(object, "mode").toUpperCase(java.util.Locale.ROOT);
            try {
                mode = RecipeRuleMode.valueOf(modeStr);
            } catch (IllegalArgumentException ignored) {
                throw new IllegalArgumentException("Unknown mode '" + modeStr + "'; valid values: restrict, exempt");
            }
        }
        boolean matchAll = GsonHelper.getAsBoolean(object, "match_all", false)
                || GsonHelper.getAsBoolean(object, "all_recipes", false);
        List<ResourceLocation> unlockSources = readLocations(object, "unlock_source", "unlock_sources");
        List<ResourceLocation> unlockAdvancements = readLocations(object, "unlock_advancement", "unlock_advancements");
        CompoundTag outputTag = object.has("output_nbt")
                ? parseOutputTag(GsonHelper.getAsString(object, "output_nbt"))
                : null;
        Component unlockHint = object.has("unlock_hint") ? parseUnlockHint(object.get("unlock_hint")) : null;
        boolean hiddenWhenLocked = !object.has("hidden_when_locked")
                || GsonHelper.getAsBoolean(object, "hidden_when_locked");

        for (ResourceLocation recipeId : recipeIds) {
            RestrictedRecipeDefinition.Builder builder = RestrictedRecipeDefinition.builder(recipeId)
                    .recipeTags(recipeTags)
                    .recipeNamespaces(recipeNamespaces)
                    .categories(categories)
                    .outputs(outputs)
                    .outputItemTags(outputItemTags)
                    .unlockSources(unlockSources)
                    .unlockAdvancements(unlockAdvancements)
                    .hiddenWhenLocked(hiddenWhenLocked);
            if (outputTag != null) {
                builder.outputTag(outputTag);
            }
            if (unlockHint != null) {
                builder.unlockHint(unlockHint);
            }
            exactOutput.put(recipeId, builder.build());
        }

        if (recipeIds.isEmpty()) {
            RestrictedRecipeRule.Builder builder = RestrictedRecipeRule.builder(fileId)
                    .priority(priority)
                    .mode(mode)
                    .matchAll(matchAll)
                    .recipeTags(recipeTags)
                    .recipeNamespaces(recipeNamespaces)
                    .categories(categories)
                    .outputs(outputs)
                    .outputItemTags(outputItemTags)
                    .unlockSources(unlockSources)
                    .unlockAdvancements(unlockAdvancements)
                    .hiddenWhenLocked(hiddenWhenLocked);
            if (outputTag != null) {
                builder.outputTag(outputTag);
            }
            if (unlockHint != null) {
                builder.unlockHint(unlockHint);
            }
            RestrictedRecipeRule rule = builder.build();
            if (!rule.hasSelectors()) {
                throw new IllegalArgumentException("Expected 'recipe', 'recipes', or rule selectors like 'match_all', 'recipe_tags', 'recipe_namespaces', 'categories', 'outputs', or 'output_item_tags' in " + fileId);
            }
            ruleOutput.put(rule.id(), rule);
        }
    }

    private static List<String> readStrings(JsonObject object, String singleKey, String pluralKey) {
        List<String> values = new ArrayList<>();
        if (object.has(singleKey)) {
            values.add(GsonHelper.getAsString(object, singleKey));
        }
        if (object.has(pluralKey)) {
            for (JsonElement element : GsonHelper.getAsJsonArray(object, pluralKey)) {
                values.add(element.getAsString());
            }
        }
        return List.copyOf(values);
    }

    private static List<ResourceLocation> readLocations(JsonObject object, String singleKey, String pluralKey) {
        List<ResourceLocation> values = new ArrayList<>();
        if (object.has(singleKey)) {
            values.add(ResourceLocation.parse(GsonHelper.getAsString(object, singleKey)));
        }
        if (object.has(pluralKey)) {
            for (JsonElement element : GsonHelper.getAsJsonArray(object, pluralKey)) {
                values.add(ResourceLocation.parse(element.getAsString()));
            }
        }
        return List.copyOf(values);
    }

    public enum RecipeAccessState {
        UNRESTRICTED,
        ALLOWED,
        LOCKED
    }

    static ResourceLocation categoryId(CraftingBookCategory category) {
        return ResourceLocation.withDefaultNamespace(category.name().toLowerCase());
    }

    private static Optional<RestrictedRecipeDefinition> findRestrictedRecipe(
            @Nullable RecipeContext context,
            ResourceLocation recipeId
    ) {
        return Optional.ofNullable(resolvedRestrictionCache(context).definitions().get(recipeId));
    }

    private static Optional<RestrictedRecipeDefinition> findRestrictedRecipe(
            RecipeContext context,
            RecipeHolder<?> recipe
    ) {
        return Optional.ofNullable(resolvedRestrictionCache(context).definitions().get(recipe.id()));
    }

    private static LinkedHashMap<ResourceLocation, RestrictedRecipeDefinition> mergedExactDefinitions() {
        LinkedHashMap<ResourceLocation, RestrictedRecipeDefinition> definitions = new LinkedHashMap<>(CODE_RESTRICTED_RECIPES);
        definitions.putAll(datapackRestrictions.exactRecipes());
        return definitions;
    }

    private static Collection<RestrictedRecipeDefinition> filterRestrictedDefinitions(
            Predicate<RestrictedRecipeDefinition> predicate
    ) {
        return resolvedRestrictionCache(null).definitions().values().stream().filter(predicate).toList();
    }

    private static List<RestrictedRecipeRule> mergedRules() {
        List<RestrictedRecipeRule> rules = new ArrayList<>(CODE_RESTRICTED_RULES.values());
        rules.addAll(datapackRestrictions.rules().values());
        return List.copyOf(rules);
    }

    private static void markRestrictionCacheDirty() {
        restrictionCacheDirty = true;
    }

    private static ResolvedRestrictionCache resolvedRestrictionCache(@Nullable RecipeContext context) {
        RecipeContext resolvedContext = context != null ? context : recipeContext(null);
        ResolvedRestrictionCache currentCache = resolvedRestrictionCache;
        if (!restrictionCacheDirty && currentCache.matches(resolvedContext)) {
            return currentCache;
        }

        ResolvedRestrictionCache rebuiltCache = buildResolvedRestrictionCache(resolvedContext);
        resolvedRestrictionCache = rebuiltCache;
        restrictionCacheDirty = false;
        return rebuiltCache;
    }

    private static ResolvedRestrictionCache buildResolvedRestrictionCache(@Nullable RecipeContext context) {
        LinkedHashMap<ResourceLocation, RestrictedRecipeDefinition> definitions = mergedExactDefinitions();
        if (context != null) {
            for (RecipeHolder<?> recipe : context.recipeManager().getRecipes()) {
                if (definitions.containsKey(recipe.id())) {
                    continue;
                }
                resolveRuleDefinition(context, recipe).ifPresent(definition -> definitions.put(recipe.id(), definition));
            }
        }
        return ResolvedRestrictionCache.create(context, definitions);
    }

    private static Optional<RestrictedRecipeDefinition> resolveRuleDefinition(RecipeContext context, RecipeHolder<?> recipe) {
        return resolveRuleDefinition(recipe, context.registries(), mergedRules());
    }

    static Optional<RestrictedRecipeDefinition> resolveRuleDefinition(
            RecipeHolder<?> recipe,
            RegistryAccess registries,
            Collection<RestrictedRecipeRule> rules
    ) {
        Optional<RestrictedRecipeRule> matchedRule = selectBestMatchingRule(
                rules.stream().filter(rule -> rule.matches(recipe, registries)).toList()
        );
        if (matchedRule.isEmpty()) {
            return Optional.empty();
        }
        RestrictedRecipeRule winner = matchedRule.get();
        // An EXEMPT rule shields the recipe from restriction: treat as unrestricted.
        if (winner.mode() == RecipeRuleMode.EXEMPT) {
            return Optional.empty();
        }
        return Optional.of(winner.toDefinition(recipe, registries));
    }

    static Optional<RestrictedRecipeRule> selectBestMatchingRule(Collection<RestrictedRecipeRule> matchedRules) {
        RestrictedRecipeRule winner = null;
        int matchedPriority = Integer.MIN_VALUE;
        for (RestrictedRecipeRule rule : matchedRules) {
            if (winner == null || rule.priority() >= matchedPriority) {
                winner = rule;
                matchedPriority = rule.priority();
            }
        }
        return Optional.ofNullable(winner);
    }

    private static @Nullable RecipeContext recipeContext(@Nullable Player player) {
        if (player != null) {
            return new RecipeContext(player.level().getRecipeManager(), player.level().registryAccess());
        }
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            return null;
        }
        return new RecipeContext(
                ServerLifecycleHooks.getCurrentServer().getRecipeManager(),
                ServerLifecycleHooks.getCurrentServer().registryAccess()
        );
    }

    private static void applyData(Player player, AbilityData previousData, AbilityData updatedData) {
        if (!updatedData.equals(previousData)) {
            ModAttachments.set(player, updatedData);
            if (player instanceof ServerPlayer serverPlayer) {
                postPermissionEvents(serverPlayer, previousData, updatedData);
            }
        }
    }

    private static AbilityData syncAutomaticPermissions(ServerPlayer player, AbilityData currentData) {
        AbilityData updatedData = currentData;
        Set<ResourceLocation> restrictedRecipes = restrictedRecipes(player);
        for (ResourceLocation recipeId : restrictedRecipes) {
            RestrictedRecipeDefinition definition = findRestrictedRecipe(player, recipeId).orElse(null);
            ResourceLocation sourceId = advancementSourceId(recipeId);
            boolean unlocked = definition != null && hasUnlockedAdvancement(player, definition);
            updatedData = updatedData.withRecipePermissionSource(recipeId, sourceId, unlocked);
        }

        for (ResourceLocation recipeId : Set.copyOf(updatedData.recipePermissions())) {
            ResourceLocation sourceId = advancementSourceId(recipeId);
            if (!updatedData.recipePermissionSourcesFor(recipeId).contains(sourceId)) {
                continue;
            }
            RestrictedRecipeDefinition definition = findRestrictedRecipe(player, recipeId).orElse(null);
            if (definition == null || !hasUnlockedAdvancement(player, definition)) {
                updatedData = updatedData.withRecipePermissionSource(recipeId, sourceId, false);
            }
        }
        return updatedData;
    }

    private static boolean hasUnlockedAdvancement(ServerPlayer player, RestrictedRecipeDefinition definition) {
        if (definition.unlockAdvancements().isEmpty()) {
            return false;
        }
        return definition.unlockAdvancements().stream().anyMatch(advancementId -> player.server.getAdvancements().get(advancementId) != null
                && player.getAdvancements().getOrStartProgress(player.server.getAdvancements().get(advancementId)).isDone());
    }

    private static ResourceLocation advancementSourceId(ResourceLocation recipeId) {
        return ResourceLocation.fromNamespaceAndPath(
                XLib.MODID,
                ADVANCEMENT_SOURCE_PATH_PREFIX + recipeId.getNamespace() + "/" + recipeId.getPath()
        );
    }

    private static void postPermissionEvents(ServerPlayer player, AbilityData previousData, AbilityData currentData) {
        Set<ResourceLocation> recipeIds = new LinkedHashSet<>(previousData.recipePermissions());
        recipeIds.addAll(currentData.recipePermissions());
        for (ResourceLocation recipeId : recipeIds) {
            Set<ResourceLocation> previousSources = previousData.recipePermissionSourcesFor(recipeId);
            Set<ResourceLocation> currentSources = currentData.recipePermissionSourcesFor(recipeId);
            if (previousSources.equals(currentSources)) {
                continue;
            }
            NeoForge.EVENT_BUS.post(new XLibRecipePermissionEvent(
                    player,
                    recipeId,
                    previousSources,
                    currentSources,
                    previousData,
                    currentData
            ));
        }
    }

    private static Component parseUnlockHint(JsonElement unlockHintElement) {
        if (unlockHintElement instanceof JsonPrimitive primitive && primitive.isString()) {
            return Component.literal(primitive.getAsString());
        }

        if (ServerLifecycleHooks.getCurrentServer() == null) {
            return Component.literal(unlockHintElement.toString());
        }
        return Component.Serializer.fromJson(unlockHintElement, ServerLifecycleHooks.getCurrentServer().registryAccess());
    }

    private static CompoundTag parseOutputTag(String rawOutputTag) {
        try {
            return TagParser.parseTag(rawOutputTag);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid output_nbt: " + rawOutputTag, exception);
        }
    }

    private record LoadedRestrictionState(
            Map<ResourceLocation, RestrictedRecipeDefinition> exactRecipes,
            Map<ResourceLocation, RestrictedRecipeRule> rules
    ) {
        private static LoadedRestrictionState empty() {
            return new LoadedRestrictionState(Map.of(), Map.of());
        }
    }

    private record ResolvedRestrictionCache(
            @Nullable net.minecraft.world.item.crafting.RecipeManager recipeManager,
            @Nullable RegistryAccess registries,
            Map<ResourceLocation, RestrictedRecipeDefinition> definitions,
            Map<String, Set<ResourceLocation>> recipesByNamespace,
            Map<ResourceLocation, Set<ResourceLocation>> recipesByCategory,
            Map<ResourceLocation, Set<ResourceLocation>> recipesByOutput,
            Map<ResourceLocation, Set<ResourceLocation>> recipesByOutputTag
    ) {
        private static ResolvedRestrictionCache empty() {
            return create(null, Map.of());
        }

        private static ResolvedRestrictionCache create(
                @Nullable RecipeContext context,
                Map<ResourceLocation, RestrictedRecipeDefinition> resolvedDefinitions
        ) {
            LinkedHashMap<ResourceLocation, RestrictedRecipeDefinition> copiedDefinitions = new LinkedHashMap<>(resolvedDefinitions);
            LinkedHashMap<String, LinkedHashSet<ResourceLocation>> namespaceIndex = new LinkedHashMap<>();
            LinkedHashMap<ResourceLocation, LinkedHashSet<ResourceLocation>> categoryIndex = new LinkedHashMap<>();
            LinkedHashMap<ResourceLocation, LinkedHashSet<ResourceLocation>> outputIndex = new LinkedHashMap<>();
            LinkedHashMap<ResourceLocation, LinkedHashSet<ResourceLocation>> outputTagIndex = new LinkedHashMap<>();
            for (RestrictedRecipeDefinition definition : copiedDefinitions.values()) {
                index(namespaceIndex, definition.recipeNamespaces(), definition.recipeId());
                index(categoryIndex, definition.categories(), definition.recipeId());
                index(outputIndex, definition.outputs(), definition.recipeId());
                index(outputTagIndex, definition.outputItemTags(), definition.recipeId());
            }
            return new ResolvedRestrictionCache(
                    context != null ? context.recipeManager() : null,
                    context != null ? context.registries() : null,
                    Map.copyOf(copiedDefinitions),
                    immutableIndex(namespaceIndex),
                    immutableIndex(categoryIndex),
                    immutableIndex(outputIndex),
                    immutableIndex(outputTagIndex)
            );
        }

        private boolean matches(@Nullable RecipeContext context) {
            if (context == null) {
                return this.recipeManager == null && this.registries == null;
            }
            return this.recipeManager == context.recipeManager() && this.registries == context.registries();
        }

        private Set<ResourceLocation> recipeIds() {
            return Set.copyOf(this.definitions.keySet());
        }

        private Set<ResourceLocation> recipesInCategory(ResourceLocation categoryId) {
            return this.recipesByCategory.getOrDefault(categoryId, Set.of());
        }

        private Set<ResourceLocation> recipesInNamespace(String namespace) {
            return this.recipesByNamespace.getOrDefault(namespace, Set.of());
        }

        private Set<ResourceLocation> recipesForOutput(ResourceLocation outputItemId) {
            return this.recipesByOutput.getOrDefault(outputItemId, Set.of());
        }

        private Set<ResourceLocation> recipesForOutputTag(ResourceLocation outputItemTagId) {
            return this.recipesByOutputTag.getOrDefault(outputItemTagId, Set.of());
        }

        private static <K> void index(
                Map<K, LinkedHashSet<ResourceLocation>> index,
                Collection<K> keys,
                ResourceLocation recipeId
        ) {
            for (K key : keys) {
                index.computeIfAbsent(key, unused -> new LinkedHashSet<>()).add(recipeId);
            }
        }

        private static <K> Map<K, Set<ResourceLocation>> immutableIndex(
                Map<K, LinkedHashSet<ResourceLocation>> mutableIndex
        ) {
            LinkedHashMap<K, Set<ResourceLocation>> immutableIndex = new LinkedHashMap<>();
            for (Map.Entry<K, LinkedHashSet<ResourceLocation>> entry : mutableIndex.entrySet()) {
                immutableIndex.put(entry.getKey(), Set.copyOf(entry.getValue()));
            }
            return Map.copyOf(immutableIndex);
        }
    }

    private record RecipeContext(net.minecraft.world.item.crafting.RecipeManager recipeManager, RegistryAccess registries) {}

    private static final class RestrictedResultSlot extends ResultSlot {
        private RestrictedResultSlot(Player player, CraftingContainer craftSlots, Container container, int slot, int xPosition, int yPosition) {
            super(player, craftSlots, container, slot, xPosition, yPosition);
        }

        @Override
        public boolean mayPickup(Player player) {
            if (!super.mayPickup(player)) {
                return false;
            }
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return true;
            }
            if (!(this.container instanceof RecipeCraftingHolder recipeHolder)) {
                return true;
            }

            RecipeHolder<?> recipe = recipeHolder.getRecipeUsed();
            if (recipe == null || canCraft(serverPlayer, recipe.id())) {
                return true;
            }

            enforceOpenCraftingResult(serverPlayer, ModAttachments.get(serverPlayer));
            return false;
        }
    }

    private static final class RestrictedRecipeReloadListener extends SimpleJsonResourceReloadListener {
        private RestrictedRecipeReloadListener() {
            super(RESTRICTION_GSON, RESTRICTION_DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonById, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, RestrictedRecipeDefinition> loadedRecipes = new LinkedHashMap<>();
            Map<ResourceLocation, RestrictedRecipeRule> loadedRules = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    readRestrictionElement(entry.getKey(), entry.getValue(), loadedRecipes, loadedRules);
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse restricted recipe file {}", entry.getKey(), exception);
                }
            }

            setDatapackRestrictions(loadedRecipes, loadedRules);

            syncOnlinePlayers();
        }
    }
}


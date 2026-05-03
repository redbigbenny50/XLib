package com.whatxe.xlib.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.whatxe.xlib.ability.RecipePermissionApi;
import com.whatxe.xlib.ability.RestrictedRecipeDefinition;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

final class RecipeAdminCommands {
    private RecipeAdminCommands() {}

    static CompletableFuture<Suggestions> suggestRecipeIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return XLibCommandSupport.suggestRecipeIds(context, builder);
    }

    static int restrict(CommandSourceStack source, ResourceLocation recipeId, boolean hiddenWhenLocked) throws CommandSyntaxException {
        XLibCommandSupport.validateRecipe(source, recipeId);
        RecipePermissionApi.registerRestrictedRecipe(RestrictedRecipeDefinition.builder(recipeId)
                .hiddenWhenLocked(hiddenWhenLocked)
                .build());
        RecipePermissionApi.syncOnlinePlayers();
        source.sendSuccess(() -> Component.translatable("command.xlib.recipes.restrict", recipeId.toString(), hiddenWhenLocked), true);
        return 1;
    }

    static int unrestrict(CommandSourceStack source, ResourceLocation recipeId) throws CommandSyntaxException {
        XLibCommandSupport.validateRecipe(source, recipeId);
        RecipePermissionApi.unregisterRestrictedRecipe(recipeId);
        RecipePermissionApi.syncOnlinePlayers();
        source.sendSuccess(() -> Component.translatable("command.xlib.recipes.unrestrict", recipeId.toString()), true);
        return 1;
    }

    static int grant(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation recipeId) throws CommandSyntaxException {
        XLibCommandSupport.validateRecipe(source, recipeId);
        for (ServerPlayer player : targets) {
            RecipePermissionApi.grant(player, recipeId);
        }
        source.sendSuccess(() -> Component.translatable("command.xlib.recipes.grant", recipeId.toString(), targets.size()), true);
        return targets.size();
    }

    static int revoke(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation recipeId) throws CommandSyntaxException {
        XLibCommandSupport.validateRecipe(source, recipeId);
        for (ServerPlayer player : targets) {
            RecipePermissionApi.revoke(player, recipeId);
        }
        source.sendSuccess(() -> Component.translatable("command.xlib.recipes.revoke", recipeId.toString(), targets.size()), true);
        return targets.size();
    }

    static int clear(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            RecipePermissionApi.clearPermissions(player);
        }
        source.sendSuccess(() -> Component.translatable("command.xlib.recipes.clear", targets.size()), true);
        return targets.size();
    }

    static int list(CommandSourceStack source, ServerPlayer target) {
        source.sendSuccess(
                () -> Component.translatable(
                        "command.xlib.recipes.list",
                        target.getName(),
                        XLibCommandSupport.joinIds(RecipePermissionApi.permissions(target)),
                        XLibCommandSupport.joinIds(RecipePermissionApi.lockedRecipes(target))
                ),
                false
        );
        return RecipePermissionApi.permissions(target).size();
    }

    static int inspect(CommandSourceStack source, ServerPlayer target, ResourceLocation recipeId) throws CommandSyntaxException {
        XLibCommandSupport.validateRecipe(source, recipeId);
        Optional<RestrictedRecipeDefinition> definition = RecipePermissionApi.findRestrictedRecipe(recipeId);
        String access = RecipePermissionApi.accessState(target, recipeId).name().toLowerCase(Locale.ROOT);
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " | " + recipeId + " | access=" + access), false);
        source.sendSuccess(() -> Component.literal("permission_sources=" + XLibCommandSupport.joinIds(RecipePermissionApi.permissionSources(target, recipeId))), false);
        source.sendSuccess(() -> Component.literal("visible_when_locked=" + !RecipePermissionApi.shouldHideLockedRecipe(target, recipeId)), false);
        if (definition.isPresent()) {
            source.sendSuccess(
                    () -> Component.literal("metadata="
                            + XLibCommandSupport.formatMetadata(
                                    definition.get().familyId(),
                                    definition.get().groupId(),
                                    definition.get().pageId(),
                                    definition.get().tags()
                            )),
                    false
            );
            source.sendSuccess(() -> Component.literal("recipe_tags=" + XLibCommandSupport.joinIds(definition.get().recipeTags())), false);
            source.sendSuccess(() -> Component.literal("recipe_namespaces=" + String.join(", ", definition.get().recipeNamespaces())), false);
            source.sendSuccess(() -> Component.literal("categories=" + XLibCommandSupport.joinIds(definition.get().categories())), false);
            source.sendSuccess(() -> Component.literal("outputs=" + XLibCommandSupport.joinIds(definition.get().outputs())), false);
            source.sendSuccess(() -> Component.literal("output_item_tags=" + XLibCommandSupport.joinIds(definition.get().outputItemTags())), false);
            source.sendSuccess(() -> Component.literal("unlock_sources=" + XLibCommandSupport.joinIds(definition.get().unlockSources())), false);
            source.sendSuccess(() -> Component.literal("unlock_advancements=" + XLibCommandSupport.joinIds(definition.get().unlockAdvancements())), false);
            source.sendSuccess(() -> Component.literal(
                    "matched_rule=" + definition.get().matchedRuleId().map(ResourceLocation::toString).orElse("-")
                            + " | matched_rule_priority=" + definition.get().matchedRulePriority()
            ), false);
            source.sendSuccess(() -> Component.literal(
                    "hidden_when_locked=" + definition.get().hiddenWhenLocked()
                            + " | unlock_hint=" + Optional.ofNullable(definition.get().unlockHint()).map(Component::getString).orElse("-")
                            + " | output_nbt=" + Optional.ofNullable(definition.get().outputTag()).map(Object::toString).orElse("-")
            ), false);
        } else {
            source.sendSuccess(() -> Component.literal("restricted_definition=-"), false);
        }
        return 1;
    }

    static int sources(CommandSourceStack source, ServerPlayer target, ResourceLocation recipeId) throws CommandSyntaxException {
        XLibCommandSupport.validateRecipe(source, recipeId);
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " | " + recipeId), false);
        source.sendSuccess(() -> Component.literal("permission_sources=" + XLibCommandSupport.joinIds(RecipePermissionApi.permissionSources(target, recipeId))), false);
        source.sendSuccess(() -> Component.literal("unlock_sources=" + XLibCommandSupport.joinIds(RecipePermissionApi.unlockSources(recipeId))), false);
        return 1;
    }
}

package com.whatxe.xlib.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityGrantApi;
import com.whatxe.xlib.ability.GrantedItemGrantApi;
import com.whatxe.xlib.ability.PassiveGrantApi;
import com.whatxe.xlib.ability.RecipePermissionApi;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.progression.UpgradeApi;
import com.whatxe.xlib.progression.UpgradeProgressData;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

final class DebugAdminCommands {
    private static final DynamicCommandExceptionType DEBUG_EXPORT_FAILED =
            new DynamicCommandExceptionType(value -> Component.literal("Failed to write XLib debug export: " + value));
    private static final DynamicCommandExceptionType DEBUG_DIFF_FAILED =
            new DynamicCommandExceptionType(value -> Component.literal("Failed to diff XLib debug snapshot: " + value));
    private static final Gson DEBUG_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter DEBUG_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);

    private DebugAdminCommands() {}

    static int counters(CommandSourceStack source, ServerPlayer target) {
        XLibDebugCounters counters = XLibDebugCounters.collect(target);
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " | debug_counters"), false);
        source.sendSuccess(() -> Component.literal(counters.summary()), false);
        return 1;
    }

    static int dump(CommandSourceStack source, ServerPlayer target) {
        AbilityData data = ModAttachments.get(target);
        UpgradeProgressData progressionData = UpgradeApi.get(target);
        XLibDebugCounters counters = XLibDebugCounters.collect(target);
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " | uuid=" + target.getStringUUID()), false);
        source.sendSuccess(() -> Component.literal("debug_counters=" + counters.summary()), false);
        source.sendSuccess(() -> Component.literal("restricted=" + data.abilityAccessRestricted()
                + " | managed_sources=" + XLibCommandSupport.joinIds(data.managedGrantSources())), false);
        source.sendSuccess(() -> Component.literal("granted_abilities=" + XLibCommandSupport.joinIds(AbilityGrantApi.grantedAbilities(target))), false);
        source.sendSuccess(() -> Component.literal("blocked_abilities=" + XLibCommandSupport.joinIds(AbilityGrantApi.blockedAbilities(target))), false);
        source.sendSuccess(() -> Component.literal("passives=" + XLibCommandSupport.joinIds(PassiveGrantApi.grantedPassives(target))), false);
        source.sendSuccess(() -> Component.literal("granted_items=" + XLibCommandSupport.joinIds(GrantedItemGrantApi.grantedItems(target))), false);
        source.sendSuccess(() -> Component.literal("recipe_permissions=" + XLibCommandSupport.joinIds(RecipePermissionApi.permissions(target))), false);
        source.sendSuccess(() -> Component.literal("locked_recipes=" + XLibCommandSupport.joinIds(RecipePermissionApi.lockedRecipes(target))), false);
        source.sendSuccess(() -> Component.literal("point_balances=" + XLibCommandSupport.formatNumericMap(progressionData.pointBalances())), false);
        source.sendSuccess(() -> Component.literal("counters=" + XLibCommandSupport.formatNumericMap(progressionData.counters())), false);
        source.sendSuccess(() -> Component.literal("unlocked_nodes=" + XLibCommandSupport.joinIds(progressionData.unlockedNodes())), false);
        source.sendSuccess(() -> Component.literal("visible_tracks=" + XLibCommandSupport.formatTrackIds(UpgradeApi.visibleTracks(progressionData))), false);
        source.sendSuccess(() -> Component.literal("slots=" + XLibCommandSupport.formatSlots(data)), false);
        source.sendSuccess(() -> Component.literal("resolved_slots=" + XLibCommandSupport.formatResolvedSlots(data)), false);
        source.sendSuccess(() -> Component.literal("mode_loadouts=" + XLibCommandSupport.formatModeLoadouts(data)), false);
        source.sendSuccess(() -> Component.literal("mode_overlays=" + XLibCommandSupport.formatModeOverlays(data)), false);
        source.sendSuccess(() -> Component.literal("active=" + XLibCommandSupport.joinIds(data.activeModes())), false);
        source.sendSuccess(() -> Component.literal("combo_windows=" + XLibCommandSupport.formatComboWindows(data)), false);
        source.sendSuccess(() -> Component.literal("combo_overrides=" + XLibCommandSupport.formatComboOverrides(data)), false);
        source.sendSuccess(() -> Component.literal("cooldowns=" + XLibCommandSupport.formatCooldowns(data)), false);
        source.sendSuccess(() -> Component.literal("charges=" + XLibCommandSupport.formatCharges(data)), false);
        source.sendSuccess(() -> Component.literal("resources=" + XLibCommandSupport.formatResources(data)), false);
        source.sendSuccess(() -> Component.literal("ability_sources=" + XLibCommandSupport.formatSourceMap(data.abilityGrantSources())), false);
        source.sendSuccess(() -> Component.literal("activation_block_sources=" + XLibCommandSupport.formatSourceMap(data.abilityActivationBlockSources())), false);
        source.sendSuccess(() -> Component.literal("passive_sources=" + XLibCommandSupport.formatSourceMap(data.passiveGrantSources())), false);
        source.sendSuccess(() -> Component.literal("granted_item_sources=" + XLibCommandSupport.formatSourceMap(data.grantedItemSources())), false);
        source.sendSuccess(() -> Component.literal("recipe_permission_sources=" + XLibCommandSupport.formatSourceMap(data.recipePermissionSources())), false);
        source.sendSuccess(() -> Component.literal("source_groups=" + XLibCommandSupport.formatSourceGroups(data)), false);
        return 1;
    }

    static int export(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        Path debugDirectory = source.getServer().getFile("debug/xlib");
        String fileName = target.getGameProfile().getName() + "-" + DEBUG_TIMESTAMP.format(LocalDateTime.now()) + ".json";
        Path exportPath = debugDirectory.resolve(fileName);

        try {
            Files.createDirectories(debugDirectory);
            Files.writeString(
                    exportPath,
                    DEBUG_GSON.toJson(XLibCommandSupport.buildDebugJson(target)),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );
        } catch (IOException exception) {
            throw DEBUG_EXPORT_FAILED.create(exception.getMessage());
        }

        source.sendSuccess(() -> Component.literal("Wrote XLib debug export: " + exportPath.toAbsolutePath()), false);
        return 1;
    }

    static int source(CommandSourceStack source, ServerPlayer target, ResourceLocation sourceId) {
        AbilityData data = ModAttachments.get(target);
        XLibCommandSupport.SourceGroup group = XLibCommandSupport.buildSourceGroups(data).get(sourceId);
        if (group == null) {
            source.sendSuccess(() -> Component.literal("No XLib state currently tracked for source " + sourceId), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " | source=" + sourceId), false);
        source.sendSuccess(() -> Component.literal("abilities=" + XLibCommandSupport.joinIds(group.abilities())), false);
        source.sendSuccess(() -> Component.literal("blocked_abilities=" + XLibCommandSupport.joinIds(group.blockedAbilities())), false);
        source.sendSuccess(() -> Component.literal("passives=" + XLibCommandSupport.joinIds(group.passives())), false);
        source.sendSuccess(() -> Component.literal("granted_items=" + XLibCommandSupport.joinIds(group.grantedItems())), false);
        source.sendSuccess(() -> Component.literal("recipe_permissions=" + XLibCommandSupport.joinIds(group.recipePermissions())), false);
        return 1;
    }

    static int diff(CommandSourceStack source, ServerPlayer target, String snapshotName) throws CommandSyntaxException {
        Path debugDirectory = source.getServer().getFile("debug/xlib");
        String safeFileName = Paths.get(snapshotName).getFileName().toString();
        Path snapshotPath = debugDirectory.resolve(safeFileName);
        JsonObject previousSnapshot;
        try {
            previousSnapshot = JsonParser.parseString(Files.readString(snapshotPath, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException | IOException exception) {
            throw DEBUG_DIFF_FAILED.create(exception.getMessage());
        }

        JsonObject currentSnapshot = XLibCommandSupport.buildDebugJson(target);
        source.sendSuccess(
                () -> Component.literal("Comparing current state for " + target.getGameProfile().getName() + " against " + snapshotPath.toAbsolutePath()),
                false
        );
        XLibCommandSupport.sendArrayDiff(source, "granted_abilities", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "blocked_abilities", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "passives", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "granted_items", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "recipe_permissions", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "locked_recipes", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendNumericMapDiff(source, "point_balances", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendNumericMapDiff(source, "counters", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "unlocked_nodes", previousSnapshot, currentSnapshot);
        XLibCommandSupport.sendArrayDiff(source, "visible_tracks", previousSnapshot, currentSnapshot);
        return 1;
    }
}

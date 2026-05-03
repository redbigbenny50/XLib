package com.whatxe.xlib.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;

final class ContentCommandTree {
    private ContentCommandTree() {}

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("content")
                .then(Commands.literal("reference")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listReferenceTopics(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("topic", StringArgumentType.word())
                                        .suggests(DataDrivenContentDebugCommands::suggestReferenceTopics)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectReferenceTopic(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "topic")
                                        )))))
                .then(Commands.literal("conditions")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listConditions(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestConditionIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectCondition(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("context_grants")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listContextGrants(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestContextGrantIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectContextGrant(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("equipment_bindings")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listEquipmentBindings(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestEquipmentBindingIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectEquipmentBinding(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("grant_bundles")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listGrantBundles(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestGrantBundleIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectGrantBundle(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("artifacts")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listArtifacts(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestArtifactIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectArtifact(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("abilities")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listAbilities(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestAbilityIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectAbility(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("passives")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listPassives(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestPassiveIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectPassive(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("identities")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listIdentities(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestIdentityIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectIdentity(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("support_packages")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listSupportPackages(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestSupportPackageIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectSupportPackage(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("profile_groups")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listProfileGroups(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestProfileGroupIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectProfileGroup(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("profiles")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listProfiles(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestProfileIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectProfile(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("modes")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listModes(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestModeIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectMode(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("combo_chains")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listComboChains(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestComboChainIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectComboChain(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("lifecycle_stages")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listLifecycleStages(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestLifecycleStageIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectLifecycleStage(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("capability_policies")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listCapabilityPolicies(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestCapabilityPolicyIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectCapabilityPolicy(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("visual_forms")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listVisualForms(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestVisualFormIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectVisualForm(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("tracked_values")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listTrackedValues(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestTrackedValueIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectTrackedValue(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("tracked_value_rules")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listTrackedValueRules(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestTrackedValueRuleIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectTrackedValueRule(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("damage_modifier_profiles")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listDamageModifierProfiles(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestDamageModifierProfileIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectDamageModifierProfile(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("upgrade_point_types")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listUpgradePointTypes(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestUpgradePointTypeIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectUpgradePointType(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("upgrade_tracks")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listUpgradeTracks(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestUpgradeTrackIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectUpgradeTrack(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("upgrade_nodes")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listUpgradeNodes(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestUpgradeNodeIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectUpgradeNode(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("upgrade_consume_rules")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listUpgradeConsumeRules(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestUpgradeConsumeRuleIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectUpgradeConsumeRule(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))))
                .then(Commands.literal("upgrade_kill_rules")
                        .then(Commands.literal("list")
                                .executes(context -> DataDrivenContentDebugCommands.listUpgradeKillRules(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(DataDrivenContentDebugCommands::suggestUpgradeKillRuleIds)
                                        .executes(context -> DataDrivenContentDebugCommands.inspectUpgradeKillRule(
                                                context.getSource(),
                                                ResourceLocationArgument.getId(context, "id")
                                        )))));
    }
}

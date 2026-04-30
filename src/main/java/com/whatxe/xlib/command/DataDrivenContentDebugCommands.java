package com.whatxe.xlib.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.whatxe.xlib.ability.AuthoredJsonReferenceDocs;
import com.whatxe.xlib.ability.AbilityRequirement;
import com.whatxe.xlib.ability.AbilityRequirements;
import com.whatxe.xlib.ability.AbilitySlotReference;
import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.ArtifactDefinition;
import com.whatxe.xlib.ability.ArtifactPresenceMode;
import com.whatxe.xlib.ability.ComboChainDefinition;
import com.whatxe.xlib.ability.ContextGrantSnapshot;
import com.whatxe.xlib.ability.DataDrivenAbilityApi;
import com.whatxe.xlib.ability.DataDrivenArtifactApi;
import com.whatxe.xlib.ability.DataDrivenComboChainApi;
import com.whatxe.xlib.ability.DataDrivenConditionApi;
import com.whatxe.xlib.ability.DataDrivenContextGrantApi;
import com.whatxe.xlib.ability.DataDrivenEquipmentBindingApi;
import com.whatxe.xlib.ability.DataDrivenGrantBundleApi;
import com.whatxe.xlib.ability.DataDrivenIdentityApi;
import com.whatxe.xlib.ability.DataDrivenModeApi;
import com.whatxe.xlib.ability.DataDrivenPassiveApi;
import com.whatxe.xlib.ability.DataDrivenProfileApi;
import com.whatxe.xlib.ability.DataDrivenProfileGroupApi;
import com.whatxe.xlib.ability.DataDrivenSupportPackageApi;
import com.whatxe.xlib.capability.CapabilityPolicyDefinition;
import com.whatxe.xlib.capability.DataDrivenCapabilityPolicyApi;
import com.whatxe.xlib.form.DataDrivenVisualFormApi;
import com.whatxe.xlib.form.VisualFormDefinition;
import com.whatxe.xlib.lifecycle.DataDrivenLifecycleStageApi;
import com.whatxe.xlib.lifecycle.LifecycleStageDefinition;
import com.whatxe.xlib.ability.GrantBundleDefinition;
import com.whatxe.xlib.ability.IdentityDefinition;
import com.whatxe.xlib.ability.ModeDefinition;
import com.whatxe.xlib.ability.PassiveDefinition;
import com.whatxe.xlib.ability.ProfileDefinition;
import com.whatxe.xlib.ability.ProfileGroupDefinition;
import com.whatxe.xlib.ability.SupportPackageDefinition;
import com.whatxe.xlib.progression.DataDrivenUpgradeConsumeRuleApi;
import com.whatxe.xlib.progression.DataDrivenUpgradeKillRuleApi;
import com.whatxe.xlib.progression.DataDrivenUpgradeNodeApi;
import com.whatxe.xlib.progression.DataDrivenUpgradePointTypeApi;
import com.whatxe.xlib.progression.DataDrivenUpgradeTrackApi;
import com.whatxe.xlib.progression.ProgressionJsonReferenceDocs;
import com.whatxe.xlib.progression.UpgradeConsumeRule;
import com.whatxe.xlib.progression.UpgradeKillRule;
import com.whatxe.xlib.progression.UpgradeNodeDefinition;
import com.whatxe.xlib.progression.UpgradePointType;
import com.whatxe.xlib.progression.UpgradeRequirement;
import com.whatxe.xlib.progression.UpgradeTrackDefinition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

final class DataDrivenContentDebugCommands {
    private static final DynamicCommandExceptionType UNKNOWN_NAMED_CONDITION =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown named condition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_CONTEXT_GRANT =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown context grant definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_EQUIPMENT_BINDING =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown equipment binding definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_GRANT_BUNDLE =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown grant bundle definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_ARTIFACT =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown artifact definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_ABILITY =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown ability definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_IDENTITY =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown identity definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_SUPPORT_PACKAGE =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown support package definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_MODE =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown mode definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_COMBO_CHAIN =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown combo chain definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_PASSIVE =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown passive definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_PROFILE_GROUP =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown profile group definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_PROFILE =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown profile definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_UPGRADE_POINT_TYPE =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown upgrade point type definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_UPGRADE_TRACK =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown upgrade track definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_UPGRADE_NODE =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown upgrade node definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_UPGRADE_CONSUME_RULE =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown upgrade consume rule definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_UPGRADE_KILL_RULE =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown upgrade kill rule definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_LIFECYCLE_STAGE =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown lifecycle stage definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_CAPABILITY_POLICY =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown capability policy definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_VISUAL_FORM =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown visual form definition: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_REFERENCE_TOPIC =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown generated reference topic: " + value));

    private DataDrivenContentDebugCommands() {}

    static CompletableFuture<Suggestions> suggestConditionIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenConditionApi.allConditionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestContextGrantIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenContextGrantApi.allDefinitionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestEquipmentBindingIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenEquipmentBindingApi.allDefinitionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestGrantBundleIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenGrantBundleApi.allDefinitionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestArtifactIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenArtifactApi.allDefinitionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestAbilityIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenAbilityApi.allDefinitionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestIdentityIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenIdentityApi.allDefinitionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestSupportPackageIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenSupportPackageApi.allDefinitionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestModeIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenModeApi.allDefinitionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestComboChainIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenComboChainApi.allDefinitionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestPassiveIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenPassiveApi.allDefinitionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestProfileGroupIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenProfileGroupApi.allDefinitionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestProfileIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenProfileApi.allDefinitionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestUpgradePointTypeIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenUpgradePointTypeApi.allDefinitionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestUpgradeTrackIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenUpgradeTrackApi.allDefinitionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestUpgradeNodeIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenUpgradeNodeApi.allDefinitionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestUpgradeConsumeRuleIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenUpgradeConsumeRuleApi.allDefinitionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestUpgradeKillRuleIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenUpgradeKillRuleApi.allDefinitionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestReferenceTopics(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggest(referenceTopicIds(), builder);
    }

    static int listConditions(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenConditionApi.allConditionIds();
        source.sendSuccess(() -> Component.literal("named_conditions=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectCondition(CommandSourceStack source, ResourceLocation conditionId) throws CommandSyntaxException {
        AbilityRequirement condition = DataDrivenConditionApi.findCondition(conditionId)
                .orElseThrow(() -> UNKNOWN_NAMED_CONDITION.create(conditionId.toString()));
        source.sendSuccess(() -> Component.literal(conditionId + " | when=" + condition.description().getString()), false);
        return 1;
    }

    static int listReferenceTopics(CommandSourceStack source) {
        String topics = referenceTopicIds()
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
        source.sendSuccess(() -> Component.literal("reference_topics=" + topics), false);
        return (int) referenceTopicIds().count();
    }

    static int inspectReferenceTopic(CommandSourceStack source, String topicId) throws CommandSyntaxException {
        Optional<ProgressionJsonReferenceDocs.ReferenceSurface> progressionSurface = ProgressionJsonReferenceDocs.findSurface(topicId);
        if (progressionSurface.isPresent()) {
            ProgressionJsonReferenceDocs.ReferenceSurface surface = progressionSurface.orElseThrow();
            source.sendSuccess(() -> Component.literal(surface.title() + " | " + surface.location()), false);
            for (String line : ProgressionJsonReferenceDocs.renderCommandLines(surface)) {
                source.sendSuccess(() -> Component.literal(line), false);
            }
            return 1;
        }
        AuthoredJsonReferenceDocs.ReferenceSurface authoredSurface = AuthoredJsonReferenceDocs.findSurface(topicId)
                .orElseThrow(() -> UNKNOWN_REFERENCE_TOPIC.create(topicId));
        source.sendSuccess(() -> Component.literal(authoredSurface.title() + " | " + authoredSurface.location()), false);
        for (String line : AuthoredJsonReferenceDocs.renderCommandLines(authoredSurface)) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    static int listContextGrants(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenContextGrantApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("context_grants=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectContextGrant(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        DataDrivenContextGrantApi.LoadedContextGrantDefinition definition = DataDrivenContextGrantApi.findDefinition(definitionId)
                .orElseThrow(() -> UNKNOWN_CONTEXT_GRANT.create(definitionId.toString()));
        source.sendSuccess(
                () -> Component.literal(definition.id() + " | source=" + definition.snapshot().sourceId()
                        + " | when=" + definition.requirement().description().getString()),
                false
        );
        source.sendSuccess(() -> Component.literal("snapshot=" + formatSnapshot(definition.snapshot())), false);
        return 1;
    }

    static int listEquipmentBindings(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenEquipmentBindingApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("equipment_bindings=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectEquipmentBinding(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        DataDrivenEquipmentBindingApi.LoadedEquipmentBindingDefinition definition = DataDrivenEquipmentBindingApi.findDefinition(definitionId)
                .orElseThrow(() -> UNKNOWN_EQUIPMENT_BINDING.create(definitionId.toString()));
        source.sendSuccess(
                () -> Component.literal(definition.id() + " | source=" + definition.sourceId()
                        + " | when=" + definition.requirement().description().getString()),
                false
        );
        source.sendSuccess(() -> Component.literal("matchers=" + formatMatchers(definition)), false);
        source.sendSuccess(
                () -> Component.literal("snapshot=" + formatSnapshot(definition.snapshot())
                        + " | grant_bundles=" + XLibCommandSupport.joinIds(definition.grantBundles())
                        + " | unlock_artifacts=" + XLibCommandSupport.joinIds(definition.unlockArtifacts())),
                false
        );
        return 1;
    }

    static int listGrantBundles(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenGrantBundleApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("grant_bundles=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectGrantBundle(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        GrantBundleDefinition definition = DataDrivenGrantBundleApi.findDefinition(definitionId)
                .map(DataDrivenGrantBundleApi.LoadedGrantBundleDefinition::definition)
                .orElseThrow(() -> UNKNOWN_GRANT_BUNDLE.create(definitionId.toString()));
        source.sendSuccess(() -> Component.literal(definition.id() + " | " + formatGrantBundle(definition)), false);
        return 1;
    }

    static int listArtifacts(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenArtifactApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("artifacts=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int listAbilities(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenAbilityApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("abilities=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectArtifact(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        ArtifactDefinition definition = DataDrivenArtifactApi.findDefinition(definitionId)
                .map(DataDrivenArtifactApi.LoadedArtifactDefinition::definition)
                .orElseThrow(() -> UNKNOWN_ARTIFACT.create(definitionId.toString()));
        source.sendSuccess(
                () -> Component.literal(definition.id()
                        + " | items=" + XLibCommandSupport.joinIds(definition.itemIds())
                        + " | presence=" + formatPresenceModes(definition.presenceModes())
                        + " | when=" + describeRequirements(definition.requirements())
                        + " | unlock_on_consume=" + definition.unlockOnConsume()),
                false
        );
        source.sendSuccess(
                () -> Component.literal("equipped_bundles=" + XLibCommandSupport.joinIds(definition.equippedBundles())
                        + " | unlocked_bundles=" + XLibCommandSupport.joinIds(definition.unlockedBundles())),
                false
        );
        return 1;
    }

    static int inspectAbility(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        AbilityDefinition definition = DataDrivenAbilityApi.findDefinition(definitionId)
                .map(DataDrivenAbilityApi.LoadedAbilityDefinition::definition)
                .orElseThrow(() -> UNKNOWN_ABILITY.create(definitionId.toString()));
        source.sendSuccess(
                () -> Component.literal(definition.id()
                        + " | display=" + definition.displayName().getString()
                        + " | family=" + definition.familyId().map(ResourceLocation::toString).orElse("-")
                        + " | group=" + definition.groupId().map(ResourceLocation::toString).orElse("-")
                        + " | page=" + definition.pageId().map(ResourceLocation::toString).orElse("-")
                        + " | tags=" + XLibCommandSupport.joinIds(definition.tags())),
                false
        );
        source.sendSuccess(
                () -> Component.literal("cooldown_ticks=" + definition.cooldownTicks()
                        + " | cooldown_policy=" + definition.cooldownPolicy().name().toLowerCase(java.util.Locale.ROOT)
                        + " | toggle=" + definition.toggleAbility()
                        + " | duration_ticks=" + definition.durationTicks()
                        + " | max_charges=" + definition.maxCharges()
                        + " | charge_recharge_ticks=" + definition.chargeRechargeTicks()),
                false
        );
        source.sendSuccess(
                () -> Component.literal("assign_when=" + describeRequirements(definition.assignRequirements())
                        + " | activate_when=" + describeRequirements(definition.activateRequirements())
                        + " | active_when=" + describeRequirements(definition.stayActiveRequirements())
                        + " | render_when=" + describeRequirements(definition.renderRequirements())),
                false
        );
        source.sendSuccess(
                () -> Component.literal("resource_costs=" + formatAbilityCosts(definition)
                        + " | custom_description=" + definition.hasCustomDescription()),
                false
        );
        return 1;
    }

    static int listIdentities(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenIdentityApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("identities=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectIdentity(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        IdentityDefinition definition = DataDrivenIdentityApi.findDefinition(definitionId)
                .map(DataDrivenIdentityApi.LoadedIdentityDefinition::definition)
                .orElseThrow(() -> UNKNOWN_IDENTITY.create(definitionId.toString()));
        source.sendSuccess(
                () -> Component.literal(definition.id()
                        + " | inherits=" + XLibCommandSupport.joinIds(definition.inheritedIdentities())
                        + " | grant_bundles=" + XLibCommandSupport.joinIds(definition.grantBundles())),
                false
        );
        return 1;
    }

    static int listSupportPackages(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenSupportPackageApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("support_packages=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectSupportPackage(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        SupportPackageDefinition definition = DataDrivenSupportPackageApi.findDefinition(definitionId)
                .map(DataDrivenSupportPackageApi.LoadedSupportPackageDefinition::definition)
                .orElseThrow(() -> UNKNOWN_SUPPORT_PACKAGE.create(definitionId.toString()));
        source.sendSuccess(
                () -> Component.literal(definition.id()
                        + " | grant_bundles=" + XLibCommandSupport.joinIds(definition.grantBundles())
                        + " | relationships=" + XLibCommandSupport.joinIds(definition.requiredRelationships())
                        + " | allow_self=" + definition.allowSelf()),
                false
        );
        return 1;
    }

    static int listModes(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenModeApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("modes=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectMode(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        ModeDefinition definition = DataDrivenModeApi.findDefinition(definitionId)
                .map(DataDrivenModeApi.LoadedModeDefinition::definition)
                .orElseThrow(() -> UNKNOWN_MODE.create(definitionId.toString()));
        source.sendSuccess(
                () -> Component.literal(definition.abilityId()
                        + " | family=" + definition.familyId().map(ResourceLocation::toString).orElse("-")
                        + " | group=" + definition.groupId().map(ResourceLocation::toString).orElse("-")
                        + " | page=" + definition.pageId().map(ResourceLocation::toString).orElse("-")
                        + " | tags=" + XLibCommandSupport.joinIds(definition.tags())),
                false
        );
        source.sendSuccess(
                () -> Component.literal("priority=" + definition.priority()
                        + " | stackable=" + definition.stackable()
                        + " | cycle_group=" + (definition.cycleGroupId() == null ? "-" : definition.cycleGroupId())
                        + " | cycle_order=" + definition.cycleOrder()
                        + " | reset_cycle_groups=" + XLibCommandSupport.joinIds(definition.resetCycleGroupsOnActivate())),
                false
        );
        source.sendSuccess(
                () -> Component.literal("cooldown_tick_rate_multiplier=" + definition.cooldownTickRateMultiplier()
                        + " | health_cost_per_tick=" + definition.healthCostPerTick()
                        + " | minimum_health=" + definition.minimumHealth()
                        + " | resource_delta_per_tick=" + formatDoubleMap(definition.resourceDeltaPerTick())),
                false
        );
        source.sendSuccess(
                () -> Component.literal("exclusive_modes=" + XLibCommandSupport.joinIds(definition.exclusiveModes())
                        + " | blocked_by_modes=" + XLibCommandSupport.joinIds(definition.blockedByModes())
                        + " | transforms_from=" + XLibCommandSupport.joinIds(definition.transformsFrom())
                        + " | overlays=" + formatModeOverlays(definition)),
                false
        );
        source.sendSuccess(() -> Component.literal("snapshot=" + formatSnapshot(definition.snapshot())), false);
        return 1;
    }

    static int listComboChains(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenComboChainApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("combo_chains=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectComboChain(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        DataDrivenComboChainApi.LoadedComboChainDefinition loaded = DataDrivenComboChainApi.findDefinition(definitionId)
                .orElseThrow(() -> UNKNOWN_COMBO_CHAIN.create(definitionId.toString()));
        ComboChainDefinition definition = loaded.definition();
        source.sendSuccess(
                () -> Component.literal(definition.id()
                        + " | trigger_ability=" + definition.triggerAbilityId()
                        + " | combo_ability=" + definition.comboAbilityId()
                        + " | trigger=" + definition.triggerType().name().toLowerCase(java.util.Locale.ROOT)
                        + " | window_ticks=" + definition.windowTicks()
                        + " | target=" + formatComboTarget(definition)),
                false
        );
        source.sendSuccess(() -> Component.literal("branches=" + formatComboBranches(loaded.branches())), false);
        return 1;
    }

    static int listPassives(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenPassiveApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("passives=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectPassive(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        PassiveDefinition definition = DataDrivenPassiveApi.findDefinition(definitionId)
                .map(DataDrivenPassiveApi.LoadedPassiveDefinition::definition)
                .orElseThrow(() -> UNKNOWN_PASSIVE.create(definitionId.toString()));
        source.sendSuccess(
                () -> Component.literal(definition.id()
                        + " | display=" + definition.displayName().getString()
                        + " | family=" + definition.familyId().map(ResourceLocation::toString).orElse("-")
                        + " | group=" + definition.groupId().map(ResourceLocation::toString).orElse("-")
                        + " | page=" + definition.pageId().map(ResourceLocation::toString).orElse("-")
                        + " | tags=" + XLibCommandSupport.joinIds(definition.tags())),
                false
        );
        source.sendSuccess(
                () -> Component.literal("grant_when=" + describeRequirements(definition.grantRequirements())
                        + " | active_when=" + describeRequirements(definition.activeRequirements())
                        + " | cooldown_tick_rate_multiplier=" + definition.cooldownTickRateMultiplier()),
                false
        );
        source.sendSuccess(
                () -> Component.literal("hooks=" + definition.authoredHooks().stream()
                        .map(hook -> hook.name().toLowerCase(java.util.Locale.ROOT))
                        .sorted()
                        .reduce((left, right) -> left + ", " + right)
                        .orElse("-")
                        + " | custom_description=" + definition.hasCustomDescription()),
                false
        );
        return 1;
    }

    static int listProfileGroups(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenProfileGroupApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("profile_groups=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectProfileGroup(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        ProfileGroupDefinition definition = DataDrivenProfileGroupApi.findDefinition(definitionId)
                .map(DataDrivenProfileGroupApi.LoadedProfileGroupDefinition::definition)
                .orElseThrow(() -> UNKNOWN_PROFILE_GROUP.create(definitionId.toString()));
        source.sendSuccess(
                () -> Component.literal(definition.id()
                        + " | selection_limit=" + definition.selectionLimit()
                        + " | required_onboarding=" + definition.requiredOnboarding()
                        + " | onboarding_triggers=" + definition.onboardingTriggers().stream()
                                .map(trigger -> trigger.name().toLowerCase(java.util.Locale.ROOT))
                                .sorted()
                                .reduce((left, right) -> left + ", " + right)
                                .orElse("-")),
                false
        );
        source.sendSuccess(
                () -> Component.literal("blocks_ability_use=" + definition.blocksAbilityUse()
                        + " | blocks_ability_menu=" + definition.blocksAbilityMenu()
                        + " | blocks_progression=" + definition.blocksProgression()
                        + " | player_can_reset=" + definition.playerCanReset()
                        + " | admin_can_reset=" + definition.adminCanReset()
                        + " | reopen_on_reset=" + definition.reopenOnReset()),
                false
        );
        return 1;
    }

    static int listProfiles(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenProfileApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("profiles=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectProfile(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        ProfileDefinition definition = DataDrivenProfileApi.findDefinition(definitionId)
                .map(DataDrivenProfileApi.LoadedProfileDefinition::definition)
                .orElseThrow(() -> UNKNOWN_PROFILE.create(definitionId.toString()));
        source.sendSuccess(
                () -> Component.literal(definition.id()
                        + " | group=" + definition.groupId()
                        + " | incompatible=" + XLibCommandSupport.joinIds(definition.incompatibleProfiles())),
                false
        );
        source.sendSuccess(
                () -> Component.literal("grant_bundles=" + XLibCommandSupport.joinIds(definition.grantBundles())
                        + " | identities=" + XLibCommandSupport.joinIds(definition.identities())
                        + " | abilities=" + XLibCommandSupport.joinIds(definition.abilities())
                        + " | modes=" + XLibCommandSupport.joinIds(definition.modes())
                        + " | passives=" + XLibCommandSupport.joinIds(definition.passives())),
                false
        );
        source.sendSuccess(
                () -> Component.literal("granted_items=" + XLibCommandSupport.joinIds(definition.grantedItems())
                        + " | recipe_permissions=" + XLibCommandSupport.joinIds(definition.recipePermissions())
                        + " | state_flags=" + XLibCommandSupport.joinIds(definition.stateFlags())
                        + " | unlock_artifacts=" + XLibCommandSupport.joinIds(definition.unlockedArtifacts())
                        + " | starting_nodes=" + XLibCommandSupport.joinIds(definition.startingNodes())),
                false
        );
        return 1;
    }

    static CompletableFuture<Suggestions> suggestLifecycleStageIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenLifecycleStageApi.allDefinitionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestCapabilityPolicyIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenCapabilityPolicyApi.allDefinitionIds(), builder);
    }

    static CompletableFuture<Suggestions> suggestVisualFormIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(DataDrivenVisualFormApi.allDefinitionIds(), builder);
    }

    static int listLifecycleStages(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenLifecycleStageApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("lifecycle_stages=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectLifecycleStage(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        LifecycleStageDefinition definition = DataDrivenLifecycleStageApi.findDefinition(definitionId)
                .map(DataDrivenLifecycleStageApi.LoadedLifecycleStageDefinition::definition)
                .orElseThrow(() -> UNKNOWN_LIFECYCLE_STAGE.create(definitionId.toString()));
        source.sendSuccess(
                () -> Component.literal(definition.id()
                        + " | duration_ticks=" + definition.durationTicks().map(Object::toString).orElse("-")
                        + " | auto_transitions=" + definition.autoTransitions().size()
                        + " | manual_targets=" + XLibCommandSupport.joinIds(definition.manualTransitionTargets())),
                false
        );
        source.sendSuccess(
                () -> Component.literal("project_state_flags=" + XLibCommandSupport.joinIds(definition.projectedStateFlags())
                        + " | project_grant_bundles=" + XLibCommandSupport.joinIds(definition.projectedGrantBundles())
                        + " | project_identities=" + XLibCommandSupport.joinIds(definition.projectedIdentities())),
                false
        );
        source.sendSuccess(
                () -> Component.literal("project_capability_policies=" + XLibCommandSupport.joinIds(definition.projectedCapabilityPolicies())
                        + " | project_visual_form=" + definition.projectedVisualForm().map(ResourceLocation::toString).orElse("-")),
                false
        );
        return 1;
    }

    static int listCapabilityPolicies(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenCapabilityPolicyApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("capability_policies=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectCapabilityPolicy(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        CapabilityPolicyDefinition definition = DataDrivenCapabilityPolicyApi.findDefinition(definitionId)
                .map(DataDrivenCapabilityPolicyApi.LoadedCapabilityPolicyDefinition::definition)
                .orElseThrow(() -> UNKNOWN_CAPABILITY_POLICY.create(definitionId.toString()));
        source.sendSuccess(
                () -> Component.literal(definition.id()
                        + " | merge_mode=" + definition.mergeMode().name().toLowerCase(java.util.Locale.ROOT)
                        + " | priority=" + definition.priority()),
                false
        );
        source.sendSuccess(
                () -> Component.literal("inventory=[open=" + definition.inventory().canOpenInventory()
                        + " move=" + definition.inventory().canMoveItems()
                        + " hotbar=" + definition.inventory().canUseHotbar()
                        + " offhand=" + definition.inventory().canUseOffhand() + "]"
                        + " | equipment=[equip_armor=" + definition.equipment().canEquipArmor()
                        + " unequip_armor=" + definition.equipment().canUnequipArmor()
                        + " equip_held=" + definition.equipment().canEquipHeldItems() + "]"),
                false
        );
        source.sendSuccess(
                () -> Component.literal("movement=[sprint=" + definition.movement().canSprint()
                        + " sneak=" + definition.movement().canSneak()
                        + " jump=" + definition.movement().canJump()
                        + " fly=" + definition.movement().canFly() + "]"
                        + " | interaction=[blocks=" + definition.interaction().canInteractWithBlocks()
                        + " entities=" + definition.interaction().canInteractWithEntities()
                        + " attack_players=" + definition.interaction().canAttackPlayers()
                        + " attack_mobs=" + definition.interaction().canAttackMobs() + "]"),
                false
        );
        return 1;
    }

    static int listVisualForms(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenVisualFormApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("visual_forms=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectVisualForm(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        VisualFormDefinition definition = DataDrivenVisualFormApi.findDefinition(definitionId)
                .map(DataDrivenVisualFormApi.LoadedVisualFormDefinition::definition)
                .orElseThrow(() -> UNKNOWN_VISUAL_FORM.create(definitionId.toString()));
        source.sendSuccess(
                () -> Component.literal(definition.id()
                        + " | kind=" + definition.kind().name().toLowerCase(java.util.Locale.ROOT)
                        + " | first_person_policy=" + definition.firstPersonPolicy().name().toLowerCase(java.util.Locale.ROOT)
                        + " | render_scale=" + definition.renderScale()),
                false
        );
        source.sendSuccess(
                () -> Component.literal("model_profile=" + definition.modelProfileId().map(ResourceLocation::toString).orElse("-")
                        + " | cue_route_profile=" + definition.cueRouteProfileId().map(ResourceLocation::toString).orElse("-")
                        + " | hud_profile=" + definition.hudProfileId().map(ResourceLocation::toString).orElse("-")
                        + " | sound_profile=" + definition.soundProfileId().map(ResourceLocation::toString).orElse("-")),
                false
        );
        return 1;
    }

    static int listUpgradePointTypes(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenUpgradePointTypeApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("upgrade_point_types=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectUpgradePointType(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        UpgradePointType definition = DataDrivenUpgradePointTypeApi.findDefinition(definitionId)
                .map(DataDrivenUpgradePointTypeApi.LoadedUpgradePointType::definition)
                .orElseThrow(() -> UNKNOWN_UPGRADE_POINT_TYPE.create(definitionId.toString()));
        source.sendSuccess(() -> Component.literal(definition.id().toString()), false);
        return 1;
    }

    static int listUpgradeTracks(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenUpgradeTrackApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("upgrade_tracks=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectUpgradeTrack(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        UpgradeTrackDefinition definition = DataDrivenUpgradeTrackApi.findDefinition(definitionId)
                .map(DataDrivenUpgradeTrackApi.LoadedUpgradeTrackDefinition::definition)
                .orElseThrow(() -> UNKNOWN_UPGRADE_TRACK.create(definitionId.toString()));
        source.sendSuccess(
                () -> Component.literal(definition.id()
                        + " | family=" + definition.familyId().map(ResourceLocation::toString).orElse("-")
                        + " | group=" + definition.groupId().map(ResourceLocation::toString).orElse("-")
                        + " | page=" + definition.pageId().map(ResourceLocation::toString).orElse("-")
                        + " | tags=" + XLibCommandSupport.joinIds(definition.tags())),
                false
        );
        source.sendSuccess(
                () -> Component.literal("root_nodes=" + XLibCommandSupport.joinIds(definition.rootNodes())
                        + " | exclusive_tracks=" + XLibCommandSupport.joinIds(definition.exclusiveTracks())),
                false
        );
        return 1;
    }

    static int listUpgradeNodes(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenUpgradeNodeApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("upgrade_nodes=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectUpgradeNode(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        UpgradeNodeDefinition definition = DataDrivenUpgradeNodeApi.findDefinition(definitionId)
                .map(DataDrivenUpgradeNodeApi.LoadedUpgradeNodeDefinition::definition)
                .orElseThrow(() -> UNKNOWN_UPGRADE_NODE.create(definitionId.toString()));
        source.sendSuccess(
                () -> Component.literal(definition.id()
                        + " | track=" + (definition.trackId() == null ? "-" : definition.trackId())
                        + " | family=" + definition.familyId().map(ResourceLocation::toString).orElse("-")
                        + " | group=" + definition.groupId().map(ResourceLocation::toString).orElse("-")
                        + " | page=" + definition.pageId().map(ResourceLocation::toString).orElse("-")
                        + " | tags=" + XLibCommandSupport.joinIds(definition.tags())),
                false
        );
        source.sendSuccess(
                () -> Component.literal("choice_group=" + definition.choiceGroupId().map(ResourceLocation::toString).orElse("-")
                        + " | point_costs=" + formatIntMap(definition.pointCosts())
                        + " | required_nodes=" + XLibCommandSupport.joinIds(definition.requiredNodes())
                        + " | locked_nodes=" + XLibCommandSupport.joinIds(definition.lockedNodes())
                        + " | locked_tracks=" + XLibCommandSupport.joinIds(definition.lockedTracks())),
                false
        );
        source.sendSuccess(
                () -> Component.literal("requirements=" + describeUpgradeRequirements(definition.requirements())
                        + " | rewards=" + formatUpgradeRewards(definition)),
                false
        );
        return 1;
    }

    static int listUpgradeConsumeRules(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenUpgradeConsumeRuleApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("upgrade_consume_rules=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectUpgradeConsumeRule(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        UpgradeConsumeRule definition = DataDrivenUpgradeConsumeRuleApi.findDefinition(definitionId)
                .map(DataDrivenUpgradeConsumeRuleApi.LoadedUpgradeConsumeRule::definition)
                .orElseThrow(() -> UNKNOWN_UPGRADE_CONSUME_RULE.create(definitionId.toString()));
        source.sendSuccess(
                () -> Component.literal(definition.id()
                        + " | items=" + XLibCommandSupport.joinIds(definition.itemIds())
                        + " | item_tags=" + formatTagKeys(definition.itemTags())
                        + " | food_only=" + definition.foodOnly()
                        + " | conditions=" + definition.conditions().size()),
                false
        );
        source.sendSuccess(
                () -> Component.literal("point_rewards=" + formatIntMap(definition.pointRewards())
                        + " | counter_rewards=" + formatIntMap(definition.counterRewards())),
                false
        );
        return 1;
    }

    static int listUpgradeKillRules(CommandSourceStack source) {
        Collection<ResourceLocation> ids = DataDrivenUpgradeKillRuleApi.allDefinitionIds();
        source.sendSuccess(() -> Component.literal("upgrade_kill_rules=" + XLibCommandSupport.joinIds(ids)), false);
        return ids.size();
    }

    static int inspectUpgradeKillRule(CommandSourceStack source, ResourceLocation definitionId) throws CommandSyntaxException {
        UpgradeKillRule definition = DataDrivenUpgradeKillRuleApi.findDefinition(definitionId)
                .map(DataDrivenUpgradeKillRuleApi.LoadedUpgradeKillRule::definition)
                .orElseThrow(() -> UNKNOWN_UPGRADE_KILL_RULE.create(definitionId.toString()));
        source.sendSuccess(
                () -> Component.literal(definition.id()
                        + " | targets=" + XLibCommandSupport.joinIds(definition.targetEntityIds())
                        + " | target_tags=" + formatTagKeys(definition.targetEntityTags())
                        + " | required_ability=" + definition.requiredAbilityId().map(ResourceLocation::toString).orElse("-")),
                false
        );
        source.sendSuccess(
                () -> Component.literal("point_rewards=" + formatIntMap(definition.pointRewards())
                        + " | counter_rewards=" + formatIntMap(definition.counterRewards())),
                false
        );
        return 1;
    }

    private static String formatSnapshot(ContextGrantSnapshot snapshot) {
        List<String> parts = new ArrayList<>();
        if (!snapshot.abilities().isEmpty()) {
            parts.add("abilities=" + XLibCommandSupport.joinIds(snapshot.abilities()));
        }
        if (!snapshot.passives().isEmpty()) {
            parts.add("passives=" + XLibCommandSupport.joinIds(snapshot.passives()));
        }
        if (!snapshot.grantedItems().isEmpty()) {
            parts.add("granted_items=" + XLibCommandSupport.joinIds(snapshot.grantedItems()));
        }
        if (!snapshot.recipePermissions().isEmpty()) {
            parts.add("recipe_permissions=" + XLibCommandSupport.joinIds(snapshot.recipePermissions()));
        }
        if (!snapshot.blockedAbilities().isEmpty()) {
            parts.add("blocked_abilities=" + XLibCommandSupport.joinIds(snapshot.blockedAbilities()));
        }
        if (!snapshot.statePolicies().isEmpty()) {
            parts.add("state_policies=" + XLibCommandSupport.joinIds(snapshot.statePolicies()));
        }
        if (!snapshot.stateFlags().isEmpty()) {
            parts.add("state_flags=" + XLibCommandSupport.joinIds(snapshot.stateFlags()));
        }
        return parts.isEmpty() ? "-" : String.join(" | ", parts);
    }

    private static String formatGrantBundle(GrantBundleDefinition definition) {
        List<String> parts = new ArrayList<>();
        if (!definition.abilities().isEmpty()) {
            parts.add("abilities=" + XLibCommandSupport.joinIds(definition.abilities()));
        }
        if (!definition.passives().isEmpty()) {
            parts.add("passives=" + XLibCommandSupport.joinIds(definition.passives()));
        }
        if (!definition.grantedItems().isEmpty()) {
            parts.add("granted_items=" + XLibCommandSupport.joinIds(definition.grantedItems()));
        }
        if (!definition.recipePermissions().isEmpty()) {
            parts.add("recipe_permissions=" + XLibCommandSupport.joinIds(definition.recipePermissions()));
        }
        if (!definition.blockedAbilities().isEmpty()) {
            parts.add("blocked_abilities=" + XLibCommandSupport.joinIds(definition.blockedAbilities()));
        }
        if (!definition.statePolicies().isEmpty()) {
            parts.add("state_policies=" + XLibCommandSupport.joinIds(definition.statePolicies()));
        }
        if (!definition.stateFlags().isEmpty()) {
            parts.add("state_flags=" + XLibCommandSupport.joinIds(definition.stateFlags()));
        }
        return parts.isEmpty() ? "-" : String.join(" | ", parts);
    }

    private static String formatAbilityCosts(AbilityDefinition definition) {
        if (definition.resourceCosts().isEmpty()) {
            return "-";
        }
        return definition.resourceCosts().stream()
                .map(cost -> cost.resourceId() + "=" + cost.amount())
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private static String formatMatchers(DataDrivenEquipmentBindingApi.LoadedEquipmentBindingDefinition definition) {
        List<String> parts = new ArrayList<>();
        if (definition.hasGeneralMatchers()) {
            parts.add("match=" + definition.matchMode().name().toLowerCase(java.util.Locale.ROOT));
            if (!definition.itemIds().isEmpty()) {
                parts.add("items=" + XLibCommandSupport.joinIds(definition.itemIds()));
            }
            if (!definition.itemTagIds().isEmpty()) {
                parts.add("item_tags=" + joinPrefixedIds(definition.itemTagIds(), "#"));
            }
            parts.add("presence=" + definition.presenceModes().stream()
                    .map(mode -> mode.name().toLowerCase(java.util.Locale.ROOT))
                    .sorted()
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("-"));
        }
        if (definition.hasSlotMatchers()) {
            if (!definition.slotItems().isEmpty()) {
                parts.add("slot_items=" + joinSlotBindings(definition.slotItems(), ""));
            }
            if (!definition.slotTags().isEmpty()) {
                parts.add("slot_tags=" + joinSlotBindings(definition.slotTags(), "#"));
            }
        }
        return parts.isEmpty() ? "-" : String.join(" | ", parts);
    }

    private static String joinPrefixedIds(Collection<ResourceLocation> ids, String prefix) {
        return ids.stream()
                .map(ResourceLocation::toString)
                .sorted()
                .map(value -> prefix + value)
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private static String joinSlotBindings(
            Map<DataDrivenEquipmentBindingApi.EquipmentSlotBinding, ResourceLocation> bindings,
            String prefix
    ) {
        return bindings.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey().name().toLowerCase(java.util.Locale.ROOT) + "=" + prefix + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private static String formatPresenceModes(Collection<ArtifactPresenceMode> presenceModes) {
        return presenceModes.stream()
                .map(mode -> mode.name().toLowerCase(java.util.Locale.ROOT))
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private static String formatDoubleMap(Map<ResourceLocation, Double> values) {
        if (values.isEmpty()) {
            return "-";
        }
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private static String formatModeOverlays(ModeDefinition definition) {
        if (definition.overlaySlotAbilities().isEmpty()) {
            return "-";
        }
        return definition.overlaySlotAbilities().entrySet().stream()
                .sorted((left, right) -> {
                    int containerCompare = left.getKey().containerId().toString().compareTo(right.getKey().containerId().toString());
                    if (containerCompare != 0) {
                        return containerCompare;
                    }
                    int pageCompare = Integer.compare(left.getKey().pageIndex(), right.getKey().pageIndex());
                    if (pageCompare != 0) {
                        return pageCompare;
                    }
                    return Integer.compare(left.getKey().slotIndex(), right.getKey().slotIndex());
                })
                .map(entry -> entry.getKey().containerId() + ":" + entry.getKey().pageIndex() + ":" + entry.getKey().slotIndex()
                        + "=" + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private static String formatComboTarget(ComboChainDefinition definition) {
        if (definition.targetSlotReference() != null) {
            AbilitySlotReference slotReference = definition.targetSlotReference();
            return slotReference.containerId() + ":" + slotReference.pageIndex() + ":" + slotReference.slotIndex();
        }
        if (definition.transformTriggeredSlot()) {
            return "triggered_slot";
        }
        return "-";
    }

    private static String formatComboBranches(List<DataDrivenComboChainApi.LoadedComboChainBranch> branches) {
        if (branches.isEmpty()) {
            return "-";
        }
        return branches.stream()
                .map(branch -> branch.comboAbilityId() + " when " + branch.requirement().description().getString())
                .reduce((left, right) -> left + " | " + right)
                .orElse("-");
    }

    private static String describeRequirements(List<AbilityRequirement> requirements) {
        if (requirements.isEmpty()) {
            return AbilityRequirements.always().description().getString();
        }
        return AbilityRequirements.all(requirements).description().getString();
    }

    private static String describeUpgradeRequirements(List<UpgradeRequirement> requirements) {
        if (requirements.isEmpty()) {
            return "-";
        }
        return requirements.stream()
                .map(requirement -> requirement.description().getString())
                .filter(value -> !value.isBlank())
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private static String formatUpgradeRewards(UpgradeNodeDefinition definition) {
        List<String> parts = new ArrayList<>();
        if (!definition.rewards().abilities().isEmpty()) {
            parts.add("abilities=" + XLibCommandSupport.joinIds(definition.rewards().abilities()));
        }
        if (!definition.rewards().passives().isEmpty()) {
            parts.add("passives=" + XLibCommandSupport.joinIds(definition.rewards().passives()));
        }
        if (!definition.rewards().grantedItems().isEmpty()) {
            parts.add("granted_items=" + XLibCommandSupport.joinIds(definition.rewards().grantedItems()));
        }
        if (!definition.rewards().recipePermissions().isEmpty()) {
            parts.add("recipe_permissions=" + XLibCommandSupport.joinIds(definition.rewards().recipePermissions()));
        }
        if (!definition.rewards().identities().isEmpty()) {
            parts.add("identities=" + XLibCommandSupport.joinIds(definition.rewards().identities()));
        }
        return parts.isEmpty() ? "-" : String.join(" | ", parts);
    }

    private static String formatIntMap(Map<ResourceLocation, Integer> values) {
        if (values.isEmpty()) {
            return "-";
        }
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private static String formatTagKeys(Collection<? extends TagKey<?>> tags) {
        return tags.stream()
                .map(TagKey::location)
                .map(ResourceLocation::toString)
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private static java.util.stream.Stream<String> referenceTopicIds() {
        return java.util.stream.Stream.concat(
                AuthoredJsonReferenceDocs.surfaces().stream().map(AuthoredJsonReferenceDocs.ReferenceSurface::id),
                ProgressionJsonReferenceDocs.surfaces().stream().map(ProgressionJsonReferenceDocs.ReferenceSurface::id)
        );
    }
}

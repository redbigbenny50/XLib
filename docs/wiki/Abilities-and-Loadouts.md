# Abilities and Loadouts

This page covers the core combat API: registering abilities, spending resources, and deciding what appears on the player's combat bar.

## Ability Registration

Register an ability with `AbilityDefinition.builder(...)`.

```java
AbilityApi.registerAbility(AbilityDefinition.builder(ironBreakerId, AbilityIcon.ofTexture(iconId))
        .cooldownTicks(40)
        .action((player, data) -> AbilityUseResult.success(data))
        .build());
```

Common builder pieces:

- `cooldownTicks(...)`
- `maxCharges(...)`
- `toggleAbility()`
- `durationTicks(...)`
- `activateRequirement(...)`
- `ticker(...)`
- `ender(...)`

## Composable Requirements

`AbilityRequirements` now supports composition helpers so common gating logic can stay reusable instead of being rewritten as one-off predicates.

```java
AbilityRequirement combatReady = AbilityRequirements.all(
        AbilityRequirements.modeActive(stormFormId),
        AbilityRequirements.resourceAtLeast(focusGaugeId, 20)
);

AbilityApi.registerAbility(AbilityDefinition.builder(tempestDriveId, AbilityIcon.ofTexture(iconId))
        .activateRequirement(combatReady)
        .renderRequirement(AbilityRequirements.not(AbilityRequirements.recentlyHurtWithin(20)))
        .action((player, data) -> AbilityUseResult.success(data))
        .build());
```

Useful helpers:

- `AbilityRequirements.all(...)`
- `AbilityRequirements.any(...)`
- `AbilityRequirements.not(...)`
- `AbilityRequirements.detectorActive(...)`
- `AbilityRequirements.statePolicyActive(...)`
- `AbilityRequirements.stateFlagActive(...)`
- `AbilityRequirements.identityActive(...)`
- `AbilityRequirements.artifactActive(...)`
- `AbilityRequirements.artifactUnlocked(...)`

Those same `AbilityRequirement` objects can now also be reused in contextual grants and consume-rule authoring instead of maintaining separate copies of the same condition logic. `statePolicyActive(...)` is the bridge when an ability should depend on a named generalized control bundle instead of directly keying off one mode id, while `stateFlagActive(...)` is the lighter-weight path for addon-defined named state markers like `empowered`, `danger_window`, `ritual_active`, or identity/origin markers registered through `IdentityApi`.

## Authoring Metadata

Abilities can now carry neutral addon-defined metadata so your addon can organize its own catalog without XLib hardcoding genre or kit semantics.

```java
AbilityApi.registerAbility(AbilityDefinition.builder(ironBreakerId, AbilityIcon.ofTexture(iconId))
        .family(ResourceLocation.fromNamespaceAndPath("yourmod", "family/striker"))
        .group(ResourceLocation.fromNamespaceAndPath("yourmod", "group/core_attacks"))
        .page(ResourceLocation.fromNamespaceAndPath("yourmod", "page/beginner"))
        .tag(ResourceLocation.fromNamespaceAndPath("yourmod", "tag/melee"))
        .tag(ResourceLocation.fromNamespaceAndPath("yourmod", "tag/guard_break"))
        .cooldownTicks(40)
        .action((player, data) -> AbilityUseResult.success(data))
        .build());
```

Useful metadata helpers:

- `family(...)`
- `group(...)`
- `page(...)`
- `tag(...)`
- `tags(...)`
- `AbilityApi.abilitiesInFamily(...)`
- `AbilityApi.abilitiesInGroup(...)`
- `AbilityApi.abilitiesOnPage(...)`
- `AbilityApi.abilitiesWithTag(...)`

The built-in ability menu also searches these metadata ids, exposes dedicated page/group/family scope controls, and supports a metadata-aware catalog sort for larger libraries. The stock details panel now stays focused on slot and gameplay information by default; selected ability family/group/page/tag rows are still available when a custom presentation explicitly enables metadata details.

The same neutral `family/group/page/tag` model now also exists on `PassiveDefinition`, `UpgradeTrackDefinition`, and `UpgradeNodeDefinition`, so one addon vocabulary can span active abilities, passive kits, and progression content. Use `PassiveApi` and `UpgradeApi` metadata lookup helpers when you need that same organization outside the ability menu.

## Passives

Passives are long-lived granted effects that live alongside active abilities instead of occupying a combat slot. Use them for always-on bonuses, state hooks, conditional modifiers, or event-driven perks that should not look like a button on the bar.

`PassiveDefinition` now exposes lightweight introspection helpers so addon authors and maintainers can see what a passive actually wires up without reverse-reading every lambda.

Useful APIs:

- `PassiveDefinition.authoredHooks()`
- `PassiveDefinition.hasHook(...)`
- `PassiveDefinition.soundTriggers()`

Built-in command surfaces:

- `/xlib passives catalog`
- `/xlib passives describe <passive>`
- `/xlib passives inspect <player>`

Those commands report passive metadata, requirement descriptions, authored hooks, sound triggers, cooldown scaling, and the current grant/active status for a player's granted passives.

The built-in ability menu now also ships a player-facing passive browser panel. It shows currently granted passives, whether each passive is active or paused, and a focused description line for the selected passive without forcing players or addon authors to lean on `/xlib passives ...` just to answer basic "what do I currently have?" questions.

## High-Level Combat Actions

Use `AbilityActions` when you want a reusable combat helper instead of re-writing target lookup, miss handling, and hit attribution every time.

```java
AbilityApi.registerAbility(AbilityDefinition.builder(ironBreakerId, AbilityIcon.ofTexture(iconId))
        .cooldownTicks(20)
        .action(AbilityActions.meleeStrike(
                ironBreakerId,
                CombatTargetingProfile.melee(4.0D),
                10.0F
        ))
        .build());
```

Useful helpers:

- `AbilityActions.meleeStrike(...)`
- `AbilityActions.dashBehindAndStrike(...)`
- `AbilityActions.launchTarget(...)`
- `AbilityActions.counterStrike(...)`

Each helper uses XLib targeting and hit-resolution, so miss/dodge/block/parry outcomes still flow through `XLibCombatHitEvent`.

## First-Class Resource Costs

Resource costs are already native to `AbilityDefinition`.

```java
AbilityDefinition.builder(ironBreakerId, AbilityIcon.ofTexture(iconId))
        .resourceCost(nikoGaugeId, 25)
        .action((player, data) -> AbilityUseResult.success(data))
        .build();
```

XLib checks the balance before activation and deducts the cost atomically on success.

## Exact Fractional Resources

XLib resources now support exact fractional values for slow drains and gradual refills.

Useful APIs:

- `AbilityResourceApi.getExact(...)`
- `AbilityResourceApi.setExact(...)`
- `AbilityResourceApi.addExact(...)`
- `AbilityResourceBehaviors.decayExact(...)`
- `AbilityResourceBehaviors.refillOverTimeExactWhen(...)`

That means gauges can decay at rates like `0.02` or `0.05` per tick without addon authors having to hand-roll their own fixed-point storage.

## Resource Builder Aliases

The resource builder now includes shorter content-authoring aliases for the most common setup path:

```java
AbilityApi.registerResource(AbilityResourceDefinition.builder(nerveGaugeId)
        .min(0)
        .max(100)
        .startingValue(0)
        .decayPerTick(0.02D)
        .build());
```

These map onto the existing resource system:

- `min(0)` documents the implicit zero floor
- `max(...)` aliases `maxAmount(...)`
- `startingValue(...)` aliases `startingAmount(...)`
- `decayPerTick(...)` adds exact fractional decay behavior

## Charge-Release Abilities

For hold-to-charge and release-style abilities, use `chargeRelease(...)` instead of hand-writing a custom start/ticker/end trio each time.

```java
AbilityDefinition.builder(voidPalmId, AbilityIcon.ofTexture(iconId))
        .chargeRelease(40, (player, data, reason, chargedTicks, maxChargeTicks) -> {
            return AbilityUseResult.success(data);
        })
        .build();
```

This is the right fit when the ability starts charging on activation and resolves its real effect on release, toggle-off, or expiry.

XLib now also exposes the resolved charge progress directly:

- `AbilityDefinition.isChargeReleaseAbility()`
- `AbilityDefinition.chargeReleaseMaxTicks()`
- `AbilityDefinition.resolvedChargeReleaseTicks(data, reason)`

That makes it easier for addon-side HUD, animation, or effect bridges to map the current charge state without re-deriving it from the raw active-duration attachment data.

## Staged or Sequence Abilities

For authored multi-step actions, use `AbilitySequenceDefinition` and the `AbilityDefinition.sequence(...)` shorthand instead of manually stitching stage timers into one large ticker.

```java
AbilitySequenceDefinition sequence = AbilitySequenceDefinition.builder()
        .stage(AbilitySequenceStage.builder(6)
                .onEnter((player, data, state) -> data)
                .onTick((player, data, state) -> data)
                .build())
        .stage(AbilitySequenceStage.builder(10)
                .onEnter((player, data, state) -> data)
                .onComplete((player, data, state) -> data)
                .build())
        .build();

AbilityApi.registerAbility(AbilityDefinition.builder(ironBreakerId, AbilityIcon.ofTexture(iconId))
        .sequence(sequence)
        .build());
```

Useful pieces:

- `AbilitySequenceDefinition.builder()`
- `AbilitySequenceStage.builder(durationTicks)`
- `AbilitySequenceStage.onEnter(...)`
- `AbilitySequenceStage.onTick(...)`
- `AbilitySequenceStage.onComplete(...)`
- `AbilitySequenceStage.onEnd(...)`
- `AbilitySequenceState`
- `AbilityDefinition.sequence(...)`

## Runtime Cue Hooks

XLib now emits neutral runtime cues so presentation integrations can stay backend-agnostic instead of hard-wiring one animation stack into the core ability runtime.

Useful integration points:

- `XLibCueApi.registerSink(...)`
- `XLibRuntimeCueEvent`
- `XLibRuntimeCueType.ACTIVATION_START`
- `XLibRuntimeCueType.ACTIVATION_FAIL`
- `XLibRuntimeCueType.CHARGE_PROGRESS`
- `XLibRuntimeCueType.RELEASE`

Charge-release abilities automatically emit `CHARGE_PROGRESS` while active and `RELEASE` when they resolve. Normal activations emit `ACTIVATION_START` on success and `ACTIVATION_FAIL` when XLib rejects the use before it consumes the action.

## Ownership vs Loadout

These are different:

- ownership means the player has the ability
- loadout means the player has it equipped into an active primary-bar slot reference

Typical flow:

```java
AbilityGrantApi.grant(player, ironBreakerId, sourceId);
AbilityLoadoutApi.assign(player, 0, ironBreakerId);
```

The older integer-slot overloads still map to the built-in primary container, so existing addons do not need to migrate immediately when they only care about the default bar.

## Slot References

XLib no longer hardcodes all loadout plumbing directly against raw integers. The runtime uses `AbilitySlotReference`, but the shipped surface now only accepts primary combat-bar slots on page `0`.

Useful APIs:

- `AbilitySlotReference`
- `AbilityContainerState`
- `AbilitySlotContainerApi`

Assign into a specific slot reference:

```java
AbilityLoadoutApi.assign(player, AbilitySlotReference.primary(0), ironBreakerId);
```

The older integer-slot overloads still map to the built-in primary bar, so existing addons do not need to migrate immediately when they only care about the default `0-8` strip.

The built-in HUD and built-in ability menu now edit against validated primary-bar slot references, and auxiliary containers/pages are no longer supported.

## Primary Bar Layouts and Slot Metadata

XLib also lets addons keep the built-in HUD/menu while changing how the primary combat bar is arranged on screen.

Useful APIs:

- `AbilityContainerLayoutDefinition`
- `AbilityContainerLayoutApi`
- `AbilitySlotLayoutMode`
- `AbilitySlotLayoutAnchor`
- `AbilitySlotWidgetMetadata`
- `AbilitySlotWidgetRole`
- `AbilitySlotLayoutPlanner`
- `RegisterAbilityClientRenderersEvent.registerAbilityContainerLayout(...)`

This is the built-in composition layer for things like:

- a normal bottom strip
- a compact grid-based primary bar
- a radial/ring layout for the primary bar
- categorized slot groups with header labels
- per-slot labels, categories, role hints, input hints, source labels, and soft locks

If an addon wants the built-in ability menu and default HUD to stay in use, this is the right extension surface. If it wants a completely custom screen or HUD, it should still use the screen/hud replacement APIs.

## Base Loadout and Mode Presets

XLib supports:

- the normal base combat bar
- per-mode player-authored preset layers

Assign the base bar:

```java
AbilityLoadoutApi.assign(player, 0, ironBreakerId);
```

Assign a mode-specific preset:

```java
AbilityLoadoutApi.assign(player, 0, dragonFistId, superSaiyanId);
AbilityLoadoutApi.assign(player, 1, instantTransmissionId, superSaiyanId);
```

Pass `null` as the mode id when you want the base loadout target.

Mode overlays, combo overrides, payload activation, and runtime resolution now all operate on primary-bar slot references, so follow-ups and overlays stay aligned with the same built-in combat strip and per-mode preset data.

## Optional Loadout Management and Quick Switch

The core slot-assignment layer is still always there, but built-in loadout-management UI is now an addon-enabled feature instead of an always-on assumption.

Useful APIs:

- `AbilityLoadoutFeatureApi`
- `AbilityLoadoutFeature`
- `AbilityLoadoutFeatureDecision`
- `RegisterAbilityClientRenderersEvent.registerLoadoutQuickSwitchHandler(...)`

Enable built-in loadout management for a player state:

```java
AbilityLoadoutFeatureApi.registerFeature(yourSourceId, AbilityLoadoutFeature.managementOnly(
        (player, data) -> AbilityLoadoutFeatureDecision.managementOnly()
));
```

Advertise an optional quick-switch keybind too:

```java
AbilityLoadoutFeatureApi.registerFeature(yourSourceId, AbilityLoadoutFeature.managementAndQuickSwitch(
        (player, data) -> AbilityLoadoutFeatureDecision.managementAndQuickSwitch()
));
```

Then register the client-side quick-switch behavior:

```java
public void onRegisterAbilityClientRenderers(RegisterAbilityClientRenderersEvent event) {
    event.registerLoadoutQuickSwitchHandler(yourSourceId, minecraft -> {
        // Swap your addon's active loadout/profile here.
        return true;
    });
}
```

Important behavior notes:

- the built-in ability menu only shows its loadout-target control when some addon enables loadout management for that player
- the `Cycle Loadout` keybind only appears in Controls when an addon advertises quick-switch support during init, before key mappings are registered
- the quick-switch action itself is addon-owned; XLib does not yet ship a first-class named multi-loadout profile backend

The built-in combat bar visibility toggle is also a normal Controls entry now:

- `Toggle Combat Bar` defaults to `Left Alt`
- players can rebind it like any other client key mapping
- the stock bar sits flush against the bottom hotbar lane instead of floating higher above the screen edge

## Control Profiles

The primary combat bar can also own control profiles so addons can define how that surface is used without hard-wiring one global key shape.

Useful APIs:

- `AbilityControlProfile`
- `AbilityControlProfileApi`
- `AbilityControlTrigger`
- `AbilityControlAction`
- `AbilityControlKeyMappingDefinition`
- `RegisterAbilityClientRenderersEvent.registerControlKeyMapping(...)`
- `RegisterAbilityClientRenderersEvent.registerControlActionHandler(...)`

Example:

```java
AbilityControlProfileApi.registerProfile(AbilityControlProfile.builder(primaryMouseProfileId, Component.literal("Primary Mouse"))
        .priority(50)
        .bind(AbilityControlTrigger.mouseButton(3),
                AbilityControlAction.activateSlot(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID, 0))
        .bind(AbilityControlTrigger.keyMapping(specialStrikeId),
                AbilityControlAction.activateSlot(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID, 4))
        .build());
```

If an addon wants that second binding to be player-rebindable in the Controls screen, it registers the key mapping first:

```java
public void onRegisterAbilityClientRenderers(RegisterAbilityClientRenderersEvent event) {
    event.registerControlKeyMapping(AbilityControlKeyMappingDefinition.keyboard(
            specialStrikeId,
            "key.yourmod.special_strike",
            GLFW.GLFW_KEY_R
    ));
}
```

That keeps the framework neutral:

- addons can keep some control profiles fixed
- addons can expose some control paths as normal player-rebindable key mappings
- built-in or custom HUD surfaces can read readable slot hints from the active bindings

## How Resolution Works

At runtime, the final ability shown in a slot can come from several layers:

- base loadout
- active mode preset fallback
- authored mode overlays
- combo slot overrides

Those layers now resolve per `AbilitySlotReference.primary(...)` over the built-in combat bar. That lets addon authors hardcode critical bar slots while still leaving the rest of the active loadout player-editable.

## Built-In Ability Menu

The built-in ability menu is meant for real library-sized ability sets, not only tiny demos.

It supports:

- when loadout management is enabled, editing the base container or a mode-specific preset target
- registered strip/grid/radial/categorized primary-bar layouts with grouped slot placement
- search across ability ids plus family/group/page/tag metadata ids
- quick filters for visible, assignable-now, or already-slotted abilities
- sort modes for catalog metadata, name, slot, and cooldown
- dedicated page, group, and family scope controls for large ability libraries
- addon-defined palette and labeling profiles through `AbilityMenuPresentationApi`
- slot metadata details for the selected slot, including category, role hint, input hint, source ownership, and soft-lock state
- a built-in passive browser panel with granted-passive rows, active/paused state labels, row selection, and focused passive descriptions
- a details panel that focuses on currently failing requirements instead of dumping satisfied ones

## Presentation Profiles and HUD

XLib now exposes built-in presentation registries instead of leaving the menus and combat HUD fully hardcoded.

Useful APIs:

- `AbilityMenuPresentationApi`
- `ProgressionMenuPresentationApi`
- `CombatHudPresentationApi`
- `AbilityContainerLayoutDefinition`
- `AbilityContainerLayoutApi`
- `AbilitySlotWidgetMetadata`
- `AbilitySlotLayoutPlanner`
- `AbilityMenuScreenFactoryApi`
- `AbilityMenuScreenFactory`
- `AbilityMenuScreenContext`
- `AbilityMenuSessionStateApi`
- `ProgressionMenuScreenFactoryApi`
- `CombatHudRendererApi`
- `RegisterAbilityClientRenderersEvent.registerAbilityContainerLayout(...)`
- `AbilityResourceHudRegistry`
- `CombatBarPreferences`

Use `AbilityMenuPresentationApi` when you want the built-in ability menu to change list labeling or detail density without replacing the whole screen:

```java
AbilityMenuPresentationApi.register(AbilityMenuPresentation.builder(compactMenuId)
        .entryDetail(AbilityMenuPresentation.EntryDetail.GROUP)
        .showRequirementBreakdown(false)
        .build());

AbilityMenuPresentationApi.activate(compactMenuId);
```

Use `CombatHudPresentationApi` when you want the built-in combat bar/resource HUD to stay in use but render with different slot-label, overlay, or resource-label policies:

```java
CombatHudPresentationApi.register(CombatHudPresentation.builder(minimalHudId)
        .showActiveAbilityName(false)
        .showSlotNumbers(false)
        .resourceLabelMode(CombatHudPresentation.ResourceLabelMode.SHORT)
        .build());

CombatHudPresentationApi.activate(minimalHudId);
```

## Resource Bars

Use `AbilityResourceHudRegistry` and `AbilityResourceHudLayout` when you want the built-in resource bars to stay in use but sit in a different place or draw less text.

Useful APIs:

- `AbilityResourceHudRegistry.register(resourceId, layout)`
- `AbilityResourceHudRegistry.register(resourceId, renderer, layout)`
- `AbilityResourceHudLayout.builder()`
- `AbilityResourceHudAnchor`
- `AbilityResourceHudOrientation`

Example: keep the stock horizontal bar, move it relative to its anchor, and hide both the resource name and the numeric value for a cleaner bar-only look.

```java
AbilityResourceHudRegistry.register(focusId, AbilityResourceHudLayout.builder()
        .anchor(AbilityResourceHudAnchor.ABOVE_HOTBAR_CENTER)
        .width(120)
        .height(10)
        .offsetY(-6)
        .showName(false)
        .showValue(false)
        .build());
```

Example: park a vertical bar on the right side of the hotbar but keep the numeric value visible.

```java
AbilityResourceHudRegistry.register(focusId, AbilityResourceHudLayout.builder()
        .anchor(AbilityResourceHudAnchor.RIGHT_OF_HOTBAR)
        .orientation(AbilityResourceHudOrientation.VERTICAL)
        .width(18)
        .height(54)
        .showName(false)
        .showValue(true)
        .build());
```

Use the built-in layout when you want:

- top/side/hotbar-adjacent anchor control
- fine-tuning with `offsetX(...)` and `offsetY(...)`
- name/value visibility toggles without replacing the renderer
- width, height, spacing, and priority control for stacking multiple resources

Use `AbilityResourceHudRenderer` when you want a completely custom shape or texture instead of the built-in rectangular bar.

Use the screen/hud replacement APIs when you want fully custom geometry or interaction flow instead of the built-in layout/planner surfaces:

```java
AbilityMenuScreenFactoryApi.register(radialMenuId, RadialAbilityScreen::new);
ProgressionMenuScreenFactoryApi.register(radialProgressionId, RadialProgressionScreen::new);
CombatHudRendererApi.register(radialHudId, context -> {
    // Render your own circular/radial combat HUD here.
});

AbilityMenuScreenFactoryApi.activate(radialMenuId);
ProgressionMenuScreenFactoryApi.activate(radialProgressionId);
CombatHudRendererApi.activate(radialHudId);
```

That is the extension surface to use when an addon wants a round, radial, grid, or otherwise fully custom ability UI instead of the built-in XLib screen/HUD geometry.

When an addon wants to preserve selection state across custom screens, use the shared ability-menu session state too:

- `AbilityMenuSessionStateApi.state()`
- `AbilityMenuSessionStateApi.setState(...)`
- `AbilityMenuScreenContext.fromCurrentState(previousScreen)`

That lets a custom ability screen keep the current selected slot and current loadout-target mode instead of recreating that state separately every time the player swaps between addon-owned and built-in surfaces.

## Migration from Older Fixed Slots

Older addons or older player saves may still think in terms of one fixed `0-8` combat bar. XLib exposes an explicit migration layer for preserving or remapping that data through the current primary-bar slot-reference model.

Useful APIs:

- `AbilitySlotMigrationPlan`
- `AbilitySlotMigrationApi`

Use that layer when an addon changes from:

- raw integer slot indexes to explicit `AbilitySlotReference.primary(...)` values
- older fixed-slot assumptions to newer slot-reference-aware runtime code
- legacy migration plans that used to target auxiliary surfaces to the current primary-bar-only model

The built-in payload handlers now also validate slot references before applying activation or assignment requests, so stale clients and stale addon layouts fail safely instead of mutating the wrong slot.

## Ability Menu Access Policies

`AbilityMenuAccessApi` lets addons control whether the built-in ability menu is completely hidden, visible-but-locked, or fully interactive for a given player state.

```java
AbilityMenuAccessApi.registerPolicy(menuSourceId, AbilityMenuAccessPolicy.builder()
        .visibleWhen(AbilityRequirements.hasPassive(loadoutTrainingPassiveId))
        .availableWhen(AbilityRequirements.modeActive(awakenedModeId))
        .build());
```

Useful helpers:

- `AbilityMenuAccessApi.registerPolicy(...)`
- `AbilityMenuAccessApi.clearPolicy(...)`
- `AbilityMenuAccessPolicy.builder().visibleWhen(...)`
- `AbilityMenuAccessPolicy.builder().availableWhen(...)`
- `MenuAccessRequirements.predicate(...)`

Behavior notes:

- if a `visibleWhen(...)` requirement fails, the menu keybind does not open the screen at all
- if an `availableWhen(...)` requirement fails, the menu still opens for browsing/searching but assignment and clear-slot actions stay disabled
- `AbilityMenuAccessPolicy` can adapt existing `AbilityRequirement` objects directly, so menu access can reuse the same authored state checks you already use for abilities or grants

Use the menu when you want players to manage their own XLib loadouts instead of exposing a custom screen immediately, but remember that this management surface is now opt-in through `AbilityLoadoutFeatureApi`.

## Persistent Profiles and Required Choice Flow

XLib now also ships a neutral persistent-profile layer for addons that need long-lived committed selections on top of the grant, identity, and progression systems.

Useful APIs:

- `ProfileGroupDefinition`
- `ProfileDefinition`
- `ProfileApi`
- `ProfileOnboardingTrigger`
- `ProfileSelectionOrigin`
- `ProfileSelectionScreenFactoryApi`
- `ProfileSelectionScreenContext`

Register a required profile group:

```java
ProfileApi.registerGroup(ProfileGroupDefinition.builder(lineageGroupId)
        .displayName(Component.literal("Lineage"))
        .description(Component.literal("Choose one long-lived lineage package."))
        .selectionLimit(1)
        .requiredOnboarding()
        .onboardingTrigger(ProfileOnboardingTrigger.FIRST_LOGIN)
        .blockAbilityUseUntilSelected()
        .blockAbilityMenuUntilSelected()
        .playerCanReset()
        .reopenOnReset()
        .build());
```

Register a profile inside that group:

```java
ProfileApi.registerProfile(ProfileDefinition.builder(flameLineageId, lineageGroupId, AbilityIcon.ofTexture(iconId))
        .displayName(Component.literal("Flame Lineage"))
        .description(Component.literal("Projects a persistent package of starting content."))
        .grantIdentity(flameIdentityId)
        .grantBundle(flameBundleId)
        .grantAbility(signatureMoveId)
        .unlockArtifact(flameArtifactId)
        .unlockStartingNode(flameStarterNodeId)
        .build());
```

Profile definitions can project:

- identities
- grant bundles
- abilities, passives, and modes
- granted items and recipe permissions
- state flags
- unlocked artifacts
- progression starting nodes

Required groups can open the built-in selection screen automatically or route through an addon-defined replacement screen:

```java
RegisterAbilityClientRenderersEvent event = ...;
event.registerProfileSelectionScreen(radialProfileMenuId, RadialProfileSelectionScreen::new);
```

Behavior notes:

- required groups can trigger on first login, respawn, advancement progress, item use, or admin reopen flows
- groups can independently block ability use, the built-in ability menu, or progression until the selection is claimed
- selections track origin plus locked/admin/delegated/temporary state
- profile-owned starting nodes are tracked as managed unlock sources, so rebuilds or resets remove them cleanly instead of leaving orphaned progression state

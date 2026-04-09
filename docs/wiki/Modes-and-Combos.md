# Modes and Combos

This page covers stance/form systems, layered mode behavior, and combo follow-up windows.

## Modes / Forms / Stances

Modes are toggle abilities plus a `ModeDefinition`.

Use them for:

- exclusive transformations
- parent to child form upgrades
- temporary slot overlays
- bundles of granted abilities, passives, items, and recipes
- bundled state-policy projection for overdrive, silence, suppression, or similar control layers
- bundled state-flag projection for addon-defined named states like empowered windows or ritual channels
- cooldown scaling
- player-authored mode-specific loadout presets

Example:

```java
ModeApi.registerMode(ModeDefinition.builder(superSaiyanId)
        .stackable()
        .exclusiveWith(baseSaiyanModeId)
        .transformsFrom(baseSaiyanModeId)
        .cooldownTickRateMultiplier(2.0D)
        .overlayAbility(0, dragonFistId)
        .grantAbility(instantTransmissionId)
        .build());
```

If an ability should only activate while a mode is active:

```java
.activateRequirement(AbilityRequirements.modeActive(superSaiyanId))
```

## Combined Mode + Toggle Registration

If the mode and its toggle ability are really one feature, register them together:

```java
ModeApi.registerModeAbility(ModeAbilityDefinition.builder(flameKataId, AbilityIcon.ofTexture(iconId))
        .cooldownTicks(40)
        .durationTicks(120)
        .cooldownPolicy(AbilityCooldownPolicy.ON_END)
        .exclusiveWith(waterKataId)
        .overlayAbility(0, ironBreakerId)
        .cooldownTickRateMultiplier(1.5D)
        .action((player, data) -> AbilityUseResult.success(data))
        .build());
```

This removes the easy mistake where an addon author accidentally invents separate ids for the toggle ability and the mode state.

## State Policies

The broader state-control layer now lives alongside modes instead of replacing them.

Use `StatePolicyDefinition` plus `StatePolicyApi` when you want one reusable control bundle that can be projected by a mode, a contextual provider, or another source-tracked system.

```java
ResourceLocation overdrivePolicyId = ResourceLocation.fromNamespaceAndPath("yourmod", "state/overdrive");

StatePolicyApi.registerStatePolicy(StatePolicyDefinition.builder(overdrivePolicyId)
        .cooldownTickRateMultiplier(1.5D)
        .seal(AbilitySelector.tag(ResourceLocation.fromNamespaceAndPath("yourmod", "tag/guarded")))
        .suppress(AbilitySelector.tag(ResourceLocation.fromNamespaceAndPath("yourmod", "tag/finisher")))
        .build());

ModeApi.registerMode(ModeDefinition.builder(overdriveModeId)
        .statePolicy(overdrivePolicyId)
        .build());
```

Useful authoring helpers:

- `AbilitySelector.ability(...)`
- `AbilitySelector.family(...)`
- `AbilitySelector.group(...)`
- `AbilitySelector.page(...)`
- `AbilitySelector.tag(...)`
- `AbilitySelector.allAbilities()`
- `StatePolicyDefinition.lock(...)`
- `StatePolicyDefinition.silence(...)`
- `StatePolicyDefinition.suppress(...)`
- `StatePolicyDefinition.seal(...)`
- `ModeDefinition.statePolicy(...)`
- `SimpleContextGrantProvider.Builder.grantStatePolicy(...)`

If an ability should only work while a named policy is active, gate it with `AbilityRequirements.statePolicyActive(...)`.

## State Flags

Use `StateFlagApi` when you want a named source-tracked boolean state without attaching built-in control semantics like silence or suppression.

```java
ResourceLocation empoweredFlagId = ResourceLocation.fromNamespaceAndPath("yourmod", "state/empowered");

StateFlagApi.registerStateFlag(empoweredFlagId);

ModeApi.registerMode(ModeDefinition.builder(limitBreakModeId)
        .stateFlag(empoweredFlagId)
        .build());
```

Useful authoring helpers:

- `StateFlagApi.registerStateFlag(...)`
- `ModeDefinition.stateFlag(...)`
- `ModeDefinition.stateFlags(...)`
- `SimpleContextGrantProvider.Builder.grantStateFlag(...)`
- `SimpleContextGrantProvider.Builder.grantStateFlags(...)`
- `AbilityRequirements.stateFlagActive(...)`

Use this layer for addon-defined runtime markers like `empowered`, `danger_window`, `ritual_active`, or lineage/archetype markers that should be source-tracked and inspectable but do not need the stronger selector-based lock/silence/suppression behavior of a full state policy.

## Reactive Detectors and Triggers

Use detectors when an addon-defined runtime event should open a temporary authored window, and use reactive triggers when a later event should consume that window and apply a response.

```java
AbilityDetectorApi.registerDetector(AbilityDetectorDefinition.builder(counterWindowId, 20)
        .event(ReactiveEventType.HURT)
        .build());

ReactiveTriggerApi.registerTrigger(ReactiveTriggerDefinition.builder(counterTriggerId)
        .event(ReactiveEventType.ABILITY_ACTIVATE)
        .requireDetector(counterWindowId)
        .consumeRequiredDetectors()
        .action((player, data, event) -> data)
        .build());
```

Useful APIs:

- `AbilityDetectorApi.registerDetector(...)`
- `ReactiveTriggerApi.registerTrigger(...)`
- `ReactiveRuntimeEvent`
- `AbilityRequirements.detectorActive(...)`

Built-in runtime dispatch now covers ability activate/fail/end, hit confirm, hurt, kill, jump, block break, armor change, and item consumed events, so reactive kits can be authored against one neutral event vocabulary instead of one-off hooks.

## Mode Metadata

Modes now share the same neutral catalog metadata model used by abilities, passives, and progression content.

```java
ModeApi.registerMode(ModeDefinition.builder(superSaiyanId)
        .family(ResourceLocation.fromNamespaceAndPath("yourmod", "family/forms"))
        .group(ResourceLocation.fromNamespaceAndPath("yourmod", "group/transformations"))
        .page(ResourceLocation.fromNamespaceAndPath("yourmod", "page/awakenings"))
        .tag(ResourceLocation.fromNamespaceAndPath("yourmod", "tag/stateful"))
        .stackable()
        .build());
```

Useful lookup helpers:

- `ModeApi.modesInFamily(...)`
- `ModeApi.modesInGroup(...)`
- `ModeApi.modesOnPage(...)`
- `ModeApi.modesWithTag(...)`

If you use `ModeAbilityDefinition.builder(...)`, the metadata helpers now flow into both the wrapped `AbilityDefinition` and the wrapped `ModeDefinition`, so one call site keeps the toggle ability and the mode catalog entry aligned.

## Mode Id Guardrail

XLib mode ids are ability ids.

That means:

- `ModeDefinition.builder(flameKataId)` expects the same id as the toggle ability
- `AbilityRequirements.modeActive(flameKataId)` also points at that same ability id
- active-duration, upkeep, overlays, combo checks, and mode-end events all use that same id

If you prefer not to manage that relationship manually, use `ModeAbilityDefinition` and `ModeApi.registerModeAbility(...)`.

## Stackable Modes

`stackable()` makes the design intent explicit: this mode is meant to layer on top of other active modes instead of acting like a mutually exclusive stance by omission alone.

## Cycle Groups

Use cycle groups when a rotation should track which stances have already been used and block repeating one until something resets the cycle.

```java
ResourceLocation kataCycleId = ResourceLocation.fromNamespaceAndPath("yourmod", "kata_cycle");

ModeApi.registerMode(ModeDefinition.builder(flameKataId)
        .cycleGroup(kataCycleId)
        .build());

ModeApi.registerMode(ModeDefinition.builder(resetBreathId)
        .resetCycleGroupOnActivate(kataCycleId)
        .build());
```

You can also reset a cycle directly:

```java
ModeApi.resetCycleGroup(player, kataCycleId);
```

## Ordered Cycle Steps

If the rotation must happen in a strict order instead of "any unused stance once per cycle", declare an ordered step:

```java
ModeApi.registerMode(ModeDefinition.builder(tigerId)
        .orderedCycle(kataCycleId, 1)
        .build());

ModeApi.registerMode(ModeDefinition.builder(craneId)
        .orderedCycle(kataCycleId, 2)
        .build());
```

That blocks activating `craneId` until the step-1 mode has already been used in the current cycle.

## Cooldown Scaling

Modes and passives can both scale cooldown and charge recovery speed.

```java
ModeApi.registerMode(ModeDefinition.builder(advanceId)
        .stackable()
        .cooldownTickRateMultiplier(2.0D)
        .build());
```

Those multipliers combine multiplicatively while the relevant mode or passive is active.

## Mode Upkeep

Modes can also apply built-in upkeep each tick:

```java
ModeApi.registerMode(ModeDefinition.builder(removalId)
        .stackable()
        .healthCostPerTick(1.0D)
        .minimumHealth(2.0D)
        .resourceDeltaPerTick(nerveGaugeId, 0.25D)
        .build());
```

This is useful for hp-burn transformations, maintenance costs, or regenerating/draining a resource while the mode stays active.

## Runtime State Inspection

Use `/xlib debug state <player>` when you need a focused view of the player's currently active modes, state policies, state flags, and granted passives without the noisier full debug dump.

That command reports:

- active modes with duration, metadata, cycle-group info, overlays, upkeep, grant bundles, and block bundles
- active detectors with remaining ticks and authored event types
- active and unlocked artifacts with their bundle projection rules and unlock sources
- active state policies with resolved locked/silenced/suppressed ability sets plus source ownership
- active state flags with source ownership
- granted passives with metadata, requirement status, hooks, sound triggers, cooldown scaling, and sources

`/xlib debug export <player>` now also writes structured `detector_states`, `artifact_states`, `active_mode_states`, `state_policy_states`, `state_flag_states`, and `passive_states` sections into the exported JSON snapshot.

## Mode End Reasons

`XLibModeEvent.Ended` now includes specific reasons so addon code can distinguish a natural expiry from an early player toggle or forced replacement.

Useful reasons include:

- `PLAYER_TOGGLED`
- `DURATION_EXPIRED`
- `REQUIREMENT_INVALIDATED`
- `SUPPRESSED`
- `FORCE_ENDED`
- `REPLACED_BY_TRANSFORM`
- `REPLACED_BY_EXCLUSIVE`

## Runtime Cue Hooks

XLib now emits neutral runtime cues alongside its normal mode and combo lifecycle so addon-defined animation, VFX, or sound bridges can stay backend-agnostic.

Useful cues in this area:

- `STATE_ENTER` when a toggle mode starts
- `STATE_EXIT` when a mode ends, including the `AbilityEndReason`
- `HIT_CONFIRM` when addon code records a landed hit through `AbilityCombatTracker`
- `RELEASE` when a charge-release ability ends

Consume them through `XLibCueApi.registerSink(...)`, by listening for `XLibRuntimeCueEvent` on the NeoForge event bus, or by registering surface-specific adapters with `XLibCueAdapterApi`.

XLib now treats these presentation targets as separate capability surfaces:

- `XLibCueSurface.PLAYER_BODY_ANIMATION`
- `XLibCueSurface.MODEL_ANIMATION`
- `XLibCueSurface.EFFECT_PLAYBACK`

Use `XLibCueRouteProfileApi` when one backend should only handle some surfaces or some cue families:

```java
XLibCueRouteProfileApi.registerProfile(XLibCueRouteProfile.builder(routeProfileId)
        .route(XLibRuntimeCueType.ACTIVATION_START,
                XLibCueSurface.PLAYER_BODY_ANIMATION,
                XLibCueSurface.EFFECT_PLAYBACK)
        .route(XLibRuntimeCueType.HIT_CONFIRM, XLibCueSurface.EFFECT_PLAYBACK)
        .build());

XLibCueRouteProfileApi.activate(routeProfileId);
```

## Combo Chains

Use combo chains when one ability should temporarily unlock or replace another.

```java
ComboChainApi.registerChain(ComboChainDefinition.builder(chainId, heavyStrikeId, meteorFinishId)
        .windowTicks(20)
        .transformTriggeredSlot()
        .build());
```

Gate the follow-up so it only works during the combo window:

```java
.activateRequirement(AbilityRequirements.comboWindowActive(meteorFinishId))
```

## Branching Combo Follow-Ups

Branching is already built in. A single trigger can choose different follow-up abilities depending on the current player state.

```java
ComboChainApi.registerChain(ComboChainDefinition.builder(chainId, haymakerId, strikerFollowupId)
        .windowTicks(20)
        .transformTriggeredSlot()
        .branch(demonsbaneId, (player, data) -> data.isModeActive(moonKataId))
        .build());
```

How it works:

- the default combo ability is the one passed into `builder(...)`
- branches are checked in the order you register them
- the first matching branch wins
- if no branch matches, XLib uses the default follow-up

That means the system understands these follow-ups as alternatives, not as multiple unrelated chains that happened to share a trigger.

## Trigger Timing

Combo chains are no longer limited to opening only on activation.

You can also open them on hit-confirm or when the parent ability ends:

```java
ComboChainApi.registerChain(ComboChainDefinition.builder(chainId, haymakerId, followupId)
        .triggerOnHit()
        .transformTriggeredSlot()
        .build());
```

Available trigger timing options:

- activation
- hit confirm
- end/release

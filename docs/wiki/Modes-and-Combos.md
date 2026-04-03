# Modes and Combos

This page covers stance/form systems, layered mode behavior, and combo follow-up windows.

## Modes / Forms / Stances

Modes are toggle abilities plus a `ModeDefinition`.

Use them for:

- exclusive transformations
- parent to child form upgrades
- temporary slot overlays
- bundles of granted abilities, passives, items, and recipes
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

## Mode End Reasons

`XLibModeEvent.Ended` now includes specific reasons so addon code can distinguish a natural expiry from an early player toggle or forced replacement.

Useful reasons include:

- `PLAYER_TOGGLED`
- `DURATION_EXPIRED`
- `REQUIREMENT_INVALIDATED`
- `FORCE_ENDED`
- `REPLACED_BY_TRANSFORM`
- `REPLACED_BY_EXCLUSIVE`

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

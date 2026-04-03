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

## Ownership vs Loadout

These are different:

- ownership means the player has the ability
- loadout means the player has it equipped in slot `1-9`

Typical flow:

```java
AbilityGrantApi.grant(player, ironBreakerId, sourceId);
AbilityLoadoutApi.assign(player, 0, ironBreakerId);
```

## Base Loadout and Mode Presets

XLib supports:

- the normal base combat bar
- per-mode player-authored loadout presets

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

## How Resolution Works

At runtime, the final ability shown in a slot can come from several layers:

- base loadout
- active mode preset fallback
- authored mode overlays
- combo slot overrides

That lets addon authors hardcode critical slots while still leaving the rest of the bar player-editable.

## Built-In Ability Menu

The built-in ability menu is meant for real library-sized ability sets, not only tiny demos.

It supports:

- editing the base bar or a mode-specific preset target
- search
- quick filters for visible, assignable-now, or already-slotted abilities
- sort modes for name, slot, and cooldown
- a details panel that focuses on currently failing requirements instead of dumping satisfied ones

Use the menu when you want players to manage their own XLib loadouts instead of exposing a custom screen immediately.

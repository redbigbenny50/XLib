# Tracked Values and Survival

This page covers XLib's tracked-value system: named per-player values with exact fractional storage, trigger-driven rule updates, requirement hooks, HUD integration, and optional survival or food-bar replacement behavior.

Use tracked values when your addon needs a general stat layer such as:

- evolution
- corruption
- biomass
- bloodlust
- infection
- custom hunger or survival replacements

Tracked values are not tied to one gameplay loop. They exist so addon authors do not have to hand-roll attachments, sync, commands, and rule dispatch for every custom stat.

## Core Pieces

- `TrackedValueDefinition`
  Defines one stat: id, min/max, starting value, drift, HUD metadata, and optional food-replacement settings.
- `TrackedValueApi`
  Registry plus per-player read/write helpers, sync, drift ticking, and food-replacement behavior.
- `TrackedValueData`
  Player attachment that stores current value state and active food-replacement ownership.
- `TrackedValueRuleDefinition`
  Authored trigger -> condition -> action rule definition.
- `DataDrivenTrackedValueApi`
  Datapack authoring for tracked values under `data/<namespace>/xlib/tracked_values/*.json`.
- `DataDrivenTrackedValueRuleApi`
  Datapack authoring and runtime dispatch for rules under `data/<namespace>/xlib/tracked_value_rules/*.json`.

## Mental Model

The normal pipeline is:

1. define one or more tracked values
2. author rules that react to runtime events
3. let those rules mutate the value, toggle survival replacement, or project other state
4. gate abilities, modes, menus, or progression with requirements against that value

That makes tracked values the general-purpose "custom stat" layer for XLib.

## What A Tracked Value Can Do

A tracked value can currently:

- clamp to authored min/max bounds
- start at an authored initial value
- drift each tick through authored decay or regen style deltas
- render through the built-in tracked-value HUD path
- replace the vanilla food bar when it has positive replacement priority
- convert eaten food into tracked-value gain
- drive custom heal and starvation thresholds while vanilla hunger is suppressed

## Rule Triggers

Tracked-value rules currently support these trigger families:

- `tick`
- `damage_dealt`
- `damage_taken`
- `kill`
- `jump`
- `armor_changed`
- `item_used`
- `item_consumed`
- `block_broken`
- `advancement_earned`
- `advancement_progress`

Rules are ordered by explicit `priority`, then id.

## Rule Conditions and Selectors

Rules can reuse XLib requirement logic instead of inventing a second condition language. They can also filter by authored selector fields such as:

- target entity id
- target entity tag
- damage type id
- damage type tag
- armor slot
- advancement id

Target entity and target tag matching now use merged real-plus-synthetic classification state, so one rule can react to either a mob's real type or a synthetic "counts as" identity applied through `EntityClassificationApi`.

## Rule Actions

Tracked-value rules can now do more than change numbers. Current authored actions include:

- `clear_values`
- `set_values`
- `value_deltas`
- `multiply_values`
- `min_values`
- `max_values`
- enable or disable food replacement
- grant or revoke state flags
- grant or revoke state policies
- grant or revoke capability policies
- grant or revoke damage modifier profiles
- grant or revoke synthetic entity types
- grant or revoke synthetic tags
- clear synthetic classification state for a configured classification source

That makes rules a general runtime-authoring surface instead of just a stat increment table.

## Survival / Food Replacement

Tracked values can replace the vanilla food bar when configured. The current replacement system supports:

- selecting a tracked value through positive `food_replacement_priority`
- hiding the vanilla food layer client-side
- rendering the replacement value in the food-bar lane
- suppressing vanilla hunger or exhaustion underneath
- converting consumed food nutrition into tracked-value gain through `food_replacement_intake_scale`
- custom heal thresholds and intervals
- custom starvation thresholds, intervals, and damage

This is the right layer for addons that want "replace hunger with X" without building separate HUD, tick, and sync systems.

## Requirements

Tracked values integrate with XLib requirements, so they can gate:

- abilities
- passives
- modes
- progression
- menus
- other rule conditions

`food_at_least(...)` and `food_at_most(...)` also respect replacement-food state when food replacement is active.

## Commands and Debugging

Useful inspection surfaces:

- `/xlib debug content tracked_values list`
- `/xlib debug content tracked_values inspect <id>`
- `/xlib debug content tracked_value_rules list`
- `/xlib debug content tracked_value_rules inspect <id>`
- `/xlib debug state <player>`

`/xlib debug state` includes active food replacement plus current per-value tracked-value state.

## Datapack Authoring

Tracked values and tracked-value rules are both bounded JSON surfaces:

- `data/<namespace>/xlib/tracked_values/*.json`
- `data/<namespace>/xlib/tracked_value_rules/*.json`

Use [Declarative JSON Reference](Declarative-JSON-Reference.md) for the full field tables.

## When To Use Tracked Values

Use tracked values when the mechanic is "player has a named stat that changes over time or from gameplay events."

Good fits:

- evolution from many event sources
- curse buildup
- custom hunger replacements
- kill or hit momentum
- transformation fuel
- survival resources that are not vanilla hunger

Use XLib combat resources instead when the value is tightly tied to ability runtime and should live on the combat resource bar model directly.

## Current Limits

- The built-in survival replacement path is strongest for food-bar replacement. It is not yet a fully arbitrary survival-HUD layout system.
- External mods do not automatically understand tracked values unless they integrate with XLib or your addon bridges them.

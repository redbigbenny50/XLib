# XLib

XLib is a NeoForge `1.21.1` framework mod for combat, progression, and source-tracked unlock systems.

It is architected as a framework mod for addon/content mods, not a standalone gameplay-content pack.

## What It Provides

XLib ships reusable APIs and runtime systems for:

- active combat abilities
- addon-defined catalog metadata for abilities, passives, modes, granted items, restricted recipe content, upgrade tracks, and upgrade nodes
- a primary combat bar, player-authored per-mode presets, and optional addon-enabled loadout-management surfaces
- modes / forms / stances
- combo follow-up windows and branching combo flows
- passives
- custom resource pools and first-class resource costs
- composable requirement helpers and adapters across ability, contextual-grant, item-grant, and consume-rule authoring
- cooldowns, charges, charge-release abilities, and cooldown scaling
- authored control profiles, addon-owned key mappings, and source-specific input routing
- source-tracked ability / passive / item / recipe grants
- passive catalog/definition inspection plus focused passive/mode state debug summaries
- managed granted items
- recipe permission gates and selector-based recipe restrictions
- optional progression / upgrade trees
- persistent profile groups, required-choice onboarding, and reset-aware selection state
- source-tracked capability policies for restricting player interaction, inventory, movement, menu, crafting, and equipment access
- named entity bindings between two living entities with typed semantics, stacking control, timed duration, and break conditions
- lifecycle stages for players with authored timer/trigger transitions and projected state flags, bundles, identities, visual forms, and capability policies
- visual form definitions for model/cue/HUD adapter registration with primary-form resolution and source-tracked application
- body transition definitions for possession, projection, hatching, emergence, and return semantics with temporary capability and form overrides
- built-in or replaceable client UI for combat, progression, onboarding, and custom HUD layouts
- admin/debug commands
- optional JEI / EMI recipe-viewer integration

## Major Systems

- `AbilityDefinition` / `AbilityRuntime`
  Active ability registration, addon-defined catalog metadata, activation, ticking, cooldowns, charges, charge-release helpers, and end handling.
- `PassiveDefinition` / `PassiveApi`
  Passive registration, cooldown scaling hooks, event-driven passive behavior, authored-hook introspection, and shared metadata lookup helpers.
- `ModeDefinition` / `ModeApi`
  Stances/forms with overlays, explicit stackable modes, cycle groups, reset triggers, cooldown scaling, and shared metadata lookup helpers.
- `ComboChainDefinition` / `ComboChainApi`
  Temporary follow-up windows, slot overrides, and branching combo follow-ups.
- `GrantedItemDefinition` / `GrantedItemApi`
  Managed-item registration with optional catalog metadata and lookup helpers.
- `GrantedItemRuntime`
  Managed items with revoke cleanup, undroppable state, and external-storage policies.
- `RecipePermissionApi`
  Exact and selector-based recipe locks, shared metadata lookups, advancement-backed unlocks, viewer sync, and runtime restrict/unrestrict support.
- `UpgradeApi`
  Optional progression tracks/nodes with shared metadata, point types, consume/kill rules, and reward projection into XLib's grant systems.
- `CapabilityPolicyApi`
  Source-tracked player capability restriction policies controlling interaction, inventory, movement, menu, crafting, equipment, and held-item access. Resolved lazily into a shared `ResolvedCapabilityPolicyState`.
- `EntityBindingApi`
  Named, typed bindings between two living entities with stacking control (single/replace/stack), symmetry options, timed duration, break conditions (death, disconnect, range), and a runtime UUID cache for O(1) lookup.
- `LifecycleStageApi`
  Player lifecycle stages with authored timer/trigger/manual/death/respawn transitions, projected state flags, grant bundles, identities, capability policies, and visual forms during stage residence.
- `VisualFormApi`
  Source-tracked player visual form registry for model/cue/HUD adapter backends, multi-form stacking with primary-form resolution, and sanitization on login.
- `BodyTransitionApi`
  Authored possession, projection, hatching, emergence, and return transitions with temporary capability policy and visual form overrides, origin-body preservation policy, and reversible/irreversible semantics.
- `AbilityRequirements` / `GrantConditions` / `ContextGrantConditions`
  Shared authoring helpers for composable requirements and cross-surface reuse in abilities, contextual grants, item-driven grants, and consume rules. Includes capability-policy, lifecycle-stage, visual-form, entity-binding, and body-transition condition helpers.

## Included Client Features

- combat bar / loadout UI with a remappable combat-bar toggle keybind
- player-authored mode-specific presets
- built-in passive browser inside the ability menu
- icon-driven progression menu with list and tree views
- configurable resource HUD rendering

## Shipped Highlights

- Addon-defined metadata now spans abilities, passives, modes, granted items, restricted recipe content, upgrade tracks, and upgrade nodes, and the built-in menus can search and scope that data directly.
- The runtime now ships generalized state policies, source-tracked state flags, detector-driven reactive windows, and staged sequence abilities for richer combat/state authoring without one-off storage layers.
- XLib now includes persistent profile groups, required onboarding selection flows, identity and grant-bundle projection, artifact unlocks, and profile-backed progression starting nodes.
- Addons can keep the built-in HUD and menus while authoring primary-bar layouts, control profiles, player-rebindable key mappings, optional quick-switch behavior, and per-resource HUD placement rules.
- The built-in client layer now supports replaceable ability/progression/profile screens, replaceable combat HUD renderers, and shared session-state handoff for custom navigation.
- The built-in menus now include a passive browser in the ability screen plus progression-node reward descriptions, and progression presentations can tune tree node spacing, label width, and wrap depth for larger skill names.
- Progression now includes list/tree built-in presentations, choice-group specialization, explicit node/track locks, identity rewards, and identity-gated follow-up paths.

## Commands

XLib includes `/xlib` admin/debug surfaces for:

- abilities
- passives, including catalog/describe/inspect
- items
- recipes
- progression
- profiles
- capability policies
- entity bindings
- lifecycle stages
- visual forms
- body transitions
- debug / state / export / diff / counters

## Documentation

- `README.md`
  Top-level project summary.
- `docs/XLIB_USAGE_GUIDE.md`
  Wiki home for addon authors and release-facing documentation.
- `docs/wiki/`
  Topic pages for getting started, whole-library status, combat systems, unlock systems, progression, and testing.
- `docs/CODEBASE_MAP.md`
  Internal subsystem map for contributors and future maintenance.
- `tools/Sync-GitHubWiki.ps1`
  Maintainer helper that exports `docs/` into the separate GitHub wiki layout and can push it once the GitHub wiki has been bootstrapped.

## Development

- `.\gradlew.bat compileJava` compiles the mod sources.
- `.\gradlew.bat test` runs the JUnit suite.
- `.\gradlew.bat runGameTestServer` launches the NeoForge GameTest server and exits after registered tests complete.

## Coordinates

- Mod ID: `xlib`
- Display name: `XLib`
- Maven group: `com.whatxe.xlib`

## License

XLib is released as `All Rights Reserved`.

No permission is granted to copy, modify, redistribute, sublicense, or create derivative works from this repository or its compiled artifacts without prior written permission from the copyright holder. See [LICENSE](LICENSE).

## Notes

The project targets Java 21 and NeoForge `21.1.217` for Minecraft `1.21.1`.

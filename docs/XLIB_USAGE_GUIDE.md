# XLib Wiki

This document is now the release-facing wiki home for XLib.

Use [README.md](../README.md) for the short repository overview. Use [CODEBASE_MAP.md](CODEBASE_MAP.md) when you need the file-level subsystem map. Use the pages below for addon-author documentation. Local maintainer-only artifacts are intentionally not part of the published library docs.

Runtime `logs/` output from local dev/game runs is also intentionally excluded from the published repo.

Maintainers can mirror this docs set into the separate GitHub wiki repository with `tools/Sync-GitHubWiki.ps1` once the GitHub wiki has been bootstrapped with its first page in the web UI.

XLib's repository license is `All Rights Reserved`. This documentation describes the API surface and runtime behavior, but it does not grant reuse or redistribution rights.

## Start Here

1. [Getting Started](wiki/Getting-Started.md)
2. [System Overview and Status](wiki/System-Overview-and-Status.md)
3. [Abilities and Loadouts](wiki/Abilities-and-Loadouts.md)
4. [Modes and Combos](wiki/Modes-and-Combos.md)
5. [Grants, Items, and Recipes](wiki/Grants-Items-and-Recipes.md)
6. [Progression](wiki/Progression.md)
7. [Entity and Form Systems](wiki/Entity-and-Form-Systems.md)
8. [Events, Commands, and Testing](wiki/Events-Commands-and-Testing.md)

## What XLib Covers

XLib is a library mod for:

- active combat abilities with cooldowns, charges, resource costs, and slot assignment
- addon-defined catalog metadata for abilities, passives, modes, granted items, restricted recipe content, upgrade tracks, and upgrade nodes
- dynamic slot-container assignment, optional addon-enabled loadout-management surfaces, per-mode preset layers, and addon-authored control profiles for how active containers are used
- persistent profile groups, required-choice onboarding, server-authoritative selection claims, and reset-aware long-lived selection state
- modes, forms, and stances with overlays, exclusivity, transforms, ordered cycle groups, upkeep helpers, and cooldown scaling
- source-tracked state policies with selector-based lock, silence, suppression, seal-style control, and cooldown scaling that can be projected by modes or contextual systems
- generic source-tracked state flags for addon-defined runtime booleans that can also be projected by modes or contextual systems
- detector-driven reactive event windows, response rules, and staged multi-step ability authoring
- identity/origin definitions with inherited bundle projection for long-lived archetypes or lineage-style traits
- transferable grant bundles plus temporary delegated bundle grants revocable by the source
- authored artifact/equipment access policies with unlock-on-consume items, equipment-presence checks, and bundle projection for active or unlocked relic-style content
- ally-targeted support packages, entity relationship ownership, and controlled-entity command helpers
- combined mode+toggle registration for stance abilities that should share one id end-to-end
- combo windows and branching combo follow-ups, including hit-confirm and end-triggered follow-ups
- backend-agnostic runtime cue hooks for animation/effect adapters, including activation, charge, release, hit-confirm, interrupt, and state-transition cues
- custom combat marks/debuffs plus reusable targeting/hit-resolution helpers
- mutable player damage events, recent-hit reaction state, and high-level combat action helpers
- source-tracked ability, passive, item, and recipe grants
- managed granted items with storage policy enforcement
- exact/fractional resources, exact and selector-based recipe restrictions, and JEI/EMI integration
- an optional progression module for point, counter, track, and node systems
- branching progression specialization with choice groups, node or track locks, and identity-gated follow-up paths
- source-tracked capability policies for restricting player interaction, inventory, movement, menu, crafting, equipment, and held-item access
- named entity bindings between living entities with typed semantics, stacking policies, timed duration, and break conditions
- lifecycle stages for players with authored timer/trigger transitions and projected state flags, bundles, identities, forms, and policies
- visual form definitions for model/cue/HUD adapter backends with primary-form resolution and source tracking
- body transitions (possess, project, hatch, emerge, return) with temporary capability policy and visual form overrides

## Quick Mental Model

- Grants answer ownership: can the player use or receive something at all?
- Loadouts answer equipment and optional preset management: which abilities are currently bound to the active primary combat bar and per-mode presets, and should this addon expose extra preset-management or quick-switch affordances at all?
- Modes modify runtime state: overlays, bundles, transforms, stackable layers, or mode-specific presets.
- State policies modify control rules: temporary lock, silence, suppression, seal-style restrictions, and cooldown scaling that can come from modes or other sources.
- State flags modify author-owned state vocabulary: named source-tracked booleans for conditions like empowered, ritual_active, danger_window, or lineage markers.
- Identities and grant bundles answer provenance: which long-lived archetype, shared package, or delegated source is responsible for the current power set.
- Profiles answer committed long-lived selection: which authored profile packages are selected, which required groups are still pending, and whether onboarding should block ability or progression access.
- Combos open temporary follow-up windows and can branch to different follow-ups based on conditions.
- Progression is optional and projects rewards back into the same source-tracked grant systems instead of replacing them.

## Current Implementation Snapshot

XLib is already a broad framework mod, not just a starter slice.

- Server-authoritative attachments already back combat runtime, progression, combat marks, and reaction state.
- Source-tracked grants already cover abilities, passives, managed items, recipe permissions, contextual sources, mode bundles, state policies, state flags, and projected progression rewards.
- `GrantBundleApi`, `IdentityApi`, `DelegatedGrantApi`, and `GrantOwnershipApi` now add transferable power packages, inherited identity bundles, revocable delegations, and structured source descriptors for why a grant exists and when it should disappear.
- `SupportPackageApi`, `EntityRelationshipApi`, and `ControlledEntityApi` now add ally-targeted bundle projection, persistent owner or bond relationships, and simple summon or minion command-state helpers without forcing addon authors to build their own storage layer first.
- Shared gating logic can now be authored once as an `AbilityRequirement` and reused across ability definitions, contextual grants, item/unlock/consume conditions, and progression consume rules.
- `/xlib passives catalog|describe|inspect` and `/xlib debug state` now expose passive definitions plus active passive/mode state summaries without forcing maintainers to inspect raw attachment dumps.
- The generalized state layer now includes `StatePolicyDefinition` and `StatePolicyApi`, so modes and contextual sources can project shared cooldown multipliers plus selector-based lock, silence, and suppression rules without inventing special one-off mechanics.
- The generalized state layer now also includes `StateFlagApi`, so addons can project source-tracked named state markers through modes or contextual providers and gate behavior with `AbilityRequirements.stateFlagActive(...)` without overloading the harder-control state-policy path.
- Built-in client UI already covers the combat bar, resource HUD, the primary combat bar with layout metadata, a remappable combat-bar toggle keybind, optional addon-enabled loadout management, progression browsing or unlocking, metadata-aware ability catalog scoping by page/group/family, addon-defined menu/HUD presentation profiles, progression list/tree layouts with an icon-node skill-tree canvas for tree mode, menu access states that can hide a screen entirely or leave it visible-but-locked, first-class replacement hooks for custom ability/progression screens plus custom combat HUD renderers, shared menu session-state handoff for selected slot/progression state, addon-authored control profile hints, addon-owned loadout quick-switch handlers when a mod chooses to advertise that control, and per-resource HUD placement or label policies through `AbilityResourceHudLayout`.
- `AbilityContainerLayoutDefinition`, `AbilityContainerLayoutApi`, `AbilitySlotWidgetMetadata`, and `AbilitySlotLayoutPlanner` now let addons keep the built-in ability menu/HUD while authoring strip, grid, radial, or categorized layouts for the primary combat bar plus slot categories, role hints, soft locks, source labels, and input hints.
- `ProfileApi`, `ProfileGroupDefinition`, `ProfileDefinition`, and `ProfileSelectionData` now add first-class persistent profile state, required-choice lifecycle rules, reset history, and source-tracked projection into identities, bundles, artifacts, abilities, passives, recipes, state flags, and progression starting nodes.
- Built-in client UI now also includes a replaceable profile-selection screen through `ProfileSelectionScreenFactoryApi`, so required onboarding can stay on the built-in selection surface or switch to an addon-owned one.
- `AbilitySlotMigrationApi`, payload slot/container validation, `XLibMenuOpenEvent`, and the new profile selection/reset/onboarding events now provide safer addon upgrades plus explicit extension hooks around menu opening and server-authoritative profile transitions.
- The optional progression module now also supports branch commitment with choice groups, explicit node or track locks, identity rewards, and identity-gated follow-up nodes, while the built-in progression menu surfaces that branch metadata directly in the details panel.
- The core runtime now emits neutral `XLibRuntimeCue` objects plus `XLibRuntimeCueEvent`, so addon-side animation, VFX, or sound bridges can observe combat flow without XLib hard-depending on one presentation backend.
- `XLibCueAdapterApi` and `XLibCueRouteProfileApi` now let addons register optional backend adapters and route cues separately to player-body animation, model animation, or effect-playback capability surfaces.
- `/xlib` admin and debug commands, JUnit coverage, and runtime GameTests are already part of the repo.

Read [System Overview and Status](wiki/System-Overview-and-Status.md) when you want the full whole-mod map before diving into one subsystem page.

## Release Highlights

- Abilities, passives, modes, granted items, restricted recipe content, upgrade tracks, and upgrade nodes now support generic authoring metadata with `family(...)`, `group(...)`, `page(...)`, and `tag(...)`; the built-in ability menu searches ability metadata directly and the progression menu shows track/node metadata in its details panel.
- The built-in ability menu now has dedicated page/group/family scope controls plus a metadata-aware catalog sort for large libraries, and both built-in menus now support addon-authored hidden/locked/available access policies through `AbilityMenuAccessApi` and `ProgressionMenuAccessApi`. The stock details panels now stay focused on slot and unlock information by default instead of always dumping family/group/page/tag rows.
- XLib now ships addon-facing presentation registries through `AbilityMenuPresentationApi`, `ProgressionMenuPresentationApi`, and `CombatHudPresentationApi`, while the built-in progression menu can switch between list mode and an icon-node tree skill-tree view backed by `ProgressionLayoutPlanner` and `ProgressionTreeLayout`.
- The built-in progression menu now keeps its list/tree layout switcher on the top control row instead of leaving that control buried in the details area, and the tree view now reads as an actual icon-node skill tree with explicit connector pathing instead of an indented or tightly stacked button list.
- Progression presentations can now expose list-only, tree-only, or list-plus-tree built-in layouts through `availableLayoutModes(...)`; when a presentation exposes only one layout, the built-in switcher hides itself instead of showing a dead button.
- The built-in resource HUD now also supports per-resource anchors, fine-tuning offsets, and independent name/value visibility through `AbilityResourceHudLayout`, so addons can keep the stock bar renderer but place a bar where they want and suppress labels when they want a cleaner visual.
- XLib now also ships `AbilityMenuScreenFactoryApi`, `ProgressionMenuScreenFactoryApi`, and `CombatHudRendererApi`, so addons can replace the built-in ability/progression screens and the combat HUD renderer outright when they want radial, grid, or otherwise non-default UI geometry.
- XLib now also ships `AbilityMenuScreenContext`, `ProgressionMenuScreenContext`, `AbilityMenuSessionStateApi`, and `ProgressionMenuSessionStateApi`, so custom screens can preserve slot selection, loadout-target mode, selected track/node, and layout mode while handing players between built-in or addon-owned surfaces.
- XLib now also ships `AbilitySlotReference`, `AbilityContainerState`, `AbilitySlotContainerApi`, `AbilityControlProfileApi`, `AbilityControlKeyMappingApi`, and `AbilityControlActionHandlerApi`, so addons can target validated primary-bar slot references, keep per-mode preset data aligned with runtime lookup, and author control maps without rewriting runtime, payload, HUD, and built-in menu plumbing from scratch.
- XLib now also ships `AbilityLoadoutFeatureApi`, so addons can make built-in loadout-management surfaces optional and advertise an optional `Cycle Loadout` keybind only when their addon actually wants that system exposed.
- XLib now also ships `AbilityContainerLayoutDefinition`, `AbilityContainerLayoutApi`, `AbilitySlotWidgetMetadata`, and `AbilitySlotLayoutPlanner`, so addons can keep the built-in HUD/menu while arranging the primary combat bar as a strip, grid, radial/ring group, or categorized cluster with per-slot metadata instead of replacing the whole UI.
- XLib now also ships `ProfileGroupDefinition`, `ProfileDefinition`, `ProfileApi`, `ProfileSelectionScreenFactoryApi`, and `ClaimProfilePayload`, so addons can author persistent profile groups, required-choice onboarding, replaceable selection screens, reset policy, and managed progression starting nodes with server-authoritative claims and `/xlib profiles ...` admin flows.
- XLib now also ships `AbilitySlotMigrationApi`, `XLibMenuOpenEvent`, `XLibProfileSelectionClaimEvent`, `XLibProfileResetEvent`, and `XLibProfileOnboardingCompletedEvent`, so addon updates can remap older fixed-slot data into newer container layouts and integrations can observe menu-opening plus profile lifecycle transitions through explicit extension hooks.
- `PassiveDefinition` now exposes authored-hook and sound-trigger introspection via `authoredHooks()` and `soundTriggers()`, and the built-in command surface now has dedicated passive catalog/describe inspection plus focused active-state debugging through `/xlib debug state`.
- XLib now ships a generalized state-policy layer through `StatePolicyDefinition`, `StatePolicyApi`, and selector-based `AbilitySelector` targets, so modes or contextual sources can apply shared cooldown scaling plus lock/silence/suppression rules; overdrive-style mechanics now fit this path instead of requiring a special built-in type.
- XLib now also ships a generic state-flag layer through `StateFlagApi`, `ModeDefinition.stateFlag(...)`, and `SimpleContextGrantProvider.grantStateFlag(...)`, so addon-defined named states can be projected, source-tracked, surfaced in debug/export output, and reused by `AbilityRequirements.stateFlagActive(...)`.
- XLib now also ships detector/reactive authoring through `AbilityDetectorApi`, `ReactiveTriggerApi`, `ReactiveRuntimeEvent`, and `AbilitySequenceDefinition`, so addons can build custom event windows, reactive responses, and staged multi-step abilities without hardcoding those flows into one-off runtimes.
- XLib now also ships identity/origin and grant-bundle layers, so addons can register inherited archetypes, project bundle-backed power packages between entities, and inspect ownership through `/xlib debug source|dump|export` plus structured `source_descriptors`.
- XLib now also ships an authored artifact/access layer through `ArtifactDefinition`, `ArtifactApi`, artifact-aware `AbilityRequirements`/`ProgressionMenuRequirements`, and debug/export visibility for `active_detectors`, `detector_windows`, `active_artifacts`, `unlocked_artifacts`, and `artifact_unlock_sources`.
- XLib now also ships support-package and relationship helpers, so one entity can project bundle-backed support powers onto linked allies while `GrantOwnershipApi` still reports the package id and grantor in source-descriptor output.
- XLib now ships a neutral runtime cue layer through `XLibCueApi` and `XLibRuntimeCueEvent`, emitting activation start/fail, charge progress, release, hit confirm, interrupt, and state enter/exit cues for adapter modules or addon-defined presentation sinks.
- XLib now also ships optional cue-adapter routing through `XLibCueAdapterApi`, `XLibCueRouteProfileApi`, and `XLibCueSurface`, so addon integrations can split body animation, model animation, and effect playback across different backends without forking the runtime.
- Branching combo chains are already supported with `ComboChainDefinition.builder(...).branch(...)`.
- Progression nodes now support choice groups, explicit node or track locks, identity rewards via `UpgradeRewardBundle.grantIdentity(...)`, and identity-gated follow-ups through `UpgradeRequirements.identityActive(...)`.
- `AbilityRequirements` now ships `all(...)`, `any(...)`, and `not(...)`, and those same reusable requirements can be adapted directly into `GrantConditions`, `SimpleContextGrantProvider`, and `UpgradeConsumeRule` authoring paths.
- Player-authored mode presets are still part of the loadout system, but the built-in management UI is now opt-in through `AbilityLoadoutFeatureApi` instead of assumed for every addon.
- Exact fractional resource values are now first-class, including HUD rendering and helper APIs for slow decay/drain styles.
- Resource builders now have shorter aliases like `max(...)`, `startingValue(...)`, and `decayPerTick(...)`.
- Custom combat marks let addon authors define their own timed debuffs/flags without forcing everything into built-in effects.
- Targeting and combat geometry helpers now cover aimed target selection, hit resolution, teleport-behind, and knockback helpers for melee kits.
- Progression requirements can now compose branches with `all(...)`, `any(...)`, `trackCompleted(...)`, and `anyNodeUnlocked(...)`.
- Modes can now enforce strict ordered cycle steps and apply built-in upkeep like hp drain or per-tick resource changes.
- Recipe restrictions now support exact ids, selectors, advancement-backed unlocks, cached rule resolution, and live viewer sync.
- Granted items now have explicit external-storage policy control instead of only best-effort cleanup.
- The built-in docs are now split into topic pages so the project can read like a library wiki on GitHub and CurseForge.

## Recommended Reading Order

- Read [Getting Started](wiki/Getting-Started.md) first if you are new to XLib.
- Read [System Overview and Status](wiki/System-Overview-and-Status.md) next when you want the shipped architecture and whole-library status before drilling into one API surface.
- Read [Abilities and Loadouts](wiki/Abilities-and-Loadouts.md) and [Modes and Combos](wiki/Modes-and-Combos.md) next if you are building a combat addon.
- Read [Grants, Items, and Recipes](wiki/Grants-Items-and-Recipes.md) when your addon needs source-tracked content unlocks or managed items.
- Read [Progression](wiki/Progression.md) only if you want trees, points, counters, or node rewards.
- Keep [Events, Commands, and Testing](wiki/Events-Commands-and-Testing.md) open while validating your addon or building admin flows.

## Current Limits

- Combat ability state now resolves through validated primary-bar slot references and the built-in HUD/menu support authored strip/grid/radial/categorized primary-bar layouts plus per-slot metadata, but XLib still does not ship a higher-level named multi-loadout profile object for packaging whole UI/loadout schemes as reusable authored assets.
- Persistent profile groups, required-choice onboarding, reset history, and the replaceable selection-screen path now ship, but XLib still does not provide a richer built-in widget/composition layer for heavily customized profile-selection visuals.
- Control profiles can route through addon-owned key mappings, but XLib still does not ship a dedicated in-framework bind-editor surface beyond normal Controls registration.
- The built-in progression screen now supports list mode plus an icon-node tree skill-tree view, but it is still a bounded menu view rather than a free-pan or zoomable canvas/editor.
- The remaining gaps are now mostly higher-level authoring UX and optional tooling, not missing combat/profile/container foundations.
- The generalized state-control layer now covers modes plus source-tracked state policies, generic state flags, detector-driven reactive windows, and staged sequence authoring, but XLib still does not ship a freeform editor for broader non-mode state graphs.
- The neutral catalog metadata surface now spans abilities, passives, modes, granted items, restricted recipe content, upgrade tracks, and upgrade nodes. Built-in client presentation now covers ability page/group/family scoping, a built-in passive browser inside the ability menu, and gameplay-focused progression inspection including reward descriptions from selected tree nodes, while mode/item/recipe catalog browsing still leans more on APIs and command inspection than on dedicated client UI.
- XLib now ships optional cue-adapter and routing APIs, but it still does not bundle a concrete third-party animation backend implementation in-tree.
- Ability-attributed kill progression still depends on addon code recording the hit through `AbilityCombatTracker`.
- `blockExternalStorage()` is strong for player-driven inventory paths, but it is not a universal hook for every possible external inventory system another mod might add.
- XLib no longer includes bundled IDE/demo gameplay fixtures; addon authors should register their own sample content when they want manual validation content during development.

## Packaging Note

XLib's NeoForge mod-list icon is loaded from `src/main/resources/xlib.png` through `logoFile="xlib.png"` in `META-INF/neoforge.mods.toml`.

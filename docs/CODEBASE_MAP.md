# XLib Codebase Map

This file is an internal map of how the mod is organized today. It is meant to help future contributors understand where systems live before making changes.

Use `README.md` for the short project overview. Use `docs/XLIB_USAGE_GUIDE.md` as the public wiki home. Use `docs/wiki/` for topic pages. Use this file when you need the actual subsystem/file map.

## Current Size Snapshot

- Current Java footprint: `265` main-source classes plus `44` JUnit test classes and `2` runtime GameTest classes.
- Largest packages by file count today are `ability` (`110`), `client` (`48`), `command` (`17`), `api` (`14`), `combat` (`12`), `menu` / `progression` (tied at `10` each), the `presentation` / `cue` packages (`9` each), and `network` / `integration` (tied at `7` each).
- The repository already ships a broad framework surface in-tree: combat runtime, grants and items, recipe permissions, progression, client UI, commands, and automated tests.

## Documentation

- `LICENSE`
  Public repository license file. XLib is currently distributed as `All Rights Reserved`.

- `README.md`
  Short project overview for the repo root.

- `docs/XLIB_USAGE_GUIDE.md`
  Public wiki home for addon authors, GitHub release readers, and CurseForge readers.

- `docs/wiki/*.md`
  Public topic pages split by subject instead of one giant guide:
  - getting started
  - system overview and status
  - abilities and loadouts
  - modes and combos
  - grants, items, and recipes
  - progression
  - events, commands, and testing

- `logs/`
  Local runtime log output from dev/game runs. This folder is intentionally gitignored and not part of the published source repository.

- `tools/Sync-GitHubWiki.ps1`
  Maintainer helper for mirroring `docs/XLIB_USAGE_GUIDE.md` plus `docs/wiki/*.md` into the separate GitHub wiki repository layout (`Home.md`, page files, `_Sidebar.md`). It can push directly once the GitHub wiki has been bootstrapped with an initial page in the web UI.

## Root Bootstrap

- `src/main/java/com/whatxe/xlib/XLib.java`
  NeoForge mod entrypoint. Creates the mod and calls `AbilityLibrary.bootstrap(...)`.

- `src/main/java/com/whatxe/xlib/AbilityLibrary.java`
  Core bootstrap. Registers registries, attachments, network payloads, commands, reload listeners, and freezes XLib registries during common setup/server startup.

- `src/main/java/com/whatxe/xlib/XLibRegistryGuard.java`
  Shared guard that prevents late registry mutation outside IDE/dev contexts.

## Player Attachments

- `src/main/java/com/whatxe/xlib/attachment/ModAttachments.java`
  Registers and exposes:
  - `player_ability_data` via `AbilityData`
  - `player_profile_selections` via `ProfileSelectionData`
  - `player_upgrade_progress` via `UpgradeProgressData`
  - `living_combat_marks` via `CombatMarkData`
  - `living_combat_reaction` via `CombatReactionData`
  Also contains the embedded-connection sync workaround used for GameTests.

## Ability / Combat Core

- `src/main/java/com/whatxe/xlib/ability/AbilityApi.java`
  Ability and resource registries, default loadout registry, default player combat data creation, ability-data sanitization, and metadata-driven ability lookup helpers for families, groups, pages, and tags.

- `src/main/java/com/whatxe/xlib/ability/AbilityData.java`
  Main combat-state attachment data:
  - primary combat-bar slot state exposed through validated slot references
  - per-mode player-authored preset layers
  - cooldowns
  - fractional recovery progress for scaled cooldown/recharge ticking
  - active modes and durations
  - detector windows for reactive authoring
  - per-cycle used-mode history for stance rotation groups
  - combo windows and combo slot overrides
  - charges
  - resources with exact fractional progress plus regen/decay delays
  - source-tracked grants, activation blocks, state policies, state flags, grant bundles, and artifact unlock sources
  - managed grant source ids

- `src/main/java/com/whatxe/xlib/ability/AbilitySlotReference.java`
- `src/main/java/com/whatxe/xlib/ability/AbilityContainerState.java`
- `src/main/java/com/whatxe/xlib/ability/AbilitySlotContainerOwnerType.java`
- `src/main/java/com/whatxe/xlib/ability/AbilitySlotContainerDefinition.java`
- `src/main/java/com/whatxe/xlib/ability/AbilitySlotContainerApi.java`
  Primary-bar slot-reference compatibility layer. The older container abstractions remain here so legacy state and APIs can be sanitized, but the shipped runtime now only resolves the built-in primary combat bar.

- `src/main/java/com/whatxe/xlib/ability/AbilitySlotMigrationPlan.java`
- `src/main/java/com/whatxe/xlib/ability/AbilitySlotMigrationApi.java`
  Compatibility/migration layer for remapping legacy fixed-bar assignments into current primary-bar slot references before runtime state is used.

- `src/main/java/com/whatxe/xlib/ability/AbilityDefinition.java`
  Ability builder and runtime definition:
  - action
  - ticker
  - ender
  - optional addon-defined catalog metadata (`family`, `group`, `page`, `tags`)
  - `chargeRelease(...)` shorthand for hold/charge/release toggle flows plus resolved charge-progress helpers for adapters
  - `sequence(...)` shorthand for staged multi-step runtime authoring
  - cooldowns/charges
  - requirements
  - toggle mode settings
  - sound hooks

- `src/main/java/com/whatxe/xlib/ability/AbilityActions.java`
  Higher-level combat action helpers for common melee-kit behavior such as strike, dash-behind strike, launchers, and counter attacks, while still respecting XLib hit-resolution.

- `src/main/java/com/whatxe/xlib/ability/AbilityRuntime.java`
  Server activation/tick/end pipeline for abilities and toggle modes, including neutral cue emission for activation start/fail, charge progress/release, and interruptions, plus detector-window ticking and reactive runtime event dispatch for activation/fail/end flows.

- `src/main/java/com/whatxe/xlib/ability/AbilityGrantApi.java`
  Source-tracked ability ownership plus hard-control checks for activation blocking, lock, silence, and suppression. Command-source grants (`xlib:command`) also act as admin/testing overrides for normal view, assignment, and activation requirement checks so `/xlib abilities grant ...` force-grants stay usable in the menu and at runtime, while explicit activation blocks and later state-policy hard blocks still apply.

- `src/main/java/com/whatxe/xlib/ability/AbilityLoadoutApi.java`
  Primary-bar slot assignment, per-mode preset assignment, and resolved slot lookup after combo overrides, authored mode overlays, and active mode preset fallback.

- `src/main/java/com/whatxe/xlib/ability/AbilityLoadoutFeatureDecision.java`
- `src/main/java/com/whatxe/xlib/ability/AbilityLoadoutFeaturePolicy.java`
- `src/main/java/com/whatxe/xlib/ability/AbilityLoadoutFeature.java`
- `src/main/java/com/whatxe/xlib/ability/AbilityLoadoutFeatureApi.java`
  Addon-controlled loadout-management exposure layer. Lets addons opt players into built-in loadout-management UI and advertise whether the optional `Cycle Loadout` keybind should exist at all.

- `src/main/java/com/whatxe/xlib/ability/AbilityRequirements.java`
  Built-in assign/use/render requirement helpers. Includes exact-resource, combat-mark, recent-hit reaction, detector-window, state-policy, state-flag, identity, artifact, and composable `all(...)`/`any(...)`/`not(...)` helpers, and requirement labels resolve lazily so later-registered abilities, passives, and modes still display readable names in UI and failure feedback instead of raw ids.

- `src/main/java/com/whatxe/xlib/ability/AbilityControlActionType.java`
- `src/main/java/com/whatxe/xlib/ability/AbilityControlAction.java`
- `src/main/java/com/whatxe/xlib/ability/AbilityControlTriggerType.java`
- `src/main/java/com/whatxe/xlib/ability/AbilityControlTrigger.java`
- `src/main/java/com/whatxe/xlib/ability/AbilityControlBinding.java`
- `src/main/java/com/whatxe/xlib/ability/AbilityControlProfile.java`
- `src/main/java/com/whatxe/xlib/ability/AbilityControlProfileApi.java`
  Container-oriented control-profile layer for authored trigger/action bindings, profile priorities, default number-row behavior, and visible-container input resolution.

- `src/main/java/com/whatxe/xlib/ability/AbilitySelector.java`
- `src/main/java/com/whatxe/xlib/ability/AbilitySelectorType.java`
  Selector primitives for targeting one explicit ability or a metadata slice (`family`, `group`, `page`, `tag`, or `all`) when authoring state policies.

- `src/main/java/com/whatxe/xlib/ability/StatePolicyDefinition.java`
- `src/main/java/com/whatxe/xlib/ability/StatePolicyApi.java`
- `src/main/java/com/whatxe/xlib/ability/StateFlagApi.java`
- `src/main/java/com/whatxe/xlib/ability/StateControlStatus.java`
  Generalized state-control layer for source-tracked cooldown multipliers plus selector-based lock, silence, suppression, and seal-style ability control, alongside generic named state flags for addon-defined runtime booleans. Policies and flags can be projected by active modes or contextual grant providers and are surfaced through debug/export helpers.

- `src/main/java/com/whatxe/xlib/ability/GrantBundleDefinition.java`
- `src/main/java/com/whatxe/xlib/ability/GrantBundleApi.java`
- `src/main/java/com/whatxe/xlib/ability/IdentityDefinition.java`
- `src/main/java/com/whatxe/xlib/ability/IdentityApi.java`
- `src/main/java/com/whatxe/xlib/ability/DelegatedGrantApi.java`
- `src/main/java/com/whatxe/xlib/ability/GrantSourceKind.java`
- `src/main/java/com/whatxe/xlib/ability/GrantSourceDescriptor.java`
- `src/main/java/com/whatxe/xlib/ability/GrantOwnershipApi.java`
  Identity/origin and ownership layer for reusable bundle packages, inherited identity bundles, temporary delegated grants, support-package source descriptors, and clearer modeling of why a grant is active and when it should disappear.

- `src/main/java/com/whatxe/xlib/ability/ProfileOnboardingTrigger.java`
- `src/main/java/com/whatxe/xlib/ability/ProfileSelectionOrigin.java`
- `src/main/java/com/whatxe/xlib/ability/ProfileSelectionEntry.java`
- `src/main/java/com/whatxe/xlib/ability/ProfilePendingSelection.java`
- `src/main/java/com/whatxe/xlib/ability/ProfileSelectionData.java`
- `src/main/java/com/whatxe/xlib/ability/ProfileGroupDefinition.java`
- `src/main/java/com/whatxe/xlib/ability/ProfileDefinition.java`
- `src/main/java/com/whatxe/xlib/ability/ProfileApi.java`
  Persistent profile/onboarding layer for authored exclusive profile groups, pending required-choice state, selection-origin and reset-history tracking, source-projected grants/artifacts/state flags/progression starts, menu-block rules, and rebuild/reset/reopen helpers.

- `src/main/java/com/whatxe/xlib/ability/SupportPackageDefinition.java`
- `src/main/java/com/whatxe/xlib/ability/SupportPackageApi.java`
- `src/main/java/com/whatxe/xlib/ability/EntityRelationshipApi.java`
- `src/main/java/com/whatxe/xlib/ability/ControlledEntityApi.java`
  Ally/support and relationship layer for supporter-target bundle projection, persistent master/ally/bond ownership links, and simple command-state helpers for summons, minions, or companions.

- `src/main/java/com/whatxe/xlib/ability/ReactiveEventType.java`
- `src/main/java/com/whatxe/xlib/ability/ReactiveRuntimeEvent.java`
- `src/main/java/com/whatxe/xlib/ability/AbilityDetectorDefinition.java`
- `src/main/java/com/whatxe/xlib/ability/AbilityDetectorApi.java`
- `src/main/java/com/whatxe/xlib/ability/ReactiveTriggerDefinition.java`
- `src/main/java/com/whatxe/xlib/ability/ReactiveTriggerApi.java`
  Detector/reactive authoring layer for neutral runtime events, addon-defined timed windows, and authored response rules that can consume those windows.

- `src/main/java/com/whatxe/xlib/ability/AbilitySequenceStage.java`
- `src/main/java/com/whatxe/xlib/ability/AbilitySequenceState.java`
- `src/main/java/com/whatxe/xlib/ability/AbilitySequenceDefinition.java`
  Staged ability-authoring helpers for multi-step actions with per-stage enter/tick/complete/end callbacks and a builder path that plugs directly into `AbilityDefinition.sequence(...)`.

- `src/main/java/com/whatxe/xlib/ability/ArtifactPresenceMode.java`
- `src/main/java/com/whatxe/xlib/ability/ArtifactDefinition.java`
- `src/main/java/com/whatxe/xlib/ability/ArtifactApi.java`
  Authored artifact/equipment access layer for presence-based bundle projection, unlock-on-consume items, stored artifact unlock sources, and item-id based artifact matching helpers.

- `src/main/java/com/whatxe/xlib/ability/AbilityCombatTracker.java`
  Recent ability-hit attribution helper for systems that need to know which ability caused a later kill. Also kicks off hit-confirm combo triggers, reactive runtime events, and hit-confirm runtime cues when addon code records a landed ability hit.

## Combat Helpers

- `src/main/java/com/whatxe/xlib/combat/CombatMarkDefinition.java`
- `src/main/java/com/whatxe/xlib/combat/CombatMarkData.java`
- `src/main/java/com/whatxe/xlib/combat/CombatMarkState.java`
- `src/main/java/com/whatxe/xlib/combat/CombatMarkApi.java`
  Custom author-defined timed combat marks/debuffs. Supports duration, stacks, value payloads, optional source ids, living-entity attachment storage, and expire/remove/apply events.

- `src/main/java/com/whatxe/xlib/api/event/XLibCombatMarkEvent.java`
  Public lifecycle events for combat marks: applied, refreshed, expired, removed.

- `src/main/java/com/whatxe/xlib/combat/CombatTargetingProfile.java`
- `src/main/java/com/whatxe/xlib/combat/CombatTargetingMode.java`
- `src/main/java/com/whatxe/xlib/combat/CombatTargetingApi.java`
- `src/main/java/com/whatxe/xlib/combat/CombatGeometry.java`
  Reusable targeting and geometry helpers for aimed melee/circle/cone selection, line-of-sight-aware hit resolution, teleport-behind, and knockback/launch movement helpers.

- `src/main/java/com/whatxe/xlib/api/event/XLibCombatHitEvent.java`
  Mutable hit-resolution event surface for addon-defined dodge/block/parry outcomes before damage is applied.

- `src/main/java/com/whatxe/xlib/combat/CombatReactionData.java`
- `src/main/java/com/whatxe/xlib/combat/CombatReactionApi.java`
  Runtime recent-hit / last-attacker / last-damage state for counter, parry, or revenge-window abilities.

- `src/main/java/com/whatxe/xlib/api/event/XLibOutgoingDamageEvent.java`
- `src/main/java/com/whatxe/xlib/api/event/XLibIncomingDamageEvent.java`
  Public mutable player damage events for addon-side outgoing/incoming damage adjustment before XLib's player combat pipeline finalizes the amount.

## Resources

- `src/main/java/com/whatxe/xlib/ability/AbilityResourceDefinition.java`
  Persistent combat-bar resource definition and behavior hooks, including authoring aliases like `min(0)`, `max(...)`, `startingValue(...)`, and `decayPerTick(...)`.

- `src/main/java/com/whatxe/xlib/ability/AbilityResourceApi.java`
  Read/write helpers for player resource amounts, including exact fractional values.

- `src/main/java/com/whatxe/xlib/ability/AbilityResourceRuntime.java`
  Tick/eat/hit/kill resource behavior execution.

- `src/main/java/com/whatxe/xlib/ability/AbilityResourceBehaviors.java`
  Common resource behavior helpers, including exact fractional decay/refill variants.

## Modes / Forms / Combos

- `src/main/java/com/whatxe/xlib/ability/ModeDefinition.java`
  Declarative stance/form profile:
  - optional addon-defined catalog metadata (`family`, `group`, `page`, `tags`)
  - explicit `stackable()` / overlay intent
  - cycle-group membership, ordered-cycle step enforcement, and reset-on-activate triggers
  - cooldown tick-rate multipliers while active
  - optional upkeep helpers for hp drain / minimum-health floors / per-tick resource deltas
  - exclusivity
  - blocked-by modes
  - transform-parent modes
  - slot overlays
  - bundled rewards/blocks while active
  - bundled state-policy and state-flag projection while active

- `src/main/java/com/whatxe/xlib/ability/ModeApi.java`
  Mode registry, combined mode+toggle registration, metadata-driven lookup helpers for families/groups/pages/tags, active overlay resolution, bundled snapshot generation, ordered-cycle validation, cycle-history/reset helpers, upkeep application, aggregated cooldown multiplier lookup, mode-projected state-policy/state-flag projection, and lifecycle event/cue emission with specific end reasons.

- `src/main/java/com/whatxe/xlib/ability/ModeAbilityDefinition.java`
  Wrapper builder for mode-toggle abilities where one id should define both the `AbilityDefinition` and the `ModeDefinition`.

- `src/main/java/com/whatxe/xlib/ability/ComboChainDefinition.java`
  Trigger ability -> follow-up ability combo definition, including optional branch conditions that can swap to different follow-up abilities from the same trigger, trigger timing selection (`activation`, `hit confirm`, `end`), and validated primary-bar target slot references.

- `src/main/java/com/whatxe/xlib/ability/ComboChainApi.java`
  Combo registry and combo window/override application and ticking. Supports activation-triggered combos, hit-confirm/end-triggered combos, remembered primary-bar activation slots for later slot transforms, and the older trigger-only API shape.

- `src/main/java/com/whatxe/xlib/api/event/XLibModeEvent.java`
  Mode lifecycle event surface with specific end reasons for player toggle-off, duration expiry, requirement invalidation, force-end, transform replacement, and exclusive-mode replacement.

- `src/main/java/com/whatxe/xlib/api/event/XLibAbilityActivationEvent.java`
  Ability activation pre/post event surface.

- `src/main/java/com/whatxe/xlib/api/event/XLibMenuOpenEvent.java`
- `src/main/java/com/whatxe/xlib/api/event/XLibProfileSelectionClaimEvent.java`
- `src/main/java/com/whatxe/xlib/api/event/XLibProfileResetEvent.java`
- `src/main/java/com/whatxe/xlib/api/event/XLibProfileOnboardingCompletedEvent.java`
  Public menu/profile lifecycle events for addon-side UI handoff, onboarding hooks, analytics, or server-authoritative profile transition listeners.

## Runtime Cues

- `src/main/java/com/whatxe/xlib/cue/XLibRuntimeCueType.java`
  Neutral cue taxonomy for activation start/fail, charge progress, release, hit confirm, interrupt, and state enter/exit.

- `src/main/java/com/whatxe/xlib/cue/XLibRuntimeCue.java`
  Backend-agnostic cue payload with optional ability/state ids, end reasons, and progress metadata.

- `src/main/java/com/whatxe/xlib/cue/XLibCueSink.java`
  Sink contract for addon-side animation, model, sound, or effect bridges.

- `src/main/java/com/whatxe/xlib/cue/XLibCueSurface.java`
  Explicit capability-surface split for player-body animation, model animation, and effect playback.

- `src/main/java/com/whatxe/xlib/cue/XLibCueAdapter.java`
- `src/main/java/com/whatxe/xlib/cue/XLibCueAdapterApi.java`
  Optional surface-aware adapter layer for third-party animation/effect backend integrations.

- `src/main/java/com/whatxe/xlib/cue/XLibCueRouteProfile.java`
- `src/main/java/com/whatxe/xlib/cue/XLibCueRouteProfileApi.java`
  Route-profile registry that decides which cue types should dispatch to which capability surfaces.

- `src/main/java/com/whatxe/xlib/cue/XLibCueApi.java`
  Cue sink registry and dispatch surface. Posts `XLibRuntimeCueEvent`, fans emitted cues out to registered sinks, and routes them through the active surface profile into registered adapters.

- `src/main/java/com/whatxe/xlib/api/event/XLibRuntimeCueEvent.java`
  Public event-bus surface mirroring emitted runtime cues for integrations that prefer event listeners over sink registration.

## Passives

- `src/main/java/com/whatxe/xlib/ability/PassiveApi.java`
  Passive registry plus metadata-driven lookup helpers for families, groups, pages, and tags.

- `src/main/java/com/whatxe/xlib/ability/PassiveDefinition.java`
  Passive builder and event hook definition, including optional addon-defined catalog metadata, authored-hook/sound-trigger introspection, and cooldown tick-rate multipliers for passive-driven recovery acceleration.

- `src/main/java/com/whatxe/xlib/ability/PassiveGrantApi.java`
  Source-tracked passive grants and revoke lifecycle handling.

- `src/main/java/com/whatxe/xlib/ability/PassiveRuntime.java`
  Passive execution across jump/hurt/hit/kill/eat/armor-change/tick hooks.

## Granted Items

- `src/main/java/com/whatxe/xlib/ability/GrantedItemApi.java`
  Registry for managed/granted item definitions plus metadata-driven lookup helpers for families, groups, pages, and tags.

- `src/main/java/com/whatxe/xlib/ability/GrantedItemDefinition.java`
  Definition of managed item behavior, stack creation, optional addon-defined catalog metadata, revoke handling, undroppable state, and external-storage policy.

- `src/main/java/com/whatxe/xlib/ability/GrantedItemStoragePolicy.java`
  External container policy for managed items:
  - allow external storage
  - reclaim from open storage
  - block external storage

- `src/main/java/com/whatxe/xlib/ability/GrantedItemGrantApi.java`
  Source-tracked granted item ownership API.

- `src/main/java/com/whatxe/xlib/ability/GrantedItemRuntime.java`
  Player-owned managed-item markers, proactive external-slot guards for open menus, revoke pruning across inventory/cursor/open containers, grant insertion, external-storage policy enforcement, shared-storage ownership checks, and missing-item restoration.

- `src/main/java/com/whatxe/xlib/api/event/XLibGrantedItemEvent.java`
  Public reclaim/remove event surface for managed item policy enforcement and revoke cleanup.

## Artifact / Access Layer

- `src/main/java/com/whatxe/xlib/ability/ArtifactPresenceMode.java`
- `src/main/java/com/whatxe/xlib/ability/ArtifactDefinition.java`
- `src/main/java/com/whatxe/xlib/ability/ArtifactApi.java`
  Higher-level item-driven access framework for authored artifacts, equipment/inventory presence rules, unlock-on-consume handling, active/unlocked bundle projection, and stored artifact unlock sources.

## Contextual Grants / Item-Based Grants

- `src/main/java/com/whatxe/xlib/ability/ContextGrantApi.java`
  Registry for contextual grant providers.

- `src/main/java/com/whatxe/xlib/ability/ContextGrantProvider.java`
  Provider contract.

- `src/main/java/com/whatxe/xlib/ability/ContextGrantSnapshot.java`
  Source-tracked bundle of grants, blocks, state policies, and state flags emitted by a provider or mode.

- `src/main/java/com/whatxe/xlib/ability/SimpleContextGrantProvider.java`
  Builder-backed stock contextual provider implementation. Its `when(...)` entry point now accepts either a raw `ContextGrantCondition` or an `AbilityRequirement` adapter, and it can project state policies and state flags alongside normal grants.

- `src/main/java/com/whatxe/xlib/ability/ContextGrantConditions.java`
  Built-in conditions for context providers, including composition helpers and `fromRequirement(...)` adapters.

- `src/main/java/com/whatxe/xlib/ability/GrantConditions.java`
  Built-in item/unlock/resource/consume condition helpers, plus boolean composition and `fromRequirement(...)` reuse adapters for item-driven grants and progression consume rules.

- `src/main/java/com/whatxe/xlib/ability/BuiltInContextProviders.java`
  Ready-made dimension/biome/team/mode/status-effect/armor-set provider helpers.

- `src/main/java/com/whatxe/xlib/ability/AbilityGrantingItem.java`
  Item interface for inventory-based dynamic grants.

- `src/main/java/com/whatxe/xlib/ability/AbilityUnlockItem.java`
  Item interface for one-shot unlocks triggered on consume/use-finish.

## Recipe Restrictions

- `src/main/java/com/whatxe/xlib/ability/RestrictedRecipeDefinition.java`
  Restricted recipe metadata:
  - optional addon-defined catalog metadata (`family`, `group`, `page`, `tags`)
  - recipe tags
  - categories
  - outputs
  - unlock sources
  - unlock advancements
  - optional output NBT
  - unlock hint
  - hide-when-locked

- `src/main/java/com/whatxe/xlib/ability/RestrictedRecipeRule.java`
  Selector-style recipe restriction rule matching by recipe tag, crafting-book category, output item, and optional output NBT, plus the same optional addon-defined catalog metadata used by exact restricted recipe definitions.

- `src/main/java/com/whatxe/xlib/ability/RecipePermissionApi.java`
  Source-tracked recipe permissions, metadata-driven lookup helpers for restricted recipe families/groups/pages/tags, exact+rule restriction resolution, reload/mutation-time resolved caches for selector-matched recipe metadata, advancement-backed auto-unlock sync, datapack reload support, recipe book sync, result-slot enforcement, online-player resync helpers for runtime restrict/unrestrict changes, and metadata-preserving permission grants so existing recipe hints/visibility flags are not overwritten by command/API access flips.

- `src/main/java/com/whatxe/xlib/api/event/XLibRecipePermissionEvent.java`
  Public permission-change event emitted when a player's recipe sources change.

- `src/main/java/com/whatxe/xlib/integration/recipeviewer/JeiEmiRecipeViewerAddon.java`
  Dependency-free bridge API for viewer integrations.

- `src/main/java/com/whatxe/xlib/integration/recipeviewer/RecipeViewerClientRuntime.java`
  Client runtime sync that hides/unhides locked JEI recipes as player permission state changes.

- `src/main/java/com/whatxe/xlib/integration/recipeviewer/XLibJeiPlugin.java`
- `src/main/java/com/whatxe/xlib/integration/recipeviewer/XLibJeiRecipeDecorator.java`
  Optional JEI plugin and recipe decorator for live hidden-recipe sync plus visible lock labels, unlock hints, and unlock-source tooltips on locked recipes.

- `src/main/java/com/whatxe/xlib/integration/recipeviewer/XLibEmiPlugin.java`
- `src/main/java/com/whatxe/xlib/integration/recipeviewer/XLibEmiRecipeDecorator.java`
  Optional EMI plugin and recipe decorator for locked-state labels, hints, and unlock-source tooltips.

## Optional Progression / Upgrade Module

- `src/main/java/com/whatxe/xlib/progression/UpgradeApi.java`
  Main progression registry and player-state API:
  - point types
  - tracks
  - nodes
  - metadata lookups for track/node families, groups, pages, and tags
  - choice-group lookup and structural branch-lock validation
  - consume rules
  - kill rules
  - point/counter mutation
  - node unlock/revoke
  - track revoke and full progression clears
  - reward sync into the core grant APIs
  - reward projection event emission

- `src/main/java/com/whatxe/xlib/progression/UpgradeProgressData.java`
  Progression attachment data:
  - point balances
  - counters
  - unlocked nodes

- `src/main/java/com/whatxe/xlib/progression/UpgradePointType.java`
  Point-type metadata.

- `src/main/java/com/whatxe/xlib/progression/UpgradeTrackDefinition.java`
  Track definition with optional family/group/page/tag metadata, root-node grouping for UI/tree organization, and optional mutually exclusive path selection.

- `src/main/java/com/whatxe/xlib/progression/UpgradeNodeDefinition.java`
  Node definition with:
  - optional family/group/page/tag metadata
  - point costs
  - required nodes
  - additional requirements
  - choice-group branch commitment
  - explicit locked nodes/tracks
  - reward bundle

- `src/main/java/com/whatxe/xlib/progression/UpgradeRequirement.java`
- `src/main/java/com/whatxe/xlib/progression/UpgradeRequirements.java`
  Node requirement contract and built-in helpers, including `all(...)`, `any(...)`, `trackCompleted(...)`, `anyNodeUnlocked(...)`, and `identityActive(...)`.

- `src/main/java/com/whatxe/xlib/progression/UpgradeRewardBundle.java`
  Ability/passive/item/recipe/identity rewards projected into the existing grant systems.

- `src/main/java/com/whatxe/xlib/api/event/XLibUpgradeRewardProjectionEvent.java`
  Public event surface for node reward projection and clearing.

- `src/main/java/com/whatxe/xlib/progression/UpgradeConsumeRule.java`
  Point/counter earning rule for consumed items and foods, now with builder helpers that adapt existing `AbilityRequirement` objects into consume-rule conditions.

- `src/main/java/com/whatxe/xlib/progression/UpgradeKillRule.java`
  Point/counter earning rule for kills, with optional ability-attribution requirement.

## Dev / IDE Sample Content

- None bundled. XLib no longer registers an in-tree demo or sandbox content pack during IDE/dev runs.
- No bundled dev recipe locks exist; `golden_apple` and `diamond_sword` behave normally unless another addon or runtime command restricts them.

## Event Hooks

- `src/main/java/com/whatxe/xlib/event/AbilityGameplayHooks.java`
  Hurt/hit/kill/jump/block-break/armor-change events. Also calls the progression kill hook.

- `src/main/java/com/whatxe/xlib/event/AbilityItemHooks.java`
  Inventory-based dynamic grants, unlock items, resource items, eat hooks, progression consume rules, artifact consume-unlock handling, reactive item-consume dispatch, active artifact bundle sync, and managed-source pruning. It also syncs mode/context-projected state policies and state flags, and preserves active progression node reward sources plus artifact unlock sources during normal dynamic-source sync so long-lived rewards are not accidentally pruned on later ticks.

- `src/main/java/com/whatxe/xlib/event/GrantedItemHooks.java`
  Toss prevention, death-drop stripping for undroppable managed items, and stacked-on-other cancellation for `blockExternalStorage()` managed items.

- `src/main/java/com/whatxe/xlib/event/ModPlayerEvents.java`
  Login sync, death clone handling, tick pipeline, crafting-guard install, granted-item storage-guard install, advancement-driven recipe resync, and progression sync on login/container open.

## Menu Access / Catalog

- `src/main/java/com/whatxe/xlib/menu/MenuAccessState.java`
- `src/main/java/com/whatxe/xlib/menu/MenuAccessDecision.java`
- `src/main/java/com/whatxe/xlib/menu/MenuAccessRequirement.java`
- `src/main/java/com/whatxe/xlib/menu/MenuAccessRequirements.java`
  Shared client-readable hidden/locked/available menu-access model plus reusable requirement adapters and generic predicate helpers.

- `src/main/java/com/whatxe/xlib/menu/AbilityMenuAccessPolicy.java`
- `src/main/java/com/whatxe/xlib/menu/AbilityMenuAccessApi.java`
  Source-keyed ability-menu policy registry. Policies can reuse existing `AbilityRequirement` objects directly and resolve to hidden, locked, or available states per player.

- `src/main/java/com/whatxe/xlib/menu/ProgressionMenuRequirements.java`
- `src/main/java/com/whatxe/xlib/menu/ProgressionMenuAccessPolicy.java`
- `src/main/java/com/whatxe/xlib/menu/ProgressionMenuAccessApi.java`
  Client-safe progression-menu requirement helpers and source-keyed policy registry for hiding or locking the built-in progression screen based on player progression state.

- `src/main/java/com/whatxe/xlib/menu/AbilityMenuCatalog.java`
  Shared ability-catalog helper for page/group/family scope lists, scope sanitization, and metadata-aware catalog ordering used by the built-in ability menu.

## Presentation Profiles

- `src/main/java/com/whatxe/xlib/presentation/MenuPalette.java`
  Shared palette record used by the built-in menu presentation profiles.

- `src/main/java/com/whatxe/xlib/presentation/AbilityMenuPresentation.java`
- `src/main/java/com/whatxe/xlib/presentation/AbilityMenuPresentationApi.java`
  Built-in ability-menu presentation profile registry for palette, list-label detail, and requirement/metadata detail policy.

- `src/main/java/com/whatxe/xlib/presentation/ProgressionNodeLayoutMode.java`
- `src/main/java/com/whatxe/xlib/presentation/ProgressionMenuPresentation.java`
- `src/main/java/com/whatxe/xlib/presentation/ProgressionMenuPresentationApi.java`
- `src/main/java/com/whatxe/xlib/presentation/ProgressionLayoutPlanner.java`
  Built-in progression presentation registry and reusable layout planner for list mode plus icon-node tree skill-tree arrangements.

- `src/main/java/com/whatxe/xlib/presentation/CombatHudPresentation.java`
- `src/main/java/com/whatxe/xlib/presentation/CombatHudPresentationApi.java`
  Combat HUD presentation profile registry for slot-label, overlay, active-name, and resource-label policy.

## Client / UI

- `src/main/java/com/whatxe/xlib/client/AbilityMenuScreenFactoryApi.java`
- `src/main/java/com/whatxe/xlib/client/ProgressionMenuScreenFactoryApi.java`
- `src/main/java/com/whatxe/xlib/client/AbilityMenuScreenFactory.java`
- `src/main/java/com/whatxe/xlib/client/ProgressionMenuScreenFactory.java`
- `src/main/java/com/whatxe/xlib/client/AbilityMenuScreenContext.java`
- `src/main/java/com/whatxe/xlib/client/ProgressionMenuScreenContext.java`
  Client screen-factory registries for replacing the built-in ability/progression screens while preserving XLib keybind opening, built-in fallback behavior, and navigation-context handoff for custom surfaces.

- `src/main/java/com/whatxe/xlib/client/AbilityMenuSessionState.java`
- `src/main/java/com/whatxe/xlib/client/AbilityMenuSessionStateApi.java`
- `src/main/java/com/whatxe/xlib/client/ProgressionMenuSessionState.java`
- `src/main/java/com/whatxe/xlib/client/ProgressionMenuSessionStateApi.java`
  Shared client session-state holders for selected slot/loadout-target mode and selected track/node/layout state, so built-in and addon-owned screens can reuse the same menu-navigation state.

- `src/main/java/com/whatxe/xlib/client/AbilityLoadoutQuickSwitchHandler.java`
- `src/main/java/com/whatxe/xlib/client/AbilityLoadoutQuickSwitchApi.java`
  Client-side registry for addon-owned loadout quick-switch behavior. The keybind path only routes here when an addon has advertised quick-switch support through `AbilityLoadoutFeatureApi`.

- `src/main/java/com/whatxe/xlib/client/AbilityControlKeyMappingDefinition.java`
- `src/main/java/com/whatxe/xlib/client/AbilityControlKeyMappingApi.java`
- `src/main/java/com/whatxe/xlib/client/AbilityControlActionHandler.java`
- `src/main/java/com/whatxe/xlib/client/AbilityControlActionHandlerApi.java`
- `src/main/java/com/whatxe/xlib/client/AbilityControlInputHandler.java`
  Client control-profile bridge for addon-registered Controls entries, addon-owned action handlers, raw key/mouse routing, registered-key-mapping polling, and readable slot hints for HUD/menu surfaces.

- `src/main/java/com/whatxe/xlib/client/AbilitySlotLayoutMode.java`
- `src/main/java/com/whatxe/xlib/client/AbilitySlotLayoutAnchor.java`
- `src/main/java/com/whatxe/xlib/client/AbilitySlotWidgetRole.java`
- `src/main/java/com/whatxe/xlib/client/AbilitySlotWidgetMetadata.java`
- `src/main/java/com/whatxe/xlib/client/AbilityContainerLayoutDefinition.java`
- `src/main/java/com/whatxe/xlib/client/AbilityContainerLayoutApi.java`
- `src/main/java/com/whatxe/xlib/client/AbilitySlotLayoutPlanner.java`
  Built-in slot-composition layer for authoring strip/grid/radial/categorized container layouts, grouped headers, page-tab placement, screen anchors, and slot metadata consumed by the default HUD and ability menu.

- `src/main/java/com/whatxe/xlib/client/AbilityResourceHudAnchor.java`
- `src/main/java/com/whatxe/xlib/client/AbilityResourceHudOrientation.java`
- `src/main/java/com/whatxe/xlib/client/AbilityResourceHudLayout.java`
- `src/main/java/com/whatxe/xlib/client/AbilityResourceHudRegistration.java`
- `src/main/java/com/whatxe/xlib/client/AbilityResourceHudRegistry.java`
- `src/main/java/com/whatxe/xlib/client/AbilityResourceHudRenderer.java`
  Per-resource HUD layout and registration layer for the built-in resource bars. Addons can keep the stock bar renderer while choosing anchor, orientation, sizing, stacking priority, x/y offsets, and whether the built-in bar draws the resource name and numeric value at all, or swap in a fully custom renderer when they want a bespoke shape.

- `src/main/java/com/whatxe/xlib/client/CombatHudRenderer.java`
- `src/main/java/com/whatxe/xlib/client/CombatHudRenderContext.java`
- `src/main/java/com/whatxe/xlib/client/CombatHudRendererApi.java`
  Combat HUD replacement surface for addon-defined renderers that want full control over geometry while still receiving synced combat data and the active presentation profile.

- `src/main/java/com/whatxe/xlib/client/CombatBarOverlay.java`
  Combat bar layer entrypoint. Now delegates rendering through `CombatHudRendererApi`, with the built-in default HUD rendering the primary combat bar on the vanilla hotbar lane with a bottom-flush frame through registered layout definitions so strip, grid, radial, categorized, and anchor-varied presentations can all stay on the default surface while still honoring `CombatBarPreferences`, `AbilityResourceHudRegistry`, per-resource HUD offsets or label policies, and active control-profile slot hints.

- `src/main/java/com/whatxe/xlib/client/CombatBarPreferences.java`
  Persisted client HUD preferences used by the combat bar renderer. Cycling is no longer exposed as a public keybind.

- `src/main/java/com/whatxe/xlib/client/AbilityClientState.java`
  Client keybind polling and combat-bar state. The combat bar toggle now routes through a normal remappable key mapping instead of a hard-coded alt hook, ability/progression menu keybind handling honors the access-policy APIs so hidden menus do not open while locked menus still open in browse-only mode, the open actions now route through the active registered ability/progression screen factories instead of hard-coding the built-in screen classes, active control-profile key mappings are polled every tick, and the optional `Cycle Loadout` keybind now dispatches through addon-registered quick-switch handlers.

- `src/main/java/com/whatxe/xlib/client/ProfileSelectionScreenFactory.java`
- `src/main/java/com/whatxe/xlib/client/ProfileSelectionScreenContext.java`
- `src/main/java/com/whatxe/xlib/client/ProfileSelectionScreenFactoryApi.java`
  Required profile-selection screen replacement surface for built-in or addon-owned onboarding UI.

- `src/main/java/com/whatxe/xlib/client/screen/AbilityMenuScreen.java`
  Slot assignment screen for combat abilities. Visibility is driven by `AbilityGrantApi.canView(...)`, so abilities that fail render/view requirements disappear instead of lingering as disabled placeholders. Also supports addon-gated cycling between the base loadout and player-authored mode preset targets, registered primary-bar layout definitions with grouped slot placement, cached search/filter/sort controls for larger ability libraries, metadata-aware search against ability family/group/page/tag ids, dedicated page/group/family scope controls plus a catalog sort mode, gameplay-focused right-hand details with optional selected-ability metadata rows through `AbilityMenuPresentation.showMetadataDetails(...)`, slot metadata details such as category/role/input/owner/soft-lock state, visible-but-locked browse states through `AbilityMenuAccessApi`, footer-positioned loadout/progression controls aligned against the clear-slot action, presentation-profile-driven list/detail styling through `AbilityMenuPresentationApi`, and shared selected-slot/loadout-target session-state syncing for custom screen handoff.

- `src/main/java/com/whatxe/xlib/client/screen/ProgressionMenuScreen.java`
  Progression browser/unlock screen for upgrade tracks, nodes, point balances, counters, point-source hints, readable paid-cost display after unlock, reward inspection, gameplay-focused node details, choice-group and branch-lock details, identity reward summaries, exclusive-track filtering, visible-but-locked browse states through `ProgressionMenuAccessApi`, a top-row layout switcher when more than one layout is exposed, and presentation-profile-driven list plus icon-node tree skill-tree views backed by `ProgressionLayoutPlanner`, with optional track metadata rows through `ProgressionMenuPresentation.showTrackMetadata(...)` and shared selected-track/node/layout session-state syncing for custom screen handoff.

- `src/main/java/com/whatxe/xlib/client/screen/ProfileSelectionScreen.java`
  Built-in required-choice selection screen for pending profile groups. Shows authored profile metadata, sends server-authoritative claim payloads, and can be replaced through `ProfileSelectionScreenFactoryApi`.

- `src/main/java/com/whatxe/xlib/client/ModKeyMappings.java`
  Client keybinding registration for the remappable built-in combat-bar toggle, opening the ability menu and progression menu, plus the optional addon-advertised `Cycle Loadout` control and addon-registered control-profile key mappings.

- `src/main/java/com/whatxe/xlib/AbilityLibraryClient.java`
  Client bootstrap for HUD, screen, renderer, profile-selection-screen, and control-handler registration hooks, plus config loading.

## Commands / Debugging

- `src/main/java/com/whatxe/xlib/command/AbilityGrantCommands.java`
  `/xlib` dispatcher entrypoint. Domain trees register under this root.

- `src/main/java/com/whatxe/xlib/command/XLibCommandSupport.java`
  Shared validation, recipe suggestions, passive/state inspection formatting, primary-bar slot/loadout formatting, profile selection/pending formatting, detector/artifact/identity/grant-bundle/support-package ownership formatting, structured debug snapshot building, diff helpers, source-group formatting, and reusable formatting for command output.

- `src/main/java/com/whatxe/xlib/command/AbilityAdminCommands.java`
- `src/main/java/com/whatxe/xlib/command/PassiveAdminCommands.java`
- `src/main/java/com/whatxe/xlib/command/GrantedItemAdminCommands.java`
- `src/main/java/com/whatxe/xlib/command/ProfileAdminCommands.java`
- `src/main/java/com/whatxe/xlib/command/RecipeAdminCommands.java`
- `src/main/java/com/whatxe/xlib/command/ProgressionAdminCommands.java`
- `src/main/java/com/whatxe/xlib/command/DebugAdminCommands.java`
  Domain-specific admin-command handlers split out of the old monolithic command implementation, including focused profile/detector/artifact/identity/grant-bundle/state summaries plus state-aware export/source/diff surfaces.

- `src/main/java/com/whatxe/xlib/command/AbilityCommandTree.java`
- `src/main/java/com/whatxe/xlib/command/PassiveCommandTree.java`
- `src/main/java/com/whatxe/xlib/command/GrantedItemCommandTree.java`
- `src/main/java/com/whatxe/xlib/command/ProfileCommandTree.java`
- `src/main/java/com/whatxe/xlib/command/RecipeCommandTree.java`
- `src/main/java/com/whatxe/xlib/command/ProgressionCommandTree.java`
- `src/main/java/com/whatxe/xlib/command/DebugCommandTree.java`
  Per-domain `/xlib` command tree registration:
  - ability grant/revoke/restrict/list/inspect, with `grant` acting as a practical admin force-grant through the `xlib:command` source and `revoke` auto-forwarding into matching progression nodes when the ability is currently backed by unlocked node reward sources
  - passive grant/revoke/clear/list/catalog/describe/inspect/source, including authored-hook and requirement discoverability for passive definitions plus per-player passive state summaries
  - granted item grant/revoke/clear/list/inspect/source
  - profile catalog/groups/claim/reset/reopen/list/pending/resync, including pending-group management and safe rebuild/reset helpers
  - recipe restrict/unrestrict, grant/revoke/clear/list/inspect/source, with runtime metadata changes forcing a live resync of online players and open crafting menus
  - progression unlock/revoke/track revoke/clear/list/inspect
  - debug dump/state/export/diff/source/counters, including progression points/counters/unlocked nodes, focused passive/mode/profile/detector/artifact/identity/grant-bundle/state summaries, structured detector/artifact/identity/bundle/source-descriptor debug-export sections, managed profile/progression-source sections, source-group ownership views, and per-player runtime state counters

- `src/main/java/com/whatxe/xlib/command/XLibDebugCounters.java`
  Lightweight per-player counter snapshot for active modes, detector windows, artifacts, identities, selected/pending profile groups, active grant bundles, active state policies, active state flags, charge/cooldown/resource entries, source-group counts, and other tick-heavy state totals.

## Network

- `src/main/java/com/whatxe/xlib/network/ModPayloads.java`
  Registers payload handlers, defines play protocol versioning, and validates slot references before applying activation or assignment requests.

- `src/main/java/com/whatxe/xlib/network/ActivateAbilityPayload.java`
- `src/main/java/com/whatxe/xlib/network/AssignAbilityPayload.java`
- `src/main/java/com/whatxe/xlib/network/ClaimProfilePayload.java`
- `src/main/java/com/whatxe/xlib/network/UnlockUpgradeNodePayload.java`
  Client->server payloads for primary-bar slot activation, primary-bar base-or-mode-preset slot assignment, server-authoritative profile claims, and progression node unlock requests.

## Tests

- `src/test/java/com/whatxe/xlib/ability/*.java`
  Pure-state JUnit tests for ability, loadout, combo, mode, recipe, and registry behavior.

- `src/test/java/com/whatxe/xlib/ability/AbilityLoadoutFeatureApiTest.java`
  JUnit coverage for addon-controlled loadout-management decisions and conditional quick-switch-keybind advertisement.

- `src/test/java/com/whatxe/xlib/ability/AbilityContainerStateTest.java`
- `src/test/java/com/whatxe/xlib/ability/AbilitySlotContainerApiTest.java`
- `src/test/java/com/whatxe/xlib/ability/AbilitySlotMigrationApiTest.java`
- `src/test/java/com/whatxe/xlib/ability/AbilityControlProfileApiTest.java`
  JUnit coverage for container-state normalization, primary-container legacy migration behavior, explicit migration-plan remapping, container validation, visible-container control-profile resolution, and key/mouse binding exposure.

- `src/test/java/com/whatxe/xlib/ability/StatePolicyApiTest.java`
  JUnit coverage for selector resolution, policy control status, cooldown scaling, and stale-policy sanitization.

- `src/test/java/com/whatxe/xlib/ability/StateFlagApiTest.java`
  JUnit coverage for registered-state-flag activation, stale-flag sanitization, and source-tracked flag cleanup through generic grant-source removal.

- `src/test/java/com/whatxe/xlib/ability/GrantBundleApiTest.java`
- `src/test/java/com/whatxe/xlib/ability/IdentityApiTest.java`
  JUnit coverage for registered grant-bundle sanitization, inherited identity bundle resolution, and identity projection source parsing.

- `src/test/java/com/whatxe/xlib/ability/ProfileApiTest.java`
  JUnit coverage for profile-group sanitization, pending-group onboarding, claim/reset rules, rebuild projection, and managed starting-node behavior.

- `src/test/java/com/whatxe/xlib/ability/SupportPackageApiTest.java`
  JUnit coverage for support-package definition storage and support-package source-id parsing.

- `src/test/java/com/whatxe/xlib/ability/PassiveApiTest.java`
  JUnit coverage for passive metadata storage, registry lookups, and passive hook/sound-trigger introspection.

- `src/test/java/com/whatxe/xlib/ability/AbilityDetectorApiTest.java`
- `src/test/java/com/whatxe/xlib/ability/ReactiveTriggerApiTest.java`
- `src/test/java/com/whatxe/xlib/ability/AbilitySequenceDefinitionTest.java`
- `src/test/java/com/whatxe/xlib/ability/ArtifactApiTest.java`
  JUnit coverage for detector-window registration/sanitization, reactive trigger consumption, staged sequence callbacks, and artifact unlock/item-id matching behavior.

- `src/test/java/com/whatxe/xlib/command/XLibCommandSupportTest.java`
  JUnit coverage for passive, active-mode, profile, detector, artifact, identity, grant-bundle, support-package, state-policy, state-flag, and ownership-descriptor formatting used by the command/debug discoverability surfaces.

- `src/test/java/com/whatxe/xlib/ability/GrantedItemApiTest.java`
  JUnit coverage for granted-item metadata storage and registry lookups.

- `src/test/java/com/whatxe/xlib/ability/GrantConditionsTest.java`
  JUnit coverage for grant-condition composition and requirement adapters.

- `src/test/java/com/whatxe/xlib/ability/ModeApiTest.java`
  JUnit coverage for mode overlays, bundled snapshots, state-policy projection, ordered-cycle behavior, and mode metadata lookups.

- `src/test/java/com/whatxe/xlib/ability/RecipePermissionApiTest.java`
  JUnit coverage for restricted recipe metadata, category/output indexes, and datapack override behavior.

- `src/test/java/com/whatxe/xlib/ability/SimpleContextGrantProviderTest.java`
  JUnit coverage for adapting `AbilityRequirement` objects into contextual grant providers and projecting state-policy/state-flag bundles.

- `src/test/java/com/whatxe/xlib/progression/UpgradeApiTest.java`
  JUnit coverage for progression unlock rules, track/node metadata lookups, branch choice groups, node/track locks, identity rewards, managed unlock sources, node sanitization, and exclusive-track revoke behavior.

- `src/test/java/com/whatxe/xlib/progression/UpgradeConsumeRuleTest.java`
  JUnit coverage for consume-rule builder helpers that adapt `AbilityRequirement` objects into grant conditions.

- `src/test/java/com/whatxe/xlib/menu/*.java`
  JUnit coverage for ability catalog page/group/family helpers plus menu access-policy resolution for ability and progression screens.

- `src/test/java/com/whatxe/xlib/presentation/*.java`
  JUnit coverage for presentation-profile registry behavior and progression layout planning.

- `src/test/java/com/whatxe/xlib/client/AbilityUiSurfaceApiTest.java`
- `src/test/java/com/whatxe/xlib/client/AbilityContainerLayoutApiTest.java`
  JUnit coverage for custom ability/progression/profile-selection screen factories, context-aware menu-state handoff, combat HUD renderer registration/activation fallback behavior, addon loadout quick-switch handler registration, client control key-mapping/action-handler registration, and built-in container-layout registration/default behavior.

- `src/test/java/com/whatxe/xlib/cue/*.java`
  JUnit coverage for cue factories, charge-release progress resolution, sink dispatch, runtime cue event emission, adapter routing, and surface-profile dispatch behavior.

- `src/test/java/com/whatxe/xlib/network/ProtocolVersionTest.java`
  JUnit coverage for play-protocol series/revision compatibility rules.

- `src/main/java/com/whatxe/xlib/gametest/XLibRuntimeGameTests.java`
  End-to-end runtime GameTests covering attachments, mode cycle groups, cooldown scaling, charge-release abilities, branching combos, player-authored mode presets, contextual grants, support packages, controlled entities, granted items, recipe permissions, and branching progression flows.

- `src/main/java/com/whatxe/xlib/network/ModPayloadGameTests.java`
  End-to-end payload activation/assignment GameTest.

## Resources

- `src/main/resources/assets/xlib/lang/en_us.json`
  Built-in English localization keys.

- `src/main/resources/xlib.png`
  Root-jar mod logo used by NeoForge's mod list UI via `logoFile` in `META-INF/neoforge.mods.toml`.

- `src/main/resources/data/xlib/structure/empty.nbt`
  Shared GameTest template structure.

- `src/main/resources/META-INF/neoforge.mods.toml`
  Mod metadata and packaging definition.

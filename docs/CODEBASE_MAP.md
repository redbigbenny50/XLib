# XLib Codebase Map

This file is an internal map of how the mod is organized today. It is meant to help future Codex sessions and human contributors understand where systems live before making changes.

Use `README.md` for the short project overview. Use `docs/XLIB_USAGE_GUIDE.md` as the public wiki home. Use `docs/wiki/` for topic pages. Use this file when you need the actual subsystem/file map.

## Documentation

- `README.md`
  Short project overview for the repo root.

- `docs/XLIB_USAGE_GUIDE.md`
  Public wiki home for addon authors, GitHub release readers, and CurseForge readers.

- `docs/wiki/*.md`
  Public topic pages split by subject instead of one giant guide:
  - getting started
  - abilities and loadouts
  - modes and combos
  - grants, items, and recipes
  - progression
  - events, commands, and testing

- `CODEX_ACTIVITY_LOG.md`
  Local maintainer/Codex work log. This file is intentionally gitignored and not meant to ship in the public GitHub/CurseForge repo contents.

- `logs/`
  Local runtime log output from dev/game runs. This folder is intentionally gitignored and not part of the published source repository.

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
  - `player_upgrade_progress` via `UpgradeProgressData`
  Also contains the embedded-connection sync workaround used for GameTests.

## Ability / Combat Core

- `src/main/java/com/whatxe/xlib/ability/AbilityApi.java`
  Ability and resource registries, default loadout registry, default player combat data creation, and ability-data sanitization.

- `src/main/java/com/whatxe/xlib/ability/AbilityData.java`
  Main combat-state attachment data:
  - base loadout slots
  - per-mode player-authored loadout presets
  - cooldowns
  - fractional recovery progress for scaled cooldown/recharge ticking
  - active modes and durations
  - per-cycle used-mode history for stance rotation groups
  - combo windows and combo slot overrides
  - charges
  - resources and regen/decay delays
  - source-tracked grants and activation blocks
  - managed grant source ids

- `src/main/java/com/whatxe/xlib/ability/AbilityDefinition.java`
  Ability builder and runtime definition:
  - action
  - ticker
  - ender
  - `chargeRelease(...)` shorthand for hold/charge/release toggle flows
  - cooldowns/charges
  - requirements
  - toggle mode settings
  - sound hooks

- `src/main/java/com/whatxe/xlib/ability/AbilityRuntime.java`
  Server activation/tick/end pipeline for abilities and toggle modes.

- `src/main/java/com/whatxe/xlib/ability/AbilityGrantApi.java`
  Source-tracked ability ownership and activation blocking. Command-source grants (`xlib:command`) also act as admin/testing overrides for normal view, assignment, and activation requirement checks so `/xlib abilities grant ...` force-grants stay usable in the menu and at runtime, while explicit activation blocks and later mode-level hard blocks still apply.

- `src/main/java/com/whatxe/xlib/ability/AbilityLoadoutApi.java`
  Base slot assignment, per-mode preset assignment, and resolved slot lookup after combo overrides, authored mode overlays, and active mode preset fallback.

- `src/main/java/com/whatxe/xlib/ability/AbilityRequirements.java`
  Built-in assign/use/render requirement helpers. Requirement labels resolve lazily so later-registered abilities, passives, and modes still display readable names in UI and failure feedback instead of raw ids.

- `src/main/java/com/whatxe/xlib/ability/AbilityCombatTracker.java`
  Recent ability-hit attribution helper for systems that need to know which ability caused a later kill.

## Resources

- `src/main/java/com/whatxe/xlib/ability/AbilityResourceDefinition.java`
  Persistent combat-bar resource definition and behavior hooks.

- `src/main/java/com/whatxe/xlib/ability/AbilityResourceApi.java`
  Read/write helpers for player resource amounts.

- `src/main/java/com/whatxe/xlib/ability/AbilityResourceRuntime.java`
  Tick/eat/hit/kill resource behavior execution.

- `src/main/java/com/whatxe/xlib/ability/AbilityResourceBehaviors.java`
  Common resource behavior helpers.

## Modes / Forms / Combos

- `src/main/java/com/whatxe/xlib/ability/ModeDefinition.java`
  Declarative stance/form profile:
  - explicit `stackable()` / overlay intent
  - cycle-group membership and reset-on-activate triggers
  - cooldown tick-rate multipliers while active
  - exclusivity
  - blocked-by modes
  - transform-parent modes
  - slot overlays
  - bundled rewards/blocks while active

- `src/main/java/com/whatxe/xlib/ability/ModeApi.java`
  Mode registry, active overlay resolution, bundled snapshot generation, cycle-history/reset helpers, aggregated cooldown multiplier lookup, and lifecycle event emission with specific end reasons.

- `src/main/java/com/whatxe/xlib/ability/ComboChainDefinition.java`
  Trigger ability -> follow-up ability combo definition, including optional branch conditions that can swap to different follow-up abilities from the same trigger.

- `src/main/java/com/whatxe/xlib/ability/ComboChainApi.java`
  Combo registry and combo window/override application and ticking. Supports both the legacy trigger-only activation path and the player-aware branching path used at runtime.

- `src/main/java/com/whatxe/xlib/api/event/XLibModeEvent.java`
  Mode lifecycle event surface with specific end reasons for player toggle-off, duration expiry, requirement invalidation, force-end, transform replacement, and exclusive-mode replacement.

- `src/main/java/com/whatxe/xlib/api/event/XLibAbilityActivationEvent.java`
  Ability activation pre/post event surface.

## Passives

- `src/main/java/com/whatxe/xlib/ability/PassiveApi.java`
  Passive registry.

- `src/main/java/com/whatxe/xlib/ability/PassiveDefinition.java`
  Passive builder and event hook definition, including cooldown tick-rate multipliers for passive-driven recovery acceleration.

- `src/main/java/com/whatxe/xlib/ability/PassiveGrantApi.java`
  Source-tracked passive grants and revoke lifecycle handling.

- `src/main/java/com/whatxe/xlib/ability/PassiveRuntime.java`
  Passive execution across jump/hurt/hit/kill/eat/armor-change/tick hooks.

## Granted Items

- `src/main/java/com/whatxe/xlib/ability/GrantedItemApi.java`
  Registry for managed/granted item definitions.

- `src/main/java/com/whatxe/xlib/ability/GrantedItemDefinition.java`
  Definition of managed item behavior, stack creation, revoke handling, undroppable state, and external-storage policy.

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

## Contextual Grants / Item-Based Grants

- `src/main/java/com/whatxe/xlib/ability/ContextGrantApi.java`
  Registry for contextual grant providers.

- `src/main/java/com/whatxe/xlib/ability/ContextGrantProvider.java`
  Provider contract.

- `src/main/java/com/whatxe/xlib/ability/ContextGrantSnapshot.java`
  Source-tracked bundle of grants/blocks emitted by a provider or mode.

- `src/main/java/com/whatxe/xlib/ability/ContextGrantConditions.java`
  Built-in conditions for context providers.

- `src/main/java/com/whatxe/xlib/ability/BuiltInContextProviders.java`
  Ready-made dimension/biome/team/mode/status-effect/armor-set provider helpers.

- `src/main/java/com/whatxe/xlib/ability/AbilityGrantingItem.java`
  Item interface for inventory-based dynamic grants.

- `src/main/java/com/whatxe/xlib/ability/AbilityUnlockItem.java`
  Item interface for one-shot unlocks triggered on consume/use-finish.

## Recipe Restrictions

- `src/main/java/com/whatxe/xlib/ability/RestrictedRecipeDefinition.java`
  Restricted recipe metadata:
  - recipe tags
  - categories
  - outputs
  - unlock sources
  - unlock advancements
  - optional output NBT
  - unlock hint
  - hide-when-locked

- `src/main/java/com/whatxe/xlib/ability/RestrictedRecipeRule.java`
  Selector-style recipe restriction rule matching by recipe tag, crafting-book category, output item, and optional output NBT.

- `src/main/java/com/whatxe/xlib/ability/RecipePermissionApi.java`
  Source-tracked recipe permissions, exact+rule restriction resolution, reload/mutation-time resolved caches for selector-matched recipe metadata, advancement-backed auto-unlock sync, datapack reload support, recipe book sync, result-slot enforcement, online-player resync helpers for runtime restrict/unrestrict changes, and metadata-preserving permission grants so existing recipe hints/visibility flags are not overwritten by command/API access flips.

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
  Track grouping metadata for UI/tree organization, including optional mutually exclusive path selection.

- `src/main/java/com/whatxe/xlib/progression/UpgradeNodeDefinition.java`
  Node definition with:
  - point costs
  - required nodes
  - additional requirements
  - reward bundle

- `src/main/java/com/whatxe/xlib/progression/UpgradeRequirement.java`
- `src/main/java/com/whatxe/xlib/progression/UpgradeRequirements.java`
  Node requirement contract and built-in helpers.

- `src/main/java/com/whatxe/xlib/progression/UpgradeRewardBundle.java`
  Ability/passive/item/recipe rewards projected into the existing grant systems.

- `src/main/java/com/whatxe/xlib/api/event/XLibUpgradeRewardProjectionEvent.java`
  Public event surface for node reward projection and clearing.

- `src/main/java/com/whatxe/xlib/progression/UpgradeConsumeRule.java`
  Point/counter earning rule for consumed items and foods.

- `src/main/java/com/whatxe/xlib/progression/UpgradeKillRule.java`
  Point/counter earning rule for kills, with optional ability-attribution requirement.

## Dev / IDE Sample Content

- `src/main/java/com/whatxe/xlib/dev/XLibDevContent.java`
  IDE-only sample registrations used for manual testing:
  - three granted-item fixtures covering normal revoke removal, undroppable managed items, and keep-on-revoke behavior
  - no bundled dev recipe locks anymore; `golden_apple` and `diamond_sword` now behave normally unless another addon or runtime command restricts them
  - no bundled demo abilities or progression tree anymore; IDE-only manual testing is now focused on granted-item handling unless a contributor explicitly creates runtime recipe locks

## Event Hooks

- `src/main/java/com/whatxe/xlib/event/AbilityGameplayHooks.java`
  Hurt/hit/kill/jump/block-break/armor-change events. Also calls the progression kill hook.

- `src/main/java/com/whatxe/xlib/event/AbilityItemHooks.java`
  Inventory-based dynamic grants, unlock items, resource items, eat hooks, progression consume rules, and managed-source pruning. It also preserves active progression node reward sources during normal dynamic-source sync so unlocked node rewards are not accidentally pruned on later ticks.

- `src/main/java/com/whatxe/xlib/event/GrantedItemHooks.java`
  Toss prevention, death-drop stripping for undroppable managed items, and stacked-on-other cancellation for `blockExternalStorage()` managed items.

- `src/main/java/com/whatxe/xlib/event/ModPlayerEvents.java`
  Login sync, death clone handling, tick pipeline, crafting-guard install, granted-item storage-guard install, advancement-driven recipe resync, and progression sync on login/container open.

## Client / UI

- `src/main/java/com/whatxe/xlib/client/CombatBarOverlay.java`
  Combat bar and resource HUD rendering.

- `src/main/java/com/whatxe/xlib/client/CombatBarPreferences.java`
  Persisted client HUD preferences used by the combat bar renderer. Cycling is no longer exposed as a public keybind.

- `src/main/java/com/whatxe/xlib/client/screen/AbilityMenuScreen.java`
  Slot assignment screen for combat abilities. Visibility is driven by `AbilityGrantApi.canView(...)`, so abilities that fail render/view requirements disappear instead of lingering as disabled placeholders. Also supports cycling between the base loadout and player-authored mode preset targets, cached search/filter/sort controls for larger ability libraries, a dedicated right-column heading/search/filter area below the slot strip, and footer-positioned loadout/progression controls aligned against the clear-slot action.

- `src/main/java/com/whatxe/xlib/client/screen/ProgressionMenuScreen.java`
  Progression browser/unlock screen for upgrade tracks, nodes, point balances, counters, point-source hints, readable paid-cost display after unlock, reward inspection, and exclusive-track filtering.

- `src/main/java/com/whatxe/xlib/client/ModKeyMappings.java`
  Client keybinding registration for opening the ability menu and progression menu.

- `src/main/java/com/whatxe/xlib/AbilityLibraryClient.java`
  Client bootstrap for HUD, screen, renderers, and config loading.

## Commands / Debugging

- `src/main/java/com/whatxe/xlib/command/AbilityGrantCommands.java`
  `/xlib` dispatcher entrypoint. Domain trees register under this root.

- `src/main/java/com/whatxe/xlib/command/XLibCommandSupport.java`
  Shared validation, recipe suggestions, debug snapshot building, diff helpers, and reusable formatting for command output.

- `src/main/java/com/whatxe/xlib/command/AbilityAdminCommands.java`
- `src/main/java/com/whatxe/xlib/command/PassiveAdminCommands.java`
- `src/main/java/com/whatxe/xlib/command/GrantedItemAdminCommands.java`
- `src/main/java/com/whatxe/xlib/command/RecipeAdminCommands.java`
- `src/main/java/com/whatxe/xlib/command/ProgressionAdminCommands.java`
- `src/main/java/com/whatxe/xlib/command/DebugAdminCommands.java`
  Domain-specific admin-command handlers split out of the old monolithic command implementation.

- `src/main/java/com/whatxe/xlib/command/AbilityCommandTree.java`
- `src/main/java/com/whatxe/xlib/command/PassiveCommandTree.java`
- `src/main/java/com/whatxe/xlib/command/GrantedItemCommandTree.java`
- `src/main/java/com/whatxe/xlib/command/RecipeCommandTree.java`
- `src/main/java/com/whatxe/xlib/command/ProgressionCommandTree.java`
- `src/main/java/com/whatxe/xlib/command/DebugCommandTree.java`
  Per-domain `/xlib` command tree registration:
  - ability grant/revoke/restrict/list/inspect, with `grant` acting as a practical admin force-grant through the `xlib:command` source and `revoke` auto-forwarding into matching progression nodes when the ability is currently backed by unlocked node reward sources
  - passive grant/revoke/list
  - granted item grant/revoke/clear/list/inspect/source
  - recipe restrict/unrestrict, grant/revoke/clear/list/inspect/source, with runtime metadata changes forcing a live resync of online players and open crafting menus
  - progression unlock/revoke/track revoke/clear/list/inspect
  - debug dump/export/diff/source/counters, including progression points/counters/unlocked nodes and per-player runtime state counters

- `src/main/java/com/whatxe/xlib/command/XLibDebugCounters.java`
  Lightweight per-player counter snapshot for active modes, charge/cooldown/resource entries, source-group counts, and other tick-heavy state totals.

## Network

- `src/main/java/com/whatxe/xlib/network/ModPayloads.java`
  Registers payload handlers and defines play protocol versioning.

- `src/main/java/com/whatxe/xlib/network/ActivateAbilityPayload.java`
- `src/main/java/com/whatxe/xlib/network/AssignAbilityPayload.java`
- `src/main/java/com/whatxe/xlib/network/UnlockUpgradeNodePayload.java`
  Client->server payloads for slot activation, base-or-mode-preset slot assignment, and progression node unlock requests.

## Tests

- `src/test/java/com/whatxe/xlib/ability/*.java`
  Pure-state JUnit tests for ability, loadout, combo, mode, recipe, and registry behavior.

- `src/test/java/com/whatxe/xlib/progression/UpgradeApiTest.java`
  JUnit coverage for progression unlock rules, node sanitization, and exclusive-track revoke behavior.

- `src/main/java/com/whatxe/xlib/gametest/XLibRuntimeGameTests.java`
  End-to-end runtime GameTests covering attachments, mode cycle groups, cooldown scaling, charge-release abilities, branching combos, player-authored mode presets, contextual grants, granted items, recipe permissions, and the progression module.

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

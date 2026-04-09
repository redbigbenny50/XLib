# XLib Roadmap 2

This file captures the second planning pass after the first bundled roadmap in `ROADMAP.md` was completed and audited on 2026-04-06.

It focused on the next framework gaps surfaced by the codebase after the earlier foundations landed: richer built-in UI composition plus the remaining compatibility and migration follow-through. Those planned areas are now implemented and this file remains as the bundled record of that second pass.

Historical note: the original multi-container and container-page stack that landed during this roadmap has since been retired. The current repository baseline is a primary 9-slot combat bar plus per-mode presets, with the older container abstractions kept only as compatibility wrappers around that primary surface.

## Planning Direction

- Keep future roadmap language content-agnostic so the same framework can support many different addon patterns.
- Prefer source-driven, profile-driven, or state-driven terminology over theme-locked role names.
- Treat example use cases as proof of flexibility, not as the framework's intended identity.

## Current Answer Snapshot

- Built-in menus and HUD are now replaceable today:
  - `AbilityMenuAccessApi` and `ProgressionMenuAccessApi` can hide or lock the built-in screens.
  - `AbilityMenuPresentationApi`, `ProgressionMenuPresentationApi`, and `CombatHudPresentationApi` can still restyle built-in surfaces when addons want to keep them.
  - `AbilityMenuScreenFactoryApi`, `ProgressionMenuScreenFactoryApi`, and `CombatHudRendererApi` now let addons replace the built-in ability/progression screens and combat HUD renderer end-to-end.
  - `AbilityMenuScreenFactory` / `ProgressionMenuScreenFactory`, `AbilityMenuScreenContext` / `ProgressionMenuScreenContext`, and the shared menu-session-state APIs now let custom screens receive reusable navigation state instead of rebuilding selected slot, selected node, or layout handoff from scratch.
  - `AbilityContainerLayoutDefinition`, `AbilityContainerLayoutApi`, `AbilitySlotWidgetMetadata`, and `AbilitySlotLayoutPlanner` now let addons keep the built-in surfaces while authoring strip, grid, radial, or categorized slot arrangements, page tabs, grouped slot clusters, anchor rules, and slot metadata such as labels, categories, role hints, soft locks, and input hints.
- Primary combat-bar slot-reference and control-profile foundations now exist today:
  - `AbilitySlotReference`, `AbilityContainerState`, and `AbilitySlotContainerApi` keep the runtime on explicit validated slot references instead of raw integers, while still resolving against the built-in primary combat bar.
  - `AbilityData`, `AbilityLoadoutApi`, `ModeApi`, `ComboChainApi`, network payloads, built-in HUD rendering, the built-in ability menu, and debug/export formatting now all share that primary-bar slot-reference path instead of hand-maintaining separate integer-slot plumbing.
  - `AbilityControlProfileApi`, `AbilityControlTrigger`, `AbilityControlAction`, `AbilityControlKeyMappingApi`, and `AbilityControlActionHandlerApi` now provide authored control profiles, addon-owned key mappings, addon-owned action handlers, and readable input hints for the primary combat bar.
  - `AbilityLoadoutFeatureApi` still controls whether built-in loadout-management surfaces and an optional quick-switch keybind should exist for a given addon/player state.
  - XLib still does not ship a first-class named multi-loadout profile object for packaging whole assignment schemes as reusable authored assets.
- Long-lived profile, onboarding, and reset logic now exists today:
  - `ProfileGroupDefinition`, `ProfileDefinition`, `ProfileSelectionData`, and `ProfileApi` now provide first-class persistent profile groups, authored incompatibility, pending-group tracking, and server-authoritative claim/reset/reopen flows.
  - Required-choice onboarding now supports first login, respawn, advancement, item-use, and command triggers, with per-group rules for blocking ability use, the built-in ability menu, or progression until a required selection is claimed.
  - `ProfileSelectionScreenFactoryApi`, `ProfileSelectionScreenFactory`, and `ProfileSelectionScreenContext` now let addons keep the built-in required-selection screen or replace it with their own custom surface.
  - Profile-backed starting nodes now project through managed unlock sources, so rebuilds, resets, and debug/export flows can add or remove those progression starts cleanly.
- Compatibility and extension safety now also exist today:
  - `AbilitySlotMigrationApi` and `AbilitySlotMigrationPlan` can preserve or remap legacy fixed-slot data through the current primary-bar slot-reference model.
  - `ModPayloads` now validates slot/container references against resolved runtime layouts and exposes explicit `series`/`revision` protocol versioning for safer network compatibility.
  - `XLibMenuOpenEvent`, `XLibProfileSelectionClaimEvent`, `XLibProfileResetEvent`, and `XLibProfileOnboardingCompletedEvent` now give addons clearer lifecycle hooks around menu opening and server-authoritative profile transitions.
- The remaining gaps after this roadmap are now higher-level authoring UX rather than missing container/profile plumbing:
  - no first-class named multi-loadout profile object yet
  - no in-framework bind-editor surface beyond normal Controls registration
  - heavily customized profile-selection visuals still work best as full custom screens rather than as a richer built-in widget layer

## Planned Areas

### 1. Full UI Replacement and Screen Factories

- [x] Baseline hook layer is now in place through `AbilityMenuScreenFactoryApi`, `ProgressionMenuScreenFactoryApi`, and `CombatHudRendererApi`.
- [x] Context-aware navigation is now in place through `AbilityMenuScreenFactory`, `ProgressionMenuScreenFactory`, `AbilityMenuScreenContext`, and `ProgressionMenuScreenContext`.
- [x] Shared `AbilityMenuSessionStateApi` and `ProgressionMenuSessionStateApi` now preserve selected slot, loadout-target mode, selected track, selected node, and layout mode across built-in or addon-owned surfaces.
- [x] Optional fallback-to-built-in behavior remains in place when no addon screen/HUD is active.
- [x] The remaining custom-geometry work now lives in Areas 2 and 7 rather than in this factory/navigation category.

### 2. Primary Bar Loadout and Slot-Reference Architecture

- [x] Replace the old raw integer-slot plumbing with `AbilitySlotReference`, `AbilityContainerState`, and `AbilitySlotContainerApi` as the compatibility layer around the primary combat bar.
- [x] Keep the built-in nine-slot combat bar addressable through explicit validated slot references instead of only hard-coded `0-8` integers.
- [x] Make combo overrides, mode overlays, default loadouts, payloads, HUD rendering, menu assignment, and debug/export surfaces all operate against the same primary-bar slot-reference model.
- [x] Keep per-mode player-authored preset layers aligned with that slot-reference model instead of maintaining separate one-off loadout state.
- [x] Retire addon-defined auxiliary containers and container pages from the shipped runtime while preserving enough compatibility wrappers to sanitize legacy state safely.
- [x] Leave richer custom geometry and widget composition to Area 7 instead of coupling them to additional runtime slot surfaces.

### 3. Input, Keybinding, and Control Profiles

- [x] Extend the earlier optional quick-switch/loadout exposure work with first-class authored control profiles through `AbilityControlProfileApi`, `AbilityControlTrigger`, `AbilityControlAction`, and container-owned control-profile ids.
- [x] Support authored activation and navigation styles including:
  - number-row keys
  - mouse buttons
  - modifier + key combinations
  - addon-registered key mappings
  - addon-defined higher-level actions such as hold/open/select flows
- [x] Let mods decide whether a control profile is fixed or intentionally exposed through player-rebindable Controls entries by choosing direct triggers versus registered key-mapping triggers.
- [x] Support addon-owned control maps and custom input actions without forcing addons to replace the built-in input polling path.
- [x] Add conflict handling/priorities through profile ordering and active-action resolution when multiple bindings compete for the same input.
- [x] Add readable slot/input hints so built-in or custom HUD/menu surfaces can show keys, mouse prompts, and current slot context.
- [x] Keep richer custom UI composition and layout-specific widgets in Area 7 rather than coupling them to the control-profile foundation.

### 4. Persistent Profile and Choice Authoring

- [x] Add first-class profile-style definitions through `ProfileGroupDefinition`, `ProfileDefinition`, `ProfileSelectionData`, and `ProfileApi`.
- [x] Support exclusive profile groups, icons, display descriptions, starting packages, and authored incompatibility rules.
- [x] Allow profile definitions to project identities, bundles, abilities, passives, modes, artifacts, recipes, state flags, or progression starting nodes.
- [x] Keep the framework neutral so long-lived player states, inherited traits, specialization families, and other addon-defined profile systems all sit on the same layer.

### 5. Spawn / Onboarding / Required Choice Flow

- [x] Add a pending-selection marker and open a required selection menu on first join/spawn when authored content demands it.
- [x] Support blocking ability use, menu access, or progression until a required choice is claimed.
- [x] Add server-authoritative claim flow so multiplayer joins cannot desync or bypass required choices.
- [x] Support configurable triggers such as first login, respawn, advancement gate, item use, or command-driven onboarding.

### 6. Reset / Respec / Admin Policy Layer

- [x] Add authored rules for whether a player can reset, respec, reroll, or clear a committed profile/required-choice state.
- [x] Add admin commands and safe runtime helpers for clearing selections, rebuilding grants, and re-opening onboarding menus.
- [x] Track why a selection exists and whether it was locked, temporary, delegated, or admin-set.
- [x] Add diff/export visibility for pending-choice state, selection ownership, and reset history.

### 7. UI Composition and Slot Widgets

- [x] Add addon-defined slot widgets, grouped primary-bar layouts, and alternate assignment layouts for the ability menu through `AbilityContainerLayoutDefinition`, `AbilityContainerLayoutApi`, `AbilitySlotWidgetMetadata`, and `AbilitySlotLayoutPlanner`.
- [x] Let addons choose between strip, grid, radial, or categorized primary-bar presentations without forking core logic.
- [x] Support non-uniform UI arrangements like radial menus, compact grids, header-grouped strips, or anchor-shifted bars even when the runtime still resolves against the same primary slot surface.
- [x] Support custom slot metadata in the HUD and menu such as labels, categories, role hints, soft locks, mode ownership, source labels, or input hints.
- [x] Support authored anchor/layout rules so one addon can place the primary bar in a long strip while another uses a radial or isolated cluster presentation.
- [x] Keep built-in screens available as default implementations rather than the only implementations.

### 8. Compatibility and Migration

- [x] Add migration helpers for converting old fixed nine-slot data into the current slot-reference model through `AbilitySlotMigrationApi` and `AbilitySlotMigrationPlan`.
- [x] Preserve network/version safety when addons change slot layouts or selection rules between releases through protocol `series`/`revision` versioning plus payload validation against resolved primary-bar slot state.
- [x] Add clearer extension events around menu open, selection claim, profile reset, and onboarding completion.
- [x] Expand tests around login flow, respawn flow, custom-screen registration, primary-bar slot synchronization, and keybinding/profile persistence.

## Completion Notes

- All eight Roadmap 2 areas are now implemented in the repository.
- The next follow-on planning pass can focus on higher-level authoring UX, named reusable loadout assets, or optional bind-editor/configuration surfaces rather than missing slot/container/profile foundations.
- That follow-on planning pass now lives in `ROADMAP_3.md`.

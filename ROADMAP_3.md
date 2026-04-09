# XLib Roadmap 3

This file captures the third planning pass after `ROADMAP.md` and `ROADMAP_2.md` were completed and audited on 2026-04-06.

It focuses on the next layer up from the now-shipped runtime foundations: reusable authored assets, richer built-in UX, and optional tooling that makes the framework easier for addons and players to live with at scale.

## Planning Direction

- Keep the framework neutral and source-agnostic so the same systems can support many addon styles.
- Prefer higher-level reusable assets and optional tooling over one-off hardcoded screens or theme-specific mechanics.
- Treat full custom screens and HUDs as permanent extension points, while making the built-in surfaces strong enough that addons do not always need to replace them.

## Current Answer Snapshot

- XLib already ships the runtime foundations:
  - primary combat-bar slot references
  - primary-bar layout metadata
  - authored control profiles
  - persistent profile/onboarding state
  - built-in or replaceable HUD/menu/profile-selection surfaces
  - migration/version safety for slot/loadout changes
- The next real gaps are higher-level UX and authored assets:
  - no first-class named loadout-profile object yet
  - no first-class special-purpose slot action layer for things like authored selectors, mode/profile cycling, or icon-only non-ability widgets
  - no dedicated in-framework bind-editor/configuration surface beyond normal Controls registration
  - highly customized profile-selection visuals still work best as full custom screens rather than built-in reusable widgets
- the progression screen now has icon-driven bounded tree layouts plus a simpler list fallback with visible branch pathing, but it still does not provide a free-pan/zoom large-tree canvas
  - validation/debugging is strong, but there is still room for richer authoring diagnostics and UI-facing inspection tools

## Planned Areas

### 1. Named Loadout Profiles and Assignment Sets

- Add a first-class authored loadout-profile layer on top of the current attachment-driven primary-bar state.
- Support named profile definitions for:
  - baseline assignment sets
  - source-owned assignment sets
  - profile/identity-driven assignment sets
  - mode/state-specific assignment sets
- Support authored switching rules, defaults, and visibility so addons can expose zero, one, or many switchable sets without rebuilding profile storage themselves.
- Keep player-editable versus source-locked assignment ownership explicit per loadout/profile.
- Preserve migration safety when an addon revises a named assignment set between releases.

### 2. Special Slot Actions and Selector Widgets

- Add first-class slot roles beyond plain ability activation:
  - selector opener
  - profile/loadout cycle
  - mode/state cycle
  - contextual source action
  - icon-only status/action slot
- Let built-in HUD/menu surfaces render and respect those roles without forcing every addon to replace the whole UI.
- Support fixed/non-assignable authored slots alongside normal player-editable ability slots.
- Support selector-driven flows where one control opens a choice surface and the chosen entry updates a mode, profile, or assignment target.
- Keep the model neutral so addons can use the same layer for switching, paging, toggling, or other authored control intent.

### 3. Control Configuration and Binding UX

- Add an optional in-framework bind-editor/configuration surface on top of the shipped control-profile runtime.
- Let addons decide whether a control profile is:
  - fully fixed
  - partially player-editable
  - fully player-editable
- Expose readable conflict resolution, priority explanation, and active-container scope so players can understand why one binding wins.
- Support resetting to addon defaults, copying profiles, and per-source/per-profile control groups.
- Keep normal Minecraft Controls integration available instead of replacing it.

### 4. Profile Selection and Onboarding Widget Kit

- Add reusable built-in widgets for profile/required-choice surfaces instead of forcing a full custom screen for richer presentation.
- Support built-in card, list, grid, and radial/ring profile selectors plus preview/detail panels.
- Support multi-step required-choice flows where one selection unlocks or filters the next step without leaving the server-authoritative onboarding model.
- Expose authored compatibility/incompatibility warnings, reset policy hints, and projected package summaries directly in the built-in selection UX.
- Keep full custom screen replacement as the ceiling when addons want total visual control.

### 5. Progression Canvas and Large-Tree Navigation

- Add a free-pan/zoom progression canvas mode for larger authored graphs.
- Support grouped regions, lane headers, branch summaries, breadcrumbs, and optional overview/minimap affordances.
- Improve cross-track and cross-group readability when one addon uses a larger specialization graph instead of a compact tree/list.
- Keep the current bounded menu layouts available as lighter-weight defaults for smaller addons.
- Preserve existing progression data/requirement/reward backends rather than replacing them.

### 6. Validation, Diagnostics, and UX-Facing Inspection

- Add richer authoring validation for stale ids, invalid slot references, impossible requirement combinations, orphaned profile projections, and incompatible control/profile/layout definitions.
- Add UI-facing inspection/debug helpers for:
  - active control resolution
  - active primary-bar layout state
  - selected assignment profile
  - pending onboarding blockers
  - profile/projection provenance
- Expand automated coverage around named loadout profiles, selector widgets, control persistence, onboarding composition, and progression-canvas behavior.
- Keep diagnostics readable for addon authors without requiring direct attachment dumps.

## Suggested Order

1. Named Loadout Profiles and Assignment Sets
2. Special Slot Actions and Selector Widgets
3. Control Configuration and Binding UX
4. Profile Selection and Onboarding Widget Kit
5. Progression Canvas and Large-Tree Navigation
6. Validation, Diagnostics, and UX-Facing Inspection

If the immediate target is richer combat/loadout UX, start with Areas 1 and 2.

If the immediate target is player-facing configuration quality, start with Area 3.

If the immediate target is better built-in onboarding/progression presentation, start with Areas 4 and 5.

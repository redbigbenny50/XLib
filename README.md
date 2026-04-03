# XLib

XLib is a NeoForge `1.21.1` framework mod for combat, progression, and source-tracked unlock systems.

It is architected as a framework mod for addon/content mods, not a standalone gameplay-content pack.

## What It Provides

XLib ships reusable APIs and runtime systems for:

- active combat abilities
- player loadouts and per-mode loadout presets
- modes / forms / stances
- combo follow-up windows and branching combo flows
- passives
- custom resource pools and first-class resource costs
- cooldowns, charges, charge-release abilities, and cooldown scaling
- source-tracked ability / passive / item / recipe grants
- managed granted items
- recipe permission gates and selector-based recipe restrictions
- optional progression / upgrade trees
- built-in client UI for combat loadouts and progression
- admin/debug commands
- optional JEI / EMI recipe-viewer integration

## Major Systems

- `AbilityDefinition` / `AbilityRuntime`
  Active ability registration, activation, ticking, cooldowns, charges, charge-release helpers, and end handling.
- `ModeDefinition` / `ModeApi`
  Stances/forms with overlays, explicit stackable modes, cycle groups, reset triggers, and cooldown scaling.
- `ComboChainDefinition` / `ComboChainApi`
  Temporary follow-up windows, slot overrides, and branching combo follow-ups.
- `GrantedItemDefinition` / `GrantedItemRuntime`
  Managed items with revoke cleanup, undroppable state, and external-storage policies.
- `RecipePermissionApi`
  Exact and selector-based recipe locks, advancement-backed unlocks, viewer sync, and runtime restrict/unrestrict support.
- `UpgradeApi`
  Optional progression nodes, point types, consume/kill rules, and reward projection into XLib's grant systems.

## Included Client Features

- combat bar / loadout UI
- player-authored mode-specific presets
- progression menu
- resource HUD rendering

## Commands

XLib includes `/xlib` admin/debug surfaces for:

- abilities
- passives
- items
- recipes
- progression
- debug / export / diff / counters

## Documentation

- `README.md`
  Top-level project summary.
- `docs/XLIB_USAGE_GUIDE.md`
  Wiki home for addon authors and release-facing documentation.
- `docs/wiki/`
  Topic pages for getting started, combat systems, unlock systems, progression, and testing.
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

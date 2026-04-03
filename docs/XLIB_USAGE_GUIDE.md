# XLib Wiki

This document is now the release-facing wiki home for XLib.

Use [README.md](../README.md) for the short repository overview. Use [CODEBASE_MAP.md](CODEBASE_MAP.md) when you need the file-level subsystem map. Use the pages below for addon-author documentation. Local maintainer artifacts like `CODEX_ACTIVITY_LOG.md` are intentionally not part of the published library docs.

Runtime `logs/` output from local dev/game runs is also intentionally excluded from the published repo.

Maintainers can mirror this docs set into the separate GitHub wiki repository with `tools/Sync-GitHubWiki.ps1` once the GitHub wiki has been bootstrapped with its first page in the web UI.

XLib's repository license is `All Rights Reserved`. This documentation describes the API surface and runtime behavior, but it does not grant reuse or redistribution rights.

## Start Here

1. [Getting Started](wiki/Getting-Started.md)
2. [Abilities and Loadouts](wiki/Abilities-and-Loadouts.md)
3. [Modes and Combos](wiki/Modes-and-Combos.md)
4. [Grants, Items, and Recipes](wiki/Grants-Items-and-Recipes.md)
5. [Progression](wiki/Progression.md)
6. [Events, Commands, and Testing](wiki/Events-Commands-and-Testing.md)

## What XLib Covers

XLib is a library mod for:

- active combat abilities with cooldowns, charges, resource costs, and slot assignment
- base loadouts plus player-authored mode-specific presets
- modes, forms, and stances with overlays, exclusivity, transforms, ordered cycle groups, upkeep helpers, and cooldown scaling
- combo windows and branching combo follow-ups, including hit-confirm and end-triggered follow-ups
- custom combat marks/debuffs plus reusable targeting/hit-resolution helpers
- source-tracked ability, passive, item, and recipe grants
- managed granted items with storage policy enforcement
- exact/fractional resources, exact and selector-based recipe restrictions, and JEI/EMI integration
- an optional progression module for point, counter, track, and node systems

## Quick Mental Model

- Grants answer ownership: can the player use or receive something at all?
- Loadouts answer equipment: which abilities are currently bound to combat slots?
- Modes modify runtime state: overlays, bundles, transforms, stackable layers, or mode-specific presets.
- Combos open temporary follow-up windows and can branch to different follow-ups based on conditions.
- Progression is optional and projects rewards back into the same source-tracked grant systems instead of replacing them.

## Release Highlights

- Branching combo chains are already supported with `ComboChainDefinition.builder(...).branch(...)`.
- Player-authored mode presets are built into the loadout system and configurable in the ability menu.
- Exact fractional resource values are now first-class, including HUD rendering and helper APIs for slow decay/drain styles.
- Custom combat marks let addon authors define their own timed debuffs/flags without forcing everything into built-in effects.
- Targeting and combat geometry helpers now cover aimed target selection, hit resolution, teleport-behind, and knockback helpers for melee kits.
- Modes can now enforce strict ordered cycle steps and apply built-in upkeep like hp drain or per-tick resource changes.
- Recipe restrictions now support exact ids, selectors, advancement-backed unlocks, cached rule resolution, and live viewer sync.
- Granted items now have explicit external-storage policy control instead of only best-effort cleanup.
- The built-in docs are now split into topic pages so the project can read like a library wiki on GitHub and CurseForge.

## Recommended Reading Order

- Read [Getting Started](wiki/Getting-Started.md) first if you are new to XLib.
- Read [Abilities and Loadouts](wiki/Abilities-and-Loadouts.md) and [Modes and Combos](wiki/Modes-and-Combos.md) next if you are building a combat addon.
- Read [Grants, Items, and Recipes](wiki/Grants-Items-and-Recipes.md) when your addon needs source-tracked content unlocks or managed items.
- Read [Progression](wiki/Progression.md) only if you want trees, points, counters, or node rewards.
- Keep [Events, Commands, and Testing](wiki/Events-Commands-and-Testing.md) open while validating your addon or building admin flows.

## Current Limits

- There is no first-class named preset profile object yet; mode presets still live as per-player attachment data.
- The built-in progression screen is still a list/detail UI, not a graph canvas.
- Ability-attributed kill progression still depends on addon code recording the hit through `AbilityCombatTracker`.
- `blockExternalStorage()` is strong for player-driven inventory paths, but it is not a universal hook for every possible external inventory system another mod might add.

## Packaging Note

XLib's NeoForge mod-list icon is loaded from `src/main/resources/xlib.png` through `logoFile="xlib.png"` in `META-INF/neoforge.mods.toml`.

# Events, Commands, and Testing

This page covers the public integration hooks, the built-in `/xlib` admin surface, and the recommended validation flow for addon authors.

## Public Events

Useful event surfaces include:

- `XLibAbilityActivationEvent`
- `XLibModeEvent.Ended` with explicit end reasons
- `XLibCombatHitEvent`
- `XLibCombatMarkEvent`
- `XLibGrantedItemEvent.Reclaimed`
- `XLibGrantedItemEvent.Removed`
- `XLibRecipePermissionEvent`
- `XLibUpgradeRewardProjectionEvent.Projected`
- `XLibUpgradeRewardProjectionEvent.Cleared`

These exist so integrations do not need to poll attachments for recipe access changes, combat-mark lifecycles, dodge/parry/block hit resolution, granted-item cleanup, or projected progression rewards.

## Admin and Debug Commands

XLib ships `/xlib` command trees for:

- abilities
- passives
- items
- recipes
- progression
- debug

High-value commands:

- `/xlib items grant|revoke|clear|list|inspect|sources ...`
- `/xlib recipes restrict|unrestrict|grant|revoke|clear|list|inspect|sources ...`
- `/xlib progression unlock|revoke|track revoke|clear|inspect ...`
- `/xlib debug counters <player>`
- `/xlib debug export <player>`
- `/xlib debug diff <player> <snapshot>`

Notes:

- `/xlib abilities grant` uses the `xlib:command` source and acts as a practical admin override for normal view, assignment, and activation requirements.
- `/xlib abilities revoke` now also checks for matching unlocked progression-node rewards and revokes those backing nodes first when needed.
- `/xlib recipes restrict` applies runtime recipe restrictions immediately and resyncs online players plus already-open crafting menus.
- `/xlib recipes inspect` reports effective metadata including tags, categories, outputs, advancements, and output NBT when present.

## Recommended Testing Flow

1. Unit-test pure state logic around abilities, grants, recipes, and progression.
2. Add GameTests for real runtime flows like activation, kill tracking, item cleanup, and crafting locks.
3. Run:

```powershell
.\gradlew.bat test
.\gradlew.bat runGameTestServer
```

## IDE Dev Fixtures

IDE runs currently include focused granted-item fixtures:

- `xlib:demo_loaner_blade`
- `xlib:demo_bound_totem`
- `xlib:demo_keepsake_sigil`

XLib no longer auto-restricts `minecraft:golden_apple` or `minecraft:diamond_sword` in dev runs. If you want recipe-lock testing, create it explicitly with `/xlib recipes restrict ...` or addon/datapack definitions.

Quick manual flow:

1. Run `/xlib items grant @s xlib:demo_loaner_blade` and inspect the result.
2. Run `/xlib items grant @s xlib:demo_bound_totem` and verify dropping is blocked.
3. Run `/xlib items grant @s xlib:demo_keepsake_sigil`, then revoke it, and verify the item remains because that fixture is keep-on-revoke.
4. Create a runtime recipe lock with `/xlib recipes restrict <recipe> false`, then test `grant`, `revoke`, and `inspect`.
5. Run `/xlib debug counters @s` after toggling modes or changing loadouts to confirm the runtime state looks correct.

## Current Limits

- There is no first-class named preset profile object yet.
- The built-in progression UI is still not a graph canvas.
- Ability-attributed kill progression still depends on the addon recording hits.
- `blockExternalStorage()` is not a universal hook for every possible third-party inventory system.

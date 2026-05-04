# XLib

XLib is a NeoForge `1.21.1` framework mod for combat systems, progression, source-tracked grants, and player or entity state systems.

It is built for addon mods and datapacks that want reusable runtime primitives instead of one-off storage, menu, sync, and inspection code.

## What XLib Provides

XLib already ships broad framework coverage for:

- active combat abilities with cooldowns, charges, charge-release flows, and resources
- passives, modes, forms, stances, and combo follow-up chains
- source-tracked grants for abilities, passives, managed items, recipes, bundles, identities, and profiles
- tracked values, trigger-driven tracked-value rules, and tracked-value-backed survival or food-bar replacement
- persistent profile groups with required onboarding and reset-aware selection state
- optional progression tracks, nodes, point types, counters, branch locks, and identity-gated follow-ups
- generalized state policies, state flags, detector windows, reactive triggers, and staged ability sequences
- capability policies for inventory, movement, crafting, held-item, menu, interaction, pickup, hotbar, and equipment restrictions
- synthetic entity classifications that let systems treat one entity as extra entity types or tag groups at runtime
- player damage modifier profiles with exact and tag-based incoming or outgoing scaling
- entity bindings, lifecycle stages, visual forms, and body transitions
- built-in or replaceable combat, progression, and onboarding UI
- bounded datapack JSON authoring for many gameplay surfaces
- admin, debug, export, diff, and content-inspection commands

## Current Project Shape

The current architecture is:

1. Java-registered core registries for runtime behavior
2. synced attachments for player and living-entity state
3. optional datapack JSON authoring layered on top of bounded systems
4. server-authoritative enforcement with synced client presentation state

Most addon-facing systems follow the same pattern:

- register or load definitions
- apply or project source-tracked state
- inspect the result through `/xlib`
- sanitize stale references on login, respawn, or reload

## Major Runtime Systems

### Combat and Ability Runtime

- `AbilityDefinition` / `AbilityRuntime`
- `PassiveDefinition` / `PassiveApi`
- `ModeDefinition` / `ModeApi`
- `ComboChainDefinition` / `ComboChainApi`
- `AbilityRequirements`
- `AbilitySequenceDefinition`
- `AbilityDetectorApi` / `ReactiveTriggerApi`

### Ownership and Unlock State

- `AbilityGrantApi`
- `GrantBundleApi`
- `IdentityApi`
- `DelegatedGrantApi`
- `ProfileApi`
- `ArtifactApi`
- `GrantedItemApi`
- `RecipePermissionApi`

### Progression

- `UpgradeApi`
- point types, consume rules, kill rules
- tracks, nodes, branch locks, choice groups
- reward projection back into the grant systems

### Values, Classification, and Survival

- `TrackedValueApi`
- `DataDrivenTrackedValueRuleApi`
- `EntityClassificationApi`
- `DamageModifierProfileApi`

### Entity and Form Systems

- `CapabilityPolicyApi`
- `EntityBindingApi`
- `LifecycleStageApi`
- `VisualFormApi`
- `BodyTransitionApi`

### Presentation and Input

- built-in combat HUD and ability menu
- built-in progression menu with list/tree layouts
- replaceable ability/progression/profile-selection screens
- replaceable combat HUD renderer
- authored control profiles and addon-owned key mappings
- backend-agnostic cue routing for animation or effect bridges

## Declarative Authoring

XLib now supports datapack JSON authoring for a large bounded subset of its systems.

Current data-driven surfaces include:

- conditions
- context grants
- grant bundles
- artifacts
- abilities
- passives
- identities
- profile groups
- profiles
- modes
- combo chains
- lifecycle stages
- capability policies
- visual forms
- tracked values
- tracked value rules
- damage modifier profiles
- progression point types, tracks, nodes, consume rules, and kill rules

The newer entity/form systems are no longer Java-only:

- `data/<namespace>/xlib/lifecycle_stages/*.json`
- `data/<namespace>/xlib/capability_policies/*.json`
- `data/<namespace>/xlib/visual_forms/*.json`
- `data/<namespace>/xlib/tracked_values/*.json`
- `data/<namespace>/xlib/tracked_value_rules/*.json`
- `data/<namespace>/xlib/damage_modifier_profiles/*.json`

XLib validates bounded cross-references at reload time and logs warnings for unresolved ids where appropriate.

## Content Inspection and Debugging

XLib ships a deeper command/debug surface than a normal content mod because it is meant to be a framework.

High-value command groups:

- `/xlib abilities ...`
- `/xlib passives ...`
- `/xlib items ...`
- `/xlib recipes ...`
- `/xlib profiles ...`
- `/xlib progression ...`
- `/xlib capability_policy ...`
- `/xlib classification ...`
- `/xlib bindings ...`
- `/xlib stages ...`
- `/xlib visual_form ...`
- `/xlib body ...`
- `/xlib debug state|export|diff|source|counters ...`
- `/xlib debug content ...`

The `debug content` branch exposes:

- `reference list|inspect`
- `conditions list|inspect`
- `abilities list|inspect`
- `passives list|inspect`
- `modes list|inspect`
- `lifecycle_stages list|inspect`
- `capability_policies list|inspect`
- `visual_forms list|inspect`
- `tracked_values list|inspect`
- `tracked_value_rules list|inspect`
- `damage_modifier_profiles list|inspect`
- and the rest of the shipped data-driven content surfaces

## Client Experience

Built-in client surfaces already include:

- combat bar / loadout UI
- player-authored mode preset editing
- passive browser inside the ability menu
- progression list view plus icon-node tree view
- profile-selection UI for required onboarding
- configurable resource HUD placement
- tracked-value food-bar replacement with custom survival behavior when configured

Addons can keep the built-in UI and customize around it, or replace the screens and HUD entirely.

## Optional Integrations

- `JEI` and `EMI` are optional runtime integrations for recipe-viewer support.
- `BLib` is an optional integration used only by the cue bridge under `com.whatxe.xlib.integration.blib`.
- Running XLib without BLib disables only the BLib `AzCommand` cue bridge. Core combat, grants, progression, entity systems, menus, and generic cue routing still work.

## Documentation

Start here:

- [docs/XLIB_USAGE_GUIDE.md](docs/XLIB_USAGE_GUIDE.md)
- [docs/wiki/Getting-Started.md](docs/wiki/Getting-Started.md)
- [docs/wiki/System-Overview-and-Status.md](docs/wiki/System-Overview-and-Status.md)
- [docs/wiki/Abilities-and-Loadouts.md](docs/wiki/Abilities-and-Loadouts.md)
- [docs/wiki/Modes-and-Combos.md](docs/wiki/Modes-and-Combos.md)
- [docs/wiki/Grants-Items-and-Recipes.md](docs/wiki/Grants-Items-and-Recipes.md)
- [docs/wiki/Progression.md](docs/wiki/Progression.md)
- [docs/wiki/Entity-and-Form-Systems.md](docs/wiki/Entity-and-Form-Systems.md)
- [docs/wiki/Tracked-Values-and-Survival.md](docs/wiki/Tracked-Values-and-Survival.md)
- [docs/wiki/Capability-Policies-and-Restrictions.md](docs/wiki/Capability-Policies-and-Restrictions.md)
- [docs/wiki/Synthetic-Classifications-and-Damage.md](docs/wiki/Synthetic-Classifications-and-Damage.md)
- [docs/wiki/Events-Commands-and-Testing.md](docs/wiki/Events-Commands-and-Testing.md)
- [docs/wiki/Declarative-JSON-Reference.md](docs/wiki/Declarative-JSON-Reference.md)
- [docs/wiki/Progression-JSON-Reference.md](docs/wiki/Progression-JSON-Reference.md)

Contributor map:

- [docs/CODEBASE_MAP.md](docs/CODEBASE_MAP.md)

GitHub wiki mirror helper:

- `tools/Sync-GitHubWiki.ps1`

## Development

- `.\gradlew.bat compileJava`
- `.\gradlew.bat test`
- `.\gradlew.bat runGameTestServer`

## Coordinates

- Mod ID: `xlib`
- Display name: `XLib`
- Maven group: `com.whatxe.xlib`

## License

XLib is released as `All Rights Reserved`.

No permission is granted to copy, modify, redistribute, sublicense, or create derivative works from this repository or its compiled artifacts without prior written permission from the copyright holder. See [LICENSE](LICENSE).

## Notes

- targets Java `21`
- targets NeoForge `21.1.217`
- targets Minecraft `1.21.1`

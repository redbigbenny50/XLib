# Events, Commands, and Testing

This page covers the public integration hooks, the built-in `/xlib` admin surface, and the recommended validation flow for addon authors.

## Public Events

Useful event surfaces include:

- `XLibAbilityActivationEvent`
- `XLibMenuOpenEvent`
- `XLibModeEvent.Ended` with explicit end reasons
- `XLibOutgoingDamageEvent`
- `XLibIncomingDamageEvent`
- `XLibCombatHitEvent`
- `XLibCombatMarkEvent`
- `XLibGrantedItemEvent.Reclaimed`
- `XLibGrantedItemEvent.Removed`
- `XLibRecipePermissionEvent`
- `XLibRuntimeCueEvent`
- `XLibProfileSelectionClaimEvent`
- `XLibProfileResetEvent`
- `XLibProfileOnboardingCompletedEvent`
- `XLibUpgradeRewardProjectionEvent.Projected`
- `XLibUpgradeRewardProjectionEvent.Cleared`

These exist so integrations do not need to poll attachments for recipe access changes, combat-mark lifecycles, dodge/parry/block hit resolution, granted-item cleanup, projected progression rewards, or runtime presentation timing.

## Client UI Registration

`RegisterAbilityClientRenderersEvent` now also acts as the client-side registration hook for custom ability/progression screens and full combat HUD replacement.

Useful registration paths:

- `registerAbilityMenuScreen(...)`
- `registerProgressionMenuScreen(...)`
- `registerProfileSelectionScreen(...)`
- `registerCombatHudRenderer(...)`
- `registerAbilityContainerLayout(...)`
- `registerLoadoutQuickSwitchHandler(...)`
- `registerControlKeyMapping(...)`
- `registerControlActionHandler(...)`
- `registerResourceHud(...)`
- `registerCustomIconRenderer(...)`

Use the screen/hud registrations when an addon wants a radial, round, grid, or otherwise fully custom ability UI instead of the built-in XLib screen/HUD geometry.

The screen registration layer now also accepts context-aware factories, so custom screens can receive:

- `AbilityMenuScreenContext`
- `ProgressionMenuScreenContext`

Those context objects carry the shared menu-session state instead of forcing every addon screen to rediscover selected slot, selected node, or layout mode from scratch.

Profile-selection registration now also accepts `ProfileSelectionScreenContext`, so required onboarding can keep the built-in selection screen or switch to a custom addon-owned flow without bypassing XLib's pending-group state.

Use `registerAbilityContainerLayout(...)` when you want the built-in HUD/menu to keep working but the primary combat bar should render as a strip, grid, radial/ring, or categorized group with slot metadata instead of the default linear placement.

If your addon wants the built-in `Cycle Loadout` keybind to appear in Controls, advertise that during addon init through `AbilityLoadoutFeatureApi` before client key mappings register. The quick-switch behavior itself is then supplied on the client through `registerLoadoutQuickSwitchHandler(...)`.

## Control Profile Registration

The primary combat bar can also advertise its input layer through authored control profiles.

Useful pieces:

- `AbilityControlProfileApi`
- `AbilityControlTrigger`
- `AbilityControlAction`
- `AbilityControlKeyMappingDefinition`
- `registerControlKeyMapping(...)`
- `registerControlActionHandler(...)`

Use `registerControlKeyMapping(...)` when you want a player-visible Controls entry backing one of your profile triggers. Use `registerControlActionHandler(...)` when your profile points at an addon-defined action type that XLib should dispatch back into your client code instead of treating as a built-in slot or selector action.

## Compatibility and Migration

Useful pieces:

- `AbilitySlotMigrationPlan`
- `AbilitySlotMigrationApi`
- `AbilitySlotContainerApi.isValidSlotReference(...)`
- `AbilitySlotContainerApi.hasResolvedContainer(...)`
- `ModPayloads.PLAY_PROTOCOL`

Use the migration layer when an addon needs to preserve or remap older fixed-slot data into the current primary-bar slot-reference model during an upgrade.

The built-in payload handlers now validate incoming slot/container references before mutating player data, so stale clients or stale addon layouts fail safely instead of writing into the wrong slot.

## Runtime Cue Hooks

`XLibCueApi` and `XLibRuntimeCueEvent` expose a backend-agnostic presentation hook layer for addon-side animation, VFX, or sound integrations.

Cue types currently include:

- activation start
- activation fail
- charge progress
- release
- hit confirm
- interrupt
- state enter
- state exit

Example sink registration:

```java
XLibCueApi.registerSink(adapterId, (player, data, cue) -> {
    if (cue.type() == XLibRuntimeCueType.HIT_CONFIRM) {
        // forward into your own backend here
    }
});
```

Surface-specific adapter registration is also available:

```java
XLibCueAdapterApi.registerAdapter(bodyAdapterId, XLibCueSurface.PLAYER_BODY_ANIMATION,
        (player, data, cue, surface) -> {
            // forward body-animation cues into your backend here
        });
```

Use `XLibCueRouteProfileApi` when body animation, model animation, and effect playback should route to different backends.

The core library still only emits and routes neutral cues. Concrete third-party backend implementations remain optional addon-side integrations.

## Mutable Damage Events

XLib now exposes public mutable player damage events for addon-side damage buffs, reductions, and counters:

- `XLibOutgoingDamageEvent`
- `XLibIncomingDamageEvent`

Example:

```java
NeoForge.EVENT_BUS.addListener((XLibOutgoingDamageEvent event) -> {
    if (event.player().isSprinting()) {
        event.setAmount(event.amount() * 1.15F);
    }
});
```

Incoming events fire for player defenders. Outgoing events fire for player attackers. Both can mutate the final amount before XLib's player-side hurt/resource hooks finish.

## Reaction State

Recent-hit / parry-window style kits can now use the combat-reaction attachment instead of hand-rolling their own timer memory.

Useful APIs:

- `CombatReactionApi.recentlyHurtWithin(player, ticks)`
- `CombatReactionApi.lastAttacker(player)`
- `CombatReactionApi.lastDamage(player)`
- `AbilityRequirements.recentlyHurtWithin(ticks)`

## Reactive Runtime Windows

The runtime now also exposes a neutral event vocabulary for authored detector windows and reactive responses.

Useful APIs:

- `ReactiveRuntimeEvent`
- `AbilityDetectorApi`
- `ReactiveTriggerApi`
- `AbilitySequenceDefinition`

This is the layer to use when your addon wants "after being hurt, open a 20-tick counter window" or "after hit confirm, advance into the next authored stage" without building a separate attachment or timer system from scratch.

## Admin and Debug Commands

XLib ships `/xlib` command trees for:

- abilities
- passives
- items
- recipes
- profiles
- progression
- capability_policy
- bindings
- stages
- visual_form
- body
- debug

High-value commands:

- `/xlib passives catalog`
- `/xlib passives describe <passive>`
- `/xlib passives inspect <player>`
- `/xlib items grant|revoke|clear|list|inspect|sources ...`
- `/xlib profiles catalog|groups`
- `/xlib profiles claim|reset|reopen|list|pending|resync ...`
- `/xlib recipes restrict|unrestrict|grant|revoke|clear|list|inspect|sources ...`
- `/xlib progression unlock|revoke|track revoke|clear|inspect ...`
- `/xlib capability_policy apply|revoke|clear|list ...`
- `/xlib bindings list|unbind|clear ...`
- `/xlib stages set|clear|get|transition ...`
- `/xlib visual_form apply|revoke|clear|get ...`
- `/xlib body return|clear|get ...`
- `/xlib debug state <player>`
- `/xlib debug counters <player>`
- `/xlib debug source <player> <source>`
- `/xlib debug export <player>`
- `/xlib debug diff <player> <snapshot>`
- `/xlib debug content reference list`
- `/xlib debug content <system> list`
- `/xlib debug content <system> inspect <id>`

Notes:

- `/xlib abilities grant` uses the `xlib:command` source and acts as a practical admin override for normal view, assignment, and activation requirements.
- `/xlib abilities revoke` now also checks for matching unlocked progression-node rewards and revokes those backing nodes first when needed.
- `/xlib passives catalog|describe|inspect` now expose passive metadata, requirement descriptions, authored hooks, sound triggers, and current per-player grant/active status.
- `/xlib profiles ...` now exposes registered profile groups, registered profiles, current pending-group state, and safe admin flows for claim/reset/reopen/resync without addon authors having to mutate attachments directly.
- `/xlib recipes restrict` applies runtime recipe restrictions immediately and resyncs online players plus already-open crafting menus.
- `/xlib recipes inspect` reports effective metadata including tags, categories, outputs, advancements, and output NBT when present.
- `/xlib capability_policy ...`, `/xlib bindings ...`, `/xlib stages ...`, `/xlib visual_form ...`, and `/xlib body ...` expose the newer entity/form runtime systems directly instead of forcing attachment edits or custom debug code.
- `/xlib debug content ...` is the content-authoring inspection surface for registered and datapack-defined reference topics, conditions, context grants, equipment bindings, grant bundles, artifacts, abilities, passives, identities, support packages, profile groups, profiles, modes, combo chains, lifecycle stages, capability policies, visual forms, and progression definitions.
- `/xlib debug state` now also reports detector windows, active/unlocked artifacts, identities, grant bundles, selected profiles, and pending profile groups; `/xlib debug source` includes structured ownership descriptors plus grouped identities/bundles/artifact unlocks by source id; and `/xlib debug export` now includes `detector_states`, `artifact_states`, `artifact_unlock_sources`, `identity_states`, `grant_bundle_states`, `grant_bundle_sources`, `selected_profiles`, `pending_profile_groups`, `profile_selection_data`, `managed_unlock_sources`, `source_descriptors`, and the earlier passive/mode/state sections in the JSON snapshot.

## Recommended Testing Flow

1. Unit-test pure state logic around abilities, grants, recipes, progression, slot migration, and protocol compatibility.
2. Add GameTests for real runtime flows like activation, kill tracking, item cleanup, crafting locks, and onboarding/profile flows where your addon depends on them.
   The shipped runtime suite now also covers support-package ally grants, controlled-entity ownership or command state, and branching progression with identity-gated follow-up nodes.
3. Run:

```powershell
.\gradlew.bat test
.\gradlew.bat runGameTestServer
```

## IDE Dev Fixtures

XLib no longer ships built-in IDE/demo gameplay fixtures. If you want manual validation content, register your own dev-only abilities, profiles, items, and progression in your addon or datapack.

XLib also no longer auto-restricts `minecraft:golden_apple` or `minecraft:diamond_sword` in dev runs. If you want recipe-lock testing, create it explicitly with `/xlib recipes restrict ...` or addon/datapack definitions.

## Current Limits

- There is no first-class named preset-loadout profile object yet; the shipped loadout backend is still attachment-driven rather than a higher-level named loadout registry.
- The built-in progression UI is still not a free-pan/zoom canvas.
- Ability-attributed kill progression still depends on the addon recording hits.
- `blockExternalStorage()` is not a universal hook for every possible third-party inventory system.

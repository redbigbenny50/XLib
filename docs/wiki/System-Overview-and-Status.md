# System Overview and Status

This page is the quickest "what already exists today?" summary for the whole library.

## Current Architecture

- XLib bootstraps registry-style APIs for abilities, passives, modes, combos, context grants, combat marks, combat reaction, granted items, recipes, progression, attachments, payloads, and commands.
- Player state lives in synced attachments: `AbilityData` for combat, loadout, grant, and runtime state, `ProfileSelectionData` for persistent profile/onboarding state, `UpgradeProgressData` for progression, `CombatMarkData` for living-entity marks, and `CombatReactionData` for recent-hit state.
- The server is authoritative for activation, cooldown and charge ticking, grant syncing, item cleanup, recipe permission sync, and progression reward projection.
- The client mainly renders synced state and sends intent payloads for ability activation, loadout assignment, and progression unlocks.

## What Already Ships

- Active ability runtime with cooldowns, charges, charge-release helpers, resource costs, requirement hooks, sound hooks, dynamic slot-container loadout resolution, and container-aware activation payloads.
- Composable requirement adapters that let the same gating logic flow through ability definitions, contextual grants, item or unlock conditions, and progression consume rules.
- Neutral `family/group/page/tag` metadata across abilities, passives, modes, granted items, restricted recipe content, upgrade tracks, and upgrade nodes.
- Metadata lookup helpers now span `AbilityApi`, `PassiveApi`, `ModeApi`, `GrantedItemApi`, `RecipePermissionApi`, and `UpgradeApi`, while the built-in ability and progression screens now expose metadata-aware search, page/group/family scoping, and detail views for the most relevant catalog slices.
- Passive definitions now expose authored hook/sound-trigger introspection, and the built-in command/debug surface now has dedicated passive catalog/describe inspection plus focused active passive/mode state summaries.
- Modes, forms, and stances with overlays, exclusivity, transforms, stackable layers, cycle groups, ordered cycle steps, upkeep, bundled grants or blocks, and cooldown scaling.
- Detector-driven reactive windows, authored response rules, staged multi-step ability sequences, and runtime event dispatch for hit, hurt, kill, jump, block-break, armor-change, item-consume, activation, fail, and end flows.
- Generalized state policies with selector-based targeting, source-tracked projection, cooldown multipliers, lock/silence/suppression control, and mode/context integration for overdrive-style or danger-state mechanics.
- Generic named state flags with source-tracked projection, mode/context integration, requirement helpers, and debug/export visibility for addon-defined runtime state vocabulary beyond hard-control policies.
- Identity/origin definitions with inheritance, source-tracked identity state, and bundle projection for long-lived archetypes or lineage-style traits.
- Persistent profile groups, authored incompatibility rules, required-choice onboarding triggers, reset history, and source-tracked projection into identities, bundles, abilities, passives, recipes, state flags, unlocked artifacts, and progression starting nodes.
- Transferable grant bundles, delegated bundle grants, and structured ownership descriptors for who granted a power, why it is active, and when it should disappear.
- Artifact/equipment access definitions with unlock-on-consume support, presence checks across inventory or equipped slots, active/unlocked bundle projection, artifact-aware ability or menu requirements, and debug/export visibility for unlock sources and current artifact state.
- Ally-targeted support packages, persistent entity-relationship ownership, and controlled-entity command helpers for helper, summon, companion, or bonded-target designs.
- Combo chains with activation, hit-confirm, or end or release triggers plus branch conditions and slot transforms.
- Backend-agnostic runtime cue hooks for animation/effect adapters, with neutral activation/fail, charge, release, hit-confirm, interrupt, and state-transition cues plus a public event surface.
- Surface-aware cue routing with optional adapter registries for player-body animation, model animation, and effect playback backends.
- Dynamic slot containers, source-owned container visibility/editability, addon-controlled loadout feature gating, authored control profiles, addon-registered control key mappings, and optional loadout quick-switch hooks, so addons can decide both which combat surfaces exist and how they are activated.
- Combat helpers: targeting profiles, hit-resolution hooks, reusable action helpers, combat marks, recent-hit reaction tracking, and mutable incoming or outgoing damage events.
- Source-tracked grant systems for abilities, passives, granted items, and recipe permissions, plus contextual and item-driven grant providers.
- Source-tracked debug/export surfaces now also cover active state policies, active state flags, their source ownership, and grouped source summaries.
- Source-tracked debug/export surfaces now also cover identities, active grant bundles, `grant_bundle_sources`, and structured `source_descriptors` for ownership review.
- Managed granted items with revoke cleanup, keep-on-revoke behavior, undroppable behavior, owner markers, reclaim flows, and external-storage policies.
- Recipe restrictions by exact id or selector, advancement-backed auto-unlocks, crafting-result enforcement, recipe-book sync, and optional JEI or EMI bridges.
- Optional progression with point types, counters, tracks, nodes, composite requirements, exclusive tracks, choice-group branch commitment, explicit node or track locks, identity-gated follow-up paths, and reward projection back into the core grant systems.
- Built-in client UI: combat bar overlay, resource HUD, a remappable combat-bar toggle keybind, ability and loadout screen, progression screen, and a required profile-selection screen, with addon-defined menu/HUD presentation profiles, list mode plus an icon-node tree skill-tree progression layout, gameplay-focused built-in detail panels, per-resource HUD anchor/offset/name/value control through `AbilityResourceHudLayout`, access-aware hidden/locked/available menu policies, first-class replacement hooks for custom ability/progression/profile-selection screens plus custom combat HUD renderers, and shared menu-session-state/context handoff for custom screen navigation.
- Built-in client UI now also includes shared menu-session-state and context-aware factory handoff, so custom ability/progression screens can preserve selected slot, loadout target, selected track/node, and layout mode across surface switches.
- Built-in client UI now also includes `AbilityContainerLayoutApi`, `AbilitySlotWidgetMetadata`, and `AbilitySlotLayoutPlanner`, so the default HUD and ability menu can render strip/grid/radial/categorized container layouts with authored anchors, grouped headers, page tabs, slot labels, role hints, input hints, and soft locks without forcing full UI replacement.
- Compatibility/migration helpers now include `AbilitySlotMigrationApi`, payload validation for container ids and slot references, `XLibMenuOpenEvent`, and profile claim/reset/onboarding lifecycle events for safer addon upgrades plus clearer extension hooks.
- Built-in `/xlib` admin and debug commands plus both JUnit coverage and runtime GameTests.

## Runtime Flow

1. `AbilityLibrary.bootstrap(...)` registers the framework registries, attachments, payloads, commands, reload listeners, and IDE fixtures.
2. Player login and container hooks install recipe and granted-item guards, sanitize attachments, evaluate required profile onboarding, and resync progression and recipe state.
3. Per-player server ticks run ability runtime, passive runtime, granted-item runtime, and recipe-result enforcement.
4. Dynamic grant sync pulls from items, contextual providers, active modes, and unlocked progression sources into the same source-tracked ownership model.
5. Client menus and the combat HUD read the synced attachments, while activation, assignment, and unlock requests travel back to the server through payloads.

## Roadmap Snapshot

- Completed foundation: neutral `family`, `group`, `page`, and `tag` metadata is now live for abilities, passives, modes, granted items, restricted recipe content, upgrade tracks, and upgrade nodes.
- Completed foundation: requirement composition and adapter helpers now let addon authors reuse one `AbilityRequirement` definition across ability, contextual-grant, and consume-rule authoring.
- Completed foundation: passive definitions and active mode state now have dedicated discoverability surfaces through passive catalog/describe inspection, passive state summaries, and focused `/xlib debug state` output.
- Completed foundation: detector/reactive authoring now exists through `AbilityDetectorApi`, `ReactiveTriggerApi`, and `ReactiveRuntimeEvent`, while staged multi-step abilities can be authored through `AbilitySequenceDefinition` and `AbilityDefinition.sequence(...)`.
- Completed foundation: the built-in ability menu now has dedicated page/group/family presentation for large catalogs, and built-in ability/progression screens now respect addon-authored hidden/locked/available menu policies.
- Completed foundation: built-in menus and the combat HUD now expose addon-defined presentation profiles, and the progression screen now ships list mode plus an icon-node tree skill-tree presentation style through a reusable layout planner.
- Completed foundation: client UI replacement hooks now exist through `AbilityMenuScreenFactoryApi`, `ProgressionMenuScreenFactoryApi`, and `CombatHudRendererApi`, so addons can replace the built-in ability/progression screens or combat HUD instead of only recoloring them.
- Completed foundation: Roadmap 2 Area 1 is now effectively covered, because the screen-factory layer now includes `AbilityMenuScreenFactory` / `ProgressionMenuScreenFactory` plus shared `AbilityMenuSessionStateApi` / `ProgressionMenuSessionStateApi` context handoff for custom screen navigation.
- Completed foundation: Roadmap 2 Area 2 is now covered through `AbilitySlotReference`, `AbilityContainerState`, `AbilitySlotContainerDefinition`, and `AbilitySlotContainerApi`, so runtime state, payloads, combos, overlays, HUD rendering, and built-in ability-menu assignment all work against multi-container slot layouts instead of one fixed strip.
- Completed foundation: Roadmap 2 Area 3 is now covered through `AbilityControlProfileApi`, `AbilityControlTrigger`, `AbilityControlAction`, `AbilityControlKeyMappingApi`, and `AbilityControlActionHandlerApi`, so addons can author container-specific default controls, expose optional Controls entries, and route custom input actions without forking the whole client pipeline.
- Completed foundation: addon-controlled loadout management and optional quick-switch hooks now exist through `AbilityLoadoutFeatureApi` plus `RegisterAbilityClientRenderersEvent.registerLoadoutQuickSwitchHandler(...)`, so addons can opt into those built-in affordances instead of forcing them on every integration.
- Completed foundation: Roadmap 2 Area 4 is now covered through `ProfileGroupDefinition`, `ProfileDefinition`, and `ProfileApi`, so addons can author persistent profile groups, incompatibility rules, and projection into the existing grant/progression layers instead of hand-rolling separate identity-choice storage.
- Completed foundation: Roadmap 2 Area 5 is now covered through `ProfileSelectionData`, `ProfileSelectionScreenFactoryApi`, `ClaimProfilePayload`, and the onboarding hooks in `ModPlayerEvents`, so XLib can now open a required selection flow on authored triggers and block ability/menu/progression access until a required choice is claimed.
- Completed foundation: Roadmap 2 Area 6 is now covered through profile reset policy flags, `/xlib profiles ...` admin commands, selection-origin tracking, reset-history storage, and expanded debug/export sections for pending profile groups plus managed unlock sources.
- Completed foundation: Roadmap 2 Area 7 is now covered through `AbilityContainerLayoutDefinition`, `AbilityContainerLayoutApi`, `AbilitySlotWidgetMetadata`, and `AbilitySlotLayoutPlanner`, so built-in HUD/menu surfaces can consume authored strip/grid/radial/categorized layouts, page tabs, grouped headers, and per-slot metadata without forcing full UI replacement.
- Completed foundation: Roadmap 2 Area 8 is now covered through `AbilitySlotMigrationApi`, stricter payload validation in `ModPayloads`, explicit menu/profile lifecycle events, and expanded tests around layout registration, migration remapping, and play-protocol compatibility.
- Completed foundation: backend-agnostic runtime cue hooks now exist through `XLibCueApi` and `XLibRuntimeCueEvent`, and the core runtime emits cues for activation, charge-release progress, hit-confirm, interruption, and mode state transitions.
- Completed foundation: optional cue-adapter routing now exists through `XLibCueAdapterApi`, `XLibCueRouteProfileApi`, and `XLibCueSurface`, so body animation, model animation, and effect playback can be handled by separate addon integrations.
- Completed foundation: generalized state policies now broaden the mode/form system with selector-based cooldown scaling plus lock/silence/suppression control that can be projected by modes or contextual systems.
- Completed foundation: the generalized state model now also includes source-tracked `StateFlagApi` markers so addons can project named runtime flags through modes/context sources and gate logic with `AbilityRequirements.stateFlagActive(...)` without forcing everything into the harder-control policy path.
- Completed foundation: `GrantBundleApi`, `IdentityApi`, `DelegatedGrantApi`, and `GrantOwnershipApi` now cover transferable bundle packages, inherited identity bundles, revocable delegated powers, and structured ownership/access inspection.
- Completed foundation: `ArtifactDefinition`, `ArtifactApi`, artifact-aware requirements, and menu visibility helpers now cover authored equipment/access-policy content on top of the older item-grant and unlock layers.
- Completed foundation: `SupportPackageApi`, `EntityRelationshipApi`, and `ControlledEntityApi` now cover ally-targeted bundle projection, persistent relationship ownership, and simple summon or minion control helpers.
- Completed foundation: the optional progression layer now supports choice groups, explicit node or track locks, identity rewards, and identity-gated follow-up paths for stronger branch commitment and archetype progression.
- Strong groundwork already exists for modes, source ownership, item-driven access, profile/onboarding state, recipe gating, progression, and UI.
- The first bundled roadmap and all of Roadmap 2 are now implemented; follow-on work is now about higher-level authoring UX and optional tooling rather than missing slot/container/profile foundations.

## Known Remaining Gaps

- XLib still does not ship a first-class named multi-loadout profile object, even though addons can now gate the built-in loadout UI, use the shipped multi-container/layout backend, and advertise their own quick-switch behavior.
- Control profiles can now route through addon-owned key mappings, but XLib still does not ship a separate in-framework bind-editor surface beyond normal Controls registration.
- XLib now ships strong built-in ability-menu/HUD composition, but highly custom profile-selection visuals still work best as a full custom screen instead of as a richer reusable widget layer.
- The built-in progression screen supports list mode plus an icon-node tree skill-tree view, but it is still a bounded menu surface rather than a free-pan or zoomable canvas.
- Follow-on planning for those higher-level UX/tooling gaps now lives in `ROADMAP_3.md`.

This page should stay aligned with `ROADMAP.md` and `docs/CODEBASE_MAP.md` whenever new systems land.

# XLib Roadmap

This file is the dedicated repository summary of XLib's first bundled future-direction pass.

It represents the active bundled scope discussed so far as of 2026-04-06. It is a planning document, not a locked specification, and it can expand later if new requirements are added.

The checklist below has been audited against the current repository contents so shipped foundations are checked off and the current planned areas are fully covered.

## Core Direction

- Keep XLib positioned as a neutral, reusable gameplay framework rather than a genre-locked content pack.
- Prefer user-extensible authoring systems over framework-owned theme semantics.
- Grow existing systems into broader runtime, presentation, access-control, and progression layers instead of adding narrow one-off mechanics.
- Deliver the roadmap in phases instead of one monolithic implementation pass.
- Keep roadmap framing source-agnostic and profile-agnostic so the same systems remain reusable across many addon designs.

## Planned Areas

### 1. Generic Authoring and Runtime Expansion

- [x] Neutral addon-defined catalog metadata is live for abilities, passives, modes, granted items, restricted recipe content, upgrade tracks, and upgrade nodes.
- [x] Public metadata lookup helpers now exist across `AbilityApi`, `PassiveApi`, `ModeApi`, `GrantedItemApi`, `RecipePermissionApi`, and `UpgradeApi`, and the built-in ability/progression menus now surface the currently relevant metadata search and detail views.
- [x] Detector and reactive-trigger infrastructure so addons can define their own event windows and response rules.
- [x] Staged or sequence-driven ability authoring for richer multi-step actions.
- [x] Requirement adapters and related authoring helpers that reduce boilerplate without hardcoding theme-specific content categories.
- [x] Better passive and state discoverability beyond the new metadata layer so complex kits remain readable and authorable, including passive catalog/describe inspection and focused passive/mode state debug summaries.

### 2. Generalized State and Control Systems

- [x] XLib already ships a strong mode/form/stance layer with stackable modes, transform parents, exclusivity, cycle groups, ordered cycle steps, upkeep, overlays, bundled grants or blocks, and cooldown scaling.
- [x] Broaden the current mode/form system into a richer generalized state framework.
- [x] Treat overdrive-style mechanics as presets or policies within that broader state system rather than as a standalone special case.
- [x] Support stronger addon-defined lock, silence, seal, suppression, and partial-block behaviors for powers and abilities beyond today's activation-block and requirement layers.
- [x] Keep broad addon-style flexibility while expanding the state model further beyond the current mode-plus-policy layer.

### 3. UI and Presentation

- [x] Built-in combat bar, resource HUD, ability menu, and progression menu are already part of XLib core.
- [x] The current ability and progression screens already expose metadata, search or filtering, exclusive-track filtering, and requirement or reward details.
- [x] More customizable menu, HUD, and presentation surfaces for addon-defined organization and styling.
- [x] Better grouping, family, and page presentation for large ability sets.
- [x] Multiple progression presentation styles, including a more explicit graph or tree flow where appropriate.
- [x] Access-aware UI policies so menus can be hidden, visible-but-locked, or fully available depending on player state.

### 4. Persistent Identity and Power Ownership

- [x] Source-tracked ownership already exists for abilities, passives, granted items, recipes, contextual grants, modes, and projected progression rewards.
- [x] Identity/profile-style framework support for long-lived player states, inherited traits, or other addon-defined persistent classifications.
- [x] Transferable or projected grant bundles for cases where one entity shares powers with another.
- [x] Temporary delegated powers that can be revoked by the source instead of only persistent inheritance-style grants.
- [x] Clearer ownership and access modeling around who granted a power, why it is active, and when it should disappear.

### 5. Item, Equipment, and Access Policy Systems

- [x] Temporary equipment-bound power packages already exist through `AbilityGrantingItem`, contextual grant providers, and runtime source sync.
- [x] Permanent unlock items already exist through `AbilityUnlockItem`.
- [x] Managed granted items already support owner markers, undroppable behavior, keep-on-revoke behavior, and external-storage policies.
- [x] A broader item/equipment/access-policy layer built on top of current item-grant and unlock support.
- [x] Menu and system visibility rules tied to equipment, item unlock state, identity, or other access conditions.
- [x] Treat item-driven access as a more explicit authored framework layer instead of several adjacent APIs.

### 6. External Entity Support and Controlled Agents

- [x] Relationship-targeted grant and support-package runtime for buffs, linked powers, and externally projected designs.
- [x] Entity relationship and ownership systems for addon-defined controller, linked-entity, summoned-entity, or bonded-entity patterns.
- [x] Controlled-entity frameworks for authors building command-driven or source-owned external actors.

### 7. Progression and Branching Specialization

- [x] The optional progression module already ships point types, counters, tracks, nodes, composite requirements, exclusive tracks, consume or kill rules, and reward projection back into XLib grants.
- [x] The current progression layer already supports both simple track gating and more structured node-based trees.
- [x] Richer graph-native progression with deeper branching specialization paths.
- [x] Choice nodes, branch locks, and stronger path exclusivity where addons want meaningful build commitment.
- [x] Better support for persistent profiles, specialization families, and unlock paths that affect later progression options.

### 8. Animation and Effects Integration

- [x] Add generic runtime visual and effect cue hooks in XLib core.
- [x] Emit neutral cues such as activation start, charge progress, release, hit confirm, interruption, and state enter or exit.
- [x] Keep core library support backend-agnostic rather than hard-depending on one animation stack.
- [x] Provide optional adapters or integration modules for third-party animation and effect backends when needed.
- [x] Treat player-body animation, model animation, and effect playback as separate capability surfaces that may require different adapters.

## Delivery Notes

- The roadmap should be implemented in phases with checkpoints, tests, docs, and commits or archives between major batches.
- The goal is to preserve design intent while reducing context pressure, rollback risk, and architectural drift.
- This file exists so the current future scope stays easy to find in one place.
- The follow-on planning pass now lives in `ROADMAP_2.md`, which captures the second bundled framework pass after this first roadmap was completed.

# Synthetic Classifications and Damage

This page covers two newer runtime systems that often work together:

- synthetic entity classifications
- source-tracked damage modifier profiles

Use them when your addon needs runtime identity changes such as "treat this player as mob X" or reusable vulnerability/resistance layers such as "this source takes extra fire damage and ignores poison."

## Synthetic Entity Classifications

Synthetic classifications let XLib treat one entity as if it had extra entity identities or tag groups without changing the entity's real vanilla type.

Typical uses:

- disguises
- species or faction hostility
- mutation/infection identity
- "treat this player as a xenomorph-like target"
- target filters that should react to more than real entity type

### Core Pieces

- `EntityClassificationData`
  Entity attachment storing source-tracked synthetic entity types and synthetic tags.
- `ResolvedEntityClassificationState`
  Cached merged classification state.
- `EntityClassificationApi`
  Apply/revoke/clear/list helpers plus merged selector checks.
- `EntityClassificationCompatApi`
  Compat-facing change hook surface for integrations that want to mirror classification changes into another system.
- `EntityClassificationMatchMode`
  Real-only, synthetic-only, or merged matching behavior.

### What Counts As A Classification

The system supports two main shapes:

- synthetic entity types
- synthetic tags

That lets one entity count as:

- a specific mob id
- a group of mob tags
- both at once

### Core API

Typical operations:

- grant synthetic entity type
- revoke synthetic entity type
- grant synthetic tag
- revoke synthetic tag
- clear one source
- clear all
- query effective entity types or tags
- ask whether an entity matches a selector under real-only, synthetic-only, or merged rules

### Runtime Consumers

The classification layer is not just storage. XLib now uses merged real-plus-synthetic selector matching in several runtime systems, including:

- tracked-value rule target matching
- upgrade kill rules
- interaction-policy entity matching
- resource behavior helpers that filter by killed entity identity
- requirement and debug surfaces

That means an entity can behave as "counts as X" inside multiple XLib systems without each addon hand-rolling a parallel identity layer.

### Commands and Debugging

Useful surfaces:

- `/xlib classification grant_entity_type ...`
- `/xlib classification revoke_entity_type ...`
- `/xlib classification grant_tag ...`
- `/xlib classification revoke_tag ...`
- `/xlib classification clear_source ...`
- `/xlib classification clear_all ...`
- `/xlib classification list ...`
- `/xlib debug state <player>`
- `/xlib debug export <player>`

The list/debug output includes effective merged identity and tag state, not just raw synthetic grants.

### Current Limits

Synthetic classifications give XLib a generic "counts as" layer. They do not automatically force every external mod to respect that layer. Other mods need:

- an integration bridge
- a hook
- or addon-side compatibility code

`EntityClassificationCompatApi` exists so those bridges have a stable change-notification and query surface.

## Damage Modifier Profiles

Damage modifier profiles are reusable, source-tracked damage scaling definitions for player attackers and defenders.

Use them when your addon needs reusable framework-side damage behavior such as:

- fire vulnerability
- poison immunity
- bullet resistance
- outgoing bonus damage against a custom source family

### Core Pieces

- `DamageModifierProfileDefinition`
  Authored profile definition.
- `DamageModifierProfileData`
  Player attachment data for active profile sources.
- `DamageModifierProfileApi`
  Register/grant/revoke/query helpers and resolved runtime matching.
- `DataDrivenDamageModifierProfileApi`
  Datapack authoring under `data/<namespace>/xlib/damage_modifier_profiles/*.json`.

### Matching Model

Profiles can match by:

- exact damage type id
- damage type tag

for both:

- incoming damage
- outgoing damage

Matching multipliers stack multiplicatively:

- `0.0` means immunity
- values between `0.0` and `1.0` reduce damage
- values above `1.0` increase damage

### Runtime Behavior

Profiles are applied inside XLib's player damage pipeline before the public mutable incoming/outgoing damage events finish resolving.

That means you can use profiles for reusable authored defaults and still layer explicit event listeners on top when an addon needs custom one-off logic.

### Commands and Debugging

Damage profiles do not currently have a dedicated top-level admin subtree like capabilities or classifications. The main inspection surfaces are:

- `/xlib debug content damage_modifier_profiles list`
- `/xlib debug content damage_modifier_profiles inspect <id>`
- `/xlib debug state <player>`

Profiles can also be granted or revoked by addon code, tracked-value rules, or other authored state projection systems.

### Datapack Authoring

Damage modifier profiles can be authored under:

- `data/<namespace>/xlib/damage_modifier_profiles/*.json`

The datapack layer supports both:

- flat incoming/outgoing selector keys
- nested `incoming` and `outgoing` sections

Use [Declarative JSON Reference](Declarative-JSON-Reference.md) for the full schema.

## When To Use These Systems Together

These two systems pair well when:

- identity affects hostility and damage taken
- one form/species should count as a different target class and also have a vulnerability profile
- progression or tracked-value rules should change both runtime identity and damage response together

That is the common "become creature X and inherit its combat weaknesses/strengths" path.

# Progression JSON Reference

This file is generated from the bounded progression authoring surfaces shipped by XLib.
It is meant to stay aligned with the engine-owned readers instead of drifting into prose-only notes.

## Upgrade Requirement JSON

- Topic id: `upgrade_requirements`
- Authoring location: inline under `requirement` or `requirements` fields in progression-authored content
- Note: Boolean `true` and `false` are accepted shorthands for always and never.
- Note: JSON arrays are treated as `all(...)` requirement groups.
- Note: Nested parser errors report JSON paths like `$.requirements[1].requirement`.

### `always`

Always passes.

### `never`

Never passes.

### `all`

Every child requirement must pass.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `requirements` | `array<upgrade_requirement>` | yes | `-` | At least one child required. |

### `any`

At least one child requirement must pass.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `requirements` | `array<upgrade_requirement>` | yes | `-` | At least one child required. |

### `not`

Inverts one child requirement.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `requirement` | `upgrade_requirement` | yes | `-` | Exactly one nested child. |

### `advancement`

Requires a vanilla advancement id.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `advancement` | `resource_location` | yes | `-` | Namespaced advancement id. |

### `counter_at_least`

Requires a progression counter minimum.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `counter` | `resource_location` | yes | `-` | Counter id. |
| `amount` | `int` | yes | `-` | Minimum inclusive value. |

### `points_at_least`

Requires points in one progression point type.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `point_type` | `resource_location` | no | `-` | Preferred field name. |
| `point` | `resource_location` | no | `-` | Backward-compatible alias for `point_type`. |
| `amount` | `int` | yes | `-` | Minimum inclusive value. |

### `node_unlocked`

Requires one specific node to be unlocked.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `node` | `resource_location` | yes | `-` | Upgrade node id. |

### `any_node_unlocked`

Requires at least one node from a set to be unlocked.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `node` | `resource_location` | no | `-` | Optional single-node shorthand. |
| `nodes` | `array<resource_location>` | no | `-` | Plural form merged with `node`. |

### `identity_active`

Requires one active identity id.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `identity` | `resource_location` | yes | `-` | Identity id. |

### `track_completed`

Requires one completed track id.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `track` | `resource_location` | yes | `-` | Upgrade track id. |

## Upgrade Point Type Definitions

- Topic id: `upgrade_point_types`
- Authoring location: `data/<namespace>/xlib/upgrade_point_types/*.json`
- Note: Point-type definitions are intentionally minimal; the file mostly exists to declare a stable id.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `id` | `resource_location` | no | `file id` | If omitted, the datapack file id becomes the runtime id. |

## Upgrade Track Definitions

- Topic id: `upgrade_tracks`
- Authoring location: `data/<namespace>/xlib/upgrade_tracks/*.json`
- Note: Singular and plural id fields are merged where both exist.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `id` | `resource_location` | no | `file id` | If omitted, the datapack file id becomes the runtime id. |
| `family` | `resource_location` | no | `-` | Optional track family metadata. |
| `group` | `resource_location` | no | `-` | Optional track group metadata. |
| `page` | `resource_location` | no | `-` | Optional presentation page metadata. |
| `tag` | `resource_location` | no | `-` | Optional single track tag. |
| `tags` | `array<resource_location>` | no | `-` | Additional track tags. |
| `root_node` | `resource_location` | no | `-` | Optional single root node id. |
| `root_nodes` | `array<resource_location>` | no | `-` | Additional root node ids. |
| `exclusive_track` | `resource_location` | no | `-` | Optional single mutually-exclusive track id. |
| `exclusive_tracks` | `array<resource_location>` | no | `-` | Additional mutually-exclusive track ids. |

## Upgrade Node Definitions

- Topic id: `upgrade_nodes`
- Authoring location: `data/<namespace>/xlib/upgrade_nodes/*.json`
- Note: `requirement` and `requirements` both accept the shared upgrade requirement JSON vocabulary.
- Note: `point_costs`, `point_rewards`, and `counter_rewards` must use positive integer values.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `id` | `resource_location` | no | `file id` | If omitted, the datapack file id becomes the runtime id. |
| `track` | `resource_location` | no | `-` | Owning track id. |
| `family` | `resource_location` | no | `-` | Optional node family metadata. |
| `group` | `resource_location` | no | `-` | Optional node group metadata. |
| `page` | `resource_location` | no | `-` | Optional node page metadata. |
| `tag` | `resource_location` | no | `-` | Optional single node tag. |
| `tags` | `array<resource_location>` | no | `-` | Additional node tags. |
| `choice_group` | `resource_location` | no | `-` | Optional mutually-exclusive choice group id. |
| `point_costs` | `object<resource_location,int>` | no | `-` | Point type ids mapped to positive costs. |
| `required_node` | `resource_location` | no | `-` | Optional single prerequisite node. |
| `required_nodes` | `array<resource_location>` | no | `-` | Additional prerequisite nodes. |
| `locked_node` | `resource_location` | no | `-` | Optional single node locked by this one. |
| `locked_nodes` | `array<resource_location>` | no | `-` | Additional nodes locked by this one. |
| `locked_track` | `resource_location` | no | `-` | Optional single track locked by this one. |
| `locked_tracks` | `array<resource_location>` | no | `-` | Additional tracks locked by this one. |
| `requirement` | `upgrade_requirement` | no | `-` | Optional single inline requirement. |
| `requirements` | `array<upgrade_requirement>` | no | `-` | Additional inline requirements. |
| `rewards` | `object` | no | `empty bundle` | Reward bundle object with bounded child fields. |

### `rewards`

Bounded reward bundle object.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `ability` | `resource_location` | no | `-` | Optional single granted ability id. |
| `abilities` | `array<resource_location>` | no | `-` | Additional granted ability ids. |
| `passive` | `resource_location` | no | `-` | Optional single granted passive id. |
| `passives` | `array<resource_location>` | no | `-` | Additional granted passive ids. |
| `granted_item` | `resource_location` | no | `-` | Optional single granted item id. |
| `granted_items` | `array<resource_location>` | no | `-` | Additional granted item ids. |
| `recipe_permission` | `resource_location` | no | `-` | Optional single granted recipe-permission id. |
| `recipe_permissions` | `array<resource_location>` | no | `-` | Additional granted recipe-permission ids. |
| `identity` | `resource_location` | no | `-` | Optional single granted identity id. |
| `identities` | `array<resource_location>` | no | `-` | Additional granted identity ids. |

## Upgrade Consume Rule Definitions

- Topic id: `upgrade_consume_rules`
- Authoring location: `data/<namespace>/xlib/upgrade_consume_rules/*.json`
- Note: `condition` and `conditions` use the shared ability-requirement JSON vocabulary, including `condition_ref` ids.
- Note: `point_rewards` and `counter_rewards` must use positive integer values.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `id` | `resource_location` | no | `file id` | If omitted, the datapack file id becomes the runtime id. |
| `item` | `resource_location` | no | `-` | Optional single consumed item id. |
| `items` | `array<resource_location>` | no | `-` | Additional consumed item ids. |
| `item_tag` | `resource_location` | no | `-` | Optional single consumed item tag id. |
| `item_tags` | `array<resource_location>` | no | `-` | Additional consumed item tag ids. |
| `food_only` | `boolean` | no | `false` | When true, the consumed stack must be food. |
| `condition` | `ability_requirement` | no | `-` | Optional single inline consume condition. |
| `conditions` | `array<ability_requirement>` | no | `-` | Additional inline consume conditions. |
| `point_rewards` | `object<resource_location,int>` | no | `-` | Point type ids mapped to positive rewards. |
| `counter_rewards` | `object<resource_location,int>` | no | `-` | Counter ids mapped to positive increments. |

## Upgrade Kill Rule Definitions

- Topic id: `upgrade_kill_rules`
- Authoring location: `data/<namespace>/xlib/upgrade_kill_rules/*.json`
- Note: `point_rewards` and `counter_rewards` must use positive integer values.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `id` | `resource_location` | no | `file id` | If omitted, the datapack file id becomes the runtime id. |
| `target` | `resource_location` | no | `-` | Optional single target entity id. |
| `targets` | `array<resource_location>` | no | `-` | Additional target entity ids. |
| `target_tag` | `resource_location` | no | `-` | Optional single target entity tag id. |
| `target_tags` | `array<resource_location>` | no | `-` | Additional target entity tag ids. |
| `required_ability` | `resource_location` | no | `-` | Optional ability id that must be active on kill credit. |
| `point_rewards` | `object<resource_location,int>` | no | `-` | Point type ids mapped to positive rewards. |
| `counter_rewards` | `object<resource_location,int>` | no | `-` | Counter ids mapped to positive increments. |

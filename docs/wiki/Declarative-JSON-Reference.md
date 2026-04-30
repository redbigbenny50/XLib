# Declarative JSON Reference

This file is generated from the bounded authored gameplay surfaces shipped by XLib.
It complements the progression-specific reference page and is meant to stay aligned with the real definition readers.

## Named Condition Definitions

- Topic id: `conditions`
- Authoring location: `data/<namespace>/xlib/conditions/*.json` and `assets/<namespace>/xlib/conditions/*.json`
- Note: Each file body is one shared ability-requirement JSON tree, not a wrapper object.
- Note: Datapack and client condition roots are loaded separately; the same id may exist in both.
- Note: Nested parser errors report JSON paths, and `condition_ref` recursion failures report the full reference chain.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `body` | `ability_requirement` | yes | `-` | Whole-file requirement tree compiled by `AbilityRequirementJsonParser`. |

## Context Grant Definitions

- Topic id: `context_grants`
- Authoring location: `data/<namespace>/xlib/context_grants/*.json`
- Note: `when` uses the shared ability-requirement JSON vocabulary, including `condition_ref` ids.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `source` | `resource_location` | no | `file id` | Preferred source id field. |
| `source_id` | `resource_location` | no | `file id` | Backward-compatible alias for `source`. |
| `when` | `ability_requirement` | no | `always` | Inline shared condition tree. |
| `grant_ability` | `resource_location` | no | `-` | Optional single granted ability id. |
| `grant_abilities` | `array<resource_location>` | no | `-` | Additional granted ability ids. |
| `grant_passive` | `resource_location` | no | `-` | Optional single granted passive id. |
| `grant_passives` | `array<resource_location>` | no | `-` | Additional granted passive ids. |
| `grant_granted_item` | `resource_location` | no | `-` | Optional single granted-item id. |
| `grant_granted_items` | `array<resource_location>` | no | `-` | Additional granted-item ids. |
| `grant_recipe_permission` | `resource_location` | no | `-` | Optional single recipe-permission id. |
| `grant_recipe_permissions` | `array<resource_location>` | no | `-` | Additional recipe-permission ids. |
| `block_ability` | `resource_location` | no | `-` | Optional single blocked ability id. |
| `block_abilities` | `array<resource_location>` | no | `-` | Additional blocked ability ids. |
| `grant_state_policy` | `resource_location` | no | `-` | Optional single state-policy id. |
| `grant_state_policies` | `array<resource_location>` | no | `-` | Additional state-policy ids. |
| `grant_state_flag` | `resource_location` | no | `-` | Optional single state-flag id. |
| `grant_state_flags` | `array<resource_location>` | no | `-` | Additional state-flag ids. |

## Grant Bundle Definitions

- Topic id: `grant_bundles`
- Authoring location: `data/<namespace>/xlib/grant_bundles/*.json`
- Note: Bundle definitions are bounded projection snapshots without inline conditions.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `id` | `resource_location` | no | `file id` | If omitted, the datapack file id becomes the runtime id. |
| `grant_ability` | `resource_location` | no | `-` | Optional single granted ability id. |
| `grant_abilities` | `array<resource_location>` | no | `-` | Additional granted ability ids. |
| `grant_passive` | `resource_location` | no | `-` | Optional single granted passive id. |
| `grant_passives` | `array<resource_location>` | no | `-` | Additional granted passive ids. |
| `grant_granted_item` | `resource_location` | no | `-` | Optional single granted-item id. |
| `grant_granted_items` | `array<resource_location>` | no | `-` | Additional granted-item ids. |
| `grant_recipe_permission` | `resource_location` | no | `-` | Optional single recipe-permission id. |
| `grant_recipe_permissions` | `array<resource_location>` | no | `-` | Additional recipe-permission ids. |
| `block_ability` | `resource_location` | no | `-` | Optional single blocked ability id. |
| `block_abilities` | `array<resource_location>` | no | `-` | Additional blocked ability ids. |
| `grant_state_policy` | `resource_location` | no | `-` | Optional single state-policy id. |
| `grant_state_policies` | `array<resource_location>` | no | `-` | Additional state-policy ids. |
| `grant_state_flag` | `resource_location` | no | `-` | Optional single state-flag id. |
| `grant_state_flags` | `array<resource_location>` | no | `-` | Additional state-flag ids. |

## Artifact Definitions

- Topic id: `artifacts`
- Authoring location: `data/<namespace>/xlib/artifacts/*.json`
- Note: At least one `item` or `items` entry is required.
- Note: `when` uses the shared ability-requirement JSON vocabulary.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `id` | `resource_location` | no | `file id` | If omitted, the datapack file id becomes the runtime id. |
| `item` | `resource_location` | no | `-` | Optional single backing item id. |
| `items` | `array<resource_location>` | no | `-` | Additional backing item ids. |
| `presence` | `enum` | no | `inventory` | Single artifact presence mode. |
| `presence_modes` | `array<enum>` | no | `inventory` | Additional presence modes. |
| `equipped_bundle` | `resource_location` | no | `-` | Optional single bundle granted while active. |
| `equipped_bundles` | `array<resource_location>` | no | `-` | Additional active bundles. |
| `unlocked_bundle` | `resource_location` | no | `-` | Optional single bundle granted while unlocked. |
| `unlocked_bundles` | `array<resource_location>` | no | `-` | Additional unlocked bundles. |
| `when` | `ability_requirement` | no | `-` | Optional shared requirement tree. |
| `unlock_on_consume` | `boolean` | no | `false` | Whether consuming the item unlocks the artifact. |

## Authored Ability Definitions

- Topic id: `abilities`
- Authoring location: `data/<namespace>/xlib/abilities/*.json`
- Note: `icon` is required and supports item, texture, or custom-renderer shapes.
- Note: Requirement fields use the shared ability-requirement JSON vocabulary.
- Note: `action`, `tick_effects`, `end_effects`, and `sounds` use the bounded runtime-effect surface from `DataDrivenRuntimeEffects`.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `id` | `resource_location` | no | `file id` | If omitted, the datapack file id becomes the runtime id. |
| `icon` | `icon_definition` | yes | `-` | Required icon definition. |
| `display_name` | `component` | no | `literal id` | Display name component. |
| `description` | `component` | no | `empty` | Description component. |
| `family` | `resource_location` | no | `-` | Optional metadata family. |
| `group` | `resource_location` | no | `-` | Optional metadata group. |
| `page` | `resource_location` | no | `-` | Optional metadata page. |
| `tag` | `resource_location` | no | `-` | Optional single metadata tag. |
| `tags` | `array<resource_location>` | no | `-` | Additional metadata tags. |
| `cooldown_ticks` | `int` | no | `-` | Cooldown duration in ticks. |
| `cooldown_policy` | `enum` | no | `default engine policy` | Ability cooldown policy name. |
| `toggle_ability` | `boolean` | no | `false` | Marks the ability as a toggle. |
| `duration_ticks` | `int` | no | `-` | Sustain or active duration. |
| `charges` | `object` | no | `-` | Preferred charge config object with `max` and `recharge_ticks`. |
| `max_charges` | `int` | no | `-` | Legacy max-charge field. |
| `charge_recharge_ticks` | `int` | no | `-` | Legacy charge recharge field. |
| `assign_requirement` | `ability_requirement` | no | `-` | Optional single assignment requirement. |
| `assign_requirements` | `array<ability_requirement>` | no | `-` | Additional assignment requirements. |
| `activate_requirement` | `ability_requirement` | no | `-` | Optional single activation requirement. |
| `activate_requirements` | `array<ability_requirement>` | no | `-` | Additional activation requirements. |
| `stay_active_requirement` | `ability_requirement` | no | `-` | Optional single sustain requirement. |
| `stay_active_requirements` | `array<ability_requirement>` | no | `-` | Additional sustain requirements. |
| `render_requirement` | `ability_requirement` | no | `-` | Optional single render requirement. |
| `render_requirements` | `array<ability_requirement>` | no | `-` | Additional render requirements. |
| `resource_costs` | `object<resource_location,int>` | no | `-` | Resource ids mapped to integer costs. |
| `sounds` | `object` | no | `-` | Bounded trigger-to-sound map. |
| `action` | `object` | yes | `-` | Required bounded action definition. |
| `tick_effects` | `array<object>` | no | `-` | Bounded effects applied while active. |
| `end_effects` | `array<object>` | no | `-` | Bounded effects applied when the ability ends. |

## Authored Passive Definitions

- Topic id: `passives`
- Authoring location: `data/<namespace>/xlib/passives/*.json`
- Note: `icon` is required and supports item, texture, or custom-renderer shapes.
- Note: Requirement fields use the shared ability-requirement JSON vocabulary.
- Note: Effect-hook arrays use the bounded runtime-effect surface from `DataDrivenRuntimeEffects`.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `id` | `resource_location` | no | `file id` | If omitted, the datapack file id becomes the runtime id. |
| `icon` | `icon_definition` | yes | `-` | Required icon definition. |
| `display_name` | `component` | no | `literal id` | Display name component. |
| `description` | `component` | no | `empty` | Description component. |
| `family` | `resource_location` | no | `-` | Optional metadata family. |
| `group` | `resource_location` | no | `-` | Optional metadata group. |
| `page` | `resource_location` | no | `-` | Optional metadata page. |
| `tag` | `resource_location` | no | `-` | Optional single metadata tag. |
| `tags` | `array<resource_location>` | no | `-` | Additional metadata tags. |
| `grant_requirement` | `ability_requirement` | no | `-` | Optional single grant requirement. |
| `grant_requirements` | `array<ability_requirement>` | no | `-` | Additional grant requirements. |
| `active_requirement` | `ability_requirement` | no | `-` | Optional single active requirement. |
| `active_requirements` | `array<ability_requirement>` | no | `-` | Additional active requirements. |
| `cooldown_tick_rate_multiplier` | `double` | no | `-` | Cooldown-scaling multiplier while passive is active. |
| `sounds` | `object` | no | `-` | Bounded trigger-to-sound map. |
| `tick_effects` | `array<object>` | no | `-` | Bounded effects applied every tick. |
| `on_granted_effects` | `array<object>` | no | `-` | Bounded effects applied when granted. |
| `on_revoked_effects` | `array<object>` | no | `-` | Bounded effects applied when revoked. |
| `on_hit_effects` | `array<object>` | no | `-` | Bounded effects applied on hit. |
| `on_kill_effects` | `array<object>` | no | `-` | Bounded effects applied on kill. |
| `on_hurt_effects` | `array<object>` | no | `-` | Bounded effects applied on hurt. |
| `on_jump_effects` | `array<object>` | no | `-` | Bounded effects applied on jump. |
| `on_eat_effects` | `array<object>` | no | `-` | Bounded effects applied on eat. |
| `on_block_break_effects` | `array<object>` | no | `-` | Bounded effects applied on block break. |
| `on_armor_change_effects` | `array<object>` | no | `-` | Bounded effects applied on armor change. |

## Identity Definitions

- Topic id: `identities`
- Authoring location: `data/<namespace>/xlib/identities/*.json`
- Note: Identity definitions are intentionally small: inheritance plus grant-bundle projection.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `id` | `resource_location` | no | `file id` | If omitted, the datapack file id becomes the runtime id. |
| `inherits` | `array<resource_location>` | no | `-` | Identity ids inherited by this identity. |
| `grant_bundle` | `resource_location` | no | `-` | Optional single projected bundle id. |
| `grant_bundles` | `array<resource_location>` | no | `-` | Additional projected bundle ids. |

## Profile Group Definitions

- Topic id: `profile_groups`
- Authoring location: `data/<namespace>/xlib/profile_groups/*.json`
- Note: `icon` is optional for groups, unlike profiles.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `id` | `resource_location` | no | `file id` | If omitted, the datapack file id becomes the runtime id. |
| `display_name` | `component` | no | `literal id` | Display name component. |
| `description` | `component` | no | `empty` | Description component. |
| `icon` | `icon_definition` | no | `-` | Optional group icon. |
| `selection_limit` | `int` | no | `1` | How many profiles may be selected from the group. |
| `required_onboarding` | `boolean` | no | `false` | Whether onboarding is required. |
| `onboarding_trigger` | `enum` | no | `-` | Optional single onboarding trigger. |
| `onboarding_triggers` | `array<enum>` | no | `-` | Additional onboarding triggers. |
| `auto_open_menu` | `boolean` | no | `true` | Whether onboarding auto-opens the selection screen. |
| `blocks_ability_use` | `boolean` | no | `false` | Whether pending selection blocks ability use. |
| `blocks_ability_menu` | `boolean` | no | `false` | Whether pending selection blocks the ability menu. |
| `blocks_progression` | `boolean` | no | `false` | Whether pending selection blocks progression. |
| `player_can_reset` | `boolean` | no | `false` | Whether players may reset their own choice. |
| `admin_can_reset` | `boolean` | no | `true` | Whether admins may reset choices. |
| `reopen_on_reset` | `boolean` | no | `false` | Whether reset reopens onboarding. |

## Profile Definitions

- Topic id: `profiles`
- Authoring location: `data/<namespace>/xlib/profiles/*.json`
- Note: `group` and `icon` are required for profile definitions.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `id` | `resource_location` | no | `file id` | If omitted, the datapack file id becomes the runtime id. |
| `group` | `resource_location` | yes | `-` | Owning profile-group id. |
| `display_name` | `component` | no | `literal id` | Display name component. |
| `description` | `component` | no | `empty` | Description component. |
| `icon` | `icon_definition` | yes | `-` | Required profile icon. |
| `incompatible_with` | `resource_location` | no | `-` | Optional single incompatible profile id. |
| `incompatible_profiles` | `array<resource_location>` | no | `-` | Additional incompatible profile ids. |
| `grant_bundle` | `resource_location` | no | `-` | Optional single grant-bundle id. |
| `grant_bundles` | `array<resource_location>` | no | `-` | Additional grant-bundle ids. |
| `identity` | `resource_location` | no | `-` | Optional single identity id. |
| `identities` | `array<resource_location>` | no | `-` | Additional identity ids. |
| `ability` | `resource_location` | no | `-` | Optional single ability id. |
| `abilities` | `array<resource_location>` | no | `-` | Additional ability ids. |
| `mode` | `resource_location` | no | `-` | Optional single mode id. |
| `modes` | `array<resource_location>` | no | `-` | Additional mode ids. |
| `passive` | `resource_location` | no | `-` | Optional single passive id. |
| `passives` | `array<resource_location>` | no | `-` | Additional passive ids. |
| `granted_item` | `resource_location` | no | `-` | Optional single granted-item id. |
| `granted_items` | `array<resource_location>` | no | `-` | Additional granted-item ids. |
| `recipe_permission` | `resource_location` | no | `-` | Optional single recipe-permission id. |
| `recipe_permissions` | `array<resource_location>` | no | `-` | Additional recipe-permission ids. |
| `state_flag` | `resource_location` | no | `-` | Optional single state-flag id. |
| `state_flags` | `array<resource_location>` | no | `-` | Additional state-flag ids. |
| `unlock_artifact` | `resource_location` | no | `-` | Optional single unlocked artifact id. |
| `unlock_artifacts` | `array<resource_location>` | no | `-` | Additional unlocked artifact ids. |
| `starting_node` | `resource_location` | no | `-` | Optional single managed starting node id. |
| `starting_nodes` | `array<resource_location>` | no | `-` | Additional managed starting node ids. |

## Lifecycle Stage Definitions

- Topic id: `lifecycle_stages`
- Authoring location: `data/<namespace>/xlib/lifecycle_stages/*.json`
- Note: `duration_ticks` must be a positive integer when present.
- Note: Each `auto_transitions` entry requires `target` and `trigger`; valid trigger values are `timer`, `manual`, `death`, `respawn`, `advancement`, `condition`.
- Note: Cross-references (projected bundles, identities, flags, policies, and visual forms) are validated against Java-registered definitions at reload time; missing ids produce a server log warning.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `id` | `resource_location` | no | `file id` | If omitted, the datapack file id becomes the runtime id. |
| `duration_ticks` | `int` | no | `-` | Optional positive tick duration for the TIMER trigger. |
| `auto_transitions` | `array<object>` | no | `[]` | Automatic transitions; each entry has `target`, `trigger`, and optional `preserve_elapsed`. |
| `manual_transition_targets` | `array<resource_location>` | no | `[]` | Stage ids that may be targeted by manual transition requests. |
| `project_state_flags` | `array<resource_location>` | no | `[]` | State flag ids granted while this stage is active. |
| `project_grant_bundles` | `array<resource_location>` | no | `[]` | Grant bundle ids activated while this stage is active. |
| `project_identities` | `array<resource_location>` | no | `[]` | Identity ids projected while this stage is active. |
| `project_capability_policies` | `array<resource_location>` | no | `[]` | Capability policy ids applied while this stage is active. |
| `project_visual_form` | `resource_location` | no | `-` | Optional visual form id applied while this stage is active. |

## Capability Policy Definitions

- Topic id: `capability_policies`
- Authoring location: `data/<namespace>/xlib/capability_policies/*.json`
- Note: All policy dimension objects are optional; omitted dimensions default to fully permissive.
- Note: `merge_mode` controls how multiple active policies combine; valid values are `restrictive` and `permissive`.
- Note: `allowed_item_tags` and `blocked_item_tags` in `held_items` accept arrays of resource-location tag ids.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `id` | `resource_location` | no | `file id` | If omitted, the datapack file id becomes the runtime id. |
| `merge_mode` | `enum` | no | `restrictive` | How multiple active policies merge: `restrictive` or `permissive`. |
| `priority` | `int` | no | `0` | Resolution priority when multiple policies are active. |
| `inventory` | `object` | no | `full` | `can_open_inventory`, `can_move_items`, `can_use_hotbar`, `can_use_offhand`. |
| `equipment` | `object` | no | `full` | `can_equip`, `can_unequip`. |
| `held_items` | `object` | no | `full` | `can_use_tools`, `allowed_item_tags`, `blocked_item_tags`. |
| `crafting` | `object` | no | `full` | `can_craft`, `allowed_station_tags`, `blocked_station_tags`. |
| `containers` | `object` | no | `full` | `can_open_chests`, `can_open_furnaces`, `can_open_barrels`. |
| `pickup_drop` | `object` | no | `full` | `can_pickup_items`, `can_drop_items`. |
| `interaction` | `object` | no | `full` | `can_attack_players`, `can_attack_mobs`, `can_interact_with_blocks`. |
| `menus` | `object` | no | `full` | `can_open_menus`. |
| `movement` | `object` | no | `full` | `can_sprint`, `can_sneak`, `can_jump`, `can_fly`. |

## Visual Form Definitions

- Topic id: `visual_forms`
- Authoring location: `data/<namespace>/xlib/visual_forms/*.json`
- Note: `kind` is required; valid values are `humanoid`, `creature`, `vehicle`, `construct`, `spirit`, `abstract`.
- Note: `render_scale` must be a positive float when present.
- Note: Profile id fields (`model_profile`, `cue_route_profile`, `hud_profile`, `sound_profile`) are optional references to backend-specific profile registries.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `id` | `resource_location` | no | `file id` | If omitted, the datapack file id becomes the runtime id. |
| `kind` | `enum` | yes | `-` | Required form kind: `humanoid`, `creature`, `vehicle`, `construct`, `spirit`, or `abstract`. |
| `model_profile` | `resource_location` | no | `-` | Optional model-profile id for animation/skinning backends. |
| `cue_route_profile` | `resource_location` | no | `-` | Optional cue-route-profile id for animation cue routing. |
| `hud_profile` | `resource_location` | no | `-` | Optional HUD-profile id for form-specific overlay layout. |
| `sound_profile` | `resource_location` | no | `-` | Optional sound-profile id for form-specific audio. |
| `first_person_policy` | `enum` | no | `default` | First-person visibility policy: `default`, `hidden`, or `forced`. |
| `render_scale` | `float` | no | `1.0` | Positive render-scale multiplier applied to this form. |

## Mode Definitions

- Topic id: `modes`
- Authoring location: `data/<namespace>/xlib/modes/*.json`
- Note: `ability` is the preferred id field; `id` falls back to the same value when `ability` is omitted.
- Note: Mode definitions stay bounded to metadata, upkeep, overlay, projection, and block/exclusive relationships.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `ability` | `resource_location` | no | `file id or `id`` | Preferred backing ability id. |
| `id` | `resource_location` | no | `file id` | Fallback id when `ability` is omitted. |
| `family` | `resource_location` | no | `-` | Optional metadata family. |
| `group` | `resource_location` | no | `-` | Optional metadata group. |
| `page` | `resource_location` | no | `-` | Optional metadata page. |
| `tag` | `resource_location` | no | `-` | Optional single metadata tag. |
| `tags` | `array<resource_location>` | no | `-` | Additional metadata tags. |
| `priority` | `int` | no | `-` | Mode priority. |
| `stackable` | `boolean` | no | `false` | Marks the mode as stackable. |
| `overlay_mode` | `boolean` | no | `false` | Alias that also marks the mode as stackable. |
| `cycle_group` | `resource_location` | no | `-` | Optional cycle-group id. |
| `cycle_order` | `int` | no | `-` | Optional cycle order. |
| `reset_cycle_group` | `resource_location` | no | `-` | Optional single cycle group to reset on activate. |
| `reset_cycle_groups` | `array<resource_location>` | no | `-` | Additional cycle groups to reset on activate. |
| `reset_own_cycle_group_on_activate` | `boolean` | no | `false` | Whether activating the mode resets its own cycle group. |
| `cooldown_tick_rate_multiplier` | `double` | no | `-` | Cooldown-scaling multiplier. |
| `health_cost_per_tick` | `double` | no | `-` | Health upkeep per tick. |
| `minimum_health` | `double` | no | `-` | Minimum health floor for upkeep. |
| `resource_delta_per_tick` | `object<resource_location,double>` | no | `-` | Per-resource deltas while active. |
| `exclusive_mode` | `resource_location` | no | `-` | Optional single exclusive mode id. |
| `exclusive_modes` | `array<resource_location>` | no | `-` | Additional exclusive mode ids. |
| `blocked_by_mode` | `resource_location` | no | `-` | Optional single blocking mode id. |
| `blocked_by_modes` | `array<resource_location>` | no | `-` | Additional blocking mode ids. |
| `transform_from` | `resource_location` | no | `-` | Optional single source mode for transform relationships. |
| `transforms_from` | `resource_location | array<resource_location>` | no | `-` | Single or plural transform source ids. |
| `overlay_abilities` | `object<int,resource_location>` | no | `-` | Primary-bar slot index to overlay ability id. |
| `grant_ability` | `resource_location` | no | `-` | Optional single granted ability id. |
| `grant_abilities` | `array<resource_location>` | no | `-` | Additional granted ability ids. |
| `grant_passive` | `resource_location` | no | `-` | Optional single granted passive id. |
| `grant_passives` | `array<resource_location>` | no | `-` | Additional granted passive ids. |
| `grant_granted_item` | `resource_location` | no | `-` | Optional single granted-item id. |
| `grant_granted_items` | `array<resource_location>` | no | `-` | Additional granted-item ids. |
| `grant_recipe_permission` | `resource_location` | no | `-` | Optional single recipe-permission id. |
| `grant_recipe_permissions` | `array<resource_location>` | no | `-` | Additional recipe-permission ids. |
| `block_ability` | `resource_location` | no | `-` | Optional single blocked ability id. |
| `block_abilities` | `array<resource_location>` | no | `-` | Additional blocked ability ids. |
| `grant_state_policy` | `resource_location` | no | `-` | Optional single state-policy id. |
| `grant_state_policies` | `array<resource_location>` | no | `-` | Additional state-policy ids. |
| `grant_state_flag` | `resource_location` | no | `-` | Optional single state-flag id. |
| `grant_state_flags` | `array<resource_location>` | no | `-` | Additional state-flag ids. |

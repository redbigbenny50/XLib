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
- Note: Allow and block filters are additive across active restrictive policies and intersect across permissive policies when both sides specify bounded sets.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `id` | `resource_location` | no | `file id` | If omitted, the datapack file id becomes the runtime id. |
| `merge_mode` | `enum` | no | `restrictive` | How multiple active policies merge: `restrictive` or `permissive`. |
| `priority` | `int` | no | `0` | Resolution priority when multiple policies are active. |
| `inventory` | `object` | no | `full` | `can_open_inventory`, `can_move_items`, `can_use_hotbar`, `can_use_offhand`, `can_change_selected_hotbar_slot`, `allowed_hotbar_slots`, `blocked_hotbar_slots`. |
| `equipment` | `object` | no | `full` | `can_equip_armor`, `can_unequip_armor`, `can_equip_held_items`, `allowed_armor_items`, `blocked_armor_items`, `allowed_armor_item_tags`, `blocked_armor_item_tags`. |
| `held_items` | `object` | no | `full` | `can_use_main_hand`, `can_use_offhand`, `can_block_with_shields`, `can_use_tools`, `can_use_weapons`, `can_place_blocks`, `can_break_blocks`, `allowed_items`, `blocked_items`, `allowed_item_tags`, `blocked_item_tags`. |
| `crafting` | `object` | no | `full` | `can_craft`, `allowed_station_tags`, `blocked_station_tags`. |
| `containers` | `object` | no | `full` | `can_open_containers`, `can_open_chests`, `can_open_furnaces`, `can_open_brewing_stands`, `can_open_shulker_boxes`. |
| `pickup_drop` | `object` | no | `full` | `can_pickup_items`, `can_drop_items`, `allowed_items`, `blocked_items`, `allowed_item_tags`, `blocked_item_tags`. |
| `interaction` | `object` | no | `full` | `can_interact_with_blocks`, `can_interact_with_entities`, `can_use_beds`, `can_ride_entities`, `can_attack_players`, `can_attack_mobs`, `allowed_blocks`, `blocked_blocks`, `allowed_block_tags`, `blocked_block_tags`, `allowed_entities`, `blocked_entities`, `allowed_entity_tags`, `blocked_entity_tags`. |
| `menus` | `object` | no | `full` | `can_open_menus`. |
| `movement` | `object` | no | `full` | `can_sprint`, `can_sneak`, `can_jump`, `can_fly`. |

## Damage Modifier Profile Definitions

- Topic id: `damage_modifier_profiles`
- Authoring location: `data/<namespace>/xlib/damage_modifier_profiles/*.json`
- Note: Profiles are server-authored vulnerability, resistance, and immunity maps resolved against exact damage types and damage-type tags.
- Note: Both flat keys and nested `incoming` / `outgoing` objects are accepted for datapack authoring.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `id` | `resource_location` | no | `file id` | If omitted, the datapack file id becomes the runtime id. |
| `incoming_damage_types` | `object<resource_location,double>` | no | `-` | Exact incoming damage-type multipliers. |
| `incoming_damage_type_tags` | `object<resource_location,double>` | no | `-` | Incoming damage-type tag multipliers. |
| `outgoing_damage_types` | `object<resource_location,double>` | no | `-` | Exact outgoing damage-type multipliers. |
| `outgoing_damage_type_tags` | `object<resource_location,double>` | no | `-` | Outgoing damage-type tag multipliers. |
| `incoming` | `object` | no | `-` | Nested alias object with `damage_types` and `damage_type_tags`. |
| `outgoing` | `object` | no | `-` | Nested alias object with `damage_types` and `damage_type_tags`. |

## Tracked Value Definitions

- Topic id: `tracked_values`
- Authoring location: `data/<namespace>/xlib/tracked_values/*.json`
- Note: Tracked values are bounded numeric stats stored per player with exact fractional precision and optional per-tick drift.
- Note: `replace_food_bar` is a shorthand that sets `food_replacement_priority` to `100`.
- Note: Definitions also sync to clients so the HUD and food-bar replacement lane can render authored values without addon-side packets.
- Note: Food-replacement mechanics can optionally consume food items into the tracked value and drive custom healing/starvation thresholds while vanilla hunger is suppressed.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `id` | `resource_location` | no | `file id` | If omitted, the datapack file id becomes the runtime id. |
| `display_name` | `component` | no | `literal id` | Display name component. |
| `min` | `double` | no | `0.0` | Preferred lower bound. |
| `min_value` | `double` | no | `0.0` | Backward-compatible alias for `min`. |
| `max` | `double` | no | `100.0` | Preferred upper bound. |
| `max_value` | `double` | no | `100.0` | Backward-compatible alias for `max`. |
| `starting_value` | `double` | no | `0.0` | Initial value when no stored amount exists. |
| `starting_amount` | `double` | no | `0.0` | Backward-compatible alias for `starting_value`. |
| `tick_delta` | `double` | no | `0.0` | Per-tick drift applied by the runtime before rule dispatch. |
| `hud_color` | `int \| hex string` | no | `#8AD8FF` | HUD fill color. Accepts integer, `#RRGGBB`, `#AARRGGBB`, or `0x...`. |
| `food_replacement_priority` | `int` | no | `0` | Positive values make the tracked value eligible to replace the vanilla food bar. |
| `replace_food_bar` | `boolean` | no | `false` | Convenience flag for authored values that should claim the food-bar lane. |
| `food_replacement_intake_scale` | `double` | no | `0.0` | Multiplier applied to consumed food nutrition before it is added to the tracked value. |
| `food_replacement_heal_threshold` | `int` | no | `18` | Effective food level threshold used for custom natural healing while replacement is active. |
| `food_replacement_heal_interval_ticks` | `int` | no | `80` | Tick interval for replacement-driven healing checks. |
| `food_replacement_heal_cost` | `double` | no | `1.0` | Tracked-value cost removed each time replacement-driven healing succeeds. |
| `food_replacement_starvation_threshold` | `int` | no | `0` | Effective food level threshold used for replacement-driven starvation checks. |
| `food_replacement_starvation_interval_ticks` | `int` | no | `80` | Tick interval for replacement-driven starvation checks. |
| `food_replacement_starvation_damage` | `float` | no | `1.0` | Damage dealt each time replacement-driven starvation triggers. |

## Tracked Value Rule Definitions

- Topic id: `tracked_value_rules`
- Authoring location: `data/<namespace>/xlib/tracked_value_rules/*.json`
- Note: Triggers currently accept: tick, damage_dealt, damage_taken, kill, jump, armor_changed, item_used, item_consumed, block_broken, advancement_earned, advancement_progress.
- Note: Rules are evaluated per trigger by descending `priority`, then by id for deterministic ties.
- Note: Target entity and target tag selectors now use merged real-plus-synthetic classification matching.
- Note: Action order inside one rule is: clear_values, set_values, value_deltas, multiply_values, min_values, max_values, food-replacement toggles, then state/policy/profile actions, then synthetic-classification toggles.

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `id` | `resource_location` | no | `file id` | If omitted, the datapack file id becomes the runtime id. |
| `trigger` | `enum` | yes | `-` | When the rule is evaluated. |
| `priority` | `int` | no | `0` | Higher-priority rules apply first within the same trigger. |
| `food_replacement_source` | `resource_location` | no | `rule id` | Source id used for `enable_food_replacement*` and `disable_food_replacement*` actions. |
| `classification_source` | `resource_location` | no | `rule id` | Source id used for synthetic classification grants, revokes, and clears. |
| `condition` | `ability_requirement` | no | `-` | Optional single shared requirement tree. |
| `conditions` | `array<ability_requirement>` | no | `-` | Additional shared requirement trees. |
| `target_entity` | `resource_location` | no | `-` | Optional single target entity-type id. |
| `target_entities` | `array<resource_location>` | no | `-` | Additional target entity-type ids. |
| `target_entity_tag` | `resource_location` | no | `-` | Optional single entity-type tag id. |
| `target_entity_tags` | `array<resource_location>` | no | `-` | Additional entity-type tag ids. |
| `item` | `resource_location` | no | `-` | Optional single item id for item-based triggers. |
| `items` | `array<resource_location>` | no | `-` | Additional item ids. |
| `item_tag` | `resource_location` | no | `-` | Optional single item tag id. |
| `item_tags` | `array<resource_location>` | no | `-` | Additional item tag ids. |
| `block` | `resource_location` | no | `-` | Optional single block id. |
| `blocks` | `array<resource_location>` | no | `-` | Additional block ids. |
| `block_tag` | `resource_location` | no | `-` | Optional single block tag id. |
| `block_tags` | `array<resource_location>` | no | `-` | Additional block tag ids. |
| `advancement` | `resource_location` | no | `-` | Optional single advancement id for advancement-based triggers. |
| `advancements` | `array<resource_location>` | no | `-` | Additional advancement ids. |
| `armor_slot` | `enum` | no | `-` | Optional single armor slot name for `armor_changed` triggers: `head`, `chest`, `legs`, `feet`. |
| `armor_slots` | `array<enum>` | no | `-` | Additional armor slot names. |
| `damage_type` | `resource_location` | no | `-` | Optional single damage-type id for damage-based triggers. |
| `damage_types` | `array<resource_location>` | no | `-` | Additional damage-type ids. |
| `damage_type_tag` | `resource_location` | no | `-` | Optional single damage-type tag id. |
| `damage_type_tags` | `array<resource_location>` | no | `-` | Additional damage-type tag ids. |
| `clear_value` | `resource_location` | no | `-` | Optional single tracked-value id whose stored amount should be cleared. |
| `clear_values` | `array<resource_location>` | no | `-` | Additional tracked-value ids to clear before other actions. |
| `value_deltas` | `object<resource_location,double>` | no | `-` | Per-value additive changes applied after any set actions. |
| `set_values` | `object<resource_location,double>` | no | `-` | Per-value exact amounts applied before deltas. |
| `multiply_values` | `object<resource_location,double>` | no | `-` | Per-value multipliers applied after additive changes. |
| `min_values` | `object<resource_location,double>` | no | `-` | Per-value floor amounts applied after multiplication. |
| `max_values` | `object<resource_location,double>` | no | `-` | Per-value cap amounts applied after floors. |
| `enable_food_replacement` | `resource_location` | no | `-` | Optional single tracked-value id to enable as a food-bar replacement source. |
| `enable_food_replacements` | `array<resource_location>` | no | `-` | Additional tracked-value ids to enable for food-bar replacement. |
| `disable_food_replacement` | `resource_location` | no | `-` | Optional single tracked-value id to disable for the configured source id. |
| `disable_food_replacements` | `array<resource_location>` | no | `-` | Additional tracked-value ids to disable for the configured source id. |
| `grant_state_policy` | `resource_location` | no | `-` | Optional single state-policy id to grant using the rule id as the source. |
| `grant_state_policies` | `array<resource_location>` | no | `-` | Additional state-policy ids to grant. |
| `revoke_state_policy` | `resource_location` | no | `-` | Optional single state-policy id to revoke from the rule id source. |
| `revoke_state_policies` | `array<resource_location>` | no | `-` | Additional state-policy ids to revoke. |
| `grant_state_flag` | `resource_location` | no | `-` | Optional single state-flag id to grant using the rule id as the source. |
| `grant_state_flags` | `array<resource_location>` | no | `-` | Additional state-flag ids to grant. |
| `revoke_state_flag` | `resource_location` | no | `-` | Optional single state-flag id to revoke from the rule id source. |
| `revoke_state_flags` | `array<resource_location>` | no | `-` | Additional state-flag ids to revoke. |
| `grant_capability_policy` | `resource_location` | no | `-` | Optional single capability-policy id to grant using the rule id as the source. |
| `grant_capability_policies` | `array<resource_location>` | no | `-` | Additional capability-policy ids to grant. |
| `revoke_capability_policy` | `resource_location` | no | `-` | Optional single capability-policy id to revoke from the rule id source. |
| `revoke_capability_policies` | `array<resource_location>` | no | `-` | Additional capability-policy ids to revoke. |
| `grant_damage_profile` | `resource_location` | no | `-` | Optional single damage-modifier profile id to grant using the rule id as the source. |
| `grant_damage_profiles` | `array<resource_location>` | no | `-` | Additional damage-modifier profile ids to grant. |
| `revoke_damage_profile` | `resource_location` | no | `-` | Optional single damage-modifier profile id to revoke from the rule id source. |
| `revoke_damage_profiles` | `array<resource_location>` | no | `-` | Additional damage-modifier profile ids to revoke. |
| `clear_classification_source` | `boolean` | no | `false` | Clears all synthetic classifications previously granted from `classification_source` before applying new ones. |
| `grant_synthetic_entity_type` | `resource_location` | no | `-` | Optional single synthetic entity-type id to grant to the player running the rule. |
| `grant_synthetic_entity_types` | `array<resource_location>` | no | `-` | Additional synthetic entity-type ids to grant. |
| `revoke_synthetic_entity_type` | `resource_location` | no | `-` | Optional single synthetic entity-type id to revoke from the configured classification source. |
| `revoke_synthetic_entity_types` | `array<resource_location>` | no | `-` | Additional synthetic entity-type ids to revoke. |
| `grant_synthetic_tag` | `resource_location` | no | `-` | Optional single synthetic tag id to grant to the player running the rule. |
| `grant_synthetic_tags` | `array<resource_location>` | no | `-` | Additional synthetic tag ids to grant. |
| `revoke_synthetic_tag` | `resource_location` | no | `-` | Optional single synthetic tag id to revoke from the configured classification source. |
| `revoke_synthetic_tags` | `array<resource_location>` | no | `-` | Additional synthetic tag ids to revoke. |

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

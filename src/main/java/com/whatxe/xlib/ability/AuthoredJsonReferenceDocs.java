package com.whatxe.xlib.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class AuthoredJsonReferenceDocs {
    private static final List<ReferenceSurface> SURFACES = List.of(
            new ReferenceSurface(
                    "conditions",
                    "Named Condition Definitions",
                    "`data/<namespace>/xlib/conditions/*.json` and `assets/<namespace>/xlib/conditions/*.json`",
                    List.of(
                            "Each file body is one shared ability-requirement JSON tree, not a wrapper object.",
                            "Datapack and client condition roots are loaded separately; the same id may exist in both.",
                            "Nested parser errors report JSON paths, and `condition_ref` recursion failures report the full reference chain."
                    ),
                    List.of(
                            field("body", "ability_requirement", true, "-", "Whole-file requirement tree compiled by `AbilityRequirementJsonParser`.")
                    )
            ),
            new ReferenceSurface(
                    "context_grants",
                    "Context Grant Definitions",
                    "`data/<namespace>/xlib/context_grants/*.json`",
                    List.of("`when` uses the shared ability-requirement JSON vocabulary, including `condition_ref` ids."),
                    List.of(
                            field("source", "resource_location", false, "file id", "Preferred source id field."),
                            field("source_id", "resource_location", false, "file id", "Backward-compatible alias for `source`."),
                            field("when", "ability_requirement", false, "always", "Inline shared condition tree."),
                            field("grant_ability", "resource_location", false, "-", "Optional single granted ability id."),
                            field("grant_abilities", "array<resource_location>", false, "-", "Additional granted ability ids."),
                            field("grant_passive", "resource_location", false, "-", "Optional single granted passive id."),
                            field("grant_passives", "array<resource_location>", false, "-", "Additional granted passive ids."),
                            field("grant_granted_item", "resource_location", false, "-", "Optional single granted-item id."),
                            field("grant_granted_items", "array<resource_location>", false, "-", "Additional granted-item ids."),
                            field("grant_recipe_permission", "resource_location", false, "-", "Optional single recipe-permission id."),
                            field("grant_recipe_permissions", "array<resource_location>", false, "-", "Additional recipe-permission ids."),
                            field("block_ability", "resource_location", false, "-", "Optional single blocked ability id."),
                            field("block_abilities", "array<resource_location>", false, "-", "Additional blocked ability ids."),
                            field("grant_state_policy", "resource_location", false, "-", "Optional single state-policy id."),
                            field("grant_state_policies", "array<resource_location>", false, "-", "Additional state-policy ids."),
                            field("grant_state_flag", "resource_location", false, "-", "Optional single state-flag id."),
                            field("grant_state_flags", "array<resource_location>", false, "-", "Additional state-flag ids.")
                    )
            ),
            new ReferenceSurface(
                    "grant_bundles",
                    "Grant Bundle Definitions",
                    "`data/<namespace>/xlib/grant_bundles/*.json`",
                    List.of("Bundle definitions are bounded projection snapshots without inline conditions."),
                    List.of(
                            field("id", "resource_location", false, "file id", "If omitted, the datapack file id becomes the runtime id."),
                            field("grant_ability", "resource_location", false, "-", "Optional single granted ability id."),
                            field("grant_abilities", "array<resource_location>", false, "-", "Additional granted ability ids."),
                            field("grant_passive", "resource_location", false, "-", "Optional single granted passive id."),
                            field("grant_passives", "array<resource_location>", false, "-", "Additional granted passive ids."),
                            field("grant_granted_item", "resource_location", false, "-", "Optional single granted-item id."),
                            field("grant_granted_items", "array<resource_location>", false, "-", "Additional granted-item ids."),
                            field("grant_recipe_permission", "resource_location", false, "-", "Optional single recipe-permission id."),
                            field("grant_recipe_permissions", "array<resource_location>", false, "-", "Additional recipe-permission ids."),
                            field("block_ability", "resource_location", false, "-", "Optional single blocked ability id."),
                            field("block_abilities", "array<resource_location>", false, "-", "Additional blocked ability ids."),
                            field("grant_state_policy", "resource_location", false, "-", "Optional single state-policy id."),
                            field("grant_state_policies", "array<resource_location>", false, "-", "Additional state-policy ids."),
                            field("grant_state_flag", "resource_location", false, "-", "Optional single state-flag id."),
                            field("grant_state_flags", "array<resource_location>", false, "-", "Additional state-flag ids.")
                    )
            ),
            new ReferenceSurface(
                    "artifacts",
                    "Artifact Definitions",
                    "`data/<namespace>/xlib/artifacts/*.json`",
                    List.of(
                            "At least one `item` or `items` entry is required.",
                            "`when` uses the shared ability-requirement JSON vocabulary."
                    ),
                    List.of(
                            field("id", "resource_location", false, "file id", "If omitted, the datapack file id becomes the runtime id."),
                            field("item", "resource_location", false, "-", "Optional single backing item id."),
                            field("items", "array<resource_location>", false, "-", "Additional backing item ids."),
                            field("presence", "enum", false, "inventory", "Single artifact presence mode."),
                            field("presence_modes", "array<enum>", false, "inventory", "Additional presence modes."),
                            field("equipped_bundle", "resource_location", false, "-", "Optional single bundle granted while active."),
                            field("equipped_bundles", "array<resource_location>", false, "-", "Additional active bundles."),
                            field("unlocked_bundle", "resource_location", false, "-", "Optional single bundle granted while unlocked."),
                            field("unlocked_bundles", "array<resource_location>", false, "-", "Additional unlocked bundles."),
                            field("when", "ability_requirement", false, "-", "Optional shared requirement tree."),
                            field("unlock_on_consume", "boolean", false, "false", "Whether consuming the item unlocks the artifact.")
                    )
            ),
            new ReferenceSurface(
                    "abilities",
                    "Authored Ability Definitions",
                    "`data/<namespace>/xlib/abilities/*.json`",
                    List.of(
                            "`icon` is required and supports item, texture, or custom-renderer shapes.",
                            "Requirement fields use the shared ability-requirement JSON vocabulary.",
                            "`action`, `tick_effects`, `end_effects`, and `sounds` use the bounded runtime-effect surface from `DataDrivenRuntimeEffects`."
                    ),
                    List.of(
                            field("id", "resource_location", false, "file id", "If omitted, the datapack file id becomes the runtime id."),
                            field("icon", "icon_definition", true, "-", "Required icon definition."),
                            field("display_name", "component", false, "literal id", "Display name component."),
                            field("description", "component", false, "empty", "Description component."),
                            field("family", "resource_location", false, "-", "Optional metadata family."),
                            field("group", "resource_location", false, "-", "Optional metadata group."),
                            field("page", "resource_location", false, "-", "Optional metadata page."),
                            field("tag", "resource_location", false, "-", "Optional single metadata tag."),
                            field("tags", "array<resource_location>", false, "-", "Additional metadata tags."),
                            field("cooldown_ticks", "int", false, "-", "Cooldown duration in ticks."),
                            field("cooldown_policy", "enum", false, "default engine policy", "Ability cooldown policy name."),
                            field("toggle_ability", "boolean", false, "false", "Marks the ability as a toggle."),
                            field("duration_ticks", "int", false, "-", "Sustain or active duration."),
                            field("charges", "object", false, "-", "Preferred charge config object with `max` and `recharge_ticks`."),
                            field("max_charges", "int", false, "-", "Legacy max-charge field."),
                            field("charge_recharge_ticks", "int", false, "-", "Legacy charge recharge field."),
                            field("assign_requirement", "ability_requirement", false, "-", "Optional single assignment requirement."),
                            field("assign_requirements", "array<ability_requirement>", false, "-", "Additional assignment requirements."),
                            field("activate_requirement", "ability_requirement", false, "-", "Optional single activation requirement."),
                            field("activate_requirements", "array<ability_requirement>", false, "-", "Additional activation requirements."),
                            field("stay_active_requirement", "ability_requirement", false, "-", "Optional single sustain requirement."),
                            field("stay_active_requirements", "array<ability_requirement>", false, "-", "Additional sustain requirements."),
                            field("render_requirement", "ability_requirement", false, "-", "Optional single render requirement."),
                            field("render_requirements", "array<ability_requirement>", false, "-", "Additional render requirements."),
                            field("resource_costs", "object<resource_location,int>", false, "-", "Resource ids mapped to integer costs."),
                            field("sounds", "object", false, "-", "Bounded trigger-to-sound map."),
                            field("action", "object", true, "-", "Required bounded action definition."),
                            field("tick_effects", "array<object>", false, "-", "Bounded effects applied while active."),
                            field("end_effects", "array<object>", false, "-", "Bounded effects applied when the ability ends.")
                    )
            ),
            new ReferenceSurface(
                    "passives",
                    "Authored Passive Definitions",
                    "`data/<namespace>/xlib/passives/*.json`",
                    List.of(
                            "`icon` is required and supports item, texture, or custom-renderer shapes.",
                            "Requirement fields use the shared ability-requirement JSON vocabulary.",
                            "Effect-hook arrays use the bounded runtime-effect surface from `DataDrivenRuntimeEffects`."
                    ),
                    List.of(
                            field("id", "resource_location", false, "file id", "If omitted, the datapack file id becomes the runtime id."),
                            field("icon", "icon_definition", true, "-", "Required icon definition."),
                            field("display_name", "component", false, "literal id", "Display name component."),
                            field("description", "component", false, "empty", "Description component."),
                            field("family", "resource_location", false, "-", "Optional metadata family."),
                            field("group", "resource_location", false, "-", "Optional metadata group."),
                            field("page", "resource_location", false, "-", "Optional metadata page."),
                            field("tag", "resource_location", false, "-", "Optional single metadata tag."),
                            field("tags", "array<resource_location>", false, "-", "Additional metadata tags."),
                            field("grant_requirement", "ability_requirement", false, "-", "Optional single grant requirement."),
                            field("grant_requirements", "array<ability_requirement>", false, "-", "Additional grant requirements."),
                            field("active_requirement", "ability_requirement", false, "-", "Optional single active requirement."),
                            field("active_requirements", "array<ability_requirement>", false, "-", "Additional active requirements."),
                            field("cooldown_tick_rate_multiplier", "double", false, "-", "Cooldown-scaling multiplier while passive is active."),
                            field("sounds", "object", false, "-", "Bounded trigger-to-sound map."),
                            field("tick_effects", "array<object>", false, "-", "Bounded effects applied every tick."),
                            field("on_granted_effects", "array<object>", false, "-", "Bounded effects applied when granted."),
                            field("on_revoked_effects", "array<object>", false, "-", "Bounded effects applied when revoked."),
                            field("on_hit_effects", "array<object>", false, "-", "Bounded effects applied on hit."),
                            field("on_kill_effects", "array<object>", false, "-", "Bounded effects applied on kill."),
                            field("on_hurt_effects", "array<object>", false, "-", "Bounded effects applied on hurt."),
                            field("on_jump_effects", "array<object>", false, "-", "Bounded effects applied on jump."),
                            field("on_eat_effects", "array<object>", false, "-", "Bounded effects applied on eat."),
                            field("on_block_break_effects", "array<object>", false, "-", "Bounded effects applied on block break."),
                            field("on_armor_change_effects", "array<object>", false, "-", "Bounded effects applied on armor change.")
                    )
            ),
            new ReferenceSurface(
                    "identities",
                    "Identity Definitions",
                    "`data/<namespace>/xlib/identities/*.json`",
                    List.of("Identity definitions are intentionally small: inheritance plus grant-bundle projection."),
                    List.of(
                            field("id", "resource_location", false, "file id", "If omitted, the datapack file id becomes the runtime id."),
                            field("inherits", "array<resource_location>", false, "-", "Identity ids inherited by this identity."),
                            field("grant_bundle", "resource_location", false, "-", "Optional single projected bundle id."),
                            field("grant_bundles", "array<resource_location>", false, "-", "Additional projected bundle ids.")
                    )
            ),
            new ReferenceSurface(
                    "profile_groups",
                    "Profile Group Definitions",
                    "`data/<namespace>/xlib/profile_groups/*.json`",
                    List.of("`icon` is optional for groups, unlike profiles."),
                    List.of(
                            field("id", "resource_location", false, "file id", "If omitted, the datapack file id becomes the runtime id."),
                            field("display_name", "component", false, "literal id", "Display name component."),
                            field("description", "component", false, "empty", "Description component."),
                            field("icon", "icon_definition", false, "-", "Optional group icon."),
                            field("selection_limit", "int", false, "1", "How many profiles may be selected from the group."),
                            field("required_onboarding", "boolean", false, "false", "Whether onboarding is required."),
                            field("onboarding_trigger", "enum", false, "-", "Optional single onboarding trigger."),
                            field("onboarding_triggers", "array<enum>", false, "-", "Additional onboarding triggers."),
                            field("auto_open_menu", "boolean", false, "true", "Whether onboarding auto-opens the selection screen."),
                            field("blocks_ability_use", "boolean", false, "false", "Whether pending selection blocks ability use."),
                            field("blocks_ability_menu", "boolean", false, "false", "Whether pending selection blocks the ability menu."),
                            field("blocks_progression", "boolean", false, "false", "Whether pending selection blocks progression."),
                            field("player_can_reset", "boolean", false, "false", "Whether players may reset their own choice."),
                            field("admin_can_reset", "boolean", false, "true", "Whether admins may reset choices."),
                            field("reopen_on_reset", "boolean", false, "false", "Whether reset reopens onboarding.")
                    )
            ),
            new ReferenceSurface(
                    "profiles",
                    "Profile Definitions",
                    "`data/<namespace>/xlib/profiles/*.json`",
                    List.of("`group` and `icon` are required for profile definitions."),
                    List.of(
                            field("id", "resource_location", false, "file id", "If omitted, the datapack file id becomes the runtime id."),
                            field("group", "resource_location", true, "-", "Owning profile-group id."),
                            field("display_name", "component", false, "literal id", "Display name component."),
                            field("description", "component", false, "empty", "Description component."),
                            field("icon", "icon_definition", true, "-", "Required profile icon."),
                            field("incompatible_with", "resource_location", false, "-", "Optional single incompatible profile id."),
                            field("incompatible_profiles", "array<resource_location>", false, "-", "Additional incompatible profile ids."),
                            field("grant_bundle", "resource_location", false, "-", "Optional single grant-bundle id."),
                            field("grant_bundles", "array<resource_location>", false, "-", "Additional grant-bundle ids."),
                            field("identity", "resource_location", false, "-", "Optional single identity id."),
                            field("identities", "array<resource_location>", false, "-", "Additional identity ids."),
                            field("ability", "resource_location", false, "-", "Optional single ability id."),
                            field("abilities", "array<resource_location>", false, "-", "Additional ability ids."),
                            field("mode", "resource_location", false, "-", "Optional single mode id."),
                            field("modes", "array<resource_location>", false, "-", "Additional mode ids."),
                            field("passive", "resource_location", false, "-", "Optional single passive id."),
                            field("passives", "array<resource_location>", false, "-", "Additional passive ids."),
                            field("granted_item", "resource_location", false, "-", "Optional single granted-item id."),
                            field("granted_items", "array<resource_location>", false, "-", "Additional granted-item ids."),
                            field("recipe_permission", "resource_location", false, "-", "Optional single recipe-permission id."),
                            field("recipe_permissions", "array<resource_location>", false, "-", "Additional recipe-permission ids."),
                            field("state_flag", "resource_location", false, "-", "Optional single state-flag id."),
                            field("state_flags", "array<resource_location>", false, "-", "Additional state-flag ids."),
                            field("unlock_artifact", "resource_location", false, "-", "Optional single unlocked artifact id."),
                            field("unlock_artifacts", "array<resource_location>", false, "-", "Additional unlocked artifact ids."),
                            field("starting_node", "resource_location", false, "-", "Optional single managed starting node id."),
                            field("starting_nodes", "array<resource_location>", false, "-", "Additional managed starting node ids.")
                    )
            ),
            new ReferenceSurface(
                    "lifecycle_stages",
                    "Lifecycle Stage Definitions",
                    "`data/<namespace>/xlib/lifecycle_stages/*.json`",
                    List.of(
                            "Stages project grant bundles, identities, state flags, capability policies, and a visual form while active.",
                            "`trigger` in auto_transitions accepts: timer, manual, death, respawn, advancement, condition.",
                            "Duration-less stages stay active until a manual transition or death trigger removes them."
                    ),
                    List.of(
                            field("id", "resource_location", false, "file id", "If omitted, the datapack file id becomes the runtime id."),
                            field("duration_ticks", "int", false, "-", "Optional automatic stage duration in ticks."),
                            field("auto_transitions", "array<object>", false, "-", "Transitions applied automatically. Each entry: `target` (resource_location), `trigger` (enum), `preserve_elapsed` (boolean, default false)."),
                            field("manual_transition_targets", "array<resource_location>", false, "-", "Stage ids that manual transitions are allowed to target."),
                            field("project_state_flags", "array<resource_location>", false, "-", "State flag ids projected while in this stage."),
                            field("project_grant_bundles", "array<resource_location>", false, "-", "Grant bundle ids projected while in this stage."),
                            field("project_identities", "array<resource_location>", false, "-", "Identity ids projected while in this stage."),
                            field("project_capability_policies", "array<resource_location>", false, "-", "Capability policy ids projected while in this stage."),
                            field("project_visual_form", "resource_location", false, "-", "Optional visual form id projected while in this stage.")
                    )
            ),
            new ReferenceSurface(
                    "capability_policies",
                    "Capability Policy Definitions",
                    "`data/<namespace>/xlib/capability_policies/*.json`",
                    List.of(
                            "All policy dimension objects are optional; omitted dimensions default to fully permissive.",
                            "`merge_mode` controls how overlapping active policies are combined: `restrictive` (AND semantics, default) or `permissive` (OR semantics).",
                            "Tag arrays in `held_items` and `crafting` are additive across all active restrictive policies."
                    ),
                    List.of(
                            field("id", "resource_location", false, "file id", "If omitted, the datapack file id becomes the runtime id."),
                            field("merge_mode", "enum", false, "restrictive", "How multiple active policies combine: `restrictive` or `permissive`."),
                            field("priority", "int", false, "0", "Evaluation priority; higher values take precedence."),
                            field("inventory", "object", false, "all true", "Inventory dimension: can_open_inventory, can_move_items, can_use_hotbar, can_use_offhand."),
                            field("equipment", "object", false, "all true", "Equipment dimension: can_equip_armor, can_unequip_armor, can_equip_held_items."),
                            field("held_items", "object", false, "all true", "Held-item dimension: can_use_main_hand, can_use_offhand, can_block_with_shields, can_use_tools, can_use_weapons, can_place_blocks, can_break_blocks, allowed_item_tags, blocked_item_tags."),
                            field("crafting", "object", false, "all true", "Crafting dimension: can_use_player_crafting, can_use_crafting_table, allowed_station_tags, blocked_station_tags."),
                            field("containers", "object", false, "all true", "Container dimension: can_open_containers, can_open_chests, can_open_furnaces, can_open_shulker_boxes."),
                            field("pickup_drop", "object", false, "all true", "Pickup-drop dimension: can_pickup_items, can_drop_items."),
                            field("interaction", "object", false, "all true", "Interaction dimension: can_interact_with_blocks, can_interact_with_entities, can_attack_players, can_attack_mobs."),
                            field("menus", "object", false, "all true", "Menu dimension: can_open_ability_menu, can_open_progression_menu, can_open_inventory_screen."),
                            field("movement", "object", false, "all true", "Movement dimension: can_sprint, can_sneak, can_jump, can_fly.")
                    )
            ),
            new ReferenceSurface(
                    "visual_forms",
                    "Visual Form Definitions",
                    "`data/<namespace>/xlib/visual_forms/*.json`",
                    List.of(
                            "`kind` is required and determines the base rendering category.",
                            "Profile reference fields link to separately registered model, cue-route, hud, and sound profiles.",
                            "`first_person_policy` controls first-person arm rendering: default, hidden, or custom."
                    ),
                    List.of(
                            field("id", "resource_location", false, "file id", "If omitted, the datapack file id becomes the runtime id."),
                            field("kind", "enum", true, "-", "Required visual form kind: humanoid, creature, vehicle, construct, spirit, or abstract."),
                            field("model_profile", "resource_location", false, "-", "Optional model profile id."),
                            field("cue_route_profile", "resource_location", false, "-", "Optional cue-route profile id."),
                            field("hud_profile", "resource_location", false, "-", "Optional HUD profile id."),
                            field("sound_profile", "resource_location", false, "-", "Optional sound profile id."),
                            field("first_person_policy", "enum", false, "default", "First-person arm policy: default, hidden, or custom."),
                            field("render_scale", "float", false, "1.0", "Render scale multiplier; must be positive.")
                    )
            ),
            new ReferenceSurface(
                    "modes",
                    "Mode Definitions",
                    "`data/<namespace>/xlib/modes/*.json`",
                    List.of(
                            "`ability` is the preferred id field; `id` falls back to the same value when `ability` is omitted.",
                            "Mode definitions stay bounded to metadata, upkeep, overlay, projection, and block/exclusive relationships."
                    ),
                    List.of(
                            field("ability", "resource_location", false, "file id or `id`", "Preferred backing ability id."),
                            field("id", "resource_location", false, "file id", "Fallback id when `ability` is omitted."),
                            field("family", "resource_location", false, "-", "Optional metadata family."),
                            field("group", "resource_location", false, "-", "Optional metadata group."),
                            field("page", "resource_location", false, "-", "Optional metadata page."),
                            field("tag", "resource_location", false, "-", "Optional single metadata tag."),
                            field("tags", "array<resource_location>", false, "-", "Additional metadata tags."),
                            field("priority", "int", false, "-", "Mode priority."),
                            field("stackable", "boolean", false, "false", "Marks the mode as stackable."),
                            field("overlay_mode", "boolean", false, "false", "Alias that also marks the mode as stackable."),
                            field("cycle_group", "resource_location", false, "-", "Optional cycle-group id."),
                            field("cycle_order", "int", false, "-", "Optional cycle order."),
                            field("reset_cycle_group", "resource_location", false, "-", "Optional single cycle group to reset on activate."),
                            field("reset_cycle_groups", "array<resource_location>", false, "-", "Additional cycle groups to reset on activate."),
                            field("reset_own_cycle_group_on_activate", "boolean", false, "false", "Whether activating the mode resets its own cycle group."),
                            field("cooldown_tick_rate_multiplier", "double", false, "-", "Cooldown-scaling multiplier."),
                            field("health_cost_per_tick", "double", false, "-", "Health upkeep per tick."),
                            field("minimum_health", "double", false, "-", "Minimum health floor for upkeep."),
                            field("resource_delta_per_tick", "object<resource_location,double>", false, "-", "Per-resource deltas while active."),
                            field("exclusive_mode", "resource_location", false, "-", "Optional single exclusive mode id."),
                            field("exclusive_modes", "array<resource_location>", false, "-", "Additional exclusive mode ids."),
                            field("blocked_by_mode", "resource_location", false, "-", "Optional single blocking mode id."),
                            field("blocked_by_modes", "array<resource_location>", false, "-", "Additional blocking mode ids."),
                            field("transform_from", "resource_location", false, "-", "Optional single source mode for transform relationships."),
                            field("transforms_from", "resource_location | array<resource_location>", false, "-", "Single or plural transform source ids."),
                            field("overlay_abilities", "object<int,resource_location>", false, "-", "Primary-bar slot index to overlay ability id."),
                            field("grant_ability", "resource_location", false, "-", "Optional single granted ability id."),
                            field("grant_abilities", "array<resource_location>", false, "-", "Additional granted ability ids."),
                            field("grant_passive", "resource_location", false, "-", "Optional single granted passive id."),
                            field("grant_passives", "array<resource_location>", false, "-", "Additional granted passive ids."),
                            field("grant_granted_item", "resource_location", false, "-", "Optional single granted-item id."),
                            field("grant_granted_items", "array<resource_location>", false, "-", "Additional granted-item ids."),
                            field("grant_recipe_permission", "resource_location", false, "-", "Optional single recipe-permission id."),
                            field("grant_recipe_permissions", "array<resource_location>", false, "-", "Additional recipe-permission ids."),
                            field("block_ability", "resource_location", false, "-", "Optional single blocked ability id."),
                            field("block_abilities", "array<resource_location>", false, "-", "Additional blocked ability ids."),
                            field("grant_state_policy", "resource_location", false, "-", "Optional single state-policy id."),
                            field("grant_state_policies", "array<resource_location>", false, "-", "Additional state-policy ids."),
                            field("grant_state_flag", "resource_location", false, "-", "Optional single state-flag id."),
                            field("grant_state_flags", "array<resource_location>", false, "-", "Additional state-flag ids.")
                    )
            )
    );

    private AuthoredJsonReferenceDocs() {}

    public static List<ReferenceSurface> surfaces() {
        return SURFACES;
    }

    public static Optional<ReferenceSurface> findSurface(String id) {
        return SURFACES.stream().filter(surface -> surface.id().equals(id)).findFirst();
    }

    public static String renderMarkdown() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Declarative JSON Reference\n\n");
        builder.append("This file is generated from the bounded authored gameplay surfaces shipped by XLib.\n");
        builder.append("It complements the progression-specific reference page and is meant to stay aligned with the real definition readers.\n\n");
        for (ReferenceSurface surface : SURFACES) {
            builder.append("## ").append(surface.title()).append("\n\n");
            builder.append("- Topic id: `").append(surface.id()).append("`\n");
            builder.append("- Authoring location: ").append(surface.location()).append("\n");
            for (String note : surface.notes()) {
                builder.append("- Note: ").append(note).append("\n");
            }
            builder.append("\n");
            builder.append("| Field | Type | Required | Default | Notes |\n");
            builder.append("| --- | --- | --- | --- | --- |\n");
            for (ReferenceField field : surface.fields()) {
                builder.append("| `").append(field.name()).append("` | `").append(field.type()).append("` | ")
                        .append(field.required() ? "yes" : "no").append(" | `").append(field.defaultValue())
                        .append("` | ").append(field.notes()).append(" |\n");
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    public static List<String> renderCommandLines(ReferenceSurface surface) {
        List<String> lines = new ArrayList<>();
        lines.add("reference=" + surface.id() + " | location=" + surface.location());
        for (String note : surface.notes()) {
            lines.add("note=" + note);
        }
        lines.add("fields=" + joinFields(surface.fields()));
        return List.copyOf(lines);
    }

    private static String joinFields(List<ReferenceField> fields) {
        if (fields.isEmpty()) {
            return "-";
        }
        return fields.stream()
                .map(field -> field.name() + ":" + field.type()
                        + "(" + (field.required() ? "required" : "optional")
                        + (field.defaultValue().equals("-") ? "" : ", default=" + field.defaultValue())
                        + ")")
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private static ReferenceField field(String name, String type, boolean required, String defaultValue, String notes) {
        return new ReferenceField(name, type, required, defaultValue, notes);
    }

    public record ReferenceSurface(
            String id,
            String title,
            String location,
            List<String> notes,
            List<ReferenceField> fields
    ) {
        public ReferenceSurface {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(location, "location");
            notes = List.copyOf(notes);
            fields = List.copyOf(fields);
        }
    }

    public record ReferenceField(
            String name,
            String type,
            boolean required,
            String defaultValue,
            String notes
    ) {
        public ReferenceField {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(defaultValue, "defaultValue");
            Objects.requireNonNull(notes, "notes");
        }
    }
}

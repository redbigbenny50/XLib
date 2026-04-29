package com.whatxe.xlib.progression;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class ProgressionJsonReferenceDocs {
    private static final List<ReferenceSurface> SURFACES = List.of(
            new ReferenceSurface(
                    "upgrade_requirements",
                    "Upgrade Requirement JSON",
                    "inline under `requirement` or `requirements` fields in progression-authored content",
                    List.of(
                            "Boolean `true` and `false` are accepted shorthands for always and never.",
                            "JSON arrays are treated as `all(...)` requirement groups.",
                            "Nested parser errors report JSON paths like `$.requirements[1].requirement`."
                    ),
                    List.of(),
                    List.of(
                            variant("always", "Always passes."),
                            variant("never", "Never passes."),
                            variant("all", "Every child requirement must pass.",
                                    field("requirements", "array<upgrade_requirement>", true, "-", "At least one child required.")),
                            variant("any", "At least one child requirement must pass.",
                                    field("requirements", "array<upgrade_requirement>", true, "-", "At least one child required.")),
                            variant("not", "Inverts one child requirement.",
                                    field("requirement", "upgrade_requirement", true, "-", "Exactly one nested child.")),
                            variant("advancement", "Requires a vanilla advancement id.",
                                    field("advancement", "resource_location", true, "-", "Namespaced advancement id.")),
                            variant("counter_at_least", "Requires a progression counter minimum.",
                                    field("counter", "resource_location", true, "-", "Counter id."),
                                    field("amount", "int", true, "-", "Minimum inclusive value.")),
                            variant("points_at_least", "Requires points in one progression point type.",
                                    field("point_type", "resource_location", false, "-", "Preferred field name."),
                                    field("point", "resource_location", false, "-", "Backward-compatible alias for `point_type`."),
                                    field("amount", "int", true, "-", "Minimum inclusive value.")),
                            variant("node_unlocked", "Requires one specific node to be unlocked.",
                                    field("node", "resource_location", true, "-", "Upgrade node id.")),
                            variant("any_node_unlocked", "Requires at least one node from a set to be unlocked.",
                                    field("node", "resource_location", false, "-", "Optional single-node shorthand."),
                                    field("nodes", "array<resource_location>", false, "-", "Plural form merged with `node`.")),
                            variant("identity_active", "Requires one active identity id.",
                                    field("identity", "resource_location", true, "-", "Identity id.")),
                            variant("track_completed", "Requires one completed track id.",
                                    field("track", "resource_location", true, "-", "Upgrade track id."))
                    )
            ),
            new ReferenceSurface(
                    "upgrade_point_types",
                    "Upgrade Point Type Definitions",
                    "`data/<namespace>/xlib/upgrade_point_types/*.json`",
                    List.of("Point-type definitions are intentionally minimal; the file mostly exists to declare a stable id."),
                    List.of(
                            field("id", "resource_location", false, "file id", "If omitted, the datapack file id becomes the runtime id.")
                    ),
                    List.of()
            ),
            new ReferenceSurface(
                    "upgrade_tracks",
                    "Upgrade Track Definitions",
                    "`data/<namespace>/xlib/upgrade_tracks/*.json`",
                    List.of("Singular and plural id fields are merged where both exist."),
                    List.of(
                            field("id", "resource_location", false, "file id", "If omitted, the datapack file id becomes the runtime id."),
                            field("family", "resource_location", false, "-", "Optional track family metadata."),
                            field("group", "resource_location", false, "-", "Optional track group metadata."),
                            field("page", "resource_location", false, "-", "Optional presentation page metadata."),
                            field("tag", "resource_location", false, "-", "Optional single track tag."),
                            field("tags", "array<resource_location>", false, "-", "Additional track tags."),
                            field("root_node", "resource_location", false, "-", "Optional single root node id."),
                            field("root_nodes", "array<resource_location>", false, "-", "Additional root node ids."),
                            field("exclusive_track", "resource_location", false, "-", "Optional single mutually-exclusive track id."),
                            field("exclusive_tracks", "array<resource_location>", false, "-", "Additional mutually-exclusive track ids.")
                    ),
                    List.of()
            ),
            new ReferenceSurface(
                    "upgrade_nodes",
                    "Upgrade Node Definitions",
                    "`data/<namespace>/xlib/upgrade_nodes/*.json`",
                    List.of(
                            "`requirement` and `requirements` both accept the shared upgrade requirement JSON vocabulary.",
                            "`point_costs`, `point_rewards`, and `counter_rewards` must use positive integer values."
                    ),
                    List.of(
                            field("id", "resource_location", false, "file id", "If omitted, the datapack file id becomes the runtime id."),
                            field("track", "resource_location", false, "-", "Owning track id."),
                            field("family", "resource_location", false, "-", "Optional node family metadata."),
                            field("group", "resource_location", false, "-", "Optional node group metadata."),
                            field("page", "resource_location", false, "-", "Optional node page metadata."),
                            field("tag", "resource_location", false, "-", "Optional single node tag."),
                            field("tags", "array<resource_location>", false, "-", "Additional node tags."),
                            field("choice_group", "resource_location", false, "-", "Optional mutually-exclusive choice group id."),
                            field("point_costs", "object<resource_location,int>", false, "-", "Point type ids mapped to positive costs."),
                            field("required_node", "resource_location", false, "-", "Optional single prerequisite node."),
                            field("required_nodes", "array<resource_location>", false, "-", "Additional prerequisite nodes."),
                            field("locked_node", "resource_location", false, "-", "Optional single node locked by this one."),
                            field("locked_nodes", "array<resource_location>", false, "-", "Additional nodes locked by this one."),
                            field("locked_track", "resource_location", false, "-", "Optional single track locked by this one."),
                            field("locked_tracks", "array<resource_location>", false, "-", "Additional tracks locked by this one."),
                            field("requirement", "upgrade_requirement", false, "-", "Optional single inline requirement."),
                            field("requirements", "array<upgrade_requirement>", false, "-", "Additional inline requirements."),
                            field("rewards", "object", false, "empty bundle", "Reward bundle object with bounded child fields.")
                    ),
                    List.of(
                            variant("rewards", "Bounded reward bundle object.",
                                    field("ability", "resource_location", false, "-", "Optional single granted ability id."),
                                    field("abilities", "array<resource_location>", false, "-", "Additional granted ability ids."),
                                    field("passive", "resource_location", false, "-", "Optional single granted passive id."),
                                    field("passives", "array<resource_location>", false, "-", "Additional granted passive ids."),
                                    field("granted_item", "resource_location", false, "-", "Optional single granted item id."),
                                    field("granted_items", "array<resource_location>", false, "-", "Additional granted item ids."),
                                    field("recipe_permission", "resource_location", false, "-", "Optional single granted recipe-permission id."),
                                    field("recipe_permissions", "array<resource_location>", false, "-", "Additional granted recipe-permission ids."),
                                    field("identity", "resource_location", false, "-", "Optional single granted identity id."),
                                    field("identities", "array<resource_location>", false, "-", "Additional granted identity ids."))
                    )
            ),
            new ReferenceSurface(
                    "upgrade_consume_rules",
                    "Upgrade Consume Rule Definitions",
                    "`data/<namespace>/xlib/upgrade_consume_rules/*.json`",
                    List.of(
                            "`condition` and `conditions` use the shared ability-requirement JSON vocabulary, including `condition_ref` ids.",
                            "`point_rewards` and `counter_rewards` must use positive integer values."
                    ),
                    List.of(
                            field("id", "resource_location", false, "file id", "If omitted, the datapack file id becomes the runtime id."),
                            field("item", "resource_location", false, "-", "Optional single consumed item id."),
                            field("items", "array<resource_location>", false, "-", "Additional consumed item ids."),
                            field("item_tag", "resource_location", false, "-", "Optional single consumed item tag id."),
                            field("item_tags", "array<resource_location>", false, "-", "Additional consumed item tag ids."),
                            field("food_only", "boolean", false, "false", "When true, the consumed stack must be food."),
                            field("condition", "ability_requirement", false, "-", "Optional single inline consume condition."),
                            field("conditions", "array<ability_requirement>", false, "-", "Additional inline consume conditions."),
                            field("point_rewards", "object<resource_location,int>", false, "-", "Point type ids mapped to positive rewards."),
                            field("counter_rewards", "object<resource_location,int>", false, "-", "Counter ids mapped to positive increments.")
                    ),
                    List.of()
            ),
            new ReferenceSurface(
                    "upgrade_kill_rules",
                    "Upgrade Kill Rule Definitions",
                    "`data/<namespace>/xlib/upgrade_kill_rules/*.json`",
                    List.of("`point_rewards` and `counter_rewards` must use positive integer values."),
                    List.of(
                            field("id", "resource_location", false, "file id", "If omitted, the datapack file id becomes the runtime id."),
                            field("target", "resource_location", false, "-", "Optional single target entity id."),
                            field("targets", "array<resource_location>", false, "-", "Additional target entity ids."),
                            field("target_tag", "resource_location", false, "-", "Optional single target entity tag id."),
                            field("target_tags", "array<resource_location>", false, "-", "Additional target entity tag ids."),
                            field("required_ability", "resource_location", false, "-", "Optional ability id that must be active on kill credit."),
                            field("point_rewards", "object<resource_location,int>", false, "-", "Point type ids mapped to positive rewards."),
                            field("counter_rewards", "object<resource_location,int>", false, "-", "Counter ids mapped to positive increments.")
                    ),
                    List.of()
            )
    );

    private ProgressionJsonReferenceDocs() {}

    public static List<ReferenceSurface> surfaces() {
        return SURFACES;
    }

    public static Optional<ReferenceSurface> findSurface(String id) {
        return SURFACES.stream().filter(surface -> surface.id().equals(id)).findFirst();
    }

    public static String renderMarkdown() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Progression JSON Reference\n\n");
        builder.append("This file is generated from the bounded progression authoring surfaces shipped by XLib.\n");
        builder.append("It is meant to stay aligned with the engine-owned readers instead of drifting into prose-only notes.\n\n");
        for (ReferenceSurface surface : SURFACES) {
            builder.append("## ").append(surface.title()).append("\n\n");
            builder.append("- Topic id: `").append(surface.id()).append("`\n");
            builder.append("- Authoring location: ").append(surface.location()).append("\n");
            for (String note : surface.notes()) {
                builder.append("- Note: ").append(note).append("\n");
            }
            builder.append("\n");
            if (!surface.fields().isEmpty()) {
                builder.append("| Field | Type | Required | Default | Notes |\n");
                builder.append("| --- | --- | --- | --- | --- |\n");
                for (ReferenceField field : surface.fields()) {
                    appendFieldRow(builder, field);
                }
                builder.append("\n");
            }
            if (!surface.variants().isEmpty()) {
                for (ReferenceVariant variant : surface.variants()) {
                    builder.append("### `").append(variant.id()).append("`\n\n");
                    builder.append(variant.summary()).append("\n\n");
                    if (!variant.fields().isEmpty()) {
                        builder.append("| Field | Type | Required | Default | Notes |\n");
                        builder.append("| --- | --- | --- | --- | --- |\n");
                        for (ReferenceField field : variant.fields()) {
                            appendFieldRow(builder, field);
                        }
                        builder.append("\n");
                    }
                }
            }
        }
        return builder.toString();
    }

    public static List<String> renderCommandLines(ReferenceSurface surface) {
        List<String> lines = new ArrayList<>();
        lines.add("reference=" + surface.id() + " | location=" + surface.location());
        for (String note : surface.notes()) {
            lines.add("note=" + note);
        }
        if (!surface.fields().isEmpty()) {
            lines.add("fields=" + joinFields(surface.fields()));
        }
        for (ReferenceVariant variant : surface.variants()) {
            lines.add("type=" + variant.id() + " | " + variant.summary() + " | fields=" + joinFields(variant.fields()));
        }
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

    private static void appendFieldRow(StringBuilder builder, ReferenceField field) {
        builder.append("| `")
                .append(field.name())
                .append("` | `")
                .append(field.type())
                .append("` | ")
                .append(field.required() ? "yes" : "no")
                .append(" | `")
                .append(field.defaultValue())
                .append("` | ")
                .append(field.notes())
                .append(" |\n");
    }

    private static ReferenceField field(String name, String type, boolean required, String defaultValue, String notes) {
        return new ReferenceField(name, type, required, defaultValue, notes);
    }

    private static ReferenceVariant variant(String id, String summary, ReferenceField... fields) {
        return new ReferenceVariant(id, summary, List.of(fields));
    }

    public record ReferenceSurface(
            String id,
            String title,
            String location,
            List<String> notes,
            List<ReferenceField> fields,
            List<ReferenceVariant> variants
    ) {
        public ReferenceSurface {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(location, "location");
            notes = List.copyOf(notes);
            fields = List.copyOf(fields);
            variants = List.copyOf(variants);
        }
    }

    public record ReferenceVariant(
            String id,
            String summary,
            List<ReferenceField> fields
    ) {
        public ReferenceVariant {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(summary, "summary");
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

        public String status() {
            return required ? "required" : "optional";
        }

        public String displayDefault() {
            return defaultValue.equals("-") ? "-" : defaultValue;
        }

        @Override
        public String toString() {
            return name + ":" + type + ":" + status().toLowerCase(Locale.ROOT);
        }
    }
}

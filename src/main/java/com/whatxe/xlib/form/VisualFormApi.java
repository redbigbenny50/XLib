package com.whatxe.xlib.form;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.XLibRegistryGuard;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class VisualFormApi {
    public static final ResourceLocation COMMAND_SOURCE =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "command_visual_form");

    private static final Map<ResourceLocation, VisualFormDefinition> DEFINITIONS = new LinkedHashMap<>();

    private VisualFormApi() {}

    public static void bootstrap() {}

    // --- Registration ---

    public static VisualFormDefinition register(VisualFormDefinition definition) {
        XLibRegistryGuard.ensureMutable("visual_forms");
        VisualFormDefinition previous = DEFINITIONS.putIfAbsent(definition.id(), definition);
        if (previous != null) {
            throw new IllegalStateException("Duplicate visual form registration: " + definition.id());
        }
        return definition;
    }

    public static Optional<VisualFormDefinition> unregister(ResourceLocation id) {
        XLibRegistryGuard.ensureMutable("visual_forms");
        return Optional.ofNullable(DEFINITIONS.remove(id));
    }

    public static Optional<VisualFormDefinition> findDefinition(ResourceLocation id) {
        return Optional.ofNullable(DEFINITIONS.get(id));
    }

    public static Collection<VisualFormDefinition> all() {
        return List.copyOf(DEFINITIONS.values());
    }

    // --- State queries ---

    public static Optional<VisualFormDefinition> active(Player player) {
        return getData(player).primaryForm().flatMap(id -> Optional.ofNullable(DEFINITIONS.get(id)));
    }

    public static boolean hasForm(Player player, ResourceLocation formId) {
        return getData(player).hasForm(formId) && DEFINITIONS.containsKey(formId);
    }

    public static Collection<ResourceLocation> activeForms(Player player) {
        return getData(player).activeForms();
    }

    // --- Apply / Revoke ---

    public static void apply(Player player, ResourceLocation formId, ResourceLocation sourceId) {
        update(player, getData(player).withForm(formId, sourceId));
    }

    public static void revoke(Player player, ResourceLocation formId, ResourceLocation sourceId) {
        VisualFormData data = getData(player);
        Optional<ResourceLocation> currentSource = data.sourceFor(formId);
        if (currentSource.isPresent() && currentSource.get().equals(sourceId)) {
            update(player, data.withoutForm(formId));
        }
    }

    public static void revokeSource(Player player, ResourceLocation sourceId) {
        update(player, getData(player).clearSource(sourceId));
    }

    public static void clearAll(Player player) {
        update(player, VisualFormData.empty());
    }

    // --- Sanitize ---

    public static VisualFormData sanitize(VisualFormData data) {
        return data.retainRegistered(DEFINITIONS.keySet());
    }

    // --- Attachment access ---

    public static VisualFormData getData(Player player) {
        return player.getData(ModAttachments.PLAYER_VISUAL_FORM);
    }

    public static void setData(Player player, VisualFormData data) {
        player.setData(ModAttachments.PLAYER_VISUAL_FORM, sanitize(data));
    }

    private static void update(Player player, VisualFormData data) {
        VisualFormData current = getData(player);
        if (!data.equals(current)) {
            setData(player, data);
        }
    }
}

package com.whatxe.xlib.progression;

import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class UpgradePointType {
    private final ResourceLocation id;

    private UpgradePointType(ResourceLocation id) {
        this.id = id;
    }

    public static UpgradePointType of(ResourceLocation id) {
        return new UpgradePointType(Objects.requireNonNull(id, "id"));
    }

    public ResourceLocation id() {
        return this.id;
    }

    public Component displayName() {
        return Component.translatable(this.translationKey());
    }

    public String translationKey() {
        return "upgrade_point." + this.id.getNamespace() + "." + this.id.getPath();
    }

    public Component description() {
        return Component.translatable(this.descriptionKey());
    }

    public String descriptionKey() {
        return this.translationKey() + ".desc";
    }
}

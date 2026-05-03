package com.whatxe.xlib.ability;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public final class AbilityIcon {
    public enum Kind {
        ITEM,
        TEXTURE,
        CUSTOM
    }

    private final Kind kind;
    @Nullable
    private final Item item;
    @Nullable
    private final ResourceLocation texture;
    @Nullable
    private final ResourceLocation customRendererId;
    private final int width;
    private final int height;

    private AbilityIcon(
            Kind kind,
            @Nullable Item item,
            @Nullable ResourceLocation texture,
            @Nullable ResourceLocation customRendererId,
            int width,
            int height
    ) {
        this.kind = kind;
        this.item = item;
        this.texture = texture;
        this.customRendererId = customRendererId;
        this.width = width;
        this.height = height;
    }

    public static AbilityIcon ofItem(Item item) {
        return new AbilityIcon(Kind.ITEM, Objects.requireNonNull(item, "item"), null, null, 16, 16);
    }

    public static AbilityIcon ofTexture(ResourceLocation texture) {
        return ofTexture(texture, 16, 16);
    }

    public static AbilityIcon ofTexture(ResourceLocation texture, int width, int height) {
        return new AbilityIcon(Kind.TEXTURE, null, Objects.requireNonNull(texture, "texture"), null, width, height);
    }

    public static AbilityIcon ofCustom(ResourceLocation customRendererId) {
        return new AbilityIcon(Kind.CUSTOM, null, null, Objects.requireNonNull(customRendererId, "customRendererId"), 16, 16);
    }

    public Kind kind() {
        return this.kind;
    }

    @Nullable
    public Item item() {
        return this.item;
    }

    @Nullable
    public ResourceLocation texture() {
        return this.texture;
    }

    @Nullable
    public ResourceLocation customRendererId() {
        return this.customRendererId;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }
}


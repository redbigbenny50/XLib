package com.whatxe.xlib.api.event;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;

public abstract class XLibMenuOpenEvent extends Event {
    private final MenuType menuType;
    private final ResourceLocation factoryId;
    private final @Nullable ResourceLocation contextId;

    protected XLibMenuOpenEvent(MenuType menuType, ResourceLocation factoryId, @Nullable ResourceLocation contextId) {
        this.menuType = menuType;
        this.factoryId = factoryId;
        this.contextId = contextId;
    }

    public MenuType menuType() {
        return this.menuType;
    }

    public ResourceLocation factoryId() {
        return this.factoryId;
    }

    public @Nullable ResourceLocation contextId() {
        return this.contextId;
    }

    public enum MenuType {
        ABILITY,
        PROGRESSION,
        PROFILE_SELECTION
    }

    public static final class Pre extends XLibMenuOpenEvent implements ICancellableEvent {
        public Pre(MenuType menuType, ResourceLocation factoryId, @Nullable ResourceLocation contextId) {
            super(menuType, factoryId, contextId);
        }
    }

    public static final class Post extends XLibMenuOpenEvent {
        public Post(MenuType menuType, ResourceLocation factoryId, @Nullable ResourceLocation contextId) {
            super(menuType, factoryId, contextId);
        }
    }
}

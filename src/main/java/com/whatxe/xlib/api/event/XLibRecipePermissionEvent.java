package com.whatxe.xlib.api.event;

import com.whatxe.xlib.ability.AbilityData;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public final class XLibRecipePermissionEvent extends Event {
    private final ServerPlayer player;
    private final ResourceLocation recipeId;
    private final Set<ResourceLocation> previousSources;
    private final Set<ResourceLocation> currentSources;
    private final AbilityData previousData;
    private final AbilityData currentData;

    public XLibRecipePermissionEvent(
            ServerPlayer player,
            ResourceLocation recipeId,
            Set<ResourceLocation> previousSources,
            Set<ResourceLocation> currentSources,
            AbilityData previousData,
            AbilityData currentData
    ) {
        this.player = player;
        this.recipeId = recipeId;
        this.previousSources = Set.copyOf(previousSources);
        this.currentSources = Set.copyOf(currentSources);
        this.previousData = previousData;
        this.currentData = currentData;
    }

    public ServerPlayer player() {
        return this.player;
    }

    public ResourceLocation recipeId() {
        return this.recipeId;
    }

    public Set<ResourceLocation> previousSources() {
        return this.previousSources;
    }

    public Set<ResourceLocation> currentSources() {
        return this.currentSources;
    }

    public AbilityData previousData() {
        return this.previousData;
    }

    public AbilityData currentData() {
        return this.currentData;
    }

    public boolean wasAllowed() {
        return !this.previousSources.isEmpty();
    }

    public boolean isAllowed() {
        return !this.currentSources.isEmpty();
    }
}

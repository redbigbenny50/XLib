package com.whatxe.xlib.ability;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

public final class RestrictedRecipeRule {
    private final ResourceLocation id;
    private final Set<ResourceLocation> recipeTags;
    private final Set<ResourceLocation> categories;
    private final Set<ResourceLocation> outputs;
    private final Set<ResourceLocation> unlockSources;
    private final Set<ResourceLocation> unlockAdvancements;
    private final @Nullable CompoundTag outputTag;
    private final @Nullable Component unlockHint;
    private final boolean hiddenWhenLocked;

    private RestrictedRecipeRule(
            ResourceLocation id,
            Set<ResourceLocation> recipeTags,
            Set<ResourceLocation> categories,
            Set<ResourceLocation> outputs,
            Set<ResourceLocation> unlockSources,
            Set<ResourceLocation> unlockAdvancements,
            @Nullable CompoundTag outputTag,
            @Nullable Component unlockHint,
            boolean hiddenWhenLocked
    ) {
        this.id = id;
        this.recipeTags = Set.copyOf(recipeTags);
        this.categories = Set.copyOf(categories);
        this.outputs = Set.copyOf(outputs);
        this.unlockSources = Set.copyOf(unlockSources);
        this.unlockAdvancements = Set.copyOf(unlockAdvancements);
        this.outputTag = outputTag != null ? outputTag.copy() : null;
        this.unlockHint = unlockHint;
        this.hiddenWhenLocked = hiddenWhenLocked;
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public Set<ResourceLocation> recipeTags() {
        return this.recipeTags;
    }

    public Set<ResourceLocation> categories() {
        return this.categories;
    }

    public Set<ResourceLocation> outputs() {
        return this.outputs;
    }

    public Set<ResourceLocation> unlockSources() {
        return this.unlockSources;
    }

    public Set<ResourceLocation> unlockAdvancements() {
        return this.unlockAdvancements;
    }

    public @Nullable CompoundTag outputTag() {
        return this.outputTag != null ? this.outputTag.copy() : null;
    }

    public @Nullable Component unlockHint() {
        return this.unlockHint;
    }

    public boolean hiddenWhenLocked() {
        return this.hiddenWhenLocked;
    }

    public boolean hasSelectors() {
        return !this.recipeTags.isEmpty() || !this.categories.isEmpty() || !this.outputs.isEmpty() || this.outputTag != null;
    }

    public boolean matches(RecipeHolder<?> recipe, RegistryAccess registries) {
        boolean matchedAnySelector = false;

        if (!this.recipeTags.isEmpty()) {
            boolean matchesTag = registries.registryOrThrow(Registries.RECIPE)
                    .getHolder(ResourceKey.create(Registries.RECIPE, recipe.id()))
                    .stream()
                    .anyMatch(holder -> this.recipeTags.stream()
                            .map(tagId -> TagKey.create(Registries.RECIPE, tagId))
                            .anyMatch(holder::is));
            if (!matchesTag) {
                return false;
            }
            matchedAnySelector = true;
        }

        if (!this.categories.isEmpty()) {
            if (!(recipe.value() instanceof CraftingRecipe craftingRecipe)) {
                return false;
            }
            if (!this.categories.contains(RecipePermissionApi.categoryId(craftingRecipe.category()))) {
                return false;
            }
            matchedAnySelector = true;
        }

        if (!this.outputs.isEmpty() || this.outputTag != null) {
            ItemStack resultStack = recipe.value().getResultItem(registries);
            if (resultStack.isEmpty()) {
                return false;
            }
            if (!this.outputs.isEmpty()) {
                ResourceLocation outputId = BuiltInRegistriesHelper.itemId(resultStack.getItem());
                if (!this.outputs.contains(outputId)) {
                    return false;
                }
            }
            if (this.outputTag != null && !NbtUtils.compareNbt(this.outputTag, resultStack.save(registries), true)) {
                return false;
            }
            matchedAnySelector = true;
        }

        return matchedAnySelector;
    }

    public RestrictedRecipeDefinition toDefinition(RecipeHolder<?> recipe, RegistryAccess registries) {
        RestrictedRecipeDefinition.Builder builder = RestrictedRecipeDefinition.builder(recipe.id())
                .recipeTags(this.recipeTags)
                .categories(this.categories)
                .outputs(this.outputs)
                .unlockSources(this.unlockSources)
                .unlockAdvancements(this.unlockAdvancements)
                .hiddenWhenLocked(this.hiddenWhenLocked);

        ItemStack resultStack = recipe.value().getResultItem(registries);
        if (!resultStack.isEmpty()) {
            builder.output(BuiltInRegistriesHelper.itemId(resultStack.getItem()));
        }
        if (this.outputTag != null) {
            builder.outputTag(this.outputTag);
        }
        if (this.unlockHint != null) {
            builder.unlockHint(this.unlockHint);
        }
        return builder.build();
    }

    private static final class BuiltInRegistriesHelper {
        private BuiltInRegistriesHelper() {}

        private static ResourceLocation itemId(Item item) {
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        }
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final Set<ResourceLocation> recipeTags = new LinkedHashSet<>();
        private final Set<ResourceLocation> categories = new LinkedHashSet<>();
        private final Set<ResourceLocation> outputs = new LinkedHashSet<>();
        private final Set<ResourceLocation> unlockSources = new LinkedHashSet<>();
        private final Set<ResourceLocation> unlockAdvancements = new LinkedHashSet<>();
        private CompoundTag outputTag;
        private Component unlockHint;
        private boolean hiddenWhenLocked = true;

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder recipeTag(ResourceLocation recipeTagId) {
            this.recipeTags.add(Objects.requireNonNull(recipeTagId, "recipeTagId"));
            return this;
        }

        public Builder recipeTags(Collection<ResourceLocation> recipeTagIds) {
            recipeTagIds.stream().filter(Objects::nonNull).forEach(this.recipeTags::add);
            return this;
        }

        public Builder category(ResourceLocation categoryId) {
            this.categories.add(Objects.requireNonNull(categoryId, "categoryId"));
            return this;
        }

        public Builder categories(Collection<ResourceLocation> categoryIds) {
            categoryIds.stream().filter(Objects::nonNull).forEach(this.categories::add);
            return this;
        }

        public Builder output(ResourceLocation itemId) {
            this.outputs.add(Objects.requireNonNull(itemId, "itemId"));
            return this;
        }

        public Builder outputs(Collection<ResourceLocation> itemIds) {
            itemIds.stream().filter(Objects::nonNull).forEach(this.outputs::add);
            return this;
        }

        public Builder unlockSource(ResourceLocation sourceId) {
            this.unlockSources.add(Objects.requireNonNull(sourceId, "sourceId"));
            return this;
        }

        public Builder unlockSources(Collection<ResourceLocation> sourceIds) {
            sourceIds.stream().filter(Objects::nonNull).forEach(this.unlockSources::add);
            return this;
        }

        public Builder unlockAdvancement(ResourceLocation advancementId) {
            this.unlockAdvancements.add(Objects.requireNonNull(advancementId, "advancementId"));
            return this;
        }

        public Builder unlockAdvancements(Collection<ResourceLocation> advancementIds) {
            advancementIds.stream().filter(Objects::nonNull).forEach(this.unlockAdvancements::add);
            return this;
        }

        public Builder outputTag(CompoundTag outputTag) {
            this.outputTag = Objects.requireNonNull(outputTag, "outputTag").copy();
            return this;
        }

        public Builder unlockHint(Component unlockHint) {
            this.unlockHint = Objects.requireNonNull(unlockHint, "unlockHint");
            return this;
        }

        public Builder hiddenWhenLocked(boolean hiddenWhenLocked) {
            this.hiddenWhenLocked = hiddenWhenLocked;
            return this;
        }

        public RestrictedRecipeRule build() {
            return new RestrictedRecipeRule(
                    this.id,
                    this.recipeTags,
                    this.categories,
                    this.outputs,
                    this.unlockSources,
                    this.unlockAdvancements,
                    this.outputTag,
                    this.unlockHint,
                    this.hiddenWhenLocked
            );
        }
    }
}

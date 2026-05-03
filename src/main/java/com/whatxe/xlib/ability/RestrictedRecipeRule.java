package com.whatxe.xlib.ability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
    private final ResourceLocation familyId;
    private final ResourceLocation groupId;
    private final ResourceLocation pageId;
    private final Set<ResourceLocation> tags;
    private final int priority;
    private final RecipeRuleMode mode;
    private final boolean matchAll;
    private final Set<ResourceLocation> recipeTags;
    private final Set<String> recipeNamespaces;
    private final Set<ResourceLocation> categories;
    private final Set<ResourceLocation> outputs;
    private final Set<ResourceLocation> outputItemTags;
    private final Set<ResourceLocation> unlockSources;
    private final Set<ResourceLocation> unlockAdvancements;
    private final @Nullable CompoundTag outputTag;
    private final @Nullable Component unlockHint;
    private final boolean hiddenWhenLocked;

    private RestrictedRecipeRule(
            ResourceLocation id,
            ResourceLocation familyId,
            ResourceLocation groupId,
            ResourceLocation pageId,
            Set<ResourceLocation> tags,
            int priority,
            RecipeRuleMode mode,
            boolean matchAll,
            Set<ResourceLocation> recipeTags,
            Set<String> recipeNamespaces,
            Set<ResourceLocation> categories,
            Set<ResourceLocation> outputs,
            Set<ResourceLocation> outputItemTags,
            Set<ResourceLocation> unlockSources,
            Set<ResourceLocation> unlockAdvancements,
            @Nullable CompoundTag outputTag,
            @Nullable Component unlockHint,
            boolean hiddenWhenLocked
    ) {
        this.id = id;
        this.familyId = familyId;
        this.groupId = groupId;
        this.pageId = pageId;
        this.tags = Set.copyOf(tags);
        this.priority = priority;
        this.mode = Objects.requireNonNull(mode, "mode");
        this.matchAll = matchAll;
        this.recipeTags = Set.copyOf(recipeTags);
        this.recipeNamespaces = Set.copyOf(recipeNamespaces);
        this.categories = Set.copyOf(categories);
        this.outputs = Set.copyOf(outputs);
        this.outputItemTags = Set.copyOf(outputItemTags);
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

    public Optional<ResourceLocation> familyId() {
        return Optional.ofNullable(this.familyId);
    }

    public Optional<ResourceLocation> groupId() {
        return Optional.ofNullable(this.groupId);
    }

    public Optional<ResourceLocation> pageId() {
        return Optional.ofNullable(this.pageId);
    }

    public Set<ResourceLocation> tags() {
        return this.tags;
    }

    public boolean hasTag(ResourceLocation tagId) {
        return this.tags.contains(tagId);
    }

    public int priority() {
        return this.priority;
    }

    public RecipeRuleMode mode() {
        return this.mode;
    }

    public boolean matchAll() {
        return this.matchAll;
    }

    public List<ResourceLocation> metadataIds() {
        List<ResourceLocation> ids = new ArrayList<>(3 + this.tags.size());
        if (this.familyId != null) {
            ids.add(this.familyId);
        }
        if (this.groupId != null) {
            ids.add(this.groupId);
        }
        if (this.pageId != null) {
            ids.add(this.pageId);
        }
        ids.addAll(this.tags);
        return List.copyOf(ids);
    }

    public Set<ResourceLocation> recipeTags() {
        return this.recipeTags;
    }

    public Set<String> recipeNamespaces() {
        return this.recipeNamespaces;
    }

    public Set<ResourceLocation> categories() {
        return this.categories;
    }

    public Set<ResourceLocation> outputs() {
        return this.outputs;
    }

    public Set<ResourceLocation> outputItemTags() {
        return this.outputItemTags;
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
        return this.matchAll
                || !this.recipeTags.isEmpty()
                || !this.recipeNamespaces.isEmpty()
                || !this.categories.isEmpty()
                || !this.outputs.isEmpty()
                || !this.outputItemTags.isEmpty()
                || this.outputTag != null;
    }

    public boolean matches(RecipeHolder<?> recipe, RegistryAccess registries) {
        boolean matchedAnySelector = this.matchAll;

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

        if (!this.recipeNamespaces.isEmpty()) {
            if (!this.recipeNamespaces.contains(recipe.id().getNamespace())) {
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

        if (!this.outputs.isEmpty() || !this.outputItemTags.isEmpty() || this.outputTag != null) {
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
            if (!this.outputItemTags.isEmpty()) {
                boolean matchesOutputTag = this.outputItemTags.stream()
                        .map(tagId -> TagKey.create(Registries.ITEM, tagId))
                        .anyMatch(resultStack::is);
                if (!matchesOutputTag) {
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
                .recipeNamespaces(this.recipeNamespaces)
                .categories(this.categories)
                .outputs(this.outputs)
                .outputItemTags(this.outputItemTags)
                .unlockSources(this.unlockSources)
                .unlockAdvancements(this.unlockAdvancements)
                .hiddenWhenLocked(this.hiddenWhenLocked)
                .matchedRule(this.id, this.priority)
                .exempted(this.mode == RecipeRuleMode.EXEMPT);
        if (this.familyId != null) {
            builder.family(this.familyId);
        }
        if (this.groupId != null) {
            builder.group(this.groupId);
        }
        if (this.pageId != null) {
            builder.page(this.pageId);
        }
        builder.tags(this.tags);

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
        private ResourceLocation familyId;
        private ResourceLocation groupId;
        private ResourceLocation pageId;
        private final Set<ResourceLocation> tags = new LinkedHashSet<>();
        private int priority;
        private RecipeRuleMode mode = RecipeRuleMode.RESTRICT;
        private boolean matchAll;
        private final Set<ResourceLocation> recipeTags = new LinkedHashSet<>();
        private final Set<String> recipeNamespaces = new LinkedHashSet<>();
        private final Set<ResourceLocation> categories = new LinkedHashSet<>();
        private final Set<ResourceLocation> outputs = new LinkedHashSet<>();
        private final Set<ResourceLocation> outputItemTags = new LinkedHashSet<>();
        private final Set<ResourceLocation> unlockSources = new LinkedHashSet<>();
        private final Set<ResourceLocation> unlockAdvancements = new LinkedHashSet<>();
        private CompoundTag outputTag;
        private Component unlockHint;
        private boolean hiddenWhenLocked = true;

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder family(ResourceLocation familyId) {
            this.familyId = Objects.requireNonNull(familyId, "familyId");
            return this;
        }

        public Builder group(ResourceLocation groupId) {
            this.groupId = Objects.requireNonNull(groupId, "groupId");
            return this;
        }

        public Builder page(ResourceLocation pageId) {
            this.pageId = Objects.requireNonNull(pageId, "pageId");
            return this;
        }

        public Builder tag(ResourceLocation tagId) {
            this.tags.add(Objects.requireNonNull(tagId, "tagId"));
            return this;
        }

        public Builder tags(Collection<ResourceLocation> tagIds) {
            tagIds.stream().filter(Objects::nonNull).forEach(this.tags::add);
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder mode(RecipeRuleMode mode) {
            this.mode = Objects.requireNonNull(mode, "mode");
            return this;
        }

        public Builder matchAll(boolean matchAll) {
            this.matchAll = matchAll;
            return this;
        }

        public Builder recipeTag(ResourceLocation recipeTagId) {
            this.recipeTags.add(Objects.requireNonNull(recipeTagId, "recipeTagId"));
            return this;
        }

        public Builder recipeTags(Collection<ResourceLocation> recipeTagIds) {
            recipeTagIds.stream().filter(Objects::nonNull).forEach(this.recipeTags::add);
            return this;
        }

        public Builder recipeNamespace(String namespace) {
            this.recipeNamespaces.add(Objects.requireNonNull(namespace, "namespace"));
            return this;
        }

        public Builder recipeNamespaces(Collection<String> namespaces) {
            namespaces.stream().filter(Objects::nonNull).forEach(this.recipeNamespaces::add);
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

        public Builder outputItemTag(ResourceLocation tagId) {
            this.outputItemTags.add(Objects.requireNonNull(tagId, "tagId"));
            return this;
        }

        public Builder outputItemTags(Collection<ResourceLocation> tagIds) {
            tagIds.stream().filter(Objects::nonNull).forEach(this.outputItemTags::add);
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
                    this.familyId,
                    this.groupId,
                    this.pageId,
                    this.tags,
                    this.priority,
                    this.mode,
                    this.matchAll,
                    this.recipeTags,
                    this.recipeNamespaces,
                    this.categories,
                    this.outputs,
                    this.outputItemTags,
                    this.unlockSources,
                    this.unlockAdvancements,
                    this.outputTag,
                    this.unlockHint,
                    this.hiddenWhenLocked
            );
        }
    }
}

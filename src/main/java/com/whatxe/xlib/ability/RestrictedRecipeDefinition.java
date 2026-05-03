package com.whatxe.xlib.ability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class RestrictedRecipeDefinition {
    private final ResourceLocation recipeId;
    private final ResourceLocation familyId;
    private final ResourceLocation groupId;
    private final ResourceLocation pageId;
    private final Set<ResourceLocation> tags;
    private final Set<ResourceLocation> recipeTags;
    private final Set<String> recipeNamespaces;
    private final Set<ResourceLocation> categories;
    private final Set<ResourceLocation> outputs;
    private final Set<ResourceLocation> outputItemTags;
    private final Set<ResourceLocation> unlockSources;
    private final Set<ResourceLocation> unlockAdvancements;
    private final ResourceLocation matchedRuleId;
    private final int matchedRulePriority;
    private final @Nullable CompoundTag outputTag;
    private final @Nullable Component unlockHint;
    private final boolean hiddenWhenLocked;
    private final boolean exempted;

    private RestrictedRecipeDefinition(
            ResourceLocation recipeId,
            ResourceLocation familyId,
            ResourceLocation groupId,
            ResourceLocation pageId,
            Set<ResourceLocation> tags,
            Set<ResourceLocation> recipeTags,
            Set<String> recipeNamespaces,
            Set<ResourceLocation> categories,
            Set<ResourceLocation> outputs,
            Set<ResourceLocation> outputItemTags,
            Set<ResourceLocation> unlockSources,
            Set<ResourceLocation> unlockAdvancements,
            ResourceLocation matchedRuleId,
            int matchedRulePriority,
            @Nullable CompoundTag outputTag,
            @Nullable Component unlockHint,
            boolean hiddenWhenLocked,
            boolean exempted
    ) {
        this.recipeId = recipeId;
        this.familyId = familyId;
        this.groupId = groupId;
        this.pageId = pageId;
        this.tags = Set.copyOf(tags);
        this.recipeTags = Set.copyOf(recipeTags);
        this.recipeNamespaces = Set.copyOf(recipeNamespaces);
        this.categories = Set.copyOf(categories);
        this.outputs = Set.copyOf(outputs);
        this.outputItemTags = Set.copyOf(outputItemTags);
        this.unlockSources = Set.copyOf(unlockSources);
        this.unlockAdvancements = Set.copyOf(unlockAdvancements);
        this.matchedRuleId = matchedRuleId;
        this.matchedRulePriority = matchedRulePriority;
        this.outputTag = outputTag != null ? outputTag.copy() : null;
        this.unlockHint = unlockHint;
        this.hiddenWhenLocked = hiddenWhenLocked;
        this.exempted = exempted;
    }

    public static Builder builder(ResourceLocation recipeId) {
        return new Builder(recipeId);
    }

    public ResourceLocation recipeId() {
        return this.recipeId;
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

    public Optional<ResourceLocation> matchedRuleId() {
        return Optional.ofNullable(this.matchedRuleId);
    }

    public int matchedRulePriority() {
        return this.matchedRulePriority;
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

    /**
     * Returns {@code true} when this definition was produced by a {@link RecipeRuleMode#EXEMPT} rule,
     * meaning the recipe should be treated as unrestricted regardless of lower-priority restrictions.
     */
    public boolean exempted() {
        return this.exempted;
    }

    public static final class Builder {
        private final ResourceLocation recipeId;
        private ResourceLocation familyId;
        private ResourceLocation groupId;
        private ResourceLocation pageId;
        private final Set<ResourceLocation> tags = new LinkedHashSet<>();
        private final Set<ResourceLocation> recipeTags = new LinkedHashSet<>();
        private final Set<String> recipeNamespaces = new LinkedHashSet<>();
        private final Set<ResourceLocation> categories = new LinkedHashSet<>();
        private final Set<ResourceLocation> outputs = new LinkedHashSet<>();
        private final Set<ResourceLocation> outputItemTags = new LinkedHashSet<>();
        private final Set<ResourceLocation> unlockSources = new LinkedHashSet<>();
        private final Set<ResourceLocation> unlockAdvancements = new LinkedHashSet<>();
        private ResourceLocation matchedRuleId;
        private int matchedRulePriority;
        private CompoundTag outputTag;
        private Component unlockHint;
        private boolean hiddenWhenLocked = true;
        private boolean exempted = false;

        private Builder(ResourceLocation recipeId) {
            this.recipeId = Objects.requireNonNull(recipeId, "recipeId");
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

        public Builder matchedRule(ResourceLocation ruleId, int priority) {
            this.matchedRuleId = Objects.requireNonNull(ruleId, "ruleId");
            this.matchedRulePriority = priority;
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

        public Builder exempted(boolean exempted) {
            this.exempted = exempted;
            return this;
        }

        public RestrictedRecipeDefinition build() {
            return new RestrictedRecipeDefinition(
                    this.recipeId,
                    this.familyId,
                    this.groupId,
                    this.pageId,
                    this.tags,
                    this.recipeTags,
                    this.recipeNamespaces,
                    this.categories,
                    this.outputs,
                    this.outputItemTags,
                    this.unlockSources,
                    this.unlockAdvancements,
                    this.matchedRuleId,
                    this.matchedRulePriority,
                    this.outputTag,
                    this.unlockHint,
                    this.hiddenWhenLocked,
                    this.exempted
            );
        }
    }
}

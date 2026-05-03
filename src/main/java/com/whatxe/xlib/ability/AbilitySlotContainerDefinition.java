package com.whatxe.xlib.ability;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class AbilitySlotContainerDefinition {
    @FunctionalInterface
    public interface Predicate {
        boolean test(@Nullable Player player, AbilityData data);
    }

    private final ResourceLocation id;
    private final Component displayName;
    private final AbilitySlotContainerOwnerType ownerType;
    private final @Nullable ResourceLocation ownerId;
    private final int slotsPerPage;
    private final int defaultPageCount;
    private final int displayOrder;
    private final @Nullable ResourceLocation controlProfileId;
    private final Set<ResourceLocation> tags;
    private final Predicate visibilityPredicate;
    private final Predicate editabilityPredicate;

    private AbilitySlotContainerDefinition(
            ResourceLocation id,
            Component displayName,
            AbilitySlotContainerOwnerType ownerType,
            @Nullable ResourceLocation ownerId,
            int slotsPerPage,
            int defaultPageCount,
            int displayOrder,
            @Nullable ResourceLocation controlProfileId,
            Set<ResourceLocation> tags,
            Predicate visibilityPredicate,
            Predicate editabilityPredicate
    ) {
        this.id = id;
        this.displayName = displayName.copy();
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.slotsPerPage = slotsPerPage;
        this.defaultPageCount = defaultPageCount;
        this.displayOrder = displayOrder;
        this.controlProfileId = controlProfileId;
        this.tags = Set.copyOf(tags);
        this.visibilityPredicate = visibilityPredicate;
        this.editabilityPredicate = editabilityPredicate;
    }

    public static Builder builder(ResourceLocation id, Component displayName) {
        return new Builder(id, displayName);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public Component displayName() {
        return this.displayName.copy();
    }

    public AbilitySlotContainerOwnerType ownerType() {
        return this.ownerType;
    }

    public @Nullable ResourceLocation ownerId() {
        return this.ownerId;
    }

    public int slotsPerPage() {
        return this.slotsPerPage;
    }

    public int defaultPageCount() {
        return this.defaultPageCount;
    }

    public int displayOrder() {
        return this.displayOrder;
    }

    public @Nullable ResourceLocation controlProfileId() {
        return this.controlProfileId;
    }

    public Set<ResourceLocation> tags() {
        return this.tags;
    }

    public boolean hasTag(ResourceLocation tagId) {
        return this.tags.contains(tagId);
    }

    public boolean isVisible(@Nullable Player player, AbilityData data) {
        return this.visibilityPredicate.test(player, data);
    }

    public boolean isEditable(@Nullable Player player, AbilityData data) {
        return this.editabilityPredicate.test(player, data);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final Component displayName;
        private AbilitySlotContainerOwnerType ownerType = AbilitySlotContainerOwnerType.BASELINE;
        private @Nullable ResourceLocation ownerId;
        private int slotsPerPage = AbilityData.SLOT_COUNT;
        private int defaultPageCount = 1;
        private int displayOrder;
        private @Nullable ResourceLocation controlProfileId;
        private final Set<ResourceLocation> tags = new LinkedHashSet<>();
        private Predicate visibilityPredicate = (player, data) -> true;
        private Predicate editabilityPredicate = (player, data) -> true;

        private Builder(ResourceLocation id, Component displayName) {
            this.id = Objects.requireNonNull(id, "id");
            this.displayName = Objects.requireNonNull(displayName, "displayName");
        }

        public Builder owner(AbilitySlotContainerOwnerType ownerType, @Nullable ResourceLocation ownerId) {
            this.ownerType = Objects.requireNonNull(ownerType, "ownerType");
            this.ownerId = ownerId;
            return this;
        }

        public Builder slotsPerPage(int slotsPerPage) {
            if (slotsPerPage <= 0) {
                throw new IllegalArgumentException("slotsPerPage must be positive");
            }
            this.slotsPerPage = slotsPerPage;
            return this;
        }

        public Builder defaultPageCount(int defaultPageCount) {
            if (defaultPageCount <= 0) {
                throw new IllegalArgumentException("defaultPageCount must be positive");
            }
            this.defaultPageCount = defaultPageCount;
            return this;
        }

        public Builder displayOrder(int displayOrder) {
            this.displayOrder = displayOrder;
            return this;
        }

        public Builder controlProfile(ResourceLocation controlProfileId) {
            this.controlProfileId = Objects.requireNonNull(controlProfileId, "controlProfileId");
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

        public Builder visibleWhen(Predicate predicate) {
            this.visibilityPredicate = Objects.requireNonNull(predicate, "predicate");
            return this;
        }

        public Builder editableWhen(Predicate predicate) {
            this.editabilityPredicate = Objects.requireNonNull(predicate, "predicate");
            return this;
        }

        public AbilitySlotContainerDefinition build() {
            return new AbilitySlotContainerDefinition(
                    this.id,
                    this.displayName,
                    this.ownerType,
                    this.ownerId,
                    this.slotsPerPage,
                    this.defaultPageCount,
                    this.displayOrder,
                    this.controlProfileId,
                    this.tags,
                    this.visibilityPredicate,
                    this.editabilityPredicate
            );
        }
    }
}

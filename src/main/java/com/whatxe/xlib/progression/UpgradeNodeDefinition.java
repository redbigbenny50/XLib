package com.whatxe.xlib.progression;

import com.whatxe.xlib.XLib;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class UpgradeNodeDefinition {
    private final ResourceLocation id;
    private final @Nullable ResourceLocation trackId;
    private final ResourceLocation familyId;
    private final ResourceLocation groupId;
    private final ResourceLocation pageId;
    private final Set<ResourceLocation> tags;
    private final @Nullable ResourceLocation choiceGroupId;
    private final Map<ResourceLocation, Integer> pointCosts;
    private final Set<ResourceLocation> requiredNodes;
    private final Set<ResourceLocation> lockedNodes;
    private final Set<ResourceLocation> lockedTracks;
    private final List<UpgradeRequirement> requirements;
    private final UpgradeRewardBundle rewards;

    private UpgradeNodeDefinition(
            ResourceLocation id,
            @Nullable ResourceLocation trackId,
            ResourceLocation familyId,
            ResourceLocation groupId,
            ResourceLocation pageId,
            Set<ResourceLocation> tags,
            @Nullable ResourceLocation choiceGroupId,
            Map<ResourceLocation, Integer> pointCosts,
            Set<ResourceLocation> requiredNodes,
            Set<ResourceLocation> lockedNodes,
            Set<ResourceLocation> lockedTracks,
            List<UpgradeRequirement> requirements,
            UpgradeRewardBundle rewards
    ) {
        this.id = id;
        this.trackId = trackId;
        this.familyId = familyId;
        this.groupId = groupId;
        this.pageId = pageId;
        this.tags = copyTags(tags);
        this.choiceGroupId = choiceGroupId;
        this.pointCosts = Map.copyOf(pointCosts);
        this.requiredNodes = Set.copyOf(requiredNodes);
        this.lockedNodes = Set.copyOf(lockedNodes);
        this.lockedTracks = Set.copyOf(lockedTracks);
        this.requirements = List.copyOf(requirements);
        this.rewards = rewards;
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public @Nullable ResourceLocation trackId() {
        return this.trackId;
    }

    public java.util.Optional<ResourceLocation> familyId() {
        return java.util.Optional.ofNullable(this.familyId);
    }

    public java.util.Optional<ResourceLocation> groupId() {
        return java.util.Optional.ofNullable(this.groupId);
    }

    public java.util.Optional<ResourceLocation> pageId() {
        return java.util.Optional.ofNullable(this.pageId);
    }

    public Set<ResourceLocation> tags() {
        return this.tags;
    }

    public boolean hasTag(ResourceLocation tagId) {
        return this.tags.contains(tagId);
    }

    public java.util.Optional<ResourceLocation> choiceGroupId() {
        return java.util.Optional.ofNullable(this.choiceGroupId);
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

    public Map<ResourceLocation, Integer> pointCosts() {
        return this.pointCosts;
    }

    public Set<ResourceLocation> requiredNodes() {
        return this.requiredNodes;
    }

    public Set<ResourceLocation> lockedNodes() {
        return this.lockedNodes;
    }

    public Set<ResourceLocation> lockedTracks() {
        return this.lockedTracks;
    }

    public List<UpgradeRequirement> requirements() {
        return this.requirements;
    }

    public UpgradeRewardBundle rewards() {
        return this.rewards;
    }

    public ResourceLocation sourceId() {
        return ResourceLocation.fromNamespaceAndPath(
                XLib.MODID,
                "upgrade_node/" + this.id.getNamespace() + "/" + this.id.getPath()
        );
    }

    public Component displayName() {
        return Component.translatable(this.translationKey());
    }

    public String translationKey() {
        return "upgrade_node." + this.id.getNamespace() + "." + this.id.getPath();
    }

    private static Set<ResourceLocation> copyTags(Collection<ResourceLocation> source) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }

    public static final class Builder {
        private final ResourceLocation id;
        private ResourceLocation trackId;
        private ResourceLocation familyId;
        private ResourceLocation groupId;
        private ResourceLocation pageId;
        private final Set<ResourceLocation> tags = new LinkedHashSet<>();
        private ResourceLocation choiceGroupId;
        private final Map<ResourceLocation, Integer> pointCosts = new LinkedHashMap<>();
        private final Set<ResourceLocation> requiredNodes = new LinkedHashSet<>();
        private final Set<ResourceLocation> lockedNodes = new LinkedHashSet<>();
        private final Set<ResourceLocation> lockedTracks = new LinkedHashSet<>();
        private final List<UpgradeRequirement> requirements = new java.util.ArrayList<>();
        private UpgradeRewardBundle rewards = UpgradeRewardBundle.builder().build();

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder track(ResourceLocation trackId) {
            this.trackId = Objects.requireNonNull(trackId, "trackId");
            return this;
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

        public Builder choiceGroup(ResourceLocation choiceGroupId) {
            this.choiceGroupId = Objects.requireNonNull(choiceGroupId, "choiceGroupId");
            return this;
        }

        public Builder pointCost(ResourceLocation pointTypeId, int amount) {
            if (amount <= 0) {
                throw new IllegalArgumentException("Point costs must be positive");
            }
            this.pointCosts.put(Objects.requireNonNull(pointTypeId, "pointTypeId"), amount);
            return this;
        }

        public Builder requiredNode(ResourceLocation nodeId) {
            this.requiredNodes.add(Objects.requireNonNull(nodeId, "nodeId"));
            return this;
        }

        public Builder requiredNodes(Collection<ResourceLocation> nodeIds) {
            nodeIds.stream().filter(Objects::nonNull).forEach(this.requiredNodes::add);
            return this;
        }

        public Builder lockedNode(ResourceLocation nodeId) {
            this.lockedNodes.add(Objects.requireNonNull(nodeId, "nodeId"));
            return this;
        }

        public Builder lockedNodes(Collection<ResourceLocation> nodeIds) {
            nodeIds.stream().filter(Objects::nonNull).forEach(this.lockedNodes::add);
            return this;
        }

        public Builder lockedTrack(ResourceLocation trackId) {
            this.lockedTracks.add(Objects.requireNonNull(trackId, "trackId"));
            return this;
        }

        public Builder lockedTracks(Collection<ResourceLocation> trackIds) {
            trackIds.stream().filter(Objects::nonNull).forEach(this.lockedTracks::add);
            return this;
        }

        public Builder requirement(UpgradeRequirement requirement) {
            this.requirements.add(Objects.requireNonNull(requirement, "requirement"));
            return this;
        }

        public Builder rewards(UpgradeRewardBundle rewards) {
            this.rewards = Objects.requireNonNull(rewards, "rewards");
            return this;
        }

        public UpgradeNodeDefinition build() {
            return new UpgradeNodeDefinition(
                    this.id,
                    this.trackId,
                    this.familyId,
                    this.groupId,
                    this.pageId,
                    this.tags,
                    this.choiceGroupId,
                    this.pointCosts,
                    this.requiredNodes,
                    this.lockedNodes,
                    this.lockedTracks,
                    this.requirements,
                    this.rewards
            );
        }
    }
}

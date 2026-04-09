package com.whatxe.xlib.progression;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class UpgradeTrackDefinition {
    private final ResourceLocation id;
    private final ResourceLocation familyId;
    private final ResourceLocation groupId;
    private final ResourceLocation pageId;
    private final Set<ResourceLocation> tags;
    private final Set<ResourceLocation> rootNodes;
    private final Set<ResourceLocation> exclusiveTracks;

    private UpgradeTrackDefinition(
            ResourceLocation id,
            ResourceLocation familyId,
            ResourceLocation groupId,
            ResourceLocation pageId,
            Set<ResourceLocation> tags,
            Set<ResourceLocation> rootNodes,
            Set<ResourceLocation> exclusiveTracks
    ) {
        this.id = id;
        this.familyId = familyId;
        this.groupId = groupId;
        this.pageId = pageId;
        this.tags = copyTags(tags);
        this.rootNodes = Set.copyOf(rootNodes);
        this.exclusiveTracks = Set.copyOf(exclusiveTracks);
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public ResourceLocation id() {
        return this.id;
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

    public Set<ResourceLocation> rootNodes() {
        return this.rootNodes;
    }

    public Set<ResourceLocation> exclusiveTracks() {
        return this.exclusiveTracks;
    }

    public Component displayName() {
        return Component.translatable(this.translationKey());
    }

    public String translationKey() {
        return "upgrade_track." + this.id.getNamespace() + "." + this.id.getPath();
    }

    private static Set<ResourceLocation> copyTags(Collection<ResourceLocation> source) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }

    public static final class Builder {
        private final ResourceLocation id;
        private ResourceLocation familyId;
        private ResourceLocation groupId;
        private ResourceLocation pageId;
        private final Set<ResourceLocation> tags = new LinkedHashSet<>();
        private final Set<ResourceLocation> rootNodes = new LinkedHashSet<>();
        private final Set<ResourceLocation> exclusiveTracks = new LinkedHashSet<>();

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

        public Builder rootNode(ResourceLocation nodeId) {
            this.rootNodes.add(Objects.requireNonNull(nodeId, "nodeId"));
            return this;
        }

        public Builder rootNodes(Collection<ResourceLocation> nodeIds) {
            nodeIds.stream().filter(Objects::nonNull).forEach(this.rootNodes::add);
            return this;
        }

        public Builder exclusiveWith(ResourceLocation trackId) {
            this.exclusiveTracks.add(Objects.requireNonNull(trackId, "trackId"));
            return this;
        }

        public Builder exclusiveWith(Collection<ResourceLocation> trackIds) {
            trackIds.stream().filter(Objects::nonNull).forEach(this.exclusiveTracks::add);
            return this;
        }

        public UpgradeTrackDefinition build() {
            this.exclusiveTracks.remove(this.id);
            return new UpgradeTrackDefinition(
                    this.id,
                    this.familyId,
                    this.groupId,
                    this.pageId,
                    this.tags,
                    this.rootNodes,
                    this.exclusiveTracks
            );
        }
    }
}

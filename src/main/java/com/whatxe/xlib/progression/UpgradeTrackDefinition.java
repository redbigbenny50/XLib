package com.whatxe.xlib.progression;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class UpgradeTrackDefinition {
    private final ResourceLocation id;
    private final Set<ResourceLocation> rootNodes;
    private final Set<ResourceLocation> exclusiveTracks;

    private UpgradeTrackDefinition(ResourceLocation id, Set<ResourceLocation> rootNodes, Set<ResourceLocation> exclusiveTracks) {
        this.id = id;
        this.rootNodes = Set.copyOf(rootNodes);
        this.exclusiveTracks = Set.copyOf(exclusiveTracks);
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public ResourceLocation id() {
        return this.id;
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

    public static final class Builder {
        private final ResourceLocation id;
        private final Set<ResourceLocation> rootNodes = new LinkedHashSet<>();
        private final Set<ResourceLocation> exclusiveTracks = new LinkedHashSet<>();

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
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
            return new UpgradeTrackDefinition(this.id, this.rootNodes, this.exclusiveTracks);
        }
    }
}

package com.whatxe.xlib.progression;

import com.whatxe.xlib.XLib;
import java.util.Collection;
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
    private final Map<ResourceLocation, Integer> pointCosts;
    private final Set<ResourceLocation> requiredNodes;
    private final List<UpgradeRequirement> requirements;
    private final UpgradeRewardBundle rewards;

    private UpgradeNodeDefinition(
            ResourceLocation id,
            @Nullable ResourceLocation trackId,
            Map<ResourceLocation, Integer> pointCosts,
            Set<ResourceLocation> requiredNodes,
            List<UpgradeRequirement> requirements,
            UpgradeRewardBundle rewards
    ) {
        this.id = id;
        this.trackId = trackId;
        this.pointCosts = Map.copyOf(pointCosts);
        this.requiredNodes = Set.copyOf(requiredNodes);
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

    public Map<ResourceLocation, Integer> pointCosts() {
        return this.pointCosts;
    }

    public Set<ResourceLocation> requiredNodes() {
        return this.requiredNodes;
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

    public static final class Builder {
        private final ResourceLocation id;
        private ResourceLocation trackId;
        private final Map<ResourceLocation, Integer> pointCosts = new LinkedHashMap<>();
        private final Set<ResourceLocation> requiredNodes = new LinkedHashSet<>();
        private final List<UpgradeRequirement> requirements = new java.util.ArrayList<>();
        private UpgradeRewardBundle rewards = UpgradeRewardBundle.builder().build();

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder track(ResourceLocation trackId) {
            this.trackId = Objects.requireNonNull(trackId, "trackId");
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
                    this.pointCosts,
                    this.requiredNodes,
                    this.requirements,
                    this.rewards
            );
        }
    }
}

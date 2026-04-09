package com.whatxe.xlib.ability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class AbilityDefinition {
    @FunctionalInterface
    public interface AbilityAction {
        AbilityUseResult activate(ServerPlayer player, AbilityData data);
    }

    @FunctionalInterface
    public interface AbilityTicker {
        AbilityData tick(ServerPlayer player, AbilityData data);
    }

    @FunctionalInterface
    public interface AbilityEnder {
        AbilityUseResult end(ServerPlayer player, AbilityData data, AbilityEndReason reason);
    }

    @FunctionalInterface
    public interface ChargeReleaseStartAction {
        AbilityUseResult start(ServerPlayer player, AbilityData data);
    }

    @FunctionalInterface
    public interface ChargeReleaseTickAction {
        AbilityData tick(ServerPlayer player, AbilityData data, int chargedTicks, int maxChargeTicks);
    }

    @FunctionalInterface
    public interface ChargeReleaseReleaseAction {
        AbilityUseResult release(ServerPlayer player, AbilityData data, AbilityEndReason reason, int chargedTicks, int maxChargeTicks);
    }

    private static final AbilityTicker NOOP_TICKER = (player, data) -> data;
    private static final AbilityEnder NOOP_ENDER = (player, data, reason) -> AbilityUseResult.success(data);

    private final ResourceLocation id;
    private final AbilityIcon icon;
    private final int cooldownTicks;
    private final AbilityCooldownPolicy cooldownPolicy;
    private final boolean toggleAbility;
    private final int durationTicks;
    private final int chargeReleaseMaxTicks;
    private final int maxCharges;
    private final int chargeRechargeTicks;
    private final ResourceLocation familyId;
    private final ResourceLocation groupId;
    private final ResourceLocation pageId;
    private final Set<ResourceLocation> tags;
    private final List<AbilityRequirement> assignRequirements;
    private final List<AbilityRequirement> activateRequirements;
    private final List<AbilityRequirement> stayActiveRequirements;
    private final List<AbilityRequirement> renderRequirements;
    private final List<AbilityResourceCost> resourceCosts;
    private final Map<AbilitySoundTrigger, List<AbilitySound>> sounds;
    private final AbilityAction action;
    private final AbilityTicker ticker;
    private final AbilityEnder ender;

    private AbilityDefinition(
            ResourceLocation id,
            AbilityIcon icon,
            int cooldownTicks,
            AbilityCooldownPolicy cooldownPolicy,
            boolean toggleAbility,
            int durationTicks,
            int chargeReleaseMaxTicks,
            int maxCharges,
            int chargeRechargeTicks,
            ResourceLocation familyId,
            ResourceLocation groupId,
            ResourceLocation pageId,
            Set<ResourceLocation> tags,
            List<AbilityRequirement> assignRequirements,
            List<AbilityRequirement> activateRequirements,
            List<AbilityRequirement> stayActiveRequirements,
            List<AbilityRequirement> renderRequirements,
            List<AbilityResourceCost> resourceCosts,
            Map<AbilitySoundTrigger, List<AbilitySound>> sounds,
            AbilityAction action,
            AbilityTicker ticker,
            AbilityEnder ender
    ) {
        this.id = id;
        this.icon = icon;
        this.cooldownTicks = cooldownTicks;
        this.cooldownPolicy = cooldownPolicy;
        this.toggleAbility = toggleAbility;
        this.durationTicks = durationTicks;
        this.chargeReleaseMaxTicks = chargeReleaseMaxTicks;
        this.maxCharges = maxCharges;
        this.chargeRechargeTicks = chargeRechargeTicks;
        this.familyId = familyId;
        this.groupId = groupId;
        this.pageId = pageId;
        this.tags = copyTags(tags);
        this.assignRequirements = List.copyOf(assignRequirements);
        this.activateRequirements = List.copyOf(activateRequirements);
        this.stayActiveRequirements = List.copyOf(stayActiveRequirements);
        this.renderRequirements = List.copyOf(renderRequirements);
        this.resourceCosts = List.copyOf(resourceCosts);
        this.sounds = copySounds(sounds);
        this.action = action;
        this.ticker = ticker;
        this.ender = ender;
    }

    public static Builder builder(ResourceLocation id, Item iconItem) {
        return new Builder(id, AbilityIcon.ofItem(iconItem));
    }

    public static Builder builder(ResourceLocation id, AbilityIcon icon) {
        return new Builder(id, icon);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public AbilityIcon icon() {
        return this.icon;
    }

    public int cooldownTicks() {
        return this.cooldownTicks;
    }

    public AbilityCooldownPolicy cooldownPolicy() {
        return this.cooldownPolicy;
    }

    public boolean toggleAbility() {
        return this.toggleAbility;
    }

    public int durationTicks() {
        return this.durationTicks;
    }

    public boolean isChargeReleaseAbility() {
        return this.chargeReleaseMaxTicks > 0;
    }

    public int chargeReleaseMaxTicks() {
        return this.chargeReleaseMaxTicks;
    }

    public int maxCharges() {
        return this.maxCharges;
    }

    public int chargeRechargeTicks() {
        return this.chargeRechargeTicks;
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

    public boolean usesCharges() {
        return this.maxCharges > 1;
    }

    public List<AbilityRequirement> assignRequirements() {
        return this.assignRequirements;
    }

    public List<AbilityRequirement> activateRequirements() {
        return this.activateRequirements;
    }

    public List<AbilityRequirement> stayActiveRequirements() {
        return this.stayActiveRequirements;
    }

    public List<AbilityRequirement> renderRequirements() {
        return this.renderRequirements;
    }

    public List<AbilityRequirement> requirements() {
        return this.activateRequirements;
    }

    public List<AbilityResourceCost> resourceCosts() {
        return this.resourceCosts;
    }

    public List<AbilitySound> soundsFor(AbilitySoundTrigger trigger) {
        return this.sounds.getOrDefault(trigger, List.of());
    }

    public Component displayName() {
        return Component.translatable(this.translationKey());
    }

    public Component description() {
        return Component.translatable(this.translationKey() + ".desc");
    }

    public String translationKey() {
        return "ability." + this.id.getNamespace() + "." + this.id.getPath();
    }

    public ItemStack createIconStack() {
        return this.icon.kind() == AbilityIcon.Kind.ITEM && this.icon.item() != null
                ? new ItemStack(this.icon.item())
                : ItemStack.EMPTY;
    }

    public AbilityUseResult activate(ServerPlayer player, AbilityData data) {
        return this.action.activate(player, data);
    }

    public AbilityData tick(ServerPlayer player, AbilityData data) {
        return this.ticker.tick(player, data);
    }

    public AbilityUseResult end(ServerPlayer player, AbilityData data, AbilityEndReason reason) {
        return this.ender.end(player, data, reason);
    }

    public int resolvedChargeReleaseTicks(AbilityData data, @Nullable AbilityEndReason reason) {
        if (!isChargeReleaseAbility()) {
            return 0;
        }

        if (reason == AbilityEndReason.DURATION_EXPIRED) {
            return this.chargeReleaseMaxTicks;
        }

        int remainingTicks = data.activeDurationFor(this.id);
        if (remainingTicks <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(this.chargeReleaseMaxTicks, this.chargeReleaseMaxTicks - remainingTicks));
    }

    public Optional<Component> firstFailedAssignRequirement(Player player, AbilityData data) {
        return AbilityRequirements.firstFailure(player, data, this.assignRequirements);
    }

    public Optional<Component> firstFailedActivationRequirement(Player player, AbilityData data) {
        return AbilityRequirements.firstFailure(player, data, this.activateRequirements);
    }

    public Optional<Component> firstFailedActiveRequirement(Player player, AbilityData data) {
        return AbilityRequirements.firstFailure(player, data, this.stayActiveRequirements);
    }

    public Optional<Component> firstFailedRenderRequirement(Player player, AbilityData data) {
        return AbilityRequirements.firstFailure(player, data, this.renderRequirements);
    }

    public Optional<Component> firstFailedRequirement(Player player, AbilityData data) {
        return firstFailedActivationRequirement(player, data);
    }

    public void playSounds(ServerPlayer player, AbilitySoundTrigger trigger) {
        AbilitySoundPlayer.playAll(player, soundsFor(trigger));
    }

    private static Map<AbilitySoundTrigger, List<AbilitySound>> copySounds(Map<AbilitySoundTrigger, List<AbilitySound>> source) {
        Map<AbilitySoundTrigger, List<AbilitySound>> copied = new EnumMap<>(AbilitySoundTrigger.class);
        for (Map.Entry<AbilitySoundTrigger, List<AbilitySound>> entry : source.entrySet()) {
            copied.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(copied);
    }

    private static Set<ResourceLocation> copyTags(Collection<ResourceLocation> source) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final AbilityIcon icon;
        private int cooldownTicks;
        private AbilityCooldownPolicy cooldownPolicy = AbilityCooldownPolicy.ON_USE;
        private boolean toggleAbility;
        private int durationTicks;
        private int chargeReleaseMaxTicks;
        private int maxCharges = 1;
        private int chargeRechargeTicks;
        private ResourceLocation familyId;
        private ResourceLocation groupId;
        private ResourceLocation pageId;
        private final Set<ResourceLocation> tags = new LinkedHashSet<>();
        private final List<AbilityRequirement> assignRequirements = new ArrayList<>();
        private final List<AbilityRequirement> activateRequirements = new ArrayList<>();
        private final List<AbilityRequirement> stayActiveRequirements = new ArrayList<>();
        private final List<AbilityRequirement> renderRequirements = new ArrayList<>();
        private final List<AbilityResourceCost> resourceCosts = new ArrayList<>();
        private final Map<AbilitySoundTrigger, List<AbilitySound>> sounds = new EnumMap<>(AbilitySoundTrigger.class);
        private AbilityAction action;
        private AbilityTicker ticker = NOOP_TICKER;
        private AbilityEnder ender = NOOP_ENDER;

        private Builder(ResourceLocation id, AbilityIcon icon) {
            this.id = Objects.requireNonNull(id, "id");
            this.icon = Objects.requireNonNull(icon, "icon");
        }

        public Builder cooldownTicks(int cooldownTicks) {
            this.cooldownTicks = cooldownTicks;
            return this;
        }

        public Builder cooldownPolicy(AbilityCooldownPolicy cooldownPolicy) {
            this.cooldownPolicy = Objects.requireNonNull(cooldownPolicy, "cooldownPolicy");
            return this;
        }

        public Builder toggleAbility() {
            this.toggleAbility = true;
            return this;
        }

        public Builder durationTicks(int durationTicks) {
            this.durationTicks = durationTicks;
            return this;
        }

        public Builder charges(int maxCharges, int chargeRechargeTicks) {
            this.maxCharges = maxCharges;
            this.chargeRechargeTicks = chargeRechargeTicks;
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

        public Builder assignRequirement(AbilityRequirement requirement) {
            this.assignRequirements.add(Objects.requireNonNull(requirement, "requirement"));
            return this;
        }

        public Builder activateRequirement(AbilityRequirement requirement) {
            this.activateRequirements.add(Objects.requireNonNull(requirement, "requirement"));
            return this;
        }

        public Builder stayActiveRequirement(AbilityRequirement requirement) {
            this.stayActiveRequirements.add(Objects.requireNonNull(requirement, "requirement"));
            return this;
        }

        public Builder renderRequirement(AbilityRequirement requirement) {
            this.renderRequirements.add(Objects.requireNonNull(requirement, "requirement"));
            return this;
        }

        public Builder requirement(AbilityRequirement requirement) {
            return activateRequirement(requirement);
        }

        public Builder resourceCost(ResourceLocation resourceId, int amount) {
            this.resourceCosts.add(new AbilityResourceCost(resourceId, amount));
            return this;
        }

        public Builder chargeRelease(int maxChargeTicks, ChargeReleaseReleaseAction releaseAction) {
            return chargeRelease(
                    maxChargeTicks,
                    (player, data) -> AbilityUseResult.success(data),
                    (player, data, chargedTicks, ignoredMaxChargeTicks) -> data,
                    releaseAction
            );
        }

        public Builder chargeRelease(
                int maxChargeTicks,
                ChargeReleaseStartAction startAction,
                ChargeReleaseTickAction tickAction,
                ChargeReleaseReleaseAction releaseAction
        ) {
            if (maxChargeTicks <= 0) {
                throw new IllegalArgumentException("chargeRelease abilities require a positive maxChargeTicks value");
            }

            ChargeReleaseStartAction resolvedStartAction = Objects.requireNonNull(startAction, "startAction");
            ChargeReleaseTickAction resolvedTickAction = Objects.requireNonNull(tickAction, "tickAction");
            ChargeReleaseReleaseAction resolvedReleaseAction = Objects.requireNonNull(releaseAction, "releaseAction");
            this.toggleAbility = true;
            this.durationTicks = maxChargeTicks;
            this.chargeReleaseMaxTicks = maxChargeTicks;
            this.action = resolvedStartAction::start;
            this.ticker = (player, data) -> resolvedTickAction.tick(
                    player,
                    data,
                    resolveChargeReleaseTicks(this.id, data, maxChargeTicks, null),
                    maxChargeTicks
            );
            this.ender = (player, data, reason) -> resolvedReleaseAction.release(
                    player,
                    data,
                    reason,
                    resolveChargeReleaseTicks(this.id, data, maxChargeTicks, reason),
                    maxChargeTicks
            );
            return this;
        }

        public Builder sequence(AbilitySequenceDefinition sequence) {
            AbilitySequenceDefinition resolvedSequence = Objects.requireNonNull(sequence, "sequence");
            this.toggleAbility = true;
            this.durationTicks = resolvedSequence.totalDurationTicks();
            this.chargeReleaseMaxTicks = 0;
            this.action = (player, data) -> resolvedSequence.activate(this.id, player, data);
            this.ticker = (player, data) -> resolvedSequence.tick(this.id, player, data);
            this.ender = (player, data, reason) -> resolvedSequence.end(this.id, player, data, reason);
            return this;
        }

        public Builder sound(AbilitySoundTrigger trigger, ResourceLocation soundId) {
            return sound(trigger, AbilitySound.of(soundId));
        }

        public Builder sound(AbilitySoundTrigger trigger, AbilitySound sound) {
            this.sounds.computeIfAbsent(Objects.requireNonNull(trigger, "trigger"), ignored -> new ArrayList<>())
                    .add(Objects.requireNonNull(sound, "sound"));
            return this;
        }

        public Builder action(AbilityAction action) {
            this.action = Objects.requireNonNull(action, "action");
            return this;
        }

        public Builder ticker(AbilityTicker ticker) {
            this.ticker = Objects.requireNonNull(ticker, "ticker");
            return this;
        }

        public Builder ender(AbilityEnder ender) {
            this.ender = Objects.requireNonNull(ender, "ender");
            return this;
        }

        public AbilityDefinition build() {
            if (this.cooldownTicks < 0) {
                throw new IllegalStateException("cooldownTicks cannot be negative");
            }
            if (this.durationTicks < 0) {
                throw new IllegalStateException("durationTicks cannot be negative");
            }
            if (this.maxCharges <= 0) {
                throw new IllegalStateException("maxCharges must be positive");
            }
            if (this.maxCharges == 1 && this.chargeRechargeTicks != 0) {
                throw new IllegalStateException("Single-charge abilities should use cooldownTicks instead of charge recharge");
            }
            if (this.maxCharges > 1 && this.chargeRechargeTicks <= 0) {
                throw new IllegalStateException("Charge-based abilities need a positive chargeRechargeTicks value");
            }
            if (!this.toggleAbility && this.cooldownPolicy == AbilityCooldownPolicy.ON_END) {
                throw new IllegalStateException("Only toggle abilities can use ON_END cooldown policy");
            }
            if (!this.toggleAbility && this.durationTicks > 0) {
                throw new IllegalStateException("Only toggle abilities can have an active duration");
            }
            if (this.action == null) {
                throw new IllegalStateException("Ability action must be provided");
            }

            return new AbilityDefinition(
                    this.id,
                    this.icon,
                    this.cooldownTicks,
                    this.cooldownPolicy,
                    this.toggleAbility,
                    this.durationTicks,
                    this.chargeReleaseMaxTicks,
                    this.maxCharges,
                    this.chargeRechargeTicks,
                    this.familyId,
                    this.groupId,
                    this.pageId,
                    this.tags,
                    this.assignRequirements,
                    this.activateRequirements,
                    this.stayActiveRequirements,
                    this.renderRequirements,
                    this.resourceCosts,
                    this.sounds,
                    this.action,
                    this.ticker,
                    this.ender
            );
        }

        private static int resolveChargeReleaseTicks(
                ResourceLocation abilityId,
                AbilityData data,
                int maxChargeTicks,
                AbilityEndReason reason
        ) {
            if (reason == AbilityEndReason.DURATION_EXPIRED) {
                return maxChargeTicks;
            }

            int remainingTicks = data.activeDurationFor(abilityId);
            if (remainingTicks <= 0) {
                return 0;
            }
            return Math.max(0, Math.min(maxChargeTicks, maxChargeTicks - remainingTicks));
        }
    }
}


package com.whatxe.xlib.ability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class PassiveDefinition {
    public enum Hook {
        TICK,
        GRANTED,
        REVOKED,
        HIT,
        KILL,
        HURT,
        JUMP,
        EAT,
        BLOCK_BREAK,
        ARMOR_CHANGE
    }

    @FunctionalInterface
    public interface PassiveTicker {
        AbilityData tick(ServerPlayer player, AbilityData data);
    }

    @FunctionalInterface
    public interface PassiveAction {
        AbilityData apply(ServerPlayer player, AbilityData data);
    }

    @FunctionalInterface
    public interface PassiveHitAction {
        AbilityData apply(ServerPlayer player, AbilityData data, LivingEntity target);
    }

    @FunctionalInterface
    public interface PassiveHurtAction {
        AbilityData apply(ServerPlayer player, AbilityData data, DamageSource source, float amount);
    }

    @FunctionalInterface
    public interface PassiveEatAction {
        AbilityData apply(ServerPlayer player, AbilityData data, ItemStack stack);
    }

    @FunctionalInterface
    public interface PassiveBlockBreakAction {
        AbilityData apply(ServerPlayer player, AbilityData data, BlockState state, BlockPos pos);
    }

    @FunctionalInterface
    public interface PassiveArmorChangeAction {
        AbilityData apply(ServerPlayer player, AbilityData data, EquipmentSlot slot, ItemStack from, ItemStack to);
    }

    private static final PassiveTicker NOOP_TICKER = (player, data) -> data;
    private static final PassiveAction NOOP_ACTION = (player, data) -> data;
    private static final PassiveHitAction NOOP_HIT_ACTION = (player, data, target) -> data;
    private static final PassiveHurtAction NOOP_HURT_ACTION = (player, data, source, amount) -> data;
    private static final PassiveEatAction NOOP_EAT_ACTION = (player, data, stack) -> data;
    private static final PassiveBlockBreakAction NOOP_BLOCK_BREAK_ACTION = (player, data, state, pos) -> data;
    private static final PassiveArmorChangeAction NOOP_ARMOR_CHANGE_ACTION = (player, data, slot, from, to) -> data;

    private final ResourceLocation id;
    private final AbilityIcon icon;
    private final ResourceLocation familyId;
    private final ResourceLocation groupId;
    private final ResourceLocation pageId;
    private final Set<ResourceLocation> tags;
    private final List<AbilityRequirement> grantRequirements;
    private final List<AbilityRequirement> activeRequirements;
    private final double cooldownTickRateMultiplier;
    private final Map<PassiveSoundTrigger, List<AbilitySound>> sounds;
    private final PassiveTicker ticker;
    private final PassiveAction onGranted;
    private final PassiveAction onRevoked;
    private final PassiveHitAction onHit;
    private final PassiveHitAction onKill;
    private final PassiveHurtAction onHurt;
    private final PassiveAction onJump;
    private final PassiveEatAction onEat;
    private final PassiveBlockBreakAction onBlockBreak;
    private final PassiveArmorChangeAction onArmorChange;
    @org.jetbrains.annotations.Nullable private final Component customDisplayName;
    @org.jetbrains.annotations.Nullable private final Component customDescription;
    private final boolean hasCustomDescription;

    private PassiveDefinition(
            ResourceLocation id,
            AbilityIcon icon,
            ResourceLocation familyId,
            ResourceLocation groupId,
            ResourceLocation pageId,
            Set<ResourceLocation> tags,
            List<AbilityRequirement> grantRequirements,
            List<AbilityRequirement> activeRequirements,
            double cooldownTickRateMultiplier,
            Map<PassiveSoundTrigger, List<AbilitySound>> sounds,
            PassiveTicker ticker,
            PassiveAction onGranted,
            PassiveAction onRevoked,
            PassiveHitAction onHit,
            PassiveHitAction onKill,
            PassiveHurtAction onHurt,
            PassiveAction onJump,
            PassiveEatAction onEat,
            PassiveBlockBreakAction onBlockBreak,
            PassiveArmorChangeAction onArmorChange,
            @org.jetbrains.annotations.Nullable Component customDisplayName,
            @org.jetbrains.annotations.Nullable Component customDescription,
            boolean hasCustomDescription
    ) {
        this.id = id;
        this.icon = icon;
        this.familyId = familyId;
        this.groupId = groupId;
        this.pageId = pageId;
        this.tags = copyTags(tags);
        this.grantRequirements = List.copyOf(grantRequirements);
        this.activeRequirements = List.copyOf(activeRequirements);
        this.cooldownTickRateMultiplier = cooldownTickRateMultiplier;
        this.sounds = copySounds(sounds);
        this.ticker = ticker;
        this.onGranted = onGranted;
        this.onRevoked = onRevoked;
        this.onHit = onHit;
        this.onKill = onKill;
        this.onHurt = onHurt;
        this.onJump = onJump;
        this.onEat = onEat;
        this.onBlockBreak = onBlockBreak;
        this.onArmorChange = onArmorChange;
        this.customDisplayName = customDisplayName;
        this.customDescription = customDescription;
        this.hasCustomDescription = hasCustomDescription;
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

    public Set<Hook> authoredHooks() {
        EnumSet<Hook> hooks = EnumSet.noneOf(Hook.class);
        if (this.ticker != NOOP_TICKER) {
            hooks.add(Hook.TICK);
        }
        if (this.onGranted != NOOP_ACTION) {
            hooks.add(Hook.GRANTED);
        }
        if (this.onRevoked != NOOP_ACTION) {
            hooks.add(Hook.REVOKED);
        }
        if (this.onHit != NOOP_HIT_ACTION) {
            hooks.add(Hook.HIT);
        }
        if (this.onKill != NOOP_HIT_ACTION) {
            hooks.add(Hook.KILL);
        }
        if (this.onHurt != NOOP_HURT_ACTION) {
            hooks.add(Hook.HURT);
        }
        if (this.onJump != NOOP_ACTION) {
            hooks.add(Hook.JUMP);
        }
        if (this.onEat != NOOP_EAT_ACTION) {
            hooks.add(Hook.EAT);
        }
        if (this.onBlockBreak != NOOP_BLOCK_BREAK_ACTION) {
            hooks.add(Hook.BLOCK_BREAK);
        }
        if (this.onArmorChange != NOOP_ARMOR_CHANGE_ACTION) {
            hooks.add(Hook.ARMOR_CHANGE);
        }
        return Collections.unmodifiableSet(hooks);
    }

    public boolean hasHook(Hook hook) {
        return authoredHooks().contains(Objects.requireNonNull(hook, "hook"));
    }

    public Set<PassiveSoundTrigger> soundTriggers() {
        EnumSet<PassiveSoundTrigger> triggers = EnumSet.noneOf(PassiveSoundTrigger.class);
        for (Map.Entry<PassiveSoundTrigger, List<AbilitySound>> entry : this.sounds.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                triggers.add(entry.getKey());
            }
        }
        return Collections.unmodifiableSet(triggers);
    }

    public Component displayName() {
        return this.customDisplayName != null ? this.customDisplayName : Component.translatable(this.translationKey());
    }

    public Component description() {
        if (this.customDescription != null) {
            return this.customDescription;
        }
        return this.hasCustomDescription ? Component.empty() : Component.translatable(this.translationKey() + ".desc");
    }

    public boolean hasCustomDescription() {
        return this.hasCustomDescription;
    }

    public String translationKey() {
        return "passive." + this.id.getNamespace() + "." + this.id.getPath();
    }

    public ItemStack createIconStack() {
        return this.icon.kind() == AbilityIcon.Kind.ITEM && this.icon.item() != null
                ? new ItemStack(this.icon.item())
                : ItemStack.EMPTY;
    }

    public List<AbilityRequirement> grantRequirements() {
        return this.grantRequirements;
    }

    public List<AbilityRequirement> activeRequirements() {
        return this.activeRequirements;
    }

    public double cooldownTickRateMultiplier() {
        return this.cooldownTickRateMultiplier;
    }

    public AbilityData tick(ServerPlayer player, AbilityData data) {
        return this.ticker.tick(player, data);
    }

    public AbilityData onGranted(ServerPlayer player, AbilityData data) {
        return this.onGranted.apply(player, data);
    }

    public AbilityData onRevoked(ServerPlayer player, AbilityData data) {
        return this.onRevoked.apply(player, data);
    }

    public AbilityData onHit(ServerPlayer player, AbilityData data, LivingEntity target) {
        return this.onHit.apply(player, data, target);
    }

    public AbilityData onKill(ServerPlayer player, AbilityData data, LivingEntity target) {
        return this.onKill.apply(player, data, target);
    }

    public AbilityData onHurt(ServerPlayer player, AbilityData data, DamageSource source, float amount) {
        return this.onHurt.apply(player, data, source, amount);
    }

    public AbilityData onJump(ServerPlayer player, AbilityData data) {
        return this.onJump.apply(player, data);
    }

    public AbilityData onEat(ServerPlayer player, AbilityData data, ItemStack stack) {
        return this.onEat.apply(player, data, stack);
    }

    public AbilityData onBlockBreak(ServerPlayer player, AbilityData data, BlockState state, BlockPos pos) {
        return this.onBlockBreak.apply(player, data, state, pos);
    }

    public AbilityData onArmorChange(ServerPlayer player, AbilityData data, EquipmentSlot slot, ItemStack from, ItemStack to) {
        return this.onArmorChange.apply(player, data, slot, from, to);
    }

    public Optional<Component> firstFailedGrantRequirement(Player player, AbilityData data) {
        return AbilityRequirements.firstFailure(player, data, this.grantRequirements);
    }

    public Optional<Component> firstFailedActiveRequirement(Player player, AbilityData data) {
        return AbilityRequirements.firstFailure(player, data, this.activeRequirements);
    }

    public Optional<Component> firstFailedRequirement(Player player, AbilityData data) {
        return firstFailedActiveRequirement(player, data);
    }

    public List<AbilitySound> soundsFor(PassiveSoundTrigger trigger) {
        return this.sounds.getOrDefault(trigger, List.of());
    }

    public void playSounds(ServerPlayer player, PassiveSoundTrigger trigger) {
        AbilitySoundPlayer.playAll(player, soundsFor(trigger));
    }

    private static Map<PassiveSoundTrigger, List<AbilitySound>> copySounds(Map<PassiveSoundTrigger, List<AbilitySound>> source) {
        Map<PassiveSoundTrigger, List<AbilitySound>> copied = new EnumMap<>(PassiveSoundTrigger.class);
        for (Map.Entry<PassiveSoundTrigger, List<AbilitySound>> entry : source.entrySet()) {
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
        private ResourceLocation familyId;
        private ResourceLocation groupId;
        private ResourceLocation pageId;
        private final Set<ResourceLocation> tags = new LinkedHashSet<>();
        private final List<AbilityRequirement> grantRequirements = new ArrayList<>();
        private final List<AbilityRequirement> activeRequirements = new ArrayList<>();
        private double cooldownTickRateMultiplier = 1.0D;
        private final Map<PassiveSoundTrigger, List<AbilitySound>> sounds = new EnumMap<>(PassiveSoundTrigger.class);
        private PassiveTicker ticker = NOOP_TICKER;
        private PassiveAction onGranted = NOOP_ACTION;
        private PassiveAction onRevoked = NOOP_ACTION;
        private PassiveHitAction onHit = NOOP_HIT_ACTION;
        private PassiveHitAction onKill = NOOP_HIT_ACTION;
        private PassiveHurtAction onHurt = NOOP_HURT_ACTION;
        private PassiveAction onJump = NOOP_ACTION;
        private PassiveEatAction onEat = NOOP_EAT_ACTION;
        private PassiveBlockBreakAction onBlockBreak = NOOP_BLOCK_BREAK_ACTION;
        private PassiveArmorChangeAction onArmorChange = NOOP_ARMOR_CHANGE_ACTION;
        @org.jetbrains.annotations.Nullable private Component customDisplayName;
        @org.jetbrains.annotations.Nullable private Component customDescription;
        private boolean hasCustomDescription;

        private Builder(ResourceLocation id, AbilityIcon icon) {
            this.id = Objects.requireNonNull(id, "id");
            this.icon = Objects.requireNonNull(icon, "icon");
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

        public Builder grantRequirement(AbilityRequirement requirement) {
            this.grantRequirements.add(Objects.requireNonNull(requirement, "requirement"));
            return this;
        }

        public Builder activeRequirement(AbilityRequirement requirement) {
            this.activeRequirements.add(Objects.requireNonNull(requirement, "requirement"));
            return this;
        }

        public Builder requirement(AbilityRequirement requirement) {
            return activeRequirement(requirement);
        }

        public Builder cooldownTickRateMultiplier(double multiplier) {
            if (!(multiplier > 0.0D)) {
                throw new IllegalArgumentException("cooldownTickRateMultiplier must be positive");
            }
            this.cooldownTickRateMultiplier = multiplier;
            return this;
        }

        public Builder sound(PassiveSoundTrigger trigger, ResourceLocation soundId) {
            return sound(trigger, AbilitySound.of(soundId));
        }

        public Builder sound(PassiveSoundTrigger trigger, AbilitySound sound) {
            this.sounds.computeIfAbsent(Objects.requireNonNull(trigger, "trigger"), ignored -> new ArrayList<>())
                    .add(Objects.requireNonNull(sound, "sound"));
            return this;
        }

        public Builder ticker(PassiveTicker ticker) {
            this.ticker = Objects.requireNonNull(ticker, "ticker");
            return this;
        }

        public Builder onGranted(PassiveAction onGranted) {
            this.onGranted = Objects.requireNonNull(onGranted, "onGranted");
            return this;
        }

        public Builder onRevoked(PassiveAction onRevoked) {
            this.onRevoked = Objects.requireNonNull(onRevoked, "onRevoked");
            return this;
        }

        public Builder onHit(PassiveHitAction onHit) {
            this.onHit = Objects.requireNonNull(onHit, "onHit");
            return this;
        }

        public Builder onKill(PassiveHitAction onKill) {
            this.onKill = Objects.requireNonNull(onKill, "onKill");
            return this;
        }

        public Builder onHurt(PassiveHurtAction onHurt) {
            this.onHurt = Objects.requireNonNull(onHurt, "onHurt");
            return this;
        }

        public Builder onJump(PassiveAction onJump) {
            this.onJump = Objects.requireNonNull(onJump, "onJump");
            return this;
        }

        public Builder onEat(PassiveEatAction onEat) {
            this.onEat = Objects.requireNonNull(onEat, "onEat");
            return this;
        }

        public Builder onBlockBreak(PassiveBlockBreakAction onBlockBreak) {
            this.onBlockBreak = Objects.requireNonNull(onBlockBreak, "onBlockBreak");
            return this;
        }

        public Builder onArmorChange(PassiveArmorChangeAction onArmorChange) {
            this.onArmorChange = Objects.requireNonNull(onArmorChange, "onArmorChange");
            return this;
        }

        public Builder displayName(Component displayName) {
            this.customDisplayName = Objects.requireNonNull(displayName, "displayName");
            return this;
        }

        public Builder description(Component description) {
            this.customDescription = Objects.requireNonNull(description, "description");
            this.hasCustomDescription = true;
            return this;
        }

        public Builder emptyDescription() {
            this.customDescription = null;
            this.hasCustomDescription = true;
            return this;
        }

        public PassiveDefinition build() {
            return new PassiveDefinition(
                    this.id,
                    this.icon,
                    this.familyId,
                    this.groupId,
                    this.pageId,
                    this.tags,
                    this.grantRequirements,
                    this.activeRequirements,
                    this.cooldownTickRateMultiplier,
                    this.sounds,
                    this.ticker,
                    this.onGranted,
                    this.onRevoked,
                    this.onHit,
                    this.onKill,
                    this.onHurt,
                    this.onJump,
                    this.onEat,
                    this.onBlockBreak,
                    this.onArmorChange,
                    this.customDisplayName,
                    this.customDescription,
                    this.hasCustomDescription
            );
        }
    }
}


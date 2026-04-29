package com.whatxe.xlib.attachment;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityGrantApi;
import com.whatxe.xlib.ability.ProfileApi;
import com.whatxe.xlib.ability.ProfileSelectionData;
import com.whatxe.xlib.binding.EntityBindingData;
import com.whatxe.xlib.capability.CapabilityPolicyApi;
import com.whatxe.xlib.form.VisualFormApi;
import com.whatxe.xlib.form.VisualFormData;
import com.whatxe.xlib.lifecycle.LifecycleStageApi;
import com.whatxe.xlib.lifecycle.LifecycleStageData;
import com.whatxe.xlib.capability.CapabilityPolicyData;
import com.whatxe.xlib.combat.CombatMarkApi;
import com.whatxe.xlib.combat.CombatMarkData;
import com.whatxe.xlib.combat.CombatReactionData;
import com.whatxe.xlib.progression.UpgradeApi;
import com.whatxe.xlib.progression.UpgradeProgressData;
import io.netty.channel.embedded.EmbeddedChannel;
import java.net.SocketAddress;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, XLib.MODID);

    public static final Supplier<AttachmentType<AbilityData>> PLAYER_ABILITY_DATA = ATTACHMENT_TYPES.register(
            "player_ability_data",
            () -> AttachmentType.builder(AbilityApi::createDefaultData)
                    .serialize(AbilityData.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to && !hasEmbeddedConnection(to), AbilityData.STREAM_CODEC)
                    .build()
    );
    public static final Supplier<AttachmentType<UpgradeProgressData>> PLAYER_UPGRADE_PROGRESS = ATTACHMENT_TYPES.register(
            "player_upgrade_progress",
            () -> AttachmentType.builder(UpgradeApi::createDefaultData)
                    .serialize(UpgradeProgressData.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to && !hasEmbeddedConnection(to), UpgradeProgressData.STREAM_CODEC)
                    .build()
    );
    public static final Supplier<AttachmentType<ProfileSelectionData>> PLAYER_PROFILE_SELECTIONS = ATTACHMENT_TYPES.register(
            "player_profile_selections",
            () -> AttachmentType.builder(ProfileApi::createDefaultData)
                    .serialize(ProfileSelectionData.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to && !hasEmbeddedConnection(to), ProfileSelectionData.STREAM_CODEC)
                    .build()
    );
    public static final Supplier<AttachmentType<CapabilityPolicyData>> PLAYER_CAPABILITY_POLICY = ATTACHMENT_TYPES.register(
            "player_capability_policy",
            () -> AttachmentType.builder(CapabilityPolicyData::empty)
                    .serialize(CapabilityPolicyData.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to && !hasEmbeddedConnection(to), CapabilityPolicyData.STREAM_CODEC)
                    .build()
    );
    public static final Supplier<AttachmentType<VisualFormData>> PLAYER_VISUAL_FORM = ATTACHMENT_TYPES.register(
            "player_visual_form",
            () -> AttachmentType.builder(VisualFormData::empty)
                    .serialize(VisualFormData.CODEC)
                    .sync((holder, to) -> holder == to && !hasEmbeddedConnection(to), VisualFormData.STREAM_CODEC)
                    .build()
    );
    public static final Supplier<AttachmentType<LifecycleStageData>> PLAYER_LIFECYCLE_STAGE = ATTACHMENT_TYPES.register(
            "player_lifecycle_stage",
            () -> AttachmentType.builder(LifecycleStageData::empty)
                    .serialize(LifecycleStageData.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to && !hasEmbeddedConnection(to), LifecycleStageData.STREAM_CODEC)
                    .build()
    );
    public static final Supplier<AttachmentType<EntityBindingData>> LIVING_ENTITY_BINDINGS = ATTACHMENT_TYPES.register(
            "living_entity_bindings",
            () -> AttachmentType.builder(EntityBindingData::empty)
                    .serialize(EntityBindingData.CODEC)
                    .build()
    );
    public static final Supplier<AttachmentType<CombatMarkData>> LIVING_COMBAT_MARKS = ATTACHMENT_TYPES.register(
            "living_combat_marks",
            () -> AttachmentType.builder(CombatMarkData::empty)
                    .serialize(CombatMarkData.CODEC)
                    .build()
    );
    public static final Supplier<AttachmentType<CombatReactionData>> LIVING_COMBAT_REACTION = ATTACHMENT_TYPES.register(
            "living_combat_reaction",
            () -> AttachmentType.builder(CombatReactionData::empty).build()
    );

    private ModAttachments() {}

    public static CapabilityPolicyData getCapabilityPolicy(Player player) {
        return player.getData(PLAYER_CAPABILITY_POLICY);
    }

    public static void setCapabilityPolicy(Player player, CapabilityPolicyData data) {
        player.setData(PLAYER_CAPABILITY_POLICY, CapabilityPolicyApi.sanitize(data));
    }

    public static AbilityData get(Player player) {
        return player.getData(PLAYER_ABILITY_DATA);
    }

    public static void set(Player player, AbilityData data) {
        player.setData(PLAYER_ABILITY_DATA, AbilityGrantApi.sanitize(AbilityApi.sanitizeData(data)));
    }

    public static UpgradeProgressData getProgression(Player player) {
        return player.getData(PLAYER_UPGRADE_PROGRESS);
    }

    public static void setProgression(Player player, UpgradeProgressData data) {
        player.setData(PLAYER_UPGRADE_PROGRESS, UpgradeApi.sanitizeData(data));
    }

    public static ProfileSelectionData getProfiles(Player player) {
        return player.getData(PLAYER_PROFILE_SELECTIONS);
    }

    public static void setProfiles(Player player, ProfileSelectionData data) {
        player.setData(PLAYER_PROFILE_SELECTIONS, ProfileApi.sanitizeData(data));
    }

    public static CombatMarkData getMarks(LivingEntity entity) {
        return entity.getData(LIVING_COMBAT_MARKS);
    }

    public static void setMarks(LivingEntity entity, CombatMarkData data) {
        entity.setData(LIVING_COMBAT_MARKS, CombatMarkApi.sanitize(data));
    }

    public static CombatReactionData getReaction(LivingEntity entity) {
        return entity.getData(LIVING_COMBAT_REACTION);
    }

    public static void setReaction(LivingEntity entity, CombatReactionData data) {
        entity.setData(LIVING_COMBAT_REACTION, data == null ? CombatReactionData.empty() : data);
    }

    private static boolean hasEmbeddedConnection(ServerPlayer player) {
        if (player.connection == null) {
            return false;
        }

        if (player.connection.getConnection().channel() instanceof EmbeddedChannel) {
            return true;
        }

        SocketAddress remoteAddress = player.connection.getConnection().getRemoteAddress();
        return remoteAddress != null
                && remoteAddress.getClass().getName().contains("EmbeddedSocketAddress");
    }
}


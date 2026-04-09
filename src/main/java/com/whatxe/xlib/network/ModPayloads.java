package com.whatxe.xlib.network;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.AbilityGrantApi;
import com.whatxe.xlib.ability.AbilityLoadoutApi;
import com.whatxe.xlib.ability.AbilityRuntime;
import com.whatxe.xlib.ability.ProfileApi;
import com.whatxe.xlib.ability.AbilitySlotContainerApi;
import com.whatxe.xlib.ability.AbilitySlotReference;
import com.whatxe.xlib.ability.AbilityUseResult;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.progression.UpgradeApi;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModPayloads {
    public static final int NETWORK_SERIES = 1;
    public static final int NETWORK_REVISION = 5;
    public static final ProtocolVersion PLAY_PROTOCOL = new ProtocolVersion(NETWORK_SERIES, NETWORK_REVISION);
    public static final String PLAY_PROTOCOL_VERSION = PLAY_PROTOCOL.asRegistrarVersion(XLib.MODID);

    private ModPayloads() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PLAY_PROTOCOL_VERSION);
        registrar.playToServer(ActivateAbilityPayload.TYPE, ActivateAbilityPayload.STREAM_CODEC, ModPayloads::handleActivateAbility);
        registrar.playToServer(AssignAbilityPayload.TYPE, AssignAbilityPayload.STREAM_CODEC, ModPayloads::handleAssignAbility);
        registrar.playToServer(UnlockUpgradeNodePayload.TYPE, UnlockUpgradeNodePayload.STREAM_CODEC, ModPayloads::handleUnlockUpgradeNode);
        registrar.playToServer(ClaimProfilePayload.TYPE, ClaimProfilePayload.STREAM_CODEC, ModPayloads::handleClaimProfile);
    }

    private static void handleActivateAbility(ActivateAbilityPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            activateAbility(player, payload.slotReference());
        }
    }

    private static void handleAssignAbility(AssignAbilityPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        assignAbility(player, payload.slotReference(), payload.abilityId(), payload.modeId());
    }

    private static void handleUnlockUpgradeNode(UnlockUpgradeNodePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            unlockUpgradeNode(player, payload.nodeId());
        }
    }

    private static void handleClaimProfile(ClaimProfilePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            claimProfile(player, payload.groupId(), payload.profileId());
        }
    }

    static void assignAbility(
            ServerPlayer player,
            AbilitySlotReference slotReference,
            Optional<ResourceLocation> abilityId,
            Optional<ResourceLocation> modeId
    ) {
        AbilityData currentData = ModAttachments.get(player);
        if (!AbilitySlotContainerApi.isValidSlotReference(currentData, slotReference)) {
            return;
        }
        AbilityLoadoutApi.assign(player, slotReference, abilityId.orElse(null), modeId.orElse(null));
    }

    static void assignAbility(
            ServerPlayer player,
            int slot,
            Optional<ResourceLocation> abilityId,
            Optional<ResourceLocation> modeId
    ) {
        assignAbility(player, AbilitySlotReference.primary(slot), abilityId, modeId);
    }

    static void activateAbility(ServerPlayer player, AbilitySlotReference slotReference) {
        if (slotReference.slotIndex() < 0 || slotReference.pageIndex() < 0) {
            return;
        }

        AbilityData currentData = ModAttachments.get(player);
        if (!AbilitySlotContainerApi.isValidSlotReference(currentData, slotReference)) {
            return;
        }
        Optional<ResourceLocation> maybeAbilityId = AbilityLoadoutApi.resolvedAbilityId(currentData, slotReference);
        if (maybeAbilityId.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.xlib.slot_empty", slotReference.slotIndex() + 1), true);
            return;
        }

        ResourceLocation abilityId = maybeAbilityId.get();
        Optional<AbilityDefinition> maybeDefinition = AbilityApi.findAbility(abilityId);
        if (maybeDefinition.isEmpty()) {
            AbilityData updatedData = currentData.withAbilityInSlot(slotReference, null);
            ModAttachments.set(player, updatedData);
            player.displayClientMessage(Component.translatable("message.xlib.ability_missing"), true);
            return;
        }

        AbilityDefinition definition = maybeDefinition.get();
        AbilityUseResult result = AbilityRuntime.activate(player, currentData, definition, slotReference);
        if (result.feedback() != null) {
            player.displayClientMessage(result.feedback(), true);
        }

        if (result.consumed() && !result.data().equals(currentData)) {
            ModAttachments.set(player, result.data());
        }
    }

    static void activateAbility(ServerPlayer player, int slot) {
        activateAbility(player, AbilitySlotReference.primary(slot));
    }

    static void unlockUpgradeNode(ServerPlayer player, ResourceLocation nodeId) {
        UpgradeApi.unlockNode(player, nodeId);
    }

    static void claimProfile(ServerPlayer player, ResourceLocation groupId, ResourceLocation profileId) {
        if (ProfileApi.findProfile(profileId).filter(profile -> profile.groupId().equals(groupId)).isEmpty()) {
            player.displayClientMessage(Component.translatable("command.xlib.profiles.invalid", profileId.toString()), true);
            return;
        }
        ProfileApi.claimPendingProfile(player, profileId)
                .ifPresentOrElse(
                        failure -> player.displayClientMessage(failure, true),
                        () -> player.displayClientMessage(Component.translatable("message.xlib.profile_claimed", profileId.toString()), true)
                );
    }

    public record ProtocolVersion(int series, int revision) {
        public String asRegistrarVersion(String modId) {
            return modId + ":play/" + this.series + "." + this.revision;
        }

        public boolean compatibleWith(ProtocolVersion other) {
            return this.series == other.series && other.revision <= this.revision;
        }
    }
}


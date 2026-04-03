package com.whatxe.xlib.network;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.AbilityGrantApi;
import com.whatxe.xlib.ability.AbilityLoadoutApi;
import com.whatxe.xlib.ability.AbilityRuntime;
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
    public static final int NETWORK_REVISION = 2;
    public static final ProtocolVersion PLAY_PROTOCOL = new ProtocolVersion(NETWORK_SERIES, NETWORK_REVISION);
    public static final String PLAY_PROTOCOL_VERSION = PLAY_PROTOCOL.asRegistrarVersion(XLib.MODID);

    private ModPayloads() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PLAY_PROTOCOL_VERSION);
        registrar.playToServer(ActivateAbilityPayload.TYPE, ActivateAbilityPayload.STREAM_CODEC, ModPayloads::handleActivateAbility);
        registrar.playToServer(AssignAbilityPayload.TYPE, AssignAbilityPayload.STREAM_CODEC, ModPayloads::handleAssignAbility);
        registrar.playToServer(UnlockUpgradeNodePayload.TYPE, UnlockUpgradeNodePayload.STREAM_CODEC, ModPayloads::handleUnlockUpgradeNode);
    }

    private static void handleActivateAbility(ActivateAbilityPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            activateAbility(player, payload.slot());
        }
    }

    private static void handleAssignAbility(AssignAbilityPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        assignAbility(player, payload.slot(), payload.abilityId(), payload.modeId());
    }

    private static void handleUnlockUpgradeNode(UnlockUpgradeNodePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            unlockUpgradeNode(player, payload.nodeId());
        }
    }

    static void assignAbility(
            ServerPlayer player,
            int slot,
            Optional<ResourceLocation> abilityId,
            Optional<ResourceLocation> modeId
    ) {
        AbilityLoadoutApi.assign(player, slot, abilityId.orElse(null), modeId.orElse(null));
    }

    static void activateAbility(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= AbilityData.SLOT_COUNT) {
            return;
        }

        AbilityData currentData = ModAttachments.get(player);
        Optional<ResourceLocation> maybeAbilityId = AbilityLoadoutApi.resolvedAbilityId(currentData, slot);
        if (maybeAbilityId.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.xlib.slot_empty", slot + 1), true);
            return;
        }

        ResourceLocation abilityId = maybeAbilityId.get();
        Optional<AbilityDefinition> maybeDefinition = AbilityApi.findAbility(abilityId);
        if (maybeDefinition.isEmpty()) {
            AbilityData updatedData = currentData.withAbilityInSlot(slot, null);
            ModAttachments.set(player, updatedData);
            player.displayClientMessage(Component.translatable("message.xlib.ability_missing"), true);
            return;
        }

        AbilityDefinition definition = maybeDefinition.get();
        AbilityUseResult result = AbilityRuntime.activate(player, currentData, definition, slot);
        if (result.feedback() != null) {
            player.displayClientMessage(result.feedback(), true);
        }

        if (result.consumed() && !result.data().equals(currentData)) {
            ModAttachments.set(player, result.data());
        }
    }

    static void unlockUpgradeNode(ServerPlayer player, ResourceLocation nodeId) {
        UpgradeApi.unlockNode(player, nodeId);
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


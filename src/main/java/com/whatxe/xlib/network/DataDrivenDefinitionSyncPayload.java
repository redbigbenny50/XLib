package com.whatxe.xlib.network;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.DataDrivenAbilityApi;
import com.whatxe.xlib.ability.DataDrivenArtifactApi;
import com.whatxe.xlib.ability.DataDrivenConditionApi;
import com.whatxe.xlib.ability.DataDrivenIdentityApi;
import com.whatxe.xlib.ability.DataDrivenPassiveApi;
import com.whatxe.xlib.ability.DataDrivenProfileApi;
import com.whatxe.xlib.ability.DataDrivenProfileGroupApi;
import com.whatxe.xlib.progression.DataDrivenUpgradeNodeApi;
import com.whatxe.xlib.progression.DataDrivenUpgradePointTypeApi;
import com.whatxe.xlib.progression.DataDrivenUpgradeTrackApi;
import com.whatxe.xlib.value.DataDrivenTrackedValueApi;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DataDrivenDefinitionSyncPayload(
        Map<ResourceLocation, String> conditionDefinitions,
        Map<ResourceLocation, String> artifactDefinitions,
        Map<ResourceLocation, String> identityDefinitions,
        Map<ResourceLocation, String> abilityDefinitions,
        Map<ResourceLocation, String> passiveDefinitions,
        Map<ResourceLocation, String> profileGroupDefinitions,
        Map<ResourceLocation, String> profileDefinitions,
        Map<ResourceLocation, String> trackedValueDefinitions,
        Map<ResourceLocation, String> upgradePointTypeDefinitions,
        Map<ResourceLocation, String> upgradeTrackDefinitions,
        Map<ResourceLocation, String> upgradeNodeDefinitions
) implements CustomPacketPayload {
    private static final int MAX_JSON_LENGTH = 262_144;

    public static final Type<DataDrivenDefinitionSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(XLib.MODID, "sync_authored_definitions"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DataDrivenDefinitionSyncPayload> STREAM_CODEC =
            StreamCodec.of(DataDrivenDefinitionSyncPayload::encode, DataDrivenDefinitionSyncPayload::decode);

    public DataDrivenDefinitionSyncPayload {
        conditionDefinitions = Map.copyOf(conditionDefinitions);
        artifactDefinitions = Map.copyOf(artifactDefinitions);
        identityDefinitions = Map.copyOf(identityDefinitions);
        abilityDefinitions = Map.copyOf(abilityDefinitions);
        passiveDefinitions = Map.copyOf(passiveDefinitions);
        profileGroupDefinitions = Map.copyOf(profileGroupDefinitions);
        profileDefinitions = Map.copyOf(profileDefinitions);
        trackedValueDefinitions = Map.copyOf(trackedValueDefinitions);
        upgradePointTypeDefinitions = Map.copyOf(upgradePointTypeDefinitions);
        upgradeTrackDefinitions = Map.copyOf(upgradeTrackDefinitions);
        upgradeNodeDefinitions = Map.copyOf(upgradeNodeDefinitions);
    }

    public static DataDrivenDefinitionSyncPayload createCurrent() {
        return new DataDrivenDefinitionSyncPayload(
                DataDrivenConditionApi.definitionJsonsForSync(),
                DataDrivenArtifactApi.definitionJsonsForSync(),
                DataDrivenIdentityApi.definitionJsonsForSync(),
                DataDrivenAbilityApi.definitionJsonsForSync(),
                DataDrivenPassiveApi.definitionJsonsForSync(),
                DataDrivenProfileGroupApi.definitionJsonsForSync(),
                DataDrivenProfileApi.definitionJsonsForSync(),
                DataDrivenTrackedValueApi.definitionJsonsForSync(),
                DataDrivenUpgradePointTypeApi.definitionJsonsForSync(),
                DataDrivenUpgradeTrackApi.definitionJsonsForSync(),
                DataDrivenUpgradeNodeApi.definitionJsonsForSync()
        );
    }

    public static void clearClientDefinitions() {
        DataDrivenConditionApi.clearSyncedDefinitions();
        DataDrivenArtifactApi.clearSyncedDefinitions();
        DataDrivenIdentityApi.clearSyncedDefinitions();
        DataDrivenAbilityApi.clearSyncedDefinitions();
        DataDrivenPassiveApi.clearSyncedDefinitions();
        DataDrivenProfileGroupApi.clearSyncedDefinitions();
        DataDrivenProfileApi.clearSyncedDefinitions();
        DataDrivenTrackedValueApi.clearSyncedDefinitions();
        DataDrivenUpgradePointTypeApi.clearSyncedDefinitions();
        DataDrivenUpgradeTrackApi.clearSyncedDefinitions();
        DataDrivenUpgradeNodeApi.clearSyncedDefinitions();
    }

    public void applyClientSync() {
        DataDrivenConditionApi.syncDefinitionsFromJson(this.conditionDefinitions);
        DataDrivenArtifactApi.syncDefinitionsFromJson(this.artifactDefinitions);
        DataDrivenIdentityApi.syncDefinitionsFromJson(this.identityDefinitions);
        DataDrivenAbilityApi.syncDefinitionsFromJson(this.abilityDefinitions);
        DataDrivenPassiveApi.syncDefinitionsFromJson(this.passiveDefinitions);
        DataDrivenProfileGroupApi.syncDefinitionsFromJson(this.profileGroupDefinitions);
        DataDrivenProfileApi.syncDefinitionsFromJson(this.profileDefinitions);
        DataDrivenTrackedValueApi.syncDefinitionsFromJson(this.trackedValueDefinitions);
        DataDrivenUpgradePointTypeApi.syncDefinitionsFromJson(this.upgradePointTypeDefinitions);
        DataDrivenUpgradeTrackApi.syncDefinitionsFromJson(this.upgradeTrackDefinitions);
        DataDrivenUpgradeNodeApi.syncDefinitionsFromJson(this.upgradeNodeDefinitions);
    }

    @Override
    public Type<DataDrivenDefinitionSyncPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, DataDrivenDefinitionSyncPayload payload) {
        writeJsonMap(buffer, payload.conditionDefinitions);
        writeJsonMap(buffer, payload.artifactDefinitions);
        writeJsonMap(buffer, payload.identityDefinitions);
        writeJsonMap(buffer, payload.abilityDefinitions);
        writeJsonMap(buffer, payload.passiveDefinitions);
        writeJsonMap(buffer, payload.profileGroupDefinitions);
        writeJsonMap(buffer, payload.profileDefinitions);
        writeJsonMap(buffer, payload.trackedValueDefinitions);
        writeJsonMap(buffer, payload.upgradePointTypeDefinitions);
        writeJsonMap(buffer, payload.upgradeTrackDefinitions);
        writeJsonMap(buffer, payload.upgradeNodeDefinitions);
    }

    private static DataDrivenDefinitionSyncPayload decode(RegistryFriendlyByteBuf buffer) {
        return new DataDrivenDefinitionSyncPayload(
                readJsonMap(buffer),
                readJsonMap(buffer),
                readJsonMap(buffer),
                readJsonMap(buffer),
                readJsonMap(buffer),
                readJsonMap(buffer),
                readJsonMap(buffer),
                readJsonMap(buffer),
                readJsonMap(buffer),
                readJsonMap(buffer),
                readJsonMap(buffer)
        );
    }

    private static void writeJsonMap(RegistryFriendlyByteBuf buffer, Map<ResourceLocation, String> definitions) {
        buffer.writeMap(
                definitions,
                FriendlyByteBuf::writeResourceLocation,
                (FriendlyByteBuf buf, String json) -> buf.writeUtf(json, MAX_JSON_LENGTH)
        );
    }

    private static Map<ResourceLocation, String> readJsonMap(RegistryFriendlyByteBuf buffer) {
        return buffer.readMap(
                size -> new LinkedHashMap<>(),
                FriendlyByteBuf::readResourceLocation,
                buf -> buf.readUtf(MAX_JSON_LENGTH)
        );
    }
}

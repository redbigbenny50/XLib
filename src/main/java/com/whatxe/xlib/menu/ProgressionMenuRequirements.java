package com.whatxe.xlib.menu;

import com.whatxe.xlib.ability.ArtifactApi;
import com.whatxe.xlib.ability.IdentityApi;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.progression.UpgradeApi;
import com.whatxe.xlib.progression.UpgradePointType;
import com.whatxe.xlib.progression.UpgradeProgressData;
import com.whatxe.xlib.progression.UpgradeRequirements;
import java.util.function.BiPredicate;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

public final class ProgressionMenuRequirements {
    private ProgressionMenuRequirements() {}

    public static MenuAccessRequirement<UpgradeProgressData> predicate(
            Component description,
            BiPredicate<@Nullable Player, UpgradeProgressData> predicate
    ) {
        return MenuAccessRequirements.predicate(description, predicate);
    }

    public static MenuAccessRequirement<UpgradeProgressData> counterAtLeast(ResourceLocation counterId, int amount) {
        return predicate(
                Component.translatable("message.xlib.upgrade.requirement_counter", amount, UpgradeRequirements.displayCounterName(counterId)),
                (player, data) -> data.counter(counterId) >= amount
        );
    }

    public static MenuAccessRequirement<UpgradeProgressData> pointsAtLeast(ResourceLocation pointTypeId, int amount) {
        Component pointName = UpgradeApi.findPointType(pointTypeId)
                .map(UpgradePointType::displayName)
                .orElse(Component.literal(pointTypeId.toString()));
        return predicate(
                Component.translatable("message.xlib.upgrade.requirement_points", amount, pointName),
                (player, data) -> data.points(pointTypeId) >= amount
        );
    }

    public static MenuAccessRequirement<UpgradeProgressData> nodeUnlocked(ResourceLocation nodeId) {
        return predicate(
                Component.translatable(
                        "message.xlib.upgrade.requirement_node",
                        UpgradeApi.findNode(nodeId).map(node -> node.displayName()).orElse(Component.literal(nodeId.toString()))
                ),
                (player, data) -> data.hasUnlockedNode(nodeId)
        );
    }

    public static MenuAccessRequirement<UpgradeProgressData> trackCompleted(ResourceLocation trackId) {
        return predicate(
                Component.translatable("message.xlib.upgrade.requirement_track", UpgradeApi.displayTrackName(trackId)),
                (player, data) -> UpgradeApi.trackCompleted(data, trackId)
        );
    }

    public static MenuAccessRequirement<UpgradeProgressData> identityActive(ResourceLocation identityId) {
        return predicate(
                Component.translatable("message.xlib.requirement_identity", UpgradeRequirements.displayIdentityName(identityId)),
                (player, data) -> player != null && IdentityApi.hasIdentity(ModAttachments.get(player), identityId)
        );
    }

    public static MenuAccessRequirement<UpgradeProgressData> artifactActive(ResourceLocation artifactId) {
        return predicate(
                Component.translatable("message.xlib.requirement_artifact_active", Component.literal(artifactId.toString())),
                (player, data) -> player != null && ArtifactApi.isActive(player, artifactId)
        );
    }

    public static MenuAccessRequirement<UpgradeProgressData> artifactUnlocked(ResourceLocation artifactId) {
        return predicate(
                Component.translatable("message.xlib.requirement_artifact_unlocked", Component.literal(artifactId.toString())),
                (player, data) -> player != null && ArtifactApi.isUnlocked(ModAttachments.get(player), artifactId)
        );
    }

    public static MenuAccessRequirement<UpgradeProgressData> holding(ItemLike itemLike) {
        Item item = itemLike.asItem();
        return predicate(
                Component.translatable("message.xlib.requirement_holding_item", item.getDescription()),
                (player, data) -> player != null && (player.getMainHandItem().is(item) || player.getOffhandItem().is(item))
        );
    }

    public static MenuAccessRequirement<UpgradeProgressData> wearing(ItemLike itemLike) {
        Item item = itemLike.asItem();
        return predicate(
                Component.translatable("message.xlib.requirement_wearing_item", item.getDescription()),
                (player, data) -> player != null && player.getInventory().armor.stream().anyMatch(stack -> stack.is(item))
        );
    }
}

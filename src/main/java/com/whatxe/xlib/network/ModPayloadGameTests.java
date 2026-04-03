package com.whatxe.xlib.network;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.AbilityIcon;
import com.whatxe.xlib.ability.AbilityUseResult;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.gametest.GameTestPlayerFactory;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(XLib.MODID)
@PrefixGameTestTemplate(false)
public final class ModPayloadGameTests {
    private ModPayloadGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void assignAndActivateAbilityPayloadLogic(GameTestHelper helper) {
        ResourceLocation abilityId = id("payload_activation");
        AbilityApi.unregisterAbility(abilityId);

        AbilityApi.registerAbility(AbilityDefinition.builder(abilityId, AbilityIcon.ofTexture(id("payload_icon")))
                .cooldownTicks(20)
                .action((player, data) -> AbilityUseResult.success(data))
                .build());

        ServerPlayer player = GameTestPlayerFactory.create(helper);
        ModPayloads.assignAbility(player, 0, Optional.of(abilityId), Optional.empty());
        helper.assertTrue(
                ModAttachments.get(player).abilityInSlot(0).filter(abilityId::equals).isPresent(),
                "Payload assignment logic should bind abilities to combat slots"
        );

        ModPayloads.activateAbility(player, 0);
        helper.assertTrue(
                ModAttachments.get(player).cooldownFor(abilityId) > 0,
                "Payload activation logic should route through the server ability runtime"
        );

        AbilityApi.unregisterAbility(abilityId);
        helper.succeed();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(XLib.MODID, "gametest/" + path);
    }
}

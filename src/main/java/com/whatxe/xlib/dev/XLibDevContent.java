package com.whatxe.xlib.dev;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.GrantedItemApi;
import com.whatxe.xlib.ability.GrantedItemDefinition;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class XLibDevContent {
    public static final ResourceLocation LOANER_BLADE = id("loaner_blade");
    public static final ResourceLocation BOUND_TOTEM = id("bound_totem");
    public static final ResourceLocation KEEPSAKE_SIGIL = id("keepsake_sigil");

    private static boolean registered;

    private XLibDevContent() {}

    public static void registerIfNeeded() {
        if (!SharedConstants.IS_RUNNING_IN_IDE || registered) {
            return;
        }
        registered = true;

        registerGrantedItems();
    }

    private static void registerGrantedItems() {
        GrantedItemApi.registerGrantedItem(GrantedItemDefinition.builder(LOANER_BLADE, Items.IRON_SWORD).build());
        GrantedItemApi.registerGrantedItem(GrantedItemDefinition.builder(BOUND_TOTEM, Items.TOTEM_OF_UNDYING)
                .undroppable()
                .build());
        GrantedItemApi.registerGrantedItem(GrantedItemDefinition.builder(KEEPSAKE_SIGIL, (player, data) -> new ItemStack(Items.ECHO_SHARD, 2))
                .keepWhenRevoked()
                .build());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(XLib.MODID, "demo_" + path);
    }
}

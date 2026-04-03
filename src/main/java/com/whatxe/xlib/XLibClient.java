package com.whatxe.xlib;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = XLib.MODID, dist = Dist.CLIENT)
public class XLibClient {
    public XLibClient(IEventBus modEventBus) {
        AbilityLibraryClient.bootstrap(modEventBus);
    }
}


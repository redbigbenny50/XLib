package com.whatxe.xlib;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(XLib.MODID)
public class XLib {
    public static final String MODID = "xlib";
    public static final Logger LOGGER = LogUtils.getLogger();

    public XLib(IEventBus modEventBus) {
        AbilityLibrary.bootstrap(modEventBus);
    }
}


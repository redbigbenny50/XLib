package com.whatxe.xlib.mixin.client;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GLX.class)
abstract class GLXMixin {
    @Shadow
    private static String cpuInfo;

    @Inject(method = "_init", at = @At("HEAD"), cancellable = true)
    private static void xlib$skipCpuProbeWhenRequested(int debugVerbosity, boolean synchronous, CallbackInfo callbackInfo) {
        if (!Boolean.getBoolean("xlib.disableHardwareSystemReport")) {
            return;
        }

        cpuInfo = "<unknown>";
        GlDebug.enableDebugCallback(debugVerbosity, synchronous);
        callbackInfo.cancel();
    }
}

package com.whatxe.xlib.mixin.client;

import net.minecraft.SystemReport;
import oshi.SystemInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SystemReport.class)
abstract class SystemReportMixin {
    @Inject(method = "putHardware", at = @At("HEAD"), cancellable = true)
    private void xlib$skipHardwareProbeWhenRequested(SystemInfo info, CallbackInfo callbackInfo) {
        if (Boolean.getBoolean("xlib.disableHardwareSystemReport")) {
            callbackInfo.cancel();
        }
    }
}

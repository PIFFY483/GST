package com.gst.client.mixin;

import com.gst.client.render.SpaceTransitionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void gst$suppressLoadingScreen(Screen screen, CallbackInfo ci) {
        // Eğer uzay geçişi aktifse ve Minecraft yükleme ekranı açmaya çalışıyorsa: ENGELE!
        if (SpaceTransitionManager.isTransitioning()) {
            if (screen instanceof DownloadingTerrainScreen || screen instanceof ProgressScreen) {
                ci.cancel(); // Vanilla loading ekranı asssla ekrana gelemez!
            }
        }
    }
}
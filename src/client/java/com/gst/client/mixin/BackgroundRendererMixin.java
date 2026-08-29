package com.gst.client.mixin;

import com.gst.client.render.PlanetApproachClientHandler;
import com.gst.client.render.SpaceFogHandler;
import com.gst.client.render.SpaceTransitionManager;
import com.gst.world.PlanetDimensions;
import com.gst.world.SpaceDimensions;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {

    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void gst$applySpaceFogDistance(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        Entity entity = camera.getFocusedEntity();
        if (entity == null) return;

        double atmosphereCurtain = SpaceFogHandler.getCurtainProgress(entity.getY());
        double planetApproach = PlanetApproachClientHandler.update(entity);

        // İki farklı "perde kaynağından" hangisi daha baskınsa o kullanılır:
        // atmosferden çıkış sisi VEYA bir gezegene yaklaşma sisi.
        double effectiveCurtain = Math.max(atmosphereCurtain, planetApproach);

        if (effectiveCurtain > 0.5) {
            SpaceTransitionManager.startTransition();
        } else if (entity.getWorld().getRegistryKey() == SpaceDimensions.SPACE_WORLD_KEY
                || entity.getWorld().getRegistryKey() == PlanetDimensions.PLANETS_WORLD_KEY) {
            // Uzayda ya da bir gezegen yüzeyinde, ve perde açıldıysa: geçiş modunu kapat
            SpaceTransitionManager.stopTransition();
        }

        if (effectiveCurtain <= 0.0) return;

        float newStart = (float) MathHelper.lerp(effectiveCurtain, viewDistance * 0.75f, 0.0f);
        float newEnd = (float) MathHelper.lerp(effectiveCurtain, viewDistance, viewDistance * 0.02f);

        RenderSystem.setShaderFogStart(newStart);
        RenderSystem.setShaderFogEnd(newEnd);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private static void gst$applySpaceFogColor(Camera camera, float tickDelta, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfo ci) {
        Entity entity = camera.getFocusedEntity();
        if (entity == null) return;

        double atmosphereBlackout = SpaceFogHandler.getSpaceBlackoutProgress(entity.getY());
        double planetApproach = PlanetApproachClientHandler.getLastProgress();

        double effectiveBlackout = Math.max(atmosphereBlackout, planetApproach);
        if (effectiveBlackout <= 0.0) return;

        float[] fogColor = RenderSystem.getShaderFogColor();

        float r = (float) MathHelper.lerp(effectiveBlackout, fogColor[0], 0.0f);
        float g = (float) MathHelper.lerp(effectiveBlackout, fogColor[1], 0.0f);
        float b = (float) MathHelper.lerp(effectiveBlackout, fogColor[2], 0.0f);

        RenderSystem.setShaderFogColor(r, g, b);
    }
}
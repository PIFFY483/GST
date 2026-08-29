package com.gst.client.mixin;

import com.gst.client.render.PlanetApproachClientHandler;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    /**
     * Gezegene yaklaşırken FOV kademeli olarak daraltılır (tam yaklaşmada normalin %60'ı).
     * Bu, "atmosfere giriyoruz" hissini güçlendiren ucuz ama etkili bir efekt.
     */
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void gst$narrowFovOnPlanetApproach(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        double progress = PlanetApproachClientHandler.getLastProgress();
        if (progress <= 0.0) return;

        double fov = cir.getReturnValue();
        double narrowedFov = MathHelper.lerp(progress, fov, fov * 0.6);
        cir.setReturnValue(narrowedFov);
    }
}
package com.gst.client.render;

import com.gst.client.network.ClientUniverseSeedCache;
import com.gst.world.star.PlanetApproachHandler;
import net.minecraft.entity.Entity;

/**
 * Client tarafında her frame'de en yakın gezegene yaklaşma ilerlemesini hesaplayıp
 * cache'ler. Hem sis/perde render'ı hem de FOV daraltma mixin'i bu değeri okur.
 *
 * Evren seed'i World'den DEĞİL, login'de senkronize edilen ClientUniverseSeedCache'ten
 * okunur - client-side World/ClientWorld gerçek seed'i taşımaz.
 */
public final class PlanetApproachClientHandler {

    private static double lastProgress = 0.0;

    private PlanetApproachClientHandler() {
    }

    public static double update(Entity cameraEntity) {
        if (!ClientUniverseSeedCache.isReceived()) {
            lastProgress = 0.0;
            return 0.0;
        }

        long universeSeed = ClientUniverseSeedCache.get();
        long worldTime = cameraEntity.getWorld().getTime();

        PlanetApproachHandler.NearestPlanet nearest = PlanetApproachHandler.findNearest(
                universeSeed, cameraEntity.getX(), cameraEntity.getY(), cameraEntity.getZ(), worldTime
        );

        if (nearest == null) {
            lastProgress = 0.0;
            return 0.0;
        }

        lastProgress = PlanetApproachHandler.getApproachProgress(nearest.distance(), nearest.orbit().visualRadius());
        return lastProgress;
    }

    public static double getLastProgress() {
        return lastProgress;
    }
}
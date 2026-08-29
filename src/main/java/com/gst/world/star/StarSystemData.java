package com.gst.world.star;

import java.util.List;

/**
 * Tek bir yıldız sisteminin çözülmüş verisi: yıldızın konumu/görseli + etrafındaki gezegenler.
 */
public record StarSystemData(
        int sectorX,
        int sectorZ,
        long seed,
        double starX,
        double starY,
        double starZ,
        float starVisualRadius,
        int starColorRgb,
        List<PlanetOrbit> planets
) {
}
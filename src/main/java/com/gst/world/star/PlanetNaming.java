package com.gst.world.star;

import com.gst.world.UniverseSeedManager;
import com.gst.world.planet.PlanetData;
import com.gst.world.planet.PlanetGridManager;

/**
 * Yıldız/gezegen isimlendirmesi.
 *
 * Format:
 * GST-XXXXXX <yörünge harfi> <gezegen tipi harfi>
 *
 * Örnek:
 * GST-910546 b i
 *
 * Burada:
 * GST-910546 -> yıldız katalog adı
 * b          -> yörüngedeki gezegen sırası
 * i          -> gezegen tipi: ICE
 */
public final class PlanetNaming {

    private PlanetNaming() {
    }

    public static String getStarName(StarSystemData system) {
        long code = Math.floorMod(system.seed(), 1_000_000L);
        return String.format("GST-%06d", code);
    }

    public static String getPlanetName(StarSystemData system, PlanetOrbit orbit) {
        char orbitLetter = (char) ('b' + orbit.index());
        char typeLetter = getPlanetTypeSuffix(orbit);

        return getStarName(system) + " " + orbitLetter + " " + typeLetter;
    }

    /**
     * Gezegenin gerçek terrain hücresine bakarak PlanetType'ını çözer
     * ve tipe ait kısa harfi döner.
     */
    private static char getPlanetTypeSuffix(PlanetOrbit orbit) {
        long universeSeed = UniverseSeedManager.getUniverseSeed();

        PlanetData planet = PlanetGridManager.getPlanetAtCell(
                universeSeed,
                orbit.terrainCellX(),
                orbit.terrainCellZ()
        );

        return planet.type().getTypeSuffix();
    }
}
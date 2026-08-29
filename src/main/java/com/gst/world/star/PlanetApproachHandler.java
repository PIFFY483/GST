package com.gst.world.star;

import com.gst.world.planet.PlanetGridManager;

import java.util.List;
import java.util.function.LongPredicate;

/**
 * Oyuncunun en yakın gezegene ne kadar yaklaştığını hesaplayan paylaşılan (client + server) mantık.
 *
 * Bilerek I/O içermez, tamamen deterministiktir (seed + konum + zaman -> sonuç).
 * Bu sayede client (sis/FOV için) ve server (gerçek teleport anını tetiklemek için)
 * birbirinden habersiz ama aynı formülü kullanarak senkron çalışabilir -
 * tıpkı SpaceFogHandler'ın atmosferden çıkışta yaptığı gibi.
 */
public final class PlanetApproachHandler {

    /** Bu mesafe çarpanında (gezegen görsel yarıçapına göre) sis/perde başlar. */
    private static final double ATMOSPHERE_START_MULTIPLIER = 3.0;
    /** Bu mesafe çarpanında perde tamamlanır - yani teleport anı. */
    private static final double ATMOSPHERE_END_MULTIPLIER = 1.05;

    /**
     * findNearestExcluding için: hariç tutulan gezegenler yüzünden radius=1'de
     * hiçbir aday bulunamazsa, bu sınıra kadar arama yarıçapını genişletiriz.
     */
    private static final int MAX_EXCLUDING_SEARCH_RADIUS = 6;

    private PlanetApproachHandler() {
    }

    public record NearestPlanet(
            StarSystemData system,
            PlanetOrbit orbit,
            double planetX,
            double planetY,
            double planetZ,
            double distance
    ) {
    }

    /**
     * Oyuncunun bulunduğu konuma göre en yakın gezegeni bulur (komşu sektörler dahil).
     * @return en yakın gezegen, ya da yakında hiç gezegen yoksa null
     */
    public static NearestPlanet findNearest(long universeSeed, double x, double y, double z, long worldTime) {
        return findNearestInRadius(universeSeed, x, y, z, worldTime, 1, cellKey -> false);
    }

    /**
     * findNearest ile aynı, ancak "hariç tutulan" (ör. oyuncunun yakın zamanda
     * ziyaret ettiği) gezegen hücrelerini atlar - bkz. PlanetVisitHistory.
     * Aday radius=1 içinde bulunamazsa (hepsi hariç tutulmuşsa), yakında yeni
     * bir tane bulana ya da MAX_EXCLUDING_SEARCH_RADIUS'a ulaşana kadar arama
     * yarıçapını kademeli genişletir.
     *
     * @return en yakın hariç-tutulmamış gezegen, ya da bulunamazsa null
     */
    public static NearestPlanet findNearestExcluding(long universeSeed, double x, double y, double z, long worldTime, LongPredicate exclude) {
        for (int radius = 1; radius <= MAX_EXCLUDING_SEARCH_RADIUS; radius++) {
            NearestPlanet found = findNearestInRadius(universeSeed, x, y, z, worldTime, radius, exclude);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static NearestPlanet findNearestInRadius(long universeSeed, double x, double y, double z, long worldTime, int radiusInSectors, LongPredicate exclude) {
        List<StarSystemData> nearbySystems = StarSystemGenerator.getNearbySystems(universeSeed, x, z, radiusInSectors);

        NearestPlanet closest = null;
        for (StarSystemData system : nearbySystems) {
            for (PlanetOrbit orbit : system.planets()) {
                long cellKey = PlanetGridManager.packCell(orbit.terrainCellX(), orbit.terrainCellZ());
                if (exclude.test(cellKey)) {
                    continue;
                }

                double px = orbit.getX(system.starX(), worldTime);
                double pz = orbit.getZ(system.starZ(), worldTime);
                double py = system.starY();

                double dx = x - px;
                double dy = y - py;
                double dz = z - pz;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (closest == null || distance < closest.distance()) {
                    closest = new NearestPlanet(system, orbit, px, py, pz, distance);
                }
            }
        }
        return closest;
    }

    /**
     * 0.0 = gezegen çok uzakta, hiçbir efekt yok.
     * 1.0 = tam atmosfere giriş anı - bu değere ulaşınca server teleportu tetikler.
     * Arada: sis/FOV/atmosfer efektleri bu değerle orantılı uygulanmalı.
     */
    public static double getApproachProgress(double distance, float visualRadius) {
        double startDist = visualRadius * ATMOSPHERE_START_MULTIPLIER;
        double endDist = visualRadius * ATMOSPHERE_END_MULTIPLIER;

        if (distance >= startDist) return 0.0;
        if (distance <= endDist) return 1.0;

        return 1.0 - ((distance - endDist) / (startDist - endDist));
    }
}

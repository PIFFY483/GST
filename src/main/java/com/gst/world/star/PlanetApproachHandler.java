package com.gst.world.star;

import java.util.List;

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
        List<StarSystemData> nearbySystems = StarSystemGenerator.getNearbySystems(universeSeed, x, z, 1);

        NearestPlanet closest = null;
        for (StarSystemData system : nearbySystems) {
            for (PlanetOrbit orbit : system.planets()) {
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
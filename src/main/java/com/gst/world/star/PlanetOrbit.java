package com.gst.world.star;

/**
 * Bir yıldıza bağlı tek bir gezegenin yörünge ve görsel verisi.
 *
 * terrainCellX/terrainCellZ, bu gezegenin gerçek yüzeyinin "gst:planets"
 * boyutunda hangi hücrede üretileceğini belirtir (bkz. PlanetGridManager).
 * Yani bir gezegenin KONUMU burada (uzayda), YÜZEYİ orada (terrain hücresinde).
 */
public record PlanetOrbit(
        int index,
        long seed,
        int terrainCellX,
        int terrainCellZ,
        double orbitRadius,
        double orbitAngleRad,
        double orbitSpeedRadPerTick,
        float visualRadius
) {

    /** Verilen an için gezegenin mutlak X koordinatını döner (yıldız merkezine göre). */
    public double getX(double starX, long worldTime) {
        double angle = orbitAngleRad + orbitSpeedRadPerTick * worldTime;
        return starX + Math.cos(angle) * orbitRadius;
    }

    public double getZ(double starZ, long worldTime) {
        double angle = orbitAngleRad + orbitSpeedRadPerTick * worldTime;
        return starZ + Math.sin(angle) * orbitRadius;
    }
}
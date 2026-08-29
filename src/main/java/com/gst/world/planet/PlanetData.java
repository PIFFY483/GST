package com.gst.world.planet;

import java.util.Random;

/**
 * Tek bir gezegen hücresinin çözülmüş (resolved) verisi.
 * Bu obje tamamen deterministiktir: aynı cellX/cellZ + world seed her zaman
 * birebir aynı PlanetData'yı üretir. Diskte saklanmasına gerek yok, her an
 * yeniden hesaplanabilir (bkz. PlanetGridManager).
 */
public record PlanetData(
        int cellX,
        int cellZ,
        long seed,
        PlanetType type,
        int baseHeight,
        int heightVariance,
        boolean hasAtmosphere,
        float gravityFactor
) {

    public static PlanetData resolve(int cellX, int cellZ, long seed) {
        PlanetType type = PlanetType.selectForSeed(seed);

        // Seed'i tüket ki tip seçimiyle diğer parametreler birbirini etkilemesin
        Random random = new Random(seed ^ 0x9E3779B97F4A7C15L);

        int baseHeight = switch (type) {
            case GAS_GIANT -> 0; // Gaz devlerinde "katı yüzey" kavramı farklı ele alınacak (ileride)
            case ROCKY, BARREN, CHAOTIC -> 64 + random.nextInt(40);
            case ICE -> 68 + random.nextInt(20);
            case LAVA -> 40 + random.nextInt(30);
            case CRYSTAL -> 72 + random.nextInt(50);
            case OCEAN -> 50 + random.nextInt(20);
            case EARTH_LIKE -> 64 + random.nextInt(24);
        };

        int heightVariance = 8 + random.nextInt(24);
        boolean hasAtmosphere = type != PlanetType.GAS_GIANT && random.nextFloat() < 0.6f;

        // Yerçekimi katsayısı: 0.3 (çok hafif, ay gibi zıplarsın) - 1.7 (çok ağır) arası.
        // 1.0 = vanilla Minecraft yerçekimi.
        float gravityFactor = 0.3f + random.nextFloat() * 1.4f;

        return new PlanetData(cellX, cellZ, seed, type, baseHeight, heightVariance, hasAtmosphere, gravityFactor);
    }
}
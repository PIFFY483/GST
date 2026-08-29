package com.gst.world.planet;

import java.util.concurrent.ConcurrentHashMap;

/**
 * "gst:planets" dimension'ını sabit boyutlu hücrelere (gezegenlere) bölen ve
 * her hücre için deterministik seed/veri üreten merkezi sınıf.
 *
 * Bir blok koordinatı verildiğinde hangi "gezegenin" içinde olduğunu ve o
 * gezegenin özelliklerini (PlanetData) söyler. Chunk generator, bu sınıfı
 * kullanarak hangi terraini üreteceğine karar verecek (bir sonraki adım).
 */
public final class PlanetGridManager {

    /** Bir gezegen hücresinin blok cinsinden kenar uzunluğu. */
    public static final int CELL_SIZE = 512;

    private static final ConcurrentHashMap<Long, PlanetData> CACHE = new ConcurrentHashMap<>();

    private PlanetGridManager() {
    }

    public static int blockToCell(int blockCoord) {
        return Math.floorDiv(blockCoord, CELL_SIZE);
    }

    /**
     * Verilen dünya koordinatının ait olduğu gezegenin verisini döner.
     * Sonuç cache'lenir; ilk çağrıda hesaplanır, sonrasında tekrar hesaplanmaz.
     */
    public static PlanetData getPlanetAt(long worldSeed, int blockX, int blockZ) {
        int cellX = blockToCell(blockX);
        int cellZ = blockToCell(blockZ);
        return getPlanetAtCell(worldSeed, cellX, cellZ);
    }

    public static PlanetData getPlanetAtCell(long worldSeed, int cellX, int cellZ) {
        long cacheKey = packCell(cellX, cellZ);
        return CACHE.computeIfAbsent(cacheKey, key -> {
            long cellSeed = computeCellSeed(worldSeed, cellX, cellZ);
            return PlanetData.resolve(cellX, cellZ, cellSeed);
        });
    }

    /**
     * cellX/cellZ'yi tek bir long'a paketler (hash map key'i için).
     * Vanilla'nın ChunkPos#toLong yaklaşımına benzer.
     */
    public static long packCell(int cellX, int cellZ) {
        return ((long) cellX & 0xFFFFFFFFL) | (((long) cellZ & 0xFFFFFFFFL) << 32);
    }

    /**
     * Dünya seed'i + hücre koordinatlarından deterministik bir seed üretir.
     * SplitMix64 tarzı bir finalizer kullanır: iyi dağılım sağlar, aynı girdi
     * her zaman aynı çıktıyı verir.
     */
    public static long computeCellSeed(long worldSeed, int cellX, int cellZ) {
        long seed = worldSeed;
        seed = seed * 6364136223846793005L + 1442695040888963407L + cellX;
        seed = seed * 6364136223846793005L + 1442695040888963407L + cellZ;

        seed ^= (seed >>> 33);
        seed *= 0xff51afd7ed558ccdL;
        seed ^= (seed >>> 33);
        seed *= 0xc4ceb9fe1a85ec53L;
        seed ^= (seed >>> 33);

        return seed;
    }

    /**
     * Test/debug amaçlı: cache'i temizler. Normal oyun akışında çağrılmasına
     * gerek yok çünkü veri zaten deterministik (bozulma riski yok).
     */
    public static void clearCache() {
        CACHE.clear();
    }
}
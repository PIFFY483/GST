package com.gst.world.star;

import com.gst.world.planet.PlanetGridManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "gst:space" boyutunu dev sektörlere böler, her sektöre deterministik olarak
 * 0 ya da 1 yıldız sistemi yerleştirir. Chunk generation'a çok benzer bir
 * mantık: oyuncunun yakınındaki sektörler talep üzerine hesaplanır, evrenin
 * tamamı hafızada tutulmaz.
 *
 * ÖLÇEK NOTU: SECTOR_SIZE, vanilla world border'ın (~30.000.000 blok, merkeze
 * göre) içinde kalacak şekilde seçildi. Daha büyük bir evren istenirse,
 * ileride bu boyutun world border kontrolünü mixin ile devre dışı bırakmak
 * gerekecek - şimdilik buna gerek yok.
 */
public final class StarSystemGenerator {

    /** Bir sektörün blok cinsinden kenar uzunluğu. */
    public static final double SECTOR_SIZE = 4_000_000.0;

    /** Bir sektörde yıldız bulunma ihtimali (0.0 - 1.0). Geri kalanı boş uzay. */
    private static final float STAR_PRESENCE_CHANCE = 0.65f;

    /** Görsel gezegen yarıçapı aralığı (blok). "Oyuncudan devasa büyük" hissi için yüksek tutuldu. */
    private static final float PLANET_MIN_VISUAL_RADIUS = 150_000f;
    private static final float PLANET_MAX_VISUAL_RADIUS = 500_000f;

    /**
     * Terrain hücre koordinatının (cellX/cellZ) alabileceği aralık.
     * cellX * PlanetGridManager.CELL_SIZE (512) sonucu, vanilla world border'ının
     * (varsayılan merkeze göre ~29.999.984 blok) GÜVENLE içinde kalmalı.
     * 20.000 * 512 = 10.240.000 blok - bol güvenlik payı bırakır.
     */
    private static final long TERRAIN_CELL_RANGE = 20_000L;

    private static final ConcurrentHashMap<Long, StarSystemData> CACHE = new ConcurrentHashMap<>();

    private StarSystemGenerator() {
    }

    public static int worldToSector(double blockCoord) {
        return (int) Math.floor(blockCoord / SECTOR_SIZE);
    }

    /**
     * @return o sektördeki sistem verisi, ya da sektör boşsa null (uzayın çoğu boş olmalı)
     */
    public static StarSystemData getSystemAt(long universeSeed, int sectorX, int sectorZ) {
        long key = PlanetGridManager.packCell(sectorX, sectorZ);
        return CACHE.computeIfAbsent(key, k -> generate(universeSeed, sectorX, sectorZ));
    }

    /**
     * Oyuncunun mevcut konumuna göre "görüş mesafesi" içindeki tüm sistemleri döner.
     * radiusInSectors=1 -> oyuncunun bulunduğu sektör + komşu 8 sektör (3x3 alan).
     */
    public static List<StarSystemData> getNearbySystems(long universeSeed, double playerX, double playerZ, int radiusInSectors) {
        int centerSectorX = worldToSector(playerX);
        int centerSectorZ = worldToSector(playerZ);

        List<StarSystemData> result = new ArrayList<>();
        for (int dx = -radiusInSectors; dx <= radiusInSectors; dx++) {
            for (int dz = -radiusInSectors; dz <= radiusInSectors; dz++) {
                StarSystemData system = getSystemAt(universeSeed, centerSectorX + dx, centerSectorZ + dz);
                if (system != null) {
                    result.add(system);
                }
            }
        }
        return result;
    }

    private static StarSystemData generate(long universeSeed, int sectorX, int sectorZ) {
        long sectorSeed = PlanetGridManager.computeCellSeed(universeSeed ^ 0x53544152L /* "STAR" */, sectorX, sectorZ);
        Random random = new Random(sectorSeed);

        if (random.nextFloat() >= STAR_PRESENCE_CHANCE) {
            return null; // Bu sektör boş uzay
        }

        // Yıldızı sektörün kenarlarına çok yaklaştırmadan, içinde rastgele konumlandır
        double margin = SECTOR_SIZE * 0.25;
        double starX = sectorX * SECTOR_SIZE + margin + random.nextDouble() * (SECTOR_SIZE - margin * 2);
        double starZ = sectorZ * SECTOR_SIZE + margin + random.nextDouble() * (SECTOR_SIZE - margin * 2);
        double starY = 1200.0 + random.nextDouble() * 3000.0; // derin uzayda dikey dağılım

        float starVisualRadius = 600_000f + random.nextFloat() * 900_000f;
        int starColor = randomStarColor(random);

        int planetCount = random.nextInt(6); // 0-5 gezegen
        List<PlanetOrbit> planets = new ArrayList<>(planetCount);

        double orbitRadius = starVisualRadius * 3.0;
        for (int i = 0; i < planetCount; i++) {
            orbitRadius += starVisualRadius * 1.5 + random.nextDouble() * starVisualRadius * 4.0;

            long planetSeed = sectorSeed * 31L + i + 1;

            // Bu gezegenin terrain hücresi - sınırlı bir aralıkta (bkz. TERRAIN_CELL_RANGE),
            // world border'ın disina cikmayacak sekilde.
            long cellHashX = PlanetGridManager.computeCellSeed(planetSeed, 1, 0);
            long cellHashZ = PlanetGridManager.computeCellSeed(planetSeed, 2, 0);
            int terrainCellX = (int) (cellHashX % TERRAIN_CELL_RANGE);
            int terrainCellZ = (int) (cellHashZ % TERRAIN_CELL_RANGE);

            float visualRadius = PLANET_MIN_VISUAL_RADIUS
                    + random.nextFloat() * (PLANET_MAX_VISUAL_RADIUS - PLANET_MIN_VISUAL_RADIUS);

            double angle = random.nextDouble() * Math.PI * 2;

            // Yörünge hızı: çok yavaş, isteğe göre 0 verilip statik de bırakılabilir
            double orbitSpeed = (random.nextBoolean() ? 1 : -1) * (0.00001 + random.nextDouble() * 0.00003);

            planets.add(new PlanetOrbit(i, planetSeed, terrainCellX, terrainCellZ, orbitRadius, angle, orbitSpeed, visualRadius));
        }

        return new StarSystemData(sectorX, sectorZ, sectorSeed, starX, starY, starZ, starVisualRadius, starColor, planets);
    }

    private static int randomStarColor(Random random) {
        // ARGB paketlenmiş renkler: mavi-beyaz, sarı, turuncu, kızıl dev, beyaz cüce
        int[] palette = {
                0xFFCCE0FF,
                0xFFFFF4C1,
                0xFFFFB86B,
                0xFFFF6B57,
                0xFFFFFFFF
        };
        return palette[random.nextInt(palette.length)];
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
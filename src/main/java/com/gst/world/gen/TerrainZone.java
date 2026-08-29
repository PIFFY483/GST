package com.gst.world.gen;

import com.gst.world.planet.PlanetType;

import java.util.Random;

/**
 * Bir gezegenin arazisini "kullanım bölgelerine" ayırır: Dağ, Düzlük, Göl
 * Havzası, Deniz, Obruk. Terrain şekli VE sıvı yerleşimi bu bölgeye göre
 * belirlenir - böylece göller/denizler sadece mantıklı yerlerde (kendileri
 * için ayrılmış havza/deniz bölgelerinde) oluşur, dağ tepesinde rastgele
 * su birikintisi çıkmaz.
 *
 * Her PlanetType'ın kendine has bölge ağırlıkları var: örneğin OCEAN
 * tipinde SEA bölgesi çok baskın, LAVA'da ise sıvı yok tiplerde (ROCKY,
 * BARREN, CRYSTAL, CHAOTIC, GAS_GIANT) hiç göl/deniz bölgesi oluşmaz.
 */
public enum TerrainZone {
    PLAINS,
    MOUNTAIN,
    LAKE_BASIN,
    SEA,
    SINKHOLE;

    /**
     * Verilen seed'e göre, bu PlanetType için ağırlıklı rastgele bir bölge seçer.
     * Aynı seed + aynı tip her zaman aynı bölgeyi üretir (deterministik).
     */
    public static TerrainZone selectForSeed(long seed, PlanetType type) {
        int[] weights = weightsFor(type);

        Random random = new Random(seed);
        int total = 0;
        for (int w : weights) {
            total += w;
        }
        if (total <= 0) {
            return PLAINS;
        }

        int roll = random.nextInt(total);
        int cursor = 0;
        TerrainZone[] zones = values();
        for (int i = 0; i < zones.length; i++) {
            cursor += weights[i];
            if (roll < cursor) {
                return zones[i];
            }
        }
        return PLAINS;
    }

    /** Sırasıyla: PLAINS, MOUNTAIN, LAKE_BASIN, SEA, SINKHOLE ağırlıkları. */
    private static int[] weightsFor(PlanetType type) {
        return switch (type) {
            case OCEAN -> new int[]{10, 5, 15, 65, 5};
            case EARTH_LIKE -> new int[]{40, 20, 25, 10, 5};
            case LAVA -> new int[]{35, 25, 30, 0, 10};
            case ICE -> new int[]{40, 30, 20, 5, 5};
            // Sıvısı olmayan tipler: hiç göl/deniz bölgesi yok, sadece dağ/düzlük (+ kuru obruk)
            case ROCKY, BARREN, CRYSTAL, CHAOTIC, GAS_GIANT -> new int[]{55, 40, 0, 0, 5};
        };
    }
}
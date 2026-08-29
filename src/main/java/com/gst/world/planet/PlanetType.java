package com.gst.world.planet;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import java.util.Random;

/**
 * Bir hücrenin (gezegenin) çözülebileceği temel tipler.
 * Her tip artık sadece blok paleti değil, kendine has bir "arazi karakteri"
 * taşıyor: kaç katmanlı (octave) gürültü kullanılacağı, genlik çarpanı,
 * varsa sıvı seviyesi/bloğu, ve nehir üretilip üretilmeyeceği.
 *
 * Ayrıca her tipin isim üretiminde kullanılacak bir sınıflandırma harfi vardır.
 */
public enum PlanetType {

    // weight, yüzey, alt yüzey, aksan, octave, frekans çarpanı, genlik çarpanı, sıvı, sıvı seviye offset, nehir, isim harfi
    ROCKY(30, Blocks.STONE, Blocks.DEEPSLATE, Blocks.GRAVEL, 3, 1.0, 1.0, null, 0, false, 'r'),
    ICE(20, Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.SNOW_BLOCK, 2, 0.6, 0.45, Blocks.ICE, -2, false, 'i'),
    LAVA(15, Blocks.BASALT, Blocks.MAGMA_BLOCK, Blocks.BLACKSTONE, 3, 1.3, 1.3, Blocks.LAVA, -6, false, 'f'),
    BARREN(20, Blocks.STONE, Blocks.RED_SAND, Blocks.TERRACOTTA, 2, 0.8, 0.6, null, 0, false, 'b'),
    CRYSTAL(10, Blocks.AMETHYST_BLOCK, Blocks.CALCITE, Blocks.TUFF, 3, 1.1, 1.2, null, 0, false, 'k'),
    GAS_GIANT(5, Blocks.WHITE_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.WHITE_CONCRETE, 1, 1.0, 0.1, null, 0, false, 'g'),
    OCEAN(12, Blocks.SAND, Blocks.SANDSTONE, Blocks.SAND, 2, 0.7, 0.4, Blocks.WATER, 12, false, 'o'),
    EARTH_LIKE(15, Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.STONE, 3, 0.9, 0.8, Blocks.WATER, 0, true, 'e'),
    CHAOTIC(8, Blocks.GRAVEL, Blocks.DEEPSLATE, Blocks.COBBLESTONE, 5, 1.6, 2.0, null, 0, false, 'c');

    private final int weight;
    private final Block surfaceBlock;
    private final Block subSurfaceBlock;
    private final Block accentBlock;
    private final int noiseOctaves;
    private final double noiseFrequencyMultiplier;
    private final double amplitudeMultiplier;
    private final Block liquidBlock;
    private final int liquidLevelOffset;
    private final boolean hasRivers;

    /**
     * Gezegen isimlerinde kullanılacak sınıflandırma harfi.
     * Örnek:
     * -897546 e -> Earth-like
     * -897546 o -> Ocean
     * -897546 i -> Ice
     * -897546 f -> Lava / sıcak gezegen
     */
    public char getTypeSuffix() {
        return switch (this) {
            case ROCKY -> 'r';
            case ICE -> 'i';
            case LAVA -> 'f';
            case BARREN -> 'b';
            case CRYSTAL -> 'k';
            case GAS_GIANT -> 'g';
            case OCEAN -> 'o';
            case EARTH_LIKE -> 'e';
            case CHAOTIC -> 'c';
        };
    }


    private final char nameSuffix;

    PlanetType(
            int weight,
            Block surfaceBlock,
            Block subSurfaceBlock,
            Block accentBlock,
            int noiseOctaves,
            double noiseFrequencyMultiplier,
            double amplitudeMultiplier,
            Block liquidBlock,
            int liquidLevelOffset,
            boolean hasRivers,
            char nameSuffix
    ) {
        this.weight = weight;
        this.surfaceBlock = surfaceBlock;
        this.subSurfaceBlock = subSurfaceBlock;
        this.accentBlock = accentBlock;
        this.noiseOctaves = noiseOctaves;
        this.noiseFrequencyMultiplier = noiseFrequencyMultiplier;
        this.amplitudeMultiplier = amplitudeMultiplier;
        this.liquidBlock = liquidBlock;
        this.liquidLevelOffset = liquidLevelOffset;
        this.hasRivers = hasRivers;
        this.nameSuffix = nameSuffix;
    }

    public Block getSurfaceBlock() {
        return surfaceBlock;
    }

    public Block getSubSurfaceBlock() {
        return subSurfaceBlock;
    }

    public Block getAccentBlock() {
        return accentBlock;
    }

    public int getNoiseOctaves() {
        return noiseOctaves;
    }

    public double getNoiseFrequencyMultiplier() {
        return noiseFrequencyMultiplier;
    }

    public double getAmplitudeMultiplier() {
        return amplitudeMultiplier;
    }

    /**
     * null ise bu gezegende sıvı yok.
     */
    public Block getLiquidBlock() {
        return liquidBlock;
    }

    /**
     * Gezegenin baseHeight'ine göre sıvı seviyesi ofseti.
     */
    public int getLiquidLevelOffset() {
        return liquidLevelOffset;
    }

    public boolean hasRivers() {
        return hasRivers;
    }

    /**
     * Gezegen adı üretirken kullanılacak tip harfi.
     */
    public char getNameSuffix() {
        return nameSuffix;
    }

    /**
     * Verilen seed'e göre ağırlıklı rastgele bir gezegen tipi seçer.
     * Aynı seed her zaman aynı tipi üretir.
     */
    public static PlanetType selectForSeed(long seed) {
        Random random = new Random(seed);

        int totalWeight = 0;
        for (PlanetType type : values()) {
            totalWeight += type.weight;
        }

        int roll = random.nextInt(totalWeight);
        int cursor = 0;

        for (PlanetType type : values()) {
            cursor += type.weight;

            if (roll < cursor) {
                return type;
            }
        }

        return ROCKY;
    }
}
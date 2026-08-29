package com.gst.world.gen;

import com.gst.world.UniverseSeedManager;
import com.gst.world.planet.PlanetData;
import com.gst.world.planet.PlanetGridManager;
import com.gst.world.planet.PlanetType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class PlanetChunkGenerator extends ChunkGenerator {

    public static final Codec<PlanetChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(gen -> gen.biomeSource)
    ).apply(instance, instance.stable(PlanetChunkGenerator::new)));

    private static final double BASE_NOISE_CELL_SIZE = 32.0;

    private static final double RIVER_THRESHOLD = 0.035;
    private static final double RIVER_NOISE_CELL_SIZE = 96.0;
    private static final int RIVER_DEPTH = 4;

    private static final int RIVER_BANK_OFFSET = 1;

    private static final double ZONE_NOISE_CELL_SIZE = 192.0;

    private static final double ZONE_HEIGHT_SCALE = 45.0;

    private static final double MAX_SLOPE = 1.0;

    private static final int SLOPE_CHECK_DISTANCE = 4;

    private static final double LAKE_CELL_SIZE = 96.0;

    private static final double RIM_FEATHER_FRACTION = 0.85;

    private static final double MIN_LAKE_FEATHER_BLOCKS = 12.0;

    private static final double LAKE_FEATHER_RELIEF_MULTIPLIER = 2.0;

    private static final int LAKE_RIM_BANK_OFFSET = 1;

    private static final int LAKE_WOBBLE_HARMONICS = 4;

    private static final double LAKE_WOBBLE_AMPLITUDE = 0.35;

    private static final double OCEAN_BLEND_MIN_MASK = 0.12;

    private static final int OCEAN_SEA_LEVEL = 64;

    private static final int OCEAN_BEACH_OFFSET = 1;

    private static final int OCEAN_SHORE_MIN_DEPTH = 2;

    private static final int OCEAN_DEEP_MAX_DEPTH = 24;

    private static final double LAKE_OCEAN_MIN_DISTANCE = 150.0;

    public PlanetChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public void carve(
            ChunkRegion chunkRegion,
            long seed,
            NoiseConfig noiseConfig,
            BiomeAccess biomeAccess,
            StructureAccessor structureAccessor,
            Chunk chunk,
            GenerationStep.Carver generationStep
    ) {
        // Mağara/oyuk oyma yok.
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        // Yüzey blokları populateNoise'da direkt yerleştiriliyor.
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        // Mob spawn mantığına dokunmuyoruz.
    }

    @Override
    public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
        ChunkPos pos = chunk.getPos();
        long universeSeed = UniverseSeedManager.getUniverseSeed();

        int centerX = pos.getStartX() + 8;
        int centerZ = pos.getStartZ() + 8;

        PlanetData data = PlanetGridManager.getPlanetAt(universeSeed, centerX, centerZ);

        if (data.type() == PlanetType.EARTH_LIKE) {
            super.generateFeatures(world, chunk, structureAccessor);
        }
    }

    @Override
    public int getMinimumY() {
        return -64;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getWorldHeight() {
        return 384;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(
            Executor executor,
            Blender blender,
            NoiseConfig noiseConfig,
            StructureAccessor structureAccessor,
            Chunk chunk
    ) {
        ChunkPos chunkPos = chunk.getPos();
        int bottomY = chunk.getBottomY();

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int blockX = chunkPos.getStartX() + localX;
                int blockZ = chunkPos.getStartZ() + localZ;

                ColumnProfile profile = resolveColumnSmoothed(blockX, blockZ);

                for (int y = bottomY; y < profile.surfaceY() && y < chunk.getTopY(); y++) {
                    BlockState state;

                    if (y == bottomY) {
                        state = Blocks.BEDROCK.getDefaultState();
                    } else if (y >= profile.surfaceY() - 1) {
                        state = profile.paletteType().getSurfaceBlock().getDefaultState();
                    } else {
                        state = profile.paletteType().getSubSurfaceBlock().getDefaultState();
                    }

                    mutable.set(localX, y, localZ);
                    chunk.setBlockState(mutable, state, false);
                }

                Block liquidBlock = profile.liquidBlock();

                if (liquidBlock != null && profile.liquidTop() >= profile.surfaceY()) {
                    BlockState liquidState = liquidBlock.getDefaultState();

                    int fillStart = Math.max(profile.surfaceY(), bottomY + 1);

                    for (int y = fillStart; y <= profile.liquidTop() && y < chunk.getTopY(); y++) {
                        mutable.set(localX, y, localZ);
                        chunk.setBlockState(mutable, liquidState, false);
                        chunk.markBlockForPostProcessing(mutable);
                    }
                }
            }
        }

        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        ColumnProfile profile = resolveColumnSmoothed(x, z);
        return Math.max(profile.surfaceY(), profile.liquidTop() + 1);
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        ColumnProfile profile = resolveColumnSmoothed(x, z);
        BlockState[] states = new BlockState[world.getHeight()];

        Block liquidBlock = profile.liquidBlock();

        for (int i = 0; i < states.length; i++) {
            int y = world.getBottomY() + i;

            if (y == world.getBottomY()) {
                states[i] = Blocks.BEDROCK.getDefaultState();
            } else if (y < profile.surfaceY() - 1) {
                states[i] = profile.paletteType().getSubSurfaceBlock().getDefaultState();
            } else if (y < profile.surfaceY()) {
                states[i] = profile.paletteType().getSurfaceBlock().getDefaultState();
            } else if (liquidBlock != null && y <= profile.liquidTop()) {
                states[i] = liquidBlock.getDefaultState();
            } else {
                states[i] = Blocks.AIR.getDefaultState();
            }
        }

        return new VerticalBlockSample(world.getBottomY(), states);
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        ColumnProfile profile = resolveColumn(pos.getX(), pos.getZ());
        text.add("GST Planet cell paleti: " + profile.paletteType());
    }

    // ------------------------------------------------------------------
    // Sütun çözümleme
    // ------------------------------------------------------------------

    private record ColumnProfile(int surfaceY, int liquidTop, PlanetType paletteType, Block liquidBlock) {
    }

    private record RawProfile(int surfaceY) {
    }

    private ColumnProfile resolveColumn(int blockX, int blockZ) {
        long universeSeed = UniverseSeedManager.getUniverseSeed();
        int cellSize = PlanetGridManager.CELL_SIZE;

        double gx = (blockX - cellSize / 2.0) / (double) cellSize;
        double gz = (blockZ - cellSize / 2.0) / (double) cellSize;

        int cx0 = (int) Math.floor(gx);
        int cz0 = (int) Math.floor(gz);
        int cx1 = cx0 + 1;
        int cz1 = cz0 + 1;

        double fx = smoothstep(gx - cx0);
        double fz = smoothstep(gz - cz0);

        PlanetData d00 = PlanetGridManager.getPlanetAtCell(universeSeed, cx0, cz0);
        PlanetData d10 = PlanetGridManager.getPlanetAtCell(universeSeed, cx1, cz0);
        PlanetData d01 = PlanetGridManager.getPlanetAtCell(universeSeed, cx0, cz1);
        PlanetData d11 = PlanetGridManager.getPlanetAtCell(universeSeed, cx1, cz1);

        // 1. DOĞAL ARAZİ YÜKSEKLİĞİ (Ham, dokunulmamış)
        int surfaceY = rawBlendedHeight(blockX, blockZ);

        // 2. OKYANUS ŞEKİLLENDİRMESİ (SÜREKLİ, EŞİKSİZ & STATELESS)
        // actualData.type() kontrolü YOKTUR. Sadece sürekli (continuous) oceanMask kullanılır.
        // Bu sayede hücre sınırlarında (512'nin katları) bıçak kesiği matematiksel olarak OLAMAZ.
        double oceanMask = blendedOceanMask(blockX, blockZ);
        int seaLevel = OCEAN_SEA_LEVEL;
        int deepFloor = seaLevel - OCEAN_DEEP_MAX_DEPTH;

        // A) PLAJ / KIYI YÜKSELTmesi (Sürekli Türevli)
        // beachT: oceanMask 0.0 iken 0, 0.15 iken 1, 0.30 iken 0.
        // smootherstep ile türevi de sürekli, ani kırılma/dik duvar yok.
        double beachT = 1.0 - Math.abs(oceanMask - 0.15) / 0.15;
        beachT = Math.max(0.0, Math.min(1.0, beachT));
        beachT = smootherstep(beachT);

        // B) OKYANUS TABANI OYMASI (Sürekli Türevli)
        // deepT: oceanMask 0.30 iken 0, 1.0 iken 1.
        // Plaj biterken okyanus tabanı oyması başlar, arada boşluk/kesik yok.
        double deepT = (oceanMask - 0.30) / 0.70;
        deepT = Math.max(0.0, Math.min(1.0, deepT));
        deepT = smootherstep(deepT);

        // ÖNCE BEACH RAISE (sadece yüzey beachTarget'tan düşükse yukarı çek)
        int beachTarget = seaLevel + OCEAN_BEACH_OFFSET;
        if (surfaceY < beachTarget && beachT > 0.0) {
            surfaceY = (int) Math.round(lerp(surfaceY, beachTarget, beachT));
        }

        // SONRA DEEP CARVE (eşiksiz lerp, her zaman çalışır, ani duvar yaratmaz)
        surfaceY = (int) Math.round(lerp(surfaceY, deepFloor, deepT));

        // 3. SIVI DOLDURMA KARARI
        int actualCellX = PlanetGridManager.blockToCell(blockX);
        int actualCellZ = PlanetGridManager.blockToCell(blockZ);
        PlanetData actualData = PlanetGridManager.getPlanetAtCell(universeSeed, actualCellX, actualCellZ);

        int liquidTop = Integer.MIN_VALUE;
        Block liquidBlock = null;

        // A) OKYANUS SUYU
        // Eğer okyanus etkisi yeterince güçlüyse ve zemin deniz seviyesinin altındaysa su koy.
        if (oceanMask >= OCEAN_BLEND_MIN_MASK) {
            if (surfaceY < OCEAN_SEA_LEVEL) {
                liquidTop = OCEAN_SEA_LEVEL;
                liquidBlock = PlanetType.OCEAN.getLiquidBlock();
            }
        }

        // B) GÖLLER VE NEHİRLER (Sadece okyanus bu sütunu doldurmadıysa)
        if (liquidBlock == null && actualData.type().getLiquidBlock() != null && actualData.type() != PlanetType.OCEAN) {
            LakeBasin basin = findStrongestLakeBasin(actualData, blockX, blockZ);
            double riverMask = riverMaskAt(actualData, blockX, blockZ);

            if (basin != null && (basin.basinMask() > 0.0 || basin.rimMask() > 0.0)) {
                int waterLevel = basin.waterLevel();
                int rimTarget = waterLevel + LAKE_RIM_BANK_OFFSET;

                if (basin.basinMask() > 0.0) {
                    int depth = (int) Math.round(basinDepthFor(actualData.type()));
                    int bedHeight = waterLevel - depth;
                    int rimHeight = Math.max(surfaceY, rimTarget);

                    int newSurfaceY = (int) Math.round(lerp(rimHeight, bedHeight, basin.basinMask()));
                    surfaceY = newSurfaceY;

                    if (newSurfaceY < waterLevel) {
                        liquidTop = waterLevel;
                        liquidBlock = actualData.type().getLiquidBlock();
                    }
                } else if (basin.rimMask() > 0.0) {
                    int rimHeight = Math.max(surfaceY, rimTarget);
                    surfaceY = (int) Math.round(lerp(surfaceY, rimHeight, basin.rimMask()));
                }
            } else if (riverMask > 0.0 && isInValley(actualData, blockX, blockZ)) {
                int riverWaterLevel = surfaceY - RIVER_BANK_OFFSET;
                int riverBed = riverWaterLevel - (int) Math.round(riverMask * RIVER_DEPTH);

                surfaceY = riverBed;
                liquidTop = riverWaterLevel;
                liquidBlock = actualData.type().getLiquidBlock();
            }
        }

        // 4. PALET (Blok Tipleri)
        double w00 = (1 - fx) * (1 - fz);
        double w10 = fx * (1 - fz);
        double w01 = (1 - fx) * fz;
        double w11 = fx * fz;

        PlanetType paletteType = pickWeightedPalette(
                blockX, blockZ,
                d00.type(), w00,
                d10.type(), w10,
                d01.type(), w01,
                d11.type(), w11
        );

        return new ColumnProfile(surfaceY, liquidTop, paletteType, liquidBlock);
    }

    /**
     * Su/lav içindeki izole hava ceplerini düzeltir.
     */
    private ColumnProfile resolveColumnSmoothed(int blockX, int blockZ) {
        ColumnProfile center = resolveColumn(blockX, blockZ);

        if (center.liquidBlock() != null) {
            return center;
        }

        int[][] offsets = {
                {3, 0},
                {-3, 0},
                {0, 3},
                {0, -3}
        };

        int liquidNeighbors = 0;
        int liquidTopSum = 0;
        Block neighborLiquidBlock = null;

        for (int[] off : offsets) {
            ColumnProfile neighbor = resolveColumn(blockX + off[0], blockZ + off[1]);

            if (neighbor.liquidBlock() != null && neighbor.liquidTop() >= center.surfaceY()) {
                liquidNeighbors++;
                liquidTopSum += neighbor.liquidTop();
                neighborLiquidBlock = neighbor.liquidBlock();
            }
        }

        if (liquidNeighbors >= 4) {
            int averagedLiquidTop = liquidTopSum / liquidNeighbors;

            return new ColumnProfile(
                    center.surfaceY(),
                    averagedLiquidTop,
                    center.paletteType(),
                    neighborLiquidBlock
            );
        }

        return center;
    }

    private PlanetType pickWeightedPalette(
            int blockX,
            int blockZ,
            PlanetType t00, double w00,
            PlanetType t10, double w10,
            PlanetType t01, double w01,
            PlanetType t11, double w11
    ) {
        long h = PlanetGridManager.computeCellSeed(0xD17E5DL, blockX, blockZ);
        double r = ((h >>> 40) % 10000L) / 10000.0;

        double c1 = w00;
        double c2 = c1 + w10;
        double c3 = c2 + w01;

        if (r < c1) return t00;
        if (r < c2) return t10;
        if (r < c3) return t01;
        return t11;
    }

    // ------------------------------------------------------------------
    // Bölge / raw height
    // ------------------------------------------------------------------

    private double zoneNoiseAt(PlanetData data, int blockX, int blockZ) {
        return simpleNoise2D(data.seed() ^ 0x5A0E5A0EL, blockX, blockZ, ZONE_NOISE_CELL_SIZE);
    }

    private int rawBlendedHeight(int blockX, int blockZ) {
        long universeSeed = UniverseSeedManager.getUniverseSeed();
        int cellSize = PlanetGridManager.CELL_SIZE;

        double gx = (blockX - cellSize / 2.0) / (double) cellSize;
        double gz = (blockZ - cellSize / 2.0) / (double) cellSize;

        int cx0 = (int) Math.floor(gx);
        int cz0 = (int) Math.floor(gz);
        int cx1 = cx0 + 1;
        int cz1 = cz0 + 1;

        double fx = smoothstep(gx - cx0);
        double fz = smoothstep(gz - cz0);

        PlanetData d00 = PlanetGridManager.getPlanetAtCell(universeSeed, cx0, cz0);
        PlanetData d10 = PlanetGridManager.getPlanetAtCell(universeSeed, cx1, cz0);
        PlanetData d01 = PlanetGridManager.getPlanetAtCell(universeSeed, cx0, cz1);
        PlanetData d11 = PlanetGridManager.getPlanetAtCell(universeSeed, cx1, cz1);

        int h00 = computeRawProfile(d00, blockX, blockZ).surfaceY();
        int h10 = computeRawProfile(d10, blockX, blockZ).surfaceY();
        int h01 = computeRawProfile(d01, blockX, blockZ).surfaceY();
        int h11 = computeRawProfile(d11, blockX, blockZ).surfaceY();

        double top = lerp(h00, h10, fx);
        double bottom = lerp(h01, h11, fx);

        return (int) Math.round(lerp(top, bottom, fz));
    }

    // ------------------------------------------------------------------
    // Göller
    // ------------------------------------------------------------------

    private record LakeBasin(double basinMask, double rimMask, int waterLevel) {
    }

    private record LakeCellInfo(
            boolean isLake,
            double centerX,
            double centerZ,
            double radius,
            int waterLevel,
            double feather,
            double[] wobblePhases,
            double[] wobbleWeights
    ) {
        static final LakeCellInfo NONE = new LakeCellInfo(
                false,
                0,
                0,
                0,
                0,
                0.0,
                null,
                null
        );
    }

    private LakeBasin findStrongestLakeBasin(PlanetData data, int blockX, int blockZ) {
        int lcx = (int) Math.floor(blockX / LAKE_CELL_SIZE);
        int lcz = (int) Math.floor(blockZ / LAKE_CELL_SIZE);

        double bestBasinMask = 0.0;
        double bestRimMask = 0.0;
        int bestWaterLevel = Integer.MIN_VALUE;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                LakeCellInfo info = getLakeCellInfo(data, lcx + dx, lcz + dz);

                if (!info.isLake()) {
                    continue;
                }

                double relX = blockX - info.centerX();
                double relZ = blockZ - info.centerZ();

                double distance = Math.hypot(relX, relZ);
                double angle = Math.atan2(relZ, relX);

                double wobble = lakeEdgeWobble(angle, info.wobblePhases(), info.wobbleWeights());
                double effectiveRadius = info.radius() * (1.0 + LAKE_WOBBLE_AMPLITUDE * wobble);

                double feather = Math.max(info.feather(), effectiveRadius * RIM_FEATHER_FRACTION);
                double rimRadius = effectiveRadius + feather;

                if (distance >= rimRadius) {
                    continue;
                }

                if (distance < effectiveRadius) {
                    // Çanak bölgesi
                    double t = 1.0 - (distance / effectiveRadius);
                    double mask = smootherstep(t);

                    if (mask > bestBasinMask) {
                        bestBasinMask = mask;
                        bestRimMask = 0.0;
                        bestWaterLevel = info.waterLevel();
                    }
                } else {
                    // Feather bölgesi
                    double t = 1.0 - ((distance - effectiveRadius) / feather);
                    double mask = smootherstep(t);

                    if (mask > bestRimMask) {
                        bestRimMask = mask;
                        bestBasinMask = 0.0;
                        bestWaterLevel = info.waterLevel();
                    }
                }
            }
        }

        if (bestBasinMask > 0.0 || bestRimMask > 0.0) {
            return new LakeBasin(bestBasinMask, bestRimMask, bestWaterLevel);
        }

        return null;
    }

    private LakeCellInfo getLakeCellInfo(PlanetData data, int lakeCellX, int lakeCellZ) {
        if (data.type().getLiquidBlock() == null) {
            return LakeCellInfo.NONE;
        }

        long cellSeed = PlanetGridManager.computeCellSeed(data.seed() ^ 0x1A4E1A4EL, lakeCellX, lakeCellZ);
        java.util.Random random = new java.util.Random(cellSeed);

        if (random.nextDouble() >= lakeProbabilityFor(data.type())) {
            return LakeCellInfo.NONE;
        }

        // Yan yana göl yasağı.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                long neighborSeed = PlanetGridManager.computeCellSeed(
                        data.seed() ^ 0x1A4E1A4EL,
                        lakeCellX + dx,
                        lakeCellZ + dz
                );

                if (neighborSeed < cellSeed) {
                    java.util.Random nr = new java.util.Random(neighborSeed);

                    if (nr.nextDouble() < lakeProbabilityFor(data.type())) {
                        return LakeCellInfo.NONE;
                    }
                }
            }
        }

        double margin = LAKE_CELL_SIZE * 0.3;

        double centerX = lakeCellX * LAKE_CELL_SIZE + margin
                + random.nextDouble() * (LAKE_CELL_SIZE - margin * 2);

        double centerZ = lakeCellZ * LAKE_CELL_SIZE + margin
                + random.nextDouble() * (LAKE_CELL_SIZE - margin * 2);

        double radius = LAKE_CELL_SIZE * lakeRadiusFractionFor(data.type())
                * (0.7 + random.nextDouble() * 0.6);

        // ------------------------------------------------------------
        // OKYANUS YAKINLIK KONTROLÜ (Performans için erken çıkış)
        // ------------------------------------------------------------
        // Göl, okyanus suyundan LAKE_OCEAN_MIN_DISTANCE (150) bloktan daha yakınsa oluşmasın.
        // Böylece göller denizin içine girip kıyıyı bozmaz.
        boolean tooCloseToOcean = false;

        // Merkez noktasını kontrol et
        double centerMask = blendedOceanMask((int) Math.round(centerX), (int) Math.round(centerZ));
        if (centerMask >= OCEAN_BLEND_MIN_MASK) {
            tooCloseToOcean = true;
        }

        // Çevresindeki 8 noktayı kontrol et (150 blok yarıçapında)
        if (!tooCloseToOcean) {
            int proximitySamples = 8;
            for (int i = 0; i < proximitySamples; i++) {
                double angle = (2.0 * Math.PI * i) / proximitySamples;
                int checkX = (int) Math.round(centerX + Math.cos(angle) * LAKE_OCEAN_MIN_DISTANCE);
                int checkZ = (int) Math.round(centerZ + Math.sin(angle) * LAKE_OCEAN_MIN_DISTANCE);

                double checkMask = blendedOceanMask(checkX, checkZ);
                if (checkMask >= OCEAN_BLEND_MIN_MASK) {
                    tooCloseToOcean = true;
                    break;
                }
            }
        }

        if (tooCloseToOcean) {
            return LakeCellInfo.NONE;
        }
        // ------------------------------------------------------------

        double[] wobblePhases = new double[LAKE_WOBBLE_HARMONICS];
        double[] wobbleWeights = new double[LAKE_WOBBLE_HARMONICS];

        double weightSum = 0.0;

        for (int i = 0; i < LAKE_WOBBLE_HARMONICS; i++) {
            wobblePhases[i] = random.nextDouble() * Math.PI * 2.0;
            wobbleWeights[i] = 1.0 / (i + 1);
            weightSum += wobbleWeights[i];
        }

        for (int i = 0; i < LAKE_WOBBLE_HARMONICS; i++) {
            wobbleWeights[i] /= weightSum;
        }

        int sampleCount = 8;
        int[] edgeHeights = new int[sampleCount];

        for (int i = 0; i < sampleCount; i++) {
            double angle = (2.0 * Math.PI * i) / sampleCount;

            double wobble = lakeEdgeWobble(angle, wobblePhases, wobbleWeights);
            double sampleRadius = radius * (1.0 + LAKE_WOBBLE_AMPLITUDE * wobble) * 0.95;

            int sx = (int) Math.round(centerX + Math.cos(angle) * sampleRadius);
            int sz = (int) Math.round(centerZ + Math.sin(angle) * sampleRadius);

            edgeHeights[i] = computeHeight(data, sx, sz);
        }

        java.util.Arrays.sort(edgeHeights);

        int waterLevel = edgeHeights[sampleCount / 2];

        int centerHeight = computeHeight(
                data,
                (int) Math.round(centerX),
                (int) Math.round(centerZ)
        );

        if (centerHeight >= waterLevel) {
            return LakeCellInfo.NONE;
        }

        // Feather mesafesini yükseklik farkına göre uzat.
        int minEdge = edgeHeights[0];
        int maxEdge = edgeHeights[sampleCount - 1];

        int relief = Math.max(Math.abs(maxEdge - waterLevel), Math.abs(waterLevel - minEdge));
        relief = Math.max(relief, Math.abs(centerHeight - waterLevel));
        relief = Math.max(relief, LAKE_RIM_BANK_OFFSET);

        double maxExpectedRadius = radius * (1.0 + LAKE_WOBBLE_AMPLITUDE);
        double baseFeather = maxExpectedRadius * RIM_FEATHER_FRACTION;
        double reliefFeather = relief * LAKE_FEATHER_RELIEF_MULTIPLIER + 6.0;

        double feather = Math.max(Math.max(baseFeather, MIN_LAKE_FEATHER_BLOCKS), reliefFeather);

        return new LakeCellInfo(
                true,
                centerX,
                centerZ,
                radius,
                waterLevel,
                feather,
                wobblePhases,
                wobbleWeights
        );
    }

    private double lakeEdgeWobble(double angle, double[] phases, double[] weights) {
        double sum = 0.0;

        for (int i = 0; i < phases.length; i++) {
            sum += weights[i] * Math.sin((i + 1) * angle + phases[i]);
        }

        return sum;
    }

    private double lakeProbabilityFor(PlanetType type) {
        return switch (type) {
            case OCEAN -> 0.5;
            case EARTH_LIKE -> 0.15;
            case LAVA -> 0.12;
            case ICE -> 0.12;
            default -> 0.0;
        };
    }

    private double lakeRadiusFractionFor(PlanetType type) {
        return switch (type) {
            case OCEAN -> 0.55;
            case EARTH_LIKE -> 0.32;
            case LAVA -> 0.28;
            case ICE -> 0.30;
            default -> 0.0;
        };
    }

    private double basinDepthFor(PlanetType type) {
        return switch (type) {
            case OCEAN -> 12.0;
            case EARTH_LIKE -> 6.0;
            case LAVA -> 8.0;
            case ICE -> 4.0;
            default -> 0.0;
        };
    }

    private boolean isInValley(PlanetData data, int blockX, int blockZ) {
        int centerHeight = computeHeight(data, blockX, blockZ);

        int[][] offsets = {
                {4, 0},
                {-4, 0},
                {0, 4},
                {0, -4}
        };

        int higherNeighbors = 0;

        for (int[] off : offsets) {
            int neighborHeight = computeHeight(data, blockX + off[0], blockZ + off[1]);

            if (neighborHeight > centerHeight) {
                higherNeighbors++;
            }
        }

        return higherNeighbors >= 2;
    }

    private RawProfile computeRawProfile(PlanetData data, int blockX, int blockZ) {
        return new RawProfile(computeHeight(data, blockX, blockZ));
    }

    // ------------------------------------------------------------------
    // Ocean
    // ------------------------------------------------------------------

    private int oceanLevelFor(PlanetData data) {
        return OCEAN_SEA_LEVEL;
    }

    private int oceanLevelPotential(PlanetData data) {
        if (data.type() == PlanetType.OCEAN) {
            return oceanLevelFor(data);
        }

        return data.baseHeight();
    }

    private double blendedOceanMask(int blockX, int blockZ) {
        long universeSeed = UniverseSeedManager.getUniverseSeed();
        int cellSize = PlanetGridManager.CELL_SIZE;

        double gx = (blockX - cellSize / 2.0) / (double) cellSize;
        double gz = (blockZ - cellSize / 2.0) / (double) cellSize;

        int cx0 = (int) Math.floor(gx);
        int cz0 = (int) Math.floor(gz);
        int cx1 = cx0 + 1;
        int cz1 = cz0 + 1;

        double fx = smoothstep(gx - cx0);
        double fz = smoothstep(gz - cz0);

        PlanetData d00 = PlanetGridManager.getPlanetAtCell(universeSeed, cx0, cz0);
        PlanetData d10 = PlanetGridManager.getPlanetAtCell(universeSeed, cx1, cz0);
        PlanetData d01 = PlanetGridManager.getPlanetAtCell(universeSeed, cx0, cz1);
        PlanetData d11 = PlanetGridManager.getPlanetAtCell(universeSeed, cx1, cz1);

        double m00 = d00.type() == PlanetType.OCEAN ? 1.0 : 0.0;
        double m10 = d10.type() == PlanetType.OCEAN ? 1.0 : 0.0;
        double m01 = d01.type() == PlanetType.OCEAN ? 1.0 : 0.0;
        double m11 = d11.type() == PlanetType.OCEAN ? 1.0 : 0.0;

        double top = lerp(m00, m10, fx);
        double bottom = lerp(m01, m11, fx);

        return lerp(top, bottom, fz);
    }

    // ------------------------------------------------------------------
    // Yükseklik / noise
    // ------------------------------------------------------------------

    private int computeHeight(PlanetData data, int blockX, int blockZ) {
        PlanetType type = data.type();

        double sum = 0.0;
        double maxAmplitude = 0.0;
        double amplitude = 1.0;
        double cellSize = BASE_NOISE_CELL_SIZE;

        int octaves = Math.max(1, type.getNoiseOctaves());

        for (int i = 0; i < octaves; i++) {
            sum += simpleNoise2D(data.seed() + i * 7919L, blockX, blockZ, cellSize) * amplitude;
            maxAmplitude += amplitude;

            amplitude *= 0.5;
            cellSize /= (1.5 * Math.max(0.1, type.getNoiseFrequencyMultiplier()));

            if (cellSize < 12.0) {
                cellSize = 12.0;
            }
        }

        double normalized = maxAmplitude > 0.0 ? (sum / maxAmplitude) : 0.0;

        double zoneNoise = zoneNoiseAt(data, blockX, blockZ);

        double heightOffset = zoneNoise * ZONE_HEIGHT_SCALE;
        double amplitudeScale = 1.0 + Math.abs(zoneNoise) * 1.2;

        int offset = (int) Math.round(
                normalized * data.heightVariance() * type.getAmplitudeMultiplier() * amplitudeScale
        );

        return data.baseHeight() + offset + (int) Math.round(heightOffset);
    }

    // ------------------------------------------------------------------
    // Nehir
    // ------------------------------------------------------------------

    private double riverMaskAt(PlanetData data, int blockX, int blockZ) {
        if (!data.type().hasRivers()) {
            return 0.0;
        }

        double riverNoise = simpleNoise2D(data.seed() ^ 0x52495645L, blockX, blockZ, RIVER_NOISE_CELL_SIZE);
        double abs = Math.abs(riverNoise);

        if (abs >= RIVER_THRESHOLD) {
            return 0.0;
        }

        double t = 1.0 - (abs / RIVER_THRESHOLD);
        return smoothstep(t);
    }

    // ------------------------------------------------------------------
    // Yardımcı noise / interpolasyon
    // ------------------------------------------------------------------

    private double simpleNoise2D(long seed, int worldX, int worldZ, double cellSize) {
        double gx = worldX / cellSize;
        double gz = worldZ / cellSize;

        int cellX0 = (int) Math.floor(gx);
        int cellZ0 = (int) Math.floor(gz);
        int cellX1 = cellX0 + 1;
        int cellZ1 = cellZ0 + 1;

        double fx = gx - cellX0;
        double fz = gz - cellZ0;

        double v00 = hashToUnit(seed, cellX0, cellZ0);
        double v10 = hashToUnit(seed, cellX1, cellZ0);
        double v01 = hashToUnit(seed, cellX0, cellZ1);
        double v11 = hashToUnit(seed, cellX1, cellZ1);

        double i1 = lerp(v00, v10, fx);
        double i2 = lerp(v01, v11, fx);

        return lerp(i1, i2, fz);
    }

    private double hashToUnit(long seed, int x, int z) {
        long h = PlanetGridManager.computeCellSeed(seed, x, z);
        return ((h >>> 40) % 2000L - 1000L) / 1000.0;
    }

    private double smoothstep(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3.0 - 2.0 * t);
    }

    private double smootherstep(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
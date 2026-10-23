package com.eldenring.spells.worldgen;

import com.eldenring.spells.registry.ModBiomes;
import com.eldenring.spells.worldgen.biome.MagicLakeSurfaceRules;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * 魔法之湖浅湖整平：把群系内的原始地形「压扁」成大片 1–2 格浅水 + 贴着水面的低矮小岛。
 * <p>
 * TerraBlender 只决定群系放在哪，不控制地形高度；魔法之湖是内陆湿度团块里的平地和丘陵（山地 / 高原除外，
 * 见 {@code MagicLakeRegion}），原地形有平原、河谷也有丘陵，所以在群系 JSON 的 {@code lakes} 步骤
 * （地表规则之后、本区块植被之前）逐列修整：
 * <ul>
 *   <li><b>高度压缩</b>：每列「高出水面的高度」先减去下沉量（8–14 格），再乘以保留比例 0.15。
 *       结果 &lt; 0 的变成浅水，≥ 0 的成为小岛。平原、森林全部淹没，只有较高的丘陵剩下 2–3 格高的矮岛，
 *       岛的边缘正好落在水面高度，不会出现「岛比水面高好几格」的断崖。陆地刻意压到最少，湖面优先</li>
 *   <li><b>边界渐变</b>：在本区块周围 ±24 格按 8 格网格采样群系，算出每列「深入湖区的程度」，
 *       湖心完全压平，靠近相邻群系逐渐过渡回原地形，形成约 24 格长的湖岸坡</li>
 *   <li><b>水太深</b>：用石头垫高，水深 1 格为主、2 格为辅。湖底材质取
 *       {@code MagicLakeSurfaceRules#lakebedStateAt}（原版河床风格），和地表规则铺的天然湖底同一套分布，不出接缝</li>
 * </ul>
 * 相邻区块的植被可能先于本区块整平就长到本区块的陆地上（原版先装饰完邻居再轮到这里），
 * 所以压低 / 淹水时会一并清掉贴地的草、甘蔗等，并在找地面时跳过邻居树冠伸过来的树叶。
 * <p>
 * 每区块执行一次，只写当前区块 16×16 列，只处理水面高度处群系为魔法之湖的列；每列只处理一次
 * （压低不是幂等操作，不能扩到邻居区块重复执行）。Region 把湖下方的溶洞 / 繁茂洞穴点也切成了魔法之湖，
 * 所以原地形较高、y=62 落在洞穴群系深度的列同样会被整平，湖面不会被切成长条。
 * 噪声尺度 / 阈值是地形外观参数，写死不进 toml（同种子同地形，避免改配置后新旧区块错位）。
 */
public final class MagicLakeShallowsFeature extends Feature<NoneFeatureConfiguration> {

    /** 湖底深度噪声盐（与世界种子异或），和其它噪声错开相位。 */
    private static final long FLOOR_DEPTH_NOISE_SALT = 0x4D41_4C41_4B45_0001L;
    /** 陆地下沉量噪声盐。 */
    private static final long SINK_NOISE_SALT = 0x4D41_4C41_4B45_0002L;

    /**
     * 湖底深度噪声的水平尺度（方块）。越大 → 1 格浅滩 / 2 格水域的斑块越大、越平缓。
     */
    private static final double FLOOR_DEPTH_NOISE_SCALE_BLOCKS = 28.0;
    /**
     * 湖底深度噪声高于此值 → 2 格深，否则 1 格。噪声范围 [-1, 1]、集中在 0 附近；
     * 0.35 时 2 格水约占三成。调大 → 2 格水更少。
     */
    private static final double DEEP_FLOOR_NOISE_THRESHOLD = 0.35;
    /** 最深水深（方块）。需求硬上限 2，不要调大。 */
    private static final int MAX_WATER_DEPTH_BLOCKS = 2;

    /**
     * 陆地下沉量噪声的水平尺度（方块）。越大 → 下沉量变化越缓，湖面和小岛成片越大。
     */
    private static final double SINK_NOISE_SCALE_BLOCKS = 64.0;
    /**
     * 陆地最少下沉几格（方块）。原地面高出水面 ≤ 此值的地方在湖心一定变成水；
     * 8 格覆盖了原版平原 / 森林的常见高度，所以它们全部变成湖面。
     */
    private static final int MIN_SINK_BLOCKS = 8;
    /**
     * 陆地最多下沉几格（方块）。调大 → 陆地更少、湖面更开阔；调小 → 小岛更多更大。
     */
    private static final int MAX_SINK_BLOCKS = 14;
    /**
     * 下沉后剩余高度的保留比例（0–1）。0.15 = 高出水面 30 格的丘陵压成约 2–3 格高的矮岛。
     * 调小 → 小岛更矮更平（0 = 湖心没有岛，杉树也长不出来）；调大 → 保留更多原地形起伏。
     */
    private static final double ISLAND_HEIGHT_KEEP_RATIO = 0.15;

    /**
     * 边界渐变的群系采样网格间距（方块）。必须整除 8 和 {@link #CORE_FACTOR_RADIUS_BLOCKS}。
     */
    private static final int CORE_FACTOR_GRID_SPACING_BLOCKS = 8;
    /**
     * 边界渐变半径（方块）：统计每个网格点周围 ±此距离内落在魔法之湖的比例。
     * 越大 → 湖岸到相邻群系的过渡坡越长越缓，但岸边陆地越多、窄条湖区压得越不彻底；
     * 越小 → 湖面一直铺到群系边界附近，湖岸坡更陡。
     */
    private static final int CORE_FACTOR_RADIUS_BLOCKS = 24;
    /**
     * 湖区比例低于此值时完全不压低（0–1）。群系边界线上比例约 0.5，
     * 取 0.3 让边界处已被压低一截（不留台阶），窄条湖区也基本能压平。
     */
    private static final float CORE_FACTOR_START_FRACTION = 0.3f;
    /** 本区块 3×3 个网格节点（区块内偏移 0 / 8 / 16 格），供逐列双线性插值。 */
    private static final int CHUNK_GRID_NODES_PER_AXIS = 16 / CORE_FACTOR_GRID_SPACING_BLOCKS + 1;

    /** 清理贴地植物时最多往上看几格（方块）：高草丛 2 格、甘蔗最高 3 格，留 1 格余量。 */
    private static final int PLANT_CLEAR_MAX_HEIGHT_BLOCKS = 4;

    public MagicLakeShallowsFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkPos chunkPos = new ChunkPos(context.origin());
        long worldSeed = level.getSeed();
        // 每次新建噪声：只有几百个 int 的置换表，开销小，且天然线程安全（世界生成多线程）
        SimplexNoise floorDepthNoise = new SimplexNoise(RandomSource.create(worldSeed ^ FLOOR_DEPTH_NOISE_SALT));
        SimplexNoise sinkNoise = new SimplexNoise(RandomSource.create(worldSeed ^ SINK_NOISE_SALT));
        RandomState randomState = level.getLevel().getChunkSource().randomState();

        int waterSurfaceY = context.chunkGenerator().getSeaLevel() - 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        float[][] chunkCoreFactorNodes = null;
        boolean changedAnyColumn = false;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = chunkPos.getMinBlockX() + localX;
                int worldZ = chunkPos.getMinBlockZ() + localZ;

                if (!level.getBiome(cursor.set(worldX, waterSurfaceY, worldZ)).is(ModBiomes.MAGIC_LAKE)) {
                    continue;
                }

                int groundY = findGroundY(level, cursor, worldX, worldZ);
                if (groundY == Integer.MIN_VALUE) {
                    // 邻居的树干立在这一列：不动，免得把树悬空
                    continue;
                }

                int floorDepthBlocks = floorDepthNoise.getValue(
                        worldX / FLOOR_DEPTH_NOISE_SCALE_BLOCKS,
                        worldZ / FLOOR_DEPTH_NOISE_SCALE_BLOCKS
                ) > DEEP_FLOOR_NOISE_THRESHOLD ? MAX_WATER_DEPTH_BLOCKS : 1;
                int deepestLakebedY = waterSurfaceY - floorDepthBlocks;
                BlockState lakebedState = MagicLakeSurfaceRules.lakebedStateAt(randomState, worldX, worldZ);

                if (groundY >= waterSurfaceY) {
                    if (chunkCoreFactorNodes == null) {
                        chunkCoreFactorNodes = sampleChunkCoreFactorNodes(context, chunkPos, waterSurfaceY);
                    }
                    float lakeCoreFactor = interpolateCoreFactor(chunkCoreFactorNodes, localX, localZ);
                    int targetGroundY = computeTargetGroundY(sinkNoise, worldX, worldZ, groundY, waterSurfaceY, lakeCoreFactor);
                    if (targetGroundY >= waterSurfaceY) {
                        if (targetGroundY < groundY) {
                            lowerLand(level, cursor, worldX, worldZ, groundY, targetGroundY);
                            changedAnyColumn = true;
                        }
                    } else {
                        // 刚沉到水面下的地方取较浅的那个，岸边自然是 1 格浅滩
                        int lakebedY = Math.max(deepestLakebedY, targetGroundY);
                        floodLand(level, cursor, worldX, worldZ, groundY, lakebedY, waterSurfaceY, lakebedState);
                        changedAnyColumn = true;
                    }
                } else {
                    int lakebedY = Math.max(deepestLakebedY, groundY);
                    if (lakebedY > groundY) {
                        raiseLakebed(level, cursor, worldX, worldZ, groundY, lakebedY, lakebedState);
                        changedAnyColumn = true;
                    }
                    // 含水层水位偏低时可能是干坑，补满到统一水面
                    changedAnyColumn |= fillAirWithWater(level, cursor, worldX, worldZ, lakebedY + 1, waterSurfaceY);
                }
            }
        }
        return changedAnyColumn;
    }

    /**
     * 高出水面的一列压缩后的新地面高度：湖心按「(高度 - 下沉量) × 保留比例」压扁，
     * 再按 lakeCoreFactor 在原高度和压扁高度之间插值（边界处 0 = 原样，湖心 1 = 完全压扁）。
     */
    private static int computeTargetGroundY(
            SimplexNoise sinkNoise,
            int worldX,
            int worldZ,
            int groundY,
            int waterSurfaceY,
            float lakeCoreFactor
    ) {
        double sinkNoise01 = (sinkNoise.getValue(
                worldX / SINK_NOISE_SCALE_BLOCKS,
                worldZ / SINK_NOISE_SCALE_BLOCKS
        ) + 1.0) * 0.5;
        double sinkBlocks = MIN_SINK_BLOCKS + sinkNoise01 * (MAX_SINK_BLOCKS - MIN_SINK_BLOCKS);
        int heightAboveWaterBlocks = groundY - waterSurfaceY;
        // floor：剩余高度在 [0, 1) 的列正好贴着水面，(-1, 0) 的列变成 1 格浅滩
        int fullyCompressedGroundY = waterSurfaceY
                + Mth.floor((heightAboveWaterBlocks - sinkBlocks) * ISLAND_HEIGHT_KEEP_RATIO);
        return Math.round(Mth.lerp(lakeCoreFactor, (float) groundY, (float) fullyCompressedGroundY));
    }

    /**
     * 在本区块周围 ±{@link #CORE_FACTOR_RADIUS_BLOCKS} 格按网格直接向群系源采样（不依赖邻居区块是否已加载），
     * 算出本区块 3×3 个节点的「深入湖区程度」（0 = 群系边界外沿，1 = 湖心）。
     */
    private static float[][] sampleChunkCoreFactorNodes(
            FeaturePlaceContext<NoneFeatureConfiguration> context,
            ChunkPos chunkPos,
            int waterSurfaceY
    ) {
        BiomeSource biomeSource = context.chunkGenerator().getBiomeSource();
        Climate.Sampler climateSampler = context.level().getLevel().getChunkSource().randomState().sampler();
        int quartY = QuartPos.fromBlock(waterSurfaceY);
        int radiusInCells = CORE_FACTOR_RADIUS_BLOCKS / CORE_FACTOR_GRID_SPACING_BLOCKS;
        int samplesPerAxis = (16 + 2 * CORE_FACTOR_RADIUS_BLOCKS) / CORE_FACTOR_GRID_SPACING_BLOCKS + 1;
        int sampleOriginX = chunkPos.getMinBlockX() - CORE_FACTOR_RADIUS_BLOCKS;
        int sampleOriginZ = chunkPos.getMinBlockZ() - CORE_FACTOR_RADIUS_BLOCKS;

        boolean[][] isMagicLakeSample = new boolean[samplesPerAxis][samplesPerAxis];
        for (int sampleIndexX = 0; sampleIndexX < samplesPerAxis; sampleIndexX++) {
            for (int sampleIndexZ = 0; sampleIndexZ < samplesPerAxis; sampleIndexZ++) {
                int sampleBlockX = sampleOriginX + sampleIndexX * CORE_FACTOR_GRID_SPACING_BLOCKS;
                int sampleBlockZ = sampleOriginZ + sampleIndexZ * CORE_FACTOR_GRID_SPACING_BLOCKS;
                isMagicLakeSample[sampleIndexX][sampleIndexZ] = biomeSource.getNoiseBiome(
                        QuartPos.fromBlock(sampleBlockX),
                        quartY,
                        QuartPos.fromBlock(sampleBlockZ),
                        climateSampler
                ).is(ModBiomes.MAGIC_LAKE);
            }
        }

        int windowSampleCount = (2 * radiusInCells + 1) * (2 * radiusInCells + 1);
        float[][] coreFactorNodes = new float[CHUNK_GRID_NODES_PER_AXIS][CHUNK_GRID_NODES_PER_AXIS];
        for (int nodeX = 0; nodeX < CHUNK_GRID_NODES_PER_AXIS; nodeX++) {
            for (int nodeZ = 0; nodeZ < CHUNK_GRID_NODES_PER_AXIS; nodeZ++) {
                int centerIndexX = radiusInCells + nodeX;
                int centerIndexZ = radiusInCells + nodeZ;
                int magicLakeSampleCount = 0;
                for (int windowX = centerIndexX - radiusInCells; windowX <= centerIndexX + radiusInCells; windowX++) {
                    for (int windowZ = centerIndexZ - radiusInCells; windowZ <= centerIndexZ + radiusInCells; windowZ++) {
                        if (isMagicLakeSample[windowX][windowZ]) {
                            magicLakeSampleCount++;
                        }
                    }
                }
                float magicLakeFraction = magicLakeSampleCount / (float) windowSampleCount;
                float linearFactor = Mth.clamp(
                        (magicLakeFraction - CORE_FACTOR_START_FRACTION) / (1.0f - CORE_FACTOR_START_FRACTION),
                        0.0f,
                        1.0f
                );
                // smoothstep：坡顶坡底都平缓过渡，不出现折角
                coreFactorNodes[nodeX][nodeZ] = linearFactor * linearFactor * (3.0f - 2.0f * linearFactor);
            }
        }
        return coreFactorNodes;
    }

    private static float interpolateCoreFactor(float[][] coreFactorNodes, int localX, int localZ) {
        int cellX = localX / CORE_FACTOR_GRID_SPACING_BLOCKS;
        int cellZ = localZ / CORE_FACTOR_GRID_SPACING_BLOCKS;
        float fractionX = (localX % CORE_FACTOR_GRID_SPACING_BLOCKS) / (float) CORE_FACTOR_GRID_SPACING_BLOCKS;
        float fractionZ = (localZ % CORE_FACTOR_GRID_SPACING_BLOCKS) / (float) CORE_FACTOR_GRID_SPACING_BLOCKS;
        return (float) Mth.lerp2(
                fractionX,
                fractionZ,
                coreFactorNodes[cellX][cellZ],
                coreFactorNodes[cellX + 1][cellZ],
                coreFactorNodes[cellX][cellZ + 1],
                coreFactorNodes[cellX + 1][cellZ + 1]
        );
    }

    /**
     * 本列真实地面高度：从 OCEAN_FLOOR_WG（已跳过水和草类）往下，跳过邻居伸过来的树叶，
     * 以及树叶下面的空气 / 草 / 水，直到第一个实心方块。
     *
     * @return 地面方块 Y；若碰到树干（邻居的大云杉 2×2 树干可能跨进本区块）返回 {@link Integer#MIN_VALUE}
     */
    private static int findGroundY(WorldGenLevel level, BlockPos.MutableBlockPos cursor, int worldX, int worldZ) {
        int groundY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, worldX, worldZ) - 1;
        int minY = level.getMinBuildHeight();
        while (groundY > minY) {
            BlockState state = level.getBlockState(cursor.set(worldX, groundY, worldZ));
            if (state.is(BlockTags.LOGS)) {
                return Integer.MIN_VALUE;
            }
            if (state.blocksMotion() && !state.is(BlockTags.LEAVES)) {
                return groundY;
            }
            groundY--;
        }
        return groundY;
    }

    /**
     * 清掉地面上方贴地的植物（草、高草丛、甘蔗、花等不挡路的方块），遇到空气 / 实心方块 / 流体即停。
     * 双格植物从下往上逐格清，flag 不触发邻居更新，上半格不会掉落成物品。
     */
    private static void clearPlantsAbove(WorldGenLevel level, BlockPos.MutableBlockPos cursor, int worldX, int worldZ, int groundY) {
        BlockState airState = Blocks.AIR.defaultBlockState();
        for (int plantY = groundY + 1; plantY <= groundY + PLANT_CLEAR_MAX_HEIGHT_BLOCKS; plantY++) {
            BlockState state = level.getBlockState(cursor.set(worldX, plantY, worldZ));
            if (state.isAir() || state.blocksMotion() || !state.getFluidState().isEmpty()) {
                return;
            }
            level.setBlock(cursor, airState, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * 小岛：整列压低到 targetGroundY，原表层方块（草 / 石斑）挪到新高度，岛边缘因此正好贴着水面。
     */
    private static void lowerLand(
            WorldGenLevel level,
            BlockPos.MutableBlockPos cursor,
            int worldX,
            int worldZ,
            int groundY,
            int targetGroundY
    ) {
        BlockState surfaceState = level.getBlockState(cursor.set(worldX, groundY, worldZ));
        clearPlantsAbove(level, cursor, worldX, worldZ, groundY);
        BlockState airState = Blocks.AIR.defaultBlockState();
        for (int clearY = targetGroundY + 1; clearY <= groundY; clearY++) {
            level.setBlock(cursor.set(worldX, clearY, worldZ), airState, Block.UPDATE_CLIENTS);
        }
        level.setBlock(cursor.set(worldX, targetGroundY, worldZ), surfaceState, Block.UPDATE_CLIENTS);
    }

    /**
     * 沉到水面下的陆地：水面以上清空，湖底到水面之间灌水源，湖底铺湖底材质。
     */
    private static void floodLand(
            WorldGenLevel level,
            BlockPos.MutableBlockPos cursor,
            int worldX,
            int worldZ,
            int groundY,
            int lakebedY,
            int waterSurfaceY,
            BlockState lakebedState
    ) {
        clearPlantsAbove(level, cursor, worldX, worldZ, groundY);
        BlockState airState = Blocks.AIR.defaultBlockState();
        BlockState waterState = Blocks.WATER.defaultBlockState();
        for (int clearY = waterSurfaceY + 1; clearY <= groundY; clearY++) {
            level.setBlock(cursor.set(worldX, clearY, worldZ), airState, Block.UPDATE_CLIENTS);
        }
        for (int waterY = lakebedY + 1; waterY <= waterSurfaceY; waterY++) {
            level.setBlock(cursor.set(worldX, waterY, worldZ), waterState, Block.UPDATE_CLIENTS);
        }
        level.setBlock(cursor.set(worldX, lakebedY, worldZ), lakebedState, Block.UPDATE_CLIENTS);
    }

    /**
     * 水太深：从原湖底上一格垫石头到新湖底下一格，新湖底铺湖底材质。上方水体保持不动。
     */
    private static void raiseLakebed(
            WorldGenLevel level,
            BlockPos.MutableBlockPos cursor,
            int worldX,
            int worldZ,
            int groundY,
            int lakebedY,
            BlockState lakebedState
    ) {
        BlockState fillerState = Blocks.STONE.defaultBlockState();
        for (int fillY = groundY + 1; fillY < lakebedY; fillY++) {
            level.setBlock(cursor.set(worldX, fillY, worldZ), fillerState, Block.UPDATE_CLIENTS);
        }
        level.setBlock(cursor.set(worldX, lakebedY, worldZ), lakebedState, Block.UPDATE_CLIENTS);
    }

    /**
     * 把 [fromY, toY] 区间内的空气换成水源（已有水或其它方块不动）。
     *
     * @return 是否改动了至少一格
     */
    private static boolean fillAirWithWater(
            WorldGenLevel level,
            BlockPos.MutableBlockPos cursor,
            int worldX,
            int worldZ,
            int fromY,
            int toY
    ) {
        BlockState waterState = Blocks.WATER.defaultBlockState();
        boolean placedAnyWater = false;
        for (int waterY = fromY; waterY <= toY; waterY++) {
            cursor.set(worldX, waterY, worldZ);
            if (level.getBlockState(cursor).isAir()) {
                level.setBlock(cursor, waterState, Block.UPDATE_CLIENTS);
                placedAnyWater = true;
            }
        }
        return placedAnyWater;
    }
}

package com.eldenring.spells.worldgen;

import com.eldenring.spells.registry.ModBlocks;
import com.eldenring.spells.tuning.GlintstoneWorldTuning;
import com.eldenring.spells.world.GlintstoneColor;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 辉石矿洞 Feature：大尺度噪声决定「辉石地带 + 整洞同色」；
 * 小尺度 chunk 门控避免整片连通洞穴每个 chunk 都刷满。
 * 水晶簇优先长在已替换的辉石水晶块上，裸石面概率极低。
 * 轮廓混搭不在本 Feature 里抽：同一簇方块的 blockstate 已对放射簇 / 尖塔 / 扇形 / 密丛 / 双晶 / 细针加权随机。
 */
public final class GlintstoneCaveFeature extends Feature<NoneFeatureConfiguration> {
    public GlintstoneCaveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        long seed = level.getSeed() ^ GlintstoneWorldTuning.CAVE_NOISE_SALT;

        double regionCell = GlintstoneWorldTuning.CAVE_REGION_NOISE_CELL_SIZE_CHUNKS;
        double regionNoise = valueNoise2D(seed, chunkX / regionCell, chunkZ / regionCell);
        if (regionNoise < GlintstoneWorldTuning.CAVE_REGION_PRESENCE_THRESHOLD) {
            return false;
        }

        double chunkCell = GlintstoneWorldTuning.CAVE_CHUNK_NOISE_CELL_SIZE_CHUNKS;
        double chunkGateNoise = valueNoise2D(
                seed ^ GlintstoneWorldTuning.CAVE_CHUNK_GATE_SALT,
                chunkX / chunkCell,
                chunkZ / chunkCell
        );
        if (chunkGateNoise < GlintstoneWorldTuning.CAVE_CHUNK_DECORATE_THRESHOLD) {
            return false;
        }

        // 大尺度第二噪声 → 三档等宽映射，保证三色等概率且同一地带颜色稳定
        double colorNoise = valueNoise2D(seed + 0x9E3779B97F4A7C15L, chunkX / regionCell, chunkZ / regionCell);
        GlintstoneColor color = colorFromUniformNoise(colorNoise);
        ModBlocks.ColorSet colorSet = ModBlocks.BY_COLOR.get(color);
        Block crystalBlock = colorSet.crystalBlock.get();
        Block clusterBlock = colorSet.cluster.get();

        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int minY = Math.max(GlintstoneWorldTuning.CAVE_SCAN_MIN_Y, level.getMinBuildHeight());
        int maxY = Math.min(GlintstoneWorldTuning.CAVE_SCAN_MAX_Y, level.getMaxBuildHeight() - 1);

        boolean placedAny = false;
        int clustersPlaced = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();

        for (int y = minY; y <= maxY; y++) {
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    cursor.set(minX + dx, y, minZ + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (!isReplaceableStone(state)) {
                        continue;
                    }

                    Direction openFace = findOpenFace(level, cursor, neighbor);
                    if (openFace == null) {
                        continue;
                    }

                    // 先低概率把表面石头换成同色水晶块
                    if (random.nextFloat() < GlintstoneWorldTuning.CAVE_SURFACE_BLOCK_CHANCE) {
                        level.setBlock(cursor, crystalBlock.defaultBlockState(), Block.UPDATE_ALL);
                        placedAny = true;
                    }

                    BlockState surfaceState = level.getBlockState(cursor);
                    boolean onCrystalBlock = surfaceState.is(crystalBlock);
                    float clusterChance = onCrystalBlock
                            ? GlintstoneWorldTuning.CAVE_SURFACE_CLUSTER_ON_BLOCK_CHANCE
                            : GlintstoneWorldTuning.CAVE_SURFACE_CLUSTER_ON_STONE_CHANCE;

                    if (clustersPlaced >= GlintstoneWorldTuning.CAVE_MAX_CLUSTERS_PER_CHUNK) {
                        continue;
                    }
                    if (random.nextFloat() >= clusterChance) {
                        continue;
                    }

                    neighbor.setWithOffset(cursor, openFace);
                    BlockState airSideState = level.getBlockState(neighbor);
                    if (!airSideState.isAir() && !airSideState.is(Blocks.CAVE_AIR)) {
                        continue;
                    }
                    if (airSideState.is(Blocks.WATER)) {
                        continue;
                    }
                    if (level.getBlockState(neighbor).is(clusterBlock)) {
                        continue;
                    }

                    BlockState crystalCluster = clusterBlock.defaultBlockState()
                            .setValue(AmethystClusterBlock.FACING, openFace);
                    if (crystalCluster.canSurvive(level, neighbor)) {
                        level.setBlock(neighbor, crystalCluster, Block.UPDATE_ALL);
                        placedAny = true;
                        clustersPlaced++;
                    }
                }
            }
        }

        return placedAny;
    }

    /**
     * 价值噪声 ∈ [0,1] 映射到三色等宽区间。
     */
    private static GlintstoneColor colorFromUniformNoise(double noise01) {
        if (noise01 < 1.0 / 3.0) {
            return GlintstoneColor.CYAN;
        }
        if (noise01 < 2.0 / 3.0) {
            return GlintstoneColor.BLUE;
        }
        return GlintstoneColor.PURPLE;
    }

    private static boolean isReplaceableStone(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.TUFF)
                || state.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    /**
     * 找一个邻接可替换空气的方向；没有则返回 null（表示在实心内部）。
     */
    private static Direction findOpenFace(
            WorldGenLevel level,
            BlockPos.MutableBlockPos stonePos,
            BlockPos.MutableBlockPos neighborOut
    ) {
        for (Direction direction : Direction.values()) {
            neighborOut.setWithOffset(stonePos, direction);
            BlockState neighborState = level.getBlockState(neighborOut);
            if (neighborState.isAir() || neighborState.is(Blocks.CAVE_AIR)) {
                return direction;
            }
        }
        return null;
    }

    /** 双线性插值价值噪声，输出约 [0,1]。 */
    private static double valueNoise2D(long seed, double x, double z) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double fx = smoothstep(x - x0);
        double fz = smoothstep(z - z0);
        double v00 = hash01(seed, x0, z0);
        double v10 = hash01(seed, x0 + 1, z0);
        double v01 = hash01(seed, x0, z0 + 1);
        double v11 = hash01(seed, x0 + 1, z0 + 1);
        double xA = lerp(fx, v00, v10);
        double xB = lerp(fx, v01, v11);
        return lerp(fz, xA, xB);
    }

    private static double smoothstep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double hash01(long seed, int x, int z) {
        long h = seed;
        h ^= (long) x * 0x9E3779B97F4A7C15L;
        h ^= (long) z * 0xC2B2AE3D27D4EB4FL;
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        // 取高位构造 [0,1)
        return (h >>> 11) * 0x1.0p-53;
    }
}

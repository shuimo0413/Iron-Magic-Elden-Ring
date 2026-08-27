package com.eldenring.spells.worldgen;

import com.eldenring.spells.config.EldenRingCommonConfig;
import com.eldenring.spells.registry.ModBlocks;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 辉石矿洞 Feature：大尺度噪声决定「辉石地带 + 整洞同色」；
 * 小尺度 chunk 门控抽出分散的合格 chunk，每个 chunk 只在一处囊心周围刷晶体。
 * 不新挖空洞，只装饰现成洞穴表面；晶簇优先长在已替换的辉石水晶块上。
 * 轮廓混搭不在本 Feature 里抽：同一簇方块的 blockstate 已对放射簇 / 尖塔 / 扇形 / 密丛 / 双晶 / 细针加权随机。
 */
public final class GlintstoneCaveFeature extends Feature<NoneFeatureConfiguration> {

    /**
     * 与世界种子异或的盐，避免和其它结构抢同一噪声相位。不进 toml。
     */
    private static final long CAVE_NOISE_SALT = 0x67C15_70C5_CAFEL;

    /** chunk 门控噪声盐（与大尺度、颜色噪声错相位）。 */
    private static final long CAVE_CHUNK_GATE_SALT = 0x51A71C57E0DEL;

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
        long seed = level.getSeed() ^ CAVE_NOISE_SALT;

        double regionCell = EldenRingCommonConfig.caveRegionNoiseCellSizeChunks;
        double regionNoise = valueNoise2D(seed, chunkX / regionCell, chunkZ / regionCell);
        if (regionNoise < EldenRingCommonConfig.caveRegionPresenceThreshold) {
            return false;
        }

        double chunkCell = EldenRingCommonConfig.caveChunkNoiseCellSizeChunks;
        double chunkGateNoise = valueNoise2D(
                seed ^ CAVE_CHUNK_GATE_SALT,
                chunkX / chunkCell,
                chunkZ / chunkCell
        );
        if (chunkGateNoise < EldenRingCommonConfig.caveChunkDecorateThreshold) {
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
        int minY = Math.max(EldenRingCommonConfig.caveScanMinY, level.getMinBuildHeight());
        int maxY = Math.min(EldenRingCommonConfig.caveScanMaxY, level.getMaxBuildHeight() - 1);

        List<BlockPos> pocketCenters = pickPocketCenters(level, minX, minZ, minY, maxY, random);
        if (pocketCenters.isEmpty()) {
            return false;
        }

        boolean placedAny = false;
        int clustersPlaced = 0;
        int pocketRadiusBlocks = EldenRingCommonConfig.cavePocketRadiusBlocks;
        int pocketRadiusSquared = pocketRadiusBlocks * pocketRadiusBlocks;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();

        for (BlockPos pocketCenter : pocketCenters) {
            int yStart = Math.max(minY, pocketCenter.getY() - pocketRadiusBlocks);
            int yEnd = Math.min(maxY, pocketCenter.getY() + pocketRadiusBlocks);
            int xStart = Math.max(minX, pocketCenter.getX() - pocketRadiusBlocks);
            int xEnd = Math.min(minX + 15, pocketCenter.getX() + pocketRadiusBlocks);
            int zStart = Math.max(minZ, pocketCenter.getZ() - pocketRadiusBlocks);
            int zEnd = Math.min(minZ + 15, pocketCenter.getZ() + pocketRadiusBlocks);

            for (int y = yStart; y <= yEnd; y++) {
                for (int x = xStart; x <= xEnd; x++) {
                    for (int z = zStart; z <= zEnd; z++) {
                        int offsetX = x - pocketCenter.getX();
                        int offsetY = y - pocketCenter.getY();
                        int offsetZ = z - pocketCenter.getZ();
                        if (offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ > pocketRadiusSquared) {
                            continue;
                        }

                        cursor.set(x, y, z);
                        BlockState state = level.getBlockState(cursor);
                        if (!isReplaceableStone(state)) {
                            continue;
                        }

                        Direction openFace = findOpenFace(level, cursor, neighbor);
                        if (openFace == null) {
                            continue;
                        }

                        if (random.nextFloat() < EldenRingCommonConfig.caveSurfaceBlockChance) {
                            level.setBlock(cursor, crystalBlock.defaultBlockState(), Block.UPDATE_ALL);
                            placedAny = true;
                        }

                        BlockState surfaceState = level.getBlockState(cursor);
                        boolean onCrystalBlock = surfaceState.is(crystalBlock);
                        float clusterChance = onCrystalBlock
                                ? EldenRingCommonConfig.caveSurfaceClusterOnBlockChance
                                : EldenRingCommonConfig.caveSurfaceClusterOnStoneChance;

                        if (clustersPlaced >= EldenRingCommonConfig.caveMaxClustersPerChunk) {
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
        }

        return placedAny;
    }

    /**
     * 在本 chunk 洞穴表面做步长抽样，蓄水池抽出若干囊心。
     * 装饰阶段只处理囊心欧氏半径内的表面，所以这里不必扫满每一格。
     */
    private static List<BlockPos> pickPocketCenters(
            WorldGenLevel level,
            int minX,
            int minZ,
            int minY,
            int maxY,
            RandomSource random
    ) {
        int pocketCount = Math.max(1, EldenRingCommonConfig.cavePocketsPerChunk);
        int stride = Math.max(1, EldenRingCommonConfig.cavePocketCenterScanStride);
        BlockPos[] reservoir = new BlockPos[pocketCount];
        int seenSurfaceCount = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();

        for (int y = minY; y <= maxY; y += stride) {
            for (int localX = 0; localX < 16; localX += stride) {
                for (int localZ = 0; localZ < 16; localZ += stride) {
                    cursor.set(minX + localX, y, minZ + localZ);
                    BlockState state = level.getBlockState(cursor);
                    if (!isReplaceableStone(state)) {
                        continue;
                    }
                    if (findOpenFace(level, cursor, neighbor) == null) {
                        continue;
                    }

                    seenSurfaceCount++;
                    if (seenSurfaceCount <= pocketCount) {
                        reservoir[seenSurfaceCount - 1] = cursor.immutable();
                    } else {
                        int replaceIndex = random.nextInt(seenSurfaceCount);
                        if (replaceIndex < pocketCount) {
                            reservoir[replaceIndex] = cursor.immutable();
                        }
                    }
                }
            }
        }

        if (seenSurfaceCount == 0) {
            return List.of();
        }

        int actualPocketCount = Math.min(pocketCount, seenSurfaceCount);
        List<BlockPos> pocketCenters = new ArrayList<>(actualPocketCount);
        for (int index = 0; index < actualPocketCount; index++) {
            pocketCenters.add(reservoir[index]);
        }
        return pocketCenters;
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

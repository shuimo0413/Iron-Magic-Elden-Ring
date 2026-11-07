package com.eldenring.spells.worldgen.structure;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * 学院周边「原始自然地面」高度采样器：直接问区块生成器的噪声，不读世界里的方块 / 高度图。
 * <p>
 * 为什么不能读高度图：特性阶段（FEATURES）的 {@code WorldGenRegion} 只保证本区块 ±1 有地形，
 * ±2~8 的区块可能还停在 STRUCTURE_STARTS（没有任何方块，高度图 = 世界底部），
 * 也可能已经放好了学院本体。读到的高度随区块生成顺序变化，会在学院底下挖出空腔、
 * 让缓坡在区块缝上撕裂。噪声高度与生成顺序无关，任何区块算出来都一样。
 * <p>
 * 采样点对齐世界坐标 {@link #GRID_STEP_BLOCKS} 的整数倍网格，再双线性插值；
 * 相邻区块共享同一批网格点，插值结果在区块缝上连续。
 * 实例只在一次 {@code afterPlace} 内使用（自带小缓存），不要跨线程共享。
 */
public final class AcademyTerrainSnapshot {

    /**
     * 噪声采样网格间距。调大 → 采样次数少、地面更「糊」；调小 → 更贴原地形但更慢。
     * 单位：方块。
     */
    private static final int GRID_STEP_BLOCKS = 8;

    private final ChunkGenerator chunkGenerator;
    private final RandomState randomState;
    private final LevelHeightAccessor heightAccessor;
    private final Long2IntOpenHashMap gridHeightCache = new Long2IntOpenHashMap();

    public AcademyTerrainSnapshot(
            ChunkGenerator chunkGenerator,
            RandomState randomState,
            LevelHeightAccessor heightAccessor
    ) {
        this.chunkGenerator = chunkGenerator;
        this.randomState = randomState;
        this.heightAccessor = heightAccessor;
        this.gridHeightCache.defaultReturnValue(Integer.MIN_VALUE);
    }

    /**
     * 某一点原始自然地面的顶面 Y（最上面一格实心方块；水下取水底）。
     * 网格插值结果，带小数，调用方自行取整。
     */
    public float naturalGroundY(int blockX, int blockZ) {
        int cellX = Math.floorDiv(blockX, GRID_STEP_BLOCKS);
        int cellZ = Math.floorDiv(blockZ, GRID_STEP_BLOCKS);
        float fractionX = (blockX - cellX * GRID_STEP_BLOCKS) / (float) GRID_STEP_BLOCKS;
        float fractionZ = (blockZ - cellZ * GRID_STEP_BLOCKS) / (float) GRID_STEP_BLOCKS;
        int heightNorthWest = gridHeight(cellX, cellZ);
        int heightNorthEast = gridHeight(cellX + 1, cellZ);
        int heightSouthWest = gridHeight(cellX, cellZ + 1);
        int heightSouthEast = gridHeight(cellX + 1, cellZ + 1);
        float northRow = Mth.lerp(fractionX, heightNorthWest, heightNorthEast);
        float southRow = Mth.lerp(fractionX, heightSouthWest, heightSouthEast);
        return Mth.lerp(fractionZ, northRow, southRow);
    }

    private int gridHeight(int cellX, int cellZ) {
        long cacheKey = ((long) cellX << 32) ^ (cellZ & 0xFFFFFFFFL);
        int cached = gridHeightCache.get(cacheKey);
        if (cached != Integer.MIN_VALUE) {
            return cached;
        }
        int sampled = sampleNaturalGroundY(
                chunkGenerator, randomState, heightAccessor,
                cellX * GRID_STEP_BLOCKS, cellZ * GRID_STEP_BLOCKS
        );
        gridHeightCache.put(cacheKey, sampled);
        return sampled;
    }

    /**
     * 单点噪声地面高度。{@code OCEAN_FLOOR_WG} 只认阻挡移动的方块，水面不算地面。
     */
    public static int sampleNaturalGroundY(
            ChunkGenerator chunkGenerator,
            RandomState randomState,
            LevelHeightAccessor heightAccessor,
            int blockX,
            int blockZ
    ) {
        return chunkGenerator.getFirstOccupiedHeight(
                blockX, blockZ, Heightmap.Types.OCEAN_FLOOR_WG, heightAccessor, randomState
        );
    }
}

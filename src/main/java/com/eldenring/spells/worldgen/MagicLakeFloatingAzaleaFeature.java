package com.eldenring.spells.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 魔法之湖水面杜鹃树：在开阔浅水上垫 1 格泥土，再长出原版骨粉催熟杜鹃花丛那种树
 * （{@code minecraft:azalea_tree}：橡木干 + 杜鹃叶 / 开花杜鹃叶）。
 * <p>
 * 挂在群系 JSON 的 {@code vegetal_decoration}：浅湖整平与辉石立柱已完成，荷叶尚未铺，
 * 所以落点认的是整平后的水面，树冠不会被后续荷叶抢格。
 * <p>
 * 稀有度在 placed feature JSON 的 {@code rarity_filter} 里调；邻水判定写死在本类。
 */
public final class MagicLakeFloatingAzaleaFeature extends Feature<NoneFeatureConfiguration> {

    /**
     * 落点水平四邻中，水面格至少要有几格才算「开阔水面」。
     * 调大 → 更靠湖心、更少贴岸；调小 → 岸边浅湾也会长。
     */
    private static final int MIN_OPEN_WATER_NEIGHBOR_COUNT = 3;

    public MagicLakeFloatingAzaleaFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int waterSurfaceY = chunkGenerator.getSeaLevel() - 1;
        BlockPos dirtPos = new BlockPos(origin.getX(), waterSurfaceY, origin.getZ());
        BlockState surfaceState = level.getBlockState(dirtPos);
        if (!isWaterSource(surfaceState)) {
            return false;
        }
        BlockState aboveDirtState = level.getBlockState(dirtPos.above());
        if (!isOpenAir(aboveDirtState)) {
            return false;
        }
        if (countOpenWaterNeighbors(level, dirtPos) < MIN_OPEN_WATER_NEIGHBOR_COUNT) {
            return false;
        }

        Holder<ConfiguredFeature<?, ?>> azaleaTree = level.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolder(TreeFeatures.AZALEA_TREE)
                .orElse(null);
        if (azaleaTree == null) {
            return false;
        }

        // 树干原点需要脚下有泥土：先临时垫泥，失败再还原成水，避免留下裸泥岛
        BlockState previousSurfaceState = surfaceState;
        level.setBlock(dirtPos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        boolean treePlaced = azaleaTree.value().place(level, chunkGenerator, random, dirtPos.above());
        if (!treePlaced) {
            level.setBlock(dirtPos, previousSurfaceState, Block.UPDATE_CLIENTS);
            return false;
        }
        return true;
    }

    /**
     * 统计 dirtPos 水平四邻在同一 Y 上仍是水源的格数。
     */
    private static int countOpenWaterNeighbors(WorldGenLevel level, BlockPos dirtPos) {
        int openWaterNeighborCount = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isWaterSource(level.getBlockState(dirtPos.relative(direction)))) {
                openWaterNeighborCount++;
            }
        }
        return openWaterNeighborCount;
    }

    /** 纯水源方块（不含海草等含水植物）。 */
    private static boolean isWaterSource(BlockState state) {
        return state.is(Blocks.WATER) && state.getFluidState().isSource();
    }

    /** 树干需要的净空：空气，或不含流体的可替换植物。 */
    private static boolean isOpenAir(BlockState state) {
        return state.isAir() || (state.canBeReplaced() && state.getFluidState().isEmpty());
    }
}

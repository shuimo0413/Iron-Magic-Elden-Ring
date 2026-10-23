package com.eldenring.spells.worldgen;

import com.eldenring.spells.registry.ModBlocks;
import com.eldenring.spells.world.GlintstoneColor;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 魔法之湖辉石水晶点缀：每处随机一种颜色（青 / 蓝 / 紫等概率），按落点分两种形态：
 * <ul>
 *   <li><b>湖中立柱</b>：落点在浅水里时，从湖底立起一根水晶块柱，露出水面 1–3 格，顶端朝上长晶簇；
 *       旁边随机伴生齐平水面的矮柱，主柱露出水面的侧面偶尔挂朝外的晶簇</li>
 *   <li><b>陆地露头</b>：落点在岛 / 湖岸上时，地面那格换成水晶块（半埋），上面再叠 0–2 块，顶端长晶簇；
 *       周围 2 格内零星散落半埋的水晶块和朝上的晶簇</li>
 * </ul>
 * 挂在群系 JSON 的 {@code local_modifications} 步骤：本区块已经整平（{@link MagicLakeShallowsFeature} 在 lakes 步骤），
 * 杉树、苔藓等植被还没长，所以杉树的存活判定会自动避开水晶块。
 * <p>
 * 所有写入都限制在落点所在区块内：邻居区块可能还没整平，越界写进去的水晶会被它的整平逻辑当成地面挪走或淹掉。
 * 形状参数是地形外观，写死不进 toml；稀有度在 placed feature JSON 的 {@code rarity_filter} 里，整合包可用数据包覆盖。
 */
public final class MagicLakeGlintstoneFeature extends Feature<NoneFeatureConfiguration> {

    /**
     * 湖中立柱允许的最大水深（方块）。湖心水深 ≤ 2，这里多留 1 格；更深的坑在湖岸过渡带，立柱会显得突兀，直接放弃。
     */
    private static final int PILLAR_MAX_WATER_DEPTH_BLOCKS = 3;
    /** 主柱露出水面的最少格数（方块）。 */
    private static final int PILLAR_MIN_RISE_ABOVE_WATER_BLOCKS = 1;
    /** 主柱露出水面的最多格数（方块）。调大 → 立柱更醒目，但在矮岛之间会显得过高。 */
    private static final int PILLAR_MAX_RISE_ABOVE_WATER_BLOCKS = 3;
    /** 主柱每个水平方向生成伴生矮柱的概率（0–1）。调大 → 一处水晶更像一丛。 */
    private static final float PILLAR_COMPANION_CHANCE = 0.35f;
    /** 伴生矮柱最多露出水面几格（方块）；0 表示柱顶齐平水面、只有晶簇露出来。 */
    private static final int COMPANION_MAX_RISE_ABOVE_WATER_BLOCKS = 1;
    /** 主柱每个水平方向在露出水面的那段挂一个朝外晶簇的概率（0–1）。 */
    private static final float PILLAR_SIDE_CLUSTER_CHANCE = 0.3f;

    /** 陆地露头在半埋水晶块上最多再叠几块（方块）。调大 → 露头更高更像小尖塔。 */
    private static final int OUTCROP_MAX_STACK_BLOCKS = 2;
    /** 陆地露头周围散落水晶的水平半径（方块）。 */
    private static final int OUTCROP_SCATTER_RADIUS_BLOCKS = 2;
    /** 周围散落点最少几个。 */
    private static final int OUTCROP_MIN_SCATTER_COUNT = 2;
    /** 周围散落点最多几个。调大 → 露头周围更密。 */
    private static final int OUTCROP_MAX_SCATTER_COUNT = 4;
    /** 散落点把地面换成水晶块（而不是只长一个晶簇）的概率（0–1）。 */
    private static final float OUTCROP_SCATTER_BLOCK_CHANCE = 0.5f;
    /** 散落点地面与露头地面的最大高差（方块），超出视为坡下 / 坡上，不放，免得晶簇挂在陡坎上。 */
    private static final int OUTCROP_SCATTER_MAX_HEIGHT_DIFF_BLOCKS = 2;

    public MagicLakeGlintstoneFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        GlintstoneColor[] colors = GlintstoneColor.values();
        ModBlocks.ColorSet colorSet = ModBlocks.BY_COLOR.get(colors[random.nextInt(colors.length)]);
        CrystalPlacer placer = new CrystalPlacer(
                level,
                SectionPos.blockToSectionCoord(origin.getX()),
                SectionPos.blockToSectionCoord(origin.getZ()),
                colorSet.crystalBlock.get().defaultBlockState(),
                colorSet.cluster.get()
        );

        // placed feature 用 OCEAN_FLOOR_WG 定位：origin 是地面上方第一格（水下就是湖底上方的水）
        BlockPos groundPos = origin.below();
        BlockState groundState = level.getBlockState(groundPos);
        if (!isSolidGround(groundState)) {
            return false;
        }

        int waterSurfaceY = context.chunkGenerator().getSeaLevel() - 1;
        BlockState originState = level.getBlockState(origin);
        if (isWaterSource(originState)) {
            int waterDepthBlocks = waterSurfaceY - groundPos.getY();
            if (waterDepthBlocks < 1 || waterDepthBlocks > PILLAR_MAX_WATER_DEPTH_BLOCKS) {
                return false;
            }
            return placeLakePillar(placer, random, groundPos, waterSurfaceY);
        }
        if (isOpenAboveGround(originState) && isLandGround(groundState)) {
            return placeLandOutcrop(placer, level, random, groundPos);
        }
        return false;
    }

    /**
     * 湖中立柱：湖底那格换成水晶块当根，往上立到水面以上 1–3 格，顶端朝上晶簇；
     * 先放伴生矮柱再挂侧面晶簇，免得侧簇占了伴柱的位置。
     */
    private static boolean placeLakePillar(CrystalPlacer placer, RandomSource random, BlockPos groundPos, int waterSurfaceY) {
        int riseAboveWaterBlocks = random.nextIntBetweenInclusive(
                PILLAR_MIN_RISE_ABOVE_WATER_BLOCKS,
                PILLAR_MAX_RISE_ABOVE_WATER_BLOCKS
        );
        int pillarTopY = buildCrystalColumn(placer, groundPos, waterSurfaceY + riseAboveWaterBlocks);
        if (pillarTopY <= waterSurfaceY) {
            // 被上方障碍物（邻居伸过来的树叶等）截断、没露出水面：保留水下那截，不再加晶簇装饰
            return true;
        }
        placer.placeCluster(new BlockPos(groundPos.getX(), pillarTopY + 1, groundPos.getZ()), Direction.UP);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (random.nextFloat() >= PILLAR_COMPANION_CHANCE) {
                continue;
            }
            placeCompanionPillar(placer, random, groundPos.relative(direction), waterSurfaceY);
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (random.nextFloat() >= PILLAR_SIDE_CLUSTER_CHANCE) {
                continue;
            }
            int sideClusterY = random.nextIntBetweenInclusive(waterSurfaceY + 1, pillarTopY);
            BlockPos pillarSidePos = new BlockPos(groundPos.getX(), sideClusterY, groundPos.getZ());
            placer.placeCluster(pillarSidePos.relative(direction), direction);
        }
        return true;
    }

    /**
     * 伴生矮柱：只在相邻一列水面也是水源时放；从那一列的湖底立到水面（或高出 1 格），顶上放朝上晶簇。
     *
     * @param neighborAtGroundY 相邻列、与主柱湖底同高的位置，只用它的 X / Z
     */
    private static void placeCompanionPillar(
            CrystalPlacer placer,
            RandomSource random,
            BlockPos neighborAtGroundY,
            int waterSurfaceY
    ) {
        if (!placer.isInsideChunk(neighborAtGroundY)) {
            return;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
                neighborAtGroundY.getX(), waterSurfaceY, neighborAtGroundY.getZ());
        if (!isWaterSource(placer.level.getBlockState(cursor))) {
            return;
        }
        int lowestAllowedGroundY = waterSurfaceY - PILLAR_MAX_WATER_DEPTH_BLOCKS;
        while (cursor.getY() > lowestAllowedGroundY && isWaterSource(placer.level.getBlockState(cursor))) {
            cursor.move(Direction.DOWN);
        }
        if (!isSolidGround(placer.level.getBlockState(cursor))) {
            return;
        }
        BlockPos companionGroundPos = cursor.immutable();
        int companionTopY = buildCrystalColumn(
                placer,
                companionGroundPos,
                waterSurfaceY + random.nextInt(COMPANION_MAX_RISE_ABOVE_WATER_BLOCKS + 1)
        );
        if (companionTopY >= waterSurfaceY) {
            placer.placeCluster(new BlockPos(companionGroundPos.getX(), companionTopY + 1, companionGroundPos.getZ()), Direction.UP);
        }
    }

    /**
     * 陆地露头：地面换成水晶块，上面叠 0–2 块，顶端朝上晶簇；周围散落半埋水晶块 / 晶簇。
     */
    private static boolean placeLandOutcrop(CrystalPlacer placer, WorldGenLevel level, RandomSource random, BlockPos groundPos) {
        int outcropTopY = buildCrystalColumn(
                placer,
                groundPos,
                groundPos.getY() + random.nextInt(OUTCROP_MAX_STACK_BLOCKS + 1)
        );
        placer.placeCluster(new BlockPos(groundPos.getX(), outcropTopY + 1, groundPos.getZ()), Direction.UP);

        int scatterCount = random.nextIntBetweenInclusive(OUTCROP_MIN_SCATTER_COUNT, OUTCROP_MAX_SCATTER_COUNT);
        for (int scatterIndex = 0; scatterIndex < scatterCount; scatterIndex++) {
            int offsetX = random.nextIntBetweenInclusive(-OUTCROP_SCATTER_RADIUS_BLOCKS, OUTCROP_SCATTER_RADIUS_BLOCKS);
            int offsetZ = random.nextIntBetweenInclusive(-OUTCROP_SCATTER_RADIUS_BLOCKS, OUTCROP_SCATTER_RADIUS_BLOCKS);
            if (offsetX == 0 && offsetZ == 0) {
                continue;
            }
            int scatterX = groundPos.getX() + offsetX;
            int scatterZ = groundPos.getZ() + offsetZ;
            if (!placer.isInsideChunk(scatterX, scatterZ)) {
                continue;
            }
            int scatterGroundY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, scatterX, scatterZ) - 1;
            if (Math.abs(scatterGroundY - groundPos.getY()) > OUTCROP_SCATTER_MAX_HEIGHT_DIFF_BLOCKS) {
                continue;
            }
            BlockPos scatterGroundPos = new BlockPos(scatterX, scatterGroundY, scatterZ);
            BlockState scatterGroundState = level.getBlockState(scatterGroundPos);
            BlockState scatterAboveState = level.getBlockState(scatterGroundPos.above());
            if (!isLandGround(scatterGroundState) || !isOpenAboveGround(scatterAboveState)) {
                continue;
            }
            if (random.nextFloat() < OUTCROP_SCATTER_BLOCK_CHANCE) {
                placer.setCrystalBlock(scatterGroundPos);
            }
            placer.placeCluster(scatterGroundPos.above(), Direction.UP);
        }
        return true;
    }

    /**
     * 从 groundPos 开始（含，把地面换成水晶块当根）往上逐格放水晶块到 topY；
     * 碰到既不是水也不是空气 / 可替换植物的方块就停。
     *
     * @return 实际放到的最高 Y（只放了根就是 groundPos 的 Y）
     */
    private static int buildCrystalColumn(CrystalPlacer placer, BlockPos groundPos, int topY) {
        placer.setCrystalBlock(groundPos);
        BlockPos.MutableBlockPos cursor = groundPos.mutable();
        int placedTopY = groundPos.getY();
        for (int columnY = groundPos.getY() + 1; columnY <= topY; columnY++) {
            cursor.setY(columnY);
            BlockState existingState = placer.level.getBlockState(cursor);
            if (!isWaterSource(existingState) && !isOpenAboveGround(existingState)) {
                break;
            }
            placer.setCrystalBlock(cursor);
            placedTopY = columnY;
        }
        return placedTopY;
    }

    /** 能当水晶根基的实心地面：挡路、不是树叶 / 原木（邻居树冠伸过来时不要长在叶子上）。 */
    private static boolean isSolidGround(BlockState state) {
        return state.blocksMotion() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS);
    }

    /** 陆地露头允许的地面：泥土类（草方块、苔藓块、泥）或原版石头 / 砂砾，岛上的石斑也能长。 */
    private static boolean isLandGround(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.GRAVEL);
    }

    /** 地面上方可以放东西：空气，或不含流体的可替换植物（邻居区块长过来的草等）。 */
    private static boolean isOpenAboveGround(BlockState state) {
        return state.isAir() || (state.canBeReplaced() && state.getFluidState().isEmpty());
    }

    /** 纯水源方块（不含海草等含水植物）。 */
    private static boolean isWaterSource(BlockState state) {
        return state.is(Blocks.WATER) && state.getFluidState().isSource();
    }

    /**
     * 一处水晶的放置上下文：颜色固定、写入限制在落点区块内。
     */
    private static final class CrystalPlacer {
        private final WorldGenLevel level;
        private final int chunkX;
        private final int chunkZ;
        private final BlockState crystalBlockState;
        private final AmethystClusterBlock clusterBlock;

        private CrystalPlacer(
                WorldGenLevel level,
                int chunkX,
                int chunkZ,
                BlockState crystalBlockState,
                AmethystClusterBlock clusterBlock
        ) {
            this.level = level;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.crystalBlockState = crystalBlockState;
            this.clusterBlock = clusterBlock;
        }

        private boolean isInsideChunk(BlockPos pos) {
            return isInsideChunk(pos.getX(), pos.getZ());
        }

        private boolean isInsideChunk(int blockX, int blockZ) {
            return SectionPos.blockToSectionCoord(blockX) == chunkX && SectionPos.blockToSectionCoord(blockZ) == chunkZ;
        }

        private void setCrystalBlock(BlockPos pos) {
            if (isInsideChunk(pos)) {
                level.setBlock(pos, crystalBlockState, Block.UPDATE_CLIENTS);
            }
        }

        /**
         * 放一个朝 facing 的晶簇：目标格须为空气 / 可替换植物 / 水源（水源时设为含水），且背后有能附着的面。
         */
        private void placeCluster(BlockPos pos, Direction facing) {
            if (!isInsideChunk(pos)) {
                return;
            }
            BlockState existingState = level.getBlockState(pos);
            boolean inWater = isWaterSource(existingState);
            if (!inWater && !isOpenAboveGround(existingState)) {
                return;
            }
            BlockState clusterState = clusterBlock.defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, facing)
                    .setValue(AmethystClusterBlock.WATERLOGGED, inWater);
            if (clusterState.canSurvive(level, pos)) {
                level.setBlock(pos, clusterState, Block.UPDATE_CLIENTS);
            }
        }
    }
}

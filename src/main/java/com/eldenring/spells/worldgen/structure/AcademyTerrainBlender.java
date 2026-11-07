package com.eldenring.spells.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.common.Tags;

/**
 * 学院落成后的地形融合（每个区块 {@code afterPlace} 调一次，只改本区块的列）。
 * <p>
 * 三条硬规则，对应三类历史问题：
 * <ol>
 *   <li><b>不悬空</b>：脚印内每列从模板底面 {@code floorY-1} 往下补实，直到碰到自然实心地面。
 *       只往下补，不碰模板体积内部。</li>
 *   <li><b>不撕裂</b>：缓坡目标高度只依赖 {@link AcademyTerrainSnapshot}（噪声地面，和区块生成顺序无关）
 *       与本列自己的方块，所以相邻区块各算各的也能对上缝。
 *       外侧带宽 {@link #blendExtentPaddingBlocks()} 受原版结构引用半径（±8 区块）限制，最多 20 格。</li>
 *   <li><b>不堵路</b>：模板体积内只允许一种改动——外侧地形更高时，把模板边缘
 *       「露天平地」垫高成缓坡（用户允许地形蔓延进学院一部分）。
 *       该列头顶 {@link #ENCROACH_HEADROOM_BLOCKS} 格内只要有任何建筑方块 / 水 / 树，就整列不动。</li>
 * </ol>
 * 坡形：从模板地面高度 {@code groundY} 到外侧自然地面用一条 smoothstep 过渡；
 * 外侧更高且落差大时，坡起点向学院内侧推（最多 {@link #INNER_ENCROACH_MAX_BLOCKS} 格），
 * 让坡度不超过 {@link #TARGET_MAX_GRADE}。内推深度再受 {@link AcademyTemplateEdgeProfile}
 * 封顶：边缘内侧是护城河 / 建筑的段落不内推，坡完全在外侧做完，避免边界留台阶。
 */
public final class AcademyTerrainBlender {

    /**
     * 外侧缓坡基础带宽。调大 → 坡更缓、改动范围更大；调小 → 更贴边、更陡。
     * 与 {@link #OUTER_BLEND_NOISE_BLOCKS} 之和不能超过 20（学院 Z 半深 115 + 20 = 原版引用半径边缘），
     * 否则超出部分的区块收不到 afterPlace，坡会在区块边界被一刀切断。
     * 单位：方块。
     */
    private static final int OUTER_BLEND_BASE_BLOCKS = 16;

    /**
     * 外侧带宽随噪声伸缩的幅度，让坡脚线不是一条直线。
     * 单位：方块。
     */
    private static final int OUTER_BLEND_NOISE_BLOCKS = 4;

    /**
     * 外侧地形更高时，缓坡最多向学院内侧推进多少格。调大 → 高地蔓延进院子更深；调小 → 坡更陡。
     * 单位：方块。
     */
    private static final int INNER_ENCROACH_MAX_BLOCKS = 24;

    /**
     * 目标最大坡度 |ΔY/ΔX|。约 0.45 ≈ 24°，像山坡不像断崖。
     * 调大 → 同样落差需要的内推距离更短；调小 → 更缓、更吃院子。
     */
    private static final float TARGET_MAX_GRADE = 0.45F;

    /**
     * 内侧垫高时要求目标顶面上方保持净空的格数。调大 → 离建筑更远才垫；调小 → 贴着门口也会垫。
     * 单位：方块。
     */
    private static final int ENCROACH_HEADROOM_BLOCKS = 3;

    /**
     * 内侧扫描模板自带地面时，最多从 floorY 往上数多少格实心地面。
     * 模板边缘地面约在 floorY+2；调大只会多扫几格。
     * 单位：方块。
     */
    private static final int TEMPLATE_GROUND_SCAN_BLOCKS = 12;

    /**
     * 模板底面往下补实的最大深度。正常选址落差 ≤ 20 格；这里只防极端峡谷时死循环式补柱。
     * 单位：方块。
     */
    private static final int MAX_SUPPORT_DEPTH_BLOCKS = 128;

    /**
     * 外侧列找真实地面时，从参考高度往上多扫多少格（要跨过树冠）。
     * 单位：方块。
     */
    private static final int GROUND_SCAN_UP_BLOCKS = 40;

    /**
     * 外侧列找真实地面时，最多往下扫多少格（峡谷 / 深水）。
     * 单位：方块。
     */
    private static final int GROUND_SCAN_DOWN_BLOCKS = 96;

    /**
     * 削坡后要连根清掉的树干 / 植物高度上限。
     * 单位：方块。
     */
    private static final int VEGETATION_CLEAR_HEIGHT_BLOCKS = 24;

    /**
     * 坡面表土下的泥土层厚度（再往下是石头）。调大 → 陡处露出更厚的土；调小 → 更快露石。
     * 单位：方块。
     */
    private static final int SUBSOIL_DEPTH_BLOCKS = 3;

    /**
     * 坡带边缘（带宽）噪声空间频率。调大 → 坡脚线更碎；调小 → 大弧度起伏。
     */
    private static final float EDGE_NOISE_FREQUENCY = 0.07F;

    /**
     * 坡面高度噪声空间频率（低于边缘频率，避免碎阶梯）。
     */
    private static final float SLOPE_HEIGHT_NOISE_FREQUENCY = 0.045F;

    /**
     * 坡面高度噪声最大振幅；坡两端自动衰减为 0，保证与学院地面、自然地面无缝。
     * 单位：方块。
     */
    private static final float SLOPE_HEIGHT_NOISE_MAX_BLOCKS = 2.0F;

    /** 高度噪声采样相对边缘噪声的盐偏移，避免两路同相。 */
    private static final int HEIGHT_NOISE_SALT_OFFSET = 0x51ED;

    /**
     * 内侧参考高度按「到各边距离」加权时，超过此距离的边直接忽略（权重已 &lt; 0.05%）。
     * 单位：方块。
     */
    private static final int EDGE_REFERENCE_MAX_DISTANCE_BLOCKS = 48;

    /** 本列没找到可用地面 / 遇到别的建筑时的哨兵值。 */
    private static final int NO_HEIGHT = Integer.MIN_VALUE;

    /** setBlock 旗标：与结构放置一致，只通知客户端，不触发邻居更新。 */
    private static final int PLACE_FLAGS = 2;

    private AcademyTerrainBlender() {
    }

    /**
     * {@link AcademyBlendExtentPiece} 外扩用的外侧最大带宽（含噪声）。
     */
    public static int blendExtentPaddingBlocks() {
        return OUTER_BLEND_BASE_BLOCKS + OUTER_BLEND_NOISE_BLOCKS;
    }

    /** {@link AcademyTemplateEdgeProfile} 统计露天深度时的上限，与缓坡内推上限一致。 */
    public static int innerEncroachMaxBlocks() {
        return INNER_ENCROACH_MAX_BLOCKS;
    }

    /**
     * 在当前区块生成盒内修补地形。
     *
     * @param level     世界（特性阶段为 WorldGenRegion，/place 时为 ServerLevel）
     * @param chunkBox  本次可写的区块包围盒
     * @param footprint 学院模板脚印（不含外扩空片）
     * @param groundY     模板边缘地面顶面 Y（= floorY + 模板地面偏移）
     * @param snapshot    原始自然地面采样器
     * @param edgeProfile 模板边缘露天深度；null 时完全不往院内垫高
     */
    public static void blend(
            WorldGenLevel level,
            BoundingBox chunkBox,
            BoundingBox footprint,
            int groundY,
            AcademyTerrainSnapshot snapshot,
            AcademyTemplateEdgeProfile edgeProfile
    ) {
        int seedSalt = footprint.minX() * 734287 ^ footprint.minZ() * 912371 ^ footprint.minY();
        int padding = blendExtentPaddingBlocks();
        int minX = Math.max(chunkBox.minX(), footprint.minX() - padding);
        int maxX = Math.min(chunkBox.maxX(), footprint.maxX() + padding);
        int minZ = Math.max(chunkBox.minZ(), footprint.minZ() - padding);
        int maxZ = Math.min(chunkBox.maxZ(), footprint.maxZ() + padding);
        int floorY = footprint.minY();
        int writableMinY = Math.max(chunkBox.minY(), level.getMinBuildHeight());
        int writableMaxY = Math.min(chunkBox.maxY(), level.getMaxBuildHeight() - 1);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean insideFootprint = isInsideFootprint(x, z, footprint);
                if (insideFootprint) {
                    fillSupportBelowFloor(level, cursor, x, z, floorY, writableMinY);
                }
                blendColumn(
                        level, cursor, x, z, footprint, floorY, groundY, snapshot, edgeProfile,
                        seedSalt, insideFootprint, writableMinY, writableMaxY
                );
            }
        }
    }

    /**
     * 脚印内：从模板底面下一格往下补实，直到碰到自然实心方块。
     * 模板最底层几乎全是泥土，所以这一步只填「模板与地面之间的缝」，看不到也不会堵路。
     */
    private static void fillSupportBelowFloor(
            WorldGenLevel level,
            BlockPos.MutableBlockPos cursor,
            int x,
            int z,
            int floorY,
            int writableMinY
    ) {
        int bottomLimit = Math.max(writableMinY, floorY - MAX_SUPPORT_DEPTH_BLOCKS);
        for (int y = floorY - 1; y >= bottomLimit; y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            if (!isGap(state)) {
                return;
            }
            level.setBlock(cursor, foundationBlockAt(y), PLACE_FLAGS);
        }
    }

    /**
     * 单列缓坡：先求目标顶面，再按脚印内外分别「垫高模板边缘平地」或「削 / 垫外侧自然地形」。
     */
    private static void blendColumn(
            WorldGenLevel level,
            BlockPos.MutableBlockPos cursor,
            int x,
            int z,
            BoundingBox footprint,
            int floorY,
            int groundY,
            AcademyTerrainSnapshot snapshot,
            AcademyTemplateEdgeProfile edgeProfile,
            int seedSalt,
            boolean insideFootprint,
            int writableMinY,
            int writableMaxY
    ) {
        float signedDistance = signedDistanceToFootprintEdge(x, z, footprint);
        if (signedDistance < -INNER_ENCROACH_MAX_BLOCKS) {
            return;
        }
        float edgeNoise = smoothNoise2D(x, z, seedSalt, EDGE_NOISE_FREQUENCY);
        float outerReach = OUTER_BLEND_BASE_BLOCKS + edgeNoise * OUTER_BLEND_NOISE_BLOCKS;
        if (signedDistance > outerReach) {
            return;
        }

        // 内侧列用「最近几条边外一格」的噪声地面加权；外侧列用本列噪声地面。两者在边界上相等。
        float snapshotReferenceY = insideFootprint
                ? insideEdgeReferenceY(x, z, footprint, snapshot)
                : snapshot.naturalGroundY(x, z);
        float snapshotDelta = snapshotReferenceY - groundY;
        float innerReach = 0.0F;
        if (snapshotDelta > 0.0F && edgeProfile != null) {
            // 坡度需要的内推距离，再被「这段边缘往里到底有多少露天平地」封顶；
            // 外侧列与内侧列查同一份模板剖面，边界两侧对坡起点的认知一致
            float gradeReach = snapshotDelta / TARGET_MAX_GRADE - outerReach;
            float templateReach = edgeProfile.allowedInnerDepth(x, z, footprint);
            innerReach = Mth.clamp(Math.min(gradeReach, templateReach), 0.0F, INNER_ENCROACH_MAX_BLOCKS);
        }
        if (signedDistance < -innerReach || (insideFootprint && innerReach <= 0.0F)) {
            return;
        }

        float blendProgress = (signedDistance + innerReach) / (innerReach + outerReach);
        if (insideFootprint) {
            int targetY = targetSurfaceY(x, z, seedSalt, groundY, snapshotReferenceY, blendProgress);
            raiseTemplateEdgeGround(level, cursor, x, z, floorY, Math.min(targetY, writableMaxY - 1));
            return;
        }

        float expectedHighestY = Math.max(groundY, snapshotReferenceY);
        LiveColumn liveColumn = scanLiveColumn(
                level, cursor, x, z,
                Math.min(writableMaxY, Mth.ceil(expectedHighestY) + GROUND_SCAN_UP_BLOCKS),
                Math.max(writableMinY, Mth.floor(Math.min(groundY, snapshotReferenceY)) - GROUND_SCAN_DOWN_BLOCKS),
                writableMaxY
        );
        if (liveColumn == null) {
            return;
        }
        // 坡脚处完全贴回本列真实地面，避免噪声估算与实际地形差几格留下台阶
        float liveWeight = smoothstep01(signedDistance / outerReach);
        float referenceY = Mth.lerp(liveWeight, snapshotReferenceY, (float) liveColumn.groundY);
        int targetY = targetSurfaceY(x, z, seedSalt, groundY, referenceY, blendProgress);
        targetY = Mth.clamp(targetY, writableMinY + SUBSOIL_DEPTH_BLOCKS + 1, writableMaxY - 1);
        reshapeOuterColumn(level, cursor, x, z, liveColumn, targetY);
    }

    /**
     * 缓坡目标顶面：groundY → referenceY 的 smoothstep，叠加两端归零的高度噪声。
     */
    private static int targetSurfaceY(
            int x,
            int z,
            int seedSalt,
            int groundY,
            float referenceY,
            float blendProgress
    ) {
        float easedProgress = smoothstep01(blendProgress);
        float targetHeight = groundY + (referenceY - groundY) * easedProgress;
        float clampedProgress = Mth.clamp(blendProgress, 0.0F, 1.0F);
        float noiseEnvelope = 4.0F * clampedProgress * (1.0F - clampedProgress);
        float noiseAmplitude = Math.min(SLOPE_HEIGHT_NOISE_MAX_BLOCKS, Math.abs(referenceY - groundY) / 4.0F);
        float heightNoise = smoothNoise2D(x, z, seedSalt + HEIGHT_NOISE_SALT_OFFSET, SLOPE_HEIGHT_NOISE_FREQUENCY);
        return Math.round(targetHeight + heightNoise * noiseAmplitude * noiseEnvelope);
    }

    /**
     * 脚印内（外侧地形更高）：把模板边缘的露天平地垫高到 {@code targetY}。
     * <p>
     * 只认「从 floorY 起连续的自然地面」作为模板地面；其上方直到目标顶面再往上
     * {@link #ENCROACH_HEADROOM_BLOCKS} 格必须全是空气或小草，否则（墙、门洞、路面、水、树）整列跳过。
     */
    private static void raiseTemplateEdgeGround(
            WorldGenLevel level,
            BlockPos.MutableBlockPos cursor,
            int x,
            int z,
            int floorY,
            int targetY
    ) {
        cursor.set(x, floorY, z);
        BlockState bottomState = level.getBlockState(cursor);
        if (!isNaturalGround(bottomState)) {
            return;
        }
        int templateGroundTopY = floorY;
        while (templateGroundTopY < floorY + TEMPLATE_GROUND_SCAN_BLOCKS) {
            cursor.set(x, templateGroundTopY + 1, z);
            if (!isNaturalGround(level.getBlockState(cursor))) {
                break;
            }
            templateGroundTopY++;
        }
        int maxRaisedTopY = templateGroundTopY + AcademyTemplateEdgeProfile.RAISE_CLEARANCE_BLOCKS - ENCROACH_HEADROOM_BLOCKS;
        targetY = Math.min(targetY, maxRaisedTopY);
        if (targetY <= templateGroundTopY) {
            return;
        }
        for (int y = templateGroundTopY + 1; y <= targetY + ENCROACH_HEADROOM_BLOCKS; y++) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            boolean openAir = state.isAir() || (state.canBeReplaced() && state.getFluidState().isEmpty());
            if (!openAir) {
                return;
            }
        }

        cursor.set(x, templateGroundTopY, z);
        BlockState templateSurface = level.getBlockState(cursor);
        BlockState surfaceState = surfaceBlockAboveWater(templateSurface);
        BlockState subsoilState = subsoilBlockFor(templateSurface);
        for (int y = templateGroundTopY; y <= targetY; y++) {
            cursor.set(x, y, z);
            level.setBlock(cursor, y == targetY ? surfaceState : subsoilState, PLACE_FLAGS);
        }
        clearPlantOnTop(level, cursor, x, z, targetY + 1);
    }

    /**
     * 脚印外：本列真实地面高于目标就削（连带清掉会浮空的树干 / 植物），低于目标就垫。
     */
    private static void reshapeOuterColumn(
            WorldGenLevel level,
            BlockPos.MutableBlockPos cursor,
            int x,
            int z,
            LiveColumn liveColumn,
            int targetY
    ) {
        int currentGroundY = liveColumn.groundY;
        boolean targetUnderwater = liveColumn.waterSurfaceY != NO_HEIGHT && targetY < liveColumn.waterSurfaceY;
        BlockState surfaceState = targetUnderwater
                ? surfaceBlockUnderwater(liveColumn.surfaceState)
                : surfaceBlockAboveWater(liveColumn.surfaceState);
        BlockState subsoilState = subsoilBlockFor(liveColumn.surfaceState);

        if (currentGroundY > targetY) {
            clearVegetationAbove(level, cursor, x, z, currentGroundY + 1, liveColumn.waterSurfaceY);
            for (int y = currentGroundY; y > targetY; y--) {
                cursor.set(x, y, z);
                BlockState state = level.getBlockState(cursor);
                if (isNaturalGround(state) || isGap(state)) {
                    level.setBlock(cursor, emptyBlockAt(y, liveColumn.waterSurfaceY), PLACE_FLAGS);
                }
            }
            // 削出的新坡面铺表土，避免整片裸石断面
            for (int y = targetY; y >= targetY - SUBSOIL_DEPTH_BLOCKS; y--) {
                cursor.set(x, y, z);
                BlockState state = level.getBlockState(cursor);
                if (!isNaturalGround(state) && !isGap(state)) {
                    break;
                }
                level.setBlock(cursor, y == targetY ? surfaceState : subsoilState, PLACE_FLAGS);
            }
            return;
        }

        if (currentGroundY < targetY) {
            for (int y = currentGroundY + 1; y <= targetY; y++) {
                cursor.set(x, y, z);
                BlockState state = level.getBlockState(cursor);
                if (!isGap(state) && !isNaturalGround(state)) {
                    continue;
                }
                BlockState placeState;
                if (y == targetY) {
                    placeState = surfaceState;
                } else if (y >= targetY - SUBSOIL_DEPTH_BLOCKS) {
                    placeState = subsoilState;
                } else {
                    placeState = foundationBlockAt(y);
                }
                level.setBlock(cursor, placeState, PLACE_FLAGS);
            }
            // 原来的草皮被埋住后改成泥土，免得剖面里夹一层草
            cursor.set(x, currentGroundY, z);
            if (level.getBlockState(cursor).is(Blocks.GRASS_BLOCK)) {
                level.setBlock(cursor, Blocks.DIRT.defaultBlockState(), PLACE_FLAGS);
            }
            clearPlantOnTop(level, cursor, x, z, targetY + 1);
        }
    }

    /**
     * 本列真实地面信息。{@code waterSurfaceY} 为最上面一格水 / 冰，没有为 {@link #NO_HEIGHT}。
     */
    private record LiveColumn(int groundY, int waterSurfaceY, BlockState surfaceState) {
    }

    /**
     * 自上而下找本列第一格自然地面，跳过空气、水、植物、树。
     * 先碰到别的建筑方块（村庄路面、其它结构）就返回 null，整列不动。
     */
    private static LiveColumn scanLiveColumn(
            WorldGenLevel level,
            BlockPos.MutableBlockPos cursor,
            int x,
            int z,
            int scanTopY,
            int scanBottomY,
            int writableMaxY
    ) {
        int topY = scanTopY;
        cursor.set(x, topY, z);
        // 扫描起点就在地里：说明地面比估算高，先往上找到真正的顶
        while (topY < writableMaxY && isNaturalGround(level.getBlockState(cursor))) {
            topY++;
            cursor.set(x, topY, z);
        }
        int waterSurfaceY = NO_HEIGHT;
        for (int y = topY; y >= scanBottomY; y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            if (isNaturalGround(state)) {
                return new LiveColumn(y, waterSurfaceY, state);
            }
            if (!isGap(state)) {
                return null;
            }
            if (waterSurfaceY == NO_HEIGHT && (!state.getFluidState().isEmpty() || isIce(state))) {
                waterSurfaceY = y;
            }
        }
        return null;
    }

    /**
     * 削坡前清掉本列地面以上会浮空的树干 / 植物（树叶留给邻列，不在这里砍整棵树冠）。
     */
    private static void clearVegetationAbove(
            WorldGenLevel level,
            BlockPos.MutableBlockPos cursor,
            int x,
            int z,
            int startY,
            int waterSurfaceY
    ) {
        for (int y = startY; y < startY + VEGETATION_CLEAR_HEIGHT_BLOCKS; y++) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            if (state.isAir()) {
                return;
            }
            if (!isPlantOrTrunk(state)) {
                return;
            }
            level.setBlock(cursor, emptyBlockAt(y, waterSurfaceY), PLACE_FLAGS);
        }
    }

    /** 垫高后顶面上方若残留小草 / 花，会悬在半空，直接清掉。 */
    private static void clearPlantOnTop(WorldGenLevel level, BlockPos.MutableBlockPos cursor, int x, int z, int y) {
        cursor.set(x, y, z);
        BlockState state = level.getBlockState(cursor);
        if (!state.isAir() && state.getFluidState().isEmpty() && isPlantOrTrunk(state)) {
            level.setBlock(cursor, Blocks.AIR.defaultBlockState(), PLACE_FLAGS);
        }
    }

    /**
     * 内侧列的参考自然地面：取四条边外一格的噪声地面，按距离平方反比加权。
     * 靠某条边时几乎就是那条边的值（与外侧列连续）；在角上两条边平滑混合，不出折痕。
     */
    private static float insideEdgeReferenceY(int x, int z, BoundingBox footprint, AcademyTerrainSnapshot snapshot) {
        int distanceWest = x - footprint.minX();
        int distanceEast = footprint.maxX() - x;
        int distanceNorth = z - footprint.minZ();
        int distanceSouth = footprint.maxZ() - z;
        float weightedSum = 0.0F;
        float weightTotal = 0.0F;
        if (distanceWest <= EDGE_REFERENCE_MAX_DISTANCE_BLOCKS) {
            float weight = edgeWeight(distanceWest);
            weightedSum += weight * snapshot.naturalGroundY(footprint.minX() - 1, z);
            weightTotal += weight;
        }
        if (distanceEast <= EDGE_REFERENCE_MAX_DISTANCE_BLOCKS) {
            float weight = edgeWeight(distanceEast);
            weightedSum += weight * snapshot.naturalGroundY(footprint.maxX() + 1, z);
            weightTotal += weight;
        }
        if (distanceNorth <= EDGE_REFERENCE_MAX_DISTANCE_BLOCKS) {
            float weight = edgeWeight(distanceNorth);
            weightedSum += weight * snapshot.naturalGroundY(x, footprint.minZ() - 1);
            weightTotal += weight;
        }
        if (distanceSouth <= EDGE_REFERENCE_MAX_DISTANCE_BLOCKS) {
            float weight = edgeWeight(distanceSouth);
            weightedSum += weight * snapshot.naturalGroundY(x, footprint.maxZ() + 1);
            weightTotal += weight;
        }
        if (weightTotal <= 0.0F) {
            return Float.NEGATIVE_INFINITY;
        }
        return weightedSum / weightTotal;
    }

    private static float edgeWeight(int distanceToEdge) {
        return 1.0F / (distanceToEdge * distanceToEdge + 1.0F);
    }

    // ---------------------------------------------------------------- 方块分类

    /**
     * 「缝」：可以被补实 / 被坡体覆盖的格子——空气、液体、冰、可替换植被、树干树叶。
     */
    private static boolean isGap(BlockState state) {
        return state.isAir()
                || !state.getFluidState().isEmpty()
                || state.canBeReplaced()
                || isIce(state)
                || isPlantOrTrunk(state)
                || state.is(BlockTags.LEAVES);
    }

    /** 植物、树干、蘑菇等「长在地面上」的东西（削坡后会浮空）。 */
    private static boolean isPlantOrTrunk(BlockState state) {
        return state.is(BlockTags.LOGS)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.is(BlockTags.REPLACEABLE)
                || state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.LILY_PAD)
                || state.is(Blocks.BROWN_MUSHROOM)
                || state.is(Blocks.RED_MUSHROOM)
                || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
                || state.is(Blocks.RED_MUSHROOM_BLOCK)
                || state.is(Blocks.MUSHROOM_STEM)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.BAMBOO)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.PUMPKIN)
                || state.is(Blocks.MELON)
                || state.is(Blocks.MOSS_CARPET)
                || state.is(Blocks.MANGROVE_ROOTS)
                || state.is(Blocks.MANGROVE_PROPAGULE)
                || state.is(Blocks.KELP)
                || state.is(Blocks.KELP_PLANT)
                || state.is(Blocks.BEE_NEST);
    }

    private static boolean isIce(BlockState state) {
        return state.is(BlockTags.ICE);
    }

    /**
     * 自然地面：石头类、泥土类、沙、沙砾、黏土、陶瓦、雪块、矿石。
     * 只有这些会被削；其它实心方块一律视为「建筑」不碰。
     */
    static boolean isNaturalGround(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.TERRACOTTA)
                || state.is(Tags.Blocks.ORES)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.MUDDY_MANGROVE_ROOTS);
    }

    // ---------------------------------------------------------------- 材质

    /** 露天坡顶：沿用本列原表层（沙 / 泥 / 灰化土等），石头和泥土一律换成草方块。 */
    private static BlockState surfaceBlockAboveWater(BlockState originalSurface) {
        if (originalSurface.is(BlockTags.SAND)
                || originalSurface.is(Blocks.MUD)
                || originalSurface.is(Blocks.PODZOL)
                || originalSurface.is(Blocks.MYCELIUM)
                || originalSurface.is(Blocks.MOSS_BLOCK)
                || originalSurface.is(Blocks.SNOW_BLOCK)
                || originalSurface.is(Blocks.GRASS_BLOCK)) {
            return originalSurface.getBlock().defaultBlockState();
        }
        return Blocks.GRASS_BLOCK.defaultBlockState();
    }

    /** 水下坡顶：沙 / 沙砾 / 黏土 / 泥保持原样，其余用泥土（水下不长草）。 */
    private static BlockState surfaceBlockUnderwater(BlockState originalSurface) {
        if (originalSurface.is(BlockTags.SAND)
                || originalSurface.is(Blocks.GRAVEL)
                || originalSurface.is(Blocks.CLAY)
                || originalSurface.is(Blocks.MUD)) {
            return originalSurface.getBlock().defaultBlockState();
        }
        return Blocks.DIRT.defaultBlockState();
    }

    /** 表层下的土层：沙地用沙，泥沼用泥，其余用泥土。 */
    private static BlockState subsoilBlockFor(BlockState originalSurface) {
        if (originalSurface.is(BlockTags.SAND)) {
            return originalSurface.getBlock().defaultBlockState();
        }
        if (originalSurface.is(Blocks.MUD)) {
            return Blocks.MUD.defaultBlockState();
        }
        return Blocks.DIRT.defaultBlockState();
    }

    /** 削掉的格子：原水面以下回填水，以上为空气，避免在湖边挖出水下空腔。 */
    private static BlockState emptyBlockAt(int y, int waterSurfaceY) {
        if (waterSurfaceY != NO_HEIGHT && y <= waterSurfaceY) {
            return Blocks.WATER.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static BlockState foundationBlockAt(int y) {
        return y < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
    }

    // ---------------------------------------------------------------- 几何 / 噪声

    private static boolean isInsideFootprint(int x, int z, BoundingBox box) {
        return x >= box.minX() && x <= box.maxX() && z >= box.minZ() && z <= box.maxZ();
    }

    /**
     * 到脚印边界的有符号水平距离：内部为负（到最近边的格数取负，边上为 0），外部为正。
     * 外部角区用欧氏距离，坡脚是圆角。
     */
    public static float signedDistanceToFootprintEdge(int x, int z, BoundingBox box) {
        if (isInsideFootprint(x, z, box)) {
            int distanceWest = x - box.minX();
            int distanceEast = box.maxX() - x;
            int distanceNorth = z - box.minZ();
            int distanceSouth = box.maxZ() - z;
            return -Math.min(Math.min(distanceWest, distanceEast), Math.min(distanceNorth, distanceSouth));
        }
        int outsideX = x < box.minX() ? box.minX() - x : Math.max(0, x - box.maxX());
        int outsideZ = z < box.minZ() ? box.minZ() - z : Math.max(0, z - box.maxZ());
        if (outsideX > 0 && outsideZ > 0) {
            return Mth.sqrt(outsideX * outsideX + outsideZ * outsideZ);
        }
        return Math.max(outsideX, outsideZ);
    }

    private static float smoothstep01(float progress) {
        float clamped = Mth.clamp(progress, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    /** 值噪声，输出约 [-1, 1)。只依赖世界坐标与盐，跨区块连续。 */
    private static float smoothNoise2D(int x, int z, int salt, float frequency) {
        float scaledX = x * frequency;
        float scaledZ = z * frequency;
        int cellX = Mth.floor(scaledX);
        int cellZ = Mth.floor(scaledZ);
        float fractionX = scaledX - cellX;
        float fractionZ = scaledZ - cellZ;
        float easedX = fractionX * fractionX * (3.0F - 2.0F * fractionX);
        float easedZ = fractionZ * fractionZ * (3.0F - 2.0F * fractionZ);
        float cornerNorthWest = hashCorner(cellX, cellZ, salt);
        float cornerNorthEast = hashCorner(cellX + 1, cellZ, salt);
        float cornerSouthWest = hashCorner(cellX, cellZ + 1, salt);
        float cornerSouthEast = hashCorner(cellX + 1, cellZ + 1, salt);
        float northRow = Mth.lerp(easedX, cornerNorthWest, cornerNorthEast);
        float southRow = Mth.lerp(easedX, cornerSouthWest, cornerSouthEast);
        return Mth.lerp(easedZ, northRow, southRow);
    }

    private static float hashCorner(int x, int z, int salt) {
        int hash = x * 374761393 ^ z * 668265263 ^ salt;
        hash = (hash << 13) ^ hash;
        hash = hash * (hash * hash * 15731 + 789221) + 1376312589;
        return ((hash >> 16) & 0xFFFF) / 32768.0F - 1.0F;
    }
}

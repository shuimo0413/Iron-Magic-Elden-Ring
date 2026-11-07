package com.eldenring.spells.worldgen.structure;

import com.eldenring.spells.EldenRingSpellsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 学院模板四条边「从边缘往里连续多少格是露天平地」——决定外侧高地能往院子里蔓延多深。
 * <p>
 * 必须从<b>模板数据</b>算而不是读世界：外侧列所在区块生成时，边缘内侧的列可能在 2 个区块外，
 * 世界里还没有方块。模板对每座学院都一样，所以整服只算一次（模板热重载后自动重算）。
 * <p>
 * 例：南边紧贴边缘有一条护城河，这一段露天深度为 0，外侧缓坡就不能指望院内垫高，
 * 改成在外侧自己把坡做完，不会在河岸留下一道台阶。
 */
public final class AcademyTemplateEdgeProfile {

    /**
     * 只看模板最底层几格内的地面与净空。地面约在模板 y=2，再加垫高检查高度足够。
     * 单位：方块。
     */
    private static final int SCAN_HEIGHT_BLOCKS = 32;

    /**
     * 模板地面最多认多厚（从 y=0 起连续自然地面）。单位：方块。
     */
    private static final int MAX_TEMPLATE_GROUND_THICKNESS_BLOCKS = 12;

    /**
     * 判定「露天」时要求模板地面上方净空的高度：最大垫高 + 头顶净空。
     * 调大 → 认定为可垫高的列更少；调小 → 可能垫到树冠 / 拱门下面。
     * 单位：方块。
     */
    static final int RAISE_CLEARANCE_BLOCKS = 16;

    /**
     * 沿边方向取最小值的半窗宽：某处往里深度要被左右这么多格内的最浅值限制，
     * 防止坡体侧向贴到旁边建筑。单位：方块。
     */
    private static final int LATERAL_CLEARANCE_BLOCKS = 4;

    /**
     * 沿边方向平滑的半窗宽：让允许深度沿边连续变化，坡面不出侧向台阶。单位：方块。
     */
    private static final int LATERAL_SMOOTHING_BLOCKS = 8;

    private static final byte CELL_OPEN = 0;
    private static final byte CELL_GROUND = 1;
    private static final byte CELL_BLOCKING = 2;

    private static final int SIDE_WEST = 0;
    private static final int SIDE_EAST = 1;
    private static final int SIDE_NORTH = 2;
    private static final int SIDE_SOUTH = 3;

    private static volatile AcademyTemplateEdgeProfile cachedProfile;

    /** 计算时用到的模板对象，用于判断是否被热重载替换。 */
    private final StructureTemplate sourceTemplate;
    private final int sizeX;
    private final int sizeZ;
    /** [side][沿边下标] → 允许向内推进的格数（已做侧向限制与平滑）。 */
    private final float[][] allowedDepthBySide;

    private AcademyTemplateEdgeProfile(StructureTemplate sourceTemplate, int sizeX, int sizeZ, float[][] allowedDepthBySide) {
        this.sourceTemplate = sourceTemplate;
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
        this.allowedDepthBySide = allowedDepthBySide;
    }

    /**
     * 取（必要时计算）当前模板的边缘露天剖面。读不到模板时返回 null，调用方应完全禁用内侧垫高。
     *
     * @param maxDepthBlocks 向内推进上限（与缓坡算法一致）
     */
    public static AcademyTemplateEdgeProfile getOrCompute(PiecesContainer pieces, BoundingBox footprint, int maxDepthBlocks) {
        StructureTemplate referenceTemplate = firstTemplate(pieces);
        if (referenceTemplate == null) {
            return null;
        }
        AcademyTemplateEdgeProfile profile = cachedProfile;
        if (profile != null && profile.sourceTemplate == referenceTemplate) {
            return profile;
        }
        synchronized (AcademyTemplateEdgeProfile.class) {
            profile = cachedProfile;
            if (profile != null && profile.sourceTemplate == referenceTemplate) {
                return profile;
            }
            profile = compute(pieces, footprint, referenceTemplate, maxDepthBlocks);
            if (profile != null) {
                cachedProfile = profile;
            }
            return profile;
        }
    }

    /**
     * 某列（世界坐标）所对应边段允许的向内推进格数。
     * 脚印内取最近的边；脚印外取所在一侧的边，角区取两条边端点的较小值。
     */
    public float allowedInnerDepth(int x, int z, BoundingBox footprint) {
        int localX = x - footprint.minX();
        int localZ = z - footprint.minZ();
        int clampedX = Math.clamp(localX, 0, sizeX - 1);
        int clampedZ = Math.clamp(localZ, 0, sizeZ - 1);
        boolean outsideX = localX < 0 || localX >= sizeX;
        boolean outsideZ = localZ < 0 || localZ >= sizeZ;
        int sideAlongX = localX < sizeX / 2 ? SIDE_WEST : SIDE_EAST;
        int sideAlongZ = localZ < sizeZ / 2 ? SIDE_NORTH : SIDE_SOUTH;
        if (outsideX && outsideZ) {
            return Math.min(allowedDepthBySide[sideAlongX][clampedZ], allowedDepthBySide[sideAlongZ][clampedX]);
        }
        if (outsideX) {
            return allowedDepthBySide[sideAlongX][clampedZ];
        }
        if (outsideZ) {
            return allowedDepthBySide[sideAlongZ][clampedX];
        }
        int distanceWest = localX;
        int distanceEast = sizeX - 1 - localX;
        int distanceNorth = localZ;
        int distanceSouth = sizeZ - 1 - localZ;
        int nearest = Math.min(Math.min(distanceWest, distanceEast), Math.min(distanceNorth, distanceSouth));
        if (nearest == distanceWest) {
            return allowedDepthBySide[SIDE_WEST][clampedZ];
        }
        if (nearest == distanceEast) {
            return allowedDepthBySide[SIDE_EAST][clampedZ];
        }
        if (nearest == distanceNorth) {
            return allowedDepthBySide[SIDE_NORTH][clampedX];
        }
        return allowedDepthBySide[SIDE_SOUTH][clampedX];
    }

    private static StructureTemplate firstTemplate(PiecesContainer pieces) {
        for (StructurePiece piece : pieces.pieces()) {
            if (piece instanceof AcademyPiece academyPiece) {
                return academyPiece.template();
            }
        }
        return null;
    }

    private static AcademyTemplateEdgeProfile compute(
            PiecesContainer pieces,
            BoundingBox footprint,
            StructureTemplate referenceTemplate,
            int maxDepthBlocks
    ) {
        Field palettesField;
        try {
            palettesField = StructureTemplate.class.getDeclaredField("palettes");
            palettesField.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            EldenRingSpellsMod.LOGGER.warn("学院边缘剖面：无法读取模板调色板，内侧垫高将关闭", exception);
            return null;
        }
        int sizeX = footprint.getXSpan();
        int sizeZ = footprint.getZSpan();
        byte[] cells = new byte[sizeX * sizeZ * SCAN_HEIGHT_BLOCKS];
        for (StructurePiece piece : pieces.pieces()) {
            if (!(piece instanceof AcademyPiece academyPiece)) {
                continue;
            }
            BlockPos pieceOrigin = academyPiece.templatePosition();
            int offsetY = pieceOrigin.getY() - footprint.minY();
            if (offsetY >= SCAN_HEIGHT_BLOCKS) {
                continue;
            }
            List<StructureTemplate.StructureBlockInfo> blockInfos;
            try {
                @SuppressWarnings("unchecked")
                List<StructureTemplate.Palette> palettes =
                        (List<StructureTemplate.Palette>) palettesField.get(academyPiece.template());
                if (palettes.isEmpty()) {
                    continue;
                }
                blockInfos = palettes.get(0).blocks();
            } catch (ReflectiveOperationException | RuntimeException exception) {
                EldenRingSpellsMod.LOGGER.warn("学院边缘剖面：读取模板失败，内侧垫高将关闭", exception);
                return null;
            }
            int offsetX = pieceOrigin.getX() - footprint.minX();
            int offsetZ = pieceOrigin.getZ() - footprint.minZ();
            for (StructureTemplate.StructureBlockInfo blockInfo : blockInfos) {
                int localX = offsetX + blockInfo.pos().getX();
                int localY = offsetY + blockInfo.pos().getY();
                int localZ = offsetZ + blockInfo.pos().getZ();
                if (localY < 0 || localY >= SCAN_HEIGHT_BLOCKS
                        || localX < 0 || localX >= sizeX || localZ < 0 || localZ >= sizeZ) {
                    continue;
                }
                cells[(localX * sizeZ + localZ) * SCAN_HEIGHT_BLOCKS + localY] = classify(blockInfo.state());
            }
        }

        boolean[] raisable = new boolean[sizeX * sizeZ];
        for (int localX = 0; localX < sizeX; localX++) {
            for (int localZ = 0; localZ < sizeZ; localZ++) {
                raisable[localX * sizeZ + localZ] = isRaisableColumn(cells, (localX * sizeZ + localZ) * SCAN_HEIGHT_BLOCKS);
            }
        }

        float[][] allowedDepthBySide = new float[4][];
        allowedDepthBySide[SIDE_WEST] = sideProfile(raisable, sizeX, sizeZ, sizeZ, maxDepthBlocks,
                (along, depth) -> depth * sizeZ + along);
        allowedDepthBySide[SIDE_EAST] = sideProfile(raisable, sizeX, sizeZ, sizeZ, maxDepthBlocks,
                (along, depth) -> (sizeX - 1 - depth) * sizeZ + along);
        allowedDepthBySide[SIDE_NORTH] = sideProfile(raisable, sizeX, sizeZ, sizeX, maxDepthBlocks,
                (along, depth) -> along * sizeZ + depth);
        allowedDepthBySide[SIDE_SOUTH] = sideProfile(raisable, sizeX, sizeZ, sizeX, maxDepthBlocks,
                (along, depth) -> along * sizeZ + (sizeZ - 1 - depth));
        return new AcademyTemplateEdgeProfile(referenceTemplate, sizeX, sizeZ, allowedDepthBySide);
    }

    private static byte classify(BlockState state) {
        if (AcademyTerrainBlender.isNaturalGround(state)) {
            return CELL_GROUND;
        }
        if (state.isAir() || (state.canBeReplaced() && state.getFluidState().isEmpty())) {
            return CELL_OPEN;
        }
        return CELL_BLOCKING;
    }

    /** 底层是自然地面、地面之上 {@link #RAISE_CLEARANCE_BLOCKS} 格全是露天，才允许垫高。 */
    private static boolean isRaisableColumn(byte[] cells, int columnBase) {
        if (cells[columnBase] != CELL_GROUND) {
            return false;
        }
        int groundTop = 0;
        while (groundTop + 1 < MAX_TEMPLATE_GROUND_THICKNESS_BLOCKS && cells[columnBase + groundTop + 1] == CELL_GROUND) {
            groundTop++;
        }
        int clearanceTop = Math.min(SCAN_HEIGHT_BLOCKS - 1, groundTop + RAISE_CLEARANCE_BLOCKS);
        for (int localY = groundTop + 1; localY <= clearanceTop; localY++) {
            if (cells[columnBase + localY] != CELL_OPEN) {
                return false;
            }
        }
        return true;
    }

    @FunctionalInterface
    private interface ColumnIndexer {
        int index(int along, int depth);
    }

    /**
     * 一条边：先求每个沿边位置从边缘往里连续可垫高的格数，
     * 再取侧向窗口最小值（避开旁边建筑），最后侧向平均（连续变化）。
     */
    private static float[] sideProfile(
            boolean[] raisable,
            int sizeX,
            int sizeZ,
            int sideLength,
            int maxDepthBlocks,
            ColumnIndexer indexer
    ) {
        int depthLimit = Math.min(maxDepthBlocks, Math.min(sizeX, sizeZ) / 2);
        int[] openDepth = new int[sideLength];
        for (int along = 0; along < sideLength; along++) {
            int depth = 0;
            while (depth < depthLimit && raisable[indexer.index(along, depth)]) {
                depth++;
            }
            openDepth[along] = depth;
        }
        // 最小值窗口 = 侧向净空 + 平滑半宽：之后再做 ±平滑半宽 的平均，
        // 结果在每一处都不超过「±侧向净空」内的最浅深度，同时沿边连续
        int minimumWindow = LATERAL_CLEARANCE_BLOCKS + LATERAL_SMOOTHING_BLOCKS;
        int[] clearedDepth = new int[sideLength];
        for (int along = 0; along < sideLength; along++) {
            int minimum = Integer.MAX_VALUE;
            for (int offset = -minimumWindow; offset <= minimumWindow; offset++) {
                int neighbor = Math.clamp(along + offset, 0, sideLength - 1);
                minimum = Math.min(minimum, openDepth[neighbor]);
            }
            clearedDepth[along] = minimum;
        }
        float[] smoothedDepth = new float[sideLength];
        for (int along = 0; along < sideLength; along++) {
            float sum = 0.0F;
            int count = 0;
            for (int offset = -LATERAL_SMOOTHING_BLOCKS; offset <= LATERAL_SMOOTHING_BLOCKS; offset++) {
                int neighbor = Math.clamp(along + offset, 0, sideLength - 1);
                sum += clearedDepth[neighbor];
                count++;
            }
            smoothedDepth[along] = sum / count;
        }
        return smoothedDepth;
    }
}

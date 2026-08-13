package com.eldenring.spells.entity;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户端弹道历史点环形缓冲。
 * <p>
 * 仅保存世界坐标，不含任何客户端专用类型，因此实体类在独立服务端加载也安全。
 * 每次记录后同时按最大点数与累计路径长度裁剪，避免“整条轨迹”无限增长。
 */
public final class TrailHistoryBuffer {
    /** 小于此距离（方块）的连续点不重复记录，减少近似重合顶点。 */
    private static final double MIN_SAMPLE_DISTANCE_BLOCKS = 0.08;

    /** 单 tick 跳跃超过此距离（方块）视为传送/网络校正，清空旧轨迹避免跨地图拉线。 */
    private static final double TELEPORT_RESET_DISTANCE_BLOCKS = 8.0;

    private final ArrayDeque<Vec3> worldPositions = new ArrayDeque<>();
    private double accumulatedLengthBlocks;

    /**
     * 记录新世界坐标，并按法术配置裁剪最旧路径。
     *
     * @param worldPosition 当前弹头世界坐标
     * @param maximumLengthBlocks 最长保留路径（方块）
     * @param maximumPointCount 最多保留点数
     */
    public void record(
            Vec3 worldPosition,
            double maximumLengthBlocks,
            int maximumPointCount
    ) {
        Vec3 newestPosition = worldPositions.peekLast();
        if (newestPosition != null) {
            double distanceBlocks = newestPosition.distanceTo(worldPosition);
            if (distanceBlocks > TELEPORT_RESET_DISTANCE_BLOCKS) {
                clear();
            } else if (distanceBlocks < MIN_SAMPLE_DISTANCE_BLOCKS) {
                return;
            } else {
                accumulatedLengthBlocks += distanceBlocks;
            }
        }

        worldPositions.addLast(worldPosition);
        trim(Math.max(0.1, maximumLengthBlocks), Math.max(2, maximumPointCount));
    }

    /**
     * 返回按“最旧 → 最新”排列的只读快照，供每帧 Renderer 使用。
     */
    public List<Vec3> snapshot() {
        return List.copyOf(new ArrayList<>(worldPositions));
    }

    public void clear() {
        worldPositions.clear();
        accumulatedLengthBlocks = 0.0;
    }

    private void trim(double maximumLengthBlocks, int maximumPointCount) {
        while (worldPositions.size() > maximumPointCount
                || (worldPositions.size() > 2 && accumulatedLengthBlocks > maximumLengthBlocks)) {
            Vec3 removedPosition = worldPositions.removeFirst();
            Vec3 nextPosition = worldPositions.peekFirst();
            if (nextPosition != null) {
                accumulatedLengthBlocks = Math.max(
                        0.0,
                        accumulatedLengthBlocks - removedPosition.distanceTo(nextPosition)
                );
            }
        }
    }
}

package com.eldenring.spells.client.render.glintstone;

import com.eldenring.spells.tuning.GlintstoneTrailTuning;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 辉石历史路径光轨绘制器。
 * <p>
 * 客户端实体逐 tick 保存真实世界坐标；本类把相邻历史点连接成始终朝向相机的
 * 自发光 ribbon，因此追踪转弯、抛射上扬与双螺旋都会留下实际曲线，而不是弹头后的直杆。
 * <p>
 * 只提交几何顶点，不沿路径生成粒子。一个 N 点轨迹每层仅 {@code (N-1)*4} 个顶点，
 * 内外两层仍远低于过去逐点生成并长期存活的粒子开销。
 */
public final class GlintstoneTrailRenderer {
    /** 自发光透明光带无需事件注册；RenderType 按纹理 ResourceLocation 自动缓存。 */
    private static final RenderType TRAIL_RENDER_TYPE =
            RenderType.entityTranslucentEmissive(GlintstoneCometModels.TRAIL_BEAM_TEXTURE);

    /** 相邻历史点距离小于此值时合并，避免最后插值点制造零长度四边形。 */
    private static final double MIN_RENDER_SEGMENT_LENGTH_BLOCKS = 0.025;

    private GlintstoneTrailRenderer() {
    }

    /**
     * 绘制“历史点 + 当前插值弹头”的完整曲线。
     *
     * @param poseStack 当前 EntityRenderer 的 PoseStack（原点已位于 renderOriginWorld）
     * @param renderOriginWorld 当前 PoseStack 原点对应的世界坐标
     * @param currentHeadWorld 当前帧插值后的弹头世界坐标
     * @param cameraWorld 相机世界坐标，用于计算 billboard ribbon 横向向量
     * @param historyWorldPositions 最旧 → 最新的客户端历史点
     */
    public static void renderHistoryRibbon(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Vec3 renderOriginWorld,
            Vec3 currentHeadWorld,
            Vec3 cameraWorld,
            List<Vec3> historyWorldPositions,
            GlintstoneTrailTuning.TrailStyle trailStyle,
            GlintstoneCometHeadDrawer.VisualStyle visualStyle
    ) {
        List<Vec3> renderPoints = smoothPolylineOnce(
                buildRenderPoints(historyWorldPositions, currentHeadWorld)
        );
        if (renderPoints.size() < 2) {
            return;
        }

        double[] cumulativeDistances = cumulativeDistances(renderPoints);
        double totalLengthBlocks = cumulativeDistances[cumulativeDistances.length - 1];
        if (totalLengthBlocks < MIN_RENDER_SEGMENT_LENGTH_BLOCKS) {
            return;
        }

        List<Vec3> sideDirections = buildStableSideDirections(renderPoints, cameraWorld);
        VertexConsumer consumer = bufferSource.getBuffer(TRAIL_RENDER_TYPE);
        Matrix4f poseMatrix = poseStack.last().pose();

        int glowColor = visualStyle.glowColorArgb();
        int coreColor = visualStyle.coreColorArgb();

        // 外层先画：宽、淡、柔边。
        putRibbonLayer(
                consumer,
                poseMatrix,
                renderOriginWorld,
                renderPoints,
                sideDirections,
                cumulativeDistances,
                totalLengthBlocks,
                trailStyle.tailHalfWidthBlocks() * GlintstoneTrailTuning.BEAM_OUTER_WIDTH_SCALE,
                trailStyle.headHalfWidthBlocks() * GlintstoneTrailTuning.BEAM_OUTER_WIDTH_SCALE,
                unpackRed(glowColor),
                unpackGreen(glowColor),
                unpackBlue(glowColor),
                (int) (unpackAlpha(glowColor) * GlintstoneTrailTuning.BEAM_OUTER_ALPHA_SCALE)
        );
        // 内层后画：窄、亮，形成连续蓝绿光芯。
        putRibbonLayer(
                consumer,
                poseMatrix,
                renderOriginWorld,
                renderPoints,
                sideDirections,
                cumulativeDistances,
                totalLengthBlocks,
                trailStyle.tailHalfWidthBlocks() * 0.48f,
                trailStyle.headHalfWidthBlocks() * 0.48f,
                unpackRed(coreColor),
                unpackGreen(coreColor),
                unpackBlue(coreColor),
                235
        );
    }

    /**
     * 复制历史点并追加当前插值弹头；过近时用当前点替换末点，保证无接缝。
     */
    private static List<Vec3> buildRenderPoints(List<Vec3> historyWorldPositions, Vec3 currentHeadWorld) {
        List<Vec3> points = new ArrayList<>(historyWorldPositions.size() + 1);
        for (Vec3 historyPosition : historyWorldPositions) {
            if (points.isEmpty()
                    || points.get(points.size() - 1).distanceTo(historyPosition)
                    >= MIN_RENDER_SEGMENT_LENGTH_BLOCKS) {
                points.add(historyPosition);
            }
        }

        if (points.isEmpty()) {
            points.add(currentHeadWorld);
        } else if (points.get(points.size() - 1).distanceTo(currentHeadWorld)
                < MIN_RENDER_SEGMENT_LENGTH_BLOCKS) {
            points.set(points.size() - 1, currentHeadWorld);
        } else {
            points.add(currentHeadWorld);
        }
        return points;
    }

    /**
     * 对逐 tick 折线做一次 Chaikin 圆角：每段生成 25% / 75% 两点，保留首尾。
     * 该算法不会像高阶样条那样在急弯处越界，尤其适合 28°/tick 的双螺旋轨迹。
     */
    private static List<Vec3> smoothPolylineOnce(List<Vec3> sourcePoints) {
        if (sourcePoints.size() < 3) {
            return sourcePoints;
        }
        List<Vec3> smoothedPoints = new ArrayList<>(sourcePoints.size() * 2);
        smoothedPoints.add(sourcePoints.get(0));
        for (int pointIndex = 0; pointIndex < sourcePoints.size() - 1; pointIndex++) {
            Vec3 start = sourcePoints.get(pointIndex);
            Vec3 end = sourcePoints.get(pointIndex + 1);
            smoothedPoints.add(start.scale(0.75).add(end.scale(0.25)));
            smoothedPoints.add(start.scale(0.25).add(end.scale(0.75)));
        }
        smoothedPoints.add(sourcePoints.get(sourcePoints.size() - 1));
        return smoothedPoints;
    }

    private static double[] cumulativeDistances(List<Vec3> points) {
        double[] distances = new double[points.size()];
        for (int pointIndex = 1; pointIndex < points.size(); pointIndex++) {
            distances[pointIndex] = distances[pointIndex - 1]
                    + points.get(pointIndex - 1).distanceTo(points.get(pointIndex));
        }
        return distances;
    }

    /**
     * 每个路径点按“轨迹切线 × 视线”计算横向方向，并在相邻点间保持符号一致，
     * 防止急转弯时 ribbon 突然翻面。
     */
    private static List<Vec3> buildStableSideDirections(List<Vec3> points, Vec3 cameraWorld) {
        List<Vec3> sideDirections = new ArrayList<>(points.size());
        Vec3 previousSideDirection = null;

        for (int pointIndex = 0; pointIndex < points.size(); pointIndex++) {
            Vec3 tangent;
            if (pointIndex == 0) {
                tangent = points.get(1).subtract(points.get(0));
            } else if (pointIndex == points.size() - 1) {
                tangent = points.get(pointIndex).subtract(points.get(pointIndex - 1));
            } else {
                tangent = points.get(pointIndex + 1).subtract(points.get(pointIndex - 1));
            }
            tangent = tangent.lengthSqr() > 1.0e-8 ? tangent.normalize() : new Vec3(0.0, 0.0, 1.0);

            Vec3 towardCamera = cameraWorld.subtract(points.get(pointIndex));
            Vec3 sideDirection = tangent.cross(towardCamera);
            if (sideDirection.lengthSqr() < 1.0e-8) {
                Vec3 fallbackAxis = Math.abs(tangent.y) < 0.92
                        ? new Vec3(0.0, 1.0, 0.0)
                        : new Vec3(1.0, 0.0, 0.0);
                sideDirection = tangent.cross(fallbackAxis);
            }
            sideDirection = sideDirection.normalize();

            if (previousSideDirection != null && sideDirection.dot(previousSideDirection) < 0.0) {
                sideDirection = sideDirection.scale(-1.0);
            }
            sideDirections.add(sideDirection);
            previousSideDirection = sideDirection;
        }
        return sideDirections;
    }

    private static void putRibbonLayer(
            VertexConsumer consumer,
            Matrix4f poseMatrix,
            Vec3 renderOriginWorld,
            List<Vec3> points,
            List<Vec3> sideDirections,
            double[] cumulativeDistances,
            double totalLengthBlocks,
            float tailHalfWidthBlocks,
            float headHalfWidthBlocks,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        int clampedAlpha = Mth.clamp(alpha, 0, 255);
        for (int segmentIndex = 0; segmentIndex < points.size() - 1; segmentIndex++) {
            double startProgress = cumulativeDistances[segmentIndex] / totalLengthBlocks;
            double endProgress = cumulativeDistances[segmentIndex + 1] / totalLengthBlocks;
            float startWidth = Mth.lerp((float) startProgress, tailHalfWidthBlocks, headHalfWidthBlocks);
            float endWidth = Mth.lerp((float) endProgress, tailHalfWidthBlocks, headHalfWidthBlocks);

            Vec3 startCenter = points.get(segmentIndex).subtract(renderOriginWorld);
            Vec3 endCenter = points.get(segmentIndex + 1).subtract(renderOriginWorld);
            Vec3 startSide = sideDirections.get(segmentIndex).scale(startWidth);
            Vec3 endSide = sideDirections.get(segmentIndex + 1).scale(endWidth);

            Vec3 startLeft = startCenter.subtract(startSide);
            Vec3 startRight = startCenter.add(startSide);
            Vec3 endLeft = endCenter.subtract(endSide);
            Vec3 endRight = endCenter.add(endSide);

            // 纹理 V=1 是最旧尾端，V=0 是当前弹头；全路径只淡出一次，不按段重复。
            float startV = 1.0f - (float) startProgress;
            float endV = 1.0f - (float) endProgress;
            putTexturedVertex(consumer, poseMatrix, startLeft, 0.0f, startV, red, green, blue, clampedAlpha);
            putTexturedVertex(consumer, poseMatrix, startRight, 1.0f, startV, red, green, blue, clampedAlpha);
            putTexturedVertex(consumer, poseMatrix, endRight, 1.0f, endV, red, green, blue, clampedAlpha);
            putTexturedVertex(consumer, poseMatrix, endLeft, 0.0f, endV, red, green, blue, clampedAlpha);
        }
    }

    private static void putTexturedVertex(
            VertexConsumer consumer,
            Matrix4f poseMatrix,
            Vec3 localPosition,
            float u,
            float v,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        consumer.addVertex(
                        poseMatrix,
                        (float) localPosition.x,
                        (float) localPosition.y,
                        (float) localPosition.z
                )
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 1.0f, 0.0f);
    }

    private static int unpackRed(int argb) {
        return (argb >> 16) & 0xFF;
    }

    private static int unpackGreen(int argb) {
        return (argb >> 8) & 0xFF;
    }

    private static int unpackBlue(int argb) {
        return argb & 0xFF;
    }

    private static int unpackAlpha(int argb) {
        return (argb >> 24) & 0xFF;
    }
}

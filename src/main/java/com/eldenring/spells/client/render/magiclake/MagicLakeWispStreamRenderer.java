package com.eldenring.spells.client.render.magiclake;

import com.eldenring.spells.EldenRingSpellsMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 魔法之湖迷幻光带的网格绘制：一条路径画成「几股相互缠绕的宽烟丝 + 一层更宽的淡雾」。
 * <p>
 * 和辉石弹道光轨（{@code GlintstoneTrailRenderer}）的区别：没有亮芯，两端都淡出，宽度中段最粗，
 * 贴图是沿路径平铺并缓慢滚动的烟丝纹理（{@code wisp_stream.png}，由 {@code 工具链/gen_magic_lake_wisp_texture.py} 生成）。
 * 全部走加法混合：多股重叠处自然更亮，没有半透明排序问题；白天背景亮时基本看不见（预期）。
 * <p>
 * 每个路径点的横向展开方向 = 切线 × 视线，并与上一个点保持同号，急弯处不翻面。
 */
public final class MagicLakeWispStreamRenderer {

    public static final ResourceLocation WISP_STREAM_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EldenRingSpellsMod.MOD_ID,
            "textures/entity/magic_lake/wisp_stream.png"
    );

    /** 加法 + 线性过滤（烟雾要柔，不要像素锯齿）。只写颜色，不挡后面的东西。 */
    private static final RenderType WISP_STREAM_ADDITIVE = RenderType.create(
            "iss_elden_ring_magic_lake_wisp_stream",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            65536,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(WISP_STREAM_TEXTURE, true, false))
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false)
    );

    /** 烟丝股数。调大 → 更蓬松，但顶点数线性增加。 */
    private static final int STRAND_COUNT = 3;
    /** 每股烟丝在中段的半宽（方块），再乘每条光带自己的体型系数。 */
    private static final float STRAND_HALF_WIDTH_BLOCKS = 0.55f;
    /** 外层淡雾的半宽（方块）。调大 → 光带周围的蓝色晕更开。 */
    private static final float HAZE_HALF_WIDTH_BLOCKS = 1.7f;
    /** 两端宽度占中段的比例（0–1）。调小 → 两端更尖。 */
    private static final float END_WIDTH_FRACTION = 0.35f;

    /** 烟丝绕中心线的缠绕半径（方块），中段最大，两端收拢。 */
    private static final double BRAID_RADIUS_BLOCKS = 0.75;
    /** 缠绕半径中竖直方向的比例。横向（屏幕内）为 1，竖直给一点起伏，不全贴在一个平面里。 */
    private static final double BRAID_VERTICAL_SCALE = 0.55;
    /** 沿路径的缠绕扭率（弧度 / 方块）。0.28 ≈ 每 22 格转一整圈。调大 → 拧得更紧。 */
    private static final double BRAID_TWIST_RADIANS_PER_BLOCK = 0.28;
    /** 缠绕整体随时间转动（弧度 / tick）。调大 → 烟丝翻滚更快。 */
    private static final double BRAID_SPIN_RADIANS_PER_TICK = 0.025;

    /** 贴图沿路径每重复一次的长度（方块）。调小 → 烟丝纹理更密。 */
    private static final double TEXTURE_REPEAT_LENGTH_BLOCKS = 9.0;
    /** 贴图 V 的滚动速度（贴图长度 / tick）。烟丝相对光带缓慢流动。 */
    private static final double TEXTURE_SCROLL_PER_TICK = 0.006;

    /** 尾端淡入占全长的比例（从尾巴到这里亮度从 0 升到满）。调大 → 尾巴更长更虚。 */
    private static final float TAIL_FADE_FRACTION = 0.45f;
    /** 头端淡出占全长的比例。头部也是软的，不像弹道那样有亮头。 */
    private static final float HEAD_FADE_FRACTION = 0.18f;

    /** 几股烟丝的颜色（RGB），轮流使用：幽蓝 / 浅冰蓝 / 深一点的蓝。 */
    private static final int[] STRAND_COLORS_RGB = {0x6FA6FF, 0x9CCBFF, 0x5687F0};
    /** 烟丝最大 alpha（0–255）。加法混合下即亮度，调小 → 更「淡淡的」。 */
    private static final int STRAND_MAX_ALPHA = 120;
    /** 外层淡雾颜色（RGB）。 */
    private static final int HAZE_COLOR_RGB = 0x3F6FD8;
    /** 外层淡雾最大 alpha（0–255）。 */
    private static final int HAZE_MAX_ALPHA = 46;

    /** 相邻路径点太近时合并（方块），避免零长度段算不出切线。 */
    private static final double MIN_SEGMENT_LENGTH_BLOCKS = 0.02;

    private MagicLakeWispStreamRenderer() {
    }

    /**
     * 画一条光带。
     *
     * @param poseStack          原点即相机的 PoseStack（{@code RenderLevelStageEvent} 给的）
     * @param pathWorldPositions 最旧（尾）→ 最新（头）的世界坐标
     * @param cameraWorld        相机世界坐标，同时作为顶点的相对原点（在 double 里相减，远离世界原点不抖）
     * @param animationTicks     光带年龄 + partialTick，驱动缠绕旋转与贴图滚动
     * @param sizeScale          体型系数，乘到所有宽度和缠绕半径上
     * @param brightness         整体亮度（0–1）：生命周期淡入淡出 × 湖区混合系数
     */
    public static void renderStream(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            List<Vec3> pathWorldPositions,
            Vec3 cameraWorld,
            float animationTicks,
            float sizeScale,
            float brightness
    ) {
        List<Vec3> centerPoints = dropTinySegments(pathWorldPositions);
        int pointCount = centerPoints.size();
        if (pointCount < 3) {
            return;
        }
        double[] distanceFromTail = new double[pointCount];
        for (int pointIndex = 1; pointIndex < pointCount; pointIndex++) {
            distanceFromTail[pointIndex] = distanceFromTail[pointIndex - 1]
                    + centerPoints.get(pointIndex - 1).distanceTo(centerPoints.get(pointIndex));
        }
        double totalLengthBlocks = distanceFromTail[pointCount - 1];
        if (totalLengthBlocks < 0.5) {
            return;
        }

        List<Vec3> sideDirections = buildStableSideDirections(centerPoints, cameraWorld);
        float[] progress = new float[pointCount];
        float[] envelope = new float[pointCount];
        float[] widthProfile = new float[pointCount];
        float[] textureV = new float[pointCount];
        for (int pointIndex = 0; pointIndex < pointCount; pointIndex++) {
            float tailToHead = (float) (distanceFromTail[pointIndex] / totalLengthBlocks);
            progress[pointIndex] = tailToHead;
            envelope[pointIndex] = smoothstep(0.0f, TAIL_FADE_FRACTION, tailToHead)
                    * smoothstep(1.0f, 1.0f - HEAD_FADE_FRACTION, tailToHead);
            float middleBulge = Mth.sin(tailToHead * Mth.PI);
            widthProfile[pointIndex] = END_WIDTH_FRACTION + (1.0f - END_WIDTH_FRACTION) * middleBulge;
            double distanceFromHead = totalLengthBlocks - distanceFromTail[pointIndex];
            textureV[pointIndex] = (float) (distanceFromHead / TEXTURE_REPEAT_LENGTH_BLOCKS
                    + animationTicks * TEXTURE_SCROLL_PER_TICK);
        }

        VertexConsumer consumer = bufferSource.getBuffer(WISP_STREAM_ADDITIVE);
        Matrix4f poseMatrix = poseStack.last().pose();

        putStrip(
                consumer, poseMatrix, cameraWorld, centerPoints, sideDirections,
                HAZE_HALF_WIDTH_BLOCKS * sizeScale, widthProfile, envelope, textureV,
                HAZE_COLOR_RGB, HAZE_MAX_ALPHA * brightness
        );

        double braidSpinRadians = animationTicks * BRAID_SPIN_RADIANS_PER_TICK;
        for (int strandIndex = 0; strandIndex < STRAND_COUNT; strandIndex++) {
            double strandPhaseRadians = Math.PI * 2.0 * strandIndex / STRAND_COUNT + braidSpinRadians;
            List<Vec3> strandPoints = new ArrayList<>(pointCount);
            for (int pointIndex = 0; pointIndex < pointCount; pointIndex++) {
                double distanceFromHead = totalLengthBlocks - distanceFromTail[pointIndex];
                double braidAngle = strandPhaseRadians + distanceFromHead * BRAID_TWIST_RADIANS_PER_BLOCK;
                double braidRadius = BRAID_RADIUS_BLOCKS * sizeScale * widthProfile[pointIndex];
                strandPoints.add(centerPoints.get(pointIndex)
                        .add(sideDirections.get(pointIndex).scale(Math.cos(braidAngle) * braidRadius))
                        .add(0.0, Math.sin(braidAngle) * braidRadius * BRAID_VERTICAL_SCALE, 0.0));
            }
            float[] strandTextureV = new float[pointCount];
            for (int pointIndex = 0; pointIndex < pointCount; pointIndex++) {
                strandTextureV[pointIndex] = textureV[pointIndex] + strandIndex * 0.37f;
            }
            putStrip(
                    consumer, poseMatrix, cameraWorld, strandPoints, sideDirections,
                    STRAND_HALF_WIDTH_BLOCKS * sizeScale, widthProfile, envelope, strandTextureV,
                    STRAND_COLORS_RGB[strandIndex % STRAND_COLORS_RGB.length], STRAND_MAX_ALPHA * brightness
            );
        }
    }

    /**
     * 沿一串中心点铺四边形带。宽度 = 基础半宽 × 宽度轮廓；alpha = 最大 alpha × 两端淡出包络。
     * 横向方向沿用中心线的方向（几股烟丝几乎平行，不必各算一遍）。
     */
    private static void putStrip(
            VertexConsumer consumer,
            Matrix4f poseMatrix,
            Vec3 cameraWorld,
            List<Vec3> stripCenters,
            List<Vec3> sideDirections,
            float baseHalfWidthBlocks,
            float[] widthProfile,
            float[] envelope,
            float[] textureV,
            int colorRgb,
            float maxAlpha
    ) {
        int red = (colorRgb >> 16) & 0xFF;
        int green = (colorRgb >> 8) & 0xFF;
        int blue = colorRgb & 0xFF;
        for (int pointIndex = 0; pointIndex < stripCenters.size() - 1; pointIndex++) {
            int nextIndex = pointIndex + 1;
            int startAlpha = Mth.clamp((int) (maxAlpha * envelope[pointIndex]), 0, 255);
            int endAlpha = Mth.clamp((int) (maxAlpha * envelope[nextIndex]), 0, 255);
            if (startAlpha == 0 && endAlpha == 0) {
                continue;
            }
            Vec3 startCenter = stripCenters.get(pointIndex).subtract(cameraWorld);
            Vec3 endCenter = stripCenters.get(nextIndex).subtract(cameraWorld);
            Vec3 startSide = sideDirections.get(pointIndex).scale(baseHalfWidthBlocks * widthProfile[pointIndex]);
            Vec3 endSide = sideDirections.get(nextIndex).scale(baseHalfWidthBlocks * widthProfile[nextIndex]);

            putVertex(consumer, poseMatrix, startCenter.subtract(startSide), 0.0f, textureV[pointIndex], red, green, blue, startAlpha);
            putVertex(consumer, poseMatrix, startCenter.add(startSide), 1.0f, textureV[pointIndex], red, green, blue, startAlpha);
            putVertex(consumer, poseMatrix, endCenter.add(endSide), 1.0f, textureV[nextIndex], red, green, blue, endAlpha);
            putVertex(consumer, poseMatrix, endCenter.subtract(endSide), 0.0f, textureV[nextIndex], red, green, blue, endAlpha);
        }
    }

    private static void putVertex(
            VertexConsumer consumer,
            Matrix4f poseMatrix,
            Vec3 cameraRelativePosition,
            float u,
            float v,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        consumer.addVertex(
                        poseMatrix,
                        (float) cameraRelativePosition.x,
                        (float) cameraRelativePosition.y,
                        (float) cameraRelativePosition.z
                )
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 1.0f, 0.0f);
    }

    private static List<Vec3> dropTinySegments(List<Vec3> sourcePoints) {
        List<Vec3> keptPoints = new ArrayList<>(sourcePoints.size());
        for (Vec3 sourcePoint : sourcePoints) {
            if (keptPoints.isEmpty()
                    || keptPoints.get(keptPoints.size() - 1).distanceTo(sourcePoint) >= MIN_SEGMENT_LENGTH_BLOCKS) {
                keptPoints.add(sourcePoint);
            }
        }
        return keptPoints;
    }

    /**
     * 横向方向 = 切线 × 指向相机，单位化后与上一个点保持同号，避免急弯处带子翻面。
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

            Vec3 sideDirection = tangent.cross(cameraWorld.subtract(points.get(pointIndex)));
            if (sideDirection.lengthSqr() < 1.0e-8) {
                Vec3 fallbackAxis = Math.abs(tangent.y) < 0.92 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
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

    private static float smoothstep(float edge0, float edge1, float value) {
        float normalized = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return normalized * normalized * (3.0f - 2.0f * normalized);
    }
}

package com.eldenring.spells.client.render.glintstone;

import com.eldenring.spells.client.render.ProjectileOrientation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 可复用的辉石「彗星头」绘制器：立体晶核 + 朝向相机的青色光晕。
 * <p>
 * 其它辉石弹道只需 bake {@link GlintstoneCometModels#COMET_HEAD_LAYER}（或帚星刺簇层），
 * 再调用 {@link #render(Entity, float, PoseStack, MultiBufferSource, ModelPart, EntityRenderDispatcher, VisualStyle)}。
 */
public final class GlintstoneCometHeadDrawer {
    private GlintstoneCometHeadDrawer() {
    }

    /**
     * 颜色与尺寸参数；各法术可自定义一份，不必改绘制逻辑。
     *
     * @param bodyScaleRadial            垂直于飞行方向的缩放；调大 → 更胖
     * @param bodyScaleAlongFlight       沿飞行轴（模型 -Z）的缩放；调大 → 更长
     * @param glowScale                  相机朝向光晕基础缩放
     * @param glowPulseAmplitude         光晕呼吸振幅
     * @param glowSpinDegreesPerTick     光晕绕视线旋转（度 / tick）
     * @param glowAlongFlightScale       沿飞行方向的光晕拉伸倍率；1 = 球形
     * @param coreColorArgb              晶核 / 刺簇核心颜色
     * @param glowColorArgb              光晕颜色
     * @param spikeColorArgb             刺簇尖刺颜色；非刺簇时与核心相同
     * @param usesSpikedCrystalCluster   true 时按 core/spikes 两组分别着色
     * @param clusterSpinDegreesPerTick  刺簇绕飞行轴自转（度 / tick）；0 = 不转
     */
    public record VisualStyle(
            float bodyScaleRadial,
            float bodyScaleAlongFlight,
            float glowScale,
            float glowPulseAmplitude,
            float glowSpinDegreesPerTick,
            float glowAlongFlightScale,
            int coreColorArgb,
            int glowColorArgb,
            int spikeColorArgb,
            ResourceLocation bodyTexture,
            ResourceLocation glowTexture,
            boolean usesSpikedCrystalCluster,
            float clusterSpinDegreesPerTick
    ) {
        /** 兼容旧调用：均匀缩放时的等效整体缩放。 */
        public float bodyScale() {
            return bodyScaleRadial;
        }

        /** 辉石魔砾默认外观（饱和蓝绿菱形晶核 + 自发光晕）。 */
        public static VisualStyle glintstonePebbleDefault() {
            return fromFloatColors(
                    0.42f, 0.78f, 0.10f, 18.0f,
                    0.12f, 0.78f, 1.0f,
                    0.10f, 0.82f, 1.0f, 1.0f
            );
        }

        /**
         * 用 0–1 浮点色构造均匀缩放样式（流星 / 旋飞等未改剪影的弹道）。
         */
        public static VisualStyle fromFloatColors(
                float bodyScale,
                float glowScale,
                float glowPulseAmplitude,
                float glowSpinDegreesPerTick,
                float coreRed, float coreGreen, float coreBlue,
                float glowRed, float glowGreen, float glowBlue, float glowAlpha
        ) {
            return anisotropic(
                    bodyScale,
                    bodyScale,
                    glowScale,
                    glowPulseAmplitude,
                    glowSpinDegreesPerTick,
                    1.0f,
                    coreRed, coreGreen, coreBlue,
                    glowRed, glowGreen, glowBlue, glowAlpha
            );
        }

        /**
         * 各向异性菱形晶核（魔砾 / 迅魔砾 / 大魔砾 / 辉石彗星）。
         *
         * @param glowAlongFlightScale 1 = 球形光晕；大于 1 沿速度拉成残影
         */
        public static VisualStyle anisotropic(
                float bodyScaleRadial,
                float bodyScaleAlongFlight,
                float glowScale,
                float glowPulseAmplitude,
                float glowSpinDegreesPerTick,
                float glowAlongFlightScale,
                float coreRed, float coreGreen, float coreBlue,
                float glowRed, float glowGreen, float glowBlue, float glowAlpha
        ) {
            int coreColor = packRgb(coreRed, coreGreen, coreBlue, 1.0f);
            return new VisualStyle(
                    bodyScaleRadial,
                    bodyScaleAlongFlight,
                    glowScale,
                    glowPulseAmplitude,
                    glowSpinDegreesPerTick,
                    glowAlongFlightScale,
                    coreColor,
                    packRgb(glowRed, glowGreen, glowBlue, glowAlpha),
                    coreColor,
                    GlintstoneCometModels.COMET_HEAD_TEXTURE,
                    GlintstoneCometModels.COMET_GLOW_TEXTURE,
                    false,
                    0.0f
            );
        }

        /**
         * 帚星带刺晶簇：核更深、刺更亮，绕飞行轴慢转。
         */
        public static VisualStyle spikedCluster(
                float bodyScale,
                float glowScale,
                float glowPulseAmplitude,
                float glowSpinDegreesPerTick,
                float glowAlongFlightScale,
                float clusterSpinDegreesPerTick,
                float coreRed, float coreGreen, float coreBlue,
                float spikeRed, float spikeGreen, float spikeBlue,
                float glowRed, float glowGreen, float glowBlue, float glowAlpha
        ) {
            return new VisualStyle(
                    bodyScale,
                    bodyScale,
                    glowScale,
                    glowPulseAmplitude,
                    glowSpinDegreesPerTick,
                    glowAlongFlightScale,
                    packRgb(coreRed, coreGreen, coreBlue, 1.0f),
                    packRgb(glowRed, glowGreen, glowBlue, glowAlpha),
                    packRgb(spikeRed, spikeGreen, spikeBlue, 1.0f),
                    GlintstoneCometModels.COMET_HEAD_TEXTURE,
                    GlintstoneCometModels.COMET_GLOW_TEXTURE,
                    true,
                    clusterSpinDegreesPerTick
            );
        }

        private static int packRgb(float red, float green, float blue, float alpha) {
            int a = Mth.clamp((int) (alpha * 255.0f), 0, 255);
            int r = Mth.clamp((int) (red * 255.0f), 0, 255);
            int g = Mth.clamp((int) (green * 255.0f), 0, 255);
            int b = Mth.clamp((int) (blue * 255.0f), 0, 255);
            return (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    public static void render(
            Entity entity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            ModelPart cometBodyRoot,
            EntityRenderDispatcher entityRenderDispatcher,
            VisualStyle visualStyle
    ) {
        renderCrystalBody(entity, partialTicks, poseStack, bufferSource, cometBodyRoot, visualStyle);
        renderCameraFacingGlow(entity, partialTicks, poseStack, bufferSource, entityRenderDispatcher, visualStyle);
        renderFlightAlignedGlow(entity, partialTicks, poseStack, bufferSource, visualStyle);
    }

    /**
     * 按显式方向绘制彗星头。旋飞魔砾的单颗切向速度与中心实体速度不同，必须用本入口，
     * 否则晶核会朝中心轴、光轨却朝切线，视觉上就会“彗星歪了”。
     */
    public static void renderAlongDirection(
            Entity entity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            ModelPart cometBodyRoot,
            EntityRenderDispatcher entityRenderDispatcher,
            VisualStyle visualStyle,
            Vec3 flightDirectionWorld
    ) {
        renderCrystalBodyAlongDirection(
                poseStack,
                bufferSource,
                cometBodyRoot,
                visualStyle,
                flightDirectionWorld,
                entity.tickCount + partialTicks
        );
        renderCameraFacingGlow(entity, partialTicks, poseStack, bufferSource, entityRenderDispatcher, visualStyle);
        renderFlightAlignedGlowAlongDirection(
                poseStack,
                bufferSource,
                visualStyle,
                flightDirectionWorld,
                entity.tickCount + partialTicks
        );
    }

    /**
     * 沿速度方向画立体晶核（尖端朝前）。
     */
    private static void renderCrystalBody(
            Entity entity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            ModelPart cometBodyRoot,
            VisualStyle visualStyle
    ) {
        poseStack.pushPose();
        ProjectileOrientation.alignPoseToDeltaMovement(poseStack, entity, partialTicks);
        applyBodyScaleAndSpin(poseStack, visualStyle, entity.tickCount + partialTicks);
        renderCrystalMesh(poseStack, bufferSource, cometBodyRoot, visualStyle);
        poseStack.popPose();
    }

    /**
     * 沿传入世界方向画立体晶核；与连续光轨共用同一方向计算。
     */
    private static void renderCrystalBodyAlongDirection(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            ModelPart cometBodyRoot,
            VisualStyle visualStyle,
            Vec3 flightDirectionWorld,
            float animationTicks
    ) {
        poseStack.pushPose();
        ProjectileOrientation.alignPoseToDirection(poseStack, flightDirectionWorld);
        applyBodyScaleAndSpin(poseStack, visualStyle, animationTicks);
        renderCrystalMesh(poseStack, bufferSource, cometBodyRoot, visualStyle);
        poseStack.popPose();
    }

    /**
     * 径向 / 轴向缩放；帚星再绕飞行轴慢转，让侧面也能看见刺。
     */
    private static void applyBodyScaleAndSpin(PoseStack poseStack, VisualStyle visualStyle, float animationTicks) {
        poseStack.scale(
                visualStyle.bodyScaleRadial(),
                visualStyle.bodyScaleRadial(),
                visualStyle.bodyScaleAlongFlight()
        );
        if (visualStyle.clusterSpinDegreesPerTick() != 0.0f) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(animationTicks * visualStyle.clusterSpinDegreesPerTick()));
        }
    }

    private static void renderCrystalMesh(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            ModelPart cometBodyRoot,
            VisualStyle visualStyle
    ) {
        VertexConsumer bodyConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(visualStyle.bodyTexture())
        );
        if (visualStyle.usesSpikedCrystalCluster()) {
            cometBodyRoot.getChild(GlintstoneCometModels.SPIKED_CLUSTER_CORE_PART).render(
                    poseStack,
                    bodyConsumer,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    visualStyle.coreColorArgb()
            );
            cometBodyRoot.getChild(GlintstoneCometModels.SPIKED_CLUSTER_SPIKES_PART).render(
                    poseStack,
                    bodyConsumer,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    visualStyle.spikeColorArgb()
            );
            return;
        }
        cometBodyRoot.render(
                poseStack,
                bodyConsumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                visualStyle.coreColorArgb()
        );
    }

    /**
     * 相机朝向的柔光晕：内层亮核 + 外层更大的自发光晕。
     * 沿飞行拉伸时略缩小球形光晕，避免把拉长残影重新吃成圆斑。
     */
    private static void renderCameraFacingGlow(
            Entity entity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            EntityRenderDispatcher entityRenderDispatcher,
            VisualStyle visualStyle
    ) {
        float age = entity.tickCount + partialTicks;
        float alongStretch = Math.max(1.0f, visualStyle.glowAlongFlightScale());
        float sphericalScale = alongStretch > 1.05f
                ? visualStyle.glowScale() * 0.82f
                : visualStyle.glowScale();
        float pulseScale = sphericalScale + Mth.sin(age * 0.45f) * visualStyle.glowPulseAmplitude();

        int glowColor = visualStyle.glowColorArgb();
        int red = (glowColor >> 16) & 0xFF;
        int green = (glowColor >> 8) & 0xFF;
        int blue = glowColor & 0xFF;
        int alpha = (glowColor >> 24) & 0xFF;

        renderGlowShell(
                poseStack,
                bufferSource,
                entityRenderDispatcher,
                visualStyle,
                pulseScale * 1.55f,
                age * visualStyle.glowSpinDegreesPerTick() * 0.55f,
                red, green, blue,
                (int) (alpha * 0.42f)
        );
        renderGlowShell(
                poseStack,
                bufferSource,
                entityRenderDispatcher,
                visualStyle,
                pulseScale,
                age * visualStyle.glowSpinDegreesPerTick(),
                red, green, blue,
                alpha
        );
    }

    /**
     * 沿速度拉长的残影光斑：两张含飞行轴的交叉面片，避免所有弹头都是圆光球。
     */
    private static void renderFlightAlignedGlow(
            Entity entity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            VisualStyle visualStyle
    ) {
        if (visualStyle.glowAlongFlightScale() <= 1.05f) {
            return;
        }
        poseStack.pushPose();
        ProjectileOrientation.alignPoseToDeltaMovement(poseStack, entity, partialTicks);
        renderFlightAlignedGlowQuads(poseStack, bufferSource, visualStyle, entity.tickCount + partialTicks);
        poseStack.popPose();
    }

    private static void renderFlightAlignedGlowAlongDirection(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            VisualStyle visualStyle,
            Vec3 flightDirectionWorld,
            float animationTicks
    ) {
        if (visualStyle.glowAlongFlightScale() <= 1.05f) {
            return;
        }
        poseStack.pushPose();
        ProjectileOrientation.alignPoseToDirection(poseStack, flightDirectionWorld);
        renderFlightAlignedGlowQuads(poseStack, bufferSource, visualStyle, animationTicks);
        poseStack.popPose();
    }

    private static void renderFlightAlignedGlowQuads(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            VisualStyle visualStyle,
            float animationTicks
    ) {
        float pulse = visualStyle.glowScale()
                + Mth.sin(animationTicks * 0.45f) * visualStyle.glowPulseAmplitude() * 0.5f;
        poseStack.scale(
                pulse * 0.62f,
                pulse * 0.62f,
                pulse * visualStyle.glowAlongFlightScale()
        );

        int glowColor = visualStyle.glowColorArgb();
        int red = (glowColor >> 16) & 0xFF;
        int green = (glowColor >> 8) & 0xFF;
        int blue = glowColor & 0xFF;
        int alpha = (int) (((glowColor >> 24) & 0xFF) * 0.70f);

        VertexConsumer glowConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(visualStyle.glowTexture())
        );
        putGlowQuad(glowConsumer, poseStack.last().pose(), red, green, blue, alpha, 0.0f);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));
        putGlowQuad(glowConsumer, poseStack.last().pose(), red, green, blue, (int) (alpha * 0.75f), 0.0f);
    }

    /**
     * 绘制一层相机朝向的自发光晕壳（交叉双四边形）。
     */
    private static void renderGlowShell(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            EntityRenderDispatcher entityRenderDispatcher,
            VisualStyle visualStyle,
            float scale,
            float spinDegrees,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotationDegrees(spinDegrees));

        VertexConsumer glowConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(visualStyle.glowTexture())
        );

        Matrix4f poseMatrix = poseStack.last().pose();
        putGlowQuad(glowConsumer, poseMatrix, red, green, blue, alpha, 0.0f);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));
        Matrix4f crossedPoseMatrix = poseStack.last().pose();
        putGlowQuad(glowConsumer, crossedPoseMatrix, red, green, blue, (int) (alpha * 0.7f), 0.0f);

        poseStack.popPose();
    }

    private static void putGlowQuad(
            VertexConsumer consumer,
            Matrix4f poseMatrix,
            int red,
            int green,
            int blue,
            int alpha,
            float zOffset
    ) {
        consumer.addVertex(poseMatrix, -1.0f, -1.0f, zOffset)
                .setColor(red, green, blue, alpha)
                .setUv(0.0f, 1.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(poseMatrix, 1.0f, -1.0f, zOffset)
                .setColor(red, green, blue, alpha)
                .setUv(1.0f, 1.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(poseMatrix, 1.0f, 1.0f, zOffset)
                .setColor(red, green, blue, alpha)
                .setUv(1.0f, 0.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(poseMatrix, -1.0f, 1.0f, zOffset)
                .setColor(red, green, blue, alpha)
                .setUv(0.0f, 0.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 1.0f, 0.0f);
    }
}

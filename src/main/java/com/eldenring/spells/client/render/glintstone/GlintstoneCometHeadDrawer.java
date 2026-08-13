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
 * 其它辉石弹道只需 bake {@link GlintstoneCometModels#COMET_HEAD_LAYER}，
 * 再调用 {@link #render(Entity, float, PoseStack, MultiBufferSource, ModelPart, EntityRenderDispatcher, VisualStyle)}。
 */
public final class GlintstoneCometHeadDrawer {
    private GlintstoneCometHeadDrawer() {
    }

    /**
     * 颜色与尺寸参数；各法术可自定义一份，不必改绘制逻辑。
     */
    public record VisualStyle(
            float bodyScale,
            float glowScale,
            float glowPulseAmplitude,
            float glowSpinDegreesPerTick,
            int coreColorArgb,
            int glowColorArgb,
            ResourceLocation bodyTexture,
            ResourceLocation glowTexture
    ) {
        /** 辉石魔砾默认外观（饱和蓝绿菱形晶核 + 自发光晕）。 */
        public static VisualStyle glintstonePebbleDefault() {
            return fromFloatColors(
                    0.42f, 0.78f, 0.10f, 18.0f,
                    0.12f, 0.78f, 1.0f,
                    0.10f, 0.82f, 1.0f, 1.0f
            );
        }

        /**
         * 用 0–1 浮点色构造视觉样式（各辉石 Tuning 的便捷入口）。
         */
        public static VisualStyle fromFloatColors(
                float bodyScale,
                float glowScale,
                float glowPulseAmplitude,
                float glowSpinDegreesPerTick,
                float coreRed, float coreGreen, float coreBlue,
                float glowRed, float glowGreen, float glowBlue, float glowAlpha
        ) {
            return new VisualStyle(
                    bodyScale,
                    glowScale,
                    glowPulseAmplitude,
                    glowSpinDegreesPerTick,
                    packRgb(coreRed, coreGreen, coreBlue, 1.0f),
                    packRgb(glowRed, glowGreen, glowBlue, glowAlpha),
                    GlintstoneCometModels.COMET_HEAD_TEXTURE,
                    GlintstoneCometModels.COMET_GLOW_TEXTURE
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
                flightDirectionWorld
        );
        renderCameraFacingGlow(entity, partialTicks, poseStack, bufferSource, entityRenderDispatcher, visualStyle);
    }

    /**
     * 沿速度方向画立体菱形晶核（尖端朝前）。
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
        poseStack.scale(visualStyle.bodyScale(), visualStyle.bodyScale(), visualStyle.bodyScale());

        VertexConsumer bodyConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(visualStyle.bodyTexture())
        );
        cometBodyRoot.render(
                poseStack,
                bodyConsumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                visualStyle.coreColorArgb()
        );
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
            Vec3 flightDirectionWorld
    ) {
        poseStack.pushPose();
        ProjectileOrientation.alignPoseToDirection(poseStack, flightDirectionWorld);
        poseStack.scale(visualStyle.bodyScale(), visualStyle.bodyScale(), visualStyle.bodyScale());

        VertexConsumer bodyConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(visualStyle.bodyTexture())
        );
        cometBodyRoot.render(
                poseStack,
                bodyConsumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                visualStyle.coreColorArgb()
        );
        poseStack.popPose();
    }

    /**
     * 相机朝向的柔光晕：内层亮核 + 外层更大的自发光晕，让彗星本体明显「带光」。
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
        float pulseScale = visualStyle.glowScale()
                + Mth.sin(age * 0.45f) * visualStyle.glowPulseAmplitude();

        int glowColor = visualStyle.glowColorArgb();
        int red = (glowColor >> 16) & 0xFF;
        int green = (glowColor >> 8) & 0xFF;
        int blue = glowColor & 0xFF;
        int alpha = (glowColor >> 24) & 0xFF;

        // 外层大光晕：更淡、更大，形成本体周围的辉光场
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
        // 内层亮核：自发光交叉四边形
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

package com.eldenring.spells.client.render.glintstone;

import com.eldenring.spells.client.render.ProjectileOrientation;
import com.eldenring.spells.entity.GlintstoneVisualStyle;
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
 * 再调用 {@link #render(Entity, float, PoseStack, MultiBufferSource, ModelPart, EntityRenderDispatcher, GlintstoneVisualStyle)}。
 */
public final class GlintstoneCometHeadDrawer {
    private GlintstoneCometHeadDrawer() {
    }

    public static void render(
            Entity entity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            ModelPart cometBodyRoot,
            EntityRenderDispatcher entityRenderDispatcher,
            GlintstoneVisualStyle visualStyle
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
            GlintstoneVisualStyle visualStyle,
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
            GlintstoneVisualStyle visualStyle
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
            GlintstoneVisualStyle visualStyle,
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
    private static void applyBodyScaleAndSpin(PoseStack poseStack, GlintstoneVisualStyle visualStyle, float animationTicks) {
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
            GlintstoneVisualStyle visualStyle
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
            GlintstoneVisualStyle visualStyle
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
            GlintstoneVisualStyle visualStyle
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
            GlintstoneVisualStyle visualStyle,
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
            GlintstoneVisualStyle visualStyle,
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
            GlintstoneVisualStyle visualStyle,
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

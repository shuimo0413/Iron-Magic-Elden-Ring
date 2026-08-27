package com.eldenring.spells.client.render.haima;

import com.eldenring.spells.entity.GavelOfHaimaEntity;
import com.eldenring.spells.spell.curve.GavelOfHaimaCastCurve;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 海摩大槌客户端渲染：自发光半透明立方体 + 相机朝向柔光晕。
 * <p>
 * {@link ModelPart#render} 已按 1/16 方块解释坐标，这里<strong>禁止再乘 1/16</strong>，
 * 否则锤子会缩成几格高的小点（曾导致实机「锤子特别小」）。
 * 俯仰由 {@link GavelOfHaimaEntity#getSwingProgress} 插值：近 0° 竖直握持 → 正角向前砸地。
 */
public class HaimaGavelRenderer extends EntityRenderer<GavelOfHaimaEntity> {

    /** 模型整体缩放。约玩家身高量级的单手大槌。 */
    private static final float HAMMER_RENDER_SCALE = 1.08f;
    private static final float RENDER_PIVOT_Y_OFFSET_BLOCKS = 0.0f;
    private static final int HAMMER_BODY_COLOR_ARGB = 0xB000E8E0;
    private static final int HAMMER_HEAD_COLOR_ARGB = 0xC820F0E8;
    private static final int HAMMER_CAP_COLOR_ARGB = 0xD040FFF0;
    private static final int HAMMER_GLOW_COLOR_ARGB = 0x8800E8D8;

    private final ModelPart gavelRoot;

    public HaimaGavelRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.gavelRoot = context.bakeLayer(HaimaGavelModels.GAVEL_LAYER);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(GavelOfHaimaEntity entity) {
        return HaimaGavelModels.GAVEL_BODY_TEXTURE;
    }

    @Override
    public void render(
            GavelOfHaimaEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        float fadeAlpha = entity.getFadeAlpha(partialTicks);
        if (fadeAlpha <= 0.01f) {
            return;
        }

        float swingProgress = entity.getSwingProgress(partialTicks);
        float pitchDegrees = Mth.lerp(
                swingProgress,
                GavelOfHaimaCastCurve.HAMMER_RAISED_PITCH_DEGREES,
                GavelOfHaimaCastCurve.HAMMER_SLAMMED_PITCH_DEGREES
        );
        float scale = HAMMER_RENDER_SCALE;

        poseStack.pushPose();
        poseStack.translate(0.0, RENDER_PIVOT_Y_OFFSET_BLOCKS, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitchDegrees));
        poseStack.scale(scale, scale, scale);

        int bodyColor = applyAlpha(HAMMER_BODY_COLOR_ARGB, fadeAlpha);
        int headColor = applyAlpha(HAMMER_HEAD_COLOR_ARGB, fadeAlpha);
        int glowColor = applyAlpha(HAMMER_GLOW_COLOR_ARGB, fadeAlpha * 0.85f);

        VertexConsumer bodyConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(HaimaGavelModels.GAVEL_BODY_TEXTURE)
        );
        gavelRoot.getChild(HaimaGavelModels.HANDLE_PART).render(
                poseStack,
                bodyConsumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                bodyColor
        );
        gavelRoot.getChild(HaimaGavelModels.HEAD_BAND_PART).render(
                poseStack,
                bodyConsumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                headColor
        );
        gavelRoot.getChild(HaimaGavelModels.HEAD_PART).render(
                poseStack,
                bodyConsumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                headColor
        );
        gavelRoot.getChild(HaimaGavelModels.HEAD_CAP_PART).render(
                poseStack,
                bodyConsumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                applyAlpha(HAMMER_CAP_COLOR_ARGB, fadeAlpha)
        );
        poseStack.popPose();

        renderHeadGlow(entity, partialTicks, pitchDegrees, poseStack, bufferSource, glowColor, fadeAlpha);
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    /**
     * 在锤头附近画两层相机朝向光斑，强化能量轮廓。
     */
    private void renderHeadGlow(
            GavelOfHaimaEntity entity,
            float partialTicks,
            float pitchDegrees,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int glowColorArgb,
            float fadeAlpha
    ) {
        double headLength = GavelOfHaimaCastCurve.HEAD_LENGTH_ALONG_HANDLE_BLOCKS
                * HAMMER_RENDER_SCALE;
        double pitchRadians = Math.toRadians(pitchDegrees);
        double localY = Math.cos(pitchRadians) * headLength;
        double localZ = Math.sin(pitchRadians) * headLength;
        float yawRadians = entity.getYRot() * Mth.DEG_TO_RAD;
        double offsetX = -Math.sin(yawRadians) * localZ;
        double offsetZ = Math.cos(yawRadians) * localZ;

        poseStack.pushPose();
        poseStack.translate(
                offsetX,
                RENDER_PIVOT_Y_OFFSET_BLOCKS + localY,
                offsetZ
        );
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        float age = entity.tickCount + partialTicks;
        float pulse = 1.35f + Mth.sin(age * 0.55f) * 0.14f;
        int red = (glowColorArgb >> 16) & 0xFF;
        int green = (glowColorArgb >> 8) & 0xFF;
        int blue = glowColorArgb & 0xFF;
        int alpha = (int) (((glowColorArgb >> 24) & 0xFF) * fadeAlpha);

        drawGlowQuad(poseStack, bufferSource, pulse * 1.55f, red, green, blue, (int) (alpha * 0.4f));
        drawGlowQuad(poseStack, bufferSource, pulse, red, green, blue, alpha);
        poseStack.popPose();
    }

    private static void drawGlowQuad(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float scale,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        if (alpha <= 0) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(HaimaGavelModels.GAVEL_GLOW_TEXTURE)
        );
        int color = (alpha << 24) | (red << 16) | (green << 8) | blue;
        consumer.addVertex(matrix, -0.5f, -0.5f, 0.0f)
                .setColor(color)
                .setUv(0.0f, 1.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 0.0f, 1.0f);
        consumer.addVertex(matrix, 0.5f, -0.5f, 0.0f)
                .setColor(color)
                .setUv(1.0f, 1.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 0.0f, 1.0f);
        consumer.addVertex(matrix, 0.5f, 0.5f, 0.0f)
                .setColor(color)
                .setUv(1.0f, 0.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 0.0f, 1.0f);
        consumer.addVertex(matrix, -0.5f, 0.5f, 0.0f)
                .setColor(color)
                .setUv(0.0f, 0.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 0.0f, 1.0f);
        poseStack.popPose();
    }

    private static int applyAlpha(int argb, float alphaMultiplier) {
        int alpha = Mth.clamp((int) (((argb >> 24) & 0xFF) * alphaMultiplier), 0, 255);
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}

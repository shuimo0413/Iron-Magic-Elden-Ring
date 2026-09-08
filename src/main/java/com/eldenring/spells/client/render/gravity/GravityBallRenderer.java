package com.eldenring.spells.client.render.gravity;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GravityBallProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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
 * 重力球：始终朝向相机的黑核 + 旋转紫晕 + 蚀环感，不要辉石菱晶。
 * <p>
 * 贴图复用粒子 {@code gravity_core} / {@code gravity_glow} / {@code gravity_eclipse}。
 */
public class GravityBallRenderer extends EntityRenderer<GravityBallProjectile> {

    private static final ResourceLocation CORE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/particle/gravity_core.png");
    private static final ResourceLocation GLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/particle/gravity_glow.png");
    private static final ResourceLocation ECLIPSE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/particle/gravity_eclipse.png");

    private static final RenderType CORE_RENDER_TYPE = RenderType.entityTranslucentEmissive(CORE_TEXTURE);
    private static final RenderType GLOW_RENDER_TYPE = RenderType.entityTranslucentEmissive(GLOW_TEXTURE);
    private static final RenderType ECLIPSE_RENDER_TYPE = RenderType.entityTranslucentEmissive(ECLIPSE_TEXTURE);

    /** 外晕半边长（方块）。调大 → 紫晕更散。 */
    private static final float GLOW_HALF_SIZE_BLOCKS = 0.55f;

    /** 蚀环半边长（方块）。 */
    private static final float ECLIPSE_HALF_SIZE_BLOCKS = 0.38f;

    /** 黑核半边长（方块）。 */
    private static final float CORE_HALF_SIZE_BLOCKS = 0.18f;

    /** 外晕不透明度（0–1）。 */
    private static final float GLOW_OPACITY = 0.55f;

    /** 蚀环不透明度（0–1）。 */
    private static final float ECLIPSE_OPACITY = 0.72f;

    /** 黑核不透明度（0–1）。 */
    private static final float CORE_OPACITY = 0.98f;

    /** 绕视线自转（度 / tick）。调大 → 晕圈转得更明显。 */
    private static final float SPIN_DEGREES_PER_TICK = 8.0f;

    /** 呼吸缩放振幅（相对 1）。 */
    private static final float PULSE_SCALE_AMPLITUDE = 0.08f;

    /** 呼吸角频率（弧度 / tick）。 */
    private static final float PULSE_RADIANS_PER_TICK = 0.22f;

    public GravityBallRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(GravityBallProjectile entity) {
        return CORE_TEXTURE;
    }

    @Override
    public void render(
            GravityBallProjectile entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        float ageTicks = entity.tickCount + partialTick;
        float pulseScale = 1.0f + PULSE_SCALE_AMPLITUDE * Mth.sin(ageTicks * PULSE_RADIANS_PER_TICK);

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(ageTicks * SPIN_DEGREES_PER_TICK));
        poseStack.scale(pulseScale, pulseScale, pulseScale);

        int fullBright = LightTexture.FULL_BRIGHT;

        putBillboardQuad(
                bufferSource.getBuffer(GLOW_RENDER_TYPE),
                poseStack.last().pose(),
                GLOW_HALF_SIZE_BLOCKS,
                packPurple(GLOW_OPACITY, 0.55f),
                fullBright
        );
        putBillboardQuad(
                bufferSource.getBuffer(ECLIPSE_RENDER_TYPE),
                poseStack.last().pose(),
                ECLIPSE_HALF_SIZE_BLOCKS,
                packPurple(ECLIPSE_OPACITY, 0.35f),
                fullBright
        );
        putBillboardQuad(
                bufferSource.getBuffer(CORE_RENDER_TYPE),
                poseStack.last().pose(),
                CORE_HALF_SIZE_BLOCKS,
                packVoid(CORE_OPACITY),
                fullBright
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    /**
     * @param opacity  0–1
     * @param whiteMix 0 = 深紫，1 = 近白薰衣草
     */
    private static int packPurple(float opacity, float whiteMix) {
        int alpha = Mth.clamp(Math.round(opacity * 255.0f), 0, 255);
        float clampedWhite = Mth.clamp(whiteMix, 0.0f, 1.0f);
        int red = Mth.clamp(Math.round(104 + 120 * clampedWhite), 0, 255);
        int green = Mth.clamp(Math.round(80 + 128 * clampedWhite), 0, 255);
        int blue = Mth.clamp(Math.round(160 + 80 * clampedWhite), 0, 255);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int packVoid(float opacity) {
        int alpha = Mth.clamp(Math.round(opacity * 255.0f), 0, 255);
        return (alpha << 24) | (12 << 16) | (0 << 8) | 28;
    }

    private static void putBillboardQuad(
            VertexConsumer consumer,
            Matrix4f poseMatrix,
            float halfSizeBlocks,
            int packedColor,
            int packedLight
    ) {
        putVertex(consumer, poseMatrix, -halfSizeBlocks, -halfSizeBlocks, 0.0f, 0.0f, 1.0f, packedColor, packedLight);
        putVertex(consumer, poseMatrix, halfSizeBlocks, -halfSizeBlocks, 0.0f, 1.0f, 1.0f, packedColor, packedLight);
        putVertex(consumer, poseMatrix, halfSizeBlocks, halfSizeBlocks, 0.0f, 1.0f, 0.0f, packedColor, packedLight);
        putVertex(consumer, poseMatrix, -halfSizeBlocks, halfSizeBlocks, 0.0f, 0.0f, 0.0f, packedColor, packedLight);
    }

    private static void putVertex(
            VertexConsumer consumer,
            Matrix4f poseMatrix,
            float x,
            float y,
            float z,
            float u,
            float v,
            int packedColor,
            int packedLight
    ) {
        consumer.addVertex(poseMatrix, x, y, z)
                .setColor(packedColor)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0.0f, 0.0f, 1.0f);
    }
}

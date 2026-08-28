package com.eldenring.spells.client.render;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.StarlightEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
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
 * 星光头顶小星：始终朝向相机的青色四角星 + 软光晕。
 * <p>
 * 原版方块光是无色的；这块自发光四边形负责「看起来是青光」。
 * 第一人称不画自己的星，避免抬头时糊在镜头上。
 */
public class StarlightRenderer extends EntityRenderer<StarlightEntity> {

    private static final ResourceLocation STAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/starlight/star.png");

    private static final RenderType STAR_RENDER_TYPE = RenderType.entityTranslucentEmissive(STAR_TEXTURE);

    /**
     * 光晕半边长（方块）。调大 → 青晕更散；调小 → 更像一粒星。
     */
    private static final float HALO_HALF_SIZE_BLOCKS = 0.42f;

    /**
     * 星体半边长（方块）。调大 → 星星本身更大。
     */
    private static final float STAR_HALF_SIZE_BLOCKS = 0.16f;

    /**
     * 亮核半边长（方块）。
     */
    private static final float CORE_HALF_SIZE_BLOCKS = 0.055f;

    /**
     * 光晕不透明度（0–1）。叠在星体后面，给周围一点青色。
     */
    private static final float HALO_OPACITY = 0.38f;

    /**
     * 星体不透明度（0–1）。
     */
    private static final float STAR_OPACITY = 0.96f;

    /**
     * 绕视线缓慢自转（度 / tick）。调大 → 转得更明显。
     */
    private static final float SPIN_DEGREES_PER_TICK = 0.55f;

    /**
     * 呼吸缩放振幅（相对 1）。调大 → 明灭更夸张。
     */
    private static final float PULSE_SCALE_AMPLITUDE = 0.07f;

    /**
     * 呼吸角频率（弧度 / tick）。
     */
    private static final float PULSE_RADIANS_PER_TICK = 0.14f;

    /**
     * 寿命最后多少 tick 淡出。调大 → 消失更慢。
     */
    private static final int FADE_TICKS = 24;

    public StarlightRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(StarlightEntity entity) {
        return STAR_TEXTURE;
    }

    @Override
    public void render(
            StarlightEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.getCameraType().isFirstPerson()
                && entity.getOwner() == minecraft.getCameraEntity()) {
            return;
        }

        float fadeAlpha = fadeAlpha(entity.remainingLifetimeTicks());
        if (fadeAlpha <= 0.02f) {
            return;
        }

        float ageTicks = entity.tickCount + partialTick;
        float pulseScale = 1.0f + PULSE_SCALE_AMPLITUDE * Mth.sin(ageTicks * PULSE_RADIANS_PER_TICK);

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(ageTicks * SPIN_DEGREES_PER_TICK));
        poseStack.scale(pulseScale, pulseScale, pulseScale);

        VertexConsumer consumer = bufferSource.getBuffer(STAR_RENDER_TYPE);
        Matrix4f poseMatrix = poseStack.last().pose();
        int fullBright = LightTexture.FULL_BRIGHT;

        putBillboardQuad(
                consumer,
                poseMatrix,
                HALO_HALF_SIZE_BLOCKS,
                packCyan(HALO_OPACITY * fadeAlpha * 0.85f, 0.35f),
                fullBright
        );
        putBillboardQuad(
                consumer,
                poseMatrix,
                STAR_HALF_SIZE_BLOCKS,
                packCyan(STAR_OPACITY * fadeAlpha, 0.82f),
                fullBright
        );
        putBillboardQuad(
                consumer,
                poseMatrix,
                CORE_HALF_SIZE_BLOCKS,
                packCyan(fadeAlpha, 1.0f),
                fullBright
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    /**
     * 寿命末段线性淡出。remaining 大于 {@link #FADE_TICKS} 时为 1。
     */
    private static float fadeAlpha(int remainingTicks) {
        if (remainingTicks >= FADE_TICKS) {
            return 1.0f;
        }
        return Mth.clamp(remainingTicks / (float) FADE_TICKS, 0.0f, 1.0f);
    }

    /**
     * @param opacity 0–1
     * @param whiteMix 0 = 纯青，1 = 近白核
     */
    private static int packCyan(float opacity, float whiteMix) {
        int alpha = Mth.clamp(Math.round(opacity * 255.0f), 0, 255);
        float clampedWhite = Mth.clamp(whiteMix, 0.0f, 1.0f);
        int red = Mth.clamp(Math.round(48 + 190 * clampedWhite), 0, 255);
        int green = Mth.clamp(Math.round(210 + 40 * clampedWhite), 0, 255);
        int blue = Mth.clamp(Math.round(220 + 30 * clampedWhite), 0, 255);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
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

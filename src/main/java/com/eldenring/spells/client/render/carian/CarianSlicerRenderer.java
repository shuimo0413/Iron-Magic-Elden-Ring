package com.eldenring.spells.client.render.carian;

import com.eldenring.spells.entity.CarianSlicerEntity;
import com.eldenring.spells.spell.curve.CarianSlicerCastCurve;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 卡利亚迅剑客户端渲染：自发光直剑 + 贴在挥砍平面上的扫掠刀光。
 * <p>
 * 刀光不是朝向相机的细带（那种会扭歪），而是护手到刃尖扫过的扇面，
 * 和剑刃同一套旋转，所以新月始终贴着这一刀的弧。
 */
public class CarianSlicerRenderer extends EntityRenderer<CarianSlicerEntity> {

    /** 模型整体缩放。迅剑比辉剑略短略宽。 */
    private static final float SWORD_RENDER_SCALE = 0.92f;
    private static final int SWORD_BODY_COLOR_ARGB = 0xC01848C8;
    private static final int SWORD_BLADE_COLOR_ARGB = 0xD04890FF;
    private static final int SWORD_EDGE_COLOR_ARGB = 0xE0C8E8FF;
    private static final int SLASH_ARC_COLOR_ARGB = 0xCC3A7CFF;
    private static final int SLASH_ARC_SEGMENT_COUNT = 14;
    /** 斩击光弧内沿半径（方块），大约在护手外。 */
    private static final double SLASH_ARC_INNER_RADIUS_BLOCKS = 0.38;

    private final ModelPart swordRoot;

    public CarianSlicerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.swordRoot = context.bakeLayer(CarianSwordModels.SWORD_LAYER);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(CarianSlicerEntity entity) {
        return CarianSwordModels.SWORD_BODY_TEXTURE;
    }

    @Override
    public void render(
            CarianSlicerEntity entity,
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
        float rollDegrees = Mth.lerp(
                swingProgress,
                entity.getSwingStartRollDegrees(),
                entity.getSwingEndRollDegrees()
        );
        float bladePitchDegrees = Mth.lerp(
                swingProgress,
                entity.getSwingStartPitchDegrees(),
                entity.getSwingEndPitchDegrees()
        );
        float lookPitchDegrees = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        float scale = SWORD_RENDER_SCALE;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(lookPitchDegrees));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rollDegrees));
        poseStack.mulPose(Axis.XP.rotationDegrees(bladePitchDegrees));
        poseStack.scale(scale, scale, scale);
        renderSwordMesh(this.swordRoot, poseStack, bufferSource, fadeAlpha);
        poseStack.popPose();

        renderSlashArc(entity, swingProgress, fadeAlpha, entityYaw, lookPitchDegrees, partialTicks, poseStack, bufferSource);
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    /**
     * 画剑身各段。
     */
    private static void renderSwordMesh(
            ModelPart swordRoot,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float fadeAlpha
    ) {
        int bodyColor = applyAlpha(SWORD_BODY_COLOR_ARGB, fadeAlpha);
        int bladeColor = applyAlpha(SWORD_BLADE_COLOR_ARGB, fadeAlpha);
        int edgeColor = applyAlpha(SWORD_EDGE_COLOR_ARGB, fadeAlpha);
        VertexConsumer bodyConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(CarianSwordModels.SWORD_BODY_TEXTURE)
        );
        swordRoot.getChild(CarianSwordModels.POMMEL_PART).render(
                poseStack, bodyConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, bodyColor
        );
        swordRoot.getChild(CarianSwordModels.HANDLE_PART).render(
                poseStack, bodyConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, bodyColor
        );
        swordRoot.getChild(CarianSwordModels.GUARD_PART).render(
                poseStack, bodyConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, bladeColor
        );
        swordRoot.getChild(CarianSwordModels.BLADE_PART).render(
                poseStack, bodyConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, bladeColor
        );
        swordRoot.getChild(CarianSwordModels.EDGE_PART).render(
                poseStack, bodyConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, edgeColor
        );
    }

    /**
     * 把 0→当前进度 的「护手→刃尖」扫掠连成贴在挥砍平面上的扇面。
     */
    private void renderSlashArc(
            CarianSlicerEntity entity,
            float swingProgress,
            float fadeAlpha,
            float yawDegrees,
            float lookPitchDegrees,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource
    ) {
        if (swingProgress <= 0.04f) {
            return;
        }
        int segmentCount = SLASH_ARC_SEGMENT_COUNT;
        int arcColor = applyAlpha(SLASH_ARC_COLOR_ARGB, fadeAlpha * 0.95f);
        int coreColor = applyAlpha(SWORD_EDGE_COLOR_ARGB, fadeAlpha);

        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(CarianSwordModels.SLASH_CRESCENT_TEXTURE)
        );
        Vec3 entityWorld = entity.getPosition(partialTicks);

        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();
        for (int segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {
            float progressStart = swingProgress * segmentIndex / segmentCount;
            float progressEnd = swingProgress * (segmentIndex + 1) / segmentCount;
            Vec3 innerStart = entity.computeBladePointWorld(
                    progressStart,
                    SLASH_ARC_INNER_RADIUS_BLOCKS,
                    entityWorld,
                    yawDegrees,
                    lookPitchDegrees
            ).subtract(entityWorld);
            Vec3 outerStart = entity.computeBladePointWorld(
                    progressStart,
                    CarianSlicerCastCurve.BLADE_LENGTH_BLOCKS,
                    entityWorld,
                    yawDegrees,
                    lookPitchDegrees
            ).subtract(entityWorld);
            Vec3 innerEnd = entity.computeBladePointWorld(
                    progressEnd,
                    SLASH_ARC_INNER_RADIUS_BLOCKS,
                    entityWorld,
                    yawDegrees,
                    lookPitchDegrees
            ).subtract(entityWorld);
            Vec3 outerEnd = entity.computeBladePointWorld(
                    progressEnd,
                    CarianSlicerCastCurve.BLADE_LENGTH_BLOCKS,
                    entityWorld,
                    yawDegrees,
                    lookPitchDegrees
            ).subtract(entityWorld);
            float vStart = segmentIndex / (float) segmentCount;
            float vEnd = (segmentIndex + 1) / (float) segmentCount;
            drawSweepQuad(matrix, consumer, innerStart, outerStart, outerEnd, innerEnd, arcColor, vStart, vEnd);
            // 刃锋更亮的窄一层：从 70% 半径拉到刃尖
            double coreInnerRadius = Mth.lerp(
                    0.70,
                    SLASH_ARC_INNER_RADIUS_BLOCKS,
                    CarianSlicerCastCurve.BLADE_LENGTH_BLOCKS
            );
            Vec3 coreInnerStart = entity.computeBladePointWorld(
                    progressStart,
                    coreInnerRadius,
                    entityWorld,
                    yawDegrees,
                    lookPitchDegrees
            ).subtract(entityWorld);
            Vec3 coreInnerEnd = entity.computeBladePointWorld(
                    progressEnd,
                    coreInnerRadius,
                    entityWorld,
                    yawDegrees,
                    lookPitchDegrees
            ).subtract(entityWorld);
            drawSweepQuad(matrix, consumer, coreInnerStart, outerStart, outerEnd, coreInnerEnd, coreColor, vStart, vEnd);
        }
        poseStack.popPose();
    }

    /**
     * 扫掠四边形双面提交，避免从背面看刀光被剔除。
     */
    private static void drawSweepQuad(
            Matrix4f matrix,
            VertexConsumer consumer,
            Vec3 innerStart,
            Vec3 outerStart,
            Vec3 outerEnd,
            Vec3 innerEnd,
            int colorArgb,
            float vStart,
            float vEnd
    ) {
        addVertex(consumer, matrix, innerStart, 0.0f, vStart, colorArgb);
        addVertex(consumer, matrix, outerStart, 1.0f, vStart, colorArgb);
        addVertex(consumer, matrix, outerEnd, 1.0f, vEnd, colorArgb);
        addVertex(consumer, matrix, innerEnd, 0.0f, vEnd, colorArgb);

        addVertex(consumer, matrix, innerStart, 0.0f, vStart, colorArgb);
        addVertex(consumer, matrix, innerEnd, 0.0f, vEnd, colorArgb);
        addVertex(consumer, matrix, outerEnd, 1.0f, vEnd, colorArgb);
        addVertex(consumer, matrix, outerStart, 1.0f, vStart, colorArgb);
    }

    private static void addVertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            Vec3 position,
            float u,
            float v,
            int colorArgb
    ) {
        consumer.addVertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .setColor(colorArgb)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 1.0f, 0.0f);
    }

    private static int applyAlpha(int argb, float alphaMultiplier) {
        int alpha = Mth.clamp((int) (((argb >> 24) & 0xFF) * alphaMultiplier), 0, 255);
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}

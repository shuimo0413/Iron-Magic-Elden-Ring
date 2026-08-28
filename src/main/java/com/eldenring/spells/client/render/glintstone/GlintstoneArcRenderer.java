package com.eldenring.spells.client.render.glintstone;

import com.eldenring.spells.entity.GlintstoneArcProjectile;
import com.eldenring.spells.entity.GlintstoneVisualStyle;
import com.eldenring.spells.spell.combat.GlintstoneArcCombat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
 * 辉石弯弧：几层左右对称的月牙，从小到大套在一起，读成一圈圈水波。
 * <p>
 * 几何在水平面里对中轴线镜像；每层月牙是一堵矮墙（有高度），第三人称才看得见，
 * 不会变成贴地薄片的侧棱。贴图用平滑光晕，不用月牙 PNG——那种图铺在分段四边形上会切成竖条。
 */
public class GlintstoneArcRenderer extends EntityRenderer<GlintstoneArcProjectile> {

    /**
     * 四层月牙相对最外层半径的比例。越靠内越小，叠起来像涟漪。
     */
    private static final float[] CRESCENT_RADIUS_SCALES = {0.42f, 0.62f, 0.82f, 1.00f};

    /**
     * 由内到外：内层更亮更实，外层更淡，像水波往外散。
     */
    private static final int[] CRESCENT_COLOR_ARGB = {
            0xF0E8FFFF,
            0xDD88F8FF,
            0xC040E8FF,
            0xA028D0FF
    };

    /**
     * 各层矮墙高度（方块）。外层略高，叠差也更像波。
     */
    private static final float[] CRESCENT_HEIGHT_BLOCKS = {0.28f, 0.38f, 0.48f, 0.58f};

    /**
     * 月牙最厚处（弧顶）相对该层半径的比例。调大 → 更胖的月牙；调小 → 更细的刃。
     */
    private static final float CRESCENT_BELLY_THICKNESS_FRACTION = 0.22f;

    /**
     * 尖端相对弧顶的厚度比例。0 = 尖端收成线；留一点避免破面。
     */
    private static final float CRESCENT_TIP_THICKNESS_FRACTION = 0.22f;

    /**
     * 每一层月牙的分段。必须是偶数，左右才能严格对称。
     */
    private static final int CRESCENT_SEGMENT_COUNT = 24;

    /**
     * 略抬离地面（方块），减少贴地 z-fight。
     */
    private static final float ARC_LIFT_BLOCKS = 0.06f;

    public GlintstoneArcRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(GlintstoneArcProjectile entity) {
        return GlintstoneVisualStyle.COMET_GLOW_TEXTURE;
    }

    @Override
    public void render(
            GlintstoneArcProjectile entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        float halfWidthBlocks = entity.currentHalfWidthBlocks(partialTicks);
        if (halfWidthBlocks < 0.05f) {
            return;
        }

        Vec3 horizontalForward = GlintstoneArcCombat.horizontalForward(entity.resolveFlightDirection());
        Vec3 horizontalRight = GlintstoneArcCombat.horizontalRight(horizontalForward);
        float maxRadiusBlocks = GlintstoneArcCombat.crescentOuterRadius(halfWidthBlocks);
        float halfAngleRadians = (float) Math.toRadians(GlintstoneArcCombat.CRESCENT_HALF_ANGLE_DEGREES);

        poseStack.pushPose();
        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(GlintstoneVisualStyle.COMET_GLOW_TEXTURE)
        );
        Matrix4f matrix = poseStack.last().pose();

        for (int layerIndex = 0; layerIndex < CRESCENT_RADIUS_SCALES.length; layerIndex++) {
            float layerRadiusBlocks = maxRadiusBlocks * CRESCENT_RADIUS_SCALES[layerIndex];
            float bellyThicknessBlocks = layerRadiusBlocks * CRESCENT_BELLY_THICKNESS_FRACTION;
            drawSymmetricCrescent(
                    matrix,
                    consumer,
                    horizontalForward,
                    horizontalRight,
                    maxRadiusBlocks,
                    layerRadiusBlocks,
                    bellyThicknessBlocks,
                    CRESCENT_HEIGHT_BLOCKS[layerIndex],
                    halfAngleRadians,
                    CRESCENT_COLOR_ARGB[layerIndex]
            );
        }
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    /**
     * 画一层左右镜像的月牙矮墙：弧顶厚、两尖收细，圆心与最外层共用。
     */
    private static void drawSymmetricCrescent(
            Matrix4f matrix,
            VertexConsumer consumer,
            Vec3 horizontalForward,
            Vec3 horizontalRight,
            float sharedCenterRadiusBlocks,
            float layerRadiusBlocks,
            float bellyThicknessBlocks,
            float heightBlocks,
            float halfAngleRadians,
            int colorArgb
    ) {
        int segmentCount = CRESCENT_SEGMENT_COUNT;
        for (int segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {
            float tStart = segmentIndex / (float) segmentCount;
            float tEnd = (segmentIndex + 1) / (float) segmentCount;
            float angleStart = Mth.lerp(tStart, -halfAngleRadians, halfAngleRadians);
            float angleEnd = Mth.lerp(tEnd, -halfAngleRadians, halfAngleRadians);

            CrescentSlice startSlice = crescentSlice(
                    horizontalForward,
                    horizontalRight,
                    sharedCenterRadiusBlocks,
                    layerRadiusBlocks,
                    bellyThicknessBlocks,
                    heightBlocks,
                    angleStart,
                    halfAngleRadians
            );
            CrescentSlice endSlice = crescentSlice(
                    horizontalForward,
                    horizontalRight,
                    sharedCenterRadiusBlocks,
                    layerRadiusBlocks,
                    bellyThicknessBlocks,
                    heightBlocks,
                    angleEnd,
                    halfAngleRadians
            );

            // 顶面：俯视也能看出月牙，不只是一条线
            drawQuad(
                    matrix, consumer,
                    startSlice.innerTop, startSlice.outerTop, endSlice.outerTop, endSlice.innerTop,
                    colorArgb, tStart, tEnd
            );
            // 外面那堵矮墙：第三人称从身后看就是对称的月牙
            drawQuad(
                    matrix, consumer,
                    startSlice.outerBottom, startSlice.outerTop, endSlice.outerTop, endSlice.outerBottom,
                    colorArgb, tStart, tEnd
            );
        }
    }

    /**
     * 某一角度上的月牙切片。厚度按 {@code 1 - (θ/α)²} 在弧顶最胖、两尖收细，左右同一公式所以对称。
     */
    private static CrescentSlice crescentSlice(
            Vec3 horizontalForward,
            Vec3 horizontalRight,
            float sharedCenterRadiusBlocks,
            float layerRadiusBlocks,
            float bellyThicknessBlocks,
            float heightBlocks,
            float angleRadians,
            float halfAngleRadians
    ) {
        float angleFraction = halfAngleRadians <= 1.0e-4f
                ? 0.0f
                : Mth.clamp(Math.abs(angleRadians) / halfAngleRadians, 0.0f, 1.0f);
        float bellyFraction = 1.0f - angleFraction * angleFraction;
        float thicknessBlocks = bellyThicknessBlocks * Mth.lerp(
                bellyFraction,
                CRESCENT_TIP_THICKNESS_FRACTION,
                1.0f
        );
        float innerRadiusBlocks = Math.max(0.08f, layerRadiusBlocks - thicknessBlocks);

        Vec3 outer = ringPoint(
                horizontalForward, horizontalRight, angleRadians, layerRadiusBlocks, sharedCenterRadiusBlocks
        );
        Vec3 inner = ringPoint(
                horizontalForward, horizontalRight, angleRadians, innerRadiusBlocks, sharedCenterRadiusBlocks
        );
        Vec3 up = new Vec3(0.0, heightBlocks, 0.0);
        return new CrescentSlice(
                inner.add(0.0, ARC_LIFT_BLOCKS, 0.0),
                outer.add(0.0, ARC_LIFT_BLOCKS, 0.0),
                inner.add(0.0, ARC_LIFT_BLOCKS, 0.0).add(up),
                outer.add(0.0, ARC_LIFT_BLOCKS, 0.0).add(up)
        );
    }

    /**
     * 共用圆心（实体后方 {@code sharedCenterRadius}）上的一点。{@code angle=0} 在射向中轴上。
     */
    private static Vec3 ringPoint(
            Vec3 horizontalForward,
            Vec3 horizontalRight,
            float angleRadians,
            float pointRadiusBlocks,
            float sharedCenterRadiusBlocks
    ) {
        double alongForward = Math.cos(angleRadians) * pointRadiusBlocks - sharedCenterRadiusBlocks;
        double alongRight = Math.sin(angleRadians) * pointRadiusBlocks;
        return horizontalForward.scale(alongForward).add(horizontalRight.scale(alongRight));
    }

    private static void drawQuad(
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

    private record CrescentSlice(Vec3 innerBottom, Vec3 outerBottom, Vec3 innerTop, Vec3 outerTop) {
    }
}

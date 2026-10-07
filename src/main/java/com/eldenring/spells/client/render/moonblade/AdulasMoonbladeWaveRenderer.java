package com.eldenring.spells.client.render.moonblade;

import com.eldenring.spells.client.render.ShaderDepthProxy;
import com.eldenring.spells.entity.AdulasMoonbladeWaveProjectile;
import com.eldenring.spells.entity.GlintstoneVisualStyle;
import com.eldenring.spells.spell.combat.GlintstoneArcCombat;
import com.eldenring.spells.spell.combat.GlintstoneArcCombat.ArcBasis;
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
 * 月光剑剑气：从辉石弯弧渲染器复制，但只画<strong>一道</strong>左右对称的月牙。
 * <p>
 * 这一道由两层同心几何叠成：外层冰蓝辉光（厚、高），内层冷白亮核（薄、矮、半径略小，避免 z-fight），
 * 远看仍读成一道刃，不会像弯弧那样一圈圈涟漪。几何在飞行局部平面（forward × right）里镜像，
 * 抬头 / 低头施法时整片跟着俯仰。
 */
public class AdulasMoonbladeWaveRenderer extends EntityRenderer<AdulasMoonbladeWaveProjectile> {

    /** 白色外辉颜色（ARGB）。调 alpha → 外层更实 / 更透。 */
    private static final int GLOW_COLOR_ARGB = 0xB4FFFFFF;

    /** 纯白亮核颜色（ARGB）。 */
    private static final int CORE_COLOR_ARGB = 0xFFFFFFFF;

    /** 亮核半径相对外辉半径的比例。略小于 1，让两面墙不重合。 */
    private static final float CORE_RADIUS_SCALE = 0.96f;

    /** 外辉矮墙高度（方块）。调大 → 第三人称看起来更高的一道刃。 */
    private static final float GLOW_HEIGHT_BLOCKS = 0.70f;

    /** 亮核矮墙高度（方块）。比外辉矮，居中抬起后像夹在辉光里。 */
    private static final float CORE_HEIGHT_BLOCKS = 0.42f;

    /** 外辉弧顶厚度相对半径的比例。调大 → 更胖的月牙。 */
    private static final float GLOW_BELLY_THICKNESS_FRACTION = 0.24f;

    /** 亮核弧顶厚度相对半径的比例。 */
    private static final float CORE_BELLY_THICKNESS_FRACTION = 0.09f;

    /** 尖端相对弧顶的厚度比例。0 = 尖端收成线；留一点避免破面。 */
    private static final float CRESCENT_TIP_THICKNESS_FRACTION = 0.15f;

    /** 月牙分段数。必须是偶数，左右才能严格对称。 */
    private static final int CRESCENT_SEGMENT_COUNT = 28;

    /** 外辉沿局部 up 抬离刃面（方块），减少与方块表面的 z-fight。 */
    private static final float GLOW_LIFT_BLOCKS = 0.06f;

    /** 亮核沿局部 up 的抬升（方块）：让亮核落在外辉高度中段。 */
    private static final float CORE_LIFT_BLOCKS = GLOW_LIFT_BLOCKS + (GLOW_HEIGHT_BLOCKS - CORE_HEIGHT_BLOCKS) * 0.5f;

    public AdulasMoonbladeWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(AdulasMoonbladeWaveProjectile entity) {
        return GlintstoneVisualStyle.COMET_GLOW_TEXTURE;
    }

    @Override
    public void render(
            AdulasMoonbladeWaveProjectile entity,
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

        ArcBasis arcBasis = ArcBasis.fromFlightDirection(entity.resolveFlightDirection());
        float outerRadiusBlocks = GlintstoneArcCombat.crescentOuterRadius(halfWidthBlocks);
        float halfAngleRadians = (float) Math.toRadians(GlintstoneArcCombat.CRESCENT_HALF_ANGLE_DEGREES);

        poseStack.pushPose();
        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(GlintstoneVisualStyle.COMET_GLOW_TEXTURE)
        );
        Matrix4f matrix = poseStack.last().pose();

        drawSymmetricCrescent(
                matrix,
                consumer,
                arcBasis,
                outerRadiusBlocks,
                outerRadiusBlocks,
                outerRadiusBlocks * GLOW_BELLY_THICKNESS_FRACTION,
                GLOW_HEIGHT_BLOCKS,
                GLOW_LIFT_BLOCKS,
                halfAngleRadians,
                GLOW_COLOR_ARGB
        );
        float coreRadiusBlocks = outerRadiusBlocks * CORE_RADIUS_SCALE;
        drawSymmetricCrescent(
                matrix,
                consumer,
                arcBasis,
                outerRadiusBlocks,
                coreRadiusBlocks,
                coreRadiusBlocks * CORE_BELLY_THICKNESS_FRACTION,
                CORE_HEIGHT_BLOCKS,
                CORE_LIFT_BLOCKS,
                halfAngleRadians,
                CORE_COLOR_ARGB
        );

        // 亮核同几何重提交为深度代理（在颜色层之后），光影下剑气不会被天空 / 冰面吃掉。
        drawSymmetricCrescent(
                matrix,
                bufferSource.getBuffer(ShaderDepthProxy.depthOnly(GlintstoneVisualStyle.COMET_GLOW_TEXTURE)),
                arcBasis,
                outerRadiusBlocks,
                coreRadiusBlocks,
                coreRadiusBlocks * CORE_BELLY_THICKNESS_FRACTION,
                CORE_HEIGHT_BLOCKS,
                CORE_LIFT_BLOCKS,
                halfAngleRadians,
                ShaderDepthProxy.proxyColor(CORE_COLOR_ARGB)
        );
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    /** 画一层左右镜像的月牙矮墙：弧顶厚、两尖收细，圆心与外辉共用。 */
    private static void drawSymmetricCrescent(
            Matrix4f matrix,
            VertexConsumer consumer,
            ArcBasis arcBasis,
            float sharedCenterRadiusBlocks,
            float layerRadiusBlocks,
            float bellyThicknessBlocks,
            float heightBlocks,
            float liftBlocks,
            float halfAngleRadians,
            int colorArgb
    ) {
        for (int segmentIndex = 0; segmentIndex < CRESCENT_SEGMENT_COUNT; segmentIndex++) {
            float startFraction = segmentIndex / (float) CRESCENT_SEGMENT_COUNT;
            float endFraction = (segmentIndex + 1) / (float) CRESCENT_SEGMENT_COUNT;
            float angleStart = Mth.lerp(startFraction, -halfAngleRadians, halfAngleRadians);
            float angleEnd = Mth.lerp(endFraction, -halfAngleRadians, halfAngleRadians);

            CrescentSlice startSlice = crescentSlice(
                    arcBasis, sharedCenterRadiusBlocks, layerRadiusBlocks, bellyThicknessBlocks,
                    heightBlocks, liftBlocks, angleStart, halfAngleRadians
            );
            CrescentSlice endSlice = crescentSlice(
                    arcBasis, sharedCenterRadiusBlocks, layerRadiusBlocks, bellyThicknessBlocks,
                    heightBlocks, liftBlocks, angleEnd, halfAngleRadians
            );

            // 顶面：俯视也能看出月牙，不只是一条线
            drawQuad(
                    matrix, consumer,
                    startSlice.innerTop, startSlice.outerTop, endSlice.outerTop, endSlice.innerTop,
                    colorArgb, startFraction, endFraction
            );
            // 外面那堵矮墙：第三人称从身后看就是对称的月牙
            drawQuad(
                    matrix, consumer,
                    startSlice.outerBottom, startSlice.outerTop, endSlice.outerTop, endSlice.outerBottom,
                    colorArgb, startFraction, endFraction
            );
        }
    }

    /**
     * 某一角度上的月牙切片。厚度按 {@code 1 - (θ/α)²} 在弧顶最胖、两尖收细。
     */
    private static CrescentSlice crescentSlice(
            ArcBasis arcBasis,
            float sharedCenterRadiusBlocks,
            float layerRadiusBlocks,
            float bellyThicknessBlocks,
            float heightBlocks,
            float liftBlocks,
            float angleRadians,
            float halfAngleRadians
    ) {
        float angleFraction = halfAngleRadians <= 1.0e-4f
                ? 0.0f
                : Mth.clamp(Math.abs(angleRadians) / halfAngleRadians, 0.0f, 1.0f);
        float bellyFraction = 1.0f - angleFraction * angleFraction;
        float thicknessBlocks = bellyThicknessBlocks * Mth.lerp(bellyFraction, CRESCENT_TIP_THICKNESS_FRACTION, 1.0f);
        float innerRadiusBlocks = Math.max(0.08f, layerRadiusBlocks - thicknessBlocks);

        Vec3 outer = ringPoint(arcBasis, angleRadians, layerRadiusBlocks, sharedCenterRadiusBlocks);
        Vec3 inner = ringPoint(arcBasis, angleRadians, innerRadiusBlocks, sharedCenterRadiusBlocks);
        Vec3 liftOffset = arcBasis.up().scale(liftBlocks);
        Vec3 heightOffset = arcBasis.up().scale(heightBlocks);
        return new CrescentSlice(
                inner.add(liftOffset),
                outer.add(liftOffset),
                inner.add(liftOffset).add(heightOffset),
                outer.add(liftOffset).add(heightOffset)
        );
    }

    /** 共用圆心（实体后方 {@code sharedCenterRadius}）上的一点。{@code angle=0} 在射向中轴上。 */
    private static Vec3 ringPoint(
            ArcBasis arcBasis,
            float angleRadians,
            float pointRadiusBlocks,
            float sharedCenterRadiusBlocks
    ) {
        double alongForward = Math.cos(angleRadians) * pointRadiusBlocks - sharedCenterRadiusBlocks;
        double alongRight = Math.sin(angleRadians) * pointRadiusBlocks;
        return arcBasis.forward().scale(alongForward).add(arcBasis.right().scale(alongRight));
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

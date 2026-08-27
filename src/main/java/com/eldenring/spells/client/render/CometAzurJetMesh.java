package com.eldenring.spells.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import com.eldenring.spells.particle.cometazur.CometAzurFx;

/**
 * 彗星亚兹勒喷流几何：口部 UV 圆球 + 沿朝向的圆管。
 * <p>
 * 顶点都在喷流口局部坐标里（PoseStack 原点已是实体/喷流口）。
 * 圆管半径沿程插值；贴图 U 只采样 {@code trail_beam} 中间亮带，避免绕一圈出现暗缝。
 */
public final class CometAzurJetMesh {

    /**
     * {@code trail_beam.png} 水平亮核大约在 U=0.5。绕圆周只扫这一小段，管壁才均匀发亮。
     */
    private static final float CYLINDER_TEXTURE_U_MIN = 0.40f;
    private static final float CYLINDER_TEXTURE_U_MAX = 0.60f;

    private CometAzurJetMesh() {
    }

    /**
     * 画一层圆管：从喷流口到远端，半径口→尖插值。
     */
    public static void renderCylinderLayer(
            PoseStack poseStack,
            VertexConsumer consumer,
            Vec3 forwardAxis,
            Vec3 rightAxis,
            Vec3 upAxis,
            float beamLengthBlocks,
            float mouthRadiusBlocks,
            float tipRadiusBlocks,
            int colorArgb
    ) {
        int sideCount = Math.max(6, CometAzurFx.JET_BEAM_CYLINDER_SIDE_COUNT);
        int ringCount = Math.max(3, CometAzurFx.JET_BEAM_CYLINDER_RING_COUNT);
        Matrix4f poseMatrix = poseStack.last().pose();
        int red = unpackRed(colorArgb);
        int green = unpackGreen(colorArgb);
        int blue = unpackBlue(colorArgb);
        int alpha = unpackAlpha(colorArgb);
        float mouthTextureV = CometAzurFx.JET_BEAM_TEXTURE_MOUTH_V;
        float tipTextureV = CometAzurFx.JET_BEAM_TEXTURE_TIP_V;

        for (int ringIndex = 0; ringIndex < ringCount - 1; ringIndex++) {
            float startProgress = ringIndex / (float) (ringCount - 1);
            float endProgress = (ringIndex + 1) / (float) (ringCount - 1);
            float startRadiusBlocks = Mth.lerp(startProgress, mouthRadiusBlocks, tipRadiusBlocks);
            float endRadiusBlocks = Mth.lerp(endProgress, mouthRadiusBlocks, tipRadiusBlocks);
            float startAlongBlocks = beamLengthBlocks * startProgress;
            float endAlongBlocks = beamLengthBlocks * endProgress;
            float startV = Mth.lerp(startProgress, mouthTextureV, tipTextureV);
            float endV = Mth.lerp(endProgress, mouthTextureV, tipTextureV);
            Vec3 startCenter = forwardAxis.scale(startAlongBlocks);
            Vec3 endCenter = forwardAxis.scale(endAlongBlocks);

            for (int sideIndex = 0; sideIndex < sideCount; sideIndex++) {
                float startAngleRadians = (float) (Math.PI * 2.0 * sideIndex / sideCount);
                float endAngleRadians = (float) (Math.PI * 2.0 * (sideIndex + 1) / sideCount);
                float startU = Mth.lerp(sideIndex / (float) sideCount, CYLINDER_TEXTURE_U_MIN, CYLINDER_TEXTURE_U_MAX);
                float endU = Mth.lerp((sideIndex + 1) / (float) sideCount, CYLINDER_TEXTURE_U_MIN, CYLINDER_TEXTURE_U_MAX);

                Vec3 startA = ringPoint(startCenter, rightAxis, upAxis, startRadiusBlocks, startAngleRadians);
                Vec3 startB = ringPoint(startCenter, rightAxis, upAxis, startRadiusBlocks, endAngleRadians);
                Vec3 endA = ringPoint(endCenter, rightAxis, upAxis, endRadiusBlocks, startAngleRadians);
                Vec3 endB = ringPoint(endCenter, rightAxis, upAxis, endRadiusBlocks, endAngleRadians);

                putVertex(consumer, poseMatrix, startA, startU, startV, red, green, blue, alpha);
                putVertex(consumer, poseMatrix, startB, endU, startV, red, green, blue, alpha);
                putVertex(consumer, poseMatrix, endB, endU, endV, red, green, blue, alpha);
                putVertex(consumer, poseMatrix, endA, startU, endV, red, green, blue, alpha);
            }
        }
    }

    /**
     * 口部 UV 圆球。极轴沿喷流朝向，赤道正好对上圆管第一圈。
     */
    public static void renderOriginSphere(
            PoseStack poseStack,
            VertexConsumer consumer,
            Vec3 forwardAxis,
            Vec3 rightAxis,
            Vec3 upAxis,
            float radiusBlocks,
            int colorArgb
    ) {
        int stackCount = Math.max(4, CometAzurFx.JET_BEAM_SPHERE_STACK_COUNT);
        int sliceCount = Math.max(6, CometAzurFx.JET_BEAM_SPHERE_SLICE_COUNT);
        Matrix4f poseMatrix = poseStack.last().pose();
        int red = unpackRed(colorArgb);
        int green = unpackGreen(colorArgb);
        int blue = unpackBlue(colorArgb);
        int alpha = unpackAlpha(colorArgb);

        for (int stackIndex = 0; stackIndex < stackCount; stackIndex++) {
            float startPhi = (float) (Math.PI * stackIndex / stackCount);
            float endPhi = (float) (Math.PI * (stackIndex + 1) / stackCount);
            float startV = Mth.lerp(stackIndex / (float) stackCount, 0.08f, 0.45f);
            float endV = Mth.lerp((stackIndex + 1) / (float) stackCount, 0.08f, 0.45f);

            for (int sliceIndex = 0; sliceIndex < sliceCount; sliceIndex++) {
                float startTheta = (float) (Math.PI * 2.0 * sliceIndex / sliceCount);
                float endTheta = (float) (Math.PI * 2.0 * (sliceIndex + 1) / sliceCount);
                float startU = Mth.lerp(sliceIndex / (float) sliceCount, CYLINDER_TEXTURE_U_MIN, CYLINDER_TEXTURE_U_MAX);
                float endU = Mth.lerp((sliceIndex + 1) / (float) sliceCount, CYLINDER_TEXTURE_U_MIN, CYLINDER_TEXTURE_U_MAX);

                Vec3 southWest = spherePoint(forwardAxis, rightAxis, upAxis, radiusBlocks, startPhi, startTheta);
                Vec3 southEast = spherePoint(forwardAxis, rightAxis, upAxis, radiusBlocks, startPhi, endTheta);
                Vec3 northEast = spherePoint(forwardAxis, rightAxis, upAxis, radiusBlocks, endPhi, endTheta);
                Vec3 northWest = spherePoint(forwardAxis, rightAxis, upAxis, radiusBlocks, endPhi, startTheta);

                putVertex(consumer, poseMatrix, southWest, startU, startV, red, green, blue, alpha);
                putVertex(consumer, poseMatrix, southEast, endU, startV, red, green, blue, alpha);
                putVertex(consumer, poseMatrix, northEast, endU, endV, red, green, blue, alpha);
                putVertex(consumer, poseMatrix, northWest, startU, endV, red, green, blue, alpha);
            }
        }
    }

    /**
     * 口部相机朝向光晕。圆球网格负责体积，这一层用圆形光斑把轮廓收成球。
     */
    public static void renderOriginGlowBillboard(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            org.joml.Quaternionf cameraOrientation,
            float radiusBlocks,
            int colorArgb
    ) {
        poseStack.pushPose();
        poseStack.mulPose(cameraOrientation);
        poseStack.scale(radiusBlocks, radiusBlocks, radiusBlocks);
        VertexConsumer consumer = bufferSource.getBuffer(CometAzurJetRenderTypes.ORIGIN_GLOW);
        Matrix4f poseMatrix = poseStack.last().pose();
        int red = unpackRed(colorArgb);
        int green = unpackGreen(colorArgb);
        int blue = unpackBlue(colorArgb);
        int alpha = unpackAlpha(colorArgb);
        putLocalQuad(consumer, poseMatrix, red, green, blue, alpha);
        poseStack.popPose();
    }

    private static Vec3 ringPoint(
            Vec3 center,
            Vec3 rightAxis,
            Vec3 upAxis,
            float radiusBlocks,
            float angleRadians
    ) {
        double cosineAngle = Math.cos(angleRadians);
        double sineAngle = Math.sin(angleRadians);
        return center
                .add(rightAxis.scale(radiusBlocks * cosineAngle))
                .add(upAxis.scale(radiusBlocks * sineAngle));
    }

    /**
     * {@code phi=0} 朝喷流前方，{@code phi=π/2} 在口部赤道（圆管接口）。
     */
    private static Vec3 spherePoint(
            Vec3 forwardAxis,
            Vec3 rightAxis,
            Vec3 upAxis,
            float radiusBlocks,
            float phiRadians,
            float thetaRadians
    ) {
        double sinPhi = Math.sin(phiRadians);
        double cosPhi = Math.cos(phiRadians);
        double cosTheta = Math.cos(thetaRadians);
        double sinTheta = Math.sin(thetaRadians);
        return forwardAxis.scale(radiusBlocks * cosPhi)
                .add(rightAxis.scale(radiusBlocks * sinPhi * cosTheta))
                .add(upAxis.scale(radiusBlocks * sinPhi * sinTheta));
    }

    private static void putLocalQuad(
            VertexConsumer consumer,
            Matrix4f poseMatrix,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        putVertex(consumer, poseMatrix, new Vec3(-1.0, -1.0, 0.0), 0.0f, 1.0f, red, green, blue, alpha);
        putVertex(consumer, poseMatrix, new Vec3(1.0, -1.0, 0.0), 1.0f, 1.0f, red, green, blue, alpha);
        putVertex(consumer, poseMatrix, new Vec3(1.0, 1.0, 0.0), 1.0f, 0.0f, red, green, blue, alpha);
        putVertex(consumer, poseMatrix, new Vec3(-1.0, 1.0, 0.0), 0.0f, 0.0f, red, green, blue, alpha);
    }

    private static void putVertex(
            VertexConsumer consumer,
            Matrix4f poseMatrix,
            Vec3 localPosition,
            float u,
            float v,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        consumer.addVertex(
                        poseMatrix,
                        (float) localPosition.x,
                        (float) localPosition.y,
                        (float) localPosition.z
                )
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 1.0f, 0.0f);
    }

    private static int unpackRed(int argb) {
        return (argb >> 16) & 0xFF;
    }

    private static int unpackGreen(int argb) {
        return (argb >> 8) & 0xFF;
    }

    private static int unpackBlue(int argb) {
        return argb & 0xFF;
    }

    private static int unpackAlpha(int argb) {
        return Mth.clamp((argb >> 24) & 0xFF, 0, 255);
    }
}

package com.eldenring.spells.client.render;

import com.eldenring.spells.client.render.glintstone.GlintstoneCometModels;
import com.eldenring.spells.client.render.glintstone.GlintstoneTrailRenderer;
import com.eldenring.spells.entity.CometAzurJetEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import com.eldenring.spells.particle.cometazur.CometAzurFx;

/**
 * 彗星亚兹勒星河喷流。
 * <p>
 * 本体是口部圆球 + 沿锁定朝向的墨绿圆柱管。
 * 细丝带中轴波纹，周围粒子换成星云 / 星团 / 闪星套管，读成喷射而出的星河。
 */
public class CometAzurJetRenderer extends EntityRenderer<CometAzurJetEntity> {

    public CometAzurJetRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(
            CometAzurJetEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        Vec3 mouthWorld = new Vec3(
                Mth.lerp(partialTicks, entity.xo, entity.getX()),
                Mth.lerp(partialTicks, entity.yo, entity.getY()),
                Mth.lerp(partialTicks, entity.zo, entity.getZ())
        );
        float yawDegrees = entity.syncedYawDegrees();
        float pitchDegrees = entity.syncedPitchDegrees();
        Vec3 forwardAxis = Vec3.directionFromRotation(pitchDegrees, yawDegrees);
        float beamLengthBlocks = Math.max(0.75f, entity.beamLengthBlocks());
        float animationTicks = entity.tickCount + partialTicks;
        Vec3 cameraWorld = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 rightAxis = forwardAxis.cross(worldUp);
        if (rightAxis.lengthSqr() < 1.0e-8) {
            rightAxis = new Vec3(1.0, 0.0, 0.0);
        } else {
            rightAxis = rightAxis.normalize();
        }
        Vec3 upAxis = rightAxis.cross(forwardAxis).normalize();

        float mouthRadius = CometAzurFx.JET_BEAM_MOUTH_RADIUS_BLOCKS;
        float tipRadius = CometAzurFx.JET_BEAM_TIP_RADIUS_BLOCKS;

        VertexConsumer cylinderConsumer = bufferSource.getBuffer(CometAzurJetRenderTypes.CYLINDER);
        CometAzurJetMesh.renderOriginSphere(
                poseStack,
                cylinderConsumer,
                forwardAxis,
                rightAxis,
                upAxis,
                CometAzurFx.JET_BEAM_ORIGIN_SPHERE_RADIUS_BLOCKS,
                CometAzurFx.JET_BEAM_NEBULA_COLOR_ARGB
        );
        CometAzurJetMesh.renderOriginSphere(
                poseStack,
                cylinderConsumer,
                forwardAxis,
                rightAxis,
                upAxis,
                CometAzurFx.JET_BEAM_ORIGIN_SPHERE_RADIUS_BLOCKS * 0.62f,
                CometAzurFx.JET_BEAM_CORE_COLOR_ARGB
        );
        // 外雾 → 星云 → 中管，真正的圆管截面。
        renderCylinderShell(
                poseStack,
                cylinderConsumer,
                forwardAxis,
                rightAxis,
                upAxis,
                beamLengthBlocks,
                mouthRadius * CometAzurFx.JET_BEAM_VEIL_WIDTH_SCALE,
                tipRadius * CometAzurFx.JET_BEAM_VEIL_WIDTH_SCALE,
                CometAzurFx.JET_BEAM_VEIL_COLOR_ARGB
        );
        renderCylinderShell(
                poseStack,
                cylinderConsumer,
                forwardAxis,
                rightAxis,
                upAxis,
                beamLengthBlocks,
                mouthRadius * CometAzurFx.JET_BEAM_NEBULA_WIDTH_SCALE,
                tipRadius * CometAzurFx.JET_BEAM_NEBULA_WIDTH_SCALE,
                CometAzurFx.JET_BEAM_NEBULA_COLOR_ARGB
        );
        renderCylinderShell(
                poseStack,
                cylinderConsumer,
                forwardAxis,
                rightAxis,
                upAxis,
                beamLengthBlocks,
                mouthRadius,
                tipRadius,
                CometAzurFx.JET_BEAM_MID_COLOR_ARGB
        );

        VertexConsumer coreConsumer = bufferSource.getBuffer(CometAzurJetRenderTypes.CORE);
        renderCylinderShell(
                poseStack,
                coreConsumer,
                forwardAxis,
                rightAxis,
                upAxis,
                beamLengthBlocks,
                mouthRadius * CometAzurFx.JET_BEAM_CORE_WIDTH_SCALE,
                tipRadius * CometAzurFx.JET_BEAM_CORE_WIDTH_SCALE,
                CometAzurFx.JET_BEAM_CORE_COLOR_ARGB
        );

        CometAzurJetMesh.renderOriginGlowBillboard(
                poseStack,
                bufferSource,
                this.entityRenderDispatcher.cameraOrientation(),
                CometAzurFx.JET_BEAM_ORIGIN_GLOW_RADIUS_BLOCKS,
                CometAzurFx.JET_BEAM_CORE_COLOR_ARGB
        );

        renderRotatingFilaments(
                poseStack,
                bufferSource,
                mouthWorld,
                cameraWorld,
                forwardAxis,
                rightAxis,
                upAxis,
                beamLengthBlocks,
                animationTicks
        );

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private static void renderCylinderShell(
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
        CometAzurJetMesh.renderCylinderLayer(
                poseStack,
                consumer,
                forwardAxis,
                rightAxis,
                upAxis,
                beamLengthBlocks,
                mouthRadiusBlocks,
                tipRadiusBlocks,
                colorArgb
        );
    }

    /**
     * 绕圆柱旋转的星河细丝。仍用曲线 ribbon，这是「转着的线」而不是柱体截面。
     */
    private static void renderRotatingFilaments(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Vec3 mouthWorld,
            Vec3 cameraWorld,
            Vec3 forwardAxis,
            Vec3 rightAxis,
            Vec3 upAxis,
            float beamLengthBlocks,
            float animationTicks
    ) {
        float mouthTextureV = CometAzurFx.JET_BEAM_TEXTURE_MOUTH_V;
        float tipTextureV = CometAzurFx.JET_BEAM_TEXTURE_TIP_V;
        int filamentCount = Math.max(1, CometAzurFx.JET_BEAM_FILAMENT_COUNT);
        for (int filamentIndex = 0; filamentIndex < filamentCount; filamentIndex++) {
            float phaseRadians = (float) (Math.PI * 2.0 * filamentIndex / filamentCount);
            List<Vec3> filamentPath = buildHelixFilament(
                    mouthWorld,
                    forwardAxis,
                    rightAxis,
                    upAxis,
                    beamLengthBlocks,
                    animationTicks,
                    phaseRadians
            );
            int filamentColor = (filamentIndex & 1) == 0
                    ? CometAzurFx.JET_BEAM_FILAMENT_COLOR_ARGB
                    : CometAzurFx.JET_BEAM_FILAMENT_ALT_COLOR_ARGB;
            GlintstoneTrailRenderer.renderPolylineRibbonLayer(
                    poseStack,
                    bufferSource,
                    mouthWorld,
                    cameraWorld,
                    filamentPath,
                    CometAzurFx.JET_BEAM_FILAMENT_HALF_WIDTH_BLOCKS,
                    CometAzurFx.JET_BEAM_FILAMENT_HALF_WIDTH_BLOCKS * 0.72f,
                    filamentColor,
                    true,
                    mouthTextureV,
                    tipTextureV
            );
        }

        for (int filamentIndex = 0; filamentIndex < Math.max(2, filamentCount / 2); filamentIndex++) {
            float phaseRadians = (float) (Math.PI * 2.0 * filamentIndex / Math.max(2, filamentCount / 2))
                    + 0.55f;
            List<Vec3> innerFilamentPath = buildHelixFilament(
                    mouthWorld,
                    forwardAxis,
                    rightAxis,
                    upAxis,
                    beamLengthBlocks,
                    animationTicks,
                    phaseRadians,
                    CometAzurFx.JET_BEAM_FILAMENT_RADIUS_BLOCKS * 0.55f,
                    -CometAzurFx.JET_BEAM_FILAMENT_TWIST_RADIANS_PER_BLOCK * 0.85f,
                    -CometAzurFx.JET_BEAM_FILAMENT_SPIN_RADIANS_PER_TICK
            );
            GlintstoneTrailRenderer.renderPolylineRibbonLayer(
                    poseStack,
                    bufferSource,
                    mouthWorld,
                    cameraWorld,
                    innerFilamentPath,
                    CometAzurFx.JET_BEAM_FILAMENT_HALF_WIDTH_BLOCKS * 0.70f,
                    CometAzurFx.JET_BEAM_FILAMENT_HALF_WIDTH_BLOCKS * 0.50f,
                    CometAzurFx.JET_BEAM_CORE_COLOR_ARGB,
                    true,
                    mouthTextureV,
                    tipTextureV
            );
        }
    }

    /**
     * 实体碰撞箱只有喷流口那么大；旁观者看整根柱子时口可能在视锥外。
     */
    @Override
    public boolean shouldRender(
            CometAzurJetEntity entity,
            Frustum frustum,
            double cameraX,
            double cameraY,
            double cameraZ
    ) {
        Vec3 mouthWorld = entity.position();
        Vec3 tipWorld = mouthWorld.add(
                Vec3.directionFromRotation(entity.syncedPitchDegrees(), entity.syncedYawDegrees())
                        .scale(Math.max(0.75f, entity.beamLengthBlocks()))
        );
        float inflateBlocks = CometAzurFx.JET_FIELD_EULER_RING_RADIUS_BLOCKS
                + CometAzurFx.JET_FIELD_GLOW_QUAD_SIZE_BLOCKS
                + 0.75f;
        AABB beamBox = new AABB(mouthWorld, tipWorld).inflate(inflateBlocks);
        return frustum.isVisible(beamBox);
    }

    private static List<Vec3> buildHelixFilament(
            Vec3 mouthWorld,
            Vec3 forwardAxis,
            Vec3 rightAxis,
            Vec3 upAxis,
            float beamLengthBlocks,
            float animationTicks,
            float phaseRadians
    ) {
        return buildHelixFilament(
                mouthWorld,
                forwardAxis,
                rightAxis,
                upAxis,
                beamLengthBlocks,
                animationTicks,
                phaseRadians,
                CometAzurFx.JET_BEAM_FILAMENT_RADIUS_BLOCKS,
                CometAzurFx.JET_BEAM_FILAMENT_TWIST_RADIANS_PER_BLOCK,
                CometAzurFx.JET_BEAM_FILAMENT_SPIN_RADIANS_PER_TICK
        );
    }

    /**
     * 绕喷流轴的螺旋细丝：只负责星河曲线，不表示光束截面。
     */
    private static List<Vec3> buildHelixFilament(
            Vec3 mouthWorld,
            Vec3 forwardAxis,
            Vec3 rightAxis,
            Vec3 upAxis,
            float beamLengthBlocks,
            float animationTicks,
            float phaseRadians,
            float radiusBlocks,
            float twistRadiansPerBlock,
            float spinRadiansPerTick
    ) {
        int sampleCount = Math.max(4, CometAzurFx.JET_BEAM_SAMPLE_COUNT);
        List<Vec3> points = new ArrayList<>(sampleCount);
        float spinRadians = animationTicks * spinRadiansPerTick;
        float wavePhaseRadians = animationTicks * CometAzurFx.JET_BEAM_RIVER_WAVE_PHASE_RADIANS_PER_TICK;
        float waveAmplitudeBlocks = CometAzurFx.JET_BEAM_RIVER_WAVE_AMPLITUDE_BLOCKS;
        float waveFrequencyPerBlock = CometAzurFx.JET_BEAM_RIVER_WAVE_FREQUENCY_PER_BLOCK;
        for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
            float progress = sampleIndex / (float) (sampleCount - 1);
            float alongBlocks = beamLengthBlocks * progress;
            float helixRadians = phaseRadians + spinRadians + alongBlocks * twistRadiansPerBlock;
            float localRadiusBlocks = radiusBlocks * Mth.lerp(
                    progress,
                    CometAzurFx.JET_BEAM_FILAMENT_MOUTH_RADIUS_SCALE,
                    CometAzurFx.JET_BEAM_FILAMENT_TIP_RADIUS_SCALE
            );
            // 中轴轻微起伏，细丝跟着走，读成流动的星河而不是直电缆。
            float riverWaveRightBlocks = waveAmplitudeBlocks
                    * (float) Math.sin(alongBlocks * waveFrequencyPerBlock + wavePhaseRadians);
            float riverWaveUpBlocks = waveAmplitudeBlocks * 0.65f
                    * (float) Math.cos(alongBlocks * waveFrequencyPerBlock * 0.83f + wavePhaseRadians + phaseRadians);
            double cosineAngle = Math.cos(helixRadians);
            double sineAngle = Math.sin(helixRadians);
            points.add(
                    mouthWorld
                            .add(forwardAxis.scale(alongBlocks))
                            .add(rightAxis.scale(localRadiusBlocks * cosineAngle + riverWaveRightBlocks))
                            .add(upAxis.scale(localRadiusBlocks * sineAngle + riverWaveUpBlocks))
            );
        }
        return points;
    }

    @Override
    public ResourceLocation getTextureLocation(CometAzurJetEntity entity) {
        return GlintstoneCometModels.TRAIL_BEAM_TEXTURE;
    }
}

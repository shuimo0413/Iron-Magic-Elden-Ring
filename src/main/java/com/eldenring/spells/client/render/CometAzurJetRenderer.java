package com.eldenring.spells.client.render;

import com.eldenring.spells.client.render.glintstone.GlintstoneCometModels;
import com.eldenring.spells.client.render.glintstone.GlintstoneTrailRenderer;
import com.eldenring.spells.entity.CometAzurJetEntity;
import com.eldenring.spells.tuning.CometAzurTuning;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 彗星亚兹勒星河喷流：用多层自发光 ribbon 画墨绿星河柱，而不是密粒子激光。
 * <p>
 * 原理同辉石曲线拖尾（billboard 光带 + 可选 Chaikin），但路径是「当前射线」而不是飞行历史：
 * 暗靛外雾 → 星云层 → 亮芯，再加几条绕轴螺旋细丝，保持星河质感、避免细彗星尾那种单条塑料带。
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

        List<Vec3> centerline = buildRiverCenterline(
                mouthWorld,
                forwardAxis,
                rightAxis,
                upAxis,
                beamLengthBlocks,
                animationTicks
        );

        float mouthHalfWidth = CometAzurTuning.JET_BEAM_MOUTH_HALF_WIDTH_BLOCKS;
        float tipHalfWidth = CometAzurTuning.JET_BEAM_TIP_HALF_WIDTH_BLOCKS;

        // 外雾：最宽、最暗，垫出星河体积。
        GlintstoneTrailRenderer.renderPolylineRibbonLayer(
                poseStack,
                bufferSource,
                mouthWorld,
                cameraWorld,
                centerline,
                mouthHalfWidth * CometAzurTuning.JET_BEAM_VEIL_WIDTH_SCALE,
                tipHalfWidth * CometAzurTuning.JET_BEAM_VEIL_WIDTH_SCALE,
                CometAzurTuning.JET_BEAM_VEIL_COLOR_ARGB,
                true
        );
        // 星云层：墨绿主色。
        GlintstoneTrailRenderer.renderPolylineRibbonLayer(
                poseStack,
                bufferSource,
                mouthWorld,
                cameraWorld,
                centerline,
                mouthHalfWidth * CometAzurTuning.JET_BEAM_NEBULA_WIDTH_SCALE,
                tipHalfWidth * CometAzurTuning.JET_BEAM_NEBULA_WIDTH_SCALE,
                CometAzurTuning.JET_BEAM_NEBULA_COLOR_ARGB,
                true
        );
        // 中间青绿过渡。
        GlintstoneTrailRenderer.renderPolylineRibbonLayer(
                poseStack,
                bufferSource,
                mouthWorld,
                cameraWorld,
                centerline,
                mouthHalfWidth,
                tipHalfWidth,
                CometAzurTuning.JET_BEAM_MID_COLOR_ARGB,
                true
        );
        // 亮芯：窄而亮，但不刺成白色塑料。
        GlintstoneTrailRenderer.renderPolylineRibbonLayer(
                poseStack,
                bufferSource,
                mouthWorld,
                cameraWorld,
                centerline,
                mouthHalfWidth * CometAzurTuning.JET_BEAM_CORE_WIDTH_SCALE,
                tipHalfWidth * CometAzurTuning.JET_BEAM_CORE_WIDTH_SCALE,
                CometAzurTuning.JET_BEAM_CORE_COLOR_ARGB,
                true
        );

        int filamentCount = Math.max(1, CometAzurTuning.JET_BEAM_FILAMENT_COUNT);
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
            GlintstoneTrailRenderer.renderPolylineRibbonLayer(
                    poseStack,
                    bufferSource,
                    mouthWorld,
                    cameraWorld,
                    filamentPath,
                    CometAzurTuning.JET_BEAM_FILAMENT_HALF_WIDTH_BLOCKS,
                    CometAzurTuning.JET_BEAM_FILAMENT_HALF_WIDTH_BLOCKS * 0.72f,
                    CometAzurTuning.JET_BEAM_FILAMENT_COLOR_ARGB,
                    true
            );
        }

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    /**
     * 中轴带轻微横向波纹，读作流动星河而不是死直线。
     */
    private static List<Vec3> buildRiverCenterline(
            Vec3 mouthWorld,
            Vec3 forwardAxis,
            Vec3 rightAxis,
            Vec3 upAxis,
            float beamLengthBlocks,
            float animationTicks
    ) {
        int sampleCount = Math.max(4, CometAzurTuning.JET_BEAM_SAMPLE_COUNT);
        List<Vec3> points = new ArrayList<>(sampleCount);
        float wavePhase = animationTicks * CometAzurTuning.JET_BEAM_RIVER_WAVE_PHASE_RADIANS_PER_TICK;
        for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
            float progress = sampleIndex / (float) (sampleCount - 1);
            float alongBlocks = beamLengthBlocks * progress;
            float waveRadians = alongBlocks * CometAzurTuning.JET_BEAM_RIVER_WAVE_FREQUENCY_PER_BLOCK + wavePhase;
            float waveEnvelope = Mth.sin(progress * (float) Math.PI);
            double rightOffset = Math.sin(waveRadians)
                    * CometAzurTuning.JET_BEAM_RIVER_WAVE_AMPLITUDE_BLOCKS
                    * waveEnvelope;
            double upOffset = Math.cos(waveRadians * 0.83f)
                    * CometAzurTuning.JET_BEAM_RIVER_WAVE_AMPLITUDE_BLOCKS
                    * 0.55
                    * waveEnvelope;
            points.add(
                    mouthWorld
                            .add(forwardAxis.scale(alongBlocks))
                            .add(rightAxis.scale(rightOffset))
                            .add(upAxis.scale(upOffset))
            );
        }
        return points;
    }

    /**
     * 绕喷流轴的螺旋细丝：{@code e^(iθ)} 映到 (right, up)，沿程扭 + 整体自旋。
     */
    private static List<Vec3> buildHelixFilament(
            Vec3 mouthWorld,
            Vec3 forwardAxis,
            Vec3 rightAxis,
            Vec3 upAxis,
            float beamLengthBlocks,
            float animationTicks,
            float phaseRadians
    ) {
        int sampleCount = Math.max(4, CometAzurTuning.JET_BEAM_SAMPLE_COUNT);
        List<Vec3> points = new ArrayList<>(sampleCount);
        float spinRadians = animationTicks * CometAzurTuning.JET_BEAM_FILAMENT_SPIN_RADIANS_PER_TICK;
        for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
            float progress = sampleIndex / (float) (sampleCount - 1);
            float alongBlocks = beamLengthBlocks * progress;
            float helixRadians = phaseRadians
                    + spinRadians
                    + alongBlocks * CometAzurTuning.JET_BEAM_FILAMENT_TWIST_RADIANS_PER_BLOCK;
            float radiusBlocks = CometAzurTuning.JET_BEAM_FILAMENT_RADIUS_BLOCKS
                    * (0.82f + 0.18f * Mth.sin(progress * (float) Math.PI));
            double cosineAngle = Math.cos(helixRadians);
            double sineAngle = Math.sin(helixRadians);
            points.add(
                    mouthWorld
                            .add(forwardAxis.scale(alongBlocks))
                            .add(rightAxis.scale(radiusBlocks * cosineAngle))
                            .add(upAxis.scale(radiusBlocks * sineAngle))
            );
        }
        return points;
    }

    @Override
    public ResourceLocation getTextureLocation(CometAzurJetEntity entity) {
        return GlintstoneCometModels.TRAIL_BEAM_TEXTURE;
    }
}

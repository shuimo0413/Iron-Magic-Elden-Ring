package com.eldenring.spells.particle.cometazur;

import com.eldenring.spells.client.render.AdditiveParticleRenderType;
import com.eldenring.spells.tuning.CometAzurTuning;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 喷流周围的飞粒子。
 * <p>
 * 出生在玩家面前一圈，然后沿喷流方向往前冲。平面偏移用欧拉公式
 * {@code e^(iθ) = cosθ + i sinθ} 绕喷流轴转；前向位移用显式欧拉积分。
 * 半径 / 极角再叠一层平滑噪声，避免整圈像齿轮。
 */
public class CometAzurJetSurroundParticle extends TextureSheetParticle {

    /**
     * 贴图种类。顺序必须对上 {@code particles/comet_azur_jet_surround.json}：
     * glow → spark → mote_1 → mote_2 → impact → filament。
     */
    public enum Kind {
        GLOW(CometAzurTuning.JET_GLOW_QUAD_SIZE_BLOCKS, 0.55f, true),
        SPARK(CometAzurTuning.JET_SPARK_QUAD_SIZE_BLOCKS, 0.95f, false),
        MOTE_1(CometAzurTuning.JET_MOTE_QUAD_SIZE_BLOCKS, 0.90f, false),
        MOTE_2(CometAzurTuning.JET_MOTE_QUAD_SIZE_BLOCKS, 0.90f, false),
        IMPACT(CometAzurTuning.JET_IMPACT_QUAD_SIZE_BLOCKS, 0.92f, false),
        FILAMENT(CometAzurTuning.JET_FILAMENT_QUAD_SIZE_BLOCKS, 0.85f, true);

        final float quadSizeBlocks;
        final float peakAlpha;
        /** 光晕 / 能量丝轻微自转，避免同一张贴图排成栅栏。 */
        final boolean spin;

        Kind(float quadSizeBlocks, float peakAlpha, boolean spin) {
            this.quadSizeBlocks = quadSizeBlocks;
            this.peakAlpha = peakAlpha;
            this.spin = spin;
        }

        static Kind fromOrdinal(int ordinal) {
            Kind[] values = values();
            return values[Mth.clamp(ordinal, 0, values.length - 1)];
        }
    }

    /**
     * 轨迹模式。
     */
    public enum MotionMode {
        /**
         * 绕喷流轴按欧拉公式转，同时前向加速冲击。
         */
        EULER,
        /**
         * 从出生点近似直线前飞，噪声更弱。
         */
        STRAIGHT;

        static MotionMode fromOrdinal(int ordinal) {
            MotionMode[] values = values();
            return values[Mth.clamp(ordinal, 0, values.length - 1)];
        }
    }

    private static final int SPRITE_LAST_INDEX = Kind.values().length - 1;

    private final Kind kind;
    private final MotionMode motionMode;
    private final Vec3 beamOrigin;
    private final Vec3 forwardAxis;
    private final Vec3 rightAxis;
    private final Vec3 upAxis;
    private final float ringAngleBirthRadians;
    private final float ringRadiusBlocks;
    private final float helixAngularSpeedRadiansPerTick;
    private final float noiseAmplitudeBlocks;
    private final float noiseAngleAmplitudeRadians;
    private final int noiseSeed;
    private final float birthQuadSize;
    private final float peakAlpha;
    private final float rollRadiansPerTick;

    private float alongBeamBlocks;
    private float forwardSpeedBlocksPerTick;

    protected CometAzurJetSurroundParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            CometAzurJetOptions options,
            SpriteSet sprites
    ) {
        super(level, x, y, z);
        this.kind = Kind.fromOrdinal(options.kindOrdinal());
        this.motionMode = MotionMode.fromOrdinal(options.motionOrdinal());
        this.beamOrigin = new Vec3(x, y, z);
        this.forwardAxis = Vec3.directionFromRotation(options.pitchDegrees(), options.yawDegrees());
        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 computedRightAxis = this.forwardAxis.cross(worldUp);
        if (computedRightAxis.lengthSqr() < 1.0e-8) {
            computedRightAxis = new Vec3(1.0, 0.0, 0.0);
        } else {
            computedRightAxis = computedRightAxis.normalize();
        }
        this.rightAxis = computedRightAxis;
        this.upAxis = this.rightAxis.cross(this.forwardAxis).normalize();
        this.ringAngleBirthRadians = options.ringAngleRadians();
        this.ringRadiusBlocks = options.ringRadiusBlocks();
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.hasPhysics = false;
        this.gravity = 0.0f;

        boolean eulerMotion = this.motionMode == MotionMode.EULER;
        int baseLifetimeTicks = eulerMotion
                ? CometAzurTuning.JET_EULER_LIFETIME_TICKS
                : CometAzurTuning.JET_STRAIGHT_LIFETIME_TICKS;
        this.lifetime = Math.max(4, baseLifetimeTicks + level.random.nextInt(Math.max(1, baseLifetimeTicks / 5)));
        this.forwardSpeedBlocksPerTick = eulerMotion
                ? CometAzurTuning.JET_EULER_FORWARD_SPEED_BLOCKS_PER_TICK
                : CometAzurTuning.JET_STRAIGHT_FORWARD_SPEED_BLOCKS_PER_TICK;
        this.forwardSpeedBlocksPerTick *= 0.90f + level.random.nextFloat() * 0.20f;
        this.helixAngularSpeedRadiansPerTick = eulerMotion
                ? CometAzurTuning.JET_EULER_HELIX_RADIANS_PER_TICK
                * (level.random.nextBoolean() ? 1.0f : -1.0f)
                : 0.0f;
        float noiseScale = eulerMotion ? 1.0f : CometAzurTuning.JET_STRAIGHT_NOISE_SCALE;
        this.noiseAmplitudeBlocks = CometAzurTuning.JET_NOISE_AMPLITUDE_BLOCKS * noiseScale;
        this.noiseAngleAmplitudeRadians = CometAzurTuning.JET_NOISE_ANGLE_AMPLITUDE_RADIANS * noiseScale;
        this.noiseSeed = level.random.nextInt();
        this.alongBeamBlocks = 0.0f;

        this.birthQuadSize = this.kind.quadSizeBlocks * (0.82f + level.random.nextFloat() * 0.36f);
        this.quadSize = this.birthQuadSize;
        this.peakAlpha = this.kind.peakAlpha;
        this.alpha = 0.0f;
        this.rCol = 1.0f;
        this.gCol = 1.0f;
        this.bCol = 1.0f;
        setSprite(sprites.get(this.kind.ordinal(), SPRITE_LAST_INDEX));
        if (this.kind.spin) {
            this.roll = level.random.nextFloat() * ((float) Math.PI * 2.0f);
            this.rollRadiansPerTick = (level.random.nextFloat() - 0.5f) * 0.28f;
        } else {
            this.roll = 0.0f;
            this.rollRadiansPerTick = 0.0f;
        }
        this.oRoll = this.roll;
        applyPoseAtAge(0);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        this.age++;
        if (this.age >= this.lifetime) {
            remove();
            return;
        }

        if (this.motionMode == MotionMode.EULER) {
            this.forwardSpeedBlocksPerTick +=
                    CometAzurTuning.JET_EULER_FORWARD_ACCELERATION_BLOCKS_PER_TICK_SQUARED;
        }
        this.alongBeamBlocks += this.forwardSpeedBlocksPerTick;
        this.roll += this.rollRadiansPerTick;
        applyPoseAtAge(this.age);
        applyLifetimeVisuals();
    }

    /**
     * 世界坐标 = 喷流口 + 前向距离 × 朝向 + r e^{iθ} 映到 (right, up) + 噪声。
     */
    private void applyPoseAtAge(int ageTicks) {
        float noiseTime = ageTicks * CometAzurTuning.JET_NOISE_FREQUENCY_PER_TICK;
        float radiusNoiseBlocks = this.noiseAmplitudeBlocks * interpolatedNoise(this.noiseSeed, noiseTime);
        float angleNoiseRadians = this.noiseAngleAmplitudeRadians
                * interpolatedNoise(this.noiseSeed + 19, noiseTime * 1.17f);
        float alongNoiseBlocks = this.noiseAmplitudeBlocks * 0.35f
                * interpolatedNoise(this.noiseSeed + 41, noiseTime * 0.83f);

        float helixAngleRadians = this.ringAngleBirthRadians
                + this.helixAngularSpeedRadiansPerTick * ageTicks
                + angleNoiseRadians;
        float radiusBlocks = Math.max(0.02f, this.ringRadiusBlocks + radiusNoiseBlocks);
        double cosineAngle = Math.cos(helixAngleRadians);
        double sineAngle = Math.sin(helixAngleRadians);
        Vec3 planeOffset = this.rightAxis.scale(radiusBlocks * cosineAngle)
                .add(this.upAxis.scale(radiusBlocks * sineAngle));
        double lateralNoiseRight = this.noiseAmplitudeBlocks
                * interpolatedNoise(this.noiseSeed + 67, noiseTime * 0.91f);
        double lateralNoiseUp = this.noiseAmplitudeBlocks
                * interpolatedNoise(this.noiseSeed + 89, noiseTime * 1.09f);
        Vec3 worldPosition = this.beamOrigin
                .add(this.forwardAxis.scale(this.alongBeamBlocks + alongNoiseBlocks))
                .add(planeOffset)
                .add(this.rightAxis.scale(lateralNoiseRight))
                .add(this.upAxis.scale(lateralNoiseUp));
        this.x = worldPosition.x;
        this.y = worldPosition.y;
        this.z = worldPosition.z;
    }

    /**
     * 前两 tick 淡入，最后 30% 寿命淡出并略缩小。
     */
    private void applyLifetimeVisuals() {
        float lifeFraction = (float) this.age / (float) this.lifetime;
        float fadeIn = Mth.clamp(lifeFraction / 0.12f, 0.0f, 1.0f);
        float fadeOut = lifeFraction < 0.70f
                ? 1.0f
                : 1.0f - (lifeFraction - 0.70f) / 0.30f;
        this.alpha = this.peakAlpha * fadeIn * fadeOut;
        this.quadSize = this.birthQuadSize * (0.88f + 0.12f * fadeIn) * (0.55f + 0.45f * fadeOut);
    }

    /**
     * 把整数打散到 [-1, 1]，做轨迹噪声。
     */
    private static float hashToSignedUnit(int value) {
        int hashed = value;
        hashed ^= hashed >>> 16;
        hashed *= 0x7feb352d;
        hashed ^= hashed >>> 15;
        hashed *= 0x846ca68b;
        hashed ^= hashed >>> 16;
        return (hashed & 0xFFFF) / 32767.5f - 1.0f;
    }

    /**
     * 一维平滑噪声：相邻整数格点之间用 smoothstep 插值，轨迹连续而不僵直。
     */
    private static float interpolatedNoise(int seed, float time) {
        int leftIndex = Mth.floor(time);
        float fraction = time - leftIndex;
        float smoothedFraction = fraction * fraction * (3.0f - 2.0f * fraction);
        float leftSample = hashToSignedUnit(seed * 31 + leftIndex);
        float rightSample = hashToSignedUnit(seed * 31 + leftIndex + 1);
        return Mth.lerp(smoothedFraction, leftSample, rightSample);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return this.kind == Kind.GLOW
                ? AdditiveParticleRenderType.SOFT_TRANSLUCENT
                : AdditiveParticleRenderType.ADDITIVE;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }
}

package com.eldenring.spells.particle.cometazur;

import com.eldenring.spells.client.render.AdditiveParticleRenderType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * 喷流周围的星河粒子。
 * <p>
 * 出生在玩家面前一圈（或沿喷流中段），然后沿喷流方向往前冲。
 * 平面偏移用欧拉公式 {@code e^(iθ) = cosθ + i sinθ} 绕喷流轴转；
 * 前向位移用显式欧拉积分。半径 / 极角再叠一层平滑噪声，避免整圈像齿轮。
 * 贴图走星云 / 星团 / 闪星，全部加法混合并贴着激光飞；
 * 半透明大团在白天会变成喷烟，所以不再用。
 * 碰到实心方块就消失，截断方式和喷流激光射线一致。
 */
public class CometAzurJetSurroundParticle extends TextureSheetParticle {

    /**
     * 贴图种类。顺序必须对上 {@code particles/comet_azur_jet_surround.json}。
     *
     * @param quadSizeBlocks 四边形边长（方块）
     * @param peakAlpha      寿命中段峰值透明度
     * @param tintRgb        乘到贴图上的墨绿色（0xRRGGBB）
     * @param spin           是否自转。光带 {@code GLOW} 必须为 false，贴图本身是横条，转了就不像直线。
     * @param volumeCloud    true = 星云纹理（尺寸略大）；全部走加法，不再用半透明烟团
     */
    public enum Kind {
        HAZE(
                CometAzurFx.JET_HAZE_QUAD_SIZE_BLOCKS,
                0.70f,
                CometAzurFx.JET_TINT_HAZE_RGB,
                true,
                true
        ),
        STAR_RIVER_MIST(
                CometAzurFx.JET_STAR_RIVER_MIST_QUAD_SIZE_BLOCKS,
                0.78f,
                CometAzurFx.JET_TINT_STAR_RIVER_MIST_RGB,
                true,
                true
        ),
        STARDUST(
                CometAzurFx.JET_STARDUST_QUAD_SIZE_BLOCKS,
                0.80f,
                CometAzurFx.JET_TINT_STARDUST_RGB,
                true,
                true
        ),
        WISP(
                CometAzurFx.JET_WISP_QUAD_SIZE_BLOCKS,
                0.72f,
                CometAzurFx.JET_TINT_WISP_RGB,
                true,
                true
        ),
        SPIRAL(
                CometAzurFx.JET_SPIRAL_QUAD_SIZE_BLOCKS,
                0.88f,
                CometAzurFx.JET_TINT_SPIRAL_RGB,
                true,
                false
        ),
        CLUSTER(
                CometAzurFx.JET_CLUSTER_QUAD_SIZE_BLOCKS,
                0.90f,
                CometAzurFx.JET_TINT_CLUSTER_RGB,
                false,
                false
        ),
        COMET_MIST(
                CometAzurFx.JET_COMET_MIST_QUAD_SIZE_BLOCKS,
                0.80f,
                CometAzurFx.JET_TINT_COMET_MIST_RGB,
                true,
                false
        ),
        DUST(
                CometAzurFx.JET_DUST_QUAD_SIZE_BLOCKS,
                0.85f,
                CometAzurFx.JET_TINT_DUST_RGB,
                false,
                false
        ),
        HEAD(
                CometAzurFx.JET_HEAD_QUAD_SIZE_BLOCKS,
                0.92f,
                CometAzurFx.JET_TINT_HEAD_RGB,
                false,
                false
        ),
        NOVA(
                CometAzurFx.JET_NOVA_QUAD_SIZE_BLOCKS,
                0.95f,
                CometAzurFx.JET_TINT_NOVA_RGB,
                false,
                false
        ),
        FILAMENT(
                CometAzurFx.JET_FILAMENT_QUAD_SIZE_BLOCKS,
                0.72f,
                CometAzurFx.JET_TINT_FILAMENT_RGB,
                true,
                false
        ),
        STREAK(
                CometAzurFx.JET_STREAK_QUAD_SIZE_BLOCKS,
                0.82f,
                CometAzurFx.JET_TINT_STREAK_RGB,
                false,
                false
        ),
        MOTE(
                CometAzurFx.JET_MOTE_QUAD_SIZE_BLOCKS,
                0.90f,
                CometAzurFx.JET_TINT_MOTE_RGB,
                false,
                false
        ),
        IMPACT(
                CometAzurFx.JET_FIELD_IMPACT_QUAD_SIZE_BLOCKS,
                0.94f,
                CometAzurFx.JET_TINT_FIELD_IMPACT_RGB,
                false,
                false
        ),
        MOTE_1(
                CometAzurFx.JET_FIELD_MOTE_1_QUAD_SIZE_BLOCKS,
                0.92f,
                CometAzurFx.JET_TINT_FIELD_MOTE_1_RGB,
                false,
                false
        ),
        MOTE_2(
                CometAzurFx.JET_FIELD_MOTE_2_QUAD_SIZE_BLOCKS,
                0.92f,
                CometAzurFx.JET_TINT_FIELD_MOTE_2_RGB,
                false,
                false
        ),
        GLINT_MOTE(
                CometAzurFx.JET_FIELD_GLINT_MOTE_QUAD_SIZE_BLOCKS,
                0.93f,
                CometAzurFx.JET_TINT_FIELD_GLINT_MOTE_RGB,
                false,
                false
        ),
        GLOW(
                CometAzurFx.JET_FIELD_GLOW_QUAD_SIZE_BLOCKS,
                0.78f,
                CometAzurFx.JET_TINT_FIELD_GLOW_RGB,
                false,
                false
        );

        final float quadSizeBlocks;
        final float peakAlpha;
        final int tintRgb;
        final boolean spin;
        final boolean volumeCloud;

        Kind(float quadSizeBlocks, float peakAlpha, int tintRgb, boolean spin, boolean volumeCloud) {
            this.quadSizeBlocks = quadSizeBlocks;
            this.peakAlpha = peakAlpha;
            this.tintRgb = tintRgb;
            this.spin = spin;
            this.volumeCloud = volumeCloud;
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
         * 星云纹理：跟激光同速贴轴飞，绕轴轻转。
         */
        DRIFT,
        /**
         * 绕喷流轴按欧拉公式转，同时前向加速，读成螺旋星河臂。
         */
        EULER,
        /**
         * 从出生点近似直线前飞，噪声更弱，给闪星和彗星残影。
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

        int baseLifetimeTicks = lifetimeTicksFor(this.motionMode);
        this.lifetime = Math.max(4, baseLifetimeTicks + level.random.nextInt(Math.max(1, baseLifetimeTicks / 5)));
        this.forwardSpeedBlocksPerTick = forwardSpeedFor(this.motionMode);
        this.forwardSpeedBlocksPerTick *= 0.90f + level.random.nextFloat() * 0.20f;
        this.helixAngularSpeedRadiansPerTick = options.helixRadiansPerTick() != 0.0f
                ? options.helixRadiansPerTick()
                : helixSpeedFor(this.motionMode, level.random.nextBoolean());
        float noiseScale = noiseScaleFor(this.motionMode);
        this.noiseAmplitudeBlocks = CometAzurFx.JET_NOISE_AMPLITUDE_BLOCKS * noiseScale;
        this.noiseAngleAmplitudeRadians = CometAzurFx.JET_NOISE_ANGLE_AMPLITUDE_RADIANS * noiseScale;
        this.noiseSeed = level.random.nextInt();
        this.alongBeamBlocks = Math.max(0.0f, options.birthAlongBeamBlocks());
        if (this.alongBeamBlocks >= CometAzurFx.JET_PARTICLE_MAX_ALONG_BLOCKS) {
            this.lifetime = 1;
        } else {
            float remainingBlocks = CometAzurFx.JET_PARTICLE_MAX_ALONG_BLOCKS - this.alongBeamBlocks;
            int ticksUntilRangeEnd = Math.max(
                    4,
                    (int) Math.ceil(remainingBlocks / Math.max(0.05f, this.forwardSpeedBlocksPerTick))
            );
            this.lifetime = Math.min(this.lifetime, ticksUntilRangeEnd);
        }

        this.birthQuadSize = this.kind.quadSizeBlocks * (0.82f + level.random.nextFloat() * 0.36f);
        this.quadSize = this.birthQuadSize;
        this.peakAlpha = this.kind.peakAlpha;
        this.alpha = 0.0f;
        applyInkGreenTint(level.random.nextFloat());
        setSprite(sprites.get(this.kind.ordinal(), SPRITE_LAST_INDEX));
        if (this.kind.spin) {
            this.roll = level.random.nextFloat() * ((float) Math.PI * 2.0f);
            float spinScale = this.kind == Kind.SPIRAL ? 0.42f : 0.22f;
            this.rollRadiansPerTick = (level.random.nextFloat() - 0.5f) * spinScale;
        } else {
            this.roll = 0.0f;
            this.rollRadiansPerTick = 0.0f;
        }
        this.oRoll = this.roll;
        applyPoseAtAge(0);
        if (isBlockedBySolid(this.beamOrigin, new Vec3(this.x, this.y, this.z))) {
            this.alpha = 0.0f;
            remove();
        }
    }

    private static int lifetimeTicksFor(MotionMode motionMode) {
        return switch (motionMode) {
            case DRIFT -> CometAzurFx.JET_NEBULA_LIFETIME_TICKS;
            case EULER -> CometAzurFx.JET_GALAXY_LIFETIME_TICKS;
            case STRAIGHT -> CometAzurFx.JET_SPARKLE_LIFETIME_TICKS;
        };
    }

    private static float forwardSpeedFor(MotionMode motionMode) {
        return switch (motionMode) {
            case DRIFT -> CometAzurFx.JET_NEBULA_FORWARD_SPEED_BLOCKS_PER_TICK;
            case EULER -> CometAzurFx.JET_GALAXY_FORWARD_SPEED_BLOCKS_PER_TICK;
            case STRAIGHT -> CometAzurFx.JET_SPARKLE_FORWARD_SPEED_BLOCKS_PER_TICK;
        };
    }

    private static float helixSpeedFor(MotionMode motionMode, boolean clockwise) {
        float baseRadiansPerTick = switch (motionMode) {
            case DRIFT -> CometAzurFx.JET_NEBULA_HELIX_RADIANS_PER_TICK;
            case EULER -> CometAzurFx.JET_GALAXY_HELIX_RADIANS_PER_TICK;
            case STRAIGHT -> 0.0f;
        };
        return clockwise ? baseRadiansPerTick : -baseRadiansPerTick;
    }

    private static float noiseScaleFor(MotionMode motionMode) {
        return switch (motionMode) {
            case DRIFT -> 1.0f;
            case EULER -> CometAzurFx.JET_GALAXY_NOISE_SCALE;
            case STRAIGHT -> CometAzurFx.JET_SPARKLE_NOISE_SCALE;
        };
    }

    /**
     * 把 0xRRGGBB 乘到贴图上，再轻微抖动，避免整河同一块绿。
     */
    private void applyInkGreenTint(float randomUnit) {
        float tintJitter = 0.88f + randomUnit * 0.22f;
        this.rCol = Mth.clamp(channelFromRgb(this.kind.tintRgb, 16) * tintJitter, 0.0f, 1.0f);
        this.gCol = Mth.clamp(channelFromRgb(this.kind.tintRgb, 8) * tintJitter, 0.0f, 1.0f);
        this.bCol = Mth.clamp(channelFromRgb(this.kind.tintRgb, 0) * tintJitter, 0.0f, 1.0f);
    }

    private static float channelFromRgb(int rgb, int shift) {
        return ((rgb >> shift) & 0xFF) / 255.0f;
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
                    CometAzurFx.JET_GALAXY_FORWARD_ACCELERATION_BLOCKS_PER_TICK_SQUARED;
        }
        this.alongBeamBlocks += this.forwardSpeedBlocksPerTick;
        if (this.alongBeamBlocks >= CometAzurFx.JET_PARTICLE_MAX_ALONG_BLOCKS) {
            remove();
            return;
        }
        this.roll += this.rollRadiansPerTick;
        applyPoseAtAge(this.age);
        if (isBlockedBySolid(new Vec3(this.xo, this.yo, this.zo), new Vec3(this.x, this.y, this.z))) {
            remove();
            return;
        }
        applyLifetimeVisuals();
    }

    /**
     * 世界坐标 = 喷流口 + 前向距离 × 朝向 + r e^{iθ} 映到 (right, up) + 噪声。
     */
    private void applyPoseAtAge(int ageTicks) {
        float noiseTime = ageTicks * CometAzurFx.JET_NOISE_FREQUENCY_PER_TICK;
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
     * 与喷流本体相同：沿轨迹做 COLLIDER 射线，碰到实心方块就该消失。
     * 中段补粒子也会从喷流口扫到出生点，避免穿墙刷在墙后。
     */
    private boolean isBlockedBySolid(Vec3 fromWorld, Vec3 toWorld) {
        if (toWorld.distanceToSqr(fromWorld) < 1.0e-10) {
            return isInsideSolidBlock(toWorld);
        }
        BlockHitResult blockHit = this.level.clip(new ClipContext(
                fromWorld,
                toWorld,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
        ));
        if (blockHit.getType() != HitResult.Type.MISS) {
            return true;
        }
        return isInsideSolidBlock(toWorld);
    }

    /**
     * 终点已经嵌进方块碰撞箱时也算命中（贴墙螺旋会绕进墙里）。
     */
    private boolean isInsideSolidBlock(Vec3 worldPosition) {
        BlockPos blockPos = BlockPos.containing(worldPosition.x, worldPosition.y, worldPosition.z);
        return !this.level.getBlockState(blockPos).getCollisionShape(this.level, blockPos).isEmpty();
    }

    /**
     * 贴着激光淡入淡出，不胀开。胀开就会再次读成喷烟。
     */
    private void applyLifetimeVisuals() {
        float lifeFraction = (float) this.age / (float) this.lifetime;
        float fadeIn = Mth.clamp(lifeFraction / 0.10f, 0.0f, 1.0f);
        float fadeOut = lifeFraction < 0.72f
                ? 1.0f
                : 1.0f - (lifeFraction - 0.72f) / 0.28f;
        this.alpha = this.peakAlpha * fadeIn * fadeOut;
        if (this.kind == Kind.NOVA || this.kind == Kind.MOTE || this.kind == Kind.HEAD
                || this.kind == Kind.IMPACT || this.kind == Kind.MOTE_1 || this.kind == Kind.MOTE_2
                || this.kind == Kind.GLINT_MOTE) {
            float twinkle = 0.70f + 0.30f * (0.5f + 0.5f * Mth.sin(this.age * 0.55f + this.noiseSeed));
            this.alpha *= twinkle;
        }
        this.quadSize = this.birthQuadSize * (0.90f + 0.10f * fadeIn) * (0.70f + 0.30f * fadeOut);
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
        return AdditiveParticleRenderType.ADDITIVE;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }
}

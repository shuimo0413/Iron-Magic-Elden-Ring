package com.eldenring.spells.particle.cometazur;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * 喷流口发射器：服务端每圈只同步这一颗，客户端再铺星云 / 星系 / 闪星三层。
 * 自己不画。
 */
public class CometAzurJetEmitterParticle extends TextureSheetParticle {

    /** 贴着激光的星云纹理：小、亮、加法。 */
    private static final CometAzurJetSurroundParticle.Kind[] NEBULA_KINDS = {
            CometAzurJetSurroundParticle.Kind.STAR_RIVER_MIST,
            CometAzurJetSurroundParticle.Kind.STARDUST,
            CometAzurJetSurroundParticle.Kind.WISP,
            CometAzurJetSurroundParticle.Kind.COMET_MIST
    };

    /** 激光内部螺旋星团。 */
    private static final CometAzurJetSurroundParticle.Kind[] GALAXY_KINDS = {
            CometAzurJetSurroundParticle.Kind.SPIRAL,
            CometAzurJetSurroundParticle.Kind.CLUSTER,
            CometAzurJetSurroundParticle.Kind.DUST,
            CometAzurJetSurroundParticle.Kind.HAZE
    };

    /** 激光内部闪星。 */
    private static final CometAzurJetSurroundParticle.Kind[] SPARKLE_KINDS = {
            CometAzurJetSurroundParticle.Kind.NOVA,
            CometAzurJetSurroundParticle.Kind.HEAD,
            CometAzurJetSurroundParticle.Kind.MOTE,
            CometAzurJetSurroundParticle.Kind.STREAK
    };

    /** 外围欧拉螺旋：小颗闪星，围着喷流拧。 */
    private static final CometAzurJetSurroundParticle.Kind[] FIELD_EULER_KINDS = {
            CometAzurJetSurroundParticle.Kind.IMPACT,
            CometAzurJetSurroundParticle.Kind.MOTE_1,
            CometAzurJetSurroundParticle.Kind.MOTE_2,
            CometAzurJetSurroundParticle.Kind.GLINT_MOTE
    };

    /** 外围直线能量线：横向光带，不旋转。 */
    private static final CometAzurJetSurroundParticle.Kind[] FIELD_STRAIGHT_KINDS = {
            CometAzurJetSurroundParticle.Kind.GLOW
    };

    /** 沿喷流中段补的能量场小星点。 */
    private static final CometAzurJetSurroundParticle.Kind[] FILL_KINDS = {
            CometAzurJetSurroundParticle.Kind.IMPACT,
            CometAzurJetSurroundParticle.Kind.MOTE_1,
            CometAzurJetSurroundParticle.Kind.SPIRAL,
            CometAzurJetSurroundParticle.Kind.GLINT_MOTE,
            CometAzurJetSurroundParticle.Kind.GLOW
    };

    private final float yawDegrees;
    private final float pitchDegrees;
    private boolean spawnedRing;

    protected CometAzurJetEmitterParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            CometAzurJetOptions options
    ) {
        super(level, x, y, z);
        this.yawDegrees = options.yawDegrees();
        this.pitchDegrees = options.pitchDegrees();
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.hasPhysics = false;
        this.gravity = 0.0f;
        this.lifetime = 2;
        this.quadSize = 0.001f;
        this.alpha = 0.0f;
    }

    @Override
    public void tick() {
        if (!this.spawnedRing) {
            this.spawnedRing = true;
            spawnSurroundRings();
        }
        remove();
    }

    /**
     * 内层贴着激光；外层用多条欧拉螺旋 + 几道直线做成更大的能量场。
     */
    private void spawnSurroundRings() {
        float ringPhaseRadians = this.random.nextFloat() * ((float) Math.PI * 2.0f);
        spawnRing(
                CometAzurFx.JET_NEBULA_PARTICLE_COUNT,
                CometAzurFx.JET_NEBULA_RING_RADIUS_BLOCKS,
                ringPhaseRadians,
                NEBULA_KINDS,
                CometAzurJetSurroundParticle.MotionMode.EULER,
                0.0f,
                0.0f
        );
        spawnRing(
                CometAzurFx.JET_GALAXY_PARTICLE_COUNT,
                CometAzurFx.JET_GALAXY_RING_RADIUS_BLOCKS,
                ringPhaseRadians + 0.41f,
                GALAXY_KINDS,
                CometAzurJetSurroundParticle.MotionMode.EULER,
                0.0f,
                0.0f
        );
        spawnRing(
                CometAzurFx.JET_SPARKLE_PARTICLE_COUNT,
                CometAzurFx.JET_SPARKLE_RING_RADIUS_BLOCKS,
                ringPhaseRadians + 0.83f,
                SPARKLE_KINDS,
                CometAzurJetSurroundParticle.MotionMode.STRAIGHT,
                0.0f,
                0.0f
        );
        spawnEulerEnergyField(ringPhaseRadians);
        spawnStraightEnergyLines(ringPhaseRadians);
        spawnMidBeamFill(ringPhaseRadians);
    }

    /**
     * 多条欧拉螺旋：每条臂固定一个 ω，粒子沿 r e^{iωt} 绕喷流转着往前飞。
     */
    private void spawnEulerEnergyField(float ringPhaseRadians) {
        int armCount = Math.max(1, CometAzurFx.JET_FIELD_EULER_ARM_COUNT);
        int particlesPerArm = Math.max(1, CometAzurFx.JET_FIELD_EULER_PARTICLES_PER_ARM);
        float[] helixSpeeds = CometAzurFx.JET_FIELD_EULER_RADIANS_PER_TICK;
        for (int armIndex = 0; armIndex < armCount; armIndex++) {
            float armPhaseRadians = ringPhaseRadians + (float) (Math.PI * 2.0 * armIndex / armCount);
            float helixRadiansPerTick = helixSpeeds[armIndex % helixSpeeds.length];
            for (int particleIndex = 0; particleIndex < particlesPerArm; particleIndex++) {
                float slotJitterRadians = (this.random.nextFloat() - 0.5f)
                        * CometAzurFx.JET_RING_SLOT_JITTER_RADIANS;
                CometAzurJetSurroundParticle.Kind kind =
                        FIELD_EULER_KINDS[(armIndex + particleIndex) % FIELD_EULER_KINDS.length];
                float radiusScale = CometAzurFx.JET_RING_RADIUS_RANDOM_MIN_SCALE
                        + this.random.nextFloat()
                        * (CometAzurFx.JET_RING_RADIUS_RANDOM_MAX_SCALE
                        - CometAzurFx.JET_RING_RADIUS_RANDOM_MIN_SCALE);
                this.level.addParticle(
                        CometAzurJetOptions.flying(
                                this.yawDegrees,
                                this.pitchDegrees,
                                kind.ordinal(),
                                CometAzurJetSurroundParticle.MotionMode.EULER.ordinal(),
                                armPhaseRadians + slotJitterRadians,
                                CometAzurFx.JET_FIELD_EULER_RING_RADIUS_BLOCKS * radiusScale,
                                0.0f,
                                helixRadiansPerTick
                        ),
                        this.x,
                        this.y,
                        this.z,
                        0.0,
                        0.0,
                        0.0
                );
            }
        }
    }

    /**
     * 几道直线能量线：半径固定、不绕轴，光带贴着喷流外侧往前冲。
     */
    private void spawnStraightEnergyLines(float ringPhaseRadians) {
        int lineCount = Math.max(1, CometAzurFx.JET_FIELD_STRAIGHT_LINE_COUNT);
        for (int lineIndex = 0; lineIndex < lineCount; lineIndex++) {
            float lineAngleRadians = ringPhaseRadians
                    + (float) (Math.PI * 2.0 * lineIndex / lineCount)
                    + 0.21f;
            CometAzurJetSurroundParticle.Kind kind =
                    FIELD_STRAIGHT_KINDS[lineIndex % FIELD_STRAIGHT_KINDS.length];
            this.level.addParticle(
                    CometAzurJetOptions.flying(
                            this.yawDegrees,
                            this.pitchDegrees,
                            kind.ordinal(),
                            CometAzurJetSurroundParticle.MotionMode.STRAIGHT.ordinal(),
                            lineAngleRadians,
                            CometAzurFx.JET_FIELD_STRAIGHT_RING_RADIUS_BLOCKS,
                            0.0f,
                            0.0f
                    ),
                    this.x,
                    this.y,
                    this.z,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    /**
     * 在 0～{@link CometAzurFx#JET_PARTICLE_MAX_ALONG_BLOCKS} 之间随机落点，
     * 只补贴着激光的小星团，避免开喷后前几十格空荡荡。
     * 出生距离不超过当前射线碰到的实心方块，避免墙后还刷一串。
     */
    private void spawnMidBeamFill(float ringPhaseRadians) {
        int fillCount = Math.max(1, CometAzurFx.JET_FIELD_FILL_PARTICLE_COUNT);
        float clippedFillMaxAlongBlocks = Math.min(
                CometAzurFx.JET_FILL_MAX_ALONG_BLOCKS,
                clipSolidAlongBlocks()
        );
        if (clippedFillMaxAlongBlocks < 0.35f) {
            return;
        }
        float radiusSpan = CometAzurFx.JET_RING_RADIUS_RANDOM_MAX_SCALE
                - CometAzurFx.JET_RING_RADIUS_RANDOM_MIN_SCALE;
        for (int slotIndex = 0; slotIndex < fillCount; slotIndex++) {
            float alongFraction = (slotIndex + this.random.nextFloat()) / fillCount;
            float birthAlongBeamBlocks = alongFraction * clippedFillMaxAlongBlocks;
            float jitterRadians = (this.random.nextFloat() - 0.5f)
                    * CometAzurFx.JET_RING_SLOT_JITTER_RADIANS;
            float ringAngleRadians = ringPhaseRadians
                    + (float) (Math.PI * 2.0 * slotIndex / fillCount)
                    + jitterRadians;
            float radiusScale = CometAzurFx.JET_RING_RADIUS_RANDOM_MIN_SCALE
                    + this.random.nextFloat() * radiusSpan;
            CometAzurJetSurroundParticle.Kind kind = FILL_KINDS[slotIndex % FILL_KINDS.length];
            boolean eulerFill = kind != CometAzurJetSurroundParticle.Kind.GLOW;
            float helixRadiansPerTick = 0.0f;
            if (eulerFill) {
                float[] helixSpeeds = CometAzurFx.JET_FIELD_EULER_RADIANS_PER_TICK;
                helixRadiansPerTick = helixSpeeds[slotIndex % helixSpeeds.length];
            }
            this.level.addParticle(
                    CometAzurJetOptions.flying(
                            this.yawDegrees,
                            this.pitchDegrees,
                            kind.ordinal(),
                            (eulerFill
                                    ? CometAzurJetSurroundParticle.MotionMode.EULER
                                    : CometAzurJetSurroundParticle.MotionMode.STRAIGHT).ordinal(),
                            ringAngleRadians,
                            CometAzurFx.JET_FIELD_EULER_RING_RADIUS_BLOCKS * radiusScale,
                            birthAlongBeamBlocks,
                            helixRadiansPerTick
                    ),
                    this.x,
                    this.y,
                    this.z,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    private void spawnRing(
            int particleCount,
            float nominalRadiusBlocks,
            float ringPhaseRadians,
            CometAzurJetSurroundParticle.Kind[] kindCycle,
            CometAzurJetSurroundParticle.MotionMode motionMode,
            float birthAlongBeamBlocks,
            float helixRadiansPerTick
    ) {
        int count = Math.max(1, particleCount);
        float radiusSpan = CometAzurFx.JET_RING_RADIUS_RANDOM_MAX_SCALE
                - CometAzurFx.JET_RING_RADIUS_RANDOM_MIN_SCALE;
        for (int slotIndex = 0; slotIndex < count; slotIndex++) {
            float jitterRadians = (this.random.nextFloat() - 0.5f)
                    * CometAzurFx.JET_RING_SLOT_JITTER_RADIANS;
            float ringAngleRadians = ringPhaseRadians
                    + (float) (Math.PI * 2.0 * slotIndex / count)
                    + jitterRadians;
            float radiusScale = CometAzurFx.JET_RING_RADIUS_RANDOM_MIN_SCALE
                    + this.random.nextFloat() * radiusSpan;
            CometAzurJetSurroundParticle.Kind kind = kindCycle[slotIndex % kindCycle.length];
            this.level.addParticle(
                    CometAzurJetOptions.flying(
                            this.yawDegrees,
                            this.pitchDegrees,
                            kind.ordinal(),
                            motionMode.ordinal(),
                            ringAngleRadians,
                            nominalRadiusBlocks * radiusScale,
                            birthAlongBeamBlocks,
                            helixRadiansPerTick
                    ),
                    this.x,
                    this.y,
                    this.z,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    /**
     * 从喷流口沿锁定朝向扫到最大射程，碰到实心方块就截断。
     * 与 {@code CometAzurJetEntity#refreshBeamLength} 同一套 COLLIDER 射线。
     */
    private float clipSolidAlongBlocks() {
        Vec3 mouthWorld = new Vec3(this.x, this.y, this.z);
        Vec3 lookDirection = Vec3.directionFromRotation(this.pitchDegrees, this.yawDegrees);
        Vec3 farPoint = mouthWorld.add(lookDirection.scale(CometAzurFx.JET_PARTICLE_MAX_ALONG_BLOCKS));
        BlockHitResult blockHit = this.level.clip(new ClipContext(
                mouthWorld,
                farPoint,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
        ));
        if (blockHit.getType() == HitResult.Type.MISS) {
            return CometAzurFx.JET_PARTICLE_MAX_ALONG_BLOCKS;
        }
        return (float) mouthWorld.distanceTo(blockHit.getLocation());
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.NO_RENDER;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<CometAzurJetOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                CometAzurJetOptions options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd
        ) {
            if (options.emitter()) {
                return new CometAzurJetEmitterParticle(level, x, y, z, options);
            }
            CometAzurJetSurroundParticle surroundParticle =
                    new CometAzurJetSurroundParticle(level, x, y, z, options, this.sprites);
            return surroundParticle.isAlive() ? surroundParticle : null;
        }
    }
}

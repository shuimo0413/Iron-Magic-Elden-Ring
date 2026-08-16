package com.eldenring.spells.particle.cometazur;

import com.eldenring.spells.tuning.CometAzurTuning;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

/**
 * 喷流口发射器：服务端每圈只同步这一颗，客户端再铺欧拉组和直线组。
 * 自己不画。
 */
public class CometAzurJetEmitterParticle extends TextureSheetParticle {

    private static final CometAzurJetSurroundParticle.Kind[] EULER_KINDS = {
            CometAzurJetSurroundParticle.Kind.GLOW,
            CometAzurJetSurroundParticle.Kind.SPARK,
            CometAzurJetSurroundParticle.Kind.MOTE_1,
            CometAzurJetSurroundParticle.Kind.MOTE_2,
            CometAzurJetSurroundParticle.Kind.IMPACT
    };

    private static final CometAzurJetSurroundParticle.Kind[] STRAIGHT_KINDS = {
            CometAzurJetSurroundParticle.Kind.FILAMENT,
            CometAzurJetSurroundParticle.Kind.IMPACT
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
     * 在垂直视线的平面上铺两圈：内圈欧拉螺旋冲击，外圈近似直线前飞。
     */
    private void spawnSurroundRings() {
        float ringPhaseRadians = this.random.nextFloat() * ((float) Math.PI * 2.0f);
        spawnRing(
                CometAzurTuning.JET_EULER_PARTICLE_COUNT,
                CometAzurTuning.JET_EULER_RING_RADIUS_BLOCKS,
                ringPhaseRadians,
                EULER_KINDS,
                CometAzurJetSurroundParticle.MotionMode.EULER
        );
        spawnRing(
                CometAzurTuning.JET_STRAIGHT_PARTICLE_COUNT,
                CometAzurTuning.JET_STRAIGHT_RING_RADIUS_BLOCKS,
                ringPhaseRadians + 0.37f,
                STRAIGHT_KINDS,
                CometAzurJetSurroundParticle.MotionMode.STRAIGHT
        );
    }

    private void spawnRing(
            int particleCount,
            float nominalRadiusBlocks,
            float ringPhaseRadians,
            CometAzurJetSurroundParticle.Kind[] kindCycle,
            CometAzurJetSurroundParticle.MotionMode motionMode
    ) {
        int count = Math.max(1, particleCount);
        float radiusSpan = CometAzurTuning.JET_RING_RADIUS_RANDOM_MAX_SCALE
                - CometAzurTuning.JET_RING_RADIUS_RANDOM_MIN_SCALE;
        for (int slotIndex = 0; slotIndex < count; slotIndex++) {
            float jitterRadians = (this.random.nextFloat() - 0.5f)
                    * CometAzurTuning.JET_RING_SLOT_JITTER_RADIANS;
            float ringAngleRadians = ringPhaseRadians
                    + (float) (Math.PI * 2.0 * slotIndex / count)
                    + jitterRadians;
            float radiusScale = CometAzurTuning.JET_RING_RADIUS_RANDOM_MIN_SCALE
                    + this.random.nextFloat() * radiusSpan;
            CometAzurJetSurroundParticle.Kind kind = kindCycle[slotIndex % kindCycle.length];
            this.level.addParticle(
                    CometAzurJetOptions.flying(
                            this.yawDegrees,
                            this.pitchDegrees,
                            kind.ordinal(),
                            motionMode.ordinal(),
                            ringAngleRadians,
                            nominalRadiusBlocks * radiusScale
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
            return new CometAzurJetSurroundParticle(level, x, y, z, options, this.sprites);
        }
    }
}

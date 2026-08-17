package com.eldenring.spells.particle.cometazur;

import com.eldenring.spells.client.render.AdditiveParticleRenderType;
import com.eldenring.spells.registry.ModParticles;
import com.eldenring.spells.tuning.CometAzurTuning;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 起手漩涡中心：两张 shrink 贴图做成朝向相机的四边形，只绕平面法线转。
 * <p>
 * 主层第一 tick 在「垂直于视线」的平面上铺对数螺线汇聚粒子，之后不再每 tick 乱喷。
 */
public class CometAzurVortexParticle extends TextureSheetParticle {

    private final float rollRadiansPerTick;
    private final boolean spawnSpirals;
    private final float yawDegrees;
    private final float pitchDegrees;
    private final float birthQuadSize;
    private final float birthAlpha;
    private boolean spawnedSpirals;

    protected CometAzurVortexParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            CometAzurVortexOptions options,
            SpriteSet sprites
    ) {
        super(level, x, y, z);
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.hasPhysics = false;
        this.gravity = 0.0f;
        this.lifetime = CometAzurTuning.STARTUP_DURATION_TICKS;
        this.rollRadiansPerTick = options.rollRadiansPerTick();
        this.spawnSpirals = options.spawnSpirals();
        this.yawDegrees = options.yawDegrees();
        this.pitchDegrees = options.pitchDegrees();
        int spriteIndex = Mth.clamp(options.spriteIndex(), 0, 1);
        setSprite(sprites.get(spriteIndex, 1));
        this.birthQuadSize = spriteIndex == 1
                ? CometAzurTuning.STARTUP_SHRINK_2_QUAD_SIZE_BLOCKS
                : CometAzurTuning.STARTUP_SHRINK_1_QUAD_SIZE_BLOCKS;
        this.quadSize = this.birthQuadSize;
        this.birthAlpha = 0.95f;
        this.alpha = 0.0f;
        this.rCol = 0.42f;
        this.gCol = 0.88f;
        this.bCol = 0.78f;
        this.roll = level.random.nextFloat() * ((float) Math.PI * 2.0f);
        this.oRoll = this.roll;
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

        this.roll += this.rollRadiansPerTick;
        applyLifetimeVisuals();
        if (this.spawnSpirals && !this.spawnedSpirals) {
            this.spawnedSpirals = true;
            spawnLogSpiralInboundParticles();
        }
    }

    /**
     * 前几 tick 淡入，最后 20% 寿命淡出并略缩小，中间保持峰值。
     */
    private void applyLifetimeVisuals() {
        float lifeFraction = (float) this.age / (float) this.lifetime;
        float fadeIn = Mth.clamp(lifeFraction / 0.12f, 0.0f, 1.0f);
        float fadeOut = lifeFraction < 0.80f
                ? 1.0f
                : 1.0f - (lifeFraction - 0.80f) / 0.20f;
        this.alpha = this.birthAlpha * fadeIn * fadeOut;
        this.quadSize = this.birthQuadSize * (0.88f + 0.12f * fadeIn) * (0.72f + 0.28f * fadeOut);
    }

    /**
     * 在垂直视线的平面上，按 {@code r = 外半径 × e^(wA)}、{@code A ∈ [0, 12π]} 铺多条螺线。
     * 每颗粒子带着自己的 w / 出生角 / 臂相位，2 秒内走到 {@code A = 12π}。
     */
    private void spawnLogSpiralInboundParticles() {
        Vec3 vortexCenter = new Vec3(this.x, this.y, this.z);
        Vec3 lookDirection = Vec3.directionFromRotation(this.pitchDegrees, this.yawDegrees);
        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 rightAxis = lookDirection.cross(worldUp);
        if (rightAxis.lengthSqr() < 1.0e-8) {
            rightAxis = new Vec3(1.0, 0.0, 0.0);
        } else {
            rightAxis = rightAxis.normalize();
        }
        Vec3 upAxis = rightAxis.cross(lookDirection).normalize();
        CometAzurInboundParticle.bindSpiralFrame(new CometAzurInboundParticle.SpiralFrame(
                vortexCenter,
                rightAxis,
                upAxis
        ));
        try {
            float[] spiralWValues = CometAzurTuning.STARTUP_SPIRAL_W_PER_CURVE;
            int armCount = Math.max(1, CometAzurTuning.STARTUP_SPIRAL_ARM_COUNT);
            int samplesPerCurve = Math.max(1, CometAzurTuning.STARTUP_SPIRAL_SAMPLES_PER_CURVE);
            float maxAngleRadians = CometAzurTuning.STARTUP_SPIRAL_MAX_ANGLE_RADIANS;
            for (int curveIndex = 0; curveIndex < spiralWValues.length; curveIndex++) {
                float spiralW = spiralWValues[curveIndex];
                for (int armIndex = 0; armIndex < armCount; armIndex++) {
                    float armPhaseRadians = (float) (Math.PI * 2.0 * armIndex / armCount);
                    for (int sampleIndex = 0; sampleIndex < samplesPerCurve; sampleIndex++) {
                        float angleBirthRadians = maxAngleRadians * sampleIndex / samplesPerCurve;
                        this.level.addParticle(
                                pickInboundParticleType(curveIndex, armIndex, sampleIndex),
                                this.x,
                                this.y,
                                this.z,
                                spiralW,
                                angleBirthRadians,
                                armPhaseRadians
                        );
                    }
                }
            }
        } finally {
            CometAzurInboundParticle.clearSpiralFrame();
        }
    }

    private ParticleOptions pickInboundParticleType(int curveIndex, int armIndex, int sampleIndex) {
        int mixIndex = (curveIndex + armIndex * 2 + sampleIndex) % 4;
        return switch (mixIndex) {
            case 0 -> ModParticles.COMET_AZUR_MOTE.get();
            case 1 -> ModParticles.COMET_AZUR_DUST.get();
            case 2 -> ModParticles.COMET_AZUR_HEAD.get();
            default -> ModParticles.COMET_AZUR_IMPACT.get();
        };
    }

    @Override
    public ParticleRenderType getRenderType() {
        return AdditiveParticleRenderType.SOFT_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<CometAzurVortexOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                CometAzurVortexOptions options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd
        ) {
            return new CometAzurVortexParticle(level, x, y, z, options, this.sprites);
        }
    }
}

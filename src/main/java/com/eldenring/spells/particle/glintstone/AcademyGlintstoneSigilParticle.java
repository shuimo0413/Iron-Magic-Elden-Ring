package com.eldenring.spells.particle.glintstone;

import com.eldenring.spells.registry.ModParticles;
import com.eldenring.spells.sigil.AcademySigilFx;
import com.eldenring.spells.tuning.GlintstoneCastSigilTuning;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 学院辉石法阵：贴在施法者头顶的相机朝向四边形。
 * <p>
 * 不旋转、不物理模拟；每 tick 锁到施法者头顶。一出现即满不透明，随后淡出。
 */
public class AcademyGlintstoneSigilParticle extends TextureSheetParticle {

    /** 寿命（tick）。20 tick = 1 秒。 */
    private static final int LIFETIME_TICKS = 20;

    /** 开始淡出的寿命比例（0–1）。此前保持峰值，之后 t² 收到 0。 */
    private static final float FADE_OUT_START_LIFETIME_FRACTION = 0.42f;

    /** 法阵仍处于峰值时，每隔多少 tick 在外沿补一颗闪星。 */
    private static final int RIM_MOTE_INTERVAL_TICKS = 3;

    private final int casterEntityId;

    protected AcademyGlintstoneSigilParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            int casterEntityId,
            SpriteSet sprites
    ) {
        super(level, x, y, z);
        this.casterEntityId = casterEntityId;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.lifetime = LIFETIME_TICKS;
        this.quadSize = GlintstoneCastSigilTuning.QUAD_HALF_SIZE_BLOCKS;
        this.rCol = 1.0f;
        this.gCol = 1.0f;
        this.bCol = 1.0f;
        this.alpha = GlintstoneCastSigilTuning.PEAK_ALPHA;
        setSprite(sprites.get(0, 1));
        applyLifetimeVisuals(0.0f);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            remove();
            return;
        }

        Entity caster = this.level.getEntity(this.casterEntityId);
        if (caster == null || !caster.isAlive()) {
            remove();
            return;
        }

        Vec3 sigilPosition = AcademySigilFx.worldPositionAboveHead(caster);
        this.x = sigilPosition.x;
        this.y = sigilPosition.y;
        this.z = sigilPosition.z;
        applyLifetimeVisuals(0.0f);
        emitRimMoteIfHolding();
    }

    /**
     * 峰值阶段在外沿稀疏补闪星，跟着法阵走；淡出后停，避免粒子往下落进视野。
     */
    private void emitRimMoteIfHolding() {
        float lifeFraction = (float) this.age / (float) this.lifetime;
        if (lifeFraction >= FADE_OUT_START_LIFETIME_FRACTION) {
            return;
        }
        if (this.age % RIM_MOTE_INTERVAL_TICKS != 0) {
            return;
        }
        double angleRadians = this.random.nextDouble() * Math.PI * 2.0;
        double rimRadiusBlocks = GlintstoneCastSigilTuning.QUAD_HALF_SIZE_BLOCKS
                * (1.08 + this.random.nextDouble() * 0.22);
        this.level.addParticle(
                ModParticles.GLINTSTONE_MOTE.get(),
                this.x + Math.cos(angleRadians) * rimRadiusBlocks,
                this.y + 0.04 + this.random.nextDouble() * 0.06,
                this.z + Math.sin(angleRadians) * rimRadiusBlocks,
                0.0,
                0.012,
                0.0
        );
    }

    private void applyLifetimeVisuals(float extraAgeTicks) {
        float lifeFraction = Mth.clamp(
                (this.age + extraAgeTicks) / (float) this.lifetime,
                0.0f,
                1.0f
        );
        this.alpha = GlintstoneCastSigilTuning.PEAK_ALPHA * fadeOutEnvelope(lifeFraction);
    }

    private static float fadeOutEnvelope(float lifeFraction) {
        if (lifeFraction < FADE_OUT_START_LIFETIME_FRACTION) {
            return 1.0f;
        }
        float fadeOutProgress = (lifeFraction - FADE_OUT_START_LIFETIME_FRACTION)
                / Math.max(1.0f - FADE_OUT_START_LIFETIME_FRACTION, 1.0e-4f);
        return 1.0f - fadeOutProgress * fadeOutProgress;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<AcademyGlintstoneSigilParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                AcademyGlintstoneSigilParticleOptions options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd
        ) {
            return new AcademyGlintstoneSigilParticle(
                    level, x, y, z, options.casterEntityId(), this.sprites
            );
        }
    }
}

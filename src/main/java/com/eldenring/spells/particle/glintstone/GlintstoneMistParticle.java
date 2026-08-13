package com.eldenring.spells.particle.glintstone;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * 辉石雾气：主要用于命中烟雾团与施法爆裂，不再作为飞行拖尾主体。
 */
public class GlintstoneMistParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    /** 出生时四边形边长（方块）。 */
    private final float birthQuadSize;
    /** 出生时不透明度。 */
    private final float birthAlpha;

    protected GlintstoneMistParticle(ClientLevel level, double x, double y, double z,
                                     double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.xd = xd * 0.28;
        this.yd = yd * 0.28;
        this.zd = zd * 0.28;
        this.lifetime = 12 + level.random.nextInt(6);
        float sizeScale = GlintstoneFx.clientParticleSizeScale();
        this.birthQuadSize = (0.36f + level.random.nextFloat() * 0.28f) * sizeScale;
        this.quadSize = this.birthQuadSize;
        this.gravity = -0.01f;
        this.hasPhysics = false;
        this.friction = 0.94f;
        // 辉石蓝绿雾
        this.rCol = 0.18f + level.random.nextFloat() * 0.06f;
        this.gCol = 0.72f + level.random.nextFloat() * 0.10f;
        this.bCol = 0.85f + level.random.nextFloat() * 0.08f;
        this.birthAlpha = 0.58f;
        this.alpha = this.birthAlpha;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
        float life = (float) age / (float) lifetime;
        this.alpha = this.birthAlpha * (1.0f - life);
        this.quadSize = this.birthQuadSize * (1.0f - life * 0.70f);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        float life = ((float) age + partialTick) / (float) lifetime;
        life = Mth.clamp(1.0f - life * 0.3f, 0.5f, 1.0f);
        int block = (int) (220 * life);
        return block | (block << 16);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new GlintstoneMistParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}

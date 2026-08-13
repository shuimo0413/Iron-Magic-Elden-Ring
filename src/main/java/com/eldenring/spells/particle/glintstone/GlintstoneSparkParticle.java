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
 * Bright crystalline spark — short life, sharp fade, slight gravity.
 */
public class GlintstoneSparkParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected GlintstoneSparkParticle(ClientLevel level, double x, double y, double z,
                                      double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.lifetime = 8 + level.random.nextInt(6);
        float sizeScale = GlintstoneFx.clientParticleSizeScale();
        this.quadSize = (0.09f + level.random.nextFloat() * 0.07f) * sizeScale;
        this.gravity = 0.12f;
        this.hasPhysics = false;
        // 晶体火花：亮核偏白青，整体偏辉石蓝绿
        float bright = 0.82f + level.random.nextFloat() * 0.18f;
        this.rCol = bright * 0.55f;
        this.gCol = bright;
        this.bCol = bright;
        this.alpha = 1.0f;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
        float life = (float) age / (float) lifetime;
        this.alpha = 1.0f - life * life;
        this.quadSize *= 0.96f;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        float life = ((float) age + partialTick) / (float) lifetime;
        life = Mth.clamp(1.0f - life, 0.0f, 1.0f);
        int blockLight = super.getLightColor(partialTick);
        int block = blockLight & 0xFF;
        int sky = (blockLight >> 16) & 0xFF;
        block = Math.max(block, (int) (240 * life));
        return block | (sky << 16);
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
            return new GlintstoneSparkParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}

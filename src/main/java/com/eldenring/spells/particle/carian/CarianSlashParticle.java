package com.eldenring.spells.particle.carian;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * 卡利亚斩击新月：短命、无重力、偏宝蓝。贴图本身是弯弧，用作挥砍点缀而不是拖尾主体。
 */
public class CarianSlashParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected CarianSlashParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd,
            SpriteSet sprites
    ) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.xd = xd * 0.45;
        this.yd = yd * 0.45;
        this.zd = zd * 0.45;
        this.lifetime = 5 + level.random.nextInt(3);
        this.quadSize = 0.22f + level.random.nextFloat() * 0.10f;
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.rCol = 0.28f + level.random.nextFloat() * 0.10f;
        this.gCol = 0.48f + level.random.nextFloat() * 0.12f;
        this.bCol = 0.95f + level.random.nextFloat() * 0.05f;
        this.alpha = 0.92f;
        this.roll = (level.random.nextFloat() - 0.5f) * 0.8f;
        this.oRoll = this.roll;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
        float life = (float) age / (float) lifetime;
        this.alpha = 0.92f * (1.0f - life * life);
        this.quadSize *= 1.04f;
        this.oRoll = this.roll;
        this.roll += 0.08f;
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
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd
        ) {
            return new CarianSlashParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}

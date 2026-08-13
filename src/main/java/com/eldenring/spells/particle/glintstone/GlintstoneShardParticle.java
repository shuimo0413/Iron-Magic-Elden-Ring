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
 * 辉石碎晶碎片：菱形贴图、轻微重力、命中时向外飞溅。
 * <p>
 * 寿命约 10–18 tick；尺寸随寿命缓慢缩小，模拟晶体碎裂后消散。
 */
public class GlintstoneShardParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected GlintstoneShardParticle(ClientLevel level, double x, double y, double z,
                                      double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.lifetime = 10 + level.random.nextInt(9);
        float sizeScale = GlintstoneFx.clientParticleSizeScale();
        this.quadSize = (0.10f + level.random.nextFloat() * 0.10f) * sizeScale;
        this.gravity = 0.18f;
        this.hasPhysics = false;
        this.friction = 0.92f;
        float bright = 0.7f + level.random.nextFloat() * 0.3f;
        this.rCol = bright * 0.5f;
        this.gCol = bright;
        this.bCol = bright;
        // 随机初始旋转，避免碎晶朝向完全一致
        this.roll = level.random.nextFloat() * ((float) Math.PI * 2.0f);
        this.oRoll = this.roll;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();
        setSpriteFromAge(sprites);
        // 飞行中缓慢自旋（弧度/tick）
        this.roll += 0.18f;
        float life = (float) age / (float) lifetime;
        this.alpha = 1.0f - life * life;
        this.quadSize *= 0.97f;
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
        block = Math.max(block, (int) (230 * life));
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
            return new GlintstoneShardParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}

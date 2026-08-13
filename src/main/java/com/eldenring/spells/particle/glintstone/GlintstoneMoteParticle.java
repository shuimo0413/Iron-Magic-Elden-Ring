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
 * 辉石闪星：多帧十字星贴图，拖尾中稀疏闪烁。
 * <p>
 * 寿命短、几乎无位移阻尼；alpha 用正弦脉冲制造「一闪」感。
 * 贴图序列见 {@code particles/glintstone_mote.json}（3 帧）。
 */
public class GlintstoneMoteParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected GlintstoneMoteParticle(ClientLevel level, double x, double y, double z,
                                     double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.xd = xd * 0.25;
        this.yd = yd * 0.25;
        this.zd = zd * 0.25;
        this.lifetime = 6 + level.random.nextInt(6);
        float sizeScale = GlintstoneFx.clientParticleSizeScale();
        this.quadSize = (0.05f + level.random.nextFloat() * 0.07f) * sizeScale;
        this.gravity = 0f;
        this.hasPhysics = false;
        this.rCol = 0.75f;
        this.gCol = 1.0f;
        this.bCol = 0.95f;
        this.alpha = 0.95f;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
        float life = (float) age / (float) lifetime;
        // 中段最亮，两端淡出，形成闪烁
        float pulse = Mth.sin(life * (float) Math.PI);
        this.alpha = 0.25f + 0.75f * pulse;
        this.quadSize *= 0.98f;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        float life = ((float) age + partialTick) / (float) lifetime;
        float pulse = Mth.sin(Mth.clamp(life, 0.0f, 1.0f) * (float) Math.PI);
        int blockLight = super.getLightColor(partialTick);
        int block = blockLight & 0xFF;
        int sky = (blockLight >> 16) & 0xFF;
        block = Math.max(block, (int) (240 * (0.4f + 0.6f * pulse)));
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
            return new GlintstoneMoteParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}

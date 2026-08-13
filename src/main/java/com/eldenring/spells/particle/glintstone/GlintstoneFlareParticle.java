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
 * 辉石绽光：施法/命中瞬间的中心能量场闪光，快速膨胀后淡出。
 * <p>
 * 寿命约 5–9 tick；几乎不移动，靠尺寸扩张制造冲击感与「能量场」轮廓。
 */
public class GlintstoneFlareParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    /** 初始四边形边长（方块），用于按寿命曲线放大。 */
    private final float baseQuadSize;

    protected GlintstoneFlareParticle(ClientLevel level, double x, double y, double z,
                                      double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.xd = xd * 0.04;
        this.yd = yd * 0.04;
        this.zd = zd * 0.04;
        this.lifetime = 5 + level.random.nextInt(5);
        this.baseQuadSize = 0.42f + level.random.nextFloat() * 0.32f;
        this.quadSize = this.baseQuadSize;
        this.gravity = 0f;
        this.hasPhysics = false;
        this.rCol = 0.55f;
        this.gCol = 0.95f;
        this.bCol = 1.0f;
        this.alpha = 0.92f;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
        float life = (float) age / (float) lifetime;
        // 前段迅速胀成能量场盘面，后段收缩并淡出
        float expand = life < 0.3f
                ? (0.85f + life / 0.3f * 1.35f)
                : (2.2f - (life - 0.3f) / 0.7f * 1.1f);
        this.quadSize = this.baseQuadSize * expand;
        this.alpha = 0.92f * (1.0f - life * life);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        float life = ((float) age + partialTick) / (float) lifetime;
        life = Mth.clamp(1.0f - life * 0.6f, 0.5f, 1.0f);
        int blockLight = super.getLightColor(partialTick);
        int block = blockLight & 0xFF;
        int sky = (blockLight >> 16) & 0xFF;
        block = Math.max(block, (int) (250 * life));
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
            return new GlintstoneFlareParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}

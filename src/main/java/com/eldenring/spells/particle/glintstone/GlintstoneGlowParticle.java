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
 * 辉石蓝绿光晕：命中/施法体积层（飞行光轨已改由几何光束绘制）。
 */
public class GlintstoneGlowParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    /** 出生时四边形边长（方块），tick 中按寿命比例缩小。 */
    private final float birthQuadSize;
    /** 出生时不透明度；寿命内线性衰减。 */
    private final float birthAlpha;

    protected GlintstoneGlowParticle(ClientLevel level, double x, double y, double z,
                                     double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.xd = xd * 0.28;
        this.yd = yd * 0.28;
        this.zd = zd * 0.28;
        this.lifetime = 9 + level.random.nextInt(5);
        float sizeScale = GlintstoneFx.clientParticleSizeScale();
        this.birthQuadSize = (0.24f + level.random.nextFloat() * 0.14f) * sizeScale;
        this.quadSize = this.birthQuadSize;
        this.gravity = 0f;
        this.hasPhysics = false;
        // 辉石蓝绿色（青蓝偏绿），避免纯深蓝
        this.rCol = 0.22f + level.random.nextFloat() * 0.08f;
        this.gCol = 0.80f + level.random.nextFloat() * 0.10f;
        this.bCol = 0.90f + level.random.nextFloat() * 0.08f;
        this.birthAlpha = 0.78f;
        this.alpha = this.birthAlpha;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
        float life = (float) age / (float) lifetime;
        this.alpha = this.birthAlpha * (1.0f - life);
        this.quadSize = this.birthQuadSize * (1.0f - life * 0.68f);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        float life = ((float) age + partialTick) / (float) lifetime;
        life = Mth.clamp(1.0f - life * 0.35f, 0.55f, 1.0f);
        int block = (int) (240 * life);
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
            return new GlintstoneGlowParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}

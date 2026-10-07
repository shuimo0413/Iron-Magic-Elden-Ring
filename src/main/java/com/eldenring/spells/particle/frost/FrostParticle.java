package com.eldenring.spells.particle.frost;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * 冰霜星辰粒子：贴图（32×32，来自 {@code 粒子库/tool/gen_frost_star_particles.py}）已烘焙
 * 深冰蓝 → 浅青 → 冷白配色，着色只做极轻的冷色偏移，保留贴图本身的冷白高光。
 * <p>
 * 十三种贴图共用本类，用 {@link Kind} 区分手感。
 */
public class FrostParticle extends TextureSheetParticle {

    /**
     * 寿命内尺寸 / 透明度怎么走。
     */
    public enum Fade {
        /** 线性淡出，尺寸略缩。 */
        LINEAR,
        /** 正弦脉冲，中段最亮（闪烁、微粒）。 */
        PULSE,
        /** 略胀再淡，给寒雾体积感。 */
        EXPAND_FADE,
        /** 前段迅速胀开、后段收缩，给光环 / 绽光。 */
        FLARE
    }

    /**
     * 每种冰霜贴图的手感。寿命单位 tick；尺寸是四边形边长（方块）。
     * 寿命调大 = 停留更久；尺寸调大 = 屏幕上更显眼；重力为正下坠、为负上浮。
     */
    public enum Kind {
        STAR(10, 6, 0.20f, 0.10f, 0.0f, 0.30f, 0.97f, Fade.PULSE, true, 0.03f),
        SPARK(8, 6, 0.09f, 0.07f, 0.10f, 0.90f, 0.96f, Fade.LINEAR, false, 0.0f),
        GLOW(9, 5, 0.26f, 0.14f, 0.0f, 0.28f, 0.97f, Fade.LINEAR, false, 0.0f),
        HALO(10, 4, 0.32f, 0.16f, 0.0f, 0.04f, 0.98f, Fade.FLARE, false, 0.0f),
        SHARD(12, 6, 0.10f, 0.10f, 0.18f, 0.90f, 0.92f, Fade.LINEAR, true, 0.12f),
        CRYSTAL(14, 6, 0.16f, 0.10f, 0.06f, 0.60f, 0.95f, Fade.LINEAR, true, 0.05f),
        SNOWFLAKE(24, 12, 0.12f, 0.08f, 0.015f, 0.40f, 0.96f, Fade.LINEAR, true, 0.04f),
        STARDUST(14, 8, 0.24f, 0.12f, 0.0f, 0.20f, 0.97f, Fade.PULSE, false, 0.0f),
        MIST(14, 6, 0.36f, 0.28f, -0.01f, 0.28f, 0.94f, Fade.EXPAND_FADE, false, 0.0f),
        FLARE(6, 3, 0.36f, 0.16f, 0.0f, 0.0f, 0.98f, Fade.FLARE, false, 0.0f),
        SPARKLE(6, 6, 0.06f, 0.06f, 0.0f, 0.25f, 0.98f, Fade.PULSE, false, 0.0f),
        AURA(12, 6, 0.40f, 0.16f, 0.0f, 0.04f, 0.98f, Fade.EXPAND_FADE, true, 0.02f),
        MOTE(8, 6, 0.05f, 0.07f, 0.0f, 0.25f, 0.98f, Fade.PULSE, false, 0.0f);

        /** 基础寿命（tick）。 */
        final int lifetimeBaseTicks;
        /** 额外随机寿命上限（tick），实际寿命 = base + random(0..extra)。 */
        final int lifetimeRandomTicks;
        /** 出生时最小四边形边长（方块）。 */
        final float quadSizeMin;
        /** 出生时额外随机边长（方块）。 */
        final float quadSizeRandom;
        /** 重力。正值下坠（碎晶、雪花），负值缓慢上浮（寒雾）。 */
        final float gravity;
        /** 入口速度阻尼，1 表示完全保留传入速度。 */
        final float velocityDamp;
        /** 每 tick 速度保留比例，越小停得越快。 */
        final float friction;
        final Fade fade;
        final boolean spin;
        /** 自旋角速度（弧度 / tick）。 */
        final float rollRadiansPerTick;

        Kind(
                int lifetimeBaseTicks,
                int lifetimeRandomTicks,
                float quadSizeMin,
                float quadSizeRandom,
                float gravity,
                float velocityDamp,
                float friction,
                Fade fade,
                boolean spin,
                float rollRadiansPerTick
        ) {
            this.lifetimeBaseTicks = lifetimeBaseTicks;
            this.lifetimeRandomTicks = lifetimeRandomTicks;
            this.quadSizeMin = quadSizeMin;
            this.quadSizeRandom = quadSizeRandom;
            this.gravity = gravity;
            this.velocityDamp = velocityDamp;
            this.friction = friction;
            this.fade = fade;
            this.spin = spin;
            this.rollRadiansPerTick = rollRadiansPerTick;
        }
    }

    private final SpriteSet sprites;
    private final Kind kind;
    private final float birthQuadSize;
    private final float birthAlpha;

    protected FrostParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd,
            SpriteSet sprites,
            Kind kind
    ) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.kind = kind;
        this.xd = xd * kind.velocityDamp;
        this.yd = yd * kind.velocityDamp;
        this.zd = zd * kind.velocityDamp;
        this.lifetime = kind.lifetimeBaseTicks + level.random.nextInt(kind.lifetimeRandomTicks + 1);
        this.birthQuadSize = kind.quadSizeMin + level.random.nextFloat() * kind.quadSizeRandom;
        this.quadSize = this.birthQuadSize;
        this.gravity = kind.gravity;
        this.friction = kind.friction;
        this.hasPhysics = false;
        // 贴图自带冰蓝配色，只压一点红让冷白不发暖。
        float tintJitter = 0.94f + level.random.nextFloat() * 0.06f;
        this.rCol = tintJitter * 0.92f;
        this.gCol = tintJitter * 0.98f;
        this.bCol = tintJitter;
        this.birthAlpha = kind == Kind.MIST || kind == Kind.AURA ? 0.80f : 0.92f;
        this.alpha = this.birthAlpha;
        if (kind.spin) {
            this.roll = level.random.nextFloat() * ((float) Math.PI * 2.0f);
            this.oRoll = this.roll;
        }
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        if (kind.spin) {
            this.oRoll = this.roll;
        }
        super.tick();
        setSpriteFromAge(sprites);
        if (kind.spin) {
            this.roll += kind.rollRadiansPerTick;
        }

        float life = (float) age / (float) lifetime;
        switch (kind.fade) {
            case PULSE -> {
                float pulse = Mth.sin(life * (float) Math.PI);
                this.alpha = 0.22f + 0.78f * pulse;
                this.quadSize = this.birthQuadSize * (0.85f + 0.25f * pulse);
            }
            case EXPAND_FADE -> {
                this.alpha = this.birthAlpha * (1.0f - life);
                this.quadSize = this.birthQuadSize * (1.0f + life * 0.45f);
            }
            case FLARE -> {
                float expand = life < 0.28f
                        ? (0.80f + life / 0.28f * 1.25f)
                        : (2.05f - (life - 0.28f) / 0.72f * 1.05f);
                this.quadSize = this.birthQuadSize * expand;
                this.alpha = this.birthAlpha * (1.0f - life * life);
            }
            default -> {
                this.alpha = this.birthAlpha * (1.0f - life);
                this.quadSize = this.birthQuadSize * (1.0f - life * 0.35f);
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        float life = ((float) age + partialTick) / (float) lifetime;
        life = Mth.clamp(1.0f - life * 0.35f, 0.55f, 1.0f);
        int block = (int) (245 * life);
        return block | (block << 16);
    }

    /**
     * 把一种 {@link Kind} 绑到对应的 {@link SimpleParticleType} 上。
     */
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Kind kind;

        public Provider(SpriteSet sprites, Kind kind) {
            this.sprites = sprites;
            this.kind = kind;
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
            return new FrostParticle(level, x, y, z, xd, yd, zd, sprites, kind);
        }
    }
}

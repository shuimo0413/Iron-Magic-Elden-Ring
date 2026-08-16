package com.eldenring.spells.particle.starriver;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * 星河 / 星云粒子：贴图已带蓝紫配色，这里只负责寿命、尺寸曲线和轻微漂移。
 * <p>
 * 十六种预选贴图共用本类，用 {@link Kind} 区分手感，避免复制十六份几乎一样的 Provider。
 */
public class StarRiverParticle extends TextureSheetParticle {

    /**
     * 寿命内尺寸 / 透明度怎么走。
     */
    public enum Fade {
        /** 线性淡出，尺寸略缩。 */
        LINEAR,
        /** 正弦脉冲，中段最亮（闪星、新星）。 */
        PULSE,
        /** 略胀再淡，给雾气/星尘体积感。 */
        EXPAND_FADE,
        /** 前段迅速胀开、后段收缩，给绽光和脉冲环。 */
        FLARE
    }

    /**
     * 每种星云贴图的手感。寿命单位 tick；尺寸是四边形边长（方块）。
     */
    public enum Kind {
        MIST(18, 10, 0.50f, 0.26f, -0.008f, 0.16f, 0.96f, Fade.EXPAND_FADE, false, 0.0f),
        GLOW(12, 8, 0.30f, 0.18f, 0.0f, 0.20f, 0.97f, Fade.LINEAR, false, 0.0f),
        MOTE(8, 6, 0.08f, 0.08f, 0.0f, 0.18f, 0.98f, Fade.PULSE, false, 0.0f),
        CORE(16, 8, 0.24f, 0.10f, 0.0f, 0.06f, 0.98f, Fade.LINEAR, false, 0.0f),
        SPIRAL(18, 8, 0.38f, 0.14f, 0.0f, 0.05f, 0.98f, Fade.LINEAR, true, 0.035f),
        SHARD(10, 6, 0.11f, 0.08f, 0.12f, 0.90f, 0.92f, Fade.LINEAR, true, 0.16f),
        FLARE(6, 4, 0.40f, 0.18f, 0.0f, 0.04f, 0.98f, Fade.FLARE, false, 0.0f),
        STREAK(8, 6, 0.16f, 0.10f, 0.0f, 0.82f, 0.94f, Fade.LINEAR, false, 0.0f),
        CLUSTER(14, 8, 0.28f, 0.14f, 0.0f, 0.10f, 0.97f, Fade.LINEAR, false, 0.0f),
        RING(12, 6, 0.34f, 0.14f, 0.0f, 0.04f, 0.98f, Fade.EXPAND_FADE, true, 0.03f),
        DUST(14, 8, 0.34f, 0.18f, -0.006f, 0.14f, 0.96f, Fade.EXPAND_FADE, false, 0.0f),
        NOVA(8, 6, 0.13f, 0.10f, 0.0f, 0.14f, 0.98f, Fade.PULSE, false, 0.0f),
        FILAMENT(14, 8, 0.24f, 0.12f, 0.0f, 0.10f, 0.97f, Fade.LINEAR, false, 0.0f),
        PULSE(8, 4, 0.32f, 0.16f, 0.0f, 0.04f, 0.98f, Fade.FLARE, false, 0.0f),
        CRESCENT(12, 6, 0.26f, 0.12f, 0.0f, 0.08f, 0.97f, Fade.LINEAR, true, 0.045f),
        BINARY(10, 6, 0.14f, 0.08f, 0.0f, 0.12f, 0.98f, Fade.PULSE, false, 0.0f),
        /**
         * 创星雨升空拖尾：短命残影，钉在光点刚走过的位置上淡出。
         * 寿命短才能看成「小拖尾」而不是一团雾。
         */
        ASCENT_TRAIL(5, 2, 0.09f, 0.05f, 0.0f, 0.55f, 0.88f, Fade.LINEAR, false, 0.0f),
        /**
         * 创星雨落地飞沫：短命扇形水珠，略带重力，不构成雨点本体。
         */
        SPRAY(8, 3, 0.22f, 0.12f, 0.08f, 0.62f, 0.90f, Fade.LINEAR, false, 0.0f);

        /** 基础寿命（tick）。 */
        final int lifetimeBaseTicks;
        /** 额外随机寿命上限（tick），实际寿命 = base + random(0..extra)。 */
        final int lifetimeRandomTicks;
        /** 出生时最小四边形边长（方块）。 */
        final float quadSizeMin;
        /** 出生时额外随机边长（方块）。 */
        final float quadSizeRandom;
        /** 重力。负值缓慢上浮（雾气）。 */
        final float gravity;
        /** 入口速度阻尼，1 表示完全保留 MagicManager 传入的速度。 */
        final float velocityDamp;
        final float friction;
        final Fade fade;
        final boolean spin;
        /** 自旋角速度（弧度/tick）。 */
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

    protected StarRiverParticle(
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
        // 贴图本身已是深蓝 / 亮蓝 / 紫，保持近白着色以免洗成辉石青。
        float tintJitter = 0.94f + level.random.nextFloat() * 0.06f;
        this.rCol = tintJitter;
        this.gCol = tintJitter;
        this.bCol = 1.0f;
        this.birthAlpha = 0.88f;
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
            return new StarRiverParticle(level, x, y, z, xd, yd, zd, sprites, kind);
        }
    }
}

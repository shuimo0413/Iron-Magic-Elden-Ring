package com.eldenring.spells.particle.foundingrain;

import com.eldenring.spells.spell.FoundingRainOfStarsSpell;

import com.eldenring.spells.client.render.AdditiveParticleRenderType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * 创星雨身前星云气团：深紫 / 深蓝半透明软边，寿命约 3 秒。
 * <p>
 * 颜色在贴图里，顶点近白着色。边缘只降透明度、不混近黑，再关掉深度写入，才不会一块一块像贴纸。
 */
public class NebulaCloudParticle extends TextureSheetParticle {

    /**
     * 每种云朵贴图的尺寸与不透明度。寿命统一走 {@link FoundingRainFx}，方便整团一起淡。
     *
     * @param quadSizeMinBlocks     出生时四边形边长下限（方块）。贴近手里星云雾气。
     * @param quadSizeRandomBlocks  额外随机边长（方块）
     * @param peakAlpha             持有段峰值不透明度。气团半透明，叠几层才够把天空染深。
     * @param rollRadiansPerTick    慢旋（弧度/tick），让叠层不像盖章。
     * @param tintRed               顶点着色红（0–1）。近白才能让贴图自己的深紫/深蓝出来。
     * @param tintGreen             顶点着色绿（0–1）
     * @param tintBlue              顶点着色蓝（0–1）
     */
    public enum Kind {
        /** 深紫气团。 */
        VEIL(1.15f, 0.50f, 0.56f, 0.007f, 1.00f, 1.00f, 1.00f),
        /** 深蓝气团。 */
        BLOOM(1.10f, 0.45f, 0.54f, 0.005f, 1.00f, 1.00f, 1.00f),
        /** 斜向软絮：深紫/深蓝交界，咬开正圆外沿。 */
        VEIL_WISP(0.95f, 0.40f, 0.44f, 0.016f, 1.00f, 1.00f, 1.00f),
        CUMULUS(0.40f, 0.12f, 0.50f, 0.012f, 1.00f, 1.00f, 1.00f),
        CIRRUS(0.36f, 0.12f, 0.42f, 0.022f, 1.00f, 1.00f, 1.00f),
        CORE(0.32f, 0.10f, 0.52f, 0.008f, 1.00f, 1.00f, 1.00f),
        STARDUST(0.30f, 0.10f, 0.48f, 0.014f, 1.00f, 1.00f, 1.00f),
        WISP(0.28f, 0.10f, 0.40f, 0.026f, 1.00f, 1.00f, 1.00f),
        PUFF(0.42f, 0.14f, 0.46f, 0.016f, 1.00f, 1.00f, 1.00f),
        HAZE(0.52f, 0.16f, 0.38f, 0.010f, 1.00f, 1.00f, 1.00f),
        RIFT(0.34f, 0.12f, 0.42f, 0.016f, 1.00f, 1.00f, 1.00f),
        STRATUS(0.48f, 0.14f, 0.40f, 0.008f, 1.00f, 1.00f, 1.00f),
        TWIN(0.42f, 0.14f, 0.46f, 0.014f, 1.00f, 1.00f, 1.00f);

        final float quadSizeMinBlocks;
        final float quadSizeRandomBlocks;
        final float peakAlpha;
        final float rollRadiansPerTick;
        /** 顶点着色 RGB（0–1）。压低绿、略压红，白天也能读出深紫。 */
        final float tintRed;
        final float tintGreen;
        final float tintBlue;

        Kind(
                float quadSizeMinBlocks,
                float quadSizeRandomBlocks,
                float peakAlpha,
                float rollRadiansPerTick,
                float tintRed,
                float tintGreen,
                float tintBlue
        ) {
            this.quadSizeMinBlocks = quadSizeMinBlocks;
            this.quadSizeRandomBlocks = quadSizeRandomBlocks;
            this.peakAlpha = peakAlpha;
            this.rollRadiansPerTick = rollRadiansPerTick;
            this.tintRed = tintRed;
            this.tintGreen = tintGreen;
            this.tintBlue = tintBlue;
        }
    }

    private final SpriteSet sprites;
    private final Kind kind;
    private final float birthQuadSize;
    private final float peakAlpha;

    protected NebulaCloudParticle(
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
        super(level, x, y, z);
        this.sprites = sprites;
        this.kind = kind;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.hasPhysics = false;
        this.gravity = 0.0f;
        this.friction = 0.985f;
        this.lifetime = FoundingRainOfStarsSpell.OVERHEAD_CLOUD_LIFETIME_TICKS
                + level.random.nextInt(FoundingRainFx.OVERHEAD_CLOUD_LIFETIME_RANDOM_TICKS + 1);
        this.birthQuadSize = kind.quadSizeMinBlocks + level.random.nextFloat() * kind.quadSizeRandomBlocks;
        this.quadSize = this.birthQuadSize * 0.82f;
        this.peakAlpha = kind.peakAlpha;
        this.alpha = 0.0f;
        float tintJitter = 0.94f + level.random.nextFloat() * 0.06f;
        this.rCol = kind.tintRed * tintJitter;
        this.gCol = kind.tintGreen * tintJitter;
        this.bCol = kind.tintBlue;
        this.roll = level.random.nextFloat() * ((float) Math.PI * 2.0f);
        this.oRoll = this.roll;
        setSprite(sprites.get(0, 1));
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.age++;
        if (this.age >= this.lifetime) {
            remove();
            return;
        }

        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;
        this.roll += this.kind.rollRadiansPerTick;
        applyFadeEnvelope();
    }

    /**
     * 淡入 → 持有（略呼吸）→ 淡出。包络按自己的寿命算，所以整团散开时是陆续消失。
     */
    private void applyFadeEnvelope() {
        float life = (float) this.age / (float) this.lifetime;
        float fadeInEnd = FoundingRainFx.OVERHEAD_CLOUD_FADE_IN_TICKS / (float) this.lifetime;
        float fadeOutStart = 1.0f - FoundingRainFx.OVERHEAD_CLOUD_FADE_OUT_TICKS / (float) this.lifetime;
        float envelope;
        if (life < fadeInEnd) {
            float fadeInProgress = Mth.clamp(life / Math.max(1.0e-4f, fadeInEnd), 0.0f, 1.0f);
            envelope = fadeInProgress * fadeInProgress * (3.0f - 2.0f * fadeInProgress);
            this.quadSize = this.birthQuadSize * (0.82f + 0.18f * envelope);
        } else if (life > fadeOutStart) {
            float fadeOutProgress = Mth.clamp(
                    (life - fadeOutStart) / Math.max(1.0e-4f, 1.0f - fadeOutStart),
                    0.0f,
                    1.0f
            );
            envelope = 1.0f - fadeOutProgress * fadeOutProgress;
            this.quadSize = this.birthQuadSize * (1.0f + fadeOutProgress * 0.14f);
        } else {
            envelope = 1.0f;
            this.quadSize = this.birthQuadSize * (1.0f + 0.035f * Mth.sin(this.age * 0.11f));
        }
        this.alpha = this.peakAlpha * envelope;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return AdditiveParticleRenderType.SOFT_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

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
            return new NebulaCloudParticle(level, x, y, z, xd, yd, zd, sprites, kind);
        }
    }
}

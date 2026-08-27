package com.eldenring.spells.particle.foundingrain;

import com.eldenring.spells.spell.FoundingRainOfStarsSpell;

import com.eldenring.spells.client.render.AdditiveParticleRenderType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

/**
 * 身前星云的白星星：加法叠在深紫/深蓝气团上。
 * 贴图下标来自 {@link OverheadNebulaAccentOptions.Accent#ordinal()}，构造时锁死，不当动画播。
 */
public class OverheadNebulaAccentParticle extends TextureSheetParticle {

    private final OverheadNebulaAccentOptions.Accent accent;
    private final float birthQuadSize;
    private final float peakAlpha;

    protected OverheadNebulaAccentParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd,
            SpriteSet sprites,
            OverheadNebulaAccentOptions.Accent accent
    ) {
        super(level, x, y, z);
        this.accent = accent;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.hasPhysics = false;
        this.gravity = 0.0f;
        this.friction = 0.985f;
        this.lifetime = FoundingRainOfStarsSpell.OVERHEAD_CLOUD_LIFETIME_TICKS
                + level.random.nextInt(FoundingRainFx.OVERHEAD_CLOUD_LIFETIME_RANDOM_TICKS + 1);
        this.birthQuadSize = accent.quadSizeMinBlocks + level.random.nextFloat() * accent.quadSizeRandomBlocks;
        this.quadSize = this.birthQuadSize * 0.88f;
        this.peakAlpha = accent.peakAlpha;
        this.alpha = 0.0f;
        float tintJitter = 0.94f + level.random.nextFloat() * 0.06f;
        this.rCol = accent.tintRed * tintJitter;
        this.gCol = accent.tintGreen * tintJitter;
        this.bCol = accent.tintBlue;
        if (accent.spin) {
            this.roll = level.random.nextFloat() * ((float) Math.PI * 2.0f);
            this.oRoll = this.roll;
        }
        int spriteSpan = Math.max(1, OverheadNebulaAccentOptions.Accent.values().length - 1);
        setSprite(sprites.get(Mth.clamp(accent.ordinal(), 0, spriteSpan), spriteSpan));
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
        if (accent.spin) {
            this.roll += accent.rollRadiansPerTick;
        }
        applyFadeEnvelope();
    }

    private void applyFadeEnvelope() {
        float life = (float) this.age / (float) this.lifetime;
        float fadeInEnd = FoundingRainFx.OVERHEAD_CLOUD_FADE_IN_TICKS / (float) this.lifetime;
        float fadeOutStart = 1.0f - FoundingRainFx.OVERHEAD_CLOUD_FADE_OUT_TICKS / (float) this.lifetime;
        float envelope;
        if (life < fadeInEnd) {
            float fadeInProgress = Mth.clamp(life / Math.max(1.0e-4f, fadeInEnd), 0.0f, 1.0f);
            envelope = fadeInProgress * fadeInProgress * (3.0f - 2.0f * fadeInProgress);
        } else if (life > fadeOutStart) {
            float fadeOutProgress = Mth.clamp(
                    (life - fadeOutStart) / Math.max(1.0e-4f, 1.0f - fadeOutStart),
                    0.0f,
                    1.0f
            );
            envelope = 1.0f - fadeOutProgress * fadeOutProgress;
        } else {
            envelope = 1.0f;
        }
        if (accent.pulse) {
            float pulse = Mth.sin(life * (float) Math.PI * 3.0f);
            this.alpha = this.peakAlpha * envelope * (0.28f + 0.72f * Math.abs(pulse));
            this.quadSize = this.birthQuadSize * (0.88f + 0.18f * Math.abs(pulse));
        } else {
            this.alpha = this.peakAlpha * envelope;
            this.quadSize = this.birthQuadSize * (0.92f + 0.08f * envelope);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return AdditiveParticleRenderType.ADDITIVE;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<OverheadNebulaAccentOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                OverheadNebulaAccentOptions options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd
        ) {
            return new OverheadNebulaAccentParticle(
                    level, x, y, z, xd, yd, zd, sprites, options.accent()
            );
        }
    }
}

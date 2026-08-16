package com.eldenring.spells.particle.cometazur;

import com.eldenring.spells.client.render.AdditiveParticleRenderType;
import com.eldenring.spells.tuning.CometAzurTuning;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 星辰涟漪上的星点。
 * <p>
 * {@code yd < 0.5}：跟着波前从 0.5 走到 2，并沿圆周微旋。<br>
 * {@code yd ≥ 0.5}：余波，钉在出生点闪一下就淡。
 */
public class CometAzurShockwaveAccentParticle extends TextureSheetParticle {

    public record ShockwaveFrame(Vec3 center, Vec3 rightAxis, Vec3 upAxis) {
    }

    private static final ThreadLocal<ShockwaveFrame> CLIENT_SHOCKWAVE_FRAME = new ThreadLocal<>();
    private static final int ACCENT_SPRITE_COUNT = 7;

    public static void bindShockwaveFrame(ShockwaveFrame shockwaveFrame) {
        CLIENT_SHOCKWAVE_FRAME.set(shockwaveFrame);
    }

    public static void clearShockwaveFrame() {
        CLIENT_SHOCKWAVE_FRAME.remove();
    }

    private final ShockwaveFrame shockwaveFrame;
    private final float ringAngleBirthRadians;
    private final boolean ridingWavefront;
    private final float birthQuadSize;
    private final float birthAlpha;
    private final float twinklePhaseRadians;

    protected CometAzurShockwaveAccentParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double ringAnglePayload,
            double residueFlagPayload,
            SpriteSet sprites
    ) {
        super(level, x, y, z);
        this.ridingWavefront = residueFlagPayload < 0.5;
        ShockwaveFrame boundFrame = CLIENT_SHOCKWAVE_FRAME.get();
        this.shockwaveFrame = boundFrame != null
                ? boundFrame
                : new ShockwaveFrame(new Vec3(x, y, z), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 1.0, 0.0));
        this.ringAngleBirthRadians = (float) ringAnglePayload;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.hasPhysics = false;
        this.gravity = 0.0f;
        this.lifetime = this.ridingWavefront
                ? CometAzurTuning.SHOCKWAVE_DURATION_TICKS
                : CometAzurTuning.SHOCKWAVE_RESIDUE_LIFETIME_TICKS;
        this.birthQuadSize = CometAzurTuning.SHOCKWAVE_ACCENT_QUAD_SIZE_BLOCKS
                * (this.ridingWavefront ? 0.90f : 0.55f)
                * (0.80f + level.random.nextFloat() * 0.50f);
        this.quadSize = this.birthQuadSize;
        this.birthAlpha = this.ridingWavefront ? 0.95f : 0.70f;
        this.alpha = this.birthAlpha;
        this.rCol = 0.82f;
        this.gCol = 0.97f;
        this.bCol = 1.0f;
        this.twinklePhaseRadians = level.random.nextFloat() * ((float) Math.PI * 2.0f);
        int spriteIndex = level.random.nextInt(ACCENT_SPRITE_COUNT);
        setSprite(sprites.get(spriteIndex, ACCENT_SPRITE_COUNT - 1));
        if (this.ridingWavefront) {
            moveToRadius(CometAzurTuning.SHOCKWAVE_RADIUS_START_BLOCKS, this.ringAngleBirthRadians);
        }
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.age++;
        if (this.age >= this.lifetime) {
            remove();
            return;
        }

        float lifeFraction = (float) this.age / (float) this.lifetime;
        float twinkle = 0.62f + 0.38f * Mth.sin(this.age * 0.85f + this.twinklePhaseRadians);
        if (this.ridingWavefront) {
            float eased = 1.0f - (1.0f - lifeFraction) * (1.0f - lifeFraction);
            float radiusBlocks = Mth.lerp(
                    eased,
                    CometAzurTuning.SHOCKWAVE_RADIUS_START_BLOCKS,
                    CometAzurTuning.SHOCKWAVE_RADIUS_END_BLOCKS
            );
            float swirlRadians = this.age * CometAzurTuning.SHOCKWAVE_STAR_SWIRL_RADIANS_PER_TICK;
            moveToRadius(radiusBlocks, this.ringAngleBirthRadians + swirlRadians);
            this.alpha = this.birthAlpha * (1.0f - lifeFraction * lifeFraction) * twinkle;
            this.quadSize = this.birthQuadSize * (1.05f - 0.20f * lifeFraction);
        } else {
            this.alpha = this.birthAlpha * (1.0f - lifeFraction) * twinkle;
            this.quadSize = this.birthQuadSize * (1.0f - 0.45f * lifeFraction);
        }
    }

    private void moveToRadius(float radiusBlocks, float ringAngleRadians) {
        Vec3 worldOffset = this.shockwaveFrame.rightAxis().scale(radiusBlocks * Math.cos(ringAngleRadians))
                .add(this.shockwaveFrame.upAxis().scale(radiusBlocks * Math.sin(ringAngleRadians)));
        Vec3 worldPosition = this.shockwaveFrame.center().add(worldOffset);
        this.x = worldPosition.x;
        this.y = worldPosition.y;
        this.z = worldPosition.z;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return AdditiveParticleRenderType.ADDITIVE;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
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
            return new CometAzurShockwaveAccentParticle(level, x, y, z, xd, yd, this.sprites);
        }
    }
}

package com.eldenring.spells.particle.foundingrain;

import com.eldenring.spells.registry.ModParticles;
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
 * 创星雨升空光点：不走原版速度积分，而是把 {@code xd/yd/zd} 当成「出生点 → 终点」的位移，
 * 在寿命内 ease-out 插值过去，到达后淡出。
 * <p>
 * 每 tick 在上一帧位置刷一颗短命残影，形成小拖尾。贴图复用虚空闪星三帧。
 */
public class StarAscentParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final double birthX;
    private final double birthY;
    private final double birthZ;
    private final double destinationOffsetX;
    private final double destinationOffsetY;
    private final double destinationOffsetZ;
    private final float birthQuadSize;
    private final float birthAlpha;

    protected StarAscentParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double destinationOffsetX,
            double destinationOffsetY,
            double destinationOffsetZ,
            SpriteSet sprites
    ) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.birthX = x;
        this.birthY = y;
        this.birthZ = z;
        this.destinationOffsetX = destinationOffsetX;
        this.destinationOffsetY = destinationOffsetY;
        this.destinationOffsetZ = destinationOffsetZ;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.hasPhysics = false;
        this.gravity = 0.0f;
        this.lifetime = FoundingRainFx.ASCENT_FLIGHT_DURATION_TICKS
                + level.random.nextInt(FoundingRainFx.ASCENT_FLIGHT_DURATION_RANDOM_TICKS + 1);
        this.birthQuadSize = FoundingRainFx.ASCENT_MOTE_QUAD_SIZE_BLOCKS
                + level.random.nextFloat() * FoundingRainFx.ASCENT_MOTE_QUAD_SIZE_RANDOM_BLOCKS;
        this.quadSize = this.birthQuadSize;
        this.birthAlpha = 0.95f;
        this.alpha = this.birthAlpha;
        // 偏亮青白，从紫星云里抽出来仍能读成「光点」，不要再乘一层深紫。
        this.rCol = 0.82f;
        this.gCol = 0.94f;
        this.bCol = 1.0f;
        setSpriteFromAge(sprites);
    }

    /**
     * 不用 {@code super.tick()}：父类会按 xd/yd/zd 做物理位移，而这里的三个分量是终点偏移不是速度。
     */
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

        float life = (float) this.age / (float) this.lifetime;
        // ease-out：出手快抽离星云，接近顶点时放慢，像被吸到头顶。
        float eased = 1.0f - (1.0f - life) * (1.0f - life);
        this.x = this.birthX + this.destinationOffsetX * eased;
        this.y = this.birthY + this.destinationOffsetY * eased;
        this.z = this.birthZ + this.destinationOffsetZ * eased;

        if (life < 0.78f) {
            this.alpha = this.birthAlpha;
            this.quadSize = this.birthQuadSize * (0.90f + 0.12f * Mth.sin(life * (float) Math.PI));
        } else {
            float fade = (1.0f - life) / 0.22f;
            this.alpha = this.birthAlpha * fade;
            this.quadSize = this.birthQuadSize * (0.50f + 0.50f * fade);
        }

        setSpriteFromAge(sprites);
        maybeSpawnTrail();
    }

    /**
     * 在上一帧位置留残影。速度取当前→终点方向的一小段，让拖尾略被拉长。
     */
    private void maybeSpawnTrail() {
        int trailIntervalTicks = Math.max(1, FoundingRainFx.ASCENT_TRAIL_INTERVAL_TICKS);
        if (this.age % trailIntervalTicks != 0) {
            return;
        }
        Vec3 remainingOffset = new Vec3(
                this.birthX + this.destinationOffsetX - this.x,
                this.birthY + this.destinationOffsetY - this.y,
                this.birthZ + this.destinationOffsetZ - this.z
        );
        Vec3 trailVelocity = remainingOffset.lengthSqr() > 1.0e-8
                ? remainingOffset.normalize().scale(FoundingRainFx.ASCENT_TRAIL_STRETCH_SPEED_BLOCKS_PER_TICK)
                : new Vec3(0.0, FoundingRainFx.ASCENT_TRAIL_STRETCH_SPEED_BLOCKS_PER_TICK, 0.0);
        this.level.addParticle(
                ModParticles.STAR_ASCENT_TRAIL.get(),
                this.xo,
                this.yo,
                this.zo,
                trailVelocity.x,
                trailVelocity.y,
                trailVelocity.z
        );
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        float life = ((float) this.age + partialTick) / (float) this.lifetime;
        float brightness = Mth.clamp(1.0f - life * 0.25f, 0.65f, 1.0f);
        int block = (int) (245 * brightness);
        return block | (block << 16);
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
            return new StarAscentParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}

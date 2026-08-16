package com.eldenring.spells.particle.foundingrain;

import com.eldenring.spells.client.render.AdditiveParticleRenderType;
import com.eldenring.spells.tuning.FoundingRainOfStarsTuning;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
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
 * 创星雨落地涟漪：贴在撞击点上方一点点的水平四边形，不朝相机转。
 * <p>
 * 雨点本体是光带，这里只负责「砸到地面」的水圈反馈；三帧贴图按寿命播放。
 */
public class StarRiverRippleParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float birthQuadSize;

    protected StarRiverRippleParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            SpriteSet sprites
    ) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.hasPhysics = false;
        this.gravity = 0.0f;
        this.lifetime = FoundingRainOfStarsTuning.RAIN_RIPPLE_LIFETIME_TICKS;
        this.birthQuadSize = FoundingRainOfStarsTuning.RAIN_RIPPLE_QUAD_SIZE_MIN_BLOCKS
                + level.random.nextFloat() * FoundingRainOfStarsTuning.RAIN_RIPPLE_QUAD_SIZE_RANDOM_BLOCKS;
        this.quadSize = this.birthQuadSize * 0.55f;
        this.rCol = 1.0f;
        this.gCol = 0.94f;
        this.bCol = 1.0f;
        this.alpha = 0.92f;
        // 随机偏航，避免所有水圈盖章对齐。
        this.roll = level.random.nextFloat() * ((float) Math.PI * 2.0f);
        this.oRoll = this.roll;
        setSpriteFromAge(sprites);
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
        setSpriteFromAge(sprites);
        float life = (float) this.age / (float) this.lifetime;
        this.quadSize = this.birthQuadSize * (0.55f + life * 0.75f);
        this.alpha = 0.92f * (1.0f - life * life);
    }

    /**
     * 水平铺在 XZ 上，正反两面都画，站在雨里低头或从下往上看平台都能读到水圈。
     */
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cameraPosition = camera.getPosition();
        float renderX = (float) (Mth.lerp(partialTick, this.xo, this.x) - cameraPosition.x);
        float renderY = (float) (Mth.lerp(partialTick, this.yo, this.y) - cameraPosition.y);
        float renderZ = (float) (Mth.lerp(partialTick, this.zo, this.z) - cameraPosition.z);
        float halfSize = getQuadSize(partialTick);
        float cosYaw = Mth.cos(this.roll);
        float sinYaw = Mth.sin(this.roll);
        float u0 = this.sprite.getU0();
        float u1 = this.sprite.getU1();
        float v0 = this.sprite.getV0();
        float v1 = this.sprite.getV1();
        int packedLight = getLightColor(partialTick);

        putHorizontalVertex(buffer, renderX, renderY, renderZ, -halfSize, -halfSize, u0, v1, cosYaw, sinYaw, packedLight);
        putHorizontalVertex(buffer, renderX, renderY, renderZ, halfSize, -halfSize, u1, v1, cosYaw, sinYaw, packedLight);
        putHorizontalVertex(buffer, renderX, renderY, renderZ, halfSize, halfSize, u1, v0, cosYaw, sinYaw, packedLight);
        putHorizontalVertex(buffer, renderX, renderY, renderZ, -halfSize, halfSize, u0, v0, cosYaw, sinYaw, packedLight);

        putHorizontalVertex(buffer, renderX, renderY, renderZ, -halfSize, halfSize, u0, v0, cosYaw, sinYaw, packedLight);
        putHorizontalVertex(buffer, renderX, renderY, renderZ, halfSize, halfSize, u1, v0, cosYaw, sinYaw, packedLight);
        putHorizontalVertex(buffer, renderX, renderY, renderZ, halfSize, -halfSize, u1, v1, cosYaw, sinYaw, packedLight);
        putHorizontalVertex(buffer, renderX, renderY, renderZ, -halfSize, -halfSize, u0, v1, cosYaw, sinYaw, packedLight);
    }

    private void putHorizontalVertex(
            VertexConsumer buffer,
            float originX,
            float originY,
            float originZ,
            float localX,
            float localZ,
            float u,
            float v,
            float cosYaw,
            float sinYaw,
            int packedLight
    ) {
        float worldX = originX + localX * cosYaw - localZ * sinYaw;
        float worldZ = originZ + localX * sinYaw + localZ * cosYaw;
        buffer.addVertex(worldX, originY, worldZ)
                .setUv(u, v)
                .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                .setLight(packedLight);
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
            return new StarRiverRippleParticle(level, x, y, z, sprites);
        }
    }
}

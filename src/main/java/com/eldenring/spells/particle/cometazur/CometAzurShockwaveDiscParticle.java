package com.eldenring.spells.particle.cometazur;

import com.eldenring.spells.client.render.AdditiveParticleRenderType;
import com.eldenring.spells.registry.ModParticles;
import com.eldenring.spells.tuning.CometAzurTuning;
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
 * 星辰涟漪的光圈：贴在垂直视线的平面上，半径 0.5→2，加法混合，看起来是一圈星光而不是实心盘。
 * <p>
 * {@code xd/yd} = yaw/pitch（度）。{@code zd} = 波次：0 主环（喷星点），1 回声环，2 中心绽光。
 */
public class CometAzurShockwaveDiscParticle extends TextureSheetParticle {

    private enum WaveKind {
        PRIMARY,
        ECHO,
        CORE
    }

    private final WaveKind waveKind;
    private final Vec3 rightAxis;
    private final Vec3 upAxis;
    private final float rollRadiansPerTick;
    private final float radiusStartBlocks;
    private final float radiusEndBlocks;
    private float previousRadiusBlocks;
    private float currentRadiusBlocks;
    private boolean spawnedRidingStars;

    protected CometAzurShockwaveDiscParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double yawDegreesPayload,
            double pitchDegreesPayload,
            double waveIndexPayload,
            SpriteSet sprites
    ) {
        super(level, x, y, z);
        this.waveKind = waveKindFromPayload(waveIndexPayload);
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.hasPhysics = false;
        this.gravity = 0.0f;
        this.rCol = 0.78f;
        this.gCol = 0.96f;
        this.bCol = 1.0f;
        this.roll = 0.0f;
        this.oRoll = 0.0f;

        if (this.waveKind == WaveKind.CORE) {
            this.lifetime = CometAzurTuning.SHOCKWAVE_CORE_DURATION_TICKS;
            this.radiusStartBlocks = CometAzurTuning.SHOCKWAVE_CORE_RADIUS_START_BLOCKS;
            this.radiusEndBlocks = CometAzurTuning.SHOCKWAVE_CORE_RADIUS_END_BLOCKS;
            this.rollRadiansPerTick = 0.10f;
            this.alpha = 0.80f;
            setSprite(sprites.get(2, 2));
        } else if (this.waveKind == WaveKind.ECHO) {
            this.lifetime = CometAzurTuning.SHOCKWAVE_DURATION_TICKS;
            this.radiusStartBlocks = CometAzurTuning.SHOCKWAVE_RADIUS_START_BLOCKS * 0.85f;
            this.radiusEndBlocks = CometAzurTuning.SHOCKWAVE_RADIUS_END_BLOCKS * 0.92f;
            this.rollRadiansPerTick = CometAzurTuning.SHOCKWAVE_ECHO_ROLL_RADIANS_PER_TICK;
            this.alpha = 0.42f;
            setSprite(sprites.get(1, 2));
        } else {
            this.lifetime = CometAzurTuning.SHOCKWAVE_DURATION_TICKS;
            this.radiusStartBlocks = CometAzurTuning.SHOCKWAVE_RADIUS_START_BLOCKS;
            this.radiusEndBlocks = CometAzurTuning.SHOCKWAVE_RADIUS_END_BLOCKS;
            this.rollRadiansPerTick = CometAzurTuning.SHOCKWAVE_RING_ROLL_RADIANS_PER_TICK;
            this.alpha = 0.55f;
            setSprite(sprites.get(0, 2));
        }

        this.previousRadiusBlocks = this.radiusStartBlocks;
        this.currentRadiusBlocks = this.radiusStartBlocks;
        this.quadSize = this.currentRadiusBlocks;

        Vec3 lookDirection = Vec3.directionFromRotation(
                (float) pitchDegreesPayload,
                (float) yawDegreesPayload
        );
        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 computedRight = lookDirection.cross(worldUp);
        if (computedRight.lengthSqr() < 1.0e-8) {
            computedRight = new Vec3(1.0, 0.0, 0.0);
        } else {
            computedRight = computedRight.normalize();
        }
        this.rightAxis = computedRight;
        this.upAxis = computedRight.cross(lookDirection).normalize();
    }

    private static WaveKind waveKindFromPayload(double waveIndexPayload) {
        int waveIndex = (int) Math.round(waveIndexPayload);
        if (waveIndex >= 2) {
            return WaveKind.CORE;
        }
        if (waveIndex == 1) {
            return WaveKind.ECHO;
        }
        return WaveKind.PRIMARY;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        this.previousRadiusBlocks = this.currentRadiusBlocks;
        this.age++;
        if (this.age >= this.lifetime) {
            remove();
            return;
        }

        this.roll += this.rollRadiansPerTick;
        float lifeFraction = (float) this.age / (float) this.lifetime;
        float eased = 1.0f - (1.0f - lifeFraction) * (1.0f - lifeFraction);
        this.currentRadiusBlocks = Mth.lerp(eased, this.radiusStartBlocks, this.radiusEndBlocks);
        this.quadSize = this.currentRadiusBlocks;
        float peakAlpha = this.waveKind == WaveKind.CORE
                ? 0.80f
                : this.waveKind == WaveKind.ECHO ? 0.42f : 0.55f;
        this.alpha = peakAlpha * (1.0f - lifeFraction * lifeFraction);

        if (this.waveKind != WaveKind.PRIMARY) {
            return;
        }
        if (!this.spawnedRidingStars) {
            this.spawnedRidingStars = true;
            spawnRidingStars();
            spawnRadialStreaks();
        }
        if (this.age % CometAzurTuning.SHOCKWAVE_RESIDUE_INTERVAL_TICKS == 0) {
            spawnResidueStars();
        }
    }

    @Override
    public float getQuadSize(float partialTick) {
        return Mth.lerp(partialTick, this.previousRadiusBlocks, this.currentRadiusBlocks);
    }

    private void spawnRidingStars() {
        CometAzurShockwaveAccentParticle.bindShockwaveFrame(new CometAzurShockwaveAccentParticle.ShockwaveFrame(
                new Vec3(this.x, this.y, this.z),
                this.rightAxis,
                this.upAxis
        ));
        try {
            int ridingCount = CometAzurTuning.SHOCKWAVE_RIDING_STAR_COUNT;
            for (int starIndex = 0; starIndex < ridingCount; starIndex++) {
                float ringAngleRadians = (float) (Math.PI * 2.0 * starIndex / ridingCount);
                this.level.addParticle(
                        ModParticles.COMET_AZUR_SHOCKWAVE_ACCENT.get(),
                        this.x,
                        this.y,
                        this.z,
                        ringAngleRadians,
                        0.0,
                        0.0
                );
            }
        } finally {
            CometAzurShockwaveAccentParticle.clearShockwaveFrame();
        }
    }

    /**
     * 余波钉在当前半径上，不再跟着波前走，淡出后留下一圈星尘。
     */
    private void spawnResidueStars() {
        int residueCount = CometAzurTuning.SHOCKWAVE_RESIDUE_STARS_PER_PULSE;
        for (int residueIndex = 0; residueIndex < residueCount; residueIndex++) {
            float ringAngleRadians = (this.random.nextFloat() * ((float) Math.PI * 2.0f));
            Vec3 worldPosition = worldPositionOnRing(this.currentRadiusBlocks, ringAngleRadians);
            this.level.addParticle(
                    ModParticles.COMET_AZUR_SHOCKWAVE_ACCENT.get(),
                    worldPosition.x,
                    worldPosition.y,
                    worldPosition.z,
                    ringAngleRadians,
                    1.0,
                    0.0
            );
        }
    }

    private void spawnRadialStreaks() {
        int streakCount = CometAzurTuning.SHOCKWAVE_STREAK_COUNT;
        for (int streakIndex = 0; streakIndex < streakCount; streakIndex++) {
            float ringAngleRadians = (float) (Math.PI * 2.0 * streakIndex / streakCount)
                    + this.random.nextFloat() * 0.20f;
            Vec3 radialDirection = this.rightAxis.scale(Math.cos(ringAngleRadians))
                    .add(this.upAxis.scale(Math.sin(ringAngleRadians)));
            Vec3 spawnPosition = worldPositionOnRing(this.currentRadiusBlocks * 0.65f, ringAngleRadians);
            this.level.addParticle(
                    ModParticles.STAR_STREAK.get(),
                    spawnPosition.x,
                    spawnPosition.y,
                    spawnPosition.z,
                    radialDirection.x * 0.16,
                    radialDirection.y * 0.16,
                    radialDirection.z * 0.16
            );
        }
    }

    private Vec3 worldPositionOnRing(float radiusBlocks, float ringAngleRadians) {
        return new Vec3(this.x, this.y, this.z)
                .add(this.rightAxis.scale(radiusBlocks * Math.cos(ringAngleRadians)))
                .add(this.upAxis.scale(radiusBlocks * Math.sin(ringAngleRadians)));
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cameraPosition = camera.getPosition();
        float renderX = (float) (Mth.lerp(partialTick, this.xo, this.x) - cameraPosition.x);
        float renderY = (float) (Mth.lerp(partialTick, this.yo, this.y) - cameraPosition.y);
        float renderZ = (float) (Mth.lerp(partialTick, this.zo, this.z) - cameraPosition.z);
        float halfSize = getQuadSize(partialTick);
        float cosineRoll = Mth.cos(Mth.lerp(partialTick, this.oRoll, this.roll));
        float sineRoll = Mth.sin(Mth.lerp(partialTick, this.oRoll, this.roll));
        float u0 = this.sprite.getU0();
        float u1 = this.sprite.getU1();
        float v0 = this.sprite.getV0();
        float v1 = this.sprite.getV1();
        int packedLight = getLightColor(partialTick);

        putPlaneVertex(buffer, renderX, renderY, renderZ, -halfSize, -halfSize, u0, v1, cosineRoll, sineRoll, packedLight);
        putPlaneVertex(buffer, renderX, renderY, renderZ, halfSize, -halfSize, u1, v1, cosineRoll, sineRoll, packedLight);
        putPlaneVertex(buffer, renderX, renderY, renderZ, halfSize, halfSize, u1, v0, cosineRoll, sineRoll, packedLight);
        putPlaneVertex(buffer, renderX, renderY, renderZ, -halfSize, halfSize, u0, v0, cosineRoll, sineRoll, packedLight);

        putPlaneVertex(buffer, renderX, renderY, renderZ, -halfSize, halfSize, u0, v0, cosineRoll, sineRoll, packedLight);
        putPlaneVertex(buffer, renderX, renderY, renderZ, halfSize, halfSize, u1, v0, cosineRoll, sineRoll, packedLight);
        putPlaneVertex(buffer, renderX, renderY, renderZ, halfSize, -halfSize, u1, v1, cosineRoll, sineRoll, packedLight);
        putPlaneVertex(buffer, renderX, renderY, renderZ, -halfSize, -halfSize, u0, v1, cosineRoll, sineRoll, packedLight);
    }

    private void putPlaneVertex(
            VertexConsumer buffer,
            float originX,
            float originY,
            float originZ,
            float localX,
            float localY,
            float u,
            float v,
            float cosineRoll,
            float sineRoll,
            int packedLight
    ) {
        float rotatedX = localX * cosineRoll - localY * sineRoll;
        float rotatedY = localX * sineRoll + localY * cosineRoll;
        float worldX = originX + (float) (this.rightAxis.x * rotatedX + this.upAxis.x * rotatedY);
        float worldY = originY + (float) (this.rightAxis.y * rotatedX + this.upAxis.y * rotatedY);
        float worldZ = originZ + (float) (this.rightAxis.z * rotatedX + this.upAxis.z * rotatedY);
        buffer.addVertex(worldX, worldY, worldZ)
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
            return new CometAzurShockwaveDiscParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}

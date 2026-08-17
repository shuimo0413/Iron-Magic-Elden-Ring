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
 * 星辰涟漪光圈：贴在垂直视线的平面上，多层嵌套胀开，墨绿星河色。
 * <p>
 * {@code xd/yd} = yaw/pitch（度）。{@code zd} = 波次：
 * 0 主环（喷星点）、1 回声、2 中心绽光、3 最外嵌套、4 中层嵌套、5 内层嵌套。
 */
public class CometAzurShockwaveDiscParticle extends TextureSheetParticle {

    private enum WaveKind {
        PRIMARY,
        ECHO,
        CORE,
        OUTER,
        MID,
        INNER
    }

    private final WaveKind waveKind;
    private final Vec3 rightAxis;
    private final Vec3 upAxis;
    private final float rollRadiansPerTick;
    private final float radiusStartBlocks;
    private final float radiusEndBlocks;
    private final float peakAlpha;
    private float previousRadiusBlocks;
    private float currentRadiusBlocks;
    private boolean spawnedRidingStars;

    private record WaveVisuals(
            int lifetimeTicks,
            float radiusStartBlocks,
            float radiusEndBlocks,
            float rollRadiansPerTick,
            float peakAlpha,
            float red,
            float green,
            float blue,
            int spriteIndex
    ) {
    }

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
        WaveVisuals visuals = visualsFor(this.waveKind);
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.hasPhysics = false;
        this.gravity = 0.0f;
        this.lifetime = visuals.lifetimeTicks();
        this.radiusStartBlocks = visuals.radiusStartBlocks();
        this.radiusEndBlocks = visuals.radiusEndBlocks();
        this.rollRadiansPerTick = visuals.rollRadiansPerTick();
        this.peakAlpha = visuals.peakAlpha();
        this.rCol = visuals.red();
        this.gCol = visuals.green();
        this.bCol = visuals.blue();
        this.alpha = this.peakAlpha;
        this.roll = level.random.nextFloat() * 0.35f;
        this.oRoll = this.roll;
        setSprite(sprites.get(visuals.spriteIndex(), 2));

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

    /**
     * 墨绿星河环：暗靛外圈、青绿中圈、偏亮芯。避免原来的冷白青。
     */
    private static WaveVisuals visualsFor(WaveKind waveKind) {
        float baseStart = CometAzurTuning.SHOCKWAVE_RADIUS_START_BLOCKS;
        float baseEnd = CometAzurTuning.SHOCKWAVE_RADIUS_END_BLOCKS;
        return switch (waveKind) {
            case CORE -> new WaveVisuals(
                    CometAzurTuning.SHOCKWAVE_CORE_DURATION_TICKS,
                    CometAzurTuning.SHOCKWAVE_CORE_RADIUS_START_BLOCKS,
                    CometAzurTuning.SHOCKWAVE_CORE_RADIUS_END_BLOCKS,
                    0.12f,
                    0.88f,
                    0.55f,
                    0.92f,
                    0.82f,
                    2
            );
            case OUTER -> new WaveVisuals(
                    CometAzurTuning.SHOCKWAVE_DURATION_TICKS + 4,
                    baseStart * 0.70f,
                    baseEnd * CometAzurTuning.SHOCKWAVE_OUTER_RADIUS_END_SCALE,
                    -0.035f,
                    0.38f,
                    0.18f,
                    0.42f,
                    0.40f,
                    1
            );
            case MID -> new WaveVisuals(
                    CometAzurTuning.SHOCKWAVE_DURATION_TICKS + 2,
                    baseStart * 0.80f,
                    baseEnd * CometAzurTuning.SHOCKWAVE_MID_RADIUS_END_SCALE,
                    0.045f,
                    0.48f,
                    0.22f,
                    0.58f,
                    0.52f,
                    0
            );
            case INNER -> new WaveVisuals(
                    CometAzurTuning.SHOCKWAVE_DURATION_TICKS,
                    baseStart * 1.05f,
                    baseEnd * CometAzurTuning.SHOCKWAVE_INNER_RADIUS_END_SCALE,
                    -0.09f,
                    0.58f,
                    0.28f,
                    0.72f,
                    0.64f,
                    2
            );
            case ECHO -> new WaveVisuals(
                    CometAzurTuning.SHOCKWAVE_DURATION_TICKS,
                    baseStart * 0.90f,
                    baseEnd * 0.95f,
                    CometAzurTuning.SHOCKWAVE_ECHO_ROLL_RADIANS_PER_TICK,
                    0.40f,
                    0.24f,
                    0.62f,
                    0.58f,
                    1
            );
            case PRIMARY -> new WaveVisuals(
                    CometAzurTuning.SHOCKWAVE_DURATION_TICKS,
                    baseStart,
                    baseEnd,
                    CometAzurTuning.SHOCKWAVE_RING_ROLL_RADIANS_PER_TICK,
                    0.62f,
                    0.30f,
                    0.78f,
                    0.70f,
                    0
            );
        };
    }

    private static WaveKind waveKindFromPayload(double waveIndexPayload) {
        int waveIndex = (int) Math.round(waveIndexPayload);
        return switch (waveIndex) {
            case 1 -> WaveKind.ECHO;
            case 2 -> WaveKind.CORE;
            case 3 -> WaveKind.OUTER;
            case 4 -> WaveKind.MID;
            case 5 -> WaveKind.INNER;
            default -> WaveKind.PRIMARY;
        };
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
        // 外圈稍慢展开，内圈更快，多层错开读作嵌套。
        float easePower = switch (this.waveKind) {
            case OUTER -> 1.55f;
            case MID -> 1.35f;
            case INNER -> 0.85f;
            case CORE -> 0.70f;
            default -> 1.15f;
        };
        float eased = 1.0f - (float) Math.pow(1.0f - lifeFraction, easePower);
        this.currentRadiusBlocks = Mth.lerp(eased, this.radiusStartBlocks, this.radiusEndBlocks);
        this.quadSize = this.currentRadiusBlocks;
        float fade = this.waveKind == WaveKind.CORE
                ? (1.0f - lifeFraction)
                : (1.0f - lifeFraction * lifeFraction);
        this.alpha = this.peakAlpha * fade;

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
            Vec3 spawnPosition = worldPositionOnRing(this.currentRadiusBlocks * 0.55f, ringAngleRadians);
            this.level.addParticle(
                    ModParticles.STAR_STREAK.get(),
                    spawnPosition.x,
                    spawnPosition.y,
                    spawnPosition.z,
                    radialDirection.x * 0.22,
                    radialDirection.y * 0.22,
                    radialDirection.z * 0.22
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

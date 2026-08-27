package com.eldenring.spells.spell.fx;

import com.eldenring.spells.entity.GavelOfHaimaEntity;
import com.eldenring.spells.particle.glintstone.GlintstoneFx;
import com.eldenring.spells.spell.curve.GavelOfHaimaCastCurve;
import com.eldenring.spells.registry.ModParticles;
import io.redspace.ironsspellbooks.api.util.CameraShakeData;
import io.redspace.ironsspellbooks.api.util.CameraShakeManager;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 海摩大槌特效：砸地音效、多层冲击环、径向碎晶、镜头震。颜色与粒子密度写死。
 */
public final class GavelOfHaimaFx {

    /** 落地爆裂粒子强度（交给 {@link GlintstoneFx#impact}）。 */
    public static final float IMPACT_PARTICLE_INTENSITY = 3.2f;

    /** 铁魔法 Blastwave 主环缩放。 */
    public static final float BLASTWAVE_SCALE = 3.4f;

    public static final float BLASTWAVE_OUTER_SCALE_A = 1.18f;
    public static final float BLASTWAVE_OUTER_SCALE_B = 1.42f;

    public static final float IMPACT_CYAN_RED = 0.05f;
    public static final float IMPACT_CYAN_GREEN = 0.92f;
    public static final float IMPACT_CYAN_BLUE = 0.88f;

    public static final float IMPACT_CYAN_CORE_RED = 0.15f;
    public static final float IMPACT_CYAN_CORE_GREEN = 0.98f;
    public static final float IMPACT_CYAN_CORE_BLUE = 0.92f;

    public static final int SHOCKWAVE_RING_SPARK_COUNT = 48;
    public static final int SHOCKWAVE_RING_GLOW_COUNT = 28;
    public static final int SHOCKWAVE_RING_MIST_COUNT = 22;
    public static final double SHOCKWAVE_RING_OUTWARD_SPEED = 0.55;

    public static final int CAMERA_SHAKE_DURATION_TICKS = 28;
    public static final float CAMERA_SHAKE_RANGE_BLOCKS = 14.0f;

    private GavelOfHaimaFx() {
    }

    /**
     * 砸地瞬间：重音效 → 多层冲击环 → 径向碎晶雨 → 中心爆裂 → 强镜头震。
     */
    /**
     * 举起阶段客户端点缀：锤头附近少量辉石微粒。必须用 {@link Level#addParticle}。
     */
    public static void spawnRaiseAura(GavelOfHaimaEntity gavelEntity, Level level) {
        if (level.random.nextFloat() > 0.45f) {
            return;
        }
        float swingProgress = gavelEntity.getSwingProgress(0.0f);
        float pitchDegrees = Mth.lerp(
                swingProgress,
                GavelOfHaimaCastCurve.HAMMER_RAISED_PITCH_DEGREES,
                GavelOfHaimaCastCurve.HAMMER_SLAMMED_PITCH_DEGREES
        );
        double headLength = GavelOfHaimaCastCurve.HEAD_LENGTH_ALONG_HANDLE_BLOCKS;
        double pitchRadians = Math.toRadians(pitchDegrees);
        double localY = Math.cos(pitchRadians) * headLength;
        double localZ = Math.sin(pitchRadians) * headLength;
        float yawRadians = gavelEntity.getYRot() * Mth.DEG_TO_RAD;
        double worldX = gavelEntity.getX() + (-Math.sin(yawRadians) * localZ);
        double worldY = gavelEntity.getY() + localY;
        double worldZ = gavelEntity.getZ() + (Math.cos(yawRadians) * localZ);

        double scatter = 0.18;
        level.addParticle(
                ModParticles.GLINTSTONE_GLOW.get(),
                worldX + (level.random.nextDouble() - 0.5) * scatter,
                worldY + (level.random.nextDouble() - 0.5) * scatter,
                worldZ + (level.random.nextDouble() - 0.5) * scatter,
                0.0,
                0.02,
                0.0
        );
        if (level.random.nextFloat() < 0.55f) {
            level.addParticle(
                    ModParticles.GLINTSTONE_MOTE.get(),
                    worldX + (level.random.nextDouble() - 0.5) * scatter,
                    worldY + (level.random.nextDouble() - 0.5) * scatter,
                    worldZ + (level.random.nextDouble() - 0.5) * scatter,
                    (level.random.nextDouble() - 0.5) * 0.04,
                    0.03,
                    (level.random.nextDouble() - 0.5) * 0.04
            );
        }
        if (level.random.nextFloat() < 0.35f) {
            level.addParticle(
                    ModParticles.GLINTSTONE_SPARK.get(),
                    worldX,
                    worldY,
                    worldZ,
                    (level.random.nextDouble() - 0.5) * 0.08,
                    0.05,
                    (level.random.nextDouble() - 0.5) * 0.08
            );
        }
    }

    public static void spawnImpact(Level level, Vec3 impactCenter, float shockwaveRadiusBlocks) {
        spawnImpactSounds(level, impactCenter);
        spawnImpactVisuals(level, impactCenter, shockwaveRadiusBlocks);
        CameraShakeManager.addCameraShake(new CameraShakeData(
                level,
                CAMERA_SHAKE_DURATION_TICKS,
                impactCenter,
                CAMERA_SHAKE_RANGE_BLOCKS
        ));
    }

    private static void spawnImpactSounds(Level level, Vec3 impactCenter) {
        level.playSound(
                null,
                impactCenter.x,
                impactCenter.y,
                impactCenter.z,
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.NEUTRAL,
                1.35f,
                0.55f + level.random.nextFloat() * 0.12f
        );
        level.playSound(
                null,
                impactCenter.x,
                impactCenter.y,
                impactCenter.z,
                SoundEvents.ANVIL_LAND,
                SoundSource.NEUTRAL,
                1.25f,
                0.45f + level.random.nextFloat() * 0.1f
        );
        level.playSound(
                null,
                impactCenter.x,
                impactCenter.y,
                impactCenter.z,
                SoundEvents.AMETHYST_BLOCK_BREAK,
                SoundSource.NEUTRAL,
                1.4f,
                0.65f + level.random.nextFloat() * 0.2f
        );
        level.playSound(
                null,
                impactCenter.x,
                impactCenter.y,
                impactCenter.z,
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.NEUTRAL,
                0.55f,
                1.35f + level.random.nextFloat() * 0.15f
        );
    }

    private static void spawnImpactVisuals(Level level, Vec3 impactCenter, float shockwaveRadiusBlocks) {
        Vector3f cyanDeep = new Vector3f(
                IMPACT_CYAN_RED * 0.75f,
                IMPACT_CYAN_GREEN * 0.85f,
                IMPACT_CYAN_BLUE * 0.85f
        );
        Vector3f cyanEdge = new Vector3f(IMPACT_CYAN_RED, IMPACT_CYAN_GREEN, IMPACT_CYAN_BLUE);
        Vector3f cyanCore = new Vector3f(IMPACT_CYAN_CORE_RED, IMPACT_CYAN_CORE_GREEN, IMPACT_CYAN_CORE_BLUE);

        spawnBlastwaveRing(level, impactCenter, cyanEdge, BLASTWAVE_SCALE * 1.03f, 0.10);
        spawnBlastwaveRing(level, impactCenter, cyanCore, BLASTWAVE_SCALE, 0.14);
        spawnBlastwaveRing(level, impactCenter, cyanEdge, BLASTWAVE_SCALE * 0.97f, 0.18);
        spawnBlastwaveRing(level, impactCenter, cyanDeep, BLASTWAVE_SCALE * BLASTWAVE_OUTER_SCALE_A, 0.08);
        spawnBlastwaveRing(level, impactCenter, cyanEdge, BLASTWAVE_SCALE * BLASTWAVE_OUTER_SCALE_B, 0.06);

        spawnRadialBurstRing(
                level, impactCenter, shockwaveRadiusBlocks * 0.55,
                SHOCKWAVE_RING_SPARK_COUNT, ModParticles.GLINTSTONE_SPARK.get(),
                SHOCKWAVE_RING_OUTWARD_SPEED, true
        );
        spawnRadialBurstRing(
                level, impactCenter, shockwaveRadiusBlocks * 0.72,
                SHOCKWAVE_RING_GLOW_COUNT, ModParticles.GLINTSTONE_GLOW.get(),
                SHOCKWAVE_RING_OUTWARD_SPEED * 0.55, false
        );
        spawnRadialBurstRing(
                level, impactCenter, shockwaveRadiusBlocks * 0.40,
                SHOCKWAVE_RING_MIST_COUNT, ModParticles.GLINTSTONE_MIST.get(),
                SHOCKWAVE_RING_OUTWARD_SPEED * 0.35, false
        );
        spawnRadialBurstRing(
                level, impactCenter, shockwaveRadiusBlocks * 0.85,
                20, ModParticles.GLINTSTONE_SHARD.get(),
                SHOCKWAVE_RING_OUTWARD_SPEED * 0.75, true
        );
        spawnRadialBurstRing(
                level, impactCenter, shockwaveRadiusBlocks * 0.62,
                24, ModParticles.GLINTSTONE_MOTE.get(),
                SHOCKWAVE_RING_OUTWARD_SPEED * 0.45, false
        );

        GlintstoneFx.impact(
                level,
                impactCenter.x,
                impactCenter.y + 0.25,
                impactCenter.z,
                IMPACT_PARTICLE_INTENSITY
        );
        MagicManager.spawnParticles(
                level,
                ModParticles.GLINTSTONE_FLARE.get(),
                impactCenter.x,
                impactCenter.y + 0.35,
                impactCenter.z,
                10,
                0.18,
                0.22,
                0.18,
                0.04,
                false
        );
        MagicManager.spawnParticles(
                level,
                ModParticles.GLINTSTONE_MIST.get(),
                impactCenter.x,
                impactCenter.y + 0.15,
                impactCenter.z,
                28,
                shockwaveRadiusBlocks * 0.55,
                0.12,
                shockwaveRadiusBlocks * 0.55,
                0.06,
                false
        );
    }

    private static void spawnBlastwaveRing(
            Level level,
            Vec3 impactCenter,
            Vector3f color,
            float scale,
            double yOffsetBlocks
    ) {
        MagicManager.spawnParticles(
                level,
                new BlastwaveParticleOptions(color, scale),
                impactCenter.x,
                impactCenter.y + yOffsetBlocks,
                impactCenter.z,
                1,
                0,
                0,
                0,
                0,
                true
        );
    }

    private static void spawnRadialBurstRing(
            Level level,
            Vec3 impactCenter,
            double ringRadiusBlocks,
            int particleCount,
            ParticleOptions particleOptions,
            double outwardSpeed,
            boolean force
    ) {
        for (int index = 0; index < particleCount; index++) {
            double angleRadians = (Math.PI * 2.0) * index / particleCount
                    + level.random.nextDouble() * 0.12;
            double cosine = Math.cos(angleRadians);
            double sine = Math.sin(angleRadians);
            double spawnX = impactCenter.x + cosine * ringRadiusBlocks;
            double spawnY = impactCenter.y + 0.08 + level.random.nextDouble() * 0.25;
            double spawnZ = impactCenter.z + sine * ringRadiusBlocks;
            double velocityY = 0.12 + level.random.nextDouble() * 0.18;
            MagicManager.spawnParticles(
                    level,
                    particleOptions,
                    spawnX,
                    spawnY,
                    spawnZ,
                    0,
                    cosine * outwardSpeed,
                    velocityY,
                    sine * outwardSpeed,
                    1.0,
                    force
            );
        }
    }
}

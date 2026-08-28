package com.eldenring.spells.spell.fx;

import com.eldenring.spells.particle.glintstone.GlintstoneFx;
import com.eldenring.spells.registry.ModParticles;
import com.eldenring.spells.spell.CannonOfHaimaSpell;
import io.redspace.ironsspellbooks.api.util.CameraShakeData;
import io.redspace.ironsspellbooks.api.util.CameraShakeManager;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 海摩炮弹特效：蓄力收束、爆炸音、辉石烟雾、晶体碎片、冲击环、镜头震。颜色与密度写死。
 */
public final class CannonOfHaimaFx {

    public static final float IMPACT_CYAN_RED = 0.05f;
    public static final float IMPACT_CYAN_GREEN = 0.92f;
    public static final float IMPACT_CYAN_BLUE = 0.88f;

    public static final float IMPACT_CYAN_CORE_RED = 0.18f;
    public static final float IMPACT_CYAN_CORE_GREEN = 0.98f;
    public static final float IMPACT_CYAN_CORE_BLUE = 0.94f;

    public static final float BLASTWAVE_SCALE = 4.6f;
    public static final float BLASTWAVE_OUTER_SCALE_A = 1.16f;
    public static final float BLASTWAVE_OUTER_SCALE_B = 1.38f;

    public static final int SMOKE_RING_COUNT = 36;
    public static final int SHARD_BURST_COUNT = 42;
    public static final int SPARK_BURST_COUNT = 32;
    public static final int MIST_CLOUD_COUNT = 48;

    public static final double SMOKE_OUTWARD_SPEED = 0.42;
    public static final double SHARD_OUTWARD_SPEED = 0.68;

    public static final int CAMERA_SHAKE_DURATION_TICKS = 22;
    public static final float CAMERA_SHAKE_RANGE_BLOCKS = 16.0f;

    private CannonOfHaimaFx() {
    }

    /**
     * 蓄力 tick：在施法者眼前收束少量辉石微粒。服务端用 {@link MagicManager} 同步给附近玩家。
     */
    public static void spawnChargeGathering(Level level, LivingEntity caster) {
        Vec3 eyePosition = caster.getEyePosition();
        Vec3 lookDirection = caster.getLookAngle();
        Vec3 gatherCenter = eyePosition.add(lookDirection.scale(0.85));
        MagicManager.spawnParticles(
                level,
                ModParticles.GLINTSTONE_GLOW.get(),
                gatherCenter.x,
                gatherCenter.y,
                gatherCenter.z,
                3,
                0.18,
                0.18,
                0.18,
                0.01,
                false
        );
        MagicManager.spawnParticles(
                level,
                ModParticles.GLINTSTONE_MOTE.get(),
                gatherCenter.x,
                gatherCenter.y,
                gatherCenter.z,
                2,
                0.12,
                0.12,
                0.12,
                0.02,
                false
        );
        if (level.random.nextFloat() < 0.45f) {
            MagicManager.spawnParticles(
                    level,
                    ModParticles.GLINTSTONE_MIST.get(),
                    gatherCenter.x,
                    gatherCenter.y,
                    gatherCenter.z,
                    1,
                    0.10,
                    0.10,
                    0.10,
                    0.008,
                    false
            );
        }
    }

    /**
     * 落地 / 碰敌爆炸：重音 → 冲击环 → 烟雾团 → 碎片雨 → 镜头震。
     */
    public static void spawnImpact(Level level, Vec3 impactCenter) {
        spawnImpactSounds(level, impactCenter);
        spawnImpactVisuals(level, impactCenter);
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
                1.45f,
                0.62f + level.random.nextFloat() * 0.12f
        );
        level.playSound(
                null,
                impactCenter.x,
                impactCenter.y,
                impactCenter.z,
                SoundEvents.AMETHYST_BLOCK_BREAK,
                SoundSource.NEUTRAL,
                1.55f,
                0.55f + level.random.nextFloat() * 0.18f
        );
        level.playSound(
                null,
                impactCenter.x,
                impactCenter.y,
                impactCenter.z,
                SoundEvents.GLASS_BREAK,
                SoundSource.NEUTRAL,
                0.85f,
                0.45f + level.random.nextFloat() * 0.12f
        );
    }

    private static void spawnImpactVisuals(Level level, Vec3 impactCenter) {
        float explosionRadiusBlocks = CannonOfHaimaSpell.EXPLOSION_RADIUS_BLOCKS;
        Vector3f cyanDeep = new Vector3f(
                IMPACT_CYAN_RED * 0.75f,
                IMPACT_CYAN_GREEN * 0.85f,
                IMPACT_CYAN_BLUE * 0.85f
        );
        Vector3f cyanEdge = new Vector3f(IMPACT_CYAN_RED, IMPACT_CYAN_GREEN, IMPACT_CYAN_BLUE);
        Vector3f cyanCore = new Vector3f(IMPACT_CYAN_CORE_RED, IMPACT_CYAN_CORE_GREEN, IMPACT_CYAN_CORE_BLUE);

        spawnBlastwaveRing(level, impactCenter, cyanEdge, BLASTWAVE_SCALE * 1.04f, 0.10);
        spawnBlastwaveRing(level, impactCenter, cyanCore, BLASTWAVE_SCALE, 0.14);
        spawnBlastwaveRing(level, impactCenter, cyanDeep, BLASTWAVE_SCALE * BLASTWAVE_OUTER_SCALE_A, 0.08);
        spawnBlastwaveRing(level, impactCenter, cyanEdge, BLASTWAVE_SCALE * BLASTWAVE_OUTER_SCALE_B, 0.06);

        spawnRadialBurstRing(
                level, impactCenter, explosionRadiusBlocks * 0.38,
                SMOKE_RING_COUNT, ModParticles.GLINTSTONE_MIST.get(),
                SMOKE_OUTWARD_SPEED * 0.55, false
        );
        spawnRadialBurstRing(
                level, impactCenter, explosionRadiusBlocks * 0.58,
                24, ModParticles.GLINTSTONE_GLOW.get(),
                SMOKE_OUTWARD_SPEED * 0.40, false
        );
        spawnRadialBurstRing(
                level, impactCenter, explosionRadiusBlocks * 0.72,
                SHARD_BURST_COUNT, ModParticles.GLINTSTONE_SHARD.get(),
                SHARD_OUTWARD_SPEED, true
        );
        spawnRadialBurstRing(
                level, impactCenter, explosionRadiusBlocks * 0.64,
                SPARK_BURST_COUNT, ModParticles.GLINTSTONE_SPARK.get(),
                SHARD_OUTWARD_SPEED * 0.85, true
        );
        spawnRadialBurstRing(
                level, impactCenter, explosionRadiusBlocks * 0.50,
                22, ModParticles.GLINTSTONE_MOTE.get(),
                SMOKE_OUTWARD_SPEED * 0.70, false
        );

        GlintstoneFx.impact(
                level,
                impactCenter.x,
                impactCenter.y + 0.28,
                impactCenter.z,
                CannonOfHaimaSpell.IMPACT_PARTICLE_INTENSITY
        );
        MagicManager.spawnParticles(
                level,
                ModParticles.GLINTSTONE_FLARE.get(),
                impactCenter.x,
                impactCenter.y + 0.40,
                impactCenter.z,
                12,
                0.22,
                0.28,
                0.22,
                0.05,
                false
        );
        MagicManager.spawnParticles(
                level,
                ModParticles.GLINTSTONE_MIST.get(),
                impactCenter.x,
                impactCenter.y + 0.18,
                impactCenter.z,
                MIST_CLOUD_COUNT,
                explosionRadiusBlocks * 0.62,
                0.22,
                explosionRadiusBlocks * 0.62,
                0.05,
                false
        );
        MagicManager.spawnParticles(
                level,
                ModParticles.GLINTSTONE_SHARD.get(),
                impactCenter.x,
                impactCenter.y + 0.35,
                impactCenter.z,
                18,
                0.35,
                0.45,
                0.35,
                0.42,
                true
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
                    + level.random.nextDouble() * 0.14;
            double cosine = Math.cos(angleRadians);
            double sine = Math.sin(angleRadians);
            double spawnX = impactCenter.x + cosine * ringRadiusBlocks;
            double spawnY = impactCenter.y + 0.10 + level.random.nextDouble() * 0.35;
            double spawnZ = impactCenter.z + sine * ringRadiusBlocks;
            double velocityY = 0.10 + level.random.nextDouble() * 0.28;
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

package com.eldenring.spells.particle.frost;

import com.eldenring.spells.entity.GlintstoneTrailStyle;
import com.eldenring.spells.registry.ModParticles;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 冷辉石弹道粒子：复用已注册的 {@code FROST_*} 贴图，结构对齐辉石 {@code GlintstoneFx} 的拖尾点缀 / 命中爆裂。
 */
public final class FrostFx {

    private FrostFx() {
    }

    /**
     * 弹头附近稀疏寒气 / 碎冰 / 火花。连续光轨仍由彗星头 Renderer 画。
     */
    public static void trailAccents(
            Level level,
            double x,
            double y,
            double z,
            Vec3 motion,
            float intensity,
            GlintstoneTrailStyle trailStyle
    ) {
        float clampedIntensity = Mth.clamp(intensity, 0.25f, 3.0f);
        float densityScale = 0.75f + 0.25f * clampedIntensity;
        Vec3 normalizedFlightDirection = motion.lengthSqr() > 1.0e-8
                ? motion.normalize()
                : Vec3.ZERO;
        double backwardOffsetBlocks = GlintstoneTrailStyle.PARTICLE_TRAIL_MINIMUM_BACK_OFFSET_BLOCKS
                + level.random.nextDouble() * GlintstoneTrailStyle.PARTICLE_TRAIL_RANDOM_BACK_OFFSET_BLOCKS;
        Vec3 particleTrailOrigin = new Vec3(x, y, z)
                .subtract(normalizedFlightDirection.scale(backwardOffsetBlocks));
        double scatterRadiusBlocks = Math.max(
                0.04,
                trailStyle.headHalfWidthBlocks() * (0.85 + 0.15 * clampedIntensity)
        );

        if (level.random.nextFloat() < 0.28f * clampedIntensity) {
            Vec3 glowOffset = Utils.getRandomVec3(scatterRadiusBlocks * 0.75);
            level.addParticle(
                    ModParticles.FROST_GLOW.get(),
                    particleTrailOrigin.x + glowOffset.x,
                    particleTrailOrigin.y + glowOffset.y,
                    particleTrailOrigin.z + glowOffset.z,
                    -normalizedFlightDirection.x * 0.018 + glowOffset.x * 0.06,
                    -normalizedFlightDirection.y * 0.018 + glowOffset.y * 0.06,
                    -normalizedFlightDirection.z * 0.018 + glowOffset.z * 0.06
            );
        }
        if (level.random.nextFloat() < Mth.clamp(trailStyle.sparkChance() * densityScale, 0.0f, 0.85f)) {
            Vec3 sparkOffset = Utils.getRandomVec3(scatterRadiusBlocks);
            level.addParticle(
                    ModParticles.FROST_SPARK.get(),
                    particleTrailOrigin.x + sparkOffset.x,
                    particleTrailOrigin.y + sparkOffset.y,
                    particleTrailOrigin.z + sparkOffset.z,
                    -motion.x * 0.03 + sparkOffset.x * 0.25,
                    -motion.y * 0.03 + sparkOffset.y * 0.25,
                    -motion.z * 0.03 + sparkOffset.z * 0.25
            );
        }
        if (level.random.nextFloat() < 0.22f * clampedIntensity) {
            Vec3 shardOffset = Utils.getRandomVec3(scatterRadiusBlocks * 1.15);
            level.addParticle(
                    ModParticles.FROST_SHARD.get(),
                    particleTrailOrigin.x + shardOffset.x,
                    particleTrailOrigin.y + shardOffset.y,
                    particleTrailOrigin.z + shardOffset.z,
                    -normalizedFlightDirection.x * 0.045 + shardOffset.x * 0.45,
                    -normalizedFlightDirection.y * 0.045 + shardOffset.y * 0.45,
                    -normalizedFlightDirection.z * 0.045 + shardOffset.z * 0.45
            );
        }
        if (level.random.nextFloat() < Mth.clamp(trailStyle.moteChance() * densityScale, 0.0f, 0.45f)) {
            Vec3 sparkleOffset = Utils.getRandomVec3(scatterRadiusBlocks * 0.8);
            level.addParticle(
                    ModParticles.FROST_SPARKLE.get(),
                    particleTrailOrigin.x + sparkleOffset.x,
                    particleTrailOrigin.y + sparkleOffset.y,
                    particleTrailOrigin.z + sparkleOffset.z,
                    sparkleOffset.x * 0.15,
                    sparkleOffset.y * 0.15,
                    sparkleOffset.z * 0.15
            );
        }
    }

    /**
     * 命中寒爆：雾气 + 绽光 + 冰晶飞溅。
     *
     * @param intensity 相对大魔砾命中的密度倍率
     */
    public static void impact(Level level, double x, double y, double z, float intensity) {
        float clampedIntensity = Mth.clamp(intensity, 0.25f, 3.5f);
        double fieldRadiusBlocks = 0.42 * clampedIntensity;
        double smokeRadiusBlocks = 0.55 * clampedIntensity;

        MagicManager.spawnParticles(
                level, ModParticles.FROST_FLARE.get(), x, y, z,
                Math.max(2, Math.round(3 * clampedIntensity)), 0.04, 0.04, 0.04,
                0.02, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.FROST_STAR.get(), x, y, z,
                Math.round(8 * clampedIntensity), fieldRadiusBlocks, fieldRadiusBlocks, fieldRadiusBlocks,
                0.12 * clampedIntensity, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.FROST_MIST.get(), x, y, z,
                Math.round(14 * clampedIntensity), smokeRadiusBlocks, smokeRadiusBlocks * 0.85, smokeRadiusBlocks,
                0.08 * clampedIntensity, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.FROST_CRYSTAL.get(), x, y, z,
                Math.round(10 * clampedIntensity), 0.14 * clampedIntensity, 0.14 * clampedIntensity, 0.14 * clampedIntensity,
                0.36 * clampedIntensity, true
        );
        MagicManager.spawnParticles(
                level, ModParticles.FROST_SNOWFLAKE.get(), x, y, z,
                Math.round(8 * clampedIntensity), 0.22 * clampedIntensity, 0.22 * clampedIntensity, 0.22 * clampedIntensity,
                0.16 * clampedIntensity, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.FROST_SPARK.get(), x, y, z,
                Math.round(16 * clampedIntensity), 0.18 * clampedIntensity, 0.18 * clampedIntensity, 0.18 * clampedIntensity,
                0.32 * clampedIntensity, true
        );
    }
}

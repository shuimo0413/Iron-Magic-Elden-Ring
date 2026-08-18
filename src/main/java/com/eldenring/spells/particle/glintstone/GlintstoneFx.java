package com.eldenring.spells.particle.glintstone;

import com.eldenring.spells.particle.foundingrain.FoundingRainFx;
import com.eldenring.spells.registry.ModParticles;
import com.eldenring.spells.tuning.FoundingRainOfStarsTuning;
import com.eldenring.spells.tuning.GlintstoneTrailTuning;
import com.eldenring.spells.tuning.StarRiverTuning;
import com.eldenring.spells.tuning.StarsOfRuinTuning;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 辉石学派通用粒子助手。新辉石法术应优先调用这里，保持青蓝晶体视觉一致。
 * <p>
     * {@code intensity}：相对魔砾基准的倍率（点缀密度 / 爆裂数量），建议 0.6–2.5。
     * <p>
     * 飞行光轨主体由客户端几何光束绘制；本类只在弹头附近做稀疏点缀，以及命中/施法爆裂。
 */
public final class GlintstoneFx {

    /**
     * 客户端 {@code addParticle} → Provider 同步调用链上的尺寸倍率提示。
     * SimpleParticleType 无法传参，故用 ThreadLocal；仅客户端粒子构造读取。
     */
    private static final ThreadLocal<Float> CLIENT_PARTICLE_SIZE_SCALE = ThreadLocal.withInitial(() -> 1.0f);

    private GlintstoneFx() {
    }

    /**
     * 供辉石粒子构造读取：当前生成点期望的尺寸倍率（1 = 默认）。
     */
    public static float clientParticleSizeScale() {
        return CLIENT_PARTICLE_SIZE_SCALE.get();
    }

    private static void withParticleSizeScale(float sizeScale, Runnable spawnAction) {
        Float previousScale = CLIENT_PARTICLE_SIZE_SCALE.get();
        CLIENT_PARTICLE_SIZE_SCALE.set(sizeScale);
        try {
            spawnAction.run();
        } finally {
            CLIENT_PARTICLE_SIZE_SCALE.set(previousScale);
        }
    }

    public static void trail(Level level, double x, double y, double z, Vec3 motion) {
        trail(level, x, y, z, motion, 1.0f);
    }

    /**
     * 兼容旧调用：在弹头附近生成少量点缀粒子（无雾气、无整条路径采样）。
     */
    public static void trail(Level level, double x, double y, double z, Vec3 motion, float intensity) {
        trailAccents(
                level, x, y, z, motion, intensity,
                new GlintstoneTrailTuning.TrailStyle(8.0, 0.055f, 0.012f, 0.28f, 0.08f, 24)
        );
    }

    /**
     * 飞行拖尾点缀：只在弹头附近稀疏生成光晕 / 火花 / 碎晶 / 闪星。
     * <p>
     * 连续轨迹由客户端几何光带承担，不再沿位移线段采样粒子。
     */
    public static void trailAccents(
            Level level,
            double x,
            double y,
            double z,
            Vec3 motion,
            float intensity,
            GlintstoneTrailTuning.TrailStyle trailStyle
    ) {
        float clampedIntensity = Mth.clamp(intensity, 0.25f, 3.0f);
        float densityScale = 0.75f + 0.25f * clampedIntensity;
        Vec3 normalizedFlightDirection = motion.lengthSqr() > 1.0e-8
                ? motion.normalize()
                : Vec3.ZERO;
        double backwardOffsetBlocks = GlintstoneTrailTuning.PARTICLE_TRAIL_MINIMUM_BACK_OFFSET_BLOCKS
                + level.random.nextDouble() * GlintstoneTrailTuning.PARTICLE_TRAIL_RANDOM_BACK_OFFSET_BLOCKS;
        Vec3 particleTrailOrigin = new Vec3(x, y, z)
                .subtract(normalizedFlightDirection.scale(backwardOffsetBlocks));
        double scatterRadiusBlocks = Math.max(
                0.04,
                trailStyle.headHalfWidthBlocks() * (0.85 + 0.15 * clampedIntensity)
        );
        float glowChance = Mth.clamp(
                GlintstoneTrailTuning.PARTICLE_GLOW_BASE_CHANCE
                        + GlintstoneTrailTuning.PARTICLE_GLOW_CHANCE_PER_INTENSITY * clampedIntensity,
                0.0f,
                0.35f
        );
        if (level.random.nextFloat() < glowChance) {
            Vec3 glowOffset = Utils.getRandomVec3(scatterRadiusBlocks * 0.75);
            float glowSizeScale = Mth.clamp(
                    0.42f + clampedIntensity * 0.16f + trailStyle.headHalfWidthBlocks() * 1.6f,
                    0.55f,
                    1.25f
            );
            withParticleSizeScale(glowSizeScale, () -> level.addParticle(
                    ModParticles.GLINTSTONE_GLOW.get(),
                    particleTrailOrigin.x + glowOffset.x,
                    particleTrailOrigin.y + glowOffset.y,
                    particleTrailOrigin.z + glowOffset.z,
                    -normalizedFlightDirection.x * 0.018 + glowOffset.x * 0.06,
                    -normalizedFlightDirection.y * 0.018 + glowOffset.y * 0.06,
                    -normalizedFlightDirection.z * 0.018 + glowOffset.z * 0.06
            ));
        }

        float mistChance = Mth.clamp(
                GlintstoneTrailTuning.PARTICLE_MIST_BASE_CHANCE
                        + GlintstoneTrailTuning.PARTICLE_MIST_CHANCE_PER_INTENSITY * clampedIntensity,
                0.0f,
                0.18f
        );
        if (level.random.nextFloat() < mistChance) {
            Vec3 mistOffset = Utils.getRandomVec3(scatterRadiusBlocks);
            Vec3 mistOrigin = particleTrailOrigin.subtract(
                    normalizedFlightDirection.scale(level.random.nextDouble() * 0.25)
            );
            float mistSizeScale = Mth.clamp(
                    0.24f + clampedIntensity * 0.10f + trailStyle.headHalfWidthBlocks(),
                    0.28f,
                    0.68f
            );
            withParticleSizeScale(mistSizeScale, () -> level.addParticle(
                    ModParticles.GLINTSTONE_MIST.get(),
                    mistOrigin.x + mistOffset.x,
                    mistOrigin.y + mistOffset.y,
                    mistOrigin.z + mistOffset.z,
                    mistOffset.x * 0.08,
                    0.004 + mistOffset.y * 0.08,
                    mistOffset.z * 0.08
            ));
        }

        float sparkChance = Mth.clamp(trailStyle.sparkChance() * densityScale, 0.0f, 0.85f);
        if (level.random.nextFloat() < sparkChance) {
            Vec3 sparkOffset = Utils.getRandomVec3(scatterRadiusBlocks);
            withParticleSizeScale(0.85f, () -> level.addParticle(
                    ModParticles.GLINTSTONE_SPARK.get(),
                    particleTrailOrigin.x + sparkOffset.x,
                    particleTrailOrigin.y + sparkOffset.y,
                    particleTrailOrigin.z + sparkOffset.z,
                    -motion.x * 0.03 + sparkOffset.x * 0.25,
                    -motion.y * 0.03 + sparkOffset.y * 0.25,
                    -motion.z * 0.03 + sparkOffset.z * 0.25
            ));
        }

        float shardChance = Mth.clamp(
                GlintstoneTrailTuning.PARTICLE_SHARD_BASE_CHANCE
                        + GlintstoneTrailTuning.PARTICLE_SHARD_CHANCE_PER_INTENSITY * clampedIntensity,
                0.0f,
                0.32f
        );
        if (level.random.nextFloat() < shardChance) {
            Vec3 shardOffset = Utils.getRandomVec3(scatterRadiusBlocks * 1.15);
            float shardSizeScale = Mth.clamp(0.48f + clampedIntensity * 0.10f, 0.50f, 0.82f);
            withParticleSizeScale(shardSizeScale, () -> level.addParticle(
                    ModParticles.GLINTSTONE_SHARD.get(),
                    particleTrailOrigin.x + shardOffset.x,
                    particleTrailOrigin.y + shardOffset.y,
                    particleTrailOrigin.z + shardOffset.z,
                    -normalizedFlightDirection.x * 0.045 + shardOffset.x * 0.45,
                    -normalizedFlightDirection.y * 0.045 + shardOffset.y * 0.45,
                    -normalizedFlightDirection.z * 0.045 + shardOffset.z * 0.45
            ));
        }

        float moteChance = Mth.clamp(trailStyle.moteChance() * densityScale, 0.0f, 0.45f);
        if (level.random.nextFloat() < moteChance) {
            Vec3 moteOffset = Utils.getRandomVec3(scatterRadiusBlocks * 0.8);
            withParticleSizeScale(0.9f, () -> level.addParticle(
                    ModParticles.GLINTSTONE_MOTE.get(),
                    particleTrailOrigin.x + moteOffset.x,
                    particleTrailOrigin.y + moteOffset.y,
                    particleTrailOrigin.z + moteOffset.z,
                    moteOffset.x * 0.15,
                    moteOffset.y * 0.15,
                    moteOffset.z * 0.15
            ));
        }
    }

    /**
     * @deprecated 飞行拖尾已改为几何光束；请改用 {@link #trailAccents}。
     */
    @Deprecated
    public static void cometTrailSegment(
            Level level,
            double x,
            double y,
            double z,
            Vec3 motion,
            float intensity,
            float taper01,
            GlintstoneTrailTuning.TrailStyle trailStyle
    ) {
        if (taper01 <= 0.08f) {
            trailAccents(level, x, y, z, motion, intensity, trailStyle);
        }
    }

    public static void impact(Level level, double x, double y, double z) {
        impact(level, x, y, z, 1.0f);
    }

    /**
     * 命中爆裂：中心能量场闪光 + 外扩光环 + 辉石烟雾团 + 晶体碎片飞溅。
     * <p>
     * {@code intensity} 同时放大粒子数量与散布半径；蓄力大型弹道可调到 2.0–3.0。
     * intensity ≥ 2.0 时额外叠一层「蓄力爆」外环与二次碎片雨。
     */
    public static void impact(Level level, double x, double y, double z, float intensity) {
        float clampedIntensity = Mth.clamp(intensity, 0.25f, 3.5f);
        double fieldRadiusBlocks = 0.42 * clampedIntensity;
        double smokeRadiusBlocks = 0.55 * clampedIntensity;

        // 能量场核心：多层绽光叠在中心，制造瞬间「场」感
        int coreFlareCount = Math.max(2, Math.round(3 * clampedIntensity));
        MagicManager.spawnParticles(
                level, ModParticles.GLINTSTONE_FLARE.get(), x, y, z,
                coreFlareCount, 0.04, 0.04, 0.04,
                0.02, false
        );

        // 能量场外环：光晕沿壳层散开
        int fieldGlowCount = Math.round(12 * clampedIntensity);
        MagicManager.spawnParticles(
                level, ModParticles.GLINTSTONE_GLOW.get(), x, y, z,
                fieldGlowCount, fieldRadiusBlocks, fieldRadiusBlocks, fieldRadiusBlocks,
                0.14 * clampedIntensity, false
        );

        // 第二层更淡、更大的场光，拉长能量场残留
        int outerFieldGlowCount = Math.round(6 * clampedIntensity);
        MagicManager.spawnParticles(
                level, ModParticles.GLINTSTONE_GLOW.get(), x, y, z,
                outerFieldGlowCount, fieldRadiusBlocks * 1.35, fieldRadiusBlocks * 1.35, fieldRadiusBlocks * 1.35,
                0.06 * clampedIntensity, false
        );

        // 辉石烟雾：浓密雾团是命中观感的主体之一
        int mistCount = Math.round(14 * clampedIntensity);
        MagicManager.spawnParticles(
                level, ModParticles.GLINTSTONE_MIST.get(), x, y, z,
                mistCount, smokeRadiusBlocks, smokeRadiusBlocks * 0.85, smokeRadiusBlocks,
                0.08 * clampedIntensity, false
        );

        // 晶体飞溅
        int sparkCount = Math.round(16 * clampedIntensity);
        int shardCount = Math.round(10 * clampedIntensity);
        int moteCount = Math.round(6 * clampedIntensity);
        MagicManager.spawnParticles(
                level, ModParticles.GLINTSTONE_SPARK.get(), x, y, z,
                sparkCount, 0.18 * clampedIntensity, 0.18 * clampedIntensity, 0.18 * clampedIntensity,
                0.32 * clampedIntensity, true
        );
        MagicManager.spawnParticles(
                level, ModParticles.GLINTSTONE_SHARD.get(), x, y, z,
                shardCount, 0.14 * clampedIntensity, 0.14 * clampedIntensity, 0.14 * clampedIntensity,
                0.36 * clampedIntensity, true
        );
        MagicManager.spawnParticles(
                level, ModParticles.GLINTSTONE_MOTE.get(), x, y, z,
                moteCount, 0.22 * clampedIntensity, 0.22 * clampedIntensity, 0.22 * clampedIntensity,
                0.16 * clampedIntensity, false
        );

        // 蓄力弹额外堆叠：更远外环 + 二次碎片雨 + 中心二次绽光
        if (clampedIntensity >= 2.0f) {
            float overflowIntensity = clampedIntensity - 1.5f;
            MagicManager.spawnParticles(
                    level, ModParticles.GLINTSTONE_FLARE.get(), x, y, z,
                    Math.round(4 * overflowIntensity), 0.08, 0.08, 0.08,
                    0.04, false
            );
            MagicManager.spawnParticles(
                    level, ModParticles.GLINTSTONE_GLOW.get(), x, y, z,
                    Math.round(10 * overflowIntensity),
                    fieldRadiusBlocks * 1.8, fieldRadiusBlocks * 1.8, fieldRadiusBlocks * 1.8,
                    0.1 * overflowIntensity, false
            );
            MagicManager.spawnParticles(
                    level, ModParticles.GLINTSTONE_MIST.get(), x, y, z,
                    Math.round(12 * overflowIntensity),
                    smokeRadiusBlocks * 1.6, smokeRadiusBlocks * 1.3, smokeRadiusBlocks * 1.6,
                    0.1 * overflowIntensity, false
            );
            MagicManager.spawnParticles(
                    level, ModParticles.GLINTSTONE_SPARK.get(), x, y, z,
                    Math.round(14 * overflowIntensity),
                    0.28 * clampedIntensity, 0.28 * clampedIntensity, 0.28 * clampedIntensity,
                    0.4 * clampedIntensity, true
            );
            MagicManager.spawnParticles(
                    level, ModParticles.GLINTSTONE_SHARD.get(), x, y, z,
                    Math.round(8 * overflowIntensity),
                    0.22 * clampedIntensity, 0.22 * clampedIntensity, 0.22 * clampedIntensity,
                    0.42 * clampedIntensity, true
            );
            MagicManager.spawnParticles(
                    level, ModParticles.GLINTSTONE_MOTE.get(), x, y, z,
                    Math.round(8 * overflowIntensity),
                    0.32 * clampedIntensity, 0.32 * clampedIntensity, 0.32 * clampedIntensity,
                    0.2 * clampedIntensity, false
            );
        }
    }

    public static void castBurst(Level level, double x, double y, double z) {
        castBurst(level, x, y, z, 1.0f);
    }

    public static void castBurst(Level level, double x, double y, double z, float intensity) {
        float clampedIntensity = Mth.clamp(intensity, 0.25f, 3.0f);
        int sparkCount = Math.round(5 * clampedIntensity);
        int glowCount = Math.round(2 * clampedIntensity);
        int mistCount = Math.round(2 * clampedIntensity);
        int moteCount = Math.round(2 * clampedIntensity);
        int shardCount = Math.round(2 * clampedIntensity);

        MagicManager.spawnParticles(
                level, ModParticles.GLINTSTONE_FLARE.get(), x, y, z,
                1, 0.01, 0.01, 0.01,
                0.0, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.GLINTSTONE_SPARK.get(), x, y, z,
                sparkCount, 0.08 * clampedIntensity, 0.08 * clampedIntensity, 0.08 * clampedIntensity,
                0.15 * clampedIntensity, true
        );
        MagicManager.spawnParticles(
                level, ModParticles.GLINTSTONE_SHARD.get(), x, y, z,
                shardCount, 0.06 * clampedIntensity, 0.06 * clampedIntensity, 0.06 * clampedIntensity,
                0.12 * clampedIntensity, true
        );
        MagicManager.spawnParticles(
                level, ModParticles.GLINTSTONE_GLOW.get(), x, y, z,
                glowCount, 0.1 * clampedIntensity, 0.1 * clampedIntensity, 0.1 * clampedIntensity,
                0.05 * clampedIntensity, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.GLINTSTONE_MIST.get(), x, y, z,
                mistCount, 0.12 * clampedIntensity, 0.12 * clampedIntensity, 0.12 * clampedIntensity,
                0.04 * clampedIntensity, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.GLINTSTONE_MOTE.get(), x, y, z,
                moteCount, 0.1 * clampedIntensity, 0.1 * clampedIntensity, 0.1 * clampedIntensity,
                0.08 * clampedIntensity, false
        );
    }

    /**
     * 毁灭流星飞行点缀：用蓝紫星河贴图替换青蓝辉石库。
     * <p>
     * 连续轨迹由客户端几何光束绘制，这里只在弹头后稀疏剥落星尘 / 残影 / 碎晶。
     * 不沿本 tick 位移做路径采样——12 连发叠上去会糊成粒子雾。
     */
    public static void ruinTrailAccents(
            Level level,
            double x,
            double y,
            double z,
            Vec3 motion,
            float intensity,
            GlintstoneTrailTuning.TrailStyle trailStyle
    ) {
        float clampedIntensity = Mth.clamp(intensity, 0.25f, 3.0f);
        float densityScale = (0.70f + 0.22f * clampedIntensity)
                * StarsOfRuinTuning.TRAIL_ACCENT_CHANCE_SCALE;
        Vec3 normalizedFlightDirection = motion.lengthSqr() > 1.0e-8
                ? motion.normalize()
                : Vec3.ZERO;
        double backwardOffsetBlocks = GlintstoneTrailTuning.PARTICLE_TRAIL_MINIMUM_BACK_OFFSET_BLOCKS
                + level.random.nextDouble() * GlintstoneTrailTuning.PARTICLE_TRAIL_RANDOM_BACK_OFFSET_BLOCKS;
        Vec3 particleTrailOrigin = new Vec3(x, y, z)
                .subtract(normalizedFlightDirection.scale(backwardOffsetBlocks));
        double scatterRadiusBlocks = Math.max(
                0.04,
                trailStyle.headHalfWidthBlocks() * (0.85 + 0.15 * clampedIntensity)
        );

        float glowChance = Mth.clamp(
                (GlintstoneTrailTuning.PARTICLE_GLOW_BASE_CHANCE
                        + GlintstoneTrailTuning.PARTICLE_GLOW_CHANCE_PER_INTENSITY * clampedIntensity)
                        * StarsOfRuinTuning.TRAIL_ACCENT_CHANCE_SCALE,
                0.0f,
                0.40f
        );
        if (level.random.nextFloat() < glowChance) {
            Vec3 glowOffset = Utils.getRandomVec3(scatterRadiusBlocks * 0.75);
            float glowSizeScale = Mth.clamp(
                    0.40f + clampedIntensity * 0.14f + trailStyle.headHalfWidthBlocks() * 1.5f,
                    0.50f,
                    1.15f
            );
            withParticleSizeScale(glowSizeScale, () -> level.addParticle(
                    ModParticles.STAR_RIVER_GLOW.get(),
                    particleTrailOrigin.x + glowOffset.x,
                    particleTrailOrigin.y + glowOffset.y,
                    particleTrailOrigin.z + glowOffset.z,
                    -normalizedFlightDirection.x * 0.018 + glowOffset.x * 0.06,
                    -normalizedFlightDirection.y * 0.018 + glowOffset.y * 0.06,
                    -normalizedFlightDirection.z * 0.018 + glowOffset.z * 0.06
            ));
        }

        float dustChance = Mth.clamp(
                (GlintstoneTrailTuning.PARTICLE_MIST_BASE_CHANCE
                        + GlintstoneTrailTuning.PARTICLE_MIST_CHANCE_PER_INTENSITY * clampedIntensity)
                        * StarsOfRuinTuning.TRAIL_ACCENT_CHANCE_SCALE,
                0.0f,
                0.22f
        );
        if (level.random.nextFloat() < dustChance) {
            Vec3 dustOffset = Utils.getRandomVec3(scatterRadiusBlocks);
            Vec3 dustOrigin = particleTrailOrigin.subtract(
                    normalizedFlightDirection.scale(level.random.nextDouble() * 0.25)
            );
            float dustSizeScale = Mth.clamp(
                    0.26f + clampedIntensity * 0.10f + trailStyle.headHalfWidthBlocks(),
                    0.28f,
                    0.72f
            );
            withParticleSizeScale(dustSizeScale, () -> level.addParticle(
                    ModParticles.TWIN_DUST.get(),
                    dustOrigin.x + dustOffset.x,
                    dustOrigin.y + dustOffset.y,
                    dustOrigin.z + dustOffset.z,
                    dustOffset.x * 0.08,
                    0.004 + dustOffset.y * 0.08,
                    dustOffset.z * 0.08
            ));
        }

        float streakChance = Mth.clamp(trailStyle.sparkChance() * densityScale, 0.0f, 0.28f);
        if (level.random.nextFloat() < streakChance) {
            Vec3 streakOffset = Utils.getRandomVec3(scatterRadiusBlocks);
            withParticleSizeScale(0.90f, () -> level.addParticle(
                    ModParticles.STAR_STREAK.get(),
                    particleTrailOrigin.x + streakOffset.x,
                    particleTrailOrigin.y + streakOffset.y,
                    particleTrailOrigin.z + streakOffset.z,
                    -motion.x * 0.04 + streakOffset.x * 0.20,
                    -motion.y * 0.04 + streakOffset.y * 0.20,
                    -motion.z * 0.04 + streakOffset.z * 0.20
            ));
        }

        float shardChance = Mth.clamp(
                (GlintstoneTrailTuning.PARTICLE_SHARD_BASE_CHANCE
                        + GlintstoneTrailTuning.PARTICLE_SHARD_CHANCE_PER_INTENSITY * clampedIntensity)
                        * StarsOfRuinTuning.TRAIL_ACCENT_CHANCE_SCALE,
                0.0f,
                0.16f
        );
        if (level.random.nextFloat() < shardChance) {
            Vec3 shardOffset = Utils.getRandomVec3(scatterRadiusBlocks * 1.15);
            float shardSizeScale = Mth.clamp(0.48f + clampedIntensity * 0.10f, 0.50f, 0.82f);
            withParticleSizeScale(shardSizeScale, () -> level.addParticle(
                    ModParticles.VIOLET_SHARD.get(),
                    particleTrailOrigin.x + shardOffset.x,
                    particleTrailOrigin.y + shardOffset.y,
                    particleTrailOrigin.z + shardOffset.z,
                    -normalizedFlightDirection.x * 0.045 + shardOffset.x * 0.45,
                    -normalizedFlightDirection.y * 0.045 + shardOffset.y * 0.45,
                    -normalizedFlightDirection.z * 0.045 + shardOffset.z * 0.45
            ));
        }

        float clusterChance = Mth.clamp(trailStyle.moteChance() * densityScale, 0.0f, 0.18f);
        if (level.random.nextFloat() < clusterChance) {
            Vec3 clusterOffset = Utils.getRandomVec3(scatterRadiusBlocks * 0.8);
            boolean useCluster = level.random.nextBoolean();
            withParticleSizeScale(0.88f, () -> level.addParticle(
                    useCluster ? ModParticles.STAR_CLUSTER.get() : ModParticles.VOID_MOTE.get(),
                    particleTrailOrigin.x + clusterOffset.x,
                    particleTrailOrigin.y + clusterOffset.y,
                    particleTrailOrigin.z + clusterOffset.z,
                    clusterOffset.x * 0.12,
                    clusterOffset.y * 0.12,
                    clusterOffset.z * 0.12
            ));
        }
    }

    /**
     * 毁灭流星命中爆裂：虚空核 + 蚀环做中心，紫晶碎片飞溅，星团 / 双色星尘铺体积。
     * 数量按 intensity 放大，但比青蓝辉石命中更克制，避免十二发叠成白雾。
     */
    public static void ruinImpact(Level level, double x, double y, double z, float intensity) {
        float clampedIntensity = Mth.clamp(intensity, 0.25f, 3.5f);
        double fieldRadiusBlocks = 0.38 * clampedIntensity;
        double smokeRadiusBlocks = 0.48 * clampedIntensity;

        MagicManager.spawnParticles(
                level, ModParticles.VOID_CORE.get(), x, y, z,
                1, 0.01, 0.01, 0.01,
                0.0, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.ABYSS_FLARE.get(), x, y, z,
                Math.max(1, Math.round(2 * clampedIntensity)), 0.04, 0.04, 0.04,
                0.02, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.ECLIPSE_RING.get(), x, y, z,
                1, 0.02, 0.02, 0.02,
                0.0, false
        );
        if (clampedIntensity >= 1.15f) {
            MagicManager.spawnParticles(
                    level, ModParticles.PULSE_RING.get(), x, y, z,
                    1, 0.03, 0.03, 0.03,
                    0.0, false
            );
        }

        MagicManager.spawnParticles(
                level, ModParticles.STAR_RIVER_GLOW.get(), x, y, z,
                Math.round(8 * clampedIntensity), fieldRadiusBlocks, fieldRadiusBlocks, fieldRadiusBlocks,
                0.10 * clampedIntensity, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.TWIN_DUST.get(), x, y, z,
                Math.round(7 * clampedIntensity), smokeRadiusBlocks, smokeRadiusBlocks * 0.85, smokeRadiusBlocks,
                0.07 * clampedIntensity, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.STAR_RIVER_MIST.get(), x, y, z,
                Math.round(6 * clampedIntensity), smokeRadiusBlocks, smokeRadiusBlocks * 0.80, smokeRadiusBlocks,
                0.06 * clampedIntensity, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.VIOLET_SHARD.get(), x, y, z,
                Math.round(8 * clampedIntensity), 0.14 * clampedIntensity, 0.14 * clampedIntensity, 0.14 * clampedIntensity,
                0.32 * clampedIntensity, true
        );
        MagicManager.spawnParticles(
                level, ModParticles.STAR_CLUSTER.get(), x, y, z,
                Math.round(4 * clampedIntensity), 0.18 * clampedIntensity, 0.18 * clampedIntensity, 0.18 * clampedIntensity,
                0.12 * clampedIntensity, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.VOID_MOTE.get(), x, y, z,
                Math.round(5 * clampedIntensity), 0.16 * clampedIntensity, 0.16 * clampedIntensity, 0.16 * clampedIntensity,
                0.14 * clampedIntensity, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.NOVA_STAR.get(), x, y, z,
                Math.round(2 * clampedIntensity), 0.10 * clampedIntensity, 0.10 * clampedIntensity, 0.10 * clampedIntensity,
                0.08 * clampedIntensity, false
        );
    }

    /**
     * 紫色星云：在右手前方、与头部平齐处，用对数螺线 + 高斯椭球铺一团深蓝紫星云。
     * <p>
     * 不再沿视线拉粉尘螺旋（会挡准星）。{@code useRuinPalette} 保留调用方语义，
     * 贴图本身已是毁灭流星配色。
     */
    public static void starRiver(Level level, LivingEntity caster, float intensity, boolean useRuinPalette) {
        starRiver(level, caster.getEyePosition(), caster.getLookAngle(), intensity, useRuinPalette);
    }

    /**
     * @param headOrigin    头部 / 眼睛位置；方法内部再偏到右手前方
     * @param lookDirection 当前朝向，用来算水平右/前，并让星盘大致朝向镜头
     */
    public static void starRiver(
            Level level,
            Vec3 headOrigin,
            Vec3 lookDirection,
            float intensity,
            boolean useRuinPalette
    ) {
        spawnStarNebula(level, headOrigin, lookDirection, intensity, useRuinPalette, false);
    }

    /**
     * 兼容旧签名：长度不再沿视线铺开，半径改由 {@link StarRiverTuning#NEBULA_RADIUS_BLOCKS} 决定。
     */
    public static void starRiver(
            Level level,
            Vec3 origin,
            Vec3 lookDirection,
            float intensity,
            double riverLengthBlocks,
            double riverRadiusBlocks,
            boolean useRuinPalette
    ) {
        // 旧参数曾表示沿视线拉长的螺旋；星云改用 StarRiverTuning，这里只保留签名兼容。
        spawnStarNebula(level, origin, lookDirection, intensity, useRuinPalette, false);
    }

    /**
     * 每发流星从星云里拽出残影与闪星，不整团重铺。
     */
    public static void starRiverLaunch(Level level, LivingEntity caster, float intensity) {
        spawnStarNebula(level, caster.getEyePosition(), caster.getLookAngle(), intensity, true, true);
    }

    /**
     * 施法者右手旁的星云。蓄力环等仍走同一锚点，避免胸口再喷一条河。
     */
    public static void starRiverOrbit(Level level, LivingEntity caster, float intensity, boolean useRuinPalette) {
        starRiver(level, caster, intensity, useRuinPalette);
    }

    /**
     * 创星雨身前星云：气团由实体软光面片绘制，这里只保留兼容入口。
     * 实现见 {@link FoundingRainFx#spawnOverheadStars}。
     */
    public static void overheadRainCloud(Level level, Vec3 cloudCenter) {
        FoundingRainFx.spawnOverheadStars(level, cloudCenter, 0.0f);
    }

    /**
     * 创星雨升空：从右手星云体内抽样，把光点射向出手瞬间钉死的雨云圆心。
     */
    public static void starRiverAscent(Level level, LivingEntity caster, int moteCountThisTick) {
        starRiverAscent(level, caster, moteCountThisTick, FoundingRainFx.cloudCenterInFrontOf(caster));
    }

    /**
     * 创星雨升空：从右手星云体内抽样，把光点射向已经钉死的雨云圆心。
     * <p>
     * {@code gatheringCenter} 必须是出手瞬间算好的世界坐标，不能每 tick 跟玩家重算。
     * {@code moteCountThisTick} 由时序实体按 tick 分批传入。
     * 粒子收到的 {@code xd/yd/zd} 是终点相对出生点的位移，不是速度；见 {@code StarAscentParticle}。
     */
    public static void starRiverAscent(
            Level level,
            LivingEntity caster,
            int moteCountThisTick,
            Vec3 gatheringCenter
    ) {
        if (moteCountThisTick <= 0) {
            return;
        }
        NebulaFrame frame = nebulaFrame(caster.getEyePosition(), caster.getLookAngle());
        double scatterRadiusBlocks = FoundingRainOfStarsTuning.ASCENT_TARGET_SCATTER_RADIUS_BLOCKS;
        double heightJitterBlocks = FoundingRainOfStarsTuning.ASCENT_TARGET_HEIGHT_JITTER_BLOCKS;
        double sampleRadiusBlocks = StarRiverTuning.NEBULA_RADIUS_BLOCKS;
        double sampleDepthBlocks = sampleRadiusBlocks * StarRiverTuning.NEBULA_DEPTH_FRACTION;

        for (int moteIndex = 0; moteIndex < moteCountThisTick; moteIndex++) {
            Vec3 originOffset = gaussianEllipsoidOffset(level, frame, sampleRadiusBlocks, sampleDepthBlocks);
            Vec3 origin = frame.center.add(originOffset);
            double yawRadians = level.random.nextDouble() * Math.PI * 2.0;
            double radialBlocks = scatterRadiusBlocks * Math.sqrt(level.random.nextDouble());
            Vec3 destination = gatheringCenter.add(
                    Math.cos(yawRadians) * radialBlocks,
                    (level.random.nextDouble() - 0.5) * 2.0 * heightJitterBlocks,
                    Math.sin(yawRadians) * radialBlocks
            );
            Vec3 destinationOffset = destination.subtract(origin);
            spawnOne(level, ModParticles.STAR_ASCENT_MOTE.get(), origin, destinationOffset);

            Vec3 birthTrailVelocity = destinationOffset.lengthSqr() > 1.0e-8
                    ? destinationOffset.normalize().scale(0.07)
                    : new Vec3(0.0, 0.07, 0.0);
            spawnOne(level, ModParticles.STAR_ASCENT_TRAIL.get(), origin, birthTrailVelocity);
        }
    }

    /**
     * @param launchOnly {@code true} 只刷从核指向视线的残影/闪星，给齐射用
     */
    private static void spawnStarNebula(
            Level level,
            Vec3 headOrigin,
            Vec3 lookDirection,
            float intensity,
            boolean useRuinPalette,
            boolean launchOnly
    ) {
        float clampedIntensity = Mth.clamp(intensity, 0.25f, 3.0f);
        NebulaFrame frame = nebulaFrame(headOrigin, lookDirection);
        if (launchOnly) {
            spawnNebulaLaunch(level, frame, clampedIntensity);
            return;
        }
        spawnNebulaCore(level, frame, clampedIntensity);
        spawnNebulaVolume(level, frame, clampedIntensity);
        spawnNebulaSpiralArms(level, frame, clampedIntensity);
        spawnNebulaStars(level, frame, clampedIntensity);
        spawnNebulaFilaments(level, frame, clampedIntensity);
        spawnNebulaRim(level, frame, clampedIntensity, useRuinPalette);
    }

    /**
     * 星云局部标架：锚点在右手前方、与头平齐；盘面由镜头右轴与世界上轴张成，方便第一人称看见漩涡正面。
     */
    private static NebulaFrame nebulaFrame(Vec3 headOrigin, Vec3 lookDirection) {
        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 look = lookDirection.lengthSqr() > 1.0e-8 ? lookDirection.normalize() : new Vec3(0.0, 0.0, 1.0);
        Vec3 horizontalForward = new Vec3(look.x, 0.0, look.z);
        if (horizontalForward.lengthSqr() < 1.0e-8) {
            horizontalForward = new Vec3(0.0, 0.0, 1.0);
        } else {
            horizontalForward = horizontalForward.normalize();
        }
        Vec3 horizontalRight = horizontalForward.cross(worldUp);
        if (horizontalRight.lengthSqr() < 1.0e-8) {
            horizontalRight = new Vec3(1.0, 0.0, 0.0);
        } else {
            horizontalRight = horizontalRight.normalize();
        }

        Vec3 nebulaCenter = headOrigin
                .add(horizontalRight.scale(StarRiverTuning.ANCHOR_RIGHT_OFFSET_BLOCKS))
                .add(horizontalForward.scale(StarRiverTuning.ANCHOR_FORWARD_OFFSET_BLOCKS))
                .add(0.0, StarRiverTuning.ANCHOR_UP_OFFSET_BLOCKS, 0.0);

        Vec3 cameraRight = look.cross(worldUp);
        if (cameraRight.lengthSqr() < 1.0e-8) {
            cameraRight = horizontalRight;
        } else {
            cameraRight = cameraRight.normalize();
        }
        Vec3 cameraUp = cameraRight.cross(look).normalize();
        return new NebulaFrame(nebulaCenter, look, cameraRight, cameraUp);
    }

    private static void spawnNebulaCore(Level level, NebulaFrame frame, float intensity) {
        spawnOne(level, ModParticles.VOID_CORE.get(), frame.center, Vec3.ZERO);
        spawnOne(level, ModParticles.NEBULA_SPIRAL.get(), frame.center, Vec3.ZERO);
        spawnOne(level, ModParticles.ECLIPSE_RING.get(), frame.center, Vec3.ZERO);
        if (intensity >= 1.15f) {
            spawnOne(level, ModParticles.ABYSS_FLARE.get(), frame.center, Vec3.ZERO);
        }
        if (intensity >= 1.45f) {
            spawnOne(level, ModParticles.PULSE_RING.get(), frame.center, Vec3.ZERO);
        }
    }

    /**
     * 高斯椭球体积：雾气 / 辉光 / 双色星尘构成星云本体。
     */
    private static void spawnNebulaVolume(Level level, NebulaFrame frame, float intensity) {
        double radiusBlocks = StarRiverTuning.NEBULA_RADIUS_BLOCKS;
        double depthBlocks = radiusBlocks * StarRiverTuning.NEBULA_DEPTH_FRACTION;
        int mistCount = Math.max(3, Math.round(StarRiverTuning.MIST_COUNT_PER_INTENSITY * intensity));
        int glowCount = Math.max(2, Math.round(StarRiverTuning.GLOW_COUNT_PER_INTENSITY * intensity));
        int dustCount = Math.max(2, Math.round(StarRiverTuning.DUST_COUNT_PER_INTENSITY * intensity));

        for (int mistIndex = 0; mistIndex < mistCount; mistIndex++) {
            Vec3 offset = gaussianEllipsoidOffset(level, frame, radiusBlocks, depthBlocks);
            spawnOne(level, ModParticles.STAR_RIVER_MIST.get(), frame.center.add(offset), offset.scale(0.04));
        }
        for (int glowIndex = 0; glowIndex < glowCount; glowIndex++) {
            Vec3 offset = gaussianEllipsoidOffset(level, frame, radiusBlocks * 0.72, depthBlocks * 0.8);
            spawnOne(level, ModParticles.STAR_RIVER_GLOW.get(), frame.center.add(offset), offset.scale(0.03));
        }
        for (int dustIndex = 0; dustIndex < dustCount; dustIndex++) {
            Vec3 offset = gaussianEllipsoidOffset(level, frame, radiusBlocks * 0.85, depthBlocks);
            spawnOne(level, ModParticles.TWIN_DUST.get(), frame.center.add(offset), offset.scale(0.025));
        }
    }

    /**
     * 双臂对数螺线：沿臂撒闪星、彗星残影，臂心叠辉光。
     */
    private static void spawnNebulaSpiralArms(Level level, NebulaFrame frame, float intensity) {
        int samplesPerArm = Math.max(5, Math.round(StarRiverTuning.SPIRAL_SAMPLES_PER_ARM_PER_INTENSITY * intensity));
        long gameTimeTicks = level.getGameTime();
        double radiusBlocks = StarRiverTuning.NEBULA_RADIUS_BLOCKS;
        int streakBudget = Math.max(1, Math.round(StarRiverTuning.STREAK_COUNT_PER_INTENSITY * intensity));
        int streaksSpawned = 0;

        for (int armIndex = 0; armIndex < 2; armIndex++) {
            for (int sampleIndex = 0; sampleIndex < samplesPerArm; sampleIndex++) {
                double theta = ((sampleIndex + 0.5) / samplesPerArm) * StarRiverTuning.SPIRAL_THETA_SPAN_RADIANS;
                Vec3 armOffset = logSpiralOffset(theta, armIndex, frame, radiusBlocks, gameTimeTicks);
                Vec3 scatter = gaussianEllipsoidOffset(
                        level,
                        frame,
                        radiusBlocks * StarRiverTuning.ARM_SCATTER_FRACTION,
                        radiusBlocks * StarRiverTuning.ARM_SCATTER_FRACTION * 0.6
                );
                Vec3 particlePosition = frame.center.add(armOffset).add(scatter);
                Vec3 tangent = spiralTangent(theta, armIndex, frame, gameTimeTicks).scale(0.08);

                spawnOne(level, ModParticles.VOID_MOTE.get(), particlePosition, tangent);
                if (sampleIndex % 2 == 0) {
                    spawnOne(level, ModParticles.STAR_RIVER_GLOW.get(), particlePosition, tangent.scale(0.4));
                }
                if (streaksSpawned < streakBudget && (sampleIndex + armIndex) % 3 == 0) {
                    spawnOne(level, ModParticles.STAR_STREAK.get(), particlePosition, tangent.scale(1.8).add(frame.look.scale(0.04)));
                    streaksSpawned++;
                }
            }
        }
    }

    /**
     * 盘面上按半径平方根采样亮星，避免全堆在核里。
     */
    private static void spawnNebulaStars(Level level, NebulaFrame frame, float intensity) {
        int starCount = Math.max(3, Math.round(StarRiverTuning.STAR_ACCENT_COUNT_PER_INTENSITY * intensity));
        int moteCount = Math.max(4, Math.round(StarRiverTuning.MOTE_COUNT_PER_INTENSITY * intensity));
        double radiusBlocks = StarRiverTuning.NEBULA_RADIUS_BLOCKS;

        for (int starIndex = 0; starIndex < starCount; starIndex++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double radial = radiusBlocks * (0.25 + 0.75 * Math.sqrt(level.random.nextDouble()));
            Vec3 offset = frame.diskRight.scale(Math.cos(angle) * radial)
                    .add(frame.diskUp.scale(Math.sin(angle) * radial))
                    .add(frame.look.scale((level.random.nextDouble() - 0.5) * radiusBlocks * 0.2));
            Vec3 starPosition = frame.center.add(offset);
            int roll = starIndex % 4;
            if (roll == 0) {
                spawnOne(level, ModParticles.NOVA_STAR.get(), starPosition, Vec3.ZERO);
            } else if (roll == 1) {
                spawnOne(level, ModParticles.BINARY_STAR.get(), starPosition, Vec3.ZERO);
            } else if (roll == 2) {
                spawnOne(level, ModParticles.STAR_CLUSTER.get(), starPosition, Vec3.ZERO);
            } else {
                spawnOne(level, ModParticles.VOID_MOTE.get(), starPosition, offset.scale(0.02));
            }
        }

        for (int moteIndex = 0; moteIndex < moteCount; moteIndex++) {
            Vec3 offset = gaussianEllipsoidOffset(level, frame, radiusBlocks, radiusBlocks * StarRiverTuning.NEBULA_DEPTH_FRACTION);
            spawnOne(level, ModParticles.VOID_MOTE.get(), frame.center.add(offset), offset.scale(0.03));
        }
    }

    /**
     * S 形暗丝穿过盘面，给星云骨架。
     */
    private static void spawnNebulaFilaments(Level level, NebulaFrame frame, float intensity) {
        int filamentCount = Math.max(2, Math.round(StarRiverTuning.FILAMENT_COUNT_PER_INTENSITY * intensity));
        double radiusBlocks = StarRiverTuning.NEBULA_RADIUS_BLOCKS;
        long gameTimeTicks = level.getGameTime();
        int samplesPerFilament = 3;
        for (int filamentIndex = 0; filamentIndex < filamentCount; filamentIndex++) {
            for (int sampleIndex = 0; sampleIndex < samplesPerFilament; sampleIndex++) {
                double along = (sampleIndex + 0.5) / samplesPerFilament;
                Vec3 offset = filamentOffset(along, filamentIndex, frame, radiusBlocks, gameTimeTicks);
                spawnOne(level, ModParticles.DARK_FILAMENT.get(), frame.center.add(offset), offset.scale(0.02));
            }
        }
    }

    private static void spawnNebulaRim(Level level, NebulaFrame frame, float intensity, boolean useRuinPalette) {
        double radiusBlocks = StarRiverTuning.NEBULA_RADIUS_BLOCKS;
        Vec3 crescentOffset = frame.diskRight.scale(-radiusBlocks * 0.55)
                .add(frame.diskUp.scale(-radiusBlocks * 0.2));
        spawnOne(level, ModParticles.CRESCENT_WAKE.get(), frame.center.add(crescentOffset), Vec3.ZERO);

        int shardCount = Math.max(1, Math.round(2 * intensity));
        for (int shardIndex = 0; shardIndex < shardCount; shardIndex++) {
            Vec3 offset = gaussianEllipsoidOffset(level, frame, radiusBlocks * 0.9, radiusBlocks * 0.3);
            Vec3 shardVelocity = offset.lengthSqr() > 1.0e-8 ? offset.normalize().scale(0.12) : frame.look.scale(0.08);
            spawnOne(level, ModParticles.VIOLET_SHARD.get(), frame.center.add(offset), shardVelocity);
        }
        if (!useRuinPalette) {
            spawnOne(level, ModParticles.STAR_RIVER_GLOW.get(), frame.center, Vec3.ZERO);
        }
    }

    /**
     * 从星云核沿视线喷出残影，模拟流星被甩出去。
     * 主体用彗星残影 + 双色星尘；紫晶碎片、星团、虚空核做点缀，不再叠青蓝辉石。
     */
    private static void spawnNebulaLaunch(Level level, NebulaFrame frame, float intensity) {
        int streakCount = Math.max(1, Math.round(StarRiverTuning.LAUNCH_STREAK_COUNT_PER_INTENSITY * intensity));
        int moteCount = Math.max(1, Math.round(StarRiverTuning.LAUNCH_MOTE_COUNT_PER_INTENSITY * intensity));
        double radiusBlocks = StarRiverTuning.NEBULA_RADIUS_BLOCKS * 0.35;
        for (int streakIndex = 0; streakIndex < streakCount; streakIndex++) {
            Vec3 jitter = gaussianEllipsoidOffset(level, frame, radiusBlocks, radiusBlocks * 0.5);
            Vec3 velocity = frame.look.scale(0.18 + level.random.nextDouble() * 0.10).add(jitter.scale(0.08));
            spawnOne(level, ModParticles.STAR_STREAK.get(), frame.center.add(jitter), velocity);
        }
        for (int moteIndex = 0; moteIndex < moteCount; moteIndex++) {
            Vec3 jitter = gaussianEllipsoidOffset(level, frame, radiusBlocks, radiusBlocks * 0.4);
            Vec3 launchVelocity = frame.look.scale(0.08);
            if (moteIndex % 2 == 0) {
                spawnOne(level, ModParticles.TWIN_DUST.get(), frame.center.add(jitter), launchVelocity);
            } else {
                spawnOne(level, ModParticles.VOID_MOTE.get(), frame.center.add(jitter), launchVelocity);
            }
        }
        Vec3 shardJitter = gaussianEllipsoidOffset(level, frame, radiusBlocks, radiusBlocks * 0.4);
        spawnOne(
                level,
                ModParticles.VIOLET_SHARD.get(),
                frame.center.add(shardJitter),
                frame.look.scale(0.14).add(shardJitter.scale(0.12))
        );
        if (level.random.nextFloat() < 0.55f) {
            spawnOne(level, ModParticles.STAR_CLUSTER.get(), frame.center, Vec3.ZERO);
        }
        if (level.random.nextFloat() < 0.35f) {
            spawnOne(level, ModParticles.VOID_CORE.get(), frame.center, Vec3.ZERO);
        }
    }

    /**
     * 三维高斯采样后裁到椭球内，得到蓬松而不规则的星云体。
     */
    private static Vec3 gaussianEllipsoidOffset(Level level, NebulaFrame frame, double radiusBlocks, double depthBlocks) {
        double alongRight = Mth.clamp(level.random.nextGaussian() * 0.42, -1.15, 1.15);
        double alongUp = Mth.clamp(level.random.nextGaussian() * 0.42, -1.15, 1.15);
        double alongLook = Mth.clamp(level.random.nextGaussian() * 0.28, -0.9, 0.9);
        return frame.diskRight.scale(alongRight * radiusBlocks)
                .add(frame.diskUp.scale(alongUp * radiusBlocks))
                .add(frame.look.scale(alongLook * depthBlocks));
    }

    /**
     * 对数螺线：{@code r = inner + (1-inner) * (e^{k t} - 1) / (e^k - 1)}，再绕游戏时间旋转。
     */
    private static Vec3 logSpiralOffset(
            double theta,
            int armIndex,
            NebulaFrame frame,
            double radiusBlocks,
            long gameTimeTicks
    ) {
        double armPhase = armIndex * Math.PI;
        double spin = gameTimeTicks * StarRiverTuning.SPIRAL_SPIN_RADIANS_PER_TICK;
        double angle = theta + armPhase + spin;
        double span = StarRiverTuning.SPIRAL_THETA_SPAN_RADIANS;
        double t = span <= 1.0e-6 ? 0.0 : theta / span;
        double growth = StarRiverTuning.SPIRAL_GROWTH;
        double radialFraction = StarRiverTuning.SPIRAL_INNER_RADIUS_FRACTION
                + (1.0 - StarRiverTuning.SPIRAL_INNER_RADIUS_FRACTION)
                * ((Math.exp(growth * t) - 1.0) / (Math.exp(growth) - 1.0));
        double radius = radiusBlocks * radialFraction;
        double weave = Math.sin(angle * 2.0) * StarRiverTuning.ARM_DEPTH_WEAVE_BLOCKS;
        return frame.diskRight.scale(Math.cos(angle) * radius)
                .add(frame.diskUp.scale(Math.sin(angle) * radius))
                .add(frame.look.scale(weave));
    }

    private static Vec3 spiralTangent(double theta, int armIndex, NebulaFrame frame, long gameTimeTicks) {
        Vec3 here = logSpiralOffset(theta, armIndex, frame, StarRiverTuning.NEBULA_RADIUS_BLOCKS, gameTimeTicks);
        Vec3 ahead = logSpiralOffset(theta + 0.18, armIndex, frame, StarRiverTuning.NEBULA_RADIUS_BLOCKS, gameTimeTicks);
        Vec3 delta = ahead.subtract(here);
        return delta.lengthSqr() > 1.0e-8 ? delta.normalize() : frame.look;
    }

    /**
     * 沿盘面的 S 曲线：主摆 + 二次谐波，两条暗丝错开相位。
     */
    private static Vec3 filamentOffset(
            double along01,
            int filamentIndex,
            NebulaFrame frame,
            double radiusBlocks,
            long gameTimeTicks
    ) {
        double phase = filamentIndex * 2.15 + gameTimeTicks * 0.04;
        double along = (along01 - 0.5) * 2.0;
        double sway = Math.sin(along * Math.PI + phase) * 0.38;
        double ripple = Math.sin(along * Math.PI * 2.2 + phase * 1.7) * 0.12;
        return frame.diskRight.scale((along * 0.58 + ripple) * radiusBlocks)
                .add(frame.diskUp.scale((sway + along * 0.12) * radiusBlocks))
                .add(frame.look.scale(Math.sin(along * Math.PI + phase) * StarRiverTuning.ARM_DEPTH_WEAVE_BLOCKS));
    }

    /**
     * count=0 且带速度：原版把 delta 当速度，残影沿切线甩出。
     * 速度为零时改 count=1，避免部分环境下 0 个粒子被直接丢掉。
     */
    private static void spawnOne(Level level, ParticleOptions particle, Vec3 position, Vec3 velocity) {
        boolean hasDirectedVelocity = velocity.lengthSqr() > 1.0e-8;
        MagicManager.spawnParticles(
                level,
                particle,
                position.x,
                position.y,
                position.z,
                hasDirectedVelocity ? 0 : 1,
                hasDirectedVelocity ? velocity.x : 0.01,
                hasDirectedVelocity ? velocity.y : 0.01,
                hasDirectedVelocity ? velocity.z : 0.01,
                hasDirectedVelocity ? 1.0 : 0.0,
                false
        );
    }

    /**
     * 星云局部坐标：盘面朝向镜头，锚点在右手前方。
     */
    private record NebulaFrame(Vec3 center, Vec3 look, Vec3 diskRight, Vec3 diskUp) {
    }

    /**
     * 魔法之境环境粒子：在整片圆形法阵内均匀升起多种辉石粒子。
     * <p>
     * 仅客户端调用。采样用 {@code r = R * sqrt(u)} 保证面积均匀（不是只围着外圈）。
     * 种类含 mote / spark / glow / shard / mist，偶发 flare。
     *
     * @param centerX             法阵中心 X
     * @param centerY             法阵贴地高度 Y
     * @param centerZ             法阵中心 Z
     * @param radiusBlocks        法阵半径（方块）
     * @param particleCountBase   密度基准（再乘半径并 clamp）
     * @param fillRadiusFraction  相对半径的铺满比例（1 = 铺到边沿）
     */
    public static void zoneAmbient(
            Level level,
            double centerX,
            double centerY,
            double centerZ,
            float radiusBlocks,
            float particleCountBase,
            float fillRadiusFraction
    ) {
        if (!level.isClientSide) {
            return;
        }
        float clampedRadius = Mth.clamp(radiusBlocks, 0.5f, 32.0f);
        // 整面铺开比单圈更吃粒子，上限略放宽。
        float particleBudget = Mth.clamp(
                particleCountBase * clampedRadius,
                particleCountBase * 0.75f,
                particleCountBase * 10.0f
        );
        int spawnAttempts = Mth.ceil(particleBudget);
        float maxFillRadius = clampedRadius * Mth.clamp(fillRadiusFraction, 0.5f, 1.0f);

        for (int attemptIndex = 0; attemptIndex < spawnAttempts; attemptIndex++) {
            if (particleBudget - attemptIndex < 1.0f && level.random.nextFloat() > (particleBudget - attemptIndex)) {
                return;
            }
            // 圆盘均匀采样：半径取 sqrt，避免中心过稀、外圈过密。
            float theta = level.random.nextFloat() * Mth.TWO_PI;
            float radialDistance = maxFillRadius * Mth.sqrt(level.random.nextFloat());
            double offsetX = Math.cos(theta) * radialDistance;
            double offsetZ = Math.sin(theta) * radialDistance;
            double particleX = centerX + offsetX;
            double particleY = centerY + 0.08 + level.random.nextDouble() * 0.35;
            double particleZ = centerZ + offsetZ;
            double driftX = (level.random.nextDouble() - 0.5) * 0.02;
            double riseSpeed = 0.008 + level.random.nextDouble() * 0.03;
            double driftZ = (level.random.nextDouble() - 0.5) * 0.02;

            int roll = level.random.nextInt(20);
            if (roll < 5) {
                level.addParticle(ModParticles.GLINTSTONE_MOTE.get(), particleX, particleY, particleZ, driftX, riseSpeed, driftZ);
            } else if (roll < 9) {
                level.addParticle(ModParticles.GLINTSTONE_SPARK.get(), particleX, particleY, particleZ, driftX, riseSpeed * 1.25, driftZ);
            } else if (roll < 13) {
                level.addParticle(ModParticles.GLINTSTONE_GLOW.get(), particleX, particleY, particleZ, driftX * 0.5, riseSpeed * 0.55, driftZ * 0.5);
            } else if (roll < 16) {
                level.addParticle(ModParticles.GLINTSTONE_SHARD.get(), particleX, particleY, particleZ, driftX * 1.4, riseSpeed * 0.9, driftZ * 1.4);
            } else if (roll < 19) {
                level.addParticle(ModParticles.GLINTSTONE_MIST.get(), particleX, particleY, particleZ, driftX * 0.3, riseSpeed * 0.35, driftZ * 0.3);
            } else {
                // 偶发绽光，整面点缀高亮
                level.addParticle(ModParticles.GLINTSTONE_FLARE.get(), particleX, particleY, particleZ, 0.0, riseSpeed * 0.2, 0.0);
            }
        }
    }
}

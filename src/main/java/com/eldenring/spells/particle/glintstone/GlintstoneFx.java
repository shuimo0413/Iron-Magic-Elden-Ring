package com.eldenring.spells.particle.glintstone;

import com.eldenring.spells.registry.ModParticles;
import com.eldenring.spells.tuning.GlintstoneTrailTuning;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 辉石学派通用粒子助手。新辉石法术应优先调用这里，保持青蓝晶体视觉一致。
 * <p>
 * {@code intensity}：相对魔砾基准的倍率（点缀密度 / 爆裂数量），建议 0.6–2.5。
 * <p>
 * 飞行光轨主体由客户端几何光束绘制；本类负责弹头后的光晕、薄雾、火花、碎晶与闪星点缀，
 * 以及命中/施法爆裂。
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
     * 飞行拖尾点缀：沿彗星反向飞行方向生成分层粒子，不构成光带主体。
     * <p>
     * 光晕提供柔和能量残影，缩小的薄雾补充体积感，火花/碎晶向外剥落，
     * 闪星则作为低频高亮。生成密度随法术自己的 intensity 与 TrailStyle 递增。
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
        spawnPathParticleSamples(
                level,
                new Vec3(x, y, z),
                motion,
                clampedIntensity,
                scatterRadiusBlocks
        );

        float glowChance = Mth.clamp(
                GlintstoneTrailTuning.PARTICLE_GLOW_BASE_CHANCE
                        + GlintstoneTrailTuning.PARTICLE_GLOW_CHANCE_PER_INTENSITY * clampedIntensity,
                0.0f,
                0.95f
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
                0.45f
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
     * 对本 tick 的真实位移线段做分层采样，使粒子铺在弹道经过的位置，而非只堆在弹头附近。
     * 每个采样点只生成一种粒子，并用概率混合出光晕、火花、闪星、薄雾和碎晶。
     */
    private static void spawnPathParticleSamples(
            Level level,
            Vec3 currentPosition,
            Vec3 motion,
            float clampedIntensity,
            double scatterRadiusBlocks
    ) {
        if (motion.lengthSqr() < 1.0e-8) {
            return;
        }

        int pathParticleCount = Mth.clamp(
                Math.round(
                        GlintstoneTrailTuning.PARTICLE_PATH_BASE_SAMPLE_COUNT
                                + clampedIntensity
                                * GlintstoneTrailTuning.PARTICLE_PATH_SAMPLE_COUNT_PER_INTENSITY
                ),
                1,
                GlintstoneTrailTuning.PARTICLE_PATH_MAXIMUM_SAMPLE_COUNT
        );

        for (int sampleIndex = 0; sampleIndex < pathParticleCount; sampleIndex++) {
            double sampleProgress = (sampleIndex + level.random.nextDouble()) / pathParticleCount;
            Vec3 sampledPathPosition = currentPosition.subtract(motion.scale(sampleProgress));
            Vec3 sampleScatterOffset = Utils.getRandomVec3(scatterRadiusBlocks * 0.9);
            double particleX = sampledPathPosition.x + sampleScatterOffset.x;
            double particleY = sampledPathPosition.y + sampleScatterOffset.y;
            double particleZ = sampledPathPosition.z + sampleScatterOffset.z;
            float particleKindRoll = level.random.nextFloat();

            if (particleKindRoll < 0.34f) {
                float glowSizeScale = Mth.clamp(0.56f + clampedIntensity * 0.10f, 0.60f, 0.95f);
                withParticleSizeScale(glowSizeScale, () -> level.addParticle(
                        ModParticles.GLINTSTONE_GLOW.get(),
                        particleX, particleY, particleZ,
                        -motion.x * 0.015 + sampleScatterOffset.x * 0.05,
                        -motion.y * 0.015 + sampleScatterOffset.y * 0.05,
                        -motion.z * 0.015 + sampleScatterOffset.z * 0.05
                ));
            } else if (particleKindRoll < 0.62f) {
                withParticleSizeScale(0.78f, () -> level.addParticle(
                        ModParticles.GLINTSTONE_SPARK.get(),
                        particleX, particleY, particleZ,
                        -motion.x * 0.035 + sampleScatterOffset.x * 0.30,
                        -motion.y * 0.035 + sampleScatterOffset.y * 0.30,
                        -motion.z * 0.035 + sampleScatterOffset.z * 0.30
                ));
            } else if (particleKindRoll < 0.80f) {
                withParticleSizeScale(0.82f, () -> level.addParticle(
                        ModParticles.GLINTSTONE_MOTE.get(),
                        particleX, particleY, particleZ,
                        sampleScatterOffset.x * 0.12,
                        sampleScatterOffset.y * 0.12,
                        sampleScatterOffset.z * 0.12
                ));
            } else if (particleKindRoll < 0.93f) {
                float mistSizeScale = Mth.clamp(0.24f + clampedIntensity * 0.08f, 0.28f, 0.52f);
                withParticleSizeScale(mistSizeScale, () -> level.addParticle(
                        ModParticles.GLINTSTONE_MIST.get(),
                        particleX, particleY, particleZ,
                        sampleScatterOffset.x * 0.05,
                        0.003 + sampleScatterOffset.y * 0.05,
                        sampleScatterOffset.z * 0.05
                ));
            } else {
                float shardSizeScale = Mth.clamp(0.46f + clampedIntensity * 0.08f, 0.50f, 0.72f);
                withParticleSizeScale(shardSizeScale, () -> level.addParticle(
                        ModParticles.GLINTSTONE_SHARD.get(),
                        particleX, particleY, particleZ,
                        -motion.x * 0.045 + sampleScatterOffset.x * 0.40,
                        -motion.y * 0.045 + sampleScatterOffset.y * 0.40,
                        -motion.z * 0.045 + sampleScatterOffset.z * 0.40
                ));
            }
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
     * 星河：沿视线铺一条旋转螺旋，亮蓝 / 深蓝粉尘与闪星交织。
     * <p>
     * 毁灭流星蓄力与每发流星出手时调用。{@code ruinPalette=true} 时用深蓝粉尘，
     * 否则仍用辉石青闪星点缀。
     *
     * @param origin                 螺旋起点（通常是眼睛前方）
     * @param lookDirection          星河流向
     * @param intensity              密度倍率
     * @param riverLengthBlocks      沿视线铺开的长度（方块）
     * @param riverRadiusBlocks      螺旋半径（方块）
     * @param useRuinPalette         true = 亮蓝与深蓝交织
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
        float clampedIntensity = Mth.clamp(intensity, 0.25f, 3.0f);
        Vec3 forward = lookDirection.lengthSqr() > 1.0e-8 ? lookDirection.normalize() : new Vec3(0.0, 0.0, 1.0);
        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 right = forward.cross(worldUp);
        if (right.lengthSqr() < 1.0e-8) {
            right = new Vec3(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }
        Vec3 helixUp = right.cross(forward).normalize();

        long gameTimeTicks = level.getGameTime();
        int helixSampleCount = Math.max(8, Math.round(14 * clampedIntensity));
        for (int sampleIndex = 0; sampleIndex < helixSampleCount; sampleIndex++) {
            double alongFraction = (sampleIndex + 0.5) / helixSampleCount;
            double alongBlocks = alongFraction * riverLengthBlocks;
            double spinRadians = alongFraction * Math.PI * 4.0 + gameTimeTicks * 0.18;
            double radiusBlocks = riverRadiusBlocks * (0.55 + 0.45 * Math.sin(alongFraction * Math.PI));
            Vec3 radialOffset = right.scale(Math.cos(spinRadians) * radiusBlocks)
                    .add(helixUp.scale(Math.sin(spinRadians) * radiusBlocks));
            Vec3 particlePosition = origin.add(forward.scale(alongBlocks)).add(radialOffset);

            boolean deepBlueSample = (sampleIndex & 1) == 1;
            Vector3f dustColor = useRuinPalette
                    ? (deepBlueSample
                    ? new Vector3f(0.10f, 0.16f, 0.47f)
                    : new Vector3f(0.29f, 0.49f, 1.0f))
                    : (deepBlueSample
                    ? new Vector3f(0.18f, 0.78f, 0.91f)
                    : new Vector3f(0.54f, 0.93f, 1.0f));
            MagicManager.spawnParticles(
                    level,
                    new DustParticleOptions(dustColor, 1.15f),
                    particlePosition.x,
                    particlePosition.y,
                    particlePosition.z,
                    1,
                    0.02,
                    0.02,
                    0.02,
                    0.0,
                    false
            );

            if (sampleIndex % 2 == 0) {
                MagicManager.spawnParticles(
                        level, ModParticles.GLINTSTONE_MOTE.get(),
                        particlePosition.x, particlePosition.y, particlePosition.z,
                        1, 0.04, 0.04, 0.04,
                        0.01, false
                );
            }
            if (sampleIndex % 3 == 0) {
                MagicManager.spawnParticles(
                        level, ModParticles.GLINTSTONE_GLOW.get(),
                        particlePosition.x, particlePosition.y, particlePosition.z,
                        1, 0.06, 0.06, 0.06,
                        0.01, false
                );
            }
        }

        MagicManager.spawnParticles(
                level, ModParticles.GLINTSTONE_MIST.get(),
                origin.x, origin.y, origin.z,
                Math.round(3 * clampedIntensity),
                riverRadiusBlocks * 0.45, riverRadiusBlocks * 0.35, riverRadiusBlocks * 0.45,
                0.02, false
        );
    }

    /**
     * 施法者周身星河环：蓄力时每 tick 在胸口/杖头附近转一圈星尘。
     */
    public static void starRiverOrbit(Level level, LivingEntity caster, float intensity, boolean useRuinPalette) {
        Vec3 chestPosition = caster.position().add(0.0, caster.getBbHeight() * 0.62, 0.0);
        Vec3 lookDirection = caster.getLookAngle();
        Vec3 orbitOrigin = chestPosition.add(lookDirection.scale(0.15));
        starRiver(
                level,
                orbitOrigin,
                lookDirection,
                intensity,
                1.8,
                0.70,
                useRuinPalette
        );
    }
}

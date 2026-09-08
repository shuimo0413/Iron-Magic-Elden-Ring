package com.eldenring.spells.particle.gravity;

import com.eldenring.spells.particle.carian.CarianLogSpiral;
import com.eldenring.spells.registry.ModParticles;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 重力学派通用粒子助手。新重力法术应优先调用这里，保持黑核 + 紫晕视觉一致。
 * <p>
 * {@code intensity}：相对重力球基准的倍率（点缀密度 / 爆裂数量），建议 0.6–2.5。
 * 对数螺旋漩涡走 {@link #logSpiralVortex}：粒子沿 {@code r = e^{C-θ}} 从外吸进球心。
 */
public final class GravityFx {

    /**
     * 客户端 {@code addParticle} → Provider 同步调用链上的尺寸倍率提示。
     * SimpleParticleType 无法传参，故用 ThreadLocal；仅客户端粒子构造读取。
     */
    private static final ThreadLocal<Float> CLIENT_PARTICLE_SIZE_SCALE = ThreadLocal.withInitial(() -> 1.0f);

    /**
     * 绕盘面均布的螺线相位条数。叠在一起外轮廓接近圆盘。
     */
    private static final int LOG_SPIRAL_CURVE_PHASE_COUNT = 8;

    /**
     * 嵌套等值线的外半径相对盘半径。
     */
    private static final double[] LOG_SPIRAL_ISOLINE_RADIUS_FRACTIONS = {1.00, 0.78, 0.58, 0.40};

    /**
     * 本 tick 沿一条曲线取几个点。
     */
    private static final int LOG_SPIRAL_SAMPLES_PER_CURVE = 5;

    /**
     * 珠子沿 {@code θ} 从盘边走到涡眼的速度（圈 / tick）。
     * 调小 → 从外往里爬得更慢、更好读「被吸进去」。
     */
    private static final double LOG_SPIRAL_PATH_FLOW_TURNS_PER_TICK = 0.028;

    /**
     * 整盘缓转（弧度 / tick）。
     */
    private static final double LOG_SPIRAL_PATH_SPIN_RADIANS_PER_TICK = 0.08;

    private GravityFx() {
    }

    /**
     * 供重力粒子构造读取：当前生成点期望的尺寸倍率（1 = 默认）。
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

    /**
     * 重力球飞行点缀：光晕 / 雾 / 电弧 / 闪点 / 残影，密度随 intensity 放大。
     */
    public static void trailAccents(
            Level level,
            double x,
            double y,
            double z,
            Vec3 motion,
            float intensity
    ) {
        float clampedIntensity = Mth.clamp(intensity, 0.25f, 3.0f);
        Vec3 normalizedFlightDirection = motion.lengthSqr() > 1.0e-8
                ? motion.normalize()
                : Vec3.ZERO;
        double scatterRadiusBlocks = 0.10 + 0.05 * clampedIntensity;

        if (level.random.nextFloat() < 0.28f * clampedIntensity) {
            Vec3 glowOffset = Utils.getRandomVec3(scatterRadiusBlocks * 0.7);
            withParticleSizeScale(Mth.clamp(0.55f + clampedIntensity * 0.14f, 0.50f, 1.20f), () -> level.addParticle(
                    ModParticles.GRAVITY_GLOW.get(),
                    x + glowOffset.x, y + glowOffset.y, z + glowOffset.z,
                    -normalizedFlightDirection.x * 0.02 + glowOffset.x * 0.05,
                    -normalizedFlightDirection.y * 0.02 + glowOffset.y * 0.05,
                    -normalizedFlightDirection.z * 0.02 + glowOffset.z * 0.05
            ));
        }
        if (level.random.nextFloat() < 0.24f * clampedIntensity) {
            Vec3 mistOffset = Utils.getRandomVec3(scatterRadiusBlocks);
            withParticleSizeScale(0.9f, () -> level.addParticle(
                    ModParticles.GRAVITY_MIST.get(),
                    x + mistOffset.x, y + mistOffset.y, z + mistOffset.z,
                    -normalizedFlightDirection.x * 0.03 + mistOffset.x * 0.08,
                    -normalizedFlightDirection.y * 0.03 + mistOffset.y * 0.08,
                    -normalizedFlightDirection.z * 0.03 + mistOffset.z * 0.08
            ));
        }
        if (level.random.nextFloat() < 0.16f * clampedIntensity) {
            Vec3 sparkOffset = Utils.getRandomVec3(scatterRadiusBlocks);
            withParticleSizeScale(0.85f, () -> level.addParticle(
                    ModParticles.GRAVITY_SPARK.get(),
                    x + sparkOffset.x, y + sparkOffset.y, z + sparkOffset.z,
                    -motion.x * 0.04 + sparkOffset.x * 0.22,
                    -motion.y * 0.04 + sparkOffset.y * 0.22,
                    -motion.z * 0.04 + sparkOffset.z * 0.22
            ));
        }
        if (level.random.nextFloat() < 0.18f * clampedIntensity) {
            Vec3 moteOffset = Utils.getRandomVec3(scatterRadiusBlocks * 0.85);
            // 速度指向球心：看起来被吸进去
            withParticleSizeScale(0.9f, () -> level.addParticle(
                    ModParticles.GRAVITY_MOTE.get(),
                    x + moteOffset.x, y + moteOffset.y, z + moteOffset.z,
                    -moteOffset.x * 0.35, -moteOffset.y * 0.35, -moteOffset.z * 0.35
            ));
        }
        if (level.random.nextFloat() < 0.12f * clampedIntensity) {
            Vec3 streakOffset = Utils.getRandomVec3(scatterRadiusBlocks * 0.5);
            level.addParticle(
                    ModParticles.GRAVITY_STREAK.get(),
                    x + streakOffset.x, y + streakOffset.y, z + streakOffset.z,
                    -normalizedFlightDirection.x * 0.08,
                    -normalizedFlightDirection.y * 0.08,
                    -normalizedFlightDirection.z * 0.08
            );
        }
    }

    /**
     * 命中爆裂：蚀环 + 脉冲环 + 雾团 + 电弧飞溅 + 黑核闪一下。
     * <p>
     * 只走服务端 {@link MagicManager}（会同步到附近客户端）。
     * 客户端直接调会因 {@code Level.getServer() == null} 空指针。
     */
    public static void impact(Level level, double x, double y, double z, float intensity) {
        if (level.isClientSide || level.getServer() == null) {
            return;
        }
        float clampedIntensity = Mth.clamp(intensity, 0.25f, 3.5f);
        double fieldRadiusBlocks = 0.42 * clampedIntensity;
        double smokeRadiusBlocks = 0.55 * clampedIntensity;

        MagicManager.spawnParticles(
                level, ModParticles.GRAVITY_CORE.get(), x, y, z,
                Math.max(1, Math.round(2 * clampedIntensity)), 0.03, 0.03, 0.03,
                0.01, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.GRAVITY_ECLIPSE.get(), x, y, z,
                1, 0.02, 0.02, 0.02, 0.0, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.GRAVITY_RING.get(), x, y, z,
                1, 0.02, 0.02, 0.02, 0.0, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.GRAVITY_VORTEX.get(), x, y, z,
                Math.max(1, Math.round(2 * clampedIntensity)), 0.04, 0.04, 0.04,
                0.02, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.GRAVITY_GLOW.get(), x, y, z,
                Math.round(10 * clampedIntensity), fieldRadiusBlocks, fieldRadiusBlocks, fieldRadiusBlocks,
                0.12 * clampedIntensity, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.GRAVITY_MIST.get(), x, y, z,
                Math.round(12 * clampedIntensity), smokeRadiusBlocks, smokeRadiusBlocks * 0.85, smokeRadiusBlocks,
                0.07 * clampedIntensity, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.GRAVITY_SPARK.get(), x, y, z,
                Math.round(14 * clampedIntensity), 0.16 * clampedIntensity, 0.16 * clampedIntensity, 0.16 * clampedIntensity,
                0.30 * clampedIntensity, true
        );
        MagicManager.spawnParticles(
                level, ModParticles.GRAVITY_MOTE.get(), x, y, z,
                Math.round(6 * clampedIntensity), 0.20 * clampedIntensity, 0.20 * clampedIntensity, 0.20 * clampedIntensity,
                0.14 * clampedIntensity, false
        );
    }

    /**
     * 对数螺旋漩涡：粒子从外沿盘面吸进球心。仅客户端。
     */
    public static void logSpiralVortex(Level level, Vec3 center, Vec3 facing) {
        logSpiralVortex(level, center, facing, 1.0f);
    }

    /**
     * 同 {@link #logSpiralVortex(Level, Vec3, Vec3)}，用世界时间当动画相位。
     */
    public static void logSpiralVortex(Level level, Vec3 center, Vec3 facing, float intensity) {
        logSpiralVortex(level, center, facing, intensity, (int) (level.getGameTime() & 0x7FFF));
    }

    /**
     * 同 {@link #logSpiralVortex(Level, Vec3, Vec3, float)}，{@code animationAgeTicks} 驱动内流与整盘旋转。
     */
    public static void logSpiralVortex(
            Level level,
            Vec3 center,
            Vec3 facing,
            float intensity,
            int animationAgeTicks
    ) {
        if (!level.isClientSide) {
            return;
        }
        float clampedIntensity = Mth.clamp(intensity, 0.25f, 3.0f);
        CarianLogSpiral.PlaneFrame frame = CarianLogSpiral.PlaneFrame.facing(center, facing);
        drawLogSpiralPath(level, frame, clampedIntensity, animationAgeTicks);

        if (level.random.nextFloat() < 0.35f * clampedIntensity) {
            withParticleSizeScale(0.85f, () -> level.addParticle(
                    ModParticles.GRAVITY_CORE.get(),
                    center.x, center.y, center.z,
                    0.0, 0.0, 0.0
            ));
        }
        if (level.random.nextFloat() < 0.25f * clampedIntensity) {
            withParticleSizeScale(1.05f, () -> level.addParticle(
                    ModParticles.GRAVITY_ECLIPSE.get(),
                    center.x, center.y, center.z,
                    0.0, 0.0, 0.0
            ));
        }
    }

    /**
     * 单颗带速度粒子。速度为零时改 count=1，避免被丢掉。
     * 仅服务端；客户端 {@code getServer()} 为空时直接返回。
     */
    public static void spawnOne(Level level, ParticleOptions particle, Vec3 position, Vec3 velocity) {
        if (level.isClientSide || level.getServer() == null) {
            return;
        }
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

    private static void drawLogSpiralPath(
            Level level,
            CarianLogSpiral.PlaneFrame frame,
            float intensity,
            int animationAgeTicks
    ) {
        double discRadiusBlocks = CarianLogSpiral.VORTEX_RADIUS_BLOCKS;
        double innerRadiusBlocks = CarianLogSpiral.PATH_MIN_DRAW_RADIUS_BLOCKS;
        double spinRadians = animationAgeTicks * LOG_SPIRAL_PATH_SPIN_RADIANS_PER_TICK;
        int phaseCount = Math.max(6, Math.round(LOG_SPIRAL_CURVE_PHASE_COUNT * intensity));
        int nestedCurveCount = LOG_SPIRAL_ISOLINE_RADIUS_FRACTIONS.length;

        for (int phaseIndex = 0; phaseIndex < phaseCount; phaseIndex++) {
            double curvePhaseRadians = (Math.PI * 2.0) * phaseIndex / phaseCount;
            for (int nestedIndex = 0; nestedIndex < nestedCurveCount; nestedIndex++) {
                double outerRadiusBlocks = discRadiusBlocks * LOG_SPIRAL_ISOLINE_RADIUS_FRACTIONS[nestedIndex];
                double isolineC = CarianLogSpiral.isolineCForOuterRadius(outerRadiusBlocks, 0.0);
                double thetaSpanRadians = CarianLogSpiral.thetaSpanRadians(outerRadiusBlocks, innerRadiusBlocks);
                if (thetaSpanRadians <= 1.0e-8) {
                    continue;
                }
                for (int sampleIndex = 0; sampleIndex < LOG_SPIRAL_SAMPLES_PER_CURVE; sampleIndex++) {
                    double wrappedProgress = wrap01(
                            (sampleIndex + 0.5) / LOG_SPIRAL_SAMPLES_PER_CURVE
                                    + animationAgeTicks * LOG_SPIRAL_PATH_FLOW_TURNS_PER_TICK
                                    + phaseIndex * 0.07
                                    + nestedIndex * 0.11
                    );
                    double thetaRadians = wrappedProgress * thetaSpanRadians;
                    double radiusBlocks = CarianLogSpiral.isolineRadiusBlocks(isolineC, thetaRadians);
                    if (radiusBlocks < innerRadiusBlocks || radiusBlocks > outerRadiusBlocks + 1.0e-6) {
                        continue;
                    }
                    Vec3 pathPosition = CarianLogSpiral.pathWorldPosition(
                            frame,
                            isolineC,
                            thetaRadians,
                            curvePhaseRadians,
                            spinRadians
                    );
                    boolean nearCore = wrappedProgress > 0.72;
                    boolean onOuterRim = nestedIndex == 0 && wrappedProgress < 0.18;
                    ParticleOptions pathParticle = nearCore
                            ? ModParticles.GRAVITY_SPARK.get()
                            : (onOuterRim ? ModParticles.GRAVITY_GLOW.get() : ModParticles.GRAVITY_MOTE.get());
                    float sizeScale = onOuterRim ? 0.70f : (nearCore ? 0.48f : 0.60f);
                    withParticleSizeScale(sizeScale, () -> level.addParticle(
                            pathParticle,
                            pathPosition.x,
                            pathPosition.y,
                            pathPosition.z,
                            0.0,
                            0.0,
                            0.0
                    ));
                }
            }
        }
    }

    private static double wrap01(double value) {
        double wrapped = value - Math.floor(value);
        return wrapped < 0.0 ? wrapped + 1.0 : wrapped;
    }
}

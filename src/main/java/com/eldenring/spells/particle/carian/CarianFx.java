package com.eldenring.spells.particle.carian;

import com.eldenring.spells.registry.ModParticles;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 卡利亚学派通用粒子助手。新卡利亚法术应优先调用这里，保持深蓝辉剑视觉一致。
 * <p>
 * {@code intensity}：相对迅剑基准的倍率（点缀密度 / 爆裂数量），建议 0.6–2.5。
 * 贴图与着色走 {@link CarianParticle}，不要再套青辉石库。
 * 对数螺旋漩涡走 {@link #logSpiralVortex}：多条不同相位 / 半径的 {@code r = e^{C-θ}} 叠成接近圆的盘。
 */
public final class CarianFx {

    /**
     * 客户端 {@code addParticle} → Provider 同步调用链上的尺寸倍率提示。
     * SimpleParticleType 无法传参，故用 ThreadLocal；仅客户端粒子构造读取。
     */
    private static final ThreadLocal<Float> CLIENT_PARTICLE_SIZE_SCALE = ThreadLocal.withInitial(() -> 1.0f);

    /**
     * 绕盘面均布的螺线相位条数。两条臂看起来像 S；十条以上叠在一起就接近圆盘。
     */
    private static final int LOG_SPIRAL_CURVE_PHASE_COUNT = 10;

    /**
     * 嵌套等值线的外半径相对盘半径。不同 {@code C} 的曲线叠层，外圈更圆、里圈更密。
     */
    private static final double[] LOG_SPIRAL_ISOLINE_RADIUS_FRACTIONS = {1.00, 0.82, 0.64, 0.48, 0.34};

    /**
     * 本 tick 沿一条曲线取几个点。粒子寿命会把相邻 tick 连成线；调大 → 单臂更实、更吃粒子预算。
     */
    private static final int LOG_SPIRAL_SAMPLES_PER_CURVE = 6;

    /**
     * 珠子沿 {@code θ} 从盘边走到涡眼的速度（圈 / tick）。
     * 调小 → 从外往里爬得更慢、更好读。
     */
    private static final double LOG_SPIRAL_PATH_FLOW_TURNS_PER_TICK = 0.024;

    /**
     * 整盘缓转（弧度 / tick）。只带动画旋转，不改变从外到内的流向。
     */
    private static final double LOG_SPIRAL_PATH_SPIN_RADIANS_PER_TICK = 0.07;

    private CarianFx() {
    }

    /**
     * 卡利亚对数螺旋漩涡（直径 1 格）：多条不同相位、不同半径的 {@code r = e^{C-θ}} 从外往里流。
     * 叠在一起外轮廓接近圆；不要走亚兹勒那种专用汇聚粒子 / 贴图圆盘。
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
     * 同 {@link #logSpiralVortex(Level, Vec3, Vec3, float)}，{@code animationAgeTicks} 驱动珠子沿螺线内流和整盘旋转。
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
    }

    /**
     * 供卡利亚粒子构造读取：当前生成点期望的尺寸倍率（1 = 默认）。
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
     * 飞行 / 刃尖点缀：光晕 / 火花 / 碎晶 / 闪星 / 新月，密度随 intensity 放大。
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
        double scatterRadiusBlocks = 0.08 + 0.04 * clampedIntensity;

        if (level.random.nextFloat() < 0.22f * clampedIntensity) {
            Vec3 glowOffset = Utils.getRandomVec3(scatterRadiusBlocks * 0.75);
            withParticleSizeScale(Mth.clamp(0.50f + clampedIntensity * 0.14f, 0.50f, 1.15f), () -> level.addParticle(
                    ModParticles.CARIAN_GLOW.get(),
                    x + glowOffset.x, y + glowOffset.y, z + glowOffset.z,
                    -normalizedFlightDirection.x * 0.018 + glowOffset.x * 0.06,
                    -normalizedFlightDirection.y * 0.018 + glowOffset.y * 0.06,
                    -normalizedFlightDirection.z * 0.018 + glowOffset.z * 0.06
            ));
        }
        if (level.random.nextFloat() < 0.18f * clampedIntensity) {
            Vec3 sparkOffset = Utils.getRandomVec3(scatterRadiusBlocks);
            withParticleSizeScale(0.85f, () -> level.addParticle(
                    ModParticles.CARIAN_SPARK.get(),
                    x + sparkOffset.x, y + sparkOffset.y, z + sparkOffset.z,
                    -motion.x * 0.03 + sparkOffset.x * 0.25,
                    -motion.y * 0.03 + sparkOffset.y * 0.25,
                    -motion.z * 0.03 + sparkOffset.z * 0.25
            ));
        }
        if (level.random.nextFloat() < 0.14f * clampedIntensity) {
            Vec3 shardOffset = Utils.getRandomVec3(scatterRadiusBlocks * 1.15);
            withParticleSizeScale(Mth.clamp(0.48f + clampedIntensity * 0.10f, 0.50f, 0.82f), () -> level.addParticle(
                    ModParticles.CARIAN_SHARD.get(),
                    x + shardOffset.x, y + shardOffset.y, z + shardOffset.z,
                    -normalizedFlightDirection.x * 0.045 + shardOffset.x * 0.45,
                    -normalizedFlightDirection.y * 0.045 + shardOffset.y * 0.45,
                    -normalizedFlightDirection.z * 0.045 + shardOffset.z * 0.45
            ));
        }
        if (level.random.nextFloat() < 0.16f * clampedIntensity) {
            Vec3 moteOffset = Utils.getRandomVec3(scatterRadiusBlocks * 0.8);
            withParticleSizeScale(0.9f, () -> level.addParticle(
                    ModParticles.CARIAN_MOTE.get(),
                    x + moteOffset.x, y + moteOffset.y, z + moteOffset.z,
                    moteOffset.x * 0.15, moteOffset.y * 0.15, moteOffset.z * 0.15
            ));
        }
        if (level.random.nextFloat() < 0.12f * clampedIntensity) {
            Vec3 crescentOffset = Utils.getRandomVec3(scatterRadiusBlocks);
            level.addParticle(
                    ModParticles.CARIAN_CRESCENT.get(),
                    x + crescentOffset.x, y + crescentOffset.y, z + crescentOffset.z,
                    crescentOffset.x * 0.04, crescentOffset.y * 0.03, crescentOffset.z * 0.04
            );
        }
    }

    /**
     * 命中爆裂：绽光 + 光晕场 + 雾团 + 晶体飞溅 + 新月点缀。
     */
    public static void impact(Level level, double x, double y, double z, float intensity) {
        float clampedIntensity = Mth.clamp(intensity, 0.25f, 3.5f);
        double fieldRadiusBlocks = 0.40 * clampedIntensity;
        double smokeRadiusBlocks = 0.52 * clampedIntensity;

        MagicManager.spawnParticles(
                level, ModParticles.CARIAN_FLARE.get(), x, y, z,
                Math.max(2, Math.round(3 * clampedIntensity)), 0.04, 0.04, 0.04,
                0.02, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.CARIAN_RING.get(), x, y, z,
                1, 0.02, 0.02, 0.02, 0.0, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.CARIAN_GLOW.get(), x, y, z,
                Math.round(10 * clampedIntensity), fieldRadiusBlocks, fieldRadiusBlocks, fieldRadiusBlocks,
                0.12 * clampedIntensity, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.CARIAN_MIST.get(), x, y, z,
                Math.round(12 * clampedIntensity), smokeRadiusBlocks, smokeRadiusBlocks * 0.85, smokeRadiusBlocks,
                0.07 * clampedIntensity, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.CARIAN_SPARK.get(), x, y, z,
                Math.round(14 * clampedIntensity), 0.16 * clampedIntensity, 0.16 * clampedIntensity, 0.16 * clampedIntensity,
                0.30 * clampedIntensity, true
        );
        MagicManager.spawnParticles(
                level, ModParticles.CARIAN_SHARD.get(), x, y, z,
                Math.round(8 * clampedIntensity), 0.13 * clampedIntensity, 0.13 * clampedIntensity, 0.13 * clampedIntensity,
                0.34 * clampedIntensity, true
        );
        MagicManager.spawnParticles(
                level, ModParticles.CARIAN_MOTE.get(), x, y, z,
                Math.round(5 * clampedIntensity), 0.20 * clampedIntensity, 0.20 * clampedIntensity, 0.20 * clampedIntensity,
                0.14 * clampedIntensity, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.CARIAN_CRESCENT.get(), x, y, z,
                Math.round(3 * clampedIntensity), 0.10 * clampedIntensity, 0.10 * clampedIntensity, 0.10 * clampedIntensity,
                0.08 * clampedIntensity, false
        );
        if (clampedIntensity >= 1.4f) {
            MagicManager.spawnParticles(
                    level, ModParticles.CARIAN_NOVA.get(), x, y, z,
                    Math.round(2 * clampedIntensity), 0.08, 0.08, 0.08,
                    0.06, false
            );
        }
    }

    /**
     * 施法瞬间小爆：一颗绽光 + 少量火花 / 碎晶 / 雾。
     */
    public static void castBurst(Level level, double x, double y, double z, float intensity) {
        float clampedIntensity = Mth.clamp(intensity, 0.25f, 3.0f);
        MagicManager.spawnParticles(
                level, ModParticles.CARIAN_FLARE.get(), x, y, z,
                1, 0.01, 0.01, 0.01, 0.0, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.CARIAN_SPARK.get(), x, y, z,
                Math.round(5 * clampedIntensity), 0.08 * clampedIntensity, 0.08 * clampedIntensity, 0.08 * clampedIntensity,
                0.15 * clampedIntensity, true
        );
        MagicManager.spawnParticles(
                level, ModParticles.CARIAN_SHARD.get(), x, y, z,
                Math.round(2 * clampedIntensity), 0.06 * clampedIntensity, 0.06 * clampedIntensity, 0.06 * clampedIntensity,
                0.12 * clampedIntensity, true
        );
        MagicManager.spawnParticles(
                level, ModParticles.CARIAN_GLOW.get(), x, y, z,
                Math.round(2 * clampedIntensity), 0.10 * clampedIntensity, 0.10 * clampedIntensity, 0.10 * clampedIntensity,
                0.05 * clampedIntensity, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.CARIAN_MIST.get(), x, y, z,
                Math.round(2 * clampedIntensity), 0.12 * clampedIntensity, 0.12 * clampedIntensity, 0.12 * clampedIntensity,
                0.04 * clampedIntensity, false
        );
        MagicManager.spawnParticles(
                level, ModParticles.CARIAN_GLINT.get(), x, y, z,
                Math.round(3 * clampedIntensity), 0.08 * clampedIntensity, 0.08 * clampedIntensity, 0.08 * clampedIntensity,
                0.06 * clampedIntensity, false
        );
    }

    /**
     * 单颗带速度粒子。速度为零时改 count=1，避免被丢掉。
     */
    public static void spawnOne(Level level, ParticleOptions particle, Vec3 position, Vec3 velocity) {
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
     * 本 tick 用多条不同相位、不同半径的对数螺线铺漩涡。
     * 每条都沿 {@code r = e^{C-θ}} 从外往里流；叠在一起外轮廓接近圆，而不是两根细 S 臂。
     */
    private static void drawLogSpiralPath(
            Level level,
            CarianLogSpiral.PlaneFrame frame,
            float intensity,
            int animationAgeTicks
    ) {
        double discRadiusBlocks = CarianLogSpiral.VORTEX_RADIUS_BLOCKS;
        double innerRadiusBlocks = CarianLogSpiral.PATH_MIN_DRAW_RADIUS_BLOCKS;
        double spinRadians = animationAgeTicks * LOG_SPIRAL_PATH_SPIN_RADIANS_PER_TICK;
        int phaseCount = Math.max(8, Math.round(LOG_SPIRAL_CURVE_PHASE_COUNT * intensity));
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
                    // 各相位错开一点，避免十根曲线同步跳动成一个刚体风车。
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
                    ParticleOptions pathParticle = nearCore || onOuterRim
                            ? ModParticles.CARIAN_GLINT.get()
                            : ModParticles.CARIAN_MOTE.get();
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

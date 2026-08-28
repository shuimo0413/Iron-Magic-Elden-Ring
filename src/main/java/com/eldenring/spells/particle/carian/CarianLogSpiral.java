package com.eldenring.spells.particle.carian;

import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * 复对数螺旋场，用来生成卡利亚漩涡纹理。
 * <p>
 * 把盘面点看成复变量 {@code z = x + iy}，取对数：
 * {@code ln z = ln r + i θ}，其中 {@code r = √(x²+y²)}，{@code θ = atan2(y, x)}。
 * 把径向对数与极角组合成螺旋标量场（默认缠绕系数 {@code k = 1}）：
 * <pre>
 *   f(r, θ) = ln r + k · θ
 * </pre>
 * 等值线 {@code f = C} 满足 {@code r = e^{C - kθ}}，即对数螺旋。
 * {@code k} 越大，同样转角内半径收得越快、臂越紧。
 */
public final class CarianLogSpiral {

    /**
     * 缠绕系数 {@code k}（无量纲）。{@code k = 1} 时等值线就是 {@code r = e^{C-θ}}。
     * 调大 → 同样圈数内更快收到核里；调小 → 臂更开、更像缓旋星系。
     */
    public static final double SPIRAL_K = 1.0;

    /**
     * 默认漩涡直径（方块）。整盘视觉尺度固定 1 格。
     */
    public static final double VORTEX_DIAMETER_BLOCKS = 1.0;

    /**
     * 默认漩涡盘面半径（方块）。{@link #VORTEX_DIAMETER_BLOCKS} 的一半；所有默认采样都裁在这个圆里。
     */
    public static final double VORTEX_RADIUS_BLOCKS = VORTEX_DIAMETER_BLOCKS * 0.5;

    /**
     * 路径极角跨度（弧度）上限。实际绘制用 {@link #thetaSpanRadians} 按可见内外半径裁切，
     * 避免 {@code k=1} 转满 2π 时外圈点数太少、看起来像从核往外喷。
     */
    public static final double PATH_THETA_SPAN_RADIANS = Math.PI * 2.0;

    /**
     * 路径上比这更靠核的点不再单独铺粒，改由中心一颗闪星表示涡眼，避免全堆在原点。
     */
    public static final double PATH_MIN_DRAW_RADIUS_BLOCKS = 0.018;

    /**
     * {@code ln r} 的安全下限（方块）。小于这个值时场函数直接当成负无穷，避免 NaN。
     */
    private static final double FIELD_MIN_RADIUS_BLOCKS = 1.0e-8;

    private CarianLogSpiral() {
    }

    /**
     * 螺旋标量场 {@code f(x, y) = ln r + k · atan2(y, x)}。
     * 原点附近返回 {@link Double#NEGATIVE_INFINITY}。
     *
     * @param localXBlocks 盘面局部 X（方块），沿 {@link PlaneFrame#rightAxis}
     * @param localYBlocks 盘面局部 Y（方块），沿 {@link PlaneFrame#upAxis}
     */
    public static double field(double localXBlocks, double localYBlocks) {
        return field(localXBlocks, localYBlocks, SPIRAL_K);
    }

    /**
     * 同 {@link #field(double, double)}，可指定缠绕系数 {@code spiralK}。
     */
    public static double field(double localXBlocks, double localYBlocks, double spiralK) {
        double radiusBlocks = Math.hypot(localXBlocks, localYBlocks);
        if (radiusBlocks < FIELD_MIN_RADIUS_BLOCKS) {
            return Double.NEGATIVE_INFINITY;
        }
        return Math.log(radiusBlocks) + spiralK * Math.atan2(localYBlocks, localXBlocks);
    }

    /**
     * 等值线 {@code f = isolineC} 在极角 {@code θ} 处的半径：{@code r = exp(C - kθ)}。
     *
     * @param isolineC     等值线常数（无量纲，与 {@code ln r} 同量纲）
     * @param thetaRadians 极角（弧度），与 {@code atan2(y, x)} 同约定
     * @return 半径（方块）；数值上可为任意正数，调用方负责裁到盘面内
     */
    public static double isolineRadiusBlocks(double isolineC, double thetaRadians) {
        return isolineRadiusBlocks(isolineC, thetaRadians, SPIRAL_K);
    }

    /**
     * 同 {@link #isolineRadiusBlocks(double, double)}，可指定 {@code spiralK}。
     */
    public static double isolineRadiusBlocks(double isolineC, double thetaRadians, double spiralK) {
        return Math.exp(isolineC - spiralK * thetaRadians);
    }

    /**
     * 让等值线在 {@code θ = thetaStartRadians} 处刚好经过 {@code outerRadiusBlocks} 的 {@code C}。
     * {@code θ} 从该起点增大时半径按 {@code e^{-k Δθ}} 收进核里。
     */
    public static double isolineCForOuterRadius(double outerRadiusBlocks, double thetaStartRadians) {
        return isolineCForOuterRadius(outerRadiusBlocks, thetaStartRadians, SPIRAL_K);
    }

    /**
     * 同 {@link #isolineCForOuterRadius(double, double)}，可指定 {@code spiralK}。
     */
    public static double isolineCForOuterRadius(
            double outerRadiusBlocks,
            double thetaStartRadians,
            double spiralK
    ) {
        double clampedOuterRadius = Math.max(outerRadiusBlocks, FIELD_MIN_RADIUS_BLOCKS);
        return Math.log(clampedOuterRadius) + spiralK * thetaStartRadians;
    }

    /**
     * 从外半径收到内半径需要转过的极角：{@code Δθ = ln(r外 / r内) / k}。
     */
    public static double thetaSpanRadians(double outerRadiusBlocks, double innerRadiusBlocks) {
        return thetaSpanRadians(outerRadiusBlocks, innerRadiusBlocks, SPIRAL_K);
    }

    /**
     * 同 {@link #thetaSpanRadians(double, double)}，可指定 {@code spiralK}。
     */
    public static double thetaSpanRadians(double outerRadiusBlocks, double innerRadiusBlocks, double spiralK) {
        double safeOuter = Math.max(outerRadiusBlocks, FIELD_MIN_RADIUS_BLOCKS);
        double safeInner = Math.max(innerRadiusBlocks, FIELD_MIN_RADIUS_BLOCKS);
        if (safeOuter <= safeInner || Math.abs(spiralK) < 1.0e-8) {
            return 0.0;
        }
        return Math.log(safeOuter / safeInner) / spiralK;
    }

    /**
     * 路径上一点：{@code r = exp(C - kθ)}，再绕臂相位和自旋映到世界坐标。
     * 这就是用函数取点、交给粒子去画的那条对数螺线。
     */
    public static Vec3 pathWorldPosition(
            PlaneFrame frame,
            double isolineC,
            double thetaRadians,
            double armPhaseRadians,
            double spinRadians
    ) {
        double radiusBlocks = isolineRadiusBlocks(isolineC, thetaRadians);
        double planeAngleRadians = thetaRadians + armPhaseRadians + spinRadians;
        return frame.toWorldPolar(radiusBlocks, planeAngleRadians);
    }

    /**
     * 沿一条对数螺线等值线均匀采 {@code sampleCount} 个点（对 {@code θ} 均匀，核更密、外臂更疏）。
     *
     * @param frame              盘面标架
     * @param outerRadiusBlocks  这条臂最外圈半径（方块）
     * @param innerRadiusBlocks  收到这里就停（方块）
     * @param armPhaseRadians    臂相位；双臂漩涡用 {@code 0} 与 {@code π}
     * @param sampleCount        采样点数，至少 2
     * @param spinRadians        整盘额外旋转（弧度），用来让连续调用看起来在转
     * @param consumer           每个采样点回调一次
     */
    public static void forEachIsolineSample(
            PlaneFrame frame,
            double outerRadiusBlocks,
            double innerRadiusBlocks,
            double armPhaseRadians,
            int sampleCount,
            double spinRadians,
            Consumer<IsolineSample> consumer
    ) {
        int clampedSampleCount = Math.max(2, sampleCount);
        double thetaStartRadians = 0.0;
        double isolineC = isolineCForOuterRadius(outerRadiusBlocks, thetaStartRadians);
        double thetaMaxRadians = thetaSpanRadians(outerRadiusBlocks, innerRadiusBlocks);
        if (thetaMaxRadians <= 1.0e-8) {
            return;
        }
        for (int sampleIndex = 0; sampleIndex < clampedSampleCount; sampleIndex++) {
            double sampleFraction = sampleIndex / (double) (clampedSampleCount - 1);
            double thetaRadians = thetaStartRadians + sampleFraction * thetaMaxRadians;
            IsolineSample sample = sampleIsoline(
                    frame,
                    isolineC,
                    thetaRadians,
                    armPhaseRadians,
                    spinRadians
            );
            if (sample.radiusBlocks() < innerRadiusBlocks) {
                continue;
            }
            consumer.accept(sample);
        }
    }

    /**
     * 等值线上一个点：位置在盘面内，切向指向 {@code θ} 增大、半径减小的方向（旋进核里）。
     */
    public static IsolineSample sampleIsoline(
            PlaneFrame frame,
            double isolineC,
            double thetaRadians,
            double armPhaseRadians,
            double spinRadians
    ) {
        double radiusBlocks = isolineRadiusBlocks(isolineC, thetaRadians);
        double planeAngleRadians = thetaRadians + armPhaseRadians + spinRadians;
        double cosineAngle = Math.cos(planeAngleRadians);
        double sineAngle = Math.sin(planeAngleRadians);
        Vec3 worldPosition = frame.toWorldPolar(radiusBlocks, planeAngleRadians);

        // r = exp(C - kθ) ⇒ dr/dθ = -k r；ψ = θ + 相位 ⇒ dψ/dθ = 1。
        // 平面切向 (dx/dθ, dy/dθ) 指向 θ 增大、半径收进核里。
        double radialDerivative = -SPIRAL_K * radiusBlocks;
        double planeTangentX = radialDerivative * cosineAngle - radiusBlocks * sineAngle;
        double planeTangentY = radialDerivative * sineAngle + radiusBlocks * cosineAngle;
        Vec3 worldTangent = frame.toWorld(planeTangentX, planeTangentY);
        if (worldTangent.lengthSqr() > 1.0e-12) {
            worldTangent = worldTangent.normalize();
        } else {
            worldTangent = frame.normalAxis();
        }
        return new IsolineSample(worldPosition, worldTangent, radiusBlocks, thetaRadians);
    }

    /**
     * 漩涡盘面标架：中心 + 两个正交轴张成粒子所在平面，法线朝向观察方向。
     *
     * @param center     漩涡中心（世界坐标，方块）
     * @param rightAxis  盘面局部 +X（已单位化）
     * @param upAxis     盘面局部 +Y（已单位化）
     * @param normalAxis 盘面法线，通常等于施法朝向（已单位化）
     */
    public record PlaneFrame(Vec3 center, Vec3 rightAxis, Vec3 upAxis, Vec3 normalAxis) {

        /**
         * 用朝向当盘面法线：玩家正面能看见螺旋，而不是侧看成一条线。
         * {@code facing} 接近零或竖直时回退到水平 XZ 盘。
         */
        public static PlaneFrame facing(Vec3 center, Vec3 facing) {
            Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
            Vec3 normalAxis = facing.lengthSqr() > 1.0e-8
                    ? facing.normalize()
                    : worldUp;
            Vec3 rightAxis = normalAxis.cross(worldUp);
            if (rightAxis.lengthSqr() < 1.0e-8) {
                rightAxis = new Vec3(1.0, 0.0, 0.0);
            } else {
                rightAxis = rightAxis.normalize();
            }
            Vec3 upAxis = rightAxis.cross(normalAxis).normalize();
            return new PlaneFrame(center, rightAxis, upAxis, normalAxis);
        }

        /**
         * 盘面局部笛卡尔坐标映到世界偏移后的点。
         */
        public Vec3 toWorld(double localXBlocks, double localYBlocks) {
            return rightAxis.scale(localXBlocks).add(upAxis.scale(localYBlocks));
        }

        /**
         * 盘面极坐标映到世界坐标（已加上中心）。
         */
        public Vec3 toWorldPolar(double radiusBlocks, double planeAngleRadians) {
            return center.add(toWorld(
                    radiusBlocks * Math.cos(planeAngleRadians),
                    radiusBlocks * Math.sin(planeAngleRadians)
            ));
        }
    }

    /**
     * 等值线上的一个采样：世界位置、旋进核里的单位切向、该点半径与极角。
     *
     * @param worldPosition  世界坐标（方块）
     * @param inwardTangent  沿对数螺线旋进核里的单位切向
     * @param radiusBlocks   该点到中心的盘面半径（方块）
     * @param thetaRadians   这条等值线的参数角 {@code θ}（弧度），不含臂相位
     */
    public record IsolineSample(
            Vec3 worldPosition,
            Vec3 inwardTangent,
            double radiusBlocks,
            double thetaRadians
    ) {
    }
}

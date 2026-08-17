package com.eldenring.spells.tuning;

/**
 * 辉石系追踪索敌共用可调数值。
 * <p>
 * 所有继承 {@code AbstractGlintstoneProjectile} 的弹道在「锥内选谁」时共用本文件，
 * 各法术自己的锥半角 / 转向角 / 射程仍放在各自 {@code XxxTuning}。
 * <p>
 * 设计目标：优先锁「更正对飞行轴」的目标，而不是「最近」；
 * 并剔除按当前限角转向在飞到之前拐不过去的目标。
 */
public final class GlintstoneHomingTuning {

    private GlintstoneHomingTuning() {
    }

    /**
     * 索敌打分里角度项权重（无量纲）。
     * 分数越小越优先；角度项 = {@code (angle / coneHalf)^2 * 本权重}。
     * 调大 → 更死盯准星方向；调小 → 距离更有话语权。
     */
    public static final double ACQUIRE_ANGULAR_WEIGHT = 1.0;

    /**
     * 索敌打分里距离项权重（无量纲）。
     * 距离项 = {@code (distance / trackingRange) * 本权重}。
     * 保持明显小于角度权重，避免侧前方近怪抢走正前方远怪。
     * 调大 → 更倾向近目标；调小 → 几乎只看角度。
     */
    public static final double ACQUIRE_DISTANCE_WEIGHT = 0.25;

    /**
     * 转向预算松弛系数（无量纲，0–1）。
     * {@code maxReachableAngle = maxTurnPerTick * (distance / speed) * slack}。
     * 实际弧线比直线长，取小于 1 留余量。
     * 调小 → 更严格（近处大偏角更难被锁）；调大 → 更宽松。
     */
    public static final double TURN_BUDGET_SLACK = 0.85;

    /**
     * 施法者准星射线命中检测时，目标碰撞箱外扩（方块）。
     * 调大 → 准星更容易「点中」并优先锁该目标；调小 → 要更精确对准。
     */
    public static final float LOOK_RAY_HIT_INFLATION_BLOCKS = 0.30f;
}

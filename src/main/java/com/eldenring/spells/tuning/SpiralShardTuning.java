package com.eldenring.spells.tuning;

/**
 * 旋飞魔砾（Spiral Shard）可调数值。
 * <p>
 * 双彗星绕飞行轴线按欧拉公式（{@code e^{iθ}=cosθ+i·sinθ}）互旋；
 * 弱追踪只扭转中心轴线，不破坏两星相位差 π 的双螺旋关系。
 */
public final class SpiralShardTuning {

    private SpiralShardTuning() {
    }

    // -------------------------------------------------------------------------
    // 弹道飞行与弱追踪（作用在螺旋中心轴上）
    // -------------------------------------------------------------------------

    /**
     * 中心轴前进速度（方块/tick）。
     * 调大 → 螺旋整体冲得更快，双星轨迹更被拉长；调小 → 螺旋更密、更易看清互旋。
     */
    public static final float PROJECTILE_FLIGHT_SPEED = 0.95f;

    /** 索敌半径（方块）。 */
    public static final double PROJECTILE_TRACKING_RANGE_BLOCKS = 22.0;

    /**
     * 每 tick 最大转向（度）。弱追踪：只缓缓弯中心轴，保持双螺旋外形。
     * 调大 → 更容易拐弯；调小 → 更接近直飞螺旋。
     */
    public static final float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 2.0f;

    /** 出手后直飞、不追踪的 tick 数。 */
    public static final int PROJECTILE_TRACKING_START_DELAY_TICKS = 4;

    /** 索敌锥半角（度）。 */
    public static final float PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES = 35.0f;

    public static final double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.40;
    public static final double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;
    public static final double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

    /**
     * 实体穿透次数：每次命中敌人消耗 1 次，可重复命中同一目标；撞方块仍直接销毁。
     * <p>
     * 实现写入铁魔法 {@code pierceLevel = 本值 - 1}：命中时若剩余 &gt;0 则减 1 续飞，
     * 剩余为 0 时本命中结算后销毁，故 {@code 7} = 整段飞行最多结算 7 次实体伤害。
     * 调大 → 穿透更久；调小 → 更快消散。
     */
    public static final int PROJECTILE_MAX_ENTITY_HITS = 14;

    /**
     * 彗星命中判定相对目标碰撞箱的外扩（方块）。
     * 调大 → 更容易在螺旋扫过时连续挂上目标；调小 → 更挑准心。
     */
    public static final float PROJECTILE_HIT_INFLATION_BLOCKS = 0.55f;

    // -------------------------------------------------------------------------
    // 欧拉双螺旋（两颗彗星相对中心轴）
    // -------------------------------------------------------------------------

    /**
     * 螺旋半径（方块）：彗星到中心轴的距离。
     * 调大 → 两条轨迹分得更开；调小 → 更贴轴、像拧成一股。
     */
    public static final double SPIRAL_ORBIT_RADIUS_BLOCKS = 0.65;

    /**
     * 绕轴角速度（度/tick），对应欧拉相位 θ。
     * 调大 → 互旋更快、螺旋更密；调小 → 大螺距、旋转更慢。
     */
    public static final float SPIRAL_ANGULAR_SPEED_DEGREES_PER_TICK = 28.0f;

    /**
     * 出手后螺旋半径从 0 涨到满值的 tick 数，避免出生点两星重叠穿模。
     * 调大 → 展开更慢；0 = 立刻满半径。
     */
    public static final int SPIRAL_RADIUS_RAMP_TICKS = 3;

    // -------------------------------------------------------------------------
    // 彗星头视觉（单颗略小于魔砾，强调「双星」）
    // -------------------------------------------------------------------------

    public static final float COMET_HEAD_BODY_SCALE = 0.34f;
    public static final float COMET_HEAD_GLOW_SCALE = 0.68f;
    public static final float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.09f;
    public static final float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 24.0f;

    public static final float COMET_HEAD_CORE_RED = 0.18f;
    public static final float COMET_HEAD_CORE_GREEN = 0.90f;
    public static final float COMET_HEAD_CORE_BLUE = 1.0f;

    public static final float COMET_HEAD_GLOW_RED = 0.14f;
    public static final float COMET_HEAD_GLOW_GREEN = 0.92f;
    public static final float COMET_HEAD_GLOW_BLUE = 1.0f;
    public static final float COMET_HEAD_GLOW_ALPHA = 1.0f;

    /**
     * 旋飞魔砾单颗曲线光轨：每条最多保留约 32 方块 / 64 点，完整展现双螺旋。
     */
    public static final GlintstoneTrailTuning.TrailStyle TRAIL_STYLE =
            new GlintstoneTrailTuning.TrailStyle(32.0, 0.050f, 0.012f, 0.26f, 0.08f, 64);

    /** 拖尾点缀强度倍率；不影响几何光束长宽。 */
    public static final float TRAIL_PARTICLE_INTENSITY = 0.70f;
    public static final float IMPACT_PARTICLE_INTENSITY = 1.15f;
    public static final float CAST_BURST_PARTICLE_INTENSITY = 1.1f;

    // -------------------------------------------------------------------------
    // 法术数值
    // -------------------------------------------------------------------------

    public static final int SPELL_BASE_MANA_COST = 10;
    public static final int SPELL_MANA_COST_PER_LEVEL = 2;
    public static final int SPELL_BASE_SPELL_POWER = 10;
    public static final int SPELL_SPELL_POWER_PER_LEVEL = 1;
    public static final int SPELL_CAST_TIME_TICKS = 0;
    public static final double SPELL_COOLDOWN_SECONDS = 0.75;
    /** 最大等级。法环辉石咒固定 1 级。 */
    public static final int SPELL_MAX_LEVEL = 1;

    /**
     * 命中伤害系数（任一彗星命中即结算并销毁整对）。
     * 最终伤害 = spellPower × 本系数。
     */
    public static final float SPELL_DAMAGE_PER_SPELL_POWER = 0.58f;

    public static final double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.60;
}

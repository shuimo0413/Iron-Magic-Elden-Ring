package com.eldenring.spells.tuning;

/**
 * 辉石流星（Glintstone Stars）可调数值。
 * <p>
 * 流程：三发由齐射实体按 tick 依次出现在视线前方圆阵顶点（正三角形），再前冲并立刻强追踪。
 */
public final class GlintstoneStarsTuning {

    private GlintstoneStarsTuning() {
    }

    /** 单次施法流星数量。 */
    public static final int PROJECTILE_COUNT = 3;

    /**
     * 相邻两发出现的间隔（tick）。
     * 必须由 {@link com.eldenring.spells.entity.GlintstoneStarVolleyEntity} 按实体 tick 发射；
     * 调大 → 连射更疏、更不像齐射骗伤；调小 → 更接近齐射。
     * 原 3 tick 几乎看不出先后，现拉到约半拍，三发依次飞出。
     */
    public static final int PROJECTILE_SPAWN_STAGGER_TICKS = 2;

    /**
     * 生成圆半径（方块）。圆面垂直于视线，顶点按流星数量等分。
     * 调大 → 三角形更大、三发离得更开；调小 → 更挤在杖头附近。
     */
    public static final double SPAWN_CIRCLE_RADIUS_BLOCKS = 1.0;

    /**
     * 第一发在圆上的起始极角（度）。0 = 视野右侧，90 = 正上方。
     * 之后按 {@code 360 / 流星数}（整数除法）顺时针步进。
     */
    public static final int SPAWN_CIRCLE_START_ANGLE_DEGREES = 90;

    /**
     * 初始飞行方向在视线基础上叠加的世界上扬分量（无量纲，与视线相加后再归一化）。
     * {@code 0} = 完全平行于视线平射；调大 → 出手瞬间往上抛再折向目标。
     */
    public static final double PROJECTILE_INITIAL_UPWARD_LIFT = 0.0;

    /** 追踪飞行速度（方块/tick 量级）。 */
    public static final float PROJECTILE_FLIGHT_SPEED = 1.15f;

    public static final double PROJECTILE_TRACKING_RANGE_BLOCKS = 32.0;

    /** 强追踪：高于魔砾，贴近原作「朝目标飞去」。 */
    public static final float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 5.5f;

    /**
     * 出手后直飞、不追踪的 tick 数。
     * 流星要求「出现瞬间即追踪」，故为 0。
     */
    public static final int PROJECTILE_TRACKING_START_DELAY_TICKS = 0;

    public static final float PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES = 70.0f;
    public static final double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.35;
    public static final double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;
    public static final double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

    public static final float COMET_HEAD_BODY_SCALE = 0.26f;
    public static final float COMET_HEAD_GLOW_SCALE = 0.55f;
    public static final float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.09f;
    public static final float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 22.0f;

    public static final float COMET_HEAD_CORE_RED = 0.28f;
    public static final float COMET_HEAD_CORE_GREEN = 0.88f;
    public static final float COMET_HEAD_CORE_BLUE = 1.0f;

    public static final float COMET_HEAD_GLOW_RED = 0.24f;
    public static final float COMET_HEAD_GLOW_GREEN = 0.90f;
    public static final float COMET_HEAD_GLOW_BLUE = 1.0f;
    public static final float COMET_HEAD_GLOW_ALPHA = 1.0f;

    /**
     * 辉石流星曲线光轨：最多保留约 20 方块 / 40 点，突出强追踪弧线。
     */
    public static final GlintstoneTrailTuning.TrailStyle TRAIL_STYLE =
            new GlintstoneTrailTuning.TrailStyle(20.0, 0.045f, 0.010f, 0.24f, 0.07f, 40);

    /** 拖尾点缀强度倍率；不影响几何光束长宽。 */
    public static final float TRAIL_PARTICLE_INTENSITY = 0.70f;
    public static final float IMPACT_PARTICLE_INTENSITY = 1.2f;
    public static final float CAST_BURST_PARTICLE_INTENSITY = 1.2f;

    public static final int SPELL_BASE_MANA_COST = 12;
    public static final int SPELL_MANA_COST_PER_LEVEL = 2;
    public static final int SPELL_BASE_SPELL_POWER = 9;
    public static final int SPELL_SPELL_POWER_PER_LEVEL = 1;
    public static final int SPELL_CAST_TIME_TICKS = 0;
    public static final double SPELL_COOLDOWN_SECONDS = 1.0;
    /** 最大等级。法环辉石咒固定 1 级。 */
    public static final int SPELL_MAX_LEVEL = 1;

    /** 单发伤害系数；总输出约 = 系数 × 法强 × 3。 */
    public static final float SPELL_DAMAGE_PER_SPELL_POWER = 0.38f;

    public static final double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.65;
}

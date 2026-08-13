package com.eldenring.spells.tuning;

/**
 * 流星雨（Star Shower）可调数值。
 * <p>
 * 六发辉石流星依次出现在视线前方圆阵顶点（正六边形），再飞出并强追踪。
 */
public final class StarShowerTuning {

    private StarShowerTuning() {
    }

    /** 单次施法流星数量。 */
    public static final int PROJECTILE_COUNT = 6;

    /**
     * 相邻两发出现的间隔（tick）。
     * 调大 → 连射更疏、更不容易叠在同一视觉命中上；调小 → 更接近齐射。
     */
    public static final int PROJECTILE_SPAWN_STAGGER_TICKS = 2;

    /**
     * 生成圆半径（方块）。圆面垂直于视线，六等分后呈正六边形。
     * 调大 → 阵面更散；调小 → 更挤在杖头附近。
     */
    public static final double SPAWN_CIRCLE_RADIUS_BLOCKS = 1.0;

    /**
     * 第一发在圆上的起始极角（度）。0 = 视野右侧，90 = 正上方。
     * 之后按 {@code 360 / 流星数}（整数除法）顺时针步进。
     */
    public static final int SPAWN_CIRCLE_START_ANGLE_DEGREES = 90;

    /**
     * 初始飞行方向在视线基础上叠加的上扬分量（无量纲，与视线向量相加后再归一化）。
     * 调大 → 更明显往上抛；调小 → 更贴视线平射。
     */
    public static final double PROJECTILE_INITIAL_UPWARD_LIFT = 0.22;

    /** 追踪飞行速度（方块/tick 量级）。 */
    public static final float PROJECTILE_FLIGHT_SPEED = 1.12f;

    public static final double PROJECTILE_TRACKING_RANGE_BLOCKS = 34.0;

    /** 强追踪：贴近原作「朝目标飞去」。 */
    public static final float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 5.2f;

    /**
     * 出手后直飞、不追踪的 tick 数。
     * 流星雨要求「出现瞬间即追踪」，故为 0。
     */
    public static final int PROJECTILE_TRACKING_START_DELAY_TICKS = 0;

    public static final float PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES = 72.0f;
    public static final double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.35;
    public static final double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;
    public static final double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

    public static final float COMET_HEAD_BODY_SCALE = 0.24f;
    public static final float COMET_HEAD_GLOW_SCALE = 0.50f;
    public static final float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.08f;
    public static final float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 24.0f;

    public static final float COMET_HEAD_CORE_RED = 0.30f;
    public static final float COMET_HEAD_CORE_GREEN = 0.86f;
    public static final float COMET_HEAD_CORE_BLUE = 1.0f;

    public static final float COMET_HEAD_GLOW_RED = 0.26f;
    public static final float COMET_HEAD_GLOW_GREEN = 0.88f;
    public static final float COMET_HEAD_GLOW_BLUE = 1.0f;
    public static final float COMET_HEAD_GLOW_ALPHA = 1.0f;

    /**
     * 流星雨曲线光轨：略短于辉石流星，突出连发弧线而不是单条长尾。
     */
    public static final GlintstoneTrailTuning.TrailStyle TRAIL_STYLE =
            new GlintstoneTrailTuning.TrailStyle(16.0, 0.040f, 0.009f, 0.22f, 0.08f, 36);

    /** 拖尾点缀强度倍率；不影响几何光束长宽。 */
    public static final float TRAIL_PARTICLE_INTENSITY = 0.62f;
    public static final float IMPACT_PARTICLE_INTENSITY = 1.05f;
    public static final float CAST_BURST_PARTICLE_INTENSITY = 1.35f;

    public static final int SPELL_BASE_MANA_COST = 22;
    public static final int SPELL_MANA_COST_PER_LEVEL = 3;
    public static final int SPELL_BASE_SPELL_POWER = 9;
    public static final int SPELL_SPELL_POWER_PER_LEVEL = 1;
    public static final int SPELL_CAST_TIME_TICKS = 0;
    public static final double SPELL_COOLDOWN_SECONDS = 2.5;
    public static final int SPELL_MAX_LEVEL = 10;

    /** 单发伤害系数；总输出约 = 系数 × 法强 × 6。 */
    public static final float SPELL_DAMAGE_PER_SPELL_POWER = 0.30f;

    public static final double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.65;
}

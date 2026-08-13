package com.eldenring.spells.tuning;

/**
 * 毁灭流星（Stars of Ruin）可调数值。
 * <p>
 * 蓄力期间在施法者周围铺星河；吟唱结束后八发深蓝/亮蓝交织的追踪流星
 * 依次出现在视线前方圆阵顶点（正八边形）。
 */
public final class StarsOfRuinTuning {

    private StarsOfRuinTuning() {
    }

    /** 单次施法流星数量。 */
    public static final int PROJECTILE_COUNT = 8;

    /**
     * 相邻两发出现的间隔（tick）。
     * 调大 → 连射更疏；调小 → 更密的星雨。必须由齐射实体按 tick 发射。
     */
    public static final int PROJECTILE_SPAWN_STAGGER_TICKS = 2;

    /**
     * 生成圆半径（方块）。圆面垂直于视线，八等分后呈正八边形。
     * 调大 → 阵面更散；调小 → 更挤在杖头附近。
     */
    public static final double SPAWN_CIRCLE_RADIUS_BLOCKS = 1.0;

    /**
     * 第一发在圆上的起始极角（度）。0 = 视野右侧，90 = 正上方。
     * 之后按 {@code 360 / 流星数}（整数除法）顺时针步进。
     */
    public static final int SPAWN_CIRCLE_START_ANGLE_DEGREES = 90;

    /**
     * 初始飞行方向在视线基础上叠加的上扬分量（无量纲）。
     * 调大 → 更明显往上抛后再折向目标。
     */
    public static final double PROJECTILE_INITIAL_UPWARD_LIFT = 0.34;

    /** 追踪飞行速度（方块/tick 量级）。 */
    public static final float PROJECTILE_FLIGHT_SPEED = 1.22f;

    public static final double PROJECTILE_TRACKING_RANGE_BLOCKS = 40.0;

    /** 强追踪：高于辉石流星，贴近原作灭亡流星的追击感。 */
    public static final float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 6.2f;

    /**
     * 出手后直飞、不追踪的 tick 数。
     * 毁灭流星要求「出现瞬间即追踪」，故为 0。
     */
    public static final int PROJECTILE_TRACKING_START_DELAY_TICKS = 0;

    public static final float PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES = 78.0f;
    public static final double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.40;
    public static final double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;
    public static final double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

    public static final float COMET_HEAD_BODY_SCALE = 0.28f;
    public static final float COMET_HEAD_GLOW_SCALE = 0.62f;
    public static final float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.11f;
    public static final float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 26.0f;

    /**
     * 亮蓝色晶核 RGB（0–1）。偶数发使用，与深蓝色交替形成交织。
     */
    public static final float BRIGHT_CORE_RED = 0.28f;
    public static final float BRIGHT_CORE_GREEN = 0.52f;
    public static final float BRIGHT_CORE_BLUE = 1.0f;
    public static final float BRIGHT_GLOW_RED = 0.22f;
    public static final float BRIGHT_GLOW_GREEN = 0.48f;
    public static final float BRIGHT_GLOW_BLUE = 1.0f;
    public static final float BRIGHT_GLOW_ALPHA = 1.0f;

    /**
     * 深蓝色晶核 RGB（0–1）。奇数发使用。调绿更低、蓝更沉，避免回到辉石青。
     */
    public static final float DEEP_CORE_RED = 0.08f;
    public static final float DEEP_CORE_GREEN = 0.14f;
    public static final float DEEP_CORE_BLUE = 0.58f;
    public static final float DEEP_GLOW_RED = 0.10f;
    public static final float DEEP_GLOW_GREEN = 0.18f;
    public static final float DEEP_GLOW_BLUE = 0.72f;
    public static final float DEEP_GLOW_ALPHA = 1.0f;

    /**
     * 毁灭流星曲线光轨：更长、略宽，强调星河拖尾。
     */
    public static final GlintstoneTrailTuning.TrailStyle TRAIL_STYLE =
            new GlintstoneTrailTuning.TrailStyle(24.0, 0.050f, 0.011f, 0.20f, 0.12f, 48);

    /** 拖尾点缀强度倍率；不影响几何光束长宽。 */
    public static final float TRAIL_PARTICLE_INTENSITY = 0.85f;
    public static final float IMPACT_PARTICLE_INTENSITY = 1.35f;
    public static final float CAST_BURST_PARTICLE_INTENSITY = 1.8f;

    /**
     * 蓄力星河每 tick 的强度倍率。调大 → 施法者周围星尘更密。
     */
    public static final float STAR_RIVER_CAST_INTENSITY = 1.15f;

    /**
     * 星河螺旋沿视线铺开的长度（方块）。调大 → 河面更长。
     */
    public static final double STAR_RIVER_LENGTH_BLOCKS = 5.5;

    /**
     * 星河螺旋半径（方块）。调大 → 环绕施法者/视线的圆环更宽。
     */
    public static final double STAR_RIVER_RADIUS_BLOCKS = 0.85;

    public static final int SPELL_BASE_MANA_COST = 55;
    public static final int SPELL_MANA_COST_PER_LEVEL = 8;
    public static final int SPELL_BASE_SPELL_POWER = 10;
    public static final int SPELL_SPELL_POWER_PER_LEVEL = 1;

    /**
     * 蓄力吟唱时长（tick）。调大 → 星河特效铺得更久，也更容易被打断。
     */
    public static final int SPELL_CAST_TIME_TICKS = 24;

    public static final double SPELL_COOLDOWN_SECONDS = 6.0;
    public static final int SPELL_MAX_LEVEL = 8;

    /** 单发伤害系数；总输出约 = 系数 × 法强 × 8。 */
    public static final float SPELL_DAMAGE_PER_SPELL_POWER = 0.36f;

    public static final double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.70;
}

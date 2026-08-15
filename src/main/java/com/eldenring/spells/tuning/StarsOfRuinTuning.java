package com.eldenring.spells.tuning;

/**
 * 毁灭流星（Stars of Ruin）可调数值。
 * <p>
 * 出手瞬间在右手前方铺一团星云，随后十二发深蓝 / 紫色交织的追踪流星
 * 沿视线平射、依次出现在前方圆阵顶点。星云布局见 {@link StarRiverTuning}。
 */
public final class StarsOfRuinTuning {

    private StarsOfRuinTuning() {
    }

    /** 单次施法流星数量。 */
    public static final int PROJECTILE_COUNT = 12;

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
     * 初始飞行方向在视线基础上叠加的世界上扬分量（无量纲，与视线相加后再归一化）。
     * {@code 0} = 完全平行于视线平射；调大 → 出手瞬间往上抛再折向目标。
     */
    public static final double PROJECTILE_INITIAL_UPWARD_LIFT = 0.0;

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
     * 紫色晶核 RGB（0–1）。偶数发使用，与深蓝色交替形成蓝紫交织。
     * 绿通道压低，避免洗成辉石青；红略抬高才像紫晶而不是品红。
     */
    public static final float PURPLE_CORE_RED = 0.48f;
    public static final float PURPLE_CORE_GREEN = 0.14f;
    public static final float PURPLE_CORE_BLUE = 0.86f;
    public static final float PURPLE_GLOW_RED = 0.58f;
    public static final float PURPLE_GLOW_GREEN = 0.22f;
    public static final float PURPLE_GLOW_BLUE = 0.94f;
    public static final float PURPLE_GLOW_ALPHA = 1.0f;

    /**
     * 深蓝色晶核 RGB（0–1）。奇数发使用。绿更低、蓝更沉，贴近虚空核外圈。
     */
    public static final float DEEP_CORE_RED = 0.07f;
    public static final float DEEP_CORE_GREEN = 0.16f;
    public static final float DEEP_CORE_BLUE = 0.62f;
    public static final float DEEP_GLOW_RED = 0.10f;
    public static final float DEEP_GLOW_GREEN = 0.22f;
    public static final float DEEP_GLOW_BLUE = 0.78f;
    public static final float DEEP_GLOW_ALPHA = 1.0f;

    /**
     * 毁灭流星曲线光轨：几何光束负责连续轨迹，粒子只做弹头点缀。
     * spark / mote 低于单发彗星——12 连发会线性叠加，调大会糊成雾。
     */
    public static final GlintstoneTrailTuning.TrailStyle TRAIL_STYLE =
            new GlintstoneTrailTuning.TrailStyle(24.0, 0.050f, 0.011f, 0.10f, 0.05f, 48);

    /**
     * 拖尾点缀强度倍率。只影响弹头附近光晕/星尘概率，不影响几何光束长宽。
     * 调大 → 每发更密；调小 → 十二条轨迹更干净。
     */
    public static final float TRAIL_PARTICLE_INTENSITY = 0.48f;

    /**
     * 弹头点缀相对通用辉石拖尾的概率倍率（光晕、星尘、残影、碎晶、星团）。
     * 几何光束已勾出轨迹；12 连发只需稀疏剥落。调大 → 点缀更密。
     */
    public static final float TRAIL_ACCENT_CHANCE_SCALE = 0.55f;
    public static final float IMPACT_PARTICLE_INTENSITY = 1.35f;
    public static final float CAST_BURST_PARTICLE_INTENSITY = 1.8f;

    /**
     * @deprecated 星云不再沿视线拉长，半径改 {@link StarRiverTuning#NEBULA_RADIUS_BLOCKS}。
     */
    @Deprecated
    public static final double STAR_RIVER_LENGTH_BLOCKS = 5.5;

    /**
     * @deprecated 改 {@link StarRiverTuning#NEBULA_RADIUS_BLOCKS}。
     */
    @Deprecated
    public static final double STAR_RIVER_RADIUS_BLOCKS = 0.85;

    public static final int SPELL_BASE_MANA_COST = 55;
    public static final int SPELL_MANA_COST_PER_LEVEL = 8;
    public static final int SPELL_BASE_SPELL_POWER = 10;
    public static final int SPELL_SPELL_POWER_PER_LEVEL = 1;

    /**
     * 吟唱时长（tick）。瞬时施法固定为 0。
     */
    public static final int SPELL_CAST_TIME_TICKS = 0;

    public static final double SPELL_COOLDOWN_SECONDS = 6.0;
    /** 最大等级。法环辉石咒固定 1 级。 */
    public static final int SPELL_MAX_LEVEL = 1;

    /** 单发伤害系数；总输出约 = 系数 × 法强 × 8。 */
    public static final float SPELL_DAMAGE_PER_SPELL_POWER = 0.36f;

    public static final double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.70;
}

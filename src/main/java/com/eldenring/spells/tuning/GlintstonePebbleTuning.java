package com.eldenring.spells.tuning;

/**
 * 辉石魔砾（Glintstone Pebble）全部可调数值集中在此。
 * <p>
 * 调手感 / 平衡时优先改本文件，避免在 Spell / Projectile 里四处搜散落的魔法数字。
 * 后续其它辉石法术可各自新建 {@code XxxTuning}，或抽公共基类到 {@code tuning/glintstone/}。
 */
public final class GlintstonePebbleTuning {

    private GlintstonePebbleTuning() {
    }

    // -------------------------------------------------------------------------
    // 弹道飞行与追踪（GlintstonePebbleProjectile）
    // -------------------------------------------------------------------------

    /**
     * 弹道飞行速度（方块/tick 量级，传给 {@code AbstractMagicProjectile#getSpeed()}）。
     * 越大飞得越快、越难躲开，但限角追踪也更容易「跟不上」急转目标。
     */
    public static final float PROJECTILE_FLIGHT_SPEED = 0.7f;

    /**
     * 追踪索敌半径（方块）。超出此距离的生物不会被当作追踪目标。
     */
    public static final double PROJECTILE_TRACKING_RANGE_BLOCKS = 25.0;

    /**
     * 每 tick 允许的最大转向角度（度）。
     * 越小越像法环「轻微追踪」、越容易因侧移而打空；越大越接近强锁。
     */
    public static final float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 3.0f;

    /**
     * 射出后先沿视线直飞的 tick 数；其间不做任何追踪转向。
     * 避免刚出手就被身旁目标拽歪，保证「正前方射出」。
     */
    public static final int PROJECTILE_TRACKING_START_DELAY_TICKS = 5;

    /**
     * 索敌锥半角（度）：目标须落在「当前飞行方向」此锥内才会被追踪。
     * 侧面 / 身后的怪不会把弹道一出手就拧歪。
     */
    public static final float PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES = 40.0f;

    /**
     * 生成点相对眼睛、沿视线再前移的距离（方块），减少出生在碰撞箱内导致的异常。
     */
    public static final double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.35;

    /**
     * 当前速度过小（近似静止）时跳过本 tick 转向，避免除零 / 方向抖动。
     */
    public static final double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;

    /**
     * 当前朝向与目标朝向夹角极小时，直接对齐目标方向（弧度阈值，避免 acos 噪声）。
     */
    public static final double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

    // -------------------------------------------------------------------------
    // 彗星头视觉（GlintstonePebbleRenderer / GlintstoneCometHeadDrawer）
    // -------------------------------------------------------------------------

    /** 垂直飞行方向的晶核缩放。魔砾保持接近球形的短菱形。 */
    public static final float COMET_HEAD_BODY_SCALE_RADIAL = 0.42f;

    /** 沿飞行轴的晶核缩放。与径向相同 = 不拉成针。 */
    public static final float COMET_HEAD_BODY_SCALE_ALONG = 0.42f;

    /** 兼容旧名：均匀缩放时等于径向。 */
    public static final float COMET_HEAD_BODY_SCALE = COMET_HEAD_BODY_SCALE_RADIAL;

    /** 相机朝向光晕基础缩放。调大 → 本体周围辉光更大。 */
    public static final float COMET_HEAD_GLOW_SCALE = 0.78f;

    /** 光晕沿飞行方向的拉伸倍率。1 = 球形。 */
    public static final float COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE = 1.0f;

    /** 光晕呼吸振幅（叠加在 GLOW_SCALE 上）。 */
    public static final float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.10f;

    /** 光晕绕视线旋转角速度（度 / tick）。 */
    public static final float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 18.0f;

    /** 晶核着色 RGB（0–1），辉石蓝绿色（偏青而非纯蓝）。 */
    public static final float COMET_HEAD_CORE_RED = 0.12f;
    public static final float COMET_HEAD_CORE_GREEN = 0.78f;
    public static final float COMET_HEAD_CORE_BLUE = 1.0f;

    /** 光晕着色 RGBA（0–1）。alpha 越高本体越亮。 */
    public static final float COMET_HEAD_GLOW_RED = 0.10f;
    public static final float COMET_HEAD_GLOW_GREEN = 0.82f;
    public static final float COMET_HEAD_GLOW_BLUE = 1.0f;
    public static final float COMET_HEAD_GLOW_ALPHA = 1.0f;

    // -------------------------------------------------------------------------
    // 法术数值（GlintstonePebbleSpell — 铁魔法 DefaultConfig / AbstractSpell 字段）
    // -------------------------------------------------------------------------

    /** 1 级基础法力消耗。 */
    public static final int SPELL_BASE_MANA_COST = 8;

    /** 每升一级额外法力消耗。 */
    public static final int SPELL_MANA_COST_PER_LEVEL = 2;

    /** 1 级基础法术强度（参与伤害公式）。 */
    public static final int SPELL_BASE_SPELL_POWER = 10;

    /** 每升一级额外法术强度。 */
    public static final int SPELL_SPELL_POWER_PER_LEVEL = 1;

    /** 吟唱时间（tick）。0 = 瞬时施法，贴近法环可移动连发。 */
    public static final int SPELL_CAST_TIME_TICKS = 0;

    /** 冷却时间（秒）。 */
    public static final double SPELL_COOLDOWN_SECONDS = 0.5;

    /** 最大等级。法环辉石咒固定 1 级。 */
    public static final int SPELL_MAX_LEVEL = 1;
    

    /**
     * 基础魔砾曲线光轨：短细、无螺旋细丝。
     */
    public static final GlintstoneTrailTuning.TrailStyle TRAIL_STYLE =
            new GlintstoneTrailTuning.TrailStyle(7.0, 0.050f, 0.010f, 0.18f, 0.05f, 22);

    /** 拖尾点缀强度倍率；不影响几何光束长宽。 */
    public static final float TRAIL_PARTICLE_INTENSITY = 0.45f;

    /**
     * 命中爆裂粒子强度（相对魔砾基准）。调大 → 能量场/烟雾更浓。
     */
    public static final float IMPACT_PARTICLE_INTENSITY = 1.25f;

    /**
     * 最终伤害 = {@code getSpellPower(level, caster) * SPELL_DAMAGE_PER_SPELL_POWER}。
     */
    public static final float SPELL_DAMAGE_PER_SPELL_POWER = 0.55f;

    /**
     * 施法瞬间粒子爆发相对眼睛位置、沿视线方向的前移距离（方块）。
     */
    public static final double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.6;
}

package com.eldenring.spells.tuning;

/**
 * 辉石迅魔砾（Swift Glintstone Shard）可调数值。
 * <p>
 * 相对魔砾：更快、更薄、更低伤、更弱追踪、更短冷却 — 适合走位连发。
 */
public final class SwiftGlintstoneShardTuning {

    private SwiftGlintstoneShardTuning() {
    }

    // -------------------------------------------------------------------------
    // 弹道飞行与追踪
    // -------------------------------------------------------------------------

    /** 飞行速度（方块/tick）；高于魔砾，更难侧移躲开。 */
    public static final float PROJECTILE_FLIGHT_SPEED = 1.65f;

    /** 索敌半径（方块）；略短于魔砾。 */
    public static final double PROJECTILE_TRACKING_RANGE_BLOCKS = 18.0;

    /** 每 tick 最大转向（度）；弱追踪，强调「快打快收」。 */
    public static final float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 1.8f;

    /** 出手直飞 tick 数。 */
    public static final int PROJECTILE_TRACKING_START_DELAY_TICKS = 3;

    /** 索敌锥半角（度）。 */
    public static final float PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES = 28.0f;

    /** 生成点沿视线前移（方块）。 */
    public static final double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.35;

    public static final double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;
    public static final double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

    // -------------------------------------------------------------------------
    // 彗星头视觉（更细、更亮的针状感）
    // -------------------------------------------------------------------------

    public static final float COMET_HEAD_BODY_SCALE = 0.30f;
    public static final float COMET_HEAD_GLOW_SCALE = 0.58f;
    public static final float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.08f;
    public static final float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 26.0f;

    public static final float COMET_HEAD_CORE_RED = 0.20f;
    public static final float COMET_HEAD_CORE_GREEN = 0.92f;
    public static final float COMET_HEAD_CORE_BLUE = 1.0f;

    public static final float COMET_HEAD_GLOW_RED = 0.18f;
    public static final float COMET_HEAD_GLOW_GREEN = 0.95f;
    public static final float COMET_HEAD_GLOW_BLUE = 1.0f;
    public static final float COMET_HEAD_GLOW_ALPHA = 1.0f;

    /**
     * 迅魔砾曲线光轨：高速弹保留约 12 方块 / 32 点，仍保持细针观感。
     */
    public static final GlintstoneTrailTuning.TrailStyle TRAIL_STYLE =
            new GlintstoneTrailTuning.TrailStyle(12.0, 0.040f, 0.008f, 0.22f, 0.06f, 32);

    /** 拖尾点缀强度倍率；不影响几何光束长宽。 */
    public static final float TRAIL_PARTICLE_INTENSITY = 0.65f;
    public static final float IMPACT_PARTICLE_INTENSITY = 1.05f;
    public static final float CAST_BURST_PARTICLE_INTENSITY = 0.8f;

    // -------------------------------------------------------------------------
    // 法术数值
    // -------------------------------------------------------------------------

    public static final int SPELL_BASE_MANA_COST = 6;
    public static final int SPELL_MANA_COST_PER_LEVEL = 1;
    public static final int SPELL_BASE_SPELL_POWER = 8;
    public static final int SPELL_SPELL_POWER_PER_LEVEL = 1;
    public static final int SPELL_CAST_TIME_TICKS = 0;
    public static final double SPELL_COOLDOWN_SECONDS = 0.25;
    public static final int SPELL_MAX_LEVEL = 10;
    public static final float SPELL_DAMAGE_PER_SPELL_POWER = 0.42f;
    public static final double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.55;
}

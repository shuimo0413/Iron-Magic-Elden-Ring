package com.eldenring.spells.tuning;

/**
 * 辉石大魔砾（Great Glintstone Shard）可调数值。
 * <p>
 * 相对魔砾：更大、更慢、更高伤、命中小范围爆炸。
 */
public final class GreatGlintstoneShardTuning {

    private GreatGlintstoneShardTuning() {
    }

    public static final float PROJECTILE_FLIGHT_SPEED = 0.95f;
    public static final double PROJECTILE_TRACKING_RANGE_BLOCKS = 28.0;
    public static final float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 2.4f;
    public static final int PROJECTILE_TRACKING_START_DELAY_TICKS = 6;
    public static final float PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES = 36.0f;
    public static final double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.45;
    public static final double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;
    public static final double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

    /**
     * 命中爆炸半径（方块）。调大 → 清小群更强；调小 → 更偏单体。
     */
    public static final float EXPLOSION_RADIUS_BLOCKS = 1.8f;

    public static final float COMET_HEAD_BODY_SCALE_RADIAL = 1.05f;
    public static final float COMET_HEAD_BODY_SCALE_ALONG = 0.85f;
    public static final float COMET_HEAD_BODY_SCALE = COMET_HEAD_BODY_SCALE_RADIAL;
    public static final float COMET_HEAD_GLOW_SCALE = 1.45f;
    public static final float COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE = 1.0f;
    public static final float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.14f;
    public static final float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 14.0f;

    public static final float COMET_HEAD_CORE_RED = 0.08f;
    public static final float COMET_HEAD_CORE_GREEN = 0.70f;
    public static final float COMET_HEAD_CORE_BLUE = 1.0f;

    public static final float COMET_HEAD_GLOW_RED = 0.06f;
    public static final float COMET_HEAD_GLOW_GREEN = 0.72f;
    public static final float COMET_HEAD_GLOW_BLUE = 1.0f;
    public static final float COMET_HEAD_GLOW_ALPHA = 1.0f;

    /**
     * 大魔砾曲线光轨：短而粗，强调块状弹头而不是彗尾。
     */
    public static final GlintstoneTrailTuning.TrailStyle TRAIL_STYLE =
            new GlintstoneTrailTuning.TrailStyle(12.0, 0.145f, 0.034f, 0.22f, 0.08f, 32);

    /** 拖尾点缀强度倍率；不影响几何光束长宽。 */
    public static final float TRAIL_PARTICLE_INTENSITY = 0.55f;
    public static final float IMPACT_PARTICLE_INTENSITY = 2.35f;
    public static final float CAST_BURST_PARTICLE_INTENSITY = 1.85f;

    public static final int SPELL_BASE_MANA_COST = 14;
    public static final int SPELL_MANA_COST_PER_LEVEL = 3;
    public static final int SPELL_BASE_SPELL_POWER = 14;
    public static final int SPELL_SPELL_POWER_PER_LEVEL = 2;
    public static final int SPELL_CAST_TIME_TICKS = 0;
    public static final double SPELL_COOLDOWN_SECONDS = 0.85;
    /** 最大等级。法环辉石咒固定 1 级。 */
    public static final int SPELL_MAX_LEVEL = 1;
    public static final float SPELL_DAMAGE_PER_SPELL_POWER = 0.78f;
    public static final double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.75;
}

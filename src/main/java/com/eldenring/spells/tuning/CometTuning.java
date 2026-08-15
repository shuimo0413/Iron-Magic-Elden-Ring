package com.eldenring.spells.tuning;

/**
 * 帚星（Comet）可调数值。
 * <p>
 * 巨型彗星、长拖尾、高伤；命中后在固定半径内爆炸。
 */
public final class CometTuning {

    private CometTuning() {
    }

    public static final float PROJECTILE_FLIGHT_SPEED = 1.4f;
    public static final double PROJECTILE_TRACKING_RANGE_BLOCKS = 30.0;
    public static final float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 2.2f;
    public static final int PROJECTILE_TRACKING_START_DELAY_TICKS = 4;
    public static final float PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES = 32.0f;
    public static final double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.55;
    public static final double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;
    public static final double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

    /**
     * 命中爆炸半径（方块）。调大 → 清群更强；调小 → 更接近单体高伤。
     */
    public static final float EXPLOSION_RADIUS_BLOCKS = 2.8f;

    public static final float COMET_HEAD_BODY_SCALE = 1.45f;
    public static final float COMET_HEAD_GLOW_SCALE = 2.35f;
    public static final float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.20f;
    public static final float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 12.0f;

    public static final float COMET_HEAD_CORE_RED = 0.05f;
    public static final float COMET_HEAD_CORE_GREEN = 0.62f;
    public static final float COMET_HEAD_CORE_BLUE = 1.0f;

    public static final float COMET_HEAD_GLOW_RED = 0.04f;
    public static final float COMET_HEAD_GLOW_GREEN = 0.68f;
    public static final float COMET_HEAD_GLOW_BLUE = 1.0f;
    public static final float COMET_HEAD_GLOW_ALPHA = 1.0f;

    /**
     * 帚星曲线光轨：最多保留约 72 方块 / 96 点，通常可看到整条有效飞行路径。
     */
    public static final GlintstoneTrailTuning.TrailStyle TRAIL_STYLE =
            new GlintstoneTrailTuning.TrailStyle(72.0, 0.260f, 0.050f, 0.55f, 0.20f, 96);

    /** 拖尾点缀强度倍率；不影响几何光束长宽。 */
    public static final float TRAIL_PARTICLE_INTENSITY = 1.75f;
    public static final float IMPACT_PARTICLE_INTENSITY = 2.85f;
    public static final float CAST_BURST_PARTICLE_INTENSITY = 2.1f;

    public static final int SPELL_BASE_MANA_COST = 24;
    public static final int SPELL_MANA_COST_PER_LEVEL = 4;
    public static final int SPELL_BASE_SPELL_POWER = 18;
    public static final int SPELL_SPELL_POWER_PER_LEVEL = 2;
    public static final int SPELL_CAST_TIME_TICKS = 0;
    public static final double SPELL_COOLDOWN_SECONDS = 1.6;
    /** 最大等级。法环辉石咒固定 1 级。 */
    public static final int SPELL_MAX_LEVEL = 1;
    public static final float SPELL_DAMAGE_PER_SPELL_POWER = 1.15f;
    public static final double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.9;
}

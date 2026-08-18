package com.eldenring.spells.tuning;

/**
 * 辉石彗星（Glintstone Comet）可调数值。
 * <p>
 * 强度介于 {@link GreatGlintstoneShardTuning} 与 {@link CometTuning} 之间。
 */
public final class GlintstoneCometTuning {

    private GlintstoneCometTuning() {
    }

    public static final float PROJECTILE_FLIGHT_SPEED = 1.2f;
    public static final double PROJECTILE_TRACKING_RANGE_BLOCKS = 29.0;
    public static final float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 2.3f;
    public static final int PROJECTILE_TRACKING_START_DELAY_TICKS = 5;
    public static final float PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES = 34.0f;
    public static final double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.5;
    public static final double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;
    public static final double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

    /**
     * 命中爆炸半径（方块）：介于大魔砾与帚星之间。
     */
    public static final float EXPLOSION_RADIUS_BLOCKS = 2.2f;

    public static final float COMET_HEAD_BODY_SCALE_RADIAL = 0.70f;
    public static final float COMET_HEAD_BODY_SCALE_ALONG = 1.70f;
    public static final float COMET_HEAD_BODY_SCALE = COMET_HEAD_BODY_SCALE_RADIAL;
    public static final float COMET_HEAD_GLOW_SCALE = 1.35f;
    public static final float COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE = 2.05f;
    public static final float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.16f;
    public static final float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 13.0f;

    public static final float COMET_HEAD_CORE_RED = 0.06f;
    public static final float COMET_HEAD_CORE_GREEN = 0.66f;
    public static final float COMET_HEAD_CORE_BLUE = 1.0f;

    public static final float COMET_HEAD_GLOW_RED = 0.05f;
    public static final float COMET_HEAD_GLOW_GREEN = 0.70f;
    public static final float COMET_HEAD_GLOW_BLUE = 1.0f;
    public static final float COMET_HEAD_GLOW_ALPHA = 1.0f;

    /**
     * 辉石彗星：长尾 + 加法亮芯 + 两条螺旋细丝，开始读成彗星而不是大号魔砾。
     */
    public static final GlintstoneTrailTuning.TrailStyle TRAIL_STYLE =
            new GlintstoneTrailTuning.TrailStyle(
                    40.0,
                    0.155f,
                    0.032f,
                    0.16f,
                    0.06f,
                    64,
                    new GlintstoneTrailTuning.HelixStyle(2, 0.18f, 0.08f, 0.045f, 0.22f, 0.10f),
                    true,
                    false
            );

    /** 拖尾点缀强度倍率；不影响几何光束长宽。 */
    public static final float TRAIL_PARTICLE_INTENSITY = 0.50f;
    public static final float IMPACT_PARTICLE_INTENSITY = 2.55f;
    public static final float CAST_BURST_PARTICLE_INTENSITY = 1.95f;

    public static final int SPELL_BASE_MANA_COST = 18;
    public static final int SPELL_MANA_COST_PER_LEVEL = 3;
    public static final int SPELL_BASE_SPELL_POWER = 16;
    public static final int SPELL_SPELL_POWER_PER_LEVEL = 2;
    public static final int SPELL_CAST_TIME_TICKS = 0;
    public static final double SPELL_COOLDOWN_SECONDS = 1.2;
    /** 最大等级。法环辉石咒固定 1 级。 */
    public static final int SPELL_MAX_LEVEL = 1;
    public static final float SPELL_DAMAGE_PER_SPELL_POWER = 0.95f;
    public static final double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.8;
}

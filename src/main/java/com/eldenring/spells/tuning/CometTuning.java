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

    /**
     * 刺簇整体缩放。块状，不要再沿飞行轴拉成梭子。
     * 调大 → 整团晶刺更大。
     */
    public static final float COMET_HEAD_BODY_SCALE = 1.80f;

    /** 包住刺簇的柔光晕缩放。 */
    public static final float COMET_HEAD_GLOW_SCALE = 2.05f;

    /** 略拉长，仍以球形光晕为主，避免把刺簇重新吃成梭子。 */
    public static final float COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE = 1.25f;

    public static final float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.16f;
    public static final float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 10.0f;

    /**
     * 刺簇绕飞行轴自转（度 / tick）。调大 → 侧面更容易看见刺。
     */
    public static final float CLUSTER_SPIN_DEGREES_PER_TICK = 7.0f;

    /** 不规则核心：更深的青，对照原作暗核。 */
    public static final float COMET_HEAD_CORE_RED = 0.04f;
    public static final float COMET_HEAD_CORE_GREEN = 0.32f;
    public static final float COMET_HEAD_CORE_BLUE = 0.48f;

    /** 尖刺：更亮的白青。 */
    public static final float COMET_HEAD_SPIKE_RED = 0.38f;
    public static final float COMET_HEAD_SPIKE_GREEN = 0.94f;
    public static final float COMET_HEAD_SPIKE_BLUE = 1.0f;

    public static final float COMET_HEAD_GLOW_RED = 0.22f;
    public static final float COMET_HEAD_GLOW_GREEN = 0.88f;
    public static final float COMET_HEAD_GLOW_BLUE = 1.0f;
    public static final float COMET_HEAD_GLOW_ALPHA = 1.0f;

    /**
     * 帚星扫帚彗尾：更宽更长 + 外雾层 + 加法芯 + 五条螺旋细丝。
     */
    public static final GlintstoneTrailTuning.TrailStyle TRAIL_STYLE =
            new GlintstoneTrailTuning.TrailStyle(
                    64.0,
                    0.280f,
                    0.055f,
                    0.14f,
                    0.05f,
                    80,
                    new GlintstoneTrailTuning.HelixStyle(5, 0.42f, 0.16f, 0.055f, 0.16f, 0.07f),
                    true,
                    true
            );

    /** 拖尾点缀强度倍率；不影响几何光束长宽。 */
    public static final float TRAIL_PARTICLE_INTENSITY = 0.55f;
    public static final float IMPACT_PARTICLE_INTENSITY = 1.80f;
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

package com.eldenring.spells.tuning;

/**
 * 彗星亚兹勒可调常量。
 * <p>
 * 流程：2 秒蓄力漩涡（对数螺线汇聚）→ 按住右键星河喷流（ribbon + 周围粒子）→ 松手或没蓝停止。
 * 铁魔法 CONTINUOUS 的 {@link #SPELL_CAST_TIME_TICKS} 是最长按住时间，不是蓄力条。
 */
public final class CometAzurTuning {

    private CometAzurTuning() {
    }

    public static final int SPELL_MAX_LEVEL = 5;
    public static final int SPELL_COOLDOWN_SECONDS = 18;
    /**
     * 每次脉冲蓝耗。CONTINUOUS 约每 10 tick 扣一次，UI「每秒」大约是这个数 ×2。
     * 喷流写好后，没蓝会在下一次脉冲停吟唱。
     */
    public static final int SPELL_BASE_MANA_COST = 10;
    public static final int SPELL_MANA_COST_PER_LEVEL = 2;
    public static final int SPELL_BASE_SPELL_POWER = 8;
    public static final int SPELL_SPELL_POWER_PER_LEVEL = 2;

    /**
     * CONTINUOUS 最长按住时间（tick）。含 2 秒蓄力，剩下的是以后喷流能持续的上限。
     * 400 tick = 20 秒；蓝不够会更早停。调大 → 蓝够就能喷更久。
     */
    public static final int SPELL_CAST_TIME_TICKS = 400;

    /**
     * 蓄力 / 对数螺线汇聚时长（tick）。20 tick = 1 秒，40 = 2 秒。
     * 这段结束之后才进入喷流（ribbon 星河柱 + 周围粒子）。
     */
    public static final int STARTUP_DURATION_TICKS = 40;

    /**
     * 漩涡中心相对眼睛沿朝向的前方距离（方块）。
     * 调大 → 更远、第三人称更好看；调小 → 更贴脸。
     */
    public static final double STARTUP_VORTEX_FORWARD_OFFSET_BLOCKS = 1.90;

    /**
     * 漩涡中心相对眼睛向下偏（方块）。正值往下，避免整团糊在准星上。
     */
    public static final double STARTUP_VORTEX_DOWN_OFFSET_BLOCKS = 0.18;

    /**
     * {@code comet_azur_shrink_2} 中心层四边形边长（方块）。
     */
    public static final float STARTUP_SHRINK_2_QUAD_SIZE_BLOCKS = 2.15f;

    /**
     * {@code comet_azur_shrink_1} 叠加层四边形边长（方块）。略小、反向转。
     */
    public static final float STARTUP_SHRINK_1_QUAD_SIZE_BLOCKS = 1.70f;

    /**
     * shrink_2 平面旋转角速度（弧度 / tick）。正值逆时针。
     * 0.28 ≈ 2 秒转约 1.8 圈。
     */
    public static final float STARTUP_SHRINK_2_ROLL_RADIANS_PER_TICK = 0.28f;

    /**
     * shrink_1 角速度（弧度 / tick）。负值 = 与主层反向。
     */
    public static final float STARTUP_SHRINK_1_ROLL_RADIANS_PER_TICK = -0.18f;

    /**
     * 对数螺线极角上限（弧度）。{@code A} 从 0 走到这个值，对应 {@code 12π}（6 圈）。
     */
    public static final float STARTUP_SPIRAL_MAX_ANGLE_RADIANS = (float) (12.0 * Math.PI);

    /**
     * {@code A = 0} 时的世界半径（方块）。公式是 {@code r = 外半径 × e^(wA)}。
     * 调大 → 螺线更散、蓄力盘更大。
     */
    public static final float STARTUP_SPIRAL_OUTER_RADIUS_BLOCKS = 5.0f;

    /**
     * 对数螺线增长率 {@code w}（1/弧度）。必须为负才会往中心收：
     * {@code r = e^(wA)}，A 增大时 r 变小。
     * 每个 w 是一条松紧不同的汇聚曲线；越负收得越快、越贴中心。
     */
    public static final float[] STARTUP_SPIRAL_W_PER_CURVE = {
            -0.045f,
            -0.070f,
            -0.100f,
            -0.140f
    };

    /**
     * 把同一组 w 再绕法线复制成几条臂，避免所有曲线叠在同一条角上。
     * 调成 1 就只看 w 本身的多条嵌套螺线。
     */
    public static final int STARTUP_SPIRAL_ARM_COUNT = 3;

    /**
     * 每条（w, 臂）上沿 A 铺多少颗粒子。粒子在 2 秒内从各自的 A 走到 {@code 12π}，整条螺线一起收进中心。
     * 调大更密；总数 ≈ w条数 × 臂数 × 本值。
     */
    public static final int STARTUP_SPIRAL_SAMPLES_PER_CURVE = 8;

    /** 闪星（mote_1 / mote_2）四边形边长（方块）。 */
    public static final float STARTUP_MOTE_QUAD_SIZE_BLOCKS = 0.11f;

    /** 十字冲击星（impact）四边形边长（方块）。 */
    public static final float STARTUP_IMPACT_QUAD_SIZE_BLOCKS = 0.16f;

    /** 星团头（head）四边形边长（方块）。 */
    public static final float STARTUP_HEAD_QUAD_SIZE_BLOCKS = 0.22f;

    /** 星尘（dust）四边形边长（方块）。 */
    public static final float STARTUP_DUST_QUAD_SIZE_BLOCKS = 0.20f;

    /**
     * 蓄力结束后的星辰涟漪时长（tick）。10 tick = 0.5 秒。
     */
    public static final int SHOCKWAVE_DURATION_TICKS = 15;

    /**
     * 星环起始半径（方块）。
     */
    public static final float SHOCKWAVE_RADIUS_START_BLOCKS = 0.5f;

    /**
     * 星环结束半径（方块）。0.5 秒末胀到这里后淡出。
     */
    public static final float SHOCKWAVE_RADIUS_END_BLOCKS = 4.0f;

    /**
     * 中心绽光半径：从这么大胀到 {@link #SHOCKWAVE_CORE_RADIUS_END_BLOCKS}。
     */
    public static final float SHOCKWAVE_CORE_RADIUS_START_BLOCKS = 0.22f;

    public static final float SHOCKWAVE_CORE_RADIUS_END_BLOCKS = 0.85f;

    /** 中心绽光寿命（tick），比外环更短，先亮一下再让星环接手。 */
    public static final int SHOCKWAVE_CORE_DURATION_TICKS = 6;

    /**
     * 主星环自旋（弧度 / tick）。轻微即可，太快会不像涟漪。
     */
    public static final float SHOCKWAVE_RING_ROLL_RADIANS_PER_TICK = 0.06f;

    /**
     * 回声星环反向自旋。
     */
    public static final float SHOCKWAVE_ECHO_ROLL_RADIANS_PER_TICK = -0.08f;

    /**
     * 跟着波前走的星点数。构成「一圈星」。
     */
    public static final int SHOCKWAVE_RIDING_STAR_COUNT = 16;

    /**
     * 每 2 tick 在当前半径留下的星尘数。留在原地淡出，形成星辰涟漪的余波。
     */
    public static final int SHOCKWAVE_RESIDUE_STARS_PER_PULSE = 6;

    public static final int SHOCKWAVE_RESIDUE_INTERVAL_TICKS = 2;

    /** 余波星尘寿命（tick）。 */
    public static final int SHOCKWAVE_RESIDUE_LIFETIME_TICKS = 5;

    /** 波前星点沿圆周额外转过的角速度（弧度 / tick），让星圈微微旋开。 */
    public static final float SHOCKWAVE_STAR_SWIRL_RADIANS_PER_TICK = 0.045f;

    /** 星点 / 星尘四边形边长（方块）。 */
    public static final float SHOCKWAVE_ACCENT_QUAD_SIZE_BLOCKS = 0.11f;

    /** 径向彗星残影数量。只在爆开瞬间沿半径甩出去。 */
    public static final int SHOCKWAVE_STREAK_COUNT = 6;

    // -------------------------------------------------------------------------
    // 喷流周围粒子（配合 ribbon 星河柱）
    // -------------------------------------------------------------------------

    /**
     * 每隔多少 tick 在喷流口刷一圈周围粒子。1 = 每 tick 一圈；调大 → 更稀、更省。
     */
    public static final int JET_SURROUND_SPAWN_INTERVAL_TICKS = 2;

    /**
     * 欧拉组一圈颗数。glow / spark / mote_1 / mote_2 / impact 按槽位轮换。
     * 调大更密。
     */
    public static final int JET_EULER_PARTICLE_COUNT = 10;

    /**
     * 直线组一圈颗数。filament / impact 交替，铺在欧拉组外侧。
     */
    public static final int JET_STRAIGHT_PARTICLE_COUNT = 8;

    /**
     * 欧拉组出生圆半径（方块）。贴着以后激光的外沿。
     * 调大 → 套管更粗。
     */
    public static final float JET_EULER_RING_RADIUS_BLOCKS = 0.62f;

    /**
     * 直线组出生圆半径（方块）。略大于欧拉组，形成外层能量丝。
     */
    public static final float JET_STRAIGHT_RING_RADIUS_BLOCKS = 0.92f;

    /**
     * 圆周槽位随机偏移（弧度）。避免每圈都叠在同一组齿轮齿上。
     */
    public static final float JET_RING_SLOT_JITTER_RADIANS = 0.22f;

    /**
     * 出生半径相对标称值的随机比例下限。1 = 不抖；调低 → 圆环更毛。
     */
    public static final float JET_RING_RADIUS_RANDOM_MIN_SCALE = 0.82f;

    /**
     * 出生半径相对标称值的随机比例上限。
     */
    public static final float JET_RING_RADIUS_RANDOM_MAX_SCALE = 1.18f;

    /**
     * 欧拉组初始前向速度（方块 / tick）。调大 → 冲得更远更快。
     */
    public static final float JET_EULER_FORWARD_SPEED_BLOCKS_PER_TICK = 1.15f;

    /**
     * 欧拉组前向加速度（方块 / tick²）。显式欧拉每 tick 加到速度上，做出「冲击」感。
     * 0 = 匀速；调大 → 越飞越快。
     */
    public static final float JET_EULER_FORWARD_ACCELERATION_BLOCKS_PER_TICK_SQUARED = 0.035f;

    /**
     * 欧拉组绕喷流轴的角速度（弧度 / tick）。
     * 平面偏移用欧拉公式 {@code e^(iθ) = cosθ + i sinθ}。
     * 0.20 ≈ 18 tick 里转约 0.57 圈。
     */
    public static final float JET_EULER_HELIX_RADIANS_PER_TICK = 0.20f;

    /**
     * 欧拉组寿命（tick）。× 平均速度 ≈ 飞行距离。
     */
    public static final int JET_EULER_LIFETIME_TICKS = 18;

    /**
     * 直线组前向速度（方块 / tick）。略快于欧拉组，让丝状残影往前扯一点。
     */
    public static final float JET_STRAIGHT_FORWARD_SPEED_BLOCKS_PER_TICK = 1.28f;

    /**
     * 直线组寿命（tick）。
     */
    public static final int JET_STRAIGHT_LIFETIME_TICKS = 16;

    /**
     * 轨迹噪声振幅（方块）。加在垂直喷流的平面上，避免圆管太死。
     * 欧拉组用满值，直线组再乘 {@link #JET_STRAIGHT_NOISE_SCALE}。
     */
    public static final float JET_NOISE_AMPLITUDE_BLOCKS = 0.16f;

    /**
     * 噪声随时间变化的速率（1 / tick）。调大 → 抖得更碎。
     */
    public static final float JET_NOISE_FREQUENCY_PER_TICK = 0.38f;

    /**
     * 直线组噪声相对欧拉组的比例。1 = 一样乱；0 = 完全直线。
     */
    public static final float JET_STRAIGHT_NOISE_SCALE = 0.45f;

    /**
     * 对螺旋极角的噪声振幅（弧度）。让齿距也不均匀。
     */
    public static final float JET_NOISE_ANGLE_AMPLITUDE_RADIANS = 0.18f;

    /** 喷流光晕四边形边长（方块）。 */
    public static final float JET_GLOW_QUAD_SIZE_BLOCKS = 0.48f;

    /** 喷流火花四边形边长（方块）。 */
    public static final float JET_SPARK_QUAD_SIZE_BLOCKS = 0.11f;

    /** 喷流闪星四边形边长（方块）。 */
    public static final float JET_MOTE_QUAD_SIZE_BLOCKS = 0.09f;

    /** 喷流冲击星四边形边长（方块）。 */
    public static final float JET_IMPACT_QUAD_SIZE_BLOCKS = 0.17f;

    /** 喷流能量丝四边形边长（方块）。 */
    public static final float JET_FILAMENT_QUAD_SIZE_BLOCKS = 0.24f;

    // -------------------------------------------------------------------------
    // 星河喷流本体（ribbon 光带，不是密粒子）
    // -------------------------------------------------------------------------

    /**
     * 喷流最大射程（方块）。撞到实心方块会提前截断。
     * 调大 → 激光更长；太长会抬高每帧采样点数。
     */
    public static final double JET_BEAM_MAX_RANGE_BLOCKS = 48.0;

    /**
     * 中心轴线采样点数。越多越圆滑，但顶点数近似按比例上升。
     */
    public static final int JET_BEAM_SAMPLE_COUNT = 28;

    /**
     * 喷流口半宽（方块）。比辉石拖尾粗很多，读作星河柱而不是细彗星尾。
     */
    public static final float JET_BEAM_MOUTH_HALF_WIDTH_BLOCKS = 0.72f;

    /**
     * 远端半宽（方块）。略收一点，避免远处方块感；仍保持粗管。
     */
    public static final float JET_BEAM_TIP_HALF_WIDTH_BLOCKS = 0.48f;

    /**
     * 外层暗靛雾半宽倍率（相对中轴半宽）。调大 → 星河体积更胖。
     */
    public static final float JET_BEAM_VEIL_WIDTH_SCALE = 2.35f;

    /**
     * 中层星云半宽倍率。
     */
    public static final float JET_BEAM_NEBULA_WIDTH_SCALE = 1.35f;

    /**
     * 亮芯半宽倍率。
     */
    public static final float JET_BEAM_CORE_WIDTH_SCALE = 0.38f;

    /**
     * 螺旋细丝条数。绕中轴错相位，做出星河丝缕，而不是一根塑料棍。
     */
    public static final int JET_BEAM_FILAMENT_COUNT = 3;

    /**
     * 螺旋细丝绕轴半径（方块）。
     */
    public static final float JET_BEAM_FILAMENT_RADIUS_BLOCKS = 0.38f;

    /**
     * 螺旋细丝半宽（方块）。
     */
    public static final float JET_BEAM_FILAMENT_HALF_WIDTH_BLOCKS = 0.085f;

    /**
     * 螺旋沿程扭率（弧度 / 方块）。调大 → 丝拧得更紧。
     */
    public static final float JET_BEAM_FILAMENT_TWIST_RADIANS_PER_BLOCK = 0.22f;

    /**
     * 螺旋整体自旋（弧度 / tick）。让星河在持续喷射时缓缓转动。
     */
    public static final float JET_BEAM_FILAMENT_SPIN_RADIANS_PER_TICK = 0.07f;

    /**
     * 中轴横向波纹振幅（方块）。轻微起伏，避免死直线。
     */
    public static final float JET_BEAM_RIVER_WAVE_AMPLITUDE_BLOCKS = 0.11f;

    /**
     * 中轴波纹空间频率（1 / 方块）。
     */
    public static final float JET_BEAM_RIVER_WAVE_FREQUENCY_PER_BLOCK = 0.55f;

    /**
     * 中轴波纹时间相位速率（弧度 / tick）。
     */
    public static final float JET_BEAM_RIVER_WAVE_PHASE_RADIANS_PER_TICK = 0.12f;

    /**
     * 伤害结算圆柱半径（方块）。略小于视觉管径。
     */
    public static final float JET_BEAM_DAMAGE_RADIUS_BLOCKS = 0.85f;

    /**
     * 每隔多少 tick 结算一次射线伤害。CONTINUOUS 已按脉冲扣蓝，伤害别每 tick 满额。
     */
    public static final int JET_BEAM_DAMAGE_INTERVAL_TICKS = 4;

    /**
     * 每次伤害结算 = 法术强度 × 本系数。
     */
    public static final float JET_BEAM_DAMAGE_PER_SPELL_POWER = 0.55f;

    /**
     * 墨绿色星河配色（ARGB）。偏暗靛 / 墨绿，避免辉石拖尾那种亮青塑料感。
     * 参考：#153033 / #164458 / #218685 / #1D81A9 / #82C0C8
     */
    public static final int JET_BEAM_VEIL_COLOR_ARGB = 0x6A153033;
    public static final int JET_BEAM_NEBULA_COLOR_ARGB = 0xB0164458;
    public static final int JET_BEAM_MID_COLOR_ARGB = 0xCC218685;
    public static final int JET_BEAM_CORE_COLOR_ARGB = 0xF082C0C8;
    public static final int JET_BEAM_FILAMENT_COLOR_ARGB = 0xD01D81A9;
}

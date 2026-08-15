package com.eldenring.spells.tuning;

/**
 * 创星雨（Founding Rain of Stars）可调数值。
 * <p>
 * 本阶段只做出手星云 + 光点升空 + 头顶雨云；落星雨伤害下一步再接。改手感只改这里，
 * 不要在 {@code GlintstoneFx} / 时序实体里写魔法数字。
 */
public final class FoundingRainOfStarsTuning {

    private FoundingRainOfStarsTuning() {
    }

    /**
     * 出手瞬间铺星云的强度，传给 {@code GlintstoneFx.starRiver}。
     * 调大 → 核/雾/臂更密；调小 → 更稀薄。
     */
    public static final float CAST_NEBULA_INTENSITY = 1.65f;

    /**
     * 出手后等待多少 tick 再从星云里抽光点（tick）。
     * 星云雾气寿命约 18–28 tick、核约 16–24 tick；取 10 时云已开始淡但仍看得见。
     * 调大 → 光点更晚飞出、云更残；调小 → 几乎刚出手就升空。
     */
    public static final int ASCENT_LAUNCH_DELAY_TICKS = 10;

    /**
     * 光点从星云里错峰飞出的窗口（tick）。
     * 调大 → 更像陆续被抽走；调小 → 更齐爆。
     */
    public static final int ASCENT_STAGGER_WINDOW_TICKS = 4;

    /**
     * 整波升空光点总数。调大 → 更密的星群；调小 → 更疏、更像图标里那十几道。
     */
    public static final int ASCENT_MOTE_COUNT = 16;

    /**
     * 终点相对施法者眼睛的世界上移（方块）。
     */
    public static final double ASCENT_TARGET_HEIGHT_ABOVE_EYES_BLOCKS = 4.0;

    /**
     * 终点水平散布半径（方块）。光点不汇成一个像素，而是在头顶铺一小盘星群。
     * 调大 → 更散；调小 → 更收束。
     */
    public static final double ASCENT_TARGET_SCATTER_RADIUS_BLOCKS = 1.35;

    /**
     * 终点高度额外抖动半幅（方块）。实际 Y = 目标高度 ± 本值。
     */
    public static final double ASCENT_TARGET_HEIGHT_JITTER_BLOCKS = 0.45;

    /**
     * 单颗光点从星云飞到终点的基础寿命（tick）。调大 → 升得更慢、拖尾更长。
     */
    public static final int ASCENT_FLIGHT_DURATION_TICKS = 30;

    /**
     * 飞升寿命额外随机上限（tick）。实际寿命 = 基础 + random(0..本值)，避免同时闪灭。
     */
    public static final int ASCENT_FLIGHT_DURATION_RANDOM_TICKS = 4;

    /**
     * 升空光点出生时四边形边长（方块）。调大 → 星星更大、更抢眼。
     */
    public static final float ASCENT_MOTE_QUAD_SIZE_BLOCKS = 0.11f;

    /**
     * 升空光点尺寸额外随机（方块）。
     */
    public static final float ASCENT_MOTE_QUAD_SIZE_RANDOM_BLOCKS = 0.05f;

    /**
     * 每隔多少 tick 在光点身后刷一颗拖尾。1 = 每 tick 都留残影。
     */
    public static final int ASCENT_TRAIL_INTERVAL_TICKS = 1;

    /**
     * 拖尾粒子沿飞升方向的速度（方块/tick）。让残影略被拉长，而不是钉在原地。
     */
    public static final double ASCENT_TRAIL_STRETCH_SPEED_BLOCKS_PER_TICK = 0.04;

    /**
     * 星云圆心沿水平前方的偏移（方块）。不跟视线俯仰，避免抬头时云贴脸、低头时砸进地面。
     */
    public static final double OVERHEAD_CLOUD_FORWARD_OFFSET_BLOCKS = 3.0;

    /**
     * 头顶星云水平半宽（方块）。平铺在天空上的一层，不是一团球。
     */
    public static final double OVERHEAD_CLOUD_RADIUS_BLOCKS = 4.0;

    /**
     * 星云沿水平前方的半长（方块）。和半宽接近，铺成扁盘而不是竖着的蛋。
     */
    public static final double OVERHEAD_CLOUD_FORWARD_HALF_BLOCKS = 3.6;

    /**
     * 星云竖直厚度（方块）。只给层与层之间留一点点错开，防面片共面闪烁。
     * 调大 → 又变回球体。
     */
    public static final double OVERHEAD_CLOUD_SHEET_THICKNESS_BLOCKS = 0.12;

    /**
     * 白色闪星数量。小、密，嵌在气团里。
     */
    public static final int OVERHEAD_STAR_COUNT = 56;

    /**
     * 头顶星云存在时长（tick）。20 tick = 1 秒，60 = 3 秒。
     */
    public static final int OVERHEAD_CLOUD_LIFETIME_TICKS = 60;

    /**
     * 淡入时长（tick）。光点消失的那几帧里云从透明胀出来。
     */
    public static final int OVERHEAD_CLOUD_FADE_IN_TICKS = 8;

    /**
     * 淡出时长（tick）。调大 → 散得更慢。
     */
    public static final int OVERHEAD_CLOUD_FADE_OUT_TICKS = 12;

    /**
     * 单颗云朵寿命额外随机上限（tick）。避免整团同一帧闪灭。
     */
    public static final int OVERHEAD_CLOUD_LIFETIME_RANDOM_TICKS = 6;

    /**
     * 云朵粒子出生后的水平游走（方块/tick）。只要一点点，3 秒里挪大约一掌宽。
     */
    public static final double OVERHEAD_CLOUD_DRIFT_BLOCKS_PER_TICK = 0.004;

    public static final int SPELL_BASE_MANA_COST = 48;
    public static final int SPELL_MANA_COST_PER_LEVEL = 8;
    public static final int SPELL_BASE_SPELL_POWER = 10;
    public static final int SPELL_SPELL_POWER_PER_LEVEL = 1;

    /**
     * 吟唱时长（tick）。本阶段先瞬时出手，好单独看星云→升空；以后若改蓄力再调。
     */
    public static final int SPELL_CAST_TIME_TICKS = 0;

    public static final double SPELL_COOLDOWN_SECONDS = 8.0;

    /** 最大等级。法环辉石咒固定 1 级。 */
    public static final int SPELL_MAX_LEVEL = 1;

    /**
     * 时序实体在雨云淡尽后再活多少 tick。给下一步接落星雨留钩子。
     */
    public static final int PRELUDE_TAIL_TICKS = 2;

    /**
     * 第一批升空光点到达顶点、开始消失的 tick。
     * 等于等待 + 基础飞升；错峰只有 4 tick，看起来就是「消失的一瞬间」。
     */
    public static int overheadCloudSpawnTick() {
        return ASCENT_LAUNCH_DELAY_TICKS + ASCENT_FLIGHT_DURATION_TICKS;
    }

    /**
     * 时序实体最长寿命（tick）= 雨云出现 + 雨云寿命 + 收尾。
     */
    public static int sequenceLifetimeTicks() {
        return overheadCloudSpawnTick()
                + OVERHEAD_CLOUD_LIFETIME_TICKS
                + OVERHEAD_CLOUD_LIFETIME_RANDOM_TICKS
                + PRELUDE_TAIL_TICKS;
    }
}

package com.eldenring.spells.tuning;

/**
 * 毁灭流星「星云」视觉布局。平衡数字只改这里，{@code GlintstoneFx.starRiver} 只引用常量。
 * <p>
 * 星云锚在右手前方、与头部平齐，不沿视线拉一条粉尘螺旋（会挡准星且像脏雾）。
 */
public final class StarRiverTuning {

    private StarRiverTuning() {
    }

    /**
     * 相对眼睛、沿水平右侧的偏移（方块）。调大 → 更靠右手外侧，容易出画面；调小 → 更靠近准星。
     */
    public static final double ANCHOR_RIGHT_OFFSET_BLOCKS = 0.58;

    /**
     * 相对眼睛、沿水平前方的偏移（方块）。只把星云送到「手前面」，不要铺进视野正中。
     */
    public static final double ANCHOR_FORWARD_OFFSET_BLOCKS = 0.42;

    /**
     * 相对眼睛的世界 Y 上移（方块）。右手本身低于头，上移后与头部平齐并略高一点。
     */
    public static final double ANCHOR_UP_OFFSET_BLOCKS = 0.10;

    /**
     * 星云盘面半径（方块）。对数螺线与椭球采样都缩放到这个尺度。
     */
    public static final double NEBULA_RADIUS_BLOCKS = 0.62;

    /**
     * 沿视线的厚度相对半径的比例。调大 → 更「一团」；调小 → 更扁、更像贴在右手旁的星盘。
     */
    public static final double NEBULA_DEPTH_FRACTION = 0.38;

    /**
     * 对数螺线转过的弧度。约 1.6 圈；调大 → 臂更长、更绕。
     */
    public static final double SPIRAL_THETA_SPAN_RADIANS = Math.PI * 3.2;

    /**
     * 螺线最内圈相对半径的比例。调大 → 核更空、臂更靠外。
     */
    public static final double SPIRAL_INNER_RADIUS_FRACTION = 0.12;

    /**
     * 对数螺线生长率（无量纲）。调大 → 外圈张得更快，更像漩涡星系。
     */
    public static final double SPIRAL_GROWTH = 1.65;

    /**
     * 螺线随游戏时间旋转的角速度（弧度/tick）。调大 → 星云转得更明显。
     */
    public static final double SPIRAL_SPIN_RADIANS_PER_TICK = 0.055;

    /**
     * 旋臂沿视线方向的起伏幅度（方块）。让双臂不完全共面。
     */
    public static final double ARM_DEPTH_WEAVE_BLOCKS = 0.08;

    /**
     * 旋臂采样点相对半径的横向抖动比例。调大 → 臂更蓬、更像星云而不是线。
     */
    public static final double ARM_SCATTER_FRACTION = 0.16;

    /** intensity=1 时星云雾气体积层数量。 */
    public static final float MIST_COUNT_PER_INTENSITY = 7.0f;

    /** intensity=1 时辉光层数量。 */
    public static final float GLOW_COUNT_PER_INTENSITY = 5.0f;

    /** intensity=1 时双色星尘数量。 */
    public static final float DUST_COUNT_PER_INTENSITY = 4.0f;

    /** intensity=1 时单条旋臂的采样点数（共两条臂）。 */
    public static final float SPIRAL_SAMPLES_PER_ARM_PER_INTENSITY = 8.0f;

    /** intensity=1 时闪星数量。 */
    public static final float MOTE_COUNT_PER_INTENSITY = 9.0f;

    /** intensity=1 时暗丝数量。 */
    public static final float FILAMENT_COUNT_PER_INTENSITY = 5.0f;

    /** intensity=1 时亮星 / 双星 / 星团点缀数量。 */
    public static final float STAR_ACCENT_COUNT_PER_INTENSITY = 5.0f;

    /** intensity=1 时彗星残影数量。 */
    public static final float STREAK_COUNT_PER_INTENSITY = 3.0f;

    /**
     * 每发流星从星云里「拽出」的残影数量倍率。
     * 齐射只补发射感，不再整团重铺，避免 12 发叠成粒子风暴。
     */
    public static final float LAUNCH_STREAK_COUNT_PER_INTENSITY = 3.0f;

    public static final float LAUNCH_MOTE_COUNT_PER_INTENSITY = 2.0f;
}

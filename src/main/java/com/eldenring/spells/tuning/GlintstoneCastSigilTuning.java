package com.eldenring.spells.tuning;

/**
 * 学院辉石法阵的空间 / 外观数值（大小、高度、不透明度）。
 * <p>
 * 寿命、淡出写死在粒子里；法术侧只调 {@link com.eldenring.spells.sigil.AcademySigilFx#spawnAboveHead}。
 */
public final class GlintstoneCastSigilTuning {

    private GlintstoneCastSigilTuning() {
    }

    /**
     * 峰值不透明度（0–1）。贴图本身已有 alpha，此值再乘一层。
     * 调小 → 更通透；调大 → 纹章更实、更亮。
     */
    public static final float PEAK_ALPHA = 0.96f;

    /**
     * 四边形半边长（方块）。视觉宽度约为 {@code 2 * 本值}。
     * 调大 → 头顶法阵更大；调小 → 更像一枚小徽记。
     */
    public static final float QUAD_HALF_SIZE_BLOCKS = 0.5f;

    /**
     * 相对碰撞箱顶端再抬高的距离（方块），避免徽记嵌进头发/头盔。
     * 调大 → 更高、第三人称更醒目；调小 → 更贴头。
     */
    public static final float HEAD_Y_OFFSET_BLOCKS = 0.8f;
}

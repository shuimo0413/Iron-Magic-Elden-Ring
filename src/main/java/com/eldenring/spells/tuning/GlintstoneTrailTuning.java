package com.eldenring.spells.tuning;

/**
 * 辉石弹道连续光轨共用约定。
 * <p>
 * 主体拖尾由客户端几何光束绘制（类似闪电/信标的连续线段），不再依赖密集粒子采样。
 * 每个法术通过 {@link TrailStyle} 独立配置长度、头尾半宽与粒子点缀概率。
 */
public final class GlintstoneTrailTuning {

    private GlintstoneTrailTuning() {
    }

    /**
     * 光束外层相对内层光芯的半宽倍率。
     * 调大 → 外辉光更胖；调小 → 更接近细针。
     */
    public static final float BEAM_OUTER_WIDTH_SCALE = 1.85f;

    /**
     * 光束外层相对内层的不透明度倍率（叠在法术颜色 alpha 上）。
     */
    public static final float BEAM_OUTER_ALPHA_SCALE = 0.38f;

    /**
     * 粒子出生点沿飞行反方向后移的最小距离（方块）。
     * 调大 → 粒子与彗星头分离更明显；调小 → 粒子更集中在晶核周围。
     */
    public static final double PARTICLE_TRAIL_MINIMUM_BACK_OFFSET_BLOCKS = 0.10;

    /**
     * 粒子出生点沿飞行反方向后移的随机距离（方块）。
     * 实际后移量为最小距离加上此范围内随机值。
     */
    public static final double PARTICLE_TRAIL_RANDOM_BACK_OFFSET_BLOCKS = 0.55;

    /**
     * 每 tick 沿弹道实际位移线段生成的基础粒子数。
     * 调大 → 整条飞行轨迹更密；调小 → 粒子更稀疏、性能开销更低。
     */
    public static final int PARTICLE_PATH_BASE_SAMPLE_COUNT = 2;

    /**
     * 每点拖尾强度额外增加的线段采样数。
     * 最终数量约为 {@code 基础数量 + intensity × 本值}。
     */
    public static final float PARTICLE_PATH_SAMPLE_COUNT_PER_INTENSITY = 2.0f;

    /** 每颗彗星每 tick 的轨迹粒子上限，避免极高强度造成粒子爆炸。 */
    public static final int PARTICLE_PATH_MAXIMUM_SAMPLE_COUNT = 8;

    /**
     * 光晕每 tick 的基础生成概率；强度倍率会继续提高概率。
     * 光晕负责让飞行中的彗星拥有连续但不过厚的能量残影。
     */
    public static final float PARTICLE_GLOW_BASE_CHANCE = 0.34f;

    /** 光晕概率随拖尾粒子强度增加的系数。 */
    public static final float PARTICLE_GLOW_CHANCE_PER_INTENSITY = 0.24f;

    /**
     * 薄雾每 tick 的基础生成概率；只使用缩小后的粒子，避免遮住曲线光带。
     */
    public static final float PARTICLE_MIST_BASE_CHANCE = 0.05f;

    /** 薄雾概率随拖尾粒子强度增加的系数。 */
    public static final float PARTICLE_MIST_CHANCE_PER_INTENSITY = 0.10f;

    /**
     * 碎晶每 tick 的基础生成概率；高速离散碎片用于打破纯光带的单调轮廓。
     */
    public static final float PARTICLE_SHARD_BASE_CHANCE = 0.025f;

    /** 碎晶概率随拖尾粒子强度增加的系数。 */
    public static final float PARTICLE_SHARD_CHANCE_PER_INTENSITY = 0.07f;

    /**
     * 单个法术的连续光轨外观。长度与半宽单位均为方块。
     *
     * @param lengthBlocks 客户端保留历史轨迹的最大累计长度（方块）
     * @param headHalfWidthBlocks 弹头处半宽；决定光轨主体粗细
     * @param tailHalfWidthBlocks 尾端半宽；应明显小于头部以形成收尖
     * @param sparkChance 弹头附近辉石火花生成概率（0–1）；仅点缀，不构成光带
     * @param moteChance 弹头附近闪星生成概率（0–1）
     * @param maximumHistoryPointCount 历史路径最大点数；调大曲线更长但顶点开销上升
     */
    public record TrailStyle(
            double lengthBlocks,
            float headHalfWidthBlocks,
            float tailHalfWidthBlocks,
            float sparkChance,
            float moteChance,
            int maximumHistoryPointCount
    ) {
    }
}

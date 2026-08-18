package com.eldenring.spells.tuning;

/**
 * 辉石弹道连续光轨共用约定。
 * <p>
 * 主体拖尾由客户端几何光束绘制（类似闪电/信标的连续线段），不再依赖密集粒子采样。
 * 每个法术通过 {@link TrailStyle} 独立配置长度、半宽、螺旋细丝与粒子点缀概率。
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
     * 帚星额外外雾带相对头部半宽的倍率。调大 → 彗尾更蓬。
     */
    public static final float BEAM_EXTRA_VEIL_WIDTH_SCALE = 3.15f;

    /**
     * 额外外雾带不透明度倍率（叠在 glow alpha 上）。
     */
    public static final float BEAM_EXTRA_VEIL_ALPHA_SCALE = 0.22f;

    /**
     * 粒子出生点沿飞行反方向后移的最小距离（方块）。
     * 调大 → 粒子与彗星头分离更明显；调小 → 粒子更集中在晶核周围。
     */
    public static final double PARTICLE_TRAIL_MINIMUM_BACK_OFFSET_BLOCKS = 0.10;

    /**
     * 粒子出生点沿飞行反方向后移的随机距离（方块）。
     * 实际后移量为最小距离加上此范围内随机值。
     */
    public static final double PARTICLE_TRAIL_RANDOM_BACK_OFFSET_BLOCKS = 0.40;

    /**
     * 光晕每 tick 的基础生成概率。体积已由几何光带承担，只作弹头点缀。
     */
    public static final float PARTICLE_GLOW_BASE_CHANCE = 0.10f;

    /** 光晕概率随拖尾粒子强度增加的系数。 */
    public static final float PARTICLE_GLOW_CHANCE_PER_INTENSITY = 0.06f;

    /**
     * 薄雾每 tick 的基础生成概率。必须很低，避免遮住曲线光带。
     */
    public static final float PARTICLE_MIST_BASE_CHANCE = 0.02f;

    /** 薄雾概率随拖尾粒子强度增加的系数。 */
    public static final float PARTICLE_MIST_CHANCE_PER_INTENSITY = 0.03f;

    /**
     * 碎晶每 tick 的基础生成概率。
     */
    public static final float PARTICLE_SHARD_BASE_CHANCE = 0.015f;

    /** 碎晶概率随拖尾粒子强度增加的系数。 */
    public static final float PARTICLE_SHARD_CHANCE_PER_INTENSITY = 0.04f;

    /**
     * 沿历史路径缠绕的螺旋细丝。条数为 0 时不画。
     *
     * @param filamentCount           细丝条数；0 = 关闭
     * @param headRadiusBlocks        弹头处螺旋半径（方块）；调大 → 彗尾更开
     * @param tailRadiusBlocks        旧尾处螺旋半径（方块）；应小于头部以收成扫帚
     * @param halfWidthBlocks         细丝本身半宽（方块）
     * @param twistRadiansPerBlock    沿路径扭率（弧度 / 方块）
     * @param spinRadiansPerTick      整体绕轴自旋（弧度 / tick）
     */
    public record HelixStyle(
            int filamentCount,
            float headRadiusBlocks,
            float tailRadiusBlocks,
            float halfWidthBlocks,
            float twistRadiansPerBlock,
            float spinRadiansPerTick
    ) {
        public static final HelixStyle NONE = new HelixStyle(0, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);

        public boolean enabled() {
            return filamentCount > 0;
        }
    }

    /**
     * 单个法术的连续光轨外观。长度与半宽单位均为方块。
     *
     * @param lengthBlocks              客户端保留历史轨迹的最大累计长度（方块）
     * @param headHalfWidthBlocks       弹头处半宽；决定光轨主体粗细
     * @param tailHalfWidthBlocks       尾端半宽；应明显小于头部以形成收尖
     * @param sparkChance               弹头附近辉石火花生成概率（0–1）；仅点缀，不构成光带
     * @param moteChance                弹头附近闪星生成概率（0–1）
     * @param maximumHistoryPointCount  历史路径最大点数；调大曲线更长但顶点开销上升
     * @param helixStyle                沿曲线的螺旋细丝；无细丝用 {@link HelixStyle#NONE}
     * @param additiveCore              true 时内层光芯走加法混合，看起来更亮
     * @param extraOuterVeil            true 时再叠一层更宽更淡的外雾（帚星彗尾）
     */
    public record TrailStyle(
            double lengthBlocks,
            float headHalfWidthBlocks,
            float tailHalfWidthBlocks,
            float sparkChance,
            float moteChance,
            int maximumHistoryPointCount,
            HelixStyle helixStyle,
            boolean additiveCore,
            boolean extraOuterVeil
    ) {
        /**
         * 旧六参构造：无螺旋细丝、半透明光芯、无额外外雾。
         * 流星 / 旋飞 / 创星雨等未改剪影的法术继续走这里。
         */
        public TrailStyle(
                double lengthBlocks,
                float headHalfWidthBlocks,
                float tailHalfWidthBlocks,
                float sparkChance,
                float moteChance,
                int maximumHistoryPointCount
        ) {
            this(
                    lengthBlocks,
                    headHalfWidthBlocks,
                    tailHalfWidthBlocks,
                    sparkChance,
                    moteChance,
                    maximumHistoryPointCount,
                    HelixStyle.NONE,
                    false,
                    false
            );
        }
    }
}

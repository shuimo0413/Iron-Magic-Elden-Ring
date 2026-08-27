package com.eldenring.spells.spell.curve;

import net.minecraft.util.Mth;

/**
 * 海摩大槌时间轴与抡砸姿态。全部写死，不进 toml。
 * <p>
 * 伤害半径 / 击退读 {@link com.eldenring.spells.spell.GavelOfHaimaSpell}；本类只回答举起 → 下砸 → 淡出。
 */
public final class GavelOfHaimaCastCurve {

    /** 实体总寿命（tick）。到期 discard。 */
    public static final int ENTITY_LIFETIME_TICKS = 28;

    /**
     * 命中结算 tick（从生成起算）。应对渲染下砸动画的触地瞬间。
     */
    public static final int IMPACT_TICK = 12;

    /**
     * 开始下砸的 tick。此前保持竖直握持。
     */
    public static final int SWING_START_TICK = 5;

    /**
     * 砸地后仍短暂跟手的 tick 数（相对 IMPACT_TICK 之后）。
     */
    public static final int FOLLOW_OWNER_AFTER_IMPACT_TICKS = 0;

    /**
     * 握点相对脚底向前（方块）。
     */
    public static final double GRIP_FORWARD_OFFSET_BLOCKS = 0.55;

    /**
     * 握点相对身体中线向右（方块，主手侧）。
     */
    public static final double GRIP_RIGHT_OFFSET_BLOCKS = 0.42;

    /**
     * 握点相对脚底高度（方块）。约腰侧/右手高度。
     */
    public static final double GRIP_HEIGHT_BLOCKS = 0.95;

    /**
     * 砸地点相对施法者水平前移（方块）。伤害中心用这个，不跟握点重合。
     */
    public static final double IMPACT_FORWARD_OFFSET_BLOCKS = 1.6;

    /**
     * 贴地时竖直搜索最大步数（方块）。
     */
    public static final int GROUND_SNAP_MAX_STEPS = 8;

    /** 竖直握持时绕 X 轴俯仰（度）。0 = 柄竖直朝上。 */
    public static final float HAMMER_RAISED_PITCH_DEGREES = 0.0f;

    /**
     * 砸地瞬间绕 X 轴俯仰（度）。正值 = 锤头向前下方抡出。
     */
    public static final float HAMMER_SLAMMED_PITCH_DEGREES = 112.0f;

    /**
     * 模型锤头中心相对枢轴的长度（方块），仅用于客户端点缀粒子估位置。
     */
    public static final double HEAD_LENGTH_ALONG_HANDLE_BLOCKS = 1.40;

    private GavelOfHaimaCastCurve() {
    }

    /**
     * 下砸插值进度 0–1：先慢后快，砸地更有分量。
     */
    public static float swingProgress(float ageTicks) {
        if (ageTicks <= SWING_START_TICK) {
            return 0.0f;
        }
        if (ageTicks >= IMPACT_TICK) {
            return 1.0f;
        }
        float swingDurationTicks = IMPACT_TICK - SWING_START_TICK;
        float linear = (ageTicks - SWING_START_TICK) / swingDurationTicks;
        return linear * linear;
    }

    /**
     * 砸地后淡出：1 = 完全不透明，0 = 消失。
     */
    public static float fadeAlpha(float ageTicks) {
        if (ageTicks <= IMPACT_TICK) {
            return 1.0f;
        }
        float fadeSpan = ENTITY_LIFETIME_TICKS - IMPACT_TICK;
        return Mth.clamp(1.0f - (ageTicks - IMPACT_TICK) / fadeSpan, 0.0f, 1.0f);
    }

    public static boolean shouldFollowOwner(int tickCount) {
        return tickCount <= IMPACT_TICK + FOLLOW_OWNER_AFTER_IMPACT_TICKS;
    }
}

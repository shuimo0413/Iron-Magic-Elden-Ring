package com.eldenring.spells.spell.curve;

import net.minecraft.util.Mth;

/**
 * 卡利亚迅剑时间轴与挥砍姿态。全部写死，不进 toml。
 * <p>
 * 单刀周期（toml）决定何时接下刀；本类只回答「这一刀挥到哪、何时命中、何时淡出」。
 */
public final class CarianSlicerCastCurve {

    /**
     * 相对本刀起点，开始挥动的 tick。0 = 立刻抡。
     */
    public static final int SWING_START_TICK = 0;

    /**
     * 相对本刀起点的命中结算 tick。应对挥砍弧线扫过身前的瞬间。
     */
    public static final int HIT_TICK = 5;

    /**
     * 最后一刀收完后的淡出时长（tick）。调大 → 剑在空中停更久。
     */
    public static final int SLASH_FADE_TICKS = 4;

    /**
     * 客户端：进入吟唱后这么多 tick 里从没见到施法键按住，就当点按，只砍一刀。
     */
    public static final int HOLD_CANCEL_GRACE_TICKS = 4;

    /**
     * 服务端：超过这么多 tick 没收到「仍在吟唱」刷新，就不再连下一刀。
     */
    public static final int HOLD_STALE_TICKS = 2;

    /**
     * 安全寿命余量（tick）。正常由松手/没蓝结束；这是防止实体残留。
     */
    public static final int ENTITY_LIFETIME_PADDING_TICKS = 20;

    /** 正手预备绕本地 Z 轴滚转（度）。正值 = 剑趴向右侧。 */
    public static final float SWORD_START_ROLL_DEGREES = 78.0f;

    /** 正手斩完绕本地 Z 轴滚转（度）。负值 = 剑趴到左侧。 */
    public static final float SWORD_END_ROLL_DEGREES = -82.0f;

    /** 正手预备绕本地 X 轴俯仰（度）。 */
    public static final float SWORD_START_PITCH_DEGREES = 32.0f;

    /** 正手斩完绕本地 X 轴俯仰（度）。 */
    public static final float SWORD_END_PITCH_DEGREES = 48.0f;

    /**
     * 握点相对脚底向前（方块）。略收一点，避免剑身顶在准星上。
     */
    public static final double GRIP_FORWARD_OFFSET_BLOCKS = 0.32;

    /**
     * 握点相对身体中线向右（方块，主手侧）。
     */
    public static final double GRIP_RIGHT_OFFSET_BLOCKS = 0.48;

    /**
     * 握点相对脚底高度（方块）。
     */
    public static final double GRIP_HEIGHT_BLOCKS = 0.92;

    /**
     * 剑刃沿本地 +Y 的长度（方块，含缩放），供斩击光弧外沿和粒子估尖端。
     */
    public static final double BLADE_LENGTH_BLOCKS = 1.28;

    private CarianSlicerCastCurve() {
    }

    /**
     * 实体安全寿命上限（tick）= 最长按住时间 + 淡出 + 余量。
     */
    public static int entityMaxLifetimeTicks(int maxCastTimeTicks) {
        return maxCastTimeTicks + SLASH_FADE_TICKS + ENTITY_LIFETIME_PADDING_TICKS;
    }

    /**
     * 当前这一刀的挥砍进度 0–1。先快后收，迅剑要有「一下子砍过去」的手感。
     */
    public static float swingProgress(float swingAgeTicks) {
        if (swingAgeTicks <= SWING_START_TICK) {
            return 0.0f;
        }
        if (swingAgeTicks >= HIT_TICK) {
            return 1.0f;
        }
        float swingDurationTicks = HIT_TICK - SWING_START_TICK;
        if (swingDurationTicks <= 1.0e-4f) {
            return 1.0f;
        }
        float linear = (swingAgeTicks - SWING_START_TICK) / swingDurationTicks;
        return 1.0f - (1.0f - linear) * (1.0f - linear);
    }

    /**
     * 斩完淡出：1 = 完全不透明，0 = 消失。
     */
    public static float fadeAlpha(float fadeAgeTicks) {
        if (SLASH_FADE_TICKS <= 1.0e-4f) {
            return 0.0f;
        }
        return Mth.clamp(1.0f - fadeAgeTicks / SLASH_FADE_TICKS, 0.0f, 1.0f);
    }

    public static boolean isHitWindow(int swingAgeTicks, boolean alreadyResolved) {
        return !alreadyResolved && swingAgeTicks >= HIT_TICK;
    }

    public static float startRollDegrees(boolean backhandSlash) {
        return backhandSlash ? SWORD_END_ROLL_DEGREES : SWORD_START_ROLL_DEGREES;
    }

    public static float endRollDegrees(boolean backhandSlash) {
        return backhandSlash ? SWORD_START_ROLL_DEGREES : SWORD_END_ROLL_DEGREES;
    }

    public static float startPitchDegrees(boolean backhandSlash) {
        return backhandSlash ? SWORD_END_PITCH_DEGREES : SWORD_START_PITCH_DEGREES;
    }

    public static float endPitchDegrees(boolean backhandSlash) {
        return backhandSlash ? SWORD_START_PITCH_DEGREES : SWORD_END_PITCH_DEGREES;
    }
}

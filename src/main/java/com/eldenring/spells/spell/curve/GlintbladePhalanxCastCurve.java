package com.eldenring.spells.spell.curve;

import net.minecraft.util.Mth;

/**
 * 辉剑圆阵时间轴与半圆视觉。全部写死，不进 toml。
 * <p>
 * 玩法上的「附近几格才射」「跟手多久消失」仍在 {@code GlintbladePhalanxSpell}。
 * 半圆几何在 {@link com.eldenring.spells.spell.helper.GlintbladePhalanxHelper}。
 */
public final class GlintbladePhalanxCastCurve {

    /**
     * 半圆半径（方块）。圆心是眼睛。
     * 调大 → 剑离头更远、第三人称更像「罩」；调小 → 更贴头、第一人称两侧更挤。
     */
    public static final double ORBIT_RADIUS_BLOCKS = 1.08;

    /**
     * 相对头心再沿水平前向挪的距离（方块）。
     * 调大 → 剑更在脸前、第三人称更好看见；调小 → 更贴头、不易穿墙。
     */
    public static final double ORBIT_FORWARD_OFFSET_BLOCKS = 0.22;

    /**
     * 出现时由小变大的时长（tick）。到期后才允许索敌射出。
     * 调大 → 更能看清半圆成型；调小 → 落地就能打。
     */
    public static final int APPEAR_TICKS = 8;

    /**
     * 相邻两把剑允许射出的错峰（tick）。
     * 乘槽位序号：左肩先、头顶后、右肩更后，避免五把同一 tick 叠在同一点。
     * 调大 → 连射更疏；调小 → 更接近同时出手。
     */
    public static final int LAUNCH_STAGGER_TICKS = 4;

    /**
     * 射出后最长飞行（tick）。跟手阶段不走这条，超时在 Spell 的 hover 寿命里。
     * 调大 → 追得更久；调小 → 很快自己碎。
     */
    public static final int FLIGHT_LIFETIME_TICKS = 72;

    /**
     * 相对魔法辉剑网格的视觉倍率。辉剑圆阵 / 卡利亚圆阵都是 1。
     * 巨剑阵用 {@link #GREATBLADE_SWORD_VISUAL_SCALE} 原地放大，不另做模型。
     */
    public static final float SWORD_VISUAL_SCALE = 1.0f;

    /**
     * 巨剑阵模型倍率。调大 → 三把剑更像「大剑」；调小 → 更接近辉剑圆阵。
     * 只改渲染 / 命中外扩，不进 toml。
     */
    public static final float GREATBLADE_SWORD_VISUAL_SCALE = 1.9f;

    private GlintbladePhalanxCastCurve() {
    }

    /**
     * 跟手阶段剑模型缩放 0–1。出现段二次缓出，之后钉在 1。
     */
    public static float swordScale(float ageTicks) {
        if (ageTicks >= APPEAR_TICKS) {
            return 1.0f;
        }
        if (ageTicks <= 0.0f) {
            return 0.0f;
        }
        float linear = Mth.clamp(ageTicks / (float) APPEAR_TICKS, 0.0f, 1.0f);
        return 1.0f - (1.0f - linear) * (1.0f - linear);
    }

    /**
     * 这一把剑最早允许射出的 tick（含出现 + 槽位错峰）。
     */
    public static int readyToLaunchTick(int slotIndex) {
        return APPEAR_TICKS + Math.max(0, slotIndex) * LAUNCH_STAGGER_TICKS;
    }
}

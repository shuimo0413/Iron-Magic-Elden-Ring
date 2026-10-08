package com.eldenring.spells.spell.helper;

import net.minecraft.world.entity.LivingEntity;

/**
 * 冰系命中后的原版结霜：把目标冻结槽顶满并多留指定秒数，期间吃原版冻伤扣血、结霜画面与减速。
 * <p>
 * 只碰原版冻结槽，不上铁魔法 {@code CHILLED}，所以不会单独触发冰牢；
 * 别的来源（铁魔法雪球 / 冰霜波）先挂上 {@code CHILLED} 时仍可按原版连招冻住。
 */
public final class FrostHelper {

    /** 原版不在细雪里时冻结槽每 tick 回落的量（{@code LivingEntity.aiStep}）。 */
    private static final int VANILLA_FROZEN_TICKS_DECAY_PER_TICK = 2;

    private static final int TICKS_PER_SECOND = 20;

    private FrostHelper() {
    }

    /**
     * 让目标保持完全冻结约 {@code frostSeconds} 秒。只往上抬不往下压，连续命中只刷新不叠加。
     *
     * @param frostSeconds 完全冻结持续秒数；≤0 时不做任何事
     */
    public static void applyFrost(LivingEntity target, int frostSeconds) {
        if (frostSeconds <= 0 || !target.isAlive() || !target.canFreeze()) {
            return;
        }
        int requiredFrozenTicks = target.getTicksRequiredToFreeze()
                + frostSeconds * TICKS_PER_SECOND * VANILLA_FROZEN_TICKS_DECAY_PER_TICK;
        if (target.getTicksFrozen() < requiredFrozenTicks) {
            target.setTicksFrozen(requiredFrozenTicks);
        }
    }
}

package com.eldenring.spells.item;

import com.eldenring.spells.spell.cost.SpellManaCostPolicy;

/**
 * 旧测试兼容入口；运行时代码使用通用的 {@link SpellManaCostPolicy}。
 */
@Deprecated
public final class AzurManaCostPolicy {
    private AzurManaCostPolicy() {
    }

    public static double effectiveMultiplier(boolean mainhandAzur, double configured, double castStart) {
        return Math.max(mainhandAzur ? configured : 1.0D, castStart);
    }

    public static boolean chargesMana(boolean consumesMana, boolean recast, boolean creative,
                                      boolean creativeConsumesMana) {
        return SpellManaCostPolicy.chargesMana(consumesMana, recast, creative, creativeConsumesMana);
    }

    public static int apply(int original, double multiplier, boolean chargesMana) {
        return SpellManaCostPolicy.apply(original, multiplier, chargesMana);
    }

    public static boolean canPay(float mana, int cost, boolean chargesMana) {
        return SpellManaCostPolicy.canPay(mana, cost, chargesMana);
    }
}

package com.eldenring.spells.spell.cost;

/**
 * 所有装备共用的法力消耗计算规则，保持服务端扣费与客户端预览一致。
 */
public final class SpellManaCostPolicy {
    private SpellManaCostPolicy() {
    }

    public static boolean chargesMana(boolean consumesMana, boolean recast, boolean creative,
                                      boolean creativeConsumesMana) {
        return consumesMana && !recast && (!creative || creativeConsumesMana);
    }

    public static int apply(int originalCost, double multiplier, boolean chargesMana) {
        if (!chargesMana || originalCost <= 0 || multiplier <= 0.0D) {
            return originalCost;
        }
        double scaledCost = Math.ceil(Math.nextDown(originalCost * multiplier));
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1.0D, scaledCost));
    }

    public static boolean canPay(float currentMana, int manaCost, boolean chargesMana) {
        return !chargesMana || currentMana >= manaCost;
    }
}

package com.eldenring.spells.item;

/** Pure cost arithmetic shared by runtime code and the offline regression test. */
public final class AzurManaCostPolicy {
    private AzurManaCostPolicy() {
    }

    public static double effectiveMultiplier(boolean mainhandAzur, double configured, double castStart) {
        return Math.max(mainhandAzur ? configured : 1.0D, castStart);
    }

    public static boolean chargesMana(boolean consumesMana, boolean recast, boolean creative,
                                      boolean creativeConsumesMana) {
        return consumesMana && !recast && (!creative || creativeConsumesMana);
    }

    public static int apply(int original, double multiplier, boolean chargesMana) {
        if (!chargesMana || original <= 0 || multiplier <= 1.0D) {
            return original;
        }
        // Do not round a binary floating-point value infinitesimally above an integer up twice.
        double scaled = Math.ceil(Math.nextDown(original * multiplier));
        return (int) Math.min(Integer.MAX_VALUE, Math.max(original, scaled));
    }

    public static boolean canPay(float mana, int cost, boolean chargesMana) {
        return !chargesMana || mana >= cost;
    }
}

package com.eldenring.spells.item;

/** Runs standalone via javac/java, and from the equipment GameTest suite without an external framework. */
public final class AzurManaCostPolicyTest {
    private static int assertions;

    public static void main(String[] args) {
        equal(12, AzurManaCostPolicy.apply(10, 1.2D, true), "normal surcharge");
        equal(9, AzurManaCostPolicy.apply(7, 1.2D, true), "fraction rounds up");
        equal(18, AzurManaCostPolicy.apply(15, 1.2D, true), "integer rounding");
        equal(0, AzurManaCostPolicy.apply(0, 1.2D, true), "zero-cost spell");
        equal(10, AzurManaCostPolicy.apply(10, 1.2D, false), "non-consuming source");
        equal(10, AzurManaCostPolicy.apply(10, 1.0D, true), "disabled surcharge");
        equal(Integer.MAX_VALUE, AzurManaCostPolicy.apply(Integer.MAX_VALUE, 10.0D, true), "overflow clamp");
        for (int base = 1; base <= 10000; base++) {
            equal((base * 6 + 4) / 5, AzurManaCostPolicy.apply(base, 1.2D, true), "20 percent rounding " + base);
        }
        check(!AzurManaCostPolicy.chargesMana(false, false, false, false), "scroll exemption");
        check(!AzurManaCostPolicy.chargesMana(true, true, false, false), "recast exemption");
        check(!AzurManaCostPolicy.chargesMana(true, false, true, false), "creative exemption");
        check(AzurManaCostPolicy.chargesMana(true, false, true, true), "configured creative cost");
        check(AzurManaCostPolicy.chargesMana(true, false, false, false), "survival cost");
        equal(12, AzurManaCostPolicy.apply(10,
                AzurManaCostPolicy.effectiveMultiplier(false, 1.2D, 1.2D), true), "switch away midcast");
        equal(12, AzurManaCostPolicy.apply(10,
                AzurManaCostPolicy.effectiveMultiplier(true, 1.2D, 1.0D), true), "switch to staff midcast");
        equal(10, AzurManaCostPolicy.apply(10,
                AzurManaCostPolicy.effectiveMultiplier(false, 1.2D, 1.0D), true), "offhand or next cast");
        check(!AzurManaCostPolicy.canPay(11.0F, 12, true), "reject partial payment at completion");
        check(AzurManaCostPolicy.canPay(12.0F, 12, true), "exact payment");
        check(AzurManaCostPolicy.canPay(0.0F, 12, false), "exempt affordability");
        System.out.println("AzurManaCostPolicyTest: " + assertions + " assertions passed");
    }

    private static void equal(int expected, int actual, String label) {
        check(expected == actual, label + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean result, String label) {
        assertions++;
        if (!result) {
            throw new AssertionError(label);
        }
    }
}

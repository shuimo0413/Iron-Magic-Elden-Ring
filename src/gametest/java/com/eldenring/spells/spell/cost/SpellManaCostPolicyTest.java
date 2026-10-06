package com.eldenring.spells.spell.cost;

/** Runs standalone via javac/java, and from the equipment GameTest suite without an external framework. */
public final class SpellManaCostPolicyTest {
    private static int assertions;

    public static void main(String[] args) {
        equal(12, SpellManaCostPolicy.apply(10, 1.2D, true), "normal surcharge");
        equal(9, SpellManaCostPolicy.apply(7, 1.2D, true), "fraction rounds up");
        equal(18, SpellManaCostPolicy.apply(15, 1.2D, true), "integer rounding");
        equal(0, SpellManaCostPolicy.apply(0, 1.2D, true), "zero-cost spell");
        equal(10, SpellManaCostPolicy.apply(10, 1.2D, false), "non-consuming source");
        equal(10, SpellManaCostPolicy.apply(10, 1.0D, true), "disabled surcharge");
        equal(Integer.MAX_VALUE, SpellManaCostPolicy.apply(Integer.MAX_VALUE, 10.0D, true), "overflow clamp");
        for (int base = 1; base <= 10000; base++) {
            equal((base * 6 + 4) / 5, SpellManaCostPolicy.apply(base, 1.2D, true), "20 percent rounding " + base);
        }
        check(!SpellManaCostPolicy.chargesMana(false, false, false, false), "scroll exemption");
        check(!SpellManaCostPolicy.chargesMana(true, true, false, false), "recast exemption");
        check(!SpellManaCostPolicy.chargesMana(true, false, true, false), "creative exemption");
        check(SpellManaCostPolicy.chargesMana(true, false, true, true), "configured creative cost");
        check(SpellManaCostPolicy.chargesMana(true, false, false, false), "survival cost");
        check(!SpellManaCostPolicy.canPay(11.0F, 12, true), "reject partial payment at completion");
        check(SpellManaCostPolicy.canPay(12.0F, 12, true), "exact payment");
        check(SpellManaCostPolicy.canPay(0.0F, 12, false), "exempt affordability");
        System.out.println("SpellManaCostPolicyTest: " + assertions + " assertions passed");
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

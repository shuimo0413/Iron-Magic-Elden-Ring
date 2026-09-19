package com.eldenring.spells.item.talisman;

import com.eldenring.spells.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 源辉石刀的独立玩法效果：提供生命上限减益数值，并检测护符蓝耗减免。
 */
public final class PrimalGlintstoneBladeEffect {
    private static volatile double maxHealthReduction;
    private static volatile double manaCostReduction;
    private static volatile double spellPowerBonus;

    private PrimalGlintstoneBladeEffect() {
    }

    public static void configure(double configuredMaxHealthReduction, double configuredManaCostReduction, double configuredSpellPowerBonus) {
        maxHealthReduction = configuredMaxHealthReduction;
        manaCostReduction = configuredManaCostReduction;
        spellPowerBonus = configuredSpellPowerBonus;
    }

    public static void reset() {
        maxHealthReduction = 0.0D;
        manaCostReduction = 0.0D;
        spellPowerBonus = 0.0D;
    }

    public static double maxHealthReduction() {
        return maxHealthReduction;
    }

    public static double spellPowerBonus() {
        return spellPowerBonus;
    }

    public static double currentManaCostMultiplier(Player player) {
        return isEquipped(player) ? 1.0D - manaCostReduction : 1.0D;
    }

    private static boolean isEquipped(Player player) {
        return player != null && CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.isEquipped(ModItems.PRIMAL_GLINTSTONE_BLADE.get()))
                .orElse(false);
    }
}

package com.eldenring.spells.spell.cost;

import com.eldenring.spells.item.AzurStaffBalance;
import com.eldenring.spells.item.talisman.PrimalGlintstoneBladeEffect;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import net.minecraft.world.entity.player.Player;

/**
 * 汇总法杖、护符等彼此独立的蓝耗倍率，并为铁魔法扣费入口提供统一结果。
 */
public final class SpellManaCostCalculator {
    private SpellManaCostCalculator() {
    }

    public static double currentMultiplier(Player player) {
        return AzurStaffBalance.currentMultiplier(player)
                * PrimalGlintstoneBladeEffect.currentManaCostMultiplier(player);
    }

    public static boolean chargesMana(Player player, CastSource source, AbstractSpell spell, MagicData data) {
        return SpellManaCostPolicy.chargesMana(
                source.consumesMana(),
                data.getPlayerRecasts().hasRecastForSpell(spell.getSpellId()),
                player.isCreative(),
                ServerConfigs.CREATIVE_MANA_COST.get()
        );
    }

    public static double multiplier(Player player, MagicData data, AbstractSpell spell, boolean activeCast) {
        if (activeCast && data.isCasting() && spell.getSpellId().equals(data.getCastingSpellId())) {
            return ((CastManaCostData) data).eldenRingSpells$getManaCostMultiplier();
        }
        return currentMultiplier(player);
    }

    public static int manaCost(int originalCost, Player player, CastSource source, AbstractSpell spell,
                               MagicData data, boolean activeCast) {
        return SpellManaCostPolicy.apply(
                originalCost,
                multiplier(player, data, spell, activeCast),
                chargesMana(player, source, spell, data)
        );
    }
}

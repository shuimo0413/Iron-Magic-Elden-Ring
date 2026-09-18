package com.eldenring.spells.client;

import com.eldenring.spells.spell.cost.SpellManaCostCalculator;
import com.eldenring.spells.spell.cost.SpellManaCostPolicy;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * 使用本地玩家已同步的装备与配置，计算法术轮盘、卷轴和铭刻界面的显示蓝耗。
 */
public final class SpellManaCostClientCosts {
    private SpellManaCostClientCosts() {
    }

    public static int manaCost(int originalCost, AbstractSpell spell, CastSource source) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return originalCost;
        }
        boolean chargesMana = SpellManaCostPolicy.chargesMana(
                source.consumesMana(),
                ClientMagicData.getRecasts().hasRecastForSpell(spell.getSpellId()),
                player.isCreative(),
                ServerConfigs.CREATIVE_MANA_COST.get()
        );
        return SpellManaCostPolicy.apply(
                originalCost,
                SpellManaCostCalculator.currentMultiplier(player),
                chargesMana
        );
    }
}

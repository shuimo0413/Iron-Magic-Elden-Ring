package com.eldenring.spells.client;

import com.eldenring.spells.item.AzurManaCostPolicy;
import com.eldenring.spells.item.AzurStaffBalance;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Client-only previews use the local player's synchronized config and held equipment. */
public final class AzurStaffClientCosts {
    private AzurStaffClientCosts() {
    }

    public static int manaCost(int original, AbstractSpell spell, CastSource source) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return original;
        }
        boolean charges = AzurManaCostPolicy.chargesMana(source.consumesMana(),
                ClientMagicData.getRecasts().hasRecastForSpell(spell.getSpellId()), player.isCreative(),
                ServerConfigs.CREATIVE_MANA_COST.get());
        return AzurManaCostPolicy.apply(original, AzurStaffBalance.currentMultiplier(player), charges);
    }
}

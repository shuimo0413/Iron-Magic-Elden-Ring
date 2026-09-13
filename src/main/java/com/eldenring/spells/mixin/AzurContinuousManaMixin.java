package com.eldenring.spells.mixin;

import com.eldenring.spells.item.AzurStaffBalance;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MagicManager.class)
public abstract class AzurContinuousManaMixin {
    @WrapOperation(method = "lambda$tick$0", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getManaCost(I)I"))
    private int eldenRingSpells$continuousAzurCost(AbstractSpell spell, int level, Operation<Integer> original,
                                                  @Local(argsOnly = true) Player player) {
        MagicData data = MagicData.getPlayerMagicData(player);
        return AzurStaffBalance.manaCost(original.call(spell, level), player, data.getCastSource(), spell,
                data, true);
    }
}

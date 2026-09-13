package com.eldenring.spells.mixin;

import com.eldenring.spells.client.AzurStaffClientCosts;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.player.ServerPlayerEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayerEvents.class)
public abstract class AzurClientUseCostMixin {
    @WrapOperation(method = "onUseItem", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getManaCost(I)I"))
    private static int eldenRingSpells$clientUseAzurCost(AbstractSpell spell, int level, Operation<Integer> original,
                                                       @Local SpellSelectionManager.SelectionOption selection) {
        return AzurStaffClientCosts.manaCost(original.call(spell, level), spell, selection.getCastSource());
    }
}

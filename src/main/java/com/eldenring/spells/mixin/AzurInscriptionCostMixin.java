package com.eldenring.spells.mixin;

import com.eldenring.spells.client.AzurStaffClientCosts;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InscriptionTableScreen.class)
public abstract class AzurInscriptionCostMixin {
    @WrapOperation(method = "renderLorePage", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getManaCost(I)I"))
    private int eldenRingSpells$inscriptionAzurCost(AbstractSpell spell, int level, Operation<Integer> original) {
        return AzurStaffClientCosts.manaCost(original.call(spell, level), spell, CastSource.SPELLBOOK);
    }
}

package com.eldenring.spells.mixin;

import com.eldenring.spells.client.AzurStaffClientCosts;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TooltipsUtils.class)
public abstract class AzurTooltipCostMixin {
    @WrapOperation(method = "formatActiveSpellTooltip", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getManaCost(I)I"))
    private static int eldenRingSpells$activeAzurCost(AbstractSpell spell, int level, Operation<Integer> original,
                                                    @Local(argsOnly = true) CastSource source) {
        return AzurStaffClientCosts.manaCost(original.call(spell, level), spell, source);
    }

    @WrapOperation(method = "formatScrollTooltip", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getManaCost(I)I"))
    private static int eldenRingSpells$scrollAzurCost(AbstractSpell spell, int level, Operation<Integer> original) {
        // Scroll tooltips advertise the spell's cost when inscribed, not the consumable's free cast.
        return AzurStaffClientCosts.manaCost(original.call(spell, level), spell, CastSource.SPELLBOOK);
    }
}

package com.eldenring.spells.mixin;

import com.eldenring.spells.client.SpellManaCostClientCosts;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.player.ServerPlayerEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// remap = false：目标是铁魔法自己的类与方法，成品 jar 里不混淆，refmap 里查不到映射
@Mixin(value = ServerPlayerEvents.class, remap = false)
public abstract class AzurClientUseCostMixin {
    @WrapOperation(method = "onUseItem", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getManaCost(I)I"))
    private static int eldenRingSpells$clientUseAzurCost(AbstractSpell spell, int level, Operation<Integer> original,
                                                       @Local SpellSelectionManager.SelectionOption selection) {
        return SpellManaCostClientCosts.manaCost(
                original.call(spell, level), spell, selection.getCastSource()
        );
    }
}

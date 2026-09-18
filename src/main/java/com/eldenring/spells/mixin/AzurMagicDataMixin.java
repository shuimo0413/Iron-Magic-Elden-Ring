package com.eldenring.spells.mixin;

import com.eldenring.spells.item.AzurCastCostData;
import com.eldenring.spells.spell.cost.SpellManaCostCalculator;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MagicData.class)
public abstract class AzurMagicDataMixin implements AzurCastCostData {
    @Shadow
    private ServerPlayer serverPlayer;

    @Unique
    private double eldenRingSpells$manaCostMultiplier = 1.0D;

    @Inject(method = "initiateCast", at = @At("HEAD"))
    private void eldenRingSpells$snapshotManaCost(AbstractSpell spell, int level, int duration,
                                                  CastSource source, String slot, CallbackInfo ci) {
        eldenRingSpells$manaCostMultiplier = SpellManaCostCalculator.currentMultiplier(serverPlayer);
    }

    @Inject(method = "resetCastingState", at = @At("HEAD"))
    private void eldenRingSpells$clearManaCost(CallbackInfo ci) {
        eldenRingSpells$manaCostMultiplier = 1.0D;
    }

    @Override
    public double eldenRingSpells$getManaCostMultiplier() {
        return eldenRingSpells$manaCostMultiplier;
    }
}

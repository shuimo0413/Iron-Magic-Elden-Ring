package com.eldenring.spells.mixin;

import com.eldenring.spells.item.AzurManaCostPolicy;
import com.eldenring.spells.item.AzurStaffBalance;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSpell.class)
public abstract class AzurSpellManaMixin {
    @ModifyExpressionValue(method = "canBeCastedBy", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getManaCost(I)I"))
    private int eldenRingSpells$checkAzurCost(int original, @Local(argsOnly = true) Player player,
                                            @Local(argsOnly = true) CastSource source,
                                            @Local(argsOnly = true) MagicData data) {
        return AzurStaffBalance.manaCost(original, player, source, (AbstractSpell) (Object) this, data, false);
    }

    @ModifyExpressionValue(method = "castSpell", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getManaCost(I)I"))
    private int eldenRingSpells$chargeAzurCost(int original, @Local(argsOnly = true) ServerPlayer player,
                                             @Local(argsOnly = true) CastSource source) {
        return AzurStaffBalance.manaCost(original, player, source, (AbstractSpell) (Object) this,
                MagicData.getPlayerMagicData(player), true);
    }

    @Inject(method = "castSpell", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/events/SpellOnCastEvent;getManaCost()I"), cancellable = true)
    private void eldenRingSpells$requireFullCost(Level level, int spellLevel, ServerPlayer player,
                                               CastSource source, boolean triggerCooldown, CallbackInfo ci,
                                               @Local SpellOnCastEvent event) {
        AbstractSpell spell = (AbstractSpell) (Object) this;
        MagicData data = MagicData.getPlayerMagicData(player);
        if (AzurStaffBalance.multiplier(player, data, spell, true) <= 1.0D) {
            return;
        }
        // This branch is entered only when native code will pay mana. All event listeners have run,
        // and native creative/source/recast exemptions have already been checked.
        if (!AzurManaCostPolicy.canPay(data.getMana(), event.getManaCost(), true)) {
            player.displayClientMessage(Component.translatable("ui.irons_spellbooks.cast_error_mana",
                    spell.getDisplayName(player)).withStyle(ChatFormatting.RED), true);
            // Earlier continuous pulses have already cast. An unpaid last pulse must not erase cooldown.
            if (spell.getCastType() == CastType.CONTINUOUS
                    && !data.getPlayerRecasts().hasRecastForSpell(spell.getSpellId())
                    && (!player.isCreative() || ServerConfigs.CREATIVE_COOLDOWN.get())) {
                MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, source);
            }
            // The native manager completes final pulses itself; do not run completion twice.
            if (!triggerCooldown && data.isCasting()) {
                Utils.serverSideCancelCast(player, false);
            }
            ci.cancel();
        }
    }
}

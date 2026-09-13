package com.eldenring.spells.item;

import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * 亚兹勒的辉石杖：铁魔法 {@link StaffItem} 扩展，带
 * {@link io.redspace.ironsspellbooks.registries.ComponentRegistry#CASTING_IMPLEMENT}
 * 触媒标记，右键施法与原版/铁魔法魔杖相同。
 * <p>
 * 属性由 {@link AzurGlintstoneStaffTier} 提供（辉石法术强度 +10%）。
 */
public class AzurGlintstoneStaffItem extends StaffItem {
    public AzurGlintstoneStaffItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .attributes(io.redspace.ironsspellbooks.api.item.weapons.ExtendedSwordItem.createAttributes(
                        AzurGlintstoneStaffTier.INSTANCE
                )));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        double percent = (AzurStaffBalance.manaCostMultiplier() - 1.0D) * 100.0D;
        if (percent > 0.0D) {
            String amount = String.format(Locale.ROOT, "%.1f", percent).replaceFirst("\\.0$", "");
            tooltip.add(Component.translatable("tooltip.elden_ring_spells.azur_mana_cost", amount)
                    .withStyle(ChatFormatting.RED));
        }
    }
}

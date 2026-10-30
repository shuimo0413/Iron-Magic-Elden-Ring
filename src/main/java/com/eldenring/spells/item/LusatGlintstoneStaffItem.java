package com.eldenring.spells.item;

import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/**
 * 卢瑟特的辉石杖：铁魔法 {@link StaffItem} 扩展，主手右键施法与铁魔法魔杖相同。
 * <p>
 * 属性由 {@link LusatGlintstoneStaffTier} 提供（通用法术强度 +35%）；
 * 主手持有时全学派蓝耗按 {@link LusatStaffBalance} 的倍率上调（默认 +50%）。
 */
public class LusatGlintstoneStaffItem extends StaffItem {
    public LusatGlintstoneStaffItem() {
        super(
                new Item.Properties()
                        .stacksTo(1)
                        .rarity(Rarity.EPIC),
                LusatGlintstoneStaffTier.INSTANCE
        );
    }

    /** 在铁魔法属性提示之后追加蓝耗倍率行（与亚兹勒杖共用同一句翻译）。 */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        double percent = (LusatStaffBalance.manaCostMultiplier() - 1.0D) * 100.0D;
        if (percent > 0.0D) {
            String amount = String.format(Locale.ROOT, "%.1f", percent).replaceFirst("\\.0$", "");
            tooltip.add(Component.translatable("tooltip.iss_elden_ring.azur_mana_cost", amount)
                    .withStyle(ChatFormatting.RED));
        }
    }
}

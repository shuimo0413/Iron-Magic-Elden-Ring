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
 * 亚兹勒的辉石杖：铁魔法 {@link StaffItem} 扩展。1.20.1 的 {@code StaffItem} 本身就是施法触媒
 * （{@code ComponentRegistry.CASTING_IMPLEMENT} 那套数据组件标记在 1.20.1 不存在），
 * 右键施法与原版/铁魔法魔杖相同。
 * <p>
 * 属性由 {@link AzurGlintstoneStaffTier} 提供（辉石法术强度 +10%）；
 * 额外的全学派吟唱加速由 {@link AzurStaffBalance} 的物品属性事件注入。
 */
public class AzurGlintstoneStaffItem extends StaffItem {
    public AzurGlintstoneStaffItem() {
        super(
                new Item.Properties()
                        .stacksTo(1)
                        .rarity(Rarity.RARE),
                AzurGlintstoneStaffTier.INSTANCE
        );
    }

    /**
     * 1.20.1 的悬浮提示签名是 {@code (ItemStack, Level, List, TooltipFlag)}；
     * 1.21 才改成 {@code TooltipContext}。这里在已有提示后追加蓝耗倍率行。
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        double percent = (AzurStaffBalance.manaCostMultiplier() - 1.0D) * 100.0D;
        if (percent > 0.0D) {
            String amount = String.format(Locale.ROOT, "%.1f", percent).replaceFirst("\\.0$", "");
            tooltip.add(Component.translatable("tooltip.iss_elden_ring.azur_mana_cost", amount)
                    .withStyle(ChatFormatting.RED));
        }
    }
}

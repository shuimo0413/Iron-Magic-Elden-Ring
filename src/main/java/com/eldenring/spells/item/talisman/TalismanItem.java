package com.eldenring.spells.item.talisman;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * 艾尔登法环护符的通用物品基类，由 Curios 负责装备生命周期。
 */
public class TalismanItem extends Item implements ICurioItem {


    public TalismanItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * 同一种护符只能装备一件，避免扩展槽位后属性与独立效果重复叠加。
     */
    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return CuriosApi.getCuriosInventory(slotContext.entity())
                .map(handler -> !handler.isEquipped(stack.getItem()))
                .orElse(true);
    }
}

package com.eldenring.spells.item;

import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * 观星杖：铁魔法 {@link StaffItem} 扩展，带 {@link io.redspace.ironsspellbooks.registries.ComponentRegistry#CASTING_IMPLEMENT}
 * 触媒标记，右键施法与原版/铁魔法魔杖相同。
 * <p>
 * 属性由 {@link AstrologerStaffTier} 提供（辉石法术强度 +10%）。
 */
public class AstrologerStaffItem extends StaffItem {
    public AstrologerStaffItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
                .attributes(io.redspace.ironsspellbooks.api.item.weapons.ExtendedSwordItem.createAttributes(
                        AstrologerStaffTier.INSTANCE
                )));
    }
}

package com.eldenring.spells.item;

import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * 观星杖：铁魔法 {@link StaffItem} 扩展。1.20.1 的 {@code StaffItem} 本身就是施法触媒
 * （{@code ComponentRegistry.CASTING_IMPLEMENT} 那套数据组件标记在 1.20.1 不存在），
 * 右键施法与原版/铁魔法魔杖相同。
 * <p>
 * 属性由 {@link AstrologerStaffTier} 提供（辉石法术强度 +10%）；1.20.1 只能通过
 * {@code StaffItem(Properties, StaffTier)} 传入，不再有 {@code Item.Properties.attributes(...)}。
 */
public class AstrologerStaffItem extends StaffItem {
    public AstrologerStaffItem() {
        super(
                new Item.Properties()
                        .stacksTo(1)
                        .rarity(Rarity.UNCOMMON),
                AstrologerStaffTier.INSTANCE
        );
    }
}

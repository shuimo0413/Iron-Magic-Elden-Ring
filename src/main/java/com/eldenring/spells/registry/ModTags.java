package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * 本模组物品 Tag。辉石触媒用于卷轴锻造台判定学派。
 */
public final class ModTags {
    /**
     * 辉石学派触媒：放入铁魔法卷轴锻造台「焦点」槽时，产出辉石系卷轴。
     * 包含青 / 蓝 / 紫三色辉石碎片（见数据包 tags），不含紫水晶。
     */
    public static final TagKey<Item> GLINTSTONE_FOCUS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "glintstone_focus")
    );

    private ModTags() {
    }
}

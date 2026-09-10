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
     * 辉石学派触媒：放入铁魔法卷轴锻造台「焦点」槽时可抄辉石系卷轴。
     * 含青 / 蓝 / 紫碎片与起源辉石（见数据包 tags）；具体哪颗对应哪些咒见
     * {@link com.eldenring.spells.recipe.GlintstoneScrollRecipes}。不含紫水晶。
     */
    public static final TagKey<Item> GLINTSTONE_FOCUS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "glintstone_focus")
    );

    /**
     * 三色辉石水晶簇：起源晶体有序合成周围八格用此标签，可混色。
     */
    public static final TagKey<Item> GLINTSTONE_CLUSTERS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "glintstone_clusters")
    );

    private ModTags() {
    }
}

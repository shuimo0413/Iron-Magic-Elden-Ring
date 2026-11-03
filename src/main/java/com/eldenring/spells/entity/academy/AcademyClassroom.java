package com.eldenring.spells.entity.academy;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * 雷亚卢卡利亚学院的四间教室。每间教室对应一种学者实体：
 * 实体 ID、出生时佩戴的辉石头冠、以及 {@code data/iss_elden_ring/mob_spellcasting/<实体ID>.json} 法术配置。
 * <p>
 * 头冠用 Supplier 延迟取物品，避免枚举初始化时提前触发物品注册表。
 */
public enum AcademyClassroom {
    KAROLOS("karolos_scholar", () -> ModItems.KAROLOS_GLINTSTONE_CROWN.get()),
    TWINSAGE("twinsage_scholar", () -> ModItems.TWINSAGE_GLINTSTONE_CROWN.get()),
    LAZULI("lazuli_scholar", () -> ModItems.LAZULI_GLINTSTONE_CROWN.get()),
    OLIVINUS("olivinus_scholar", () -> ModItems.OLIVINUS_GLINTSTONE_CROWN.get());

    private final String entityPath;
    private final Supplier<Item> crownItem;

    AcademyClassroom(String entityPath, Supplier<Item> crownItem) {
        this.entityPath = entityPath;
        this.crownItem = crownItem;
    }

    /** 实体注册名（不含命名空间），同时是法术配置 JSON 的文件名。 */
    public String entityPath() {
        return entityPath;
    }

    /** 该教室学者出生时戴的辉石头冠。 */
    public Item crownItem() {
        return crownItem.get();
    }

    /** 法术配置资源 ID：{@code iss_elden_ring:<实体ID>} → {@code data/iss_elden_ring/mob_spellcasting/<实体ID>.json}。 */
    public ResourceLocation spellConfigId() {
        return new ResourceLocation(EldenRingSpellsMod.MOD_ID, entityPath);
    }
}

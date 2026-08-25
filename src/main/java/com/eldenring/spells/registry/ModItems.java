package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 本模组物品注册入口。辉石法术不单独注册卷轴物品。
 * <p>
 * 创造栏只用铁魔法通用卷轴 {@code irons_spellbooks:scroll}，法术写在
 * {@link ISpellContainer} 里，因此抄写台可以直接抄。外观由客户端
 * {@code ScrollModel} mixin 切到 {@code item/<spell>_scroll}。
 * <p>
 * 三色辉石碎片是学派触媒（Focus）：放入卷轴锻造台焦点槽，产出辉石咒。
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(EldenRingSpellsMod.MOD_ID);

    /**
     * 青色辉石碎片。学院系主色触媒，与 {@link ModTags#GLINTSTONE_FOCUS} 绑定。
     */
    public static final DeferredItem<Item> CYAN_GLINTSTONE_SHARD = ITEMS.register(
            "cyan_glintstone_shard",
            () -> new Item(new Item.Properties())
    );

    /**
     * 蓝色辉石碎片。更深的亚兹勒蓝触媒，与青色/紫色同样可作为辉石焦点。
     */
    public static final DeferredItem<Item> BLUE_GLINTSTONE_SHARD = ITEMS.register(
            "blue_glintstone_shard",
            () -> new Item(new Item.Properties())
    );

    /**
     * 紫色辉石碎片。夜紫触媒，与青色/蓝色同样可作为辉石焦点。
     */
    public static final DeferredItem<Item> PURPLE_GLINTSTONE_SHARD = ITEMS.register(
            "purple_glintstone_shard",
            () -> new Item(new Item.Properties())
    );

    static {
        // BlockItem 与方块同 id；必须在 ModBlocks 已向总线注册之后再 register(ITEMS)
        for (ModBlocks.ColorSet set : ModBlocks.BY_COLOR.values()) {
            ITEMS.registerSimpleBlockItem(set.ore);
            ITEMS.registerSimpleBlockItem(set.deepslateOre);
            ITEMS.registerSimpleBlockItem(set.crystalBlock);
            ITEMS.registerSimpleBlockItem(set.cluster);
        }
    }

    private ModItems() {
    }

    /**
     * 生成一张铁魔法通用卷轴并写入指定法术。
     *
     * @param spell      要写入的法术
     * @param spellLevel 法术等级（从 1 起；目前辉石咒最高 1 级）
     */
    public static ItemStack createFilledScroll(Supplier<AbstractSpell> spell, int spellLevel) {
        ItemStack stack = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell.get(), spellLevel, stack);
        return stack;
    }

    /** 创造栏图标用：1 级辉石魔砾通用卷轴。 */
    public static ItemStack createFilledScroll(int spellLevel) {
        return createFilledScroll(ModSpells.GLINTSTONE_PEBBLE, spellLevel);
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}

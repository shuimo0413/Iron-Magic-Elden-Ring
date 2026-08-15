package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 本模组物品注册入口。辉石法术不单独注册卷轴物品。
 * <p>
 * 创造栏只用铁魔法通用卷轴 {@code irons_spellbooks:scroll}，法术写在
 * {@link ISpellContainer} 里，因此抄写台可以直接抄。外观由客户端
 * {@code ScrollModel} mixin 切到 {@code item/<spell>_scroll}。
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(EldenRingSpellsMod.MOD_ID);

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

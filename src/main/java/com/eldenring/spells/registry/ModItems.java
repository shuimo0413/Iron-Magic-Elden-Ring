package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(EldenRingSpellsMod.MOD_ID);

    public static final DeferredItem<Scroll> GLINTSTONE_PEBBLE_SCROLL =
            ITEMS.register("glintstone_pebble_scroll",
                    () -> new Scroll(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(16)));

    public static final DeferredItem<Scroll> SWIFT_GLINTSTONE_SHARD_SCROLL =
            ITEMS.register("swift_glintstone_shard_scroll",
                    () -> new Scroll(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(16)));

    public static final DeferredItem<Scroll> GREAT_GLINTSTONE_SHARD_SCROLL =
            ITEMS.register("great_glintstone_shard_scroll",
                    () -> new Scroll(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(16)));

    public static final DeferredItem<Scroll> GLINTSTONE_COMET_SCROLL =
            ITEMS.register("glintstone_comet_scroll",
                    () -> new Scroll(new Item.Properties().rarity(Rarity.RARE).stacksTo(16)));

    public static final DeferredItem<Scroll> GLINTSTONE_STARS_SCROLL =
            ITEMS.register("glintstone_stars_scroll",
                    () -> new Scroll(new Item.Properties().rarity(Rarity.RARE).stacksTo(16)));

    public static final DeferredItem<Scroll> STAR_SHOWER_SCROLL =
            ITEMS.register("star_shower_scroll",
                    () -> new Scroll(new Item.Properties().rarity(Rarity.RARE).stacksTo(16)));

    public static final DeferredItem<Scroll> STARS_OF_RUIN_SCROLL =
            ITEMS.register("stars_of_ruin_scroll",
                    () -> new Scroll(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)));

    public static final DeferredItem<Scroll> COMET_SCROLL =
            ITEMS.register("comet_scroll",
                    () -> new Scroll(new Item.Properties().rarity(Rarity.RARE).stacksTo(16)));

    public static final DeferredItem<Scroll> SPIRAL_SHARD_SCROLL =
            ITEMS.register("spiral_shard_scroll",
                    () -> new Scroll(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(16)));

    private ModItems() {
    }

    public static ItemStack createFilledScroll(DeferredItem<Scroll> scrollItem, Supplier<AbstractSpell> spell, int spellLevel) {
        ItemStack stack = new ItemStack(scrollItem.get());
        ISpellContainer.createScrollContainer(spell.get(), spellLevel, stack);
        return stack;
    }

    /** 兼容旧调用：默认辉石魔砾卷轴。 */
    public static ItemStack createFilledScroll(int spellLevel) {
        return createFilledScroll(GLINTSTONE_PEBBLE_SCROLL, ModSpells.GLINTSTONE_PEBBLE, spellLevel);
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}

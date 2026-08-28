package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * 只保留本模组创造栏「艾尔登法环法术」。
 * 里面是铁魔法通用卷轴（可抄写）；铁魔法自己的「法术卷轴」栏会把辉石咒再塞一遍，这里去掉以免两份。
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EldenRingSpellsMod.MOD_ID);

    /** 铁魔法「法术卷轴」创造栏 id。 */
    private static final ResourceLocation IRONS_SPELLS_SCROLLS_TAB_ID =
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spellbook_scrolls");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.elden_ring_spells"))
                    .icon(() -> ModItems.createFilledScroll(1))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.CYAN_GLINTSTONE_SHARD.get());
                        output.accept(ModItems.BLUE_GLINTSTONE_SHARD.get());
                        output.accept(ModItems.PURPLE_GLINTSTONE_SHARD.get());
                        for (ModBlocks.ColorSet set : ModBlocks.BY_COLOR.values()) {
                            output.accept(set.crystalBlock.get());
                            output.accept(set.cluster.get());
                        }
                        output.accept(ModItems.createFilledScroll(ModSpells.GLINTSTONE_PEBBLE, 1));
                        output.accept(ModItems.createFilledScroll(ModSpells.SWIFT_GLINTSTONE_SHARD, 1));
                        output.accept(ModItems.createFilledScroll(ModSpells.GLINTSTONE_ARC, 1));
                        output.accept(ModItems.createFilledScroll(ModSpells.CRYSTAL_BURST, 1));
                        output.accept(ModItems.createFilledScroll(ModSpells.CRYSTAL_BARRAGE, 1));
                        output.accept(ModItems.createFilledScroll(ModSpells.GREAT_GLINTSTONE_SHARD, 1));
                        output.accept(ModItems.createFilledScroll(ModSpells.GLINTSTONE_COMET, 1));
                        output.accept(ModItems.createFilledScroll(ModSpells.GLINTSTONE_STARS, 1));
                        output.accept(ModItems.createFilledScroll(ModSpells.STAR_SHOWER, 1));
                        output.accept(ModItems.createFilledScroll(ModSpells.STARS_OF_RUIN, 1));
                        output.accept(ModItems.createFilledScroll(ModSpells.FOUNDING_RAIN_OF_STARS, 1));
                        output.accept(ModItems.createFilledScroll(ModSpells.COMET, 1));
                        output.accept(ModItems.createFilledScroll(ModSpells.SPIRAL_SHARD, 1));
                        output.accept(ModItems.createFilledScroll(ModSpells.STARLIGHT, 1));
                        output.accept(ModItems.createFilledScroll(ModSpells.TERRA_MAGICA, 1));
                        output.accept(ModItems.createFilledScroll(ModSpells.COMET_AZUR, 1));
                        output.accept(ModItems.createFilledScroll(ModSpells.GAVEL_OF_HAIMA, 1));
                        output.accept(ModItems.createFilledScroll(ModSpells.CANNON_OF_HAIMA, 1));
                        output.accept(ModItems.createFilledScroll(ModSpells.CARIAN_SLICER, 1));
                        output.accept(ModItems.createFilledScroll(ModSpells.MAGIC_GLINTBLADE, 1));
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_TABS.register(modEventBus);
        modEventBus.addListener(EventPriority.LOW, ModCreativeTabs::hideGlintstoneScrollsFromIronsTab);
    }

    /**
     * 铁魔法会把所有启用法术填进自己的卷轴栏。辉石咒已经在本模组栏里，这里删掉重复项。
     */
    private static void hideGlintstoneScrollsFromIronsTab(BuildCreativeModeTabContentsEvent event) {
        if (!IRONS_SPELLS_SCROLLS_TAB_ID.equals(event.getTabKey().location())) {
            return;
        }
        List<ItemStack> stacksToRemove = new ArrayList<>();
        for (ItemStack stack : event.getParentEntries()) {
            if (isEldenRingSpellScroll(stack)) {
                stacksToRemove.add(stack);
            }
        }
        for (ItemStack stack : stacksToRemove) {
            event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    private static boolean isEldenRingSpellScroll(ItemStack stack) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            return false;
        }
        ResourceLocation spellId = ISpellContainer.get(stack).getSpellAtIndex(0).getSpell().getSpellResource();
        return EldenRingSpellsMod.MOD_ID.equals(spellId.getNamespace());
    }
}

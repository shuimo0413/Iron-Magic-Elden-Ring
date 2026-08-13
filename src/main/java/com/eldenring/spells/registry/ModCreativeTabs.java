package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EldenRingSpellsMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.elden_ring_spells"))
                    .icon(() -> ModItems.createFilledScroll(1))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.createFilledScroll(
                                ModItems.GLINTSTONE_PEBBLE_SCROLL, ModSpells.GLINTSTONE_PEBBLE, 1));
                        output.accept(ModItems.createFilledScroll(
                                ModItems.SWIFT_GLINTSTONE_SHARD_SCROLL, ModSpells.SWIFT_GLINTSTONE_SHARD, 1));
                        output.accept(ModItems.createFilledScroll(
                                ModItems.GREAT_GLINTSTONE_SHARD_SCROLL, ModSpells.GREAT_GLINTSTONE_SHARD, 1));
                        output.accept(ModItems.createFilledScroll(
                                ModItems.GLINTSTONE_COMET_SCROLL, ModSpells.GLINTSTONE_COMET, 1));
                        output.accept(ModItems.createFilledScroll(
                                ModItems.GLINTSTONE_STARS_SCROLL, ModSpells.GLINTSTONE_STARS, 1));
                        output.accept(ModItems.createFilledScroll(
                                ModItems.STAR_SHOWER_SCROLL, ModSpells.STAR_SHOWER, 1));
                        output.accept(ModItems.createFilledScroll(
                                ModItems.STARS_OF_RUIN_SCROLL, ModSpells.STARS_OF_RUIN, 1));
                        output.accept(ModItems.createFilledScroll(
                                ModItems.COMET_SCROLL, ModSpells.COMET, 1));
                        output.accept(ModItems.createFilledScroll(
                                ModItems.SPIRAL_SHARD_SCROLL, ModSpells.SPIRAL_SHARD, 1));
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_TABS.register(modEventBus);
    }
}

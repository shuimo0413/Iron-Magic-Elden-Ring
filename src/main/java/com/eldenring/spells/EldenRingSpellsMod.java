package com.eldenring.spells;

import com.eldenring.spells.config.EldenRingConfigs;
import com.eldenring.spells.registry.ModAttributes;
import com.eldenring.spells.registry.ModBlocks;
import com.eldenring.spells.registry.ModCreativeTabs;
import com.eldenring.spells.registry.ModEffects;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModFeatures;
import com.eldenring.spells.registry.ModItems;
import com.eldenring.spells.registry.ModParticles;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.registry.ModSounds;
import com.eldenring.spells.registry.ModSpells;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(EldenRingSpellsMod.MOD_ID)
public class EldenRingSpellsMod {
    public static final String MOD_ID = "elden_ring_spells";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EldenRingSpellsMod(IEventBus modEventBus, ModContainer modContainer) {
        
        EldenRingConfigs.register(modContainer, modEventBus);
        ModAttributes.register(modEventBus);
        ModSchools.register(modEventBus);
        ModEffects.register(modEventBus);
        // 方块必须先于物品：BlockItem 依赖方块 DeferredHolder
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModFeatures.register(modEventBus);
        ModParticles.register(modEventBus);
        ModSounds.register(modEventBus);
        ModEntities.register(modEventBus);
        ModSpells.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Elden Ring Spells addon loaded (Iron's Spells dependency OK).");
    }
}

package com.eldenring.spells;

import com.eldenring.spells.config.EldenRingConfigs;
import com.eldenring.spells.network.TrackingIgnorePrefsPayload;
import com.eldenring.spells.registry.ModAttachments;
import com.eldenring.spells.registry.ModAttributes;
import com.eldenring.spells.registry.ModBlocks;
import com.eldenring.spells.registry.ModCreativeTabs;
import com.eldenring.spells.registry.ModEffects;
import com.eldenring.spells.registry.ModEntityAttributes;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModFeatures;
import com.eldenring.spells.registry.ModFluids;
import com.eldenring.spells.registry.ModItems;
import com.eldenring.spells.registry.ModParticles;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.registry.ModSounds;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.registry.ModRecipes;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EldenRingSpellsMod.MOD_ID)
public class EldenRingSpellsMod {
    public static final String MOD_ID = "iss_elden_ring";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Forge 只允许无参 {@code ()} 或 {@code (FMLJavaModLoadingContext)} 构造器，
     * 不再有 {@code (IEventBus, ModContainer)} 注入。因此：
     * <ul>
     *   <li>mod 事件总线从 {@link FMLJavaModLoadingContext#get()} 取；</li>
     *   <li>配置注册交给 {@link EldenRingConfigs}（内部走 {@code ModLoadingContext.get().registerConfig}）。</li>
     * </ul>
     */
    public EldenRingSpellsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        EldenRingConfigs.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModAttributes.register(modEventBus);
        ModSchools.register(modEventBus);
        ModEffects.register(modEventBus);
        // 方块必须先于物品：BlockItem 依赖方块 RegistryObject
        ModBlocks.register(modEventBus);
        ModFluids.register(modEventBus);
        ModItems.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModFeatures.register(modEventBus);
        ModParticles.register(modEventBus);
        ModSounds.register(modEventBus);
        ModEntities.register(modEventBus);
        ModEntityAttributes.register(modEventBus);
        ModSpells.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Forge 没有 NeoForge 的 RegisterPayloadHandlersEvent / IPayloadContext：
        // 两个包（追踪偏好 C2S+S2C、亚兹勒杖设置 S2C）共用一个 SimpleChannel，
        // 由 network agent 在 TrackingIgnorePrefsPayload.register() 中一次登记两个 message。
        // FMLCommonSetupEvent 是多线程触发的，SimpleChannel 登记放到主线程执行更稳妥。
        event.enqueueWork(TrackingIgnorePrefsPayload::register);
        LOGGER.info("Iron's Spells 'n Spellbooks: Elden Ring loaded (Iron's Spells dependency OK).");
    }
}

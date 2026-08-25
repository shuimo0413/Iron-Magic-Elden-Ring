package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.worldgen.GlintstoneCaveFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 世界生成 Feature 类型注册。配置与放置仍走数据包 JSON。
 */
public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, EldenRingSpellsMod.MOD_ID);

    /**
     * 辉石矿洞：在现成洞穴表面刷同色水晶/水晶块（三色等概率、一洞一色）。
     */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> GLINTSTONE_CAVE =
            FEATURES.register("glintstone_cave", () -> new GlintstoneCaveFeature(NoneFeatureConfiguration.CODEC));

    private ModFeatures() {
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}

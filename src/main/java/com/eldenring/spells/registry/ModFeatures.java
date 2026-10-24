package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.worldgen.GlintstoneCaveFeature;
import com.eldenring.spells.worldgen.MagicLakeFloatingAzaleaFeature;
import com.eldenring.spells.worldgen.MagicLakeGlintstoneFeature;
import com.eldenring.spells.worldgen.MagicLakeShallowsFeature;
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

    /**
     * 魔法之湖浅湖整平：深水垫底到 ≤ 2 格、低地淹成浅水、高处留作小岛。只由魔法之湖群系 JSON 引用。
     */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> MAGIC_LAKE_SHALLOWS =
            FEATURES.register("magic_lake_shallows", () -> new MagicLakeShallowsFeature(NoneFeatureConfiguration.CODEC));

    /**
     * 魔法之湖辉石水晶点缀：湖中立柱 / 陆地露头，每处随机一色（三色等概率）。只由魔法之湖群系 JSON 引用。
     */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> MAGIC_LAKE_GLINTSTONE =
            FEATURES.register("magic_lake_glintstone", () -> new MagicLakeGlintstoneFeature(NoneFeatureConfiguration.CODEC));

    /**
     * 魔法之湖水面杜鹃树：开阔浅水垫 1 格泥土后长原版杜鹃树。只由魔法之湖群系 JSON 引用。
     */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> MAGIC_LAKE_FLOATING_AZALEA =
            FEATURES.register("magic_lake_floating_azalea", () -> new MagicLakeFloatingAzaleaFeature(NoneFeatureConfiguration.CODEC));

    private ModFeatures() {
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}

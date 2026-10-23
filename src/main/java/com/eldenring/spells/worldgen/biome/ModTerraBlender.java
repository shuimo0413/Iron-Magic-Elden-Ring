package com.eldenring.spells.worldgen.biome;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.config.EldenRingCommonConfig;
import net.minecraft.resources.ResourceLocation;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;

/**
 * TerraBlender 接入点：注册本模组 Region 与命名空间地表规则。
 * <p>
 * 必须在 {@code FMLCommonSetupEvent#enqueueWork} 里调用（TerraBlender 的注册表非线程安全）。
 * 此时 common toml 已加载，{@link EldenRingCommonConfig#magicLakeRegionWeight} 是整合包填的值。
 */
public final class ModTerraBlender {

    /** Region 名称（TerraBlender 内部唯一键，F3 调试信息也显示它）。 */
    private static final ResourceLocation MAGIC_LAKE_REGION_NAME =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "overworld_magic_lake");

    private ModTerraBlender() {
    }

    public static void register() {
        // 地表规则只对本命名空间的群系生效；Region 关掉时群系不出现，规则不会被用到，照常注册无副作用
        SurfaceRuleManager.addSurfaceRules(
                SurfaceRuleManager.RuleCategory.OVERWORLD,
                EldenRingSpellsMod.MOD_ID,
                MagicLakeSurfaceRules.build()
        );

        int regionWeight = EldenRingCommonConfig.magicLakeRegionWeight;
        if (regionWeight <= 0) {
            EldenRingSpellsMod.LOGGER.info("Magic Lake region disabled (region_weight = 0).");
            return;
        }
        Regions.register(new MagicLakeRegion(MAGIC_LAKE_REGION_NAME, regionWeight));
        EldenRingSpellsMod.LOGGER.info("Registered Magic Lake TerraBlender region, weight {}.", regionWeight);
    }
}

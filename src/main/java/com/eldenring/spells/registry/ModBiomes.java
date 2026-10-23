package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * 本模组群系 ResourceKey。群系本体由数据包 {@code data/iss_elden_ring/worldgen/biome/*.json} 提供，
 * 1.21.1 群系是动态注册表，不走 DeferredRegister。
 * <p>
 * 放进主世界靠 TerraBlender（见 {@code worldgen/biome/ModTerraBlender}）。
 * 群系 id 一旦发布不要改名：旧存档区块里记的是 id，改名后那片群系会变成未知。
 */
public final class ModBiomes {
    /**
     * 魔法之湖：原型为艾尔登法环的湖之利耶尼亚。广阔浅湖（水深 ≤ 2 格）、散布杉树小岛、清冷灰蓝色调；
     * 客户端夜间额外提亮（只改光照贴图，不动逻辑光照）。
     */
    public static final ResourceKey<Biome> MAGIC_LAKE = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "magic_lake")
    );

    private ModBiomes() {
    }
}

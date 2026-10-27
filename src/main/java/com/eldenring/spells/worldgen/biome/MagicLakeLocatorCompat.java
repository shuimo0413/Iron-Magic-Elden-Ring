package com.eldenring.spells.worldgen.biome;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.config.EldenRingCommonConfig;
import com.eldenring.spells.mixin.BiomeSourcePossibleBiomesAccessor;
import com.eldenring.spells.registry.ModBiomes;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import terrablender.api.RegionType;
import terrablender.util.LevelUtils;

import java.util.Set;
import java.util.function.Supplier;

/**
 * 保证魔法之湖出现在主世界 {@link BiomeSource#possibleBiomes()} 里，
 * 从而可被 {@code /locate biome} 与 Nature's Compass（群系指南针）探测。
 * <p>
 * <b>为什么「注册了群系」还不够：</b>数据包只把 {@code iss_elden_ring:magic_lake}
 * 写进 {@link Registries#BIOME}；真正放进主世界靠 TerraBlender Region。
 * TB 在 {@code ServerAboutToStart}（{@link EventPriority#LOWEST}）里把各 Region 的群系
 * append 进 {@code possibleBiomes}。原版 locate / 自然罗盘的维度栏都先查这份集合——
 * 不在集合里会直接判定「本维度不存在」，搜索也会立刻失败，哪怕气候 RTree 里其实能生成湖。
 * <p>
 * 本类同样挂 {@code LOWEST}，且本模组在 mods.toml 里 {@code ordering=AFTER} TerraBlender，
 * 监听器注册晚于 TB，因此会在 TB 的 append 之后再跑一遍：已在集合里则只打日志；
 * 若缺失（TB 时序 / Holder 匹配边缘情况）则补写一次。
 * {@code region_weight = 0} 时 Region 未注册、湖不会生成，本类直接跳过，避免假阳性。
 */
@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID)
public final class MagicLakeLocatorCompat {

    private MagicLakeLocatorCompat() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ensureMagicLakeInPossibleBiomes(ServerAboutToStartEvent event) {
        if (EldenRingCommonConfig.magicLakeRegionWeight <= 0) {
            EldenRingSpellsMod.LOGGER.info(
                    "Magic Lake locator compat skipped (region_weight = 0; biome will not generate)."
            );
            return;
        }

        MinecraftServer server = event.getServer();
        Registry<Biome> biomeRegistry = server.registryAccess().registryOrThrow(Registries.BIOME);
        Holder.Reference<Biome> magicLakeHolder = biomeRegistry.getHolderOrThrow(ModBiomes.MAGIC_LAKE);

        Registry<LevelStem> levelStemRegistry = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
        int patchedStemCount = 0;
        int alreadyPresentStemCount = 0;

        for (var stemEntry : levelStemRegistry.entrySet()) {
            ResourceKey<LevelStem> stemKey = stemEntry.getKey();
            LevelStem stem = stemEntry.getValue();
            // 只处理挂了 TerraBlender OVERWORLD Region 的维度（主世界及同类）；下界 / 末地不注入
            if (LevelUtils.getRegionTypeForDimension(stem.type()) != RegionType.OVERWORLD) {
                continue;
            }
            if (!LevelUtils.shouldApplyToBiomeSource(stem.generator().getBiomeSource())) {
                continue;
            }

            ChunkGenerator chunkGenerator = stem.generator();
            BiomeSource biomeSource = chunkGenerator.getBiomeSource();

            if (!containsMagicLake(biomeSource, magicLakeHolder)) {
                injectMagicLake(biomeSource, magicLakeHolder);
                patchedStemCount++;
                EldenRingSpellsMod.LOGGER.info(
                        "Magic Lake was missing from possibleBiomes for level stem {}; injected for locator mods.",
                        stemKey.location()
                );
            } else {
                alreadyPresentStemCount++;
            }
        }

        EldenRingSpellsMod.LOGGER.info(
                "Magic Lake locator compat: {} stem(s) already listed, {} stem(s) patched.",
                alreadyPresentStemCount,
                patchedStemCount
        );
    }

    /**
     * 用 {@link Holder#is(ResourceKey)} 判断，避免 Direct / Reference Holder 的 {@code equals} 不一致
     * 导致 {@link Set#contains} 假阴性（Nature's Compass 维度栏也会踩这个坑）。
     */
    private static boolean containsMagicLake(BiomeSource biomeSource, Holder<Biome> magicLakeHolder) {
        for (Holder<Biome> possibleBiome : biomeSource.possibleBiomes()) {
            if (possibleBiome.is(ModBiomes.MAGIC_LAKE) || possibleBiome.equals(magicLakeHolder)) {
                return true;
            }
        }
        return false;
    }

    private static void injectMagicLake(BiomeSource biomeSource, Holder<Biome> magicLakeHolder) {
        BiomeSourcePossibleBiomesAccessor accessor = (BiomeSourcePossibleBiomesAccessor) biomeSource;
        Supplier<Set<Holder<Biome>>> currentSupplier = accessor.eldenRingSpells$getPossibleBiomes();
        Set<Holder<Biome>> merged = new ObjectLinkedOpenHashSet<>(currentSupplier.get());
        merged.add(magicLakeHolder);
        // 固定快照：避免之后再被懒加载的原版 collectPossibleBiomes 盖掉
        Set<Holder<Biome>> snapshot = new ObjectLinkedOpenHashSet<>(merged);
        accessor.eldenRingSpells$setPossibleBiomes(() -> snapshot);
    }
}

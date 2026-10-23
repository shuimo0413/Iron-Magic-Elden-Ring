package com.eldenring.spells.worldgen.biome;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModBiomes;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 魔法之湖的 TerraBlender 主世界 Region：稀有但大片。Region 权重默认很低（见 common toml），
 * 一旦某块区域分给这个 Region，就沿用原版主世界布局，把除山地以外的内陆地表气候点全部换成魔法之湖，
 * 这块区域的内陆基本就是一整个大湖（湖心以后要放魔法学院）。
 * <p>
 * 选点条件：erosion 下界 ≥ -0.2225（原版 erosion 第 3–6 档：丘陵、平原、森林、沼泽等；
 * 第 0–2 档的尖峰 / 高原 / 风袭山地留给原版，压平量太大）；
 * continentalness 区间伸进内陆（上界 &gt; -0.11）且不碰海洋（下界 ≥ -0.19）；地表 depth。
 * 不分温度 / 湿度，河流也一起换掉，湖区不会被河道切碎。
 * 原版很多气候点的 continentalness 是「海岸 → 远内陆」一整段，必须按「区间重叠」而不是「完全落在内陆」判断，
 * 否则会漏掉大半，湖会很零碎。
 * <p>
 * TerraBlender 只决定「哪里是这个群系」，不改地形噪声；原地形的丘陵起伏交给
 * {@code worldgen/MagicLakeShallowsFeature} 在生成时压扁成浅水 + 低矮小岛。
 */
public final class MagicLakeRegion extends Region {

    /**
     * erosion 下界阈值（原版 {@code OverworldBiomeBuilder} 的 erosions[3] = [-0.2225, 0.05]）。
     * 调大到 0.05 / 0.55 → 只换更平坦的档，湖更小更碎；调小到 -0.375 → 连山地档也换，湖更大但湖岸会贴着高山、压平量极大。
     */
    private static final float LOW_RELIEF_EROSION_MIN = -0.2225f;

    /** 内陆下界（原版 inlandContinentalness 起点 -0.11）。气候点 continentalness 上界必须高于此值。 */
    private static final float INLAND_CONTINENTALNESS_MIN = -0.11f;

    /** 海岸下界（原版 coastContinentalness 起点 -0.19）。气候点 continentalness 下界低于此值说明伸进海洋，不换。 */
    private static final float COAST_CONTINENTALNESS_MIN = -0.19f;

    /** 原版地表群系的 depth 只取 0（地表）和 1（地表以下一格起的「地面」变体）两个点。 */
    private static final long SURFACE_DEPTH = Climate.quantizeCoord(0.0f);
    private static final long FLOOR_DEPTH = Climate.quantizeCoord(1.0f);

    public MagicLakeRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        List<Climate.ParameterPoint> lowReliefInlandPoints = collectLowReliefInlandSurfacePoints();
        addModifiedVanillaOverworldBiomes(mapper, builder -> {
            // 与 TerraBlender 内部用同一个原版 OverworldBiomeBuilder 生成，ParameterPoint 是 record，按值精确匹配
            for (Climate.ParameterPoint point : lowReliefInlandPoints) {
                builder.replaceBiome(point, ModBiomes.MAGIC_LAKE);
            }
            builder.replaceBiome(Biomes.SWAMP, ModBiomes.MAGIC_LAKE);
        });
    }

    /**
     * 遍历原版主世界气候点，挑出「非山地 + 伸进内陆 + 不碰海洋 + 地表」的点。
     */
    private static List<Climate.ParameterPoint> collectLowReliefInlandSurfacePoints() {
        long lowReliefErosionMin = Climate.quantizeCoord(LOW_RELIEF_EROSION_MIN);
        long inlandContinentalnessMin = Climate.quantizeCoord(INLAND_CONTINENTALNESS_MIN);
        long coastContinentalnessMin = Climate.quantizeCoord(COAST_CONTINENTALNESS_MIN);
        List<Climate.ParameterPoint> matchedPoints = new ArrayList<>();
        // knownPresets 内部同样调用 OverworldBiomeBuilder#addBiomes（该方法 protected，无法直接调用）
        List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> vanillaOverworldPoints = MultiNoiseBiomeSourceParameterList
                .knownPresets()
                .get(MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD)
                .values();
        for (Pair<Climate.ParameterPoint, ResourceKey<Biome>> pointAndBiome : vanillaOverworldPoints) {
            Climate.ParameterPoint point = pointAndBiome.getFirst();
            Climate.Parameter depth = point.depth();
            boolean isSurfacePoint = depth.min() == depth.max()
                    && (depth.min() == SURFACE_DEPTH || depth.min() == FLOOR_DEPTH);
            Climate.Parameter continentalness = point.continentalness();
            boolean reachesInland = continentalness.max() > inlandContinentalnessMin;
            boolean staysOutOfOcean = continentalness.min() >= coastContinentalnessMin;
            if (isSurfacePoint
                    && point.erosion().min() >= lowReliefErosionMin
                    && reachesInland
                    && staysOutOfOcean) {
                matchedPoints.add(point);
            }
        }
        EldenRingSpellsMod.LOGGER.info("Magic Lake region replaces {} vanilla climate points.", matchedPoints.size());
        return matchedPoints;
    }
}

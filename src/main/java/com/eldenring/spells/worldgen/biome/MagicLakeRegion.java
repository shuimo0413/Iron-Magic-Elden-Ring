package com.eldenring.spells.worldgen.biome;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModBiomes;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.List;
import java.util.function.Consumer;

/**
 * 魔法之湖的 TerraBlender 主世界 Region：在原版主世界气候空间里切出一个「湖盒子」，
 * 原版气候点落进盒子的部分换成魔法之湖，盒子外的部分保持原版群系不变。
 * <p>
 * 湖的形状主要由湿度决定：原版湿度（vegetation）噪声只有 2 个有效八度、非常平滑，
 * 取它的高值区（≥ {@link #LAKE_HUMIDITY_MIN}）得到的是一团团接近圆 / 椭圆的区域，大小由这一个常量控制。
 * 温度、大陆度、侵蚀度只做宽松裁剪（排除冰冻 / 炎热、海洋 / 海岸、山地 / 高原），很少切到团块中间。
 * <p>
 * 不能按侵蚀度 / 大陆度挑点（第一到第四轮的做法）：它们的阈值接近中位数，满足条件的地块连成一大片，
 * 而且边界跟着多八度噪声走，又碎又长；原版河流点的侵蚀度跨第 2–5 档，从来没被替换，河道会把湖切成长条。
 * 按「盒子」切分则与原版群系无关：河流、沼泽等落进盒子的部分一律变成湖，流进湖里就并入湖。
 * <p>
 * 深度：切分 depth 范围落在 [0, {@link #LAKE_MAX_DEPTH}] 内的所有点，包括地表点（0、1）和
 * 溶洞 / 繁茂洞穴的 [0.2, 0.9]。否则原地形较高的地方 y=62 处会是溶洞，
 * {@code worldgen/MagicLakeShallowsFeature} 只认 y=62 的群系，会跳过这些列不整平。深暗之域（depth 1.1）不受影响。
 * <p>
 * 盒子外的部分完全是原版，所以 Region 权重只决定「多大比例的世界有可能出湖」，不影响单片湖的大小。
 * TerraBlender 只决定群系，不改地形噪声；原地形起伏交给整平 Feature 压成浅水 + 低矮小岛。
 */
public final class MagicLakeRegion extends Region {

    /**
     * 湖盒子温度下界（原版 temperatures[1] 起点）。排除最冷的冰冻档，湖不会紧挨雪原 / 冰刺之地。
     * 温度噪声频率最低（最低八度 -10），很少切到湖的团块中间。
     */
    private static final float LAKE_TEMPERATURE_MIN = -0.45f;
    /** 湖盒子温度上界（原版 temperatures[3] 终点）。排除最热档，湖不会紧挨沙漠 / 恶地。 */
    private static final float LAKE_TEMPERATURE_MAX = 0.55f;

    /**
     * 湖盒子湿度下界：<b>控制单片湖大小的主旋钮</b>。原版黑森林用的是 ≥ 0.3，0.35 预计直径约 250–500 格。
     * 调小（如 0.3）→ 湖更大、更多、更容易连成片；调大（如 0.45）→ 湖更小、更少、更圆。上界固定为 1。
     */
    private static final float LAKE_HUMIDITY_MIN = 0.35f;

    /** 湖盒子大陆度下界（原版内陆起点 -0.11）。只在内陆生成，碰到海岸线（沙滩 / 石岸）就停。 */
    private static final float LAKE_CONTINENTALNESS_MIN = -0.11f;

    /**
     * 湖盒子侵蚀度下界（原版 erosions[3] 起点）。丘陵（第 3 档）并入湖，由整平 Feature 压成矮岛；
     * 第 0–2 档的山峰 / 高原 / 风袭地形留给原版。调大到 0.05 → 丘陵不并入，团块碰到丘陵会缺一块；
     * 调小到 -0.375 → 连山坡也并入，湖更完整但湖边贴着高山时坡很陡。
     */
    private static final float LAKE_EROSION_MIN = -0.2225f;

    /**
     * 湖盒子深度上界：depth 范围完全落在 [0, 此值] 内的原版点才切分。
     * 1.0 覆盖地表点（0、1）和溶洞 / 繁茂洞穴（0.2–0.9），湖下方约 128 格内都是魔法之湖；深暗之域（1.1）不动。
     */
    private static final float LAKE_MAX_DEPTH = 1.0f;

    /** 气候参数上界（原版各轴 span 的最大值都是 1.0）。 */
    private static final float CLIMATE_AXIS_MAX = 1.0f;

    private static final Climate.Parameter LAKE_TEMPERATURE_RANGE =
            Climate.Parameter.span(LAKE_TEMPERATURE_MIN, LAKE_TEMPERATURE_MAX);
    private static final Climate.Parameter LAKE_HUMIDITY_RANGE =
            Climate.Parameter.span(LAKE_HUMIDITY_MIN, CLIMATE_AXIS_MAX);
    private static final Climate.Parameter LAKE_CONTINENTALNESS_RANGE =
            Climate.Parameter.span(LAKE_CONTINENTALNESS_MIN, CLIMATE_AXIS_MAX);
    private static final Climate.Parameter LAKE_EROSION_RANGE =
            Climate.Parameter.span(LAKE_EROSION_MIN, CLIMATE_AXIS_MAX);
    private static final long LAKE_DEPTH_MIN_QUANTIZED = Climate.quantizeCoord(0.0f);
    private static final long LAKE_DEPTH_MAX_QUANTIZED = Climate.quantizeCoord(LAKE_MAX_DEPTH);

    public MagicLakeRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        // knownPresets 内部调用的就是原版 OverworldBiomeBuilder#addBiomes（该方法 protected，无法直接调用）
        List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> vanillaOverworldPoints = MultiNoiseBiomeSourceParameterList
                .knownPresets()
                .get(MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD)
                .values();
        int carvedPointCount = 0;
        for (Pair<Climate.ParameterPoint, ResourceKey<Biome>> pointAndBiome : vanillaOverworldPoints) {
            Climate.ParameterPoint vanillaPoint = pointAndBiome.getFirst();
            if (isDepthInsideLakeBox(vanillaPoint) && overlapsLakeBox(vanillaPoint)) {
                carveLakeBox(vanillaPoint, pointAndBiome.getSecond(), mapper);
                carvedPointCount++;
            } else {
                mapper.accept(pointAndBiome);
            }
        }
        EldenRingSpellsMod.LOGGER.info(
                "Magic Lake region carved {} of {} vanilla climate points.",
                carvedPointCount,
                vanillaOverworldPoints.size()
        );
    }

    /** depth 范围完全落在 [0, LAKE_MAX_DEPTH] 内：地表点和浅层洞穴点切分，深暗之域等更深的点不动。 */
    private static boolean isDepthInsideLakeBox(Climate.ParameterPoint point) {
        Climate.Parameter depth = point.depth();
        return depth.min() >= LAKE_DEPTH_MIN_QUANTIZED && depth.max() <= LAKE_DEPTH_MAX_QUANTIZED;
    }

    /** 四个切分轴上都与湖盒子有正长度的重叠（只碰到边界不算）。 */
    private static boolean overlapsLakeBox(Climate.ParameterPoint point) {
        for (CarvedAxis axis : CarvedAxis.values()) {
            Climate.Parameter pointRange = axis.get(point);
            Climate.Parameter lakeRange = axis.lakeRange();
            if (Math.max(pointRange.min(), lakeRange.min()) >= Math.min(pointRange.max(), lakeRange.max())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 把一个与湖盒子重叠的原版点切成若干块：依次沿四个轴，落在湖盒子外的上下两段各输出一块（保持原版群系），
     * 收窄到盒子内后继续切下一个轴；四个轴都切完剩下的那块整体落在盒子内，输出为魔法之湖。
     * 各块互不重叠且拼起来正好是原点，所以盒子外的最近邻查找结果与原版完全一致。
     */
    private static void carveLakeBox(
            Climate.ParameterPoint vanillaPoint,
            ResourceKey<Biome> vanillaBiome,
            Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper
    ) {
        Climate.ParameterPoint remainingPoint = vanillaPoint;
        for (CarvedAxis axis : CarvedAxis.values()) {
            Climate.Parameter pointRange = axis.get(remainingPoint);
            Climate.Parameter lakeRange = axis.lakeRange();
            long insideMin = Math.max(pointRange.min(), lakeRange.min());
            long insideMax = Math.min(pointRange.max(), lakeRange.max());
            if (pointRange.min() < insideMin) {
                mapper.accept(Pair.of(
                        axis.with(remainingPoint, new Climate.Parameter(pointRange.min(), insideMin)),
                        vanillaBiome
                ));
            }
            if (insideMax < pointRange.max()) {
                mapper.accept(Pair.of(
                        axis.with(remainingPoint, new Climate.Parameter(insideMax, pointRange.max())),
                        vanillaBiome
                ));
            }
            remainingPoint = axis.with(remainingPoint, new Climate.Parameter(insideMin, insideMax));
        }
        mapper.accept(Pair.of(remainingPoint, ModBiomes.MAGIC_LAKE));
    }

    /**
     * 参与切分的四个气候轴。depth / weirdness / offset 保持原点不变（weirdness 不限制：河谷也并入湖）。
     */
    private enum CarvedAxis {
        TEMPERATURE,
        HUMIDITY,
        CONTINENTALNESS,
        EROSION;

        Climate.Parameter lakeRange() {
            return switch (this) {
                case TEMPERATURE -> LAKE_TEMPERATURE_RANGE;
                case HUMIDITY -> LAKE_HUMIDITY_RANGE;
                case CONTINENTALNESS -> LAKE_CONTINENTALNESS_RANGE;
                case EROSION -> LAKE_EROSION_RANGE;
            };
        }

        Climate.Parameter get(Climate.ParameterPoint point) {
            return switch (this) {
                case TEMPERATURE -> point.temperature();
                case HUMIDITY -> point.humidity();
                case CONTINENTALNESS -> point.continentalness();
                case EROSION -> point.erosion();
            };
        }

        Climate.ParameterPoint with(Climate.ParameterPoint point, Climate.Parameter range) {
            return switch (this) {
                case TEMPERATURE -> new Climate.ParameterPoint(range, point.humidity(), point.continentalness(),
                        point.erosion(), point.depth(), point.weirdness(), point.offset());
                case HUMIDITY -> new Climate.ParameterPoint(point.temperature(), range, point.continentalness(),
                        point.erosion(), point.depth(), point.weirdness(), point.offset());
                case CONTINENTALNESS -> new Climate.ParameterPoint(point.temperature(), point.humidity(), range,
                        point.erosion(), point.depth(), point.weirdness(), point.offset());
                case EROSION -> new Climate.ParameterPoint(point.temperature(), point.humidity(), point.continentalness(),
                        range, point.depth(), point.weirdness(), point.offset());
            };
        }
    }
}

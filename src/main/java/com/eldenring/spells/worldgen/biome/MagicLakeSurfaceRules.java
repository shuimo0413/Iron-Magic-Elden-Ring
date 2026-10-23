package com.eldenring.spells.worldgen.biome;

import com.eldenring.spells.registry.ModBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * 魔法之湖地表规则（经 TerraBlender {@code SurfaceRuleManager} 按命名空间注册）。
 * <p>
 * 只接管表层两档（ON_FLOOR / UNDER_FLOOR），其余（深板岩、基岩等）返回空，由 TerraBlender 回退到原版规则。
 * <ul>
 *   <li>露出水面：以草方块为主，用原版地表噪声切出零星石头 / 安山岩 / 砂砾斑块，表现「裸露灰岩」</li>
 *   <li>水下：原版河床风格，砂砾为主，夹泥土 / 沙子 / 黏土斑块和零星石头 / 圆石碎石堆</li>
 * </ul>
 * 浅湖整平 Feature 自己铺新湖底时调用 {@link #lakebedStateAt}，与这里的水下规则用同一组原版噪声、
 * 同样的阈值和优先级，所以天然湖底和整平出来的湖底是同一张分布图，交界处不出接缝。
 * 阈值是外观参数，写死不进 toml。
 * <p>
 * 原版地表噪声（{@link Noises}）取值大致在 [-1, 1]、集中在 0 附近；下面的比例是粗略估计。
 */
public final class MagicLakeSurfaceRules {

    /**
     * 原版 {@code surface} 噪声高于此值 → 石头斑块。取值大致在 [-1, 1]；
     * 调小 → 石头斑块更多更大，调大 → 更零星。
     */
    private static final double STONE_PATCH_SURFACE_NOISE_MIN = 0.30;

    /**
     * 原版 {@code surface} 噪声低于此值 → 安山岩斑块（与石头斑块在噪声两端，互不重叠）。
     * 调大（往 0 靠）→ 安山岩更多。
     */
    private static final double ANDESITE_PATCH_SURFACE_NOISE_MAX = -0.36;

    /**
     * 原版 {@code gravel} 噪声落在 ±此值内 → 岸边砂砾细带。调大 → 砂砾带更宽。
     */
    private static final double SHORE_GRAVEL_NOISE_HALF_WIDTH = 0.04;

    /**
     * 湖底碎石堆：原版 {@code ice} 噪声（较高频，斑块约十几格）高于此值 → 石头 / 圆石，约占湖底 15%。
     * 优先级最高。调小 → 碎石堆更多更大；调大 → 更零星。
     */
    private static final double LAKEBED_RUBBLE_ICE_NOISE_MIN = 0.45;

    /**
     * 碎石堆内部：原版 {@code surface_swamp} 噪声（高频）高于此值 → 圆石，否则石头。
     * 0 = 两者约各半、细碎交错。调大 → 石头更多；调小 → 圆石更多。
     */
    private static final double LAKEBED_COBBLESTONE_SWAMP_NOISE_MIN = 0.0;

    /**
     * 湖底黏土：原版 {@code surface_secondary} 噪声高于此值 → 黏土，约占湖底 12%（不含碎石堆处）。
     * 调小 → 黏土更多；调大 → 更少。
     */
    private static final double LAKEBED_CLAY_SURFACE_SECONDARY_NOISE_MIN = 0.45;

    /**
     * 湖底底色：原版 {@code patch} 噪声低于此值 → 泥土。调大（往 0 靠）→ 泥土更多、湖底更暗。
     */
    private static final double LAKEBED_DIRT_PATCH_NOISE_MAX = -0.30;

    /**
     * 湖底底色：原版 {@code patch} 噪声高于此值 → 沙子；介于泥土阈值和此值之间 → 砂砾（底色主体）。
     * 调小（往 0 靠）→ 沙子更多、湖底更亮。
     */
    private static final double LAKEBED_SAND_PATCH_NOISE_MIN = 0.35;

    private MagicLakeSurfaceRules() {
    }

    /**
     * 构建魔法之湖地表规则。只在 {@code abovePreliminarySurface} 之上生效，避免洞穴地面也铺草。
     */
    public static SurfaceRules.RuleSource build() {
        SurfaceRules.ConditionSource isMagicLake = SurfaceRules.isBiome(ModBiomes.MAGIC_LAKE);
        // 与原版草地判定一致：方块上方没有水 → 露出水面
        SurfaceRules.ConditionSource isAboveWater = SurfaceRules.waterBlockCheck(0, 0);

        SurfaceRules.ConditionSource stonePatch = SurfaceRules.noiseCondition(
                Noises.SURFACE, STONE_PATCH_SURFACE_NOISE_MIN, Double.MAX_VALUE);
        SurfaceRules.ConditionSource andesitePatch = SurfaceRules.noiseCondition(
                Noises.SURFACE, -Double.MAX_VALUE, ANDESITE_PATCH_SURFACE_NOISE_MAX);
        SurfaceRules.ConditionSource shoreGravelBand = SurfaceRules.noiseCondition(
                Noises.GRAVEL, -SHORE_GRAVEL_NOISE_HALF_WIDTH, SHORE_GRAVEL_NOISE_HALF_WIDTH);

        // 以下湖底条件必须与 lakebedStateAt 一一对应（噪声、阈值、闭区间、优先级）
        SurfaceRules.ConditionSource lakebedRubble = SurfaceRules.noiseCondition(
                Noises.ICE, LAKEBED_RUBBLE_ICE_NOISE_MIN, Double.MAX_VALUE);
        SurfaceRules.ConditionSource lakebedCobblestone = SurfaceRules.noiseCondition(
                Noises.SWAMP, LAKEBED_COBBLESTONE_SWAMP_NOISE_MIN, Double.MAX_VALUE);
        SurfaceRules.ConditionSource lakebedClay = SurfaceRules.noiseCondition(
                Noises.SURFACE_SECONDARY, LAKEBED_CLAY_SURFACE_SECONDARY_NOISE_MIN, Double.MAX_VALUE);
        SurfaceRules.ConditionSource lakebedDirt = SurfaceRules.noiseCondition(
                Noises.PATCH, -Double.MAX_VALUE, LAKEBED_DIRT_PATCH_NOISE_MAX);
        SurfaceRules.ConditionSource lakebedSand = SurfaceRules.noiseCondition(
                Noises.PATCH, LAKEBED_SAND_PATCH_NOISE_MIN, Double.MAX_VALUE);

        // 岩斑在表层和次表层用同一噪声，挖下去仍是石头，不会出现「一层皮」
        SurfaceRules.RuleSource rockPatches = SurfaceRules.sequence(
                SurfaceRules.ifTrue(stonePatch, state(Blocks.STONE)),
                SurfaceRules.ifTrue(andesitePatch, state(Blocks.ANDESITE))
        );

        SurfaceRules.RuleSource dryFloor = SurfaceRules.sequence(
                rockPatches,
                SurfaceRules.ifTrue(shoreGravelBand, state(Blocks.GRAVEL)),
                state(Blocks.GRASS_BLOCK)
        );
        SurfaceRules.RuleSource lakebedFloor = SurfaceRules.sequence(
                SurfaceRules.ifTrue(lakebedRubble, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(lakebedCobblestone, state(Blocks.COBBLESTONE)),
                        state(Blocks.STONE)
                )),
                SurfaceRules.ifTrue(lakebedClay, state(Blocks.CLAY)),
                SurfaceRules.ifTrue(lakebedDirt, state(Blocks.DIRT)),
                SurfaceRules.ifTrue(lakebedSand, state(Blocks.SAND)),
                state(Blocks.GRAVEL)
        );

        SurfaceRules.RuleSource floor = SurfaceRules.sequence(
                SurfaceRules.ifTrue(isAboveWater, dryFloor),
                lakebedFloor
        );
        // 水下次表层：碎石堆下面仍是石头，其余垫泥土（原版河床下面也是泥土 / 石头）
        SurfaceRules.RuleSource underFloor = SurfaceRules.sequence(
                SurfaceRules.ifTrue(isAboveWater, SurfaceRules.sequence(rockPatches, state(Blocks.DIRT))),
                SurfaceRules.ifTrue(lakebedRubble, state(Blocks.STONE)),
                state(Blocks.DIRT)
        );

        return SurfaceRules.ifTrue(
                isMagicLake,
                SurfaceRules.ifTrue(
                        SurfaceRules.abovePreliminarySurface(),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, floor),
                                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, underFloor)
                        )
                )
        );
    }

    /**
     * 某一列的湖底表层方块，与 {@link #build()} 的水下 ON_FLOOR 规则逐条等价：
     * 碎石堆（圆石 / 石头）&gt; 黏土 &gt; 泥土 / 沙子 &gt; 砂砾。
     * 原版 {@code noiseCondition} 以 (blockX, 0, blockZ) 采样、上下界都是闭区间，这里保持一致。
     * <p>
     * 噪声实例由 {@link RandomState} 按世界种子缓存（线程安全），整平 Feature 每列调用开销很小。
     */
    public static BlockState lakebedStateAt(RandomState randomState, int blockX, int blockZ) {
        if (sampleSurfaceNoise(randomState, Noises.ICE, blockX, blockZ) >= LAKEBED_RUBBLE_ICE_NOISE_MIN) {
            return sampleSurfaceNoise(randomState, Noises.SWAMP, blockX, blockZ) >= LAKEBED_COBBLESTONE_SWAMP_NOISE_MIN
                    ? Blocks.COBBLESTONE.defaultBlockState()
                    : Blocks.STONE.defaultBlockState();
        }
        if (sampleSurfaceNoise(randomState, Noises.SURFACE_SECONDARY, blockX, blockZ) >= LAKEBED_CLAY_SURFACE_SECONDARY_NOISE_MIN) {
            return Blocks.CLAY.defaultBlockState();
        }
        double patchNoise = sampleSurfaceNoise(randomState, Noises.PATCH, blockX, blockZ);
        if (patchNoise <= LAKEBED_DIRT_PATCH_NOISE_MAX) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (patchNoise >= LAKEBED_SAND_PATCH_NOISE_MIN) {
            return Blocks.SAND.defaultBlockState();
        }
        return Blocks.GRAVEL.defaultBlockState();
    }

    private static double sampleSurfaceNoise(
            RandomState randomState,
            ResourceKey<NormalNoise.NoiseParameters> noiseKey,
            int blockX,
            int blockZ
    ) {
        return randomState.getOrCreateNoise(noiseKey).getValue(blockX, 0.0, blockZ);
    }

    private static SurfaceRules.RuleSource state(Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }
}

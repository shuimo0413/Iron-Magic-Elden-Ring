package com.eldenring.spells.worldgen.biome;

import com.eldenring.spells.registry.ModBiomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * 魔法之湖地表规则（经 TerraBlender {@code SurfaceRuleManager} 按命名空间注册）。
 * <p>
 * 只接管表层两档（ON_FLOOR / UNDER_FLOOR），其余（深板岩、基岩等）返回空，由 TerraBlender 回退到原版规则。
 * <ul>
 *   <li>露出水面：以草方块为主，用原版地表噪声切出零星石头 / 安山岩 / 砂砾斑块，表现「裸露灰岩」</li>
 *   <li>水下：砾石 / 泥 / 黏土按噪声混搭，偏灰的湖底</li>
 * </ul>
 * 浅湖整平 Feature 会自己给新湖底铺材质，不依赖这里的结果。
 * 阈值是外观参数，写死不进 toml。
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
     * 原版 {@code gravel} 噪声高于此值 → 湖底砾石。调小 → 湖底砾石更多。
     */
    private static final double LAKEBED_GRAVEL_NOISE_MIN = 0.0;

    /**
     * 原版 {@code surface_swamp} 噪声高于此值 → 湖底泥块；其余湖底为黏土。调小 → 泥更多、湖底更暗。
     */
    private static final double LAKEBED_MUD_NOISE_MIN = 0.25;

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
        SurfaceRules.ConditionSource lakebedGravel = SurfaceRules.noiseCondition(
                Noises.GRAVEL, LAKEBED_GRAVEL_NOISE_MIN, Double.MAX_VALUE);
        SurfaceRules.ConditionSource lakebedMud = SurfaceRules.noiseCondition(
                Noises.SWAMP, LAKEBED_MUD_NOISE_MIN, Double.MAX_VALUE);

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
                SurfaceRules.ifTrue(lakebedGravel, state(Blocks.GRAVEL)),
                SurfaceRules.ifTrue(lakebedMud, state(Blocks.MUD)),
                state(Blocks.CLAY)
        );

        SurfaceRules.RuleSource floor = SurfaceRules.sequence(
                SurfaceRules.ifTrue(isAboveWater, dryFloor),
                lakebedFloor
        );
        SurfaceRules.RuleSource underFloor = SurfaceRules.sequence(
                rockPatches,
                SurfaceRules.ifTrue(isAboveWater, state(Blocks.DIRT)),
                state(Blocks.CLAY)
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

    private static SurfaceRules.RuleSource state(Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }
}

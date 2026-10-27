package com.eldenring.spells.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;
import java.util.function.Supplier;

/**
 * 读写 {@link BiomeSource} 的 {@code possibleBiomes} 供应商。
 * <p>
 * TerraBlender 会在 {@code ServerAboutToStart} 里把 Region 群系 append 进这份集合；
 * {@code /locate biome} 与 Nature's Compass（群系指南针）的维度标注都先查它。
 * 若 deferred 注入漏了魔法之湖，用本 Accessor 补进去（见 {@code MagicLakeLocatorCompat}）。
 */
@Mixin(BiomeSource.class)
public interface BiomeSourcePossibleBiomesAccessor {

    @Accessor("possibleBiomes")
    Supplier<Set<Holder<Biome>>> eldenRingSpells$getPossibleBiomes();

    @Accessor("possibleBiomes")
    void eldenRingSpells$setPossibleBiomes(Supplier<Set<Holder<Biome>>> possibleBiomes);
}

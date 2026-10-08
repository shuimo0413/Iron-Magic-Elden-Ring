package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

/**
 * 本模组伤害类型 ResourceKey。实际条目由数据包 {@code data/.../damage_type/*.json} 提供。
 * <p>
 * 须同步挂到 {@code #iss_elden_ring:glintstone_magic}，并经由
 * {@code data/forge/tags/damage_type/is_magic.json} 并入 {@code #forge:is_magic}，
 * 否则伤害数字 / 其它模组会把它当成物理伤害（铁魔法本体各学派同此约定）。 */
public final class ModDamageTypes {
    /**
     * 辉石魔法伤害：所有辉石学派法术的默认 DamageSource 类型（含起源三咒：毁灭流星 / 创星雨 / 彗星亚兹勒）。
     * 类型本身不挂 {@code bypasses_armor}；起源三咒与魔砾一样走默认 {@code SpellDamageSource}。
     */
    public static final ResourceKey<DamageType> GLINTSTONE_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(EldenRingSpellsMod.MOD_ID, "glintstone_magic")
    );

    private ModDamageTypes() {
    }
}

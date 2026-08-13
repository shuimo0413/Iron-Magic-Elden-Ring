package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

/**
 * 本模组伤害类型 ResourceKey。实际条目由数据包 {@code data/.../damage_type/*.json} 提供。
 */
public final class ModDamageTypes {
    /**
     * 辉石魔法伤害：所有辉石学派法术的默认 DamageSource 类型。
     */
    public static final ResourceKey<DamageType> GLINTSTONE_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "glintstone_magic")
    );

    private ModDamageTypes() {
    }
}

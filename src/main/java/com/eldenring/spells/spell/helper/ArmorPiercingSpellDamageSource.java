package com.eldenring.spells.spell.helper;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * 无视护甲的铁魔法法术伤害源。起源三咒（彗星亚兹勒 / 毁灭流星 / 创星雨）专用。
 * <p>
 * 伤害类型仍是学派的 {@code iss_elden_ring:glintstone_magic}，死亡信息、{@code is_magic} 标签、
 * 辉石抗性与铁魔法法抗照常生效；只额外对 {@link DamageTypeTags#BYPASSES_ARMOR} 回答 true，
 * 原版据此跳过护甲减伤与护甲耐久扣除。
 * <p>
 * 不把 {@code glintstone_magic} 整体挂进 {@code minecraft:bypasses_armor}：那会让所有辉石咒都无视护甲。
 */
public class ArmorPiercingSpellDamageSource extends SpellDamageSource {

    protected ArmorPiercingSpellDamageSource(@NotNull Entity directEntity, @NotNull Entity causingEntity, AbstractSpell spell) {
        super(directEntity, causingEntity, null, spell);
    }

    /**
     * 与 {@link SpellDamageSource#source(Entity, Entity, AbstractSpell)} 同参，供法术的 {@code getDamageSource} 替换默认实现。
     */
    public static SpellDamageSource source(@NotNull Entity directEntity, @NotNull Entity causingEntity, @NotNull AbstractSpell spell) {
        return new ArmorPiercingSpellDamageSource(directEntity, causingEntity, spell);
    }

    @Override
    public boolean is(@NotNull TagKey<DamageType> damageTypeTag) {
        return DamageTypeTags.BYPASSES_ARMOR.equals(damageTypeTag) || super.is(damageTypeTag);
    }
}

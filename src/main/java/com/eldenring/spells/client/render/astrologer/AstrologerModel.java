package com.eldenring.spells.client.render.astrologer;

import com.eldenring.spells.EldenRingSpellsMod;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import net.minecraft.resources.ResourceLocation;

/**
 * 观星者模型：复用铁魔法通用施法者骨骼与动画，身体贴图沿用通用法师纹理。
 * 外观主体由星辰护甲装备层覆盖。
 */
public final class AstrologerModel extends AbstractSpellCastingMobModel {
    @Override
    public ResourceLocation getTextureResource(AbstractSpellCastingMob animatable) {
        return ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/astrologer.png");
    }
}

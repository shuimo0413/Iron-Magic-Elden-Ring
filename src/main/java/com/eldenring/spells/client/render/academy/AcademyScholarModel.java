package com.eldenring.spells.client.render.academy;

import com.eldenring.spells.EldenRingSpellsMod;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import net.minecraft.resources.ResourceLocation;

/**
 * 学院学者模型：与观星者相同，复用铁魔法通用施法者骨骼与动画，只换身体贴图（拷自观星者）。
 * 外观主体由长袍与辉石头冠装备层覆盖；四间教室共用同一张身体贴图。
 */
public final class AcademyScholarModel extends AbstractSpellCastingMobModel {

    private static final ResourceLocation BODY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/academy_scholar.png");

    @Override
    public ResourceLocation getTextureResource(AbstractSpellCastingMob animatable) {
        return BODY_TEXTURE;
    }
}

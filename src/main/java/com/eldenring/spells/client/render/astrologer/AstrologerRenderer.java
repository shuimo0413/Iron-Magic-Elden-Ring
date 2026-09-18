package com.eldenring.spells.client.render.astrologer;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * 观星者渲染器：复用铁魔法通用施法者渲染管线与装备层。
 */
public final class AstrologerRenderer extends AbstractSpellCastingMobRenderer {
    public AstrologerRenderer(EntityRendererProvider.Context context) {
        super(context, new AstrologerModel());
    }
}

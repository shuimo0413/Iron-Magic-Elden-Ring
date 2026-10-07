package com.eldenring.spells.mixin;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 访问铁魔法卷轴锻造台内部 {@code SpellCardInfo} 的法术与按钮字段，
 * 供 {@link ScrollForgeScreenMixin} 在列表生成后按配方表剔除卡片。
 */
// remap = false：目标是铁魔法自己的类与字段，成品 jar 里不混淆，refmap 里查不到映射
@Mixin(targets = "io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeScreen$SpellCardInfo", remap = false)
public interface ScrollForgeSpellCardAccessor {

    @Accessor("spell")
    AbstractSpell eldenRingSpells$getSpell();

    @Accessor("button")
    Button eldenRingSpells$getButton();
}

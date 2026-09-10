package com.eldenring.spells.mixin;

import com.eldenring.spells.recipe.GlintstoneScrollRecipes;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 卷轴锻造台服务端：抄写结果槽校验焦点是否与 {@link GlintstoneScrollRecipes} 一致。
 * <p>
 * 仅改客户端列表不够——玩家仍可通过选咒数据包指定任意辉石咒；
 * 这里在 {@code setupResultSlot} 阻断错误焦点产出卷轴。
 */
@Mixin(ScrollForgeMenu.class)
public abstract class ScrollForgeMenuMixin {

    @Shadow
    public abstract Slot getFocusSlot();

    /**
     * 替换 {@code setupResultSlot} 里对 {@link AbstractSpell#allowCrafting()} 的调用，
     * 叠加本模组配方焦点校验。
     */
    @Redirect(
            method = "setupResultSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;allowCrafting()Z"
            )
    )
    private boolean eldenRingSpells$requireMappedFocus(AbstractSpell spell) {
        if (!spell.allowCrafting()) {
            return false;
        }
        ItemStack focusStack = this.getFocusSlot().getItem();
        return GlintstoneScrollRecipes.matchesFocus(spell, focusStack);
    }
}

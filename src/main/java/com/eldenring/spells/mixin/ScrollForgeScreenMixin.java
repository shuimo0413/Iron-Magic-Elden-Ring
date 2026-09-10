package com.eldenring.spells.mixin;

import com.eldenring.spells.recipe.GlintstoneScrollRecipes;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 卷轴锻造台 GUI：按 {@link GlintstoneScrollRecipes} 过滤可选法术列表。
 * <p>
 * 不能用 {@code @Redirect} 拦截 {@code AbstractSpell::allowCrafting}——流式方法引用
 * 会编成 invokedynamic，Mixin 扫不到调用点。改为在 {@code generateSpellList} 返回后
 * 剔除不匹配焦点的卡片，并用 {@link ScreenRemoveWidgetInvoker} 卸掉按钮，避免残留可点控件。
 */
@Mixin(ScrollForgeScreen.class)
public abstract class ScrollForgeScreenMixin {

    /** 铁魔法原版字段类型为 {@code List<SpellCardInfo>}；用原始类型以便 removeIf。 */
    @Shadow
    @SuppressWarnings("rawtypes")
    private List availableSpells;

    /**
     * 列表生成完毕后：本模组法术仅保留焦点与配方表一致的项。
     */
    @Inject(method = "generateSpellList", at = @At("RETURN"))
    @SuppressWarnings("unchecked")
    private void eldenRingSpells$filterSpellsByFocusRecipe(CallbackInfo callbackInfo) {
        ItemStack focusStack = ((ScrollForgeScreen) (Object) this).getMenu().getFocusSlot().getItem();
        ScreenRemoveWidgetInvoker screen = (ScreenRemoveWidgetInvoker) this;
        this.availableSpells.removeIf(cardObject -> {
            ScrollForgeSpellCardAccessor card = (ScrollForgeSpellCardAccessor) cardObject;
            if (GlintstoneScrollRecipes.matchesFocus(card.eldenRingSpells$getSpell(), focusStack)) {
                return false;
            }
            screen.eldenRingSpells$removeWidget(card.eldenRingSpells$getButton());
            return true;
        });
    }
}

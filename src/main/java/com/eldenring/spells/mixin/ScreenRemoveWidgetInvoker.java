package com.eldenring.spells.mixin;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 调用原版 {@link Screen#removeWidget}（声明在 Screen，不在 ScrollForgeScreen），
 * 避免在子类 Mixin 上 {@code @Shadow} 找不到方法。
 */
@Mixin(Screen.class)
public interface ScreenRemoveWidgetInvoker {

    @Invoker("removeWidget")
    void eldenRingSpells$removeWidget(GuiEventListener listener);
}

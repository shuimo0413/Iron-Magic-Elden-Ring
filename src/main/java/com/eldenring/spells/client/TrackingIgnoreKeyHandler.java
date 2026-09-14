package com.eldenring.spells.client;

import com.eldenring.spells.EldenRingSpellsMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 检测默认 X+C 和弦：修饰键按住且打开键刚按下、且当前无 Screen 时打开追踪排除菜单。
 * 有 MaFgLib 时打开 malilib 风格界面（可与 Tweakerge 右上角互切）；否则简易屏。
 */
@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID, value = Dist.CLIENT)
public final class TrackingIgnoreKeyHandler {
    private TrackingIgnoreKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        if (ModKeyMappings.trackingIgnoreMenuModifier == null || ModKeyMappings.trackingIgnoreMenuOpen == null) {
            return;
        }
        if (!ModKeyMappings.trackingIgnoreMenuModifier.isDown()) {
            return;
        }
        if (!ModKeyMappings.trackingIgnoreMenuOpen.consumeClick()) {
            return;
        }
        TrackingIgnoreGuiOpener.open();
    }
}

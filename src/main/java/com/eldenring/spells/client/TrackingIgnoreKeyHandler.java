package com.eldenring.spells.client;

import com.eldenring.spells.EldenRingSpellsMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
/**
 * 检测默认 X+C 和弦：修饰键按住且打开键刚按下、且当前无 Screen 时打开追踪排除菜单。
 * 有 MaFgLib 时打开 malilib 风格界面（可与 Tweakerge 右上角互切）；否则简易屏。
 */
@Mod.EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID, value = Dist.CLIENT)
public final class TrackingIgnoreKeyHandler {
    private TrackingIgnoreKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // 1.20.1 的 ClientTickEvent 每 tick 有 PRE / END 两次；迁移前只订阅过 NeoForge 的 Post 阶段（等价 END）。
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
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

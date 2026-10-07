package com.eldenring.spells.client;

import com.eldenring.spells.EldenRingSpellsMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 检测追踪排除设置热键（默认 P）：在游戏内且当前没有打开任何界面时，按下即打开 {@link TrackingIgnoreScreen}。
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
        if (ModKeyMappings.trackingIgnoreMenu == null || !ModKeyMappings.trackingIgnoreMenu.consumeClick()) {
            return;
        }
        minecraft.setScreen(new TrackingIgnoreScreen());
    }
}

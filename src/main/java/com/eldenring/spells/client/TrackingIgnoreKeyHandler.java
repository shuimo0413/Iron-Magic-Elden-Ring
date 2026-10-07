package com.eldenring.spells.client;

import com.eldenring.spells.EldenRingSpellsMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
/**
 * 检测追踪排除设置热键（默认 P）：在游戏内且当前没有打开任何界面时，按下即打开 {@link TrackingIgnoreScreen}。
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
        if (ModKeyMappings.trackingIgnoreMenu == null || !ModKeyMappings.trackingIgnoreMenu.consumeClick()) {
            return;
        }
        minecraft.setScreen(new TrackingIgnoreScreen());
    }
}

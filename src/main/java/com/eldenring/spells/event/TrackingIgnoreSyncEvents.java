package com.eldenring.spells.event;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.network.TrackingIgnorePrefsPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 登录 / 重生时把追踪排除偏好同步到客户端，保证 GUI 与存档一致。
 */
@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID)
public final class TrackingIgnoreSyncEvents {
    private TrackingIgnoreSyncEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            TrackingIgnorePrefsPayload.syncTo(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            TrackingIgnorePrefsPayload.syncTo(serverPlayer);
        }
    }
}

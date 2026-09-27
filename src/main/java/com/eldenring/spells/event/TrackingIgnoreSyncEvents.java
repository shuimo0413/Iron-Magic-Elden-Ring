package com.eldenring.spells.event;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.network.TrackingIgnorePrefsPayload;
import com.eldenring.spells.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
/**
 * 登录 / 重生时把追踪排除偏好同步到客户端，保证 GUI 与存档一致；
 * 另外承接原 NeoForge attachment {@code copyOnDeath()} 的语义：死亡重生时把偏好复制到新实体。
 */
@Mod.EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID)
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

    /**
     * 等价于 NeoForge {@code AttachmentType#copyOnDeath()}：Forge 能力本身不带跨死亡复制，
     * 必须在 Clone 事件里手动搬运。只在真死亡时复制；换维度 / 传送触发的 Clone 应保持新实体默认值。
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            ModAttachments.setPrefs(event.getEntity(), ModAttachments.getPrefs(event.getOriginal()));
        }
    }
}

package com.eldenring.spells.network;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModAttachments;
import com.eldenring.spells.tracking.TrackingIgnorePrefs;
import com.eldenring.spells.tracking.TrackingIgnorePrefsCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 辉石追踪排除偏好同步。
 * <ul>
 *   <li>C2S：客户端 GUI 勾选变更 → 服务端写入 Attachment → 回推 S2C</li>
 *   <li>S2C：登录 / 变更后刷新客户端缓存，供 GUI 显示</li>
 * </ul>
 */
public record TrackingIgnorePrefsPayload(TrackingIgnorePrefs prefs) implements CustomPacketPayload {
    public static final Type<TrackingIgnorePrefsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "tracking_ignore_prefs"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrackingIgnorePrefsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    TrackingIgnorePrefs.STREAM_CODEC,
                    TrackingIgnorePrefsPayload::prefs,
                    TrackingIgnorePrefsPayload::new
            );

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playBidirectional(TYPE, STREAM_CODEC, TrackingIgnorePrefsPayload::handle);
    }

    private static void handle(TrackingIgnorePrefsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                TrackingIgnorePrefsCache.acceptFromServer(payload.prefs());
                return;
            }
            if (context.player() instanceof ServerPlayer serverPlayer) {
                serverPlayer.setData(ModAttachments.TRACKING_IGNORE_PREFS.get(), payload.prefs());
                syncTo(serverPlayer);
            }
        });
    }

    /**
     * 把当前 Attachment 推给该玩家客户端（登录 / 改勾选后）。
     */
    public static void syncTo(ServerPlayer player) {
        if (!player.connection.hasChannel(TYPE)) {
            return;
        }
        TrackingIgnorePrefs prefs = player.getData(ModAttachments.TRACKING_IGNORE_PREFS.get());
        PacketDistributor.sendToPlayer(player, new TrackingIgnorePrefsPayload(prefs));
    }

    /**
     * 客户端 GUI：把新偏好发给服务端。
     */
    public static void sendToServer(TrackingIgnorePrefs prefs) {
        PacketDistributor.sendToServer(new TrackingIgnorePrefsPayload(prefs));
    }

    @Override
    public Type<TrackingIgnorePrefsPayload> type() {
        return TYPE;
    }
}

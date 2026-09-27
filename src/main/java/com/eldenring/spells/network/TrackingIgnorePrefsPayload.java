package com.eldenring.spells.network;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModAttachments;
import com.eldenring.spells.tracking.TrackingIgnorePrefs;
import com.eldenring.spells.tracking.TrackingIgnorePrefsCache;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 辉石追踪排除偏好同步。
 * <ul>
 *   <li>C2S：客户端 GUI 勾选变更 → 服务端写入玩家偏好 → 回推 S2C</li>
 *   <li>S2C：登录 / 变更后刷新客户端缓存，供 GUI 显示</li>
 * </ul>
 * Forge 1.20.1 没有 {@code CustomPacketPayload} / {@code StreamCodec} / {@code IPayloadContext}，
 * 因此本类改成普通 POJO，并顺带承载本模组唯一的 {@link SimpleChannel}。
 */
public final class TrackingIgnorePrefsPayload {
    /**
     * 本模组唯一网络通道。Forge 1.20.1 的 {@link PacketDistributor} 只有「目标选择器」builder，
     * 真正的发送动作必须落在这条通道上，所以两个 payload 共用它。
     */
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(EldenRingSpellsMod.MOD_ID, "main"))
            .networkProtocolVersion(() -> "1")
            .clientAcceptedVersions("1"::equals)
            .serverAcceptedVersions("1"::equals)
            .simpleChannel();

    private final TrackingIgnorePrefs prefs;

    public TrackingIgnorePrefsPayload(TrackingIgnorePrefs prefs) {
        this.prefs = prefs;
    }

    /** 网络解码构造器：字段顺序必须与 {@link #toBytes(FriendlyByteBuf)} 完全一致。 */
    public TrackingIgnorePrefsPayload(FriendlyByteBuf buffer) {
        this(TrackingIgnorePrefs.read(buffer));
    }

    public TrackingIgnorePrefs prefs() {
        return prefs;
    }

    public void toBytes(FriendlyByteBuf buffer) {
        prefs.writeTo(buffer);
    }

    /**
     * 由 {@code FMLCommonSetupEvent} 调用一次，注册本模组全部消息。
     * 替代旧 NeoForge 的 {@code RegisterPayloadHandlersEvent} 回调。
     */
    public static void register() {
        // 不带 NetworkDirection = 双向注册：本消息既 C2S（GUI 提交）又 S2C（服务端回推）。
        CHANNEL.messageBuilder(TrackingIgnorePrefsPayload.class, 0)
                .encoder(TrackingIgnorePrefsPayload::toBytes)
                .decoder(TrackingIgnorePrefsPayload::new)
                .consumerMainThread(TrackingIgnorePrefsPayload::handle)
                .add();
        CHANNEL.messageBuilder(AzurStaffSettingsPayload.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AzurStaffSettingsPayload::toBytes)
                .decoder(AzurStaffSettingsPayload::new)
                .consumerMainThread(AzurStaffSettingsPayload::handle)
                .add();
    }

    private static void handle(TrackingIgnorePrefsPayload message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                TrackingIgnorePrefsCache.acceptFromServer(message.prefs());
                return;
            }
            ServerPlayer serverPlayer = context.getSender();
            if (serverPlayer != null) {
                ModAttachments.setPrefs(serverPlayer, message.prefs());
                syncTo(serverPlayer);
            }
        });
    }

    /**
     * 把该玩家当前的偏好推给其客户端（登录 / 改勾选后）。
     */
    public static void syncTo(ServerPlayer player) {
        // 假玩家 / 尚未协商完自定义通道的连接不支持本模组消息，直接跳过。
        if (!CHANNEL.isRemotePresent(player.connection.connection)) {
            return;
        }
        TrackingIgnorePrefs prefs = ModAttachments.getPrefs(player);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new TrackingIgnorePrefsPayload(prefs));
    }

    /**
     * 客户端 GUI：把新偏好发给服务端。
     */
    public static void sendToServer(TrackingIgnorePrefs prefs) {
        CHANNEL.sendToServer(new TrackingIgnorePrefsPayload(prefs));
    }
}

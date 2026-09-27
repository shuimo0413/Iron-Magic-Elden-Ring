package com.eldenring.spells.network;

import com.eldenring.spells.item.AzurStaffBalance;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * 亚兹勒法杖的世界配置（吟唱减免 / 蓝耗倍率）S2C 同步。
 * <p>
 * Forge 1.20.1 没有 {@code CustomPacketPayload} / {@code StreamCodec} / {@code IPayloadContext}，
 * 本类改成普通 POJO，由 {@link TrackingIgnorePrefsPayload#CHANNEL} 负责注册与发送。
 */
public record AzurStaffSettingsPayload(double castTimeReduction, double manaCostMultiplier) {

    /** 网络解码构造器：字段顺序必须与 {@link #toBytes(FriendlyByteBuf)} 完全一致。 */
    public AzurStaffSettingsPayload(FriendlyByteBuf buffer) {
        this(buffer.readDouble(), buffer.readDouble());
    }

    public AzurStaffSettingsPayload {
        if (!Double.isFinite(castTimeReduction) || castTimeReduction < 0.0D || castTimeReduction > 1.0D
                || !Double.isFinite(manaCostMultiplier) || manaCostMultiplier < 1.0D || manaCostMultiplier > 10.0D) {
            throw new IllegalArgumentException("Invalid Azur staff settings");
        }
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeDouble(castTimeReduction);
        buffer.writeDouble(manaCostMultiplier);
    }

    public static void handle(AzurStaffSettingsPayload message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 集成服（单机存档）里客户端与逻辑服共享同一份内存数值，服务端值本就等于客户端值；
            // 迟到的 S2C 包若再覆盖，会把玩家刚在本地改好的设置弹回去。
            // NeoForge 的 Connection#isMemoryConnection 在 1.20.1 Forge 不存在，
            // 等价判断是「客户端当前是否跑着集成服」：单机为 true（跳过覆盖），
            // 专用客户端连远程服为 false（正常应用服务端配置）。
            // 本消息只注册为 PLAY_TO_CLIENT，因此这里必然运行在客户端线程。
            if (Minecraft.getInstance().hasSingleplayerServer()) {
                return;
            }
            AzurStaffBalance.acceptServerSettings(message.castTimeReduction(), message.manaCostMultiplier());
        });
    }
}

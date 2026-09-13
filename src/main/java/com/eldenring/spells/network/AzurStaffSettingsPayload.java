package com.eldenring.spells.network;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.item.AzurStaffBalance;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record AzurStaffSettingsPayload(double castTimeReduction, double manaCostMultiplier)
        implements CustomPacketPayload {
    public static final Type<AzurStaffSettingsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "azur_staff_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AzurStaffSettingsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE, AzurStaffSettingsPayload::castTimeReduction,
                    ByteBufCodecs.DOUBLE, AzurStaffSettingsPayload::manaCostMultiplier,
                    AzurStaffSettingsPayload::new);

    public AzurStaffSettingsPayload {
        if (!Double.isFinite(castTimeReduction) || castTimeReduction < 0.0D || castTimeReduction > 1.0D
                || !Double.isFinite(manaCostMultiplier) || manaCostMultiplier < 1.0D || manaCostMultiplier > 10.0D) {
            throw new IllegalArgumentException("Invalid Azur staff settings");
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, (payload, context) -> {
            // An integrated client already shares the server's values; delayed packets must not revert them.
            if (!context.connection().isMemoryConnection()) {
                AzurStaffBalance.acceptServerSettings(payload.castTimeReduction(), payload.manaCostMultiplier());
            }
        });
    }

    @Override
    public Type<AzurStaffSettingsPayload> type() {
        return TYPE;
    }
}

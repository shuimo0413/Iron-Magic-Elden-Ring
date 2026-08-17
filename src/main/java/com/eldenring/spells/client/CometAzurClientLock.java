package com.eldenring.spells.client;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModSpells;
import io.redspace.ironsspellbooks.network.casting.CancelCastPacket;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 客户端：施放彗星亚兹勒时锁死移动输入与视角。
 * <p>
 * 铁魔法本身只把施法移速乘到约 0.2；本类在之后把冲量清零，并把 yaw/pitch 钉回出手瞬间。
 * 角度在进入吟唱的第一帧捕获，与服务端 {@code onServerPreCast} 基本一致。
 * 铁魔法 CONTINUOUS 松手不会自动停，这里在右键抬起时发取消包，喷流立刻收掉。
 */
@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID, value = Dist.CLIENT)
public final class CometAzurClientLock {

    private static boolean lockActive;
    private static boolean cancelPacketSent;
    private static float lockedYawDegrees;
    private static float lockedPitchDegrees;
    private static Vec3 lockedFeetPosition = Vec3.ZERO;

    private CometAzurClientLock() {
    }

    private static boolean isLocalPlayerCastingCometAzur() {
        if (!ClientMagicData.isCasting()) {
            return false;
        }
        String castingSpellId = ClientMagicData.getCastingSpellId();
        return castingSpellId != null
                && castingSpellId.equals(ModSpells.COMET_AZUR.get().getSpellId());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer localPlayer)) {
            return;
        }
        if (localPlayer != Minecraft.getInstance().player) {
            return;
        }
        if (!isLocalPlayerCastingCometAzur()) {
            return;
        }
        event.getInput().forwardImpulse = 0.0f;
        event.getInput().leftImpulse = 0.0f;
        event.getInput().jumping = false;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) {
            lockActive = false;
            cancelPacketSent = false;
            return;
        }
        if (!isLocalPlayerCastingCometAzur()) {
            lockActive = false;
            cancelPacketSent = false;
            return;
        }
        if (!lockActive) {
            lockActive = true;
            cancelPacketSent = false;
            lockedYawDegrees = localPlayer.getYRot();
            lockedPitchDegrees = localPlayer.getXRot();
            lockedFeetPosition = localPlayer.position();
            applyLookAndPositionLock(localPlayer);
            return;
        }
        if (!Minecraft.getInstance().options.keyUse.isDown()) {
            if (!cancelPacketSent) {
                PacketDistributor.sendToServer(new CancelCastPacket(true));
                cancelPacketSent = true;
            }
            return;
        }
        applyLookAndPositionLock(localPlayer);
    }

    private static void applyLookAndPositionLock(LocalPlayer localPlayer) {
        localPlayer.setDeltaMovement(Vec3.ZERO);
        localPlayer.setPos(lockedFeetPosition.x, lockedFeetPosition.y, lockedFeetPosition.z);
        localPlayer.setYRot(lockedYawDegrees);
        localPlayer.setXRot(lockedPitchDegrees);
        localPlayer.yRotO = lockedYawDegrees;
        localPlayer.xRotO = lockedPitchDegrees;
        localPlayer.yHeadRot = lockedYawDegrees;
        localPlayer.yHeadRotO = lockedYawDegrees;
        localPlayer.yBodyRot = lockedYawDegrees;
        localPlayer.yBodyRotO = lockedYawDegrees;
    }
}

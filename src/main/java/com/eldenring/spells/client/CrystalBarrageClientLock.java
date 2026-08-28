package com.eldenring.spells.client;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModSpells;
import com.mojang.blaze3d.platform.InputConstants;
import io.redspace.ironsspellbooks.network.casting.CancelCastPacket;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.player.KeyMappings;
import net.minecraft.client.KeyMapping;
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
import org.lwjgl.glfw.GLFW;

/**
 * 客户端：施放结晶连弹时锁死移动输入与脚底位置，但允许转视角扫射。
 * <p>
 * 铁魔法本身只把施法移速乘到约 0.2；本类在之后把冲量清零，并把脚钉回出手瞬间。
 * 松手停连射：铁魔法 CONTINUOUS 默认会射到时间/蓝耗尽，所以要自己发取消包。
 * 不能用 {@link KeyMapping#isDown()} 判断魔法书施法键——铁魔法 {@code consume()} 之后
 * 这个标志经常是 false。改为读当前绑定的物理键（GLFW）。
 * 点按起手时，等服务端回「正在吟唱」时键往往已经弹起，这时不能立刻取消；
 * 只有本段吟唱里确实按住过施法键，再松开才停。
 */
@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID, value = Dist.CLIENT)
public final class CrystalBarrageClientLock {

    private static boolean lockActive;
    private static boolean cancelPacketSent;
    /**
     * 本段吟唱是否已经见过施法键处于按下。见过之后松开才发取消；从没见过说明是点按起手。
     */
    private static boolean holdKeyLatched;
    private static Vec3 lockedFeetPosition = Vec3.ZERO;

    private CrystalBarrageClientLock() {
    }

    private static boolean isLocalPlayerCastingCrystalBarrage() {
        if (!ClientMagicData.isCasting()) {
            return false;
        }
        String castingSpellId = ClientMagicData.getCastingSpellId();
        return castingSpellId != null
                && castingSpellId.equals(ModSpells.CRYSTAL_BARRAGE.get().getSpellId());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer localPlayer)) {
            return;
        }
        if (localPlayer != Minecraft.getInstance().player) {
            return;
        }
        if (!isLocalPlayerCastingCrystalBarrage()) {
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
            resetLockState();
            return;
        }
        if (!isLocalPlayerCastingCrystalBarrage()) {
            resetLockState();
            return;
        }
        if (!lockActive) {
            lockActive = true;
            cancelPacketSent = false;
            holdKeyLatched = false;
            lockedFeetPosition = localPlayer.position();
            applyPositionLock(localPlayer);
        }
        if (isAnyCastHoldKeyPhysicallyDown()) {
            holdKeyLatched = true;
            applyPositionLock(localPlayer);
            return;
        }
        if (holdKeyLatched) {
            sendCancelIfNeeded();
            return;
        }
        applyPositionLock(localPlayer);
    }

    private static void resetLockState() {
        lockActive = false;
        cancelPacketSent = false;
        holdKeyLatched = false;
    }

    private static void sendCancelIfNeeded() {
        if (cancelPacketSent) {
            return;
        }
        PacketDistributor.sendToServer(new CancelCastPacket(true));
        cancelPacketSent = true;
    }

    /**
     * 任一施法键的<strong>物理按下</strong>。读 {@link KeyMapping#getKey()}，跟玩家改键走。
     * 卷轴/法杖：右键；魔法书：施法键（默认 V）；快捷施法 1–15：对应快捷键。
     */
    private static boolean isAnyCastHoldKeyPhysicallyDown() {
        if (isKeyMappingPhysicallyDown(Minecraft.getInstance().options.keyUse)) {
            return true;
        }
        if (isKeyMappingPhysicallyDown(KeyMappings.SPELLBOOK_CAST_ACTIVE_KEYMAP)) {
            return true;
        }
        for (KeyMapping quickCastMapping : KeyMappings.QUICK_CAST_MAPPINGS) {
            if (isKeyMappingPhysicallyDown(quickCastMapping)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 不走 {@link KeyMapping#isDown()}，直接问 GLFW 这个绑定现在有没有按着。
     */
    private static boolean isKeyMappingPhysicallyDown(KeyMapping mapping) {
        if (mapping.isUnbound()) {
            return false;
        }
        return isPhysicalKeyDown(mapping.getKey());
    }

    private static boolean isPhysicalKeyDown(InputConstants.Key key) {
        if (key == null || key.equals(InputConstants.UNKNOWN)) {
            return false;
        }
        long windowHandle = Minecraft.getInstance().getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(windowHandle, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(windowHandle, key.getValue());
    }

    private static void applyPositionLock(LocalPlayer localPlayer) {
        localPlayer.setDeltaMovement(Vec3.ZERO);
        localPlayer.setPos(lockedFeetPosition.x, lockedFeetPosition.y, lockedFeetPosition.z);
    }
}

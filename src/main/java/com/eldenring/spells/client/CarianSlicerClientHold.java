package com.eldenring.spells.client;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.CarianSlicerEntity;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.spell.CarianSlicerSpell;
import com.eldenring.spells.spell.curve.CarianSlicerCastCurve;
import com.mojang.blaze3d.platform.InputConstants;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.config.ClientConfigs;
import io.redspace.ironsspellbooks.network.casting.CancelCastPacket;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.player.KeyMappings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 卡利亚迅剑客户端：松手停连斩、隐藏蓄力条、每一刀只重播右手动作。
 * <p>
 * 铁魔法 CONTINUOUS 默认只会在起手播一次动作，并且会画蓄力条。迅剑要看起来像近战连斩，
 * 所以这里按实体连斩序号重播 {@code horizontal_slash_one_handed}，但关掉除右手外的通道。
 * 左右挥砍由身前的剑实体自己做，不再镜像到左手。
 */
@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID, value = Dist.CLIENT)
public final class CarianSlicerClientHold {

    private static final ResourceLocation CAST_BAR_LAYER_ID = IronsSpellbooks.id("cast_bar");

    private static boolean watchingCast;
    private static boolean cancelPacketSent;
    private static boolean holdKeyLatched;
    private static int ticksSinceCastStart;

    /** 迅剑实体 id → 已经播过动作的连斩序号，用来侦测「新的一刀」。 */
    private static final Map<Integer, Integer> lastPlayedComboBySlicerId = new HashMap<>();

    private CarianSlicerClientHold() {
    }

    private static boolean isLocalPlayerCastingCarianSlicer() {
        if (!ClientMagicData.isCasting()) {
            return false;
        }
        String castingSpellId = ClientMagicData.getCastingSpellId();
        return castingSpellId != null
                && castingSpellId.equals(ModSpells.CARIAN_SLICER.get().getSpellId());
    }

    /**
     * 迅剑不是蓄力咒，把铁魔法那条剩余时间条藏掉。
     */
    @SubscribeEvent
    public static void hideChargeBar(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(CAST_BAR_LAYER_ID)) {
            return;
        }
        if (isLocalPlayerCastingCarianSlicer()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null) {
            replaySlashAnimations(localPlayer);
        }
        if (localPlayer == null || !isLocalPlayerCastingCarianSlicer()) {
            resetWatchState();
            return;
        }
        if (!watchingCast) {
            watchingCast = true;
            cancelPacketSent = false;
            holdKeyLatched = false;
            ticksSinceCastStart = 0;
        } else {
            ticksSinceCastStart++;
        }
        if (isAnyCastHoldKeyPhysicallyDown()) {
            holdKeyLatched = true;
            return;
        }
        if (holdKeyLatched) {
            sendCancelIfNeeded();
            return;
        }
        if (ticksSinceCastStart >= CarianSlicerCastCurve.HOLD_CANCEL_GRACE_TICKS) {
            sendCancelIfNeeded();
        }
    }

    /**
     * 附近每把迅剑换刀时，给持有者重播一次右手横斩。剑本身在身前左右挥，手臂只给一点右手动作。
     */
    private static void replaySlashAnimations(LocalPlayer localPlayer) {
        AABB searchBox = localPlayer.getBoundingBox().inflate(32.0);
        Set<Integer> seenSlicerIds = new HashSet<>();
        for (CarianSlicerEntity slicerEntity : localPlayer.level().getEntitiesOfClass(
                CarianSlicerEntity.class,
                searchBox
        )) {
            if (slicerEntity.isFinishing()) {
                continue;
            }
            Entity owner = slicerEntity.getOwner();
            if (!(owner instanceof AbstractClientPlayer clientPlayer)) {
                continue;
            }
            int slicerId = slicerEntity.getId();
            int comboIndex = slicerEntity.getComboIndex();
            seenSlicerIds.add(slicerId);
            Integer lastPlayedComboIndex = lastPlayedComboBySlicerId.get(slicerId);
            if (lastPlayedComboIndex != null && lastPlayedComboIndex == comboIndex) {
                continue;
            }
            lastPlayedComboBySlicerId.put(slicerId, comboIndex);
            playRightHandSlash(clientPlayer);
        }
        lastPlayedComboBySlicerId.keySet().removeIf(slicerId -> !seenSlicerIds.contains(slicerId));
    }

    /**
     * 重播铁魔法单手横斩，只驱动右手。左右挥砍由身前的剑实体自己做，这里不镜像、不跟准星加俯仰。
     */
    @SuppressWarnings("unchecked")
    private static void playRightHandSlash(AbstractClientPlayer player) {
        ResourceLocation animationId = SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION
                .getForPlayer()
                .orElse(null);
        if (animationId == null) {
            return;
        }
        Object rawAnimation = PlayerAnimationRegistry.getAnimation(animationId);
        if (!(rawAnimation instanceof KeyframeAnimation keyframeAnimation)) {
            return;
        }
        ModifierLayer<IAnimation> playerAnimationData =
                (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player)
                        .get(SpellAnimations.ANIMATION_RESOURCE);
        if (playerAnimationData == null) {
            return;
        }
        KeyframeAnimation rightArmOnly = keepRightArmOnly(keyframeAnimation);
        KeyframeAnimationPlayer keyframePlayer = new KeyframeAnimationPlayer(rightArmOnly);
        boolean showRightArm = ClientConfigs.SHOW_FIRST_PERSON_ARMS.get();
        if (showRightArm) {
            keyframePlayer.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
            keyframePlayer.setFirstPersonConfiguration(new FirstPersonConfiguration(
                    true, false, false, false
            ));
        } else {
            keyframePlayer.setFirstPersonMode(FirstPersonMode.DISABLED);
        }
        ModifierLayer<IAnimation> slashLayer = new ModifierLayer<>(keyframePlayer);
        slashLayer.addModifierLast(new SpeedModifier(
                CarianSlicerCastCurve.slashAnimationSpeed(CarianSlicerSpell.SLASH_CYCLE_TICKS)
        ));
        playerAnimationData.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(2, Ease.INOUTSINE),
                slashLayer,
                true
        );
    }

    /**
     * 关掉除 rightArm 以外的通道。原片的 left_arm / torso / legs 会和走路抢骨骼。
     */
    private static KeyframeAnimation keepRightArmOnly(KeyframeAnimation source) {
        KeyframeAnimation.AnimationBuilder builder = source.mutableCopy();
        for (var partEntry : builder.getBodyParts().entrySet()) {
            if ("rightArm".equals(partEntry.getKey()) || "right_arm".equals(partEntry.getKey())) {
                continue;
            }
            if (partEntry.getValue() != null) {
                partEntry.getValue().setEnabled(false);
            }
        }
        return builder.build();
    }

    private static void resetWatchState() {
        watchingCast = false;
        cancelPacketSent = false;
        holdKeyLatched = false;
        ticksSinceCastStart = 0;
    }

    private static void sendCancelIfNeeded() {
        if (cancelPacketSent) {
            return;
        }
        PacketDistributor.sendToServer(new CancelCastPacket(true));
        cancelPacketSent = true;
    }

    /**
     * 任一施法键的<strong>物理按下</strong>。读 {@link KeyMapping#getKey()}，跟玩家改键走，不写死 V。
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
}

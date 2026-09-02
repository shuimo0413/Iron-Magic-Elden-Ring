package com.eldenring.spells.client;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.spell.curve.CarianPiercerCastCurve;
import dev.kosmx.playerAnim.api.IPlayable;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.network.casting.CancelCastPacket;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 卡利亚贯刺客户端：点按只播一次 {@code carian_puncture}（0.75 秒），不连刺。
 * <p>
 * 长按也不会接第二刺：动作一结束就 CancelCast。松手也要播完这一刺再发取消包，
 * 避免铁魔法中途清掉动画。突刺走本 mod 专用层 {@link #CARIAN_PIERCER_ANIMATION_LAYER}，
 * 不挂铁魔法 Mirror / 准星修正。手里的剑由
 * {@link com.eldenring.spells.client.render.carian.CarianPiercerHandLayer} 画。
 */
@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID, value = Dist.CLIENT)
public final class CarianPiercerClientHold {

    private static final ResourceLocation CAST_BAR_LAYER_ID = IronsSpellbooks.id("cast_bar");

    /**
     * 贯刺专用动画层。在 {@link com.eldenring.spells.EldenRingSpellsClient} 里注册，
     * 优先级高于铁魔法的 42，且不挂 Mirror / 准星修正。
     */
    public static final ResourceLocation CARIAN_PIERCER_ANIMATION_LAYER =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "carian_piercer_animation");

    /**
     * 唯一 clip。与 Blockbench {@code elden_ring_spells.carian_puncture} 同名 1:1，不对调。
     */
    private static final String PIERCE_CLIP_NAME = "carian_puncture";

    /**
     * 起手淡入 tick。片长 0.75 秒时 2 tick 只作衔接，不吃掉突刺前伸。
     */
    private static final int ANIMATION_FADE_IN_TICKS = 2;

    /** 正在播这一刺（含松手后收完当前这一刺）。 */
    private static boolean slashPlaybackActive;

    /**
     * 当前这一刺已过 tick（0 起计）。满 {@link CarianPiercerCastCurve#SLASH_DURATION_TICKS} 即停。
     */
    private static int ticksIntoCurrentSlash;

    private static boolean cancelPacketSent;

    /**
     * 铁魔法施法减速前的前后冲量。{@link EventPriority#HIGH} 记下，
     * {@link EventPriority#LOWEST} 写回，抵消约 0.2 倍的施法移速惩罚。
     */
    private static float unslowedForwardImpulse;

    /** 铁魔法施法减速前的左右冲量。单位与 {@link #unslowedForwardImpulse} 相同（-1～1）。 */
    private static float unslowedLeftImpulse;

    private CarianPiercerClientHold() {
    }

    /**
     * 正在播这一刺（含松手后收完）。手里的剑跟这个窗口对齐。
     */
    public static boolean isSlashPlaybackActive() {
        return slashPlaybackActive;
    }

    /**
     * 铁魔法 {@code ClientPlayerEvents.onCalculatePlayerSpeed} 优先级是 NORMAL，
     * 会把冲量乘上 {@code 0.2 + CASTING_MOVESPEED - 1}。这里赶在它之前记下原冲量。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void captureUnslowedMovement(MovementInputUpdateEvent event) {
        if (!shouldRestoreFullMoveSpeed(event)) {
            return;
        }
        unslowedForwardImpulse = event.getInput().forwardImpulse;
        unslowedLeftImpulse = event.getInput().leftImpulse;
    }

    /**
     * 铁魔法乘完之后把冲量写回减速前，突刺时就能按正常走路 / 冲刺速度移动。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void restoreUnslowedMovement(MovementInputUpdateEvent event) {
        if (!shouldRestoreFullMoveSpeed(event)) {
            return;
        }
        event.getInput().forwardImpulse = unslowedForwardImpulse;
        event.getInput().leftImpulse = unslowedLeftImpulse;
    }

    @SubscribeEvent
    public static void hideChargeBar(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(CAST_BAR_LAYER_ID)) {
            return;
        }
        if (isLocalPlayerCastingCarianPiercer() || slashPlaybackActive) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) {
            resetAll();
            return;
        }

        boolean castingCarianPiercer = isLocalPlayerCastingCarianPiercer();
        if (!castingCarianPiercer) {
            // 上一刺的 CancelCast 已经落地，允许下一次点按重新起手。
            cancelPacketSent = false;
        }
        if (castingCarianPiercer && !slashPlaybackActive && !cancelPacketSent) {
            beginPierce(localPlayer);
        }

        if (!slashPlaybackActive) {
            return;
        }

        ticksIntoCurrentSlash++;
        if (ticksIntoCurrentSlash < CarianPiercerCastCurve.SLASH_DURATION_TICKS) {
            return;
        }

        sendCancelIfNeeded();
        resetAll();
    }

    private static void beginPierce(LocalPlayer localPlayer) {
        slashPlaybackActive = true;
        ticksIntoCurrentSlash = 0;
        playPierceAnimation(localPlayer);
    }

    @SuppressWarnings("unchecked")
    private static void playPierceAnimation(LocalPlayer player) {
        IPlayable playable = resolvePierceClip();
        if (playable == null) {
            return;
        }
        clearIronSpellAnimationLayer(player);

        ModifierLayer<IAnimation> piercerAnimationLayer =
                (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player)
                        .get(CARIAN_PIERCER_ANIMATION_LAYER);
        if (piercerAnimationLayer == null) {
            EldenRingSpellsMod.LOGGER.warn(
                    "Carian piercer animation layer {} missing; was registerFactory called?",
                    CARIAN_PIERCER_ANIMATION_LAYER
            );
            return;
        }
        IAnimation animationToPlay = createPiercePlayer(playable);
        piercerAnimationLayer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(ANIMATION_FADE_IN_TICKS, Ease.INOUTSINE),
                animationToPlay,
                true
        );
    }

    /**
     * 铁魔法 {@code SpellAnimations.ANIMATION_RESOURCE} 带 Mirror / 准星修正。
     * 贯刺不在那一层播，但起手或其它逻辑可能仍往里塞过动画，这里硬清掉。
     */
    @SuppressWarnings("unchecked")
    private static void clearIronSpellAnimationLayer(LocalPlayer player) {
        ModifierLayer<IAnimation> ironAnimationLayer =
                (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player)
                        .get(SpellAnimations.ANIMATION_RESOURCE);
        if (ironAnimationLayer != null) {
            ironAnimationLayer.setAnimation(null);
        }
    }

    /**
     * 收招时清掉贯刺层，避免最后一帧粘住。
     */
    @SuppressWarnings("unchecked")
    private static void clearPiercerAnimationLayer(LocalPlayer player) {
        if (player == null) {
            return;
        }
        ModifierLayer<IAnimation> piercerAnimationLayer =
                (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player)
                        .get(CARIAN_PIERCER_ANIMATION_LAYER);
        if (piercerAnimationLayer != null) {
            piercerAnimationLayer.setAnimation(null);
        }
    }

    /**
     * PlayerAnimator 2.x 的注册 path 可能是 clip 名、gecko 长名或带目录的资源 path。
     * 按精确名查找，找不到再扫本 mod 已注册动画。
     */
    private static IPlayable resolvePierceClip() {
        String[] candidatePaths = {
                PIERCE_CLIP_NAME,
                "animation.elden_ring_spells." + PIERCE_CLIP_NAME,
                "elden_ring_spells." + PIERCE_CLIP_NAME,
                "player_animation/" + PIERCE_CLIP_NAME,
                "player_animations/" + PIERCE_CLIP_NAME
        };
        for (String candidatePath : candidatePaths) {
            IPlayable playable = PlayerAnimationRegistry.getAnimation(
                    ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, candidatePath)
            );
            if (playable != null) {
                return playable;
            }
        }
        Map<String, IPlayable> registeredClips =
                PlayerAnimationRegistry.getModAnimations(EldenRingSpellsMod.MOD_ID);
        IPlayable exactName = registeredClips.get(PIERCE_CLIP_NAME);
        if (exactName != null) {
            return exactName;
        }
        for (Map.Entry<String, IPlayable> entry : registeredClips.entrySet()) {
            if (entry.getKey().contains(PIERCE_CLIP_NAME)) {
                return entry.getValue();
            }
        }
        EldenRingSpellsMod.LOGGER.warn(
                "Carian piercer clip {} not in PlayerAnimator registry. Have: {}",
                PIERCE_CLIP_NAME,
                registeredClips.keySet()
        );
        return null;
    }

    private static IAnimation createPiercePlayer(IPlayable playable) {
        KeyframeAnimationPlayer keyframePlayer;
        if (playable instanceof KeyframeAnimation keyframeAnimation) {
            keyframePlayer = new KeyframeAnimationPlayer(keyframeAnimation);
        } else {
            IAnimation played = playable.playAnimation();
            if (played instanceof KeyframeAnimationPlayer typedPlayer) {
                keyframePlayer = typedPlayer;
            } else {
                return played;
            }
        }
        keyframePlayer.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
        // 右臂开、左手/双手物品关：第一人称只看到突刺右臂，法术书仍藏着。
        keyframePlayer.setFirstPersonConfiguration(new FirstPersonConfiguration(
                true, false, false, false
        ));
        return keyframePlayer;
    }

    private static void resetAll() {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        clearPiercerAnimationLayer(localPlayer);
        slashPlaybackActive = false;
        ticksIntoCurrentSlash = 0;
    }

    /**
     * 只对正在施放贯刺的本地玩家还原移速。别人的输入事件不会进这里。
     */
    private static boolean shouldRestoreFullMoveSpeed(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer localPlayer)) {
            return false;
        }
        if (localPlayer != Minecraft.getInstance().player) {
            return false;
        }
        return isLocalPlayerCastingCarianPiercer() || slashPlaybackActive;
    }

    private static boolean isLocalPlayerCastingCarianPiercer() {
        if (!ClientMagicData.isCasting()) {
            return false;
        }
        String castingSpellId = ClientMagicData.getCastingSpellId();
        return castingSpellId != null
                && castingSpellId.equals(ModSpells.CARIAN_PIERCER.get().getSpellId());
    }

    private static void sendCancelIfNeeded() {
        if (cancelPacketSent) {
            return;
        }
        PacketDistributor.sendToServer(new CancelCastPacket(true));
        cancelPacketSent = true;
    }
}

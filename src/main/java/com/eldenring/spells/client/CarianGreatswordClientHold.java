package com.eldenring.spells.client;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModSpells;
import com.mojang.blaze3d.platform.InputConstants;
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
import io.redspace.ironsspellbooks.player.KeyMappings;
import java.util.Map;
import net.minecraft.client.KeyMapping;
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
import org.lwjgl.glfw.GLFW;

/**
 * 卡利亚大剑客户端：交替播放 {@code carian_great_sword1}（第一刀）与 {@code carian_great_sword2}（第二刀）。
 * <p>
 * 调度与迅剑相同：每刀 0.5 秒（10 tick），按下出第一刀，长按交替；松手也要播完当前刀再 CancelCast。
 * 斩击走本 mod 专用层 {@link #CARIAN_GREATSWORD_ANIMATION_LAYER}，不挂铁魔法 Mirror / 准星修正。
 * 手里的剑由 {@link com.eldenring.spells.client.render.carian.CarianGreatswordHandLayer} 用大剑自己的握点画。
 */
@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID, value = Dist.CLIENT)
public final class CarianGreatswordClientHold {

    private static final ResourceLocation CAST_BAR_LAYER_ID = IronsSpellbooks.id("cast_bar");

    /**
     * 大剑专用动画层。在 {@link com.eldenring.spells.EldenRingSpellsClient} 里注册，
     * 优先级高于铁魔法的 42，且不挂 Mirror / 准星修正。
     */
    public static final ResourceLocation CARIAN_GREATSWORD_ANIMATION_LAYER =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "carian_greatsword_animation");

    /**
     * 下标 0 = 点按第一刀（资源 {@code carian_great_sword1}），1 = 连斩第二刀（{@code carian_great_sword2}）。
     * 与 Blockbench {@code elden_ring_spells.carian_great_sword1} / {@code sword2} 同名 1:1。
     */
    private static final String[] SLASH_CLIP_NAMES = {
            "carian_great_sword1",
            "carian_great_sword2"
    };

    /**
     * 与 player_animation JSON {@code animation_length: 0.5} 对齐（10 tick = 0.5 秒）。
     */
    private static final int SLASH_ANIMATION_LENGTH_TICKS = 10;

    /**
     * 刀与刀之间的淡入 tick。片长 0.5 秒时 4 tick 会占近半刀，用 2 tick 衔接。
     */
    private static final int ANIMATION_FADE_IN_TICKS = 2;

    /** 是否正在跑某一刀的 0.5 秒计时（含松手后收完当前刀）。 */
    private static boolean slashPlaybackActive;

    /** 0 = 点按第一刀（{@code carian_great_sword1}），1 = 连斩第二刀（{@code carian_great_sword2}）。 */
    private static int slashSequenceIndex;

    /** 当前刀已播放 tick（0 起计，满 {@link #SLASH_ANIMATION_LENGTH_TICKS} 才允许下一刀）。 */
    private static int ticksIntoCurrentSlash;

    /** 起手这一 tick 不计入片长，避免少播最后一帧。 */
    private static boolean skipLengthTickThisFrame;

    /** 松手后为 true：当前刀播完即停，不再交替。取消包也要等到这一刀结束才发。 */
    private static boolean stopChainingAfterCurrentSlash;

    private static boolean cancelPacketSent;

    /**
     * 铁魔法施法减速前的前后冲量。{@link EventPriority#HIGH} 记下，
     * {@link EventPriority#LOWEST} 写回，抵消约 0.2 倍的施法移速惩罚。
     */
    private static float unslowedForwardImpulse;

    /** 铁魔法施法减速前的左右冲量。单位与 {@link #unslowedForwardImpulse} 相同（-1～1）。 */
    private static float unslowedLeftImpulse;

    private CarianGreatswordClientHold() {
    }

    /**
     * 正在播某一刀（含松手后收完这一刀）。手里的剑跟这个窗口对齐。
     */
    public static boolean isSlashPlaybackActive() {
        return slashPlaybackActive;
    }

    /**
     * 当前是第几刀（0 = {@code carian_great_sword1}）。光轨在换刀时清空，避免两刀之间拉线。
     */
    public static int slashSequenceIndex() {
        return slashSequenceIndex;
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
     * 铁魔法乘完之后把冲量写回减速前，斩击时就能按正常走路 / 冲刺速度移动。
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
        if (isLocalPlayerCastingCarianGreatsword()) {
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

        boolean castingCarianGreatsword = isLocalPlayerCastingCarianGreatsword();

        if (castingCarianGreatsword) {
            if (!slashPlaybackActive) {
                beginSlashSequence(localPlayer);
            }
            updateHoldKeyState();
        } else if (slashPlaybackActive) {
            stopChainingAfterCurrentSlash = true;
        }

        if (!slashPlaybackActive) {
            return;
        }

        if (skipLengthTickThisFrame) {
            skipLengthTickThisFrame = false;
            return;
        }

        ticksIntoCurrentSlash++;

        if (ticksIntoCurrentSlash < SLASH_ANIMATION_LENGTH_TICKS) {
            return;
        }

        if (shouldChainIntoNextSlash(castingCarianGreatsword)) {
            slashSequenceIndex++;
            ticksIntoCurrentSlash = 0;
            skipLengthTickThisFrame = true;
            playSlashAnimation(localPlayer, slashSequenceIndex);
            return;
        }

        sendCancelIfNeeded();
        resetAll();
    }

    private static void beginSlashSequence(LocalPlayer localPlayer) {
        slashPlaybackActive = true;
        slashSequenceIndex = 0;
        ticksIntoCurrentSlash = 0;
        skipLengthTickThisFrame = true;
        stopChainingAfterCurrentSlash = false;
        cancelPacketSent = false;
        playSlashAnimation(localPlayer, 0);
    }

    /**
     * 松手只标记「这一刀之后不再连斩」，不立刻 CancelCast。
     * 铁魔法 {@code OnCastFinishedPacket(cancelled)} 会清动画，中途发取消等于砍掉当前刀。
     */
    private static void updateHoldKeyState() {
        stopChainingAfterCurrentSlash = !isAnyCastHoldKeyPhysicallyDown();
    }

    private static boolean shouldChainIntoNextSlash(boolean castingCarianGreatsword) {
        if (stopChainingAfterCurrentSlash) {
            return false;
        }
        if (!castingCarianGreatsword) {
            return false;
        }
        return isAnyCastHoldKeyPhysicallyDown();
    }

    @SuppressWarnings("unchecked")
    private static void playSlashAnimation(LocalPlayer player, int sequenceIndex) {
        int slashClipIndex = sequenceIndex & 1;
        IPlayable playable = resolveSlashClip(slashClipIndex);
        if (playable == null) {
            return;
        }
        clearIronSpellAnimationLayer(player);

        ModifierLayer<IAnimation> greatswordAnimationLayer =
                (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player)
                        .get(CARIAN_GREATSWORD_ANIMATION_LAYER);
        if (greatswordAnimationLayer == null) {
            EldenRingSpellsMod.LOGGER.warn(
                    "Carian greatsword animation layer {} missing; was registerFactory called?",
                    CARIAN_GREATSWORD_ANIMATION_LAYER
            );
            return;
        }
        IAnimation animationToPlay = createSlashPlayer(playable);
        greatswordAnimationLayer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(ANIMATION_FADE_IN_TICKS, Ease.INOUTSINE),
                animationToPlay,
                true
        );
    }

    /**
     * 铁魔法 {@code SpellAnimations.ANIMATION_RESOURCE} 带 Mirror / 准星修正。
     * 大剑不在那一层播，但起手或其它逻辑可能仍往里塞过动画，这里硬清掉。
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
     * 收招时清掉大剑层，避免最后一帧粘住。
     */
    @SuppressWarnings("unchecked")
    private static void clearGreatswordAnimationLayer(LocalPlayer player) {
        if (player == null) {
            return;
        }
        ModifierLayer<IAnimation> greatswordAnimationLayer =
                (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player)
                        .get(CARIAN_GREATSWORD_ANIMATION_LAYER);
        if (greatswordAnimationLayer != null) {
            greatswordAnimationLayer.setAnimation(null);
        }
    }

    /**
     * PlayerAnimator 2.x 的注册 path 可能是 clip 名、gecko 长名或带目录的资源 path。
     * 按精确名查找，找不到再扫本 mod 已注册动画，避免 {@code carian_great_sword1} 静默失败后直接播到 2。
     */
    private static IPlayable resolveSlashClip(int slashClipIndex) {
        String wantedClipName = SLASH_CLIP_NAMES[slashClipIndex];
        String otherClipName = SLASH_CLIP_NAMES[slashClipIndex ^ 1];
        String[] candidatePaths = {
                wantedClipName,
                "animation.elden_ring_spells." + wantedClipName,
                "elden_ring_spells." + wantedClipName,
                "player_animation/" + wantedClipName,
                "player_animations/" + wantedClipName
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
        IPlayable exactName = registeredClips.get(wantedClipName);
        if (exactName != null) {
            return exactName;
        }
        for (Map.Entry<String, IPlayable> entry : registeredClips.entrySet()) {
            String registeredPath = entry.getKey();
            if (!registeredPath.contains(wantedClipName) || registeredPath.contains(otherClipName)) {
                continue;
            }
            return entry.getValue();
        }
        EldenRingSpellsMod.LOGGER.warn(
                "Carian greatsword clip {} not in PlayerAnimator registry. Have: {}",
                wantedClipName,
                registeredClips.keySet()
        );
        return null;
    }

    private static IAnimation createSlashPlayer(IPlayable playable) {
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
        // 右臂开、左手/双手物品关：第一人称只看到斩击右臂，法术书仍藏着。
        // 大剑不走这里的「右手物品」开关，由 CarianGreatswordHandLayer 自己画。
        keyframePlayer.setFirstPersonConfiguration(new FirstPersonConfiguration(
                true, false, false, false
        ));
        return keyframePlayer;
    }

    private static void resetAll() {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        clearGreatswordAnimationLayer(localPlayer);
        slashPlaybackActive = false;
        slashSequenceIndex = 0;
        ticksIntoCurrentSlash = 0;
        skipLengthTickThisFrame = false;
        stopChainingAfterCurrentSlash = false;
        cancelPacketSent = false;
    }

    /**
     * 只对正在施放大剑的本地玩家还原移速。别人的输入事件不会进这里。
     */
    private static boolean shouldRestoreFullMoveSpeed(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer localPlayer)) {
            return false;
        }
        if (localPlayer != Minecraft.getInstance().player) {
            return false;
        }
        return isLocalPlayerCastingCarianGreatsword();
    }

    private static boolean isLocalPlayerCastingCarianGreatsword() {
        if (!ClientMagicData.isCasting()) {
            return false;
        }
        String castingSpellId = ClientMagicData.getCastingSpellId();
        return castingSpellId != null
                && castingSpellId.equals(ModSpells.CARIAN_GREATSWORD.get().getSpellId());
    }

    private static void sendCancelIfNeeded() {
        if (cancelPacketSent) {
            return;
        }
        PacketDistributor.sendToServer(new CancelCastPacket(true));
        cancelPacketSent = true;
    }

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

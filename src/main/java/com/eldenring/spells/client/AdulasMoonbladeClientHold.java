package com.eldenring.spells.client;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.spell.curve.AdulasMoonbladeCastCurve;
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
 * 亚杜拉的月光剑客户端：从 {@link CarianGreatswordClientHold} 拷出的独立副本，
 * 同样交替播放 {@code carian_great_sword1}（第一刀）与 {@code carian_great_sword2}（第二刀）。
 * <p>
 * 每刀 0.5 秒（10 tick），播完后再空 0.5 秒才允许下一刀；按下出第一刀，长按交替。
 * 点按只出一刀：当前刀播完即 CancelCast。松手也要播完当前刀再发取消包。
 * 斩击走本法术专用层 {@link #ADULAS_MOONBLADE_ANIMATION_LAYER}，不挂铁魔法 Mirror / 准星修正。
 * 手里的月光剑由 {@link com.eldenring.spells.client.render.moonblade.AdulasMoonbladeHandLayer} 画。
 */
@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID, value = Dist.CLIENT)
public final class AdulasMoonbladeClientHold {

    private static final ResourceLocation CAST_BAR_LAYER_ID = IronsSpellbooks.id("cast_bar");

    /**
     * 月光剑专用动画层。在 {@link com.eldenring.spells.EldenRingSpellsClient} 里注册，
     * 优先级高于铁魔法的 42，且不挂 Mirror / 准星修正。
     */
    public static final ResourceLocation ADULAS_MOONBLADE_ANIMATION_LAYER =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "adulas_moonblade_animation");

    /**
     * 下标 0 = 点按第一刀，1 = 连斩第二刀。动作与卡利亚大剑共用同一套动画资源。
     */
    private static final String[] SLASH_CLIP_NAMES = {
            "carian_great_sword1",
            "carian_great_sword2"
    };

    /** 刀与刀之间的淡入 tick。片长 0.5 秒时用 2 tick 衔接。 */
    private static final int ANIMATION_FADE_IN_TICKS = 2;

    /** 是否正在跑某一刀（含松手后收完当前刀，以及连斩前的收招空档）。 */
    private static boolean slashPlaybackActive;

    /** 0 = 第一刀，1 = 第二刀，之后继续累加（取奇偶选动作）。 */
    private static int slashSequenceIndex;

    /**
     * 当前周期已过 tick（0 起计）。未满片长是挥砍；满片长未满周期是收招空档；满周期才允许下一刀。
     */
    private static int ticksIntoCurrentSlash;

    /** 松手后为 true：当前刀播完即停，不再交替。 */
    private static boolean stopChainingAfterCurrentSlash;

    private static boolean cancelPacketSent;

    /** 铁魔法施法减速前的前后冲量（-1～1），HIGH 记下、LOWEST 写回。 */
    private static float unslowedForwardImpulse;

    /** 铁魔法施法减速前的左右冲量（-1～1）。 */
    private static float unslowedLeftImpulse;

    private AdulasMoonbladeClientHold() {
    }

    /** 正在播某一刀（含收完这一刀以及连斩前的空档）。手里的剑跟这个窗口对齐。 */
    public static boolean isSlashPlaybackActive() {
        return slashPlaybackActive;
    }

    /** 当前是第几刀。光轨在换刀时清空，避免两刀之间拉线。 */
    public static int slashSequenceIndex() {
        return slashSequenceIndex;
    }

    /** 赶在铁魔法 NORMAL 优先级减速之前记下原冲量。 */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void captureUnslowedMovement(MovementInputUpdateEvent event) {
        if (!shouldRestoreFullMoveSpeed(event)) {
            return;
        }
        unslowedForwardImpulse = event.getInput().forwardImpulse;
        unslowedLeftImpulse = event.getInput().leftImpulse;
    }

    /** 铁魔法乘完之后把冲量写回减速前，斩击时按正常走路 / 冲刺速度移动。 */
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
        if (isLocalPlayerCastingMoonblade()) {
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

        boolean castingMoonblade = isLocalPlayerCastingMoonblade();

        if (castingMoonblade) {
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

        ticksIntoCurrentSlash++;

        if (ticksIntoCurrentSlash < AdulasMoonbladeCastCurve.SLASH_DURATION_TICKS) {
            return;
        }

        if (ticksIntoCurrentSlash < AdulasMoonbladeCastCurve.SLASH_CYCLE_TICKS) {
            // 当前刀已经播完：松手就立刻停，不必把空档走完。
            if (!shouldChainIntoNextSlash(castingMoonblade)) {
                sendCancelIfNeeded();
                resetAll();
            }
            return;
        }

        if (shouldChainIntoNextSlash(castingMoonblade)) {
            slashSequenceIndex++;
            // 这一 tick 算下一刀的第 1 帧，片长保持 10 tick，避免服务端多砍一刀。
            ticksIntoCurrentSlash = 1;
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
        stopChainingAfterCurrentSlash = false;
        cancelPacketSent = false;
        playSlashAnimation(localPlayer, 0);
    }

    /** 松手只标记「这一刀之后不再连斩」，不立刻 CancelCast，否则会砍掉当前刀动画。 */
    private static void updateHoldKeyState() {
        stopChainingAfterCurrentSlash = !isAnyCastHoldKeyPhysicallyDown();
    }

    private static boolean shouldChainIntoNextSlash(boolean castingMoonblade) {
        if (stopChainingAfterCurrentSlash) {
            return false;
        }
        if (!castingMoonblade) {
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

        ModifierLayer<IAnimation> moonbladeAnimationLayer =
                (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player)
                        .get(ADULAS_MOONBLADE_ANIMATION_LAYER);
        if (moonbladeAnimationLayer == null) {
            EldenRingSpellsMod.LOGGER.warn(
                    "Adula's moonblade animation layer {} missing; was registerFactory called?",
                    ADULAS_MOONBLADE_ANIMATION_LAYER
            );
            return;
        }
        moonbladeAnimationLayer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(ANIMATION_FADE_IN_TICKS, Ease.INOUTSINE),
                createSlashPlayer(playable),
                true
        );
    }

    /** 铁魔法默认动画层带 Mirror / 准星修正，起手可能往里塞过动画，这里硬清掉。 */
    @SuppressWarnings("unchecked")
    private static void clearIronSpellAnimationLayer(LocalPlayer player) {
        ModifierLayer<IAnimation> ironAnimationLayer =
                (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player)
                        .get(SpellAnimations.ANIMATION_RESOURCE);
        if (ironAnimationLayer != null) {
            ironAnimationLayer.setAnimation(null);
        }
    }

    /** 收招时清掉月光剑层，避免最后一帧粘住。 */
    @SuppressWarnings("unchecked")
    private static void clearMoonbladeAnimationLayer(LocalPlayer player) {
        if (player == null) {
            return;
        }
        ModifierLayer<IAnimation> moonbladeAnimationLayer =
                (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player)
                        .get(ADULAS_MOONBLADE_ANIMATION_LAYER);
        if (moonbladeAnimationLayer != null) {
            moonbladeAnimationLayer.setAnimation(null);
        }
    }

    /**
     * PlayerAnimator 2.x 的注册 path 可能是 clip 名、gecko 长名或带目录的资源 path。
     * 按精确名查找，找不到再扫本 mod 已注册动画。
     */
    private static IPlayable resolveSlashClip(int slashClipIndex) {
        String wantedClipName = SLASH_CLIP_NAMES[slashClipIndex];
        String otherClipName = SLASH_CLIP_NAMES[slashClipIndex ^ 1];
        String[] candidatePaths = {
                wantedClipName,
                "animation.iss_elden_ring." + wantedClipName,
                "iss_elden_ring." + wantedClipName,
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
                "Adula's moonblade clip {} not in PlayerAnimator registry. Have: {}",
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
        // 右臂开、左手/双手物品关：第一人称只看到斩击右臂；剑由 AdulasMoonbladeHandLayer 自己画。
        keyframePlayer.setFirstPersonConfiguration(new FirstPersonConfiguration(
                true, false, false, false
        ));
        return keyframePlayer;
    }

    private static void resetAll() {
        clearMoonbladeAnimationLayer(Minecraft.getInstance().player);
        slashPlaybackActive = false;
        slashSequenceIndex = 0;
        ticksIntoCurrentSlash = 0;
        stopChainingAfterCurrentSlash = false;
        cancelPacketSent = false;
    }

    /** 只对正在施放月光剑的本地玩家还原移速。 */
    private static boolean shouldRestoreFullMoveSpeed(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer localPlayer)) {
            return false;
        }
        if (localPlayer != Minecraft.getInstance().player) {
            return false;
        }
        return isLocalPlayerCastingMoonblade();
    }

    private static boolean isLocalPlayerCastingMoonblade() {
        if (!ClientMagicData.isCasting()) {
            return false;
        }
        String castingSpellId = ClientMagicData.getCastingSpellId();
        return castingSpellId != null
                && castingSpellId.equals(ModSpells.ADULAS_MOONBLADE.get().getSpellId());
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

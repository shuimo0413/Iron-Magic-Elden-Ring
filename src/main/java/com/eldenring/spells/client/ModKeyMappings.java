package com.eldenring.spells.client;

import com.eldenring.spells.EldenRingSpellsMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * 追踪排除菜单热键：默认「按住 X + 按下 C」。
 * <p>
 * 原版无法把任意键注册成修饰键，故拆成两个可在「控制」里改的 {@link KeyMapping}。
 */
@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID, value = Dist.CLIENT)
public final class ModKeyMappings {
    /**
     * 控制菜单里的分类名（lang：{@code key.categories.elden_ring_spells}）。
     */
    public static final String CATEGORY = "key.categories.elden_ring_spells";

    /**
     * 和弦修饰键，默认 {@code X}：须按住才响应打开键。
     */
    public static KeyMapping trackingIgnoreMenuModifier;

    /**
     * 打开追踪排除菜单，默认 {@code C}：在修饰键按住时 consume 一次按下。
     */
    public static KeyMapping trackingIgnoreMenuOpen;

    private ModKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        trackingIgnoreMenuModifier = new KeyMapping(
                "key.elden_ring_spells.tracking_ignore_modifier",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                CATEGORY
        );
        trackingIgnoreMenuOpen = new KeyMapping(
                "key.elden_ring_spells.tracking_ignore_open",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                CATEGORY
        );
        event.register(trackingIgnoreMenuModifier);
        event.register(trackingIgnoreMenuOpen);
    }
}

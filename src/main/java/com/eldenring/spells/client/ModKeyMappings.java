package com.eldenring.spells.client;

import com.eldenring.spells.EldenRingSpellsMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

import net.minecraftforge.fml.common.Mod;
/**
 * 追踪排除设置热键：默认单键 {@code P}，可在「控制」里改绑。
 * <p>
 * {@link RegisterKeyMappingsEvent} 只在 MOD 总线触发，Forge 1.20.1 的订阅注解默认挂游戏总线，
 * 必须显式写 {@code bus = MOD}，否则热键永远不会注册。
 */
@Mod.EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModKeyMappings {
    /**
     * 控制菜单里的分类名（lang：{@code key.categories.iss_elden_ring}）。
     */
    public static final String CATEGORY = "key.categories.iss_elden_ring";

    /**
     * 打开追踪排除设置界面，默认 {@code P}。
     * <p>
     * 键名不沿用旧的 {@code tracking_ignore_open}，让老存档里保存的旧 C 绑定失效、统一回到默认 P。
     */
    public static KeyMapping trackingIgnoreMenu;

    private ModKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        trackingIgnoreMenu = new KeyMapping(
                "key.iss_elden_ring.tracking_ignore_menu",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                CATEGORY
        );
        event.register(trackingIgnoreMenu);
    }
}

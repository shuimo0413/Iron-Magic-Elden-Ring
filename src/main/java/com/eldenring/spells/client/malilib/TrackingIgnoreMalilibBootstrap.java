package com.eldenring.spells.client.malilib;

import com.eldenring.spells.EldenRingSpellsMod;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * MaFgLib 存在时才通过反射调用：注册配置屏工厂，并打开 malilib 风格 GUI。
 * <p>
 * 本类直接依赖 malilib API，禁止被无条件加载的类静态引用。
 */
public final class TrackingIgnoreMalilibBootstrap {
    private TrackingIgnoreMalilibBootstrap() {
    }

    /**
     * 注册到 {@link Registry#CONFIG_SCREEN}，使 Tweakerge 右上角下拉能切到本模组。
     */
    public static void register() {
        TrackingIgnoreMalilibConfigs.installCallbacks();
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(new ModInfo(
                EldenRingSpellsMod.MOD_ID,
                "Iron's Spells: Elden Ring",
                TrackingIgnoreMalilibGui::new
        ));
        EldenRingSpellsMod.LOGGER.info("Registered MaLiLib config screen for tracking ignore prefs.");
    }

    /**
     * 打开本模组 malilib 配置界面（会先从缓存拉一次开关状态）。
     */
    public static void openGui() {
        TrackingIgnoreMalilibConfigs.pullFromCache();
        Minecraft.getInstance().setScreen(new TrackingIgnoreMalilibGui());
    }

    /**
     * @return 当前是否已是本模组的 malilib 配置 Screen（避免重复打开）
     */
    public static boolean isOurConfigScreen(Screen screen) {
        return screen instanceof TrackingIgnoreMalilibGui;
    }
}

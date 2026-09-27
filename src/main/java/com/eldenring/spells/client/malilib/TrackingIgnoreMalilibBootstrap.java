package com.eldenring.spells.client.malilib;

import com.eldenring.spells.EldenRingSpellsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.thinkingstudio.mafglib.util.ForgePlatformUtils;

/**
 * MaFgLib 存在时才通过反射调用：注册配置屏工厂，并打开 malilib 风格 GUI。
 * <p>
 * 本类直接依赖 malilib API，禁止被无条件加载的类静态引用。
 * <p>
 * <strong>为什么这里和 1.21.1 分支长得不一样</strong>：1.21.1 线用 MaFgLib 0.4.x，配置屏注册走
 * {@code fi.dy.masa.malilib.registry.Registry.CONFIG_SCREEN} +
 * {@code fi.dy.masa.malilib.util.data.ModInfo}；1.20.1 线只有 0.1.14，这两个类都还没有
 * （malilib 的 registry 体系是后面版本才重构出来的）。0.1.14 的等价入口是
 * {@code org.thinkingstudio.mafglib.util.ForgePlatformUtils#registerModConfigScreen}。
 * <p>
 * <strong>玩家侧行为</strong>：装了 MaFgLib（及依赖它的 Tweakerge）时，右上角模组切换下拉与
 * 「模组列表 → 配置」都能切到本模组；没装 MaFgLib 时本类根本不会被加载，
 * 热键仍打开本模组自带的简易 {@code TrackingIgnoreScreen}，功能不缺失。
 */
public final class TrackingIgnoreMalilibBootstrap {
    private TrackingIgnoreMalilibBootstrap() {
    }

    /**
     * 注册进 Forge 的模组配置屏接口，Tweakerge 右上角下拉与「模组列表 → 配置」都能切到本模组。
     * <p>
     * MaFgLib 0.1.x 的入口是 {@link ForgePlatformUtils#registerModConfigScreen(String, org.thinkingstudio.mafglib.util.ModConfigScreenProvider)}，
     * 内部会按 mod id 找到 {@code ModContainer} 并注册 {@code ConfigScreenHandler.ConfigScreenFactory}。
     */
    public static void register() {
        TrackingIgnoreMalilibConfigs.installCallbacks();
        ForgePlatformUtils.getInstance().registerModConfigScreen(
                EldenRingSpellsMod.MOD_ID,
                parentScreen -> new TrackingIgnoreMalilibGui()
        );
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

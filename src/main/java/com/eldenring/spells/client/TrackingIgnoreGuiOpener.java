package com.eldenring.spells.client;

import com.eldenring.spells.EldenRingSpellsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModList;

/**
 * 有 MaFgLib 时打开 malilib 风格配置界面；否则回退到简易 {@link TrackingIgnoreScreen}。
 * <p>
 * 用反射调用 {@code client.malilib} 包，避免无 MaFgLib 时类加载失败。
 */
public final class TrackingIgnoreGuiOpener {
    private static final String MAFGLIB_MOD_ID = "mafglib";
    private static final String BOOTSTRAP_CLASS =
            "com.eldenring.spells.client.malilib.TrackingIgnoreMalilibBootstrap";

    private static Boolean mafglibPresent;
    private static boolean bootstrapRegistered;

    private TrackingIgnoreGuiOpener() {
    }

    /**
     * 客户端启动：若装了 MaFgLib，注册右上角模组切换列表里的本模组入口。
     */
    public static void tryRegisterMalilibConfigScreen() {
        if (!isMafglibPresent() || bootstrapRegistered) {
            return;
        }
        try {
            Class<?> bootstrap = Class.forName(BOOTSTRAP_CLASS);
            bootstrap.getMethod("register").invoke(null);
            bootstrapRegistered = true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            EldenRingSpellsMod.LOGGER.warn(
                    "MaFgLib present but tracking-ignore MaLiLib GUI failed to register: {}",
                    exception.toString()
            );
        }
    }

    /**
     * 热键打开配置：优先 malilib GUI，失败则简易屏。
     */
    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (tryOpenMalilibGui(minecraft)) {
            return;
        }
        minecraft.setScreen(new TrackingIgnoreScreen());
    }

    private static boolean tryOpenMalilibGui(Minecraft minecraft) {
        if (!isMafglibPresent()) {
            return false;
        }
        tryRegisterMalilibConfigScreen();
        try {
            Class<?> bootstrap = Class.forName(BOOTSTRAP_CLASS);
            Screen current = minecraft.screen;
            if (current != null) {
                Object isOurs = bootstrap.getMethod("isOurConfigScreen", Screen.class).invoke(null, current);
                if (Boolean.TRUE.equals(isOurs)) {
                    return true;
                }
            }
            bootstrap.getMethod("openGui").invoke(null);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            EldenRingSpellsMod.LOGGER.warn(
                    "Falling back to simple tracking-ignore screen: {}",
                    exception.toString()
            );
            return false;
        }
    }

    private static boolean isMafglibPresent() {
        if (mafglibPresent == null) {
            mafglibPresent = ModList.get().isLoaded(MAFGLIB_MOD_ID);
        }
        return mafglibPresent;
    }
}

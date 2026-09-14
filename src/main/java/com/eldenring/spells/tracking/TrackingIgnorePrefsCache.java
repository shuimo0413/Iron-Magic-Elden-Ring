package com.eldenring.spells.tracking;

/**
 * 逻辑客户端缓存的追踪排除偏好，供 GUI 显示。
 * 以服务端 S2C 为准；本地勾选会先乐观更新再发 C2S。
 * <p>
 * 放在 common 包，避免网络处理器在专用服务端加载 {@code client} 类。
 */
public final class TrackingIgnorePrefsCache {
    private static TrackingIgnorePrefs synced = TrackingIgnorePrefs.DEFAULT;

    private TrackingIgnorePrefsCache() {
    }

    public static TrackingIgnorePrefs get() {
        return synced;
    }

    /**
     * 服务端推送后更新缓存。
     */
    public static void acceptFromServer(TrackingIgnorePrefs prefs) {
        synced = prefs;
    }

    /**
     * GUI 勾选时立刻改本地显示，避免等回包闪烁。
     */
    public static void optimisticLocal(TrackingIgnorePrefs prefs) {
        synced = prefs;
    }
}

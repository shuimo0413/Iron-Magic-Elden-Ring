package com.eldenring.spells.client.malilib;

import com.eldenring.spells.network.TrackingIgnorePrefsPayload;
import com.eldenring.spells.tracking.TrackingIgnorePrefs;
import com.eldenring.spells.tracking.TrackingIgnorePrefsCache;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.interfaces.IValueChangeCallback;

/**
 * MaLiLib {@link ConfigBoolean} 列表：UI 用，真正持久化仍走服务端玩家偏好 + 网络包。
 * <p>
 * 注意这里的 ConfigBoolean 只是「界面上的开关」：勾选后由回调打包成
 * {@link TrackingIgnorePrefsPayload} 发往服务端，服务端落进玩家 Capability
 * （1.20.1 无 NeoForge attachment，见 {@code registry/ModAttachments}），
 * 因此 malilib 自己的 config 文件里不留这些值，换存档也不会串味。
 * <p>
 * 仅在 MaFgLib 存在时由此包加载；不要从无条件加载的类里直接 import。
 * <p>
 * 1.20.1 线的 MaFgLib 是 0.1.x，{@code ConfigBoolean} 最多只到
 * {@code (name, defaultValue, comment, prettyName)} 四参；1.21 线 0.4.x 的第五参
 * （translatedName）不存在，且 0.1.x 的 {@code getPrettyName()} 本身就会翻译 prettyName，
 * 所以直接省掉重复的第五参即可保持同样的显示文本。
 */
public final class TrackingIgnoreMalilibConfigs {
    public static final ConfigBoolean IGNORE_PLAYERS = new ConfigBoolean(
            "ignorePlayers",
            true,
            "iss_elden_ring.config.comment.ignore_players",
            "iss_elden_ring.config.name.ignore_players"
    );

    public static final ConfigBoolean IGNORE_PEACEFUL = new ConfigBoolean(
            "ignorePeaceful",
            true,
            "iss_elden_ring.config.comment.ignore_peaceful",
            "iss_elden_ring.config.name.ignore_peaceful"
    );

    public static final ConfigBoolean IGNORE_NEUTRAL = new ConfigBoolean(
            "ignoreNeutral",
            false,
            "iss_elden_ring.config.comment.ignore_neutral",
            "iss_elden_ring.config.name.ignore_neutral"
    );

    public static final ConfigBoolean IGNORE_HOSTILE = new ConfigBoolean(
            "ignoreHostile",
            false,
            "iss_elden_ring.config.comment.ignore_hostile",
            "iss_elden_ring.config.name.ignore_hostile"
    );

    public static final ImmutableList<ConfigBoolean> OPTIONS = ImmutableList.of(
            IGNORE_PLAYERS,
            IGNORE_PEACEFUL,
            IGNORE_NEUTRAL,
            IGNORE_HOSTILE
    );

    private static final IValueChangeCallback<ConfigBoolean> PUSH_CALLBACK = config -> pushToServer();

    private static boolean suppressPush;

    private TrackingIgnoreMalilibConfigs() {
    }

    /**
     * 挂上变更回调：勾选后立刻打包发往服务端（写入服务端玩家偏好），并乐观更新本地缓存。
     * <p>
     * 注意回调仍会在 {@link #pullFromCache()} 里被触发，所以用 {@code suppressPush} 抑制，
     * 否则「打开界面」这个动作本身就会把旧值回写一遍。
     */
    public static void installCallbacks() {
        for (ConfigBoolean option : OPTIONS) {
            option.setValueChangeCallback(PUSH_CALLBACK);
        }
    }

    /**
     * 打开 GUI 前：用客户端缓存刷新开关，避免显示过期值。
     */
    public static void pullFromCache() {
        TrackingIgnorePrefs prefs = TrackingIgnorePrefsCache.get();
        suppressPush = true;
        try {
            IGNORE_PLAYERS.setBooleanValue(prefs.ignorePlayers());
            IGNORE_PEACEFUL.setBooleanValue(prefs.ignorePeaceful());
            IGNORE_NEUTRAL.setBooleanValue(prefs.ignoreNeutral());
            IGNORE_HOSTILE.setBooleanValue(prefs.ignoreHostile());
        } finally {
            suppressPush = false;
        }
    }

    private static void pushToServer() {
        if (suppressPush) {
            return;
        }
        TrackingIgnorePrefs prefs = new TrackingIgnorePrefs(
                IGNORE_PLAYERS.getBooleanValue(),
                IGNORE_PEACEFUL.getBooleanValue(),
                IGNORE_NEUTRAL.getBooleanValue(),
                IGNORE_HOSTILE.getBooleanValue()
        );
        TrackingIgnorePrefsCache.optimisticLocal(prefs);
        TrackingIgnorePrefsPayload.sendToServer(prefs);
    }
}

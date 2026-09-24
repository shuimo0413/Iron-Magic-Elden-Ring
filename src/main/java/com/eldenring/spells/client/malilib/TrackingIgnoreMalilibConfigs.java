package com.eldenring.spells.client.malilib;

import com.eldenring.spells.network.TrackingIgnorePrefsPayload;
import com.eldenring.spells.tracking.TrackingIgnorePrefs;
import com.eldenring.spells.tracking.TrackingIgnorePrefsCache;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.interfaces.IValueChangeCallback;

/**
 * MaLiLib {@link ConfigBoolean} 列表：UI 用，真正持久化仍走玩家 Attachment + 网络包。
 * <p>
 * 仅在 MaFgLib 存在时由此包加载；不要从无条件加载的类里直接 import。
 */
public final class TrackingIgnoreMalilibConfigs {
    public static final ConfigBoolean IGNORE_PLAYERS = new ConfigBoolean(
            "ignorePlayers",
            true,
            "iss_elden_ring.config.comment.ignore_players",
            "iss_elden_ring.config.name.ignore_players",
            "iss_elden_ring.config.name.ignore_players"
    );

    public static final ConfigBoolean IGNORE_PEACEFUL = new ConfigBoolean(
            "ignorePeaceful",
            true,
            "iss_elden_ring.config.comment.ignore_peaceful",
            "iss_elden_ring.config.name.ignore_peaceful",
            "iss_elden_ring.config.name.ignore_peaceful"
    );

    public static final ConfigBoolean IGNORE_NEUTRAL = new ConfigBoolean(
            "ignoreNeutral",
            false,
            "iss_elden_ring.config.comment.ignore_neutral",
            "iss_elden_ring.config.name.ignore_neutral",
            "iss_elden_ring.config.name.ignore_neutral"
    );

    public static final ConfigBoolean IGNORE_HOSTILE = new ConfigBoolean(
            "ignoreHostile",
            false,
            "iss_elden_ring.config.comment.ignore_hostile",
            "iss_elden_ring.config.name.ignore_hostile",
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
     * 挂上变更回调：勾选后立刻写入服务端 Attachment。
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

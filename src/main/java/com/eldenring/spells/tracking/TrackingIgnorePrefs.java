package com.eldenring.spells.tracking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 玩家级辉石追踪「排除」偏好：勾选表示不追踪该类目标。
 * <p>
 * 默认不追玩家、不追明确和平生物；中立 / 敌对 / 未分类均可追。
 * 未识别出类别的模组生物不会被任何排除项命中，因此默认仍可追踪。
 */
public record TrackingIgnorePrefs(
        boolean ignorePlayers,
        boolean ignorePeaceful,
        boolean ignoreNeutral,
        boolean ignoreHostile
) {
    /**
     * 默认：不追玩家、不追和平；中立与敌对照追。
     */
    public static final TrackingIgnorePrefs DEFAULT = new TrackingIgnorePrefs(true, true, false, false);

    public static final Codec<TrackingIgnorePrefs> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("ignore_players", true).forGetter(TrackingIgnorePrefs::ignorePlayers),
            Codec.BOOL.optionalFieldOf("ignore_peaceful", true).forGetter(TrackingIgnorePrefs::ignorePeaceful),
            Codec.BOOL.optionalFieldOf("ignore_neutral", false).forGetter(TrackingIgnorePrefs::ignoreNeutral),
            Codec.BOOL.optionalFieldOf("ignore_hostile", false).forGetter(TrackingIgnorePrefs::ignoreHostile)
    ).apply(instance, TrackingIgnorePrefs::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrackingIgnorePrefs> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, TrackingIgnorePrefs::ignorePlayers,
                    ByteBufCodecs.BOOL, TrackingIgnorePrefs::ignorePeaceful,
                    ByteBufCodecs.BOOL, TrackingIgnorePrefs::ignoreNeutral,
                    ByteBufCodecs.BOOL, TrackingIgnorePrefs::ignoreHostile,
                    TrackingIgnorePrefs::new
            );

    /**
     * 复制并只改「不追踪玩家」。
     */
    public TrackingIgnorePrefs withIgnorePlayers(boolean value) {
        return new TrackingIgnorePrefs(value, ignorePeaceful, ignoreNeutral, ignoreHostile);
    }

    /**
     * 复制并只改「不追踪和平」。
     */
    public TrackingIgnorePrefs withIgnorePeaceful(boolean value) {
        return new TrackingIgnorePrefs(ignorePlayers, value, ignoreNeutral, ignoreHostile);
    }

    /**
     * 复制并只改「不追踪中立」。
     */
    public TrackingIgnorePrefs withIgnoreNeutral(boolean value) {
        return new TrackingIgnorePrefs(ignorePlayers, ignorePeaceful, value, ignoreHostile);
    }

    /**
     * 复制并只改「不追踪敌对」。
     */
    public TrackingIgnorePrefs withIgnoreHostile(boolean value) {
        return new TrackingIgnorePrefs(ignorePlayers, ignorePeaceful, ignoreNeutral, value);
    }
}

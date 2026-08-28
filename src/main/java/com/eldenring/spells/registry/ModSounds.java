package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本模组 {@link SoundEvent} 注册。资源文件在
 * {@code assets/elden_ring_spells/sounds/}，事件名与 {@code sounds.json} 键一致。
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, EldenRingSpellsMod.MOD_ID);

    /**
     * 飞弹射出音。瞬时弹道咒走 {@code getCastFinishSound}；延迟射出（辉剑、亚兹勒喷流）
     * 在真正出弹时再调 {@link #playProjectileLaunch}。资源：{@code sounds/spell_cast.ogg}。
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> SPELL_CAST =
            SOUND_EVENTS.register("spell_cast", () -> SoundEvent.createVariableRangeEvent(id("spell_cast")));

    /**
     * 蓄力 / 起手音。长吟唱与持续咒的 {@code getCastStartSound} 接这条；
     * 辉剑凝结虽然是瞬时咒，也在漩涡起手时播一次。资源：{@code sounds/spell_cast_start.ogg}。
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> SPELL_CAST_START =
            SOUND_EVENTS.register(
                    "spell_cast_start",
                    () -> SoundEvent.createVariableRangeEvent(id("spell_cast_start"))
            );

    private ModSounds() {
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }

    /**
     * 在实体处播蓄力起手音。只在服务端调，会广播给附近玩家。
     */
    public static void playCastStart(Level level, Entity at) {
        play(level, at.getX(), at.getY(), at.getZ(), SPELL_CAST_START.get(), 1.0f, 1.0f);
    }

    /**
     * 在世界坐标播蓄力起手音。只在服务端调。
     */
    public static void playCastStart(Level level, Vec3 at) {
        play(level, at.x, at.y, at.z, SPELL_CAST_START.get(), 1.0f, 1.0f);
    }

    /**
     * 在实体处播飞弹射出音。只在服务端调。
     */
    public static void playProjectileLaunch(Level level, Entity at) {
        playProjectileLaunch(level, at.getX(), at.getY(), at.getZ(), 1.0f);
    }

    /**
     * 在世界坐标播飞弹射出音。只在服务端调。
     */
    public static void playProjectileLaunch(Level level, Vec3 at) {
        playProjectileLaunch(level, at.x, at.y, at.z, 1.0f);
    }

    /**
     * 在世界坐标播飞弹射出音，可调音高。
     *
     * @param pitch 音高倍率。1.0 为原速；连发时略抬高可听出错峰，不要叠太多次 1.3 秒的原片
     */
    public static void playProjectileLaunch(Level level, double x, double y, double z, float pitch) {
        play(level, x, y, z, SPELL_CAST.get(), 1.0f, pitch);
    }

    private static void play(
            Level level,
            double x,
            double y,
            double z,
            SoundEvent soundEvent,
            float volume,
            float pitch
    ) {
        if (level.isClientSide) {
            return;
        }
        level.playSound(null, x, y, z, soundEvent, SoundSource.PLAYERS, volume, pitch);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, path);
    }
}

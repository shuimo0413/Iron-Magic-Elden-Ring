package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
/**
 * 本模组 {@link SoundEvent} 注册。资源文件在
 * {@code assets/iss_elden_ring/sounds/}，事件名与 {@code sounds.json} 键一致。
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, EldenRingSpellsMod.MOD_ID);

    /**
     * 飞弹射出音。瞬时弹道咒走 {@code getCastFinishSound}；延迟射出（辉剑、亚兹勒喷流）
     * 在真正出弹时再调 {@link #playProjectileLaunch}。资源：{@code sounds/spell_cast.ogg}。
     */
    public static final RegistryObject<SoundEvent> SPELL_CAST =
            SOUND_EVENTS.register("spell_cast", () -> SoundEvent.createVariableRangeEvent(id("spell_cast")));

    /**
     * 蓄力 / 起手音。长吟唱与持续咒的 {@code getCastStartSound} 接这条；
     * 辉剑凝结虽然是瞬时咒，也在漩涡起手时播一次。资源：{@code sounds/spell_cast_start.ogg}。
     */
    public static final RegistryObject<SoundEvent> SPELL_CAST_START =
            SOUND_EVENTS.register(
                    "spell_cast_start",
                    () -> SoundEvent.createVariableRangeEvent(id("spell_cast_start"))
            );

    /**
     * 本模组手动播放施法音时的音量，与铁魔法 {@code AbstractSpell#playSound} 的 2.0 对齐。
     * <p>
     * 客户端增益上限为 1.0，大于 1 只放大可听范围：范围 = {@code sounds.json} 的
     * {@code attenuation_distance}（16 格）× 音量 = 32 格，线性衰减到 0。
     * 调大 → 更远能听见；调小到 1.0 → 只剩 16 格。近处响度不变。
     * <p>
     * 距离衰减只对<b>单声道</b> OGG 生效，立体声会变成全局音量；换音频文件时先跑
     * {@code 工具链/sound_to_mono.py}。
     */
    private static final float SPELL_SOUND_VOLUME = 2.0f;

    private ModSounds() {
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }

    /**
     * 在实体处播蓄力起手音。只在服务端调，会广播给附近玩家。
     */
    public static void playCastStart(Level level, Entity at) {
        play(level, at.getX(), at.getY(), at.getZ(), SPELL_CAST_START.get(), SPELL_SOUND_VOLUME, 1.0f);
    }

    /**
     * 在世界坐标播蓄力起手音。只在服务端调。
     */
    public static void playCastStart(Level level, Vec3 at) {
        play(level, at.x, at.y, at.z, SPELL_CAST_START.get(), SPELL_SOUND_VOLUME, 1.0f);
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
        play(level, x, y, z, SPELL_CAST.get(), SPELL_SOUND_VOLUME, pitch);
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
        return new ResourceLocation(EldenRingSpellsMod.MOD_ID, path);
    }
}

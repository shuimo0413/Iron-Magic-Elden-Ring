package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本模组粒子类型注册。辉石系视觉统一经 {@link com.eldenring.spells.particle.glintstone.GlintstoneFx} 生成。
 */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, EldenRingSpellsMod.MOD_ID);

    /** 尖锐青蓝火花 — 拖尾与命中碎片的基础层。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLINTSTONE_SPARK =
            PARTICLE_TYPES.register("glintstone_spark", () -> new SimpleParticleType(false));

    /** 柔和青蓝光晕 — 弹道周围体积感。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLINTSTONE_GLOW =
            PARTICLE_TYPES.register("glintstone_glow", () -> new SimpleParticleType(false));

    /** 菱形碎晶 — 命中飞溅的晶体碎片。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLINTSTONE_SHARD =
            PARTICLE_TYPES.register("glintstone_shard", () -> new SimpleParticleType(false));

    /** 十字闪星（多帧）— 拖尾中的稀疏高光闪烁。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLINTSTONE_MOTE =
            PARTICLE_TYPES.register("glintstone_mote", () -> new SimpleParticleType(false));

    /** 中心绽光 — 施法/命中瞬间的冲击闪光。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLINTSTONE_FLARE =
            PARTICLE_TYPES.register("glintstone_flare", () -> new SimpleParticleType(false));

    /** 稀薄雾气 — 拖尾与爆发的软体积层。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLINTSTONE_MIST =
            PARTICLE_TYPES.register("glintstone_mist", () -> new SimpleParticleType(false));

    private ModParticles() {
    }

    public static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }
}

package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.particle.cometazur.CometAzurJetOptions;
import com.eldenring.spells.particle.cometazur.CometAzurVortexOptions;
import com.eldenring.spells.particle.foundingrain.OverheadNebulaAccentOptions;
import com.eldenring.spells.particle.glintstone.AcademyGlintstoneSigilParticleOptions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
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

    /** 星云雾气团 — 星河体积层。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STAR_RIVER_MIST =
            PARTICLE_TYPES.register("star_river_mist", () -> new SimpleParticleType(false));

    /** 星云深靛辉光。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STAR_RIVER_GLOW =
            PARTICLE_TYPES.register("star_river_glow", () -> new SimpleParticleType(false));

    /** 虚空闪星（三帧）。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VOID_MOTE =
            PARTICLE_TYPES.register("void_mote", () -> new SimpleParticleType(false));

    /** 微型黑洞核。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VOID_CORE =
            PARTICLE_TYPES.register("void_core", () -> new SimpleParticleType(false));

    /** 迷你双臂漩涡星系。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NEBULA_SPIRAL =
            PARTICLE_TYPES.register("nebula_spiral", () -> new SimpleParticleType(false));

    /** 紫晶碎片。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VIOLET_SHARD =
            PARTICLE_TYPES.register("violet_shard", () -> new SimpleParticleType(false));

    /** 湮灭绽光。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ABYSS_FLARE =
            PARTICLE_TYPES.register("abyss_flare", () -> new SimpleParticleType(false));

    /** 斜向彗星残影。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STAR_STREAK =
            PARTICLE_TYPES.register("star_streak", () -> new SimpleParticleType(false));

    /** 微星团。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STAR_CLUSTER =
            PARTICLE_TYPES.register("star_cluster", () -> new SimpleParticleType(false));

    /** 蚀环。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ECLIPSE_RING =
            PARTICLE_TYPES.register("eclipse_ring", () -> new SimpleParticleType(false));

    /** 蓝紫双色星尘。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TWIN_DUST =
            PARTICLE_TYPES.register("twin_dust", () -> new SimpleParticleType(false));

    /** 八芒新星。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NOVA_STAR =
            PARTICLE_TYPES.register("nova_star", () -> new SimpleParticleType(false));

    /** 暗物质丝。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DARK_FILAMENT =
            PARTICLE_TYPES.register("dark_filament", () -> new SimpleParticleType(false));

    /** 脉冲环。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PULSE_RING =
            PARTICLE_TYPES.register("pulse_ring", () -> new SimpleParticleType(false));

    /** 月牙星河残影。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CRESCENT_WAKE =
            PARTICLE_TYPES.register("crescent_wake", () -> new SimpleParticleType(false));

    /** 双星。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BINARY_STAR =
            PARTICLE_TYPES.register("binary_star", () -> new SimpleParticleType(false));

    /**
     * 创星雨升空光点。贴图复用 {@code void_mote} 三帧；运动由 {@code StarAscentParticle} 插值到头顶。
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STAR_ASCENT_MOTE =
            PARTICLE_TYPES.register("star_ascent_mote", () -> new SimpleParticleType(false));

    /**
     * 创星雨升空小拖尾。贴图复用 {@code star_streak}；短命，钉在光点刚走过的位置。
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STAR_ASCENT_TRAIL =
            PARTICLE_TYPES.register("star_ascent_trail", () -> new SimpleParticleType(false));

    /** 星云云朵：团雾。{@code overrideLimiter=true}，最低粒子设置下仍显示签名雨云。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NEBULA_CLOUD_PUFF =
            registerAlwaysShown("nebula_cloud_puff");

    /** 星云云朵：薄雾。最大最淡，用来垫体积。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NEBULA_CLOUD_HAZE =
            registerAlwaysShown("nebula_cloud_haze");

    /** 星云云朵：积云。三瓣主体。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NEBULA_CLOUD_CUMULUS =
            registerAlwaysShown("nebula_cloud_cumulus");

    /** 星云云朵：卷云。斜向丝缕，剪开圆形外沿。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NEBULA_CLOUD_CIRRUS =
            registerAlwaysShown("nebula_cloud_cirrus");

    /** 星云云朵：暗核。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NEBULA_CLOUD_CORE =
            registerAlwaysShown("nebula_cloud_core");

    /** 星云云朵：星尘团。云里嵌星。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NEBULA_CLOUD_STARDUST =
            registerAlwaysShown("nebula_cloud_stardust");

    /** 星云云朵：裂隙。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NEBULA_CLOUD_RIFT =
            registerAlwaysShown("nebula_cloud_rift");

    /** 星云云朵：飘絮。外沿碎云。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NEBULA_CLOUD_WISP =
            registerAlwaysShown("nebula_cloud_wisp");

    /** 星云云朵：层云。扁平底边。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NEBULA_CLOUD_STRATUS =
            registerAlwaysShown("nebula_cloud_stratus");

    /** 星云云朵：双色。蓝紫交界。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NEBULA_CLOUD_TWIN =
            registerAlwaysShown("nebula_cloud_twin");

    /**
     * 创星雨气团：亮色软边，加法混合。大、淡，叠多张才像云。
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NEBULA_VEIL =
            registerAlwaysShown("nebula_veil");

    /**
     * 创星雨核光：中心近白的软光斑。
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NEBULA_BLOOM =
            registerAlwaysShown("nebula_bloom");

    /**
     * 创星雨斜向软絮：拉开圆形外沿。
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NEBULA_VEIL_WISP =
            registerAlwaysShown("nebula_veil_wisp");

    /**
     * 创星雨落地涟漪：贴地水平展开，三帧从小环胀到淡圈。
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STAR_RIVER_RIPPLE =
            registerAlwaysShown("star_river_ripple");

    /**
     * 创星雨落地飞沫：扇形水珠，只在撞击点刷一颗，不沿雨点轨迹铺。
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STAR_RIVER_SPRAY =
            registerAlwaysShown("star_river_spray");

    /**
     * 头顶星云的星河点缀：一张表里塞 mist / mote / filament 等，用 {@code Accent} 选贴图。
     * 寿命 3 秒，和云朵主体一起淡入淡出。
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<OverheadNebulaAccentOptions>> OVERHEAD_NEBULA_ACCENT =
            PARTICLE_TYPES.register("overhead_nebula_accent", () -> new ParticleType<>(true) {
                @Override
                public MapCodec<OverheadNebulaAccentOptions> codec() {
                    return OverheadNebulaAccentOptions.CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, OverheadNebulaAccentOptions> streamCodec() {
                    return OverheadNebulaAccentOptions.STREAM_CODEC;
                }
            });

    /**
     * 学院辉石法阵 — 施法时锁在施法者头顶的镂空纹章。
     * {@code overrideLimiter=true}：最低粒子设置下仍显示，避免签名特效被画质选项吃掉。
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<AcademyGlintstoneSigilParticleOptions>> ACADEMY_GLINTSTONE_SIGIL =
            PARTICLE_TYPES.register("academy_glintstone_sigil", () -> new ParticleType<>(true) {
                @Override
                public MapCodec<AcademyGlintstoneSigilParticleOptions> codec() {
                    return AcademyGlintstoneSigilParticleOptions.CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, AcademyGlintstoneSigilParticleOptions> streamCodec() {
                    return AcademyGlintstoneSigilParticleOptions.STREAM_CODEC;
                }
            });

    /**
     * 彗星亚兹勒起手漩涡中心：{@code comet_azur_shrink_1} / {@code shrink_2} 两帧，由粒子自己平面旋转。
     * 带朝向数据，供客户端在垂直视线的平面上铺对数螺线。
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<CometAzurVortexOptions>> COMET_AZUR_SHRINK =
            PARTICLE_TYPES.register("comet_azur_shrink", () -> new ParticleType<>(true) {
                @Override
                public MapCodec<CometAzurVortexOptions> codec() {
                    return CometAzurVortexOptions.CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, CometAzurVortexOptions> streamCodec() {
                    return CometAzurVortexOptions.STREAM_CODEC;
                }
            });

    /** 亚兹勒起手汇聚：mote_1 / mote_2 十字闪星。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COMET_AZUR_MOTE =
            registerAlwaysShown("comet_azur_mote");

    /** 亚兹勒起手汇聚：八芒冲击星。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COMET_AZUR_IMPACT =
            registerAlwaysShown("comet_azur_impact");

    /** 亚兹勒起手汇聚：碎星团。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COMET_AZUR_HEAD =
            registerAlwaysShown("comet_azur_head");

    /** 亚兹勒起手汇聚：星尘。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COMET_AZUR_DUST =
            registerAlwaysShown("comet_azur_dust");

    /** 蓄力结束涟漪：vortex_field 软边圆盘。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COMET_AZUR_VORTEX_FIELD =
            registerAlwaysShown("comet_azur_vortex_field");

    /** 蓄力结束涟漪：vortex 双臂螺旋圆盘。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COMET_AZUR_VORTEX =
            registerAlwaysShown("comet_azur_vortex");

    /**
     * 星辰涟漪外沿点缀：闪星 / 星团 / 暗核 / 火花。
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COMET_AZUR_SHOCKWAVE_ACCENT =
            registerAlwaysShown("comet_azur_shockwave_accent");

    /**
     * 星辰涟漪光圈：pulse_ring / eclipse_ring / lens，加法混合的空心环。
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COMET_AZUR_SHOCKWAVE_RING =
            registerAlwaysShown("comet_azur_shockwave_ring");

    /**
     * 喷流周围粒子发射器。服务端只同步这一颗；客户端再铺墨绿星云 / 星系 / 闪星。
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<CometAzurJetOptions>> COMET_AZUR_JET_SURROUND =
            PARTICLE_TYPES.register("comet_azur_jet_surround", () -> new ParticleType<>(true) {
                @Override
                public MapCodec<CometAzurJetOptions> codec() {
                    return CometAzurJetOptions.CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, CometAzurJetOptions> streamCodec() {
                    return CometAzurJetOptions.STREAM_CODEC;
                }
            });

    private ModParticles() {
    }

    /**
     * 签名特效粒子：{@code overrideLimiter=true}，最低粒子设置下仍显示。
     */
    private static DeferredHolder<ParticleType<?>, SimpleParticleType> registerAlwaysShown(String path) {
        return PARTICLE_TYPES.register(path, () -> new SimpleParticleType(true));
    }

    public static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }
}

package com.eldenring.spells.particle.foundingrain;

import com.eldenring.spells.registry.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

/**
 * 头顶星云里「手里那套星河贴图」的网络数据。
 * <p>
 * 不能靠 ThreadLocal 改寿命：粒子是在客户端收到包之后才构造的。
 * {@link Accent#ordinal()} 必须和 {@code particles/overhead_nebula_accent.json} 的贴图顺序一致。
 */
public final class OverheadNebulaAccentOptions implements ParticleOptions {

    /**
     * 贴图种类。顺序必须对上 {@code overhead_nebula_accent.json}：
     * glow → twin_dust → void_mote_2。
     */
    public enum Accent {
        GLOW(0.22f, 0.10f, 0.22f, false, 0.0f, false, 1.00f, 1.00f, 1.00f),
        DUST(0.20f, 0.08f, 0.18f, false, 0.0f, false, 1.00f, 1.00f, 1.00f),
        MOTE(0.07f, 0.04f, 0.92f, false, 0.0f, true, 1.00f, 1.00f, 1.00f);

        final float quadSizeMinBlocks;
        final float quadSizeRandomBlocks;
        final float peakAlpha;
        final boolean spin;
        final float rollRadiansPerTick;
        final boolean pulse;
        final float tintRed;
        final float tintGreen;
        final float tintBlue;

        Accent(
                float quadSizeMinBlocks,
                float quadSizeRandomBlocks,
                float peakAlpha,
                boolean spin,
                float rollRadiansPerTick,
                boolean pulse,
                float tintRed,
                float tintGreen,
                float tintBlue
        ) {
            this.quadSizeMinBlocks = quadSizeMinBlocks;
            this.quadSizeRandomBlocks = quadSizeRandomBlocks;
            this.peakAlpha = peakAlpha;
            this.spin = spin;
            this.rollRadiansPerTick = rollRadiansPerTick;
            this.pulse = pulse;
            this.tintRed = tintRed;
            this.tintGreen = tintGreen;
            this.tintBlue = tintBlue;
        }

        static Accent fromOrdinal(int ordinal) {
            Accent[] values = values();
            return values[Mth.clamp(ordinal, 0, values.length - 1)];
        }
    }

    public static final MapCodec<OverheadNebulaAccentOptions> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.INT.fieldOf("accent")
                            .forGetter(options -> options.accent.ordinal())
            ).apply(instance, ordinal -> new OverheadNebulaAccentOptions(Accent.fromOrdinal(ordinal)))
    );

    private final Accent accent;

    public OverheadNebulaAccentOptions(Accent accent) {
        this.accent = accent;
    }

    public Accent accent() {
        return accent;
    }

    /**
     * 1.20.1 网络写入口：只发贴图下标。
     * <p>
     * 用定长 {@code writeInt}（对应原 {@code ByteBufCodecs.INT}）；变长的是
     * {@code ByteBufCodecs.VAR_INT}，两者字节布局不同，不能混用。
     */
    public void writeTo(FriendlyByteBuf buffer) {
        buffer.writeInt(this.accent.ordinal());
    }

    /**
     * 1.20.1 网络读入口：越界下标由 {@link Accent#fromOrdinal(int)} 收敛到合法范围，
     * 避免联机时因版本不一致的贴图表造成数组越界。
     */
    public static OverheadNebulaAccentOptions read(FriendlyByteBuf buffer) {
        int accentOrdinal = buffer.readInt();
        return new OverheadNebulaAccentOptions(Accent.fromOrdinal(accentOrdinal));
    }

    /** 1.20.1 {@link ParticleOptions} 的网络序列化钩子（1.20.5+ 才改为 StreamCodec）。 */
    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        writeTo(buffer);
    }

    /** 1.20.1 {@link ParticleOptions} 要求的调试文本（命令回显用，不参与同步）。 */
    @Override
    public String writeToString() {
        return BuiltInRegistries.PARTICLE_TYPE.getKey(getType()) + " " + this.accent.ordinal();
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.OVERHEAD_NEBULA_ACCENT.get();
    }
}

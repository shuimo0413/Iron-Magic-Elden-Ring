package com.eldenring.spells.particle.foundingrain;

import com.eldenring.spells.registry.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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

    public static final StreamCodec<ByteBuf, OverheadNebulaAccentOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            options -> options.accent.ordinal(),
            ordinal -> new OverheadNebulaAccentOptions(Accent.fromOrdinal(ordinal))
    );

    private final Accent accent;

    public OverheadNebulaAccentOptions(Accent accent) {
        this.accent = accent;
    }

    public Accent accent() {
        return accent;
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.OVERHEAD_NEBULA_ACCENT.get();
    }
}

package com.eldenring.spells.particle.glintstone;

import com.eldenring.spells.registry.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 学院辉石法阵粒子的额外数据：施法者实体 ID，供客户端每 tick 跟随头顶。
 */
public final class AcademyGlintstoneSigilParticleOptions implements ParticleOptions {

    public static final MapCodec<AcademyGlintstoneSigilParticleOptions> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.INT.fieldOf("caster_entity_id")
                            .forGetter(AcademyGlintstoneSigilParticleOptions::casterEntityId)
            ).apply(instance, AcademyGlintstoneSigilParticleOptions::new)
    );

    public static final StreamCodec<ByteBuf, AcademyGlintstoneSigilParticleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            AcademyGlintstoneSigilParticleOptions::casterEntityId,
            AcademyGlintstoneSigilParticleOptions::new
    );

    private final int casterEntityId;

    public AcademyGlintstoneSigilParticleOptions(int casterEntityId) {
        this.casterEntityId = casterEntityId;
    }

    /**
     * 施法者在当前维度的运行时实体 ID（不是 UUID）。
     */
    public int casterEntityId() {
        return casterEntityId;
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.ACADEMY_GLINTSTONE_SIGIL.get();
    }
}

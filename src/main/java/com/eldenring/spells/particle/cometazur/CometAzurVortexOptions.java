package com.eldenring.spells.particle.cometazur;

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
 * 亚兹勒漩涡中心的网络数据：哪张 shrink、转多快、施法时朝向（用来在垂直视线的平面上铺对数螺线）。
 */
public final class CometAzurVortexOptions implements ParticleOptions {

    public static final MapCodec<CometAzurVortexOptions> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.INT.fieldOf("sprite_index").forGetter(CometAzurVortexOptions::spriteIndex),
                    Codec.FLOAT.fieldOf("roll_radians_per_tick").forGetter(CometAzurVortexOptions::rollRadiansPerTick),
                    Codec.FLOAT.fieldOf("yaw_degrees").forGetter(CometAzurVortexOptions::yawDegrees),
                    Codec.FLOAT.fieldOf("pitch_degrees").forGetter(CometAzurVortexOptions::pitchDegrees),
                    Codec.BOOL.fieldOf("spawn_spirals").forGetter(CometAzurVortexOptions::spawnSpirals)
            ).apply(instance, CometAzurVortexOptions::new)
    );

    public static final StreamCodec<ByteBuf, CometAzurVortexOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            CometAzurVortexOptions::spriteIndex,
            ByteBufCodecs.FLOAT,
            CometAzurVortexOptions::rollRadiansPerTick,
            ByteBufCodecs.FLOAT,
            CometAzurVortexOptions::yawDegrees,
            ByteBufCodecs.FLOAT,
            CometAzurVortexOptions::pitchDegrees,
            ByteBufCodecs.BOOL,
            CometAzurVortexOptions::spawnSpirals,
            CometAzurVortexOptions::new
    );

    private final int spriteIndex;
    private final float rollRadiansPerTick;
    private final float yawDegrees;
    private final float pitchDegrees;
    private final boolean spawnSpirals;

    public CometAzurVortexOptions(
            int spriteIndex,
            float rollRadiansPerTick,
            float yawDegrees,
            float pitchDegrees,
            boolean spawnSpirals
    ) {
        this.spriteIndex = spriteIndex;
        this.rollRadiansPerTick = rollRadiansPerTick;
        this.yawDegrees = yawDegrees;
        this.pitchDegrees = pitchDegrees;
        this.spawnSpirals = spawnSpirals;
    }

    /** 0 = shrink_1，1 = shrink_2。 */
    public int spriteIndex() {
        return spriteIndex;
    }

    /** 平面旋转角速度（弧度 / tick）。 */
    public float rollRadiansPerTick() {
        return rollRadiansPerTick;
    }

    /** 出手瞬间水平朝向（度），用来建螺线平面。 */
    public float yawDegrees() {
        return yawDegrees;
    }

    /** 出手瞬间俯仰（度）。 */
    public float pitchDegrees() {
        return pitchDegrees;
    }

    /** 只有主层为 true，避免两层各铺一遍螺线。 */
    public boolean spawnSpirals() {
        return spawnSpirals;
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.COMET_AZUR_SHRINK.get();
    }
}

package com.eldenring.spells.particle.cometazur;

import com.eldenring.spells.registry.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;

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

    /**
     * 1.20.1 网络写入口。字段顺序与原 {@code StreamCodec.composite(...)} 一致：
     * sprite_index(int) → roll → yaw → pitch → spawn_spirals(bool)，
     * 必须与 {@link #read(FriendlyByteBuf)} 严格对应，否则联机端螺线会错位。
     * <p>
     * sprite_index 用定长 {@code writeInt}（对应原 {@code ByteBufCodecs.INT}），
     * 不是 {@code writeVarInt}（那是 {@code ByteBufCodecs.VAR_INT}）。
     */
    public void writeTo(FriendlyByteBuf buffer) {
        buffer.writeInt(this.spriteIndex);
        buffer.writeFloat(this.rollRadiansPerTick);
        buffer.writeFloat(this.yawDegrees);
        buffer.writeFloat(this.pitchDegrees);
        buffer.writeBoolean(this.spawnSpirals);
    }

    /** 1.20.1 网络读入口，顺序与 {@link #writeTo(FriendlyByteBuf)} 一致。 */
    public static CometAzurVortexOptions read(FriendlyByteBuf buffer) {
        int spriteIndex = buffer.readInt();
        float rollRadiansPerTick = buffer.readFloat();
        float yawDegrees = buffer.readFloat();
        float pitchDegrees = buffer.readFloat();
        boolean spawnSpirals = buffer.readBoolean();
        return new CometAzurVortexOptions(spriteIndex, rollRadiansPerTick, yawDegrees, pitchDegrees, spawnSpirals);
    }

    /** 1.20.1 {@link ParticleOptions} 的网络序列化钩子（1.20.5+ 才改为 StreamCodec）。 */
    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        writeTo(buffer);
    }

    /** 1.20.1 {@link ParticleOptions} 要求的调试文本（命令回显用，不参与同步）。 */
    @Override
    public String writeToString() {
        return BuiltInRegistries.PARTICLE_TYPE.getKey(getType()) + " " + this.spriteIndex
                + " " + this.rollRadiansPerTick + " " + this.yawDegrees + " " + this.pitchDegrees
                + " " + this.spawnSpirals;
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.COMET_AZUR_SHRINK.get();
    }
}

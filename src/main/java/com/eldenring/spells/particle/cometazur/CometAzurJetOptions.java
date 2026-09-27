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
 * 亚兹勒喷流周围粒子的网络 / 本地数据。
 * <p>
 * 服务端只发发射器：yaw / pitch 重建喷流口坐标系。周围飞粒子只在客户端
 * {@link CometAzurJetEmitterParticle} 里用 {@link #flying} 再刷，不走网络。
 */
public final class CometAzurJetOptions implements ParticleOptions {

    public static final MapCodec<CometAzurJetOptions> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.FLOAT.fieldOf("yaw_degrees").forGetter(CometAzurJetOptions::yawDegrees),
                    Codec.FLOAT.fieldOf("pitch_degrees").forGetter(CometAzurJetOptions::pitchDegrees)
            ).apply(instance, CometAzurJetOptions::emitter)
    );

    private final boolean emitter;
    private final float yawDegrees;
    private final float pitchDegrees;
    private final int kindOrdinal;
    private final int motionOrdinal;
    private final float ringAngleRadians;
    private final float ringRadiusBlocks;
    private final float birthAlongBeamBlocks;
    private final float helixRadiansPerTick;

    private CometAzurJetOptions(
            boolean emitter,
            float yawDegrees,
            float pitchDegrees,
            int kindOrdinal,
            int motionOrdinal,
            float ringAngleRadians,
            float ringRadiusBlocks,
            float birthAlongBeamBlocks,
            float helixRadiansPerTick
    ) {
        this.emitter = emitter;
        this.yawDegrees = yawDegrees;
        this.pitchDegrees = pitchDegrees;
        this.kindOrdinal = kindOrdinal;
        this.motionOrdinal = motionOrdinal;
        this.ringAngleRadians = ringAngleRadians;
        this.ringRadiusBlocks = ringRadiusBlocks;
        this.birthAlongBeamBlocks = birthAlongBeamBlocks;
        this.helixRadiansPerTick = helixRadiansPerTick;
    }

    /**
     * 服务端每圈喷流口发一颗：只带朝向。
     */
    public static CometAzurJetOptions emitter(float yawDegrees, float pitchDegrees) {
        return new CometAzurJetOptions(true, yawDegrees, pitchDegrees, 0, 0, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    /**
     * 客户端发射器在喷流口本地再刷的飞粒子。角速度用运动模式默认值。
     */
    public static CometAzurJetOptions flying(
            float yawDegrees,
            float pitchDegrees,
            int kindOrdinal,
            int motionOrdinal,
            float ringAngleRadians,
            float ringRadiusBlocks,
            float birthAlongBeamBlocks
    ) {
        return flying(
                yawDegrees,
                pitchDegrees,
                kindOrdinal,
                motionOrdinal,
                ringAngleRadians,
                ringRadiusBlocks,
                birthAlongBeamBlocks,
                0.0f
        );
    }

    /**
     * @param helixRadiansPerTick 绕喷流轴角速度（弧度 / tick）。0 = 粒子自己按运动模式选；
     *                            能量场各条欧拉臂传入不同值，形成多条螺旋曲线。
     */
    public static CometAzurJetOptions flying(
            float yawDegrees,
            float pitchDegrees,
            int kindOrdinal,
            int motionOrdinal,
            float ringAngleRadians,
            float ringRadiusBlocks,
            float birthAlongBeamBlocks,
            float helixRadiansPerTick
    ) {
        return new CometAzurJetOptions(
                false,
                yawDegrees,
                pitchDegrees,
                kindOrdinal,
                motionOrdinal,
                ringAngleRadians,
                ringRadiusBlocks,
                birthAlongBeamBlocks,
                helixRadiansPerTick
        );
    }

    /** true：这颗自己不画，只负责在客户端铺一圈。 */
    public boolean emitter() {
        return emitter;
    }

    /** 喷流口水平朝向（度）。 */
    public float yawDegrees() {
        return yawDegrees;
    }

    /** 喷流口俯仰（度）。 */
    public float pitchDegrees() {
        return pitchDegrees;
    }

    /** {@link CometAzurJetSurroundParticle.Kind#ordinal()}。发射器上无意义。 */
    public int kindOrdinal() {
        return kindOrdinal;
    }

    /** {@link CometAzurJetSurroundParticle.MotionMode#ordinal()}。发射器上无意义。 */
    public int motionOrdinal() {
        return motionOrdinal;
    }

    /** 出生在垂直喷流平面上的极角（弧度）。 */
    public float ringAngleRadians() {
        return ringAngleRadians;
    }

    /** 出生圆半径（方块）。 */
    public float ringRadiusBlocks() {
        return ringRadiusBlocks;
    }

    /** 出生时沿喷流轴的距离（方块）。 */
    public float birthAlongBeamBlocks() {
        return birthAlongBeamBlocks;
    }

    /**
     * 绕喷流轴角速度（弧度 / tick）。0 表示粒子按运动模式自己选。
     */
    public float helixRadiansPerTick() {
        return helixRadiansPerTick;
    }

    /**
     * 1.20.1 网络写入口：只发 yaw / pitch 两个朝向。
     * <p>
     * 这是原 {@code StreamCodec.composite(...)} 里两个字段的等价实现，字段顺序必须与
     * {@link #read(FriendlyByteBuf)} 完全一致，否则联机端喷流朝向会错位。
     * {@code emitter} 与各 ordinal / 半径是纯客户端本地量，不进网络。
     */
    public void writeTo(FriendlyByteBuf buffer) {
        buffer.writeFloat(this.yawDegrees);
        buffer.writeFloat(this.pitchDegrees);
    }

    /**
     * 1.20.1 网络读入口。读回的两数交给 {@link #emitter(float, float)}，
     * 因为服务端只会发「发射器」形态（飞粒子由客户端 {@link CometAzurJetEmitterParticle} 本地再刷）。
     */
    public static CometAzurJetOptions read(FriendlyByteBuf buffer) {
        float yawDegrees = buffer.readFloat();
        float pitchDegrees = buffer.readFloat();
        return emitter(yawDegrees, pitchDegrees);
    }

    /** 1.20.1 {@link ParticleOptions} 的网络序列化钩子（1.20.5+ 才改为 StreamCodec）。 */
    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        writeTo(buffer);
    }

    /** 1.20.1 {@link ParticleOptions} 要求的调试文本（命令回显用，不参与同步）。 */
    @Override
    public String writeToString() {
        return BuiltInRegistries.PARTICLE_TYPE.getKey(getType()) + " " + this.yawDegrees + " " + this.pitchDegrees;
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.COMET_AZUR_JET_SURROUND.get();
    }
}

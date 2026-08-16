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

    public static final StreamCodec<ByteBuf, CometAzurJetOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            CometAzurJetOptions::yawDegrees,
            ByteBufCodecs.FLOAT,
            CometAzurJetOptions::pitchDegrees,
            CometAzurJetOptions::emitter
    );

    private final boolean emitter;
    private final float yawDegrees;
    private final float pitchDegrees;
    private final int kindOrdinal;
    private final int motionOrdinal;
    private final float ringAngleRadians;
    private final float ringRadiusBlocks;

    private CometAzurJetOptions(
            boolean emitter,
            float yawDegrees,
            float pitchDegrees,
            int kindOrdinal,
            int motionOrdinal,
            float ringAngleRadians,
            float ringRadiusBlocks
    ) {
        this.emitter = emitter;
        this.yawDegrees = yawDegrees;
        this.pitchDegrees = pitchDegrees;
        this.kindOrdinal = kindOrdinal;
        this.motionOrdinal = motionOrdinal;
        this.ringAngleRadians = ringAngleRadians;
        this.ringRadiusBlocks = ringRadiusBlocks;
    }

    /**
     * 服务端每圈喷流口发一颗：只带朝向。
     */
    public static CometAzurJetOptions emitter(float yawDegrees, float pitchDegrees) {
        return new CometAzurJetOptions(true, yawDegrees, pitchDegrees, 0, 0, 0.0f, 0.0f);
    }

    /**
     * 客户端发射器在喷流口本地再刷的飞粒子。
     */
    public static CometAzurJetOptions flying(
            float yawDegrees,
            float pitchDegrees,
            int kindOrdinal,
            int motionOrdinal,
            float ringAngleRadians,
            float ringRadiusBlocks
    ) {
        return new CometAzurJetOptions(
                false,
                yawDegrees,
                pitchDegrees,
                kindOrdinal,
                motionOrdinal,
                ringAngleRadians,
                ringRadiusBlocks
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

    @Override
    public ParticleType<?> getType() {
        return ModParticles.COMET_AZUR_JET_SURROUND.get();
    }
}

package com.eldenring.spells.particle.cometazur;

import com.eldenring.spells.registry.ModParticles;
import com.eldenring.spells.spell.CometAzurCastData;
import com.eldenring.spells.tuning.CometAzurTuning;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 彗星亚兹勒视觉入口。
 * <p>
 * 蓄力：服务端刷两层旋转中心；主层在客户端按对数螺线铺汇聚粒子。
 * 蓄力结束：同一位置爆星辰涟漪。
 * 喷流：法术 tick 维持 {@link com.eldenring.spells.entity.CometAzurJetEntity}（圆管星河柱），
 * 并在喷流口刷墨绿星云 / 星团 / 闪星套管。
 * 只在服务端调用粒子入口；客户端再调会双端各刷一次。
 */
public final class CometAzurFx {

    private CometAzurFx() {
    }

    /**
     * 在施法者视线前方钉一个 2 秒漩涡。位置和朝向按出手瞬间计算，之后不跟随玩家。
     */
    public static void spawnStartupVortex(Level level, LivingEntity caster) {
        if (level.isClientSide || caster == null) {
            return;
        }
        Vec3 vortexCenter = vortexCenterInFrontOf(caster);
        float yawDegrees = caster.getYRot();
        float pitchDegrees = caster.getXRot();
        spawnVortexLayer(
                level,
                vortexCenter,
                0,
                CometAzurTuning.STARTUP_SHRINK_1_ROLL_RADIANS_PER_TICK,
                yawDegrees,
                pitchDegrees,
                false
        );
        spawnVortexLayer(
                level,
                vortexCenter,
                1,
                CometAzurTuning.STARTUP_SHRINK_2_ROLL_RADIANS_PER_TICK,
                yawDegrees,
                pitchDegrees,
                true
        );
    }

    /**
     * 眼睛前方、略往下：第三人称能看见整团，第一人称不至于糊满准星。
     */
    public static Vec3 vortexCenterInFrontOf(LivingEntity caster) {
        Vec3 lookDirection = caster.getLookAngle();
        return caster.getEyePosition()
                .add(lookDirection.scale(CometAzurTuning.STARTUP_VORTEX_FORWARD_OFFSET_BLOCKS))
                .subtract(0.0, CometAzurTuning.STARTUP_VORTEX_DOWN_OFFSET_BLOCKS, 0.0);
    }

    /**
     * 喷流口：沿视线钉在玩家正前方，不跟漩涡下偏，出来就是一根粗柱。
     */
    public static Vec3 jetMouthInFrontOf(LivingEntity caster) {
        return caster.getEyePosition()
                .add(caster.getLookAngle().scale(CometAzurTuning.JET_BEAM_MOUTH_FORWARD_OFFSET_BLOCKS));
    }

    private static void spawnVortexLayer(
            Level level,
            Vec3 vortexCenter,
            int spriteIndex,
            float rollRadiansPerTick,
            float yawDegrees,
            float pitchDegrees,
            boolean spawnSpirals
    ) {
        MagicManager.spawnParticles(
                level,
                new CometAzurVortexOptions(
                        spriteIndex,
                        rollRadiansPerTick,
                        yawDegrees,
                        pitchDegrees,
                        spawnSpirals
                ),
                vortexCenter.x,
                vortexCenter.y,
                vortexCenter.z,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                true
        );
    }

    /**
     * 蓄力结束后在漩涡原位置爆多层嵌套星辰涟漪。无伤害。
     * 波次：0 主环、1 回声、2 中心绽光、3 最外、4 中层、5 内层。
     */
    public static void spawnChargeShockwave(Level level, CometAzurCastData castData) {
        if (level.isClientSide || castData == null) {
            return;
        }
        Vec3 shockwaveCenter = castData.vortexCenter();
        float yawDegrees = castData.yawDegrees();
        float pitchDegrees = castData.pitchDegrees();
        spawnShockwaveRing(level, shockwaveCenter, yawDegrees, pitchDegrees, 0.0);
        spawnShockwaveRing(level, shockwaveCenter, yawDegrees, pitchDegrees, 1.0);
        spawnShockwaveRing(level, shockwaveCenter, yawDegrees, pitchDegrees, 2.0);
        spawnShockwaveRing(level, shockwaveCenter, yawDegrees, pitchDegrees, 3.0);
        spawnShockwaveRing(level, shockwaveCenter, yawDegrees, pitchDegrees, 4.0);
        spawnShockwaveRing(level, shockwaveCenter, yawDegrees, pitchDegrees, 5.0);
    }

    /**
     * {@code count = 0} 时 yaw / pitch / 波次原样进 xd / yd / zd。
     */
    private static void spawnShockwaveRing(
            Level level,
            Vec3 shockwaveCenter,
            float yawDegrees,
            float pitchDegrees,
            double waveIndex
    ) {
        MagicManager.spawnParticles(
                level,
                ModParticles.COMET_AZUR_SHOCKWAVE_RING.get(),
                shockwaveCenter.x,
                shockwaveCenter.y,
                shockwaveCenter.z,
                0,
                yawDegrees,
                pitchDegrees,
                waveIndex,
                1.0,
                true
        );
    }

    /**
     * 在锁定喷流口刷一圈星河套管（星云体积 + 螺旋星团 + 闪星），朝向用出手时钉死的 yaw/pitch。
     */
    public static void spawnJetSurround(Level level, CometAzurCastData castData) {
        if (level.isClientSide || castData == null) {
            return;
        }
        Vec3 jetMouth = castData.jetMouthWorld();
        MagicManager.spawnParticles(
                level,
                CometAzurJetOptions.emitter(castData.yawDegrees(), castData.pitchDegrees()),
                jetMouth.x,
                jetMouth.y,
                jetMouth.z,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                true
        );
    }
}

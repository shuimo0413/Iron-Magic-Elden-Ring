package com.eldenring.spells.spell.fx;

import com.eldenring.spells.particle.carian.CarianFx;
import com.eldenring.spells.registry.ModParticles;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 罗蕾塔的大弓特效：蓄力时弓前汇聚卡利亚粒子，放箭瞬间弓前爆闪。只在服务端调用，经 MagicManager 同步。
 * 颜色与密度写死，不进 toml。
 */
public final class LorettaGreatbowFx {

    /**
     * 汇聚中心相对眼睛沿视线前移（方块）。对应拉弓时箭头所在位置。
     * 调大 → 粒子离脸更远、第一人称更不挡视线。
     */
    private static final double GATHER_CENTER_FORWARD_OFFSET_BLOCKS = 1.1;

    /** 汇聚中心相对眼睛下移（方块）。拉弓动作箭在下巴高度。 */
    private static final double GATHER_CENTER_DOWN_OFFSET_BLOCKS = 0.15;

    /** 外圈微粒的生成半径（方块）。粒子从这一圈往中心飞。 */
    private static final double GATHER_SPAWN_RADIUS_BLOCKS = 0.75;

    /**
     * 微粒向心速度（每 tick 走完生成半径的比例）。调大 → 收束更快、粒子更短命地挤到中心。
     */
    private static final double GATHER_INWARD_SPEED_FRACTION_PER_TICK = 0.12;

    /** 蓄力刚开始每 tick 汇聚微粒数。 */
    private static final int GATHER_MOTES_PER_TICK_AT_START = 1;

    /** 满弦时每 tick 汇聚微粒数。调大 → 满弦更亮，但吃粒子预算。 */
    private static final int GATHER_MOTES_PER_TICK_AT_FULL = 4;

    /** 满弦时箭头处闪星的每 tick 概率（0–1）。按蓄力进度线性增长。 */
    private static final float GATHER_CORE_GLINT_CHANCE_AT_FULL = 0.7f;

    /** 放箭时沿视线喷出的流光条数。 */
    private static final int RELEASE_STREAK_COUNT = 6;

    /** 放箭流光的前向速度（方块/tick）。 */
    private static final double RELEASE_STREAK_FORWARD_SPEED = 0.55;

    /** 放箭流光的横向散布（方块/tick）。 */
    private static final double RELEASE_STREAK_SPREAD_SPEED = 0.06;

    private LorettaGreatbowFx() {
    }

    /**
     * 蓄力 tick：外圈卡利亚微粒向箭头收束，进度越高越密，并在箭头处点闪星。
     *
     * @param chargeProgress 蓄力进度 [0,1]
     */
    public static void chargeGather(Level level, LivingEntity caster, float chargeProgress) {
        float clampedProgress = Mth.clamp(chargeProgress, 0.0f, 1.0f);
        Vec3 gatherCenter = gatherCenter(caster);
        int moteCount = Math.round(Mth.lerp(
                clampedProgress,
                GATHER_MOTES_PER_TICK_AT_START,
                GATHER_MOTES_PER_TICK_AT_FULL
        ));
        for (int moteIndex = 0; moteIndex < moteCount; moteIndex++) {
            Vec3 outwardOffset = Utils.getRandomVec3(1.0);
            if (outwardOffset.lengthSqr() < 1.0e-6) {
                continue;
            }
            outwardOffset = outwardOffset.normalize().scale(GATHER_SPAWN_RADIUS_BLOCKS);
            Vec3 inwardVelocity = outwardOffset.scale(-GATHER_INWARD_SPEED_FRACTION_PER_TICK);
            CarianFx.spawnOne(
                    level,
                    moteIndex % 2 == 0 ? ModParticles.CARIAN_MOTE.get() : ModParticles.CARIAN_GLOW.get(),
                    gatherCenter.add(outwardOffset),
                    inwardVelocity
            );
        }
        if (level.random.nextFloat() < GATHER_CORE_GLINT_CHANCE_AT_FULL * clampedProgress) {
            CarianFx.spawnOne(level, ModParticles.CARIAN_GLINT.get(), gatherCenter, Vec3.ZERO);
        }
    }

    /**
     * 放箭瞬间：箭头处卡利亚小爆 + 沿视线喷出几道流光。
     *
     * @param intensity 爆闪强度倍率，传给 {@link CarianFx#castBurst}
     */
    public static void releaseBurst(Level level, LivingEntity caster, float intensity) {
        Vec3 gatherCenter = gatherCenter(caster);
        CarianFx.castBurst(level, gatherCenter.x, gatherCenter.y, gatherCenter.z, intensity);
        Vec3 lookDirection = caster.getLookAngle().normalize();
        for (int streakIndex = 0; streakIndex < RELEASE_STREAK_COUNT; streakIndex++) {
            Vec3 spreadVelocity = Utils.getRandomVec3(RELEASE_STREAK_SPREAD_SPEED);
            CarianFx.spawnOne(
                    level,
                    ModParticles.CARIAN_STREAK.get(),
                    gatherCenter,
                    lookDirection.scale(RELEASE_STREAK_FORWARD_SPEED).add(spreadVelocity)
            );
        }
    }

    private static Vec3 gatherCenter(LivingEntity caster) {
        return caster.getEyePosition()
                .add(caster.getLookAngle().normalize().scale(GATHER_CENTER_FORWARD_OFFSET_BLOCKS))
                .subtract(0.0, GATHER_CENTER_DOWN_OFFSET_BLOCKS, 0.0);
    }
}

package com.eldenring.spells.spell.fx;

import com.eldenring.spells.particle.gravity.GravityFx;
import com.eldenring.spells.registry.ModParticles;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * 岩石球特效：重力紫粒子走 {@link GravityFx} 同一套贴图，另加原版深板岩碎屑表现「岩石」。
 * <p>
 * 蓄力内聚 / 飞行拖尾只在客户端刷（由实体 {@code trailParticles} 调用）；
 * 命中爆裂 / 取消碎裂只在服务端刷（经 {@link MagicManager} 同步到附近客户端）。
 */
public final class RockSlingFx {

    /**
     * 蓄力时粒子出生壳层半径（方块）。粒子从这层往岩石中心吸。调大 → 吸取范围更夸张。
     */
    private static final double CHARGE_GATHER_SHELL_RADIUS_BLOCKS = 0.9;

    /**
     * 蓄力内吸速度系数（每 tick 走完出生距离的比例）。调大 → 吸得更快、更急。
     */
    private static final double CHARGE_GATHER_INWARD_SPEED_FRACTION = 0.16;

    /** 生长阶段每 tick 刷紫色微粒的概率（满强度）。 */
    private static final float CHARGE_MOTE_CHANCE_WHILE_GROWING = 0.85f;

    /** 生长阶段每 tick 刷石屑被吸入的概率。调大 → 碎石聚拢感更强，但更脏。 */
    private static final float CHARGE_DEBRIS_CHANCE_WHILE_GROWING = 0.35f;

    /** 长满后每 tick 刷淡紫辉光的概率：岩石浮空时裹着一层微光。 */
    private static final float CHARGE_GLOW_CHANCE_WHEN_GROWN = 0.30f;

    /**
     * 飞行拖尾强度（相对 GravityFx 基准）。岩石球要「小拖尾」，明显低于重力球的 1.0。
     */
    private static final float TRAIL_PARTICLE_INTENSITY = 0.45f;

    /** 飞行时每 tick 掉落细碎石屑的概率。 */
    private static final float TRAIL_DEBRIS_CHANCE = 0.18f;

    /** 命中重力爆裂强度。三块会同时炸，单块比重力球（1.35）收敛。 */
    private static final float IMPACT_PARTICLE_INTENSITY = 0.75f;

    /** 命中时迸出的石屑数量。 */
    private static final int IMPACT_DEBRIS_COUNT = 14;

    /** 取消蓄力碎裂时的重力爆裂强度（乘当前岩石尺寸）。 */
    private static final float SHATTER_PARTICLE_INTENSITY = 0.40f;

    /** 取消碎裂时迸出的石屑数量（乘当前岩石尺寸）。 */
    private static final int SHATTER_DEBRIS_COUNT = 10;

    private static final BlockParticleOption ROCK_DEBRIS =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLED_DEEPSLATE.defaultBlockState());

    private RockSlingFx() {
    }

    /**
     * 蓄力内聚：生长中紫色微粒与石屑从四周吸向岩石；长满后只留一层淡紫微光。仅客户端。
     *
     * @param growthProgress 本块生长进度 0–1
     */
    public static void chargeGather(Level level, Vec3 rockCenter, float growthProgress) {
        if (!level.isClientSide) {
            return;
        }
        boolean stillGrowing = growthProgress < 1.0f;
        if (stillGrowing && growthProgress > 0.0f) {
            if (level.random.nextFloat() < CHARGE_MOTE_CHANCE_WHILE_GROWING) {
                spawnInwardParticle(level, rockCenter, level.random.nextBoolean()
                        ? ModParticles.GRAVITY_MOTE.get()
                        : ModParticles.GRAVITY_GLOW.get());
            }
            if (level.random.nextFloat() < CHARGE_DEBRIS_CHANCE_WHILE_GROWING) {
                spawnInwardParticle(level, rockCenter, ROCK_DEBRIS);
            }
            return;
        }
        if (!stillGrowing && level.random.nextFloat() < CHARGE_GLOW_CHANCE_WHEN_GROWN) {
            Vec3 glowOffset = Utils.getRandomVec3(0.35);
            level.addParticle(
                    ModParticles.GRAVITY_GLOW.get(),
                    rockCenter.x + glowOffset.x,
                    rockCenter.y + glowOffset.y,
                    rockCenter.z + glowOffset.z,
                    -glowOffset.x * 0.05,
                    0.01,
                    -glowOffset.z * 0.05
            );
        }
    }

    /**
     * 飞行小拖尾：低强度重力紫点缀 + 偶尔掉一粒石屑。仅客户端。
     */
    public static void trail(Level level, Vec3 rockCenter, Vec3 motion) {
        if (!level.isClientSide || motion.lengthSqr() < 1.0e-8) {
            return;
        }
        GravityFx.trailAccents(level, rockCenter.x, rockCenter.y, rockCenter.z, motion, TRAIL_PARTICLE_INTENSITY);
        if (level.random.nextFloat() < TRAIL_DEBRIS_CHANCE) {
            Vec3 debrisOffset = Utils.getRandomVec3(0.25);
            level.addParticle(
                    ROCK_DEBRIS,
                    rockCenter.x + debrisOffset.x,
                    rockCenter.y + debrisOffset.y,
                    rockCenter.z + debrisOffset.z,
                    -motion.x * 0.05,
                    -motion.y * 0.05,
                    -motion.z * 0.05
            );
        }
    }

    /**
     * 命中爆裂：重力紫蚀环 / 雾 / 电弧 + 深板岩碎屑。仅服务端。
     */
    public static void impact(Level level, Vec3 impactCenter) {
        if (level.isClientSide || level.getServer() == null) {
            return;
        }
        GravityFx.impact(level, impactCenter.x, impactCenter.y, impactCenter.z, IMPACT_PARTICLE_INTENSITY);
        MagicManager.spawnParticles(
                level, ROCK_DEBRIS,
                impactCenter.x, impactCenter.y, impactCenter.z,
                IMPACT_DEBRIS_COUNT, 0.25, 0.25, 0.25,
                0.15, false
        );
    }

    /**
     * 取消蓄力：岩石原地崩散，规模随当前尺寸缩放。仅服务端。
     *
     * @param rockScale 碎裂时岩石的尺寸倍率 0–1
     */
    public static void shatter(Level level, Vec3 rockCenter, float rockScale) {
        if (level.isClientSide || level.getServer() == null) {
            return;
        }
        float clampedScale = Mth.clamp(rockScale, 0.2f, 1.0f);
        GravityFx.impact(level, rockCenter.x, rockCenter.y, rockCenter.z, SHATTER_PARTICLE_INTENSITY * clampedScale);
        MagicManager.spawnParticles(
                level, ROCK_DEBRIS,
                rockCenter.x, rockCenter.y, rockCenter.z,
                Math.max(2, Math.round(SHATTER_DEBRIS_COUNT * clampedScale)), 0.2 * clampedScale, 0.2 * clampedScale, 0.2 * clampedScale,
                0.08, false
        );
    }

    private static void spawnInwardParticle(Level level, Vec3 rockCenter, ParticleOptions particle) {
        Vec3 spawnOffset = Utils.getRandomVec3(1.0);
        if (spawnOffset.lengthSqr() < 1.0e-6) {
            return;
        }
        spawnOffset = spawnOffset.normalize().scale(CHARGE_GATHER_SHELL_RADIUS_BLOCKS * (0.7 + 0.3 * level.random.nextDouble()));
        level.addParticle(
                particle,
                rockCenter.x + spawnOffset.x,
                rockCenter.y + spawnOffset.y,
                rockCenter.z + spawnOffset.z,
                -spawnOffset.x * CHARGE_GATHER_INWARD_SPEED_FRACTION,
                -spawnOffset.y * CHARGE_GATHER_INWARD_SPEED_FRACTION,
                -spawnOffset.z * CHARGE_GATHER_INWARD_SPEED_FRACTION
        );
    }
}

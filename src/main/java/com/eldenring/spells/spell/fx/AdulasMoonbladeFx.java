package com.eldenring.spells.spell.fx;

import com.eldenring.spells.entity.AdulasMoonbladeWaveProjectile;
import com.eldenring.spells.particle.frost.FrostFx;
import com.eldenring.spells.registry.ModParticles;
import com.eldenring.spells.registry.ModSounds;
import com.eldenring.spells.spell.AdulasMoonbladeSpell;
import com.eldenring.spells.spell.combat.GlintstoneArcCombat;
import com.eldenring.spells.spell.combat.GlintstoneArcCombat.ArcBasis;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 亚杜拉的月光剑特效：沿刃冰霜星屑（替代卡利亚大剑的 {@code CARIAN_*} 星星）、剑气冰暴风拖尾、
 * 剑气穿透 / 碎裂，以及每刀命中帧的斩击音。
 * <p>
 * 只用已注册的 {@code FROST_*} 粒子；密度全部写死，不进 toml。
 * 沿刃粒子必须在客户端调用；穿透 / 碎裂由服务端 {@link MagicManager} 同步。
 */
public final class AdulasMoonbladeFx {

    /** 沿刃星屑默认采样点数（每点一颗 {@code FROST_SPARKLE}）。调大 → 刃上更密。 */
    private static final int BLADE_SAMPLE_COUNT = 6;

    /** 本 tick 刃尖位移超过此值（方块）才在刃中 / 近尖加冰晶与雪花。 */
    private static final double FLASH_MIN_TIP_TRAVEL_BLOCKS = 0.22;

    /** 星屑残留速度倍率。接近 0 时粒子停在挥过的位置慢慢闪灭。 */
    private static final double STAR_LINGER_VELOCITY_SCALE = 0.04;

    /** 沿刃每 tick 刷一团寒雾的概率。调大 → 挥剑时雾更浓。 */
    private static final float BLADE_MIST_CHANCE = 0.45f;

    /** 剑气每 tick 沿月牙刃口采样的点数（每点必刷一颗）。调大 → 刃口冰屑更密。 */
    private static final int WAVE_EDGE_SAMPLE_COUNT = 18;

    /** 剑气每 tick 在本 tick 扫过的区域里撒下的暴风冰屑数。调大 → 身后的冰风更浓。 */
    private static final int WAVE_GUST_PARTICLES_PER_TICK = 12;

    /** 暴风冰屑相对刃面沿局部 up 的上下散布（方块）。调大 → 冰风带更厚。 */
    private static final double WAVE_GUST_VERTICAL_SPREAD_BLOCKS = 0.55;

    /** 刃口冰屑随剑气前冲的速度区间（方块/tick，补偿阻尼后的实际初速）。 */
    private static final double WAVE_EDGE_FORWARD_SPEED_MIN = 0.25;
    private static final double WAVE_EDGE_FORWARD_SPEED_MAX = 0.55;

    /** 刃口冰屑沿月牙向外甩的速度（方块/tick）。调大 → 两尖更像被风撕开。 */
    private static final double WAVE_EDGE_OUTWARD_SPEED = 0.06;

    /** 暴风冰屑被卷着往前吹的速度区间（方块/tick）。 */
    private static final double WAVE_GUST_FORWARD_SPEED_MIN = 0.08;
    private static final double WAVE_GUST_FORWARD_SPEED_MAX = 0.35;

    /** 暴风冰屑横向 / 竖向乱流速度幅度（方块/tick）。调大 → 更狂乱。 */
    private static final double WAVE_GUST_TURBULENCE_SPEED = 0.07;

    /**
     * 四种冰屑的速度阻尼补偿：{@code FrostParticle.Kind} 会把初速乘以各自的 velocityDamp
     * （星 0.30 / 冰片 0.90 / 雪花 0.40 / 星尘 0.20），这里预先除回去，让四种粒子实际飞得一样快。
     */
    private static final double FROST_STAR_VELOCITY_COMPENSATION = 1.0 / 0.30;
    private static final double FROST_SHARD_VELOCITY_COMPENSATION = 1.0 / 0.90;
    private static final double FROST_SNOWFLAKE_VELOCITY_COMPENSATION = 1.0 / 0.40;
    private static final double FROST_STARDUST_VELOCITY_COMPENSATION = 1.0 / 0.20;

    /** 粒子相对刃面沿局部 up 的抬升（方块），避免埋进几何矮墙。 */
    private static final double WAVE_TRAIL_LIFT_ALONG_UP_BLOCKS = 0.30;

    /** 剑气穿透时冰片数量。 */
    private static final int WAVE_HIT_SHARD_COUNT = 8;

    /** 剑气穿透时冰火花数量。 */
    private static final int WAVE_HIT_SPARK_COUNT = 6;

    /** 剑气穿透时冰雾数量。 */
    private static final int WAVE_HIT_MIST_COUNT = 5;

    /** 剑气撞墙 / 飞尽时寒爆强度（相对 {@link FrostFx#impact} 大魔砾基准）。 */
    private static final float WAVE_SHATTER_INTENSITY = 0.70f;

    /**
     * 剑气从出手半宽张到最大半宽所需飞行距离（方块）。调大 → 张开更慢、近身更窄。
     */
    private static final double WAVE_FULL_SPREAD_DISTANCE_BLOCKS = 10.0;

    private AdulasMoonbladeFx() {
    }

    /** 每刀命中帧播一次辉石蓄力起手音，与卡利亚大剑的斩击音一致。仅服务端。 */
    public static void playSlashSound(Level level, LivingEntity caster) {
        if (level.isClientSide) {
            return;
        }
        ModSounds.playCastStart(level, caster);
    }

    /** 本 tick 沿当前剑刃刷一层冰霜星屑（默认采样数）。 */
    public static void spawnAlongSlash(
            Level level,
            Vec3 hiltWorld,
            Vec3 tipWorld,
            Vec3 tipTravelSinceLastTick
    ) {
        spawnAlongSlash(level, hiltWorld, tipWorld, tipTravelSinceLastTick, BLADE_SAMPLE_COUNT);
    }

    /**
     * 沿刃冰霜星屑：每点一颗 {@code FROST_SPARKLE}，近尖一颗 {@code FROST_STAR}，
     * 挥得快时刃中加 {@code FROST_CRYSTAL}、近尖加 {@code FROST_SNOWFLAKE}，再稀疏飘一团 {@code FROST_MIST}。
     *
     * @param bladeSampleCount 沿刃采样点数
     */
    public static void spawnAlongSlash(
            Level level,
            Vec3 hiltWorld,
            Vec3 tipWorld,
            Vec3 tipTravelSinceLastTick,
            int bladeSampleCount
    ) {
        if (!level.isClientSide) {
            return;
        }
        Vec3 bladeOffset = tipWorld.subtract(hiltWorld);
        if (bladeOffset.lengthSqr() < 1.0e-6) {
            return;
        }
        double tipTravelBlocks = tipTravelSinceLastTick.length();
        boolean fastSwing = tipTravelBlocks >= FLASH_MIN_TIP_TRAVEL_BLOCKS;
        Vec3 swingDirection = tipTravelBlocks > 1.0e-6
                ? tipTravelSinceLastTick.normalize()
                : Vec3.ZERO;
        Vec3 starVelocity = swingDirection.scale(STAR_LINGER_VELOCITY_SCALE);

        int sampleCount = Math.max(1, bladeSampleCount);
        for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
            float alongBlade = (sampleIndex + 0.35f) / sampleCount;
            Vec3 samplePosition = hiltWorld.add(bladeOffset.scale(alongBlade));
            level.addParticle(
                    ModParticles.FROST_SPARKLE.get(),
                    samplePosition.x, samplePosition.y, samplePosition.z,
                    starVelocity.x, starVelocity.y, starVelocity.z
            );
        }
        Vec3 tipStarPosition = hiltWorld.add(bladeOffset.scale((sampleCount - 0.65f) / sampleCount));
        level.addParticle(
                ModParticles.FROST_STAR.get(),
                tipStarPosition.x, tipStarPosition.y, tipStarPosition.z,
                starVelocity.x * 0.6, starVelocity.y * 0.6, starVelocity.z * 0.6
        );
        if (level.random.nextFloat() < BLADE_MIST_CHANCE) {
            Vec3 mistPosition = hiltWorld.add(bladeOffset.scale(0.3 + level.random.nextDouble() * 0.7));
            level.addParticle(
                    ModParticles.FROST_MIST.get(),
                    mistPosition.x, mistPosition.y, mistPosition.z,
                    starVelocity.x * 0.5, 0.004, starVelocity.z * 0.5
            );
        }
        if (!fastSwing) {
            return;
        }
        Vec3 midBlade = hiltWorld.add(bladeOffset.scale(0.55));
        Vec3 nearTip = hiltWorld.add(bladeOffset.scale(0.88));
        level.addParticle(
                ModParticles.FROST_CRYSTAL.get(),
                midBlade.x, midBlade.y, midBlade.z,
                starVelocity.x, starVelocity.y, starVelocity.z
        );
        level.addParticle(
                ModParticles.FROST_SNOWFLAKE.get(),
                nearTip.x, nearTip.y, nearTip.z,
                starVelocity.x * 0.5, starVelocity.y * 0.5, starVelocity.z * 0.5
        );
    }

    /**
     * 客户端剑气拖尾：冰暴风。
     * <ul>
     *   <li>刃口：沿整条月牙每点刷一颗冰屑，随剑气前冲并向两尖外甩，像刃口卷起的冰风</li>
     *   <li>身后：在本 tick 扫过的整片宽度里撒一把冰屑，带乱流地被往前吹，形成一条翻卷的冰风带</li>
     * </ul>
     * 粒子只用 {@code FROST_SHARD} / {@code FROST_SNOWFLAKE} / {@code FROST_STAR} / {@code FROST_STARDUST}。
     */
    public static void trailAlongWave(AdulasMoonbladeWaveProjectile waveProjectile, Level level) {
        if (!level.isClientSide) {
            return;
        }
        Vec3 flightDirection = waveProjectile.resolveFlightDirection();
        if (flightDirection.lengthSqr() < 1.0e-8) {
            return;
        }
        ArcBasis arcBasis = ArcBasis.fromFlightDirection(flightDirection);
        float halfWidthBlocks = waveProjectile.currentHalfWidthBlocks(0.0f);
        float outerRadiusBlocks = GlintstoneArcCombat.crescentOuterRadius(halfWidthBlocks);
        float halfAngleRadians = (float) Math.toRadians(GlintstoneArcCombat.CRESCENT_HALF_ANGLE_DEGREES);
        Vec3 origin = waveProjectile.position();
        Vec3 liftOffset = arcBasis.up().scale(WAVE_TRAIL_LIFT_ALONG_UP_BLOCKS);

        for (int sampleIndex = 0; sampleIndex < WAVE_EDGE_SAMPLE_COUNT; sampleIndex++) {
            float sampleFraction = (sampleIndex + level.random.nextFloat()) / WAVE_EDGE_SAMPLE_COUNT;
            float angleRadians = Mth.lerp(sampleFraction, -halfAngleRadians, halfAngleRadians);
            double alongForward = Math.cos(angleRadians) * outerRadiusBlocks - outerRadiusBlocks;
            double alongRight = Math.sin(angleRadians) * outerRadiusBlocks;
            Vec3 samplePosition = origin
                    .add(arcBasis.forward().scale(alongForward))
                    .add(arcBasis.right().scale(alongRight))
                    .add(liftOffset);
            double forwardSpeed = Mth.lerp(level.random.nextDouble(), WAVE_EDGE_FORWARD_SPEED_MIN, WAVE_EDGE_FORWARD_SPEED_MAX);
            Vec3 edgeVelocity = arcBasis.forward().scale(forwardSpeed)
                    .add(arcBasis.right().scale(Math.sin(angleRadians) * WAVE_EDGE_OUTWARD_SPEED))
                    .add(arcBasis.up().scale(randomSigned(level) * WAVE_GUST_TURBULENCE_SPEED * 0.5));
            spawnGustParticle(level, samplePosition, edgeVelocity);
        }

        double tickTravelBlocks = waveProjectile.getDeltaMovement().length();
        for (int gustIndex = 0; gustIndex < WAVE_GUST_PARTICLES_PER_TICK; gustIndex++) {
            double sideOffset = randomSigned(level) * halfWidthBlocks;
            double upOffset = randomSigned(level) * WAVE_GUST_VERTICAL_SPREAD_BLOCKS;
            double backOffset = level.random.nextDouble() * tickTravelBlocks;
            Vec3 gustPosition = origin
                    .subtract(arcBasis.forward().scale(backOffset))
                    .add(arcBasis.right().scale(sideOffset))
                    .add(arcBasis.up().scale(upOffset + WAVE_TRAIL_LIFT_ALONG_UP_BLOCKS));
            double forwardSpeed = Mth.lerp(level.random.nextDouble(), WAVE_GUST_FORWARD_SPEED_MIN, WAVE_GUST_FORWARD_SPEED_MAX);
            Vec3 gustVelocity = arcBasis.forward().scale(forwardSpeed)
                    .add(arcBasis.right().scale(randomSigned(level) * WAVE_GUST_TURBULENCE_SPEED))
                    .add(arcBasis.up().scale(randomSigned(level) * WAVE_GUST_TURBULENCE_SPEED * 0.6));
            spawnGustParticle(level, gustPosition, gustVelocity);
        }
    }

    /**
     * 随机挑一种冰屑并按它的阻尼补偿初速。权重：星尘 35% / 雪花 25% / 冰片 20% / 冰星 20%，
     * 细碎的星尘和雪花打底，冰片和冰星点缀出闪光。
     *
     * @param desiredVelocity 希望粒子实际拿到的初速（方块/tick）
     */
    private static void spawnGustParticle(Level level, Vec3 position, Vec3 desiredVelocity) {
        float particleRoll = level.random.nextFloat();
        SimpleParticleType particleType;
        double velocityCompensation;
        if (particleRoll < 0.35f) {
            particleType = ModParticles.FROST_STARDUST.get();
            velocityCompensation = FROST_STARDUST_VELOCITY_COMPENSATION;
        } else if (particleRoll < 0.60f) {
            particleType = ModParticles.FROST_SNOWFLAKE.get();
            velocityCompensation = FROST_SNOWFLAKE_VELOCITY_COMPENSATION;
        } else if (particleRoll < 0.80f) {
            particleType = ModParticles.FROST_SHARD.get();
            velocityCompensation = FROST_SHARD_VELOCITY_COMPENSATION;
        } else {
            particleType = ModParticles.FROST_STAR.get();
            velocityCompensation = FROST_STAR_VELOCITY_COMPENSATION;
        }
        Vec3 launchVelocity = desiredVelocity.scale(velocityCompensation);
        level.addParticle(
                particleType,
                position.x, position.y, position.z,
                launchVelocity.x, launchVelocity.y, launchVelocity.z
        );
    }

    /** [-1, 1) 均匀随机数。 */
    private static double randomSigned(Level level) {
        return level.random.nextDouble() * 2.0 - 1.0;
    }

    /** 剑气穿过某个敌人时在命中点刷一小撮冰片 / 火花 / 寒雾。仅服务端调用。 */
    public static void waveHitSpark(Level level, double impactX, double impactY, double impactZ) {
        if (level.isClientSide) {
            return;
        }
        MagicManager.spawnParticles(
                level, ModParticles.FROST_SHARD.get(), impactX, impactY, impactZ,
                WAVE_HIT_SHARD_COUNT, 0.12, 0.12, 0.12, 0.24, true
        );
        MagicManager.spawnParticles(
                level, ModParticles.FROST_SPARK.get(), impactX, impactY, impactZ,
                WAVE_HIT_SPARK_COUNT, 0.10, 0.10, 0.10, 0.20, true
        );
        MagicManager.spawnParticles(
                level, ModParticles.FROST_MIST.get(), impactX, impactY, impactZ,
                WAVE_HIT_MIST_COUNT, 0.25, 0.20, 0.25, 0.03, false
        );
    }

    /** 剑气撞墙或飞尽射程：低强度寒爆。仅服务端调用。 */
    public static void waveShatter(Level level, double impactX, double impactY, double impactZ) {
        if (level.isClientSide) {
            return;
        }
        FrostFx.impact(level, impactX, impactY, impactZ, WAVE_SHATTER_INTENSITY);
    }

    /**
     * 按飞行距离把剑气半宽从出手值插到最大值。ease-out：前面张得快，后面慢慢铺满。
     *
     * @param traveledBlocks 已飞行直线距离（方块）
     */
    public static float waveHalfWidthAtDistance(double traveledBlocks) {
        float travelFraction = Mth.clamp((float) (traveledBlocks / WAVE_FULL_SPREAD_DISTANCE_BLOCKS), 0.0f, 1.0f);
        float spreadEase = 1.0f - (1.0f - travelFraction) * (1.0f - travelFraction);
        return Mth.lerp(
                spreadEase,
                AdulasMoonbladeSpell.WAVE_START_HALF_WIDTH_BLOCKS,
                AdulasMoonbladeSpell.WAVE_MAX_HALF_WIDTH_BLOCKS
        );
    }
}

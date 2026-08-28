package com.eldenring.spells.spell.fx;

import com.eldenring.spells.entity.GlintstoneArcProjectile;
import com.eldenring.spells.registry.ModParticles;
import com.eldenring.spells.spell.GlintstoneArcSpell;
import com.eldenring.spells.spell.combat.GlintstoneArcCombat;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 辉石弯弧特效：沿对称月牙点缀青色雾气 / 辉光，以及穿透火花、消散碎裂。
 * <p>
 * 密度写死，不进 toml。不要走彗星拖尾，否则俯视会看成一串竖条。
 */
public final class GlintstoneArcFx {

    /**
     * 客户端每 tick 沿水波采样的点数。调大 → 弧上雾更密。
     */
    private static final int TRAIL_SAMPLE_COUNT = 6;

    /**
     * 穿透命中时飞出的碎晶数量。只要一小撮，证明「穿过去了」而不是整条刃炸开。
     */
    private static final int PIERCE_SHARD_COUNT = 6;

    /**
     * 穿透火花数量。
     */
    private static final int PIERCE_SPARK_COUNT = 4;

    /**
     * 消散碎晶数量。比穿透火花密一截，仍远小于彗星爆裂。
     */
    private static final int DISCARD_SHARD_COUNT = 14;

    /**
     * 消散火花数量。
     */
    private static final int DISCARD_SPARK_COUNT = 8;

    /**
     * 消散闪光数量。
     */
    private static final int DISCARD_FLARE_COUNT = 2;

    private GlintstoneArcFx() {
    }

    /**
     * 客户端沿对称月牙稀疏点缀雾气 / 辉光。不要走彗星拖尾，否则会看成一串竖条。
     */
    public static void trailAlongBlade(GlintstoneArcProjectile arcProjectile, Level level) {
        if (!level.isClientSide) {
            return;
        }
        Vec3 flightDirection = arcProjectile.resolveFlightDirection();
        if (flightDirection.lengthSqr() < 1.0e-8) {
            return;
        }
        Vec3 horizontalForward = GlintstoneArcCombat.horizontalForward(flightDirection);
        Vec3 horizontalRight = GlintstoneArcCombat.horizontalRight(horizontalForward);
        float halfWidthBlocks = arcProjectile.currentHalfWidthBlocks(0.0f);
        float outerRadiusBlocks = GlintstoneArcCombat.crescentOuterRadius(halfWidthBlocks);
        float halfAngleRadians = (float) Math.toRadians(GlintstoneArcCombat.CRESCENT_HALF_ANGLE_DEGREES);
        Vec3 origin = arcProjectile.position();
        for (int sampleIndex = 0; sampleIndex < TRAIL_SAMPLE_COUNT; sampleIndex++) {
            if (level.random.nextFloat() > 0.45f) {
                continue;
            }
            // 从中轴向两侧对称取样，不要随机偏一侧。
            float t = TRAIL_SAMPLE_COUNT == 1
                    ? 0.0f
                    : sampleIndex / (float) (TRAIL_SAMPLE_COUNT - 1);
            float angleRadians = Mth.lerp(t, -halfAngleRadians, halfAngleRadians);
            double alongForward = Math.cos(angleRadians) * outerRadiusBlocks - outerRadiusBlocks;
            double alongRight = Math.sin(angleRadians) * outerRadiusBlocks;
            Vec3 samplePosition = origin
                    .add(horizontalForward.scale(alongForward))
                    .add(horizontalRight.scale(alongRight))
                    .add(0.0, 0.12, 0.0);
            if (level.random.nextBoolean()) {
                level.addParticle(
                        ModParticles.GLINTSTONE_MIST.get(),
                        samplePosition.x,
                        samplePosition.y,
                        samplePosition.z,
                        0.0,
                        0.01,
                        0.0
                );
            } else {
                level.addParticle(
                        ModParticles.GLINTSTONE_GLOW.get(),
                        samplePosition.x,
                        samplePosition.y,
                        samplePosition.z,
                        0.0,
                        0.008,
                        0.0
                );
            }
        }
    }

    /**
     * 穿透某个敌人时在命中点刷一小撮碎晶。仅服务端调用，由 MagicManager 同步。
     */
    public static void pierceSpark(Level level, double impactX, double impactY, double impactZ) {
        if (level.isClientSide) {
            return;
        }
        MagicManager.spawnParticles(
                level,
                ModParticles.GLINTSTONE_SHARD.get(),
                impactX,
                impactY,
                impactZ,
                PIERCE_SHARD_COUNT,
                0.10,
                0.10,
                0.10,
                0.22,
                true
        );
        MagicManager.spawnParticles(
                level,
                ModParticles.GLINTSTONE_SPARK.get(),
                impactX,
                impactY,
                impactZ,
                PIERCE_SPARK_COUNT,
                0.08,
                0.08,
                0.08,
                0.18,
                true
        );
    }

    /**
     * 撞墙或飞尽射程：碎裂 + 淡光晕。数量克制，避免宽刃消散时糊屏。
     */
    public static void shatter(Level level, double impactX, double impactY, double impactZ) {
        if (level.isClientSide) {
            return;
        }
        MagicManager.spawnParticles(
                level,
                ModParticles.GLINTSTONE_SHARD.get(),
                impactX,
                impactY,
                impactZ,
                DISCARD_SHARD_COUNT,
                0.18,
                0.12,
                0.18,
                0.36,
                true
        );
        MagicManager.spawnParticles(
                level,
                ModParticles.GLINTSTONE_SPARK.get(),
                impactX,
                impactY,
                impactZ,
                DISCARD_SPARK_COUNT,
                0.14,
                0.10,
                0.14,
                0.24,
                true
        );
        MagicManager.spawnParticles(
                level,
                ModParticles.GLINTSTONE_FLARE.get(),
                impactX,
                impactY,
                impactZ,
                DISCARD_FLARE_COUNT,
                0.04,
                0.04,
                0.04,
                0.02,
                false
        );
        MagicManager.spawnParticles(
                level,
                ModParticles.GLINTSTONE_GLOW.get(),
                impactX,
                impactY,
                impactZ,
                3,
                0.12,
                0.08,
                0.12,
                0.06,
                false
        );
    }

    /**
     * 按飞行距离把半宽从出手值插到最大值。ease-out：前面张得快，后面慢慢铺满。
     *
     * @param traveledBlocks 已飞行直线距离（方块）
     */
    public static float halfWidthAtDistance(double traveledBlocks) {
        float maxRange = (float) Math.max(0.5, GlintstoneArcSpell.PROJECTILE_MAX_RANGE_BLOCKS);
        float travelFraction = Mth.clamp((float) traveledBlocks / maxRange, 0.0f, 1.0f);
        float spreadEase = 1.0f - (1.0f - travelFraction) * (1.0f - travelFraction);
        return Mth.lerp(
                spreadEase,
                GlintstoneArcSpell.ARC_START_HALF_WIDTH_BLOCKS,
                GlintstoneArcSpell.ARC_MAX_HALF_WIDTH_BLOCKS
        );
    }
}

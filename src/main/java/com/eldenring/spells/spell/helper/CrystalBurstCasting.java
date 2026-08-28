package com.eldenring.spells.spell.helper;

import com.eldenring.spells.entity.CrystalBurstShardProjectile;
import com.eldenring.spells.spell.CrystalBurstSpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 结晶散射出手辅助：在视线锥里一次刷出整捧不追踪碎片。
 * <p>
 * {@code CrystalBurstSpell} 只保留铁魔法生命周期回调；清障 / 锥方向不进 Spell 本体。
 * 方向用黄金角均匀铺在锥面上，避免随机采样叠成一条线。
 */
public final class CrystalBurstCasting {

    /**
     * 生成点在垂直于视线的平面上的最大偏移（方块）。
     * 外圈碎片会偏到这个半径；调大 → 出口更散；调小 → 更像从杖尖同一点喷出。
     */
    private static final double SPAWN_LOOK_PLANE_JITTER_BLOCKS = 0.22;

    /**
     * 黄金角（弧度）。Vogel 圆盘采样用它把碎片均匀铺开，不会挤在中心轴上。
     */
    private static final double GOLDEN_ANGLE_RADIANS = Math.PI * (3.0 - Math.sqrt(5.0));

    private CrystalBurstCasting() {
    }

    /**
     * 一次齐射：按 {@link CrystalBurstSpell#PROJECTILE_COUNT} 在锥内均匀刷碎片。
     * 出手闪光只给第一发，避免十来片同时闪瞎屏幕。
     */
    public static void spawnScatterVolley(Level level, LivingEntity caster, float damageAmount) {
        Vec3 lookDirection = caster.getLookAngle();
        int shardCount = Math.max(1, CrystalBurstSpell.PROJECTILE_COUNT);
        for (int shardIndex = 0; shardIndex < shardCount; shardIndex++) {
            ScatterSample scatterSample = sampleEvenCone(
                    lookDirection,
                    CrystalBurstSpell.SCATTER_HALF_ANGLE_DEGREES,
                    shardIndex,
                    shardCount
            );
            GlintstoneCastHelper.spawnAlongLook(
                    level,
                    caster,
                    CrystalBurstShardProjectile::new,
                    CrystalBurstSpell.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                    CrystalBurstSpell.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS,
                    CrystalBurstSpell.CAST_BURST_PARTICLE_INTENSITY,
                    damageAmount,
                    scatterSample.flightDirection(),
                    scatterSample.spawnLookPlaneOffset(),
                    shardIndex == 0
            );
        }
    }

    /**
     * 在视线锥内做 Vogel 圆盘采样：半径用 {@code sqrt} 均匀铺开面积，方位走黄金角。
     * 生成点偏移与飞行方向共用同一方位，扇面从杖尖附近就打开，不会整捧叠在一条路径上。
     *
     * @param halfAngleDegrees 锥半角（度）。调大 → 扇面更开
     * @param shardIndex       当前碎片序号（从 0 起）
     * @param shardCount       这次齐射总片数
     */
    private static ScatterSample sampleEvenCone(
            Vec3 lookDirection,
            float halfAngleDegrees,
            int shardIndex,
            int shardCount
    ) {
        Vec3 forward = lookDirection.lengthSqr() > 1.0e-8
                ? lookDirection.normalize()
                : new Vec3(0.0, 0.0, 1.0);
        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 right = forward.cross(worldUp);
        if (right.lengthSqr() < 1.0e-8) {
            right = new Vec3(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }
        Vec3 planeUp = right.cross(forward).normalize();

        double unitRadius = Math.sqrt((shardIndex + 0.5) / (double) Math.max(1, shardCount));
        double coneRadius = Math.tan(Math.toRadians(halfAngleDegrees)) * unitRadius;
        double azimuthRadians = shardIndex * GOLDEN_ANGLE_RADIANS;
        double cosineAzimuth = Math.cos(azimuthRadians);
        double sineAzimuth = Math.sin(azimuthRadians);

        Vec3 flightDirection = forward
                .add(right.scale(cosineAzimuth * coneRadius))
                .add(planeUp.scale(sineAzimuth * coneRadius))
                .normalize();
        double spawnOffsetBlocks = SPAWN_LOOK_PLANE_JITTER_BLOCKS * unitRadius;
        Vec3 spawnLookPlaneOffset = right.scale(cosineAzimuth * spawnOffsetBlocks)
                .add(planeUp.scale(sineAzimuth * spawnOffsetBlocks));
        return new ScatterSample(flightDirection, spawnLookPlaneOffset);
    }

    /**
     * 一发碎片的飞行方向，以及生成点在视线平面上的偏移。
     */
    private record ScatterSample(Vec3 flightDirection, Vec3 spawnLookPlaneOffset) {
    }
}

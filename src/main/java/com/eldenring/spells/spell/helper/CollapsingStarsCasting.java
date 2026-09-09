package com.eldenring.spells.spell.helper;

import com.eldenring.spells.entity.GravityBallProjectile;
import com.eldenring.spells.spell.CollapsingStarsSpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 碎星出手辅助：在视线锥里一次刷出整捧不追踪的重力球。
 * <p>
 * {@code CollapsingStarsSpell} 只保留铁魔法生命周期回调；清障 / 锥方向不进 Spell 本体。
 * <strong>第 0 发永远沿视线正前方直射</strong>；其余发用黄金角均匀铺在锥面上，
 * 避免随机采样叠成一条线。弹道实体复用 {@link GravityBallProjectile}。
 */
public final class CollapsingStarsCasting {

    /**
     * 生成点在垂直于视线的平面上的最大偏移（方块）。
     * 仅用于散射发；正前方那发偏移为 0。调大 → 出口更散。
     */
    private static final double SPAWN_LOOK_PLANE_JITTER_BLOCKS = 0.28;

    /**
     * 黄金角（弧度）。Vogel 圆盘采样用它把散射球均匀铺开，不会挤在中心轴上。
     */
    private static final double GOLDEN_ANGLE_RADIANS = Math.PI * (3.0 - Math.sqrt(5.0));

    private CollapsingStarsCasting() {
    }

    /**
     * 一次齐射：第 0 发沿视线直射，其余在锥内均匀散射。
     *
     * @param pullDistanceBlocks 本级最大拉取格数，写入每一发弹道
     */
    public static void spawnScatterVolley(
            Level level,
            LivingEntity caster,
            double pullDistanceBlocks
    ) {
        Vec3 lookDirection = caster.getLookAngle();
        int projectileCount = Math.max(1, CollapsingStarsSpell.PROJECTILE_COUNT);
        for (int projectileIndex = 0; projectileIndex < projectileCount; projectileIndex++) {
            ScatterSample scatterSample = sampleConeWithForwardShot(
                    lookDirection,
                    CollapsingStarsSpell.SCATTER_HALF_ANGLE_DEGREES,
                    projectileIndex,
                    projectileCount
            );
            GravityCastHelper.spawnAlongLook(
                    level,
                    caster,
                    GravityBallProjectile::new,
                    CollapsingStarsSpell.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                    0.0f,
                    pullDistanceBlocks,
                    scatterSample.flightDirection(),
                    scatterSample.spawnLookPlaneOffset(),
                    CollapsingStarsSpell.PROJECTILE_FLIGHT_SPEED,
                    CollapsingStarsSpell.PROJECTILE_MAX_RANGE_BLOCKS,
                    CollapsingStarsSpell.HIT_RADIUS_BLOCKS,
                    CollapsingStarsSpell.SUCTION_STAND_OFF_BLOCKS
            );
        }
    }

    /**
     * 第 0 发：飞行方向 = 视线，生成点无平面偏移（正前方直射）。
     * 其余发：在锥内做 Vogel 圆盘采样；序号从 1 起重新映射到圆盘，避免中心再叠一发。
     *
     * @param halfAngleDegrees 锥半角（度）。调大 → 扇面更开
     * @param projectileIndex  当前球序号（从 0 起；0 = 正前方）
     * @param projectileCount  这次齐射总发数
     */
    private static ScatterSample sampleConeWithForwardShot(
            Vec3 lookDirection,
            float halfAngleDegrees,
            int projectileIndex,
            int projectileCount
    ) {
        Vec3 forward = lookDirection.lengthSqr() > 1.0e-8
                ? lookDirection.normalize()
                : new Vec3(0.0, 0.0, 1.0);

        if (projectileIndex == 0) {
            return new ScatterSample(forward, Vec3.ZERO);
        }

        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 right = forward.cross(worldUp);
        if (right.lengthSqr() < 1.0e-8) {
            right = new Vec3(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }
        Vec3 planeUp = right.cross(forward).normalize();

        // 散射发：把 index 1..N-1 映射到圆盘 0..N-2，半径从内圈铺到外圈。
        int scatterSlotIndex = projectileIndex - 1;
        int scatterSlotCount = Math.max(1, projectileCount - 1);
        double unitRadius = Math.sqrt((scatterSlotIndex + 0.5) / (double) scatterSlotCount);
        double coneRadius = Math.tan(Math.toRadians(halfAngleDegrees)) * unitRadius;
        double azimuthRadians = scatterSlotIndex * GOLDEN_ANGLE_RADIANS;
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
     * 一发重力球的飞行方向，以及生成点在视线平面上的偏移。
     */
    private record ScatterSample(Vec3 flightDirection, Vec3 spawnLookPlaneOffset) {
    }
}

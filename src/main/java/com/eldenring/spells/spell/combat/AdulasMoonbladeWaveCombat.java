package com.eldenring.spells.spell.combat;

import com.eldenring.spells.entity.AdulasMoonbladeWaveProjectile;
import com.eldenring.spells.spell.combat.GlintstoneArcCombat.ArcBasis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 月光剑剑气命中：随飞行方向倾斜的月牙定向盒（半宽随距离变大），判定照辉石弯弧。
 * <p>
 * 局部坐标基 {@link ArcBasis}、月牙半角 / 外半径直接复用 {@link GlintstoneArcCombat}；
 * 这里只保留针对剑气实体类型的扫掠收集。厚度 / 高度写死，不进 toml。
 */
public final class AdulasMoonbladeWaveCombat {

    /** 剑气相对飞行平面的垂直半高（方块）。调大 → 略偏离刃面也能刮到。 */
    public static final float WAVE_VERTICAL_HALF_HEIGHT_BLOCKS = 0.6f;

    /** 剑气沿飞行方向的半厚（方块），再叠加本 tick 位移的一半当扫掠。 */
    public static final float WAVE_FORWARD_HALF_THICKNESS_BLOCKS = 0.45f;

    /** 目标碰撞箱额外外扩（方块），补偿用中心点测定向盒对大碰撞箱的低估。 */
    public static final float HIT_INFLATION_BLOCKS = 0.28f;

    private AdulasMoonbladeWaveCombat() {
    }

    /** 收集本 tick 扫过的实体，按距路径起点从近到远排序。同一实体只留最近的一次。 */
    public static List<EntityHitResult> collectEntityHits(
            AdulasMoonbladeWaveProjectile waveProjectile,
            Level level,
            Vec3 pathStart,
            Vec3 pathEnd,
            float halfWidthBlocks
    ) {
        ArcBasis arcBasis = ArcBasis.fromFlightDirection(waveProjectile.resolveFlightDirection());
        Vec3 pathMidpoint = pathStart.add(pathEnd).scale(0.5);
        double movementLength = pathStart.distanceTo(pathEnd);
        double forwardHalfThickness = WAVE_FORWARD_HALF_THICKNESS_BLOCKS + movementLength * 0.5;

        double searchInflation = Math.max(halfWidthBlocks, WAVE_VERTICAL_HALF_HEIGHT_BLOCKS) + 1.25;
        AABB searchBox = new AABB(pathStart, pathEnd).inflate(searchInflation);

        Map<Integer, EntityHitResult> nearestHitsByEntityId = new HashMap<>();
        for (Entity target : level.getEntities(waveProjectile, searchBox, waveProjectile::isValidWaveTarget)) {
            if (!isInsideWaveVolume(target, pathMidpoint, arcBasis, halfWidthBlocks, forwardHalfThickness)) {
                continue;
            }
            Vec3 hitLocation = target.getBoundingBox().getCenter();
            int entityId = target.getId();
            EntityHitResult existingHit = nearestHitsByEntityId.get(entityId);
            if (existingHit == null
                    || hitLocation.distanceToSqr(pathStart) < existingHit.getLocation().distanceToSqr(pathStart)) {
                nearestHitsByEntityId.put(entityId, new EntityHitResult(target, hitLocation));
            }
        }

        List<EntityHitResult> orderedHits = new ArrayList<>(nearestHitsByEntityId.values());
        orderedHits.sort(Comparator.comparingDouble(hit -> hit.getLocation().distanceToSqr(pathStart)));
        return orderedHits;
    }

    /** 目标中心落到随飞行方向倾斜的剑气盒内（局部前向 / 右侧 / 上）。 */
    private static boolean isInsideWaveVolume(
            Entity target,
            Vec3 volumeCenter,
            ArcBasis arcBasis,
            float halfWidthBlocks,
            double forwardHalfThickness
    ) {
        Vec3 towardTarget = target.getBoundingBox().getCenter().subtract(volumeCenter);
        double alongForward = towardTarget.dot(arcBasis.forward());
        double alongRight = towardTarget.dot(arcBasis.right());
        double alongUp = towardTarget.dot(arcBasis.up());

        double targetRadius = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.5 + HIT_INFLATION_BLOCKS;
        return Math.abs(alongForward) <= forwardHalfThickness + targetRadius
                && Math.abs(alongRight) <= halfWidthBlocks + targetRadius
                && Math.abs(alongUp) <= WAVE_VERTICAL_HALF_HEIGHT_BLOCKS + targetRadius;
    }
}

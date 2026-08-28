package com.eldenring.spells.spell.combat;

import com.eldenring.spells.entity.GlintstoneArcProjectile;
import com.eldenring.spells.spell.GlintstoneArcSpell;
import net.minecraft.util.Mth;
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
 * 辉石弯弧命中：贴地对称月牙水波的水平判定盒（半宽随距离变大）。
 * <p>
 * MC 实体 AABB 不会跟着朝向转，所以碰撞箱只做追踪占位，真正打人走这里。
 * 垂直半高 / 前后厚度写死；半宽读 {@link GlintstoneArcSpell} 运行时字段。
 */
public final class GlintstoneArcCombat {

    /**
     * 弯弧相对飞行平面的垂直半高（方块）。调大 → 略抬头/低头也能刮到；调小 → 必须对准躯干。
     */
    public static final float ARC_VERTICAL_HALF_HEIGHT_BLOCKS = 0.55f;

    /**
     * 弯弧沿飞行方向的半厚（方块），再叠加上本 tick 位移的一半当扫掠。
     * 调大 → 高速时更不容易漏怪；调小 → 判定更贴刃。
     */
    public static final float ARC_FORWARD_HALF_THICKNESS_BLOCKS = 0.42f;

    /**
     * 目标碰撞箱额外外扩（方块），补偿「用中心点测定向盒」对大碰撞箱的低估。
     */
    public static final float HIT_INFLATION_BLOCKS = 0.28f;

    /**
     * 单层月牙半角（度）。左右各 65°，合计 130°，比 90° 更像月牙而不是一小段环。
     */
    public static final float CRESCENT_HALF_ANGLE_DEGREES = 65.0f;

    private GlintstoneArcCombat() {
    }

    /**
     * 收集本 tick 扫过的实体，按距路径起点从近到远排序。同一实体只留最近的一次。
     */
    public static List<EntityHitResult> collectEntityHits(
            GlintstoneArcProjectile arcProjectile,
            Level level,
            Vec3 pathStart,
            Vec3 pathEnd,
            float halfWidthBlocks
    ) {
        Vec3 flightDirection = arcProjectile.resolveFlightDirection();
        Vec3 horizontalForward = horizontalForward(flightDirection);
        Vec3 horizontalRight = horizontalRight(horizontalForward);
        Vec3 pathMidpoint = pathStart.add(pathEnd).scale(0.5);
        double movementLength = pathStart.distanceTo(pathEnd);
        double forwardHalfThickness = ARC_FORWARD_HALF_THICKNESS_BLOCKS + movementLength * 0.5;

        double searchInflation = Math.max(halfWidthBlocks, ARC_VERTICAL_HALF_HEIGHT_BLOCKS) + 1.25;
        AABB searchBox = new AABB(pathStart, pathEnd).inflate(searchInflation);

        Map<Integer, EntityHitResult> nearestHitsByEntityId = new HashMap<>();
        for (Entity target : level.getEntities(arcProjectile, searchBox, arcProjectile::isValidArcTarget)) {
            if (!isInsideArcVolume(
                    target,
                    pathMidpoint,
                    horizontalForward,
                    horizontalRight,
                    halfWidthBlocks,
                    forwardHalfThickness
            )) {
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

    /**
     * 目标中心落到贴地弯弧盒内（水平前向 / 水平右侧 / 世界 Y）。
     */
    private static boolean isInsideArcVolume(
            Entity target,
            Vec3 volumeCenter,
            Vec3 horizontalForward,
            Vec3 horizontalRight,
            float halfWidthBlocks,
            double forwardHalfThickness
    ) {
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 towardTarget = targetCenter.subtract(volumeCenter);
        double alongForward = towardTarget.dot(horizontalForward);
        double alongRight = towardTarget.dot(horizontalRight);
        double alongUp = towardTarget.y;

        double targetRadius = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.5 + HIT_INFLATION_BLOCKS;
        return Math.abs(alongForward) <= forwardHalfThickness + targetRadius
                && Math.abs(alongRight) <= halfWidthBlocks + targetRadius
                && Math.abs(alongUp) <= ARC_VERTICAL_HALF_HEIGHT_BLOCKS + targetRadius;
    }

    /**
     * 贴地水波用的水平前向：丢掉俯仰，只保留水平射击方向。
     * 近乎垂直仰视/俯视时水平分量退化，回退到 +Z。
     */
    public static Vec3 horizontalForward(Vec3 flightDirection) {
        Vec3 flattened = new Vec3(flightDirection.x, 0.0, flightDirection.z);
        if (flattened.lengthSqr() < 1.0e-8) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return flattened.normalize();
    }

    /**
     * 水平右侧：世界 +Y 叉水平前向，得到 (forwardZ, 0, -forwardX)。
     */
    public static Vec3 horizontalRight(Vec3 horizontalForward) {
        return new Vec3(horizontalForward.z, 0.0, -horizontalForward.x);
    }

    /**
     * 月牙外半径（方块）。半角处左右尖端的横向距离 = 半宽，
     * 所以 {@code R = halfWidth / sin(半角)}。
     */
    public static float crescentOuterRadius(float halfWidthBlocks) {
        return halfWidthBlocks / Mth.sin((float) Math.toRadians(CRESCENT_HALF_ANGLE_DEGREES));
    }
}

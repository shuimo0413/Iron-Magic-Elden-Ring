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
 * 辉石弯弧命中：随飞行方向倾斜的对称月牙定向盒（半宽随距离变大）。
 * <p>
 * MC 实体 AABB 不会跟着朝向转，所以碰撞箱只做追踪占位，真正打人走这里。
 * 垂直半高 / 前后厚度写死；半宽读 {@link GlintstoneArcSpell} 运行时字段。
 * 坐标系由 {@link ArcBasis} 提供，平视时与旧「贴地水平」一致，抬头/低头时整片弯弧跟着俯仰。
 */
public final class GlintstoneArcCombat {

    /**
     * 弯弧相对飞行平面的垂直半高（方块）。调大 → 略偏离刃面也能刮到；调小 → 必须对准刃面。
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

    /**
     * 叉积长度平方低于此值视为退化（近乎垂直仰视/俯视），改用备用参考轴。
     */
    private static final double BASIS_DEGENERATE_LENGTH_SQR = 1.0e-8;

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
        ArcBasis arcBasis = ArcBasis.fromFlightDirection(arcProjectile.resolveFlightDirection());
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
                    arcBasis,
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
     * 目标中心落到随飞行方向倾斜的弯弧盒内（局部前向 / 右侧 / 上）。
     */
    private static boolean isInsideArcVolume(
            Entity target,
            Vec3 volumeCenter,
            ArcBasis arcBasis,
            float halfWidthBlocks,
            double forwardHalfThickness
    ) {
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 towardTarget = targetCenter.subtract(volumeCenter);
        double alongForward = towardTarget.dot(arcBasis.forward());
        double alongRight = towardTarget.dot(arcBasis.right());
        double alongUp = towardTarget.dot(arcBasis.up());

        double targetRadius = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.5 + HIT_INFLATION_BLOCKS;
        return Math.abs(alongForward) <= forwardHalfThickness + targetRadius
                && Math.abs(alongRight) <= halfWidthBlocks + targetRadius
                && Math.abs(alongUp) <= ARC_VERTICAL_HALF_HEIGHT_BLOCKS + targetRadius;
    }

    /**
     * 月牙外半径（方块）。半角处左右尖端的横向距离 = 半宽，
     * 所以 {@code R = halfWidth / sin(半角)}。
     */
    public static float crescentOuterRadius(float halfWidthBlocks) {
        return halfWidthBlocks / Mth.sin((float) Math.toRadians(CRESCENT_HALF_ANGLE_DEGREES));
    }

    /**
     * 弯弧局部正交基：月牙在 (forward, right) 平面展开，矮墙高度沿 up。
     * <p>
     * 平视时与旧水平基一致（up ≈ 世界 +Y）；抬头/低头时整片跟着俯仰。
     * 近乎垂直时 worldUp×forward 退化，改用世界 +X 作参考，避免 NaN。
     */
    public record ArcBasis(Vec3 forward, Vec3 right, Vec3 up) {

        /**
         * 由完整飞行方向（含俯仰）建基。零向量回退到 +Z。
         */
        public static ArcBasis fromFlightDirection(Vec3 flightDirection) {
            Vec3 forward = flightDirection.lengthSqr() > BASIS_DEGENERATE_LENGTH_SQR
                    ? flightDirection.normalize()
                    : new Vec3(0.0, 0.0, 1.0);

            Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
            Vec3 right = worldUp.cross(forward);
            if (right.lengthSqr() < BASIS_DEGENERATE_LENGTH_SQR) {
                // 近乎竖直仰视/俯视：世界 +Y 与 forward 共线，改用 +X 作参考。
                right = new Vec3(1.0, 0.0, 0.0).cross(forward);
            }
            if (right.lengthSqr() < BASIS_DEGENERATE_LENGTH_SQR) {
                right = new Vec3(0.0, 0.0, 1.0);
            } else {
                right = right.normalize();
            }

            Vec3 up = forward.cross(right);
            if (up.lengthSqr() < BASIS_DEGENERATE_LENGTH_SQR) {
                up = worldUp;
            } else {
                up = up.normalize();
            }
            return new ArcBasis(forward, right, up);
        }
    }
}

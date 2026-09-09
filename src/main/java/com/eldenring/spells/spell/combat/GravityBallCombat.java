package com.eldenring.spells.spell.combat;

import com.eldenring.spells.entity.GravityBallProjectile;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 重力球命中：无伤害；按本级拉取格数把范围内敌人拉向施法者。
 * <p>
 * 距离 ≤ 拉取格数 → 直接拉到身前（停在弹道上的 stand-off）；
 * 距离更大 → 只沿 3D 连线朝施法者移动拉取格数（可上下拉）。
 * 命中半径 / 身前停距读自弹道实例（重力球与碎星共用本 Combat）。
 * <p>
 * 挡墙 / 挡顶 / 挡地用胸口细射线（不用整箱扫路径，避免贴地大怪被地面判死）；
 * 落点若仍嵌块则只向上抬出，再不行沿拉取方向回退。
 */
public final class GravityBallCombat {

    /**
     * 搜箱竖直半高相对命中半径的倍率。落点贴地时也要扫到站着的人。
     */
    public static final float HIT_VERTICAL_HALF_HEIGHT_FRACTION = 0.85f;

    /**
     * 挡障射线取在碰撞箱高度的比例（0.5 = 胸口附近）。
     * 不贴脚底，避免草径 / 半砖 / 贴地宽箱把水平拉力整段判死。
     */
    private static final double OBSTACLE_RAY_HEIGHT_FRACTION = 0.5;

    /**
     * 侧面撞墙时，半宽之外再回退的空隙（方块）。
     */
    private static final double WALL_SIDE_EXTRA_CLEARANCE_BLOCKS = 0.15;

    /**
     * 撞天花板时，头顶之外再回退的空隙（方块）。
     */
    private static final double CEILING_EXTRA_CLEARANCE_BLOCKS = 0.1;

    /**
     * 落点嵌块时最多向上抬出的格数。只升不降，防止拉进地里。
     */
    private static final double MAX_LIFT_OUT_OF_GROUND_BLOCKS = 2.0;

    /**
     * 向上抬出时的步长（方块）。
     */
    private static final double LIFT_STEP_BLOCKS = 0.1;

    /**
     * 抬出后仍嵌块时，沿拉取方向往回二分的迭代次数。
     */
    private static final int PULL_BACKOFF_BINARY_SEARCH_ITERATIONS = 12;

    private GravityBallCombat() {
    }

    /**
     * 以落点为球心搜合法敌人，按弹道上记下的拉取格数瞬移（无伤害）。
     */
    public static void resolve(
            GravityBallProjectile gravityBallProjectile,
            Level level,
            Vec3 impactCenter
    ) {
        Entity owner = gravityBallProjectile.getOwner();
        if (!(owner instanceof LivingEntity livingOwner)) {
            return;
        }

        float hitRadiusBlocks = gravityBallProjectile.hitRadiusBlocks();
        double hitRadiusSquared = hitRadiusBlocks * hitRadiusBlocks;
        double verticalHalfHeight = hitRadiusBlocks * HIT_VERTICAL_HALF_HEIGHT_FRACTION;
        double pullBlocks = Math.max(0.0, gravityBallProjectile.pullDistanceBlocks());

        AABB searchBox = new AABB(impactCenter, impactCenter).inflate(
                hitRadiusBlocks,
                verticalHalfHeight,
                hitRadiusBlocks
        );

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchBox, candidate ->
                candidate.isAlive()
                        && candidate.isPickable()
                        && !candidate.isSpectator()
                        && candidate != livingOwner
                        && !DamageSources.isFriendlyFireBetween(candidate, livingOwner)
        )) {
            double closestDistanceSquared = closestDistanceSquaredToBoundingBox(impactCenter, target.getBoundingBox());
            if (closestDistanceSquared > hitRadiusSquared) {
                continue;
            }
            pullTargetTowardCaster(
                    level,
                    livingOwner,
                    target,
                    pullBlocks,
                    gravityBallProjectile.standOffBlocks()
            );
        }
    }

    /**
     * 按与施法者的 3D 距离决定：贴脸拉到身前，或只拉近 {@code pullBlocks} 格。
     * 含上下方向，玩家在上方 / 下方也能把目标拉起来或拉下去。
     */
    private static void pullTargetTowardCaster(
            Level level,
            LivingEntity caster,
            LivingEntity target,
            double pullBlocks,
            double standOffBlocks
    ) {
        if (pullBlocks <= 1.0e-6) {
            return;
        }

        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 casterCenter = caster.getBoundingBox().getCenter();
        Vec3 towardCaster = casterCenter.subtract(targetCenter);
        double distanceBlocks = towardCaster.length();
        if (distanceBlocks < 1.0e-4) {
            return;
        }
        Vec3 pullDirection = towardCaster.scale(1.0 / distanceBlocks);

        Vec3 desiredCenter;
        if (distanceBlocks <= pullBlocks) {
            desiredCenter = casterCenter.subtract(pullDirection.scale(standOffBlocks));
        } else {
            desiredCenter = targetCenter.add(pullDirection.scale(pullBlocks));
        }

        double halfHeight = target.getBbHeight() * 0.5;
        Vec3 startFeet = target.position();
        Vec3 desiredFeet = new Vec3(desiredCenter.x, desiredCenter.y - halfHeight, desiredCenter.z);
        Vec3 safeFeet = resolvePullFeet(level, target, startFeet, desiredFeet);
        teleportLiving(level, target, safeFeet.x, safeFeet.y, safeFeet.z);
    }

    /**
     * 3D 拉取落点：胸口细射线挡障 → 嵌块只上抬 → 仍嵌则沿拉取方向回退。
     */
    private static Vec3 resolvePullFeet(
            Level level,
            LivingEntity target,
            Vec3 startFeet,
            Vec3 desiredFeet
    ) {
        Vec3 obstacleClampedFeet = clampByChestRay(level, target, startFeet, desiredFeet);
        Vec3 liftedFeet = liftOutOfGroundOnly(level, target, obstacleClampedFeet);
        if (level.noCollision(target, collisionBoxAtFeet(target, liftedFeet))) {
            return liftedFeet;
        }
        return backoffAlongPullUntilClear(level, target, startFeet, liftedFeet);
    }

    /**
     * 从起点胸口打到终点胸口的细射线；撞实心块则按撞击面回退，使整箱大致停在障碍外。
     * 不用整箱扫路径，避免贴地宽碰撞箱把地面当成墙导致大怪拉不动。
     */
    private static Vec3 clampByChestRay(
            Level level,
            LivingEntity target,
            Vec3 startFeet,
            Vec3 desiredFeet
    ) {
        double sampleHeightBlocks = target.getBbHeight() * OBSTACLE_RAY_HEIGHT_FRACTION;
        Vec3 rayStart = new Vec3(startFeet.x, startFeet.y + sampleHeightBlocks, startFeet.z);
        Vec3 rayEnd = new Vec3(desiredFeet.x, desiredFeet.y + sampleHeightBlocks, desiredFeet.z);
        Vec3 feetDelta = desiredFeet.subtract(startFeet);
        double desiredTravelBlocks = feetDelta.length();
        if (desiredTravelBlocks < 1.0e-6) {
            return startFeet;
        }

        BlockHitResult blockHit = level.clip(new ClipContext(
                rayStart,
                rayEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                target
        ));
        if (blockHit.getType() != HitResult.Type.BLOCK) {
            return desiredFeet;
        }

        double clearanceBlocks = clearanceForHitFace(
                blockHit.getDirection(),
                sampleHeightBlocks,
                target.getBbWidth(),
                target.getBbHeight()
        );
        double hitTravelBlocks = rayStart.distanceTo(blockHit.getLocation());
        double stopTravelBlocks = hitTravelBlocks - clearanceBlocks;
        if (stopTravelBlocks <= 1.0e-4) {
            return startFeet;
        }

        double travelFraction = Math.min(1.0, stopTravelBlocks / desiredTravelBlocks);
        return startFeet.add(feetDelta.scale(travelFraction));
    }

    /**
     * 按撞击面决定回退量：地面按胸口高度、天花板按头顶、侧面按半宽。
     */
    private static double clearanceForHitFace(
            Direction hitFace,
            double sampleHeightBlocks,
            float entityWidthBlocks,
            float entityHeightBlocks
    ) {
        if (hitFace == Direction.UP) {
            // 地面：把脚底大致停在地面上，而不是胸口穿进地里。
            return sampleHeightBlocks;
        }
        if (hitFace == Direction.DOWN) {
            return entityHeightBlocks - sampleHeightBlocks + CEILING_EXTRA_CLEARANCE_BLOCKS;
        }
        return entityWidthBlocks * 0.5 + WALL_SIDE_EXTRA_CLEARANCE_BLOCKS;
    }

    /**
     * 若脚底箱子嵌进方块，只向上抬到空隙；绝不向下塞进地里。
     */
    private static Vec3 liftOutOfGroundOnly(Level level, LivingEntity target, Vec3 feet) {
        if (level.noCollision(target, collisionBoxAtFeet(target, feet))) {
            return feet;
        }

        for (double liftBlocks = LIFT_STEP_BLOCKS;
             liftBlocks <= MAX_LIFT_OUT_OF_GROUND_BLOCKS;
             liftBlocks += LIFT_STEP_BLOCKS
        ) {
            Vec3 liftedFeet = feet.add(0.0, liftBlocks, 0.0);
            if (level.noCollision(target, collisionBoxAtFeet(target, liftedFeet))) {
                return liftedFeet;
            }
        }
        return feet;
    }

    /**
     * 抬出后仍碰撞：沿 3D 拉取方向退回起点，找最远仍不嵌块的位置。
     */
    private static Vec3 backoffAlongPullUntilClear(
            Level level,
            LivingEntity target,
            Vec3 startFeet,
            Vec3 attemptedFeet
    ) {
        Vec3 feetDelta = attemptedFeet.subtract(startFeet);
        if (feetDelta.lengthSqr() < 1.0e-8) {
            return startFeet;
        }

        double lowFraction = 0.0;
        double highFraction = 1.0;
        Vec3 bestFeet = startFeet;
        for (int iteration = 0; iteration < PULL_BACKOFF_BINARY_SEARCH_ITERATIONS; iteration++) {
            double midFraction = (lowFraction + highFraction) * 0.5;
            Vec3 candidateFeet = liftOutOfGroundOnly(
                    level,
                    target,
                    startFeet.add(feetDelta.scale(midFraction))
            );
            if (level.noCollision(target, collisionBoxAtFeet(target, candidateFeet))) {
                lowFraction = midFraction;
                bestFeet = candidateFeet;
            } else {
                highFraction = midFraction;
            }
        }
        return bestFeet;
    }

    private static AABB collisionBoxAtFeet(LivingEntity target, Vec3 feet) {
        return target.getDimensions(target.getPose()).makeBoundingBox(feet);
    }

    private static void teleportLiving(Level level, LivingEntity target, double x, double y, double z) {
        if (target instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            serverPlayer.teleportTo(
                    serverLevel,
                    x,
                    y,
                    z,
                    serverPlayer.getYRot(),
                    serverPlayer.getXRot()
            );
        } else {
            target.teleportTo(x, y, z);
        }
        target.setDeltaMovement(Vec3.ZERO);
        target.hasImpulse = true;
        target.fallDistance = 0.0f;
        target.hurtMarked = true;
    }

    private static double closestDistanceSquaredToBoundingBox(Vec3 point, AABB box) {
        double clampedX = Mth.clamp(point.x, box.minX, box.maxX);
        double clampedY = Mth.clamp(point.y, box.minY, box.maxY);
        double clampedZ = Mth.clamp(point.z, box.minZ, box.maxZ);
        double deltaX = point.x - clampedX;
        double deltaY = point.y - clampedY;
        double deltaZ = point.z - clampedZ;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }
}

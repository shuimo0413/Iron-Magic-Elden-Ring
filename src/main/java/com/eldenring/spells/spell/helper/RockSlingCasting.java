package com.eldenring.spells.spell.helper;

import com.eldenring.spells.entity.RockSlingProjectile;
import com.eldenring.spells.spell.RockSlingSpell;
import com.eldenring.spells.spell.data.RockSlingCastData;
import com.eldenring.spells.tracking.TrackingTargetFilter;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 岩石球的出手逻辑：身前横排槽位、生成悬停岩石、满蓄发射与索敌、取消碎裂。
 * <p>
 * 只放「锁人 / 摆位」；伤害与击退在 {@code RockSlingCombat}，粒子在 {@code RockSlingFx}。
 */
public final class RockSlingCasting {

    /**
     * 槽位相对眼睛沿视线前移（方块）。调大 → 岩石离脸更远，不挡准星；调小 → 更有压迫感但易遮视野。
     */
    private static final double HOVER_FORWARD_OFFSET_BLOCKS = 1.6;

    /**
     * 相邻两块岩石的横向间距（方块）。调大 → 横排更宽；三块时左右两块各偏 1 个间距。
     */
    private static final double HOVER_SLOT_SPACING_BLOCKS = 1.0;

    /**
     * 槽位相对视线平面向上的偏移（方块）。负值 = 略低于准星，不挡瞄准。
     */
    private static final double HOVER_UP_OFFSET_BLOCKS = -0.2;

    /**
     * 发射索敌锥半角（度）：目标须在施法者视线此锥内。调大 → 更容易锁到侧面的怪。
     */
    private static final double LAUNCH_ACQUIRE_CONE_HALF_ANGLE_DEGREES = 30.0;

    /**
     * 准星射线命中实体时的碰撞箱外扩（方块）。准星点在谁身上就优先锁谁。
     */
    private static final float LOOK_RAY_HIT_INFLATION_BLOCKS = 0.30f;

    /**
     * 找回悬停岩石时的搜索半径（方块）。仅在附加施法数据丢失时兜底使用。
     */
    private static final double HOVERING_ROCK_FALLBACK_SEARCH_RADIUS_BLOCKS = 6.0;

    private RockSlingCasting() {
    }

    /**
     * 第 {@code slotIndex} 块岩石的悬停中心（世界坐标）：眼前一段距离，横排居中均分。
     */
    public static Vec3 slotCenterWorldPosition(LivingEntity caster, int slotIndex, int slotCount) {
        Vec3 lookDirection = caster.getLookAngle();
        Vec3 forward = lookDirection.lengthSqr() > 1.0e-8 ? lookDirection.normalize() : new Vec3(0.0, 0.0, 1.0);
        Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
        if (right.lengthSqr() < 1.0e-8) {
            float yawRadians = caster.getYRot() * Mth.DEG_TO_RAD;
            right = new Vec3(-Mth.cos(yawRadians), 0.0, -Mth.sin(yawRadians));
        }
        right = right.normalize();
        Vec3 planeUp = right.cross(forward).normalize();
        double lateralOffsetBlocks = (slotIndex - (slotCount - 1) * 0.5) * HOVER_SLOT_SPACING_BLOCKS;
        return caster.getEyePosition()
                .add(forward.scale(HOVER_FORWARD_OFFSET_BLOCKS))
                .add(right.scale(lateralOffsetBlocks))
                .add(planeUp.scale(HOVER_UP_OFFSET_BLOCKS));
    }

    /**
     * 生成本次蓄力的悬停岩石，并把 UUID 记进施法数据。
     */
    public static void spawnHoveringRocks(
            Level level,
            LivingEntity caster,
            float rockDamage,
            RockSlingCastData castData
    ) {
        int rockCount = Math.max(1, RockSlingSpell.ROCK_COUNT);
        for (int slotIndex = 0; slotIndex < rockCount; slotIndex++) {
            RockSlingProjectile rock = createHoveringRock(level, caster, rockDamage, slotIndex, rockCount);
            level.addFreshEntity(rock);
            castData.addHoveringRock(rock.getUUID());
        }
    }

    /**
     * 满蓄：找到本次凝聚的岩石并发射。施法数据丢失时按主人就近找回；
     * 一块都找不到（非玩家施法者没走预施法等）则直接生成满尺寸岩石再射。
     */
    public static void launchRocks(
            Level level,
            LivingEntity caster,
            float rockDamage,
            @Nullable RockSlingCastData castData
    ) {
        List<RockSlingProjectile> rocks = collectHoveringRocks(level, caster, castData);
        if (rocks.isEmpty()) {
            int rockCount = Math.max(1, RockSlingSpell.ROCK_COUNT);
            for (int slotIndex = 0; slotIndex < rockCount; slotIndex++) {
                RockSlingProjectile rock = createHoveringRock(level, caster, rockDamage, slotIndex, rockCount);
                level.addFreshEntity(rock);
                rocks.add(rock);
            }
        }
        if (castData != null) {
            castData.clearHoveringRocks();
        }

        LivingEntity launchTarget = findLaunchTarget(level, caster);
        Vec3 convergencePoint = launchTarget != null ? null : crosshairConvergencePoint(level, caster);
        for (RockSlingProjectile rock : rocks) {
            rock.setDamage(rockDamage);
            rock.launch(launchTarget, convergencePoint);
        }
    }

    /**
     * 取消蓄力：还在悬停的岩石原地碎裂。
     */
    public static void shatterHoveringRocks(Level level, RockSlingCastData castData) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (UUID rockUuid : castData.hoveringRockUuids()) {
            if (serverLevel.getEntity(rockUuid) instanceof RockSlingProjectile rock && !rock.hasLaunched()) {
                rock.shatterAndDiscard();
            }
        }
        castData.clearHoveringRocks();
    }

    private static RockSlingProjectile createHoveringRock(
            Level level,
            LivingEntity caster,
            float rockDamage,
            int slotIndex,
            int slotCount
    ) {
        RockSlingProjectile rock = new RockSlingProjectile(level, caster);
        rock.setSlot(slotIndex, slotCount);
        rock.setDamage(rockDamage);
        rock.setPos(rock.feetPositionForCenter(slotCenterWorldPosition(caster, slotIndex, slotCount)));
        return rock;
    }

    private static List<RockSlingProjectile> collectHoveringRocks(
            Level level,
            LivingEntity caster,
            @Nullable RockSlingCastData castData
    ) {
        List<RockSlingProjectile> rocks = new ArrayList<>();
        if (castData != null && level instanceof ServerLevel serverLevel) {
            for (UUID rockUuid : castData.hoveringRockUuids()) {
                if (serverLevel.getEntity(rockUuid) instanceof RockSlingProjectile rock
                        && rock.isAlive()
                        && !rock.hasLaunched()) {
                    rocks.add(rock);
                }
            }
        }
        if (!rocks.isEmpty()) {
            return rocks;
        }
        AABB searchBox = caster.getBoundingBox().inflate(HOVERING_ROCK_FALLBACK_SEARCH_RADIUS_BLOCKS);
        rocks.addAll(level.getEntitiesOfClass(RockSlingProjectile.class, searchBox, rock ->
                rock.isAlive() && !rock.hasLaunched() && rock.getOwner() == caster
        ));
        return rocks;
    }

    /**
     * 发射目标：准星直接点中的合法敌人优先；否则取视线锥内「角度主导、距离次之」最优者。
     */
    @Nullable
    private static LivingEntity findLaunchTarget(Level level, LivingEntity caster) {
        double trackingRangeBlocks = RockSlingSpell.PROJECTILE_TRACKING_RANGE_BLOCKS;
        Vec3 eyePosition = caster.getEyePosition();
        Vec3 lookDirection = caster.getLookAngle().normalize();
        AABB searchBox = caster.getBoundingBox().inflate(trackingRangeBlocks);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox, candidate ->
                isValidTarget(candidate, caster)
                        && candidate.distanceToSqr(caster) <= trackingRangeBlocks * trackingRangeBlocks
                        && hasLineOfSight(level, caster, eyePosition, aimPointOnTarget(candidate))
        );
        if (candidates.isEmpty()) {
            return null;
        }

        Vec3 lookEnd = eyePosition.add(lookDirection.scale(trackingRangeBlocks));
        LivingEntity crosshairTarget = null;
        double closestCrosshairDistanceSquared = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            HitResult hit = Utils.checkEntityIntersecting(candidate, eyePosition, lookEnd, LOOK_RAY_HIT_INFLATION_BLOCKS);
            if (hit.getType() == HitResult.Type.MISS) {
                continue;
            }
            double hitDistanceSquared = hit.getLocation().distanceToSqr(eyePosition);
            if (hitDistanceSquared < closestCrosshairDistanceSquared) {
                closestCrosshairDistanceSquared = hitDistanceSquared;
                crosshairTarget = candidate;
            }
        }
        if (crosshairTarget != null) {
            return crosshairTarget;
        }

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            Vec3 towardCandidate = aimPointOnTarget(candidate).subtract(eyePosition);
            double distanceBlocks = towardCandidate.length();
            if (distanceBlocks < 1.0e-4) {
                continue;
            }
            double angleDegrees = Math.toDegrees(Math.acos(Mth.clamp(
                    lookDirection.dot(towardCandidate.scale(1.0 / distanceBlocks)),
                    -1.0,
                    1.0
            )));
            if (angleDegrees > LAUNCH_ACQUIRE_CONE_HALF_ANGLE_DEGREES) {
                continue;
            }
            double score = angleDegrees * 8.0 + distanceBlocks;
            if (score < bestScore) {
                bestScore = score;
                bestTarget = candidate;
            }
        }
        return bestTarget;
    }

    /**
     * 没有目标时岩石汇聚到准星落点（射线撞方块的位置，或射程尽头），三块从两侧斜着收拢。
     */
    private static Vec3 crosshairConvergencePoint(Level level, LivingEntity caster) {
        Vec3 eyePosition = caster.getEyePosition();
        Vec3 lookEnd = eyePosition.add(caster.getLookAngle().normalize().scale(RockSlingSpell.PROJECTILE_MAX_RANGE_BLOCKS));
        BlockHitResult blockHit = level.clip(new ClipContext(
                eyePosition,
                lookEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        ));
        return blockHit.getType() == HitResult.Type.MISS ? lookEnd : blockHit.getLocation();
    }

    /**
     * 是否可以当作敌人：活着、可点选、不是主人或盟友，且未被玩家的追踪忽略设置排除。
     */
    public static boolean isValidTarget(LivingEntity candidate, @Nullable Entity owner) {
        if (!candidate.isAlive() || candidate.isSpectator() || !candidate.isPickable()) {
            return false;
        }
        if (owner != null && (candidate == owner || owner.isAlliedTo(candidate) || candidate.isAlliedTo(owner))) {
            return false;
        }
        return TrackingTargetFilter.allowsTracking(owner, candidate);
    }

    /** 瞄准点：目标碰撞箱中部偏上，避免砸地。 */
    public static Vec3 aimPointOnTarget(LivingEntity target) {
        AABB box = target.getBoundingBox();
        return new Vec3(box.getCenter().x, Mth.lerp(0.6, box.minY, box.maxY), box.getCenter().z);
    }

    private static boolean hasLineOfSight(Level level, Entity viewer, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                viewer
        )).getType() == HitResult.Type.MISS;
    }
}

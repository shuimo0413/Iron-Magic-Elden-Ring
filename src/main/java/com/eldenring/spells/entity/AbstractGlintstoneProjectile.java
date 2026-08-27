package com.eldenring.spells.entity;

import com.eldenring.spells.particle.glintstone.GlintstoneFx;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 辉石系弹道公共基类：限角锥形追踪 + {@link GlintstoneFx} 拖尾/爆裂。
 * <p>
 * 索敌：飞行方向锥 + 转向预算硬过滤后，按「角度主导、距离弱惩罚」打分；
 * 施法者准星射线命中合法目标时优先锁它。粘滞 UUID 只在目标失效后重选。
 * <p>
 * 消失问题说明（重要）：找不到追踪目标时<strong>绝不会</strong> discard，只会直飞。
 * 真正会销毁的只有：撞方块、撞实体后结算、超时、反魔法等。
 * 因此「飞一会就没」几乎总是撞到了方块（含被追踪拽进地面），而不是丢目标。
 */
public abstract class AbstractGlintstoneProjectile extends AbstractMagicProjectile {

    /**
     * 出手后忽略命中检测的 tick 数，避免出生重叠立刻销毁。
     * 期间照常飞行；不做贴墖setPos / 嵌块挤出（那些反而会把弹塞进方块里）。
     */
    private static final int COLLISION_GRACE_TICKS = 4;

    /**
     * 命中射线相对目标碰撞箱的外扩（方块）。
     */
    private static final float HIT_DETECTION_INFLATION_BLOCKS = 0.35f;

    /**
     * 施法者准星射线命中检测时，目标碰撞箱外扩（方块）。
     */
    private static final float LOOK_RAY_HIT_INFLATION_BLOCKS = 0.30f;

    /**
     * 索敌打分角度权重。调大更死盯准星方向。
     */
    private static final double ACQUIRE_ANGULAR_WEIGHT = 1.0;

    /**
     * 索敌打分距离权重。调大更倾向近目标。
     */
    private static final double ACQUIRE_DISTANCE_WEIGHT = 0.25;

    /**
     * 转向预算松弛 [0,1]。调小则近处大偏角更难被锁。
     */
    private static final double TURN_BUDGET_SLACK = 0.85;

    /**
     * 瞄准点相对目标碰撞箱：0=脚底，1=头顶。取偏上避免扎地。
     */
    private static final double TRACKING_AIM_HEIGHT_FRACTION = 0.7;

    /** 粘滞追踪目标 UUID；失效只是改直飞，不销毁。*/
    @Nullable
    private UUID lockedTrackingTargetUuid;

    /**
     * 仅客户端 tick 写入的真实飞行历史；不参与网络同步与存档。
     * Renderer 用这些世界坐标重建弯曲光轨。
     */
    private final TrailHistoryBuffer clientTrailHistory = new TrailHistoryBuffer();

    protected AbstractGlintstoneProjectile(
            EntityType<? extends AbstractGlintstoneProjectile> entityType,
            Level level
    ) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    protected abstract float flightSpeed();

    protected abstract double trackingRangeBlocks();

    protected abstract float maxTurnAngleDegreesPerTick();

    protected abstract int trackingStartDelayTicks();

    protected abstract float trackingAcquireConeHalfAngleDegrees();

    protected abstract double minimumSpeedForHoming();

    protected abstract double directionAlignEpsilonRadians();

    protected abstract float trailParticleIntensity();

    /**
     * 当前法术独立的连续光轨配置（长度、半宽、点缀概率）。
     * 供客户端 Renderer 与粒子点缀共用。
     */
    public abstract GlintstoneTrailStyle trailStyle();

    /**
     * 返回普通辉石弹道的客户端历史路径（最旧 → 最新）。
     */
    public List<Vec3> trailHistoryWorldPositions() {
        return clientTrailHistory.snapshot();
    }

    protected abstract float impactParticleIntensity();

    protected abstract AbstractSpell damageSourceSpell();

    public abstract GlintstoneVisualStyle visualStyle();

    @Override
    public void trailParticles() {
        Vec3 deltaMovement = getDeltaMovement();
        double movementLength = deltaMovement.length();
        if (movementLength < 1.0e-4) {
            return;
        }
        GlintstoneTrailStyle trailStyle = trailStyle();
        clientTrailHistory.record(
                position(),
                trailStyle.lengthBlocks(),
                trailStyle.maximumHistoryPointCount()
        );
        // 几何光束由 Renderer 绘制；这里只在弹头补极少火花/闪星
        spawnTrailAccentParticles(deltaMovement, trailStyle);
    }

    /**
     * 弹头点缀粒子。普通辉石走青蓝库；毁灭流星等可覆盖成蓝紫星河粒子。
     */
    protected void spawnTrailAccentParticles(Vec3 deltaMovement, GlintstoneTrailStyle trailStyle) {
        GlintstoneFx.trailAccents(
                level(),
                getX(),
                getY(),
                getZ(),
                deltaMovement,
                trailParticleIntensity(),
                trailStyle
        );
    }

    @Override
    public void impactParticles(double impactX, double impactY, double impactZ) {
        spawnImpactParticles(impactX, impactY, impactZ);
    }

    /**
     * 命中爆裂粒子。覆盖本方法即可换调色板，不必重写光轨历史记录。
     */
    protected void spawnImpactParticles(double impactX, double impactY, double impactZ) {
        GlintstoneFx.impact(level(), impactX, impactY, impactZ, impactParticleIntensity());
    }

    @Override
    public float getSpeed() {
        return flightSpeed();
    }

    @Override
    public float getHitDetectionInflation() {
        return HIT_DETECTION_INFLATION_BLOCKS;
    }

    protected float explosionRadiusBlocks() {
        return 0.0f;
    }

    @Override
    public Optional<Holder<SoundEvent>> getImpactSound() {
        return Optional.of(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_HIT));
    }

    @Override
    public void tick() {
        // 非法坐标会让实体直接从世界上“消失”，并在读档时刷 Entity has invalid position
        if (!isFinitePositionAndMotion()) {
            discard();
            return;
        }
        super.tick();
    }

    private boolean isFinitePositionAndMotion() {
        Vec3 motion = getDeltaMovement();
        return Double.isFinite(getX())
                && Double.isFinite(getY())
                && Double.isFinite(getZ())
                && Double.isFinite(motion.x)
                && Double.isFinite(motion.y)
                && Double.isFinite(motion.z);
    }

    /**
     * 宽限期内完全跳过命中；之后走铁魔法流程，但实体已销毁时不再二次撞方块。
     */
    @Override
    public void handleHitDetection() {
        if (tickCount <= COLLISION_GRACE_TICKS) {
            return;
        }
        Vec3 startPosition = position();
        Vec3 destination = startPosition.add(getDeltaMovement());
        BlockHitResult blockCollision = level().clip(new ClipContext(
                startPosition,
                destination,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));
        if (collidesWithBlocks() && blockCollision.getType() != HitResult.Type.MISS) {
            destination = blockCollision.getLocation();
        }

        for (HitResult hitResult : raycastEntitiesAlongPath(destination, startPosition)) {
            if (!(hitResult instanceof EntityHitResult entityHitResult)) {
                continue;
            }
            if (entityHitResult.getType() != HitResult.Type.MISS
                    && !NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(this, entityHitResult)).isCanceled()) {
                onHit(entityHitResult);
            }
            if (this.isRemoved()) {
                return;
            }
        }

        if (collidesWithBlocks()
                && blockCollision.getType() != HitResult.Type.MISS
                && !this.isRemoved()
                && !NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(this, blockCollision)).isCanceled()) {
            onHit(blockCollision);
        }
    }

    private List<HitResult> raycastEntitiesAlongPath(Vec3 destination, Vec3 startPosition) {
        AABB searchBox = getBoundingBox().expandTowards(destination.subtract(startPosition)).inflate(0.1);
        List<HitResult> hits = new ArrayList<>();
        List<Entity> alreadyHitEntities = new ArrayList<>();
        for (Entity target : level().getEntities(this, searchBox, this::canHitEntity)) {
            if (alreadyHitEntities.contains(target)) {
                continue;
            }
            HitResult hit = Utils.checkEntityIntersecting(
                    target,
                    startPosition,
                    destination,
                    getHitDetectionInflation()
            );
            if (hit.getType() != HitResult.Type.MISS) {
                hits.add(hit);
                alreadyHitEntities.add(target);
            }
        }
        hits.sort(Comparator.comparingDouble(hit -> hit.getLocation().distanceToSqr(startPosition)));
        return hits;
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity targetEntity) {
        if (targetEntity instanceof AbstractGlintstoneProjectile otherGlintstoneProjectile) {
            Entity thisOwner = getOwner();
            Entity otherOwner = otherGlintstoneProjectile.getOwner();
            if (thisOwner != null && thisOwner == otherOwner) {
                return false;
            }
        }
        return super.canHitEntity(targetEntity);
    }

    @Override
    protected void handleEntityHoming() {
        if (level().isClientSide) {
            return;
        }
        if (tickCount < trackingStartDelayTicks()) {
            return;
        }

        LivingEntity trackingTarget = resolveTrackingTarget();
        // 找不到/ 丢失目标：保持当前速度直飞，绝不 discard
        if (trackingTarget == null) {
            return;
        }

        Vec3 currentDeltaMovement = getDeltaMovement();
        double currentSpeed = currentDeltaMovement.length();
        if (currentSpeed < minimumSpeedForHoming()) {
            return;
        }

        Vec3 currentFlightDirection = currentDeltaMovement.normalize();
        // 瞄躯干偏上，降低追地面怪时把弹道拽进地板
        Vec3 aimPoint = aimPointOnTarget(trackingTarget);
        Vec3 vectorTowardTarget = aimPoint.subtract(getBoundingBox().getCenter());
        double distanceTowardTarget = vectorTowardTarget.length();
        if (distanceTowardTarget < minimumSpeedForHoming()) {
            return;
        }
        Vec3 desiredDirectionTowardTarget = vectorTowardTarget.scale(1.0 / distanceTowardTarget);

        double directionDotProduct = Mth.clamp(
                currentFlightDirection.dot(desiredDirectionTowardTarget),
                -1.0,
                1.0
        );
        double angleBetweenDirectionsRadians = Math.acos(directionDotProduct);
        double maxTurnAngleRadians = Math.toRadians(maxTurnAngleDegreesPerTick());

        float slerpInterpolationFactor = angleBetweenDirectionsRadians < directionAlignEpsilonRadians()
                ? 1.0f
                : (float) Math.min(1.0, maxTurnAngleRadians / angleBetweenDirectionsRadians);

        Vec3 limitedTurnDirection = Utils.slerp(
                slerpInterpolationFactor,
                currentFlightDirection,
                desiredDirectionTowardTarget
        ).normalize();

        if (!Double.isFinite(limitedTurnDirection.x)
                || !Double.isFinite(limitedTurnDirection.y)
                || !Double.isFinite(limitedTurnDirection.z)) {
            return;
        }

        setDeltaMovement(limitedTurnDirection.scale(currentSpeed));
    }

    private static Vec3 aimPointOnTarget(LivingEntity target) {
        AABB box = target.getBoundingBox();
        return new Vec3(
                box.getCenter().x,
                Mth.lerp(TRACKING_AIM_HEIGHT_FRACTION, box.minY, box.maxY),
                box.getCenter().z
        );
    }

    @Nullable
    private LivingEntity resolveTrackingTarget() {
        Entity ownerEntity = getOwner();
        if (lockedTrackingTargetUuid != null && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            Entity lockedEntity = serverLevel.getEntity(lockedTrackingTargetUuid);
            if (lockedEntity instanceof LivingEntity lockedLiving
                    && canContinueTrackingLivingEntity(lockedLiving, ownerEntity)) {
                return lockedLiving;
            }
            lockedTrackingTargetUuid = null;
        }

        LivingEntity bestTrackableTarget = findBestTrackableTarget();
        if (bestTrackableTarget != null) {
            lockedTrackingTargetUuid = bestTrackableTarget.getUUID();
        }
        return bestTrackableTarget;
    }

    /**
     * 在合法候选中选最优追踪目标：施法者准星射线命中优先，否则角度主导打分。
     */
    @Nullable
    private LivingEntity findBestTrackableTarget() {
        Entity ownerEntity = getOwner();
        AABB searchBoundingBox = getBoundingBox().inflate(trackingRangeBlocks());
        List<LivingEntity> candidates = level().getEntitiesOfClass(
                LivingEntity.class,
                searchBoundingBox,
                candidateEntity -> canAcquireTrackingLivingEntity(candidateEntity, ownerEntity)
        );
        if (candidates.isEmpty()) {
            return null;
        }

        LivingEntity lookRayTarget = findLookRayPreferredTarget(ownerEntity, candidates);
        if (lookRayTarget != null) {
            return lookRayTarget;
        }

        return candidates.stream()
                .min(Comparator.comparingDouble(this::acquireScore))
                .orElse(null);
    }

    /**
     * 若施法者眼睛沿准星射线打中某个<strong>已通过硬过滤</strong>的候选，直接优先锁它。
     * 对应「准星就点在那个更远的怪身上」；弹已飞出后仍用施法者当前视线做一次优先，
     * 不绑死准星持续重选（外层仍有 UUID 粘滞）。
     */
    @Nullable
    private LivingEntity findLookRayPreferredTarget(
            @Nullable Entity ownerEntity,
            List<LivingEntity> candidates
    ) {
        if (!(ownerEntity instanceof LivingEntity caster)) {
            return null;
        }

        Vec3 eyePosition = caster.getEyePosition();
        Vec3 lookEnd = eyePosition.add(
                caster.getLookAngle().scale(trackingRangeBlocks())
        );
        LivingEntity closestHitCandidate = null;
        double closestHitDistanceSquared = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            HitResult hit = Utils.checkEntityIntersecting(
                    candidate,
                    eyePosition,
                    lookEnd,
                    LOOK_RAY_HIT_INFLATION_BLOCKS
            );
            if (hit.getType() == HitResult.Type.MISS) {
                continue;
            }
            double hitDistanceSquared = hit.getLocation().distanceToSqr(eyePosition);
            if (hitDistanceSquared < closestHitDistanceSquared) {
                closestHitDistanceSquared = hitDistanceSquared;
                closestHitCandidate = candidate;
            }
        }

        if (closestHitCandidate == null) {
            return null;
        }

        // 准星射线被方块挡住时，不优先锁射线后的实体
        BlockHitResult blockHit = level().clip(new ClipContext(
                eyePosition,
                lookEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        ));
        if (blockHit.getType() != HitResult.Type.MISS
                && blockHit.getLocation().distanceToSqr(eyePosition) + 1.0e-4 < closestHitDistanceSquared) {
            return null;
        }

        return closestHitCandidate;
    }

    /**
     * 索敌分数：越小越优先。
     * {@code (angle/cone)^2 * 角度权重 + (distance/range) * 距离权重}。
     */
    private double acquireScore(LivingEntity candidateEntity) {
        double distanceToCandidate = aimPointOnTarget(candidateEntity)
                .subtract(getBoundingBox().getCenter())
                .length();
        double angleDegrees = angleFromFlightAxisDegrees(candidateEntity);
        float coneHalfAngleDegrees = Math.max(1.0e-3f, trackingAcquireConeHalfAngleDegrees());
        double trackingRange = Math.max(1.0e-3, trackingRangeBlocks());
        double normalizedAngle = angleDegrees / coneHalfAngleDegrees;
        double normalizedDistance = distanceToCandidate / trackingRange;
        return normalizedAngle * normalizedAngle * ACQUIRE_ANGULAR_WEIGHT
                + normalizedDistance * ACQUIRE_DISTANCE_WEIGHT;
    }

    /**
     * 目标瞄准点相对当前飞行方向的夹角（度）。速度过小或目标重合时返回 180，表示不可索。
     */
    private double angleFromFlightAxisDegrees(LivingEntity candidateEntity) {
        Vec3 currentDeltaMovement = getDeltaMovement();
        double currentSpeed = currentDeltaMovement.length();
        if (currentSpeed < minimumSpeedForHoming()) {
            return 180.0;
        }

        Vec3 vectorToCandidate = aimPointOnTarget(candidateEntity).subtract(getBoundingBox().getCenter());
        double distanceToCandidate = vectorToCandidate.length();
        if (distanceToCandidate < minimumSpeedForHoming()) {
            return 180.0;
        }

        Vec3 currentFlightDirection = currentDeltaMovement.normalize();
        Vec3 directionToCandidate = vectorToCandidate.scale(1.0 / distanceToCandidate);
        double forwardDotProduct = Mth.clamp(currentFlightDirection.dot(directionToCandidate), -1.0, 1.0);
        return Math.toDegrees(Math.acos(forwardDotProduct));
    }

    /**
     * 按当前限角转向，在飞到目标大致距离内能否转够偏角。
     * {@code maxReachable = maxTurnPerTick * (distance / speed) * slack}。
     */
    private boolean isWithinTurnBudget(LivingEntity candidateEntity, double angleFromFlightAxisDegrees) {
        float flightSpeed = flightSpeed();
        if (flightSpeed < minimumSpeedForHoming()) {
            return false;
        }

        double distanceToCandidate = aimPointOnTarget(candidateEntity)
                .subtract(getBoundingBox().getCenter())
                .length();
        double estimatedTicks = distanceToCandidate / flightSpeed;
        double maxReachableAngleDegrees = maxTurnAngleDegreesPerTick()
                * estimatedTicks
                * TURN_BUDGET_SLACK;
        return angleFromFlightAxisDegrees <= maxReachableAngleDegrees;
    }

    private boolean canAcquireTrackingLivingEntity(LivingEntity candidateEntity, @Nullable Entity ownerEntity) {
        if (!canContinueTrackingLivingEntity(candidateEntity, ownerEntity)) {
            return false;
        }

        Vec3 currentDeltaMovement = getDeltaMovement();
        double currentSpeed = currentDeltaMovement.length();
        if (currentSpeed < minimumSpeedForHoming()) {
            return false;
        }

        double angleFromFlightAxisDegrees = angleFromFlightAxisDegrees(candidateEntity);
        if (angleFromFlightAxisDegrees > trackingAcquireConeHalfAngleDegrees()) {
            return false;
        }
        if (!isWithinTurnBudget(candidateEntity, angleFromFlightAxisDegrees)) {
            return false;
        }

        return hasClearLineOfSightToward(candidateEntity);
    }

    private boolean canContinueTrackingLivingEntity(LivingEntity candidateEntity, @Nullable Entity ownerEntity) {
        if (!candidateEntity.isAlive() || candidateEntity.isSpectator()) {
            return false;
        }
        if (ownerEntity != null
                && (candidateEntity == ownerEntity
                || ownerEntity.isAlliedTo(candidateEntity)
                || candidateEntity.isAlliedTo(ownerEntity))) {
            return false;
        }

        double trackingRange = trackingRangeBlocks();
        if (distanceToSqr(candidateEntity) > trackingRange * trackingRange) {
            return false;
        }

        return hasClearLineOfSightToward(candidateEntity);
    }

    /**
     * 用弹体中心→瞄准点做视线，比 Utils.hasLineOfSight(entity眼高) 更贴近飞行路径，
     * 减少「眼高看得见、弹体却在追地」的失锁/扎地。
     */
    private boolean hasClearLineOfSightToward(LivingEntity target) {
        Vec3 start = getBoundingBox().getCenter();
        Vec3 end = aimPointOnTarget(target);
        return level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        )).getType() == HitResult.Type.MISS;
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        if (this.isRemoved()) {
            return;
        }
        if (!level().isClientSide && explosionRadiusBlocks() > 0.0f) {
            dealHitDamage(blockHitResult.getLocation(), null);
        }
        super.onHitBlock(blockHitResult);
        discard();
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (!level().isClientSide) {
            Entity hitEntity = entityHitResult.getEntity();
            if (explosionRadiusBlocks() > 0.0f) {
                dealHitDamage(entityHitResult.getLocation(), hitEntity);
            } else {
                DamageSources.applyDamage(
                        hitEntity,
                        damage,
                        damageSourceSpell().getDamageSource(this, getOwner())
                );
            }
        }
        consumeEntityImpact(entityHitResult, true);
    }

    protected void dealHitDamage(@Nullable Vec3 impactLocation, @Nullable Entity primaryHitEntity) {
        var damageSource = damageSourceSpell().getDamageSource(this, getOwner());
        float radiusBlocks = explosionRadiusBlocks();
        if (radiusBlocks <= 0.0f) {
            if (primaryHitEntity != null) {
                DamageSources.applyDamage(primaryHitEntity, damage, damageSource);
            }
            return;
        }

        Vec3 explosionCenter = impactLocation != null ? impactLocation : position();
        AABB explosionSearchBox = new AABB(explosionCenter, explosionCenter).inflate(radiusBlocks);

        if (primaryHitEntity != null) {
            DamageSources.applyDamage(primaryHitEntity, damage, damageSource);
        }

        for (LivingEntity livingEntity : level().getEntitiesOfClass(
                LivingEntity.class,
                explosionSearchBox,
                candidate -> candidate.isAlive() && !candidate.isSpectator()
        )) {
            if (livingEntity == primaryHitEntity) {
                continue;
            }
            if (!isWithinExplosionRadius(livingEntity, explosionCenter, radiusBlocks)) {
                continue;
            }
            DamageSources.applyDamage(livingEntity, damage, damageSource);
        }
    }

    private static boolean isWithinExplosionRadius(
            LivingEntity livingEntity,
            Vec3 explosionCenter,
            float radiusBlocks
    ) {
        AABB targetBox = livingEntity.getBoundingBox();
        double clampedX = Mth.clamp(explosionCenter.x, targetBox.minX, targetBox.maxX);
        double clampedY = Mth.clamp(explosionCenter.y, targetBox.minY, targetBox.maxY);
        double clampedZ = Mth.clamp(explosionCenter.z, targetBox.minZ, targetBox.maxZ);
        double deltaX = explosionCenter.x - clampedX;
        double deltaY = explosionCenter.y - clampedY;
        double deltaZ = explosionCenter.z - clampedZ;
        return (deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) <= (double) radiusBlocks * radiusBlocks;
    }
}

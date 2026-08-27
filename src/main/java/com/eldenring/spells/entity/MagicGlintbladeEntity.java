package com.eldenring.spells.entity;

import com.eldenring.spells.particle.glintstone.GlintstoneFx;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModParticles;
import com.eldenring.spells.registry.ModSpells;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import com.eldenring.spells.spell.MagicGlintbladeSpell;

/**
 * 魔法辉剑：先在身前悬停，再沿准星飞出并做限角追踪。
 * <p>
 * 悬停阶段 {@code noPhysics}、速度为零；到期后 {@link #shoot} 进入普通弹道。
 * 视觉是立体辉剑 + 短曲线光带，不走彗星头。
 */
public class MagicGlintbladeEntity extends AbstractMagicProjectile {

    private static final EntityDataAccessor<Boolean> DATA_LAUNCHED =
            SynchedEntityData.defineId(MagicGlintbladeEntity.class, EntityDataSerializers.BOOLEAN);

    /** 仅客户端写入的真实飞行历史。 */
    private final TrailHistoryBuffer clientTrailHistory = new TrailHistoryBuffer();

    /** 生成时记下的发射方向；悬停结束时若没有更好目标就用它。 */
    private Vec3 storedLaunchDirection = new Vec3(0.0, 0.0, 1.0);

    @Nullable
    private UUID lockedTrackingTargetUuid;

    public MagicGlintbladeEntity(EntityType<? extends MagicGlintbladeEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public MagicGlintbladeEntity(Level level, LivingEntity shooter) {
        this(ModEntities.MAGIC_GLINTBLADE.get(), level);
        setOwner(shooter);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LAUNCHED, false);
    }

    public boolean hasLaunched() {
        return entityData.get(DATA_LAUNCHED);
    }

    /**
     * 记下悬停结束后的默认飞行方向（通常是施法者当时的视线）。
     */
    public void setStoredLaunchDirection(Vec3 launchDirection) {
        if (launchDirection.lengthSqr() < 1.0e-8) {
            this.storedLaunchDirection = new Vec3(0.0, 0.0, 1.0);
            return;
        }
        this.storedLaunchDirection = launchDirection.normalize();
    }

    public List<Vec3> trailHistoryWorldPositions() {
        return clientTrailHistory.snapshot();
    }

    public GlintstoneTrailStyle trailStyle() {
        return MagicGlintbladeSpell.TRAIL_STYLE;
    }

    @Override
    public void trailParticles() {
        if (!hasLaunched()) {
            spawnHoverParticles();
            return;
        }
        Vec3 deltaMovement = getDeltaMovement();
        if (deltaMovement.lengthSqr() < 1.0e-8) {
            return;
        }
        clientTrailHistory.record(
                position(),
                trailStyle().lengthBlocks(),
                trailStyle().maximumHistoryPointCount()
        );
        GlintstoneFx.trailAccents(
                level(),
                getX(),
                getY(),
                getZ(),
                deltaMovement,
                MagicGlintbladeSpell.TRAIL_PARTICLE_INTENSITY,
                trailStyle()
        );
    }

    @Override
    public void impactParticles(double impactX, double impactY, double impactZ) {
        GlintstoneFx.impact(level(), impactX, impactY, impactZ, MagicGlintbladeSpell.IMPACT_PARTICLE_INTENSITY);
    }

    @Override
    public float getSpeed() {
        return MagicGlintbladeSpell.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    public float getHitDetectionInflation() {
        return MagicGlintbladeSpell.HIT_DETECTION_INFLATION_BLOCKS;
    }

    @Override
    public Optional<Holder<SoundEvent>> getImpactSound() {
        return Optional.of(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_HIT));
    }

    @Override
    public void tick() {
        if (!isFinitePositionAndMotion()) {
            discard();
            return;
        }
        if (tickCount >= MagicGlintbladeSpell.ENTITY_LIFETIME_TICKS) {
            discard();
            return;
        }

        if (!hasLaunched()) {
            setDeltaMovement(Vec3.ZERO);
            this.noPhysics = true;
            super.tick();
            if (!level().isClientSide && tickCount >= MagicGlintbladeSpell.HOVER_DURATION_TICKS) {
                launchTowardTargetOrLook();
            }
            return;
        }

        super.tick();
    }

    /**
     * 悬停时不要按速度平移，也不要被零向量的 atan2 把朝向拧成 0。
     */
    @Override
    public void travel() {
        if (!hasLaunched()) {
            return;
        }
        super.travel();
    }

    @Override
    public void rotateWithMotion() {
        if (!hasLaunched()) {
            return;
        }
        super.rotateWithMotion();
    }

    /**
     * 悬停结束：优先朝锥内最近合法目标飞，否则沿生成时视线。
     */
    private void launchTowardTargetOrLook() {
        Vec3 shootDirection = storedLaunchDirection;
        LivingEntity launchTarget = findLaunchTarget();
        if (launchTarget != null) {
            Vec3 towardTarget = aimPointOnTarget(launchTarget).subtract(getBoundingBox().getCenter());
            if (towardTarget.lengthSqr() > 1.0e-6) {
                shootDirection = towardTarget.normalize();
            }
        }
        this.noPhysics = false;
        entityData.set(DATA_LAUNCHED, true);
        shoot(shootDirection);
        float yawDegrees = (float) (Mth.atan2(shootDirection.x, shootDirection.z) * Mth.RAD_TO_DEG);
        float pitchDegrees = (float) (Mth.atan2(
                shootDirection.y,
                shootDirection.horizontalDistance()
        ) * Mth.RAD_TO_DEG);
        setYRot(yawDegrees);
        setXRot(pitchDegrees);
        this.yRotO = yawDegrees;
        this.xRotO = pitchDegrees;

        level().playSound(
                null,
                getX(),
                getY(),
                getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.NEUTRAL,
                0.9f,
                1.45f + level().random.nextFloat() * 0.15f
        );
        level().playSound(
                null,
                getX(),
                getY(),
                getZ(),
                SoundEvents.TRIDENT_THROW.value(),
                SoundSource.NEUTRAL,
                0.55f,
                1.65f
        );
    }

    private void spawnHoverParticles() {
        if (level().random.nextFloat() > 0.55f) {
            return;
        }
        double scatter = 0.18;
        level().addParticle(
                ModParticles.GLINTSTONE_GLOW.get(),
                getX() + (level().random.nextDouble() - 0.5) * scatter,
                getY() + 0.35 + (level().random.nextDouble() - 0.5) * scatter,
                getZ() + (level().random.nextDouble() - 0.5) * scatter,
                0.0,
                0.015,
                0.0
        );
        if (level().random.nextFloat() < 0.4f) {
            level().addParticle(
                    ModParticles.GLINTSTONE_MOTE.get(),
                    getX() + (level().random.nextDouble() - 0.5) * scatter,
                    getY() + 0.45,
                    getZ() + (level().random.nextDouble() - 0.5) * scatter,
                    (level().random.nextDouble() - 0.5) * 0.02,
                    0.02,
                    (level().random.nextDouble() - 0.5) * 0.02
            );
        }
    }

    @Override
    public void handleHitDetection() {
        if (!hasLaunched()) {
            return;
        }
        int ticksSinceLaunch = tickCount - MagicGlintbladeSpell.HOVER_DURATION_TICKS;
        if (ticksSinceLaunch <= MagicGlintbladeSpell.COLLISION_GRACE_TICKS) {
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

        AABB searchBox = getBoundingBox().expandTowards(destination.subtract(startPosition)).inflate(0.1);
        List<HitResult> entityHits = new ArrayList<>();
        for (Entity target : level().getEntities(this, searchBox, this::canHitEntity)) {
            HitResult hit = Utils.checkEntityIntersecting(
                    target,
                    startPosition,
                    destination,
                    getHitDetectionInflation()
            );
            if (hit.getType() != HitResult.Type.MISS) {
                entityHits.add(hit);
            }
        }
        entityHits.sort(Comparator.comparingDouble(hit -> hit.getLocation().distanceToSqr(startPosition)));
        for (HitResult hitResult : entityHits) {
            if (hitResult instanceof EntityHitResult entityHitResult
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

    @Override
    protected void handleEntityHoming() {
        if (!hasLaunched() || level().isClientSide) {
            return;
        }
        int ticksSinceLaunch = tickCount - MagicGlintbladeSpell.HOVER_DURATION_TICKS;
        if (ticksSinceLaunch < MagicGlintbladeSpell.PROJECTILE_TRACKING_START_DELAY_TICKS) {
            return;
        }

        LivingEntity trackingTarget = resolveTrackingTarget();
        if (trackingTarget == null) {
            return;
        }

        Vec3 currentDeltaMovement = getDeltaMovement();
        double currentSpeed = currentDeltaMovement.length();
        if (currentSpeed < MagicGlintbladeSpell.PROJECTILE_MINIMUM_SPEED_FOR_HOMING) {
            return;
        }

        Vec3 currentFlightDirection = currentDeltaMovement.normalize();
        Vec3 vectorTowardTarget = aimPointOnTarget(trackingTarget).subtract(getBoundingBox().getCenter());
        double distanceTowardTarget = vectorTowardTarget.length();
        if (distanceTowardTarget < MagicGlintbladeSpell.PROJECTILE_MINIMUM_SPEED_FOR_HOMING) {
            return;
        }
        Vec3 desiredDirectionTowardTarget = vectorTowardTarget.scale(1.0 / distanceTowardTarget);

        double directionDotProduct = Mth.clamp(
                currentFlightDirection.dot(desiredDirectionTowardTarget),
                -1.0,
                1.0
        );
        double angleBetweenDirectionsRadians = Math.acos(directionDotProduct);
        double maxTurnAngleRadians = Math.toRadians(
                MagicGlintbladeSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK
        );
        float slerpInterpolationFactor = angleBetweenDirectionsRadians
                < MagicGlintbladeSpell.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS
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

    @Nullable
    private LivingEntity findLaunchTarget() {
        Entity ownerEntity = getOwner();
        Vec3 searchOrigin = getBoundingBox().getCenter();
        double trackingRange = MagicGlintbladeSpell.PROJECTILE_TRACKING_RANGE_BLOCKS;
        AABB searchBox = new AABB(searchOrigin, searchOrigin).inflate(trackingRange);
        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity candidate : level().getEntitiesOfClass(LivingEntity.class, searchBox, living ->
                canTrackLivingEntity(living, ownerEntity)
        )) {
            Vec3 towardCandidate = aimPointOnTarget(candidate).subtract(searchOrigin);
            if (towardCandidate.lengthSqr() < 1.0e-6) {
                continue;
            }
            double angleDegrees = Math.toDegrees(Math.acos(Mth.clamp(
                    storedLaunchDirection.dot(towardCandidate.normalize()),
                    -1.0,
                    1.0
            )));
            if (angleDegrees > MagicGlintbladeSpell.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES) {
                continue;
            }
            double score = angleDegrees * 8.0 + distanceTo(candidate);
            if (score < bestScore) {
                bestScore = score;
                bestTarget = candidate;
            }
        }
        return bestTarget;
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
        LivingEntity acquired = findLaunchTargetAlongFlight();
        if (acquired != null) {
            lockedTrackingTargetUuid = acquired.getUUID();
        }
        return acquired;
    }

    @Nullable
    private LivingEntity findLaunchTargetAlongFlight() {
        Entity ownerEntity = getOwner();
        Vec3 flightDirection = getDeltaMovement().lengthSqr() > 1.0e-8
                ? getDeltaMovement().normalize()
                : storedLaunchDirection;
        Vec3 searchOrigin = getBoundingBox().getCenter();
        double trackingRange = MagicGlintbladeSpell.PROJECTILE_TRACKING_RANGE_BLOCKS;
        AABB searchBox = new AABB(searchOrigin, searchOrigin).inflate(trackingRange);
        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity candidate : level().getEntitiesOfClass(LivingEntity.class, searchBox, living ->
                canTrackLivingEntity(living, ownerEntity)
        )) {
            Vec3 towardCandidate = aimPointOnTarget(candidate).subtract(searchOrigin);
            if (towardCandidate.lengthSqr() < 1.0e-6) {
                continue;
            }
            double angleDegrees = Math.toDegrees(Math.acos(Mth.clamp(
                    flightDirection.dot(towardCandidate.normalize()),
                    -1.0,
                    1.0
            )));
            if (angleDegrees > MagicGlintbladeSpell.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES) {
                continue;
            }
            double score = angleDegrees * 8.0 + distanceTo(candidate);
            if (score < bestScore) {
                bestScore = score;
                bestTarget = candidate;
            }
        }
        return bestTarget;
    }

    private boolean canTrackLivingEntity(LivingEntity candidateEntity, @Nullable Entity ownerEntity) {
        if (!candidateEntity.isAlive() || candidateEntity.isSpectator() || !candidateEntity.isPickable()) {
            return false;
        }
        if (ownerEntity != null
                && (candidateEntity == ownerEntity
                || ownerEntity.isAlliedTo(candidateEntity)
                || candidateEntity.isAlliedTo(ownerEntity))) {
            return false;
        }
        double trackingRange = MagicGlintbladeSpell.PROJECTILE_TRACKING_RANGE_BLOCKS;
        return distanceToSqr(candidateEntity) <= trackingRange * trackingRange;
    }

    private boolean canContinueTrackingLivingEntity(LivingEntity candidateEntity, @Nullable Entity ownerEntity) {
        return canTrackLivingEntity(candidateEntity, ownerEntity);
    }

    private static Vec3 aimPointOnTarget(LivingEntity target) {
        AABB box = target.getBoundingBox();
        return new Vec3(
                box.getCenter().x,
                Mth.lerp(MagicGlintbladeSpell.TRACKING_AIM_HEIGHT_FRACTION, box.minY, box.maxY),
                box.getCenter().z
        );
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity targetEntity) {
        if (targetEntity instanceof MagicGlintbladeEntity otherBlade) {
            Entity thisOwner = getOwner();
            Entity otherOwner = otherBlade.getOwner();
            if (thisOwner != null && thisOwner == otherOwner) {
                return false;
            }
        }
        return super.canHitEntity(targetEntity);
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        if (this.isRemoved()) {
            return;
        }
        super.onHitBlock(blockHitResult);
        discard();
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (!level().isClientSide) {
            DamageSources.applyDamage(
                    entityHitResult.getEntity(),
                    getDamage(),
                    damageSourceSpell().getDamageSource(this, getOwner())
            );
        }
        discard();
    }

    private AbstractSpell damageSourceSpell() {
        return ModSpells.MAGIC_GLINTBLADE.get();
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

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Launched", hasLaunched());
        tag.putDouble("LaunchX", storedLaunchDirection.x);
        tag.putDouble("LaunchY", storedLaunchDirection.y);
        tag.putDouble("LaunchZ", storedLaunchDirection.z);
        if (lockedTrackingTargetUuid != null) {
            tag.putUUID("LockedTarget", lockedTrackingTargetUuid);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_LAUNCHED, tag.getBoolean("Launched"));
        this.noPhysics = !hasLaunched();
        this.storedLaunchDirection = new Vec3(
                tag.getDouble("LaunchX"),
                tag.getDouble("LaunchY"),
                tag.getDouble("LaunchZ")
        );
        if (tag.hasUUID("LockedTarget")) {
            this.lockedTrackingTargetUuid = tag.getUUID("LockedTarget");
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}

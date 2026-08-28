package com.eldenring.spells.entity;

import com.eldenring.spells.particle.glintstone.GlintstoneFx;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.spell.SpiralShardSpell;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spiral Shard projectile: entity rides the spiral center with weak tracking;
 * two comets orbit with Euler phase difference pi.
 * <p>
 * Tracking only twists deltaMovement (axis). Radius/phase relation stays intact.
 * Piercing can hit entities repeatedly; block hit still discards.
 */
public class SpiralShardProjectile extends AbstractGlintstoneProjectile {

    /** Grace ticks after spawn: skip block impacts only, not entity hits. */
    private static final int COLLISION_GRACE_TICKS = 4;

    /** 两股螺旋彗星各自的客户端历史点，供 ribbon 画真实曲线。 */
    private final TrailHistoryBuffer[] clientCometTrailHistories = {
            new TrailHistoryBuffer(),
            new TrailHistoryBuffer()
    };

    public SpiralShardProjectile(
            EntityType<? extends SpiralShardProjectile> entityType,
            Level level
    ) {
        super(entityType, level);
        // pierceLevel = N - 1 => discard after N entity settlements
        setPierceLevel(Math.max(0, SpiralShardSpell.PROJECTILE_MAX_ENTITY_HITS - 1));
    }

    public SpiralShardProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.SPIRAL_SHARD.get(), level);
        setOwner(shooter);
    }

    /** Center ignores blocks; hits are sampled on both comet paths. */
    @Override
    public boolean collidesWithBlocks() {
        return false;
    }

    @Override
    protected float flightSpeed() {
        return SpiralShardSpell.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return SpiralShardSpell.PROJECTILE_TRACKING_RANGE_BLOCKS;
    }

    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return SpiralShardSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return SpiralShardSpell.PROJECTILE_TRACKING_START_DELAY_TICKS;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return SpiralShardSpell.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return SpiralShardSpell.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return SpiralShardSpell.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return SpiralShardSpell.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailStyle trailStyle() {
        return SpiralShardSpell.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return SpiralShardSpell.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.SPIRAL_SHARD.get();
    }

    @Override
    public GlintstoneVisualStyle visualStyle() {
        return GlintstoneVisualStyle.fromFloatColors(
                SpiralShardSpell.COMET_HEAD_BODY_SCALE,
                SpiralShardSpell.COMET_HEAD_GLOW_SCALE,
                SpiralShardSpell.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                SpiralShardSpell.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                SpiralShardSpell.COMET_HEAD_CORE_RED,
                SpiralShardSpell.COMET_HEAD_CORE_GREEN,
                SpiralShardSpell.COMET_HEAD_CORE_BLUE,
                SpiralShardSpell.COMET_HEAD_GLOW_RED,
                SpiralShardSpell.COMET_HEAD_GLOW_GREEN,
                SpiralShardSpell.COMET_HEAD_GLOW_BLUE,
                SpiralShardSpell.COMET_HEAD_GLOW_ALPHA
        );
    }

    /** Orbit phase theta in radians: theta = omega * t; comets use theta and theta+pi. */
    public float orbitPhaseRadians(float partialTicks) {
        float ageTicks = tickCount + partialTicks;
        return (float) Math.toRadians(SpiralShardSpell.SPIRAL_ANGULAR_SPEED_DEGREES_PER_TICK * ageTicks);
    }

    /** Current orbit radius in blocks, with short ramp after spawn. */
    public double currentOrbitRadiusBlocks(float partialTicks) {
        float ageTicks = tickCount + partialTicks;
        int rampTicks = SpiralShardSpell.SPIRAL_RADIUS_RAMP_TICKS;
        if (rampTicks <= 0) {
            return SpiralShardSpell.SPIRAL_ORBIT_RADIUS_BLOCKS;
        }
        float ramp01 = Mth.clamp(ageTicks / (float) rampTicks, 0.0f, 1.0f);
        return SpiralShardSpell.SPIRAL_ORBIT_RADIUS_BLOCKS * ramp01;
    }

    /**
     * World-space offset of cometIndex relative to center.
     *
     * @param cometIndex 0 or 1 (phase difference pi)
     */
    public Vec3 orbitWorldOffset(int cometIndex, float partialTicks) {
        Vec3 flightAxis = resolveFlightAxisDirection();
        OrthonormalFrame frame = buildOrthonormalFrame(flightAxis);
        float phaseRadians = orbitPhaseRadians(partialTicks) + (float) (cometIndex * Math.PI);
        double radiusBlocks = currentOrbitRadiusBlocks(partialTicks);
        double cosPhase = Math.cos(phaseRadians);
        double sinPhase = Math.sin(phaseRadians);
        return frame.rightAxis.scale(radiusBlocks * cosPhase)
                .add(frame.upAxis.scale(radiusBlocks * sinPhase));
    }

    /** World position of cometIndex (interpolated center + orbit offset). */
    public Vec3 orbitWorldPosition(int cometIndex, float partialTicks) {
        Vec3 interpolatedCenter = new Vec3(
                Mth.lerp(partialTicks, xo, getX()),
                Mth.lerp(partialTicks, yo, getY()),
                Mth.lerp(partialTicks, zo, getZ())
        );
        return interpolatedCenter.add(orbitWorldOffset(cometIndex, partialTicks));
    }

    /**
     * Instantaneous flight direction of cometIndex in world space (normalized).
     * Uses position delta between this and previous tick, including tangential spin.
     */
    public Vec3 orbitFlightDirection(int cometIndex, float partialTicks) {
        Vec3 currentPosition = orbitWorldPosition(cometIndex, partialTicks);
        float previousPartialTicks = partialTicks - 1.0f;
        Vec3 previousCenter = new Vec3(
                Mth.lerp(previousPartialTicks, xo, getX()),
                Mth.lerp(previousPartialTicks, yo, getY()),
                Mth.lerp(previousPartialTicks, zo, getZ())
        );
        Vec3 previousPosition = previousCenter.add(orbitWorldOffset(cometIndex, previousPartialTicks));
        Vec3 delta = currentPosition.subtract(previousPosition);
        if (delta.lengthSqr() < 1.0e-8) {
            Vec3 axisMotion = getDeltaMovement();
            if (axisMotion.lengthSqr() < 1.0e-8) {
                return new Vec3(0.0, 0.0, 1.0);
            }
            return axisMotion.normalize();
        }
        return delta.normalize();
    }

    /**
     * 某一股螺旋彗星的光轨历史点。
     *
     * @param cometIndex 0 或 1
     */
    public List<Vec3> cometTrailHistoryWorldPositions(int cometIndex) {
        if (cometIndex < 0 || cometIndex >= clientCometTrailHistories.length) {
            throw new IllegalArgumentException("cometIndex must be 0 or 1");
        }
        return clientCometTrailHistories[cometIndex].snapshot();
    }

    /**
     * Sparse head accents only; continuous beams are drawn by SpiralShardRenderer.
     */
    @Override
    public void trailParticles() {
        float intensity = trailParticleIntensity();
        GlintstoneTrailStyle trailStyle = trailStyle();
        for (int cometIndex = 0; cometIndex < 2; cometIndex++) {
            Vec3 cometPosition = position().add(orbitWorldOffset(cometIndex, 0.0f));
            Vec3 flightDirection = orbitFlightDirection(cometIndex, 0.0f);
            clientCometTrailHistories[cometIndex].record(
                    cometPosition,
                    trailStyle.lengthBlocks(),
                    trailStyle.maximumHistoryPointCount()
            );
            GlintstoneFx.trailAccents(
                    level(),
                    cometPosition.x,
                    cometPosition.y,
                    cometPosition.z,
                    flightDirection,
                    intensity,
                    trailStyle
            );
        }
    }

    @Override
    public float getHitDetectionInflation() {
        return SpiralShardSpell.PROJECTILE_HIT_INFLATION_BLOCKS;
    }

    /**
     * Dual-comet pierce hits ordered by distance from center.
     * Also counts start/end already inside target AABB to avoid miss-while-embedded.
     * Block-impact grace does not skip entity hits (melee-range targets must connect).
     */
    @Override
    public void handleHitDetection() {
        if (this.isRemoved()) {
            return;
        }
        boolean withinBlockCollisionGrace = tickCount <= COLLISION_GRACE_TICKS;

        Vec3 centerStart = position();
        Vec3 centerDelta = getDeltaMovement();
        Vec3 centerEnd = centerStart.add(centerDelta);
        Vec3 axisStart = resolveFlightAxisDirection();
        Vec3 axisEnd = centerDelta.lengthSqr() > 1.0e-8
                ? centerDelta.normalize()
                : axisStart;

        float phaseStart = orbitPhaseRadians(0.0f);
        float phaseEnd = orbitPhaseRadians(1.0f);
        double radiusStart = currentOrbitRadiusBlocks(0.0f);
        double radiusEnd = currentOrbitRadiusBlocks(1.0f);

        Map<Integer, EntityHitResult> nearestEntityHitsById = new HashMap<>();
        BlockHitResult nearestBlockHit = null;
        double nearestBlockDistanceSquared = Double.MAX_VALUE;

        for (int cometIndex = 0; cometIndex < 2; cometIndex++) {
            float phaseOffset = (float) (cometIndex * Math.PI);
            Vec3 pathStart = centerStart.add(
                    eulerOrbitOffset(axisStart, phaseStart + phaseOffset, radiusStart));
            Vec3 pathEnd = centerEnd.add(
                    eulerOrbitOffset(axisEnd, phaseEnd + phaseOffset, radiusEnd));

            BlockHitResult blockHit = level().clip(new ClipContext(
                    pathStart,
                    pathEnd,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    this
            ));
            Vec3 entityRayEnd = pathEnd;
            if (blockHit.getType() != HitResult.Type.MISS) {
                entityRayEnd = blockHit.getLocation();
                double blockDistanceSquared = blockHit.getLocation().distanceToSqr(centerStart);
                if (blockDistanceSquared < nearestBlockDistanceSquared) {
                    nearestBlockDistanceSquared = blockDistanceSquared;
                    nearestBlockHit = blockHit;
                }
            }

            collectCometEntityHits(pathStart, entityRayEnd, centerStart, nearestEntityHitsById);
        }

        List<EntityHitResult> orderedEntityHits = new ArrayList<>(nearestEntityHitsById.values());
        orderedEntityHits.sort(Comparator.comparingDouble(
                hit -> hit.getLocation().distanceToSqr(centerStart)));

        for (EntityHitResult entityHit : orderedEntityHits) {
            if (this.isRemoved()) {
                return;
            }
            double entityDistanceSquared = entityHit.getLocation().distanceToSqr(centerStart);
            if (nearestBlockHit != null && nearestBlockDistanceSquared <= entityDistanceSquared) {
                break;
            }
            if (!NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(this, entityHit)).isCanceled()) {
                onHit(entityHit);
            }
        }

        if (!withinBlockCollisionGrace
                && !this.isRemoved()
                && nearestBlockHit != null
                && nearestBlockHit.getType() != HitResult.Type.MISS
                && !NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(this, nearestBlockHit)).isCanceled()) {
            onHit(nearestBlockHit);
        }
    }

    /**
     * Clear invulnerability frames then apply damage and consume pierce.
     */
    @Override
    protected void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        Entity hitEntity = entityHitResult.getEntity();
        if (!level().isClientSide) {
            if (hitEntity instanceof LivingEntity livingEntity) {
                livingEntity.invulnerableTime = 0;
                livingEntity.hurtTime = 0;
            }
            DamageSources.applyDamage(
                    hitEntity,
                    damage,
                    damageSourceSpell().getDamageSource(this, getOwner())
            );
            if (hitEntity instanceof LivingEntity livingEntityAfterHit) {
                livingEntityAfterHit.invulnerableTime = 0;
            }
        }
        consumeEntityImpact(entityHitResult, true);
    }

    /**
     * Collect hits on one comet arm: containment + segment clip, with ISS fallback ray.
     */
    private void collectCometEntityHits(
            Vec3 pathStart,
            Vec3 pathEnd,
            Vec3 centerStart,
            Map<Integer, EntityHitResult> nearestEntityHitsById
    ) {
        float inflation = getHitDetectionInflation();
        AABB searchBox = new AABB(pathStart, pathEnd).inflate(0.35 + inflation);

        for (Entity target : level().getEntities(this, searchBox, this::canHitEntity)) {
            AABB inflatedBox = target.getBoundingBox().inflate(inflation);
            Vec3 hitLocation = null;

            if (inflatedBox.contains(pathStart)) {
                hitLocation = pathStart;
            } else if (inflatedBox.contains(pathEnd)) {
                hitLocation = pathEnd;
            } else {
                var clipPoint = inflatedBox.clip(pathStart, pathEnd);
                if (clipPoint.isPresent()) {
                    hitLocation = clipPoint.get();
                }
            }

            if (hitLocation == null) {
                HitResult toolHit = Utils.checkEntityIntersecting(
                        target,
                        pathStart,
                        pathEnd,
                        inflation
                );
                if (toolHit instanceof EntityHitResult entityHitResult
                        && toolHit.getType() != HitResult.Type.MISS) {
                    hitLocation = entityHitResult.getLocation();
                }
            }

            if (hitLocation == null) {
                continue;
            }

            int entityId = target.getId();
            EntityHitResult candidate = new EntityHitResult(target, hitLocation);
            EntityHitResult existingHit = nearestEntityHitsById.get(entityId);
            if (existingHit == null
                    || hitLocation.distanceToSqr(centerStart)
                    < existingHit.getLocation().distanceToSqr(centerStart)) {
                nearestEntityHitsById.put(entityId, candidate);
            }
        }
    }

    private Vec3 resolveFlightAxisDirection() {
        Vec3 deltaMovement = getDeltaMovement();
        if (deltaMovement.lengthSqr() > 1.0e-8) {
            return deltaMovement.normalize();
        }
        return getLookAngle().normalize();
    }

    /** Euler plane offset: radius * (cos, sin) on orthonormal (right, up). */
    private static Vec3 eulerOrbitOffset(Vec3 flightAxis, float phaseRadians, double radiusBlocks) {
        OrthonormalFrame frame = buildOrthonormalFrame(flightAxis);
        double cosPhase = Math.cos(phaseRadians);
        double sinPhase = Math.sin(phaseRadians);
        return frame.rightAxis.scale(radiusBlocks * cosPhase)
                .add(frame.upAxis.scale(radiusBlocks * sinPhase));
    }

    /** Stable right-handed frame for circular orbit around flight axis. */
    private static OrthonormalFrame buildOrthonormalFrame(Vec3 flightAxis) {
        Vec3 normalizedAxis = flightAxis.lengthSqr() > 1.0e-8
                ? flightAxis.normalize()
                : new Vec3(0.0, 0.0, 1.0);

        Vec3 referenceUp = Math.abs(normalizedAxis.y) > 0.92
                ? new Vec3(1.0, 0.0, 0.0)
                : new Vec3(0.0, 1.0, 0.0);
        Vec3 rightAxis = normalizedAxis.cross(referenceUp);
        if (rightAxis.lengthSqr() < 1.0e-8) {
            rightAxis = normalizedAxis.cross(new Vec3(0.0, 0.0, 1.0));
        }
        rightAxis = rightAxis.normalize();
        Vec3 upAxis = rightAxis.cross(normalizedAxis).normalize();
        return new OrthonormalFrame(rightAxis, upAxis);
    }

    private record OrthonormalFrame(Vec3 rightAxis, Vec3 upAxis) {
    }
}

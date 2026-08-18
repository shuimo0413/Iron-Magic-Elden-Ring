package com.eldenring.spells.entity;

import com.eldenring.spells.client.render.glintstone.GlintstoneCometHeadDrawer;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.tuning.CometTuning;
import com.eldenring.spells.tuning.GlintstoneTrailTuning;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * ?????????? + ????????????????????????????
 */
public class CometProjectile extends AbstractGlintstoneProjectile {

    public CometProjectile(EntityType<? extends CometProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public CometProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.COMET.get(), level);
        setOwner(shooter);
        setExplosionRadius(CometTuning.EXPLOSION_RADIUS_BLOCKS);
    }

    @Override
    protected float explosionRadiusBlocks() {
        return CometTuning.EXPLOSION_RADIUS_BLOCKS;
    }

    @Override
    protected float flightSpeed() {
        return CometTuning.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return CometTuning.PROJECTILE_TRACKING_RANGE_BLOCKS;
    }

    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return CometTuning.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return CometTuning.PROJECTILE_TRACKING_START_DELAY_TICKS;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return CometTuning.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return CometTuning.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return CometTuning.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return CometTuning.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailTuning.TrailStyle trailStyle() {
        return CometTuning.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return CometTuning.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.COMET.get();
    }

    @Override
    public GlintstoneCometHeadDrawer.VisualStyle visualStyle() {
        return GlintstoneCometHeadDrawer.VisualStyle.spikedCluster(
                CometTuning.COMET_HEAD_BODY_SCALE,
                CometTuning.COMET_HEAD_GLOW_SCALE,
                CometTuning.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                CometTuning.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                CometTuning.COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE,
                CometTuning.CLUSTER_SPIN_DEGREES_PER_TICK,
                CometTuning.COMET_HEAD_CORE_RED,
                CometTuning.COMET_HEAD_CORE_GREEN,
                CometTuning.COMET_HEAD_CORE_BLUE,
                CometTuning.COMET_HEAD_SPIKE_RED,
                CometTuning.COMET_HEAD_SPIKE_GREEN,
                CometTuning.COMET_HEAD_SPIKE_BLUE,
                CometTuning.COMET_HEAD_GLOW_RED,
                CometTuning.COMET_HEAD_GLOW_GREEN,
                CometTuning.COMET_HEAD_GLOW_BLUE,
                CometTuning.COMET_HEAD_GLOW_ALPHA
        );
    }
}

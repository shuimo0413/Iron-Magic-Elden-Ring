package com.eldenring.spells.entity;

import com.eldenring.spells.client.render.glintstone.GlintstoneCometHeadDrawer;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.tuning.GlintstoneCometTuning;
import com.eldenring.spells.tuning.GlintstoneTrailTuning;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * ?????????/?????????????
 */
public class GlintstoneCometProjectile extends AbstractGlintstoneProjectile {

    public GlintstoneCometProjectile(
            EntityType<? extends GlintstoneCometProjectile> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    public GlintstoneCometProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.GLINTSTONE_COMET.get(), level);
        setOwner(shooter);
        setExplosionRadius(GlintstoneCometTuning.EXPLOSION_RADIUS_BLOCKS);
    }

    @Override
    protected float explosionRadiusBlocks() {
        return GlintstoneCometTuning.EXPLOSION_RADIUS_BLOCKS;
    }

    @Override
    protected float flightSpeed() {
        return GlintstoneCometTuning.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return GlintstoneCometTuning.PROJECTILE_TRACKING_RANGE_BLOCKS;
    }

    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return GlintstoneCometTuning.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return GlintstoneCometTuning.PROJECTILE_TRACKING_START_DELAY_TICKS;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return GlintstoneCometTuning.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return GlintstoneCometTuning.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return GlintstoneCometTuning.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return GlintstoneCometTuning.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailTuning.TrailStyle trailStyle() {
        return GlintstoneCometTuning.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return GlintstoneCometTuning.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.GLINTSTONE_COMET.get();
    }

    @Override
    public GlintstoneCometHeadDrawer.VisualStyle visualStyle() {
        return GlintstoneCometHeadDrawer.VisualStyle.fromFloatColors(
                GlintstoneCometTuning.COMET_HEAD_BODY_SCALE,
                GlintstoneCometTuning.COMET_HEAD_GLOW_SCALE,
                GlintstoneCometTuning.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                GlintstoneCometTuning.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                GlintstoneCometTuning.COMET_HEAD_CORE_RED,
                GlintstoneCometTuning.COMET_HEAD_CORE_GREEN,
                GlintstoneCometTuning.COMET_HEAD_CORE_BLUE,
                GlintstoneCometTuning.COMET_HEAD_GLOW_RED,
                GlintstoneCometTuning.COMET_HEAD_GLOW_GREEN,
                GlintstoneCometTuning.COMET_HEAD_GLOW_BLUE,
                GlintstoneCometTuning.COMET_HEAD_GLOW_ALPHA
        );
    }
}

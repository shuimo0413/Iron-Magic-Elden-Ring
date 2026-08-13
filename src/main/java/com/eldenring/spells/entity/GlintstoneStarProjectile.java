package com.eldenring.spells.entity;

import com.eldenring.spells.client.render.glintstone.GlintstoneCometHeadDrawer;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.tuning.GlintstoneStarsTuning;
import com.eldenring.spells.tuning.GlintstoneTrailTuning;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * ????????????????????????????????????
 * <p>
 * ????? {@link GlintstoneStarsTuning}?
 */
public class GlintstoneStarProjectile extends AbstractGlintstoneProjectile {

    public GlintstoneStarProjectile(
            EntityType<? extends GlintstoneStarProjectile> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    public GlintstoneStarProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.GLINTSTONE_STAR.get(), level);
        setOwner(shooter);
    }

    @Override
    protected float flightSpeed() {
        return GlintstoneStarsTuning.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return GlintstoneStarsTuning.PROJECTILE_TRACKING_RANGE_BLOCKS;
    }

    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return GlintstoneStarsTuning.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return GlintstoneStarsTuning.PROJECTILE_TRACKING_START_DELAY_TICKS;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return GlintstoneStarsTuning.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return GlintstoneStarsTuning.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return GlintstoneStarsTuning.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return GlintstoneStarsTuning.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailTuning.TrailStyle trailStyle() {
        return GlintstoneStarsTuning.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return GlintstoneStarsTuning.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.GLINTSTONE_STARS.get();
    }

    @Override
    public GlintstoneCometHeadDrawer.VisualStyle visualStyle() {
        return GlintstoneCometHeadDrawer.VisualStyle.fromFloatColors(
                GlintstoneStarsTuning.COMET_HEAD_BODY_SCALE,
                GlintstoneStarsTuning.COMET_HEAD_GLOW_SCALE,
                GlintstoneStarsTuning.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                GlintstoneStarsTuning.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                GlintstoneStarsTuning.COMET_HEAD_CORE_RED,
                GlintstoneStarsTuning.COMET_HEAD_CORE_GREEN,
                GlintstoneStarsTuning.COMET_HEAD_CORE_BLUE,
                GlintstoneStarsTuning.COMET_HEAD_GLOW_RED,
                GlintstoneStarsTuning.COMET_HEAD_GLOW_GREEN,
                GlintstoneStarsTuning.COMET_HEAD_GLOW_BLUE,
                GlintstoneStarsTuning.COMET_HEAD_GLOW_ALPHA
        );
    }
}

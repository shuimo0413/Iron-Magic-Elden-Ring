package com.eldenring.spells.entity;

import com.eldenring.spells.client.render.glintstone.GlintstoneCometHeadDrawer;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.tuning.GlintstonePebbleTuning;
import com.eldenring.spells.tuning.GlintstoneTrailTuning;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * ?????????
 * <p>
 * ?????????????? {@link com.eldenring.spells.particle.glintstone.GlintstoneFx}?
 * ????????????????? tick ???????????????????
 * ????? {@link GlintstonePebbleTuning}?
 */
public class GlintstonePebbleProjectile extends AbstractGlintstoneProjectile {

    public GlintstonePebbleProjectile(EntityType<? extends GlintstonePebbleProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public GlintstonePebbleProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.GLINTSTONE_PEBBLE.get(), level);
        setOwner(shooter);
    }

    @Override
    protected float flightSpeed() {
        return GlintstonePebbleTuning.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return GlintstonePebbleTuning.PROJECTILE_TRACKING_RANGE_BLOCKS;
    }

    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return GlintstonePebbleTuning.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return GlintstonePebbleTuning.PROJECTILE_TRACKING_START_DELAY_TICKS;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return GlintstonePebbleTuning.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return GlintstonePebbleTuning.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return GlintstonePebbleTuning.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return GlintstonePebbleTuning.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailTuning.TrailStyle trailStyle() {
        return GlintstonePebbleTuning.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return GlintstonePebbleTuning.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.GLINTSTONE_PEBBLE.get();
    }

    @Override
    public GlintstoneCometHeadDrawer.VisualStyle visualStyle() {
        return GlintstoneCometHeadDrawer.VisualStyle.fromFloatColors(
                GlintstonePebbleTuning.COMET_HEAD_BODY_SCALE,
                GlintstonePebbleTuning.COMET_HEAD_GLOW_SCALE,
                GlintstonePebbleTuning.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                GlintstonePebbleTuning.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                GlintstonePebbleTuning.COMET_HEAD_CORE_RED,
                GlintstonePebbleTuning.COMET_HEAD_CORE_GREEN,
                GlintstonePebbleTuning.COMET_HEAD_CORE_BLUE,
                GlintstonePebbleTuning.COMET_HEAD_GLOW_RED,
                GlintstonePebbleTuning.COMET_HEAD_GLOW_GREEN,
                GlintstonePebbleTuning.COMET_HEAD_GLOW_BLUE,
                GlintstonePebbleTuning.COMET_HEAD_GLOW_ALPHA
        );
    }
}

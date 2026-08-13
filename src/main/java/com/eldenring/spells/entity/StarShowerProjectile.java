package com.eldenring.spells.entity;

import com.eldenring.spells.client.render.glintstone.GlintstoneCometHeadDrawer;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.tuning.GlintstoneTrailTuning;
import com.eldenring.spells.tuning.StarShowerTuning;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * 流星雨单发弹道：六连发中的一颗，数值见 {@link StarShowerTuning}。
 */
public class StarShowerProjectile extends AbstractGlintstoneProjectile {

    public StarShowerProjectile(
            EntityType<? extends StarShowerProjectile> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    public StarShowerProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.STAR_SHOWER.get(), level);
        setOwner(shooter);
    }

    @Override
    protected float flightSpeed() {
        return StarShowerTuning.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return StarShowerTuning.PROJECTILE_TRACKING_RANGE_BLOCKS;
    }

    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return StarShowerTuning.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return StarShowerTuning.PROJECTILE_TRACKING_START_DELAY_TICKS;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return StarShowerTuning.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return StarShowerTuning.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return StarShowerTuning.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return StarShowerTuning.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailTuning.TrailStyle trailStyle() {
        return StarShowerTuning.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return StarShowerTuning.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.STAR_SHOWER.get();
    }

    @Override
    public GlintstoneCometHeadDrawer.VisualStyle visualStyle() {
        return GlintstoneCometHeadDrawer.VisualStyle.fromFloatColors(
                StarShowerTuning.COMET_HEAD_BODY_SCALE,
                StarShowerTuning.COMET_HEAD_GLOW_SCALE,
                StarShowerTuning.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                StarShowerTuning.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                StarShowerTuning.COMET_HEAD_CORE_RED,
                StarShowerTuning.COMET_HEAD_CORE_GREEN,
                StarShowerTuning.COMET_HEAD_CORE_BLUE,
                StarShowerTuning.COMET_HEAD_GLOW_RED,
                StarShowerTuning.COMET_HEAD_GLOW_GREEN,
                StarShowerTuning.COMET_HEAD_GLOW_BLUE,
                StarShowerTuning.COMET_HEAD_GLOW_ALPHA
        );
    }
}

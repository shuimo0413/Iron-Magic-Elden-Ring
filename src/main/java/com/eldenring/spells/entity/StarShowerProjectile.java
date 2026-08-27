package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import com.eldenring.spells.spell.StarShowerSpell;

/**
 * 流星雨单发弹道：六连发中的一颗，数值见 {@link StarShowerSpell}。
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
        return StarShowerSpell.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return StarShowerSpell.PROJECTILE_TRACKING_RANGE_BLOCKS;
    }

    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return StarShowerSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return StarShowerSpell.PROJECTILE_TRACKING_START_DELAY_TICKS;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return StarShowerSpell.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return StarShowerSpell.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return StarShowerSpell.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return StarShowerSpell.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailStyle trailStyle() {
        return StarShowerSpell.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return StarShowerSpell.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.STAR_SHOWER.get();
    }

    @Override
    public GlintstoneVisualStyle visualStyle() {
        return GlintstoneVisualStyle.fromFloatColors(
                StarShowerSpell.COMET_HEAD_BODY_SCALE,
                StarShowerSpell.COMET_HEAD_GLOW_SCALE,
                StarShowerSpell.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                StarShowerSpell.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                StarShowerSpell.COMET_HEAD_CORE_RED,
                StarShowerSpell.COMET_HEAD_CORE_GREEN,
                StarShowerSpell.COMET_HEAD_CORE_BLUE,
                StarShowerSpell.COMET_HEAD_GLOW_RED,
                StarShowerSpell.COMET_HEAD_GLOW_GREEN,
                StarShowerSpell.COMET_HEAD_GLOW_BLUE,
                StarShowerSpell.COMET_HEAD_GLOW_ALPHA
        );
    }
}

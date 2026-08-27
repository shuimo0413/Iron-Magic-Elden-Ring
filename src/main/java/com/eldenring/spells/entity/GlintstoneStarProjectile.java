package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import com.eldenring.spells.spell.GlintstoneStarsSpell;

/**
 * 辉石流星单发弹道：三连发中的一颗。
 * <p>
 * 玩法数字读 {@link GlintstoneStarsSpell}。
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
        return GlintstoneStarsSpell.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return GlintstoneStarsSpell.PROJECTILE_TRACKING_RANGE_BLOCKS;
    }

    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return GlintstoneStarsSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return GlintstoneStarsSpell.PROJECTILE_TRACKING_START_DELAY_TICKS;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return GlintstoneStarsSpell.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return GlintstoneStarsSpell.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return GlintstoneStarsSpell.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return GlintstoneStarsSpell.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailStyle trailStyle() {
        return GlintstoneStarsSpell.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return GlintstoneStarsSpell.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.GLINTSTONE_STARS.get();
    }

    @Override
    public GlintstoneVisualStyle visualStyle() {
        return GlintstoneVisualStyle.fromFloatColors(
                GlintstoneStarsSpell.COMET_HEAD_BODY_SCALE,
                GlintstoneStarsSpell.COMET_HEAD_GLOW_SCALE,
                GlintstoneStarsSpell.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                GlintstoneStarsSpell.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                GlintstoneStarsSpell.COMET_HEAD_CORE_RED,
                GlintstoneStarsSpell.COMET_HEAD_CORE_GREEN,
                GlintstoneStarsSpell.COMET_HEAD_CORE_BLUE,
                GlintstoneStarsSpell.COMET_HEAD_GLOW_RED,
                GlintstoneStarsSpell.COMET_HEAD_GLOW_GREEN,
                GlintstoneStarsSpell.COMET_HEAD_GLOW_BLUE,
                GlintstoneStarsSpell.COMET_HEAD_GLOW_ALPHA
        );
    }
}

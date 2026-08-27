package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import com.eldenring.spells.spell.GlintstoneCometSpell;

/**
 * 辉石彗星弹道：介于大魔砾与帚星之间。玩法数字读 {@link GlintstoneCometSpell}。
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
        setExplosionRadius(GlintstoneCometSpell.EXPLOSION_RADIUS_BLOCKS);
    }

    @Override
    protected float explosionRadiusBlocks() {
        return GlintstoneCometSpell.EXPLOSION_RADIUS_BLOCKS;
    }

    @Override
    protected float flightSpeed() {
        return GlintstoneCometSpell.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return GlintstoneCometSpell.PROJECTILE_TRACKING_RANGE_BLOCKS;
    }

    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return GlintstoneCometSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return GlintstoneCometSpell.PROJECTILE_TRACKING_START_DELAY_TICKS;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return GlintstoneCometSpell.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return GlintstoneCometSpell.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return GlintstoneCometSpell.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return GlintstoneCometSpell.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailStyle trailStyle() {
        return GlintstoneCometSpell.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return GlintstoneCometSpell.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.GLINTSTONE_COMET.get();
    }

    @Override
    public GlintstoneVisualStyle visualStyle() {
        return GlintstoneVisualStyle.anisotropic(
                GlintstoneCometSpell.COMET_HEAD_BODY_SCALE_RADIAL,
                GlintstoneCometSpell.COMET_HEAD_BODY_SCALE_ALONG,
                GlintstoneCometSpell.COMET_HEAD_GLOW_SCALE,
                GlintstoneCometSpell.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                GlintstoneCometSpell.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                GlintstoneCometSpell.COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE,
                GlintstoneCometSpell.COMET_HEAD_CORE_RED,
                GlintstoneCometSpell.COMET_HEAD_CORE_GREEN,
                GlintstoneCometSpell.COMET_HEAD_CORE_BLUE,
                GlintstoneCometSpell.COMET_HEAD_GLOW_RED,
                GlintstoneCometSpell.COMET_HEAD_GLOW_GREEN,
                GlintstoneCometSpell.COMET_HEAD_GLOW_BLUE,
                GlintstoneCometSpell.COMET_HEAD_GLOW_ALPHA
        );
    }
}

package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import com.eldenring.spells.spell.CometSpell;

/**
 * 帚星弹道：更大的彗星头，命中爆炸。玩法数字读 {@link CometSpell}。
 */
public class CometProjectile extends AbstractGlintstoneProjectile {

    public CometProjectile(EntityType<? extends CometProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public CometProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.COMET.get(), level);
        setOwner(shooter);
        setExplosionRadius(CometSpell.EXPLOSION_RADIUS_BLOCKS);
    }

    @Override
    protected float explosionRadiusBlocks() {
        return CometSpell.EXPLOSION_RADIUS_BLOCKS;
    }

    @Override
    protected float flightSpeed() {
        return CometSpell.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return CometSpell.PROJECTILE_TRACKING_RANGE_BLOCKS;
    }

    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return CometSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return CometSpell.PROJECTILE_TRACKING_START_DELAY_TICKS;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return CometSpell.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return CometSpell.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return CometSpell.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return CometSpell.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailStyle trailStyle() {
        return CometSpell.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return CometSpell.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.COMET.get();
    }

    @Override
    public GlintstoneVisualStyle visualStyle() {
        return GlintstoneVisualStyle.spikedCluster(
                CometSpell.COMET_HEAD_BODY_SCALE,
                CometSpell.COMET_HEAD_GLOW_SCALE,
                CometSpell.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                CometSpell.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                CometSpell.COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE,
                CometSpell.CLUSTER_SPIN_DEGREES_PER_TICK,
                CometSpell.COMET_HEAD_CORE_RED,
                CometSpell.COMET_HEAD_CORE_GREEN,
                CometSpell.COMET_HEAD_CORE_BLUE,
                CometSpell.COMET_HEAD_SPIKE_RED,
                CometSpell.COMET_HEAD_SPIKE_GREEN,
                CometSpell.COMET_HEAD_SPIKE_BLUE,
                CometSpell.COMET_HEAD_GLOW_RED,
                CometSpell.COMET_HEAD_GLOW_GREEN,
                CometSpell.COMET_HEAD_GLOW_BLUE,
                CometSpell.COMET_HEAD_GLOW_ALPHA
        );
    }
}

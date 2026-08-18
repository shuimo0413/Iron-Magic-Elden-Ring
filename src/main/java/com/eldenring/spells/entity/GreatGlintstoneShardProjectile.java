package com.eldenring.spells.entity;

import com.eldenring.spells.client.render.glintstone.GlintstoneCometHeadDrawer;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.tuning.GlintstoneTrailTuning;
import com.eldenring.spells.tuning.GreatGlintstoneShardTuning;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * ?????????????????????????
 */
public class GreatGlintstoneShardProjectile extends AbstractGlintstoneProjectile {

    public GreatGlintstoneShardProjectile(
            EntityType<? extends GreatGlintstoneShardProjectile> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    public GreatGlintstoneShardProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.GREAT_GLINTSTONE_SHARD.get(), level);
        setOwner(shooter);
        setExplosionRadius(GreatGlintstoneShardTuning.EXPLOSION_RADIUS_BLOCKS);
    }

    @Override
    protected float explosionRadiusBlocks() {
        return GreatGlintstoneShardTuning.EXPLOSION_RADIUS_BLOCKS;
    }

    @Override
    protected float flightSpeed() {
        return GreatGlintstoneShardTuning.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return GreatGlintstoneShardTuning.PROJECTILE_TRACKING_RANGE_BLOCKS;
    }

    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return GreatGlintstoneShardTuning.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return GreatGlintstoneShardTuning.PROJECTILE_TRACKING_START_DELAY_TICKS;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return GreatGlintstoneShardTuning.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return GreatGlintstoneShardTuning.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return GreatGlintstoneShardTuning.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return GreatGlintstoneShardTuning.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailTuning.TrailStyle trailStyle() {
        return GreatGlintstoneShardTuning.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return GreatGlintstoneShardTuning.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.GREAT_GLINTSTONE_SHARD.get();
    }

    @Override
    public GlintstoneCometHeadDrawer.VisualStyle visualStyle() {
        return GlintstoneCometHeadDrawer.VisualStyle.anisotropic(
                GreatGlintstoneShardTuning.COMET_HEAD_BODY_SCALE_RADIAL,
                GreatGlintstoneShardTuning.COMET_HEAD_BODY_SCALE_ALONG,
                GreatGlintstoneShardTuning.COMET_HEAD_GLOW_SCALE,
                GreatGlintstoneShardTuning.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                GreatGlintstoneShardTuning.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                GreatGlintstoneShardTuning.COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE,
                GreatGlintstoneShardTuning.COMET_HEAD_CORE_RED,
                GreatGlintstoneShardTuning.COMET_HEAD_CORE_GREEN,
                GreatGlintstoneShardTuning.COMET_HEAD_CORE_BLUE,
                GreatGlintstoneShardTuning.COMET_HEAD_GLOW_RED,
                GreatGlintstoneShardTuning.COMET_HEAD_GLOW_GREEN,
                GreatGlintstoneShardTuning.COMET_HEAD_GLOW_BLUE,
                GreatGlintstoneShardTuning.COMET_HEAD_GLOW_ALPHA
        );
    }
}

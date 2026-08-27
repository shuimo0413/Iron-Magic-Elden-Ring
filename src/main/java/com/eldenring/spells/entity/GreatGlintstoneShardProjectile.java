package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import com.eldenring.spells.spell.GreatGlintstoneShardSpell;

/**
 * 辉石大魔砾弹道：命中爆炸的重型单发。玩法数字读 {@link GreatGlintstoneShardSpell}。
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
        setExplosionRadius(GreatGlintstoneShardSpell.EXPLOSION_RADIUS_BLOCKS);
    }

    @Override
    protected float explosionRadiusBlocks() {
        return GreatGlintstoneShardSpell.EXPLOSION_RADIUS_BLOCKS;
    }

    @Override
    protected float flightSpeed() {
        return GreatGlintstoneShardSpell.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return GreatGlintstoneShardSpell.PROJECTILE_TRACKING_RANGE_BLOCKS;
    }

    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return GreatGlintstoneShardSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return GreatGlintstoneShardSpell.PROJECTILE_TRACKING_START_DELAY_TICKS;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return GreatGlintstoneShardSpell.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return GreatGlintstoneShardSpell.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return GreatGlintstoneShardSpell.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return GreatGlintstoneShardSpell.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailStyle trailStyle() {
        return GreatGlintstoneShardSpell.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return GreatGlintstoneShardSpell.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.GREAT_GLINTSTONE_SHARD.get();
    }

    @Override
    public GlintstoneVisualStyle visualStyle() {
        return GlintstoneVisualStyle.anisotropic(
                GreatGlintstoneShardSpell.COMET_HEAD_BODY_SCALE_RADIAL,
                GreatGlintstoneShardSpell.COMET_HEAD_BODY_SCALE_ALONG,
                GreatGlintstoneShardSpell.COMET_HEAD_GLOW_SCALE,
                GreatGlintstoneShardSpell.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                GreatGlintstoneShardSpell.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                GreatGlintstoneShardSpell.COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE,
                GreatGlintstoneShardSpell.COMET_HEAD_CORE_RED,
                GreatGlintstoneShardSpell.COMET_HEAD_CORE_GREEN,
                GreatGlintstoneShardSpell.COMET_HEAD_CORE_BLUE,
                GreatGlintstoneShardSpell.COMET_HEAD_GLOW_RED,
                GreatGlintstoneShardSpell.COMET_HEAD_GLOW_GREEN,
                GreatGlintstoneShardSpell.COMET_HEAD_GLOW_BLUE,
                GreatGlintstoneShardSpell.COMET_HEAD_GLOW_ALPHA
        );
    }
}

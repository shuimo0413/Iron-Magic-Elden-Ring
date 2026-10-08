package com.eldenring.spells.entity;

import com.eldenring.spells.particle.frost.FrostFx;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.spell.GlintstoneIcecragSpell;
import com.eldenring.spells.spell.helper.FrostHelper;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 辉石冰块弹道：大魔砾同体型，冷白彗星头，拖尾/命中改冰霜粒子，命中后原版结霜（不冻进冰牢）。
 */
public class GlintstoneIcecragProjectile extends AbstractGlintstoneProjectile {

    public GlintstoneIcecragProjectile(
            EntityType<? extends GlintstoneIcecragProjectile> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    public GlintstoneIcecragProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.GLINTSTONE_ICECRAG.get(), level);
        setOwner(shooter);
        setExplosionRadius(GlintstoneIcecragSpell.EXPLOSION_RADIUS_BLOCKS);
    }

    @Override
    protected float explosionRadiusBlocks() {
        return GlintstoneIcecragSpell.EXPLOSION_RADIUS_BLOCKS;
    }

    @Override
    protected float flightSpeed() {
        return GlintstoneIcecragSpell.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return GlintstoneIcecragSpell.PROJECTILE_TRACKING_RANGE_BLOCKS;
    }

    @Override
    protected double maxRangeBlocks() {
        return GlintstoneIcecragSpell.PROJECTILE_MAX_RANGE_BLOCKS;
    }


    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return GlintstoneIcecragSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return GlintstoneIcecragSpell.PROJECTILE_TRACKING_START_DELAY_TICKS;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return GlintstoneIcecragSpell.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return GlintstoneIcecragSpell.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return GlintstoneIcecragSpell.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return GlintstoneIcecragSpell.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailStyle trailStyle() {
        return GlintstoneIcecragSpell.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return GlintstoneIcecragSpell.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.GLINTSTONE_ICECRAG.get();
    }

    @Override
    protected void spawnTrailAccentParticles(Vec3 deltaMovement, GlintstoneTrailStyle trailStyle) {
        FrostFx.trailAccents(
                level(),
                getX(),
                getY(),
                getZ(),
                deltaMovement,
                trailParticleIntensity(),
                trailStyle
        );
    }

    @Override
    protected void spawnImpactParticles(double impactX, double impactY, double impactZ) {
        FrostFx.impact(level(), impactX, impactY, impactZ, impactParticleIntensity());
    }

    @Override
    protected void afterDamagingTarget(LivingEntity livingTarget) {
        FrostHelper.applyFrost(livingTarget, GlintstoneIcecragSpell.SPELL_FROST_SECONDS);
    }

    @Override
    public GlintstoneVisualStyle visualStyle() {
        return GlintstoneVisualStyle.anisotropic(
                GlintstoneIcecragSpell.COMET_HEAD_BODY_SCALE_RADIAL,
                GlintstoneIcecragSpell.COMET_HEAD_BODY_SCALE_ALONG,
                GlintstoneIcecragSpell.COMET_HEAD_GLOW_SCALE,
                GlintstoneIcecragSpell.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                GlintstoneIcecragSpell.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                GlintstoneIcecragSpell.COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE,
                GlintstoneIcecragSpell.COMET_HEAD_CORE_RED,
                GlintstoneIcecragSpell.COMET_HEAD_CORE_GREEN,
                GlintstoneIcecragSpell.COMET_HEAD_CORE_BLUE,
                GlintstoneIcecragSpell.COMET_HEAD_GLOW_RED,
                GlintstoneIcecragSpell.COMET_HEAD_GLOW_GREEN,
                GlintstoneIcecragSpell.COMET_HEAD_GLOW_BLUE,
                GlintstoneIcecragSpell.COMET_HEAD_GLOW_ALPHA
        );
    }
}

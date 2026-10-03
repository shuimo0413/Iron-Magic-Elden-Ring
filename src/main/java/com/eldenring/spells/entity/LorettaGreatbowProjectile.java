package com.eldenring.spells.entity;

import com.eldenring.spells.particle.carian.CarianFx;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.spell.LorettaGreatbowSpell;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 罗蕾塔的大弓弹道：高速追踪的蓝色彗星箭。玩法数字读 {@link LorettaGreatbowSpell}。
 * <p>
 * 弹头 / 光轨与辉石彗星同一套渲染；拖尾点缀和命中爆裂改走卡利亚粒子 {@link CarianFx}。
 */
public class LorettaGreatbowProjectile extends AbstractGlintstoneProjectile {

    public LorettaGreatbowProjectile(
            EntityType<? extends LorettaGreatbowProjectile> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    public LorettaGreatbowProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.LORETTA_GREATBOW.get(), level);
        setOwner(shooter);
        setExplosionRadius(LorettaGreatbowSpell.EXPLOSION_RADIUS_BLOCKS);
    }

    @Override
    protected float explosionRadiusBlocks() {
        return LorettaGreatbowSpell.EXPLOSION_RADIUS_BLOCKS;
    }

    @Override
    protected float flightSpeed() {
        return LorettaGreatbowSpell.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return LorettaGreatbowSpell.PROJECTILE_TRACKING_RANGE_BLOCKS;
    }

    @Override
    protected double maxRangeBlocks() {
        return LorettaGreatbowSpell.PROJECTILE_MAX_RANGE_BLOCKS;
    }

    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return LorettaGreatbowSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return LorettaGreatbowSpell.PROJECTILE_TRACKING_START_DELAY_TICKS;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return LorettaGreatbowSpell.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return LorettaGreatbowSpell.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return LorettaGreatbowSpell.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return LorettaGreatbowSpell.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailStyle trailStyle() {
        return LorettaGreatbowSpell.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return LorettaGreatbowSpell.IMPACT_PARTICLE_INTENSITY;
    }

    /** 卡利亚点缀（深蓝光晕 / 火花 / 碎晶 / 新月），替代青辉石库。 */
    @Override
    protected void spawnTrailAccentParticles(Vec3 deltaMovement, GlintstoneTrailStyle trailStyle) {
        CarianFx.trailAccents(
                level(),
                getX(),
                getY(),
                getZ(),
                deltaMovement,
                trailParticleIntensity()
        );
    }

    @Override
    protected void spawnImpactParticles(double impactX, double impactY, double impactZ) {
        CarianFx.impact(level(), impactX, impactY, impactZ, impactParticleIntensity());
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.LORETTA_GREATBOW.get();
    }

    @Override
    public GlintstoneVisualStyle visualStyle() {
        return GlintstoneVisualStyle.anisotropic(
                LorettaGreatbowSpell.COMET_HEAD_BODY_SCALE_RADIAL,
                LorettaGreatbowSpell.COMET_HEAD_BODY_SCALE_ALONG,
                LorettaGreatbowSpell.COMET_HEAD_GLOW_SCALE,
                LorettaGreatbowSpell.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                LorettaGreatbowSpell.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                LorettaGreatbowSpell.COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE,
                LorettaGreatbowSpell.COMET_HEAD_CORE_RED,
                LorettaGreatbowSpell.COMET_HEAD_CORE_GREEN,
                LorettaGreatbowSpell.COMET_HEAD_CORE_BLUE,
                LorettaGreatbowSpell.COMET_HEAD_GLOW_RED,
                LorettaGreatbowSpell.COMET_HEAD_GLOW_GREEN,
                LorettaGreatbowSpell.COMET_HEAD_GLOW_BLUE,
                LorettaGreatbowSpell.COMET_HEAD_GLOW_ALPHA
        );
    }
}

package com.eldenring.spells.entity;

import com.eldenring.spells.particle.glintstone.GlintstoneFx;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.spell.StarsOfRuinSpell;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 毁灭流星单发弹道：12连发中的一颗。
 * <p>
 * 颜色按实体 id 奇偶在紫色 / 深蓝色之间交替，拖尾与命中走星河蓝紫粒子，
 * 无需额外同步字段。
 */
public class StarsOfRuinProjectile extends AbstractGlintstoneProjectile {

    public StarsOfRuinProjectile(
            EntityType<? extends StarsOfRuinProjectile> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    public StarsOfRuinProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.STARS_OF_RUIN.get(), level);
        setOwner(shooter);
    }

    @Override
    protected float flightSpeed() {
        return StarsOfRuinSpell.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return StarsOfRuinSpell.PROJECTILE_TRACKING_RANGE_BLOCKS;
    }

    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return StarsOfRuinSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return StarsOfRuinSpell.PROJECTILE_TRACKING_START_DELAY_TICKS;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return StarsOfRuinSpell.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return StarsOfRuinSpell.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return StarsOfRuinSpell.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return StarsOfRuinSpell.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailStyle trailStyle() {
        return StarsOfRuinSpell.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return StarsOfRuinSpell.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.STARS_OF_RUIN.get();
    }

    @Override
    protected void spawnTrailAccentParticles(Vec3 deltaMovement, GlintstoneTrailStyle trailStyle) {
            GlintstoneFx.ruinTrailAccents(
                level(),
                getX(),
                getY(),
                getZ(),
                deltaMovement,
                trailParticleIntensity(),
                trailStyle,
                StarsOfRuinSpell.TRAIL_ACCENT_CHANCE_SCALE
            );
    }

    @Override
    protected void spawnImpactParticles(double impactX, double impactY, double impactZ) {
        GlintstoneFx.ruinImpact(level(), impactX, impactY, impactZ, impactParticleIntensity());
    }

    @Override
    public GlintstoneVisualStyle visualStyle() {
        boolean useDeepBlue = (getId() & 1) == 1;
        if (useDeepBlue) {
            return GlintstoneVisualStyle.fromFloatColors(
                    StarsOfRuinSpell.COMET_HEAD_BODY_SCALE,
                    StarsOfRuinSpell.COMET_HEAD_GLOW_SCALE,
                    StarsOfRuinSpell.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                    StarsOfRuinSpell.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                    StarsOfRuinSpell.DEEP_CORE_RED,
                    StarsOfRuinSpell.DEEP_CORE_GREEN,
                    StarsOfRuinSpell.DEEP_CORE_BLUE,
                    StarsOfRuinSpell.DEEP_GLOW_RED,
                    StarsOfRuinSpell.DEEP_GLOW_GREEN,
                    StarsOfRuinSpell.DEEP_GLOW_BLUE,
                    StarsOfRuinSpell.DEEP_GLOW_ALPHA
            );
        }
        return GlintstoneVisualStyle.fromFloatColors(
                StarsOfRuinSpell.COMET_HEAD_BODY_SCALE,
                StarsOfRuinSpell.COMET_HEAD_GLOW_SCALE,
                StarsOfRuinSpell.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                StarsOfRuinSpell.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                StarsOfRuinSpell.PURPLE_CORE_RED,
                StarsOfRuinSpell.PURPLE_CORE_GREEN,
                StarsOfRuinSpell.PURPLE_CORE_BLUE,
                StarsOfRuinSpell.PURPLE_GLOW_RED,
                StarsOfRuinSpell.PURPLE_GLOW_GREEN,
                StarsOfRuinSpell.PURPLE_GLOW_BLUE,
                StarsOfRuinSpell.PURPLE_GLOW_ALPHA
        );
    }
}

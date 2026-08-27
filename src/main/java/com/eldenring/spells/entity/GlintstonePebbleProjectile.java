package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import com.eldenring.spells.spell.GlintstonePebbleSpell;

/**
 * 辉石魔砾弹道：限角追踪的单发彗星。
 * <p>
 * 飞行光轨由客户端几何绘制；本类只向 {@link com.eldenring.spells.particle.glintstone.GlintstoneFx}
 * 交稀疏点缀。玩法数字读 {@link GlintstonePebbleSpell}。
 */
public class GlintstonePebbleProjectile extends AbstractGlintstoneProjectile {

    public GlintstonePebbleProjectile(EntityType<? extends GlintstonePebbleProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public GlintstonePebbleProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.GLINTSTONE_PEBBLE.get(), level);
        setOwner(shooter);
    }

    @Override
    protected float flightSpeed() {
        return GlintstonePebbleSpell.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return GlintstonePebbleSpell.PROJECTILE_TRACKING_RANGE_BLOCKS;
    }

    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return GlintstonePebbleSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return GlintstonePebbleSpell.PROJECTILE_TRACKING_START_DELAY_TICKS;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return GlintstonePebbleSpell.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return GlintstonePebbleSpell.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return GlintstonePebbleSpell.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return GlintstonePebbleSpell.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailStyle trailStyle() {
        return GlintstonePebbleSpell.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return GlintstonePebbleSpell.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.GLINTSTONE_PEBBLE.get();
    }

    @Override
    public GlintstoneVisualStyle visualStyle() {
        return GlintstoneVisualStyle.anisotropic(
                GlintstonePebbleSpell.COMET_HEAD_BODY_SCALE_RADIAL,
                GlintstonePebbleSpell.COMET_HEAD_BODY_SCALE_ALONG,
                GlintstonePebbleSpell.COMET_HEAD_GLOW_SCALE,
                GlintstonePebbleSpell.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                GlintstonePebbleSpell.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                GlintstonePebbleSpell.COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE,
                GlintstonePebbleSpell.COMET_HEAD_CORE_RED,
                GlintstonePebbleSpell.COMET_HEAD_CORE_GREEN,
                GlintstonePebbleSpell.COMET_HEAD_CORE_BLUE,
                GlintstonePebbleSpell.COMET_HEAD_GLOW_RED,
                GlintstonePebbleSpell.COMET_HEAD_GLOW_GREEN,
                GlintstonePebbleSpell.COMET_HEAD_GLOW_BLUE,
                GlintstonePebbleSpell.COMET_HEAD_GLOW_ALPHA
        );
    }
}

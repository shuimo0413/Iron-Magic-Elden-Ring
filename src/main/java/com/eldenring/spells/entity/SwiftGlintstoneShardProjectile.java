package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import com.eldenring.spells.spell.SwiftGlintstoneShardSpell;

/** 辉石迅魔砾弹道：更快、更淡的魔砾变体。玩法数字读 {@link SwiftGlintstoneShardSpell}。 */
public class SwiftGlintstoneShardProjectile extends AbstractGlintstoneProjectile {

    public SwiftGlintstoneShardProjectile(
            EntityType<? extends SwiftGlintstoneShardProjectile> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    public SwiftGlintstoneShardProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.SWIFT_GLINTSTONE_SHARD.get(), level);
        setOwner(shooter);
    }

    @Override
    protected float flightSpeed() {
        return SwiftGlintstoneShardSpell.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return SwiftGlintstoneShardSpell.PROJECTILE_TRACKING_RANGE_BLOCKS;
    }

    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return SwiftGlintstoneShardSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return SwiftGlintstoneShardSpell.PROJECTILE_TRACKING_START_DELAY_TICKS;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return SwiftGlintstoneShardSpell.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return SwiftGlintstoneShardSpell.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return SwiftGlintstoneShardSpell.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return SwiftGlintstoneShardSpell.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailStyle trailStyle() {
        return SwiftGlintstoneShardSpell.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return SwiftGlintstoneShardSpell.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.SWIFT_GLINTSTONE_SHARD.get();
    }

    @Override
    public GlintstoneVisualStyle visualStyle() {
        return GlintstoneVisualStyle.anisotropic(
                SwiftGlintstoneShardSpell.COMET_HEAD_BODY_SCALE_RADIAL,
                SwiftGlintstoneShardSpell.COMET_HEAD_BODY_SCALE_ALONG,
                SwiftGlintstoneShardSpell.COMET_HEAD_GLOW_SCALE,
                SwiftGlintstoneShardSpell.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                SwiftGlintstoneShardSpell.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                SwiftGlintstoneShardSpell.COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE,
                SwiftGlintstoneShardSpell.COMET_HEAD_CORE_RED,
                SwiftGlintstoneShardSpell.COMET_HEAD_CORE_GREEN,
                SwiftGlintstoneShardSpell.COMET_HEAD_CORE_BLUE,
                SwiftGlintstoneShardSpell.COMET_HEAD_GLOW_RED,
                SwiftGlintstoneShardSpell.COMET_HEAD_GLOW_GREEN,
                SwiftGlintstoneShardSpell.COMET_HEAD_GLOW_BLUE,
                SwiftGlintstoneShardSpell.COMET_HEAD_GLOW_ALPHA
        );
    }
}

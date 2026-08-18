package com.eldenring.spells.entity;

import com.eldenring.spells.client.render.glintstone.GlintstoneCometHeadDrawer;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.tuning.GlintstoneTrailTuning;
import com.eldenring.spells.tuning.SwiftGlintstoneShardTuning;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** ???????????????? */
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
        return SwiftGlintstoneShardTuning.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return SwiftGlintstoneShardTuning.PROJECTILE_TRACKING_RANGE_BLOCKS;
    }

    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return SwiftGlintstoneShardTuning.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return SwiftGlintstoneShardTuning.PROJECTILE_TRACKING_START_DELAY_TICKS;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return SwiftGlintstoneShardTuning.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return SwiftGlintstoneShardTuning.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return SwiftGlintstoneShardTuning.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return SwiftGlintstoneShardTuning.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailTuning.TrailStyle trailStyle() {
        return SwiftGlintstoneShardTuning.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return SwiftGlintstoneShardTuning.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.SWIFT_GLINTSTONE_SHARD.get();
    }

    @Override
    public GlintstoneCometHeadDrawer.VisualStyle visualStyle() {
        return GlintstoneCometHeadDrawer.VisualStyle.anisotropic(
                SwiftGlintstoneShardTuning.COMET_HEAD_BODY_SCALE_RADIAL,
                SwiftGlintstoneShardTuning.COMET_HEAD_BODY_SCALE_ALONG,
                SwiftGlintstoneShardTuning.COMET_HEAD_GLOW_SCALE,
                SwiftGlintstoneShardTuning.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                SwiftGlintstoneShardTuning.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                SwiftGlintstoneShardTuning.COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE,
                SwiftGlintstoneShardTuning.COMET_HEAD_CORE_RED,
                SwiftGlintstoneShardTuning.COMET_HEAD_CORE_GREEN,
                SwiftGlintstoneShardTuning.COMET_HEAD_CORE_BLUE,
                SwiftGlintstoneShardTuning.COMET_HEAD_GLOW_RED,
                SwiftGlintstoneShardTuning.COMET_HEAD_GLOW_GREEN,
                SwiftGlintstoneShardTuning.COMET_HEAD_GLOW_BLUE,
                SwiftGlintstoneShardTuning.COMET_HEAD_GLOW_ALPHA
        );
    }
}

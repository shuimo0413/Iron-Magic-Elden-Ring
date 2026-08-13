package com.eldenring.spells.entity;

import com.eldenring.spells.client.render.glintstone.GlintstoneCometHeadDrawer;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.tuning.GlintstoneTrailTuning;
import com.eldenring.spells.tuning.StarsOfRuinTuning;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * 毁灭流星单发弹道：八连发中的一颗。
 * <p>
 * 颜色按实体 id 奇偶在亮蓝 / 深蓝之间交替，形成「蓝色与深蓝色交织」，
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
        return StarsOfRuinTuning.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return StarsOfRuinTuning.PROJECTILE_TRACKING_RANGE_BLOCKS;
    }

    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return StarsOfRuinTuning.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return StarsOfRuinTuning.PROJECTILE_TRACKING_START_DELAY_TICKS;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return StarsOfRuinTuning.PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return StarsOfRuinTuning.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return StarsOfRuinTuning.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return StarsOfRuinTuning.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailTuning.TrailStyle trailStyle() {
        return StarsOfRuinTuning.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return StarsOfRuinTuning.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.STARS_OF_RUIN.get();
    }

    @Override
    public GlintstoneCometHeadDrawer.VisualStyle visualStyle() {
        boolean useDeepBlue = (getId() & 1) == 1;
        if (useDeepBlue) {
            return GlintstoneCometHeadDrawer.VisualStyle.fromFloatColors(
                    StarsOfRuinTuning.COMET_HEAD_BODY_SCALE,
                    StarsOfRuinTuning.COMET_HEAD_GLOW_SCALE,
                    StarsOfRuinTuning.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                    StarsOfRuinTuning.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                    StarsOfRuinTuning.DEEP_CORE_RED,
                    StarsOfRuinTuning.DEEP_CORE_GREEN,
                    StarsOfRuinTuning.DEEP_CORE_BLUE,
                    StarsOfRuinTuning.DEEP_GLOW_RED,
                    StarsOfRuinTuning.DEEP_GLOW_GREEN,
                    StarsOfRuinTuning.DEEP_GLOW_BLUE,
                    StarsOfRuinTuning.DEEP_GLOW_ALPHA
            );
        }
        return GlintstoneCometHeadDrawer.VisualStyle.fromFloatColors(
                StarsOfRuinTuning.COMET_HEAD_BODY_SCALE,
                StarsOfRuinTuning.COMET_HEAD_GLOW_SCALE,
                StarsOfRuinTuning.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                StarsOfRuinTuning.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                StarsOfRuinTuning.BRIGHT_CORE_RED,
                StarsOfRuinTuning.BRIGHT_CORE_GREEN,
                StarsOfRuinTuning.BRIGHT_CORE_BLUE,
                StarsOfRuinTuning.BRIGHT_GLOW_RED,
                StarsOfRuinTuning.BRIGHT_GLOW_GREEN,
                StarsOfRuinTuning.BRIGHT_GLOW_BLUE,
                StarsOfRuinTuning.BRIGHT_GLOW_ALPHA
        );
    }
}

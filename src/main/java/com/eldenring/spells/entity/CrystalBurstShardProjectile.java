package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.spell.CrystalBurstSpell;
import com.eldenring.spells.spell.fx.CrystalBurstFx;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 结晶散射碎片：迅魔砾那套彗星头 / 光轨，但不追踪，直线飞，射程短。
 * <p>
 * 撞实体 / 撞方块由基类结算伤害并 {@code discard}；飞满射程时自己碎裂消失。
 * 玩法数字读 {@link CrystalBurstSpell}。
 */
public class CrystalBurstShardProjectile extends AbstractGlintstoneProjectile {

    /**
     * 出生世界坐标。用来按直线距离掐射程，比只数 tick 更稳（速度被 toml 改了也能对上）。
     */
    private Vec3 spawnWorldPosition;

    public CrystalBurstShardProjectile(
            EntityType<? extends CrystalBurstShardProjectile> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    public CrystalBurstShardProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.CRYSTAL_BURST_SHARD.get(), level);
        setOwner(shooter);
    }

    /**
     * 故意空实现：结晶散射是面前扇形直射，不能再拐去追怪。
     */
    @Override
    protected void handleEntityHoming() {
    }

    @Override
    public void tick() {
        if (this.spawnWorldPosition == null) {
            this.spawnWorldPosition = position();
        }
        super.tick();
        if (this.isRemoved()) {
            return;
        }
        if (hasExceededMaxRangeOrLifetime()) {
            shatterAndDiscard();
        }
    }

    /**
     * 飞出 {@link CrystalBurstSpell#PROJECTILE_MAX_RANGE_BLOCKS}，或明显超过按弹速估的寿命。
     */
    private boolean hasExceededMaxRangeOrLifetime() {
        double maxRangeBlocks = CrystalBurstSpell.PROJECTILE_MAX_RANGE_BLOCKS;
        if (this.spawnWorldPosition != null
                && this.spawnWorldPosition.distanceToSqr(position()) > maxRangeBlocks * maxRangeBlocks) {
            return true;
        }
        float flightSpeed = Math.max(0.05f, flightSpeed());
        int maximumLifetimeTicks = (int) Math.ceil(maxRangeBlocks / flightSpeed) + 4;
        return this.tickCount > maximumLifetimeTicks;
    }

    /**
     * 射程耗尽：刷碎裂粒子后消失。命中走基类 {@code onHit} → {@link #spawnImpactParticles}。
     */
    private void shatterAndDiscard() {
        if (!level().isClientSide) {
            impactParticles(getX(), getY(), getZ());
            getImpactSound().ifPresent(this::doImpactSound);
        }
        discard();
    }

    @Override
    protected float flightSpeed() {
        return CrystalBurstSpell.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    protected double trackingRangeBlocks() {
        return 0.0;
    }

    @Override
    protected float maxTurnAngleDegreesPerTick() {
        return 0.0f;
    }

    @Override
    protected int trackingStartDelayTicks() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected float trackingAcquireConeHalfAngleDegrees() {
        return 0.0f;
    }

    @Override
    protected double minimumSpeedForHoming() {
        return CrystalBurstSpell.PROJECTILE_MINIMUM_SPEED_FOR_HOMING;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return CrystalBurstSpell.PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS;
    }

    @Override
    protected float trailParticleIntensity() {
        return CrystalBurstSpell.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailStyle trailStyle() {
        return CrystalBurstSpell.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return CrystalBurstSpell.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected void spawnImpactParticles(double impactX, double impactY, double impactZ) {
        CrystalBurstFx.shatter(level(), impactX, impactY, impactZ);
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.CRYSTAL_BURST.get();
    }

    @Override
    public GlintstoneVisualStyle visualStyle() {
        return GlintstoneVisualStyle.anisotropic(
                CrystalBurstSpell.COMET_HEAD_BODY_SCALE_RADIAL,
                CrystalBurstSpell.COMET_HEAD_BODY_SCALE_ALONG,
                CrystalBurstSpell.COMET_HEAD_GLOW_SCALE,
                CrystalBurstSpell.COMET_HEAD_GLOW_PULSE_AMPLITUDE,
                CrystalBurstSpell.COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK,
                CrystalBurstSpell.COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE,
                CrystalBurstSpell.COMET_HEAD_CORE_RED,
                CrystalBurstSpell.COMET_HEAD_CORE_GREEN,
                CrystalBurstSpell.COMET_HEAD_CORE_BLUE,
                CrystalBurstSpell.COMET_HEAD_GLOW_RED,
                CrystalBurstSpell.COMET_HEAD_GLOW_GREEN,
                CrystalBurstSpell.COMET_HEAD_GLOW_BLUE,
                CrystalBurstSpell.COMET_HEAD_GLOW_ALPHA
        );
    }
}

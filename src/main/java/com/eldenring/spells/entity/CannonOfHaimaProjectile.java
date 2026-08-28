package com.eldenring.spells.entity;

import com.eldenring.spells.particle.glintstone.GlintstoneFx;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.spell.CannonOfHaimaSpell;
import com.eldenring.spells.spell.combat.CannonOfHaimaCombat;
import com.eldenring.spells.spell.fx.CannonOfHaimaFx;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 海摩炮弹弹道：受重力的实心辉石球，不追踪。
 * <p>
 * 基类 {@link AbstractGlintstoneProjectile} 默认无重力且会限角索敌；本类关掉这两项，
 * 只复用光轨历史、命中宽限和爆炸入口。落地 / 碰敌由 Combat 结算、Fx 播烟雾与碎片。
 */
public class CannonOfHaimaProjectile extends AbstractGlintstoneProjectile {

    public CannonOfHaimaProjectile(
            EntityType<? extends CannonOfHaimaProjectile> entityType,
            Level level
    ) {
        super(entityType, level);
        this.setNoGravity(false);
    }

    public CannonOfHaimaProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.CANNON_OF_HAIMA.get(), level);
        setOwner(shooter);
        setExplosionRadius(CannonOfHaimaSpell.EXPLOSION_RADIUS_BLOCKS);
        setNoGravity(false);
    }

    @Override
    protected double getDefaultGravity() {
        return CannonOfHaimaSpell.PROJECTILE_GRAVITY_BLOCKS_PER_TICK_SQUARED;
    }

    /**
     * 明确关闭追踪：炮弹必须沿出手方向做抛物线，不能半路拐向附近敌人。
     */
    @Override
    protected void handleEntityHoming() {
    }

    @Override
    public void tick() {
        if (tickCount > CannonOfHaimaSpell.ENTITY_LIFETIME_TICKS) {
            discard();
            return;
        }
        super.tick();
    }

    @Override
    public void impactParticles(double impactX, double impactY, double impactZ) {
        CannonOfHaimaFx.spawnImpact(level(), new Vec3(impactX, impactY, impactZ));
    }

    @Override
    public Optional<Holder<SoundEvent>> getImpactSound() {
        // 爆炸音已经在 Fx 里播过，这里不要再叠一层紫水晶击打。
        return Optional.empty();
    }

    @Override
    protected void dealHitDamage(@Nullable Vec3 impactLocation, @Nullable Entity primaryHitEntity) {
        Vec3 explosionCenter = impactLocation != null ? impactLocation : position();
        CannonOfHaimaCombat.resolve(this, level(), explosionCenter, getDamage());
    }

    @Override
    public float getHitDetectionInflation() {
        return CannonOfHaimaSpell.HIT_DETECTION_INFLATION_BLOCKS;
    }

    @Override
    protected float explosionRadiusBlocks() {
        return CannonOfHaimaSpell.EXPLOSION_RADIUS_BLOCKS;
    }

    @Override
    protected float flightSpeed() {
        return CannonOfHaimaSpell.PROJECTILE_FLIGHT_SPEED;
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
        return 1.0e-4;
    }

    @Override
    protected double directionAlignEpsilonRadians() {
        return 1.0e-5;
    }

    @Override
    protected float trailParticleIntensity() {
        return CannonOfHaimaSpell.TRAIL_PARTICLE_INTENSITY;
    }

    @Override
    public GlintstoneTrailStyle trailStyle() {
        return CannonOfHaimaSpell.TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return CannonOfHaimaSpell.IMPACT_PARTICLE_INTENSITY;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.CANNON_OF_HAIMA.get();
    }

    @Override
    protected Vec3 trailRecordWorldPosition() {
        return position().add(0.0, getBbHeight() * 0.5, 0.0);
    }

    @Override
    protected void spawnTrailAccentParticles(
            Vec3 deltaMovement,
            GlintstoneTrailStyle trailStyle
    ) {
        Vec3 trailPosition = trailRecordWorldPosition();
        GlintstoneFx.trailAccents(
                level(),
                trailPosition.x,
                trailPosition.y,
                trailPosition.z,
                deltaMovement,
                trailParticleIntensity(),
                trailStyle
        );
    }

    @Override
    public GlintstoneVisualStyle visualStyle() {
        // 光轨着色用；炮弹本体由 HaimaCannonRenderer 画实心球，不走彗星头。
        return GlintstoneVisualStyle.anisotropic(
                1.15f,
                1.15f,
                1.55f,
                0.12f,
                9.0f,
                1.05f,
                0.05f, 0.90f, 0.88f,
                0.08f, 0.94f, 0.92f, 1.0f
        );
    }
}

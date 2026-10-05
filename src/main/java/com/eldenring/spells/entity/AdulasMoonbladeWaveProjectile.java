package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.spell.AdulasMoonbladeSpell;
import com.eldenring.spells.spell.combat.AdulasMoonbladeCombat;
import com.eldenring.spells.spell.combat.AdulasMoonbladeWaveCombat;
import com.eldenring.spells.spell.fx.AdulasMoonbladeFx;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 亚杜拉的月光剑剑气：从辉石弯弧复制的直飞月牙，只有一道，冰蓝白配色，一路留下冰雾。
 * <p>
 * 不追踪。每个敌人只结算一次（伤害源带冻结 + 命中上 {@code CHILLED}），
 * 穿透次数耗尽、撞墙或飞满射程后碎裂。碰撞箱只作客户端追踪占位，真正打人走 {@link AdulasMoonbladeWaveCombat}。
 */
public class AdulasMoonbladeWaveProjectile extends AbstractGlintstoneProjectile {

    /** 点缀粒子用的细光轨配置。几何月牙不读这份长度，只给冰霜粒子一个半宽。 */
    public static final GlintstoneTrailStyle TRAIL_STYLE =
            new GlintstoneTrailStyle(4.0, 0.040f, 0.010f, 0.14f, 0.04f, 12);

    /** 已经结算过的实体 UUID。剑气穿人但不对同一目标连打。 */
    private final Set<UUID> alreadyHitEntityUuids = new HashSet<>();

    /** 出生世界坐标。用来按直线距离掐射程、算当前半宽。 */
    private Vec3 spawnWorldPosition;

    public AdulasMoonbladeWaveProjectile(
            EntityType<? extends AdulasMoonbladeWaveProjectile> entityType,
            Level level
    ) {
        super(entityType, level);
        setPierceLevel(Math.max(0, AdulasMoonbladeSpell.WAVE_MAX_ENTITY_HITS - 1));
    }

    public AdulasMoonbladeWaveProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.ADULAS_MOONBLADE_WAVE.get(), level);
        setOwner(shooter);
    }

    /** 故意空实现：剑气是直飞的横向月牙，不能拐去追怪。 */
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

    /** 客户端沿月牙与飞行中线留冰雾；不记彗星头那种中心光轨历史。 */
    @Override
    public void trailParticles() {
        AdulasMoonbladeFx.trailAlongWave(this, level());
    }

    /** 定向盒命中 + 中心射线撞墙。实体与方块同 tick 结算。 */
    @Override
    public void handleHitDetection() {
        if (this.isRemoved()) {
            return;
        }
        Vec3 pathStart = position();
        Vec3 pathEnd = pathStart.add(getDeltaMovement());
        float halfWidthBlocks = currentHalfWidthBlocks(0.0f);

        BlockHitResult blockCollision = level().clip(new ClipContext(
                pathStart,
                pathEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));
        Vec3 entityPathEnd = pathEnd;
        if (collidesWithBlocks() && blockCollision.getType() != HitResult.Type.MISS) {
            entityPathEnd = blockCollision.getLocation();
        }

        List<EntityHitResult> orderedHits = AdulasMoonbladeWaveCombat.collectEntityHits(
                this,
                level(),
                pathStart,
                entityPathEnd,
                halfWidthBlocks
        );
        double blockDistanceSquared = blockCollision.getType() == HitResult.Type.MISS
                ? Double.MAX_VALUE
                : blockCollision.getLocation().distanceToSqr(pathStart);

        for (EntityHitResult entityHit : orderedHits) {
            if (this.isRemoved()) {
                return;
            }
            if (entityHit.getLocation().distanceToSqr(pathStart) > blockDistanceSquared) {
                break;
            }
            if (!NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(this, entityHit)).isCanceled()) {
                onHit(entityHit);
            }
        }

        if (collidesWithBlocks()
                && blockCollision.getType() != HitResult.Type.MISS
                && !this.isRemoved()
                && !NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(this, blockCollision)).isCanceled()) {
            onHit(blockCollision);
        }
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity targetEntity) {
        if (alreadyHitEntityUuids.contains(targetEntity.getUUID())) {
            return false;
        }
        return super.canHitEntity(targetEntity);
    }

    /** Combat 在别的包，需要公开包装才能当 {@code getEntities} 谓词。 */
    public boolean isValidWaveTarget(Entity targetEntity) {
        return canHitEntity(targetEntity);
    }

    /** 清无敌帧、结算伤害并上寒冷、记入已命中，再交给铁魔法消耗穿透次数。 */
    @Override
    protected void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        Entity hitEntity = entityHitResult.getEntity();
        alreadyHitEntityUuids.add(hitEntity.getUUID());
        if (!level().isClientSide) {
            if (hitEntity instanceof LivingEntity livingEntity) {
                livingEntity.invulnerableTime = 0;
                livingEntity.hurtTime = 0;
            }
            DamageSources.applyDamage(
                    hitEntity,
                    damage,
                    damageSourceSpell().getDamageSource(this, getOwner())
            );
            if (hitEntity instanceof LivingEntity livingEntityAfterHit) {
                livingEntityAfterHit.invulnerableTime = 0;
                AdulasMoonbladeCombat.applyChill(livingEntityAfterHit);
            }
            AdulasMoonbladeFx.waveHitSpark(
                    level(),
                    entityHitResult.getLocation().x,
                    entityHitResult.getLocation().y,
                    entityHitResult.getLocation().z
            );
        }
        consumeEntityImpact(entityHitResult, true);
    }

    @Override
    protected void spawnImpactParticles(double impactX, double impactY, double impactZ) {
        AdulasMoonbladeFx.waveShatter(level(), impactX, impactY, impactZ);
    }

    /** 当前半宽（方块）。客户端带 {@code partialTicks} 做插值，服务端命中传 0。 */
    public float currentHalfWidthBlocks(float partialTicks) {
        return AdulasMoonbladeFx.waveHalfWidthAtDistance(traveledBlocks(partialTicks));
    }

    /** 当前飞行方向。速度过小时回退到实体朝向，避免 Renderer / 命中叉积退化。 */
    public Vec3 resolveFlightDirection() {
        Vec3 deltaMovement = getDeltaMovement();
        if (deltaMovement.lengthSqr() > 1.0e-8) {
            return deltaMovement.normalize();
        }
        return getLookAngle().lengthSqr() > 1.0e-8 ? getLookAngle().normalize() : new Vec3(0.0, 0.0, 1.0);
    }

    private double traveledBlocks(float partialTicks) {
        Vec3 currentPosition = partialTicks <= 0.0f
                ? position()
                : new Vec3(
                        Mth.lerp(partialTicks, xo, getX()),
                        Mth.lerp(partialTicks, yo, getY()),
                        Mth.lerp(partialTicks, zo, getZ())
                );
        if (this.spawnWorldPosition == null) {
            return tickCount * Math.max(0.05f, flightSpeed());
        }
        return this.spawnWorldPosition.distanceTo(currentPosition);
    }

    private boolean hasExceededMaxRangeOrLifetime() {
        double maxRangeBlocks = AdulasMoonbladeSpell.WAVE_MAX_RANGE_BLOCKS;
        if (this.spawnWorldPosition != null
                && this.spawnWorldPosition.distanceToSqr(position()) > maxRangeBlocks * maxRangeBlocks) {
            return true;
        }
        float flightSpeed = Math.max(0.05f, flightSpeed());
        int maximumLifetimeTicks = (int) Math.ceil(maxRangeBlocks / flightSpeed) + 4;
        return this.tickCount > maximumLifetimeTicks;
    }

    private void shatterAndDiscard() {
        if (!level().isClientSide) {
            impactParticles(getX(), getY(), getZ());
            getImpactSound().ifPresent(this::doImpactSound);
        }
        discard();
    }

    @Override
    protected float flightSpeed() {
        return AdulasMoonbladeSpell.WAVE_FLIGHT_SPEED;
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
        return 0.5f;
    }

    @Override
    public GlintstoneTrailStyle trailStyle() {
        return TRAIL_STYLE;
    }

    @Override
    protected float impactParticleIntensity() {
        return 0.6f;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        return ModSpells.ADULAS_MOONBLADE.get();
    }

    /** 占位样式：自定义 Renderer 不画彗星头。颜色给冷白，以免误接到通用渲染器时跑偏。 */
    @Override
    public GlintstoneVisualStyle visualStyle() {
        return GlintstoneVisualStyle.anisotropic(
                0.12f,
                0.35f,
                0.40f,
                0.05f,
                18.0f,
                1.4f,
                0.75f,
                0.90f,
                1.0f,
                0.85f,
                0.95f,
                1.0f,
                1.0f
        );
    }
}

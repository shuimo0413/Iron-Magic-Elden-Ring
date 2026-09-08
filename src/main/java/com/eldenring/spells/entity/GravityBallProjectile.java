package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.spell.GravityBallSpell;
import com.eldenring.spells.spell.combat.GravityBallCombat;
import com.eldenring.spells.spell.fx.GravityBallFx;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
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

import java.util.Optional;

/**
 * 重力球弹道：无追踪、无重力的直线紫球；无伤害，命中后按等级拉取格数吸敌。
 * <p>
 * 最大射程固定 {@link GravityBallSpell#PROJECTILE_MAX_RANGE_BLOCKS}（25 格）。
 */
public class GravityBallProjectile extends AbstractMagicProjectile {

    /**
     * 出手后忽略方块命中的 tick 数，避免出生略嵌实心块时立刻销毁。
     */
    private static final int BLOCK_COLLISION_GRACE_TICKS = 4;

    /**
     * 命中射线相对目标碰撞箱的外扩（方块）。
     */
    private static final float HIT_DETECTION_INFLATION_BLOCKS = 0.35f;

    private boolean hasResolvedImpact;

    /**
     * 本发弹道的最大拉取格数（由施法等级写入）。默认 1 级 = 3。
     */
    private double pullDistanceBlocks = GravityBallSpell.SUCTION_PULL_BLOCKS_AT_LEVEL_1;

    /**
     * 出手后已飞行的路程（方块）。用于 25 格射程截断。
     */
    private double traveledDistanceBlocks;

    public GravityBallProjectile(
            EntityType<? extends GravityBallProjectile> entityType,
            Level level
    ) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public GravityBallProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.GRAVITY_BALL.get(), level);
        setOwner(shooter);
        setNoGravity(true);
    }

    /**
     * 本发最大拉取格数（方块）。
     */
    public double pullDistanceBlocks() {
        return pullDistanceBlocks;
    }

    /**
     * 出手时由法术写入本级拉取格数。
     */
    public void setPullDistanceBlocks(double pullDistanceBlocks) {
        this.pullDistanceBlocks = Math.max(0.0, pullDistanceBlocks);
    }

    @Override
    public void trailParticles() {
        GravityBallFx.spawnTrail(level(), position(), getDeltaMovement());
    }

    @Override
    public void impactParticles(double impactX, double impactY, double impactZ) {
        GravityBallFx.spawnImpact(level(), new Vec3(impactX, impactY, impactZ));
    }

    @Override
    public float getSpeed() {
        return GravityBallSpell.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    public float getHitDetectionInflation() {
        return HIT_DETECTION_INFLATION_BLOCKS;
    }

    @Override
    public Optional<Holder<SoundEvent>> getImpactSound() {
        return Optional.empty();
    }

    @Override
    public void tick() {
        if (!isFinitePositionAndMotion()) {
            discard();
            return;
        }
        Vec3 positionBeforeTick = position();
        super.tick();
        if (this.isRemoved()) {
            return;
        }
        traveledDistanceBlocks += position().distanceTo(positionBeforeTick);
        if (traveledDistanceBlocks >= GravityBallSpell.PROJECTILE_MAX_RANGE_BLOCKS) {
            discard();
        }
    }

    private boolean isFinitePositionAndMotion() {
        Vec3 motion = getDeltaMovement();
        return Double.isFinite(getX())
                && Double.isFinite(getY())
                && Double.isFinite(getZ())
                && Double.isFinite(motion.x)
                && Double.isFinite(motion.y)
                && Double.isFinite(motion.z);
    }

    @Override
    public void handleHitDetection() {
        boolean withinBlockCollisionGrace = tickCount <= BLOCK_COLLISION_GRACE_TICKS;
        Vec3 startPosition = position();
        Vec3 destination = startPosition.add(getDeltaMovement());
        BlockHitResult blockCollision = level().clip(new ClipContext(
                startPosition,
                destination,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));
        if (blockCollision.getType() != HitResult.Type.MISS) {
            destination = blockCollision.getLocation();
        }

        for (var target : level().getEntities(this, getBoundingBox().expandTowards(getDeltaMovement()).inflate(0.2), this::canHitEntity)) {
            HitResult entityHit = io.redspace.ironsspellbooks.api.util.Utils.checkEntityIntersecting(
                    target,
                    startPosition,
                    destination,
                    getHitDetectionInflation()
            );
            if (entityHit.getType() != HitResult.Type.MISS
                    && entityHit instanceof EntityHitResult entityHitResult
                    && !NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(this, entityHitResult)).isCanceled()) {
                onHit(entityHitResult);
            }
            if (this.isRemoved()) {
                return;
            }
        }

        if (!withinBlockCollisionGrace
                && blockCollision.getType() != HitResult.Type.MISS
                && !this.isRemoved()
                && !NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(this, blockCollision)).isCanceled()) {
            onHit(blockCollision);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        if (this.isRemoved() || hasResolvedImpact) {
            return;
        }
        hasResolvedImpact = true;
        Vec3 impactLocation = blockHitResult.getLocation();
        if (!level().isClientSide) {
            GravityBallCombat.resolve(this, level(), impactLocation);
            impactParticles(impactLocation.x, impactLocation.y, impactLocation.z);
        }
        discard();
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        if (this.isRemoved() || hasResolvedImpact) {
            return;
        }
        hasResolvedImpact = true;
        Vec3 impactLocation = entityHitResult.getLocation();
        if (!level().isClientSide) {
            GravityBallCombat.resolve(this, level(), impactLocation);
            impactParticles(impactLocation.x, impactLocation.y, impactLocation.z);
        }
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("PullDistanceBlocks", pullDistanceBlocks);
        tag.putDouble("TraveledDistanceBlocks", traveledDistanceBlocks);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        pullDistanceBlocks = tag.getDouble("PullDistanceBlocks");
        traveledDistanceBlocks = tag.getDouble("TraveledDistanceBlocks");
    }
}

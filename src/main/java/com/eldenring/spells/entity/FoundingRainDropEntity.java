package com.eldenring.spells.entity;

import com.eldenring.spells.particle.foundingrain.FoundingRainFx;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.tuning.FoundingRainOfStarsTuning;
import com.eldenring.spells.tuning.GlintstoneTrailTuning;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
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

import java.util.List;
import java.util.Optional;

/**
 * 创星雨的单根雨针：从雨云落下的细弹道，视觉是白紫曲线光带，不是彗星头。
 * <p>
 * 只撞方块刷涟漪，不判实体伤害。站在雨柱里的敌人由 {@link FoundingRainOfStarsEntity} 按范围结算。
 */
public class FoundingRainDropEntity extends AbstractMagicProjectile {

    /**
     * 仅客户端写入的真实下落路径。不参与同步与存档。
     */
    private final TrailHistoryBuffer clientTrailHistory = new TrailHistoryBuffer();

    /**
     * 出生点世界坐标。第一次 {@link #tick()} 钉死，用来量「超过 40 格」。
     */
    private Vec3 spawnWorldPosition = Vec3.ZERO;

    private boolean recordedSpawnPosition;
    private double traveledDistanceBlocks;
    private boolean impactedBlock;

    public FoundingRainDropEntity(
            EntityType<? extends FoundingRainDropEntity> entityType,
            Level level
    ) {
        super(entityType, level);
        this.setNoGravity(true);
        this.setInvisible(true);
    }

    public FoundingRainDropEntity(Level level, LivingEntity shooter) {
        this(ModEntities.FOUNDING_RAIN_DROP.get(), level);
        setOwner(shooter);
    }

    /**
     * 返回最旧 → 最新的客户端历史点，供光带 Renderer 重建下落曲线。
     */
    public List<Vec3> trailHistoryWorldPositions() {
        return clientTrailHistory.snapshot();
    }

    public GlintstoneTrailTuning.TrailStyle trailStyle() {
        return FoundingRainOfStarsTuning.RAIN_DROP_TRAIL_STYLE;
    }

    @Override
    public void trailParticles() {
        GlintstoneTrailTuning.TrailStyle trailStyle = trailStyle();
        clientTrailHistory.record(
                position(),
                trailStyle.lengthBlocks(),
                trailStyle.maximumHistoryPointCount()
        );
    }

    @Override
    public void impactParticles(double impactX, double impactY, double impactZ) {
        FoundingRainFx.spawnRainImpact(level(), impactX, impactY, impactZ, impactedBlock);
    }

    @Override
    public float getSpeed() {
        return FoundingRainOfStarsTuning.RAIN_DROP_FALL_SPEED_BLOCKS_PER_TICK;
    }

    @Override
    public float getHitDetectionInflation() {
        return 0.0f;
    }

    @Override
    public Optional<Holder<SoundEvent>> getImpactSound() {
        return Optional.empty();
    }

    @Override
    public void tick() {
        if (!recordedSpawnPosition) {
            spawnWorldPosition = position();
            recordedSpawnPosition = true;
        }
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
        double maximumTravelBlocks = FoundingRainOfStarsTuning.RAIN_DROP_MAXIMUM_TRAVEL_BLOCKS;
        if (traveledDistanceBlocks >= maximumTravelBlocks
                || position().distanceToSqr(spawnWorldPosition) >= maximumTravelBlocks * maximumTravelBlocks
                || tickCount >= FoundingRainOfStarsTuning.RAIN_DROP_MAXIMUM_LIFETIME_TICKS) {
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

    /**
     * 雨针只撞方块（落地涟漪）。实体伤害走雨云范围结算，这里不做射线扫怪。
     */
    @Override
    public void handleHitDetection() {
        if (tickCount <= 1) {
            return;
        }
        Vec3 startPosition = position();
        Vec3 destination = startPosition.add(getDeltaMovement());
        BlockHitResult blockCollision = level().clip(new ClipContext(
                startPosition,
                destination,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));
        if (collidesWithBlocks()
                && blockCollision.getType() != HitResult.Type.MISS
                && !NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(this, blockCollision)).isCanceled()) {
            onHit(blockCollision);
        }
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity targetEntity) {
        return false;
    }

    /**
     * 必须在父类 {@code onHit} 刷粒子之前记下是不是方块，否则地面命中会丢涟漪。
     */
    @Override
    protected void onHit(HitResult hitResult) {
        this.impactedBlock = hitResult.getType() == HitResult.Type.BLOCK;
        super.onHit(hitResult);
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        if (this.isRemoved()) {
            return;
        }
        this.impactedBlock = true;
        super.onHitBlock(blockHitResult);
        discard();
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        // 雨针不结算实体伤害，也不因擦到生物而消失。
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("RecordedSpawnPosition", recordedSpawnPosition);
        tag.putDouble("SpawnX", spawnWorldPosition.x);
        tag.putDouble("SpawnY", spawnWorldPosition.y);
        tag.putDouble("SpawnZ", spawnWorldPosition.z);
        tag.putDouble("TraveledDistanceBlocks", traveledDistanceBlocks);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.recordedSpawnPosition = tag.getBoolean("RecordedSpawnPosition");
        this.spawnWorldPosition = new Vec3(
                tag.getDouble("SpawnX"),
                tag.getDouble("SpawnY"),
                tag.getDouble("SpawnZ")
        );
        this.traveledDistanceBlocks = tag.getDouble("TraveledDistanceBlocks");
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}

package com.eldenring.spells.entity;

import com.eldenring.spells.particle.foundingrain.FoundingRainFx;
import com.eldenring.spells.particle.glintstone.GlintstoneFx;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.tuning.FoundingRainOfStarsTuning;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 创星雨时序实体：自身碰撞箱不可见，负责升空节拍，并在客户端画出身前星云面片。
 * <p>
 * 星云圆心钉在世界坐标上（不跟随玩家）。气团由 {@code FoundingRainNebulaRenderer} 画软光面片，
 * 白色小星星仍走一次粒子生成。
 */
public class FoundingRainOfStarsEntity extends Projectile {

    private static final EntityDataAccessor<Boolean> DATA_CLOUD_ACTIVE =
            SynchedEntityData.defineId(FoundingRainOfStarsEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_CLOUD_X =
            SynchedEntityData.defineId(FoundingRainOfStarsEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_CLOUD_Y =
            SynchedEntityData.defineId(FoundingRainOfStarsEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_CLOUD_Z =
            SynchedEntityData.defineId(FoundingRainOfStarsEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_CLOUD_YAW_DEGREES =
            SynchedEntityData.defineId(FoundingRainOfStarsEntity.class, EntityDataSerializers.FLOAT);

    private int spawnedAscentMoteCount;
    private boolean spawnedOverheadCloud;

    public FoundingRainOfStarsEntity(
            EntityType<? extends FoundingRainOfStarsEntity> entityType,
            Level level
    ) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvisible(true);
    }

    public FoundingRainOfStarsEntity(Level level, LivingEntity caster) {
        this(ModEntities.FOUNDING_RAIN_OF_STARS.get(), level);
        setOwner(caster);
        setPos(caster.getEyePosition());
    }

    public boolean isOverheadCloudActive() {
        return this.entityData.get(DATA_CLOUD_ACTIVE);
    }

    public Vec3 overheadCloudCenter() {
        return new Vec3(
                this.entityData.get(DATA_CLOUD_X),
                this.entityData.get(DATA_CLOUD_Y),
                this.entityData.get(DATA_CLOUD_Z)
        );
    }

    /**
     * 星云横向铺开用的偏航（度 → 弧度）。取施法瞬间的水平朝向。
     */
    public float overheadCloudYawRadians() {
        return this.entityData.get(DATA_CLOUD_YAW_DEGREES) * Mth.DEG_TO_RAD;
    }

    @Override
    public void tick() {
        baseTick();

        Entity owner = getOwner();
        if (!(owner instanceof LivingEntity caster) || !caster.isAlive() || caster.isRemoved()) {
            discard();
            return;
        }

        setPos(caster.getEyePosition());
        if (level().isClientSide) {
            return;
        }

        maybeLaunchAscentMotes(caster);
        maybeSpawnOverheadCloud(caster);

        if (tickCount >= FoundingRainOfStarsTuning.sequenceLifetimeTicks()) {
            discard();
        }
    }

    /**
     * 等待结束后，在错峰窗口内把 {@link FoundingRainOfStarsTuning#ASCENT_MOTE_COUNT} 颗光点分批抽走。
     */
    private void maybeLaunchAscentMotes(LivingEntity caster) {
        int launchDelayTicks = FoundingRainOfStarsTuning.ASCENT_LAUNCH_DELAY_TICKS;
        int staggerWindowTicks = Math.max(1, FoundingRainOfStarsTuning.ASCENT_STAGGER_WINDOW_TICKS);
        int totalMoteCount = FoundingRainOfStarsTuning.ASCENT_MOTE_COUNT;
        if (tickCount < launchDelayTicks || spawnedAscentMoteCount >= totalMoteCount) {
            return;
        }
        int tickInWindow = tickCount - launchDelayTicks;
        if (tickInWindow >= staggerWindowTicks) {
            return;
        }

        if (tickInWindow == 0) {
            level().playSound(
                    null,
                    caster.getX(),
                    caster.getY(),
                    caster.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS,
                    0.70f,
                    1.35f
            );
        }

        int remainingWindowTicks = staggerWindowTicks - tickInWindow;
        int remainingMoteCount = totalMoteCount - spawnedAscentMoteCount;
        int motesThisTick = Math.max(1, (remainingMoteCount + remainingWindowTicks - 1) / remainingWindowTicks);
        motesThisTick = Math.min(motesThisTick, remainingMoteCount);
        GlintstoneFx.starRiverAscent(level(), caster, motesThisTick);
        spawnedAscentMoteCount += motesThisTick;
    }

    /**
     * 第一批光点到达汇聚点并开始消失时，钉住星云圆心并刷白色小星星。
     * 气团面片由客户端渲染器画，这里只同步圆心。
     */
    private void maybeSpawnOverheadCloud(LivingEntity caster) {
        if (spawnedOverheadCloud || tickCount < FoundingRainOfStarsTuning.overheadCloudSpawnTick()) {
            return;
        }
        spawnedOverheadCloud = true;
        Vec3 cloudCenter = FoundingRainFx.cloudCenterInFrontOf(caster);
        this.entityData.set(DATA_CLOUD_ACTIVE, true);
        this.entityData.set(DATA_CLOUD_X, (float) cloudCenter.x);
        this.entityData.set(DATA_CLOUD_Y, (float) cloudCenter.y);
        this.entityData.set(DATA_CLOUD_Z, (float) cloudCenter.z);
        this.entityData.set(DATA_CLOUD_YAW_DEGREES, caster.getYRot());
        FoundingRainFx.spawnOverheadStars(level(), cloudCenter, caster.getYRot());
        level().playSound(
                null,
                cloudCenter.x,
                cloudCenter.y,
                cloudCenter.z,
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                0.85f,
                0.72f
        );
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CLOUD_ACTIVE, false);
        builder.define(DATA_CLOUD_X, 0.0f);
        builder.define(DATA_CLOUD_Y, 0.0f);
        builder.define(DATA_CLOUD_Z, 0.0f);
        builder.define(DATA_CLOUD_YAW_DEGREES, 0.0f);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SpawnedAscentMoteCount", spawnedAscentMoteCount);
        tag.putBoolean("SpawnedOverheadCloud", spawnedOverheadCloud);
        tag.putBoolean("CloudActive", isOverheadCloudActive());
        Vec3 cloudCenter = overheadCloudCenter();
        tag.putFloat("CloudX", (float) cloudCenter.x);
        tag.putFloat("CloudY", (float) cloudCenter.y);
        tag.putFloat("CloudZ", (float) cloudCenter.z);
        tag.putFloat("CloudYawDegrees", this.entityData.get(DATA_CLOUD_YAW_DEGREES));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.spawnedAscentMoteCount = tag.getInt("SpawnedAscentMoteCount");
        this.spawnedOverheadCloud = tag.getBoolean("SpawnedOverheadCloud");
        this.entityData.set(DATA_CLOUD_ACTIVE, tag.getBoolean("CloudActive"));
        this.entityData.set(DATA_CLOUD_X, tag.getFloat("CloudX"));
        this.entityData.set(DATA_CLOUD_Y, tag.getFloat("CloudY"));
        this.entityData.set(DATA_CLOUD_Z, tag.getFloat("CloudZ"));
        this.entityData.set(DATA_CLOUD_YAW_DEGREES, tag.getFloat("CloudYawDegrees"));
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}

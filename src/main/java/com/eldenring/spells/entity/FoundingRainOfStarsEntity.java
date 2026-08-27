package com.eldenring.spells.entity;

import com.eldenring.spells.spell.FoundingRainOfStarsSpell;

import com.eldenring.spells.particle.foundingrain.FoundingRainFx;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import io.redspace.ironsspellbooks.damage.DamageSources;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 创星雨时序实体：自身碰撞箱不可见，负责升空节拍、在出手瞬间钉死雨云圆心，并抽白紫雨针。
 * <p>
 * 圆心在构造时按施法者当时的头前偏移写入，之后玩家走动也不改。实体钉在圆心，不跟随施法者。
 * 雨针只负责画面；站在椭圆雨柱里的敌人由本实体按间隔结算伤害。
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
    /**
     * 雨幕每次结算的伤害。由法术在生成本实体时写入，雨针不再各自判伤。
     */
    private float rainZoneDamagePerPulse;

    /**
     * 每个目标上次被这朵雨云打中的 tick。用来挡住同一红闪窗口里的第二次结算。
     */
    private final Map<UUID, Integer> lastRainDamageTickByTargetId = new HashMap<>();

    public FoundingRainOfStarsEntity(
            EntityType<? extends FoundingRainOfStarsEntity> entityType,
            Level level
    ) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvisible(true);
    }

    public FoundingRainOfStarsEntity(Level level, LivingEntity caster, float rainZoneDamagePerPulse) {
        this(ModEntities.FOUNDING_RAIN_OF_STARS.get(), level);
        setOwner(caster);
        this.rainZoneDamagePerPulse = rainZoneDamagePerPulse;
        // 出手瞬间就算死圆心：水平前方 + 眼上高度。后续 tick 不再用玩家当前位置重算。
        Vec3 cloudCenter = FoundingRainFx.cloudCenterInFrontOf(caster);
        setPos(cloudCenter);
        this.entityData.set(DATA_CLOUD_X, (float) cloudCenter.x);
        this.entityData.set(DATA_CLOUD_Y, (float) cloudCenter.y);
        this.entityData.set(DATA_CLOUD_Z, (float) cloudCenter.z);
        this.entityData.set(DATA_CLOUD_YAW_DEGREES, caster.getYRot());
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

        if (level().isClientSide) {
            return;
        }

        maybeLaunchAscentMotes(caster);
        maybeSpawnOverheadCloud();
        maybeSpawnRainDrops(caster);
        maybeApplyRainZoneDamage(caster);

        if (tickCount >= FoundingRainFx.sequenceLifetimeTicks()) {
            discard();
        }
    }

    /**
     * 等待结束后，在错峰窗口内把 {@link FoundingRainFx#ASCENT_MOTE_COUNT} 颗光点分批抽走。
     */
    private void maybeLaunchAscentMotes(LivingEntity caster) {
        int launchDelayTicks = FoundingRainFx.ASCENT_LAUNCH_DELAY_TICKS;
        int staggerWindowTicks = Math.max(1, FoundingRainFx.ASCENT_STAGGER_WINDOW_TICKS);
        int totalMoteCount = FoundingRainFx.ASCENT_MOTE_COUNT;
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
        FoundingRainFx.starRiverAscent(level(), caster, motesThisTick, overheadCloudCenter());
        spawnedAscentMoteCount += motesThisTick;
    }

    /**
     * 第一批光点到达汇聚点时点亮雨云。圆心在构造时已经写好，这里只把 {@code CLOUD_ACTIVE} 打开。
     */
    private void maybeSpawnOverheadCloud() {
        if (spawnedOverheadCloud || tickCount < FoundingRainFx.overheadCloudSpawnTick()) {
            return;
        }
        spawnedOverheadCloud = true;
        this.entityData.set(DATA_CLOUD_ACTIVE, true);
        Vec3 cloudCenter = overheadCloudCenter();
        FoundingRainFx.spawnOverheadStars(
                level(),
                cloudCenter,
                this.entityData.get(DATA_CLOUD_YAW_DEGREES)
        );
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

    /**
     * 雨云钉住之后，在椭圆盘里错峰抽雨针往下掉。
     * 雨点是独立实体：本实体结束后，已经落下的针仍会飞完或撞地。
     */
    private void maybeSpawnRainDrops(LivingEntity caster) {
        if (!isOverheadCloudActive()) {
            return;
        }
        if (tickCount < FoundingRainFx.rainStartTick()
                || tickCount >= FoundingRainFx.rainEndTick()) {
            return;
        }

        Vec3 cloudCenter = overheadCloudCenter();
        float yawRadians = overheadCloudYawRadians();
        Vec3 right = new Vec3(Math.cos(yawRadians), 0.0, Math.sin(yawRadians));
        Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0, Math.cos(yawRadians));
        int dropsThisTick = FoundingRainOfStarsSpell.RAIN_DROPS_PER_TICK;
        for (int dropIndex = 0; dropIndex < dropsThisTick; dropIndex++) {
            Vec3 spawnPosition = sampleRainDropSpawnPosition(cloudCenter, right, forward);
            if (spawnPosition == null) {
                continue;
            }
            spawnRainDrop(caster, spawnPosition);
        }
    }

    /**
     * 在雨云椭圆盘内拒绝采样一个点；多次都落在盘外就放弃这一针，避免边角挤成方块雨。
     */
    private Vec3 sampleRainDropSpawnPosition(Vec3 cloudCenter, Vec3 right, Vec3 forward) {
        double halfWidthBlocks = FoundingRainOfStarsSpell.OVERHEAD_CLOUD_RADIUS_BLOCKS;
        double halfDepthBlocks = FoundingRainFx.OVERHEAD_CLOUD_FORWARD_HALF_BLOCKS;
        for (int attemptIndex = 0; attemptIndex < 6; attemptIndex++) {
            double alongRight = (random.nextDouble() * 2.0 - 1.0) * halfWidthBlocks;
            double alongForward = (random.nextDouble() * 2.0 - 1.0) * halfDepthBlocks;
            double radial01 = Math.sqrt(
                    (alongRight / halfWidthBlocks) * (alongRight / halfWidthBlocks)
                            + (alongForward / Math.max(0.2, halfDepthBlocks))
                            * (alongForward / Math.max(0.2, halfDepthBlocks))
            );
            if (radial01 > 1.0) {
                continue;
            }
            return cloudCenter
                    .add(right.scale(alongRight))
                    .add(forward.scale(alongForward))
                    .add(0.0, -FoundingRainFx.RAIN_DROP_SPAWN_BELOW_CLOUD_BLOCKS, 0.0);
        }
        return null;
    }

    private void spawnRainDrop(LivingEntity caster, Vec3 spawnPosition) {
        FoundingRainDropEntity rainDrop = new FoundingRainDropEntity(level(), caster);
        double tiltSpread = FoundingRainFx.RAIN_DROP_TILT_HORIZONTAL_SPREAD;
        Vec3 fallDirection = new Vec3(
                (random.nextDouble() - 0.5) * 2.0 * tiltSpread,
                -1.0,
                (random.nextDouble() - 0.5) * 2.0 * tiltSpread
        ).normalize();
        rainDrop.setPos(
                spawnPosition.x,
                spawnPosition.y - rainDrop.getBbHeight() * 0.5,
                spawnPosition.z
        );
        rainDrop.shoot(fallDirection);
        float yawDegrees = (float) (Mth.atan2(fallDirection.x, fallDirection.z) * Mth.RAD_TO_DEG);
        float pitchDegrees = (float) (Mth.atan2(fallDirection.y, fallDirection.horizontalDistance()) * Mth.RAD_TO_DEG);
        rainDrop.setYRot(yawDegrees);
        rainDrop.setXRot(pitchDegrees);
        rainDrop.setDamage(0.0f);
        level().addFreshEntity(rainDrop);
    }

    /**
     * 椭圆雨柱内按目标分别结算。同一目标在无敌窗口内只打一次，避免红闪一次叠两下。
     */
    private void maybeApplyRainZoneDamage(LivingEntity caster) {
        if (!isOverheadCloudActive()) {
            return;
        }
        int rainStartTick = FoundingRainFx.rainStartTick();
        if (tickCount < rainStartTick || tickCount >= FoundingRainFx.rainEndTick()) {
            return;
        }

        int damageIntervalTicks = Math.max(1, FoundingRainOfStarsSpell.RAIN_ZONE_DAMAGE_INTERVAL_TICKS);
        Vec3 cloudCenter = overheadCloudCenter();
        double horizontalPadBlocks = Math.max(
                FoundingRainOfStarsSpell.OVERHEAD_CLOUD_RADIUS_BLOCKS,
                FoundingRainFx.OVERHEAD_CLOUD_FORWARD_HALF_BLOCKS
        ) + 0.5;
        AABB searchBox = new AABB(
                cloudCenter.x - horizontalPadBlocks,
                cloudCenter.y - FoundingRainFx.RAIN_DROP_MAXIMUM_TRAVEL_BLOCKS,
                cloudCenter.z - horizontalPadBlocks,
                cloudCenter.x + horizontalPadBlocks,
                cloudCenter.y + 1.0,
                cloudCenter.z + horizontalPadBlocks
        );
        var damageSource = ModSpells.FOUNDING_RAIN_OF_STARS.get().getDamageSource(this, caster);
        for (LivingEntity target : level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                candidate -> canRainDamageTarget(candidate, caster)
        )) {
            if (!isInsideRainColumn(target, cloudCenter)) {
                continue;
            }
            if (!canApplyRainDamageThisTick(target, damageIntervalTicks)) {
                continue;
            }
            DamageSources.applyDamage(target, rainZoneDamagePerPulse, damageSource);
            lastRainDamageTickByTargetId.put(target.getUUID(), tickCount);
        }
    }

    /**
     * 原版受伤后 {@code invulnerableTime} 约 20，前 10 tick 完全免伤。
     * 雨幕自己也记一份间隔，两道门都过才结算。
     */
    private boolean canApplyRainDamageThisTick(LivingEntity target, int damageIntervalTicks) {
        if (target.invulnerableTime > 10) {
            return false;
        }
        Integer lastDamageTick = lastRainDamageTickByTargetId.get(target.getUUID());
        return lastDamageTick == null || tickCount - lastDamageTick >= damageIntervalTicks;
    }

    private boolean canRainDamageTarget(LivingEntity target, LivingEntity caster) {
        if (!target.isAlive() || target.isSpectator() || target == caster) {
            return false;
        }
        return !caster.isAlliedTo(target) && !target.isAlliedTo(caster);
    }

    /**
     * 水平落在雨云椭圆盘内，竖直夹在云层到下方 {@link FoundingRainFx#RAIN_DROP_MAXIMUM_TRAVEL_BLOCKS} 格之间。
     */
    private boolean isInsideRainColumn(LivingEntity target, Vec3 cloudCenter) {
        AABB targetBox = target.getBoundingBox();
        double rainBottomY = cloudCenter.y - FoundingRainFx.RAIN_DROP_MAXIMUM_TRAVEL_BLOCKS;
        if (targetBox.maxY < rainBottomY || targetBox.minY > cloudCenter.y) {
            return false;
        }
        double relativeX = targetBox.getCenter().x - cloudCenter.x;
        double relativeZ = targetBox.getCenter().z - cloudCenter.z;
        float yawRadians = overheadCloudYawRadians();
        double alongRight = relativeX * Math.cos(yawRadians) + relativeZ * Math.sin(yawRadians);
        double alongForward = relativeX * -Math.sin(yawRadians) + relativeZ * Math.cos(yawRadians);
        double halfWidthBlocks = FoundingRainOfStarsSpell.OVERHEAD_CLOUD_RADIUS_BLOCKS;
        double halfDepthBlocks = Math.max(0.2, FoundingRainFx.OVERHEAD_CLOUD_FORWARD_HALF_BLOCKS);
        double normalizedRight = alongRight / halfWidthBlocks;
        double normalizedForward = alongForward / halfDepthBlocks;
        return (normalizedRight * normalizedRight) + (normalizedForward * normalizedForward) <= 1.0;
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
        tag.putFloat("RainZoneDamagePerPulse", rainZoneDamagePerPulse);
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
        if (tag.contains("RainZoneDamagePerPulse")) {
            this.rainZoneDamagePerPulse = tag.getFloat("RainZoneDamagePerPulse");
        } else {
            this.rainZoneDamagePerPulse = tag.getFloat("DamagePerRainDrop");
        }
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

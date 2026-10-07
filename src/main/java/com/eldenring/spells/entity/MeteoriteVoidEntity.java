package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.spell.fx.MeteoriteFx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 陨石 / 艾斯提陨石的虚空裂缝：施法者头顶前方的锚点实体。
 * 客户端由 {@code MeteoriteVoidRenderer} 画不透明锯齿黑洞网格（光影下也清楚），实体 tick 再刷点缀粒子。
 * <p>
 * 服务端由法术每个施法 tick 调 {@link #refreshWhileCasting} 保活并贴回施法者前上方；
 * 连续几 tick 没被刷新（松手 / 打断 / 施法者死亡或换维）就自己收缩消失。
 * 盘面朝向与张开时长走同步数据，客户端粒子不依赖服务端配置。
 */
public class MeteoriteVoidEntity extends Entity {

    /** 盘面朝向的水平偏航角（度，与原版 yRot 同约定）。 */
    private static final EntityDataAccessor<Float> DATA_FACING_YAW_DEGREES =
            SynchedEntityData.defineId(MeteoriteVoidEntity.class, EntityDataSerializers.FLOAT);

    /** 盘面法线相对水平向下压的角度（度）。与陨石当前下坠角一致，黑洞「口」朝着落区。 */
    private static final EntityDataAccessor<Float> DATA_FACING_DESCENT_DEGREES =
            SynchedEntityData.defineId(MeteoriteVoidEntity.class, EntityDataSerializers.FLOAT);

    /** 从出现到完全张开用的 tick。客户端按 tickCount 算张开进度。 */
    private static final EntityDataAccessor<Integer> DATA_OPENING_DURATION_TICKS =
            SynchedEntityData.defineId(MeteoriteVoidEntity.class, EntityDataSerializers.INT);

    /**
     * 连续多少 tick 没被法术刷新就视为吟唱已停（tick）。
     * 留 2 tick 余量，避免玩家 tick 与实体 tick 先后顺序造成误判。
     */
    private static final int MAX_TICKS_WITHOUT_REFRESH = 2;

    /**
     * 客户端最远渲染距离（方块）。碰撞箱只有 0.5 格，原版按碰撞箱算只有约 32 格就不画了；
     * 裂缝是大型施法视觉，和 clientTrackingRange 对齐放宽。
     */
    private static final double MAX_RENDER_DISTANCE_BLOCKS = 96.0;

    /** 服务端：距上次被法术刷新过了几 tick。 */
    private int ticksSinceLastRefresh;

    public MeteoriteVoidEntity(EntityType<? extends MeteoriteVoidEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public MeteoriteVoidEntity(Level level, Vec3 center, float facingYawDegrees, float facingDescentDegrees, int openingDurationTicks) {
        this(ModEntities.METEORITE_VOID.get(), level);
        this.entityData.set(DATA_OPENING_DURATION_TICKS, Math.max(1, openingDurationTicks));
        refreshWhileCasting(center, facingYawDegrees, facingDescentDegrees);
        this.xo = center.x;
        this.yo = center.y;
        this.zo = center.z;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_FACING_YAW_DEGREES, 0.0f);
        this.entityData.define(DATA_FACING_DESCENT_DEGREES, 30.0f);
        this.entityData.define(DATA_OPENING_DURATION_TICKS, 20);
    }

    /** 法术每个施法 tick 调用：保活，贴回锚点并更新盘面朝向。 */
    public void refreshWhileCasting(Vec3 center, float facingYawDegrees, float facingDescentDegrees) {
        this.ticksSinceLastRefresh = 0;
        setPos(center.x, center.y, center.z);
        this.entityData.set(DATA_FACING_YAW_DEGREES, facingYawDegrees);
        this.entityData.set(DATA_FACING_DESCENT_DEGREES, facingDescentDegrees);
    }

    /** 盘面法线（单位向量）：水平朝向再向下压一个下坠角。 */
    public Vec3 facingDirection() {
        return Vec3.directionFromRotation(
                this.entityData.get(DATA_FACING_DESCENT_DEGREES),
                this.entityData.get(DATA_FACING_YAW_DEGREES)
        );
    }

    /** 张开进度 0–1。 */
    public float openingProgress(float partialTick) {
        int openingDurationTicks = Math.max(1, this.entityData.get(DATA_OPENING_DURATION_TICKS));
        return Mth.clamp((tickCount + partialTick) / openingDurationTicks, 0.0f, 1.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            MeteoriteFx.voidAmbient(level(), position(), facingDirection(), openingProgress(0.0f), tickCount);
            return;
        }
        this.ticksSinceLastRefresh++;
        if (this.ticksSinceLastRefresh > MAX_TICKS_WITHOUT_REFRESH) {
            collapseAndDiscard();
        }
    }

    /** 收缩消失：服务端刷一次坍缩粒子再移除。重复调用安全。 */
    public void collapseAndDiscard() {
        if (this.isRemoved()) {
            return;
        }
        if (!level().isClientSide) {
            MeteoriteFx.voidCollapse(level(), position());
        }
        discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSquared) {
        return distanceSquared < MAX_RENDER_DISTANCE_BLOCKS * MAX_RENDER_DISTANCE_BLOCKS;
    }

    /** 只活在一次吟唱里，不进存档。 */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}

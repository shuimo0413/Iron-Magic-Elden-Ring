package com.eldenring.spells.entity;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.spell.RockSlingSpell;
import com.eldenring.spells.spell.combat.RockSlingCombat;
import com.eldenring.spells.spell.fx.RockSlingFx;
import com.eldenring.spells.spell.helper.RockSlingCasting;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 岩石球的一块岩石：蓄力期间悬停在施法者身前对应槽位、由小到大长出来；满蓄后飞向目标。
 * <p>
 * 悬停阶段 {@code noPhysics}、零速度，每 tick 贴回槽位（两端都算，客户端转身不拖影）；
 * 施法者松手 / 死亡 / 换维 / 超时则原地碎裂。发射后做轻度限角追踪，撞实体交给
 * {@link RockSlingCombat} 结算伤害与击退，撞方块只碎裂。
 * <p>
 * 命中粒子 / 命中音由铁魔法 {@code AbstractMagicProjectile#onHit} 统一触发。
 */
public class RockSlingProjectile extends AbstractMagicProjectile {

    /** 本块在横排中的下标（0 = 最左）。客户端算生长错峰要用。 */
    private static final EntityDataAccessor<Integer> DATA_SLOT_INDEX =
            SynchedEntityData.defineId(RockSlingProjectile.class, EntityDataSerializers.INT);

    /** 本次横排总块数。客户端算槽位居中要用。 */
    private static final EntityDataAccessor<Integer> DATA_SLOT_COUNT =
            SynchedEntityData.defineId(RockSlingProjectile.class, EntityDataSerializers.INT);

    /** 是否已发射。 */
    private static final EntityDataAccessor<Boolean> DATA_LAUNCHED =
            SynchedEntityData.defineId(RockSlingProjectile.class, EntityDataSerializers.BOOLEAN);

    /** 铁魔法里本咒的施法 ID，用来判断施法者是否仍在蓄这一招。 */
    private static final String ROCK_SLING_SPELL_ID = EldenRingSpellsMod.MOD_ID + ":rock_sling";

    /**
     * 单块从最小长到满尺寸用的 tick。调大 → 长得更慢更「聚」；须小于蓄力时长，否则飞出时还没长满。
     */
    private static final int GROW_DURATION_TICKS = 18;

    /**
     * 最后一块长满后到蓄力结束预留的 tick。错峰间隔 = (蓄力 − 本值 − 生长时长) / (块数 − 1)。
     */
    private static final int GROW_FINISH_MARGIN_TICKS = 4;

    /** 刚出现时的尺寸（相对满尺寸）。 */
    private static final float GROW_START_SCALE = 0.15f;

    /**
     * 悬停超过「蓄力时长 + 本值」仍未发射就碎裂（tick）。兜底：施法数据丢失或非玩家施法者卡住时不残留。
     */
    private static final int HOVER_TIMEOUT_EXTRA_TICKS = 20;

    /**
     * 开始校验「施法者是否仍在蓄本招」前的宽限 tick。生成当 tick 施法状态可能尚未同步完。
     */
    private static final int CAST_STATE_CHECK_GRACE_TICKS = 2;

    /** 发射后多少 tick 内直飞不转向，避免离手瞬间被拧歪。 */
    private static final int TRACKING_START_DELAY_TICKS = 2;

    /** 命中射线相对目标碰撞箱的外扩（方块）。岩石是实心大块，略宽一点更好打中。 */
    private static final float HIT_DETECTION_INFLATION_BLOCKS = 0.30f;

    /** 发射后的锁定目标；失效只改直飞，不销毁。 */
    @Nullable
    private UUID lockedTargetUuid;

    /** 发射后已飞行路程（方块）。用于射程截断。 */
    private double traveledDistanceBlocks;

    /** 发射那一刻的 {@code tickCount}（仅服务端用）。追踪延迟按「离手后多久」算，不含悬停时长。 */
    private int launchedAtTick;

    public RockSlingProjectile(EntityType<? extends RockSlingProjectile> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public RockSlingProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.ROCK_SLING.get(), level);
        setOwner(shooter);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SLOT_INDEX, 0);
        builder.define(DATA_SLOT_COUNT, 1);
        builder.define(DATA_LAUNCHED, false);
    }

    /** 写入横排槽位（生成时调用）。 */
    public void setSlot(int slotIndex, int slotCount) {
        int clampedCount = Math.max(1, slotCount);
        entityData.set(DATA_SLOT_COUNT, clampedCount);
        entityData.set(DATA_SLOT_INDEX, Mth.clamp(slotIndex, 0, clampedCount - 1));
    }

    public int slotIndex() {
        return entityData.get(DATA_SLOT_INDEX);
    }

    public int slotCount() {
        return entityData.get(DATA_SLOT_COUNT);
    }

    public boolean hasLaunched() {
        return entityData.get(DATA_LAUNCHED);
    }

    /** 已知岩石中心时反推实体原点（脚底）。 */
    public Vec3 feetPositionForCenter(Vec3 centerWorldPosition) {
        return centerWorldPosition.subtract(0.0, getBbHeight() * 0.5, 0.0);
    }

    /** 岩石视觉中心（世界坐标）。 */
    public Vec3 centerWorldPosition() {
        return position().add(0.0, getBbHeight() * 0.5, 0.0);
    }

    /**
     * 生长进度 0–1。按槽位错峰：左边先长，右边后长；发射后恒为 1。
     */
    public float growthProgress(float partialTick) {
        if (hasLaunched()) {
            return 1.0f;
        }
        int rockCount = Math.max(1, slotCount());
        float staggerTicks = rockCount > 1
                ? Math.max(0.0f, RockSlingSpell.SPELL_CAST_TIME_TICKS - GROW_FINISH_MARGIN_TICKS - GROW_DURATION_TICKS)
                / (float) (rockCount - 1)
                : 0.0f;
        float growthAgeTicks = tickCount + partialTick - slotIndex() * staggerTicks;
        return Mth.clamp(growthAgeTicks / GROW_DURATION_TICKS, 0.0f, 1.0f);
    }

    /**
     * 渲染尺寸倍率（相对满尺寸）。三次缓出：前段猛涨、后段收拢，读起来像碎石被吸到一起。
     */
    public float renderScale(float partialTick) {
        float progress = growthProgress(partialTick);
        float easedProgress = 1.0f - (1.0f - progress) * (1.0f - progress) * (1.0f - progress);
        return Mth.lerp(easedProgress, GROW_START_SCALE, 1.0f);
    }

    @Override
    public void trailParticles() {
        if (!hasLaunched()) {
            RockSlingFx.chargeGather(level(), centerWorldPosition(), growthProgress(0.0f));
            return;
        }
        RockSlingFx.trail(level(), centerWorldPosition(), getDeltaMovement());
    }

    @Override
    public void impactParticles(double impactX, double impactY, double impactZ) {
        RockSlingFx.impact(level(), new Vec3(impactX, impactY, impactZ));
    }

    @Override
    public float getSpeed() {
        return RockSlingSpell.PROJECTILE_FLIGHT_SPEED;
    }

    @Override
    public float getHitDetectionInflation() {
        return HIT_DETECTION_INFLATION_BLOCKS;
    }

    @Override
    public Optional<Holder<SoundEvent>> getImpactSound() {
        return Optional.of(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.DEEPSLATE_BREAK));
    }

    @Override
    public void tick() {
        if (!isFinitePositionAndMotion()) {
            discard();
            return;
        }
        if (!hasLaunched()) {
            tickHovering();
            return;
        }
        Vec3 positionBeforeTick = position();
        super.tick();
        if (this.isRemoved()) {
            return;
        }
        traveledDistanceBlocks += position().distanceTo(positionBeforeTick);
        if (!level().isClientSide && traveledDistanceBlocks >= RockSlingSpell.PROJECTILE_MAX_RANGE_BLOCKS) {
            shatterAndDiscard();
        }
    }

    /**
     * 悬停：先校验是否该碎裂，再零速度 tick 基类（客户端借此刷蓄力粒子），最后贴回槽位。
     */
    private void tickHovering() {
        LivingEntity owner = getOwner() instanceof LivingEntity livingOwner ? livingOwner : null;
        if (!level().isClientSide && shouldShatterWhileHovering(owner)) {
            shatterAndDiscard();
            return;
        }
        setDeltaMovement(Vec3.ZERO);
        this.noPhysics = true;
        super.tick();
        if (this.isRemoved() || owner == null) {
            return;
        }
        setPos(feetPositionForCenter(RockSlingCasting.slotCenterWorldPosition(owner, slotIndex(), slotCount())));
    }

    private boolean shouldShatterWhileHovering(@Nullable LivingEntity owner) {
        if (owner == null || !owner.isAlive() || owner.level() != level()) {
            return true;
        }
        if (tickCount > RockSlingSpell.SPELL_CAST_TIME_TICKS + HOVER_TIMEOUT_EXTRA_TICKS) {
            return true;
        }
        return tickCount > CAST_STATE_CHECK_GRACE_TICKS && !isOwnerStillChargingRockSling(owner);
    }

    /**
     * 施法者是否仍在蓄岩石球。只有玩家 / 铁魔法施法生物带可靠的施法状态；
     * 其他生物返回 true，交给悬停超时兜底。
     */
    private static boolean isOwnerStillChargingRockSling(LivingEntity owner) {
        MagicData magicData;
        if (owner instanceof Player) {
            magicData = MagicData.getPlayerMagicData(owner);
        } else if (owner instanceof IMagicEntity magicEntity) {
            magicData = magicEntity.getMagicData();
        } else {
            return true;
        }
        return magicData != null
                && magicData.isCasting()
                && ROCK_SLING_SPELL_ID.equals(magicData.getCastingSpellId());
    }

    /**
     * 满蓄发射。有目标则朝目标瞄准点飞并锁定；否则飞向准星汇聚点（三块从两侧收拢）。
     *
     * @param target           发射目标，可为 null
     * @param convergencePoint 无目标时的汇聚点；也为 null 时沿施法者视线
     */
    public void launch(@Nullable LivingEntity target, @Nullable Vec3 convergencePoint) {
        Vec3 rockCenter = centerWorldPosition();
        Vec3 aimPoint = target != null ? RockSlingCasting.aimPointOnTarget(target) : convergencePoint;
        Vec3 shootDirection = aimPoint != null ? aimPoint.subtract(rockCenter) : Vec3.ZERO;
        if (shootDirection.lengthSqr() < 1.0e-6) {
            Entity owner = getOwner();
            shootDirection = owner != null ? owner.getLookAngle() : new Vec3(0.0, 0.0, 1.0);
        }
        shootDirection = shootDirection.normalize();

        this.lockedTargetUuid = target != null ? target.getUUID() : null;
        this.noPhysics = false;
        this.traveledDistanceBlocks = 0.0;
        this.launchedAtTick = tickCount;
        entityData.set(DATA_LAUNCHED, true);
        shoot(shootDirection);
        float yawDegrees = (float) (Mth.atan2(shootDirection.x, shootDirection.z) * Mth.RAD_TO_DEG);
        float pitchDegrees = (float) (Mth.atan2(shootDirection.y, shootDirection.horizontalDistance()) * Mth.RAD_TO_DEG);
        setYRot(yawDegrees);
        setXRot(pitchDegrees);
        this.yRotO = yawDegrees;
        this.xRotO = pitchDegrees;
    }

    /** 原地碎裂：石屑 + 碎裂音，然后消失。仅服务端生效。 */
    public void shatterAndDiscard() {
        if (!level().isClientSide) {
            Vec3 center = centerWorldPosition();
            RockSlingFx.shatter(level(), center, renderScale(0.0f));
            getImpactSound().ifPresent(this::doImpactSound);
        }
        discard();
    }

    /** 悬停时不按速度平移。 */
    @Override
    public void travel() {
        if (!hasLaunched()) {
            return;
        }
        super.travel();
    }

    /** 悬停时零速度，别让 atan2 把朝向拧成 0。 */
    @Override
    protected void rotateWithMotion() {
        if (!hasLaunched()) {
            return;
        }
        super.rotateWithMotion();
    }

    /** 发射后的轻度限角追踪：只追发射时锁定的目标，目标失效就直飞。 */
    @Override
    protected void handleEntityHoming() {
        if (!hasLaunched() || level().isClientSide || lockedTargetUuid == null) {
            return;
        }
        if (tickCount - launchedAtTick < TRACKING_START_DELAY_TICKS) {
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)
                || !(serverLevel.getEntity(lockedTargetUuid) instanceof LivingEntity target)
                || !RockSlingCasting.isValidTarget(target, getOwner())) {
            lockedTargetUuid = null;
            return;
        }

        Vec3 currentDeltaMovement = getDeltaMovement();
        double currentSpeed = currentDeltaMovement.length();
        if (currentSpeed < 1.0e-4) {
            return;
        }
        Vec3 towardTarget = RockSlingCasting.aimPointOnTarget(target).subtract(centerWorldPosition());
        double distanceToTarget = towardTarget.length();
        if (distanceToTarget < 1.0e-4) {
            return;
        }
        Vec3 currentFlightDirection = currentDeltaMovement.scale(1.0 / currentSpeed);
        Vec3 desiredDirection = towardTarget.scale(1.0 / distanceToTarget);
        double angleBetweenRadians = Math.acos(Mth.clamp(currentFlightDirection.dot(desiredDirection), -1.0, 1.0));
        double maxTurnAngleRadians = Math.toRadians(RockSlingSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK);
        float slerpFactor = angleBetweenRadians < 1.0e-5
                ? 1.0f
                : (float) Math.min(1.0, maxTurnAngleRadians / angleBetweenRadians);
        Vec3 limitedTurnDirection = Utils.slerp(slerpFactor, currentFlightDirection, desiredDirection).normalize();
        if (!Double.isFinite(limitedTurnDirection.x)
                || !Double.isFinite(limitedTurnDirection.y)
                || !Double.isFinite(limitedTurnDirection.z)) {
            return;
        }
        setDeltaMovement(limitedTurnDirection.scale(currentSpeed));
    }

    /** 实体 / 方块同 tick 结算；悬停时完全不判定。 */
    @Override
    public void handleHitDetection() {
        if (!hasLaunched()) {
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
        if (blockCollision.getType() != HitResult.Type.MISS) {
            destination = blockCollision.getLocation();
        }

        AABB searchBox = getBoundingBox().expandTowards(destination.subtract(startPosition)).inflate(0.1);
        List<HitResult> entityHits = new ArrayList<>();
        for (Entity target : level().getEntities(this, searchBox, this::canHitEntity)) {
            HitResult hit = Utils.checkEntityIntersecting(target, startPosition, destination, getHitDetectionInflation());
            if (hit.getType() != HitResult.Type.MISS) {
                entityHits.add(hit);
            }
        }
        entityHits.sort(Comparator.comparingDouble(hit -> hit.getLocation().distanceToSqr(startPosition)));
        for (HitResult hitResult : entityHits) {
            if (hitResult instanceof EntityHitResult entityHitResult
                    && !NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(this, entityHitResult)).isCanceled()) {
                onHit(entityHitResult);
            }
            if (this.isRemoved()) {
                return;
            }
        }

        if (blockCollision.getType() != HitResult.Type.MISS
                && !this.isRemoved()
                && !NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(this, blockCollision)).isCanceled()) {
            onHit(blockCollision);
        }
    }

    /** 同一施法者的岩石互不相撞。 */
    @Override
    protected boolean canHitEntity(@NotNull Entity targetEntity) {
        if (targetEntity instanceof RockSlingProjectile otherRock) {
            Entity thisOwner = getOwner();
            if (thisOwner != null && thisOwner == otherRock.getOwner()) {
                return false;
            }
        }
        return super.canHitEntity(targetEntity);
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        if (this.isRemoved()) {
            return;
        }
        super.onHitBlock(blockHitResult);
        discard();
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        if (this.isRemoved()) {
            return;
        }
        super.onHitEntity(entityHitResult);
        if (!level().isClientSide) {
            RockSlingCombat.resolveEntityHit(this, entityHitResult.getEntity());
        }
        discard();
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

    /** 悬停 / 飞行都是一次性的，不进存档。 */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}

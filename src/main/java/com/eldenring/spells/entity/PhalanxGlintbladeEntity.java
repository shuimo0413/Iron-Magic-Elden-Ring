package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.spell.GlintbladePhalanxSpell;
import com.eldenring.spells.spell.MagicGlintbladeSpell;
import com.eldenring.spells.spell.curve.GlintbladePhalanxCastCurve;
import com.eldenring.spells.spell.fx.GlintbladePhalanxFx;
import com.eldenring.spells.spell.helper.GlintbladePhalanxHelper;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 圆阵辉剑：钉在头上半圆槽位跟手、随 yaw 转向，玩家附近有敌人就射出。
 * <p>
 * 飞行 / 命中 / 碰方块破碎复用 {@link MagicGlintbladeEntity}；模型也是同一把剑。
 * 槽位几何问 {@link GlintbladePhalanxHelper}。
 */
public class PhalanxGlintbladeEntity extends MagicGlintbladeEntity {

    /** 半圆槽位序号，客户端算外法线 / 缩放错峰要用。 */
    private static final EntityDataAccessor<Integer> DATA_SLOT_INDEX =
            SynchedEntityData.defineId(PhalanxGlintbladeEntity.class, EntityDataSerializers.INT);

    /** 这一圈总共几把。客户端与 helper 用同一公式排阵。 */
    private static final EntityDataAccessor<Integer> DATA_SLOT_COUNT =
            SynchedEntityData.defineId(PhalanxGlintbladeEntity.class, EntityDataSerializers.INT);

    /** 半圆半径（方块 × 100）同步，避免客户端用错半径导致剑抖。 */
    private static final EntityDataAccessor<Integer> DATA_ORBIT_RADIUS_HUNDREDTHS =
            SynchedEntityData.defineId(PhalanxGlintbladeEntity.class, EntityDataSerializers.INT);

    /**
     * 相对辉剑网格的视觉倍率 × 100。100 = 原尺寸，190 = 巨剑阵。
     * 客户端渲染 / 命中外扩都读这份，避免服务端放大了客户端还是小剑。
     */
    private static final EntityDataAccessor<Integer> DATA_SWORD_VISUAL_SCALE_HUNDREDTHS =
            SynchedEntityData.defineId(PhalanxGlintbladeEntity.class, EntityDataSerializers.INT);

    private double triggerRangeBlocks = GlintbladePhalanxSpell.AUTO_LAUNCH_RANGE_BLOCKS;
    private int hoverLifetimeTicks = GlintbladePhalanxSpell.HOVER_LIFETIME_TICKS;
    private float projectileFlightSpeed = GlintbladePhalanxSpell.PROJECTILE_FLIGHT_SPEED;
    private double projectileTrackingRangeBlocks = GlintbladePhalanxSpell.PROJECTILE_TRACKING_RANGE_BLOCKS;
    private float projectileMaxTurnAngleDegreesPerTick =
            GlintbladePhalanxSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK;
    @Nullable
    private AbstractSpell damageSpell;

    public PhalanxGlintbladeEntity(EntityType<? extends PhalanxGlintbladeEntity> entityType, Level level) {
        super(entityType, level);
    }

    public PhalanxGlintbladeEntity(Level level, LivingEntity shooter) {
        this(ModEntities.PHALANX_GLINTBLADE.get(), level);
        setOwner(shooter);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SLOT_INDEX, 0);
        builder.define(DATA_SLOT_COUNT, GlintbladePhalanxSpell.BLADE_COUNT);
        builder.define(
                DATA_ORBIT_RADIUS_HUNDREDTHS,
                hundredthsFromBlocks(GlintbladePhalanxCastCurve.ORBIT_RADIUS_BLOCKS)
        );
        builder.define(
                DATA_SWORD_VISUAL_SCALE_HUNDREDTHS,
                hundredthsFromScale(GlintbladePhalanxCastCurve.SWORD_VISUAL_SCALE)
        );
    }

    /**
     * 生成后写入圆阵参数。helper 调；三种圆阵都走这里。
     */
    public void configurePhalanx(
            GlintbladePhalanxHelper.SpawnSpec spawnSpec,
            int slotIndex,
            int bladeCount
    ) {
        entityData.set(DATA_SLOT_INDEX, Math.max(0, slotIndex));
        entityData.set(DATA_SLOT_COUNT, Math.max(1, bladeCount));
        entityData.set(DATA_ORBIT_RADIUS_HUNDREDTHS, hundredthsFromBlocks(spawnSpec.orbitRadiusBlocks()));
        entityData.set(DATA_SWORD_VISUAL_SCALE_HUNDREDTHS, hundredthsFromScale(spawnSpec.swordVisualScale()));
        this.triggerRangeBlocks = spawnSpec.triggerRangeBlocks();
        this.hoverLifetimeTicks = Math.max(1, spawnSpec.hoverLifetimeTicks());
        this.projectileFlightSpeed = spawnSpec.projectileFlightSpeed();
        this.projectileTrackingRangeBlocks = spawnSpec.projectileTrackingRangeBlocks();
        this.projectileMaxTurnAngleDegreesPerTick = spawnSpec.projectileMaxTurnAngleDegreesPerTick();
        this.damageSpell = spawnSpec.sourceSpell();
        setDamage(spawnSpec.damagePerBlade());
        refreshDimensions();
    }

    public int phalanxSlotIndex() {
        return entityData.get(DATA_SLOT_INDEX);
    }

    public int phalanxSlotCount() {
        return entityData.get(DATA_SLOT_COUNT);
    }

    public double orbitRadiusBlocks() {
        return entityData.get(DATA_ORBIT_RADIUS_HUNDREDTHS) / 100.0;
    }

    /**
     * 相对辉剑网格的视觉倍率。100 分之一格同步，避免浮点在客户端对不上。
     */
    public float swordVisualScale() {
        return entityData.get(DATA_SWORD_VISUAL_SCALE_HUNDREDTHS) / 100.0f;
    }

    @Override
    public float renderSwordVisualScale() {
        return swordVisualScale();
    }

    @Override
    public float getHitDetectionInflation() {
        return MagicGlintbladeSpell.HIT_DETECTION_INFLATION_BLOCKS * swordVisualScale();
    }

    @Override
    public boolean usesOutwardHoverPose() {
        return !hasLaunched();
    }

    @Override
    public float renderHoverSwordScale(float ageTicks) {
        return GlintbladePhalanxCastCurve.swordScale(ageTicks);
    }

    @Override
    public Vec3 hoverBladeTipWorldDirection() {
        if (getOwner() instanceof LivingEntity livingOwner && livingOwner.isAlive()) {
            Vec3 lookDirection = livingOwner.getLookAngle();
            if (lookDirection.lengthSqr() > 1.0e-8) {
                return lookDirection.normalize();
            }
        }
        return super.hoverBladeTipWorldDirection();
    }

    @Override
    public GlintstoneTrailStyle trailStyle() {
        GlintstoneTrailStyle baseStyle = GlintbladePhalanxSpell.TRAIL_STYLE;
        float visualScale = swordVisualScale();
        if (visualScale <= 1.02f) {
            return baseStyle;
        }
        return new GlintstoneTrailStyle(
                baseStyle.lengthBlocks() * visualScale,
                baseStyle.headHalfWidthBlocks() * visualScale,
                baseStyle.tailHalfWidthBlocks() * visualScale,
                baseStyle.sparkChance(),
                baseStyle.moteChance(),
                baseStyle.maximumHistoryPointCount(),
                baseStyle.helixStyle(),
                baseStyle.additiveCore(),
                baseStyle.extraOuterVeil()
        );
    }

    @Override
    public void trailParticles() {
        if (!hasLaunched()) {
            GlintbladePhalanxFx.tickWhileOrbiting(this, level());
            return;
        }
        super.trailParticles();
    }

    @Override
    protected boolean shouldDiscardForLifetime() {
        if (!hasLaunched()) {
            return tickCount >= hoverLifetimeTicks;
        }
        return ticksSinceLaunch() >= GlintbladePhalanxCastCurve.FLIGHT_LIFETIME_TICKS;
    }

    @Override
    protected boolean keepHovering() {
        LivingEntity owner = getOwner() instanceof LivingEntity livingOwner ? livingOwner : null;
        return owner != null && owner.isAlive() && owner.level() == level();
    }

    @Override
    protected void snapHoverFollowPose() {
        LivingEntity owner = getOwner() instanceof LivingEntity livingOwner ? livingOwner : null;
        if (owner == null) {
            return;
        }
        Vec3 slotPosition = GlintbladePhalanxHelper.slotWorldPosition(
                owner,
                phalanxSlotIndex(),
                phalanxSlotCount(),
                orbitRadiusBlocks(),
                GlintbladePhalanxCastCurve.ORBIT_FORWARD_OFFSET_BLOCKS
        );
        setPos(slotPosition);
        setStoredLaunchDirection(owner.getLookAngle());
        lockHoverFacing(owner.getYRot(), owner.getXRot());
    }

    @Override
    protected boolean shouldLaunchNow() {
        if (tickCount < GlintbladePhalanxCastCurve.readyToLaunchTick(phalanxSlotIndex())) {
            return false;
        }
        return findLaunchTarget() != null;
    }

    /**
     * 以主人为圆心、{@link #triggerRangeBlocks} 内最近的可打生物。
     * 不走魔法辉剑的准星锥：圆阵是护体，背后靠近也要射。
     * 隔墙的目标跳过，避免五把剑全部撞在方块上碎掉。
     */
    @Override
    @Nullable
    protected LivingEntity findLaunchTarget() {
        Entity ownerEntity = getOwner();
        LivingEntity owner = ownerEntity instanceof LivingEntity livingOwner ? livingOwner : null;
        Vec3 searchOrigin = owner != null ? owner.getEyePosition() : getBoundingBox().getCenter();
        AABB searchBox = new AABB(searchOrigin, searchOrigin).inflate(triggerRangeBlocks);
        LivingEntity bestTarget = null;
        double bestDistanceSquared = Double.MAX_VALUE;
        for (LivingEntity candidate : level().getEntitiesOfClass(LivingEntity.class, searchBox, living ->
                isValidCombatTarget(living, ownerEntity)
        )) {
            double ownerDistanceSquared = owner != null
                    ? owner.distanceToSqr(candidate)
                    : distanceToSqr(candidate);
            if (ownerDistanceSquared > triggerRangeBlocks * triggerRangeBlocks) {
                continue;
            }
            if (!hasLineOfSightFromOwner(owner, candidate)) {
                continue;
            }
            double bladeDistanceSquared = distanceToSqr(candidate);
            if (bladeDistanceSquared < bestDistanceSquared) {
                bestDistanceSquared = bladeDistanceSquared;
                bestTarget = candidate;
            }
        }
        return bestTarget;
    }

    /**
     * 主人眼睛到目标瞄准点是否被方块挡住。没有主人时退回「从剑到目标」。
     */
    private boolean hasLineOfSightFromOwner(@Nullable LivingEntity owner, LivingEntity target) {
        Vec3 startPosition = owner != null ? owner.getEyePosition() : getBoundingBox().getCenter();
        Vec3 endPosition = aimPointOnTarget(target);
        HitResult blockHit = level().clip(new ClipContext(
                startPosition,
                endPosition,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                owner != null ? owner : this
        ));
        return blockHit.getType() == HitResult.Type.MISS;
    }

    @Override
    protected float projectileFlightSpeed() {
        return projectileFlightSpeed;
    }

    @Override
    protected double projectileTrackingRangeBlocks() {
        return projectileTrackingRangeBlocks;
    }

    @Override
    protected float projectileMaxTurnAngleDegreesPerTick() {
        return projectileMaxTurnAngleDegreesPerTick;
    }

    @Override
    protected AbstractSpell damageSourceSpell() {
        if (damageSpell != null) {
            return damageSpell;
        }
        return ModSpells.GLINTBLADE_PHALANX.get();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SlotIndex", phalanxSlotIndex());
        tag.putInt("SlotCount", phalanxSlotCount());
        tag.putInt("OrbitRadiusHundredths", entityData.get(DATA_ORBIT_RADIUS_HUNDREDTHS));
        tag.putInt("SwordScaleHundredths", entityData.get(DATA_SWORD_VISUAL_SCALE_HUNDREDTHS));
        tag.putDouble("TriggerRange", triggerRangeBlocks);
        tag.putInt("HoverLifetime", hoverLifetimeTicks);
        tag.putFloat("FlightSpeed", projectileFlightSpeed);
        tag.putDouble("TrackingRange", projectileTrackingRangeBlocks);
        tag.putFloat("TurnAngle", projectileMaxTurnAngleDegreesPerTick);
        if (damageSpell != null) {
            tag.putString("DamageSpell", damageSpell.getSpellResource().toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_SLOT_INDEX, tag.getInt("SlotIndex"));
        entityData.set(DATA_SLOT_COUNT, Math.max(1, tag.getInt("SlotCount")));
        if (tag.contains("OrbitRadiusHundredths")) {
            entityData.set(DATA_ORBIT_RADIUS_HUNDREDTHS, tag.getInt("OrbitRadiusHundredths"));
        }
        if (tag.contains("SwordScaleHundredths")) {
            entityData.set(DATA_SWORD_VISUAL_SCALE_HUNDREDTHS, tag.getInt("SwordScaleHundredths"));
        }
        this.triggerRangeBlocks = tag.getDouble("TriggerRange");
        this.hoverLifetimeTicks = Math.max(1, tag.getInt("HoverLifetime"));
        if (tag.contains("FlightSpeed")) {
            this.projectileFlightSpeed = tag.getFloat("FlightSpeed");
        }
        if (tag.contains("TrackingRange")) {
            this.projectileTrackingRangeBlocks = tag.getDouble("TrackingRange");
        }
        if (tag.contains("TurnAngle")) {
            this.projectileMaxTurnAngleDegreesPerTick = tag.getFloat("TurnAngle");
        }
        if (tag.contains("DamageSpell")) {
            ResourceLocation spellId = ResourceLocation.tryParse(tag.getString("DamageSpell"));
            if (spellId != null) {
                this.damageSpell = SpellRegistry.getSpell(spellId);
            }
        }
        refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_SWORD_VISUAL_SCALE_HUNDREDTHS.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(swordVisualScale());
    }

    private static int hundredthsFromBlocks(double blocks) {
        return Mth.clamp((int) Math.round(blocks * 100.0), 1, 1000);
    }

    /**
     * 视觉倍率写入同步整数。1.0 → 100；巨剑阵 1.9 → 190。
     */
    private static int hundredthsFromScale(float visualScale) {
        return Mth.clamp((int) Math.round(visualScale * 100.0f), 10, 400);
    }
}

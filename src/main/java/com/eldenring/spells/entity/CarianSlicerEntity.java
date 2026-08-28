package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.spell.curve.CarianSlicerCastCurve;
import com.eldenring.spells.spell.combat.CarianSlicerCombat;
import com.eldenring.spells.spell.fx.CarianSlicerFx;
import com.eldenring.spells.spell.CarianSlicerSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 卡利亚迅剑实体：跟在施法者身前握点，按周期左右挥砍。
 * <p>
 * tick 只问 {@link CarianSlicerCastCurve} → {@link CarianSlicerCombat} → {@link CarianSlicerFx}。
 * 不是弹道。碰撞箱只作客户端追踪占位。
 */
public class CarianSlicerEntity extends Projectile implements AntiMagicSusceptible {

    private static final EntityDataAccessor<Integer> DATA_COMBO_INDEX =
            SynchedEntityData.defineId(CarianSlicerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SWING_START_TICK =
            SynchedEntityData.defineId(CarianSlicerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FINISHING =
            SynchedEntityData.defineId(CarianSlicerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_FADE_START_TICK =
            SynchedEntityData.defineId(CarianSlicerEntity.class, EntityDataSerializers.INT);

    private float slashDamage;
    private boolean slashResolved;
    private boolean stopRequested;
    private int lastHoldRefreshTick;

    public CarianSlicerEntity(EntityType<? extends CarianSlicerEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public CarianSlicerEntity(Level level, LivingEntity caster, float slashDamage) {
        this(ModEntities.CARIAN_SLICER.get(), level);
        setOwner(caster);
        this.slashDamage = slashDamage;
        snapToOwnerGrip(caster);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_COMBO_INDEX, 0);
        builder.define(DATA_SWING_START_TICK, 0);
        builder.define(DATA_FINISHING, false);
        builder.define(DATA_FADE_START_TICK, 0);
    }

    public void setSlashDamage(float slashDamage) {
        this.slashDamage = slashDamage;
    }

    /**
     * 吟唱还在继续时每 tick 调用。超时没刷新就不再连下一刀。
     * 已经 {@link #requestStop()} 之后不再清停止标志，避免松手后又被晚到的 tick 续上。
     */
    public void refreshWhileCasting() {
        if (this.stopRequested || isFinishing()) {
            return;
        }
        this.lastHoldRefreshTick = this.tickCount;
    }

    /**
     * 松手 / 吟唱结束：当前这一刀照常打完，然后淡出，不再起下一刀。
     */
    public void requestStop() {
        this.stopRequested = true;
    }

    public int getComboIndex() {
        return this.entityData.get(DATA_COMBO_INDEX);
    }

    public boolean isBackhandSlash() {
        return (getComboIndex() & 1) == 1;
    }

    public boolean isFinishing() {
        return this.entityData.get(DATA_FINISHING);
    }

    @Override
    public void tick() {
        super.tick();
        int maxLifetimeTicks = CarianSlicerCastCurve.entityMaxLifetimeTicks(CarianSlicerSpell.SPELL_CAST_TIME_TICKS);
        if (tickCount >= maxLifetimeTicks) {
            discard();
            return;
        }

        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        if (owner != null && owner.isAlive()) {
            snapToOwnerGrip(owner);
        }

        if (isFinishing()) {
            int fadeAge = tickCount - this.entityData.get(DATA_FADE_START_TICK);
            if (fadeAge >= CarianSlicerCastCurve.SLASH_FADE_TICKS) {
                discard();
            }
            return;
        }

        int swingAge = tickCount - this.entityData.get(DATA_SWING_START_TICK);
        if (!level().isClientSide && CarianSlicerCastCurve.isHitWindow(swingAge, slashResolved)) {
            LivingEntity livingOwner = getOwner() instanceof LivingEntity living ? living : null;
            Vec3 slashOrigin = livingOwner != null
                    ? livingOwner.getEyePosition().add(livingOwner.getLookAngle().scale(0.35))
                    : position();
            Vec3 lookDirection = livingOwner != null ? livingOwner.getLookAngle() : getLookAngle();
            CarianSlicerFx.playSlashSounds(level(), slashOrigin);
            CarianSlicerCombat.resolve(this, level(), slashOrigin, lookDirection, livingOwner, slashDamage);
            slashResolved = true;
        }
        if (level().isClientSide && swingAge >= CarianSlicerCastCurve.SWING_START_TICK) {
            CarianSlicerFx.spawnSwingParticles(this, level());
        }
        if (!level().isClientSide && swingAge >= CarianSlicerSpell.SLASH_CYCLE_TICKS) {
            if (shouldContinueCombo()) {
                beginNextSlash();
            } else {
                beginFade();
            }
        }
    }

    private boolean shouldContinueCombo() {
        if (stopRequested) {
            return false;
        }
        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        if (owner == null || !owner.isAlive()) {
            return false;
        }
        return tickCount - lastHoldRefreshTick <= CarianSlicerCastCurve.HOLD_STALE_TICKS;
    }

    private void beginNextSlash() {
        int nextIndex = getComboIndex() + 1;
        this.entityData.set(DATA_COMBO_INDEX, nextIndex);
        this.entityData.set(DATA_SWING_START_TICK, tickCount);
        this.slashResolved = false;
    }

    private void beginFade() {
        this.entityData.set(DATA_FINISHING, true);
        this.entityData.set(DATA_FADE_START_TICK, tickCount);
    }

    /**
     * 把实体锚到施法者身前握点。水平朝向用 {@code yBodyRot}，不用视线压平，避免走路/抬头抽搐。
     */
    private void snapToOwnerGrip(LivingEntity owner) {
        Vec3 gripWorld = computeGripWorld(owner);
        setPos(gripWorld.x, gripWorld.y, gripWorld.z);
        setYRot(owner.yBodyRot);
        setXRot(owner.getXRot());
    }

    /**
     * 握点世界坐标：脚底 + 身体前方 + 右侧 + 高。
     */
    public static Vec3 computeGripWorld(LivingEntity owner) {
        Vec3 forwardFlat = horizontalForwardFromBody(owner);
        Vec3 rightFlat = new Vec3(-forwardFlat.z, 0.0, forwardFlat.x);
        return owner.position()
                .add(forwardFlat.scale(CarianSlicerCastCurve.GRIP_FORWARD_OFFSET_BLOCKS))
                .add(rightFlat.scale(CarianSlicerCastCurve.GRIP_RIGHT_OFFSET_BLOCKS))
                .add(0.0, CarianSlicerCastCurve.GRIP_HEIGHT_BLOCKS, 0.0);
    }

    /**
     * 身体水平朝前。走路时仍是有效 yaw，抬头低头也不会退化成噪声。
     */
    private static Vec3 horizontalForwardFromBody(LivingEntity owner) {
        float bodyYawRadians = owner.yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(bodyYawRadians), 0.0, Mth.cos(bodyYawRadians));
    }

    /**
     * 当前这一刀的挥砍进度 0–1。
     */
    public float getSwingProgress(float partialTicks) {
        float swingAge = (tickCount + partialTicks) - this.entityData.get(DATA_SWING_START_TICK);
        return CarianSlicerCastCurve.swingProgress(swingAge);
    }

    /**
     * 斩完淡出：1 = 完全不透明，0 = 消失。连斩过程中始终为 1。
     */
    public float getFadeAlpha(float partialTicks) {
        if (!isFinishing()) {
            return 1.0f;
        }
        float fadeAge = (tickCount + partialTicks) - this.entityData.get(DATA_FADE_START_TICK);
        return CarianSlicerCastCurve.fadeAlpha(fadeAge);
    }

    public float getSwingStartRollDegrees() {
        return CarianSlicerCastCurve.startRollDegrees(isBackhandSlash());
    }

    public float getSwingEndRollDegrees() {
        return CarianSlicerCastCurve.endRollDegrees(isBackhandSlash());
    }

    public float getSwingStartPitchDegrees() {
        return CarianSlicerCastCurve.startPitchDegrees(isBackhandSlash());
    }

    public float getSwingEndPitchDegrees() {
        return CarianSlicerCastCurve.endPitchDegrees(isBackhandSlash());
    }

    /**
     * 当前刃尖世界坐标（粒子用，不插值）。
     */
    public Vec3 computeBladeTipWorld(float swingProgress, float partialTicks) {
        Vec3 origin = getPosition(partialTicks);
        float yawDegrees = Mth.lerp(partialTicks, this.yRotO, getYRot());
        float lookPitchDegrees = Mth.lerp(partialTicks, this.xRotO, getXRot());
        return computeBladePointWorld(
                swingProgress,
                CarianSlicerCastCurve.BLADE_LENGTH_BLOCKS,
                origin,
                yawDegrees,
                lookPitchDegrees
        );
    }

    /**
     * 刃上一点的世界坐标。旋转顺序与 {@code CarianSlicerRenderer} 的 PoseStack 一致：
     * 先斩击俯仰、再滚转（已含在局部点里），然后准星俯仰，最后水平朝向。
     */
    public Vec3 computeBladePointWorld(
            float swingProgress,
            double lengthAlongBladeBlocks,
            Vec3 originWorld,
            float yawDegrees,
            float lookPitchDegrees
    ) {
        Vec3 localPoint = computeBladePointLocal(swingProgress, lengthAlongBladeBlocks);
        return transformLocalToWorld(localPoint, originWorld, yawDegrees, lookPitchDegrees);
    }

    /**
     * 刃上一点在「已 yaw/俯仰之前」的局部空间：先斩击俯仰再滚转。
     */
    public Vec3 computeBladePointLocal(float swingProgress, double lengthAlongBladeBlocks) {
        float pitchDegrees = Mth.lerp(
                swingProgress,
                getSwingStartPitchDegrees(),
                getSwingEndPitchDegrees()
        );
        float rollDegrees = Mth.lerp(
                swingProgress,
                getSwingStartRollDegrees(),
                getSwingEndRollDegrees()
        );
        double pitchRadians = Math.toRadians(pitchDegrees);
        double rollRadians = Math.toRadians(rollDegrees);
        double afterPitchY = lengthAlongBladeBlocks * Math.cos(pitchRadians);
        double afterPitchZ = lengthAlongBladeBlocks * Math.sin(pitchRadians);
        double localX = -afterPitchY * Math.sin(rollRadians);
        double localY = afterPitchY * Math.cos(rollRadians);
        double localZ = afterPitchZ;
        return new Vec3(localX, localY, localZ);
    }

    /**
     * 把局部点转到世界：先绕 X 跟准星俯仰，再绕 Y 跟水平朝向。
     * 与 PoseStack {@code YP(-yaw) * XP(lookPitch)} 一致。
     */
    public static Vec3 transformLocalToWorld(
            Vec3 localPoint,
            Vec3 originWorld,
            float yawDegrees,
            float lookPitchDegrees
    ) {
        double lookPitchRadians = Math.toRadians(lookPitchDegrees);
        double cosLook = Math.cos(lookPitchRadians);
        double sinLook = Math.sin(lookPitchRadians);
        double afterLookX = localPoint.x;
        double afterLookY = localPoint.y * cosLook - localPoint.z * sinLook;
        double afterLookZ = localPoint.y * sinLook + localPoint.z * cosLook;

        double yawRadians = yawDegrees * Mth.DEG_TO_RAD;
        double cosYaw = Math.cos(yawRadians);
        double sinYaw = Math.sin(yawRadians);
        double worldOffsetX = afterLookX * cosYaw - afterLookZ * sinYaw;
        double worldOffsetZ = afterLookX * sinYaw + afterLookZ * cosYaw;
        return new Vec3(originWorld.x + worldOffsetX, originWorld.y + afterLookY, originWorld.z + worldOffsetZ);
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putFloat("SlashDamage", this.slashDamage);
        compoundTag.putBoolean("SlashResolved", this.slashResolved);
        compoundTag.putInt("ComboIndex", getComboIndex());
        compoundTag.putInt("SwingStartTick", this.entityData.get(DATA_SWING_START_TICK));
        compoundTag.putBoolean("Finishing", isFinishing());
        compoundTag.putBoolean("StopRequested", this.stopRequested);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.slashDamage = compoundTag.getFloat("SlashDamage");
        this.slashResolved = compoundTag.getBoolean("SlashResolved");
        this.entityData.set(DATA_COMBO_INDEX, compoundTag.getInt("ComboIndex"));
        this.entityData.set(DATA_SWING_START_TICK, compoundTag.getInt("SwingStartTick"));
        this.entityData.set(DATA_FINISHING, compoundTag.getBoolean("Finishing"));
        this.stopRequested = compoundTag.getBoolean("StopRequested");
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSquared) {
        return distanceSquared < 64.0 * 64.0;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}

package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.spell.combat.CarianPiercerCombat;
import com.eldenring.spells.spell.curve.CarianPiercerCastCurve;
import com.eldenring.spells.spell.fx.CarianSlicerFx;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/**
 * 卡利亚贯刺服务端锚点：跟在施法者身上，这一刺只结算一次扇形伤害。
 * <p>
 * 视觉剑 / 动画仍在客户端 Hold；本实体不渲染。tick 只问 Curve → Combat → Fx。
 * 斩击特效复用迅剑 {@link CarianSlicerFx}（命中音 + 沿刃星星）。
 */
public class CarianPiercerEntity extends Projectile implements AntiMagicSusceptible {

    private float slashDamage;
    private boolean stopRequested;
    private int stopRequestedAtAge = -1;

    public CarianPiercerEntity(EntityType<? extends CarianPiercerEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public CarianPiercerEntity(Level level, LivingEntity caster, float slashDamage) {
        this(ModEntities.CARIAN_PIERCER.get(), level);
        setOwner(caster);
        this.slashDamage = slashDamage;
        snapToOwner(caster);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // 无客户端同步字段；突刺由 tickCount 驱动。
    }

    /**
     * 吟唱结束：命中窗跑完后再消失，避免 CancelCast 吃掉这一刺伤害。
     */
    public void requestStop() {
        if (this.stopRequested) {
            return;
        }
        this.stopRequested = true;
        this.stopRequestedAtAge = this.tickCount;
    }

    public void setSlashDamage(float slashDamage) {
        this.slashDamage = slashDamage;
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity livingOwner = getOwner() instanceof LivingEntity living ? living : null;
        if (livingOwner == null || !livingOwner.isAlive()) {
            discard();
            return;
        }
        snapToOwner(livingOwner);

        if (!level().isClientSide && CarianPiercerCastCurve.isHitTick(this.tickCount)) {
            CarianPiercerCombat.resolveSlash(this, level(), this.slashDamage);
            CarianSlicerFx.playSlashSound(level(), livingOwner);
        }

        if (this.tickCount >= CarianPiercerCastCurve.SLASH_DURATION_TICKS) {
            discard();
            return;
        }
        if (!this.stopRequested) {
            return;
        }
        int ticksSinceStop = this.tickCount - this.stopRequestedAtAge;
        boolean hitWindowAlreadyPassed =
                CarianPiercerCastCurve.tickIntoCurrentSlash(this.tickCount)
                        > CarianPiercerCastCurve.HIT_TICK;
        if (hitWindowAlreadyPassed || ticksSinceStop >= CarianPiercerCastCurve.STOP_GRACE_TICKS) {
            discard();
        }
    }

    private void snapToOwner(LivingEntity owner) {
        setPos(owner.getX(), owner.getY() + owner.getBbHeight() * 0.5, owner.getZ());
        setYRot(owner.getYRot());
        this.yRotO = getYRot();
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putFloat("SlashDamage", this.slashDamage);
        compoundTag.putBoolean("StopRequested", this.stopRequested);
        compoundTag.putInt("StopRequestedAtAge", this.stopRequestedAtAge);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.slashDamage = compoundTag.getFloat("SlashDamage");
        this.stopRequested = compoundTag.getBoolean("StopRequested");
        this.stopRequestedAtAge = compoundTag.getInt("StopRequestedAtAge");
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSquared) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}

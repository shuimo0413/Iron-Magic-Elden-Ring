package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.spell.combat.CarianSlicerCombat;
import com.eldenring.spells.spell.curve.CarianSlicerCastCurve;
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
 * 卡利亚迅剑服务端锚点：跟在施法者身上，按刀周期结算扇形伤害与斩击音。
 * <p>
 * 视觉剑 / 动画仍在客户端 Hold；本实体不渲染。tick 只问 Curve → Combat → Fx。
 */
public class CarianSlicerEntity extends Projectile implements AntiMagicSusceptible {

    private float slashDamage;
    private boolean stopRequested;
    private int stopRequestedAtAge = -1;

    public CarianSlicerEntity(EntityType<? extends CarianSlicerEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public CarianSlicerEntity(Level level, LivingEntity caster, float slashDamage) {
        this(ModEntities.CARIAN_SLICER.get(), level);
        setOwner(caster);
        this.slashDamage = slashDamage;
        snapToOwner(caster);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // 无客户端同步字段；斩击由 tickCount 驱动。
    }

    /**
     * 吟唱结束：本刀命中窗跑完后再消失，避免 CancelCast 吃掉最后一刀伤害。
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

        if (!level().isClientSide && CarianSlicerCastCurve.isHitTick(this.tickCount)) {
            CarianSlicerCombat.resolveSlash(this, level(), this.slashDamage);
            CarianSlicerFx.playSlashSound(level(), livingOwner);
        }

        if (this.stopRequested) {
            int ticksSinceStop = this.tickCount - this.stopRequestedAtAge;
            boolean hitWindowAlreadyPassed =
                    CarianSlicerCastCurve.tickIntoCurrentSlash(this.tickCount)
                            > CarianSlicerCastCurve.HIT_TICK;
            if (hitWindowAlreadyPassed || ticksSinceStop >= CarianSlicerCastCurve.STOP_GRACE_TICKS) {
                discard();
            }
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

package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.spell.combat.AdulasMoonbladeCombat;
import com.eldenring.spells.spell.curve.AdulasMoonbladeCastCurve;
import com.eldenring.spells.spell.fx.AdulasMoonbladeFx;
import com.eldenring.spells.spell.helper.AdulasMoonbladeCasting;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/**
 * 亚杜拉的月光剑服务端锚点：跟在施法者身上，按刀周期结算扇形斩击、射出剑气、播斩击音。
 * <p>
 * 视觉剑 / 动画在客户端 Hold；本实体不渲染。tick 只问 Curve → Combat / Casting → Fx。
 */
public class AdulasMoonbladeEntity extends Projectile implements AntiMagicSusceptible {

    private float slashDamage;
    private float waveDamage;
    private boolean stopRequested;
    private int stopRequestedAtAge = -1;

    public AdulasMoonbladeEntity(EntityType<? extends AdulasMoonbladeEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public AdulasMoonbladeEntity(Level level, LivingEntity caster, float slashDamage, float waveDamage) {
        this(ModEntities.ADULAS_MOONBLADE.get(), level);
        setOwner(caster);
        this.slashDamage = slashDamage;
        this.waveDamage = waveDamage;
        snapToOwner(caster);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // 无客户端同步字段；斩击由 tickCount 驱动。
    }

    /** 吟唱结束：本刀命中窗跑完后再消失，避免 CancelCast 吃掉最后一刀伤害与剑气。 */
    public void requestStop() {
        if (this.stopRequested) {
            return;
        }
        this.stopRequested = true;
        this.stopRequestedAtAge = this.tickCount;
    }

    public void setDamages(float slashDamage, float waveDamage) {
        this.slashDamage = slashDamage;
        this.waveDamage = waveDamage;
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

        if (!level().isClientSide) {
            if (this.stopRequested && isLeftoverSlashAfterStop()) {
                discard();
                return;
            }
            if (AdulasMoonbladeCastCurve.isHitTick(this.tickCount)) {
                AdulasMoonbladeCombat.resolveSlash(this, level(), this.slashDamage);
                AdulasMoonbladeCasting.launchWave(level(), livingOwner, this.waveDamage);
                AdulasMoonbladeFx.playSlashSound(level(), livingOwner);
            }
        }

        if (this.stopRequested) {
            int ticksSinceStop = this.tickCount - this.stopRequestedAtAge;
            boolean hitWindowAlreadyPassed =
                    AdulasMoonbladeCastCurve.tickIntoCurrentSlash(this.tickCount)
                            > AdulasMoonbladeCastCurve.HIT_TICK;
            if (hitWindowAlreadyPassed || ticksSinceStop >= AdulasMoonbladeCastCurve.STOP_GRACE_TICKS) {
                discard();
            }
        }
    }

    /**
     * 客户端只在「当前刀播完」才发 CancelCast。包晚到时实体年龄可能已经跨进下一刀，
     * 那一刀玩家没按，不能再结算伤害 / 放剑气。
     */
    private boolean isLeftoverSlashAfterStop() {
        if (!this.stopRequested) {
            return false;
        }
        int slashIndexWhenStopped = AdulasMoonbladeCastCurve.slashSequenceIndex(this.stopRequestedAtAge);
        int tickInSlashWhenStopped = AdulasMoonbladeCastCurve.tickIntoCurrentSlash(this.stopRequestedAtAge);
        int slashIndexNow = AdulasMoonbladeCastCurve.slashSequenceIndex(this.tickCount);
        if (slashIndexNow > slashIndexWhenStopped) {
            return true;
        }
        return slashIndexWhenStopped > 0
                && tickInSlashWhenStopped < AdulasMoonbladeCastCurve.HIT_TICK;
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
        compoundTag.putFloat("WaveDamage", this.waveDamage);
        compoundTag.putBoolean("StopRequested", this.stopRequested);
        compoundTag.putInt("StopRequestedAtAge", this.stopRequestedAtAge);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.slashDamage = compoundTag.getFloat("SlashDamage");
        this.waveDamage = compoundTag.getFloat("WaveDamage");
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

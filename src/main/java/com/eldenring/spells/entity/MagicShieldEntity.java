package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModAttachments;
import com.eldenring.spells.registry.ModEffects;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.spell.MagicShieldSpell;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 无碰撞跟随锚点：服务端管理容量，原版实体同步供所有观察者绘制。 */
public final class MagicShieldEntity extends Projectile implements AntiMagicSusceptible {
    private static final EntityDataAccessor<Float> CAPACITY = SynchedEntityData.defineId(MagicShieldEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> HIT_TIME = SynchedEntityData.defineId(MagicShieldEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> MAX_CAPACITY = SynchedEntityData.defineId(MagicShieldEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HIT_ANGLE = SynchedEntityData.defineId(MagicShieldEntity.class, EntityDataSerializers.FLOAT);
    private int spellLevel = 1;
    private int remainingTicks;

    public MagicShieldEntity(EntityType<? extends MagicShieldEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }
    public MagicShieldEntity(Level level, LivingEntity owner) {
        this(ModEntities.MAGIC_SHIELD.get(), level);
        setOwner(owner);
        setPos(owner.position());
        refresh(1);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CAPACITY, MagicShieldSpell.capacity(1));
        builder.define(MAX_CAPACITY, MagicShieldSpell.capacity(1));
        builder.define(HIT_TIME, -100L);
        builder.define(HIT_ANGLE, -1000f);
    }
    public float capacity() { return entityData.get(CAPACITY); }
    public float maxCapacity() { return entityData.get(MAX_CAPACITY); }
    public int spellLevel() { return spellLevel; }
    /** Angle uses the renderer's Y rotation convention; sentinel means environmental damage. */
    public float hitAngle() { return entityData.get(HIT_ANGLE); }
    public float flash(float partialTick) {
        return Math.max(0, 1 - ((level().getGameTime() - entityData.get(HIT_TIME)) + partialTick) / 6);
    }
    public void refresh(int level) {
        spellLevel = MagicShieldSpell.tier(level);
        remainingTicks = MagicShieldSpell.duration(spellLevel);
        entityData.set(MAX_CAPACITY, MagicShieldSpell.capacity(spellLevel));
        entityData.set(CAPACITY, maxCapacity());
        entityData.set(HIT_TIME, -100L);
        entityData.set(HIT_ANGLE, -1000f);
    }
    public float absorb(float damage, Vec3 sourcePosition) {
        if (Float.isFinite(damage) && damage > 0) {
            Vec3 origin = getOwner() == null ? position() : getOwner().position();
            Vec3 delta = sourcePosition == null ? Vec3.ZERO : sourcePosition.subtract(origin);
            entityData.set(HIT_ANGLE, delta.horizontalDistanceSqr() > 1.0e-6
                    ? (float) Math.toDegrees(Math.atan2(delta.x, delta.z)) : -1000f);
        }
        return absorb(damage);
    }
    /** 参数已经过护甲/抗性；返回应继续扣除的伤害，不进行第二次 hurt 调用。 */
    public float absorb(float damage) {
        if (!Float.isFinite(damage) || damage <= 0) return damage;
        float absorbed = Math.min(capacity(), damage);
        entityData.set(CAPACITY, Math.max(0, capacity() - absorbed));
        entityData.set(HIT_TIME, level().getGameTime());
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.GLOW, getX(), getY() + 1, getZ(), 8, 0.5, 0.5, 0.5, 0.02);
            server.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.5f, 1.4f);
            if (capacity() <= 0) {
                server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 40, 0.8, 0.7, 0.8, 0.12);
                server.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.3f);
                if (getOwner() instanceof LivingEntity owner) owner.removeEffect(ModEffects.MAGIC_SHIELD);
                discard();
            }
        }
        return Math.max(0, damage - absorbed);
    }
    @Override public void tick() {
        super.tick();
        if (getOwner() instanceof LivingEntity owner) {
            setPos(owner.position());
            if (!level().isClientSide && (!owner.isAlive() || owner.isRemoved() || owner.level() != level()
                    || !owner.hasEffect(ModEffects.MAGIC_SHIELD) || --remainingTicks <= 0
                    || owner.getData(ModAttachments.MAGIC_SHIELD).entity != this)) {
                discard();
            }
        } else if (!level().isClientSide) discard();
    }
    @Override public void onAntiMagic(MagicData unused) {
        if (!level().isClientSide) {
            if (getOwner() instanceof LivingEntity owner) owner.removeEffect(ModEffects.MAGIC_SHIELD);
            discard();
        }
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); }
}

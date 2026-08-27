package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.spell.curve.GavelOfHaimaCastCurve;
import com.eldenring.spells.spell.combat.GavelOfHaimaCombat;
import com.eldenring.spells.spell.fx.GavelOfHaimaFx;
import com.eldenring.spells.spell.GavelOfHaimaSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 海摩大槌实体：跟在施法者手里，按 tick 播举起 → 下砸 → 结算。
 * <p>
 * tick 只问 {@link GavelOfHaimaCastCurve} → {@link GavelOfHaimaCombat} → {@link GavelOfHaimaFx}。
 * 伤害落点取身前贴地坐标，与握点分开。
 */
public class GavelOfHaimaEntity extends Projectile implements AntiMagicSusceptible {

    private float directHitDamage;
    private float shockwaveDamage;
    private boolean impactResolved;
    /** 砸地瞬间锁定的冲击波中心；砸完不再跟手时继续用。 */
    private Vec3 lockedImpactCenter = Vec3.ZERO;

    public GavelOfHaimaEntity(EntityType<? extends GavelOfHaimaEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public GavelOfHaimaEntity(
            Level level,
            LivingEntity caster,
            float directHitDamage,
            float shockwaveDamage
    ) {
        this(ModEntities.GAVEL_OF_HAIMA.get(), level);
        setOwner(caster);
        this.directHitDamage = directHitDamage;
        this.shockwaveDamage = shockwaveDamage;
        snapToOwnerGrip(caster);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // 动画完全用 tickCount，无需额外同步字段。
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount >= GavelOfHaimaCastCurve.ENTITY_LIFETIME_TICKS) {
            discard();
            return;
        }

        if (GavelOfHaimaCastCurve.shouldFollowOwner(tickCount)) {
            LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
            if (owner != null && owner.isAlive()) {
                snapToOwnerGrip(owner);
            }
        }

        if (!level().isClientSide && !impactResolved && tickCount >= GavelOfHaimaCastCurve.IMPACT_TICK) {
            LivingEntity livingOwner = getOwner() instanceof LivingEntity living ? living : null;
            Vec3 impactCenter = livingOwner != null
                    ? computeImpactWorld(level(), livingOwner)
                    : position();
            this.lockedImpactCenter = impactCenter;
            GavelOfHaimaFx.spawnImpact(level(), impactCenter, GavelOfHaimaSpell.SHOCKWAVE_RADIUS_BLOCKS);
            GavelOfHaimaCombat.resolve(this, level(), impactCenter, directHitDamage, shockwaveDamage);
            impactResolved = true;
        }
        if (level().isClientSide && tickCount < GavelOfHaimaCastCurve.IMPACT_TICK) {
            GavelOfHaimaFx.spawnRaiseAura(this, level());
        }
    }

    /**
     * 把实体锚到施法者身前握点，并同步水平朝向。
     */
    private void snapToOwnerGrip(LivingEntity owner) {
        Vec3 gripWorld = computeGripWorld(owner);
        setPos(gripWorld.x, gripWorld.y, gripWorld.z);
        setYRot(owner.getYRot());
        this.yRotO = getYRot();
    }

    /**
     * 握点世界坐标：脚底 + 前 + 右 + 高。
     */
    public static Vec3 computeGripWorld(LivingEntity owner) {
        Vec3 forwardFlat = horizontalForward(owner);
        Vec3 rightFlat = new Vec3(-forwardFlat.z, 0.0, forwardFlat.x);
        return owner.position()
                .add(forwardFlat.scale(GavelOfHaimaCastCurve.GRIP_FORWARD_OFFSET_BLOCKS))
                .add(rightFlat.scale(GavelOfHaimaCastCurve.GRIP_RIGHT_OFFSET_BLOCKS))
                .add(0.0, GavelOfHaimaCastCurve.GRIP_HEIGHT_BLOCKS, 0.0);
    }

    /**
     * 身前贴地砸点：伤害与冲击波粒子中心。
     */
    public static Vec3 computeImpactWorld(Level level, LivingEntity owner) {
        Vec3 forwardFlat = horizontalForward(owner);
        Vec3 desired = owner.position().add(forwardFlat.scale(GavelOfHaimaCastCurve.IMPACT_FORWARD_OFFSET_BLOCKS));
        return Utils.moveToRelativeGroundLevel(level, desired, GavelOfHaimaCastCurve.GROUND_SNAP_MAX_STEPS)
                .add(0.0, 0.05, 0.0);
    }

    private static Vec3 horizontalForward(LivingEntity owner) {
        Vec3 horizontalLook = owner.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (horizontalLook.lengthSqr() < 1.0e-6) {
            horizontalLook = owner.getForward().multiply(1.0, 0.0, 1.0);
        }
        if (horizontalLook.lengthSqr() < 1.0e-6) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return horizontalLook.normalize();
    }

    /**
     * 下砸插值进度 0–1。供渲染器读。
     */
    public float getSwingProgress(float partialTicks) {
        return GavelOfHaimaCastCurve.swingProgress(tickCount + partialTicks);
    }

    /**
     * 砸地后淡出：1 = 完全不透明，0 = 消失。供渲染器调 alpha。
     */
    public float getFadeAlpha(float partialTicks) {
        return GavelOfHaimaCastCurve.fadeAlpha(tickCount + partialTicks);
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putFloat("DirectHitDamage", this.directHitDamage);
        compoundTag.putFloat("ShockwaveDamage", this.shockwaveDamage);
        compoundTag.putBoolean("ImpactResolved", this.impactResolved);
        compoundTag.putDouble("ImpactX", this.lockedImpactCenter.x);
        compoundTag.putDouble("ImpactY", this.lockedImpactCenter.y);
        compoundTag.putDouble("ImpactZ", this.lockedImpactCenter.z);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.directHitDamage = compoundTag.getFloat("DirectHitDamage");
        this.shockwaveDamage = compoundTag.getFloat("ShockwaveDamage");
        this.impactResolved = compoundTag.getBoolean("ImpactResolved");
        this.lockedImpactCenter = new Vec3(
                compoundTag.getDouble("ImpactX"),
                compoundTag.getDouble("ImpactY"),
                compoundTag.getDouble("ImpactZ")
        );
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSquared) {
        return distanceSquared < 96.0 * 96.0;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}

package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.spell.MeteoriteSpell;
import com.eldenring.spells.spell.combat.MeteoriteCombat;
import com.eldenring.spells.spell.fx.MeteoriteFx;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 陨石法术的一颗陨石：从虚空黑洞里砸出，沿生成时给定的方向直线倾斜下坠。
 * <p>
 * 无重力、不追踪；撞到实体或方块都交给 {@link MeteoriteCombat} 在命中点做小范围爆炸，然后消失。
 * 飞满 {@link MeteoriteSpell#PROJECTILE_MAX_RANGE_BLOCKS} 还没落地就碎裂。
 * 命中粒子 / 命中音由铁魔法 {@code AbstractMagicProjectile#onHit} 统一触发。
 */
public class MeteoriteProjectile extends AbstractMagicProjectile {

    /** 命中射线相对目标碰撞箱的外扩（方块）。陨石是实心大块，略宽一点更好砸中。 */
    private static final float HIT_DETECTION_INFLATION_BLOCKS = 0.35f;

    /** 已飞行路程（方块）。用于射程截断。仅服务端用。 */
    private double traveledDistanceBlocks;

    /**
     * 本颗陨石的飞行速度（方块/tick）。默认取陨石法术，其它复用本实体的法术（如艾斯提陨石）在 {@code shoot} 前覆盖。
     * 仅服务端用：速度只在出手时写进 deltaMovement，客户端靠运动同步。
     */
    private float flightSpeed = MeteoriteSpell.PROJECTILE_FLIGHT_SPEED;

    /** 本颗陨石的最大射程（方块）。仅服务端用。 */
    private double maxRangeBlocks = MeteoriteSpell.PROJECTILE_MAX_RANGE_BLOCKS;

    /** 本颗陨石落地爆炸半径（方块）。仅服务端用。 */
    private float explosionRadiusBlocks = MeteoriteSpell.EXPLOSION_RADIUS_BLOCKS;

    /** 本颗陨石爆炸击退强度（原版 knockback 强度）。仅服务端用。 */
    private float knockbackStrength = MeteoriteSpell.KNOCKBACK_STRENGTH;

    /** 伤害来源归属的法术；null 表示陨石法术本身。仅服务端用。 */
    @Nullable
    private AbstractSpell sourceSpell;

    public MeteoriteProjectile(EntityType<? extends MeteoriteProjectile> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public MeteoriteProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.METEORITE.get(), level);
        setOwner(shooter);
    }

    /** 陨石视觉中心（世界坐标）。实体原点在碰撞箱底面。 */
    public Vec3 centerWorldPosition() {
        return position().add(0.0, getBbHeight() * 0.5, 0.0);
    }

    /** 已知陨石中心时反推实体原点。 */
    public Vec3 feetPositionForCenter(Vec3 centerWorldPosition) {
        return centerWorldPosition.subtract(0.0, getBbHeight() * 0.5, 0.0);
    }

    @Override
    public void trailParticles() {
        MeteoriteFx.trail(level(), centerWorldPosition(), getDeltaMovement());
    }

    @Override
    public void impactParticles(double impactX, double impactY, double impactZ) {
        MeteoriteFx.impact(level(), new Vec3(impactX, impactY, impactZ));
    }

    /**
     * 复用本实体的法术在 {@code shoot} 前调用，覆盖速度 / 射程 / 爆炸 / 击退与伤害归属。
     *
     * @param sourceSpell 伤害来源法术（决定击杀信息与法术伤害加成归属）
     */
    public void configureFor(
            AbstractSpell sourceSpell,
            float flightSpeed,
            double maxRangeBlocks,
            float explosionRadiusBlocks,
            float knockbackStrength
    ) {
        this.sourceSpell = sourceSpell;
        this.flightSpeed = flightSpeed;
        this.maxRangeBlocks = maxRangeBlocks;
        this.explosionRadiusBlocks = explosionRadiusBlocks;
        this.knockbackStrength = knockbackStrength;
    }

    @Nullable
    public AbstractSpell sourceSpell() {
        return sourceSpell;
    }

    public float explosionRadiusBlocks() {
        return explosionRadiusBlocks;
    }

    public float knockbackStrength() {
        return knockbackStrength;
    }

    @Override
    public float getSpeed() {
        return flightSpeed;
    }

    @Override
    public float getHitDetectionInflation() {
        return HIT_DETECTION_INFLATION_BLOCKS;
    }

    @Override
    public Optional<Supplier<SoundEvent>> getImpactSound() {
        return Optional.of(() -> SoundEvents.DEEPSLATE_BREAK);
    }

    @Override
    public void tick() {
        Vec3 positionBeforeTick = position();
        super.tick();
        if (this.isRemoved() || level().isClientSide) {
            return;
        }
        traveledDistanceBlocks += position().distanceTo(positionBeforeTick);
        if (traveledDistanceBlocks >= maxRangeBlocks) {
            MeteoriteFx.fizzle(level(), centerWorldPosition());
            discard();
        }
    }

    /** 同一施法者的陨石互不相撞，密集下落时不会在半空互相引爆。 */
    @Override
    protected boolean canHitEntity(@NotNull Entity targetEntity) {
        if (targetEntity instanceof MeteoriteProjectile otherMeteorite) {
            Entity thisOwner = getOwner();
            if (thisOwner != null && thisOwner == otherMeteorite.getOwner()) {
                return false;
            }
        }
        return super.canHitEntity(targetEntity);
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        if (this.isRemoved()) {
            return;
        }
        super.onHitBlock(blockHitResult);
        if (!level().isClientSide) {
            MeteoriteCombat.explode(this, blockHitResult.getLocation(), null);
        }
        discard();
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        if (this.isRemoved()) {
            return;
        }
        super.onHitEntity(entityHitResult);
        if (this.isRemoved()) {
            return;
        }
        if (!level().isClientSide) {
            MeteoriteCombat.explode(this, entityHitResult.getLocation(), entityHitResult.getEntity());
        }
        discard();
    }

    /** 一次性弹道，不进存档。 */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}

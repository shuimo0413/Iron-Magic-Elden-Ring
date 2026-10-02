package com.eldenring.spells.spell.combat;

import com.eldenring.spells.entity.MeteoriteProjectile;
import com.eldenring.spells.registry.ModSpells;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 陨石落地 / 撞敌：在命中点做一次小范围爆炸。
 * <p>
 * 被直接砸中的实体必定吃一次伤害；半径内其它生物各再结算一次，同一颗陨石不会对同一目标打两次。
 * 击退走原版 {@link LivingEntity#knockback}，会被击退抗性削减，沿爆心水平向外推开。
 */
public final class MeteoriteCombat {

    private MeteoriteCombat() {
    }

    /**
     * @param meteorite        命中的陨石（伤害、施法者都从它身上取）
     * @param explosionCenter  爆心（命中点）
     * @param directHitEntity  被直接砸中的实体；砸到方块时为 null
     */
    public static void explode(MeteoriteProjectile meteorite, Vec3 explosionCenter, @Nullable Entity directHitEntity) {
        Entity owner = meteorite.getOwner();
        AbstractSpell sourceSpell = meteorite.sourceSpell() != null ? meteorite.sourceSpell() : ModSpells.METEORITE.get();
        SpellDamageSource damageSource = sourceSpell.getDamageSource(meteorite, owner);
        float damageAmount = meteorite.getDamage();

        if (directHitEntity != null) {
            damageAndKnockBack(directHitEntity, damageAmount, damageSource, explosionCenter, meteorite);
        }

        double radiusBlocks = meteorite.explosionRadiusBlocks();
        if (radiusBlocks <= 0.0) {
            return;
        }
        double radiusSquared = radiusBlocks * radiusBlocks;
        AABB searchBox = new AABB(explosionCenter, explosionCenter).inflate(radiusBlocks);
        for (LivingEntity target : meteorite.level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                candidate -> candidate.isAlive()
                        && candidate != directHitEntity
                        && candidate != owner
                        && !candidate.isSpectator()
                        && (owner == null || !DamageSources.isFriendlyFireBetween(owner, candidate))
        )) {
            if (distanceToBoxSquared(explosionCenter, target.getBoundingBox()) > radiusSquared) {
                continue;
            }
            damageAndKnockBack(target, damageAmount, damageSource, explosionCenter, meteorite);
        }
    }

    private static void damageAndKnockBack(
            Entity target,
            float damageAmount,
            SpellDamageSource damageSource,
            Vec3 explosionCenter,
            MeteoriteProjectile meteorite
    ) {
        boolean damaged = DamageSources.applyDamage(target, damageAmount, damageSource);
        if (!damaged || !(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive()
                || meteorite.knockbackStrength() <= 0.0f) {
            return;
        }
        Vec3 pushDirection = livingTarget.position().subtract(explosionCenter);
        double horizontalLength = pushDirection.horizontalDistance();
        if (horizontalLength < 1.0e-3) {
            pushDirection = meteorite.getDeltaMovement();
            horizontalLength = pushDirection.horizontalDistance();
            if (horizontalLength < 1.0e-4) {
                return;
            }
        }
        // knockback(x, z) 把目标推向 (-x, -z)，所以传推开方向的反向
        livingTarget.knockback(
                meteorite.knockbackStrength(),
                -pushDirection.x / horizontalLength,
                -pushDirection.z / horizontalLength
        );
    }

    /** 点到轴对齐包围盒的最近距离平方；点在盒内为 0。 */
    private static double distanceToBoxSquared(Vec3 point, AABB box) {
        double deltaX = Math.max(Math.max(box.minX - point.x, 0.0), point.x - box.maxX);
        double deltaY = Math.max(Math.max(box.minY - point.y, 0.0), point.y - box.maxY);
        double deltaZ = Math.max(Math.max(box.minZ - point.z, 0.0), point.z - box.maxZ);
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }
}

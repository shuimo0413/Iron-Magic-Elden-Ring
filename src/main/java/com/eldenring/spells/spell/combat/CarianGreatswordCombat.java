package com.eldenring.spells.spell.combat;

import com.eldenring.spells.entity.CarianGreatswordEntity;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.spell.CarianGreatswordSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 卡利亚大剑命中：水平扇形（俯仰不吃有效距离）+ 竖直高度带。
 * 半径 / 半角 / 击退读 {@link CarianGreatswordSpell}；竖直带写死，不进 toml。
 */
public final class CarianGreatswordCombat {

    /**
     * 斩击竖直半高（方块）。以施法者身体中心为基准，上下各这么多；
     * 目标碰撞箱与该带有重叠即算高度命中。调大 → 更高 / 更矮的怪也容易砍到。
     * 默认 1.8：面前约一人高（±1.8）都能吃到伤害，不被视线俯仰的三维锥角误伤。
     */
    public static final float SLASH_VERTICAL_HALF_HEIGHT_BLOCKS = 1.8f;

    private CarianGreatswordCombat() {
    }

    /**
     * 对施法者面前水平扇形、且竖直落在高度带内的可攻击生物结算一次斩击伤害与击退。
     */
    public static void resolveSlash(
            CarianGreatswordEntity greatswordEntity,
            Level level,
            float slashDamage
    ) {
        CarianGreatswordSpell spell = (CarianGreatswordSpell) ModSpells.CARIAN_GREATSWORD.get();
        Entity owner = greatswordEntity.getOwner();
        if (!(owner instanceof LivingEntity livingOwner) || !livingOwner.isAlive()) {
            return;
        }

        float radiusBlocks = CarianGreatswordSpell.SLASH_RADIUS_BLOCKS;
        float halfAngleDegrees = CarianGreatswordSpell.SLASH_HALF_ANGLE_DEGREES;
        double radiusSquared = radiusBlocks * radiusBlocks;
        float halfAngleCosine = Mth.cos(halfAngleDegrees * Mth.DEG_TO_RAD);

        // 水平原点用脚底 XZ；竖直带绕身体中心，搜箱要盖住上下半高。
        double ownerBodyCenterY = livingOwner.getY() + livingOwner.getBbHeight() * 0.5;
        Vec3 horizontalLookDirection = flattenHorizontal(livingOwner.getLookAngle());
        if (horizontalLookDirection.lengthSqr() < 1.0e-6) {
            horizontalLookDirection = flattenHorizontal(livingOwner.getForward());
        }
        if (horizontalLookDirection.lengthSqr() < 1.0e-6) {
            return;
        }
        horizontalLookDirection = horizontalLookDirection.normalize();

        AABB searchBox = new AABB(
                livingOwner.getX() - radiusBlocks,
                ownerBodyCenterY - SLASH_VERTICAL_HALF_HEIGHT_BLOCKS,
                livingOwner.getZ() - radiusBlocks,
                livingOwner.getX() + radiusBlocks,
                ownerBodyCenterY + SLASH_VERTICAL_HALF_HEIGHT_BLOCKS,
                livingOwner.getZ() + radiusBlocks
        );
        var damageSource = spell.getDamageSource(greatswordEntity, livingOwner);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchBox, candidate ->
                candidate.isAlive()
                        && candidate.isPickable()
                        && !candidate.isSpectator()
                        && candidate != livingOwner
                        && !DamageSources.isFriendlyFireBetween(candidate, livingOwner)
        )) {
            // 竖直：目标碰撞箱与「身体中心 ± 半高」带重叠即可（面前约 1.8 格人高都能挨刀）。
            double targetMinY = target.getY();
            double targetMaxY = target.getY() + target.getBbHeight();
            double bandMinY = ownerBodyCenterY - SLASH_VERTICAL_HALF_HEIGHT_BLOCKS;
            double bandMaxY = ownerBodyCenterY + SLASH_VERTICAL_HALF_HEIGHT_BLOCKS;
            if (targetMaxY < bandMinY || targetMinY > bandMaxY) {
                continue;
            }

            // 水平：只用 XZ 距离与偏航扇形，俯仰不再缩短面前有效半径。
            double deltaX = target.getX() - livingOwner.getX();
            double deltaZ = target.getZ() - livingOwner.getZ();
            double horizontalDistanceSquared = deltaX * deltaX + deltaZ * deltaZ;
            if (horizontalDistanceSquared > radiusSquared || horizontalDistanceSquared < 1.0e-6) {
                continue;
            }
            Vec3 toTargetHorizontal = new Vec3(deltaX, 0.0, deltaZ).normalize();
            if (toTargetHorizontal.dot(horizontalLookDirection) < halfAngleCosine) {
                continue;
            }

            DamageSources.applyDamage(target, slashDamage, damageSource);

            double knockbackStrength = CarianGreatswordSpell.SLASH_KNOCKBACK_STRENGTH;
            target.knockback(
                    knockbackStrength,
                    livingOwner.getX() - target.getX(),
                    livingOwner.getZ() - target.getZ()
            );
            target.hurtMarked = true;
        }
    }

    /**
     * 去掉俯仰，只保留水平朝向分量，供扇形偏航判定。
     */
    private static Vec3 flattenHorizontal(Vec3 direction) {
        return new Vec3(direction.x, 0.0, direction.z);
    }
}

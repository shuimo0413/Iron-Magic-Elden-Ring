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
 * 卡利亚大剑命中：以施法者朝向为轴的扇形。半径 / 半角 / 击退读 {@link CarianGreatswordSpell}。
 */
public final class CarianGreatswordCombat {

    /**
     * 扇形搜箱竖直半高（方块）。只影响能不能搜到高处 / 脚下的怪，不进 toml。
     */
    public static final float SLASH_VERTICAL_HALF_HEIGHT_BLOCKS = 1.8f;

    private CarianGreatswordCombat() {
    }

    /**
     * 对施法者面前扇形内的可攻击生物结算一次斩击伤害与击退。
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

        Vec3 origin = livingOwner.getEyePosition();
        Vec3 lookDirection = livingOwner.getLookAngle().normalize();
        AABB searchBox = new AABB(origin, origin).inflate(
                radiusBlocks,
                SLASH_VERTICAL_HALF_HEIGHT_BLOCKS,
                radiusBlocks
        );
        var damageSource = spell.getDamageSource(greatswordEntity, livingOwner);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchBox, candidate ->
                candidate.isAlive()
                        && candidate.isPickable()
                        && !candidate.isSpectator()
                        && candidate != livingOwner
                        && !DamageSources.isFriendlyFireBetween(candidate, livingOwner)
        )) {
            Vec3 toTarget = target.getEyePosition().subtract(origin);
            double distanceSquared = toTarget.lengthSqr();
            if (distanceSquared > radiusSquared || distanceSquared < 1.0e-6) {
                continue;
            }
            Vec3 toTargetDirection = toTarget.normalize();
            double dot = toTargetDirection.dot(lookDirection);
            if (dot < halfAngleCosine) {
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
}

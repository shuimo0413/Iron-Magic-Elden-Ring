package com.eldenring.spells.spell.combat;

import com.eldenring.spells.entity.CannonOfHaimaProjectile;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.spell.CannonOfHaimaSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 海摩炮弹命中：球形范围伤害 + 径向击退。半径 / 击退读 {@link CannonOfHaimaSpell} 运行时字段。
 */
public final class CannonOfHaimaCombat {

    /**
     * 搜箱竖直半高相对爆炸半径的倍率。炮弹炸在地面时也要打到站着的人。
     */
    public static final float EXPLOSION_VERTICAL_HALF_HEIGHT_FRACTION = 0.85f;

    /**
     * 爆炸附加向上冲量（方块/tick）。让人被掀起来一点，而不是只水平滑。
     */
    public static final double EXPLOSION_UPWARD_LAUNCH = 0.28;

    private CannonOfHaimaCombat() {
    }

    /**
     * 以落点为球心打范围内所有合法敌人；距离越近击退越强。
     */
    public static void resolve(
            CannonOfHaimaProjectile cannonProjectile,
            Level level,
            Vec3 explosionCenter,
            float explosionDamage
    ) {
        CannonOfHaimaSpell spell = (CannonOfHaimaSpell) ModSpells.CANNON_OF_HAIMA.get();
        Entity owner = cannonProjectile.getOwner();
        LivingEntity livingOwner = owner instanceof LivingEntity living ? living : null;
        var damageSource = spell.getDamageSource(cannonProjectile, owner);

        float explosionRadiusBlocks = CannonOfHaimaSpell.EXPLOSION_RADIUS_BLOCKS;
        double explosionRadiusSquared = explosionRadiusBlocks * explosionRadiusBlocks;
        double verticalHalfHeight = explosionRadiusBlocks * EXPLOSION_VERTICAL_HALF_HEIGHT_FRACTION;

        AABB searchBox = new AABB(explosionCenter, explosionCenter).inflate(
                explosionRadiusBlocks,
                verticalHalfHeight,
                explosionRadiusBlocks
        );
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchBox, candidate ->
                candidate.isAlive()
                        && candidate.isPickable()
                        && !candidate.isSpectator()
                        && candidate != livingOwner
                        && (livingOwner == null || !DamageSources.isFriendlyFireBetween(candidate, livingOwner))
        )) {
            double closestDistanceSquared = closestDistanceSquaredToBoundingBox(explosionCenter, target.getBoundingBox());
            if (closestDistanceSquared > explosionRadiusSquared) {
                continue;
            }

            DamageSources.applyDamage(target, explosionDamage, damageSource);

            double distanceBlocks = Math.sqrt(Math.max(closestDistanceSquared, 1.0e-4));
            double falloff = 1.0 - Mth.clamp(distanceBlocks / explosionRadiusBlocks, 0.0, 1.0);
            double knockbackStrength = CannonOfHaimaSpell.EXPLOSION_KNOCKBACK_STRENGTH * (0.40 + 0.60 * falloff);
            target.knockback(
                    knockbackStrength,
                    explosionCenter.x - target.getX(),
                    explosionCenter.z - target.getZ()
            );
            double upward = EXPLOSION_UPWARD_LAUNCH * (0.50 + 0.50 * falloff);
            Vec3 motion = target.getDeltaMovement();
            target.setDeltaMovement(motion.x, Math.max(motion.y, upward), motion.z);
            target.hurtMarked = true;
        }
    }

    /**
     * 爆炸中心到目标碰撞箱的最近点距离平方。用最近点而不是脚底，避免高处的人吃不到炸。
     */
    private static double closestDistanceSquaredToBoundingBox(Vec3 explosionCenter, AABB targetBox) {
        double clampedX = Mth.clamp(explosionCenter.x, targetBox.minX, targetBox.maxX);
        double clampedY = Mth.clamp(explosionCenter.y, targetBox.minY, targetBox.maxY);
        double clampedZ = Mth.clamp(explosionCenter.z, targetBox.minZ, targetBox.maxZ);
        double deltaX = explosionCenter.x - clampedX;
        double deltaY = explosionCenter.y - clampedY;
        double deltaZ = explosionCenter.z - clampedZ;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }
}

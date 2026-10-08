package com.eldenring.spells.spell.combat;

import com.eldenring.spells.entity.AdulasMoonbladeEntity;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.spell.AdulasMoonbladeSpell;
import com.eldenring.spells.spell.helper.FrostHelper;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 亚杜拉的月光剑近身斩击：水平扇形（俯仰不吃有效距离）+ 竖直高度带，判定同卡利亚大剑。
 * 命中后让目标原版结霜（冻伤扣血），不上 {@code CHILLED}，不会冻进冰牢。
 * 半径 / 半角 / 击退读 {@link AdulasMoonbladeSpell}；竖直带写死，不进 toml。
 */
public final class AdulasMoonbladeCombat {

    /** 斩击竖直半高（方块）。以施法者身体中心为基准，上下各这么多。 */
    public static final float SLASH_VERTICAL_HALF_HEIGHT_BLOCKS = 0.8f;

    private AdulasMoonbladeCombat() {
    }

    /**
     * 对施法者面前水平扇形、且竖直落在高度带内的可攻击生物结算一次斩击伤害、击退与结霜。
     */
    public static void resolveSlash(
            AdulasMoonbladeEntity moonbladeEntity,
            Level level,
            float slashDamage
    ) {
        AdulasMoonbladeSpell spell = (AdulasMoonbladeSpell) ModSpells.ADULAS_MOONBLADE.get();
        Entity owner = moonbladeEntity.getOwner();
        if (!(owner instanceof LivingEntity livingOwner) || !livingOwner.isAlive()) {
            return;
        }

        float radiusBlocks = AdulasMoonbladeSpell.SLASH_RADIUS_BLOCKS;
        float halfAngleDegrees = AdulasMoonbladeSpell.SLASH_HALF_ANGLE_DEGREES;
        double radiusSquared = radiusBlocks * radiusBlocks;
        float halfAngleCosine = Mth.cos(halfAngleDegrees * Mth.DEG_TO_RAD);

        double ownerBodyCenterY = livingOwner.getY() + livingOwner.getBbHeight() * 0.5;
        Vec3 horizontalLookDirection = flattenHorizontal(livingOwner.getLookAngle());
        if (horizontalLookDirection.lengthSqr() < 1.0e-6) {
            horizontalLookDirection = flattenHorizontal(livingOwner.getForward());
        }
        if (horizontalLookDirection.lengthSqr() < 1.0e-6) {
            return;
        }
        horizontalLookDirection = horizontalLookDirection.normalize();

        double bandMinY = ownerBodyCenterY - SLASH_VERTICAL_HALF_HEIGHT_BLOCKS;
        double bandMaxY = ownerBodyCenterY + SLASH_VERTICAL_HALF_HEIGHT_BLOCKS;
        AABB searchBox = new AABB(
                livingOwner.getX() - radiusBlocks,
                bandMinY,
                livingOwner.getZ() - radiusBlocks,
                livingOwner.getX() + radiusBlocks,
                bandMaxY,
                livingOwner.getZ() + radiusBlocks
        );
        var damageSource = spell.getDamageSource(moonbladeEntity, livingOwner);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchBox, candidate ->
                candidate.isAlive()
                        && candidate.isPickable()
                        && !candidate.isSpectator()
                        && candidate != livingOwner
                        && !DamageSources.isFriendlyFireBetween(candidate, livingOwner)
        )) {
            double targetMinY = target.getY();
            double targetMaxY = target.getY() + target.getBbHeight();
            if (targetMaxY < bandMinY || targetMinY > bandMaxY) {
                continue;
            }

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
            applyFrost(target);

            target.knockback(
                    AdulasMoonbladeSpell.SLASH_KNOCKBACK_STRENGTH,
                    livingOwner.getX() - target.getX(),
                    livingOwner.getZ() - target.getZ()
            );
            target.hurtMarked = true;
        }
    }

    /**
     * 命中后原版结霜（冻伤扣血），不上 {@code CHILLED}，不会冻进冰牢。斩击与剑气共用，秒数读 Spell 运行时字段。
     */
    public static void applyFrost(LivingEntity target) {
        FrostHelper.applyFrost(target, AdulasMoonbladeSpell.SPELL_FROST_SECONDS);
    }

    /** 去掉俯仰，只保留水平朝向分量，供扇形偏航判定。 */
    private static Vec3 flattenHorizontal(Vec3 direction) {
        return new Vec3(direction.x, 0.0, direction.z);
    }
}

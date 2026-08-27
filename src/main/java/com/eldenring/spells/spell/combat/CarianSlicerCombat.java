package com.eldenring.spells.spell.combat;

import com.eldenring.spells.entity.CarianSlicerEntity;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.spell.CarianSlicerSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 卡利亚迅剑命中：身前圆锥扇形伤害 + 轻击退。半径 / 半角 / 击退读 {@link CarianSlicerSpell} 运行时字段。
 */
public final class CarianSlicerCombat {

    /**
     * 扇形竖直半高（方块），inflate 用。写死：只影响搜箱高度，不进 toml。
     */
    public static final float SLASH_VERTICAL_HALF_HEIGHT_BLOCKS = 1.35f;

    private CarianSlicerCombat() {
    }

    /**
     * 丢掉该施法者身边残留的迅剑（上次没淡完又起手时）。
     */
    public static void discardOwnedSlicers(Level level, LivingEntity owner) {
        AABB searchBox = owner.getBoundingBox().inflate(8.0);
        for (CarianSlicerEntity existing : level.getEntitiesOfClass(CarianSlicerEntity.class, searchBox)) {
            if (existing.getOwner() == owner) {
                existing.discard();
            }
        }
    }

    /**
     * 以眼睛前方为原点的圆锥扇形：距离 + 与视线夹角双重过滤。
     */
    public static void resolve(
            CarianSlicerEntity slicerEntity,
            Level level,
            Vec3 slashOrigin,
            Vec3 lookDirection,
            LivingEntity livingOwner,
            float slashDamage
    ) {
        CarianSlicerSpell spell = (CarianSlicerSpell) ModSpells.CARIAN_SLICER.get();
        Entity owner = slicerEntity.getOwner();
        var damageSource = spell.getDamageSource(slicerEntity, owner);
        Vec3 normalizedLook = lookDirection.lengthSqr() > 1.0e-8 ? lookDirection.normalize() : new Vec3(0.0, 0.0, 1.0);
        float rangeBlocks = CarianSlicerSpell.SLASH_RANGE_BLOCKS;
        double cosineThreshold = Math.cos(Math.toRadians(CarianSlicerSpell.SLASH_HALF_ANGLE_DEGREES));

        AABB searchBox = new AABB(slashOrigin, slashOrigin).inflate(
                rangeBlocks,
                SLASH_VERTICAL_HALF_HEIGHT_BLOCKS,
                rangeBlocks
        );
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchBox, candidate ->
                candidate.isAlive()
                        && candidate.isPickable()
                        && !candidate.isSpectator()
                        && candidate != livingOwner
                        && (livingOwner == null || !DamageSources.isFriendlyFireBetween(candidate, livingOwner))
        )) {
            Vec3 towardTarget = target.getBoundingBox().getCenter().subtract(slashOrigin);
            double distanceBlocks = towardTarget.length();
            if (distanceBlocks > rangeBlocks || distanceBlocks < 1.0e-4) {
                continue;
            }
            double alignment = towardTarget.normalize().dot(normalizedLook);
            if (alignment < cosineThreshold) {
                continue;
            }

            DamageSources.applyDamage(target, slashDamage, damageSource);
            target.knockback(
                    CarianSlicerSpell.SLASH_KNOCKBACK_STRENGTH,
                    slashOrigin.x - target.getX(),
                    slashOrigin.z - target.getZ()
            );
            target.hurtMarked = true;
        }
    }
}

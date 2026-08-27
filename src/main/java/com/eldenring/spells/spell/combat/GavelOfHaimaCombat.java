package com.eldenring.spells.spell.combat;

import com.eldenring.spells.entity.GavelOfHaimaEntity;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.spell.GavelOfHaimaSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 海摩大槌命中：锤头直击 + 冲击波。半径 / 击退读 {@link GavelOfHaimaSpell} 运行时字段。
 */
public final class GavelOfHaimaCombat {

    /**
     * 冲击波竖直半高（方块），inflate 用。写死：只影响搜箱高度。
     */
    public static final float SHOCKWAVE_VERTICAL_HALF_HEIGHT_BLOCKS = 1.8f;

    /**
     * 砸地附加向上冲量（方块/tick）。轻微上抬即可。
     */
    public static final double IMPACT_UPWARD_LAUNCH = 0.18;

    /**
     * 直击额外向上冲量（叠在 {@link #IMPACT_UPWARD_LAUNCH} 上）。
     */
    public static final double DIRECT_HIT_EXTRA_UPWARD_LAUNCH = 0.06;

    private GavelOfHaimaCombat() {
    }

    /**
     * 先圈直击目标，再对剩余目标打冲击波；命中后径向击退 + 上抛。
     */
    public static void resolve(
            GavelOfHaimaEntity gavelEntity,
            Level level,
            Vec3 impactCenter,
            float directHitDamage,
            float shockwaveDamage
    ) {
        GavelOfHaimaSpell spell = (GavelOfHaimaSpell) ModSpells.GAVEL_OF_HAIMA.get();
        Entity owner = gavelEntity.getOwner();
        LivingEntity livingOwner = owner instanceof LivingEntity living ? living : null;
        var damageSource = spell.getDamageSource(gavelEntity, owner);

        float directRadius = GavelOfHaimaSpell.DIRECT_HIT_RADIUS_BLOCKS;
        float shockwaveRadius = GavelOfHaimaSpell.SHOCKWAVE_RADIUS_BLOCKS;
        double shockwaveRadiusSquared = shockwaveRadius * shockwaveRadius;
        double directRadiusSquared = directRadius * directRadius;

        AABB searchBox = new AABB(impactCenter, impactCenter).inflate(
                shockwaveRadius,
                SHOCKWAVE_VERTICAL_HALF_HEIGHT_BLOCKS,
                shockwaveRadius
        );
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchBox, candidate ->
                candidate.isAlive()
                        && candidate.isPickable()
                        && !candidate.isSpectator()
                        && candidate != livingOwner
                        && (livingOwner == null || !DamageSources.isFriendlyFireBetween(candidate, livingOwner))
        )) {
            double horizontalDistanceSquared = target.distanceToSqr(
                    impactCenter.x,
                    target.getY(),
                    impactCenter.z
            );
            if (horizontalDistanceSquared > shockwaveRadiusSquared) {
                continue;
            }

            boolean isDirectHit = horizontalDistanceSquared <= directRadiusSquared;
            float damage = isDirectHit ? directHitDamage : shockwaveDamage;
            DamageSources.applyDamage(target, damage, damageSource);

            double distanceBlocks = Math.sqrt(Math.max(horizontalDistanceSquared, 1.0e-4));
            double falloff = 1.0 - Mth.clamp(distanceBlocks / shockwaveRadius, 0.0, 1.0);
            double knockbackStrength = isDirectHit
                    ? GavelOfHaimaSpell.DIRECT_HIT_KNOCKBACK_STRENGTH
                    : GavelOfHaimaSpell.SHOCKWAVE_KNOCKBACK_STRENGTH * (0.45 + 0.55 * falloff);
            target.knockback(
                    knockbackStrength,
                    impactCenter.x - target.getX(),
                    impactCenter.z - target.getZ()
            );
            double upward = IMPACT_UPWARD_LAUNCH * (0.55 + 0.45 * falloff);
            if (isDirectHit) {
                upward += DIRECT_HIT_EXTRA_UPWARD_LAUNCH;
            }
            Vec3 motion = target.getDeltaMovement();
            target.setDeltaMovement(motion.x, Math.max(motion.y, upward), motion.z);
            target.hurtMarked = true;
        }
    }
}

package com.eldenring.spells.spell.combat;

import com.eldenring.spells.entity.RockSlingProjectile;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.spell.RockSlingSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 岩石球单块命中：伤害 + 普通击退。
 * <p>
 * 击退走原版 {@link LivingEntity#knockback}：会被击退抗性按比例削减、可被事件取消，
 * 不是强制位移；抗性满额的 Boss / 铁傀儡不会被推动。
 */
public final class RockSlingCombat {

    private RockSlingCombat() {
    }

    /**
     * 对命中实体结算本块伤害；目标还活着且是生物时，沿岩石水平飞行方向推开。
     */
    public static void resolveEntityHit(RockSlingProjectile rock, Entity hitEntity) {
        boolean damaged = DamageSources.applyDamage(
                hitEntity,
                rock.getDamage(),
                ModSpells.ROCK_SLING.get().getDamageSource(rock, rock.getOwner())
        );
        if (!damaged || !(hitEntity instanceof LivingEntity livingTarget) || !livingTarget.isAlive()) {
            return;
        }
        Vec3 flightDirection = rock.getDeltaMovement();
        double horizontalLength = flightDirection.horizontalDistance();
        if (horizontalLength < 1.0e-4 || RockSlingSpell.KNOCKBACK_STRENGTH <= 0.0f) {
            return;
        }
        // knockback(x, z) 把目标推向 (-x, -z)，所以传飞行方向的反向
        livingTarget.knockback(
                RockSlingSpell.KNOCKBACK_STRENGTH,
                -flightDirection.x / horizontalLength,
                -flightDirection.z / horizontalLength
        );
    }
}

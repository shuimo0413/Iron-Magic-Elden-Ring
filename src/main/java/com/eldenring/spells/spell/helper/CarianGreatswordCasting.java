package com.eldenring.spells.spell.helper;

import com.eldenring.spells.entity.CarianGreatswordEntity;
import com.eldenring.spells.spell.data.CarianGreatswordCastData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 卡利亚大剑吟唱期实体生命周期：生成、补绑、刷新伤害。
 */
public final class CarianGreatswordCasting {

    private CarianGreatswordCasting() {
    }

    /**
     * 若尚未绑定或实体已失效，则生成新的斩击锚点并写入 CastData。
     */
    public static void ensureGreatswordEntity(
            Level level,
            LivingEntity caster,
            CarianGreatswordCastData castData,
            float slashDamage
    ) {
        CarianGreatswordEntity existing = castData.greatswordEntity();
        if (existing != null && !existing.isRemoved()) {
            existing.setSlashDamage(slashDamage);
            return;
        }
        CarianGreatswordEntity greatswordEntity = new CarianGreatswordEntity(level, caster, slashDamage);
        level.addFreshEntity(greatswordEntity);
        castData.bindGreatswordEntity(greatswordEntity);
    }

    /**
     * 请求停止并清空绑定；实体会在本刀命中窗结束后自行 discard。
     */
    public static void requestStop(@Nullable CarianGreatswordCastData castData) {
        if (castData == null) {
            return;
        }
        castData.reset();
    }
}

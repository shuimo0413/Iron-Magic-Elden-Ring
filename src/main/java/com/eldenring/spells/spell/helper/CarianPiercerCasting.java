package com.eldenring.spells.spell.helper;

import com.eldenring.spells.entity.CarianPiercerEntity;
import com.eldenring.spells.spell.data.CarianPiercerCastData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 卡利亚贯刺吟唱期实体生命周期：生成、补绑、刷新伤害。
 */
public final class CarianPiercerCasting {

    private CarianPiercerCasting() {
    }

    /**
     * 若尚未绑定或实体已失效，则生成新的突刺锚点并写入 CastData。
     */
    public static void ensurePiercerEntity(
            Level level,
            LivingEntity caster,
            CarianPiercerCastData castData,
            float slashDamage
    ) {
        CarianPiercerEntity existing = castData.piercerEntity();
        if (existing != null && !existing.isRemoved()) {
            existing.setSlashDamage(slashDamage);
            return;
        }
        CarianPiercerEntity piercerEntity = new CarianPiercerEntity(level, caster, slashDamage);
        level.addFreshEntity(piercerEntity);
        castData.bindPiercerEntity(piercerEntity);
    }

    /**
     * 请求停止并清空绑定；实体会在命中窗结束后自行 discard。
     */
    public static void requestStop(@Nullable CarianPiercerCastData castData) {
        if (castData == null) {
            return;
        }
        castData.reset();
    }
}

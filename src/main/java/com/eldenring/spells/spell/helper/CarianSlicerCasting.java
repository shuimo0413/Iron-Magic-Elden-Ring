package com.eldenring.spells.spell.helper;

import com.eldenring.spells.entity.CarianSlicerEntity;
import com.eldenring.spells.spell.data.CarianSlicerCastData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 卡利亚迅剑吟唱期实体生命周期：生成、补绑、刷新伤害。
 */
public final class CarianSlicerCasting {

    private CarianSlicerCasting() {
    }

    /**
     * 若尚未绑定或实体已失效，则生成新的斩击锚点并写入 CastData。
     */
    public static void ensureSlicerEntity(
            Level level,
            LivingEntity caster,
            CarianSlicerCastData castData,
            float slashDamage
    ) {
        CarianSlicerEntity existing = castData.slicerEntity();
        if (existing != null && !existing.isRemoved()) {
            existing.setSlashDamage(slashDamage);
            return;
        }
        CarianSlicerEntity slicerEntity = new CarianSlicerEntity(level, caster, slashDamage);
        level.addFreshEntity(slicerEntity);
        castData.bindSlicerEntity(slicerEntity);
    }

    /**
     * 请求停止并清空绑定；实体会在本刀命中窗结束后自行 discard。
     */
    public static void requestStop(@Nullable CarianSlicerCastData castData) {
        if (castData == null) {
            return;
        }
        castData.reset();
    }
}

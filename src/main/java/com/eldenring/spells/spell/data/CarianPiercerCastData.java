package com.eldenring.spells.spell.data;

import com.eldenring.spells.entity.CarianPiercerEntity;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import org.jetbrains.annotations.Nullable;

/**
 * 卡利亚贯刺一次吟唱绑定的服务端斩击实体。
 */
public final class CarianPiercerCastData implements ICastData {

    @Nullable
    private CarianPiercerEntity piercerEntity;

    public CarianPiercerCastData() {
    }

    @Nullable
    public CarianPiercerEntity piercerEntity() {
        return piercerEntity;
    }

    public void bindPiercerEntity(CarianPiercerEntity piercerEntity) {
        this.piercerEntity = piercerEntity;
    }

    @Override
    public void reset() {
        if (this.piercerEntity != null && !this.piercerEntity.isRemoved()) {
            this.piercerEntity.requestStop();
        }
        this.piercerEntity = null;
    }
}

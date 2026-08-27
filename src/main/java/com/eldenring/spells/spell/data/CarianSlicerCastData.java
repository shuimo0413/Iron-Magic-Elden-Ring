package com.eldenring.spells.spell.data;

import com.eldenring.spells.entity.CarianSlicerEntity;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import org.jetbrains.annotations.Nullable;

/**
 * 卡利亚迅剑一次按住吟唱的附加状态：绑住跟手的辉剑实体。
 */
public final class CarianSlicerCastData implements ICastData {

    @Nullable
    private CarianSlicerEntity slicerEntity;

    @Nullable
    public CarianSlicerEntity slicerEntity() {
        return slicerEntity;
    }

    public void bindSlicerEntity(CarianSlicerEntity slicerEntity) {
        this.slicerEntity = slicerEntity;
    }

    @Override
    public void reset() {
        if (this.slicerEntity != null && !this.slicerEntity.isRemoved()) {
            this.slicerEntity.requestStop();
        }
        this.slicerEntity = null;
    }
}

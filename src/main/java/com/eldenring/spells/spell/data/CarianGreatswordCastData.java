package com.eldenring.spells.spell.data;

import com.eldenring.spells.entity.CarianGreatswordEntity;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import org.jetbrains.annotations.Nullable;

/**
 * 卡利亚大剑一次吟唱绑定的服务端斩击实体。
 */
public final class CarianGreatswordCastData implements ICastData {

    @Nullable
    private CarianGreatswordEntity greatswordEntity;

    public CarianGreatswordCastData() {
    }

    @Nullable
    public CarianGreatswordEntity greatswordEntity() {
        return greatswordEntity;
    }

    public void bindGreatswordEntity(CarianGreatswordEntity greatswordEntity) {
        this.greatswordEntity = greatswordEntity;
    }

    @Override
    public void reset() {
        if (this.greatswordEntity != null && !this.greatswordEntity.isRemoved()) {
            this.greatswordEntity.requestStop();
        }
        this.greatswordEntity = null;
    }
}

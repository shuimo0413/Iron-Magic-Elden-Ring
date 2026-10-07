package com.eldenring.spells.spell.data;

import com.eldenring.spells.entity.AdulasMoonbladeEntity;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import org.jetbrains.annotations.Nullable;

/**
 * 亚杜拉的月光剑一次吟唱绑定的服务端斩击锚点。
 */
public final class AdulasMoonbladeCastData implements ICastData {

    @Nullable
    private AdulasMoonbladeEntity moonbladeEntity;

    public AdulasMoonbladeCastData() {
    }

    @Nullable
    public AdulasMoonbladeEntity moonbladeEntity() {
        return moonbladeEntity;
    }

    public void bindMoonbladeEntity(AdulasMoonbladeEntity moonbladeEntity) {
        this.moonbladeEntity = moonbladeEntity;
    }

    @Override
    public void reset() {
        if (this.moonbladeEntity != null && !this.moonbladeEntity.isRemoved()) {
            this.moonbladeEntity.requestStop();
        }
        this.moonbladeEntity = null;
    }
}

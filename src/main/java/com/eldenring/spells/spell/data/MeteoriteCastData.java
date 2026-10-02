package com.eldenring.spells.spell.data;

import com.eldenring.spells.entity.MeteoriteVoidEntity;
import com.eldenring.spells.spell.MeteoriteSpell;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import org.jetbrains.annotations.Nullable;

/**
 * 陨石一次按住吟唱的附加状态。
 * <p>
 * 绑定头顶前方的虚空黑洞实体，并记录出弹间隔与首发状态。
 * 已经落下的陨石不绑在这里，松手不会把空中的陨石删掉。
 */
public final class MeteoriteCastData implements ICastData {

    /**
     * 距离下一颗陨石还要等几 tick。0 表示本 tick 该刷。
     * 单位：tick。
     */
    private int ticksUntilNextMeteorite;

    /** 本段吟唱是否已经落下第一颗；只给第一颗播飞弹射出音。 */
    private boolean firstMeteoriteSpawned;

    /** 本段吟唱的虚空黑洞；松手 / 打断时由 {@link #reset()} 或法术收掉。 */
    @Nullable
    private MeteoriteVoidEntity voidEntity;

    public MeteoriteCastData() {
        this.ticksUntilNextMeteorite = 0;
        this.firstMeteoriteSpawned = false;
    }

    /**
     * 按 {@link MeteoriteSpell#METEORITE_SPAWN_INTERVAL_TICKS} 消费一次出弹窗口。
     *
     * @return true 本 tick 该刷一颗陨石
     */
    public boolean tryConsumeSpawnInterval() {
        if (this.ticksUntilNextMeteorite > 0) {
            this.ticksUntilNextMeteorite--;
            return false;
        }
        this.ticksUntilNextMeteorite = Math.max(0, MeteoriteSpell.METEORITE_SPAWN_INTERVAL_TICKS - 1);
        return true;
    }

    /**
     * @return true 表示这次调用是本段吟唱的第一颗陨石，应该播射出音。
     */
    public boolean tryMarkFirstMeteoriteSpawned() {
        if (this.firstMeteoriteSpawned) {
            return false;
        }
        this.firstMeteoriteSpawned = true;
        return true;
    }

    @Nullable
    public MeteoriteVoidEntity voidEntity() {
        return voidEntity;
    }

    public void bindVoidEntity(MeteoriteVoidEntity voidEntity) {
        this.voidEntity = voidEntity;
    }

    /** 收掉黑洞（带收缩粒子）。重复调用安全。 */
    public void collapseVoid() {
        if (this.voidEntity != null && !this.voidEntity.isRemoved()) {
            this.voidEntity.collapseAndDiscard();
        }
        this.voidEntity = null;
    }

    @Override
    public void reset() {
        this.ticksUntilNextMeteorite = 0;
        this.firstMeteoriteSpawned = false;
        collapseVoid();
    }
}

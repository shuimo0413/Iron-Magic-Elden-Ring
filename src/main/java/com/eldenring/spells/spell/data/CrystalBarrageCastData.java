package com.eldenring.spells.spell.data;

import com.eldenring.spells.spell.CrystalBarrageSpell;
import io.redspace.ironsspellbooks.api.spells.ICastData;

/**
 * 结晶连弹一次按住吟唱的附加状态。
 * <p>
 * 只记录连射间隔与首发状态；移动和视线始终由施法者实时控制。
 * 已飞出的碎片不绑在这里，松手不会把空中的弹删掉。
 */
public final class CrystalBarrageCastData implements ICastData {

    /**
     * 距离下一发碎片还要等几 tick。0 表示本 tick 该刷。
     * 单位：tick。
     */
    private int ticksUntilNextShard;
    /** 本段吟唱是否已经刷过第一发；只给第一发出手闪光。 */
    private boolean firstShardSpawned;

    public CrystalBarrageCastData() {
        this.ticksUntilNextShard = 0;
        this.firstShardSpawned = false;
    }

    /**
     * 按 {@link CrystalBarrageSpell#SHARD_SPAWN_INTERVAL_TICKS} 消费一次生成窗口。
     *
     * @return true 本 tick 该刷一发碎片
     */
    public boolean tryConsumeSpawnInterval() {
        if (this.ticksUntilNextShard > 0) {
            this.ticksUntilNextShard--;
            return false;
        }
        this.ticksUntilNextShard = Math.max(0, CrystalBarrageSpell.SHARD_SPAWN_INTERVAL_TICKS - 1);
        return true;
    }

    /**
     * @return true 表示这次调用是本段吟唱的第一发，应该刷出手闪光。
     */
    public boolean tryMarkFirstShardSpawned() {
        if (this.firstShardSpawned) {
            return false;
        }
        this.firstShardSpawned = true;
        return true;
    }

    @Override
    public void reset() {
        this.ticksUntilNextShard = 0;
        this.firstShardSpawned = false;
    }
}

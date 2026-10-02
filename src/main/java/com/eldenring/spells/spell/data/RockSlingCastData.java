package com.eldenring.spells.spell.data;

import io.redspace.ironsspellbooks.api.spells.ICastData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 岩石球一次蓄力的附加状态：本次凝聚出来的岩石实体 UUID。
 * <p>
 * 满蓄时按这份名单发射，松手时按它碎裂。已经飞出去的岩石不再归这里管。
 */
public final class RockSlingCastData implements ICastData {

    /** 本次蓄力生成的悬停岩石（按槽位顺序：左 → 右）。 */
    private final List<UUID> hoveringRockUuids = new ArrayList<>();

    /** 记下一块刚生成的悬停岩石。 */
    public void addHoveringRock(UUID rockUuid) {
        hoveringRockUuids.add(rockUuid);
    }

    /** 只读名单。 */
    public List<UUID> hoveringRockUuids() {
        return Collections.unmodifiableList(hoveringRockUuids);
    }

    /** 发射 / 碎裂之后清空，避免同一份数据被处理两次。 */
    public void clearHoveringRocks() {
        hoveringRockUuids.clear();
    }

    @Override
    public void reset() {
        hoveringRockUuids.clear();
    }
}

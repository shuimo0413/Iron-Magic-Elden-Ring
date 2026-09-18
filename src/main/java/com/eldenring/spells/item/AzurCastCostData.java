package com.eldenring.spells.item;

import com.eldenring.spells.spell.cost.CastManaCostData;

/**
 * 旧测试兼容入口；运行时代码使用通用的 {@link CastManaCostData}。
 */
@Deprecated
public interface AzurCastCostData extends CastManaCostData {
    default double eldenRingSpells$getAzurMultiplier() {
        return eldenRingSpells$getManaCostMultiplier();
    }
}

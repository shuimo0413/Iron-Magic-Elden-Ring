package com.eldenring.spells.spell.cost;

/**
 * 持续施法起手时记录装备总蓝耗倍率，防止施法途中切换装备改变本次扣费。
 */
public interface CastManaCostData {
    double eldenRingSpells$getManaCostMultiplier();
}

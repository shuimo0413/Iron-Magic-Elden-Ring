package com.eldenring.spells.spell;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;

/**
 * 本模组法术的薄基类：只为了在配置加载后回写 {@link AbstractSpell} 的蓝耗 / 法强 / 吟唱字段。
 * <p>
 * 那些字段是 protected，必须在子类里赋值；整合包改 toml 发生在法术构造之后。
 */
public abstract class EldenRingAbstractSpell extends AbstractSpell {

    /**
     * 把当前 Spell 运行时字段写进铁魔法 AbstractSpell。冷却仍由铁魔法 JSON 管。
     */
    public final void applyBookStats(
            int baseManaCost,
            int manaCostPerLevel,
            int baseSpellPower,
            int spellPowerPerLevel,
            int castTimeTicks
    ) {
        this.baseManaCost = baseManaCost;
        this.manaCostPerLevel = manaCostPerLevel;
        this.baseSpellPower = baseSpellPower;
        this.spellPowerPerLevel = spellPowerPerLevel;
        this.castTime = castTimeTicks;
    }
}

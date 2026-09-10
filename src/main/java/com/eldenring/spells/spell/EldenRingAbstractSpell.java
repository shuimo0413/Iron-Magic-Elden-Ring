package com.eldenring.spells.spell;

import com.eldenring.spells.registry.ModSounds;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * 本模组法术的薄基类：只为了在配置加载后回写 {@link AbstractSpell} 的蓝耗 / 法强 / 吟唱字段。
 * <p>
 * 那些字段是 protected，必须在子类里赋值；整合包改 toml 发生在法术构造之后。
 * <p>
 * 数值表的「攻击力」按最终伤害理解，且可带 0.5 这类小数；铁魔法书本字段仍是 int，
 * 因此本模组用 float 存表值，写回时 {@link Math#round(float)}，伤害则走
 * {@link #damageFromTableAttack} 避免被 round 丢掉精度。
 */
public abstract class EldenRingAbstractSpell extends AbstractSpell {

    /**
     * 默认飞弹射出音。瞬时弹道咒在 {@code onCast} 时播放。
     * 长吟唱 / 持续咒若出弹不在收招瞬间（或根本没有飞弹），子类覆盖为 {@link Optional#empty()}。
     */
    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(ModSounds.SPELL_CAST.get());
    }

    /**
     * 把当前 Spell 运行时字段写进铁魔法 AbstractSpell。冷却仍由铁魔法 JSON 管。
     * <p>
     * 法强参数为 float（承接表里每级 0.5 / 1.5）；写入铁魔法 int 字段时四舍五入，仅影响面板近似。
     */
    public final void applyBookStats(
            int baseManaCost,
            int manaCostPerLevel,
            float baseSpellPower,
            float spellPowerPerLevel,
            int castTimeTicks
    ) {
        this.baseManaCost = baseManaCost;
        this.manaCostPerLevel = manaCostPerLevel;
        this.baseSpellPower = Math.round(baseSpellPower);
        this.spellPowerPerLevel = Math.round(spellPowerPerLevel);
        this.castTime = castTimeTicks;
    }

    /**
     * 用数值表 float 攻击力算最终伤害，并保留铁魔法施法者法强倍率。
     * <p>
     * 公式：{@code 表裸攻击力 × (getSpellPower / 铁魔法 int 裸法强) × 伤害系数}。
     * 当伤害系数为 1.0 时，裸施法者 1 级伤害即等于表「初始攻击力」。
     *
     * @param tableBaseAttack      表「初始攻击力」（可小数）
     * @param tableAttackPerLevel  表「每级提升攻击力」（可 0.5 / 1.5）
     * @param spellLevel           当前法术等级（从 1 起）
     * @param caster               施法者；可为 null（无属性倍率）
     * @param damagePerSpellPower  伤害系数；数值表对齐后统一为 1.0
     * @return 最终伤害
     */
    protected final float damageFromTableAttack(
            float tableBaseAttack,
            float tableAttackPerLevel,
            int spellLevel,
            LivingEntity caster,
            float damagePerSpellPower
    ) {
        int levelsAboveOne = Math.max(0, spellLevel - 1);
        float tableBareAttack = tableBaseAttack + tableAttackPerLevel * levelsAboveOne;
        float ironBarePower = this.baseSpellPower + this.spellPowerPerLevel * levelsAboveOne;
        float ironScaledPower = getSpellPower(spellLevel, caster);
        float entityPowerMultiplier = ironBarePower > 1.0e-4f
                ? (ironScaledPower / ironBarePower)
                : 1.0f;
        return tableBareAttack * entityPowerMultiplier * damagePerSpellPower;
    }
}

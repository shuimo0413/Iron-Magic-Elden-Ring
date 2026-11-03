package com.eldenring.spells.entity.academy;

import io.redspace.ironsspellbooks.entity.mobs.goals.WizardAttackGoal;

/**
 * 学院学者的施法 AI：走位、追击、施法计时全部沿用铁魔法 {@link WizardAttackGoal}，
 * 只在每次选咒前按所属教室的 JSON 施法表刷新法术列表。
 * <p>
 * 配置重载后，正在吟唱的法术照常放完，下一次选咒才换成新施法表。
 */
public class AcademyScholarAttackGoal extends WizardAttackGoal {

    private final AcademyScholarEntity scholar;

    /** 已套用的配置版本；初始值保证第一次选咒前一定会套用一次。 */
    private int appliedConfigRevision = Integer.MIN_VALUE;

    public AcademyScholarAttackGoal(
            AcademyScholarEntity scholar,
            double speedModifier,
            int attackIntervalMinTicks,
            int attackIntervalMaxTicks
    ) {
        super(scholar, speedModifier, attackIntervalMinTicks, attackIntervalMaxTicks);
        this.scholar = scholar;
    }

    @Override
    protected void doSpellAction() {
        refreshSpellsIfConfigReloaded();
        super.doSpellAction();
    }

    private void refreshSpellsIfConfigReloaded() {
        int currentConfigRevision = AcademyScholarSpellConfigLoader.revision();
        if (currentConfigRevision == appliedConfigRevision) {
            return;
        }
        AcademyScholarSpellConfig config = AcademyScholarSpellConfigLoader.get(scholar.classroom().spellConfigId());
        setSpells(config.attackSpells(), config.defenseSpells(), config.movementSpells(), config.supportSpells());
        setSpellQuality(config.minSpellQuality(), config.maxSpellQuality());
        appliedConfigRevision = currentConfigRevision;
    }
}

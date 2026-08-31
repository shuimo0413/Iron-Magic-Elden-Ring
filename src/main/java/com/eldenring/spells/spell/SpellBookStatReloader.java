package com.eldenring.spells.spell;

import com.eldenring.spells.registry.ModSpells;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;

/**
 * 把 Spell 运行时字段里的蓝耗 / 法强 / 吟唱 tick 写回 {@link AbstractSpell} 实例。
 * <p>
 * 这些字段在法术构造时拷过一份；配置是进世界才加载的，不回写的话 toml 改蓝耗不会生效。
 * 冷却仍由铁魔法 JSON 管，这里不动。
 */
public final class SpellBookStatReloader {

    private SpellBookStatReloader() {
    }

    public static void reloadAll() {
        final AbstractSpell first;
        try {
            first = ModSpells.GLINTSTONE_PEBBLE.get();
        } catch (RuntimeException ignored) {
            return;
        }
        if (!(first instanceof EldenRingAbstractSpell)) {
            return;
        }
        apply(
                first,
                GlintstonePebbleSpell.SPELL_BASE_MANA_COST,
                GlintstonePebbleSpell.SPELL_MANA_COST_PER_LEVEL,
                GlintstonePebbleSpell.SPELL_BASE_SPELL_POWER,
                GlintstonePebbleSpell.SPELL_SPELL_POWER_PER_LEVEL,
                GlintstonePebbleSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.SWIFT_GLINTSTONE_SHARD.get(),
                SwiftGlintstoneShardSpell.SPELL_BASE_MANA_COST,
                SwiftGlintstoneShardSpell.SPELL_MANA_COST_PER_LEVEL,
                SwiftGlintstoneShardSpell.SPELL_BASE_SPELL_POWER,
                SwiftGlintstoneShardSpell.SPELL_SPELL_POWER_PER_LEVEL,
                SwiftGlintstoneShardSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.GLINTSTONE_ARC.get(),
                GlintstoneArcSpell.SPELL_BASE_MANA_COST,
                GlintstoneArcSpell.SPELL_MANA_COST_PER_LEVEL,
                GlintstoneArcSpell.SPELL_BASE_SPELL_POWER,
                GlintstoneArcSpell.SPELL_SPELL_POWER_PER_LEVEL,
                GlintstoneArcSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.CRYSTAL_BARRAGE.get(),
                CrystalBarrageSpell.SPELL_BASE_MANA_COST,
                CrystalBarrageSpell.SPELL_MANA_COST_PER_LEVEL,
                CrystalBarrageSpell.SPELL_BASE_SPELL_POWER,
                CrystalBarrageSpell.SPELL_SPELL_POWER_PER_LEVEL,
                CrystalBarrageSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.CRYSTAL_BURST.get(),
                CrystalBurstSpell.SPELL_BASE_MANA_COST,
                CrystalBurstSpell.SPELL_MANA_COST_PER_LEVEL,
                CrystalBurstSpell.SPELL_BASE_SPELL_POWER,
                CrystalBurstSpell.SPELL_SPELL_POWER_PER_LEVEL,
                CrystalBurstSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.GREAT_GLINTSTONE_SHARD.get(),
                GreatGlintstoneShardSpell.SPELL_BASE_MANA_COST,
                GreatGlintstoneShardSpell.SPELL_MANA_COST_PER_LEVEL,
                GreatGlintstoneShardSpell.SPELL_BASE_SPELL_POWER,
                GreatGlintstoneShardSpell.SPELL_SPELL_POWER_PER_LEVEL,
                GreatGlintstoneShardSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.GLINTSTONE_COMET.get(),
                GlintstoneCometSpell.SPELL_BASE_MANA_COST,
                GlintstoneCometSpell.SPELL_MANA_COST_PER_LEVEL,
                GlintstoneCometSpell.SPELL_BASE_SPELL_POWER,
                GlintstoneCometSpell.SPELL_SPELL_POWER_PER_LEVEL,
                GlintstoneCometSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.GLINTSTONE_STARS.get(),
                GlintstoneStarsSpell.SPELL_BASE_MANA_COST,
                GlintstoneStarsSpell.SPELL_MANA_COST_PER_LEVEL,
                GlintstoneStarsSpell.SPELL_BASE_SPELL_POWER,
                GlintstoneStarsSpell.SPELL_SPELL_POWER_PER_LEVEL,
                GlintstoneStarsSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.STAR_SHOWER.get(),
                StarShowerSpell.SPELL_BASE_MANA_COST,
                StarShowerSpell.SPELL_MANA_COST_PER_LEVEL,
                StarShowerSpell.SPELL_BASE_SPELL_POWER,
                StarShowerSpell.SPELL_SPELL_POWER_PER_LEVEL,
                StarShowerSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.STARS_OF_RUIN.get(),
                StarsOfRuinSpell.SPELL_BASE_MANA_COST,
                StarsOfRuinSpell.SPELL_MANA_COST_PER_LEVEL,
                StarsOfRuinSpell.SPELL_BASE_SPELL_POWER,
                StarsOfRuinSpell.SPELL_SPELL_POWER_PER_LEVEL,
                StarsOfRuinSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.FOUNDING_RAIN_OF_STARS.get(),
                FoundingRainOfStarsSpell.SPELL_BASE_MANA_COST,
                FoundingRainOfStarsSpell.SPELL_MANA_COST_PER_LEVEL,
                FoundingRainOfStarsSpell.SPELL_BASE_SPELL_POWER,
                FoundingRainOfStarsSpell.SPELL_SPELL_POWER_PER_LEVEL,
                FoundingRainOfStarsSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.COMET.get(),
                CometSpell.SPELL_BASE_MANA_COST,
                CometSpell.SPELL_MANA_COST_PER_LEVEL,
                CometSpell.SPELL_BASE_SPELL_POWER,
                CometSpell.SPELL_SPELL_POWER_PER_LEVEL,
                CometSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.SPIRAL_SHARD.get(),
                SpiralShardSpell.SPELL_BASE_MANA_COST,
                SpiralShardSpell.SPELL_MANA_COST_PER_LEVEL,
                SpiralShardSpell.SPELL_BASE_SPELL_POWER,
                SpiralShardSpell.SPELL_SPELL_POWER_PER_LEVEL,
                SpiralShardSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.STARLIGHT.get(),
                StarlightSpell.SPELL_BASE_MANA_COST,
                StarlightSpell.SPELL_MANA_COST_PER_LEVEL,
                StarlightSpell.SPELL_BASE_SPELL_POWER,
                StarlightSpell.SPELL_SPELL_POWER_PER_LEVEL,
                StarlightSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.TERRA_MAGICA.get(),
                TerraMagicaSpell.SPELL_BASE_MANA_COST,
                TerraMagicaSpell.SPELL_MANA_COST_PER_LEVEL,
                TerraMagicaSpell.SPELL_BASE_SPELL_POWER,
                TerraMagicaSpell.SPELL_SPELL_POWER_PER_LEVEL,
                TerraMagicaSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.COMET_AZUR.get(),
                CometAzurSpell.SPELL_BASE_MANA_COST,
                CometAzurSpell.SPELL_MANA_COST_PER_LEVEL,
                CometAzurSpell.SPELL_BASE_SPELL_POWER,
                CometAzurSpell.SPELL_SPELL_POWER_PER_LEVEL,
                CometAzurSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.GAVEL_OF_HAIMA.get(),
                GavelOfHaimaSpell.SPELL_BASE_MANA_COST,
                GavelOfHaimaSpell.SPELL_MANA_COST_PER_LEVEL,
                GavelOfHaimaSpell.SPELL_BASE_SPELL_POWER,
                GavelOfHaimaSpell.SPELL_SPELL_POWER_PER_LEVEL,
                GavelOfHaimaSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.CANNON_OF_HAIMA.get(),
                CannonOfHaimaSpell.SPELL_BASE_MANA_COST,
                CannonOfHaimaSpell.SPELL_MANA_COST_PER_LEVEL,
                CannonOfHaimaSpell.SPELL_BASE_SPELL_POWER,
                CannonOfHaimaSpell.SPELL_SPELL_POWER_PER_LEVEL,
                CannonOfHaimaSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.CARIAN_SLICER.get(),
                CarianSlicerSpell.SPELL_BASE_MANA_COST,
                CarianSlicerSpell.SPELL_MANA_COST_PER_LEVEL,
                CarianSlicerSpell.SPELL_BASE_SPELL_POWER,
                CarianSlicerSpell.SPELL_SPELL_POWER_PER_LEVEL,
                CarianSlicerSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.CARIAN_GREATSWORD.get(),
                CarianGreatswordSpell.SPELL_BASE_MANA_COST,
                CarianGreatswordSpell.SPELL_MANA_COST_PER_LEVEL,
                CarianGreatswordSpell.SPELL_BASE_SPELL_POWER,
                CarianGreatswordSpell.SPELL_SPELL_POWER_PER_LEVEL,
                CarianGreatswordSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.MAGIC_GLINTBLADE.get(),
                MagicGlintbladeSpell.SPELL_BASE_MANA_COST,
                MagicGlintbladeSpell.SPELL_MANA_COST_PER_LEVEL,
                MagicGlintbladeSpell.SPELL_BASE_SPELL_POWER,
                MagicGlintbladeSpell.SPELL_SPELL_POWER_PER_LEVEL,
                MagicGlintbladeSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.GLINTBLADE_PHALANX.get(),
                GlintbladePhalanxSpell.SPELL_BASE_MANA_COST,
                GlintbladePhalanxSpell.SPELL_MANA_COST_PER_LEVEL,
                GlintbladePhalanxSpell.SPELL_BASE_SPELL_POWER,
                GlintbladePhalanxSpell.SPELL_SPELL_POWER_PER_LEVEL,
                GlintbladePhalanxSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.CARIAN_PHALANX.get(),
                CarianPhalanxSpell.SPELL_BASE_MANA_COST,
                CarianPhalanxSpell.SPELL_MANA_COST_PER_LEVEL,
                CarianPhalanxSpell.SPELL_BASE_SPELL_POWER,
                CarianPhalanxSpell.SPELL_SPELL_POWER_PER_LEVEL,
                CarianPhalanxSpell.SPELL_CAST_TIME_TICKS
        );
        apply(
                ModSpells.GREATBLADE_PHALANX.get(),
                GreatbladePhalanxSpell.SPELL_BASE_MANA_COST,
                GreatbladePhalanxSpell.SPELL_MANA_COST_PER_LEVEL,
                GreatbladePhalanxSpell.SPELL_BASE_SPELL_POWER,
                GreatbladePhalanxSpell.SPELL_SPELL_POWER_PER_LEVEL,
                GreatbladePhalanxSpell.SPELL_CAST_TIME_TICKS
        );
    }

    private static void apply(
            AbstractSpell spell,
            int baseManaCost,
            int manaCostPerLevel,
            int baseSpellPower,
            int spellPowerPerLevel,
            int castTimeTicks
    ) {
        if (spell instanceof EldenRingAbstractSpell eldenRingSpell) {
            eldenRingSpell.applyBookStats(
                    baseManaCost,
                    manaCostPerLevel,
                    baseSpellPower,
                    spellPowerPerLevel,
                    castTimeTicks
            );
        }
    }
}

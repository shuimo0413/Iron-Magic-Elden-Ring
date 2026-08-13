package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.SpiralShardProjectile;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.tuning.SpiralShardTuning;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * 旋飞魔砾（Spiral Shard）：射出一对沿欧拉双螺旋互旋的辉石彗星，中心轴弱追踪。
 */
public class SpiralShardSpell extends AbstractSpell {

    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "spiral_shard");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SpiralShardTuning.SPELL_MAX_LEVEL)
            .setCooldownSeconds(SpiralShardTuning.SPELL_COOLDOWN_SECONDS)
            .build();

    public SpiralShardSpell() {
        this.manaCostPerLevel = SpiralShardTuning.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = SpiralShardTuning.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = SpiralShardTuning.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = SpiralShardTuning.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = SpiralShardTuning.SPELL_BASE_MANA_COST;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamageAmount(spellLevel, caster), 2)
                ),
                Component.literal("Pierce ×" + SpiralShardTuning.PROJECTILE_MAX_ENTITY_HITS)
        );
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellResourceLocation;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity castingEntity,
            CastSource castSource,
            MagicData playerMagicData
    ) {
        if (!level.isClientSide) {
            GlintstoneCastHelper.spawnAlongLook(
                    level,
                    castingEntity,
                    SpiralShardProjectile::new,
                    SpiralShardTuning.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                    SpiralShardTuning.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS,
                    SpiralShardTuning.CAST_BURST_PARTICLE_INTENSITY,
                    getDamageAmount(spellLevel, castingEntity),
                    castingEntity.getLookAngle(),
                    true
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDamageAmount(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity) * SpiralShardTuning.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.SwiftGlintstoneShardProjectile;
import com.eldenring.spells.tuning.SwiftGlintstoneShardTuning;
import com.eldenring.spells.registry.ModSchools;
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
 * 辉石迅魔砾：比魔砾更快、更便宜、单发更弱，适合走位连射。
 */
public class SwiftGlintstoneShardSpell extends AbstractSpell {

    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "swift_glintstone_shard");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SwiftGlintstoneShardTuning.SPELL_MAX_LEVEL)
            .setCooldownSeconds(SwiftGlintstoneShardTuning.SPELL_COOLDOWN_SECONDS)
            .build();

    public SwiftGlintstoneShardSpell() {
        this.manaCostPerLevel = SwiftGlintstoneShardTuning.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = SwiftGlintstoneShardTuning.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = SwiftGlintstoneShardTuning.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = SwiftGlintstoneShardTuning.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = SwiftGlintstoneShardTuning.SPELL_BASE_MANA_COST;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamageAmount(spellLevel, caster), 2)
                )
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
                    SwiftGlintstoneShardProjectile::new,
                    SwiftGlintstoneShardTuning.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                    SwiftGlintstoneShardTuning.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS,
                    SwiftGlintstoneShardTuning.CAST_BURST_PARTICLE_INTENSITY,
                    getDamageAmount(spellLevel, castingEntity),
                    castingEntity.getLookAngle(),
                    true
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDamageAmount(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity)
                * SwiftGlintstoneShardTuning.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

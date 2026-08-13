package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GlintstoneStarVolleyEntity;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.tuning.GlintstoneStarsTuning;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * 辉石流星：三发依次飞出，每发一出现即以「前冲 + 上扬」立刻强追踪。
 * <p>
 * 连发由 {@link GlintstoneStarVolleyEntity} 按 tick 错峰，避免三发叠在同一帧造成骗伤。
 */
public class GlintstoneStarsSpell extends AbstractSpell {

    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "glintstone_stars");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(GlintstoneStarsTuning.SPELL_MAX_LEVEL)
            .setCooldownSeconds(GlintstoneStarsTuning.SPELL_COOLDOWN_SECONDS)
            .build();

    public GlintstoneStarsSpell() {
        this.manaCostPerLevel = GlintstoneStarsTuning.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = GlintstoneStarsTuning.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = GlintstoneStarsTuning.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = GlintstoneStarsTuning.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = GlintstoneStarsTuning.SPELL_BASE_MANA_COST;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamageAmountPerProjectile(spellLevel, caster), 2)
                ),
                Component.literal("×" + GlintstoneStarsTuning.PROJECTILE_COUNT)
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

    /**
     * 连发命中间隔短于原版受伤无敌帧，必须把 i-frame 清零，否则后两发会被吞成骗伤。
     */
    @Override
    public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setIFrames(0);
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
            GlintstoneStarVolleyEntity volleyEntity = new GlintstoneStarVolleyEntity(
                    level,
                    castingEntity,
                    GlintstoneStarVolleyEntity.VolleyKind.GLINTSTONE_STARS,
                    getDamageAmountPerProjectile(spellLevel, castingEntity)
            );
            level.addFreshEntity(volleyEntity);
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDamageAmountPerProjectile(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity)
                * GlintstoneStarsTuning.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

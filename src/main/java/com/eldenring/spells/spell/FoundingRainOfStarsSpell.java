package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.FoundingRainOfStarsEntity;
import com.eldenring.spells.particle.glintstone.GlintstoneFx;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.tuning.FoundingRainOfStarsTuning;
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
 * 创星雨（Founding Rain of Stars）：原初辉石咒。
 * <p>
 * 右手前方铺星云 → 光点飞向出手瞬间钉死的雨云圆心 → 云层落下白紫雨针，雨柱内持续受伤。
 * 雨针不复用彗星/流星弹道，只复用曲线光带的画法；数字改 {@link FoundingRainOfStarsTuning}。
 */
public class FoundingRainOfStarsSpell extends AbstractSpell {

    /** 注册 ID：{@code elden_ring_spells:founding_rain_of_stars}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "founding_rain_of_stars");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(FoundingRainOfStarsTuning.SPELL_MAX_LEVEL)
            .setCooldownSeconds(FoundingRainOfStarsTuning.SPELL_COOLDOWN_SECONDS)
            .build();

    public FoundingRainOfStarsSpell() {
        this.manaCostPerLevel = FoundingRainOfStarsTuning.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = FoundingRainOfStarsTuning.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = FoundingRainOfStarsTuning.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = FoundingRainOfStarsTuning.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = FoundingRainOfStarsTuning.SPELL_BASE_MANA_COST;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamageAmountPerRainDrop(spellLevel, caster), 2)
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

    /**
     * 雨幕是持续范围伤，必须带上与结算间隔相同的 i-frame。
     * 默认铁魔法伤害若 i-frame 为 0，会在一次红闪里叠两下，体感骗伤。
     */
    @Override
    public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker)
                .setIFrames(FoundingRainOfStarsTuning.RAIN_ZONE_DAMAGE_INTERVAL_TICKS);
    }

    /**
     * 出手瞬间铺星云，并挂上时序实体。光点升空与落雨都由时序实体按 tick 推进。
     */
    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity castingEntity,
            CastSource castSource,
            MagicData playerMagicData
    ) {
        if (!level.isClientSide) {
            GlintstoneFx.starRiver(
                    level,
                    castingEntity,
                    FoundingRainOfStarsTuning.CAST_NEBULA_INTENSITY,
                    true
            );
            level.addFreshEntity(new FoundingRainOfStarsEntity(
                    level,
                    castingEntity,
                    getDamageAmountPerRainDrop(spellLevel, castingEntity)
            ));
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    /**
     * 雨幕每次结算的伤害 = 铁魔法法术强度 × Tuning 系数。站在云下会连续挨打。
     */
    private float getDamageAmountPerRainDrop(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity)
                * FoundingRainOfStarsTuning.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

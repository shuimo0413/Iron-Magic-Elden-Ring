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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * 创星雨（Founding Rain of Stars）：原初辉石咒。
 * <p>
 * <strong>本阶段</strong>：右手前方铺星云 → 光点飞向身前 3 格、眼上 4 格的汇聚点 → 消失时钉一条横向深紫/深蓝星云（3 秒淡入淡出）。
 * 落星雨与伤害下一步再做。
 * <p>
 * 星云布局复用 {@link GlintstoneFx#starRiver}；身前雨云走软光面片 + 白星星。
 * 升空节拍由 {@link FoundingRainOfStarsEntity} 按 tick 推进。数字改 {@link FoundingRainOfStarsTuning}。
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
     * 出手瞬间铺星云，并挂上时序实体。光点升空不在这里刷——必须等星云开始淡出。
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
            level.addFreshEntity(new FoundingRainOfStarsEntity(level, castingEntity));
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }
}

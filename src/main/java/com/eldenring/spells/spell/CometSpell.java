package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.CometProjectile;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.sigil.AcademySigilFx;
import com.eldenring.spells.tuning.CometTuning;
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
 * 帚星（Comet）：辉石单发线的顶点，瞬时放出巨型彗星，命中后大半径爆炸。
 * <p>
 * 类名是 {@code CometSpell}、注册 path 是 {@code comet}，对应法环「帚星」而非「辉石彗星」。
 * 辉石彗星见 {@link GlintstoneCometSpell}。本咒蓝耗 / 冷却 / 爆炸半径都明显更大，
 * 适合作为高压单体 / 小范围清场，而不是连射填充。
 */
public class CometSpell extends AbstractSpell {

    /** 注册 ID：{@code elden_ring_spells:comet}。语言键 / 图标 path 也是 {@code comet}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "comet");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(CometTuning.SPELL_MAX_LEVEL)
            .setCooldownSeconds(CometTuning.SPELL_COOLDOWN_SECONDS)
            .build();

    public CometSpell() {
        this.baseManaCost = CometTuning.SPELL_BASE_MANA_COST;
        this.manaCostPerLevel = CometTuning.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = CometTuning.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = CometTuning.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = CometTuning.SPELL_CAST_TIME_TICKS;
    }

    /** 法术书：伤害 + 爆炸半径（方块）。 */
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamageAmount(spellLevel, caster), 2)
                ),
                Component.translatable(
                        "ui.irons_spellbooks.radius",
                        Utils.stringTruncation(CometTuning.EXPLOSION_RADIUS_BLOCKS, 1)
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
     * 服务端生成 {@link CometProjectile}。弹体更大，CastHelper 里的嵌块回退对它尤其重要，
     * 否则贴墙出手会整颗彗星直接撞没。
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
            AcademySigilFx.spawnAboveHead(level, castingEntity);
            GlintstoneCastHelper.spawnAlongLook(
                    level,
                    castingEntity,
                    CometProjectile::new,
                    CometTuning.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                    CometTuning.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS,
                    CometTuning.CAST_BURST_PARTICLE_INTENSITY,
                    getDamageAmount(spellLevel, castingEntity),
                    castingEntity.getLookAngle(),
                    true
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDamageAmount(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity) * CometTuning.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

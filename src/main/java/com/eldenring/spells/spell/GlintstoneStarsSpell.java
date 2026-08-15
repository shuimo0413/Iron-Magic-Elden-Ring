package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GlintstoneStarVolleyEntity;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.sigil.AcademySigilFx;
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
 * 辉石流星：瞬时放出三发依次飞出的强追踪小彗星。
 * <p>
 * 本类<strong>不直接生成弹道</strong>。{@code onCast} 只刷一个
 * {@link GlintstoneStarVolleyEntity}（{@code VolleyKind.GLINTSTONE_STARS}），
 * 由它按 tick 错峰调用 {@link GlintstoneCastHelper} 出弹。
 * 若三发在同一帧生成，会叠在同一像素上，体感像一发骗伤。
 * <p>
 * 每发一出现就沿视线前冲并立刻强追踪，手感接近法环辉石流星而不是魔砾那种限角轻追。
 * 发数 / 间隔 / 圆阵半径改 {@link GlintstoneStarsTuning}。
 */
public class GlintstoneStarsSpell extends AbstractSpell {

    /** 注册 ID：{@code elden_ring_spells:glintstone_stars}。 */
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

    /**
     * 法术书：单发伤害 + {@code ×3}。总伤约等于单发 × 发数，但目标可能躲开后几发。
     */
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
     * 原版生物受伤后有约 10 tick 无敌帧（i-frame）。三连发间隔短于这个窗口，
     * 若不把 i-frame 清零，后两发会打在无敌上变成骗伤。
     * 流星雨 / 毁灭流星同样覆盖此方法。
     */
    @Override
    public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setIFrames(0);
    }

    /**
     * 服务端只生成齐射实体并传入「每一发」的伤害。
     * 齐射实体会自己排正三角形阵面、按间隔出弹，Spell 不再循环 {@code spawnAlongLook}。
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

    /** 单发伤害。总伤需再乘 {@link GlintstoneStarsTuning#PROJECTILE_COUNT}。 */
    private float getDamageAmountPerProjectile(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity)
                * GlintstoneStarsTuning.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

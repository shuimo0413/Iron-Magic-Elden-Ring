package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GlintstoneStarVolleyEntity;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.sigil.AcademySigilFx;
import com.eldenring.spells.tuning.StarShowerTuning;
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
 * 流星雨：辉石流星的六发升级版，仍是瞬时施法。
 * <p>
 * 实现与 {@link GlintstoneStarsSpell} 同构：本类只生成
 * {@link GlintstoneStarVolleyEntity}（{@code VolleyKind.STAR_SHOWER}），
 * 由齐射实体按正六边形阵面错峰出弹。发数、间隔、圆半径在 {@link StarShowerTuning}。
 * <p>
 * 单发伤害通常低于辉石流星，靠发数换总伤；同样必须清零 i-frame，否则后几发会被无敌吞掉。
 */
public class StarShowerSpell extends AbstractSpell {

    /** 注册 ID：{@code elden_ring_spells:star_shower}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "star_shower");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(StarShowerTuning.SPELL_MAX_LEVEL)
            .setCooldownSeconds(StarShowerTuning.SPELL_COOLDOWN_SECONDS)
            .build();

    public StarShowerSpell() {
        this.manaCostPerLevel = StarShowerTuning.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = StarShowerTuning.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = StarShowerTuning.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = StarShowerTuning.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = StarShowerTuning.SPELL_BASE_MANA_COST;
    }

    /** 法术书：单发伤害 + {@code ×6}。 */
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamageAmountPerProjectile(spellLevel, caster), 2)
                ),
                Component.literal("×" + StarShowerTuning.PROJECTILE_COUNT)
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
     * 六连发间隔短于原版受伤无敌帧，必须把 i-frame 清零，否则后几发会被吞成骗伤。
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
            AcademySigilFx.spawnAboveHead(level, castingEntity);
            GlintstoneStarVolleyEntity volleyEntity = new GlintstoneStarVolleyEntity(
                    level,
                    castingEntity,
                    GlintstoneStarVolleyEntity.VolleyKind.STAR_SHOWER,
                    getDamageAmountPerProjectile(spellLevel, castingEntity)
            );
            level.addFreshEntity(volleyEntity);
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDamageAmountPerProjectile(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity)
                * StarShowerTuning.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

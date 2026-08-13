package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GlintstoneStarVolleyEntity;
import com.eldenring.spells.particle.glintstone.GlintstoneFx;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.tuning.StarsOfRuinTuning;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 毁灭流星：蓄力铺开星河，随后八发亮蓝 / 深蓝交织的追踪流星依次飞出。
 */
public class StarsOfRuinSpell extends AbstractSpell {

    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "stars_of_ruin");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(StarsOfRuinTuning.SPELL_MAX_LEVEL)
            .setCooldownSeconds(StarsOfRuinTuning.SPELL_COOLDOWN_SECONDS)
            .build();

    public StarsOfRuinSpell() {
        this.manaCostPerLevel = StarsOfRuinTuning.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = StarsOfRuinTuning.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = StarsOfRuinTuning.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = StarsOfRuinTuning.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = StarsOfRuinTuning.SPELL_BASE_MANA_COST;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamageAmountPerProjectile(spellLevel, caster), 2)
                ),
                Component.literal("×" + StarsOfRuinTuning.PROJECTILE_COUNT)
        );
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellResourceLocation;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BEACON_AMBIENT);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_RAISED_HAND;
    }

    /**
     * 八连发间隔短于原版受伤无敌帧，必须把 i-frame 清零，否则后几发会被吞成骗伤。
     */
    @Override
    public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setIFrames(0);
    }

    /**
     * 蓄力期间每 tick 在施法者周围铺一层旋转星河（亮蓝 / 深蓝粉尘 + 闪星）。
     */
    @Override
    public void onServerCastTick(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        if (!level.isClientSide) {
            GlintstoneFx.starRiverOrbit(
                    level,
                    entity,
                    StarsOfRuinTuning.STAR_RIVER_CAST_INTENSITY,
                    true
            );
        }
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
            GlintstoneFx.starRiver(
                    level,
                    castingEntity.getEyePosition(),
                    castingEntity.getLookAngle(),
                    StarsOfRuinTuning.CAST_BURST_PARTICLE_INTENSITY,
                    StarsOfRuinTuning.STAR_RIVER_LENGTH_BLOCKS,
                    StarsOfRuinTuning.STAR_RIVER_RADIUS_BLOCKS,
                    true
            );
            GlintstoneStarVolleyEntity volleyEntity = new GlintstoneStarVolleyEntity(
                    level,
                    castingEntity,
                    GlintstoneStarVolleyEntity.VolleyKind.STARS_OF_RUIN,
                    getDamageAmountPerProjectile(spellLevel, castingEntity)
            );
            level.addFreshEntity(volleyEntity);
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDamageAmountPerProjectile(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity)
                * StarsOfRuinTuning.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.CometAzurJetEntity;
import com.eldenring.spells.particle.cometazur.CometAzurFx;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.tuning.CometAzurTuning;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 彗星亚兹勒（Comet Azur）。
 * <p>
 * {@link CastType#CONTINUOUS}：按住右键进入吟唱，同一时间只能有一道。
 * 前 {@link CometAzurTuning#STARTUP_DURATION_TICKS} tick 是蓄力漩涡；
 * 蓄力结束爆一圈无伤星辰涟漪，随后刷出星河喷流实体（ribbon 墨绿色光柱）+ 周围粒子。
 * 松手或蓝不够会停；{@link CometAzurCastData#reset()} 负责丢弃喷流实体。
 */
public class CometAzurSpell extends AbstractSpell {

    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "comet_azur");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(CometAzurTuning.SPELL_MAX_LEVEL)
            .setCooldownSeconds(CometAzurTuning.SPELL_COOLDOWN_SECONDS)
            .build();

    public CometAzurSpell() {
        this.baseManaCost = CometAzurTuning.SPELL_BASE_MANA_COST;
        this.manaCostPerLevel = CometAzurTuning.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = CometAzurTuning.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = CometAzurTuning.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = CometAzurTuning.SPELL_CAST_TIME_TICKS;
    }

    /**
     * 持续射线：关掉无敌帧，否则 4 tick 结算会被原版 i-frame 吞掉大半。
     */
    @Override
    public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setIFrames(0);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        getDamageText(spellLevel, caster)
                )
        );
    }

    private String getDamageText(int spellLevel, LivingEntity caster) {
        float damage = getDamage(spellLevel, caster);
        return String.format("%.1f", damage);
    }

    /**
     * 每次射线结算的伤害。持续吟唱会按 {@link CometAzurTuning#JET_BEAM_DAMAGE_INTERVAL_TICKS} 重复打。
     */
    public float getDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * CometAzurTuning.JET_BEAM_DAMAGE_PER_SPELL_POWER;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellResourceLocation;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    /**
     * CONTINUOUS 大约每 10 tick 调一次 {@code onCast}，结束音若放这里会每半秒响一次。
     * 喷流循环音以后再挂。
     */
    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    /**
     * 吟唱开始只走一次：刷 2 秒蓄力漩涡。脉冲 {@code onCast} 不再刷，避免叠好几团。
     */
    @Override
    public void onServerPreCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        if (!level.isClientSide && playerMagicData != null) {
            Vec3 vortexCenter = CometAzurFx.vortexCenterInFrontOf(entity);
            playerMagicData.setAdditionalCastData(new CometAzurCastData(
                    vortexCenter,
                    entity.getYRot(),
                    entity.getXRot()
            ));
            CometAzurFx.spawnStartupVortex(level, entity);
        }
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
    }

    /**
     * 每 tick 检查蓄力是否走完。冲击波只爆一次；之后维持喷流实体并刷周围粒子。
     */
    @Override
    public void onServerCastTick(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        if (level.isClientSide || playerMagicData == null) {
            return;
        }
        if (!(playerMagicData.getAdditionalCastData() instanceof CometAzurCastData castData)) {
            return;
        }
        int elapsedCastTicks = playerMagicData.getCastDuration() - playerMagicData.getCastDurationRemaining();
        if (elapsedCastTicks < CometAzurTuning.STARTUP_DURATION_TICKS) {
            return;
        }
        if (castData.tryMarkChargeShockwaveSpawned()) {
            CometAzurFx.spawnChargeShockwave(level, castData);
        }
        ensureJetEntity(level, spellLevel, entity, castData);
        int jetElapsedTicks = elapsedCastTicks - CometAzurTuning.STARTUP_DURATION_TICKS;
        if (jetElapsedTicks % CometAzurTuning.JET_SURROUND_SPAWN_INTERVAL_TICKS == 0) {
            CometAzurFx.spawnJetSurround(level, entity);
        }
    }

    /**
     * 确保世界里只有一道喷流：没有就生成，有就 refresh 保活并更新朝向 / 伤害。
     */
    private void ensureJetEntity(
            Level level,
            int spellLevel,
            LivingEntity caster,
            CometAzurCastData castData
    ) {
        float damagePerHit = getDamage(spellLevel, caster);
        CometAzurJetEntity existingJet = castData.jetEntity();
        if (existingJet == null || existingJet.isRemoved()) {
            CometAzurJetEntity jetEntity = new CometAzurJetEntity(level, caster, damagePerHit, spellLevel);
            level.addFreshEntity(jetEntity);
            castData.bindJetEntity(jetEntity);
            return;
        }
        existingJet.refreshFromCaster(caster, damagePerHit, spellLevel);
    }

    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity castingEntity,
            CastSource castSource,
            MagicData playerMagicData
    ) {
        // CONTINUOUS 脉冲：顺带保活喷流，避免 onServerCastTick 节拍抖动时实体超时自毁。
        if (!level.isClientSide
                && playerMagicData.getAdditionalCastData() instanceof CometAzurCastData castData) {
            int elapsedCastTicks = playerMagicData.getCastDuration() - playerMagicData.getCastDurationRemaining();
            if (elapsedCastTicks >= CometAzurTuning.STARTUP_DURATION_TICKS) {
                ensureJetEntity(level, spellLevel, castingEntity, castData);
            }
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }
}

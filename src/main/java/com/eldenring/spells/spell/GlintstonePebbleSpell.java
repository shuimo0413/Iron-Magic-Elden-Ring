package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GlintstonePebbleProjectile;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.tuning.GlintstonePebbleTuning;
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
 * 辉石魔砾法术（艾尔登法环基础辉石咒术）。
 * <p>
 * 施法模式：瞬时（{@link CastType#INSTANT}），对应原作「可移动、可连发」的短吟唱弹道。
 * 效果：从施法者眼部沿视线射出 {@link GlintstonePebbleProjectile}，带限角追踪。
 * <p>
 * 蓝耗、冷却、伤害系数等平衡数值改 {@link GlintstonePebbleTuning}；
 * 弹道速度 / 转向角也在同一 Tuning 类中。
 */
public class GlintstonePebbleSpell extends AbstractSpell {

    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "glintstone_pebble");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            // 辉石学派（elden_ring_spells:glintstone）
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(GlintstonePebbleTuning.SPELL_MAX_LEVEL)
            .setCooldownSeconds(GlintstonePebbleTuning.SPELL_COOLDOWN_SECONDS)
            .build();

    public GlintstonePebbleSpell() {
        this.manaCostPerLevel = GlintstonePebbleTuning.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = GlintstonePebbleTuning.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = GlintstonePebbleTuning.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = GlintstonePebbleTuning.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = GlintstonePebbleTuning.SPELL_BASE_MANA_COST;
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

    /**
     * 服务端生成弹道并播放施法爆发粒子；末尾必须 {@code super.onCast} 以走铁魔法收尾音效等逻辑。
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
            GlintstoneCastHelper.spawnAlongLook(
                    level,
                    castingEntity,
                    GlintstonePebbleProjectile::new,
                    GlintstonePebbleTuning.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                    GlintstonePebbleTuning.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS,
                    1.0f,
                    getDamageAmount(spellLevel, castingEntity),
                    castingEntity.getLookAngle(),
                    true
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDamageAmount(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity) * GlintstonePebbleTuning.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

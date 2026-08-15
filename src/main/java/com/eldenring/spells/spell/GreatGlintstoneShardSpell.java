package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GreatGlintstoneShardProjectile;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.sigil.AcademySigilFx;
import com.eldenring.spells.tuning.GreatGlintstoneShardTuning;
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
 * 辉石大魔砾：瞬时放出的大体积辉石弹。
 * <p>
 * 比魔砾更慢、更肉，命中后在 {@link GreatGlintstoneShardTuning#EXPLOSION_RADIUS_BLOCKS} 内爆炸
 * （范围伤害写在弹道实体里，本类只负责把半径显示到法术书上）。
 * 定位介于魔砾与辉石彗星之间：非稀有、冷却仍短，适合清小群。
 */
public class GreatGlintstoneShardSpell extends AbstractSpell {

    /** 注册 ID：{@code elden_ring_spells:great_glintstone_shard}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "great_glintstone_shard");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(GreatGlintstoneShardTuning.SPELL_MAX_LEVEL)
            .setCooldownSeconds(GreatGlintstoneShardTuning.SPELL_COOLDOWN_SECONDS)
            .build();

    public GreatGlintstoneShardSpell() {
        this.baseManaCost = GreatGlintstoneShardTuning.SPELL_BASE_MANA_COST;
        this.manaCostPerLevel = GreatGlintstoneShardTuning.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = GreatGlintstoneShardTuning.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = GreatGlintstoneShardTuning.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = GreatGlintstoneShardTuning.SPELL_CAST_TIME_TICKS;
    }

    /**
     * 法术书显示单发伤害 + 爆炸半径（方块）。半径不随等级变，直接读 Tuning。
     */
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamageAmount(spellLevel, caster), 2)
                ),
                Component.translatable(
                        "ui.irons_spellbooks.radius",
                        Utils.stringTruncation(GreatGlintstoneShardTuning.EXPLOSION_RADIUS_BLOCKS, 1)
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

    /** 服务端生成大魔砾弹道；爆炸逻辑在 {@link GreatGlintstoneShardProjectile} 命中时触发。 */
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
                    GreatGlintstoneShardProjectile::new,
                    GreatGlintstoneShardTuning.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                    GreatGlintstoneShardTuning.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS,
                    GreatGlintstoneShardTuning.CAST_BURST_PARTICLE_INTENSITY,
                    getDamageAmount(spellLevel, castingEntity),
                    castingEntity.getLookAngle(),
                    true
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDamageAmount(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity)
                * GreatGlintstoneShardTuning.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

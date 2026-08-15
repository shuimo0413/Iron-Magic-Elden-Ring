package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.SpiralShardProjectile;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.sigil.AcademySigilFx;
import com.eldenring.spells.tuning.SpiralShardTuning;
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
 * 旋飞魔砾（Spiral Shard）：瞬时射出一对沿欧拉双螺旋互旋的辉石彗星。
 * <p>
 * 表面上像两发，世界里其实只有<strong>一个</strong> {@link SpiralShardProjectile}：
 * 实体沿中心轴弱追踪前进，渲染 / 碰撞在轴两侧各画一股螺旋。
 * 这样两股永远同步、不会因网络不同步拧成麻花。
 * <p>
 * 可穿透有限个实体（次数见 Tuning），法术书用 {@code Pierce ×N} 提示。
 * 螺旋半径、角速度、穿透次数改 {@link SpiralShardTuning}。
 */
public class SpiralShardSpell extends AbstractSpell {

    /** 注册 ID：{@code elden_ring_spells:spiral_shard}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "spiral_shard");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SpiralShardTuning.SPELL_MAX_LEVEL)
            .setCooldownSeconds(SpiralShardTuning.SPELL_COOLDOWN_SECONDS)
            .build();

    public SpiralShardSpell() {
        this.manaCostPerLevel = SpiralShardTuning.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = SpiralShardTuning.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = SpiralShardTuning.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = SpiralShardTuning.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = SpiralShardTuning.SPELL_BASE_MANA_COST;
    }

    /**
     * 法术书：单次命中伤害 + 最多可穿透的实体数。
     * 「Pierce」沿用铁魔法玩家熟悉的穿透措辞；数字来自 Tuning，不是随等级涨。
     */
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamageAmount(spellLevel, caster), 2)
                ),
                Component.literal("Pierce ×" + SpiralShardTuning.PROJECTILE_MAX_ENTITY_HITS)
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
     * 只生成一发螺旋实体。双股视觉与穿透计数都在 {@link SpiralShardProjectile} 内部，
     * 不要在这里循环 spawn 两次，否则两发会各自追踪、螺旋对不齐。
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
                    SpiralShardProjectile::new,
                    SpiralShardTuning.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                    SpiralShardTuning.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS,
                    SpiralShardTuning.CAST_BURST_PARTICLE_INTENSITY,
                    getDamageAmount(spellLevel, castingEntity),
                    castingEntity.getLookAngle(),
                    true
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDamageAmount(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity) * SpiralShardTuning.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

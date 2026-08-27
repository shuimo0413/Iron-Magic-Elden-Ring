package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.FoundingRainOfStarsEntity;
import com.eldenring.spells.particle.glintstone.GlintstoneFx;
import com.eldenring.spells.registry.ModSchools;
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
import com.eldenring.spells.particle.foundingrain.FoundingRainFx;

/**
 * 创星雨（Founding Rain of Stars）：原初辉石咒。
 * <p>
 * 右手前方铺星云 → 光点飞向出手瞬间钉死的雨云圆心 → 云层落下白紫雨针，雨柱内持续受伤。
 * 雨针不复用彗星/流星弹道，只复用曲线光带的画法；玩法数字改 {@link FoundingRainOfStarsSpell}。
 */
public class FoundingRainOfStarsSpell extends EldenRingAbstractSpell {

    public static final int SPELL_MAX_LEVEL = 1;
    public static final double SPELL_COOLDOWN_SECONDS = 8.0;

    public static int SPELL_BASE_MANA_COST = 48;
    public static int SPELL_MANA_COST_PER_LEVEL = 8;
    public static int SPELL_BASE_SPELL_POWER = 10;
    public static int SPELL_SPELL_POWER_PER_LEVEL = 1;
    public static int SPELL_CAST_TIME_TICKS = 0;

    /** 每次雨幕结算伤害 = 法强 × 本系数。 */
    public static float SPELL_DAMAGE_PER_SPELL_POWER = 0.5f;
    /** 每 tick 落下的雨针数量。 */
    public static int RAIN_DROPS_PER_TICK = 8;
    /** 雨针下落速度（方块/tick）。 */
    public static float RAIN_DROP_FALL_SPEED_BLOCKS_PER_TICK = 1.15f;
    /** 雨幕伤害结算间隔（tick）。 */
    public static int RAIN_ZONE_DAMAGE_INTERVAL_TICKS = 10;
    /** 头顶雨云水平半径（方块）。 */
    public static double OVERHEAD_CLOUD_RADIUS_BLOCKS = 4.0;
    /** 雨云寿命（tick）。 */
    public static int OVERHEAD_CLOUD_LIFETIME_TICKS = 100;

    /** 注册 ID：{@code elden_ring_spells:founding_rain_of_stars}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "founding_rain_of_stars");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SPELL_MAX_LEVEL)
            .setCooldownSeconds(SPELL_COOLDOWN_SECONDS)
            .build();

    public FoundingRainOfStarsSpell() {
        this.manaCostPerLevel = SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = SPELL_CAST_TIME_TICKS;
        this.baseManaCost = SPELL_BASE_MANA_COST;
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
                .setIFrames(RAIN_ZONE_DAMAGE_INTERVAL_TICKS);
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
                    FoundingRainFx.CAST_NEBULA_INTENSITY,
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
     * 雨幕每次结算的伤害 = 铁魔法法术强度 × 伤害系数。站在云下会连续挨打。
     */
    private float getDamageAmountPerRainDrop(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity)
                * SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

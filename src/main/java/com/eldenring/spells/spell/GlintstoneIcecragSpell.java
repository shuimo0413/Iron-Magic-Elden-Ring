package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GlintstoneIcecragProjectile;
import com.eldenring.spells.entity.GlintstoneTrailStyle;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.spell.helper.GlintstoneCastHelper;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.config.SpellConfigParameter;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 辉石冰块：大魔砾体型的浸寒辉石弹，命中小范围爆炸，被炸到的目标原版结霜（冻伤扣血），不会冻进冰牢。
 * <p>
 * 学派登记为辉石；法强额外吃冰霜学派超额（辉石法强 + 冰霜法强 - 1）。
 */
public class GlintstoneIcecragSpell extends EldenRingAbstractSpell {

    public static float PROJECTILE_FLIGHT_SPEED = 0.95f;
    public static double PROJECTILE_TRACKING_RANGE_BLOCKS = 28.0;

    /**
     * 最大射程（方块，按飞行路径长度）。飞满后直接消失。
     * 须 ≤ 300 tick × 弹速（铁魔法硬寿命）。调小 → 落空弹道更早消失。
     */
    public static double PROJECTILE_MAX_RANGE_BLOCKS = 128.0;

    public static float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 2.4f;
    public static int PROJECTILE_TRACKING_START_DELAY_TICKS = 6;
    public static float PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES = 36.0f;
    public static double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.45;
    public static double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;
    public static double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

    /**
     * 命中爆炸半径（方块）。调大 → 清小群更强；调小 → 更偏单体。
     */
    public static float EXPLOSION_RADIUS_BLOCKS = 1.8f;

    /**
     * 命中后原版完全冻结持续秒数（结霜 + 每 2 秒一次冻伤扣血 + 减速），见 {@link com.eldenring.spells.spell.helper.FrostHelper}。
     * 连续命中只刷新不叠加；调大 → 冻伤扣血次数更多。0 = 不结霜。
     */
    public static int SPELL_FROST_SECONDS = 4;

    public static float COMET_HEAD_BODY_SCALE_RADIAL = 1.05f;
    public static float COMET_HEAD_BODY_SCALE_ALONG = 0.85f;
    public static float COMET_HEAD_GLOW_SCALE = 1.45f;
    public static float COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE = 1.0f;
    public static float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.14f;
    public static float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 14.0f;

    /** 晶核 RGB（0–1），冷白偏蓝。 */
    public static float COMET_HEAD_CORE_RED = 0.72f;
    public static float COMET_HEAD_CORE_GREEN = 0.90f;
    public static float COMET_HEAD_CORE_BLUE = 1.0f;

    public static float COMET_HEAD_GLOW_RED = 0.85f;
    public static float COMET_HEAD_GLOW_GREEN = 0.96f;
    public static float COMET_HEAD_GLOW_BLUE = 1.0f;
    public static float COMET_HEAD_GLOW_ALPHA = 1.0f;

    public static GlintstoneTrailStyle TRAIL_STYLE =
            new GlintstoneTrailStyle(12.0, 0.145f, 0.034f, 0.22f, 0.08f, 32);

    public static float TRAIL_PARTICLE_INTENSITY = 0.55f;
    public static float IMPACT_PARTICLE_INTENSITY = 2.35f;
    public static float CAST_BURST_PARTICLE_INTENSITY = 1.85f;

    public static int SPELL_BASE_MANA_COST = 20;
    public static int SPELL_MANA_COST_PER_LEVEL = 3;
    public static float SPELL_BASE_SPELL_POWER = 8;
    public static float SPELL_SPELL_POWER_PER_LEVEL = 2;
    public static int SPELL_CAST_TIME_TICKS = 0;
    public static double SPELL_COOLDOWN_SECONDS = 3.0;
    public static int SPELL_MAX_LEVEL = 7;
    public static float SPELL_DAMAGE_PER_SPELL_POWER = 1.0f;
    public static double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.75;

    private final ResourceLocation spellResourceLocation =
            new ResourceLocation(EldenRingSpellsMod.MOD_ID, "glintstone_icecrag");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(GlintstoneIcecragSpell.SPELL_MAX_LEVEL)
            .setCooldownSeconds(GlintstoneIcecragSpell.SPELL_COOLDOWN_SECONDS)
            .build();

    public GlintstoneIcecragSpell() {
        this.baseManaCost = GlintstoneIcecragSpell.SPELL_BASE_MANA_COST;
        this.manaCostPerLevel = GlintstoneIcecragSpell.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = Math.round(GlintstoneIcecragSpell.SPELL_BASE_SPELL_POWER);
        this.spellPowerPerLevel = Math.round(GlintstoneIcecragSpell.SPELL_SPELL_POWER_PER_LEVEL);
        this.castTime = GlintstoneIcecragSpell.SPELL_CAST_TIME_TICKS;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamageAmount(spellLevel, caster), 2)
                ),
                Component.translatable(
                        "ui.irons_spellbooks.radius",
                        Utils.stringTruncation(GlintstoneIcecragSpell.EXPLOSION_RADIUS_BLOCKS, 1)
                ),
                Component.translatable(
                        "ui.irons_spellbooks.freeze_time",
                        Utils.timeFromTicks(GlintstoneIcecragSpell.SPELL_FROST_SECONDS * 20, 2)
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

    /**
     * 辉石法强与冰霜法强超额相加，不让两段学派倍率互乘。
     * {@code 学派倍率 = 辉石 + 冰霜 - 1}。
     */
    @Override
    public float getSpellPower(int spellLevel, @Nullable Entity sourceEntity) {
        double entitySpellPowerModifier = 1;
        double entitySchoolPowerModifier = 1;
        float configPowerModifier = SpellConfigManager.getSpellConfigValue(this, SpellConfigParameter.POWER_MULTIPLIER).floatValue();
        if (sourceEntity instanceof LivingEntity livingEntity) {
            entitySpellPowerModifier = livingEntity.getAttributeValue(AttributeRegistry.SPELL_POWER.get());
            entitySchoolPowerModifier = combinedGlintstoneAndIceSchoolPower(livingEntity);
        }
        return (float) ((baseSpellPower + spellPowerPerLevel * (spellLevel - 1))
                * entitySpellPowerModifier
                * entitySchoolPowerModifier
                * configPowerModifier);
    }

    @Override
    public float getEntityPowerMultiplier(@Nullable LivingEntity entity) {
        float configPowerModifier = SpellConfigManager.getSpellConfigValue(this, SpellConfigParameter.POWER_MULTIPLIER).floatValue();
        if (entity == null) {
            return configPowerModifier;
        }
        double entitySpellPowerModifier = entity.getAttributeValue(AttributeRegistry.SPELL_POWER.get());
        return (float) (configPowerModifier
                * entitySpellPowerModifier
                * combinedGlintstoneAndIceSchoolPower(entity));
    }

    /**
     * 只把两学派「超过 1.0 的部分」加在一起，避免 1.10×1.10 滚雪球。
     */
    public static double combinedGlintstoneAndIceSchoolPower(LivingEntity livingEntity) {
        double glintstonePower = ModSchools.GLINTSTONE.get().getPowerFor(livingEntity);
        double icePower = SchoolRegistry.ICE.get().getPowerFor(livingEntity);
        return glintstonePower + icePower - 1.0;
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
            GlintstoneCastHelper.spawnAlongLook(
                    level,
                    castingEntity,
                    GlintstoneIcecragProjectile::new,
                    GlintstoneIcecragSpell.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                    GlintstoneIcecragSpell.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS,
                    GlintstoneIcecragSpell.CAST_BURST_PARTICLE_INTENSITY,
                    getDamageAmount(spellLevel, castingEntity),
                    castingEntity.getLookAngle(),
                    true
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDamageAmount(int spellLevel, LivingEntity castingEntity) {
        return damageFromTableAttack(
                GlintstoneIcecragSpell.SPELL_BASE_SPELL_POWER,
                GlintstoneIcecragSpell.SPELL_SPELL_POWER_PER_LEVEL,
                spellLevel,
                castingEntity,
                GlintstoneIcecragSpell.SPELL_DAMAGE_PER_SPELL_POWER
        );
    }
}

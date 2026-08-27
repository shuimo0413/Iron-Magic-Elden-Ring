package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.CometProjectile;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.sigil.AcademySigilFx;
import com.eldenring.spells.spell.helper.GlintstoneCastHelper;
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
import com.eldenring.spells.entity.GlintstoneTrailStyle;

/**
 * 帚星（Comet）：辉石单发线的顶点，瞬时放出巨型彗星，命中后大半径爆炸。
 * <p>
 * 类名是 {@code CometSpell}、注册 path 是 {@code comet}，对应法环「帚星」而非「辉石彗星」。
 * 辉石彗星见 {@link GlintstoneCometSpell}。本咒蓝耗 / 冷却 / 爆炸半径都明显更大，
 * 适合作为高压单体 / 小范围清场，而不是连射填充。
 */
public class CometSpell extends EldenRingAbstractSpell {

    // —— 玩法/视觉数字（toml 只覆盖玩法字段）——
    public static float PROJECTILE_FLIGHT_SPEED = 1.4f;
        public static double PROJECTILE_TRACKING_RANGE_BLOCKS = 30.0;
        public static float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 2.2f;
        public static int PROJECTILE_TRACKING_START_DELAY_TICKS = 4;
        public static float PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES = 32.0f;
        public static double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.55;
        public static double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;
        public static double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

        /**
         * 命中爆炸半径（方块）。调大 → 清群更强；调小 → 更接近单体高伤。
         */
        public static float EXPLOSION_RADIUS_BLOCKS = 2.8f;

        /**
         * 刺簇整体缩放。块状，不要再沿飞行轴拉成梭子。
         * 调大 → 整团晶刺更大。
         */
        public static float COMET_HEAD_BODY_SCALE = 1.80f;

        /** 包住刺簇的柔光晕缩放。 */
        public static float COMET_HEAD_GLOW_SCALE = 2.05f;

        /** 略拉长，仍以球形光晕为主，避免把刺簇重新吃成梭子。 */
        public static float COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE = 1.25f;

        public static float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.16f;
        public static float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 10.0f;

        /**
         * 刺簇绕飞行轴自转（度 / tick）。调大 → 侧面更容易看见刺。
         */
        public static float CLUSTER_SPIN_DEGREES_PER_TICK = 7.0f;

        /** 不规则核心：更深的青，对照原作暗核。 */
        public static float COMET_HEAD_CORE_RED = 0.04f;
        public static float COMET_HEAD_CORE_GREEN = 0.32f;
        public static float COMET_HEAD_CORE_BLUE = 0.48f;

        /** 尖刺：更亮的白青。 */
        public static float COMET_HEAD_SPIKE_RED = 0.38f;
        public static float COMET_HEAD_SPIKE_GREEN = 0.94f;
        public static float COMET_HEAD_SPIKE_BLUE = 1.0f;

        public static float COMET_HEAD_GLOW_RED = 0.22f;
        public static float COMET_HEAD_GLOW_GREEN = 0.88f;
        public static float COMET_HEAD_GLOW_BLUE = 1.0f;
        public static float COMET_HEAD_GLOW_ALPHA = 1.0f;

        /**
         * 帚星扫帚彗尾：更宽更长 + 外雾层 + 加法芯 + 五条螺旋细丝。
         */
        public static com.eldenring.spells.entity.GlintstoneTrailStyle TRAIL_STYLE =
                new com.eldenring.spells.entity.GlintstoneTrailStyle(
                        64.0,
                        0.280f,
                        0.055f,
                        0.14f,
                        0.05f,
                        80,
                        new com.eldenring.spells.entity.GlintstoneTrailStyle.HelixStyle(5, 0.42f, 0.16f, 0.055f, 0.16f, 0.07f),
                        true,
                        true
                );

        /** 拖尾点缀强度倍率；不影响几何光束长宽。 */
        public static float TRAIL_PARTICLE_INTENSITY = 0.55f;
        public static float IMPACT_PARTICLE_INTENSITY = 1.80f;
        public static float CAST_BURST_PARTICLE_INTENSITY = 2.1f;

        public static int SPELL_BASE_MANA_COST = 24;
        public static int SPELL_MANA_COST_PER_LEVEL = 4;
        public static int SPELL_BASE_SPELL_POWER = 18;
        public static int SPELL_SPELL_POWER_PER_LEVEL = 2;
        public static int SPELL_CAST_TIME_TICKS = 0;
        public static double SPELL_COOLDOWN_SECONDS = 1.6;
        /** 最大等级。法环辉石咒固定 1 级。 */
        public static int SPELL_MAX_LEVEL = 1;
        public static float SPELL_DAMAGE_PER_SPELL_POWER = 1.15f;
        public static double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.9;

    /** 注册 ID：{@code elden_ring_spells:comet}。语言键 / 图标 path 也是 {@code comet}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "comet");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(CometSpell.SPELL_MAX_LEVEL)
            .setCooldownSeconds(CometSpell.SPELL_COOLDOWN_SECONDS)
            .build();

    public CometSpell() {
        this.baseManaCost = CometSpell.SPELL_BASE_MANA_COST;
        this.manaCostPerLevel = CometSpell.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = CometSpell.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = CometSpell.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = CometSpell.SPELL_CAST_TIME_TICKS;
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
                        Utils.stringTruncation(CometSpell.EXPLOSION_RADIUS_BLOCKS, 1)
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
                    CometSpell.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                    CometSpell.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS,
                    CometSpell.CAST_BURST_PARTICLE_INTENSITY,
                    getDamageAmount(spellLevel, castingEntity),
                    castingEntity.getLookAngle(),
                    true
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDamageAmount(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity) * CometSpell.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

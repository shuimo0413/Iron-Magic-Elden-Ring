package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GreatGlintstoneShardProjectile;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import com.eldenring.spells.entity.GlintstoneTrailStyle;

/**
 * 辉石大魔砾：瞬时放出的大体积辉石弹。
 * <p>
 * 比魔砾更慢、更肉，命中后在 {@link GreatGlintstoneShardSpell#EXPLOSION_RADIUS_BLOCKS} 内爆炸
 * （范围伤害写在弹道实体里，本类只负责把半径显示到法术书上）。
 * 定位介于魔砾与辉石彗星之间：非稀有、冷却仍短，适合清小群。
 */
public class GreatGlintstoneShardSpell extends EldenRingAbstractSpell {

    // —— 玩法/视觉数字（toml 只覆盖玩法字段）——
    public static float PROJECTILE_FLIGHT_SPEED = 0.95f;
        public static double PROJECTILE_TRACKING_RANGE_BLOCKS = 28.0;
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

        public static float COMET_HEAD_BODY_SCALE_RADIAL = 1.05f;
        public static float COMET_HEAD_BODY_SCALE_ALONG = 0.85f;
        public static float COMET_HEAD_BODY_SCALE = COMET_HEAD_BODY_SCALE_RADIAL;
        public static float COMET_HEAD_GLOW_SCALE = 1.45f;
        public static float COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE = 1.0f;
        public static float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.14f;
        public static float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 14.0f;

        public static float COMET_HEAD_CORE_RED = 0.08f;
        public static float COMET_HEAD_CORE_GREEN = 0.70f;
        public static float COMET_HEAD_CORE_BLUE = 1.0f;

        public static float COMET_HEAD_GLOW_RED = 0.06f;
        public static float COMET_HEAD_GLOW_GREEN = 0.72f;
        public static float COMET_HEAD_GLOW_BLUE = 1.0f;
        public static float COMET_HEAD_GLOW_ALPHA = 1.0f;

        /**
         * 大魔砾曲线光轨：短而粗，强调块状弹头而不是彗尾。
         */
        public static com.eldenring.spells.entity.GlintstoneTrailStyle TRAIL_STYLE =
                new com.eldenring.spells.entity.GlintstoneTrailStyle(12.0, 0.145f, 0.034f, 0.22f, 0.08f, 32);

        /** 拖尾点缀强度倍率；不影响几何光束长宽。 */
        public static float TRAIL_PARTICLE_INTENSITY = 0.55f;
        public static float IMPACT_PARTICLE_INTENSITY = 2.35f;
        public static float CAST_BURST_PARTICLE_INTENSITY = 1.85f;

        public static int SPELL_BASE_MANA_COST = 14;
        public static int SPELL_MANA_COST_PER_LEVEL = 3;
        public static int SPELL_BASE_SPELL_POWER = 14;
        public static int SPELL_SPELL_POWER_PER_LEVEL = 2;
        public static int SPELL_CAST_TIME_TICKS = 0;
        public static double SPELL_COOLDOWN_SECONDS = 0.85;
        /** 最大等级。法环辉石咒固定 1 级。 */
        public static int SPELL_MAX_LEVEL = 1;
        public static float SPELL_DAMAGE_PER_SPELL_POWER = 0.78f;
        public static double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.75;

    /** 注册 ID：{@code elden_ring_spells:great_glintstone_shard}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "great_glintstone_shard");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(GreatGlintstoneShardSpell.SPELL_MAX_LEVEL)
            .setCooldownSeconds(GreatGlintstoneShardSpell.SPELL_COOLDOWN_SECONDS)
            .build();

    public GreatGlintstoneShardSpell() {
        this.baseManaCost = GreatGlintstoneShardSpell.SPELL_BASE_MANA_COST;
        this.manaCostPerLevel = GreatGlintstoneShardSpell.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = GreatGlintstoneShardSpell.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = GreatGlintstoneShardSpell.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = GreatGlintstoneShardSpell.SPELL_CAST_TIME_TICKS;
    }

    /**
     * 法术书显示单发伤害 + 爆炸半径（方块）。半径不随等级变，直接读本类字段。
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
                        Utils.stringTruncation(GreatGlintstoneShardSpell.EXPLOSION_RADIUS_BLOCKS, 1)
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
                    GreatGlintstoneShardSpell.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                    GreatGlintstoneShardSpell.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS,
                    GreatGlintstoneShardSpell.CAST_BURST_PARTICLE_INTENSITY,
                    getDamageAmount(spellLevel, castingEntity),
                    castingEntity.getLookAngle(),
                    true
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDamageAmount(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity)
                * GreatGlintstoneShardSpell.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

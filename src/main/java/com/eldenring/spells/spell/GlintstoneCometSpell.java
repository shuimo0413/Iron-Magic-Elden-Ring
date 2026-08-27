package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GlintstoneCometProjectile;
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
 * 辉石彗星（Glintstone Comet）：强度卡在大魔砾与帚星之间的瞬时彗星弹。
 * <p>
 * 不要和 {@link CometSpell}（帚星 / Comet Azur 那一档的巨型彗星）搞混：
 * 本类是学院常规彗星，稀有度为 RARE，爆炸半径与伤害都小于帚星。
 * 命中爆炸仍由 {@link GlintstoneCometProjectile} 处理。
 */
public class GlintstoneCometSpell extends EldenRingAbstractSpell {

    // —— 玩法/视觉数字（toml 只覆盖玩法字段）——
    public static float PROJECTILE_FLIGHT_SPEED = 1.2f;
        public static double PROJECTILE_TRACKING_RANGE_BLOCKS = 29.0;
        public static float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 2.3f;
        public static int PROJECTILE_TRACKING_START_DELAY_TICKS = 5;
        public static float PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES = 34.0f;
        public static double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.5;
        public static double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;
        public static double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

        /**
         * 命中爆炸半径（方块）：介于大魔砾与帚星之间。
         */
        public static float EXPLOSION_RADIUS_BLOCKS = 2.2f;

        public static float COMET_HEAD_BODY_SCALE_RADIAL = 0.70f;
        public static float COMET_HEAD_BODY_SCALE_ALONG = 1.70f;
        public static float COMET_HEAD_BODY_SCALE = COMET_HEAD_BODY_SCALE_RADIAL;
        public static float COMET_HEAD_GLOW_SCALE = 1.35f;
        public static float COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE = 2.05f;
        public static float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.16f;
        public static float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 13.0f;

        public static float COMET_HEAD_CORE_RED = 0.06f;
        public static float COMET_HEAD_CORE_GREEN = 0.66f;
        public static float COMET_HEAD_CORE_BLUE = 1.0f;

        public static float COMET_HEAD_GLOW_RED = 0.05f;
        public static float COMET_HEAD_GLOW_GREEN = 0.70f;
        public static float COMET_HEAD_GLOW_BLUE = 1.0f;
        public static float COMET_HEAD_GLOW_ALPHA = 1.0f;

        /**
         * 辉石彗星：长尾 + 加法亮芯 + 两条螺旋细丝，开始读成彗星而不是大号魔砾。
         */
        public static com.eldenring.spells.entity.GlintstoneTrailStyle TRAIL_STYLE =
                new com.eldenring.spells.entity.GlintstoneTrailStyle(
                        40.0,
                        0.155f,
                        0.032f,
                        0.16f,
                        0.06f,
                        64,
                        new com.eldenring.spells.entity.GlintstoneTrailStyle.HelixStyle(2, 0.18f, 0.08f, 0.045f, 0.22f, 0.10f),
                        true,
                        false
                );

        /** 拖尾点缀强度倍率；不影响几何光束长宽。 */
        public static float TRAIL_PARTICLE_INTENSITY = 0.50f;
        public static float IMPACT_PARTICLE_INTENSITY = 2.55f;
        public static float CAST_BURST_PARTICLE_INTENSITY = 1.95f;

        public static int SPELL_BASE_MANA_COST = 18;
        public static int SPELL_MANA_COST_PER_LEVEL = 3;
        public static int SPELL_BASE_SPELL_POWER = 16;
        public static int SPELL_SPELL_POWER_PER_LEVEL = 2;
        public static int SPELL_CAST_TIME_TICKS = 0;
        public static double SPELL_COOLDOWN_SECONDS = 1.2;
        /** 最大等级。法环辉石咒固定 1 级。 */
        public static int SPELL_MAX_LEVEL = 1;
        public static float SPELL_DAMAGE_PER_SPELL_POWER = 0.95f;
        public static double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.8;

    /** 注册 ID：{@code elden_ring_spells:glintstone_comet}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "glintstone_comet");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(GlintstoneCometSpell.SPELL_MAX_LEVEL)
            .setCooldownSeconds(GlintstoneCometSpell.SPELL_COOLDOWN_SECONDS)
            .build();

    public GlintstoneCometSpell() {
        this.baseManaCost = GlintstoneCometSpell.SPELL_BASE_MANA_COST;
        this.manaCostPerLevel = GlintstoneCometSpell.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = GlintstoneCometSpell.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = GlintstoneCometSpell.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = GlintstoneCometSpell.SPELL_CAST_TIME_TICKS;
    }

    /** 法术书：单发伤害 + 爆炸半径。 */
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamageAmount(spellLevel, caster), 2)
                ),
                Component.translatable(
                        "ui.irons_spellbooks.radius",
                        Utils.stringTruncation(GlintstoneCometSpell.EXPLOSION_RADIUS_BLOCKS, 1)
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
                    GlintstoneCometProjectile::new,
                    GlintstoneCometSpell.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                    GlintstoneCometSpell.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS,
                    GlintstoneCometSpell.CAST_BURST_PARTICLE_INTENSITY,
                    getDamageAmount(spellLevel, castingEntity),
                    castingEntity.getLookAngle(),
                    true
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDamageAmount(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity) * GlintstoneCometSpell.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

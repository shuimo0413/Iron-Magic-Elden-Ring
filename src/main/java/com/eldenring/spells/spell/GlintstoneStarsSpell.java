package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GlintstoneStarVolleyEntity;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.sigil.AcademySigilFx;
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
import com.eldenring.spells.entity.GlintstoneTrailStyle;

/**
 * 辉石流星：瞬时放出三发依次飞出的强追踪小彗星。
 * <p>
 * 本类<strong>不直接生成弹道</strong>。{@code onCast} 只刷一个
 * {@link GlintstoneStarVolleyEntity}（{@code VolleyKind.GLINTSTONE_STARS}），
 * 由它按 tick 错峰调用 {@link com.eldenring.spells.spell.helper.GlintstoneCastHelper} 出弹。
 * 若三发在同一帧生成，会叠在同一像素上，体感像一发骗伤。
 * <p>
 * 每发一出现就沿视线前冲并立刻强追踪，手感接近法环辉石流星而不是魔砾那种限角轻追。
 * 发数 / 间隔 / 圆阵半径改 {@link GlintstoneStarsSpell}。
 */
public class GlintstoneStarsSpell extends EldenRingAbstractSpell {

    // —— 玩法/视觉数字（toml 只覆盖玩法字段）——
    /** 单次施法流星数量。 */
        public static int PROJECTILE_COUNT = 3;

        /**
         * 相邻两发出现的间隔（tick）。
         * 必须由 {@link com.eldenring.spells.entity.GlintstoneStarVolleyEntity} 按实体 tick 发射；
         * 调大 → 连射更疏、更不像齐射骗伤；调小 → 更接近齐射。
         * 原 3 tick 几乎看不出先后，现拉到约半拍，三发依次飞出。
         */
        public static int PROJECTILE_SPAWN_STAGGER_TICKS = 2;

        /**
         * 生成圆半径（方块）。圆面垂直于视线，顶点按流星数量等分。
         * 调大 → 三角形更大、三发离得更开；调小 → 更挤在杖头附近。
         */
        public static double SPAWN_CIRCLE_RADIUS_BLOCKS = 1.0;

        /**
         * 第一发在圆上的起始极角（度）。0 = 视野右侧，90 = 正上方。
         * 之后按 {@code 360 / 流星数}（整数除法）顺时针步进。
         */
        public static int SPAWN_CIRCLE_START_ANGLE_DEGREES = 90;

        /**
         * 初始飞行方向在视线基础上叠加的世界上扬分量（无量纲，与视线相加后再归一化）。
         * {@code 0} = 完全平行于视线平射；调大 → 出手瞬间往上抛再折向目标。
         */
        public static double PROJECTILE_INITIAL_UPWARD_LIFT = 0.0;

        /** 追踪飞行速度（方块/tick 量级）。 */
        public static float PROJECTILE_FLIGHT_SPEED = 1.15f;

        public static double PROJECTILE_TRACKING_RANGE_BLOCKS = 32.0;

        /** 强追踪：高于魔砾，贴近原作「朝目标飞去」。 */
        public static float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 5.5f;

        /**
         * 出手后直飞、不追踪的 tick 数。
         * 流星要求「出现瞬间即追踪」，故为 0。
         */
        public static int PROJECTILE_TRACKING_START_DELAY_TICKS = 0;

        public static float PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES = 70.0f;
        public static double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.35;
        public static double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;
        public static double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

        public static float COMET_HEAD_BODY_SCALE = 0.26f;
        public static float COMET_HEAD_GLOW_SCALE = 0.55f;
        public static float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.09f;
        public static float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 22.0f;

        public static float COMET_HEAD_CORE_RED = 0.28f;
        public static float COMET_HEAD_CORE_GREEN = 0.88f;
        public static float COMET_HEAD_CORE_BLUE = 1.0f;

        public static float COMET_HEAD_GLOW_RED = 0.24f;
        public static float COMET_HEAD_GLOW_GREEN = 0.90f;
        public static float COMET_HEAD_GLOW_BLUE = 1.0f;
        public static float COMET_HEAD_GLOW_ALPHA = 1.0f;

        /**
         * 辉石流星曲线光轨：最多保留约 20 方块 / 40 点，突出强追踪弧线。
         */
        public static com.eldenring.spells.entity.GlintstoneTrailStyle TRAIL_STYLE =
                new com.eldenring.spells.entity.GlintstoneTrailStyle(20.0, 0.045f, 0.010f, 0.24f, 0.07f, 40);

        /** 拖尾点缀强度倍率；不影响几何光束长宽。 */
        public static float TRAIL_PARTICLE_INTENSITY = 0.70f;
        public static float IMPACT_PARTICLE_INTENSITY = 1.2f;
        public static float CAST_BURST_PARTICLE_INTENSITY = 1.2f;

        public static int SPELL_BASE_MANA_COST = 12;
        public static int SPELL_MANA_COST_PER_LEVEL = 2;
        public static int SPELL_BASE_SPELL_POWER = 9;
        public static int SPELL_SPELL_POWER_PER_LEVEL = 1;
        public static int SPELL_CAST_TIME_TICKS = 0;
        public static double SPELL_COOLDOWN_SECONDS = 1.0;
        /** 最大等级。法环辉石咒固定 1 级。 */
        public static int SPELL_MAX_LEVEL = 1;

        /** 单发伤害系数；总输出约 = 系数 × 法强 × 3。 */
        public static float SPELL_DAMAGE_PER_SPELL_POWER = 0.38f;

        public static double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.65;

    /** 注册 ID：{@code elden_ring_spells:glintstone_stars}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "glintstone_stars");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(GlintstoneStarsSpell.SPELL_MAX_LEVEL)
            .setCooldownSeconds(GlintstoneStarsSpell.SPELL_COOLDOWN_SECONDS)
            .build();

    public GlintstoneStarsSpell() {
        this.manaCostPerLevel = GlintstoneStarsSpell.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = GlintstoneStarsSpell.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = GlintstoneStarsSpell.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = GlintstoneStarsSpell.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = GlintstoneStarsSpell.SPELL_BASE_MANA_COST;
    }

    /**
     * 法术书：单发伤害 + {@code ×3}。总伤约等于单发 × 发数，但目标可能躲开后几发。
     */
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamageAmountPerProjectile(spellLevel, caster), 2)
                ),
                Component.literal("×" + GlintstoneStarsSpell.PROJECTILE_COUNT)
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
     * 原版生物受伤后有约 10 tick 无敌帧（i-frame）。三连发间隔短于这个窗口，
     * 若不把 i-frame 清零，后两发会打在无敌上变成骗伤。
     * 流星雨 / 毁灭流星同样覆盖此方法。
     */
    @Override
    public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setIFrames(0);
    }

    /**
     * 服务端只生成齐射实体并传入「每一发」的伤害。
     * 齐射实体会自己排正三角形阵面、按间隔出弹，Spell 不再循环 {@code spawnAlongLook}。
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
            GlintstoneStarVolleyEntity volleyEntity = new GlintstoneStarVolleyEntity(
                    level,
                    castingEntity,
                    GlintstoneStarVolleyEntity.VolleyKind.GLINTSTONE_STARS,
                    getDamageAmountPerProjectile(spellLevel, castingEntity)
            );
            level.addFreshEntity(volleyEntity);
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    /** 单发伤害。总伤需再乘 {@link GlintstoneStarsSpell#PROJECTILE_COUNT}。 */
    private float getDamageAmountPerProjectile(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity)
                * GlintstoneStarsSpell.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

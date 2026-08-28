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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import com.eldenring.spells.entity.GlintstoneTrailStyle;

/**
 * 流星雨：辉石流星的六发升级版，仍是瞬时施法。
 * <p>
 * 实现与 {@link GlintstoneStarsSpell} 同构：本类只生成
 * {@link GlintstoneStarVolleyEntity}（{@code VolleyKind.STAR_SHOWER}），
 * 由齐射实体按正六边形阵面错峰出弹。发数、间隔、圆半径在 {@link StarShowerSpell}。
 * <p>
 * 单发伤害通常低于辉石流星，靠发数换总伤；同样必须清零 i-frame，否则后几发会被无敌吞掉。
 */
public class StarShowerSpell extends EldenRingAbstractSpell {

    // —— 玩法/视觉数字（toml 只覆盖玩法字段）——
    /** 单次施法流星数量。 */
        public static int PROJECTILE_COUNT = 6;

        /**
         * 相邻两发出现的间隔（tick）。
         * 调大 → 连射更疏、更不容易叠在同一视觉命中上；调小 → 更接近齐射。
         */
        public static int PROJECTILE_SPAWN_STAGGER_TICKS = 2;

        /**
         * 生成圆半径（方块）。圆面垂直于视线，六等分后呈正六边形。
         * 调大 → 阵面更散；调小 → 更挤在杖头附近。
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
        public static float PROJECTILE_FLIGHT_SPEED = 1.12f;

        public static double PROJECTILE_TRACKING_RANGE_BLOCKS = 34.0;

        /** 强追踪：贴近原作「朝目标飞去」。 */
        public static float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 5.2f;

        /**
         * 出手后直飞、不追踪的 tick 数。
         * 流星雨要求「出现瞬间即追踪」，故为 0。
         */
        public static int PROJECTILE_TRACKING_START_DELAY_TICKS = 0;

        public static float PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES = 72.0f;
        public static double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.35;
        public static double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;
        public static double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

        public static float COMET_HEAD_BODY_SCALE = 0.24f;
        public static float COMET_HEAD_GLOW_SCALE = 0.50f;
        public static float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.08f;
        public static float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 24.0f;

        public static float COMET_HEAD_CORE_RED = 0.30f;
        public static float COMET_HEAD_CORE_GREEN = 0.86f;
        public static float COMET_HEAD_CORE_BLUE = 1.0f;

        public static float COMET_HEAD_GLOW_RED = 0.26f;
        public static float COMET_HEAD_GLOW_GREEN = 0.88f;
        public static float COMET_HEAD_GLOW_BLUE = 1.0f;
        public static float COMET_HEAD_GLOW_ALPHA = 1.0f;

        /**
         * 流星雨曲线光轨：略短于辉石流星，突出连发弧线而不是单条长尾。
         */
        public static com.eldenring.spells.entity.GlintstoneTrailStyle TRAIL_STYLE =
                new com.eldenring.spells.entity.GlintstoneTrailStyle(16.0, 0.040f, 0.009f, 0.22f, 0.08f, 36);

        /** 拖尾点缀强度倍率；不影响几何光束长宽。 */
        public static float TRAIL_PARTICLE_INTENSITY = 0.62f;
        public static float IMPACT_PARTICLE_INTENSITY = 1.05f;
        public static float CAST_BURST_PARTICLE_INTENSITY = 1.35f;

        public static int SPELL_BASE_MANA_COST = 22;
        public static int SPELL_MANA_COST_PER_LEVEL = 3;
        public static int SPELL_BASE_SPELL_POWER = 9;
        public static int SPELL_SPELL_POWER_PER_LEVEL = 1;
        public static int SPELL_CAST_TIME_TICKS = 0;
        public static double SPELL_COOLDOWN_SECONDS = 2.5;
        /** 最大等级。法环辉石咒固定 1 级。 */
        public static int SPELL_MAX_LEVEL = 1;

        /** 单发伤害系数；总输出约 = 系数 × 法强 × 6。 */
        public static float SPELL_DAMAGE_PER_SPELL_POWER = 0.30f;

        public static double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.65;

    /** 注册 ID：{@code elden_ring_spells:star_shower}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "star_shower");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(StarShowerSpell.SPELL_MAX_LEVEL)
            .setCooldownSeconds(StarShowerSpell.SPELL_COOLDOWN_SECONDS)
            .build();

    public StarShowerSpell() {
        this.manaCostPerLevel = StarShowerSpell.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = StarShowerSpell.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = StarShowerSpell.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = StarShowerSpell.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = StarShowerSpell.SPELL_BASE_MANA_COST;
    }

    /** 法术书：单发伤害 + {@code ×6}。 */
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamageAmountPerProjectile(spellLevel, caster), 2)
                ),
                Component.literal("×" + StarShowerSpell.PROJECTILE_COUNT)
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
     * 六连发间隔短于原版受伤无敌帧，必须把 i-frame 清零，否则后几发会被吞成骗伤。
     */
    @Override
    public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setIFrames(0);
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
            GlintstoneStarVolleyEntity volleyEntity = new GlintstoneStarVolleyEntity(
                    level,
                    castingEntity,
                    GlintstoneStarVolleyEntity.VolleyKind.STAR_SHOWER,
                    getDamageAmountPerProjectile(spellLevel, castingEntity)
            );
            level.addFreshEntity(volleyEntity);
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDamageAmountPerProjectile(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity)
                * StarShowerSpell.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

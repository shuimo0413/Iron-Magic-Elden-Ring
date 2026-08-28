package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.SwiftGlintstoneShardProjectile;
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
 * 辉石迅魔砾：魔砾的「走位连射」变体。
 * <p>
 * 同样是瞬时单发 + 限角追踪，但弹更快、蓝更便宜、单发伤害更低。
 * 结构与 {@link GlintstonePebbleSpell} 几乎相同，只换弹道类和 {@link SwiftGlintstoneShardSpell}。
 * 爆发粒子强度写死在本类，比魔砾略淡，避免连射时屏幕被闪光糊满。
 */
public class SwiftGlintstoneShardSpell extends EldenRingAbstractSpell {

    // —— 玩法/视觉数字（toml 只覆盖玩法字段）——
    // -------------------------------------------------------------------------
        // 弹道飞行与追踪
        // -------------------------------------------------------------------------

        /** 飞行速度（方块/tick）；高于魔砾，更难侧移躲开。 */
        public static float PROJECTILE_FLIGHT_SPEED = 1.65f;

        /** 索敌半径（方块）；略短于魔砾。 */
        public static double PROJECTILE_TRACKING_RANGE_BLOCKS = 18.0;

        /** 每 tick 最大转向（度）；弱追踪，强调「快打快收」。 */
        public static float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 1.8f;

        /** 出手直飞 tick 数。 */
        public static int PROJECTILE_TRACKING_START_DELAY_TICKS = 3;

        /** 索敌锥半角（度）。 */
        public static float PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES = 28.0f;

        /** 生成点沿视线前移（方块）。 */
        public static double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.35;

        public static double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;
        public static double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

        // -------------------------------------------------------------------------
        // 彗星头视觉（更细、更亮的针状感）
        // -------------------------------------------------------------------------

        /** 径向缩小，读成细针而不是小圆石。 */
        public static float COMET_HEAD_BODY_SCALE_RADIAL = 0.22f;

        /** 沿飞行轴拉长。调大 → 更像示踪弹。 */
        public static float COMET_HEAD_BODY_SCALE_ALONG = 0.70f;

        public static float COMET_HEAD_BODY_SCALE = COMET_HEAD_BODY_SCALE_RADIAL;
        public static float COMET_HEAD_GLOW_SCALE = 0.42f;
        public static float COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE = 1.85f;
        public static float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.06f;
        public static float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 26.0f;

        public static float COMET_HEAD_CORE_RED = 0.20f;
        public static float COMET_HEAD_CORE_GREEN = 0.92f;
        public static float COMET_HEAD_CORE_BLUE = 1.0f;

        public static float COMET_HEAD_GLOW_RED = 0.18f;
        public static float COMET_HEAD_GLOW_GREEN = 0.95f;
        public static float COMET_HEAD_GLOW_BLUE = 1.0f;
        public static float COMET_HEAD_GLOW_ALPHA = 1.0f;

        /**
         * 迅魔砾曲线光轨：高速细亮示踪线，无螺旋细丝。
         */
        public static com.eldenring.spells.entity.GlintstoneTrailStyle TRAIL_STYLE =
                new com.eldenring.spells.entity.GlintstoneTrailStyle(14.0, 0.028f, 0.006f, 0.12f, 0.04f, 36);

        /** 拖尾点缀强度倍率；不影响几何光束长宽。 */
        public static float TRAIL_PARTICLE_INTENSITY = 0.35f;
        public static float IMPACT_PARTICLE_INTENSITY = 1.05f;
        public static float CAST_BURST_PARTICLE_INTENSITY = 0.8f;

        // -------------------------------------------------------------------------
        // 法术数值
        // -------------------------------------------------------------------------

        public static int SPELL_BASE_MANA_COST = 6;
        public static int SPELL_MANA_COST_PER_LEVEL = 1;
        public static int SPELL_BASE_SPELL_POWER = 8;
        public static int SPELL_SPELL_POWER_PER_LEVEL = 1;
        public static int SPELL_CAST_TIME_TICKS = 0;
        public static double SPELL_COOLDOWN_SECONDS = 0.25;
        /** 最大等级。法环辉石咒固定 1 级。 */
        public static int SPELL_MAX_LEVEL = 1;
        public static float SPELL_DAMAGE_PER_SPELL_POWER = 0.42f;
        public static double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.55;

    /** 注册 ID：{@code elden_ring_spells:swift_glintstone_shard}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "swift_glintstone_shard");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SwiftGlintstoneShardSpell.SPELL_MAX_LEVEL)
            .setCooldownSeconds(SwiftGlintstoneShardSpell.SPELL_COOLDOWN_SECONDS)
            .build();

    public SwiftGlintstoneShardSpell() {
        this.manaCostPerLevel = SwiftGlintstoneShardSpell.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = SwiftGlintstoneShardSpell.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = SwiftGlintstoneShardSpell.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = SwiftGlintstoneShardSpell.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = SwiftGlintstoneShardSpell.SPELL_BASE_MANA_COST;
    }

    /** 法术书额外行：估算单发伤害。 */
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

    /**
     * 服务端生成迅魔砾弹道。{@code CAST_BURST_PARTICLE_INTENSITY} 小于 1，连射时闪光更克制。
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
                    SwiftGlintstoneShardProjectile::new,
                    SwiftGlintstoneShardSpell.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                    SwiftGlintstoneShardSpell.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS,
                    SwiftGlintstoneShardSpell.CAST_BURST_PARTICLE_INTENSITY,
                    getDamageAmount(spellLevel, castingEntity),
                    castingEntity.getLookAngle(),
                    true
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    /** 命中伤害 = 法术强度 × {@link SwiftGlintstoneShardSpell#SPELL_DAMAGE_PER_SPELL_POWER}。 */
    private float getDamageAmount(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity)
                * SwiftGlintstoneShardSpell.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GlintstonePebbleProjectile;
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
 * 辉石魔砾（Glintstone Pebble）——本模组最基础的辉石咒，也是读其它法术类的样板。
 * <p>
 * 对应法环「可移动、可连发」的短吟唱弹道：{@link CastType#INSTANT}，吟唱 tick 为 0。
 * 效果：从眼睛沿视线射出一发 {@link GlintstonePebbleProjectile}，带<strong>限角</strong>追踪
 * （不是强锁：侧移仍能躲开）。
 * <p>
 * 蓝耗、冷却、伤害系数、弹速、转向角全部在 {@link GlintstonePebbleSpell}。
 * 出手生成走 {@link com.eldenring.spells.spell.helper.GlintstoneCastHelper}，不要在本类里直接 {@code addFreshEntity}。
 * <p>
 * 铁魔法 AbstractSpell 的字段 / 回调约定见本包 {@code package-info.java}。
 */
public class GlintstonePebbleSpell extends EldenRingAbstractSpell {

    // —— 玩法/视觉数字（toml 只覆盖玩法字段）——
    // -------------------------------------------------------------------------
        // 弹道飞行与追踪（GlintstonePebbleProjectile）
        // -------------------------------------------------------------------------

        /**
         * 弹道飞行速度（方块/tick 量级，传给 {@code AbstractMagicProjectile#getSpeed()}）。
         * 越大飞得越快、越难躲开，但限角追踪也更容易「跟不上」急转目标。
         */
        public static float PROJECTILE_FLIGHT_SPEED = 0.7f;

        /**
         * 追踪索敌半径（方块）。超出此距离的生物不会被当作追踪目标。
         */
        public static double PROJECTILE_TRACKING_RANGE_BLOCKS = 25.0;

        /**
         * 每 tick 允许的最大转向角度（度）。
         * 越小越像法环「轻微追踪」、越容易因侧移而打空；越大越接近强锁。
         */
        public static float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 3.0f;

        /**
         * 射出后先沿视线直飞的 tick 数；其间不做任何追踪转向。
         * 避免刚出手就被身旁目标拽歪，保证「正前方射出」。
         */
        public static int PROJECTILE_TRACKING_START_DELAY_TICKS = 5;

        /**
         * 索敌锥半角（度）：目标须落在「当前飞行方向」此锥内才会被追踪。
         * 侧面 / 身后的怪不会把弹道一出手就拧歪。
         */
        public static float PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES = 40.0f;

        /**
         * 生成点相对眼睛、沿视线再前移的距离（方块），减少出生在碰撞箱内导致的异常。
         */
        public static double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.35;

        /**
         * 当前速度过小（近似静止）时跳过本 tick 转向，避免除零 / 方向抖动。
         */
        public static double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;

        /**
         * 当前朝向与目标朝向夹角极小时，直接对齐目标方向（弧度阈值，避免 acos 噪声）。
         */
        public static double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

        // -------------------------------------------------------------------------
        // 彗星头视觉（GlintstonePebbleRenderer / GlintstoneCometHeadDrawer）
        // -------------------------------------------------------------------------

        /** 垂直飞行方向的晶核缩放。魔砾保持接近球形的短菱形。 */
        public static float COMET_HEAD_BODY_SCALE_RADIAL = 0.42f;

        /** 沿飞行轴的晶核缩放。与径向相同 = 不拉成针。 */
        public static float COMET_HEAD_BODY_SCALE_ALONG = 0.42f;

        /** 兼容旧名：均匀缩放时等于径向。 */
        public static float COMET_HEAD_BODY_SCALE = COMET_HEAD_BODY_SCALE_RADIAL;

        /** 相机朝向光晕基础缩放。调大 → 本体周围辉光更大。 */
        public static float COMET_HEAD_GLOW_SCALE = 0.78f;

        /** 光晕沿飞行方向的拉伸倍率。1 = 球形。 */
        public static float COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE = 1.0f;

        /** 光晕呼吸振幅（叠加在 GLOW_SCALE 上）。 */
        public static float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.10f;

        /** 光晕绕视线旋转角速度（度 / tick）。 */
        public static float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 18.0f;

        /** 晶核着色 RGB（0–1），辉石蓝绿色（偏青而非纯蓝）。 */
        public static float COMET_HEAD_CORE_RED = 0.12f;
        public static float COMET_HEAD_CORE_GREEN = 0.78f;
        public static float COMET_HEAD_CORE_BLUE = 1.0f;

        /** 光晕着色 RGBA（0–1）。alpha 越高本体越亮。 */
        public static float COMET_HEAD_GLOW_RED = 0.10f;
        public static float COMET_HEAD_GLOW_GREEN = 0.82f;
        public static float COMET_HEAD_GLOW_BLUE = 1.0f;
        public static float COMET_HEAD_GLOW_ALPHA = 1.0f;

        // -------------------------------------------------------------------------
        // 法术数值（GlintstonePebbleSpell — 铁魔法 DefaultConfig / AbstractSpell 字段）
        // -------------------------------------------------------------------------

        /** 1 级基础法力消耗。 */
        public static int SPELL_BASE_MANA_COST = 8;

        /** 每升一级额外法力消耗。 */
        public static int SPELL_MANA_COST_PER_LEVEL = 2;

        /** 1 级基础法术强度（参与伤害公式）。 */
        public static int SPELL_BASE_SPELL_POWER = 10;

        /** 每升一级额外法术强度。 */
        public static int SPELL_SPELL_POWER_PER_LEVEL = 1;

        /** 吟唱时间（tick）。0 = 瞬时施法，贴近法环可移动连发。 */
        public static int SPELL_CAST_TIME_TICKS = 0;

        /** 冷却时间（秒）。 */
        public static double SPELL_COOLDOWN_SECONDS = 0.5;

        /** 最大等级。法环辉石咒固定 1 级。 */
        public static int SPELL_MAX_LEVEL = 1;


        /**
         * 基础魔砾曲线光轨：短细、无螺旋细丝。
         */
        public static com.eldenring.spells.entity.GlintstoneTrailStyle TRAIL_STYLE =
                new com.eldenring.spells.entity.GlintstoneTrailStyle(7.0, 0.050f, 0.010f, 0.18f, 0.05f, 22);

        /** 拖尾点缀强度倍率；不影响几何光束长宽。 */
        public static float TRAIL_PARTICLE_INTENSITY = 0.45f;

        /**
         * 命中爆裂粒子强度（相对魔砾基准）。调大 → 能量场/烟雾更浓。
         */
        public static float IMPACT_PARTICLE_INTENSITY = 1.25f;

        /**
         * 最终伤害 = {@code getSpellPower(level, caster) * SPELL_DAMAGE_PER_SPELL_POWER}。
         */
        public static float SPELL_DAMAGE_PER_SPELL_POWER = 0.55f;

        /**
         * 施法瞬间粒子爆发相对眼睛位置、沿视线方向的前移距离（方块）。
         */
        public static double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.6;

    /**
     * 法术注册 ID：{@code elden_ring_spells:glintstone_pebble}。
     * path 必须同时对上语言键 {@code spell.elden_ring_spells.glintstone_pebble}
     * 和图标 {@code textures/gui/spell_icons/glintstone_pebble.png}。
     */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "glintstone_pebble");

    /**
     * 铁魔法默认配置（可被服务端 irons 配置文件覆盖）。
     * 稀有度 / 学派 / 最高等级 / 冷却秒数都从这里进游戏。
     */
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(GlintstonePebbleSpell.SPELL_MAX_LEVEL)
            .setCooldownSeconds(GlintstonePebbleSpell.SPELL_COOLDOWN_SECONDS)
            .build();

    /**
     * 构造时写入 AbstractSpell 的平衡字段。铁魔法用它们算蓝耗和 {@link #getSpellPower}。
     * <ul>
     *   <li>{@code baseManaCost}：1 级蓝耗</li>
     *   <li>{@code manaCostPerLevel}：每升 1 级额外蓝耗</li>
     *   <li>{@code baseSpellPower} / {@code spellPowerPerLevel}：法术强度，再乘伤害系数才是实际伤害</li>
     *   <li>{@code castTime}：蓄力 tick；瞬时法术为 0</li>
     * </ul>
     */
    public GlintstonePebbleSpell() {
        this.manaCostPerLevel = GlintstonePebbleSpell.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = GlintstonePebbleSpell.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = GlintstonePebbleSpell.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = GlintstonePebbleSpell.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = GlintstonePebbleSpell.SPELL_BASE_MANA_COST;
    }

    /**
     * 法术书 / HUD 额外行。这里只显示估算伤害；铁魔法会自己拼蓝耗、冷却。
     */
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

    /**
     * {@link CastType#INSTANT}：按下施法即完成，不进入蓄力条。
     * 需要蓄力的法术（毁灭流星、魔法之境）改用 {@link CastType#LONG}。
     */
    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellResourceLocation;
    }

    /**
     * 出手完成音。辉石系统一用水晶铃，和学院法阵视觉配套。
     */
    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    /**
     * 施法完成回调（双端都会进）。
     * 服务端：头顶学院法阵 + 沿视线生成魔砾弹道。
     * 末尾 {@code super.onCast} 不能省，铁魔法靠它收尾音效和内部状态。
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
                    GlintstonePebbleProjectile::new,
                    GlintstonePebbleSpell.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                    GlintstonePebbleSpell.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS,
                    1.0f,
                    getDamageAmount(spellLevel, castingEntity),
                    castingEntity.getLookAngle(),
                    true
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    /**
     * 实际命中伤害 = 铁魔法法术强度 × 伤害系数。
     * {@link #getSpellPower} 已含等级、装备、魔法之境等全局加成。
     */
    private float getDamageAmount(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity) * GlintstonePebbleSpell.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

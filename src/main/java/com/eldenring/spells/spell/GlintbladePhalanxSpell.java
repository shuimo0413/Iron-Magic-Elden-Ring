package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GlintstoneTrailStyle;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.sigil.AcademySigilFx;
import com.eldenring.spells.spell.curve.GlintbladePhalanxCastCurve;
import com.eldenring.spells.spell.helper.GlintbladePhalanxHelper;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * 辉剑圆阵（Glintblade Phalanx）：瞬时在头上铺五把跟手辉剑，附近有敌人就自动射出。
 * <p>
 * 半圆生成走 {@link GlintbladePhalanxHelper}（卡利亚圆阵会复用）。
 * 剑模型复用魔法辉剑；跟手 / 12 格触发在 {@code PhalanxGlintbladeEntity}。
 */
public class GlintbladePhalanxSpell extends EldenRingAbstractSpell {

    // —— 法术书 / 蓝耗 / 冷却 ——

    /** 1 级蓝耗。五把剑比单发魔法辉剑贵一截。 */
    public static int SPELL_BASE_MANA_COST = 22;

    /** 每升 1 级额外蓝耗。 */
    public static int SPELL_MANA_COST_PER_LEVEL = 4;

    /** 1 级法术强度基数。 */
    public static int SPELL_BASE_SPELL_POWER = 12;

    /** 每级额外法术强度。 */
    public static int SPELL_SPELL_POWER_PER_LEVEL = 2;

    /** 吟唱 tick。0 = 瞬时铺阵。 */
    public static int SPELL_CAST_TIME_TICKS = 0;

    /**
     * 冷却（秒）。圆阵能挂一段时间，CD 要比单发辉剑长，避免无脑叠两圈。
     */
    public static double SPELL_COOLDOWN_SECONDS = 4.0;

    /** 最大等级。 */
    public static int SPELL_MAX_LEVEL = 1;

    /**
     * 单剑命中伤害 = 法术强度 × 本系数。
     * 调大 → 每把更痛（五把打满会很猛）；调小 → 更像骚扰阵。
     */
    public static float DAMAGE_PER_SPELL_POWER = 0.72f;

    /**
     * 半圆上的辉剑数量。辉剑圆阵固定 5；卡利亚圆阵会走 helper 传更大的数。
     * 调大 → 更密、总伤更高。
     */
    public static int BLADE_COUNT = 5;

    /**
     * 以玩家为圆心、多少格内出现可打生物就自动射出（方块）。用户指定 12。
     * 调大 → 更早出手；调小 → 必须贴身才射。
     */
    public static double AUTO_LAUNCH_RANGE_BLOCKS = 12.0;

    /**
     * 一直没有敌人时跟手多久后自行消失（tick）。20 tick = 1 秒。
     * 调大 → 阵留得更久；调小 → 更像短促护体。
     */
    public static int HOVER_LIFETIME_TICKS = 200;

    // —— 飞行 / 追踪（射出之后，与魔法辉剑同一套手感）——

    /**
     * 射出后飞行速度（方块/tick）。
     * 调大 → 更难躲开；调小 → 更能看见剑飞过去。
     */
    public static float PROJECTILE_FLIGHT_SPEED = 0.82f;

    /**
     * 射出后追踪索敌半径（方块）。触发距离是 {@link #AUTO_LAUNCH_RANGE_BLOCKS}，这条是飞出去还能追多远。
     */
    public static double PROJECTILE_TRACKING_RANGE_BLOCKS = 28.0;

    /**
     * 每 tick 允许的最大转向角度（度）。
     */
    public static float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 5.5f;

    /** 飞行光轨：与魔法辉剑同一套「剑划过」而不是彗星尾。 */
    public static GlintstoneTrailStyle TRAIL_STYLE = MagicGlintbladeSpell.TRAIL_STYLE;

    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "glintblade_phalanx");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(GlintbladePhalanxSpell.SPELL_MAX_LEVEL)
            .setCooldownSeconds(GlintbladePhalanxSpell.SPELL_COOLDOWN_SECONDS)
            .build();

    public GlintbladePhalanxSpell() {
        this.manaCostPerLevel = GlintbladePhalanxSpell.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = GlintbladePhalanxSpell.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = GlintbladePhalanxSpell.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = GlintbladePhalanxSpell.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = GlintbladePhalanxSpell.SPELL_BASE_MANA_COST;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamageAmount(spellLevel, caster), 2)
                ),
                Component.literal("×" + GlintbladePhalanxSpell.BLADE_COUNT),
                Component.translatable(
                        "ui.elden_ring_spells.projectile_range",
                        Utils.stringTruncation(GlintbladePhalanxSpell.AUTO_LAUNCH_RANGE_BLOCKS, 1)
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
     * 剑还没飞，不要在施法瞬间抢射出音。起手音在 helper 里播一次。
     */
    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public Vector3f getTargetingColor() {
        return new Vector3f(0.18f, 0.38f, 0.95f);
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
            GlintbladePhalanxHelper.spawnHeadSemicircle(
                    level,
                    castingEntity,
                    this,
                    GlintbladePhalanxSpell.BLADE_COUNT,
                    getDamageAmount(spellLevel, castingEntity),
                    GlintbladePhalanxSpell.AUTO_LAUNCH_RANGE_BLOCKS,
                    GlintbladePhalanxSpell.HOVER_LIFETIME_TICKS,
                    GlintbladePhalanxCastCurve.ORBIT_RADIUS_BLOCKS
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDamageAmount(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * GlintbladePhalanxSpell.DAMAGE_PER_SPELL_POWER;
    }
}

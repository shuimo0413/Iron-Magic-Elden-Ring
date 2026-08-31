package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
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
 * 卡利亚圆阵（Carian Phalanx）：头上九把跟手辉剑，附近有敌人就自动射出。
 * <p>
 * 几何 / 互斥 / 清旧剑全部走 {@link GlintbladePhalanxHelper}，与辉剑圆阵、巨剑阵同一圈槽位。
 * 再放任意一种圆阵都会顶掉头上还没射出的剑，连放也不会叠到 18 把。
 */
public class CarianPhalanxSpell extends EldenRingAbstractSpell {

    /** 1 级蓝耗。九把剑比辉剑圆阵更贵。 */
    public static int SPELL_BASE_MANA_COST = 28;

    /** 每升 1 级额外蓝耗。 */
    public static int SPELL_MANA_COST_PER_LEVEL = 5;

    /** 1 级法术强度基数。 */
    public static int SPELL_BASE_SPELL_POWER = 12;

    /** 每级额外法术强度。 */
    public static int SPELL_SPELL_POWER_PER_LEVEL = 2;

    /** 吟唱 tick。0 = 瞬时铺阵。 */
    public static int SPELL_CAST_TIME_TICKS = 0;

    /**
     * 冷却（秒）。九把齐射总伤更高，CD 略长于辉剑圆阵。
     */
    public static double SPELL_COOLDOWN_SECONDS = 4.5;

    /** 最大等级。 */
    public static int SPELL_MAX_LEVEL = 1;

    /**
     * 单剑命中伤害 = 法术强度 × 本系数。
     * 单把比辉剑圆阵弱一截，九把打满总伤仍更高。
     */
    public static float DAMAGE_PER_SPELL_POWER = 0.52f;

    /**
     * 半圆上的辉剑数量。卡利亚圆阵固定 9。
     */
    public static int BLADE_COUNT = 9;

    /**
     * 以玩家为圆心、多少格内出现可打生物就自动射出（方块）。
     */
    public static double AUTO_LAUNCH_RANGE_BLOCKS = 12.0;

    /**
     * 一直没有敌人时跟手多久后自行消失（tick）。20 tick = 1 秒。
     */
    public static int HOVER_LIFETIME_TICKS = 200;

    /**
     * 射出后飞行速度（方块/tick）。
     */
    public static float PROJECTILE_FLIGHT_SPEED = 0.82f;

    /**
     * 射出后追踪索敌半径（方块）。
     */
    public static double PROJECTILE_TRACKING_RANGE_BLOCKS = 28.0;

    /**
     * 每 tick 允许的最大转向角度（度）。
     */
    public static float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 5.5f;

    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "carian_phalanx");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(CarianPhalanxSpell.SPELL_MAX_LEVEL)
            .setCooldownSeconds(CarianPhalanxSpell.SPELL_COOLDOWN_SECONDS)
            .build();

    public CarianPhalanxSpell() {
        this.manaCostPerLevel = CarianPhalanxSpell.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = CarianPhalanxSpell.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = CarianPhalanxSpell.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = CarianPhalanxSpell.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = CarianPhalanxSpell.SPELL_BASE_MANA_COST;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamageAmount(spellLevel, caster), 2)
                ),
                Component.literal("×" + CarianPhalanxSpell.BLADE_COUNT),
                Component.translatable(
                        "ui.elden_ring_spells.projectile_range",
                        Utils.stringTruncation(CarianPhalanxSpell.AUTO_LAUNCH_RANGE_BLOCKS, 1)
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
                    new GlintbladePhalanxHelper.SpawnSpec(
                            this,
                            CarianPhalanxSpell.BLADE_COUNT,
                            getDamageAmount(spellLevel, castingEntity),
                            CarianPhalanxSpell.AUTO_LAUNCH_RANGE_BLOCKS,
                            CarianPhalanxSpell.HOVER_LIFETIME_TICKS,
                            GlintbladePhalanxCastCurve.ORBIT_RADIUS_BLOCKS,
                            GlintbladePhalanxCastCurve.SWORD_VISUAL_SCALE,
                            CarianPhalanxSpell.PROJECTILE_FLIGHT_SPEED,
                            CarianPhalanxSpell.PROJECTILE_TRACKING_RANGE_BLOCKS,
                            CarianPhalanxSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK
                    )
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDamageAmount(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * CarianPhalanxSpell.DAMAGE_PER_SPELL_POWER;
    }
}

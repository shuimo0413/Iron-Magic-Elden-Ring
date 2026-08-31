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
 * 巨剑阵（Greatblade Phalanx）：头上三把放大辉剑，附近有敌人就自动射出。
 * <p>
 * 模型就是辉剑圆阵那把，原地乘 {@link GlintbladePhalanxCastCurve#GREATBLADE_SWORD_VISUAL_SCALE}。
 * 与辉剑圆阵 / 卡利亚圆阵互斥，helper 会先清掉头上还没射出的旧剑。
 */
public class GreatbladePhalanxSpell extends EldenRingAbstractSpell {

    /** 1 级蓝耗。三把大剑比九把小剑更贵。 */
    public static int SPELL_BASE_MANA_COST = 36;

    /** 每升 1 级额外蓝耗。 */
    public static int SPELL_MANA_COST_PER_LEVEL = 6;

    /** 1 级法术强度基数。 */
    public static int SPELL_BASE_SPELL_POWER = 16;

    /** 每级额外法术强度。 */
    public static int SPELL_SPELL_POWER_PER_LEVEL = 3;

    /** 吟唱 tick。0 = 瞬时铺阵。 */
    public static int SPELL_CAST_TIME_TICKS = 0;

    /**
     * 冷却（秒）。单下更重，CD 比另外两圈圆阵更长。
     */
    public static double SPELL_COOLDOWN_SECONDS = 6.0;

    /** 最大等级。 */
    public static int SPELL_MAX_LEVEL = 1;

    /**
     * 单剑命中伤害 = 法术强度 × 本系数。
     * 三把打满总伤接近卡利亚圆阵，但每一击更肉。
     */
    public static float DAMAGE_PER_SPELL_POWER = 1.55f;

    /**
     * 半圆上的大剑数量。巨剑阵固定 3。
     */
    public static int BLADE_COUNT = 3;

    /**
     * 以玩家为圆心、多少格内出现可打生物就自动射出（方块）。
     */
    public static double AUTO_LAUNCH_RANGE_BLOCKS = 12.0;

    /**
     * 一直没有敌人时跟手多久后自行消失（tick）。20 tick = 1 秒。
     */
    public static int HOVER_LIFETIME_TICKS = 200;

    /**
     * 射出后飞行速度（方块/tick）。大剑略慢于辉剑，更容易看见飞过去。
     */
    public static float PROJECTILE_FLIGHT_SPEED = 0.70f;

    /**
     * 射出后追踪索敌半径（方块）。
     */
    public static double PROJECTILE_TRACKING_RANGE_BLOCKS = 28.0;

    /**
     * 每 tick 允许的最大转向角度（度）。大剑转向比小辉剑钝一点。
     */
    public static float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 4.5f;

    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "greatblade_phalanx");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(GreatbladePhalanxSpell.SPELL_MAX_LEVEL)
            .setCooldownSeconds(GreatbladePhalanxSpell.SPELL_COOLDOWN_SECONDS)
            .build();

    public GreatbladePhalanxSpell() {
        this.manaCostPerLevel = GreatbladePhalanxSpell.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = GreatbladePhalanxSpell.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = GreatbladePhalanxSpell.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = GreatbladePhalanxSpell.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = GreatbladePhalanxSpell.SPELL_BASE_MANA_COST;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamageAmount(spellLevel, caster), 2)
                ),
                Component.literal("×" + GreatbladePhalanxSpell.BLADE_COUNT),
                Component.translatable(
                        "ui.elden_ring_spells.projectile_range",
                        Utils.stringTruncation(GreatbladePhalanxSpell.AUTO_LAUNCH_RANGE_BLOCKS, 1)
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
                            GreatbladePhalanxSpell.BLADE_COUNT,
                            getDamageAmount(spellLevel, castingEntity),
                            GreatbladePhalanxSpell.AUTO_LAUNCH_RANGE_BLOCKS,
                            GreatbladePhalanxSpell.HOVER_LIFETIME_TICKS,
                            GlintbladePhalanxCastCurve.ORBIT_RADIUS_BLOCKS,
                            GlintbladePhalanxCastCurve.GREATBLADE_SWORD_VISUAL_SCALE,
                            GreatbladePhalanxSpell.PROJECTILE_FLIGHT_SPEED,
                            GreatbladePhalanxSpell.PROJECTILE_TRACKING_RANGE_BLOCKS,
                            GreatbladePhalanxSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK
                    )
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDamageAmount(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * GreatbladePhalanxSpell.DAMAGE_PER_SPELL_POWER;
    }
}

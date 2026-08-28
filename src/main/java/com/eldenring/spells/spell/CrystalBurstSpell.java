package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GlintstoneTrailStyle;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.sigil.AcademySigilFx;
import com.eldenring.spells.spell.helper.CrystalBurstCasting;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
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

/**
 * 结晶散射（Crystal Burst）。
 * <p>
 * {@link CastType#INSTANT}：一次出手同时打出一捧不追踪的辉石碎片，沿面前锥面散射。
 * 可边走边放（不像结晶连弹那样钉死站位）。碎片抄迅魔砾彗星头 / 光轨，射程短，
 * 撞敌或飞满射程会碎裂消失。
 */
public class CrystalBurstSpell extends EldenRingAbstractSpell {

    /**
     * 最大等级种子。运行时以铁魔法 JSON 为准。法环辉石咒固定 1 级。
     */
    public static final int SPELL_MAX_LEVEL = 1;

    /**
     * 冷却（秒）。齐射已经一次打出很多片，松手后再按要有一点空窗。
     * 调大 → 更难连放；调小 → 更接近走位点射。
     */
    public static final double SPELL_COOLDOWN_SECONDS = 1.2;

    /**
     * 1 级基础法力消耗。瞬时咒只在 {@link #onCast} 扣一次。
     * 调大 → 更吃蓝。
     */
    public static int SPELL_BASE_MANA_COST = 14;

    /** 每升一级额外法力消耗。当前定死 1 级。 */
    public static int SPELL_MANA_COST_PER_LEVEL = 2;

    /**
     * 1 级基础法术强度。单片伤害 = {@link #getSpellPower} × {@link #SPELL_DAMAGE_PER_SPELL_POWER}。
     */
    public static int SPELL_BASE_SPELL_POWER = 8;

    /** 每升一级额外法术强度。当前定死 1 级。 */
    public static int SPELL_SPELL_POWER_PER_LEVEL = 1;

    /**
     * 吟唱时长（tick）。0 = 瞬时，按下去就齐射，不锁站位。
     */
    public static int SPELL_CAST_TIME_TICKS = 0;

    /**
     * 单片伤害系数：最终伤害 = 法术强度 × 本值。
     * 齐射很密，默认比迅魔砾单发低；近距离吃满片数才会明显高于单发。
     * 调大 → 单片更疼，总伤一起涨。
     */
    public static float SPELL_DAMAGE_PER_SPELL_POWER = 0.20f;

    /**
     * 一次齐射的碎片数量。调大 → 扇面更密、更吃实体；调小 → 更像几发窄束。
     */
    public static int PROJECTILE_COUNT = 10;

    /**
     * 碎片飞行速度（方块/tick）。比迅魔砾略慢，短射程里还能看清扇面打开。
     * 调大 → 更难侧移躲开，扇面也更快铺开。
     */
    public static float PROJECTILE_FLIGHT_SPEED = 1.25f;

    /**
     * 直线最大射程（方块）。超过就碎裂消失。调大 → 能打到更远；调小 → 必须贴身扫。
     */
    public static double PROJECTILE_MAX_RANGE_BLOCKS = 10.0;

    /**
     * 相对视线的散射锥半角（度）。比结晶连弹（14°）更开，左右合计约 52°。
     * 调大 → 更散、近距离覆盖面更大；调小 → 更像一条窄束。
     */
    public static float SCATTER_HALF_ANGLE_DEGREES = 26.0f;

    /**
     * 生成点沿视线前移（方块）。太小容易嵌进玩家自己，太大容易穿墙。
     */
    public static double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.35;

    /**
     * 出手闪光相对生成点再沿视线前移（方块）。整次齐射只闪一次。
     */
    public static double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.45;

    public static double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;
    public static double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

    // -------------------------------------------------------------------------
    // 彗星头视觉（从迅魔砾抄过来：细亮针状）
    // -------------------------------------------------------------------------

    /** 径向缩小，读成细针而不是小圆石。 */
    public static float COMET_HEAD_BODY_SCALE_RADIAL = 0.22f;

    /** 沿飞行轴拉长。调大 → 更像示踪弹。 */
    public static float COMET_HEAD_BODY_SCALE_ALONG = 0.70f;

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
     * 迅魔砾同款高速细亮示踪线，无螺旋细丝。
     */
    public static GlintstoneTrailStyle TRAIL_STYLE =
            new GlintstoneTrailStyle(14.0, 0.028f, 0.006f, 0.12f, 0.04f, 36);

    /** 拖尾点缀强度倍率；不影响几何光束长宽。齐射时要克制。 */
    public static float TRAIL_PARTICLE_INTENSITY = 0.28f;
    public static float IMPACT_PARTICLE_INTENSITY = 1.05f;
    public static float CAST_BURST_PARTICLE_INTENSITY = 0.55f;

    /** 注册 ID：{@code elden_ring_spells:crystal_burst}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "crystal_burst");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SPELL_MAX_LEVEL)
            .setCooldownSeconds(SPELL_COOLDOWN_SECONDS)
            .build();

    public CrystalBurstSpell() {
        this.manaCostPerLevel = SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = SPELL_CAST_TIME_TICKS;
        this.baseManaCost = SPELL_BASE_MANA_COST;
    }

    /**
     * 伤害源把原版受伤无敌帧打成 0 tick。
     * 不打的话，同帧后几片会落在第一片的 i-frame 里，实际伤害接近 0。
     */
    @Override
    public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setIFrames(0);
    }

    /**
     * 法术书：单片伤害、片数、直线射程、「可移动施法」。
     */
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getShardDamage(spellLevel, caster), 2)
                ),
                Component.literal("×" + PROJECTILE_COUNT),
                Component.translatable(
                        "ui.elden_ring_spells.projectile_range",
                        Utils.stringTruncation(PROJECTILE_MAX_RANGE_BLOCKS, 1)
                ),
                Component.translatable("ui.elden_ring_spells.cast_while_moving")
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
     * 服务端一次刷出整捧散射碎片。不锁站位，也不拒绝走动。
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
            CrystalBurstCasting.spawnScatterVolley(
                    level,
                    castingEntity,
                    getShardDamage(spellLevel, castingEntity)
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    /** 当前等级下单片碎片命中伤害。 */
    public float getShardDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

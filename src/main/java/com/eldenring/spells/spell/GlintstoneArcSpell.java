package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GlintstoneArcProjectile;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.sigil.AcademySigilFx;
import com.eldenring.spells.spell.helper.GlintstoneCastHelper;
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
 * 辉石弯弧（Glintstone Arc）：瞬时沿准星放出一条横向拉开的青色魔力弯刃。
 * <p>
 * 不追踪。飞行中半宽随距离变大，命中实体后穿透续飞，可同时扫到一排敌人；
 * 撞实心方块或飞满射程后消散。视觉是学院青新月，不是彗星头，也不是卡利亚宝蓝斩击。
 */
public class GlintstoneArcSpell extends EldenRingAbstractSpell {

    /**
     * 最大等级种子。运行时以铁魔法 JSON 为准。法环辉石咒固定 1 级。
     */
    public static final int SPELL_MAX_LEVEL = 1;

    /**
     * 冷却（秒）。弯弧清群便宜，松手后再按要有一点空窗。
     * 调大 → 更难连放；调小 → 更接近走位点射。
     */
    public static final double SPELL_COOLDOWN_SECONDS = 0.85;

    /**
     * 1 级基础法力消耗。瞬时咒只在 {@link #onCast} 扣一次。
     * 调大 → 更吃蓝。
     */
    public static int SPELL_BASE_MANA_COST = 10;

    /** 每升一级额外法力消耗。当前定死 1 级。 */
    public static int SPELL_MANA_COST_PER_LEVEL = 2;

    /**
     * 1 级基础法术强度。单次命中伤害 = {@link #getSpellPower} × {@link #SPELL_DAMAGE_PER_SPELL_POWER}。
     */
    public static int SPELL_BASE_SPELL_POWER = 10;

    /** 每升一级额外法术强度。当前定死 1 级。 */
    public static int SPELL_SPELL_POWER_PER_LEVEL = 1;

    /**
     * 吟唱时长（tick）。0 = 瞬时，按下去就出刃，不锁站位。
     */
    public static int SPELL_CAST_TIME_TICKS = 0;

    /**
     * 单次命中伤害系数：最终伤害 = 法术强度 × 本值。
     * 弯弧能穿一排，单发略低于魔砾，吃满穿透才会明显高于单发弹。
     * 调大 → 每人更疼，清群总伤一起涨。
     */
    public static float SPELL_DAMAGE_PER_SPELL_POWER = 0.52f;

    /**
     * 弯弧飞行速度（方块/tick）。比魔砾略快，让横向拉开能看清但不要瞬移。
     * 调大 → 更难侧移躲开；调小 → 刃在面前停得更久。
     */
    public static float PROJECTILE_FLIGHT_SPEED = 0.80f;

    /**
     * 直线最大射程（方块）。超过就碎裂消失。调大 → 能扫到更远；调小 → 必须贴身放。
     */
    public static double PROJECTILE_MAX_RANGE_BLOCKS = 14.0;

    /**
     * 出手时弯弧半宽（方块）。左右合计约 3.2 格。
     * 调大 → 出手就已经很宽；调小 → 更依赖飞行中扩散。
     */
    public static float ARC_START_HALF_WIDTH_BLOCKS = 1.6f;

    /**
     * 飞到最大射程时的弯弧半宽（方块）。左右合计约 8 格。
     * 调大 → 「大幅横向扩散」更夸张；调小 → 更像一条窄刃。
     */
    public static float ARC_MAX_HALF_WIDTH_BLOCKS = 4.0f;

    /**
     * 实体穿透次数：每个敌人只结算一次，次数耗尽或撞墙即消失。
     * 写入铁魔法 {@code pierceLevel = 本值 - 1}。
     * 调大 → 能扫过更多人；调小 → 清一小撮就散。
     */
    public static int PROJECTILE_MAX_ENTITY_HITS = 10;

    /**
     * 生成点沿视线前移（方块）。太小容易嵌进玩家自己，太大容易穿墙。
     */
    public static double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.45;

    /**
     * 出手闪光相对生成点再沿视线前移（方块）。当前总闸关掉了准星前绽光。
     */
    public static double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.50;

    public static double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;
    public static double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

    /**
     * 拖尾点缀强度倍率。弯弧主体是几何新月，粒子只沿刃稀疏点缀。
     */
    public static float TRAIL_PARTICLE_INTENSITY = 0.32f;

    /**
     * 撞墙 / 飞尽时碎裂粒子强度（相对魔砾基准）。
     */
    public static float IMPACT_PARTICLE_INTENSITY = 0.90f;

    public static float CAST_BURST_PARTICLE_INTENSITY = 0.50f;

    /** 注册 ID：{@code elden_ring_spells:glintstone_arc}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "glintstone_arc");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SPELL_MAX_LEVEL)
            .setCooldownSeconds(SPELL_COOLDOWN_SECONDS)
            .build();

    public GlintstoneArcSpell() {
        this.manaCostPerLevel = SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = SPELL_CAST_TIME_TICKS;
        this.baseManaCost = SPELL_BASE_MANA_COST;
    }

    /**
     * 伤害源把原版受伤无敌帧打成 0 tick。
     * 不打的话，同一 tick 扫到的第二、第三个敌人会落在第一下的 i-frame 里。
     */
    @Override
    public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setIFrames(0);
    }

    /**
     * 法术书：单次伤害、穿透次数、射程、最大宽度、「可移动施法」。
     */
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getArcDamage(spellLevel, caster), 2)
                ),
                Component.literal("Pierce ×" + PROJECTILE_MAX_ENTITY_HITS),
                Component.translatable(
                        "ui.elden_ring_spells.projectile_range",
                        Utils.stringTruncation(PROJECTILE_MAX_RANGE_BLOCKS, 1)
                ),
                Component.translatable(
                        "ui.elden_ring_spells.arc_width",
                        Utils.stringTruncation(ARC_MAX_HALF_WIDTH_BLOCKS * 2.0f, 1)
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
     * 服务端刷出一发弯弧实体。不锁站位。
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
                    GlintstoneArcProjectile::new,
                    PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                    SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS,
                    CAST_BURST_PARTICLE_INTENSITY,
                    getArcDamage(spellLevel, castingEntity),
                    castingEntity.getLookAngle(),
                    true
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    /** 当前等级下弯弧命中一次的伤害。 */
    public float getArcDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

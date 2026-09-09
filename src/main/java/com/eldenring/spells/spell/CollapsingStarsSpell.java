package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.spell.helper.CollapsingStarsCasting;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
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

/**
 * 碎星（Collapsing Stars）——向前锥面散射多发紫色重力球。
 * <p>
 * <strong>无伤害</strong>。每发命中实体或方块后，把落点半径内敌人拉向施法者：
 * 距离 ≤ 本级拉取格数 → 直接拉到身前；距离更大 → 只沿连线拉近该格数。
 * 拉取距离随法术等级提升；弹道本体复用重力球实体 / 命中 / 粒子。
 * <p>
 * 玩法数字默认值在本类静态字段，由 {@code EldenRingServerConfig} 的 {@code collapsing_stars} 段覆盖。
 */
public class CollapsingStarsSpell extends EldenRingAbstractSpell {

    /**
     * 最大等级种子。运行时以铁魔法 JSON 为准。
     * 多级是为了让拉取格数随等级变长。
     */
    public static final int SPELL_MAX_LEVEL = 5;

    /**
     * 冷却（秒）。一次齐射多发，要比单发重力球略长一点空窗。
     * 调大 → 更难连放；调小 → 更接近扫射控场。
     */
    public static final double SPELL_COOLDOWN_SECONDS = 4.0;

    /** 1 级蓝耗。齐射控场，高于单发重力球。 */
    public static int SPELL_BASE_MANA_COST = 28;

    /** 每升 1 级额外蓝耗。 */
    public static int SPELL_MANA_COST_PER_LEVEL = 4;

    /** 1 级法术强度基数（本咒不造成伤害，保留给铁魔法面板 / 将来扩展）。 */
    public static int SPELL_BASE_SPELL_POWER = 10;

    /** 每级额外法术强度。 */
    public static int SPELL_SPELL_POWER_PER_LEVEL = 1;

    /** 吟唱 tick。0 = 瞬时。 */
    public static int SPELL_CAST_TIME_TICKS = 0;

    /**
     * 一次齐射的重力球数量。调大 → 扇面更密、更吃实体；调小 → 更像几发窄束。
     */
    public static int PROJECTILE_COUNT = 8;

    /**
     * 弹道飞行速度（方块/tick）。调大 → 更难躲。
     */
    public static float PROJECTILE_FLIGHT_SPEED = 0.90f;

    /**
     * 弹道最大射程（方块）。飞过这段距离后 discard。
     */
    public static double PROJECTILE_MAX_RANGE_BLOCKS = 22.0;

    /**
     * 相对视线的散射锥半角（度）。左右合计约 48°。
     * 调大 → 更散、近距离覆盖面更大；调小 → 更像一条窄束。
     */
    public static float SCATTER_HALF_ANGLE_DEGREES = 24.0f;

    /**
     * 命中搜敌半径（方块）。以落点为球心，范围内敌对生物进入拉取名单。
     * 齐射时略小于单发重力球，避免整片扇面重叠吸成一团太狠。
     */
    public static float HIT_RADIUS_BLOCKS = 2.0f;

    /**
     * 1 级最大拉取距离（方块）。距离 ≤ 此值 → 拉到身前；更大 → 只拉近这么多格。
     */
    public static double SUCTION_PULL_BLOCKS_AT_LEVEL_1 = 3.0;

    /**
     * 每升一级额外拉取格数。调大 → 高等级吸得更远。
     */
    public static double SUCTION_PULL_BLOCKS_PER_LEVEL = 1.0;

    /**
     * 拉到「身前」时，停在施法者中心前多少格（方块），避免嵌进碰撞箱。
     */
    public static double SUCTION_STAND_OFF_BLOCKS = 1.25;

    /**
     * 生成点相对眼睛、沿视线再前移的距离（方块）。
     */
    public static double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.35;

    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "collapsing_stars");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SPELL_MAX_LEVEL)
            .setCooldownSeconds(SPELL_COOLDOWN_SECONDS)
            .build();

    public CollapsingStarsSpell() {
        this.manaCostPerLevel = SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = SPELL_CAST_TIME_TICKS;
        this.baseManaCost = SPELL_BASE_MANA_COST;
    }

    /**
     * 本级最大拉取格数。1 级 = {@link #SUCTION_PULL_BLOCKS_AT_LEVEL_1}。
     */
    public static double suctionPullBlocksForLevel(int spellLevel) {
        int clampedLevel = Math.max(1, spellLevel);
        return SUCTION_PULL_BLOCKS_AT_LEVEL_1
                + (clampedLevel - 1) * SUCTION_PULL_BLOCKS_PER_LEVEL;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.elden_ring_spells.pull_distance",
                        Utils.stringTruncation(suctionPullBlocksForLevel(spellLevel), 1)
                ),
                Component.literal("×" + PROJECTILE_COUNT),
                Component.translatable(
                        "ui.elden_ring_spells.projectile_range",
                        Utils.stringTruncation(PROJECTILE_MAX_RANGE_BLOCKS, 0)
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
     * 服务端一次刷出整捧散射重力球。拉取格数按本级写入每发弹道。
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
            CollapsingStarsCasting.spawnScatterVolley(
                    level,
                    castingEntity,
                    suctionPullBlocksForLevel(spellLevel)
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }
}

package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GravityBallProjectile;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.spell.helper.GravityCastHelper;
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
 * 重力球（Gravity Ball）——向前释放一发无追踪的紫色重力弹。
 * <p>
 * <strong>无伤害</strong>。命中实体或方块后，把落点半径内敌人拉向施法者：
 * 距离 ≤ 本级拉取格数 → 直接拉到身前；距离更大 → 只沿连线拉近该格数。
 * 当前最大等级 1，1 级拉取 3 格；弹道最大射程固定 25 格。
 * <p>
 * 玩法数字默认值在本类静态字段，由 {@code EldenRingServerConfig} 的 {@code gravity_ball} 段覆盖。
 * 视觉写死在 {@link com.eldenring.spells.particle.gravity.GravityFx} / Renderer。
 */
public class GravityBallSpell extends EldenRingAbstractSpell {

    public static final int SPELL_MAX_LEVEL = 1;
    public static final double SPELL_COOLDOWN_SECONDS = 3.0;

    /** 1 级蓝耗。无伤控场，略低于旧版带伤数值。 */
    public static int SPELL_BASE_MANA_COST = 14;

    /** 每升 1 级额外蓝耗。 */
    public static int SPELL_MANA_COST_PER_LEVEL = 2;

    /** 1 级法术强度基数（本咒不造成伤害，保留给铁魔法面板 / 将来扩展）。 */
    public static int SPELL_BASE_SPELL_POWER = 10;

    /** 每级额外法术强度。 */
    public static int SPELL_SPELL_POWER_PER_LEVEL = 1;

    /** 吟唱 tick。0 = 瞬时。 */
    public static int SPELL_CAST_TIME_TICKS = 0;

    /**
     * 弹道飞行速度（方块/tick）。调大 → 更难躲。
     */
    public static float PROJECTILE_FLIGHT_SPEED = 0.95f;

    /**
     * 弹道最大射程（方块）。飞过这段距离后 discard。固定 25。
     */
    public static double PROJECTILE_MAX_RANGE_BLOCKS = 25.0;

    /**
     * 命中搜敌半径（方块）。以落点为球心，范围内敌对生物进入拉取名单。
     */
    public static float HIT_RADIUS_BLOCKS = 2.4f;

    /**
     * 1 级最大拉取距离（方块）。距离 ≤ 此值 → 拉到身前；更大 → 只拉近这么多格。
     */
    public static double SUCTION_PULL_BLOCKS_AT_LEVEL_1 = 3.0;

    /**
     * 每升一级额外拉取格数。当前最大等级 1，暂不生效；以后升上限时调这个。
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

    /**
     * 飞行拖尾粒子强度（相对 GravityFx 基准）。
     */
    public static float TRAIL_PARTICLE_INTENSITY = 1.0f;

    /**
     * 命中爆裂粒子强度。
     */
    public static float IMPACT_PARTICLE_INTENSITY = 1.35f;

    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "gravity_ball");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SPELL_MAX_LEVEL)
            .setCooldownSeconds(SPELL_COOLDOWN_SECONDS)
            .build();

    public GravityBallSpell() {
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

    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity castingEntity,
            CastSource castSource,
            MagicData playerMagicData
    ) {
        if (!level.isClientSide) {
            GravityCastHelper.spawnAlongLook(
                    level,
                    castingEntity,
                    GravityBallProjectile::new,
                    PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                    0.0f,
                    suctionPullBlocksForLevel(spellLevel),
                    castingEntity.getLookAngle()
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }
}

package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.registry.ModSounds;
import com.eldenring.spells.spell.data.MeteoriteCastData;
import com.eldenring.spells.spell.helper.MeteoriteCasting;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * 陨石（Meteorite）——重力魔法：撕裂虚空，在施法者头顶前方张开黑洞，从中倾泻陨石雨。
 * <p>
 * {@link CastType#CONTINUOUS}：按住施法键维持陨石雨；松开立刻停（客户端发 CancelCast，见
 * {@code MeteoriteClientLock}）。施法期间移动被压到潜行速度，可转身改变落点。
 * <ol>
 *   <li>{@link #onServerPreCast}：记 {@link MeteoriteCastData}，第一个施法 tick 生成黑洞实体</li>
 *   <li>前 {@link #VOID_OPENING_DURATION_TICKS} tick 黑洞张开，不出陨石</li>
 *   <li>之后每 {@link #METEORITE_SPAWN_INTERVAL_TICKS} tick 从黑洞盘面落下一颗陨石：
 *       不追踪，沿视线水平朝向倾斜下坠（低头落得更近、抬头落得更远）</li>
 *   <li>CONTINUOUS 每 10 tick 进一次 {@link #onCast}，铁魔法在 {@code super.onCast} 扣蓝</li>
 *   <li>{@link #onServerCastComplete}：黑洞收缩消失，空中的陨石继续落完</li>
 * </ol>
 * 每颗陨石落地 / 撞敌时做一次小范围爆炸（见 {@code MeteoriteCombat}）。
 * <p>
 * 玩法数字默认值在本类静态字段，由 {@code EldenRingServerConfig} 的 {@code meteorite} 段覆盖。
 * 锚点 / 方向在 {@link MeteoriteCasting}，粒子在 {@code MeteoriteFx}，外观在 {@code MeteoriteRenderer}。
 */
public class MeteoriteSpell extends EldenRingAbstractSpell {

    /** 最大等级种子（数值表：5）。运行时以铁魔法 JSON 为准。 */
    public static final int SPELL_MAX_LEVEL = 5;

    /**
     * 冷却（秒）。数值表未填，暂取 3 秒（同彗星亚兹勒）。
     * 调大 → 松手后更难立刻再开；调小 → 更接近原作随按随放。
     */
    public static final double SPELL_COOLDOWN_SECONDS = 3.0;

    /**
     * 1 级蓝耗。CONTINUOUS 每 10 tick（0.5 秒）扣一次，所以每秒实际消耗为本值的 2 倍。
     */
    public static int SPELL_BASE_MANA_COST = 15;

    /** 每升 1 级每次扣蓝额外增加的量（满级 5 级为 31）。 */
    public static int SPELL_MANA_COST_PER_LEVEL = 4;

    /** 表「初始攻击力」：1 级单颗陨石伤害。 */
    public static float SPELL_BASE_SPELL_POWER = 5;

    /** 表「每级提升攻击力」。满级 5 级为 9。 */
    public static float SPELL_SPELL_POWER_PER_LEVEL = 1;

    /** 单颗陨石伤害 = 表攻击力 × 本系数。数值表对齐后为 1.0。 */
    public static float SPELL_DAMAGE_PER_SPELL_POWER = 1.0f;

    /**
     * 一口气最长按住时长（tick）。400 = 20 秒；蓝耗尽或松手会提前结束。
     */
    public static int SPELL_CAST_TIME_TICKS = 400;

    /**
     * 黑洞张开时长（tick）。这段时间只画黑洞，不落陨石。20 = 1 秒。
     * 调大 → 起手更慢、更像原作的举杖蓄势；调小 → 按下几乎立刻下雨。
     */
    public static int VOID_OPENING_DURATION_TICKS = 20;

    /**
     * 相邻两颗陨石间隔（tick）。10 = 每秒 2 颗，接近原作每秒一两颗的密度。
     * 调小 → 雨更密、DPS 更高、实体更多；调大 → 更稀疏、更像一颗颗砸。
     */
    public static int METEORITE_SPAWN_INTERVAL_TICKS = 10;

    /**
     * 陨石飞行速度（方块/tick）。陨石是重物，默认偏慢，能看清从虚空里砸出来。
     * 调大 → 更难躲；调小 → 更有分量感但更容易被走位躲开。
     */
    public static float PROJECTILE_FLIGHT_SPEED = 0.8f;

    /** 直线最大射程（方块）。飞过这段距离还没落地就碎裂消失。 */
    public static double PROJECTILE_MAX_RANGE_BLOCKS = 60.0;

    /**
     * 平视时的基础下坠角（度，相对水平面向下）。视线俯角会叠加上去。
     * 默认 18° 时平地上约落在身前 15 格；调大 → 落得更近、更陡；调小 → 更远、更平。
     */
    public static float DESCENT_BASE_ANGLE_DEGREES = 18.0f;

    /** 下坠角下限（度）。抬头时不会低于它，保证陨石始终往下砸、不会飞上天。 */
    public static float DESCENT_MIN_ANGLE_DEGREES = 10.0f;

    /** 下坠角上限（度）。低头时不会超过它，避免陨石垂直砸到自己脚边。 */
    public static float DESCENT_MAX_ANGLE_DEGREES = 70.0f;

    /**
     * 左右散布半角（度）。每颗陨石在水平朝向两侧随机偏这么多，铺出原作那种扇形落区。
     * 调大 → 覆盖更宽但更难集中打单体；调小 → 更像一条线。
     */
    public static float SCATTER_HALF_ANGLE_DEGREES = 22.0f;

    /**
     * 下坠角随机抖动（度，±）。让落点前后错开，不会全砸在同一条弧线上。
     * 默认 8° 时平地平视落点大约在身前 11–26 格之间随机。
     */
    public static float DESCENT_JITTER_DEGREES = 8.0f;

    /**
     * 落地爆炸半径（方块）。原作约 1.6 米。范围内每个敌人各结算一次陨石伤害。
     */
    public static float EXPLOSION_RADIUS_BLOCKS = 1.6f;

    /**
     * 爆炸击退强度（原版 {@code LivingEntity#knockback} 强度，普通近战约 0.4），受击退抗性削减。
     * 0 = 不击退。
     */
    public static float KNOCKBACK_STRENGTH = 0.4f;

    /** 注册 ID：{@code iss_elden_ring:meteorite}。 */
    private final ResourceLocation spellResourceLocation =
            new ResourceLocation(EldenRingSpellsMod.MOD_ID, "meteorite");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SPELL_MAX_LEVEL)
            .setCooldownSeconds(SPELL_COOLDOWN_SECONDS)
            .build();

    public MeteoriteSpell() {
        this.manaCostPerLevel = SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = Math.round(SPELL_BASE_SPELL_POWER);
        this.spellPowerPerLevel = Math.round(SPELL_SPELL_POWER_PER_LEVEL);
        this.castTime = SPELL_CAST_TIME_TICKS;
        this.baseManaCost = SPELL_BASE_MANA_COST;
    }

    /**
     * 陨石雨很密，相邻两颗经常砸中同一目标；不关受伤无敌帧的话后一颗会落在前一颗的 i-frame 里打空。
     */
    @Override
    public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setIFrames(0);
    }

    /** 法术书：单颗伤害、爆炸半径、「按住维持陨石雨」。 */
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getMeteoriteDamage(spellLevel, caster), 2)
                ),
                Component.translatable(
                        "ui.irons_spellbooks.radius",
                        Utils.stringTruncation(EXPLOSION_RADIUS_BLOCKS, 1)
                ),
                Component.translatable("ui.iss_elden_ring.hold_to_meteor_rain")
        );
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellResourceLocation;
    }

    /** 黑洞张开是蓄势阶段，接蓄力起手音；第一颗陨石落下时另播飞弹射出音。 */
    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(ModSounds.SPELL_CAST_START.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_OVERHEAD;
    }

    /** 重力紫。 */
    @Override
    public Vector3f getTargetingColor() {
        return new Vector3f(0.62f, 0.40f, 0.95f);
    }

    /** 刚按下：挂上本段吟唱的附加状态。黑洞在第一个施法 tick 生成。 */
    @Override
    public void onServerPreCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        if (!level.isClientSide && playerMagicData != null) {
            playerMagicData.setAdditionalCastData(new MeteoriteCastData());
        }
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
    }

    /**
     * 每个吟唱 tick：保活并贴回黑洞；张开期结束后按间隔落陨石。
     */
    @Override
    public void onServerCastTick(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        if (level.isClientSide || playerMagicData == null) {
            return;
        }
        if (!(playerMagicData.getAdditionalCastData() instanceof MeteoriteCastData castData)) {
            return;
        }
        MeteoriteCasting.ensureVoidEntity(level, entity, castData);

        int elapsedCastTicks = playerMagicData.getCastDuration() - playerMagicData.getCastDurationRemaining();
        if (elapsedCastTicks < VOID_OPENING_DURATION_TICKS) {
            return;
        }
        if (!castData.tryConsumeSpawnInterval()) {
            return;
        }
        MeteoriteCasting.spawnFallingMeteorite(
                level,
                entity,
                getMeteoriteDamage(spellLevel, entity),
                castData.tryMarkFirstMeteoriteSpawned()
        );
    }

    /**
     * CONTINUOUS 每 10 tick 进这里，铁魔法在 {@code super.onCast} 里扣蓝。
     * 陨石由 {@link #onServerCastTick} 在刷，这里不要再生成实体。
     */
    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity castingEntity,
            CastSource castSource,
            MagicData playerMagicData
    ) {
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    /** 松手 / 没蓝 / 时间到：黑洞收缩消失。铁魔法随后 {@code reset()} 再收一次也安全。 */
    @Override
    public void onServerCastComplete(
            Level level,
            int spellLevel,
            LivingEntity entity,
            MagicData playerMagicData,
            boolean cancelled
    ) {
        if (playerMagicData != null && playerMagicData.getAdditionalCastData() instanceof MeteoriteCastData castData) {
            castData.collapseVoid();
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    /** 当前等级下单颗陨石（含落地爆炸）的伤害。 */
    public float getMeteoriteDamage(int spellLevel, LivingEntity caster) {
        return damageFromTableAttack(
                SPELL_BASE_SPELL_POWER,
                SPELL_SPELL_POWER_PER_LEVEL,
                spellLevel,
                caster,
                SPELL_DAMAGE_PER_SPELL_POWER
        );
    }
}

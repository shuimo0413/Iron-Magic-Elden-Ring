package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.registry.ModSounds;
import com.eldenring.spells.spell.data.AstelMeteoriteCastData;
import com.eldenring.spells.spell.helper.AstelMeteoriteCasting;
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
 * 艾斯提陨石（Meteorite of Astel）——陨石的上位重力魔法：撕裂虚空，在施法者前方扇形里随机张开多道虚无裂隙，
 * 从中倾泻陨石。
 * <p>
 * {@link CastType#CONTINUOUS}：按住施法键维持；松开立刻停（客户端发 CancelCast，见 {@code MeteoriteClientLock}）。
 * 施法期间移动被压到潜行速度。
 * <ol>
 *   <li>{@link #onServerPreCast}：记 {@link AstelMeteoriteCastData}</li>
 *   <li>起手 {@link #CAST_WINDUP_TICKS} tick：只撕开第一道裂缝并让它张开，不落陨石（与陨石黑洞张开一致）</li>
 *   <li>每 {@link #RIFT_SPAWN_INTERVAL_TICKS} tick 若存活裂缝少于 {@link #MAX_CONCURRENT_RIFTS}，
 *       就按施法者当前朝向在前方扇形内随机选点开一道新裂缝（已有裂缝固定在原位）</li>
 *   <li>每道裂缝张开 {@link #RIFT_OPENING_DURATION_TICKS} tick 后，每 {@link #RIFT_METEORITE_INTERVAL_TICKS} tick
 *       落一颗陨石，落满 {@link #METEORITES_PER_RIFT_MIN}–{@link #METEORITES_PER_RIFT_MAX} 颗就坍缩，空位再去别处开新裂缝</li>
 *   <li>CONTINUOUS 每 10 tick 进一次 {@link #onCast}，铁魔法在 {@code super.onCast} 扣蓝</li>
 *   <li>{@link #onServerCastComplete}：全部裂缝坍缩，空中的陨石继续落完</li>
 * </ol>
 * 陨石实体 / 渲染 / 落地爆炸复用陨石法术（{@code MeteoriteProjectile}、{@code MeteoriteCombat}），
 * 裂缝视觉复用 {@code MeteoriteVoidEntity}。
 * <p>
 * 玩法数字默认值在本类静态字段，由 {@code EldenRingServerConfig} 的 {@code meteorite_of_astel} 段覆盖。
 * 裂缝选点 / 出弹在 {@link AstelMeteoriteCasting}。
 */
public class AstelMeteoriteSpell extends EldenRingAbstractSpell {

    /** 最大等级种子（数值表：3）。运行时以铁魔法 JSON 为准。 */
    public static final int SPELL_MAX_LEVEL = 3;

    /** 冷却（秒，数值表：6）。调大 → 松手后更难立刻再开。 */
    public static final double SPELL_COOLDOWN_SECONDS = 6.0;

    /** 1 级蓝耗（数值表：35）。CONTINUOUS 每 10 tick（0.5 秒）扣一次，所以每秒实际消耗为本值的 2 倍。 */
    public static int SPELL_BASE_MANA_COST = 35;

    /** 每升 1 级每次扣蓝额外增加的量（数值表：5，满级 3 级为 45）。 */
    public static int SPELL_MANA_COST_PER_LEVEL = 5;

    /** 表「初始攻击力」：1 级单颗陨石伤害（数值表：5）。 */
    public static float SPELL_BASE_SPELL_POWER = 5;

    /** 表「每级提升攻击力」（数值表：2，满级 3 级为 9）。 */
    public static float SPELL_SPELL_POWER_PER_LEVEL = 2;

    /** 单颗陨石伤害 = 表攻击力 × 本系数。数值表对齐后为 1.0。 */
    public static float SPELL_DAMAGE_PER_SPELL_POWER = 1.0f;

    /** 一口气最长按住时长（tick）。400 = 20 秒；蓝耗尽或松手会提前结束。 */
    public static int SPELL_CAST_TIME_TICKS = 400;

    /**
     * 起手蓄力时长（tick）。按下后只撕开第一道裂缝并让它张开这么久，期间不落陨石、不开其它裂缝。
     * 20 = 1 秒，与陨石黑洞张开时长一致。调大 → 起手更慢更有仪式感；调小 → 更快开始下雨。
     */
    public static int CAST_WINDUP_TICKS = 20;

    /**
     * 同时存活的裂缝上限（道）。设计约定不超过 4。
     * 调大 → 雨更密、实体更多；调小 → 更像一处处轮流砸。
     */
    public static int MAX_CONCURRENT_RIFTS = 4;

    /** 每道裂缝最少落几颗陨石后坍缩（颗）。 */
    public static int METEORITES_PER_RIFT_MIN = 2;

    /** 每道裂缝最多落几颗陨石后坍缩（颗）。 */
    public static int METEORITES_PER_RIFT_MAX = 3;

    /**
     * 裂缝张开时长（tick），期间不落陨石。8 ≈ 0.4 秒。
     * 调大 → 每道裂缝起手更慢、更好躲；调小 → 一开即砸。
     */
    public static int RIFT_OPENING_DURATION_TICKS = 8;

    /** 同一道裂缝相邻两颗陨石的间隔（tick）。8 ≈ 0.4 秒。 */
    public static int RIFT_METEORITE_INTERVAL_TICKS = 8;

    /**
     * 两次开新裂缝之间的最短间隔（tick）。6 ≈ 0.3 秒。
     * 让裂缝错开出现而不是同一 tick 全开；调小 → 起手更快铺满 4 道。
     */
    public static int RIFT_SPAWN_INTERVAL_TICKS = 6;

    /**
     * 裂缝选点的扇形半角（度，相对施法者水平朝向）。
     * 调大 → 覆盖更宽；调小 → 更集中在正前方。
     */
    public static float RIFT_FAN_HALF_ANGLE_DEGREES = 30.0f;

    /**
     * 裂缝朝向相对其位置偏角的保留比例（0–1）。裂缝在扇形里散开摆放，但朝向往正前方收拢：
     * 朝向偏航 = 施法者偏航 + 位置偏角 × 本值。默认 0.4 时扇形边缘（30°）的裂缝只朝外偏 12°。
     * 调小 → 陨石更集中砸向准星正前方、更容易命中；调大 → 落区更宽、更难打中单体；1 = 朝向与位置一致。
     */
    public static float RIFT_FACING_SPREAD_FRACTION = 0.4f;

    /** 裂缝中心相对施法者眼睛的最小水平前移（方块）。 */
    public static double RIFT_FORWARD_MIN_BLOCKS = 2.0;

    /** 裂缝中心相对施法者眼睛的最大水平前移（方块）。调大 → 落区整体更远更散。 */
    public static double RIFT_FORWARD_MAX_BLOCKS = 6.0;

    /** 裂缝中心相对施法者眼睛的最小上抬高度（方块）。 */
    public static double RIFT_HEIGHT_MIN_BLOCKS = 2.5;

    /** 裂缝中心相对施法者眼睛的最大上抬高度（方块）。调大 → 陨石从更高处砸下、落点更远。 */
    public static double RIFT_HEIGHT_MAX_BLOCKS = 4.5;

    /** 陨石飞行速度（方块/tick）。调大 → 更难躲；调小 → 更有分量感。 */
    public static float PROJECTILE_FLIGHT_SPEED = 0.8f;

    /** 直线最大射程（方块）。飞过这段距离还没落地就碎裂消失。 */
    public static double PROJECTILE_MAX_RANGE_BLOCKS = 60.0;

    /**
     * 平视时的基础下坠角（度，相对水平面向下）。视线俯角会叠加上去。
     * 调大 → 落得更近更陡；调小 → 更远更平。
     */
    public static float DESCENT_BASE_ANGLE_DEGREES = 18.0f;

    /** 下坠角下限（度）。抬头时不会低于它，保证陨石始终往下砸。 */
    public static float DESCENT_MIN_ANGLE_DEGREES = 10.0f;

    /** 下坠角上限（度）。低头时不会超过它，避免陨石垂直砸到自己脚边。 */
    public static float DESCENT_MAX_ANGLE_DEGREES = 70.0f;

    /** 下坠角随机抖动（度，±）。让落点前后错开。 */
    public static float DESCENT_JITTER_DEGREES = 6.0f;

    /**
     * 单颗陨石相对所在裂缝朝向的左右随机偏转（度，±）。扇面主要靠裂缝分散铺开，这里只做细碎错落。
     */
    public static float METEORITE_YAW_JITTER_DEGREES = 8.0f;

    /** 落地爆炸半径（方块）。上位法术比陨石（1.6）略大。范围内每个敌人各结算一次伤害。 */
    public static float EXPLOSION_RADIUS_BLOCKS = 2.0f;

    /** 爆炸击退强度（原版 knockback 强度，受击退抗性削减）。0 = 不击退。 */
    public static float KNOCKBACK_STRENGTH = 0.5f;

    /** 注册 ID：{@code iss_elden_ring:meteorite_of_astel}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "meteorite_of_astel");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SPELL_MAX_LEVEL)
            .setCooldownSeconds(SPELL_COOLDOWN_SECONDS)
            .build();

    public AstelMeteoriteSpell() {
        this.manaCostPerLevel = SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = Math.round(SPELL_BASE_SPELL_POWER);
        this.spellPowerPerLevel = Math.round(SPELL_SPELL_POWER_PER_LEVEL);
        this.castTime = SPELL_CAST_TIME_TICKS;
        this.baseManaCost = SPELL_BASE_MANA_COST;
    }

    /**
     * 多道裂缝的陨石经常接连砸中同一目标；不关受伤无敌帧的话后一颗会落在前一颗的 i-frame 里打空。
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

    /** 撕裂虚空是蓄势阶段，接蓄力起手音；第一颗陨石落下时另播飞弹射出音。 */
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

    /** 刚按下：挂上本段吟唱的附加状态。裂缝在施法 tick 里陆续生成。 */
    @Override
    public void onServerPreCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        if (!level.isClientSide && playerMagicData != null) {
            playerMagicData.setAdditionalCastData(new AstelMeteoriteCastData());
        }
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
    }

    /** 每个吟唱 tick：驱动已有裂缝落陨石 / 坍缩，有空位就开新裂缝。 */
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
        if (!(playerMagicData.getAdditionalCastData() instanceof AstelMeteoriteCastData castData)) {
            return;
        }
        AstelMeteoriteCasting.tickRifts(level, entity, castData, this, getMeteoriteDamage(spellLevel, entity));
        if (castData.tryMarkFirstRiftOpened()) {
            AstelMeteoriteCasting.openRift(level, entity, castData, Math.max(CAST_WINDUP_TICKS, RIFT_OPENING_DURATION_TICKS));
            return;
        }
        int elapsedCastTicks = playerMagicData.getCastDuration() - playerMagicData.getCastDurationRemaining();
        if (elapsedCastTicks < CAST_WINDUP_TICKS) {
            return;
        }
        if (castData.tryConsumeRiftSpawnWindow()) {
            AstelMeteoriteCasting.openRift(level, entity, castData, RIFT_OPENING_DURATION_TICKS);
        }
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

    /** 松手 / 没蓝 / 时间到：全部裂缝坍缩。铁魔法随后 {@code reset()} 再收一次也安全。 */
    @Override
    public void onServerCastComplete(
            Level level,
            int spellLevel,
            LivingEntity entity,
            MagicData playerMagicData,
            boolean cancelled
    ) {
        if (playerMagicData != null && playerMagicData.getAdditionalCastData() instanceof AstelMeteoriteCastData castData) {
            castData.collapseAllRifts();
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

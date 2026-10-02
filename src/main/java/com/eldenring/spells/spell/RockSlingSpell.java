package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.registry.ModSounds;
import com.eldenring.spells.spell.data.RockSlingCastData;
import com.eldenring.spells.spell.helper.RockSlingCasting;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * 岩石球（Rock Sling）——重力魔法：蓄力 1.5 秒，身前横排凝聚三块粗糙岩石，满蓄后一齐飞向前方敌人。
 * <p>
 * 时序：
 * <ol>
 *   <li>{@link #onServerPreCast}：生成岩石实体（悬停态），记进 {@link RockSlingCastData}</li>
 *   <li>蓄力期间岩石自己贴在施法者身前对应槽位，按槽位错开由小到大长出来（见实体）</li>
 *   <li>{@link #onCast}（满蓄）：三块锁定同一目标发射；基类播飞弹射出音</li>
 *   <li>{@link #onServerCastComplete} 带 {@code cancelled}：松手 / 打断，岩石碎裂消散</li>
 * </ol>
 * 每块独立结算伤害（关闭受伤无敌帧），命中后做受击退抗性削减的普通击退。
 * <p>
 * 玩法数字默认值在本类静态字段，由 {@code EldenRingServerConfig} 的 {@code rock_sling} 段覆盖。
 * 悬停排布在 {@link RockSlingCasting}，生长时序在实体，粒子在 {@code RockSlingFx}，外观在 Renderer。
 */
public class RockSlingSpell extends EldenRingAbstractSpell {

    /** 最大等级种子。运行时以铁魔法 JSON 为准。 */
    public static final int SPELL_MAX_LEVEL = 7;

    /** 冷却（秒）。 */
    public static final double SPELL_COOLDOWN_SECONDS = 3.0;

    /** 1 级蓝耗。 */
    public static int SPELL_BASE_MANA_COST = 30;

    /** 每升 1 级额外蓝耗。 */
    public static int SPELL_MANA_COST_PER_LEVEL = 2;

    /** 表「初始攻击力」：1 级单块岩石伤害。 */
    public static float SPELL_BASE_SPELL_POWER = 5;

    /** 表「每级提升攻击力」。 */
    public static float SPELL_SPELL_POWER_PER_LEVEL = 1;

    /** 单块伤害 = 表攻击力 × 本系数。数值表对齐后为 1.0。 */
    public static float SPELL_DAMAGE_PER_SPELL_POWER = 1.0f;

    /**
     * 蓄力 tick（30 = 1.5 秒）。岩石生长时序按此长度铺开；调短会让岩石还没长满就飞出。
     */
    public static int SPELL_CAST_TIME_TICKS = 30;

    /**
     * 一次凝聚的岩石数量。横排均分在身前；调大更难全部躲开，总伤害随之线性增加。
     */
    public static int ROCK_COUNT = 3;

    /**
     * 岩石飞行速度（方块/tick）。调大更难躲；调小更有「重石砸来」的分量感。
     */
    public static float PROJECTILE_FLIGHT_SPEED = 0.7f;

    /** 最大射程（方块）。飞过这段距离后碎裂消失。 */
    public static double PROJECTILE_MAX_RANGE_BLOCKS = 32.0;

    /** 发射时索敌半径（方块）。超出此距离的敌人不会被锁定，岩石直飞准星。 */
    public static double PROJECTILE_TRACKING_RANGE_BLOCKS = 28.0;

    /**
     * 飞行中每 tick 最大转向角（度）。岩石是重物，只做轻度修正；
     * 调大更黏人，调小侧移即可躲开。
     */
    public static float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 2.5f;

    /**
     * 单块命中击退强度（原版 {@code LivingEntity#knockback} 强度，普通近战约 0.4）。
     * 会被目标击退抗性削减，抗性满额的目标纹丝不动。三块都中时击退叠加。
     */
    public static float KNOCKBACK_STRENGTH = 0.5f;

    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "rock_sling");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SPELL_MAX_LEVEL)
            .setCooldownSeconds(SPELL_COOLDOWN_SECONDS)
            .build();

    public RockSlingSpell() {
        this.manaCostPerLevel = SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = Math.round(SPELL_BASE_SPELL_POWER);
        this.spellPowerPerLevel = Math.round(SPELL_SPELL_POWER_PER_LEVEL);
        this.castTime = SPELL_CAST_TIME_TICKS;
        this.baseManaCost = SPELL_BASE_MANA_COST;
    }

    /**
     * 三块岩石几乎同时命中同一目标；不关无敌帧的话第二、三块会落在第一块的 i-frame 里打空。
     */
    @Override
    public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setIFrames(0);
    }

    /** 法术书：单块伤害、岩石数量、射程。 */
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getRockDamage(spellLevel, caster), 2)
                ),
                Component.literal("×" + ROCK_COUNT),
                Component.translatable(
                        "ui.iss_elden_ring.projectile_range",
                        Utils.stringTruncation(PROJECTILE_MAX_RANGE_BLOCKS, 0)
                )
        );
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    /** 长吟唱：蓄满 1.5 秒才出手；中途松手取消。 */
    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellResourceLocation;
    }

    /** 蓄力起手音；出手瞬间走基类的飞弹射出音（发射就在 {@link #onCast}，时间点一致）。 */
    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(ModSounds.SPELL_CAST_START.get());
    }

    /** 重力紫。 */
    @Override
    public Vector3f getTargetingColor() {
        return new Vector3f(0.62f, 0.40f, 0.95f);
    }

    /** 刚按下：在身前生成悬停岩石，后续生长 / 跟随由实体自己完成。 */
    @Override
    public void onServerPreCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        if (!level.isClientSide) {
            RockSlingCastData castData = new RockSlingCastData();
            RockSlingCasting.spawnHoveringRocks(
                    level,
                    entity,
                    getRockDamage(spellLevel, entity),
                    castData
            );
            if (playerMagicData != null) {
                playerMagicData.setAdditionalCastData(castData);
            }
        }
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
    }

    /** 满蓄：把本次凝聚的岩石一齐射向前方敌人；找不到已凝聚的岩石时（非玩家施法者等）直接生成满尺寸再射。 */
    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity castingEntity,
            CastSource castSource,
            MagicData playerMagicData
    ) {
        if (!level.isClientSide) {
            RockSlingCastData castData = playerMagicData != null
                    && playerMagicData.getAdditionalCastData() instanceof RockSlingCastData rockSlingCastData
                    ? rockSlingCastData
                    : null;
            RockSlingCasting.launchRocks(
                    level,
                    castingEntity,
                    getRockDamage(spellLevel, castingEntity),
                    castData
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    /** 松手 / 被打断：还没飞出的岩石碎裂消散。 */
    @Override
    public void onServerCastComplete(
            Level level,
            int spellLevel,
            LivingEntity entity,
            MagicData playerMagicData,
            boolean cancelled
    ) {
        if (!level.isClientSide && cancelled && playerMagicData != null
                && playerMagicData.getAdditionalCastData() instanceof RockSlingCastData castData) {
            RockSlingCasting.shatterHoveringRocks(level, castData);
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    /** 当前等级下单块岩石命中伤害。 */
    public float getRockDamage(int spellLevel, LivingEntity caster) {
        return damageFromTableAttack(
                SPELL_BASE_SPELL_POWER,
                SPELL_SPELL_POWER_PER_LEVEL,
                spellLevel,
                caster,
                SPELL_DAMAGE_PER_SPELL_POWER
        );
    }
}

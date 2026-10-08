package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.spell.data.AdulasMoonbladeCastData;
import com.eldenring.spells.spell.helper.AdulasMoonbladeCasting;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.config.SpellConfigParameter;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
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
 * 亚杜拉的月光剑（Adula's Moonblade）：施法与卡利亚大剑相同——按下出第一刀，长按交替
 * {@code carian_great_sword1} / {@code carian_great_sword2}，每刀 0.5 秒 + 0.5 秒收招空档。
 * <p>
 * 与大剑的区别：
 * <ul>
 *   <li>手里是冰蓝白的月光剑，刀光蓝白，沿刃粒子换成冰霜系（{@link com.eldenring.spells.spell.fx.AdulasMoonbladeFx}）；</li>
 *   <li>每一刀命中帧在扇形斩击之外，再沿视线射出一道单层冰月牙剑气
 *       （{@link com.eldenring.spells.entity.AdulasMoonbladeWaveProjectile}），留下一路冰雾；</li>
 *   <li>斩击与剑气各结算一次攻击力，两段命中都会让目标原版结霜（冻伤扣血），不会冻进冰牢；</li>
 *   <li>法强吃辉石 + 冰霜超额（同辉石冰块）。</li>
 * </ul>
 * 客户端动作 / 手持剑 / 光轨见 {@link com.eldenring.spells.client.AdulasMoonbladeClientHold}；
 * 服务端锚点 {@link com.eldenring.spells.entity.AdulasMoonbladeEntity}。
 */
public class AdulasMoonbladeSpell extends EldenRingAbstractSpell {

    /** 最大等级种子（数值表 5 级）；运行时以铁魔法 JSON 为准。 */
    public static final int SPELL_MAX_LEVEL = 5;

    /** 冷却（秒，数值表）。松手后要等 8 秒才能再开一轮连斩。 */
    public static final double SPELL_COOLDOWN_SECONDS = 8.0;

    /** 1 级基础法力消耗（数值表）。CONTINUOUS 按住期间按铁魔法节奏扣蓝。 */
    public static int SPELL_BASE_MANA_COST = 30;

    /** 每升一级额外法力消耗（数值表），满级 46。 */
    public static int SPELL_MANA_COST_PER_LEVEL = 4;

    /** 1 级攻击力（数值表）。斩击与剑气各吃一次。 */
    public static float SPELL_BASE_SPELL_POWER = 5;

    /** 每升一级额外攻击力（数值表），满级 11。 */
    public static float SPELL_SPELL_POWER_PER_LEVEL = 1.5f;

    /**
     * 按住最长持续时间（tick）。CONTINUOUS 上限，不是单刀片长。
     */
    public static int SPELL_CAST_TIME_TICKS = 160;

    /** 斩击伤害 = 攻击力 × 本系数。调大 → 近身一刀更痛。 */
    public static float DAMAGE_PER_SPELL_POWER = 1.0f;

    /** 剑气伤害 = 攻击力 × 本系数（默认 30%，近身斩击才是主伤害）。调大 → 远程剑气更痛。 */
    public static float WAVE_DAMAGE_PER_SPELL_POWER = 0.3f;

    /** 斩击扇形半径（方块）。同卡利亚大剑 7 格。 */
    public static float SLASH_RADIUS_BLOCKS = 7.0f;

    /** 斩击扇形半角（度）。相对视线左右各半角。 */
    public static float SLASH_HALF_ANGLE_DEGREES = 85.0f;

    /** 斩击击退强度。调大 → 被砍的怪往后弹得更开。 */
    public static double SLASH_KNOCKBACK_STRENGTH = 0.4;

    /** 剑气飞行速度（方块/tick）。调大 → 更难侧移躲开。 */
    public static float WAVE_FLIGHT_SPEED = 0.9f;

    /** 剑气直线最大射程（方块）。飞满后碎裂消散。 */
    public static double WAVE_MAX_RANGE_BLOCKS = 25.0;

    /** 剑气出手半宽（方块）。左右合计约 4 格。 */
    public static float WAVE_START_HALF_WIDTH_BLOCKS = 2.0f;

    /** 剑气张满后的半宽（方块）。左右合计约 9 格；调大 → 横向扫得更宽。 */
    public static float WAVE_MAX_HALF_WIDTH_BLOCKS = 4.5f;

    /**
     * 剑气最多结算几个敌人（每个敌人只吃一次）。写入铁魔法 {@code pierceLevel = 本值 - 1}。
     * 调大 → 能穿更长一排；调小 → 清一小撮就散。
     */
    public static int WAVE_MAX_ENTITY_HITS = 10;

    /**
     * 命中后原版完全冻结持续秒数（结霜 + 每 2 秒一次冻伤扣血 + 减速），见 {@link com.eldenring.spells.spell.helper.FrostHelper}。
     * 连斩只刷新不叠加；调大 → 冻伤扣血次数更多。0 = 不结霜。
     */
    public static int SPELL_FROST_SECONDS = 4;

    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "adulas_moonblade");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SPELL_MAX_LEVEL)
            .setCooldownSeconds(SPELL_COOLDOWN_SECONDS)
            .build();

    public AdulasMoonbladeSpell() {
        this.manaCostPerLevel = SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = Math.round(SPELL_BASE_SPELL_POWER);
        this.spellPowerPerLevel = Math.round(SPELL_SPELL_POWER_PER_LEVEL);
        this.castTime = SPELL_CAST_TIME_TICKS;
        this.baseManaCost = SPELL_BASE_MANA_COST;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    /** 按住连斩；松手后当前刀播完才 CancelCast，避免铁魔法中途清掉动画。 */
    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellResourceLocation;
    }

    /**
     * 辉石法强与冰霜法强超额相加（同辉石冰块），不让两段学派倍率互乘。
     */
    @Override
    public float getSpellPower(int spellLevel, @Nullable Entity sourceEntity) {
        double entitySpellPowerModifier = 1;
        double entitySchoolPowerModifier = 1;
        float configPowerModifier = SpellConfigManager.getSpellConfigValue(this, SpellConfigParameter.POWER_MULTIPLIER).floatValue();
        if (sourceEntity instanceof LivingEntity livingEntity) {
            entitySpellPowerModifier = livingEntity.getAttributeValue(AttributeRegistry.SPELL_POWER);
            entitySchoolPowerModifier = GlintstoneIcecragSpell.combinedGlintstoneAndIceSchoolPower(livingEntity);
        }
        return (float) ((baseSpellPower + spellPowerPerLevel * (spellLevel - 1))
                * entitySpellPowerModifier
                * entitySchoolPowerModifier
                * configPowerModifier);
    }

    @Override
    public float getEntityPowerMultiplier(@Nullable LivingEntity entity) {
        float configPowerModifier = SpellConfigManager.getSpellConfigValue(this, SpellConfigParameter.POWER_MULTIPLIER).floatValue();
        if (entity == null) {
            return configPowerModifier;
        }
        double entitySpellPowerModifier = entity.getAttributeValue(AttributeRegistry.SPELL_POWER);
        return (float) (configPowerModifier
                * entitySpellPowerModifier
                * GlintstoneIcecragSpell.combinedGlintstoneAndIceSchoolPower(entity));
    }

    /**
     * 连斩 + 剑气要吃满每一下，取消无敌帧。结霜在命中后单独写冻结槽，不走伤害源。
     */
    @Override
    public SpellDamageSource getDamageSource(@Nullable Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker)
                .setIFrames(0);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getSlashDamage(spellLevel, caster), 2)
                ),
                Component.translatable(
                        "ui.irons_spellbooks.radius",
                        Utils.stringTruncation(SLASH_RADIUS_BLOCKS, 1)
                ),
                Component.translatable(
                        "ui.iss_elden_ring.projectile_range",
                        Utils.stringTruncation(WAVE_MAX_RANGE_BLOCKS, 1)
                ),
                Component.translatable(
                        "ui.irons_spellbooks.freeze_time",
                        Utils.timeFromTicks(SPELL_FROST_SECONDS * 20, 2)
                ),
                Component.translatable("ui.iss_elden_ring.hold_to_combo")
        );
    }

    /** 单刀近身斩击伤害。 */
    public float getSlashDamage(int spellLevel, LivingEntity caster) {
        return damageFromTableAttack(SPELL_BASE_SPELL_POWER, SPELL_SPELL_POWER_PER_LEVEL, spellLevel, caster, DAMAGE_PER_SPELL_POWER);
    }

    /** 单道剑气命中一个敌人的伤害。 */
    public float getWaveDamage(int spellLevel, LivingEntity caster) {
        return damageFromTableAttack(SPELL_BASE_SPELL_POWER, SPELL_SPELL_POWER_PER_LEVEL, spellLevel, caster, WAVE_DAMAGE_PER_SPELL_POWER);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.empty();
    }

    /** 剑气出手音在锚点实体真正出弹时播放，这里留空避免按下瞬间抢响。 */
    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    /** 起手动画由客户端 Hold 在无镜像的专用层播放，这里返回 none。 */
    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public Vector3f getTargetingColor() {
        return new Vector3f(0.70f, 0.88f, 1.0f);
    }

    @Override
    public void onServerPreCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        if (!level.isClientSide && playerMagicData != null) {
            ensureMoonbladeEntity(level, spellLevel, entity, playerMagicData);
        }
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
    }

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
        ensureMoonbladeEntity(level, spellLevel, entity, playerMagicData);
    }

    private void ensureMoonbladeEntity(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        AdulasMoonbladeCastData castData;
        if (playerMagicData.getAdditionalCastData() instanceof AdulasMoonbladeCastData existingCastData) {
            castData = existingCastData;
        } else {
            castData = new AdulasMoonbladeCastData();
            playerMagicData.setAdditionalCastData(castData);
        }
        AdulasMoonbladeCasting.ensureMoonbladeEntity(
                level,
                entity,
                castData,
                getSlashDamage(spellLevel, entity),
                getWaveDamage(spellLevel, entity)
        );
    }

    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            CastSource castSource,
            MagicData playerMagicData
    ) {
        // CONTINUOUS 真正结算在锚点实体命中帧；此处只走基类扣蓝 / 冷却。
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastComplete(
            Level level,
            int spellLevel,
            LivingEntity entity,
            MagicData playerMagicData,
            boolean cancelled
    ) {
        if (playerMagicData.getAdditionalCastData() instanceof AdulasMoonbladeCastData castData) {
            AdulasMoonbladeCasting.requestStop(castData);
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }
}

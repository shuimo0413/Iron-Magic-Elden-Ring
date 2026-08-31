package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.spell.data.CarianGreatswordCastData;
import com.eldenring.spells.spell.helper.CarianGreatswordCasting;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
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
 * 卡利亚大剑（Carian Greatsword）：按下出第一刀，长按交替
 * {@code carian_great_sword1} / {@code carian_great_sword2}。
 * <p>
 * 客户端 PlayerAnimator 斩击动作；每刀 0.5 秒片长，由
 * {@link com.eldenring.spells.client.CarianGreatswordClientHold} 调度。
 * 手里的像素剑由 {@link com.eldenring.spells.client.render.carian.CarianGreatswordHandLayer} 跟右手骨骼；
 * 贴图像素与迅剑相同，握点 / 模型 JSON 是大剑自己的。
 * 挥砍光轨由 {@link com.eldenring.spells.client.render.carian.CarianGreatswordTrail} 跟剑走，
 * 沿刃星星仍用 {@link com.eldenring.spells.spell.fx.CarianSlicerFx}。
 * 服务端斩击锚点 {@link com.eldenring.spells.entity.CarianGreatswordEntity} 按刀结算扇形伤害。
 * 命中音走辉石蓄力起手（{@code spell_cast_start}），起手/收招仍静音。
 * 施法不吃铁魔法的约 0.2 倍移速惩罚，斩击期间按走路 / 冲刺原速移动。
 */
public class CarianGreatswordSpell extends EldenRingAbstractSpell {

    /** 最大等级种子；运行时以铁魔法 JSON 为准。 */
    public static final int SPELL_MAX_LEVEL = 1;

    /** 冷却（秒）。比迅剑略长，单刀更重。 */
    public static final double SPELL_COOLDOWN_SECONDS = 0.5;

    /** 1 级基础法力消耗。CONTINUOUS 按住期间按铁魔法节奏扣蓝。 */
    public static int SPELL_BASE_MANA_COST = 18;

    /** 每升一级额外法力消耗。 */
    public static int SPELL_MANA_COST_PER_LEVEL = 3;

    /** 1 级基础法术强度。 */
    public static int SPELL_BASE_SPELL_POWER = 16;

    /** 每升一级额外法术强度。 */
    public static int SPELL_SPELL_POWER_PER_LEVEL = 3;

    /**
     * 按住最长持续时间（tick）。CONTINUOUS 上限，不是单刀片长。
     * 单刀片长固定 10 tick = 0.5 s，对齐 player_animation JSON，写在 CastCurve / 客户端。
     */
    public static int SPELL_CAST_TIME_TICKS = 160;

    /**
     * 每刀伤害 = 法强 × 本系数。调大 → 单刀更痛；大剑比迅剑更重，默认约两倍。
     */
    public static float DAMAGE_PER_SPELL_POWER = 1.15f;

    /**
     * 扇形攻击半径（方块）。调大 → 更远也能砍到；搜箱与角度判定共用。
     * 大剑比迅剑更长，默认 4.5 格。
     */
    public static float SLASH_RADIUS_BLOCKS = 4.5f;

    /**
     * 扇形半角（度）。相对视线左右各半角；调大 → 侧面更容易命中。
     */
    public static float SLASH_HALF_ANGLE_DEGREES = 85.0f;

    /**
     * 命中击退强度。调大 → 被砍的怪往后弹得更开。
     */
    public static double SLASH_KNOCKBACK_STRENGTH = 0.4;

    /** 点按第一刀 clip 名；由客户端专用层播放，不再走铁魔法 cast-start 动画层。 */
    public static final AnimationHolder OPENING_SLASH_ANIMATION = new AnimationHolder(
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "carian_great_sword1"),
            true
    );

    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "carian_greatsword");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SPELL_MAX_LEVEL)
            .setCooldownSeconds(SPELL_COOLDOWN_SECONDS)
            .build();

    public CarianGreatswordSpell() {
        this.manaCostPerLevel = SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = SPELL_SPELL_POWER_PER_LEVEL;
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
     * 连斩需要吃满每刀，取消无敌帧，否则第二刀会打不穿 i-frame。
     */
    @Override
    public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setIFrames(0);
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
                Component.translatable("ui.elden_ring_spells.hold_to_combo")
        );
    }

    /** 单刀结算伤害。 */
    public float getSlashDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * DAMAGE_PER_SPELL_POWER;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    /**
     * 起手动画由 {@link com.eldenring.spells.client.CarianGreatswordClientHold} 在无镜像的专用层播放。
     * 这里返回 none，避免铁魔法默认 ANIMATION 层（带 MirrorModifier）先播一遍把左右翻反。
     */
    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

    /** 收招也不要铁魔法默认挥击，避免盖住大剑最后一帧。 */
    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public Vector3f getTargetingColor() {
        return new Vector3f(0.22f, 0.42f, 1.0f);
    }

    @Override
    public void onServerPreCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        if (!level.isClientSide && playerMagicData != null) {
            CarianGreatswordCastData castData = new CarianGreatswordCastData();
            playerMagicData.setAdditionalCastData(castData);
            CarianGreatswordCasting.ensureGreatswordEntity(
                    level,
                    entity,
                    castData,
                    getSlashDamage(spellLevel, entity)
            );
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
        CarianGreatswordCastData castData;
        if (playerMagicData.getAdditionalCastData() instanceof CarianGreatswordCastData existingCastData) {
            castData = existingCastData;
        } else {
            castData = new CarianGreatswordCastData();
            playerMagicData.setAdditionalCastData(castData);
        }
        CarianGreatswordCasting.ensureGreatswordEntity(
                level,
                entity,
                castData,
                getSlashDamage(spellLevel, entity)
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
        // CONTINUOUS 真正结算在实体命中窗；此处只走基类扣蓝 / 冷却。
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
        if (playerMagicData.getAdditionalCastData() instanceof CarianGreatswordCastData castData) {
            CarianGreatswordCasting.requestStop(castData);
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }
}

package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.CarianSlicerEntity;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.spell.combat.CarianSlicerCombat;
import com.eldenring.spells.spell.data.CarianSlicerCastData;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
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
 * 卡利亚迅剑（Carian Slicer）：右手召唤深蓝辉剑，按住则正反手连斩身前扇形。
 * <p>
 * {@link CastType#CONTINUOUS}：按住施法键维持连斩；松开后当前这一刀收完再淡出。
 * 铁魔法 CONTINUOUS 默认会持续到时间/蓝耗尽，客户端另发 CancelCast 才能松手即停。
 * 实体跟手挥砍，不做弹道。
 */
public class CarianSlicerSpell extends EldenRingAbstractSpell {

    /** 冷却 / 最大等级种子；运行时以铁魔法 JSON 为准。 */
    public static final int SPELL_MAX_LEVEL = 1;
    public static final double SPELL_COOLDOWN_SECONDS = 0.35;

    public static int SPELL_BASE_MANA_COST = 8;
    public static int SPELL_MANA_COST_PER_LEVEL = 2;
    public static int SPELL_BASE_SPELL_POWER = 12;
    public static int SPELL_SPELL_POWER_PER_LEVEL = 2;
    public static int SPELL_CAST_TIME_TICKS = 160;

    /** 挥砍伤害 = 法术强度 × 本系数。 */
    public static float SLASH_DAMAGE_PER_SPELL_POWER = 0.92f;
    /** 扇形半径（方块）。 */
    public static float SLASH_RANGE_BLOCKS = 2.85f;
    /** 扇形半角（度）。 */
    public static float SLASH_HALF_ANGLE_DEGREES = 58.0f;
    /** 击退强度。迅剑刻意偏低。 */
    public static double SLASH_KNOCKBACK_STRENGTH = 0.18;
    /** 单刀周期（tick）。调大连斩更疏。 */
    public static int SLASH_CYCLE_TICKS = 5;

    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "carian_slicer");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SPELL_MAX_LEVEL)
            .setCooldownSeconds(SPELL_COOLDOWN_SECONDS)
            .build();

    public CarianSlicerSpell() {
        this.manaCostPerLevel = SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = SPELL_CAST_TIME_TICKS;
        this.baseManaCost = SPELL_BASE_MANA_COST;
    }

    /**
     * 连斩需要打穿原版受伤无敌帧，否则第二刀会吃 0。
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
                        Utils.stringTruncation(SLASH_RANGE_BLOCKS, 1)
                ),
                Component.translatable("ui.elden_ring_spells.hold_to_combo")
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

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    /**
     * 起手不播铁魔法那一次定格动作。左右挥砍由客户端按每一刀重播，否则 CONTINUOUS 只会抡一下。
     */
    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public Vector3f getTargetingColor() {
        return new Vector3f(0.22f, 0.42f, 1.0f);
    }

    /**
     * 起手立刻刷出辉剑；之后的刀由实体按周期自己抡，不在 {@link #onCast} 里叠实体。
     */
    @Override
    public void onServerPreCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        if (!level.isClientSide && playerMagicData != null) {
            CarianSlicerCombat.discardOwnedSlicers(level, entity);
            CarianSlicerCastData castData = new CarianSlicerCastData();
            CarianSlicerEntity slicerEntity = new CarianSlicerEntity(
                    level,
                    entity,
                    getSlashDamage(spellLevel, entity)
            );
            level.addFreshEntity(slicerEntity);
            slicerEntity.refreshWhileCasting();
            castData.bindSlicerEntity(slicerEntity);
            playerMagicData.setAdditionalCastData(castData);
        }
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
    }

    /**
     * 每个吟唱 tick 告诉剑「还按着」；实体自己决定何时接下刀。
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
        if (!(playerMagicData.getAdditionalCastData() instanceof CarianSlicerCastData castData)) {
            return;
        }
        CarianSlicerEntity slicerEntity = castData.slicerEntity();
        if (slicerEntity == null || slicerEntity.isRemoved()) {
            CarianSlicerEntity replacement = new CarianSlicerEntity(
                    level,
                    entity,
                    getSlashDamage(spellLevel, entity)
            );
            level.addFreshEntity(replacement);
            castData.bindSlicerEntity(replacement);
            slicerEntity = replacement;
        }
        slicerEntity.refreshWhileCasting();
        slicerEntity.setSlashDamage(getSlashDamage(spellLevel, entity));
    }

    /**
     * CONTINUOUS 每 10 tick 会进这里扣蓝。刀已经由实体在挥，这里不要再刷一把剑。
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

    /**
     * 松手 / 没蓝 / 时间到：让当前这一刀收完再淡出，不要立刻删实体。
     */
    @Override
    public void onServerCastComplete(
            Level level,
            int spellLevel,
            LivingEntity entity,
            MagicData playerMagicData,
            boolean cancelled
    ) {
        if (playerMagicData.getAdditionalCastData() instanceof CarianSlicerCastData castData) {
            CarianSlicerEntity slicerEntity = castData.slicerEntity();
            if (slicerEntity != null && !slicerEntity.isRemoved()) {
                slicerEntity.requestStop();
            }
            castData.bindSlicerEntity(null);
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    private float getSlashDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * SLASH_DAMAGE_PER_SPELL_POWER;
    }
}

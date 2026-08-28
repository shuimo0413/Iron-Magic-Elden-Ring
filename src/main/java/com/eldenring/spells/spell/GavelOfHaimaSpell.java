package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GavelOfHaimaEntity;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.sigil.AcademySigilFx;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * 海摩大槌（Gavel of Haima）：右手召唤竖直辉石巨锤，单手抡下砸地。
 * <p>
 * {@link CastType#INSTANT} 出手后生成 {@link GavelOfHaimaEntity}（跟右手握点），
 * 玩家手臂播一次性单手挥击；锤子先竖在手里再向前砸，不做平躺抡起。
 */
public class GavelOfHaimaSpell extends EldenRingAbstractSpell {

    public static final int SPELL_MAX_LEVEL = 1;
    public static final double SPELL_COOLDOWN_SECONDS = 3.5;

    public static int SPELL_BASE_MANA_COST = 28;
    public static int SPELL_MANA_COST_PER_LEVEL = 4;
    public static int SPELL_BASE_SPELL_POWER = 16;
    public static int SPELL_SPELL_POWER_PER_LEVEL = 2;
    public static int SPELL_CAST_TIME_TICKS = 0;

    public static float DIRECT_HIT_DAMAGE_PER_SPELL_POWER = 1.15f;
    public static float SHOCKWAVE_DAMAGE_PER_SPELL_POWER = 0.92f;
    public static float DIRECT_HIT_RADIUS_BLOCKS = 1.45f;
    public static float SHOCKWAVE_RADIUS_BLOCKS = 3.6f;
    public static double DIRECT_HIT_KNOCKBACK_STRENGTH = 0.48;
    public static double SHOCKWAVE_KNOCKBACK_STRENGTH = 0.32;

    /** 注册 ID：{@code elden_ring_spells:gavel_of_haima}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "gavel_of_haima");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SPELL_MAX_LEVEL)
            .setCooldownSeconds(SPELL_COOLDOWN_SECONDS)
            .build();

    public GavelOfHaimaSpell() {
        this.manaCostPerLevel = SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = SPELL_CAST_TIME_TICKS;
        this.baseManaCost = SPELL_BASE_MANA_COST;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDirectHitDamage(spellLevel, caster), 2)
                ),
                Component.translatable(
                        "ui.elden_ring_spells.shockwave_damage",
                        Utils.stringTruncation(getShockwaveDamage(spellLevel, caster), 2)
                ),
                Component.translatable(
                        "ui.irons_spellbooks.radius",
                        Utils.stringTruncation(SHOCKWAVE_RADIUS_BLOCKS, 1)
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
     * 大槌没有飞弹。砸地音由 {@code GavelOfHaimaFx} 自己播，不要套射出音。
     */
    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    /**
     * 单手挥击。不要用双手 {@link SpellAnimations#OVERHEAD_MELEE_SWING_ANIMATION}，
     * 也不要用持续 {@link SpellAnimations#ANIMATION_CONTINUOUS_OVERHEAD}（会卡住放不下）。
     */
    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION;
    }

    @Override
    public Vector3f getTargetingColor() {
        return new Vector3f(0.20f, 0.88f, 1.0f);
    }

    /**
     * 服务端：头顶法阵 + 在施法者握点生成巨锤（实体自行跟手并砸地）。
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
            GavelOfHaimaEntity gavelEntity = new GavelOfHaimaEntity(
                    level,
                    castingEntity,
                    getDirectHitDamage(spellLevel, castingEntity),
                    getShockwaveDamage(spellLevel, castingEntity)
            );
            level.addFreshEntity(gavelEntity);
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDirectHitDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * DIRECT_HIT_DAMAGE_PER_SPELL_POWER;
    }

    private float getShockwaveDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * SHOCKWAVE_DAMAGE_PER_SPELL_POWER;
    }
}

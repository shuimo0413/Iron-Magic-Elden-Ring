package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GlintstoneStarVolleyEntity;
import com.eldenring.spells.particle.glintstone.GlintstoneFx;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.tuning.StarsOfRuinTuning;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * 毁灭流星（Stars of Ruin）：辉石连发线的顶点，瞬时施法。
 * <p>
 * 与辉石流星 / 流星雨同构：{@code onCast} 先在右手前方铺一团星云，再生成
 * {@link GlintstoneStarVolleyEntity}（{@code VolleyKind.STARS_OF_RUIN}）。
 * 不出头顶学院法阵：星云已经占满右侧头部空间，再叠法阵会糊成一团。
 * 发数改 {@link StarsOfRuinTuning}，星云布局改 {@link com.eldenring.spells.tuning.StarRiverTuning}。
 */
public class StarsOfRuinSpell extends AbstractSpell {

    /** 注册 ID：{@code elden_ring_spells:stars_of_ruin}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "stars_of_ruin");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(StarsOfRuinTuning.SPELL_MAX_LEVEL)
            .setCooldownSeconds(StarsOfRuinTuning.SPELL_COOLDOWN_SECONDS)
            .build();

    public StarsOfRuinSpell() {
        this.manaCostPerLevel = StarsOfRuinTuning.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = StarsOfRuinTuning.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = StarsOfRuinTuning.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = StarsOfRuinTuning.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = StarsOfRuinTuning.SPELL_BASE_MANA_COST;
    }

    /** 法术书：单发伤害 + {@code ×12}。 */
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamageAmountPerProjectile(spellLevel, caster), 2)
                ),
                Component.literal("×" + StarsOfRuinTuning.PROJECTILE_COUNT)
        );
    }

//    获取默认配置：法术书、法术等级上限、冷却时间
    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }


//    返回 INSTANT。铁魔法看到这个就不会蓄力，也不会走 onServerCastTick。点一下马上进 onCast。
    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

//    获取法术资源
    @Override
    public ResourceLocation getSpellResource() {
        return spellResourceLocation;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    /**
     * 12连发间隔短于原版受伤无敌帧，必须把 i-frame 清零，否则后几发会被吞成骗伤。
     */
    @Override
    public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setIFrames(0);
    }

    /**
     * 出手瞬间：先在右手前方铺一团星云，再挂上十二发齐射实体。
     * 不出 {@code AcademySigilFx}——星云已经占满右侧头部，再叠法阵会互相遮挡。
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
            GlintstoneFx.starRiver(
                    level,
                    castingEntity,
                    StarsOfRuinTuning.CAST_BURST_PARTICLE_INTENSITY,
                    true
            );
            GlintstoneStarVolleyEntity volleyEntity = new GlintstoneStarVolleyEntity(
                    level,
                    castingEntity,
                    GlintstoneStarVolleyEntity.VolleyKind.STARS_OF_RUIN,
                    getDamageAmountPerProjectile(spellLevel, castingEntity)
            );
            level.addFreshEntity(volleyEntity);
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDamageAmountPerProjectile(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity)
                * StarsOfRuinTuning.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

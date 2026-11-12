package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.MagicShieldEntity;
import com.eldenring.spells.registry.ModAttachments;
import com.eldenring.spells.registry.ModEffects;
import com.eldenring.spells.registry.ModSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.config.SpellConfigParameter;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import java.util.List;

/** 有限吸收护盾；重施刷新容量和时间，不叠加、不按法强无限成长。 */
public final class MagicShieldSpell extends EldenRingAbstractSpell {
    public static final int[] DURATION_TICKS = {400, 500, 600, 700, 800};
    public static final float[] CAPACITY = {4, 6, 8, 10, 12};
    public static final int[] MANA_COST = {20, 25, 35, 45, 60};
    private final DefaultConfig config = new DefaultConfig().setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE).setMaxLevel(5).setCooldownSeconds(25).build();

    public static int tier(int level) { return Math.max(1, Math.min(5, level)); }
    public static float capacity(int level) { return CAPACITY[tier(level) - 1]; }
    public static int duration(int level) { return DURATION_TICKS[tier(level) - 1]; }

    @Override public SpellRarity getRarity(int level) {
        return switch (tier(level)) {
            case 1 -> SpellRarity.COMMON;
            case 2 -> SpellRarity.UNCOMMON;
            case 3 -> SpellRarity.RARE;
            case 4 -> SpellRarity.EPIC;
            default -> SpellRarity.LEGENDARY;
        };
    }

    @Override public int getMinLevelForRarity(SpellRarity rarity) {
        if (rarity == SpellRarity.COMMON) return 1;
        if (rarity == SpellRarity.UNCOMMON) return 2;
        if (rarity == SpellRarity.RARE) return 3;
        if (rarity == SpellRarity.EPIC) return 4;
        if (rarity == SpellRarity.LEGENDARY) return 5;
        return 0;
    }

    public MagicShieldSpell() {
        baseManaCost = MANA_COST[0];
        manaCostPerLevel = 0;
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        castTime = 0;
    }

    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public int getManaCost(int level) {
        return (int) (MANA_COST[tier(level) - 1] * SpellConfigManager.getSpellConfigValue(this, SpellConfigParameter.MANA_MULTIPLIER));
    }
    /** 避免通用随机卷轴池绕过本模组奖励箱的品质分层。 */
    @Override public boolean allowLooting() { return false; }
    @Override public CastType getCastType() { return CastType.INSTANT; }
    @Override public ResourceLocation getSpellResource() {
        return ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "magic_shield");
    }
    @Override public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        return List.of(Component.translatable("ui.iss_elden_ring.magic_shield_capacity", capacity(level)),
                Component.translatable("ui.iss_elden_ring.magic_shield_duration", duration(level) / 20));
    }

    @Override public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        if (!caster.hasEffect(ModEffects.MAGIC_SHIELD)) return true;
        var shield = caster.getData(ModAttachments.MAGIC_SHIELD).entity;
        return shield == null || shield.isRemoved() || shield.level() != level || shield.spellLevel() <= tier(spellLevel);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (!level.isClientSide) {
            if (!checkPreCastConditions(level, spellLevel, caster, data)) return;
            var state = caster.getData(ModAttachments.MAGIC_SHIELD);
            MagicShieldEntity shield = state.entity;
            if (shield == null || shield.isRemoved() || shield.level() != level) {
                shield = new MagicShieldEntity(level, caster);
                // Do not grant protection if another mod rejects the entity spawn.
                if (!level.addFreshEntity(shield)) {
                    super.onCast(level, spellLevel, caster, source, data);
                    return;
                }
                state.entity = shield;
            }
            caster.removeEffect(ModEffects.MAGIC_SHIELD);
            if (caster.addEffect(new MobEffectInstance(ModEffects.MAGIC_SHIELD, duration(spellLevel), tier(spellLevel) - 1, false, false, true))) {
                shield.refresh(spellLevel);
            } else {
                shield.discard();
                state.entity = null;
            }
        }
        super.onCast(level, spellLevel, caster, source, data);
    }
}

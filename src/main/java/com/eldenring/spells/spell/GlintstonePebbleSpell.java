package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GlintstonePebbleProjectile;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.sigil.AcademySigilFx;
import com.eldenring.spells.tuning.GlintstonePebbleTuning;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * 辉石魔砾（Glintstone Pebble）——本模组最基础的辉石咒，也是读其它法术类的样板。
 * <p>
 * 对应法环「可移动、可连发」的短吟唱弹道：{@link CastType#INSTANT}，吟唱 tick 为 0。
 * 效果：从眼睛沿视线射出一发 {@link GlintstonePebbleProjectile}，带<strong>限角</strong>追踪
 * （不是强锁：侧移仍能躲开）。
 * <p>
 * 蓝耗、冷却、伤害系数、弹速、转向角全部在 {@link GlintstonePebbleTuning}。
 * 出手生成走 {@link GlintstoneCastHelper}，不要在本类里直接 {@code addFreshEntity}。
 * <p>
 * 铁魔法 AbstractSpell 的字段 / 回调约定见本包 {@code package-info.java}。
 */
public class GlintstonePebbleSpell extends AbstractSpell {

    /**
     * 法术注册 ID：{@code elden_ring_spells:glintstone_pebble}。
     * path 必须同时对上语言键 {@code spell.elden_ring_spells.glintstone_pebble}
     * 和图标 {@code textures/gui/spell_icons/glintstone_pebble.png}。
     */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "glintstone_pebble");

    /**
     * 铁魔法默认配置（可被服务端 irons 配置文件覆盖）。
     * 稀有度 / 学派 / 最高等级 / 冷却秒数都从这里进游戏。
     */
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(GlintstonePebbleTuning.SPELL_MAX_LEVEL)
            .setCooldownSeconds(GlintstonePebbleTuning.SPELL_COOLDOWN_SECONDS)
            .build();

    /**
     * 构造时写入 AbstractSpell 的平衡字段。铁魔法用它们算蓝耗和 {@link #getSpellPower}。
     * <ul>
     *   <li>{@code baseManaCost}：1 级蓝耗</li>
     *   <li>{@code manaCostPerLevel}：每升 1 级额外蓝耗</li>
     *   <li>{@code baseSpellPower} / {@code spellPowerPerLevel}：法术强度，再乘 Tuning 伤害系数才是实际伤害</li>
     *   <li>{@code castTime}：蓄力 tick；瞬时法术为 0</li>
     * </ul>
     */
    public GlintstonePebbleSpell() {
        this.manaCostPerLevel = GlintstonePebbleTuning.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = GlintstonePebbleTuning.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = GlintstonePebbleTuning.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = GlintstonePebbleTuning.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = GlintstonePebbleTuning.SPELL_BASE_MANA_COST;
    }

    /**
     * 法术书 / HUD 额外行。这里只显示估算伤害；铁魔法会自己拼蓝耗、冷却。
     */
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamageAmount(spellLevel, caster), 2)
                )
        );
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    /**
     * {@link CastType#INSTANT}：按下施法即完成，不进入蓄力条。
     * 需要蓄力的法术（毁灭流星、魔法之境）改用 {@link CastType#LONG}。
     */
    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellResourceLocation;
    }

    /**
     * 出手完成音。辉石系统一用水晶铃，和学院法阵视觉配套。
     */
    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    /**
     * 施法完成回调（双端都会进）。
     * 服务端：头顶学院法阵 + 沿视线生成魔砾弹道。
     * 末尾 {@code super.onCast} 不能省，铁魔法靠它收尾音效和内部状态。
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
            GlintstoneCastHelper.spawnAlongLook(
                    level,
                    castingEntity,
                    GlintstonePebbleProjectile::new,
                    GlintstonePebbleTuning.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                    GlintstonePebbleTuning.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS,
                    1.0f,
                    getDamageAmount(spellLevel, castingEntity),
                    castingEntity.getLookAngle(),
                    true
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    /**
     * 实际命中伤害 = 铁魔法法术强度 × Tuning 系数。
     * {@link #getSpellPower} 已含等级、装备、魔法之境等全局加成。
     */
    private float getDamageAmount(int spellLevel, LivingEntity castingEntity) {
        return getSpellPower(spellLevel, castingEntity) * GlintstonePebbleTuning.SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

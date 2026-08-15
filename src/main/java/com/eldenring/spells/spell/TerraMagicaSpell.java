package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.TerraMagicaZoneEntity;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.tuning.TerraMagicaTuning;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * 魔法之境（Terra Magica）：长吟唱后在脚下铺开学院徽记法阵。
 * <p>
 * 这不是弹道法术。蓄力结束生成 {@link TerraMagicaZoneEntity}（铁魔法圆形区域实体），
 * 站在阵内的施法者与队友获得全局法术强度加成（铁魔法 {@code SPELL_POWER} 属性），
 * 本模组辉石咒和铁魔法本体法术都吃得到。
 * <p>
 * 多座法阵共用同一 MobEffect ID，<strong>绝不叠层</strong>：站在两座阵里也只有一份 +30%。
 * 同一施法者再放一次会先丢掉自己的旧阵，避免地上留一串。
 * 半径 / 时长 / 加成倍率改 {@link TerraMagicaTuning}。
 */
public class TerraMagicaSpell extends AbstractSpell {

    /** 注册 ID：{@code elden_ring_spells:terra_magica}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "terra_magica");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(TerraMagicaTuning.SPELL_MAX_LEVEL)
            .setCooldownSeconds(TerraMagicaTuning.SPELL_COOLDOWN_SECONDS)
            .build();

    public TerraMagicaSpell() {
        this.manaCostPerLevel = TerraMagicaTuning.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = TerraMagicaTuning.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = TerraMagicaTuning.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = TerraMagicaTuning.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = TerraMagicaTuning.SPELL_BASE_MANA_COST;
    }

    /**
     * 法术书三行：强度加成百分比、法阵半径（方块）、持续时间。
     * 加成与半径不随等级变；时长 = 1 级基准 + 每级额外 tick。
     */
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.elden_ring_spells.spell_power_bonus_percent",
                        Utils.stringTruncation(TerraMagicaTuning.SPELL_POWER_BONUS_MULTIPLIED_TOTAL * 100.0, 0)
                ),
                Component.translatable(
                        "ui.irons_spellbooks.radius",
                        Utils.stringTruncation(TerraMagicaTuning.ZONE_RADIUS_BLOCKS, 1)
                ),
                Component.translatable(
                        "ui.irons_spellbooks.duration",
                        Utils.timeFromTicks(getZoneDurationTicks(spellLevel), 1)
                )
        );
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    /**
     * 长吟唱：举手蓄力结束后才铺阵。中途松手取消，地上不会留下半成品法阵。
     */
    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellResourceLocation;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    /** 完成音用水晶共鸣，和「法阵落地」比出手铃更沉一点。 */
    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_RESONATE);
    }

    /** 双手举过头顶的持续动画，对应法环里往地面铺徽记的姿势。 */
    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_OVERHEAD;
    }

    /**
     * 铁魔法瞄准指示器颜色（RGB 0–1）。学院辉石青，和法阵贴图一致。
     */
    @Override
    public Vector3f getTargetingColor() {
        return new Vector3f(0.24f, 0.91f, 0.94f);
    }

    /**
     * 服务端：先丢掉自己的旧法阵，再把新阵贴到脚下地面。
     * 不出头顶学院法阵——地面已经有完整徽记，再叠一层会重复。
     * <p>
     * {@code moveToRelativeGroundLevel} 会沿竖直方向找最近可站立面，
     * 避免在半空 / 坡上把法阵悬空或埋进方块。
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
            discardOwnedZones(level, castingEntity);

            Vec3 spawnPosition = Utils.moveToRelativeGroundLevel(
                    level,
                    castingEntity.position(),
                    TerraMagicaTuning.ZONE_GROUND_SNAP_MAX_STEPS
            ).add(0.0, TerraMagicaTuning.ZONE_SPAWN_Y_OFFSET_BLOCKS, 0.0);
            int durationTicks = getZoneDurationTicks(spellLevel);
            float radiusBlocks = TerraMagicaTuning.ZONE_RADIUS_BLOCKS;

            TerraMagicaZoneEntity zoneEntity = new TerraMagicaZoneEntity(level);
            zoneEntity.setOwner(castingEntity);
            zoneEntity.setCircular();
            zoneEntity.setRadius(radiusBlocks);
            zoneEntity.setDuration(durationTicks);
            zoneEntity.setReapplicationDelay(TerraMagicaTuning.ZONE_REAPPLICATION_DELAY_TICKS);
            zoneEntity.setPos(spawnPosition);
            level.addFreshEntity(zoneEntity);
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    /**
     * 法阵持续时间（tick）：1 级用基准值，之后每级加 {@link TerraMagicaTuning#ZONE_DURATION_TICKS_PER_LEVEL}。
     * {@code spellLevel - 1} 再 {@code max(0, …)}，避免 0 级或异常等级算出负数。
     */
    private int getZoneDurationTicks(int spellLevel) {
        return TerraMagicaTuning.ZONE_BASE_DURATION_TICKS
                + Math.max(0, spellLevel - 1) * TerraMagicaTuning.ZONE_DURATION_TICKS_PER_LEVEL;
    }

    /**
     * 丢弃该施法者拥有的全部魔法之境（再施放时替换旧阵）。
     * 只在施法者周围 64 格 AABB 内搜，避免全图扫描；法阵本来也不会离主人那么远。
     */
    private static void discardOwnedZones(Level level, LivingEntity owner) {
        double searchPaddingBlocks = 64.0;
        AABB searchBox = owner.getBoundingBox().inflate(searchPaddingBlocks);
        for (TerraMagicaZoneEntity existingZone : level.getEntitiesOfClass(TerraMagicaZoneEntity.class, searchBox)) {
            if (existingZone.getOwner() == owner) {
                existingZone.discard();
            }
        }
    }
}

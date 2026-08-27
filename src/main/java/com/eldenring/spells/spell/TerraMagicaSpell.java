package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.TerraMagicaZoneEntity;
import com.eldenring.spells.registry.ModSchools;
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
 * 半径 / 时长 / 加成倍率改 {@link TerraMagicaSpell}。
 */
public class TerraMagicaSpell extends EldenRingAbstractSpell {

    // —— 玩法/视觉数字（toml 只覆盖玩法字段）——
    // -------------------------------------------------------------------------
        // 法术数值（TerraMagicaSpell）
        // -------------------------------------------------------------------------

        /** 1 级基础法力消耗。 */
        public static int SPELL_BASE_MANA_COST = 40;

        /** 每升一级额外法力消耗。 */
        public static int SPELL_MANA_COST_PER_LEVEL = 5;

        /**
         * 1 级基础法术强度。本法定死 +30% 伤害，不参与伤害公式；
         * 仍保留字段以便铁魔法 UI / 等级曲线有合法 power 输入。
         */
        public static int SPELL_BASE_SPELL_POWER = 1;

        /** 每升一级额外法术强度（本法定死加成，不参与伤害）。 */
        public static int SPELL_SPELL_POWER_PER_LEVEL = 0;

        /**
         * 吟唱时间（tick）。大于 0 → {@code CastType.LONG}，给落阵一点仪式感。
         * 调大 → 更易被打断；调小 → 更接近瞬发。
         */
        public static int SPELL_CAST_TIME_TICKS = 16;

        /** 冷却时间（秒）。 */
        public static double SPELL_COOLDOWN_SECONDS = 25.0;

        /** 最大等级。法环辉石咒固定 1 级；持续时间只用 {@link #ZONE_BASE_DURATION_TICKS}。 */
        public static int SPELL_MAX_LEVEL = 1;

        /**
         * 每升一级额外的法阵持续时间（tick）。
         * 总时长 = {@link #ZONE_BASE_DURATION_TICKS} + (level - 1) * 本值。
         */
        public static int ZONE_DURATION_TICKS_PER_LEVEL = 40;

        // -------------------------------------------------------------------------
        // 法阵区域（TerraMagicaZoneEntity）
        // -------------------------------------------------------------------------

        /**
         * 法阵半径（方块）。边长视觉四边形约为 {@code 2 * radius}。
         * 调大 → 覆盖更宽、更易站进；调小 → 站桩要求更严。
         */
        public static float ZONE_RADIUS_BLOCKS = 4.5f;

        /**
         * 1 级法阵持续时间（tick）。20 tick = 1 秒；600 = 30 秒（贴近法环原作）。
         */
        public static int ZONE_BASE_DURATION_TICKS = 600;

        /**
         * 每隔多少 tick 扫描一次阵内友方并刷新效果。
         * 10 = 每 0.5 秒；调小更跟手但略增开销。
         */
        public static int ZONE_REAPPLICATION_DELAY_TICKS = 10;

        /**
         * 施加到目标上的效果剩余时间（tick）。
         * 应略大于 {@link #ZONE_REAPPLICATION_DELAY_TICKS}，离开法阵后约 1 秒掉 buff。
         */
        public static int EFFECT_REFRESH_DURATION_TICKS = 20;

        /**
         * 全局法术强度加成（乘算，{@code ADD_MULTIPLIED_TOTAL}）。
         * 0.30 → 默认 1.0 的 SPELL_POWER 变为 1.30（+30%）。全等级固定。
         */
        public static double SPELL_POWER_BONUS_MULTIPLIED_TOTAL = 0.30D;

        /**
         * 贴地搜索：相对施法者脚底，最多向上 / 向下找地面的方块数。
         */
        public static int ZONE_GROUND_SNAP_MAX_STEPS = 6;

        /**
         * 贴地后整体再抬高的距离（方块）。0.5 = 半格，避免徽记埋进地面。
         */
        public static double ZONE_SPAWN_Y_OFFSET_BLOCKS = 0.3;

        // -------------------------------------------------------------------------
        // 视觉 / 粒子
        // -------------------------------------------------------------------------

        /**
         * 法阵贴图相对实体原点再抬高（方块），减轻 z-fighting。
         * 调大 → 徽记更「浮」；调小 → 更贴地但易闪烁。
         */
        public static float SIGIL_Y_OFFSET_BLOCKS = 0.05f;

        /**
         * 徽记绕 Y 轴自转角速度（度 / tick）。
         * 0 = 静止（当前需求）；调大可恢复缓慢自转。
         */
        public static float SIGIL_SPIN_DEGREES_PER_TICK = 0.0f;

        /**
         * 徽记整体不透明度（0–1）。贴图本身已有透明度，此值再乘一层。
         */
        public static float SIGIL_OPACITY = 0.92f;

        /**
         * 客户端环境粒子密度基准（再乘半径做 clamp）。
         * 调大 → 整片法阵升起的辉石微粒更多。
         */
        public static float ZONE_AMBIENT_PARTICLE_COUNT = 2.4f;

        /**
         * 粒子散布半径相对法阵半径的比例（1 = 铺满到边沿）。
         * 略小于 1 可避免粒子刚好卡在碰撞箱外缘。
         */
        public static float ZONE_AMBIENT_FILL_RADIUS_FRACTION = 0.98f;

        /**
         * 法阵中心临时光源亮度（0–15）。
         * 使用原版 {@code Blocks.LIGHT}；仅在目标格为空气时放置，消散时若仍是我们的光则清除。
         */
        public static int ZONE_CENTER_LIGHT_LEVEL = 12;

    /** 注册 ID：{@code elden_ring_spells:terra_magica}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "terra_magica");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(TerraMagicaSpell.SPELL_MAX_LEVEL)
            .setCooldownSeconds(TerraMagicaSpell.SPELL_COOLDOWN_SECONDS)
            .build();

    public TerraMagicaSpell() {
        this.manaCostPerLevel = TerraMagicaSpell.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = TerraMagicaSpell.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = TerraMagicaSpell.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = TerraMagicaSpell.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = TerraMagicaSpell.SPELL_BASE_MANA_COST;
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
                        Utils.stringTruncation(TerraMagicaSpell.SPELL_POWER_BONUS_MULTIPLIED_TOTAL * 100.0, 0)
                ),
                Component.translatable(
                        "ui.irons_spellbooks.radius",
                        Utils.stringTruncation(TerraMagicaSpell.ZONE_RADIUS_BLOCKS, 1)
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
                    TerraMagicaSpell.ZONE_GROUND_SNAP_MAX_STEPS
            ).add(0.0, TerraMagicaSpell.ZONE_SPAWN_Y_OFFSET_BLOCKS, 0.0);
            int durationTicks = getZoneDurationTicks(spellLevel);
            float radiusBlocks = TerraMagicaSpell.ZONE_RADIUS_BLOCKS;

            TerraMagicaZoneEntity zoneEntity = new TerraMagicaZoneEntity(level);
            zoneEntity.setOwner(castingEntity);
            zoneEntity.setCircular();
            zoneEntity.setRadius(radiusBlocks);
            zoneEntity.setDuration(durationTicks);
            zoneEntity.setReapplicationDelay(TerraMagicaSpell.ZONE_REAPPLICATION_DELAY_TICKS);
            zoneEntity.setPos(spawnPosition);
            level.addFreshEntity(zoneEntity);
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    /**
     * 法阵持续时间（tick）：1 级用基准值，之后每级加 {@link TerraMagicaSpell#ZONE_DURATION_TICKS_PER_LEVEL}。
     * {@code spellLevel - 1} 再 {@code max(0, …)}，避免 0 级或异常等级算出负数。
     */
    private int getZoneDurationTicks(int spellLevel) {
        return TerraMagicaSpell.ZONE_BASE_DURATION_TICKS
                + Math.max(0, spellLevel - 1) * TerraMagicaSpell.ZONE_DURATION_TICKS_PER_LEVEL;
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

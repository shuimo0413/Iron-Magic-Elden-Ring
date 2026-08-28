package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.CannonOfHaimaProjectile;
import com.eldenring.spells.entity.GlintstoneTrailStyle;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.registry.ModSounds;
import com.eldenring.spells.sigil.AcademySigilFx;
import com.eldenring.spells.spell.fx.CannonOfHaimaFx;
import com.eldenring.spells.spell.helper.GlintstoneCastHelper;
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
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * 海摩炮弹（Cannon of Haima）：蓄力 1 秒后抛出不追踪的青色辉石炮弹。
 * <p>
 * {@link CastType#LONG} 期间只做前摇粒子；蓄力结束才生成 {@link CannonOfHaimaProjectile}。
 * 炮弹受重力走抛物线，落地或碰到敌人立刻范围爆炸。
 */
public class CannonOfHaimaSpell extends EldenRingAbstractSpell {

    public static final int SPELL_MAX_LEVEL = 1;
    public static final double SPELL_COOLDOWN_SECONDS = 4.5;

    /** 1 级蓝耗。高于大槌：远程范围弹更贵。 */
    public static int SPELL_BASE_MANA_COST = 36;

    /** 每升 1 级额外蓝耗。 */
    public static int SPELL_MANA_COST_PER_LEVEL = 4;

    /** 1 级法术强度基数。 */
    public static int SPELL_BASE_SPELL_POWER = 18;

    /** 每级额外法术强度。 */
    public static int SPELL_SPELL_POWER_PER_LEVEL = 2;

    /**
     * 蓄力时长（tick）。20 = 1 秒，对应法环海摩炮弹前摇。
     * 调大 → 更容易被打断；调小 → 更接近瞬发。
     */
    public static int SPELL_CAST_TIME_TICKS = 20;

    /**
     * 爆炸伤害 = 法强 × 本系数。
     * 调大 → 单发清群更痛；应对标大槌冲击波之上、帚星之下。
     */
    public static float DAMAGE_PER_SPELL_POWER = 1.22f;

    /**
     * 爆炸半径（方块）。范围内所有敌对生物受伤。
     * 调大 → 更像原作炮弹坑；调小 → 更偏点杀。
     */
    public static float EXPLOSION_RADIUS_BLOCKS = 4.8f;

    /**
     * 出手初速（方块/tick）。重力每 tick 往下拉，速度越大抛物线越平、射程越远。
     */
    public static float PROJECTILE_FLIGHT_SPEED = 1.32f;

    /**
     * 爆炸击退强度。调大 → 敌人被掀得更开。
     */
    public static double EXPLOSION_KNOCKBACK_STRENGTH = 0.52;

    /**
     * 相对眼睛沿视线前移（方块）。炮弹体积大，略往前避免嵌进玩家。
     */
    public static double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.55;

    /** 施法爆发粒子相对生成点再前移（方块）。 */
    public static double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.35;

    public static float CAST_BURST_PARTICLE_INTENSITY = 2.15f;

    /**
     * 重力加速度（方块/tick²）。铁魔法弹默认 0.05；略大一点让弧线一眼能读出来。
     * 调大 → 砸得更近、弧更弯；调小 → 更像平射彗星。
     */
    public static final double PROJECTILE_GRAVITY_BLOCKS_PER_TICK_SQUARED = 0.058;

    /** 命中判定相对目标箱子的外扩（方块）。 */
    public static final float HIT_DETECTION_INFLATION_BLOCKS = 0.35f;

    /**
     * 炮弹实体寿命（tick）。超时未落地则销毁，避免飞出世界。
     */
    public static final int ENTITY_LIFETIME_TICKS = 120;

    // —— 视觉（写死，不进 toml）——

    /** 炮弹立方体整体缩放。约 1 格直径的辉石实心球。 */
    public static final float CANNONBALL_RENDER_SCALE = 1.15f;

    /** 炮弹本体自发光青。 */
    public static final int CANNONBALL_BODY_COLOR_ARGB = 0xC000E8E0;

    /** 炮弹高光棱面。 */
    public static final int CANNONBALL_FACET_COLOR_ARGB = 0xD830FFF4;

    /** 炮弹核芯更亮的一层。 */
    public static final int CANNONBALL_CORE_COLOR_ARGB = 0xE060FFF8;

    /** 光晕。 */
    public static final int CANNONBALL_GLOW_COLOR_ARGB = 0x9900E8D8;

    /** 绕飞行轴自旋（度/tick）。让棱面在抛物线上闪。 */
    public static final float CANNONBALL_SPIN_DEGREES_PER_TICK = 7.5f;

    /**
     * 抛物线光轨：直接用帚星那套扫帚彗尾（宽长尾 + 外雾层 + 加法芯 + 五条螺旋细丝），
     * 否则单层光带在弧线上会显得干瘪。
     */
    public static final GlintstoneTrailStyle TRAIL_STYLE = new GlintstoneTrailStyle(
            64.0,
            0.280f,
            0.055f,
            0.14f,
            0.05f,
            80,
            new GlintstoneTrailStyle.HelixStyle(5, 0.42f, 0.16f, 0.055f, 0.16f, 0.07f),
            true,
            true
    );

    /** 飞行点缀粒子强度。 */
    public static final float TRAIL_PARTICLE_INTENSITY = 0.85f;

    /** 爆炸粒子强度（交给 Fx，再叠烟雾与碎片）。 */
    public static final float IMPACT_PARTICLE_INTENSITY = 3.05f;

    /** 注册 ID：{@code elden_ring_spells:cannon_of_haima}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "cannon_of_haima");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SPELL_MAX_LEVEL)
            .setCooldownSeconds(SPELL_COOLDOWN_SECONDS)
            .build();

    public CannonOfHaimaSpell() {
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
                        Utils.stringTruncation(getDamageAmount(spellLevel, caster), 2)
                ),
                Component.translatable(
                        "ui.irons_spellbooks.radius",
                        Utils.stringTruncation(EXPLOSION_RADIUS_BLOCKS, 1)
                )
        );
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    /**
     * 长吟唱：举手蓄力 1 秒后才出膛。中途松手取消，不会留下半成品炮弹。
     */
    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellResourceLocation;
    }

    /**
     * 蓄力起手音。炮弹真正抛出时再走基类的飞弹射出音。
     */
    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(ModSounds.SPELL_CAST_START.get());
    }

    /** 双手举过头顶蓄力，对应法环里把炮弹凝在杖头再抛出。 */
    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_OVERHEAD;
    }

    @Override
    public Vector3f getTargetingColor() {
        return new Vector3f(0.20f, 0.88f, 1.0f);
    }

    /**
     * 蓄力期间在杖头附近收束青色辉石，让 1 秒前摇能被看见。
     */
    @Override
    public void onServerCastTick(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        if (!level.isClientSide) {
            CannonOfHaimaFx.spawnChargeGathering(level, entity);
        }
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
    }

    /**
     * 服务端：头顶法阵 + 沿视线抛出受重力的炮弹。
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
                    CannonOfHaimaProjectile::new,
                    PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                    SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS,
                    CAST_BURST_PARTICLE_INTENSITY,
                    getDamageAmount(spellLevel, castingEntity),
                    castingEntity.getLookAngle(),
                    true
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    private float getDamageAmount(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * DAMAGE_PER_SPELL_POWER;
    }
}

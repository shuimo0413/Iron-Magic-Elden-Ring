package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.LorettaGreatbowProjectile;
import com.eldenring.spells.entity.GlintstoneTrailStyle;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.registry.ModSounds;
import com.eldenring.spells.spell.fx.LorettaGreatbowFx;
import com.eldenring.spells.spell.helper.GlintstoneCastHelper;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * 罗蕾塔的大弓（Loretta's Greatbow）：卡利亚魔法。拉弓蓄力 2 秒后射出一发高速追踪的蓝色彗星箭。
 * <p>
 * {@link CastType#LONG}：蓄力期间播铁魔法「魔法箭」的拉弓动作 {@link SpellAnimations#BOW_CHARGE_ANIMATION}，
 * 弓手前方汇聚卡利亚粒子；蓄力满才在 {@link #onCast} 生成 {@link LorettaGreatbowProjectile}。中途松手取消、不出弹。
 * <p>
 * 弹头 / 光轨复用辉石彗星（{@code GlintstoneProjectileRenderer}），颜色比辉石彗星更偏深蓝；
 * 拖尾点缀与命中粒子走卡利亚库 {@code CarianFx}。追踪走 {@code AbstractGlintstoneProjectile} 的公共限角锥形追踪。
 */
public class LorettaGreatbowSpell extends EldenRingAbstractSpell {

    // —— 玩法（默认值种子；运行时被 EldenRingServerConfig 的 toml 覆盖）——

    /**
     * 飞行速度（方块/tick）。高于帚星（1.4），体现「大弓」的高速箭。
     * 调大 → 更难躲、但追踪转弯半径变大，近处更容易擦过目标。
     */
    public static float PROJECTILE_FLIGHT_SPEED = 2.2f;

    /** 索敌距离（方块）。调大 → 远处目标也会被锁。 */
    public static double PROJECTILE_TRACKING_RANGE_BLOCKS = 32.0;

    /**
     * 最大射程（方块，按飞行路径长度）。飞满后直接消失（不爆炸）。
     * 须 ≤ 300 tick × 弹速（铁魔法硬寿命），默认 2.2 × 300 = 660 格，留足余量。
     * 调大 → 远程狙击更稳；调小 → 落空的箭更早消失。
     */
    public static double PROJECTILE_MAX_RANGE_BLOCKS = 220.0;

    /**
     * 每 tick 最大转向角（度/tick）。速度高，需要比彗星（2.3）更大的转向才咬得住目标。
     * 调大 → 追踪更黏；调小 → 更像直射。
     */
    public static float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 3.0f;

    /**
     * 命中爆炸半径（方块）。0 = 单体命中（原作大弓无爆炸）。整合包可在 toml 改成正数变范围伤害。
     */
    public static float EXPLOSION_RADIUS_BLOCKS = 0.0f;

    /** 1 级蓝耗（数值表：25）。 */
    public static int SPELL_BASE_MANA_COST = 25;

    /** 每升 1 级额外蓝耗（数值表：2，满级 7 级为 37）。 */
    public static int SPELL_MANA_COST_PER_LEVEL = 2;

    /** 1 级攻击力（数值表：10）。 */
    public static float SPELL_BASE_SPELL_POWER = 10;

    /** 每级额外攻击力（数值表：1.5，满级 7 级为 19）。 */
    public static float SPELL_SPELL_POWER_PER_LEVEL = 1.5f;

    /**
     * 蓄力时长（tick）。40 = 2 秒拉弓。
     * 调大 → 更容易被打断；调小 → 更接近瞬发。
     */
    public static int SPELL_CAST_TIME_TICKS = 40;

    /** 命中伤害 = 数值表攻击力 × 本系数（再乘铁魔法法强加成）。 */
    public static float SPELL_DAMAGE_PER_SPELL_POWER = 1.0f;

    /** 冷却（秒，数值表：3）；运行时可被铁魔法 JSON 覆盖。 */
    public static final double SPELL_COOLDOWN_SECONDS = 3.0;

    /** 最大等级（数值表：7）；运行时可被铁魔法 JSON 覆盖。 */
    public static final int SPELL_MAX_LEVEL = 7;

    // —— 追踪细节（写死，不进 toml）——

    /** 出手后多少 tick 才开始追踪（tick）。速度快，略早于彗星（5）开始修正。 */
    public static final int PROJECTILE_TRACKING_START_DELAY_TICKS = 3;

    /** 索敌锥半角（度）。比彗星（34）窄一点：大弓偏向「瞄哪打哪」。 */
    public static final float PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES = 30.0f;

    /** 生成点相对眼睛沿视线前移（方块）。 */
    public static final double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.6;

    /** 低于此速度不再做追踪（方块/tick），防止除零。 */
    public static final double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;

    /** 当前方向与目标方向夹角小于此值（弧度）就视为已对准，不再插值。 */
    public static final double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

    /** 施法爆发粒子相对生成点再前移（方块）；当前由 GlintstoneCastHelper 总闸关闭，仅保留参数。 */
    public static final double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.8;

    // —— 视觉（写死，不进 toml）——

    /** 彗星头横向缩放（相对模型）。与辉石彗星同档，读成一发箭头大小的彗星。 */
    public static final float COMET_HEAD_BODY_SCALE_RADIAL = 0.62f;

    /** 彗星头沿飞行方向缩放。比辉石彗星（1.70）更长，读成「箭」。 */
    public static final float COMET_HEAD_BODY_SCALE_ALONG = 2.10f;

    /** 外层光晕整体缩放。 */
    public static final float COMET_HEAD_GLOW_SCALE = 1.30f;

    /** 外层光晕沿飞行方向额外拉长倍率。 */
    public static final float COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE = 2.40f;

    /** 光晕呼吸幅度（0–1）。 */
    public static final float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.14f;

    /** 光晕自旋速度（度/tick）。 */
    public static final float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 15.0f;

    /** 核芯颜色（RGB 0–1）：卡利亚深蓝，比辉石彗星少绿。 */
    public static final float COMET_HEAD_CORE_RED = 0.14f;
    public static final float COMET_HEAD_CORE_GREEN = 0.46f;
    public static final float COMET_HEAD_CORE_BLUE = 1.0f;

    /** 光晕颜色（RGB 0–1）。 */
    public static final float COMET_HEAD_GLOW_RED = 0.16f;
    public static final float COMET_HEAD_GLOW_GREEN = 0.42f;
    public static final float COMET_HEAD_GLOW_BLUE = 1.0f;
    public static final float COMET_HEAD_GLOW_ALPHA = 1.0f;

    /**
     * 光轨：参照辉石彗星（18 方块 / 36 点 + 双螺旋细丝）。速度更快，长度拉到 22 方块，免得尾巴看起来更短。
     * 参数顺序见 {@link GlintstoneTrailStyle}。
     */
    public static final GlintstoneTrailStyle TRAIL_STYLE = new GlintstoneTrailStyle(
            22.0,
            0.140f,
            0.030f,
            0.16f,
            0.06f,
            40,
            new GlintstoneTrailStyle.HelixStyle(2, 0.16f, 0.07f, 0.042f, 0.22f, 0.10f),
            true,
            false
    );

    /** 飞行点缀粒子（卡利亚库）强度倍率。 */
    public static final float TRAIL_PARTICLE_INTENSITY = 0.75f;

    /** 命中爆裂粒子（卡利亚库）强度倍率。 */
    public static final float IMPACT_PARTICLE_INTENSITY = 1.8f;

    /** 出手瞬间弓前爆闪强度倍率。 */
    public static final float RELEASE_BURST_PARTICLE_INTENSITY = 1.4f;

    /** 注册 ID：{@code iss_elden_ring:loretta_greatbow}。 */
    private final ResourceLocation spellResourceLocation =
            new ResourceLocation(EldenRingSpellsMod.MOD_ID, "loretta_greatbow");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SPELL_MAX_LEVEL)
            .setCooldownSeconds(SPELL_COOLDOWN_SECONDS)
            .build();

    public LorettaGreatbowSpell() {
        this.baseManaCost = SPELL_BASE_MANA_COST;
        this.manaCostPerLevel = SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = Math.round(SPELL_BASE_SPELL_POWER);
        this.spellPowerPerLevel = Math.round(SPELL_SPELL_POWER_PER_LEVEL);
        this.castTime = SPELL_CAST_TIME_TICKS;
    }

    /** 法术书：单发伤害；配置了爆炸半径时再显示半径。 */
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        MutableComponent damageLine = Component.translatable(
                "ui.irons_spellbooks.damage",
                Utils.stringTruncation(getDamageAmount(spellLevel, caster), 2)
        );
        if (EXPLOSION_RADIUS_BLOCKS <= 0.0f) {
            return List.of(damageLine);
        }
        return List.of(
                damageLine,
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

    /** 长吟唱：拉弓 2 秒后才放箭；中途松手取消。 */
    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellResourceLocation;
    }

    /** 蓄力起手音；放箭瞬间走基类的飞弹射出音。 */
    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(ModSounds.SPELL_CAST_START.get());
    }

    /** 铁魔法「魔法箭」同款拉弓动作。 */
    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.BOW_CHARGE_ANIMATION;
    }

    /** 放箭后不再接收招动作，避免拉弓姿势被挥手动作打断成两段。 */
    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public Vector3f getTargetingColor() {
        return new Vector3f(0.20f, 0.45f, 1.0f);
    }

    /** 蓄力每 tick：弓手前方汇聚卡利亚粒子，越接近满弦越密。 */
    @Override
    public void onServerCastTick(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        if (!level.isClientSide) {
            LorettaGreatbowFx.chargeGather(level, entity, chargeProgress(playerMagicData));
        }
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
    }

    /** 服务端：弓前爆闪 + 沿视线射出追踪彗星箭。 */
    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity castingEntity,
            CastSource castSource,
            MagicData playerMagicData
    ) {
        if (!level.isClientSide) {
            LorettaGreatbowFx.releaseBurst(level, castingEntity, RELEASE_BURST_PARTICLE_INTENSITY);
            GlintstoneCastHelper.spawnAlongLook(
                    level,
                    castingEntity,
                    LorettaGreatbowProjectile::new,
                    PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                    SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS,
                    RELEASE_BURST_PARTICLE_INTENSITY,
                    getDamageAmount(spellLevel, castingEntity),
                    castingEntity.getLookAngle(),
                    true
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    /**
     * 蓄力进度 [0,1]。非玩家施法者拿不到 MagicData 时按满弦处理。
     */
    private static float chargeProgress(@Nullable MagicData playerMagicData) {
        if (playerMagicData == null || playerMagicData.getCastDuration() <= 0) {
            return 1.0f;
        }
        int elapsedCastTicks = playerMagicData.getCastDuration() - playerMagicData.getCastDurationRemaining();
        return Mth.clamp(elapsedCastTicks / (float) playerMagicData.getCastDuration(), 0.0f, 1.0f);
    }

    private float getDamageAmount(int spellLevel, LivingEntity castingEntity) {
        return damageFromTableAttack(
                SPELL_BASE_SPELL_POWER,
                SPELL_SPELL_POWER_PER_LEVEL,
                spellLevel,
                castingEntity,
                SPELL_DAMAGE_PER_SPELL_POWER
        );
    }
}

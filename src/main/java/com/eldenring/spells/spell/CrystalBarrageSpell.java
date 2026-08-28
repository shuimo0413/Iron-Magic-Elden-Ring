package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.GlintstoneTrailStyle;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.registry.ModSounds;
import com.eldenring.spells.sigil.AcademySigilFx;
import com.eldenring.spells.spell.data.CrystalBarrageCastData;
import com.eldenring.spells.spell.helper.CometAzurCasting;
import com.eldenring.spells.spell.helper.CrystalBarrageCasting;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 结晶连弹（Crystal Barrage）。
 * <p>
 * {@link CastType#CONTINUOUS}：按住施法键连续散射辉石碎片；松开立刻停刷（客户端发 CancelCast）。
 * 碎片抄迅魔砾彗星头 / 光轨，但不追踪，直线飞，射程短；撞敌或飞满射程会碎裂消失。
 * 地面走动、下落时不能起手；起手后钉死站位（参考彗星亚兹勒），视线仍可转。
 */
public class CrystalBarrageSpell extends EldenRingAbstractSpell {

    /**
     * 最大等级种子。运行时以铁魔法 JSON 为准。法环辉石咒固定 1 级。
     */
    public static final int SPELL_MAX_LEVEL = 1;

    /**
     * 冷却（秒）。连射本身已经靠按住维持，松手后再按要有一点空窗。
     * 调大 → 更难立刻再开；调小 → 更接近原作「几乎无 CD」。
     */
    public static final double SPELL_COOLDOWN_SECONDS = 0.8;

    /**
     * 1 级基础法力消耗。CONTINUOUS 约每 10 tick 进 {@link #onCast} 扣一次。
     * 调大 → 按住连射更吃蓝。
     */
    public static int SPELL_BASE_MANA_COST = 8;

    /** 每升一级额外法力消耗。当前定死 1 级。 */
    public static int SPELL_MANA_COST_PER_LEVEL = 2;

    /**
     * 1 级基础法术强度。单片伤害 = {@link #getSpellPower} × {@link #SPELL_DAMAGE_PER_SPELL_POWER}。
     */
    public static int SPELL_BASE_SPELL_POWER = 8;

    /** 每升一级额外法术强度。当前定死 1 级。 */
    public static int SPELL_SPELL_POWER_PER_LEVEL = 1;

    /**
     * 按住最长持续时间（tick）。20 tick = 1 秒；160 ≈ 8 秒。
     * CONTINUOUS 用它当「一口气能射多久」的上限。
     */
    public static int SPELL_CAST_TIME_TICKS = 160;

    /**
     * 单片伤害系数：最终伤害 = 法术强度 × 本值。
     * 调大 → 单片更疼，连射 DPS 一起涨。连射很密，默认比迅魔砾单发低。
     */
    public static float SPELL_DAMAGE_PER_SPELL_POWER = 0.22f;

    /**
     * 碎片飞行速度（方块/tick）。比迅魔砾略慢，短射程里还能看清散射。
     * 调大 → 更难侧移躲开，扇面也更快铺开。
     */
    public static float PROJECTILE_FLIGHT_SPEED = 1.20f;

    /**
     * 直线最大射程（方块）。超过就碎裂消失。调大 → 能打到更远；调小 → 必须贴身扫。
     */
    public static double PROJECTILE_MAX_RANGE_BLOCKS = 12.0;

    /**
     * 相对视线的散射锥半角（度）。左右合计约 28° 的扇面。
     * 调大 → 更散、更不容易叠成一条线；调小 → 更像窄束。
     */
    public static float SCATTER_HALF_ANGLE_DEGREES = 14.0f;

    /**
     * 相邻两发碎片间隔（tick）。1 = 每 tick 一发；2 = 每秒约 10 发。
     * 调大 → 连射更疏、更省实体；调小 → 更密。
     */
    public static int SHARD_SPAWN_INTERVAL_TICKS = 2;

    /**
     * 生成点沿视线前移（方块）。太小容易嵌进玩家自己，太大容易穿墙。
     */
    public static double PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.35;

    /**
     * 出手闪光相对生成点再沿视线前移（方块）。只给本段吟唱第一发用。
     */
    public static double SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS = 0.45;

    /**
     * 地面走动判定：水平速度超过这个值（方块/tick）就拒绝起手。
     * 站立抖动远小于此；步行 / 冲刺会超过。
     */
    public static double CAST_REFUSE_HORIZONTAL_SPEED_BLOCKS_PER_TICK = 0.08;

    public static double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;
    public static double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

    // -------------------------------------------------------------------------
    // 彗星头视觉（从迅魔砾抄过来：细亮针状）
    // -------------------------------------------------------------------------

    /** 径向缩小，读成细针而不是小圆石。 */
    public static float COMET_HEAD_BODY_SCALE_RADIAL = 0.22f;

    /** 沿飞行轴拉长。调大 → 更像示踪弹。 */
    public static float COMET_HEAD_BODY_SCALE_ALONG = 0.70f;

    public static float COMET_HEAD_GLOW_SCALE = 0.42f;
    public static float COMET_HEAD_GLOW_ALONG_FLIGHT_SCALE = 1.85f;
    public static float COMET_HEAD_GLOW_PULSE_AMPLITUDE = 0.06f;
    public static float COMET_HEAD_GLOW_SPIN_DEGREES_PER_TICK = 26.0f;

    public static float COMET_HEAD_CORE_RED = 0.20f;
    public static float COMET_HEAD_CORE_GREEN = 0.92f;
    public static float COMET_HEAD_CORE_BLUE = 1.0f;

    public static float COMET_HEAD_GLOW_RED = 0.18f;
    public static float COMET_HEAD_GLOW_GREEN = 0.95f;
    public static float COMET_HEAD_GLOW_BLUE = 1.0f;
    public static float COMET_HEAD_GLOW_ALPHA = 1.0f;

    /**
     * 迅魔砾同款高速细亮示踪线，无螺旋细丝。
     */
    public static GlintstoneTrailStyle TRAIL_STYLE =
            new GlintstoneTrailStyle(14.0, 0.028f, 0.006f, 0.12f, 0.04f, 36);

    /** 拖尾点缀强度倍率；不影响几何光束长宽。连射时要克制。 */
    public static float TRAIL_PARTICLE_INTENSITY = 0.28f;
    public static float IMPACT_PARTICLE_INTENSITY = 1.05f;
    public static float CAST_BURST_PARTICLE_INTENSITY = 0.45f;

    /** 注册 ID：{@code elden_ring_spells:crystal_barrage}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "crystal_barrage");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SPELL_MAX_LEVEL)
            .setCooldownSeconds(SPELL_COOLDOWN_SECONDS)
            .build();

    public CrystalBarrageSpell() {
        this.manaCostPerLevel = SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = SPELL_CAST_TIME_TICKS;
        this.baseManaCost = SPELL_BASE_MANA_COST;
    }

    /**
     * 伤害源把原版受伤无敌帧打成 0 tick。
     * 不打的话，连射第二片会落在第一片的 i-frame 里，实际伤害接近 0。
     */
    @Override
    public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setIFrames(0);
    }

    /**
     * 法术书三行：单片伤害、直线射程、「按住连射」。
     */
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getShardDamage(spellLevel, caster), 2)
                ),
                Component.translatable(
                        "ui.elden_ring_spells.projectile_range",
                        Utils.stringTruncation(PROJECTILE_MAX_RANGE_BLOCKS, 1)
                ),
                Component.translatable("ui.elden_ring_spells.hold_to_barrage")
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

    /**
     * 连弹没有蓄力前摇，起手就是第一波碎片飞出，所以接飞弹射出音而不是蓄力音。
     */
    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(ModSounds.SPELL_CAST.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_OVERHEAD;
    }

    /**
     * 下落、地面走动时拒绝起手；创造飞行 / 站立可以。起手后由锁位钉死。
     */
    @Override
    public boolean checkPreCastConditions(
            Level level,
            int spellLevel,
            LivingEntity entity,
            MagicData playerMagicData
    ) {
        if (CometAzurCasting.isCasterFalling(entity)) {
            sendRefuseMessage(entity, "ui.elden_ring_spells.crystal_barrage_cannot_cast_falling");
            return false;
        }
        if (CrystalBarrageCasting.isCasterWalkingOnGround(entity)) {
            sendRefuseMessage(entity, "ui.elden_ring_spells.crystal_barrage_cannot_cast_moving");
            return false;
        }
        return true;
    }

    private static void sendRefuseMessage(LivingEntity entity, String translationKey) {
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable(translationKey).withStyle(ChatFormatting.RED)
            ));
        }
    }

    /**
     * 刚按下：记下脚底、刷头顶点缀。碎片从 {@link #onServerCastTick} 开始刷，这里不要 new 弹。
     */
    @Override
    public void onServerPreCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        if (!level.isClientSide && playerMagicData != null) {
            CrystalBarrageCastData castData = new CrystalBarrageCastData(entity.position());
            playerMagicData.setAdditionalCastData(castData);
            CrystalBarrageCasting.applyCasterPositionLock(entity, castData);
            AcademySigilFx.spawnAboveHead(level, entity);
        }
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
    }

    /**
     * 每个吟唱 tick：钉死站位，按间隔刷一发散射碎片。
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
        if (!(playerMagicData.getAdditionalCastData() instanceof CrystalBarrageCastData castData)) {
            return;
        }
        CrystalBarrageCasting.applyCasterPositionLock(entity, castData);
        if (!castData.tryConsumeSpawnInterval()) {
            return;
        }
        CrystalBarrageCasting.spawnScatterShard(
                level,
                entity,
                getShardDamage(spellLevel, entity),
                castData.tryMarkFirstShardSpawned()
        );
    }

    /**
     * CONTINUOUS 约每 10 tick 进这里，铁魔法在 {@code super.onCast} 里扣蓝。
     * 碎片已经由 {@link #onServerCastTick} 在刷，这里不要再 {@code addFreshEntity}。
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

    /** 当前等级下单片碎片命中伤害。 */
    public float getShardDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * SPELL_DAMAGE_PER_SPELL_POWER;
    }
}

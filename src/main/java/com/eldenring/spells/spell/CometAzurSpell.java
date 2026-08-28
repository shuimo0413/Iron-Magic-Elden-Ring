package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.particle.cometazur.CometAzurFx;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.registry.ModSounds;
import com.eldenring.spells.spell.data.CometAzurCastData;
import com.eldenring.spells.spell.helper.CometAzurCasting;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
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
 * 彗星亚兹勒（Comet Azur）。
 * <p>
 * {@link CastType#CONTINUOUS}：按住施法键维持喷流；松开立刻取消（客户端发 CancelCast）。
 * 施法键含卷轴/法杖右键、魔法书施法键（默认 V，跟玩家改键）、快捷施法。
 * 魔法书点按起手时键会先弹起，不能把「没按住」立刻当成松手，否则蓄力一闪就停。
 * 铁魔法本体的 CONTINUOUS 默认会一直喷到时间/蓝耗尽，所以本类额外监听松手。
 * 地面、跳跃上升、创造飞行可以起手；正在下落则拒绝（否则会被钉在半空）。
 * 整段吟唱锁死移动与视角；前 {@link #STARTUP_DURATION_TICKS} tick 蓄力，随后喷流沿出手朝向直线延伸。
 */
public class CometAzurSpell extends EldenRingAbstractSpell {

    public static final int SPELL_MAX_LEVEL = 5;
    public static final double SPELL_COOLDOWN_SECONDS = 1.2;

    public static int SPELL_BASE_MANA_COST = 10;
    public static int SPELL_MANA_COST_PER_LEVEL = 2;
    public static int SPELL_BASE_SPELL_POWER = 8;
    public static int SPELL_SPELL_POWER_PER_LEVEL = 2;
    public static int SPELL_CAST_TIME_TICKS = 400;

    /** 蓄力漩涡时长（tick）。40 = 2 秒。 */
    public static int STARTUP_DURATION_TICKS = 40;
    /** 喷流最大射程（方块）。 */
    public static double JET_BEAM_MAX_RANGE_BLOCKS = 60.0;
    /** 喷流伤害圆柱半径（方块）。 */
    public static float JET_BEAM_DAMAGE_RADIUS_BLOCKS = 1.28f;
    /** 喷流伤害结算间隔（tick）。 */
    public static int JET_BEAM_DAMAGE_INTERVAL_TICKS = 4;
    /** 每次喷流结算伤害 = 法强 × 本系数。 */
    public static float JET_BEAM_DAMAGE_PER_SPELL_POWER = 0.55f;

    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "comet_azur");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SPELL_MAX_LEVEL)
            .setCooldownSeconds(SPELL_COOLDOWN_SECONDS)
            .build();

    public CometAzurSpell() {
        this.baseManaCost = SPELL_BASE_MANA_COST;
        this.manaCostPerLevel = SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = SPELL_CAST_TIME_TICKS;
    }

    @Override
    public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setIFrames(0);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        getDamageText(spellLevel, caster)
                )
        );
    }

    private String getDamageText(int spellLevel, LivingEntity caster) {
        return String.format("%.1f", getDamage(spellLevel, caster));
    }

    public float getDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * JET_BEAM_DAMAGE_PER_SPELL_POWER;
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
     * 前两秒蓄力用起手音；喷流真正打出时在 {@link #onServerCastTick} 里另播飞弹射出音。
     */
    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(ModSounds.SPELL_CAST_START.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    /**
     * 地面、跳跃上升、创造飞行可以起手；正在下落则拒绝，否则会被钉在半空。
     */
    @Override
    public boolean checkPreCastConditions(
            Level level,
            int spellLevel,
            LivingEntity entity,
            MagicData playerMagicData
    ) {
        if (!CometAzurCasting.isCasterFalling(entity)) {
            return true;
        }
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.elden_ring_spells.comet_azur_cannot_cast_falling")
                            .withStyle(ChatFormatting.RED)
            ));
        }
        return false;
    }

    @Override
    public void onServerPreCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        if (!level.isClientSide && playerMagicData != null) {
            CometAzurCastData castData = new CometAzurCastData(
                    entity.position(),
                    CometAzurFx.vortexCenterInFrontOf(entity),
                    CometAzurFx.jetMouthInFrontOf(entity),
                    entity.getYRot(),
                    entity.getXRot()
            );
            playerMagicData.setAdditionalCastData(castData);
            CometAzurCasting.applyCasterLock(entity, castData);
            CometAzurFx.spawnStartupVortex(level, entity);
        }
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
    }

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
        if (!(playerMagicData.getAdditionalCastData() instanceof CometAzurCastData castData)) {
            return;
        }

        // 蓄力阶段也锁：不能边蓄力边走位 / 扭头。
        CometAzurCasting.applyCasterLock(entity, castData);

        int elapsedCastTicks = playerMagicData.getCastDuration() - playerMagicData.getCastDurationRemaining();
        if (elapsedCastTicks < STARTUP_DURATION_TICKS) {
            return;
        }
        if (castData.tryMarkChargeShockwaveSpawned()) {
            CometAzurFx.spawnChargeShockwave(level, castData);
            ModSounds.playProjectileLaunch(level, entity);
        }
        CometAzurCasting.ensureJetEntity(level, entity, castData, getDamage(spellLevel, entity), spellLevel);
        int jetElapsedTicks = elapsedCastTicks - STARTUP_DURATION_TICKS;
        if (jetElapsedTicks % CometAzurFx.JET_SURROUND_SPAWN_INTERVAL_TICKS == 0) {
            CometAzurFx.spawnJetSurround(level, castData);
        }
    }

    /**
     * 松手 / 没蓝 / 时间到：立刻拆掉喷流实体。铁魔法随后会 {@code reset()} 再拆一次也安全。
     */
    @Override
    public void onServerCastComplete(
            Level level,
            int spellLevel,
            LivingEntity entity,
            MagicData playerMagicData,
            boolean cancelled
    ) {
        if (playerMagicData.getAdditionalCastData() instanceof CometAzurCastData castData) {
            CometAzurCasting.discardJet(castData);
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }
}

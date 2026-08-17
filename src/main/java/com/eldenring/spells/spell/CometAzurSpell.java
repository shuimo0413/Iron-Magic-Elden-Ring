package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.CometAzurJetEntity;
import com.eldenring.spells.particle.cometazur.CometAzurFx;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.tuning.CometAzurTuning;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 彗星亚兹勒（Comet Azur）。
 * <p>
 * {@link CastType#CONTINUOUS}：按住右键维持喷流；松开立刻取消（客户端发 CancelCast）。
 * 铁魔法本体的 CONTINUOUS 默认会一直喷到时间/蓝耗尽，所以本类额外监听松手。
 * 地面、跳跃上升、创造飞行可以起手；正在下落则拒绝（否则会被钉在半空）。
 * 整段吟唱锁死移动与视角；前 {@link CometAzurTuning#STARTUP_DURATION_TICKS} tick 蓄力，随后喷流沿出手朝向直线延伸。
 */
public class CometAzurSpell extends AbstractSpell {

    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "comet_azur");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(CometAzurTuning.SPELL_MAX_LEVEL)
            .setCooldownSeconds(CometAzurTuning.SPELL_COOLDOWN_SECONDS)
            .build();

    public CometAzurSpell() {
        this.baseManaCost = CometAzurTuning.SPELL_BASE_MANA_COST;
        this.manaCostPerLevel = CometAzurTuning.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = CometAzurTuning.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = CometAzurTuning.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = CometAzurTuning.SPELL_CAST_TIME_TICKS;
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
        return getSpellPower(spellLevel, caster) * CometAzurTuning.JET_BEAM_DAMAGE_PER_SPELL_POWER;
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

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
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
        if (!isCasterFalling(entity)) {
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

    /**
     * 竖直速度明显朝下，或已经积了坠落距离，就算下落。
     * 站地 / 攀爬 / 水中 / 创造飞行不算；鞘翅滑翔算下落。
     */
    private static boolean isCasterFalling(LivingEntity entity) {
        if (entity.onGround() || entity.onClimbable() || entity.isInWater() || entity.isPassenger()) {
            return false;
        }
        if (entity instanceof Player player && player.getAbilities().flying) {
            return false;
        }
        if (entity.isFallFlying()) {
            return true;
        }
        return entity.getDeltaMovement().y < CometAzurTuning.CAST_FALLING_Y_VELOCITY_THRESHOLD_BLOCKS_PER_TICK
                || entity.fallDistance > CometAzurTuning.CAST_FALLING_MIN_DISTANCE_BLOCKS;
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
            applyCasterLock(entity, castData);
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
        applyCasterLock(entity, castData);

        int elapsedCastTicks = playerMagicData.getCastDuration() - playerMagicData.getCastDurationRemaining();
        if (elapsedCastTicks < CometAzurTuning.STARTUP_DURATION_TICKS) {
            return;
        }
        if (castData.tryMarkChargeShockwaveSpawned()) {
            CometAzurFx.spawnChargeShockwave(level, castData);
        }
        ensureJetEntity(level, spellLevel, entity, castData);
        int jetElapsedTicks = elapsedCastTicks - CometAzurTuning.STARTUP_DURATION_TICKS;
        if (jetElapsedTicks % CometAzurTuning.JET_SURROUND_SPAWN_INTERVAL_TICKS == 0) {
            CometAzurFx.spawnJetSurround(level, castData);
        }
    }

    /**
     * 把施法者钉在出手脚底，清零速度，强制 yaw/pitch（含头/身）。
     */
    private static void applyCasterLock(LivingEntity entity, CometAzurCastData castData) {
        Vec3 feet = castData.lockedFeetPosition();
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
        entity.setPos(feet.x, feet.y, feet.z);
        entity.setYRot(castData.yawDegrees());
        entity.setXRot(castData.pitchDegrees());
        entity.yRotO = castData.yawDegrees();
        entity.xRotO = castData.pitchDegrees();
        entity.yHeadRot = castData.yawDegrees();
        entity.yBodyRot = castData.yawDegrees();
        if (entity instanceof Player player) {
            player.yHeadRotO = castData.yawDegrees();
            player.yBodyRotO = castData.yawDegrees();
        }
    }

    private void ensureJetEntity(
            Level level,
            int spellLevel,
            LivingEntity caster,
            CometAzurCastData castData
    ) {
        float damagePerHit = getDamage(spellLevel, caster);
        CometAzurJetEntity existingJet = castData.jetEntity();
        if (existingJet == null || existingJet.isRemoved()) {
            CometAzurJetEntity jetEntity = new CometAzurJetEntity(
                    level,
                    caster,
                    castData,
                    damagePerHit,
                    spellLevel
            );
            level.addFreshEntity(jetEntity);
            castData.bindJetEntity(jetEntity);
            return;
        }
        existingJet.refreshWhileCasting(damagePerHit, spellLevel);
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
            CometAzurJetEntity jetEntity = castData.jetEntity();
            if (jetEntity != null && !jetEntity.isRemoved()) {
                jetEntity.discard();
            }
            castData.bindJetEntity(null);
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }
}

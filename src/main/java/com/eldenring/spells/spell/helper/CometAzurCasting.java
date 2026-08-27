package com.eldenring.spells.spell.helper;

import com.eldenring.spells.entity.CometAzurJetEntity;
import com.eldenring.spells.particle.cometazur.CometAzurFx;
import com.eldenring.spells.spell.data.CometAzurCastData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 彗星亚兹勒施法期辅助：下落判定、锁死施法者、补刷喷流实体。
 * <p>
 * {@code CometAzurSpell} 只保留铁魔法生命周期回调；这些细节不进 Spell 本体。
 */
public final class CometAzurCasting {

    private CometAzurCasting() {
    }

    /**
     * 竖直速度明显朝下，或已经积了坠落距离，就算下落。
     * 站地 / 攀爬 / 水中 / 创造飞行不算；鞘翅滑翔算下落。
     */
    public static boolean isCasterFalling(LivingEntity entity) {
        if (entity.onGround() || entity.onClimbable() || entity.isInWater() || entity.isPassenger()) {
            return false;
        }
        if (entity instanceof Player player && player.getAbilities().flying) {
            return false;
        }
        if (entity.isFallFlying()) {
            return true;
        }
        return entity.getDeltaMovement().y < CometAzurFx.CAST_FALLING_Y_VELOCITY_THRESHOLD_BLOCKS_PER_TICK
                || entity.fallDistance > CometAzurFx.CAST_FALLING_MIN_DISTANCE_BLOCKS;
    }

    /**
     * 把施法者钉在出手脚底，清零速度，强制 yaw/pitch（含头/身）。
     */
    public static void applyCasterLock(LivingEntity entity, CometAzurCastData castData) {
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

    /**
     * 喷流实体丢了就补刷；还在就刷新伤害，避免蓄力结束后空窗。
     */
    public static void ensureJetEntity(
            Level level,
            LivingEntity caster,
            CometAzurCastData castData,
            float damagePerHit,
            int spellLevel
    ) {
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
     * 松手 / 没蓝 / 时间到：立刻拆掉喷流。铁魔法随后 {@code reset()} 再拆一次也安全。
     */
    public static void discardJet(CometAzurCastData castData) {
        CometAzurJetEntity jetEntity = castData.jetEntity();
        if (jetEntity != null && !jetEntity.isRemoved()) {
            jetEntity.discard();
        }
        castData.bindJetEntity(null);
    }
}

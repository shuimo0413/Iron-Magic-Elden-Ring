package com.eldenring.spells.spell.helper;

import com.eldenring.spells.entity.CometAzurJetEntity;
import com.eldenring.spells.spell.data.CometAzurCastData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 彗星亚兹勒施法期辅助：起手支撑判定、锁死施法者、补刷喷流实体。
 * <p>
 * {@code CometAzurSpell} 只保留铁魔法生命周期回调；这些细节不进 Spell 本体。
 */
public final class CometAzurCasting {

    private CometAzurCasting() {
    }

    /**
     * 无支撑腾空（跳跃上升 / 下落 / 鞘翅滑翔 / 被击飞）时禁止起手，否则锁位会钉在半空。
     * 站地 / 攀爬 / 水中 / 乘骑 / 主动飞行（创造或多数模组的 {@code abilities.flying}）允许。
     * 不用 {@code mayfly}：那只表示「能开飞」，落地后也可能仍为 true。
     */
    public static boolean isUnsupportedAirborne(LivingEntity entity) {
        if (entity.onGround() || entity.onClimbable() || entity.isInWater() || entity.isPassenger()) {
            return false;
        }
        if (entity instanceof Player player && player.getAbilities().flying) {
            return false;
        }
        return true;
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

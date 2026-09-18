package com.eldenring.spells.spell.helper;

import com.eldenring.spells.entity.CometAzurJetEntity;
import com.eldenring.spells.particle.cometazur.CometAzurFx;
import com.eldenring.spells.spell.data.CometAzurCastData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * 彗星亚兹勒施法期辅助：更新实时施法状态并补刷喷流实体。
 * <p>
 * {@code CometAzurSpell} 只保留铁魔法生命周期回调；这些细节不进 Spell 本体。
 */
public final class CometAzurCasting {

    private CometAzurCasting() {
    }

    /** 每个施法 tick 按施法者当前位置和视线刷新喷流锚点。 */
    public static void updateCastState(LivingEntity entity, CometAzurCastData castData) {
        castData.updateAim(
                CometAzurFx.vortexCenterInFrontOf(entity),
                CometAzurFx.jetMouthInFrontOf(entity),
                entity.getYRot(),
                entity.getXRot()
        );
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
        existingJet.refreshWhileCasting(castData, damagePerHit, spellLevel);
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

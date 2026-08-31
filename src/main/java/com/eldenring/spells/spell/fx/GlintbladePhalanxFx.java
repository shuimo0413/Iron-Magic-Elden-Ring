package com.eldenring.spells.spell.fx;

import com.eldenring.spells.entity.PhalanxGlintbladeEntity;
import com.eldenring.spells.registry.ModParticles;
import com.eldenring.spells.spell.curve.GlintbladePhalanxCastCurve;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 辉剑圆阵特效：跟手时沿刃身点缀，不要铺魔法辉剑那种整盘漩涡（五把会糊成一团）。
 * 射出音仍走 {@link MagicGlintbladeFx#playLaunchSounds}。密度写死。
 */
public final class GlintbladePhalanxFx {

    /**
     * 出现段沿剑身每 tick 采样点数。调大 → 成型更实，五把同时更吃粒子。
     */
    private static final int APPEAR_BLADE_SAMPLE_COUNT = 2;

    private GlintbladePhalanxFx() {
    }

    /**
     * 客户端跟手 tick：出现时沿刃撒卡利亚火星，成型后偶尔闪一下。
     */
    public static void tickWhileOrbiting(PhalanxGlintbladeEntity bladeEntity, Level level) {
        if (!level.isClientSide) {
            return;
        }
        Vec3 bladeCenter = bladeEntity.position();
        Vec3 bladeDirection = bladeEntity.hoverBladeTipWorldDirection();
        float swordScale = GlintbladePhalanxCastCurve.swordScale(bladeEntity.tickCount);
        if (swordScale <= 0.04f) {
            return;
        }
        double visibleBladeLengthBlocks = 0.50 * swordScale * bladeEntity.swordVisualScale();
        if (swordScale < 1.0f) {
            for (int sampleIndex = 0; sampleIndex < APPEAR_BLADE_SAMPLE_COUNT; sampleIndex++) {
                double alongBlade = (sampleIndex + 0.4) / APPEAR_BLADE_SAMPLE_COUNT * visibleBladeLengthBlocks;
                Vec3 samplePosition = bladeCenter.add(bladeDirection.scale(alongBlade));
                if (level.random.nextFloat() < 0.55f) {
                    level.addParticle(
                            ModParticles.CARIAN_MOTE.get(),
                            samplePosition.x,
                            samplePosition.y,
                            samplePosition.z,
                            (level.random.nextDouble() - 0.5) * 0.015,
                            (level.random.nextDouble() - 0.5) * 0.015,
                            (level.random.nextDouble() - 0.5) * 0.015
                    );
                }
            }
            return;
        }
        if (level.random.nextFloat() < 0.12f) {
            Vec3 sparkPosition = bladeCenter.add(bladeDirection.scale(visibleBladeLengthBlocks * 0.7));
            level.addParticle(
                    ModParticles.CARIAN_GLINT.get(),
                    sparkPosition.x,
                    sparkPosition.y,
                    sparkPosition.z,
                    bladeDirection.x * 0.015,
                    bladeDirection.y * 0.015,
                    bladeDirection.z * 0.015
            );
        }
    }
}

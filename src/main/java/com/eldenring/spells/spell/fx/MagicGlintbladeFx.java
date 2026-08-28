package com.eldenring.spells.spell.fx;

import com.eldenring.spells.entity.MagicGlintbladeEntity;
import com.eldenring.spells.particle.carian.CarianFx;
import com.eldenring.spells.registry.ModParticles;
import com.eldenring.spells.registry.ModSounds;
import com.eldenring.spells.spell.MagicGlintbladeSpell;
import com.eldenring.spells.spell.curve.MagicGlintbladeCastCurve;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 魔法辉剑特效：起手对数螺旋漩涡、盘面凝结点缀、射出音效。密度写死，不进 toml。
 * 粒子走卡利亚深蓝库，不要套青辉石。
 */
public final class MagicGlintbladeFx {

    /**
     * 客户端蓄势路径强度。略大于 1 会多铺几条相位曲线，漩涡更接近圆。
     */
    private static final float PATH_VORTEX_INTENSITY = 1.2f;

    /**
     * 凝结段沿剑身每 tick 采样点数。调大 → 剑更像从漩涡里长出来。
     */
    private static final int CONDENSE_BLADE_SAMPLE_COUNT = 3;

    private MagicGlintbladeFx() {
    }

    /**
     * 出手瞬间播蓄力起手音。漩涡路径由客户端每 tick 按函数画，不在服务端砸一盘贴图。
     */
    public static void spawnOpeningVortex(Level level, Vec3 vortexCenter, Vec3 facing) {
        ModSounds.playCastStart(level, vortexCenter);
    }

    /**
     * 客户端蓄势 tick：按对数螺线取点画漩涡，再沿正在长出来的平躺剑身点缀。
     */
    public static void tickBeforeLaunch(MagicGlintbladeEntity glintbladeEntity, Level level) {
        if (!level.isClientSide) {
            return;
        }
        int hoverDurationTicks = MagicGlintbladeSpell.HOVER_DURATION_TICKS;
        int ageTicks = glintbladeEntity.tickCount;
        Vec3 vortexCenter = glintbladeEntity.vortexCenterWorld();
        Vec3 facing = glintbladeEntity.vortexFacing();

        CarianFx.logSpiralVortex(level, vortexCenter, facing, PATH_VORTEX_INTENSITY, ageTicks);

        float condenseProgress = MagicGlintbladeCastCurve.condenseProgress(ageTicks, hoverDurationTicks);
        if (condenseProgress <= 0.02f) {
            return;
        }
        spawnCondenseBladeParticles(level, vortexCenter, facing, ageTicks, hoverDurationTicks, condenseProgress);
    }

    /**
     * 凝结段：粒子贴在平躺刃上，从核往刃尖铺，看起来是剑从漩涡里长出来。
     */
    private static void spawnCondenseBladeParticles(
            Level level,
            Vec3 vortexCenter,
            Vec3 facing,
            int ageTicks,
            int hoverDurationTicks,
            float condenseProgress
    ) {
        Vec3 bladeDirection = MagicGlintbladeCastCurve.hoverBladeWorldDirection(facing);
        double visibleBladeLengthBlocks = MagicGlintbladeCastCurve.CONDENSE_BLADE_LENGTH_BLOCKS * condenseProgress;
        for (int sampleIndex = 0; sampleIndex < CONDENSE_BLADE_SAMPLE_COUNT; sampleIndex++) {
            double alongBlade = (sampleIndex + 0.35) / CONDENSE_BLADE_SAMPLE_COUNT * visibleBladeLengthBlocks;
            Vec3 samplePosition = vortexCenter.add(bladeDirection.scale(alongBlade));
            if (sampleIndex == 0) {
                level.addParticle(
                        ModParticles.CARIAN_GLOW.get(),
                        samplePosition.x,
                        samplePosition.y,
                        samplePosition.z,
                        0.0,
                        0.0,
                        0.0
                );
            } else if (level.random.nextFloat() < 0.65f) {
                level.addParticle(
                        ModParticles.CARIAN_MOTE.get(),
                        samplePosition.x,
                        samplePosition.y,
                        samplePosition.z,
                        (level.random.nextDouble() - 0.5) * 0.02,
                        (level.random.nextDouble() - 0.5) * 0.02,
                        (level.random.nextDouble() - 0.5) * 0.02
                );
            }
            if (level.random.nextFloat() < 0.28f * condenseProgress) {
                level.addParticle(
                        ModParticles.CARIAN_GLINT.get(),
                        samplePosition.x,
                        samplePosition.y,
                        samplePosition.z,
                        bladeDirection.x * 0.02,
                        bladeDirection.y * 0.02,
                        bladeDirection.z * 0.02
                );
            }
        }
        if (level.random.nextFloat() < 0.22f * condenseProgress) {
            Vec3 sparkPosition = vortexCenter.add(bladeDirection.scale(visibleBladeLengthBlocks * 0.85));
            level.addParticle(
                    ModParticles.CARIAN_SPARK.get(),
                    sparkPosition.x,
                    sparkPosition.y,
                    sparkPosition.z,
                    bladeDirection.x * 0.04,
                    bladeDirection.y * 0.04,
                    bladeDirection.z * 0.04
            );
        }
    }

    /**
     * 凝结完毕射出：播飞弹出手音。
     */
    public static void playLaunchSounds(Level level, Vec3 launchOrigin) {
        ModSounds.playProjectileLaunch(level, launchOrigin);
    }
}

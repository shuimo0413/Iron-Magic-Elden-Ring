package com.eldenring.spells.spell.curve;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 魔法辉剑时间轴与凝结姿态。全部写死，不进 toml。
 * <p>
 * 玩法上的「蓄势多久再飞」仍是 {@code MagicGlintbladeSpell.HOVER_DURATION_TICKS}。
 * 剑从出现起就平躺、刃尖对准出手瞬间朝向，只做从小到大，不再在盘面上竖起来转。
 */
public final class MagicGlintbladeCastCurve {

    /**
     * 蓄势前段只铺漩涡、还不画剑的比例（相对悬停总时长）。
     * 调大 → 先看清往里收的漩涡；调小 → 剑更早开始由小变大。
     */
    public static final float VORTEX_ONLY_FRACTION = 0.22f;

    /**
     * 凝结段最短时长（tick）。即使 toml 把悬停压得很短，也要看清从小到大。
     */
    public static final int MIN_CONDENSE_TICKS = 12;

    /**
     * 凝结段沿剑身撒粒子时，刃长估算（方块）。只影响特效采样，不改碰撞。
     */
    public static final double CONDENSE_BLADE_LENGTH_BLOCKS = 0.55;

    private MagicGlintbladeCastCurve() {
    }

    /**
     * 前段纯漩涡时长（tick）。其余时间都留给平躺凝结。
     */
    public static int vortexOnlyTicks(int hoverDurationTicks) {
        int availableTicks = Math.max(1, hoverDurationTicks - MIN_CONDENSE_TICKS);
        int desiredTicks = Math.round(hoverDurationTicks * VORTEX_ONLY_FRACTION);
        return Mth.clamp(desiredTicks, 1, availableTicks);
    }

    /**
     * 剑开始以极小尺寸平躺出现的 tick（含）。
     */
    public static int condenseStartTick(int hoverDurationTicks) {
        return vortexOnlyTicks(hoverDurationTicks);
    }

    public static boolean isVortexOnly(float ageTicks, int hoverDurationTicks) {
        return ageTicks < condenseStartTick(hoverDurationTicks);
    }

    public static boolean shouldLaunch(int ageTicks, int hoverDurationTicks) {
        return ageTicks >= hoverDurationTicks;
    }

    /**
     * 剑模型缩放 0–1。漩涡段为 0；之后二次缓出长到 1，满尺寸再射出。
     */
    public static float swordScale(float ageTicks, int hoverDurationTicks) {
        int condenseStart = condenseStartTick(hoverDurationTicks);
        if (ageTicks < condenseStart) {
            return 0.0f;
        }
        if (ageTicks >= hoverDurationTicks) {
            return 1.0f;
        }
        float condenseDurationTicks = Math.max(1.0f, hoverDurationTicks - condenseStart);
        float linear = Mth.clamp((ageTicks - condenseStart) / condenseDurationTicks, 0.0f, 1.0f);
        return 1.0f - (1.0f - linear) * (1.0f - linear);
    }

    /**
     * 凝结进度 0–1，给粒子强度用。
     */
    public static float condenseProgress(float ageTicks, int hoverDurationTicks) {
        return swordScale(ageTicks, hoverDurationTicks);
    }

    /**
     * 平躺刃尖朝向：出手瞬间的视线。剑从出现起就沿这个方向躺着由小变大，不再转到竖直。
     */
    public static Vec3 lyingBladeWorldDirection(Vec3 facing) {
        if (facing.lengthSqr() < 1.0e-8) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return facing.normalize();
    }

    /**
     * 蓄势阶段刃尖朝哪：与出手瞬间朝向相同。
     */
    public static Vec3 hoverBladeWorldDirection(Vec3 facing) {
        return lyingBladeWorldDirection(facing);
    }
}

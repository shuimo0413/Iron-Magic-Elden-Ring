package com.eldenring.spells.spell.fx;

import com.eldenring.spells.particle.gravity.GravityFx;
import com.eldenring.spells.spell.GravityBallSpell;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 重力球特效入口：只转发到 {@link GravityFx}，强度读 Spell 视觉字段。
 */
public final class GravityBallFx {

    private GravityBallFx() {
    }

    /**
     * 飞行拖尾点缀 + 可选对数螺线内吸漩涡。
     */
    public static void spawnTrail(Level level, Vec3 position, Vec3 motion) {
        GravityFx.trailAccents(
                level,
                position.x,
                position.y,
                position.z,
                motion,
                GravityBallSpell.TRAIL_PARTICLE_INTENSITY
        );
        if (level.isClientSide && level.random.nextFloat() < 0.55f) {
            Vec3 facing = motion.lengthSqr() > 1.0e-8 ? motion.normalize() : new Vec3(0.0, 1.0, 0.0);
            GravityFx.logSpiralVortex(
                    level,
                    position,
                    facing,
                    0.75f * GravityBallSpell.TRAIL_PARTICLE_INTENSITY
            );
        }
    }

    /**
     * 命中爆裂（仅服务端；会同步到附近客户端）。
     */
    public static void spawnImpact(Level level, Vec3 impactCenter) {
        GravityFx.impact(
                level,
                impactCenter.x,
                impactCenter.y,
                impactCenter.z,
                GravityBallSpell.IMPACT_PARTICLE_INTENSITY
        );
    }
}

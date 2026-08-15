package com.eldenring.spells.particle.foundingrain;

import com.eldenring.spells.particle.foundingrain.OverheadNebulaAccentOptions.Accent;
import com.eldenring.spells.tuning.FoundingRainOfStarsTuning;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 创星雨粒子助手。手里星云仍走 {@code GlintstoneFx.starRiver}；
 * 身前气团改由软光面片渲染，这里只负责白色小星星。
 */
public final class FoundingRainFx {

    private FoundingRainFx() {
    }

    /**
     * 星云圆心：视线水平前方 {@link FoundingRainOfStarsTuning#OVERHEAD_CLOUD_FORWARD_OFFSET_BLOCKS} 格，
     * 高度仍是眼高 + {@link FoundingRainOfStarsTuning#ASCENT_TARGET_HEIGHT_ABOVE_EYES_BLOCKS}。
     */
    public static Vec3 cloudCenterInFrontOf(LivingEntity caster) {
        Vec3 lookDirection = caster.getLookAngle();
        Vec3 horizontalForward = new Vec3(lookDirection.x, 0.0, lookDirection.z);
        if (horizontalForward.lengthSqr() < 1.0e-8) {
            float yawRadians = caster.getYRot() * Mth.DEG_TO_RAD;
            horizontalForward = new Vec3(-Math.sin(yawRadians), 0.0, Math.cos(yawRadians));
        } else {
            horizontalForward = horizontalForward.normalize();
        }
        return caster.getEyePosition()
                .add(horizontalForward.scale(FoundingRainOfStarsTuning.OVERHEAD_CLOUD_FORWARD_OFFSET_BLOCKS))
                .add(0.0, FoundingRainOfStarsTuning.ASCENT_TARGET_HEIGHT_ABOVE_EYES_BLOCKS, 0.0);
    }

    /**
     * 在星云带里撒白色小星星。气团本身不走粒子，避免方块边。
     *
     * @param cloudYawDegrees 施法瞬间偏航，星星沿横向条带散布
     */
    public static void spawnOverheadStars(Level level, Vec3 cloudCenter, float cloudYawDegrees) {
        float yawRadians = cloudYawDegrees * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(Math.cos(yawRadians), 0.0, Math.sin(yawRadians));
        Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0, Math.cos(yawRadians));
        int starCount = FoundingRainOfStarsTuning.OVERHEAD_STAR_COUNT;
        double halfWidthBlocks = FoundingRainOfStarsTuning.OVERHEAD_CLOUD_RADIUS_BLOCKS;
        double halfDepthBlocks = FoundingRainOfStarsTuning.OVERHEAD_CLOUD_FORWARD_HALF_BLOCKS;
        double halfHeightBlocks = FoundingRainOfStarsTuning.OVERHEAD_CLOUD_SHEET_THICKNESS_BLOCKS;
        for (int starIndex = 0; starIndex < starCount; starIndex++) {
            double alongRight = (level.random.nextDouble() * 2.0 - 1.0) * halfWidthBlocks;
            double alongForward = (level.random.nextDouble() * 2.0 - 1.0) * halfDepthBlocks;
            double alongUp = (level.random.nextDouble() * 2.0 - 1.0) * halfHeightBlocks;
            // 椭圆：两端更稀，中间更密。
            double radial01 = Math.sqrt(
                    (alongRight / halfWidthBlocks) * (alongRight / halfWidthBlocks)
                            + (alongForward / Math.max(0.2, halfDepthBlocks)) * (alongForward / Math.max(0.2, halfDepthBlocks))
            );
            if (radial01 > 1.0 && level.random.nextDouble() < 0.55) {
                continue;
            }
            Vec3 starPosition = cloudCenter
                    .add(right.scale(alongRight))
                    .add(forward.scale(alongForward))
                    .add(0.0, alongUp, 0.0);
            spawnAccent(level, Accent.MOTE, starPosition, drift(level));
        }
    }

    private static Vec3 drift(Level level) {
        double speed = FoundingRainOfStarsTuning.OVERHEAD_CLOUD_DRIFT_BLOCKS_PER_TICK;
        return new Vec3(
                (level.random.nextDouble() - 0.5) * 2.0 * speed,
                (level.random.nextDouble() - 0.5) * speed,
                (level.random.nextDouble() - 0.5) * 2.0 * speed
        );
    }

    private static void spawnAccent(Level level, Accent accent, Vec3 position, Vec3 velocity) {
        boolean hasDirectedVelocity = velocity.lengthSqr() > 1.0e-10;
        MagicManager.spawnParticles(
                level,
                new OverheadNebulaAccentOptions(accent),
                position.x,
                position.y,
                position.z,
                hasDirectedVelocity ? 0 : 1,
                hasDirectedVelocity ? velocity.x : 0.01,
                hasDirectedVelocity ? velocity.y : 0.01,
                hasDirectedVelocity ? velocity.z : 0.01,
                hasDirectedVelocity ? 1.0 : 0.0,
                true
        );
    }
}

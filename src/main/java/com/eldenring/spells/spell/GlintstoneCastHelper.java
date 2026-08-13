package com.eldenring.spells.spell;

import com.eldenring.spells.entity.AbstractGlintstoneProjectile;
import com.eldenring.spells.particle.glintstone.GlintstoneFx;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BiFunction;

/**
 * 辉石弹道施法公共逻辑：沿视线生成、设置朝向与伤害、施法爆发粒子。
 */
public final class GlintstoneCastHelper {
    private GlintstoneCastHelper() {
    }

    /**
     * 沿施法者视线生成一发辉石弹道。
     * <p>
     * 生成点沿视线前移，并可叠加入 {@code lookPlaneOffset}（用于流星圆阵顶点）。
     * 飞行方向才使用 {@code shootDirection}。
     * 生成后会把弹体 AABB 从实心方块中推出，降低「出手即消失」概率。
     */
    public static AbstractGlintstoneProjectile spawnAlongLook(
            Level level,
            LivingEntity castingEntity,
            BiFunction<Level, LivingEntity, AbstractGlintstoneProjectile> projectileFactory,
            double spawnForwardOffsetBlocks,
            double castBurstForwardOffsetBlocks,
            float castBurstIntensity,
            float damageAmount,
            Vec3 shootDirection,
            boolean playCastBurst
    ) {
        return spawnAlongLook(
                level,
                castingEntity,
                projectileFactory,
                spawnForwardOffsetBlocks,
                castBurstForwardOffsetBlocks,
                castBurstIntensity,
                damageAmount,
                shootDirection,
                Vec3.ZERO,
                playCastBurst
        );
    }

    public static AbstractGlintstoneProjectile spawnAlongLook(
            Level level,
            LivingEntity castingEntity,
            BiFunction<Level, LivingEntity, AbstractGlintstoneProjectile> projectileFactory,
            double spawnForwardOffsetBlocks,
            double castBurstForwardOffsetBlocks,
            float castBurstIntensity,
            float damageAmount,
            Vec3 shootDirection,
            Vec3 lookPlaneOffset,
            boolean playCastBurst
    ) {
        AbstractGlintstoneProjectile projectile = projectileFactory.apply(level, castingEntity);
        Vec3 lookDirection = castingEntity.getLookAngle().normalize();
        Vec3 normalizedShootDirection = shootDirection.normalize();

        Vec3 eyePosition = castingEntity.getEyePosition();
        Vec3 desiredSpawnPosition = eyePosition
                .subtract(0, projectile.getBbHeight() * 0.5, 0)
                .add(lookDirection.scale(spawnForwardOffsetBlocks))
                .add(lookPlaneOffset);

        Vec3 spawnPosition = resolveSpawnPositionClearOfBlocks(
                level,
                castingEntity,
                eyePosition,
                desiredSpawnPosition,
                lookDirection
        );
        spawnPosition = nudgeSpawnSoProjectileBoxIsClear(
                level,
                projectile,
                spawnPosition,
                eyePosition,
                lookDirection
        );

        projectile.setPos(spawnPosition);
        projectile.shoot(normalizedShootDirection);
        float yawDegrees = (float) (Mth.atan2(normalizedShootDirection.x, normalizedShootDirection.z) * Mth.RAD_TO_DEG);
        float pitchDegrees = (float) (Mth.atan2(
                normalizedShootDirection.y,
                normalizedShootDirection.horizontalDistance()
        ) * Mth.RAD_TO_DEG);
        projectile.setYRot(yawDegrees);
        projectile.setXRot(pitchDegrees);
        projectile.setDamage(damageAmount);
        level.addFreshEntity(projectile);

        if (playCastBurst) {
            Vec3 castBurstPosition = spawnPosition.add(lookDirection.scale(castBurstForwardOffsetBlocks));
            GlintstoneFx.castBurst(
                    level,
                    castBurstPosition.x,
                    castBurstPosition.y,
                    castBurstPosition.z,
                    castBurstIntensity
            );
        }
        return projectile;
    }

    /**
     * 视线前方圆阵上的第 {@code vertexIndex} 个顶点偏移（相对圆心，不含前向位移）。
     * <p>
     * 圆面垂直于视线：0° 为视野右侧，90° 为视野上方。步长用整数除法 {@code 360 / vertexCount}，
     * 再按顺时针递减角度，因此 3 点构成正三角形、6 点正六边形、8 点正八边形。
     *
     * @param lookDirection      施法者视线，圆面法线
     * @param vertexIndex        当前顶点序号（从 0 开始，顺时针）
     * @param vertexCount        等分份数，通常等于流星数量
     * @param radiusBlocks       圆半径（方块）；调大 → 阵面更散
     * @param startAngleDegrees  第一发的极角（度）。90 = 正上方先出现
     */
    public static Vec3 clockwiseRegularPolygonOffset(
            Vec3 lookDirection,
            int vertexIndex,
            int vertexCount,
            double radiusBlocks,
            int startAngleDegrees
    ) {
        Vec3 forward = lookDirection.lengthSqr() > 1.0e-8
                ? lookDirection.normalize()
                : new Vec3(0.0, 0.0, 1.0);
        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 right = forward.cross(worldUp);
        if (right.lengthSqr() < 1.0e-8) {
            right = new Vec3(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }
        Vec3 planeUp = right.cross(forward).normalize();

        int clampedVertexCount = Math.max(1, vertexCount);
        int stepDegrees = 360 / clampedVertexCount;
        int angleDegrees = startAngleDegrees - vertexIndex * stepDegrees;
        double angleRadians = Math.toRadians(angleDegrees);
        return right.scale(Math.cos(angleRadians) * radiusBlocks)
                .add(planeUp.scale(Math.sin(angleRadians) * radiusBlocks));
    }

    private static Vec3 resolveSpawnPositionClearOfBlocks(
            Level level,
            LivingEntity castingEntity,
            Vec3 eyePosition,
            Vec3 desiredSpawnPosition,
            Vec3 lookDirection
    ) {
        HitResult blockHit = level.clip(new ClipContext(
                eyePosition,
                desiredSpawnPosition,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                castingEntity
        ));
        if (blockHit.getType() == HitResult.Type.MISS) {
            return desiredSpawnPosition;
        }
        return blockHit.getLocation().subtract(lookDirection.scale(0.15));
    }

    /**
     * 仅射线清障不够：弹体有厚度，贴墙/低头施法时 AABB 仍可能嵌块。
     * 沿视线回退并做小幅轴向挪动，直到碰撞箱不与实心方块相交。
     */
    private static Vec3 nudgeSpawnSoProjectileBoxIsClear(
            Level level,
            AbstractGlintstoneProjectile projectile,
            Vec3 spawnPosition,
            Vec3 eyePosition,
            Vec3 lookDirection
    ) {
        float halfWidth = projectile.getBbWidth() * 0.5f;
        float height = projectile.getBbHeight();

        for (int stepIndex = 0; stepIndex <= 8; stepIndex++) {
            Vec3 candidate = spawnPosition.subtract(lookDirection.scale(0.08 * stepIndex));
            if (candidate.subtract(eyePosition).dot(lookDirection) < 0.05 && stepIndex > 0) {
                candidate = eyePosition.add(lookDirection.scale(0.1)).subtract(0, height * 0.5, 0);
            }
            if (isProjectileBoxClearAt(level, candidate, halfWidth, height)) {
                return candidate;
            }
        }

        double[] axisOffsets = {0.0, 0.12, -0.12, 0.24, -0.24};
        for (double yOffset : axisOffsets) {
            for (double xOffset : axisOffsets) {
                for (double zOffset : axisOffsets) {
                    Vec3 candidate = spawnPosition.add(xOffset, yOffset, zOffset);
                    if (isProjectileBoxClearAt(level, candidate, halfWidth, height)) {
                        return candidate;
                    }
                }
            }
        }
        return spawnPosition;
    }

    private static boolean isProjectileBoxClearAt(
            Level level,
            Vec3 position,
            float halfWidth,
            float height
    ) {
        AABB boundingBox = new AABB(
                position.x - halfWidth,
                position.y,
                position.z - halfWidth,
                position.x + halfWidth,
                position.y + height,
                position.z + halfWidth
        ).deflate(0.02);
        BlockPos minPos = BlockPos.containing(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
        BlockPos maxPos = BlockPos.containing(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
        for (BlockPos blockPos : BlockPos.betweenClosed(minPos, maxPos)) {
            BlockState blockState = level.getBlockState(blockPos);
            VoxelShape collisionShape = blockState.getCollisionShape(level, blockPos);
            if (collisionShape.isEmpty()) {
                continue;
            }
            if (collisionShape.bounds().move(blockPos).intersects(boundingBox)) {
                return false;
            }
        }
        return true;
    }
}

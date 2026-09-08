package com.eldenring.spells.spell.helper;

import com.eldenring.spells.entity.GravityBallProjectile;
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
 * 重力弹道的「出手」公共逻辑。
 * <p>
 * 从眼睛沿视线前移到生成点，射线碰墙则收回墙前，再按弹体 AABB 厚度回退 / 轴向挪动，
 * 直到箱子不嵌块。不要把重力弹塞进 {@link GlintstoneCastHelper}。
 */
public final class GravityCastHelper {

    private GravityCastHelper() {
    }

    /**
     * 沿施法者视线生成一发重力球。
     *
     * @param projectileFactory        通常写 {@code GravityBallProjectile::new}
     * @param spawnForwardOffsetBlocks 生成点相对眼睛、沿视线前移（方块）
     * @param damageAmount             命中伤害；重力球固定传 0
     * @param pullDistanceBlocks       本级最大拉取格数
     * @param shootDirection           飞行方向，通常是视线
     */
    public static GravityBallProjectile spawnAlongLook(
            Level level,
            LivingEntity castingEntity,
            BiFunction<Level, LivingEntity, GravityBallProjectile> projectileFactory,
            double spawnForwardOffsetBlocks,
            float damageAmount,
            double pullDistanceBlocks,
            Vec3 shootDirection
    ) {
        GravityBallProjectile projectile = projectileFactory.apply(level, castingEntity);
        Vec3 lookDirection = castingEntity.getLookAngle().normalize();
        Vec3 normalizedShootDirection = shootDirection.lengthSqr() > 1.0e-8
                ? shootDirection.normalize()
                : lookDirection;

        Vec3 eyePosition = castingEntity.getEyePosition();
        Vec3 desiredSpawnPosition = eyePosition
                .subtract(0, projectile.getBbHeight() * 0.5, 0)
                .add(lookDirection.scale(spawnForwardOffsetBlocks));

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
        projectile.setPullDistanceBlocks(pullDistanceBlocks);
        level.addFreshEntity(projectile);
        return projectile;
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

    private static Vec3 nudgeSpawnSoProjectileBoxIsClear(
            Level level,
            GravityBallProjectile projectile,
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

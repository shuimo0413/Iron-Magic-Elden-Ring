package com.eldenring.spells.spell.helper;

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
 * 辉石弹道的「出手」公共逻辑。
 * <p>
 * 各 {@code XxxSpell.onCast} 不要自己 {@code setPos + shoot + addFreshEntity}：
 * 贴墙、低头、站在方块边时，裸生成很容易让弹体 AABB 嵌进实心块，下一 tick 就撞墙消失
 * （玩家体感是「出手即没」）。本类负责：
 * <ol>
 *   <li>从眼睛沿视线前移到 Spell 指定的生成点；</li>
 *   <li>射线碰到墙则把点收回墙前；</li>
 *   <li>再按弹体碰撞箱厚度做回退 / 轴向挪动，直到箱子不嵌块；</li>
 *   <li>写入飞行方向、yaw/pitch、伤害，再刷实体；</li>
 *   <li>可选：在生成点再往前一点刷施法爆发粒子。</li>
 * </ol>
 * 连发流星（辉石流星 / 流星雨 / 毁灭流星）也走这里，只是 {@code shootDirection}
 * 和 {@code lookPlaneOffset} 由齐射实体按圆阵顶点算好再传入。
 */
public final class GlintstoneCastHelper {
    private GlintstoneCastHelper() {
    }

    /**
     * 沿施法者视线生成一发辉石弹道（生成点与飞行方向都跟视线走）。
     * <p>
     * 等价于 {@link #spawnAlongLook(Level, LivingEntity, BiFunction, double, double, float, float, Vec3, Vec3, boolean)
     * 九参数重载} 且 {@code lookPlaneOffset = Vec3.ZERO}。
     * 单发法术（魔砾、彗星等）都调这一版。
     *
     * @param projectileFactory              弹道工厂，通常写 {@code XxxProjectile::new}
     * @param spawnForwardOffsetBlocks       生成点相对眼睛、沿视线前移的距离（方块）。太小容易嵌进玩家自己，太大容易穿墙
     * @param castBurstForwardOffsetBlocks   爆发粒子相对生成点再沿视线前移的距离（方块）
     * @param castBurstIntensity             爆发粒子密度倍率；1.0 为魔砾基准，大弹可略大于 1
     * @param damageAmount                   已算好的命中伤害（Spell 里用 spellPower × 伤害系数）
     * @param shootDirection                 飞行方向；单发法术传 {@code castingEntity.getLookAngle()} 即可
     * @param playCastBurst                  {@code true} 刷出手闪光；连发里只有第一发或齐射自己刷时才开
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

    /**
     * 沿视线生成一发辉石弹道，允许生成点在「垂直于视线的平面」上再偏一截。
     * <p>
     * <b>生成点</b>用视线（再加 {@code lookPlaneOffset}），<b>飞行方向</b>才用 {@code shootDirection}。
     * 流星圆阵就是这样：几发出生在正三角形 / 六边形顶点，但都朝同一瞄准方向飞。
     *
     * @param lookPlaneOffset 相对视线的横向/纵向偏移（方块）。单发传 {@link Vec3#ZERO}；
     *                        圆阵顶点用 {@link #clockwiseRegularPolygonOffset}
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
            Vec3 lookPlaneOffset,
            boolean playCastBurst
    ) {
        AbstractGlintstoneProjectile projectile = projectileFactory.apply(level, castingEntity);
        Vec3 lookDirection = castingEntity.getLookAngle().normalize();
        Vec3 normalizedShootDirection = shootDirection.normalize();

        // 眼睛位置减去半高：setPos 用的是实体脚底，要让碰撞箱中心落在视线高度上，否则弹会从下巴底下飞出。
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
        // AbstractMagicProjectile 用实体朝向做渲染/粒子轴向；必须与速度方向一致，否则彗星头会拧着飞。
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
     * 视线前方圆阵上第 {@code vertexIndex} 个顶点的偏移（相对圆心，不含前向位移）。
     * <p>
     * 圆面垂直于视线：0° = 视野右侧，90° = 视野正上方。
     * 步长用整数除法 {@code 360 / vertexCount}，再按顺时针递减角度，
     * 因此 3 点是正三角形、6 点正六边形、8 点正八边形。
     * 辉石流星 / 流星雨 / 毁灭流星的齐射实体用这个排阵。
     *
     * @param lookDirection     施法者视线，作为圆面法线
     * @param vertexIndex       当前顶点序号（从 0 开始，顺时针）
     * @param vertexCount       等分份数，通常等于这一齐射的流星数量
     * @param radiusBlocks      圆半径（方块）；调大 → 阵面更散、更不容易叠在同一像素上
     * @param startAngleDegrees 第一发的极角（度）。90 = 正上方先出现
     * @return 相对眼睛前方圆心的世界空间偏移，加到生成点上即可
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
        // 视线 × 世界上方向 = 视野右向量；垂直仰视/俯视时叉积退化，改用世界 +X 顶上。
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

    /**
     * 眼睛 → 期望生成点 做一次方块碰撞射线。
     * 没命中墙就用期望点；命中则沿视线收回 0.15 格，避免出生点刚好贴在碰撞面上立刻 despawn。
     */
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
     * 仅射线清障不够：弹体有厚度，贴墙 / 低头施法时 AABB 仍可能嵌块。
     * <p>
     * 两步尝试：
     * <ol>
     *   <li>沿视线往眼睛方向一步步回退（最多约 0.64 格），且不退到眼睛后方；</li>
     *   <li>仍嵌块则在 ±0.12 / ±0.24 格的轴向格子里找一个空位。</li>
     * </ol>
     * 都失败则退回原生成点（宁可出手即撞，也不要把弹刷进墙里卡实体）。
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
            // 回退越过眼睛时改放到眼前 0.1 格，避免弹从后脑勺飞出。
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

    /**
     * {@code position} 处按弹体宽高搭一个略缩小的 AABB，看是否与任何实心碰撞箱相交。
     * {@code deflate(0.02)} 是为了忽略「刚好擦到面」的浮点误差，否则贴地生成会永远判占用。
     */
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

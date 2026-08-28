package com.eldenring.spells.spell.helper;

import com.eldenring.spells.entity.CrystalBarrageShardProjectile;
import com.eldenring.spells.spell.CrystalBarrageSpell;
import com.eldenring.spells.spell.data.CrystalBarrageCastData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 结晶连弹施法期辅助：锁死站位、在视线锥里散射刷碎片。
 * <p>
 * {@code CrystalBarrageSpell} 只保留铁魔法生命周期回调；清障 / 随机锥方向不进 Spell 本体。
 */
public final class CrystalBarrageCasting {

    /**
     * 生成点在垂直于视线的平面上的最大随机偏移（方块）。
     * 调大 → 出口更散，更不像从同一个像素连射；调小 → 更像从杖尖喷。
     */
    private static final double SPAWN_LOOK_PLANE_JITTER_BLOCKS = 0.10;

    private CrystalBarrageCasting() {
    }

    /**
     * 把施法者钉在出手脚底并清零速度。不改 yaw/pitch，散射仍跟准星走。
     */
    public static void applyCasterPositionLock(LivingEntity entity, CrystalBarrageCastData castData) {
        Vec3 feet = castData.lockedFeetPosition();
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
        entity.setPos(feet.x, feet.y, feet.z);
    }

    /**
     * 沿视线前方随机锥刷一发不追踪的辉石碎片。
     *
     * @param playCastBurst {@code true} 只给本段吟唱第一发一点出手闪光；连射时不要每发都闪
     */
    public static void spawnScatterShard(
            Level level,
            LivingEntity caster,
            float damageAmount,
            boolean playCastBurst
    ) {
        RandomSource random = caster.getRandom();
        Vec3 lookDirection = caster.getLookAngle();
        Vec3 scatterDirection = scatterDirectionInsideCone(
                lookDirection,
                CrystalBarrageSpell.SCATTER_HALF_ANGLE_DEGREES,
                random
        );
        Vec3 lookPlaneOffset = randomLookPlaneOffset(lookDirection, random);
        GlintstoneCastHelper.spawnAlongLook(
                level,
                caster,
                CrystalBarrageShardProjectile::new,
                CrystalBarrageSpell.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS,
                CrystalBarrageSpell.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS,
                CrystalBarrageSpell.CAST_BURST_PARTICLE_INTENSITY,
                damageAmount,
                scatterDirection,
                lookPlaneOffset,
                playCastBurst
        );
    }

    /**
     * 在视线锥内均匀采样一个单位方向（圆盘半径用 sqrt，避免全挤在中心轴上）。
     *
     * @param halfAngleDegrees 锥半角（度）。调大 → 扇面更开；调小 → 更像一条窄束
     */
    public static Vec3 scatterDirectionInsideCone(
            Vec3 lookDirection,
            float halfAngleDegrees,
            RandomSource random
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

        double coneRadius = Math.tan(Math.toRadians(halfAngleDegrees)) * Math.sqrt(random.nextDouble());
        double azimuthRadians = random.nextDouble() * (Math.PI * 2.0);
        return forward
                .add(right.scale(Math.cos(azimuthRadians) * coneRadius))
                .add(planeUp.scale(Math.sin(azimuthRadians) * coneRadius))
                .normalize();
    }

    /**
     * 生成点在视线平面上的小幅抖动，让连射出口不完全重叠。
     */
    private static Vec3 randomLookPlaneOffset(Vec3 lookDirection, RandomSource random) {
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
        double rightOffsetBlocks = (random.nextDouble() * 2.0 - 1.0) * SPAWN_LOOK_PLANE_JITTER_BLOCKS;
        double upOffsetBlocks = (random.nextDouble() * 2.0 - 1.0) * SPAWN_LOOK_PLANE_JITTER_BLOCKS;
        return right.scale(rightOffsetBlocks).add(planeUp.scale(upOffsetBlocks));
    }

    /**
     * 地面走动 / 冲刺时水平速度明显大于站立抖动。创造飞行不算「走动」。
     */
    public static boolean isCasterWalkingOnGround(LivingEntity entity) {
        if (!entity.onGround()) {
            return false;
        }
        if (entity instanceof Player player && player.getAbilities().flying) {
            return false;
        }
        return entity.getDeltaMovement().horizontalDistance()
                > CrystalBarrageSpell.CAST_REFUSE_HORIZONTAL_SPEED_BLOCKS_PER_TICK;
    }
}

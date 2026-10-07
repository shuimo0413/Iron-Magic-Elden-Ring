package com.eldenring.spells.spell.helper;

import com.eldenring.spells.entity.MeteoriteProjectile;
import com.eldenring.spells.entity.MeteoriteVoidEntity;
import com.eldenring.spells.registry.ModSounds;
import com.eldenring.spells.spell.MeteoriteSpell;
import com.eldenring.spells.spell.data.MeteoriteCastData;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 陨石施法期辅助：黑洞锚点、黑洞实体保活、陨石出生点与倾斜下坠方向。
 * <p>
 * {@code MeteoriteSpell} 只保留铁魔法生命周期回调；几何计算与清障不进 Spell 本体。
 */
public final class MeteoriteCasting {

    /**
     * 黑洞中心相对施法者眼睛沿水平朝向前移的距离（方块）。
     * 调大 → 黑洞离人更远、第一人称更容易整个看见；调小 → 更像贴在头顶。
     */
    private static final double VOID_FORWARD_OFFSET_BLOCKS = 2.5;

    /**
     * 黑洞中心相对施法者眼睛的上抬高度（方块）。
     * 调大 → 陨石从更高处砸下、落点更远；调小 → 更低更平。
     */
    private static final double VOID_UPWARD_OFFSET_BLOCKS = 2.5;

    /** 低天花板时黑洞贴着障碍物往回收的距离（方块），避免陨石出生在方块里。 */
    private static final double VOID_OBSTRUCTION_BACKOFF_BLOCKS = 0.6;

    /**
     * 陨石出生点在黑洞盘面内的最大随机半径（方块）。
     * 应略小于黑洞视觉半径（见 {@code MeteoriteFx}），让陨石看起来是从黑洞里钻出来的。
     */
    private static final double SPAWN_DISC_RADIUS_BLOCKS = 0.9;

    private MeteoriteCasting() {
    }

    /**
     * 本段吟唱还没有黑洞（或已被移除）就生成一个；已有则保活并贴回锚点。
     */
    public static void ensureVoidEntity(Level level, LivingEntity caster, MeteoriteCastData castData) {
        Vec3 voidCenter = voidCenterFor(level, caster);
        float facingDescentDegrees = currentDescentAngleDegrees(caster);
        MeteoriteVoidEntity voidEntity = castData.voidEntity();
        if (voidEntity == null || voidEntity.isRemoved()) {
            voidEntity = new MeteoriteVoidEntity(
                    level,
                    voidCenter,
                    caster.getYRot(),
                    facingDescentDegrees,
                    MeteoriteSpell.VOID_OPENING_DURATION_TICKS
            );
            level.addFreshEntity(voidEntity);
            castData.bindVoidEntity(voidEntity);
            return;
        }
        voidEntity.refreshWhileCasting(voidCenter, caster.getYRot(), facingDescentDegrees);
    }

    /**
     * 从黑洞盘面随机一点刷一颗陨石，沿当前水平朝向倾斜下坠，不追踪。
     *
     * @param playLaunchSound {@code true} 只给本段吟唱第一颗播飞弹射出音；连发时不要每颗都响
     */
    public static void spawnFallingMeteorite(
            Level level,
            LivingEntity caster,
            float damageAmount,
            boolean playLaunchSound
    ) {
        RandomSource random = caster.getRandom();
        Vec3 voidCenter = voidCenterFor(level, caster);
        Vec3 fallDirection = randomFallDirection(caster, random);
        Vec3 spawnCenter = voidCenter.add(randomDiscOffset(fallDirection, random));

        MeteoriteProjectile meteorite = new MeteoriteProjectile(level, caster);
        Vec3 feetPosition = meteorite.feetPositionForCenter(spawnCenter);
        meteorite.setPos(feetPosition.x, feetPosition.y, feetPosition.z);
        meteorite.setDamage(damageAmount);
        meteorite.shoot(fallDirection);
        level.addFreshEntity(meteorite);

        if (playLaunchSound) {
            ModSounds.playProjectileLaunch(level, voidCenter);
        }
    }

    /**
     * 黑洞中心：眼睛沿水平朝向前移、再上抬。中途撞到方块就停在障碍物前，避免陨石出生在墙 / 天花板里。
     */
    public static Vec3 voidCenterFor(Level level, LivingEntity caster) {
        Vec3 eyePosition = caster.getEyePosition();
        Vec3 desiredCenter = eyePosition
                .add(horizontalForward(caster).scale(VOID_FORWARD_OFFSET_BLOCKS))
                .add(0.0, VOID_UPWARD_OFFSET_BLOCKS, 0.0);
        BlockHitResult obstruction = level.clip(new ClipContext(
                eyePosition,
                desiredCenter,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        ));
        if (obstruction.getType() == HitResult.Type.MISS) {
            return desiredCenter;
        }
        Vec3 towardDesired = desiredCenter.subtract(eyePosition);
        double desiredDistance = towardDesired.length();
        if (desiredDistance < 1.0e-6) {
            return eyePosition;
        }
        double clearDistance = Math.max(0.0, eyePosition.distanceTo(obstruction.getLocation()) - VOID_OBSTRUCTION_BACKOFF_BLOCKS);
        return eyePosition.add(towardDesired.scale(clearDistance / desiredDistance));
    }

    /**
     * 当前下坠角（度，相对水平向下），不含随机抖动：基础角叠加视线俯角后夹在上下限之间。
     * 原版 {@code xRot} 低头为正，所以低头 → 角度变大 → 落得更近；抬头 → 落得更远。
     */
    public static float currentDescentAngleDegrees(LivingEntity caster) {
        float minimumDegrees = Math.min(MeteoriteSpell.DESCENT_MIN_ANGLE_DEGREES, MeteoriteSpell.DESCENT_MAX_ANGLE_DEGREES);
        float maximumDegrees = Math.max(MeteoriteSpell.DESCENT_MIN_ANGLE_DEGREES, MeteoriteSpell.DESCENT_MAX_ANGLE_DEGREES);
        return Mth.clamp(MeteoriteSpell.DESCENT_BASE_ANGLE_DEGREES + caster.getXRot(), minimumDegrees, maximumDegrees);
    }

    /**
     * 单颗陨石的飞行方向：水平朝向左右随机偏 {@link MeteoriteSpell#SCATTER_HALF_ANGLE_DEGREES}，
     * 下坠角再随机抖 {@link MeteoriteSpell#DESCENT_JITTER_DEGREES}，始终保持向下。
     */
    private static Vec3 randomFallDirection(LivingEntity caster, RandomSource random) {
        float yawOffsetDegrees = (random.nextFloat() * 2.0f - 1.0f) * MeteoriteSpell.SCATTER_HALF_ANGLE_DEGREES;
        float descentJitterDegrees = (random.nextFloat() * 2.0f - 1.0f) * MeteoriteSpell.DESCENT_JITTER_DEGREES;
        float descentDegrees = Mth.clamp(currentDescentAngleDegrees(caster) + descentJitterDegrees, 1.0f, 89.0f);
        return Vec3.directionFromRotation(descentDegrees, caster.getYRot() + yawOffsetDegrees);
    }

    /** 施法者水平朝向（单位向量，忽略俯仰）。 */
    private static Vec3 horizontalForward(LivingEntity caster) {
        return Vec3.directionFromRotation(0.0f, caster.getYRot());
    }

    /**
     * 垂直于飞行方向的盘面内均匀随机偏移（半径用 sqrt，避免全挤在中心）。
     */
    private static Vec3 randomDiscOffset(Vec3 fallDirection, RandomSource random) {
        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 discRight = fallDirection.cross(worldUp);
        if (discRight.lengthSqr() < 1.0e-8) {
            discRight = new Vec3(1.0, 0.0, 0.0);
        } else {
            discRight = discRight.normalize();
        }
        Vec3 discUp = discRight.cross(fallDirection).normalize();
        double offsetRadius = SPAWN_DISC_RADIUS_BLOCKS * Math.sqrt(random.nextDouble());
        double azimuthRadians = random.nextDouble() * (Math.PI * 2.0);
        return discRight.scale(Math.cos(azimuthRadians) * offsetRadius)
                .add(discUp.scale(Math.sin(azimuthRadians) * offsetRadius));
    }
}

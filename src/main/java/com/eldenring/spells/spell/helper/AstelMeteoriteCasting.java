package com.eldenring.spells.spell.helper;

import com.eldenring.spells.entity.MeteoriteProjectile;
import com.eldenring.spells.entity.MeteoriteVoidEntity;
import com.eldenring.spells.registry.ModSounds;
import com.eldenring.spells.spell.AstelMeteoriteSpell;
import com.eldenring.spells.spell.data.AstelMeteoriteCastData;
import com.eldenring.spells.spell.data.AstelMeteoriteCastData.RiftSlot;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;

/**
 * 艾斯提陨石施法期辅助：在施法者前方扇形里随机撕开虚空裂缝、驱动每道裂缝落陨石与坍缩。
 * <p>
 * 裂缝视觉复用 {@link MeteoriteVoidEntity}（每 tick 保活，松手后自己坍缩），陨石复用 {@link MeteoriteProjectile}。
 * {@code AstelMeteoriteSpell} 只保留铁魔法生命周期回调；几何计算与清障不进 Spell 本体。
 */
public final class AstelMeteoriteCasting {

    /**
     * 两道存活裂缝中心之间的最小距离（方块）。
     * 调大 → 裂缝分得更开、扇面更均匀；调小 → 允许扎堆。选点失败多次后会放宽为接受最后一次候选点。
     */
    private static final double RIFT_MIN_SPACING_BLOCKS = 2.5;

    /** 选点时最多尝试几次随机候选，避免扇形太窄时死循环。 */
    private static final int RIFT_PLACEMENT_ATTEMPTS = 6;

    /** 裂缝贴着墙 / 天花板时往回收的距离（方块），避免陨石出生在方块里。 */
    private static final double RIFT_OBSTRUCTION_BACKOFF_BLOCKS = 0.6;

    /**
     * 陨石出生点在裂缝盘面内的最大随机半径（方块）。
     * 应略小于黑洞视觉半径（见 {@code MeteoriteFx}），让陨石看起来是从裂缝里钻出来的。
     */
    private static final double SPAWN_DISC_RADIUS_BLOCKS = 0.9;

    /**
     * 落完最后一颗陨石后裂缝停留多久再坍缩（tick）。
     * 给最后一颗陨石钻出盘面的时间；调大 → 裂缝更拖沓，调小 → 更干脆。
     */
    private static final int RIFT_LINGER_AFTER_LAST_METEORITE_TICKS = 4;

    private AstelMeteoriteCasting() {
    }

    /**
     * 按施法者<strong>当前</strong>朝向在前方扇形内随机选点，撕开一道新裂缝。
     * 已存在的裂缝固定在原位不动。
     *
     * @param openingDurationTicks 张开时长（tick），期间不落陨石；起手那道裂缝传起手蓄力时长
     */
    public static void openRift(Level level, LivingEntity caster, AstelMeteoriteCastData castData, int openingDurationTicks) {
        RandomSource random = caster.getRandom();
        Vec3 chosenCenter = null;
        float chosenYawDegrees = caster.getYRot();
        float facingSpreadFraction = Mth.clamp(AstelMeteoriteSpell.RIFT_FACING_SPREAD_FRACTION, 0.0f, 1.0f);
        for (int attemptIndex = 0; attemptIndex < RIFT_PLACEMENT_ATTEMPTS; attemptIndex++) {
            float yawOffsetDegrees = (random.nextFloat() * 2.0f - 1.0f) * AstelMeteoriteSpell.RIFT_FAN_HALF_ANGLE_DEGREES;
            Vec3 candidateCenter = riftCenterFor(level, caster, caster.getYRot() + yawOffsetDegrees, random);
            chosenCenter = candidateCenter;
            chosenYawDegrees = caster.getYRot() + yawOffsetDegrees * facingSpreadFraction;
            if (isFarEnoughFromOtherRifts(candidateCenter, castData)) {
                break;
            }
        }

        int meteoriteCount = randomMeteoriteCount(random);
        int clampedOpeningTicks = Math.max(1, openingDurationTicks);
        MeteoriteVoidEntity voidEntity = new MeteoriteVoidEntity(
                level,
                chosenCenter,
                chosenYawDegrees,
                currentDescentAngleDegrees(caster),
                clampedOpeningTicks
        );
        level.addFreshEntity(voidEntity);
        castData.addRift(new RiftSlot(voidEntity, chosenCenter, chosenYawDegrees, meteoriteCount, clampedOpeningTicks));
    }

    /**
     * 每个施法 tick：保活所有裂缝；到点的裂缝落一颗陨石，落完的裂缝坍缩并移出列表。
     *
     * @param sourceSpell  伤害归属的法术
     * @param damageAmount 单颗陨石伤害
     */
    public static void tickRifts(
            Level level,
            LivingEntity caster,
            AstelMeteoriteCastData castData,
            AbstractSpell sourceSpell,
            float damageAmount
    ) {
        float facingDescentDegrees = currentDescentAngleDegrees(caster);
        Iterator<RiftSlot> riftIterator = castData.activeRifts().iterator();
        while (riftIterator.hasNext()) {
            RiftSlot riftSlot = riftIterator.next();
            if (riftSlot.voidEntity().isRemoved()) {
                riftIterator.remove();
                continue;
            }
            riftSlot.voidEntity().refreshWhileCasting(riftSlot.center(), riftSlot.facingYawDegrees(), facingDescentDegrees);
            if (!riftSlot.tickActionCountdown()) {
                continue;
            }
            if (riftSlot.meteoritesRemaining() <= 0) {
                riftSlot.collapse();
                riftIterator.remove();
                continue;
            }
            spawnMeteoriteFromRift(
                    level,
                    caster,
                    riftSlot,
                    sourceSpell,
                    damageAmount,
                    castData.tryMarkFirstMeteoriteSpawned()
            );
            riftSlot.markMeteoriteSpawned(
                    Math.max(1, AstelMeteoriteSpell.RIFT_METEORITE_INTERVAL_TICKS),
                    RIFT_LINGER_AFTER_LAST_METEORITE_TICKS
            );
        }
    }

    /**
     * 从裂缝盘面随机一点刷一颗陨石：沿裂缝偏航（再小幅左右抖）+ 当前视线下坠角倾斜下坠，不追踪。
     *
     * @param playLaunchSound {@code true} 只给本段吟唱第一颗播飞弹射出音
     */
    private static void spawnMeteoriteFromRift(
            Level level,
            LivingEntity caster,
            RiftSlot riftSlot,
            AbstractSpell sourceSpell,
            float damageAmount,
            boolean playLaunchSound
    ) {
        RandomSource random = caster.getRandom();
        float yawJitterDegrees = (random.nextFloat() * 2.0f - 1.0f) * AstelMeteoriteSpell.METEORITE_YAW_JITTER_DEGREES;
        float descentJitterDegrees = (random.nextFloat() * 2.0f - 1.0f) * AstelMeteoriteSpell.DESCENT_JITTER_DEGREES;
        float descentDegrees = Mth.clamp(currentDescentAngleDegrees(caster) + descentJitterDegrees, 1.0f, 89.0f);
        Vec3 fallDirection = Vec3.directionFromRotation(descentDegrees, riftSlot.facingYawDegrees() + yawJitterDegrees);
        Vec3 spawnCenter = riftSlot.center().add(randomDiscOffset(fallDirection, random));

        MeteoriteProjectile meteorite = new MeteoriteProjectile(level, caster);
        meteorite.configureFor(
                sourceSpell,
                AstelMeteoriteSpell.PROJECTILE_FLIGHT_SPEED,
                AstelMeteoriteSpell.PROJECTILE_MAX_RANGE_BLOCKS,
                AstelMeteoriteSpell.EXPLOSION_RADIUS_BLOCKS,
                AstelMeteoriteSpell.KNOCKBACK_STRENGTH
        );
        Vec3 feetPosition = meteorite.feetPositionForCenter(spawnCenter);
        meteorite.setPos(feetPosition.x, feetPosition.y, feetPosition.z);
        meteorite.setDamage(damageAmount);
        meteorite.shoot(fallDirection);
        level.addFreshEntity(meteorite);

        if (playLaunchSound) {
            ModSounds.playProjectileLaunch(level, riftSlot.center());
        }
    }

    /**
     * 裂缝中心候选点：眼睛沿给定偏航前移随机距离、再随机上抬。中途撞到方块就停在障碍物前。
     */
    private static Vec3 riftCenterFor(Level level, LivingEntity caster, float yawDegrees, RandomSource random) {
        double forwardMinBlocks = Math.min(AstelMeteoriteSpell.RIFT_FORWARD_MIN_BLOCKS, AstelMeteoriteSpell.RIFT_FORWARD_MAX_BLOCKS);
        double forwardMaxBlocks = Math.max(AstelMeteoriteSpell.RIFT_FORWARD_MIN_BLOCKS, AstelMeteoriteSpell.RIFT_FORWARD_MAX_BLOCKS);
        double heightMinBlocks = Math.min(AstelMeteoriteSpell.RIFT_HEIGHT_MIN_BLOCKS, AstelMeteoriteSpell.RIFT_HEIGHT_MAX_BLOCKS);
        double heightMaxBlocks = Math.max(AstelMeteoriteSpell.RIFT_HEIGHT_MIN_BLOCKS, AstelMeteoriteSpell.RIFT_HEIGHT_MAX_BLOCKS);
        double forwardBlocks = Mth.lerp(random.nextDouble(), forwardMinBlocks, forwardMaxBlocks);
        double heightBlocks = Mth.lerp(random.nextDouble(), heightMinBlocks, heightMaxBlocks);

        Vec3 eyePosition = caster.getEyePosition();
        Vec3 desiredCenter = eyePosition
                .add(Vec3.directionFromRotation(0.0f, yawDegrees).scale(forwardBlocks))
                .add(0.0, heightBlocks, 0.0);
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
        double clearDistance = Math.max(0.0, eyePosition.distanceTo(obstruction.getLocation()) - RIFT_OBSTRUCTION_BACKOFF_BLOCKS);
        return eyePosition.add(towardDesired.scale(clearDistance / desiredDistance));
    }

    private static boolean isFarEnoughFromOtherRifts(Vec3 candidateCenter, AstelMeteoriteCastData castData) {
        double minimumSpacingSquared = RIFT_MIN_SPACING_BLOCKS * RIFT_MIN_SPACING_BLOCKS;
        for (RiftSlot existingRift : castData.activeRifts()) {
            if (existingRift.center().distanceToSqr(candidateCenter) < minimumSpacingSquared) {
                return false;
            }
        }
        return true;
    }

    /** 每道裂缝的陨石数：在 [下限, 上限] 内均匀随机（数值表约定 2–3 颗）。 */
    private static int randomMeteoriteCount(RandomSource random) {
        int minimumCount = Math.max(1, Math.min(AstelMeteoriteSpell.METEORITES_PER_RIFT_MIN, AstelMeteoriteSpell.METEORITES_PER_RIFT_MAX));
        int maximumCount = Math.max(minimumCount, Math.max(AstelMeteoriteSpell.METEORITES_PER_RIFT_MIN, AstelMeteoriteSpell.METEORITES_PER_RIFT_MAX));
        return minimumCount + random.nextInt(maximumCount - minimumCount + 1);
    }

    /**
     * 当前下坠角（度，相对水平向下），不含随机抖动：基础角叠加视线俯角后夹在上下限之间。
     * 低头 → 角度变大 → 落得更近；抬头 → 落得更远。
     */
    private static float currentDescentAngleDegrees(LivingEntity caster) {
        float minimumDegrees = Math.min(AstelMeteoriteSpell.DESCENT_MIN_ANGLE_DEGREES, AstelMeteoriteSpell.DESCENT_MAX_ANGLE_DEGREES);
        float maximumDegrees = Math.max(AstelMeteoriteSpell.DESCENT_MIN_ANGLE_DEGREES, AstelMeteoriteSpell.DESCENT_MAX_ANGLE_DEGREES);
        return Mth.clamp(AstelMeteoriteSpell.DESCENT_BASE_ANGLE_DEGREES + caster.getXRot(), minimumDegrees, maximumDegrees);
    }

    /** 垂直于飞行方向的盘面内均匀随机偏移（半径用 sqrt，避免全挤在中心）。 */
    private static Vec3 randomDiscOffset(Vec3 fallDirection, RandomSource random) {
        Vec3 discRight = fallDirection.cross(new Vec3(0.0, 1.0, 0.0));
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

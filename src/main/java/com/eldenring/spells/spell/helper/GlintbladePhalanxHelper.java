package com.eldenring.spells.spell.helper;

import com.eldenring.spells.entity.PhalanxGlintbladeEntity;
import com.eldenring.spells.registry.ModSounds;
import com.eldenring.spells.spell.curve.GlintbladePhalanxCastCurve;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 辉剑圆阵 / 卡利亚圆阵 / 巨剑阵共用的「头上半圆」生成。
 * <p>
 * 圆心是施法者眼睛（头），半圆落在水平朝向的正面平面上：
 * 从左肩（-90°）经过头顶（0°）到右肩（+90°）。
 * 头上同时只允许一圈跟手辉剑：三种圆阵互相顶替，连放同一咒也不会叠剑；
 * {@link #spawnHeadSemicircle} 会先清掉该施法者还没射出的旧剑。
 * {@code XxxSpell.onCast} 只调这一份，不要在 Spell 里自己算槽位。
 */
public final class GlintbladePhalanxHelper {

    /**
     * 跟手圆阵清理半径（方块）。剑钉在头旁约 1 格，16 格足够覆盖延迟与冲刺错位。
     */
    private static final double HOVERING_PHALANX_CLEANUP_RADIUS_BLOCKS = 16.0;

    private GlintbladePhalanxHelper() {
    }

    /**
     * 一次圆阵生成要带上的玩法 + 视觉参数。
     * Spell 填这份，helper 只负责清旧剑、排槽、刷实体。
     *
     * @param sourceSpell                            命中伤害来源（辉剑圆阵 / 卡利亚圆阵 / 巨剑阵）
     * @param bladeCount                             半圆上的剑数。5 / 9 / 3
     * @param damagePerBlade                         已算好的单剑伤害
     * @param triggerRangeBlocks                     玩家周围多少格内有敌人就自动射出（方块）
     * @param hoverLifetimeTicks                     一直没敌人则跟手多久后消失（tick）
     * @param orbitRadiusBlocks                      半圆半径（方块）。视觉，不进 toml
     * @param swordVisualScale                       相对辉剑网格的倍率。巨剑阵 &gt; 1 原地放大
     * @param projectileFlightSpeed                  射出后速度（方块/tick）
     * @param projectileTrackingRangeBlocks          射出后还能追多远（方块）
     * @param projectileMaxTurnAngleDegreesPerTick   每 tick 最大转向（度）
     */
    public record SpawnSpec(
            AbstractSpell sourceSpell,
            int bladeCount,
            float damagePerBlade,
            double triggerRangeBlocks,
            int hoverLifetimeTicks,
            double orbitRadiusBlocks,
            float swordVisualScale,
            float projectileFlightSpeed,
            double projectileTrackingRangeBlocks,
            float projectileMaxTurnAngleDegreesPerTick
    ) {
    }

    /**
     * 在施法者头上刷一整圈辉剑，并播一次起手音。
     * 会先清掉该施法者还在跟手的任意圆阵，所以三种圆阵互斥、同咒连放也不会叠剑。
     */
    public static void spawnHeadSemicircle(Level level, LivingEntity caster, SpawnSpec spawnSpec) {
        discardHoveringPhalanxBlades(level, caster);
        int clampedBladeCount = Math.max(1, spawnSpec.bladeCount());
        for (int slotIndex = 0; slotIndex < clampedBladeCount; slotIndex++) {
            PhalanxGlintbladeEntity bladeEntity = new PhalanxGlintbladeEntity(level, caster);
            Vec3 slotPosition = slotWorldPosition(
                    caster,
                    slotIndex,
                    clampedBladeCount,
                    spawnSpec.orbitRadiusBlocks(),
                    GlintbladePhalanxCastCurve.ORBIT_FORWARD_OFFSET_BLOCKS
            );
            bladeEntity.setPos(slotPosition);
            bladeEntity.configurePhalanx(
                    spawnSpec,
                    slotIndex,
                    clampedBladeCount
            );
            bladeEntity.setStoredLaunchDirection(caster.getLookAngle());
            bladeEntity.lockHoverFacing(caster.getYRot(), caster.getXRot());
            level.addFreshEntity(bladeEntity);
        }
        ModSounds.playCastStart(level, caster);
    }

    /**
     * 清掉该施法者头上还在跟手的圆阵辉剑。已射出的不管。
     * <p>
     * 搜索半径大于半圆半径，避免高速移动时漏掉刚贴到头上的旧剑。
     */
    public static void discardHoveringPhalanxBlades(Level level, LivingEntity caster) {
        AABB searchBox = caster.getBoundingBox().inflate(HOVERING_PHALANX_CLEANUP_RADIUS_BLOCKS);
        for (PhalanxGlintbladeEntity bladeEntity : level.getEntitiesOfClass(
                PhalanxGlintbladeEntity.class,
                searchBox,
                candidate -> isHoveringPhalanxOwnedBy(candidate, caster)
        )) {
            bladeEntity.discard();
        }
    }

    private static boolean isHoveringPhalanxOwnedBy(PhalanxGlintbladeEntity bladeEntity, LivingEntity caster) {
        if (bladeEntity.isRemoved() || bladeEntity.hasLaunched()) {
            return false;
        }
        return bladeEntity.getOwner() == caster;
    }

    /**
     * 第 {@code slotIndex} 把剑的世界坐标。
     * <p>
     * 平面用<strong>水平</strong>视线（只跟 yaw）：低头不会把半圆拍进地里。
     * 玩家转身时整圈跟着转。
     *
     * @param orbitRadiusBlocks        半圆半径（方块）。调大 → 剑离头更远、第三人称更好认
     * @param orbitForwardOffsetBlocks 相对头再沿水平前向挪一点（方块），避免嵌进脑袋
     */
    public static Vec3 slotWorldPosition(
            LivingEntity caster,
            int slotIndex,
            int bladeCount,
            double orbitRadiusBlocks,
            double orbitForwardOffsetBlocks
    ) {
        HeadSemicircleBasis basis = HeadSemicircleBasis.fromCaster(caster);
        Vec3 planarOffset = slotPlanarOffset(slotIndex, bladeCount, orbitRadiusBlocks, basis);
        return basis.headCenter.add(planarOffset).add(basis.horizontalForward.scale(orbitForwardOffsetBlocks));
    }

    /**
     * 半圆参数角（度）：-90 = 左肩，0 = 头顶，+90 = 右肩。
     * 多把剑在这段弧上等分。
     */
    public static double slotAngleDegrees(int slotIndex, int bladeCount) {
        int clampedBladeCount = Math.max(1, bladeCount);
        if (clampedBladeCount == 1) {
            return 0.0;
        }
        double normalizedSlot = Mth.clamp(slotIndex, 0, clampedBladeCount - 1)
                / (double) (clampedBladeCount - 1);
        return Mth.lerp(normalizedSlot, -90.0, 90.0);
    }

    private static Vec3 slotPlanarOffset(
            int slotIndex,
            int bladeCount,
            double orbitRadiusBlocks,
            HeadSemicircleBasis basis
    ) {
        double angleRadians = Math.toRadians(slotAngleDegrees(slotIndex, bladeCount));
        // sin(-90°)=-1 沿 -right（左肩）；cos(0°)=1 沿 up（头顶）；sin(+90°)=1 沿 +right（右肩）。
        return basis.right.scale(Math.sin(angleRadians) * orbitRadiusBlocks)
                .add(basis.worldUp.scale(Math.cos(angleRadians) * orbitRadiusBlocks));
    }

    /**
     * 头上半圆的瞬时正交基：头心、水平前、右、世界上。
     */
    private record HeadSemicircleBasis(Vec3 headCenter, Vec3 horizontalForward, Vec3 right, Vec3 worldUp) {

        static HeadSemicircleBasis fromCaster(LivingEntity caster) {
            Vec3 headCenter = caster.getEyePosition();
            Vec3 lookDirection = caster.getLookAngle();
            Vec3 horizontalForward = new Vec3(lookDirection.x, 0.0, lookDirection.z);
            if (horizontalForward.lengthSqr() < 1.0e-8) {
                // 纯仰视 / 俯视时水平前向退化，用 yaw 兜底，避免半圆拧成一条线。
                horizontalForward = Vec3.directionFromRotation(0.0f, caster.getYRot());
            }
            horizontalForward = horizontalForward.normalize();
            Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
            Vec3 right = worldUp.cross(horizontalForward);
            if (right.lengthSqr() < 1.0e-8) {
                right = new Vec3(1.0, 0.0, 0.0);
            } else {
                right = right.normalize();
            }
            return new HeadSemicircleBasis(headCenter, horizontalForward, right, worldUp);
        }
    }
}

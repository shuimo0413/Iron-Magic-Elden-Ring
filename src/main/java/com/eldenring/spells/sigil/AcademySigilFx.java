package com.eldenring.spells.sigil;

import com.eldenring.spells.registry.ModParticles;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 学院辉石头顶点缀：外沿闪星 / 柔光。
 * <p>
 * 某个法术要出这层点缀，在服务端 {@code onCast} 里调用 {@link #spawnAboveHead}；
 * 不想出就不要调。
 */
public final class AcademySigilFx {

    /**
     * 点缀环半径基准（方块）。调大 → 闪星散得更开；调小 → 更贴头顶。
     */
    public static final float QUAD_HALF_SIZE_BLOCKS = 0.5f;

    /**
     * 相对碰撞箱顶端再抬高的距离（方块），避免点缀嵌进头发/头盔。
     * 调大 → 更高、第三人称更醒目；调小 → 更贴头。
     */
    public static final float HEAD_Y_OFFSET_BLOCKS = 0.8f;

    /**
     * 外沿闪星数量。铺在水平圆环上，不往中心堆，也不往下落进第一人称视野。
     */
    private static final int RIM_MOTE_COUNT = 12;

    /**
     * 外沿柔光数量。少而淡，只勾轮廓。
     */
    private static final int RIM_GLOW_COUNT = 4;

    /**
     * 点缀环半径相对 {@code QUAD_HALF_SIZE_BLOCKS} 的倍率。略大于 1 → 粒子在头顶外沿。
     */
    private static final float RIM_RADIUS_TO_HALF_SIZE = 1.18f;

    /**
     * 点缀相对头顶平面向上抬的最大距离（方块）。只往上、不往下，避免落到眼前。
     */
    private static final double RIM_UPWARD_JITTER_BLOCKS = 0.10;

    private AcademySigilFx() {
    }

    /**
     * 在施法者头顶刷一层稀疏辉石闪星 / 柔光。仅服务端生效。
     */
    public static void spawnAboveHead(Level level, LivingEntity caster) {
        if (level.isClientSide || level.getServer() == null || caster == null) {
            return;
        }
        spawnRimAccents(level, worldPositionAboveHead(caster));
    }

    /**
     * 沿头顶水平圆环均匀撒闪星与少量柔光。位置略抬高，速度极小且偏上。
     */
    private static void spawnRimAccents(Level level, Vec3 sigilCenter) {
        double rimRadiusBlocks = AcademySigilFx.QUAD_HALF_SIZE_BLOCKS * RIM_RADIUS_TO_HALF_SIZE;
        spawnOnRim(
                level,
                sigilCenter,
                ModParticles.GLINTSTONE_MOTE.get(),
                RIM_MOTE_COUNT,
                rimRadiusBlocks,
                0.02
        );
        spawnOnRim(
                level,
                sigilCenter,
                ModParticles.GLINTSTONE_GLOW.get(),
                RIM_GLOW_COUNT,
                rimRadiusBlocks * 1.08,
                0.012
        );
    }

    /**
     * 在水平圆环上逐点生成粒子（不用高斯散布，避免全堆到头顶正中）。
     */
    private static void spawnOnRim(
            Level level,
            Vec3 sigilCenter,
            ParticleOptions particle,
            int count,
            double rimRadiusBlocks,
            double speed
    ) {
        for (int index = 0; index < count; index++) {
            double angleRadians = (Math.PI * 2.0 * index) / count
                    + level.random.nextDouble() * 0.22;
            double radiusBlocks = rimRadiusBlocks * (0.88 + level.random.nextDouble() * 0.22);
            double particleX = sigilCenter.x + Math.cos(angleRadians) * radiusBlocks;
            double particleY = sigilCenter.y + 0.03 + level.random.nextDouble() * RIM_UPWARD_JITTER_BLOCKS;
            double particleZ = sigilCenter.z + Math.sin(angleRadians) * radiusBlocks;
            MagicManager.spawnParticles(
                    level,
                    particle,
                    particleX,
                    particleY,
                    particleZ,
                    1,
                    0.012,
                    0.008,
                    0.012,
                    speed,
                    false
            );
        }
    }

    /**
     * 头顶点缀的世界坐标：碰撞箱顶端再抬 {@code HEAD_Y_OFFSET_BLOCKS}。
     */
    private static Vec3 worldPositionAboveHead(Entity caster) {
        return new Vec3(
                caster.getX(),
                caster.getY() + caster.getBbHeight() + AcademySigilFx.HEAD_Y_OFFSET_BLOCKS,
                caster.getZ()
        );
    }
}


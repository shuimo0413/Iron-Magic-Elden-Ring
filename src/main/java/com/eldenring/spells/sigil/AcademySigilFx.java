package com.eldenring.spells.sigil;

import com.eldenring.spells.particle.glintstone.AcademyGlintstoneSigilParticleOptions;
import com.eldenring.spells.registry.ModParticles;
import com.eldenring.spells.tuning.GlintstoneCastSigilTuning;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 学院辉石法阵视觉入口。
 * <p>
 * 某个法术要出头顶法阵，在服务端 {@code onCast} 里调用 {@link #spawnAboveHead}；
 * 不想出就不要调。寿命 / 淡出写死在粒子里，这里不暴露。
 */
public final class AcademySigilFx {

    /**
     * 外沿闪星数量。铺在法阵四周，不往中心堆，避免盖住纹章、也不往下落进第一人称视野。
     */
    private static final int RIM_MOTE_COUNT = 12;

    /**
     * 外沿柔光数量。少而淡，只勾轮廓。
     */
    private static final int RIM_GLOW_COUNT = 4;

    /**
     * 点缀环半径相对 {@link GlintstoneCastSigilTuning#QUAD_HALF_SIZE_BLOCKS} 的倍率。
     * 略大于 1 → 粒子在纹章外沿；调小会压到贴图上。
     */
    private static final float RIM_RADIUS_TO_HALF_SIZE = 1.18f;

    /**
     * 点缀相对法阵平面向上抬的最大距离（方块）。只往上、不往下，避免落到眼前。
     */
    private static final double RIM_UPWARD_JITTER_BLOCKS = 0.10;

    private AcademySigilFx() {
    }

    /**
     * 在施法者头顶放出学院法阵（跟随、不旋转；立刻出现，随后淡出）。
     * 同时在外沿刷一层稀疏辉石闪星 / 柔光。仅服务端生效。
     */
    public static void spawnAboveHead(Level level, LivingEntity caster) {
        if (level.isClientSide || level.getServer() == null || caster == null) {
            return;
        }
        Vec3 sigilPosition = worldPositionAboveHead(caster);
        MagicManager.spawnParticles(
                level,
                new AcademyGlintstoneSigilParticleOptions(caster.getId()),
                sigilPosition.x,
                sigilPosition.y,
                sigilPosition.z,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                true
        );
        spawnRimAccents(level, sigilPosition);
    }

    /**
     * 沿法阵外沿均匀撒闪星与少量柔光。位置在水平圆环上，略抬高，速度极小且偏上。
     */
    private static void spawnRimAccents(Level level, Vec3 sigilCenter) {
        double rimRadiusBlocks = GlintstoneCastSigilTuning.QUAD_HALF_SIZE_BLOCKS * RIM_RADIUS_TO_HALF_SIZE;
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
     * 在水平圆环上逐点生成粒子（不用高斯散布，避免堆到纹章正中挡住细节）。
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
     * 学院法阵的世界坐标：碰撞箱顶端再抬 {@link GlintstoneCastSigilTuning#HEAD_Y_OFFSET_BLOCKS}。
     */
    public static Vec3 worldPositionAboveHead(Entity caster) {
        return new Vec3(
                caster.getX(),
                caster.getY() + caster.getBbHeight() + GlintstoneCastSigilTuning.HEAD_Y_OFFSET_BLOCKS,
                caster.getZ()
        );
    }
}


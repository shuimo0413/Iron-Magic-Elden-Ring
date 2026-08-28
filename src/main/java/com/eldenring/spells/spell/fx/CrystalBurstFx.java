package com.eldenring.spells.spell.fx;

import com.eldenring.spells.registry.ModParticles;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.world.level.Level;

/**
 * 结晶散射视觉：碎片撞到东西或飞满射程时的碎裂。
 * <p>
 * 刻意做成「小晶体崩开」，不要走辉石彗星那种大能量场 + 浓雾。
 * 数量与散布写死，不进 toml。
 */
public final class CrystalBurstFx {

    /**
     * 碎裂时飞出的菱形碎晶数量。调大 → 更像炸开一捧玻璃；调小 → 几乎看不见碎。
     */
    private static final int SHATTER_SHARD_COUNT = 16;

    /**
     * 碎裂火花数量。给碎晶一点「崩开」的亮边。
     */
    private static final int SHATTER_SPARK_COUNT = 10;

    /**
     * 碎裂中心闪光数量。只要一小下，避免齐射时屏幕被闪光糊满。
     */
    private static final int SHATTER_FLARE_COUNT = 2;

    /**
     * 碎裂淡光晕数量。
     */
    private static final int SHATTER_GLOW_COUNT = 4;

    /**
     * 碎晶飞溅半径（方块）。调大 → 碎片散得更开；调小 → 更贴着消失点。
     */
    private static final double SHATTER_SHARD_SPREAD_BLOCKS = 0.16;

    /**
     * 碎晶飞溅速度标量（MagicManager 的 speed 参数）。调大 → 崩得更猛。
     */
    private static final double SHATTER_SHARD_SPEED = 0.42;

    private CrystalBurstFx() {
    }

    /**
     * 在消失点刷晶体碎裂。仅服务端调用，由 {@code MagicManager} 同步到附近客户端。
     */
    public static void shatter(Level level, double impactX, double impactY, double impactZ) {
        if (level.isClientSide) {
            return;
        }
        MagicManager.spawnParticles(
                level,
                ModParticles.GLINTSTONE_SHARD.get(),
                impactX,
                impactY,
                impactZ,
                SHATTER_SHARD_COUNT,
                SHATTER_SHARD_SPREAD_BLOCKS,
                SHATTER_SHARD_SPREAD_BLOCKS,
                SHATTER_SHARD_SPREAD_BLOCKS,
                SHATTER_SHARD_SPEED,
                true
        );
        MagicManager.spawnParticles(
                level,
                ModParticles.GLINTSTONE_SPARK.get(),
                impactX,
                impactY,
                impactZ,
                SHATTER_SPARK_COUNT,
                0.12,
                0.12,
                0.12,
                0.28,
                true
        );
        MagicManager.spawnParticles(
                level,
                ModParticles.GLINTSTONE_FLARE.get(),
                impactX,
                impactY,
                impactZ,
                SHATTER_FLARE_COUNT,
                0.03,
                0.03,
                0.03,
                0.02,
                false
        );
        MagicManager.spawnParticles(
                level,
                ModParticles.GLINTSTONE_GLOW.get(),
                impactX,
                impactY,
                impactZ,
                SHATTER_GLOW_COUNT,
                0.10,
                0.10,
                0.10,
                0.08,
                false
        );
    }
}

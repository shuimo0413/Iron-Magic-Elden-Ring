package com.eldenring.spells.spell.fx;

import com.eldenring.spells.registry.ModParticles;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * 星光火星：稀疏青色火花从星星周围往上蹦，体量对标火把火星，不要铺成星云。
 * 密度写死，不进 toml。
 */
public final class StarlightFx {

    /**
     * 每 tick 刷一颗火花的概率。0.32 ≈ 每秒 6 颗；调大更密，调小更像偶尔迸火星。
     */
    private static final float SPARK_CHANCE_PER_TICK = 0.32f;

    /**
     * 每 tick 刷一颗十字闪星的概率。比火花更稀，只做高光点缀。
     */
    private static final float MOTE_CHANCE_PER_TICK = 0.10f;

    /**
     * 火星水平散布半径（方块）。调大 → 火星离星星更远；调小 → 更贴核。
     */
    private static final double SPARK_HORIZONTAL_SPREAD_BLOCKS = 0.10;

    /**
     * 火星相对星星中心的竖直散布（方块）。
     */
    private static final double SPARK_VERTICAL_SPREAD_BLOCKS = 0.06;

    /**
     * 火花上蹦初速（方块/tick）。正值往上；火花自带一点重力，会先升再落，像火把火星。
     */
    private static final double SPARK_UPWARD_SPEED_BLOCKS_PER_TICK = 0.045;

    /**
     * 火花水平漂移速度（方块/tick）。调大 → 火星更飘。
     */
    private static final double SPARK_HORIZONTAL_SPEED_BLOCKS_PER_TICK = 0.012;

    private StarlightFx() {
    }

    /**
     * 客户端每 tick：在星星周围喷少量青色火星。服务端调用是空操作。
     */
    public static void ambientEmbers(Level level, double starX, double starY, double starZ) {
        if (!level.isClientSide) {
            return;
        }
        RandomSource random = level.random;
        if (random.nextFloat() < SPARK_CHANCE_PER_TICK) {
            double offsetX = (random.nextDouble() - 0.5) * 2.0 * SPARK_HORIZONTAL_SPREAD_BLOCKS;
            double offsetY = (random.nextDouble() - 0.5) * 2.0 * SPARK_VERTICAL_SPREAD_BLOCKS;
            double offsetZ = (random.nextDouble() - 0.5) * 2.0 * SPARK_HORIZONTAL_SPREAD_BLOCKS;
            double velocityX = (random.nextDouble() - 0.5) * 2.0 * SPARK_HORIZONTAL_SPEED_BLOCKS_PER_TICK;
            double velocityY = SPARK_UPWARD_SPEED_BLOCKS_PER_TICK + random.nextDouble() * 0.02;
            double velocityZ = (random.nextDouble() - 0.5) * 2.0 * SPARK_HORIZONTAL_SPEED_BLOCKS_PER_TICK;
            level.addParticle(
                    ModParticles.GLINTSTONE_SPARK.get(),
                    starX + offsetX,
                    starY + offsetY,
                    starZ + offsetZ,
                    velocityX,
                    velocityY,
                    velocityZ
            );
        }
        if (random.nextFloat() < MOTE_CHANCE_PER_TICK) {
            double offsetX = (random.nextDouble() - 0.5) * 2.0 * SPARK_HORIZONTAL_SPREAD_BLOCKS;
            double offsetY = (random.nextDouble() - 0.5) * SPARK_VERTICAL_SPREAD_BLOCKS;
            double offsetZ = (random.nextDouble() - 0.5) * 2.0 * SPARK_HORIZONTAL_SPREAD_BLOCKS;
            level.addParticle(
                    ModParticles.GLINTSTONE_MOTE.get(),
                    starX + offsetX,
                    starY + offsetY,
                    starZ + offsetZ,
                    0.0,
                    0.01,
                    0.0
            );
        }
    }
}

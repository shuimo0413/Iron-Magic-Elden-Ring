package com.eldenring.spells.block.decor;

import com.eldenring.spells.registry.ModParticles;
import com.eldenring.spells.world.GlintstoneColor;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 辉石烛台：模型与贴图来自 {@code 烛台/} 现成资源，不在本仓库重建模。
 * <p>
 * 碰撞与原模型台阶对齐；焰心粒子只在顶部小焰块上喷，颜色跟 {@link GlintstoneColor} 走。
 */
public class GlintstoneCandelabraBlock extends Block {
    public static final MapCodec<GlintstoneCandelabraBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    propertiesCodec(),
                    GlintstoneColor.CODEC.fieldOf("glintstone_color").forGetter(block -> block.color)
            ).apply(instance, GlintstoneCandelabraBlock::new)
    );

    /** 与 {@code 烛台} 模型各层 from/to 一致，单位：方块（像素 / 16）。 */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 2.5D, 15.0D),
            Block.box(2.0D, 2.5D, 2.0D, 14.0D, 3.5D, 14.0D),
            Block.box(4.0D, 3.5D, 4.0D, 12.0D, 9.0D, 12.0D),
            Block.box(3.0D, 8.5D, 3.0D, 13.0D, 10.0D, 13.0D),
            Block.box(4.5D, 9.5D, 4.5D, 11.5D, 11.0D, 11.5D),
            Block.box(5.5D, 10.5D, 5.5D, 10.5D, 15.0D, 10.5D)
    ).optimize();

    /**
     * 顶部焰块中心（像素）。模型里焰块是 7–9 / 15–16，尖焰 7.5–8.5 / 16–16.5。
     */
    private static final double FLAME_PIXEL_X = 8.0D;
    private static final double FLAME_PIXEL_Y = 16.25D;
    private static final double FLAME_PIXEL_Z = 8.0D;

    private final GlintstoneColor color;

    public GlintstoneCandelabraBlock(Properties properties, GlintstoneColor color) {
        super(properties);
        this.color = color;
    }

    @Override
    protected MapCodec<GlintstoneCandelabraBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /**
     * 客户端在焰尖喷上升粒子，青 / 蓝 / 紫分别走辉石、卡利亚、重力库。
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double worldX = pos.getX() + FLAME_PIXEL_X / 16.0D;
        double worldY = pos.getY() + FLAME_PIXEL_Y / 16.0D;
        double worldZ = pos.getZ() + FLAME_PIXEL_Z / 16.0D;
        double jitterX = (random.nextDouble() - 0.5D) * 0.04D;
        double jitterZ = (random.nextDouble() - 0.5D) * 0.04D;
        if (random.nextFloat() < 0.65F) {
            level.addParticle(sparkParticle(), worldX + jitterX, worldY, worldZ + jitterZ, 0.0D, 0.018D, 0.0D);
        }
        if (random.nextFloat() < 0.28F) {
            level.addParticle(glowParticle(), worldX, worldY + 0.02D, worldZ, 0.0D, 0.01D, 0.0D);
        }
        if (random.nextFloat() < 0.18F) {
            level.addParticle(moteParticle(), worldX + jitterX, worldY + 0.04D, worldZ + jitterZ, 0.0D, 0.022D, 0.0D);
        }
    }

    private SimpleParticleType sparkParticle() {
        return switch (color) {
            case CYAN -> ModParticles.GLINTSTONE_SPARK.get();
            case BLUE -> ModParticles.CARIAN_SPARK.get();
            case PURPLE -> ModParticles.GRAVITY_SPARK.get();
        };
    }

    private SimpleParticleType glowParticle() {
        return switch (color) {
            case CYAN -> ModParticles.GLINTSTONE_GLOW.get();
            case BLUE -> ModParticles.CARIAN_GLOW.get();
            case PURPLE -> ModParticles.GRAVITY_GLOW.get();
        };
    }

    private SimpleParticleType moteParticle() {
        return switch (color) {
            case CYAN -> ModParticles.GLINTSTONE_MOTE.get();
            case BLUE -> ModParticles.CARIAN_MOTE.get();
            case PURPLE -> ModParticles.GRAVITY_MOTE.get();
        };
    }
}

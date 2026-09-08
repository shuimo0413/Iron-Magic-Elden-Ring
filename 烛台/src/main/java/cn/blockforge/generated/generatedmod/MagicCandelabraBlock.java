package cn.blockforge.generated.generatedmod;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.core.BlockPos;

public final class MagicCandelabraBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(
            Shapes.box(1.0 / 16.0, 0.0, 1.0 / 16.0, 15.0 / 16.0, 2.5 / 16.0, 15.0 / 16.0),
            Shapes.box(2.0 / 16.0, 2.5 / 16.0, 2.0 / 16.0, 14.0 / 16.0, 3.5 / 16.0, 14.0 / 16.0),
            Shapes.box(4.0 / 16.0, 3.5 / 16.0, 4.0 / 16.0, 12.0 / 16.0, 9.0 / 16.0, 12.0 / 16.0),
            Shapes.box(3.0 / 16.0, 8.5 / 16.0, 3.0 / 16.0, 13.0 / 16.0, 10.0 / 16.0, 13.0 / 16.0),
            Shapes.box(4.5 / 16.0, 9.5 / 16.0, 4.5 / 16.0, 11.5 / 16.0, 11.0 / 16.0, 11.5 / 16.0),
            Shapes.box(5.5 / 16.0, 10.5 / 16.0, 5.5 / 16.0, 10.5 / 16.0, 15.0 / 16.0, 10.5 / 16.0)
    ).optimize();

    public MagicCandelabraBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}

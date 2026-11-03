package com.eldenring.spells.block;

import com.eldenring.spells.block.entity.MagicRealmBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 魔法之境：放在世界里后，以自身为圆心、水平半径 64 格（4 区块）内的地表上空刷魔法光带。
 * <p>
 * 只提供视觉效果，没有玩法作用。覆盖范围多个方块取并集，重叠放置不会让光带变密。
 * 光带本身是纯客户端效果，见 {@code client/biome/MagicLakeWisps}；本方块只负责通过
 * {@link MagicRealmBlockEntity} 告诉客户端覆盖源的位置。方块实体不 tick。
 * <p>
 * 注册时设为活塞推不动，避免覆盖源位置随活塞迁移、索引里留下旧坐标。
 */
public class MagicRealmBlock extends Block implements EntityBlock {
    public static final MapCodec<MagicRealmBlock> CODEC = simpleCodec(MagicRealmBlock::new);

    public MagicRealmBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagicRealmBlockEntity(pos, state);
    }
}

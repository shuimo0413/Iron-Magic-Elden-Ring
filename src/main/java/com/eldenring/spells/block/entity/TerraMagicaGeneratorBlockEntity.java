package com.eldenring.spells.block.entity;

import com.eldenring.spells.registry.ModBlockEntities;
import com.eldenring.spells.world.TerraMagicaGeneratorCoverageIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 魔法之境生成器的位置标记。没有 ticker、不存任何自定义数据，只借方块实体的加载 / 移除回调
 * 把自身坐标登记进 {@link TerraMagicaGeneratorCoverageIndex}，让客户端光带知道覆盖源在哪。
 * <p>
 * 方块实体随原版区块数据同步到客户端，所以不需要自定义网络包。服务端那一份的回调在索引里被忽略。
 */
public class TerraMagicaGeneratorBlockEntity extends BlockEntity {

    public TerraMagicaGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TERRA_MAGICA_GENERATOR.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) {
            TerraMagicaGeneratorCoverageIndex.register(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide()) {
            TerraMagicaGeneratorCoverageIndex.unregister(level, worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && level.isClientSide()) {
            TerraMagicaGeneratorCoverageIndex.unregister(level, worldPosition);
        }
    }
}

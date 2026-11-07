package com.eldenring.spells.worldgen.structure;

import com.eldenring.spells.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * 空片段：只把结构包围盒外扩到缓坡带宽，让邻接区块也会走 {@code placeInChunk → afterPlace}。
 * <p>
 * 若不加这一片，学院 AABB 外的缓坡列落在「无 piece 的区块」里时根本不会调用
 * {@link AcademyTerrainBlender}，看起来就像没做坡度。
 */
public class AcademyBlendExtentPiece extends StructurePiece {

    public AcademyBlendExtentPiece(BoundingBox expandedBox) {
        super(ModStructures.ACADEMY_BLEND_EXTENT.get(), 0, expandedBox);
    }

    /** 存档反序列化用。 */
    public AcademyBlendExtentPiece(CompoundTag tag) {
        super(ModStructures.ACADEMY_BLEND_EXTENT.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        // 无额外字段，包围盒由基类读写
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            BlockPos pivot
    ) {
        // 故意不放置方块；地形由 afterPlace → AcademyTerrainBlender 处理
    }
}

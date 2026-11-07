package com.eldenring.spells.worldgen.structure;

import com.eldenring.spells.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * 学院结构的一片模板（≤48³）。
 * <p>
 * 禁止随机旋转 / 镜像：多片按固定偏移拼成整栋楼，转了会对不齐。
 * 结构内奖励箱与刷怪笼已写在 NBT 里，此处不处理 data marker。
 * <p>
 * 当前学院模板按结构方块方式导出，空气会写进模板；但被 {@code structure_void} 标记的格子
 * 不会被放置，原先世界里的石头 / 草会留在那里，看起来像被山体吞掉。
 * 因此放置前仍先把本片 {@code size} 体积清成空气（只动当前生成盒内的格子），
 * 保证模板体积内只剩模板自己的方块；地形融合只在体积外 / 底面以下动手。
 */
public class AcademyPiece extends TemplateStructurePiece {

    /**
     * @param templateManager 模板管理器
     * @param templateId      如 {@code iss_elden_ring:academy/x2_y0_z1}
     * @param templateOrigin  该片在世界中的放置原点（已含格子偏移）
     */
    public AcademyPiece(
            StructureTemplateManager templateManager,
            ResourceLocation templateId,
            BlockPos templateOrigin
    ) {
        super(
                ModStructures.ACADEMY_PIECE.get(),
                0,
                templateManager,
                templateId,
                templateId.toString(),
                makeSettings(),
                templateOrigin
        );
    }

    /** 存档反序列化用。 */
    public AcademyPiece(StructureTemplateManager templateManager, CompoundTag tag) {
        super(ModStructures.ACADEMY_PIECE.get(), tag, templateManager, id -> makeSettings());
    }

    private static StructurePlaceSettings makeSettings() {
        return new StructurePlaceSettings()
                .setRotation(Rotation.NONE)
                .setMirror(Mirror.NONE)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
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
        clearPieceVolumeToAir(level, box);
        super.postProcess(level, structureManager, generator, random, box, chunkPos, pivot);
    }

    /**
     * 按模板 {@code size} 把本片体积清成空气，再交给模板回填建筑方块。
     * 只处理与当前区块生成盒相交的位置，避免越界写邻块。
     */
    private void clearPieceVolumeToAir(WorldGenLevel level, BoundingBox generationBox) {
        Vec3i size = this.template.getSize();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    cursor.set(
                            this.templatePosition.getX() + x,
                            this.templatePosition.getY() + y,
                            this.templatePosition.getZ() + z
                    );
                    if (!generationBox.isInside(cursor)) {
                        continue;
                    }
                    // 2 = 客户端同步；结构生成阶段用此旗标与模板放置一致
                    level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    @Override
    protected void handleDataMarker(
            String name,
            BlockPos pos,
            ServerLevelAccessor level,
            RandomSource random,
            BoundingBox box
    ) {
        // 学院 NBT 不依赖 structure void 标记箱；战利品表已绑在方块实体上。
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
    }
}

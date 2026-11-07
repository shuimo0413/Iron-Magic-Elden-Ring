package com.eldenring.spells.command;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.academy.AcademyScholarEntity;
import com.eldenring.spells.worldgen.structure.AcademyStructure;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 开发用：从建造档世界按 ≤48³ 网格导出学院结构到存档 {@code generated/}。
 * <p>
 * 走 {@link StructureTemplateManager#save}，与结构方块 GUI「保存」同一条落盘路径，
 * 因此会带上刷怪蛋放置的学者等实体（Axiom 的 Structure NBT 导出会丢掉实体）。
 * <p>
 * 用法（创造 / 作弊，原点 = 学院西南下角，须与建造时包围盒一致）：
 * <pre>
 * /iss_academy_export count 24 -3 39
 * /iss_academy_export save 24 -3 39
 * </pre>
 * 尺寸固定为 {@link AcademyStructure} 的 212×180×231。
 * 导出后到 {@code <存档>/generated/iss_elden_ring/structures/academy/} 拷贝 .nbt
 * （个别版本目录为 {@code structure} 单数）。
 */
public final class ExportAcademyGridCommand {

    private static final int CHUNK = AcademyStructure.PIECE_SIZE_BLOCKS;

    private ExportAcademyGridCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("iss_academy_export")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("count")
                                .then(Commands.argument("origin", BlockPosArgument.blockPos())
                                        .executes(ExportAcademyGridCommand::countEntities)))
                        .then(Commands.literal("save")
                                .then(Commands.argument("origin", BlockPosArgument.blockPos())
                                        .executes(ExportAcademyGridCommand::saveGrid)))
                        .then(Commands.literal("sizes")
                                .executes(ctx -> {
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal(String.format(
                                                    "[iss_academy_export] 网格 %d×%d×%d，片边长 %d，共 %d 片",
                                                    AcademyStructure.STRUCTURE_SIZE_X,
                                                    AcademyStructure.STRUCTURE_SIZE_Y,
                                                    AcademyStructure.STRUCTURE_SIZE_Z,
                                                    CHUNK,
                                                    pieceCountX() * pieceCountY() * pieceCountZ()
                                            )),
                                            false
                                    );
                                    return Command.SINGLE_SUCCESS;
                                }))
        );
    }

    private static int pieceCountX() {
        return ceilDiv(AcademyStructure.STRUCTURE_SIZE_X, CHUNK);
    }

    private static int pieceCountY() {
        return ceilDiv(AcademyStructure.STRUCTURE_SIZE_Y, CHUNK);
    }

    private static int pieceCountZ() {
        return ceilDiv(AcademyStructure.STRUCTURE_SIZE_Z, CHUNK);
    }

    private static int ceilDiv(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }

    private static int countEntities(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPosArgument.getBlockPos(context, "origin");
        AABB box = fullBounds(origin);
        forceChunks(level, origin, true);
        try {
            List<Entity> entities = level.getEntities((Entity) null, box, entity -> !(entity instanceof Player));
            int scholars = 0;
            for (Entity entity : entities) {
                if (entity instanceof AcademyScholarEntity) {
                    scholars++;
                }
            }
            int finalScholars = scholars;
            int total = entities.size();
            source.sendSuccess(
                    () -> Component.literal(String.format(
                            "[iss_academy_export] 包围盒内非玩家实体 %d，其中学院学者 %d。确认后：/iss_academy_export save %d %d %d",
                            total,
                            finalScholars,
                            origin.getX(),
                            origin.getY(),
                            origin.getZ()
                    )),
                    false
            );
            return total;
        } finally {
            forceChunks(level, origin, false);
        }
    }

    private static int saveGrid(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPosArgument.getBlockPos(context, "origin");
        StructureTemplateManager templateManager = level.getServer().getStructureManager();

        source.sendSuccess(
                () -> Component.literal("[iss_academy_export] 强制加载区块并导出中，可能卡顿数十秒…"),
                false
        );

        forceChunks(level, origin, true);
        int saved = 0;
        int failed = 0;
        int scholarHits = 0;
        try {
            for (int indexY = 0; indexY < pieceCountY(); indexY++) {
                for (int indexZ = 0; indexZ < pieceCountZ(); indexZ++) {
                    for (int indexX = 0; indexX < pieceCountX(); indexX++) {
                        int offsetX = indexX * CHUNK;
                        int offsetY = indexY * CHUNK;
                        int offsetZ = indexZ * CHUNK;
                        int sizeX = Math.min(CHUNK, AcademyStructure.STRUCTURE_SIZE_X - offsetX);
                        int sizeY = Math.min(CHUNK, AcademyStructure.STRUCTURE_SIZE_Y - offsetY);
                        int sizeZ = Math.min(CHUNK, AcademyStructure.STRUCTURE_SIZE_Z - offsetZ);
                        BlockPos pieceOrigin = origin.offset(offsetX, offsetY, offsetZ);
                        Vec3i size = new Vec3i(sizeX, sizeY, sizeZ);

                        ResourceLocation templateId = ResourceLocation.fromNamespaceAndPath(
                                EldenRingSpellsMod.MOD_ID,
                                "academy/x" + indexX + "_y" + indexY + "_z" + indexZ
                        );

                        StructureTemplate template = templateManager.getOrCreate(templateId);
                        // withEntities=true；忽略 structure_void，等同结构方块 GUI 保存
                        template.fillFromWorld(level, pieceOrigin, size, true, Blocks.STRUCTURE_VOID);

                        if (!templateManager.save(templateId)) {
                            failed++;
                            EldenRingSpellsMod.LOGGER.error("学院导出失败: {}", templateId);
                            continue;
                        }
                        saved++;
                        scholarHits += countScholarsInBox(level, pieceOrigin, size);
                    }
                }
            }
        } finally {
            forceChunks(level, origin, false);
        }

        int finalSaved = saved;
        int finalFailed = failed;
        int finalScholars = scholarHits;
        source.sendSuccess(
                () -> Component.literal(String.format(
                        "[iss_academy_export] 完成：写出 %d 片，失败 %d；各片学者计数累加 %d。请拷贝存档 generated/%s/structures/academy/*.nbt",
                        finalSaved,
                        finalFailed,
                        finalScholars,
                        EldenRingSpellsMod.MOD_ID
                )),
                true
        );
        return saved;
    }

    private static int countScholarsInBox(ServerLevel level, BlockPos origin, Vec3i size) {
        AABB box = new AABB(
                origin.getX(),
                origin.getY(),
                origin.getZ(),
                origin.getX() + size.getX(),
                origin.getY() + size.getY(),
                origin.getZ() + size.getZ()
        );
        return level.getEntities((Entity) null, box, entity -> entity instanceof AcademyScholarEntity).size();
    }

    private static AABB fullBounds(BlockPos origin) {
        return new AABB(
                origin.getX(),
                origin.getY(),
                origin.getZ(),
                origin.getX() + AcademyStructure.STRUCTURE_SIZE_X,
                origin.getY() + AcademyStructure.STRUCTURE_SIZE_Y,
                origin.getZ() + AcademyStructure.STRUCTURE_SIZE_Z
        );
    }

    /** 导出前后强制加载 / 解除整栋覆盖的区块，避免远端片缺方块或缺实体。 */
    private static void forceChunks(ServerLevel level, BlockPos origin, boolean forced) {
        ChunkPos min = new ChunkPos(origin);
        ChunkPos max = new ChunkPos(origin.offset(
                AcademyStructure.STRUCTURE_SIZE_X - 1,
                0,
                AcademyStructure.STRUCTURE_SIZE_Z - 1
        ));
        for (int chunkX = min.x; chunkX <= max.x; chunkX++) {
            for (int chunkZ = min.z; chunkZ <= max.z; chunkZ++) {
                level.setChunkForced(chunkX, chunkZ, forced);
            }
        }
    }
}

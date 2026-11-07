package com.eldenring.spells.command;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.worldgen.structure.AcademyPiece;
import com.eldenring.spells.worldgen.structure.AcademyStructure;
import com.eldenring.spells.worldgen.structure.AcademyTemplateEdgeProfile;
import com.eldenring.spells.worldgen.structure.AcademyTerrainBlender;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.common.Tags;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 开发用：体检一座已生成的学院与周边地形，量化「悬空 / 边缘断崖 / 模板内多出来的方块」。
 * <p>
 * 用法（作弊 / 控制台）：
 * <pre>
 * /iss_academy_terrain_check nearest   # 找最近的学院，按自然生成流程生成整片区块后体检
 * /iss_academy_terrain_check here      # 体检脚下这座（区块须已生成）
 * </pre>
 * nearest 会同步生成约 300 个区块，主线程会卡几十秒到几分钟；专用服请把 max-tick-time 设为 -1。
 */
public final class AcademyTerrainCheckCommand {

    private static final ResourceKey<Structure> ACADEMY_KEY = ResourceKey.create(
            Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "academy")
    );

    /**
     * 找最近学院时的搜索半径（以结构网格为单位，与 /locate 一致）。
     */
    private static final int LOCATE_SEARCH_RADIUS = 100;

    /**
     * 边缘剖面往外多看几格（超出缓坡带），确认坡脚与自然地形接上。
     * 单位：方块。
     */
    private static final int PROFILE_EXTRA_BLOCKS = 6;

    /**
     * 相邻两列地面高差达到此值算「断崖」。单位：方块。
     */
    private static final int CLIFF_STEP_BLOCKS = 5;

    /**
     * 相邻两列地面高差达到此值算「陡坎」。单位：方块。
     */
    private static final int STEEP_STEP_BLOCKS = 3;

    /**
     * 外侧列找地面时从学院地面往上扫的高度。单位：方块。
     */
    private static final int SURFACE_SCAN_UP_BLOCKS = 64;

    /**
     * 统计模板内多余方块时，离边缘多近算「缓坡垫高区」（允许出现泥土 / 草）。单位：方块。
     */
    private static final int ENCROACH_ZONE_BLOCKS = 24;

    private AcademyTerrainCheckCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("iss_academy_terrain_check")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("nearest").executes(context -> run(context, true)))
                        .then(Commands.literal("here").executes(context -> run(context, false)))
                        .then(Commands.literal("profile")
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .then(Commands.argument("stepX", IntegerArgumentType.integer(-1, 1))
                                                        .then(Commands.argument("stepZ", IntegerArgumentType.integer(-1, 1))
                                                                .then(Commands.argument("length", IntegerArgumentType.integer(1, 64))
                                                                        .executes(AcademyTerrainCheckCommand::profile)))))))
        );
    }

    /**
     * 沿一条直线逐列打印「地面顶 Y / 顶方块 / 是否在水下」，定位某处高差到底是什么。
     */
    private static int profile(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        int startX = IntegerArgumentType.getInteger(context, "x");
        int startZ = IntegerArgumentType.getInteger(context, "z");
        int stepX = IntegerArgumentType.getInteger(context, "stepX");
        int stepZ = IntegerArgumentType.getInteger(context, "stepZ");
        int length = IntegerArgumentType.getInteger(context, "length");
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        StringBuilder line = new StringBuilder();
        for (int index = 0; index < length; index++) {
            int columnX = startX + stepX * index;
            int columnZ = startZ + stepZ * index;
            level.getChunk(columnX >> 4, columnZ >> 4);
            int highestY = level.getMaxBuildHeight() - 1;
            int topY = Integer.MIN_VALUE;
            BlockState topState = null;
            for (int y = highestY; y > level.getMinBuildHeight(); y--) {
                cursor.set(columnX, y, columnZ);
                BlockState state = level.getBlockState(cursor);
                if (!state.isAir()) {
                    topY = y;
                    topState = state;
                    break;
                }
            }
            SurfaceSample ground = naturalSurfaceTop(level, cursor, columnX, columnZ, topY == Integer.MIN_VALUE ? 64 : topY);
            String groundText = ground == null ? "?" : ground.topY() + (ground.underWater() ? "w" : "");
            String topText = topState == null ? "-" : BuiltInRegistries.BLOCK.getKey(topState.getBlock()).getPath();
            line.append(String.format("[%d,%d 地%s 顶%d:%s] ", columnX, columnZ, groundText, topY, topText));
        }
        report(source, "[剖面] " + line);
        return Command.SINGLE_SUCCESS;
    }

    private static int run(CommandContext<CommandSourceStack> context, boolean locateNearest) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Optional<Holder.Reference<Structure>> academyHolder = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .getHolder(ACADEMY_KEY);
        if (academyHolder.isEmpty()) {
            return fail(source, "结构未注册: " + ACADEMY_KEY.location());
        }
        Structure academy = academyHolder.get().value();
        BlockPos sourcePos = BlockPos.containing(source.getPosition());

        StructureStart start;
        if (locateNearest) {
            Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator().findNearestMapStructure(
                    level, HolderSet.direct(academyHolder.get()), sourcePos, LOCATE_SEARCH_RADIUS, false
            );
            if (found == null) {
                return fail(source, "附近找不到学院");
            }
            ChunkPos startChunk = new ChunkPos(found.getFirst());
            start = level.getChunk(startChunk.x, startChunk.z, ChunkStatus.STRUCTURE_STARTS)
                    .getStartForStructure(academy);
            report(source, "最近学院起点区块 " + startChunk + "，方块约 " + found.getFirst().toShortString());
        } else {
            start = level.structureManager().getStructureAt(sourcePos, academy);
        }
        if (start == null || !start.isValid()) {
            return fail(source, "没有拿到有效的学院 StructureStart");
        }

        BoundingBox wholeBox = start.getBoundingBox();
        ChunkPos minChunk = new ChunkPos(wholeBox.minX() >> 4, wholeBox.minZ() >> 4);
        ChunkPos maxChunk = new ChunkPos(wholeBox.maxX() >> 4, wholeBox.maxZ() >> 4);
        long generationStartNanos = System.nanoTime();
        setForced(level, minChunk, maxChunk, true);
        try {
            for (int chunkX = minChunk.x; chunkX <= maxChunk.x; chunkX++) {
                for (int chunkZ = minChunk.z; chunkZ <= maxChunk.z; chunkZ++) {
                    level.getChunk(chunkX, chunkZ);
                }
            }
            long generationMillis = (System.nanoTime() - generationStartNanos) / 1_000_000L;
            report(source, "区块就绪，用时 " + generationMillis + " ms，开始体检");
            analyze(source, level, start);
        } finally {
            setForced(level, minChunk, maxChunk, false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void analyze(CommandSourceStack source, ServerLevel level, StructureStart start) {
        BoundingBox footprint = null;
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof AcademyPiece) {
                footprint = footprint == null ? copyOf(piece.getBoundingBox()) : footprint.encapsulate(piece.getBoundingBox());
            }
        }
        if (footprint == null) {
            report(source, "学院没有模板片");
            return;
        }
        int floorY = footprint.minY();
        int groundY = floorY + AcademyStructure.TEMPLATE_GROUND_TOP_OFFSET_BLOCKS;
        report(source, String.format("脚印 X[%d,%d] Z[%d,%d] floorY=%d groundY=%d",
                footprint.minX(), footprint.maxX(), footprint.minZ(), footprint.maxZ(), floorY, groundY));

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        reportFloating(source, level, footprint, floorY, cursor);
        reportEdgeProfiles(source, level, footprint, floorY, groundY, cursor);
        reportTemplateIntrusions(source, level, start, footprint, cursor);
        reportEdgeDepths(source, start, footprint);
    }

    /** 4) 模板边缘露天深度（缓坡最多往院内推进多少格），每 4 格取一个点。 */
    private static void reportEdgeDepths(CommandSourceStack source, StructureStart start, BoundingBox footprint) {
        AcademyTemplateEdgeProfile edgeProfile = AcademyTemplateEdgeProfile.getOrCompute(
                new PiecesContainer(start.getPieces()), footprint, AcademyTerrainBlender.innerEncroachMaxBlocks()
        );
        if (edgeProfile == null) {
            report(source, "[露天深度] 剖面不可用");
            return;
        }
        StringBuilder north = new StringBuilder();
        StringBuilder south = new StringBuilder();
        for (int x = footprint.minX(); x <= footprint.maxX(); x += 4) {
            north.append(Math.round(edgeProfile.allowedInnerDepth(x, footprint.minZ(), footprint))).append(' ');
            south.append(Math.round(edgeProfile.allowedInnerDepth(x, footprint.maxZ(), footprint))).append(' ');
        }
        StringBuilder west = new StringBuilder();
        StringBuilder east = new StringBuilder();
        for (int z = footprint.minZ(); z <= footprint.maxZ(); z += 4) {
            west.append(Math.round(edgeProfile.allowedInnerDepth(footprint.minX(), z, footprint))).append(' ');
            east.append(Math.round(edgeProfile.allowedInnerDepth(footprint.maxX(), z, footprint))).append(' ');
        }
        report(source, "[露天深度] 北(X+4): " + north);
        report(source, "[露天深度] 南(X+4): " + south);
        report(source, "[露天深度] 西(Z+4): " + west);
        report(source, "[露天深度] 东(Z+4): " + east);
    }

    /** 1) 悬空：模板底面下一格是空气 / 液体的列数。 */
    private static void reportFloating(
            CommandSourceStack source,
            ServerLevel level,
            BoundingBox footprint,
            int floorY,
            BlockPos.MutableBlockPos cursor
    ) {
        int floatingColumns = 0;
        int totalColumns = 0;
        String firstFloating = "-";
        for (int x = footprint.minX(); x <= footprint.maxX(); x++) {
            for (int z = footprint.minZ(); z <= footprint.maxZ(); z++) {
                totalColumns++;
                cursor.set(x, floorY - 1, z);
                BlockState below = level.getBlockState(cursor);
                if (below.isAir() || !below.getFluidState().isEmpty()) {
                    if (floatingColumns == 0) {
                        firstFloating = cursor.toShortString() + " " + BuiltInRegistries.BLOCK.getKey(below.getBlock()).getPath();
                    }
                    floatingColumns++;
                }
            }
        }
        report(source, String.format("[悬空] 模板底面下方为空气/液体的列: %d / %d，首个 @ %s",
                floatingColumns, totalColumns, firstFloating));
    }

    /**
     * 2) 边缘剖面：沿四条边每一格往外走，记录相邻两列地面高差。
     * d=0 为模板边缘列（取模板自带地面顶），d≥1 为外侧列（取自然地面顶）。
     */
    private static void reportEdgeProfiles(
            CommandSourceStack source,
            ServerLevel level,
            BoundingBox footprint,
            int floorY,
            int groundY,
            BlockPos.MutableBlockPos cursor
    ) {
        int profileLength = AcademyTerrainBlender.blendExtentPaddingBlocks() + PROFILE_EXTRA_BLOCKS;
        int[][] sides = {
                // startX, startZ, alongX, alongZ, outwardX, outwardZ, length
                {footprint.minX(), footprint.minZ(), 1, 0, 0, -1, footprint.getXSpan()},
                {footprint.minX(), footprint.maxZ(), 1, 0, 0, 1, footprint.getXSpan()},
                {footprint.minX(), footprint.minZ(), 0, 1, -1, 0, footprint.getZSpan()},
                {footprint.maxX(), footprint.minZ(), 0, 1, 1, 0, footprint.getZSpan()},
        };
        String[] sideNames = {"北", "南", "西", "东"};
        int bandLength = AcademyTerrainBlender.blendExtentPaddingBlocks();
        int totalSteps = 0;
        int drySteepSteps = 0;
        int dryCliffSteps = 0;
        int waterCliffSteps = 0;
        int beyondBandCliffSteps = 0;
        int dryMaxStep = 0;
        int edgeSteepCount = 0;
        int chunkSeamSteepSteps = 0;
        List<String> worstDryCliffs = new ArrayList<>();
        StringBuilder perSide = new StringBuilder();
        for (int sideIndex = 0; sideIndex < sides.length; sideIndex++) {
            int[] side = sides[sideIndex];
            int sideMaxStep = 0;
            int sideCliffs = 0;
            for (int along = 0; along < side[6]; along++) {
                int edgeX = side[0] + side[2] * along;
                int edgeZ = side[1] + side[3] * along;
                int edgeTop = templateGroundTop(level, cursor, edgeX, edgeZ, floorY);
                if (edgeTop == Integer.MIN_VALUE) {
                    continue;
                }
                SurfaceSample previous = new SurfaceSample(edgeTop, false);
                for (int distance = 1; distance <= profileLength; distance++) {
                    int columnX = edgeX + side[4] * distance;
                    int columnZ = edgeZ + side[5] * distance;
                    SurfaceSample current = naturalSurfaceTop(level, cursor, columnX, columnZ, groundY);
                    if (current == null) {
                        break;
                    }
                    int step = Math.abs(current.topY() - previous.topY());
                    boolean touchesWater = current.underWater() || previous.underWater();
                    boolean inBand = distance <= bandLength;
                    totalSteps++;
                    if (step >= CLIFF_STEP_BLOCKS && touchesWater) {
                        waterCliffSteps++;
                    } else if (step >= CLIFF_STEP_BLOCKS && !inBand) {
                        beyondBandCliffSteps++;
                    } else if (!touchesWater && inBand) {
                        if (step >= STEEP_STEP_BLOCKS) {
                            drySteepSteps++;
                            if (distance == 1) {
                                edgeSteepCount++;
                            }
                            int previousX = columnX - side[4];
                            int previousZ = columnZ - side[5];
                            if ((previousX >> 4) != (columnX >> 4) || (previousZ >> 4) != (columnZ >> 4)) {
                                chunkSeamSteepSteps++;
                            }
                        }
                        if (step >= CLIFF_STEP_BLOCKS) {
                            dryCliffSteps++;
                            sideCliffs++;
                            if (worstDryCliffs.size() < 12) {
                                worstDryCliffs.add(String.format("(%d,%d d=%d %d→%d)",
                                        columnX, columnZ, distance, previous.topY(), current.topY()));
                            }
                        }
                        dryMaxStep = Math.max(dryMaxStep, step);
                        sideMaxStep = Math.max(sideMaxStep, step);
                    }
                    previous = current;
                }
            }
            perSide.append(String.format(" %s:最大%d/断崖%d", sideNames[sideIndex], sideMaxStep, sideCliffs));
        }
        report(source, String.format(
                "[边缘] 高差样本 %d；缓坡带内旱地：≥%d 格陡坎 %d（紧贴边缘 %d），≥%d 格断崖 %d，最大 %d 格",
                totalSteps, STEEP_STEP_BLOCKS, drySteepSteps, edgeSteepCount, CLIFF_STEP_BLOCKS, dryCliffSteps, dryMaxStep
        ));
        report(source, String.format(
                "[边缘] 另计：涉及水体（湖底/河床）的 ≥%d 格高差 %d，缓坡带外自然地形 %d；带内陡坎落在区块缝上的 %d（随机分布约占 1/16）",
                CLIFF_STEP_BLOCKS, waterCliffSteps, beyondBandCliffSteps, chunkSeamSteepSteps
        ));
        report(source, "[边缘] 分边:" + perSide);
        if (!worstDryCliffs.isEmpty()) {
            report(source, "[边缘] 带内旱地断崖样例: " + String.join(" ", worstDryCliffs));
        }
    }

    /** 外侧列地面顶与其上是否有水。 */
    private record SurfaceSample(int topY, boolean underWater) {
    }

    /**
     * 3) 模板内多出来的方块：模板记录为空气的位置，世界里却是实心方块（不算植物 / 树 / 液体 / 雪）。
     * 边缘 {@link #ENCROACH_ZONE_BLOCKS} 格内的泥土 / 草视为允许的缓坡垫高，单独计数。
     */
    private static void reportTemplateIntrusions(
            CommandSourceStack source,
            ServerLevel level,
            StructureStart start,
            BoundingBox footprint,
            BlockPos.MutableBlockPos cursor
    ) {
        Field palettesField;
        try {
            palettesField = StructureTemplate.class.getDeclaredField("palettes");
            palettesField.setAccessible(true);
        } catch (ReflectiveOperationException exception) {
            report(source, "[模板内] 无法读取模板调色板，跳过: " + exception);
            return;
        }
        int deepIntrusions = 0;
        int encroachZoneFill = 0;
        int encroachZoneOther = 0;
        Map<String, Integer> deepIntrusionBlocks = new HashMap<>();
        BlockPos firstDeepIntrusion = null;
        for (StructurePiece piece : start.getPieces()) {
            if (!(piece instanceof AcademyPiece academyPiece)) {
                continue;
            }
            List<StructureTemplate.StructureBlockInfo> blockInfos;
            try {
                @SuppressWarnings("unchecked")
                List<StructureTemplate.Palette> palettes =
                        (List<StructureTemplate.Palette>) palettesField.get(academyPiece.template());
                if (palettes.isEmpty()) {
                    continue;
                }
                blockInfos = palettes.get(0).blocks();
            } catch (ReflectiveOperationException exception) {
                report(source, "[模板内] 读取模板失败: " + exception);
                return;
            }
            BlockPos templateOrigin = academyPiece.templatePosition();
            for (StructureTemplate.StructureBlockInfo blockInfo : blockInfos) {
                if (!blockInfo.state().isAir()) {
                    continue;
                }
                cursor.setWithOffset(templateOrigin, blockInfo.pos());
                BlockState worldState = level.getBlockState(cursor);
                if (isAllowedInTemplateAir(worldState) || isDecorationPlant(worldState)) {
                    continue;
                }
                float signedDistance = AcademyTerrainBlender.signedDistanceToFootprintEdge(
                        cursor.getX(), cursor.getZ(), footprint
                );
                boolean inEncroachZone = signedDistance >= -ENCROACH_ZONE_BLOCKS;
                boolean isFillMaterial = worldState.is(BlockTags.DIRT) || worldState.is(BlockTags.SAND);
                if (inEncroachZone && isFillMaterial) {
                    encroachZoneFill++;
                    continue;
                }
                if (inEncroachZone) {
                    encroachZoneOther++;
                }
                deepIntrusions++;
                if (firstDeepIntrusion == null) {
                    firstDeepIntrusion = cursor.immutable();
                }
                String blockId = BuiltInRegistries.BLOCK.getKey(worldState.getBlock()).toString();
                deepIntrusionBlocks.merge(blockId, 1, Integer::sum);
            }
        }
        StringBuilder topBlocks = new StringBuilder();
        deepIntrusionBlocks.entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getValue(), left.getValue()))
                .limit(8)
                .forEach(entry -> topBlocks.append(' ').append(entry.getKey()).append('×').append(entry.getValue()));
        report(source, String.format(
                "[模板内] 模板空气位上出现的非允许方块 %d（其中边缘区非泥土 %d），首个 @ %s；边缘缓坡泥土/草 %d",
                deepIntrusions, encroachZoneOther,
                firstDeepIntrusion == null ? "-" : firstDeepIntrusion.toShortString(), encroachZoneFill
        ));
        if (!topBlocks.isEmpty()) {
            report(source, "[模板内] 方块分布:" + topBlocks);
        }
    }

    /** 模板空气位上允许出现的东西：后续装饰阶段长出的植物 / 树、液体、雪层。 */
    private static boolean isAllowedInTemplateAir(BlockState state) {
        return state.isAir()
                || !state.getFluidState().isEmpty()
                || state.canBeReplaced()
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.is(BlockTags.ICE);
    }

    /** 模板边缘列自带地面顶：从 floorY 起连续实心方块的最高一格（树干 / 树叶 / 植物不算地面）。 */
    private static int templateGroundTop(ServerLevel level, BlockPos.MutableBlockPos cursor, int x, int z, int floorY) {
        int top = Integer.MIN_VALUE;
        for (int y = floorY; y < floorY + 40; y++) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            if (state.isAir() || !state.getFluidState().isEmpty() || state.canBeReplaced()
                    || isAllowedInTemplateAir(state) || isDecorationPlant(state)) {
                break;
            }
            top = y;
        }
        return top;
    }

    /** 外侧列自然地面顶：自上而下跳过空气 / 液体 / 植物 / 树，取第一格实心；记录途中是否穿过水。 */
    private static SurfaceSample naturalSurfaceTop(
            ServerLevel level,
            BlockPos.MutableBlockPos cursor,
            int x,
            int z,
            int groundY
    ) {
        int bottom = Math.max(level.getMinBuildHeight(), groundY - 96);
        boolean passedWater = false;
        for (int y = Math.min(level.getMaxBuildHeight() - 1, groundY + SURFACE_SCAN_UP_BLOCKS); y >= bottom; y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            if (!state.getFluidState().isEmpty() || state.is(BlockTags.ICE)) {
                passedWater = true;
                continue;
            }
            if (isAllowedInTemplateAir(state) || isDecorationPlant(state) || state.is(Tags.Blocks.FENCES)) {
                continue;
            }
            return new SurfaceSample(y, passedWater);
        }
        return null;
    }

    /** 装饰阶段长在地上的非树植物（甘蔗、睡莲、蘑菇、南瓜等）。 */
    private static boolean isDecorationPlant(BlockState state) {
        return state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.LILY_PAD)
                || state.is(Blocks.BROWN_MUSHROOM)
                || state.is(Blocks.RED_MUSHROOM)
                || state.is(Blocks.PUMPKIN)
                || state.is(Blocks.MELON)
                || state.is(Blocks.MOSS_CARPET)
                || state.is(Blocks.CAVE_VINES)
                || state.is(Blocks.CAVE_VINES_PLANT)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.BEE_NEST)
                || state.is(Blocks.MANGROVE_ROOTS)
                || state.is(Blocks.MANGROVE_PROPAGULE);
    }

    private static BoundingBox copyOf(BoundingBox box) {
        return new BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }

    private static void setForced(ServerLevel level, ChunkPos minChunk, ChunkPos maxChunk, boolean forced) {
        for (int chunkX = minChunk.x; chunkX <= maxChunk.x; chunkX++) {
            for (int chunkZ = minChunk.z; chunkZ <= maxChunk.z; chunkZ++) {
                level.setChunkForced(chunkX, chunkZ, forced);
            }
        }
    }

    private static void report(CommandSourceStack source, String message) {
        EldenRingSpellsMod.LOGGER.info("[iss_academy_terrain_check] {}", message);
        source.sendSuccess(() -> Component.literal("[学院地形体检] " + message), false);
    }

    private static int fail(CommandSourceStack source, String message) {
        EldenRingSpellsMod.LOGGER.warn("[iss_academy_terrain_check] {}", message);
        source.sendFailure(Component.literal("[学院地形体检] " + message));
        return 0;
    }
}

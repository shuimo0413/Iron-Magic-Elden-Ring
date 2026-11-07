package com.eldenring.spells.worldgen.structure;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 雷亚卢卡利亚学院：把已切好的格子模板按固定偏移铺进世界。
 * <p>
 * 体量约 212×180×231，超过 jigsaw {@code max_distance_from_center} 上限，故不用 jigsaw。
 * 群系偏好：沼泽 / 红树林一律通过；平原（含向日葵平原）仅约 20% 通过，避免平原刷脸。
 * 间距由数据包 {@code structure_set} 控制（极稀有）。
 * <p>
 * <b>必须把学院中心对准结构起点区块</b>：原版 {@code ChunkGenerator#createReferences}
 * 只向周围 ±8 区块传播结构引用（约 128 格）。若从角落往外铺 212×231，远端格子
 * 收不到引用，整翼会直接消失。半宽约 106 / 半深约 115，居中后落在 8 区块内。
 * <p>
 * 选址会拒绝周围过高 / 脚印高差过大的点；学院地面高度取脚印外一圈噪声地面的中位数
 * （见 {@link #chooseAcademyGroundY}）。落成后走 {@link AcademyTerrainBlender}
 * （模板底面以下补实 + 脚印外缓坡 + 外侧更高时边缘平地垫高）。
 * 另挂 {@link AcademyBlendExtentPiece} 外扩包围盒：外扩量受 ±8 区块引用半径限制，
 * Z 方向半深 115 时最多再外扩 20 格。
 */
public class AcademyStructure extends Structure {

    public static final MapCodec<AcademyStructure> CODEC = simpleCodec(AcademyStructure::new);

    /**
     * 单片边长（方块），与导出切分一致。
     * 调大 / 调小必须与 NBT 文件名网格同步，否则会错缝。
     */
    public static final int PIECE_SIZE_BLOCKS = 48;

    /**
     * 整栋学院在 X / Y / Z 上的尺寸（方块），来自 {@code pieces_manifest.txt}。
     * 用于把结构原点算到「中心对准起点区块」，勿与单片 48 混淆。
     */
    public static final int STRUCTURE_SIZE_X = 212;
    public static final int STRUCTURE_SIZE_Y = 180;
    public static final int STRUCTURE_SIZE_Z = 231;

    /** X 方向格子数（0..4）。 */
    private static final int GRID_COUNT_X = 5;
    /** Y 方向格子数（0..3）。 */
    private static final int GRID_COUNT_Y = 4;
    /** Z 方向格子数（0..4）。 */
    private static final int GRID_COUNT_Z = 5;

    /**
     * 平原候选点接受概率。沼泽不受此限制。
     * 调大 → 平原学院更多；调小 → 更集中在沼泽。
     */
    private static final float PLAINS_ACCEPT_CHANCE = 0.20F;

    /**
     * 脚印采样点相对锚点地表，允许高出的最大值。
     * 调大 → 更易贴悬崖生成、边缘更易撕裂；调小 → 只在较平坦处出现。
     * 单位：方块。
     */
    private static final int MAX_SURROUNDING_ABOVE_ANCHOR_BLOCKS = 12;

    /**
     * 脚印（及外侧一圈）采样点的最大高差（最高-最低）。
     * 调大 → 允许更起伏地形；调小 → 只要相对平缓的落点。
     * 单位：方块。
     */
    private static final int MAX_FOOTPRINT_RELIEF_BLOCKS = 18;

    /**
     * 锚点相对脚印内最低采样点，允许的悬空落差。
     * 调大 → 更易骑在深谷/河面上；调小 → 拒绝大片底部悬空的选址。
     * 单位：方块。
     */
    private static final int MAX_ANCHOR_ABOVE_LOWEST_BLOCKS = 16;

    /**
     * 地形采样网格每边点数（含四角与中心）。调大 → 更严、更耗；调小 → 可能漏检局部尖峰。
     */
    private static final int TERRAIN_SAMPLE_GRID = 5;

    /**
     * 模板自带地面（底部约 3 层泥土）的顶面相对模板底 floorY 的高度。
     * 由 {@code 工具链/analyze_academy_template.py} 统计：四条边地面几乎全在模板 y=2。
     * 重新导出模板后若地面厚度变了，要同步改这里，否则学院会整体高出 / 陷进周围地面。
     * 单位：方块。
     */
    public static final int TEMPLATE_GROUND_TOP_OFFSET_BLOCKS = 2;

    /**
     * 选学院地面高度时，环形采样点离脚印外缘的距离。调大 → 更看远处地形；调小 → 更看贴边地形。
     * 单位：方块。
     */
    private static final int GROUND_RING_OFFSET_BLOCKS = 10;

    /**
     * 环形采样点沿周长的间距。调小 → 采样更密更准但更耗（只在选址成功后算一次）。
     * 单位：方块。
     */
    private static final int GROUND_RING_STEP_BLOCKS = 16;

    /**
     * 全部格子名（5×4×5=100）。新导出版本学者可能落在旧版「空片」上，必须全生成。
     */
    private static final String[] PIECE_NAMES = buildAllPieceNames();

    public AcademyStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    private static String[] buildAllPieceNames() {
        List<String> names = new ArrayList<>(GRID_COUNT_X * GRID_COUNT_Y * GRID_COUNT_Z);
        for (int y = 0; y < GRID_COUNT_Y; y++) {
            for (int x = 0; x < GRID_COUNT_X; x++) {
                for (int z = 0; z < GRID_COUNT_Z; z++) {
                    names.add("x" + x + "_y" + y + "_z" + z);
                }
            }
        }
        return names.toArray(String[]::new);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int anchorX = context.chunkPos().getMiddleBlockX();
        int anchorZ = context.chunkPos().getMiddleBlockZ();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        int surfaceY = chunkGenerator.getFirstOccupiedHeight(
                anchorX,
                anchorZ,
                Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(),
                context.randomState()
        );
        BlockPos biomeSamplePos = new BlockPos(anchorX, surfaceY, anchorZ);

        Holder<Biome> biome = context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(anchorX),
                QuartPos.fromBlock(surfaceY),
                QuartPos.fromBlock(anchorZ),
                context.randomState().sampler()
        );
        if (!passesBiomePreference(biome, context.random())) {
            return Optional.empty();
        }
        if (!passesTerrainRelief(chunkGenerator, context, anchorX, anchorZ, surfaceY)) {
            return Optional.empty();
        }

        // 中心对准起点区块中心，保证整栋落在 ±8 区块引用半径内。
        int originX = anchorX - STRUCTURE_SIZE_X / 2;
        int originZ = anchorZ - STRUCTURE_SIZE_Z / 2;
        int academyGroundY = chooseAcademyGroundY(chunkGenerator, context, originX, originZ);
        BlockPos structureOrigin = new BlockPos(
                originX,
                academyGroundY - TEMPLATE_GROUND_TOP_OFFSET_BLOCKS,
                originZ
        );

        return Optional.of(new GenerationStub(biomeSamplePos, builder ->
                generatePieces(builder, context.structureTemplateManager(), structureOrigin)));
    }

    /**
     * 学院地面（模板边缘草皮）放在多高：取脚印外一圈自然地面（水底）高度的中位数，
     * 且不低于海平面，避免沼泽里学院地面泡在水下。
     * <p>
     * 旧做法用中心一点的地表（含水面）再 +2，周围普遍比学院低 / 高时整圈都是断崖；
     * 中位数让一半边缘略高、一半略低，削垫量最小，缓坡最容易接上。
     */
    private static int chooseAcademyGroundY(
            ChunkGenerator chunkGenerator,
            GenerationContext context,
            int originX,
            int originZ
    ) {
        int ringMinX = originX - GROUND_RING_OFFSET_BLOCKS;
        int ringMaxX = originX + STRUCTURE_SIZE_X - 1 + GROUND_RING_OFFSET_BLOCKS;
        int ringMinZ = originZ - GROUND_RING_OFFSET_BLOCKS;
        int ringMaxZ = originZ + STRUCTURE_SIZE_Z - 1 + GROUND_RING_OFFSET_BLOCKS;
        List<Integer> ringHeights = new ArrayList<>();
        for (int sampleX = ringMinX; sampleX <= ringMaxX; sampleX += GROUND_RING_STEP_BLOCKS) {
            ringHeights.add(sampleRingGround(chunkGenerator, context, sampleX, ringMinZ));
            ringHeights.add(sampleRingGround(chunkGenerator, context, sampleX, ringMaxZ));
        }
        for (int sampleZ = ringMinZ + GROUND_RING_STEP_BLOCKS; sampleZ < ringMaxZ; sampleZ += GROUND_RING_STEP_BLOCKS) {
            ringHeights.add(sampleRingGround(chunkGenerator, context, ringMinX, sampleZ));
            ringHeights.add(sampleRingGround(chunkGenerator, context, ringMaxX, sampleZ));
        }
        ringHeights.sort(Integer::compare);
        int medianHeight = ringHeights.get(ringHeights.size() / 2);
        return Math.max(medianHeight, chunkGenerator.getSeaLevel());
    }

    private static int sampleRingGround(
            ChunkGenerator chunkGenerator,
            GenerationContext context,
            int sampleX,
            int sampleZ
    ) {
        return AcademyTerrainSnapshot.sampleNaturalGroundY(
                chunkGenerator, context.randomState(), context.heightAccessor(), sampleX, sampleZ
        );
    }

    /**
     * 拒绝周围过高 / 脚印高差过大 / 谷地过深的落点，减轻边缘撕裂与底部悬空。
     */
    private static boolean passesTerrainRelief(
            ChunkGenerator chunkGenerator,
            GenerationContext context,
            int anchorX,
            int anchorZ,
            int anchorSurfaceY
    ) {
        int originX = anchorX - STRUCTURE_SIZE_X / 2;
        int originZ = anchorZ - STRUCTURE_SIZE_Z / 2;
        int maxHeight = anchorSurfaceY;
        int minHeight = anchorSurfaceY;
        int lastIndex = TERRAIN_SAMPLE_GRID - 1;

        for (int gridX = 0; gridX <= lastIndex; gridX++) {
            for (int gridZ = 0; gridZ <= lastIndex; gridZ++) {
                int sampleX = originX + gridX * (STRUCTURE_SIZE_X - 1) / lastIndex;
                int sampleZ = originZ + gridZ * (STRUCTURE_SIZE_Z - 1) / lastIndex;
                int sampleY = chunkGenerator.getFirstOccupiedHeight(
                        sampleX,
                        sampleZ,
                        Heightmap.Types.WORLD_SURFACE_WG,
                        context.heightAccessor(),
                        context.randomState()
                );
                maxHeight = Math.max(maxHeight, sampleY);
                minHeight = Math.min(minHeight, sampleY);
            }
        }

        // 外侧一圈：专门抓「贴着高山切开」的候选点
        int ringPad = 16;
        int[] ringX = {
                originX - ringPad,
                originX + STRUCTURE_SIZE_X / 2,
                originX + STRUCTURE_SIZE_X - 1 + ringPad
        };
        int[] ringZ = {
                originZ - ringPad,
                originZ + STRUCTURE_SIZE_Z / 2,
                originZ + STRUCTURE_SIZE_Z - 1 + ringPad
        };
        for (int sampleX : ringX) {
            for (int sampleZ : ringZ) {
                int sampleY = chunkGenerator.getFirstOccupiedHeight(
                        sampleX,
                        sampleZ,
                        Heightmap.Types.WORLD_SURFACE_WG,
                        context.heightAccessor(),
                        context.randomState()
                );
                maxHeight = Math.max(maxHeight, sampleY);
                minHeight = Math.min(minHeight, sampleY);
            }
        }

        if (maxHeight - anchorSurfaceY > MAX_SURROUNDING_ABOVE_ANCHOR_BLOCKS) {
            return false;
        }
        if (maxHeight - minHeight > MAX_FOOTPRINT_RELIEF_BLOCKS) {
            return false;
        }
        if (anchorSurfaceY - minHeight > MAX_ANCHOR_ABOVE_LOWEST_BLOCKS) {
            return false;
        }
        return true;
    }

    /**
     * 沼泽 / 红树林沼泽必过；平原系按 {@link #PLAINS_ACCEPT_CHANCE} 抽签。
     * <p>
     * 1.21.1 原版没有 {@code BiomeTags.IS_SWAMP} / {@code #minecraft:is_swamp}，
     * 所以这里直接比对 biome 键。
     */
    private static boolean passesBiomePreference(Holder<Biome> biome, WorldgenRandom random) {
        if (biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP)) {
            return true;
        }
        if (biome.is(Biomes.PLAINS) || biome.is(Biomes.SUNFLOWER_PLAINS)) {
            return random.nextFloat() < PLAINS_ACCEPT_CHANCE;
        }
        return false;
    }

    private static void generatePieces(
            StructurePiecesBuilder builder,
            StructureTemplateManager templateManager,
            BlockPos structureOrigin
    ) {
        for (String pieceName : PIECE_NAMES) {
            int indexX = parseIndex(pieceName, 'x');
            int indexY = parseIndex(pieceName, 'y');
            int indexZ = parseIndex(pieceName, 'z');
            BlockPos pieceOrigin = structureOrigin.offset(
                    indexX * PIECE_SIZE_BLOCKS,
                    indexY * PIECE_SIZE_BLOCKS,
                    indexZ * PIECE_SIZE_BLOCKS
            );
            ResourceLocation templateId = ResourceLocation.fromNamespaceAndPath(
                    EldenRingSpellsMod.MOD_ID,
                    "academy/" + pieceName
            );
            builder.addPiece(new AcademyPiece(templateManager, templateId, pieceOrigin));
        }

        // 外扩空片：让 StructureStart 包围盒盖住外侧缓坡带，邻接列所在区块也会 afterPlace
        int pad = AcademyTerrainBlender.blendExtentPaddingBlocks();
        BoundingBox footprint = new BoundingBox(
                structureOrigin.getX(),
                structureOrigin.getY(),
                structureOrigin.getZ(),
                structureOrigin.getX() + STRUCTURE_SIZE_X - 1,
                structureOrigin.getY() + STRUCTURE_SIZE_Y - 1,
                structureOrigin.getZ() + STRUCTURE_SIZE_Z - 1
        );
        builder.addPiece(new AcademyBlendExtentPiece(new BoundingBox(
                footprint.minX() - pad,
                footprint.minY(),
                footprint.minZ() - pad,
                footprint.maxX() + pad,
                footprint.maxY(),
                footprint.maxZ() + pad
        )));
    }

    /**
     * 从 {@code x2_y0_z1} 这类名字解析某一轴下标。
     */
    private static int parseIndex(String pieceName, char axis) {
        int axisIndex = pieceName.indexOf(axis);
        if (axisIndex < 0) {
            throw new IllegalArgumentException("学院格子名缺少轴 '" + axis + "': " + pieceName);
        }
        int valueStart = axisIndex + 1;
        int valueEnd = valueStart;
        while (valueEnd < pieceName.length() && Character.isDigit(pieceName.charAt(valueEnd))) {
            valueEnd++;
        }
        return Integer.parseInt(pieceName, valueStart, valueEnd, 10);
    }

    @Override
    public void afterPlace(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BoundingBox chunkBox,
            ChunkPos chunkPos,
            PiecesContainer pieces
    ) {
        // 必须用模板脚印，不能用含 BlendExtent 的总盒，否则内侧抬坡距离会算错
        BoundingBox footprint = academyTemplateFootprint(pieces);
        if (footprint != null) {
            AcademyTerrainSnapshot snapshot = new AcademyTerrainSnapshot(
                    chunkGenerator,
                    level.getLevel().getChunkSource().randomState(),
                    level
            );
            AcademyTemplateEdgeProfile edgeProfile = AcademyTemplateEdgeProfile.getOrCompute(
                    pieces, footprint, AcademyTerrainBlender.innerEncroachMaxBlocks()
            );
            AcademyTerrainBlender.blend(
                    level,
                    chunkBox,
                    footprint,
                    footprint.minY() + TEMPLATE_GROUND_TOP_OFFSET_BLOCKS,
                    snapshot,
                    edgeProfile
            );
        }
    }

    /**
     * 合并所有模板片包围盒，排除 {@link AcademyBlendExtentPiece} 外扩空片。
     */
    private static BoundingBox academyTemplateFootprint(PiecesContainer pieces) {
        BoundingBox footprint = null;
        for (var piece : pieces.pieces()) {
            if (piece instanceof AcademyBlendExtentPiece) {
                continue;
            }
            BoundingBox pieceBox = piece.getBoundingBox();
            if (footprint == null) {
                footprint = new BoundingBox(
                        pieceBox.minX(), pieceBox.minY(), pieceBox.minZ(),
                        pieceBox.maxX(), pieceBox.maxY(), pieceBox.maxZ()
                );
            } else {
                footprint.encapsulate(pieceBox);
            }
        }
        return footprint;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.ACADEMY.get();
    }
}

package com.eldenring.spells.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 世界生成等「进游戏前就要定下来」的数值。
 * <p>
 * 文件：{@code config/elden_ring_spells-common.toml}（客户端与服务端各读各的，不联网同步）。
 * 整合包改矿洞密度放这里；改完需新区块或新世界才看得到。
 * <p>
 * {@code defineInRange} 绑 toml；{@link #apply()} 写到下面这组运行时字段。逻辑读运行时字段，不读 Spec {@code .get()}。
 * 噪声盐写死在 {@code GlintstoneCaveFeature}，不开放配置。
 */
public final class EldenRingCommonConfig {

    public static final ModConfigSpec SPEC;

    /** 大尺度噪声单元格（chunk）。越大 → 同色辉石地带斑块越大。 */
    public static double caveRegionNoiseCellSizeChunks = 6.0;
    /** chunk 门控噪声单元格。1 ≈ 每 chunk 独立抽样。 */
    public static double caveChunkNoiseCellSizeChunks = 1.0;
    /** 大尺度噪声阈值 [0,1]。调高 → 辉石地带更稀有。 */
    public static double caveRegionPresenceThreshold = 0.70;
    /** 地带内 chunk 门控阈值 [0,1]。调高 → 矿洞更稀疏。 */
    public static double caveChunkDecorateThreshold = 0.66;
    /** 扫描洞穴表面的最低 Y（方块绝对高度）。 */
    public static int caveScanMinY = -56;
    /** 扫描洞穴表面的最高 Y（方块绝对高度）。 */
    public static int caveScanMaxY = 48;
    /** 合格 chunk 内囊状矿洞数量。 */
    public static int cavePocketsPerChunk = 1;
    /** 囊心搜索步长（方块）。调小更不容易漏细缝，世界生成更慢。 */
    public static int cavePocketCenterScanStride = 2;
    /** 囊状矿洞半径（方块）。 */
    public static int cavePocketRadiusBlocks = 6;
    /** 囊内邻接空气的石头被换成水晶块的概率 [0,1]。 */
    public static float caveSurfaceBlockChance = 0.42f;
    /** 水晶块表面再插完整水晶簇的概率 [0,1]。 */
    public static float caveSurfaceClusterOnBlockChance = 0.48f;
    /** 仍是石头的表面插水晶簇的概率 [0,1]。 */
    public static float caveSurfaceClusterOnStoneChance = 0.08f;
    /** 单 chunk 水晶簇硬上限。 */
    public static int caveMaxClustersPerChunk = 64;

    private static final ModConfigSpec.DoubleValue CAVE_REGION_NOISE_CELL_SIZE_CHUNKS;
    private static final ModConfigSpec.DoubleValue CAVE_CHUNK_NOISE_CELL_SIZE_CHUNKS;
    private static final ModConfigSpec.DoubleValue CAVE_REGION_PRESENCE_THRESHOLD;
    private static final ModConfigSpec.DoubleValue CAVE_CHUNK_DECORATE_THRESHOLD;
    private static final ModConfigSpec.IntValue CAVE_SCAN_MIN_Y;
    private static final ModConfigSpec.IntValue CAVE_SCAN_MAX_Y;
    private static final ModConfigSpec.IntValue CAVE_POCKETS_PER_CHUNK;
    private static final ModConfigSpec.IntValue CAVE_POCKET_CENTER_SCAN_STRIDE;
    private static final ModConfigSpec.IntValue CAVE_POCKET_RADIUS_BLOCKS;
    private static final ModConfigSpec.DoubleValue CAVE_SURFACE_BLOCK_CHANCE;
    private static final ModConfigSpec.DoubleValue CAVE_SURFACE_CLUSTER_ON_BLOCK_CHANCE;
    private static final ModConfigSpec.DoubleValue CAVE_SURFACE_CLUSTER_ON_STONE_CHANCE;
    private static final ModConfigSpec.IntValue CAVE_MAX_CLUSTERS_PER_CHUNK;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment(
                "辉石矿洞世界生成。只装饰现成洞穴表面，不新挖空洞。",
                "噪声盐不开放配置，避免整合包误改导致同种子矿洞错位。"
        );
        builder.push("glintstone_caves");

        CAVE_REGION_NOISE_CELL_SIZE_CHUNKS = ConfigSpecHelper.floating(
                builder,
                "region_noise_cell_size_chunks",
                "大尺度噪声单元格（chunk）。越大 → 同色辉石地带斑块越大。",
                caveRegionNoiseCellSizeChunks,
                1.0,
                64.0
        );
        CAVE_CHUNK_NOISE_CELL_SIZE_CHUNKS = ConfigSpecHelper.floating(
                builder,
                "chunk_noise_cell_size_chunks",
                "chunk 门控噪声单元格。1 ≈ 每 chunk 独立抽样；再大则矿洞更易连成片。",
                caveChunkNoiseCellSizeChunks,
                0.25,
                16.0
        );
        CAVE_REGION_PRESENCE_THRESHOLD = ConfigSpecHelper.floating(
                builder,
                "region_presence_threshold",
                "大尺度噪声阈值 [0,1]。调高 → 辉石地带更稀有。",
                caveRegionPresenceThreshold,
                0.0,
                1.0
        );
        CAVE_CHUNK_DECORATE_THRESHOLD = ConfigSpecHelper.floating(
                builder,
                "chunk_decorate_threshold",
                "地带内 chunk 门控阈值 [0,1]。调高 → 矿洞更稀疏。",
                caveChunkDecorateThreshold,
                0.0,
                1.0
        );
        CAVE_SCAN_MIN_Y = ConfigSpecHelper.integer(
                builder,
                "scan_min_y",
                "扫描洞穴表面的最低 Y（方块绝对高度）。",
                caveScanMinY,
                -64,
                320
        );
        CAVE_SCAN_MAX_Y = ConfigSpecHelper.integer(
                builder,
                "scan_max_y",
                "扫描洞穴表面的最高 Y（方块绝对高度）。",
                caveScanMaxY,
                -64,
                320
        );
        CAVE_POCKETS_PER_CHUNK = ConfigSpecHelper.integer(
                builder,
                "pockets_per_chunk",
                "合格 chunk 内囊状矿洞数量。1 = 一 chunk 一处。",
                cavePocketsPerChunk,
                0,
                8
        );
        CAVE_POCKET_CENTER_SCAN_STRIDE = ConfigSpecHelper.integer(
                builder,
                "pocket_center_scan_stride",
                "囊心搜索步长（方块）。调小更不容易漏细缝，世界生成更慢。",
                cavePocketCenterScanStride,
                1,
                8
        );
        CAVE_POCKET_RADIUS_BLOCKS = ConfigSpecHelper.integer(
                builder,
                "pocket_radius_blocks",
                "囊状矿洞半径（方块）。调小更袖珍；调大更容易沿隧道铺开。",
                cavePocketRadiusBlocks,
                1,
                32
        );
        CAVE_SURFACE_BLOCK_CHANCE = ConfigSpecHelper.floating(
                builder,
                "surface_block_chance",
                "囊内邻接空气的石头被换成水晶块的概率 [0,1]。",
                caveSurfaceBlockChance,
                0.0,
                1.0
        );
        CAVE_SURFACE_CLUSTER_ON_BLOCK_CHANCE = ConfigSpecHelper.floating(
                builder,
                "surface_cluster_on_block_chance",
                "水晶块表面再插完整水晶簇的概率 [0,1]。",
                caveSurfaceClusterOnBlockChance,
                0.0,
                1.0
        );
        CAVE_SURFACE_CLUSTER_ON_STONE_CHANCE = ConfigSpecHelper.floating(
                builder,
                "surface_cluster_on_stone_chance",
                "仍是石头的表面插水晶簇的概率 [0,1]。",
                caveSurfaceClusterOnStoneChance,
                0.0,
                1.0
        );
        CAVE_MAX_CLUSTERS_PER_CHUNK = ConfigSpecHelper.integer(
                builder,
                "max_clusters_per_chunk",
                "单 chunk 水晶簇硬上限。",
                caveMaxClustersPerChunk,
                0,
                512
        );

        builder.pop();
        SPEC = builder.build();
    }

    private EldenRingCommonConfig() {
    }

    /**
     * 把 toml 当前值写回运行时字段，供世界生成读取。
     */
    public static void apply() {
        caveRegionNoiseCellSizeChunks = CAVE_REGION_NOISE_CELL_SIZE_CHUNKS.get();
        caveChunkNoiseCellSizeChunks = CAVE_CHUNK_NOISE_CELL_SIZE_CHUNKS.get();
        caveRegionPresenceThreshold = CAVE_REGION_PRESENCE_THRESHOLD.get();
        caveChunkDecorateThreshold = CAVE_CHUNK_DECORATE_THRESHOLD.get();
        caveScanMinY = CAVE_SCAN_MIN_Y.get();
        caveScanMaxY = CAVE_SCAN_MAX_Y.get();
        cavePocketsPerChunk = CAVE_POCKETS_PER_CHUNK.get();
        cavePocketCenterScanStride = CAVE_POCKET_CENTER_SCAN_STRIDE.get();
        cavePocketRadiusBlocks = CAVE_POCKET_RADIUS_BLOCKS.get();
        caveSurfaceBlockChance = CAVE_SURFACE_BLOCK_CHANCE.get().floatValue();
        caveSurfaceClusterOnBlockChance = CAVE_SURFACE_CLUSTER_ON_BLOCK_CHANCE.get().floatValue();
        caveSurfaceClusterOnStoneChance = CAVE_SURFACE_CLUSTER_ON_STONE_CHANCE.get().floatValue();
        caveMaxClustersPerChunk = CAVE_MAX_CLUSTERS_PER_CHUNK.get();
    }
}

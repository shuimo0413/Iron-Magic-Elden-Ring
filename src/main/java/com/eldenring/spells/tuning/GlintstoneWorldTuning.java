package com.eldenring.spells.tuning;



/**

 * 辉石世界侧可调常量：矿脉密度、辉石矿洞噪声与表面装饰概率。

 * <p>

 * 矿洞分两层噪声：大尺度决定「这一带是否可能是辉石洞 + 整洞同色」；

 * 小尺度 chunk 门控决定「这个 chunk 是否真的刷装饰」，避免整片连通洞穴被铺满。

 */

public final class GlintstoneWorldTuning {

    private GlintstoneWorldTuning() {

    }



    // —— 矿石矿脉（placed feature 的 count；与洞穴颜色无关）——



    /** 青色辉石矿每个 chunk 尝试次数。调大更常见。 */

    public static final int CYAN_ORE_VEINS_PER_CHUNK = 8;



    /** 蓝色辉石矿每个 chunk 尝试次数。 */

    public static final int BLUE_ORE_VEINS_PER_CHUNK = 5;



    /** 紫色辉石矿每个 chunk 尝试次数。 */

    public static final int PURPLE_ORE_VEINS_PER_CHUNK = 2;



    /** 单次矿脉目标方块数（对标铁矿 size≈9）。 */

    public static final int ORE_VEIN_SIZE = 9;



    // —— 辉石矿洞 Feature（整片洞穴，三色等概率、一洞一色）——



    /**

     * 大尺度 2D 噪声缩放（chunk 坐标除以该值）。

     * 越大 → 同色辉石「地带」斑块越大；与 {@link #CAVE_CHUNK_NOISE_CELL_SIZE_CHUNKS} 配合使用。

     */

    public static final double CAVE_REGION_NOISE_CELL_SIZE_CHUNKS = 10.0;



    /**

     * 小尺度 chunk 门控噪声缩放。越小 → 门控斑块越小、边界更碎。

     */

    public static final double CAVE_CHUNK_NOISE_CELL_SIZE_CHUNKS = 2.5;



    /**

     * 大尺度噪声 ∈ [0,1]，高于此值才算进入「辉石地带」（仍可能因 chunk 门控不刷）。

     * 调高更稀有；0.90 约一成多 chunk 所在地带会中。

     */

    public static final double CAVE_REGION_PRESENCE_THRESHOLD = 0.76;



    /**

     * 小尺度 chunk 门控噪声阈值：只有在大尺度已中的地带内，且本 chunk 噪声也高于此值才装饰。

     * 调高 → 地带内更稀疏、洞更小；0.78 约两成 chunk 会真正刷（地带内）。

     */

    public static final double CAVE_CHUNK_DECORATE_THRESHOLD = 0.52;



    /**

     * 与世界种子异或的盐，避免和其它结构抢同一噪声相位。

     */

    public static final long CAVE_NOISE_SALT = 0x67C15_70C5_CAFEL;



    /** chunk 门控噪声盐（与大尺度、颜色噪声错相位）。 */

    public static final long CAVE_CHUNK_GATE_SALT = 0x51A71C57E0DEL;



    /** 扫描洞穴表面的最低 Y（方块绝对高度）。 */

    public static final int CAVE_SCAN_MIN_Y = -56;



    /** 扫描洞穴表面的最高 Y（方块绝对高度）。 */

    public static final int CAVE_SCAN_MAX_Y = 48;



    /**

     * 邻接空气的石头/深板岩被替换成水晶块的概率。

     * 调大 → 洞壁上实心辉石块更多；仍应远小于 1，保证石头占多数。

     */

    public static final float CAVE_SURFACE_BLOCK_CHANCE = 0.11F;



    /**

     * 邻接空气的水晶块表面在空气格插入完整水晶簇的概率。

     * 晶簇应主要长在水晶块上；此值可略高于旧版全局簇概率。

     */

    public static final float CAVE_SURFACE_CLUSTER_ON_BLOCK_CHANCE = 0.16F;



    /**

     * 邻接空气但仍是普通石头/深板岩的表面插入水晶簇的概率。

     * 刻意很低，避免未换块洞壁被簇铺满。

     */

    public static final float CAVE_SURFACE_CLUSTER_ON_STONE_CHANCE = 0.012F;



    /**

     * 单个 chunk 内最多放置的水晶簇数量（硬上限，防止大空洞面数过多）。

     * 调小更稀疏。

     */

    public static final int CAVE_MAX_CLUSTERS_PER_CHUNK = 18;

}

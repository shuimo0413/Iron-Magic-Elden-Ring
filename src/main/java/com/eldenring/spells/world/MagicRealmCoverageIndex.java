package com.eldenring.spells.world;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.block.entity.MagicRealmBlockEntity;
import com.eldenring.spells.registry.ModBlocks;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.lang.ref.WeakReference;

/**
 * 客户端「魔法之境」覆盖源索引：记录客户端已加载区块里每个魔法之境方块的位置，供光带判断某点是否被覆盖。
 * <p>
 * 多个方块的覆盖区域取<b>并集</b>：查询只回答「是否有任意一个方块覆盖这里」，从不按方块数量累加，
 * 所以重叠放置不会让光带变密或变亮。刷新预算由光带管理器统一控制。
 * <p>
 * 只登记 {@code level.isClientSide()} 的方块实体；单人游戏内置服务端那一份不进来。
 * 本类不引用任何客户端专属类，方块实体在专用服务器上加载它也安全。
 * <p>
 * 生命周期：方块实体 {@code onLoad} 登记、{@code setRemoved} / {@code onChunkUnloaded} 注销；
 * 客户端区块加载时再扫一遍区块方块实体兜底，区块卸载时整桶丢弃；换世界时整体清空。
 * 查询时顺手校验位置上仍是魔法之境方块，清掉任何漏网的旧记录。不会为了查询去加载区块。
 */
@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID, value = Dist.CLIENT)
public final class MagicRealmCoverageIndex {

    /**
     * 覆盖水平半径（方块），以方块中心为圆心、不限高度的圆柱。64 = 4 个区块。
     * 调大 → 单个方块覆盖更广；查询要扫的区块窗口随之变大。
     */
    public static final double COVERAGE_RADIUS_BLOCKS = 64.0;
    private static final double COVERAGE_RADIUS_SQUARED = COVERAGE_RADIUS_BLOCKS * COVERAGE_RADIUS_BLOCKS;
    /** 查询时向四周扫描的区块数。源与查询点水平距离 ≤ 64 时，两者区块坐标差最多 4。 */
    private static final int COVERAGE_SEARCH_RADIUS_CHUNKS = (int) Math.ceil(COVERAGE_RADIUS_BLOCKS / 16.0);

    /** 区块坐标（{@link ChunkPos#toLong()}）→ 该区块内方块位置（{@link BlockPos#asLong()}）集合。 */
    private static final Long2ObjectOpenHashMap<LongOpenHashSet> SOURCE_POSITIONS_BY_CHUNK = new Long2ObjectOpenHashMap<>();
    /** 当前索引所属的客户端世界；弱引用，退出世界后不阻止旧世界被回收。 */
    private static WeakReference<Level> trackedLevelReference = new WeakReference<>(null);

    private MagicRealmCoverageIndex() {
    }

    /** 登记一个覆盖源。重复登记无副作用。非客户端世界忽略。 */
    public static void register(Level level, BlockPos sourcePos) {
        if (level == null || !level.isClientSide()) {
            return;
        }
        if (trackedLevelReference.get() != level) {
            SOURCE_POSITIONS_BY_CHUNK.clear();
            trackedLevelReference = new WeakReference<>(level);
        }
        long chunkKey = ChunkPos.asLong(SectionPos.blockToSectionCoord(sourcePos.getX()), SectionPos.blockToSectionCoord(sourcePos.getZ()));
        SOURCE_POSITIONS_BY_CHUNK.computeIfAbsent(chunkKey, key -> new LongOpenHashSet()).add(sourcePos.asLong());
    }

    /** 注销一个覆盖源。不是当前世界的调用（换世界后旧方块实体迟到的移除）直接忽略。 */
    public static void unregister(Level level, BlockPos sourcePos) {
        if (level == null || !level.isClientSide() || trackedLevelReference.get() != level) {
            return;
        }
        long chunkKey = ChunkPos.asLong(SectionPos.blockToSectionCoord(sourcePos.getX()), SectionPos.blockToSectionCoord(sourcePos.getZ()));
        LongOpenHashSet chunkSources = SOURCE_POSITIONS_BY_CHUNK.get(chunkKey);
        if (chunkSources != null && chunkSources.remove(sourcePos.asLong()) && chunkSources.isEmpty()) {
            SOURCE_POSITIONS_BY_CHUNK.remove(chunkKey);
        }
    }

    /**
     * 水平坐标 (x, z) 是否落在任意一个魔法之境方块的覆盖圆内。
     * 命中第一个有效源就返回；途中发现的失效记录（区块已卸载 / 方块已不在）会被清除。
     */
    public static boolean isCovered(Level level, double worldX, double worldZ) {
        if (level == null || trackedLevelReference.get() != level || SOURCE_POSITIONS_BY_CHUNK.isEmpty()) {
            return false;
        }
        int centerChunkX = SectionPos.blockToSectionCoord((int) Math.floor(worldX));
        int centerChunkZ = SectionPos.blockToSectionCoord((int) Math.floor(worldZ));
        BlockPos.MutableBlockPos sourcePos = new BlockPos.MutableBlockPos();
        for (int chunkOffsetX = -COVERAGE_SEARCH_RADIUS_CHUNKS; chunkOffsetX <= COVERAGE_SEARCH_RADIUS_CHUNKS; chunkOffsetX++) {
            for (int chunkOffsetZ = -COVERAGE_SEARCH_RADIUS_CHUNKS; chunkOffsetZ <= COVERAGE_SEARCH_RADIUS_CHUNKS; chunkOffsetZ++) {
                long chunkKey = ChunkPos.asLong(centerChunkX + chunkOffsetX, centerChunkZ + chunkOffsetZ);
                LongOpenHashSet chunkSources = SOURCE_POSITIONS_BY_CHUNK.get(chunkKey);
                if (chunkSources == null) {
                    continue;
                }
                LongArrayList stalePositions = null;
                boolean covered = false;
                LongIterator sourceIterator = chunkSources.iterator();
                while (sourceIterator.hasNext()) {
                    long packedSourcePos = sourceIterator.nextLong();
                    sourcePos.set(packedSourcePos);
                    double deltaX = sourcePos.getX() + 0.5 - worldX;
                    double deltaZ = sourcePos.getZ() + 0.5 - worldZ;
                    if (deltaX * deltaX + deltaZ * deltaZ > COVERAGE_RADIUS_SQUARED) {
                        continue;
                    }
                    if (!level.isLoaded(sourcePos) || !level.getBlockState(sourcePos).is(ModBlocks.MAGIC_REALM.get())) {
                        if (stalePositions == null) {
                            stalePositions = new LongArrayList();
                        }
                        stalePositions.add(packedSourcePos);
                        continue;
                    }
                    covered = true;
                    break;
                }
                if (stalePositions != null) {
                    chunkSources.removeAll(stalePositions);
                    if (chunkSources.isEmpty()) {
                        SOURCE_POSITIONS_BY_CHUNK.remove(chunkKey);
                    }
                }
                if (covered) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 客户端区块到达时扫一遍区块里的方块实体登记覆盖源。
     * 与方块实体自己的 {@code onLoad} 重复无妨（登记是幂等的），用来兜住区块包加载顺序上的差异。
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (!levelAccessor.isClientSide() || !(levelAccessor instanceof Level level) || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof MagicRealmBlockEntity) {
                register(level, blockEntity.getBlockPos());
            }
        }
    }

    /** 客户端区块卸载时整桶丢弃，不依赖每个方块实体都回调到。 */
    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (!levelAccessor.isClientSide() || trackedLevelReference.get() != levelAccessor) {
            return;
        }
        SOURCE_POSITIONS_BY_CHUNK.remove(event.getChunk().getPos().toLong());
    }
}

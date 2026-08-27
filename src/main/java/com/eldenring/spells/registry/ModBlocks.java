package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.world.GlintstoneColor;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/**
 * 辉石矿物方块注册：每色一套水晶簇 / 水晶块。
 * <p>
 * 逻辑类不按颜色复制；颜色数据在 {@link GlintstoneColor}。
 * 墙上水晶只保留最大簇一档（无小/中/大芽）。不生长、无建材、无矿石。
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(EldenRingSpellsMod.MOD_ID);

    /** 按颜色索引的一整套方块句柄，供世界生成与创造栏遍历。 */
    public static final Map<GlintstoneColor, ColorSet> BY_COLOR = new EnumMap<>(GlintstoneColor.class);

    static {
        for (GlintstoneColor color : GlintstoneColor.values()) {
            BY_COLOR.put(color, ColorSet.register(color));
        }
    }

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    /**
     * 单色辉石矿物套装。字段均为 Deferred，注册完成后 {@link DeferredBlock#get()} 可用。
     */
    public static final class ColorSet {
        public final GlintstoneColor color;
        public final DeferredBlock<Block> crystalBlock;
        public final DeferredBlock<AmethystClusterBlock> cluster;

        private ColorSet(
                GlintstoneColor color,
                DeferredBlock<Block> crystalBlock,
                DeferredBlock<AmethystClusterBlock> cluster
        ) {
            this.color = color;
            this.crystalBlock = crystalBlock;
            this.cluster = cluster;
        }

        private static ColorSet register(GlintstoneColor color) {
            String prefix = color.idPrefix();
            MapColor mapColor = color.mapColor();

            DeferredBlock<Block> crystalBlock = BLOCKS.registerBlock(
                    prefix + "_glintstone_block",
                    AmethystBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(mapColor)
                            .strength(1.5F)
                            .sound(SoundType.AMETHYST)
                            .requiresCorrectToolForDrops()
            );

            // 高度 7 / 半宽 3 像素：对标原版紫水晶完整簇
            DeferredBlock<AmethystClusterBlock> cluster = registerCluster(
                    prefix + "_glintstone_cluster", mapColor, 7.0F, 3.0F, 5);

            return new ColorSet(color, crystalBlock, cluster);
        }

        private static DeferredBlock<AmethystClusterBlock> registerCluster(
                String id,
                MapColor mapColor,
                float height,
                float xzOffset,
                int lightLevel
        ) {
            return BLOCKS.registerBlock(
                    id,
                    props -> new AmethystClusterBlock(height, xzOffset, props),
                    BlockBehaviour.Properties.of()
                            .mapColor(mapColor)
                            .forceSolidOn()
                            .noOcclusion()
                            .sound(SoundType.AMETHYST_CLUSTER)
                            .strength(1.5F)
                            .lightLevel(state -> lightLevel)
                            .pushReaction(PushReaction.DESTROY)
            );
        }
    }
}

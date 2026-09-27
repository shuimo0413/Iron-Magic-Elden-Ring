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
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import java.util.EnumMap;
import java.util.Map;

/**
 * 辉石矿物方块注册：每色一套水晶簇 / 水晶块。
 * <p>
 * 逻辑类不按颜色复制；颜色数据在 {@link GlintstoneColor}。
 * 墙上水晶只保留最大簇一档（无小/中/大芽）。不生长、无建材、无矿石。
 */
public final class ModBlocks {
    // Forge 1.20.1 没有 DeferredRegister.Blocks / createBlocks，改用普通 DeferredRegister<Block>。
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, EldenRingSpellsMod.MOD_ID);

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
     * 单色辉石矿物套装。字段均为惰性句柄，注册完成后 {@link RegistryObject#get()} 可用。
     */
    public static final class ColorSet {
        public final GlintstoneColor color;
        public final RegistryObject<Block> crystalBlock;
        public final RegistryObject<AmethystClusterBlock> cluster;

        private ColorSet(
                GlintstoneColor color,
                RegistryObject<Block> crystalBlock,
                RegistryObject<AmethystClusterBlock> cluster
        ) {
            this.color = color;
            this.crystalBlock = crystalBlock;
            this.cluster = cluster;
        }

        private static ColorSet register(GlintstoneColor color) {
            String prefix = color.idPrefix();
            MapColor mapColor = color.mapColor();

            // Forge 的 DeferredRegister.register 是泛型方法，按赋值目标推断类型参数；
            // 不再有 NeoForge 的 registerBlock(factory, props) 形态。
            RegistryObject<Block> crystalBlock = BLOCKS.register(
                    prefix + "_glintstone_block",
                    () -> new AmethystBlock(BlockBehaviour.Properties.of()
                            .mapColor(mapColor)
                            .strength(1.5F)
                            .sound(SoundType.AMETHYST)
                            .requiresCorrectToolForDrops()
                    )
            );

            // 高度 7 像素 / 半宽 3 像素：对标原版紫水晶完整簇。
            // 1.20.1 的 AmethystClusterBlock 构造器收「整数像素」，不是 1.21 的 float 方块数，
            // 故这里传 7 / 3 而不是 7.0F / 3.0F（形状由父类内部 ×0.0625 折算成方块）。
            RegistryObject<AmethystClusterBlock> cluster = registerCluster(
                    prefix + "_glintstone_cluster", mapColor, 7, 3, 5);

            return new ColorSet(color, crystalBlock, cluster);
        }

        private static RegistryObject<AmethystClusterBlock> registerCluster(
                String id,
                MapColor mapColor,
                int heightPixels,
                int xzOffsetPixels,
                int lightLevel
        ) {
            return BLOCKS.register(
                    id,
                    () -> new AmethystClusterBlock(heightPixels, xzOffsetPixels, BlockBehaviour.Properties.of()
                            .mapColor(mapColor)
                            .forceSolidOn()
                            .noOcclusion()
                            .sound(SoundType.AMETHYST_CLUSTER)
                            .strength(1.5F)
                            .lightLevel(state -> lightLevel)
                            .pushReaction(PushReaction.DESTROY)
                    )
            );
        }
    }
}

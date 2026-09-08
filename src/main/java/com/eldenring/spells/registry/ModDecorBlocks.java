package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.block.decor.GlintstoneCandelabraBlock;
import com.eldenring.spells.world.GlintstoneColor;
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
 * 辉石学派装饰方块：首批为三色辉石烛台。
 * <p>
 * 与矿物方块 {@link ModBlocks} 分注册表，避免 {@code ModBlocks} 职责膨胀。
 */
public final class ModDecorBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(EldenRingSpellsMod.MOD_ID);

    /** 按颜色索引的烛台句柄，供创造栏与配方遍历。 */
    public static final Map<GlintstoneColor, CandelabraSet> CANDELABRAS_BY_COLOR = new EnumMap<>(GlintstoneColor.class);

    static {
        for (GlintstoneColor color : GlintstoneColor.values()) {
            CANDELABRAS_BY_COLOR.put(color, CandelabraSet.register(color));
        }
    }

    private ModDecorBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    /**
     * 单色辉石烛台 Deferred 句柄。
     */
    public static final class CandelabraSet {
        public final GlintstoneColor color;
        public final DeferredBlock<GlintstoneCandelabraBlock> candelabra;

        private CandelabraSet(GlintstoneColor color, DeferredBlock<GlintstoneCandelabraBlock> candelabra) {
            this.color = color;
            this.candelabra = candelabra;
        }

        private static CandelabraSet register(GlintstoneColor color) {
            String id = color.idPrefix() + "_glintstone_candelabra";
            MapColor mapColor = color.mapColor();

            DeferredBlock<GlintstoneCandelabraBlock> candelabra = BLOCKS.registerBlock(
                    id,
                    properties -> new GlintstoneCandelabraBlock(properties, color),
                    BlockBehaviour.Properties.of()
                            .mapColor(mapColor)
                            .noOcclusion()
                            .forceSolidOn()
                            .strength(0.3F)
                            .sound(SoundType.AMETHYST)
                            .lightLevel(state -> 12)
                            .pushReaction(PushReaction.DESTROY)
            );

            return new CandelabraSet(color, candelabra);
        }
    }
}

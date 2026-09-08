package cn.blockforge.generated.generatedmod;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(GeneratedMod.MOD_ID)
public final class GeneratedMod {
    public static final String MOD_ID = "generated_mod";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    public static final DeferredBlock<Block> MAGIC_CANDELABRA_CYAN = registerCandelabra(
            "magic_candelabra_cyan", DyeColor.CYAN);
    public static final DeferredBlock<Block> MAGIC_CANDELABRA_BLUE = registerCandelabra(
            "magic_candelabra_blue", DyeColor.BLUE);
    public static final DeferredBlock<Block> MAGIC_CANDELABRA_PURPLE = registerCandelabra(
            "magic_candelabra_purple", DyeColor.PURPLE);

    public static final DeferredItem<BlockItem> MAGIC_CANDELABRA_CYAN_ITEM = ITEMS.registerSimpleBlockItem(
            "magic_candelabra_cyan", MAGIC_CANDELABRA_CYAN);
    public static final DeferredItem<BlockItem> MAGIC_CANDELABRA_BLUE_ITEM = ITEMS.registerSimpleBlockItem(
            "magic_candelabra_blue", MAGIC_CANDELABRA_BLUE);
    public static final DeferredItem<BlockItem> MAGIC_CANDELABRA_PURPLE_ITEM = ITEMS.registerSimpleBlockItem(
            "magic_candelabra_purple", MAGIC_CANDELABRA_PURPLE);
    public static final DeferredItem<Item> GENERATED_ITEM = ITEMS.register(
            "generated_item", () -> new Item(new Item.Properties()));

    private static DeferredBlock<Block> registerCandelabra(String name, DyeColor color) {
        return BLOCKS.registerBlock(name, MagicCandelabraBlock::new, BlockBehaviour.Properties.of()
                .mapColor(color)
                .sound(SoundType.METAL)
                .strength(1.5f)
                .lightLevel(state -> 12)
                .noOcclusion());
    }

    public GeneratedMod(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(GeneratedMod::addCreative);
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(MAGIC_CANDELABRA_CYAN_ITEM);
            event.accept(MAGIC_CANDELABRA_BLUE_ITEM);
            event.accept(MAGIC_CANDELABRA_PURPLE_ITEM);
        }
    }
}

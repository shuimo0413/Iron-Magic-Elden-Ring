package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.block.entity.TerraMagicaGeneratorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块实体类型注册。
 */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EldenRingSpellsMod.MOD_ID);

    /** 魔法之境生成器的位置标记方块实体：无 ticker、无数据，只做客户端覆盖源登记。 */
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TerraMagicaGeneratorBlockEntity>> TERRA_MAGICA_GENERATOR =
            BLOCK_ENTITY_TYPES.register("terra_magica_generator", () -> BlockEntityType.Builder
                    .of(TerraMagicaGeneratorBlockEntity::new, ModBlocks.TERRA_MAGICA_GENERATOR.get())
                    .build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}

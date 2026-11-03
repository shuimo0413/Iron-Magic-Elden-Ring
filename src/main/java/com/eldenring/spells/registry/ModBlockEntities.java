package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.block.entity.MagicRealmBlockEntity;
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

    /** 魔法之境的位置标记方块实体：无 ticker、无数据，只做客户端覆盖源登记。 */
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MagicRealmBlockEntity>> MAGIC_REALM =
            BLOCK_ENTITY_TYPES.register("magic_realm", () -> BlockEntityType.Builder
                    .of(MagicRealmBlockEntity::new, ModBlocks.MAGIC_REALM.get())
                    .build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}

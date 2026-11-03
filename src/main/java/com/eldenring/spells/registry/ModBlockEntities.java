package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.block.entity.MagicRealmBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 方块实体类型注册。
 */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EldenRingSpellsMod.MOD_ID);

    /** 魔法之境的位置标记方块实体：无 ticker、无数据，只做客户端覆盖源登记。 */
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<MagicRealmBlockEntity>> MAGIC_REALM =
            BLOCK_ENTITY_TYPES.register("magic_realm", () -> BlockEntityType.Builder
                    .of(MagicRealmBlockEntity::new, ModBlocks.MAGIC_REALM.get())
                    .build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}

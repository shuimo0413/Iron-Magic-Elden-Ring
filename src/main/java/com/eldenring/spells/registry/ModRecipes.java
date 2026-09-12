package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.recipe.EquipmentUpgradeRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 装备升级使用独立序列化器，保证扩容过的魔法书不会丢失法术。 */
public final class ModRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, EldenRingSpellsMod.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, EquipmentUpgradeRecipe.Serializer> EQUIPMENT_UPGRADE =
            SERIALIZERS.register("equipment_upgrade", EquipmentUpgradeRecipe.Serializer::new);

    private ModRecipes() {
    }

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }
}

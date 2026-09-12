package com.eldenring.spells.recipe;

import com.eldenring.spells.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.jetbrains.annotations.NotNull;

/**
 * 本模组装备的锻造升级：保留名称、附魔、灌注法术与升级组件。
 * 铁魔法原生锻造会按目标书默认容量重建容器；这里独立 assemble，防止已扩容书的高位法术被截断。
 */
public final class EquipmentUpgradeRecipe extends SmithingTransformRecipe {
    private final Ingredient upgradeTemplate;
    private final Ingredient upgradeBase;
    private final Ingredient upgradeAddition;
    private final ItemStack upgradedResult;

    public EquipmentUpgradeRecipe(Ingredient template, Ingredient base, Ingredient addition, ItemStack result) {
        super(template, base, addition, result);
        this.upgradeTemplate = template;
        this.upgradeBase = base;
        this.upgradeAddition = addition;
        this.upgradedResult = result;
    }

    /** 只修改结果副本；保留源容器容量和锁定信息，新书默认容量只可扩充不可缩减。 */
    @Override
    public @NotNull ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        ItemStack upgraded = input.base().transmuteCopy(upgradedResult.getItem(), upgradedResult.getCount());
        upgraded.applyComponents(upgradedResult.getComponentsPatch());
        if (upgraded.getItem() instanceof SpellBook book) {
            if (ISpellContainer.isSpellContainer(input.base())) {
                ISpellContainerMutable container = ISpellContainer.get(input.base()).mutableCopy();
                container.setMaxSpellCount(Math.max(container.getMaxSpellCount(), book.getMaxSpellSlots()));
                ISpellContainer.set(upgraded, container.toImmutable());
            } else {
                book.initializeSpellContainer(upgraded);
            }
        }
        return upgraded;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.EQUIPMENT_UPGRADE.get();
    }

    /** 沿用原版锻造的数据布局和 RecipeType，配方查看器仍使用锻造台分类。 */
    public static final class Serializer implements RecipeSerializer<EquipmentUpgradeRecipe> {
        private static final MapCodec<EquipmentUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Ingredient.CODEC.fieldOf("template").forGetter(recipe -> recipe.upgradeTemplate),
                        Ingredient.CODEC.fieldOf("base").forGetter(recipe -> recipe.upgradeBase),
                        Ingredient.CODEC.fieldOf("addition").forGetter(recipe -> recipe.upgradeAddition),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.upgradedResult)
                ).apply(instance, EquipmentUpgradeRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, EquipmentUpgradeRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.upgradeTemplate,
                        Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.upgradeBase,
                        Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.upgradeAddition,
                        ItemStack.STREAM_CODEC, recipe -> recipe.upgradedResult,
                        EquipmentUpgradeRecipe::new);

        @Override
        public @NotNull MapCodec<EquipmentUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, EquipmentUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}

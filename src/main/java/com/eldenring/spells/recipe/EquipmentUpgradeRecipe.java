package com.eldenring.spells.recipe;

import com.eldenring.spells.registry.ModRecipes;
import com.google.gson.JsonObject;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 本模组装备的锻造升级：保留名称、附魔、灌注法术与升级组件。
 * 铁魔法原生锻造会按目标书默认容量重建容器；这里独立 assemble，防止已扩容书的高位法术被截断。
 * <p>
 * 1.20.1 移植说明：只复用「原版」锻造配方管线（{@link SmithingTransformRecipe} +
 * {@link RecipeSerializer} 的 fromJson / fromNetwork / toNetwork）。铁魔法 1.20.1 <b>没有</b>可复用的
 * 升级配方接口：{@code recipe_types.NoAdditionSmithingTransformRecipe} 与其序列化器
 * {@code smithing_transform_no_addition} 在 1.20.1 源码里是整文件注释掉的死代码，铁魔法改用
 * {@code SmithingRecipeMixin} 注入原版 {@code SmithingTransformRecipe#assemble}。该 mixin 注入的是
 * 父类方法体，本类覆写了 assemble 且不调 super，因此不会与本类逻辑叠加（也不会替本类补法术容器）。
 */
public final class EquipmentUpgradeRecipe extends SmithingTransformRecipe {

    /**
     * 1.20.1 原版锻造台（含升级台界面）的输入槽下标：0 = 锻造模板，1 = 基底（被升级装备），2 = 附加材料。
     * <p>
     * 依据：原版 {@code SmithingMenu} 的 inputSlotDefinitions 依次占用 0/1/2 三个槽，结果槽是 3；
     * 原版 {@code SmithingTransformRecipe#matches} 也按 template→0 / base→1 / addition→2 取件；
     * 铁魔法 1.20.1 的 {@code SmithingRecipeMixin} 同样用 {@code container.getItem(1)} 取基底，可交叉验证。
     */
    private static final int BASE_SLOT_INDEX = 1;

    private final Ingredient upgradeTemplate;
    private final Ingredient upgradeBase;
    private final Ingredient upgradeAddition;
    private final ItemStack upgradedResult;

    /**
     * 1.20.1 的 {@code SmithingTransformRecipe} 构造器首参是配方的 {@link ResourceLocation} id
     * （1.21 才把这个参数去掉）。id 由 {@code RecipeSerializer#fromJson / fromNetwork} 传入。
     */
    public EquipmentUpgradeRecipe(
            ResourceLocation id,
            Ingredient template,
            Ingredient base,
            Ingredient addition,
            ItemStack result
    ) {
        super(id, template, base, addition, result);
        this.upgradeTemplate = template;
        this.upgradeBase = base;
        this.upgradeAddition = addition;
        this.upgradedResult = result;
    }

    /** 只修改结果副本；保留源容器容量和锁定信息，新书默认容量只可扩充不可缩减。 */
    @Override
    public @NotNull ItemStack assemble(@NotNull Container input, @NotNull RegistryAccess registryAccess) {
        ItemStack baseStack = input.getItem(BASE_SLOT_INDEX);

        // 1.20.1 没有数据组件（applyComponents / getComponentsPatch），改为整体搬运基底的 NBT：
        // 自定义名称、附魔、已灌注法术与升级数据全都存在 NBT 里，必须原样带到结果物品上。
        ItemStack upgraded = new ItemStack(upgradedResult.getItem(), upgradedResult.getCount());
        CompoundTag preservedTag = baseStack.hasTag() ? baseStack.getTag().copy() : new CompoundTag();

        // 配方 result 自带 nbt 时按「补丁」语义覆盖基底同名字段，对应 1.21 的 applyComponents(resultPatch)
        CompoundTag resultTag = upgradedResult.getTag();
        if (resultTag != null) {
            for (String key : resultTag.getAllKeys()) {
                preservedTag.put(key, resultTag.get(key).copy());
            }
        }
        if (!preservedTag.isEmpty()) {
            upgraded.setTag(preservedTag);
        }

        if (upgraded.getItem() instanceof SpellBook spellBook) {
            if (ISpellContainer.isSpellContainer(baseStack)) {
                ISpellContainerMutable upgradedContainer = ISpellContainer.get(baseStack).mutableCopy();
                upgradedContainer.setMaxSpellCount(
                        Math.max(upgradedContainer.getMaxSpellCount(), spellBook.getMaxSpellSlots()));
                ISpellContainer.set(upgraded, upgradedContainer.toImmutable());
            } else {
                spellBook.initializeSpellContainer(upgraded);
            }
        }
        return upgraded;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.EQUIPMENT_UPGRADE.get();
    }

    /**
     * 沿用原版锻造的数据布局和 RecipeType，配方查看器仍使用锻造台分类。
     * <p>
     * 1.20.1 的 {@link RecipeSerializer} 只有 fromJson / fromNetwork / toNetwork 三个方法；
     * 1.20.2+ 才有 codec() / streamCodec()（原实现里的 MapCodec / StreamCodec 与
     * {@code ItemStack.STRICT_CODEC} 在 1.20.1 都不存在，故一并移除）。
     */
    public static final class Serializer implements RecipeSerializer<EquipmentUpgradeRecipe> {

        /** JSON 键与 1.21 保持一致：template / base / addition / result（nbt 由 ShapedRecipe 解析）。 */
        @Override
        public EquipmentUpgradeRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient template = Ingredient.fromJson(GsonHelper.getNonNull(json, "template"));
            Ingredient base = Ingredient.fromJson(GsonHelper.getNonNull(json, "base"));
            Ingredient addition = Ingredient.fromJson(GsonHelper.getNonNull(json, "addition"));
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new EquipmentUpgradeRecipe(recipeId, template, base, addition, result);
        }

        /** 字段顺序必须与 {@link #toNetwork(FriendlyByteBuf, EquipmentUpgradeRecipe)} 完全一致。 */
        @Override
        public @Nullable EquipmentUpgradeRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            Ingredient template = Ingredient.fromNetwork(buffer);
            Ingredient base = Ingredient.fromNetwork(buffer);
            Ingredient addition = Ingredient.fromNetwork(buffer);
            ItemStack result = buffer.readItem();
            return new EquipmentUpgradeRecipe(recipeId, template, base, addition, result);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, EquipmentUpgradeRecipe recipe) {
            recipe.upgradeTemplate.toNetwork(buffer);
            recipe.upgradeBase.toNetwork(buffer);
            recipe.upgradeAddition.toNetwork(buffer);
            buffer.writeItem(recipe.upgradedResult);
        }
    }
}

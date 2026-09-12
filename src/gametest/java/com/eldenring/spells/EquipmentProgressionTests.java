package com.eldenring.spells;

import com.eldenring.spells.recipe.EquipmentUpgradeRecipe;
import com.eldenring.spells.item.AzurStaffBalance;
import com.eldenring.spells.item.AzurManaCostPolicyTest;
import com.eldenring.spells.network.AzurStaffSettingsPayload;
import com.eldenring.spells.registry.ModItems;
import com.eldenring.spells.registry.ModSpells;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 独立 GameTestServer 使用的回归测试，不进入发布 JAR。 */
@GameTestHolder(EldenRingSpellsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EquipmentProgressionTests {
    private EquipmentProgressionTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void pureManaCostBoundaries(GameTestHelper helper) {
        AzurManaCostPolicyTest.main(new String[0]);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void expandedBookUpgradePreservesData(GameTestHelper helper) {
        ItemStack source = new ItemStack(ModItems.STAR_CODEX.get());
        source.set(DataComponents.CUSTOM_NAME, Component.literal("Expanded personal spellbook"));
        source.set(DataComponents.REPAIR_COST, 7);
        ISpellContainerMutable spells = ISpellContainer.create(14, true, true).mutableCopy();
        spells.setImproved(true);
        helper.assertTrue(spells.addSpellAtIndex(ModSpells.GLINTSTONE_PEBBLE.get(), 1, 0, false), "first slot initialized");
        helper.assertTrue(spells.addSpellAtIndex(ModSpells.STARS_OF_RUIN.get(), 1, 13, true), "last slot initialized");
        ISpellContainer.set(source, spells.toImmutable());
        EquipmentUpgradeRecipe recipe = recipe(helper, "origin_codex");
        SmithingRecipeInput input = new SmithingRecipeInput(item("irons_spellbooks:legendary_ink"), source,
                new ItemStack(ModItems.ORIGIN_CRYSTAL.get()));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "registered recipe matches ingredients");
        ItemStack output = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(output.is(ModItems.ORIGIN_CODEX.get()), "upgraded item type");
        ISpellContainer result = ISpellContainer.get(output);
        helper.assertTrue(result.getMaxSpellCount() == 14, "expanded capacity retained");
        helper.assertTrue(result.isImproved(), "slot-upgrade marker retained");
        helper.assertTrue(result.getActiveSpellCount() == 2, "all spells retained");
        helper.assertTrue(result.getSpellAtIndex(13).getSpell() == ModSpells.STARS_OF_RUIN.get(), "high-index spell retained");
        helper.assertTrue(result.getSpellAtIndex(13).isLocked(), "locked spell retained");
        helper.assertValueEqual(output.get(DataComponents.CUSTOM_NAME), source.get(DataComponents.CUSTOM_NAME), "custom name");
        helper.assertTrue(output.getOrDefault(DataComponents.REPAIR_COST, 0) == 7, "component patch preserved");
        ISpellContainerMutable changedOutput = result.mutableCopy();
        changedOutput.removeSpellAtIndex(13);
        ISpellContainer.set(output, changedOutput.toImmutable());
        helper.assertTrue(ISpellContainer.get(source).getActiveSpellCount() == 2, "input container not aliased or mutated");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void defaultBookCapacityAndWrongIngredient(GameTestHelper helper) {
        ItemStack base = new ItemStack(ModItems.STAR_CODEX.get());
        ((SpellBook) base.getItem()).initializeSpellContainer(base);
        EquipmentUpgradeRecipe recipe = recipe(helper, "origin_codex");
        SmithingRecipeInput input = new SmithingRecipeInput(item("irons_spellbooks:legendary_ink"), base,
                new ItemStack(ModItems.ORIGIN_CRYSTAL.get()));
        ItemStack output = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(ISpellContainer.get(output).getMaxSpellCount() == ((SpellBook) output.getItem()).getMaxSpellSlots(),
                "unexpanded book receives target capacity");
        helper.assertFalse(recipe.matches(new SmithingRecipeInput(input.template(), base, new ItemStack(Items.DIRT)),
                helper.getLevel()), "wrong addition is rejected");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id("star_codex")).isPresent(), "star recipe loaded");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id("astrologer_staff")).isPresent(), "staff crafting loaded");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void staffUpgradePreservesImbuedSpell(GameTestHelper helper) {
        ItemStack staff = new ItemStack(ModItems.ASTROLOGER_STAFF.get());
        staff.set(DataComponents.CUSTOM_NAME, Component.literal("My catalyst"));
        ISpellContainer.createImbuedContainer(ModSpells.GLINTSTONE_PEBBLE.get(), 1, staff);
        EquipmentUpgradeRecipe recipe = recipe(helper, "azur_glintstone_staff");
        SmithingRecipeInput input = new SmithingRecipeInput(item("irons_spellbooks:epic_ink"), staff,
                new ItemStack(ModItems.ORIGIN_CRYSTAL.get()));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "staff upgrade is registered");
        ItemStack output = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(output.is(ModItems.AZUR_GLINTSTONE_STAFF.get()), "Azur staff returned");
        helper.assertTrue(ISpellContainer.get(output).getSpellAtIndex(0).getSpell() == ModSpells.GLINTSTONE_PEBBLE.get(),
                "imbued staff spell retained");
        helper.assertValueEqual(output.get(DataComponents.CUSTOM_NAME), staff.get(DataComponents.CUSTOM_NAME), "staff name");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void equipmentRecipeNetworkRoundtrip(GameTestHelper helper) {
        EquipmentUpgradeRecipe original = recipe(helper, "origin_codex");
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            EquipmentUpgradeRecipe.Serializer serializer = new EquipmentUpgradeRecipe.Serializer();
            serializer.streamCodec().encode(buffer, original);
            EquipmentUpgradeRecipe decoded = serializer.streamCodec().decode(buffer);
            SmithingRecipeInput input = new SmithingRecipeInput(item("irons_spellbooks:legendary_ink"),
                    new ItemStack(ModItems.STAR_CODEX.get()), new ItemStack(ModItems.ORIGIN_CRYSTAL.get()));
            helper.assertTrue(decoded.matches(input, helper.getLevel()), "client recipe retains ingredients");
            helper.assertTrue(decoded.assemble(input, helper.getLevel().registryAccess()).is(ModItems.ORIGIN_CODEX.get()),
                    "client recipe retains result");
            helper.assertTrue(buffer.readableBytes() == 0, "recipe packet fully consumed");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void staffSettingsCodecAndAttributeRefresh(GameTestHelper helper) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        double previousSpeed = AzurStaffBalance.castSpeedModifier().amount();
        double previousCost = AzurStaffBalance.manaCostMultiplier();
        try {
            AzurStaffSettingsPayload settings = new AzurStaffSettingsPayload(0.25D, 1.5D);
            AzurStaffSettingsPayload.STREAM_CODEC.encode(buffer, settings);
            AzurStaffSettingsPayload decoded = AzurStaffSettingsPayload.STREAM_CODEC.decode(buffer);
            helper.assertValueEqual(decoded, settings, "server settings packet roundtrip");
            AzurStaffBalance.acceptServerSettings(decoded.castTimeReduction(), decoded.manaCostMultiplier());
            helper.assertTrue(AzurStaffBalance.manaCostMultiplier() == 1.5D, "synchronized cost applied");
            ItemStack staff = new ItemStack(ModItems.AZUR_GLINTSTONE_STAFF.get());
            double[] bonus = {0.0D};
            staff.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                if (modifier.id().equals(AzurStaffBalance.CAST_SPEED_ID)) {
                    bonus[0] += modifier.amount();
                }
            });
            helper.assertTrue(bonus[0] == 0.25D, "attribute event uses new synchronized settings");
            double[] offhandBonus = {0.0D};
            staff.forEachModifier(EquipmentSlot.OFFHAND, (attribute, modifier) -> {
                if (modifier.id().equals(AzurStaffBalance.CAST_SPEED_ID)) {
                    offhandBonus[0] += modifier.amount();
                }
            });
            helper.assertTrue(offhandBonus[0] == 0.0D, "offhand has no casting-speed bonus");
        } finally {
            AzurStaffBalance.acceptServerSettings(previousSpeed, previousCost);
            buffer.release();
        }
        helper.succeed();
    }

    private static EquipmentUpgradeRecipe recipe(GameTestHelper helper, String path) {
        var holder = helper.getLevel().getRecipeManager().byKey(id(path)).orElseThrow();
        helper.assertTrue(holder.value() instanceof EquipmentUpgradeRecipe, "custom serializer decoded " + path);
        return (EquipmentUpgradeRecipe) holder.value();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, path);
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }
}

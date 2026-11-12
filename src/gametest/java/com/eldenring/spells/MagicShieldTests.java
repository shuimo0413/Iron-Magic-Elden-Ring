package com.eldenring.spells;

import com.eldenring.spells.entity.MagicShieldEntity;
import com.eldenring.spells.registry.*;
import com.eldenring.spells.recipe.GlintstoneScrollRecipes;
import com.eldenring.spells.spell.MagicShieldSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.item.InkItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EldenRingSpellsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MagicShieldTests {
    private static LivingEntity target(GameTestHelper helper) {
        var owner = helper.spawn(EntityType.COW, new BlockPos(1, 2, 1));
        owner.setNoAi(true);
        owner.setNoGravity(true);
        return owner;
    }
    private static MagicShieldEntity grant(LivingEntity owner) {
        var shield = new MagicShieldEntity(owner.level(), owner);
        shield.refresh(5);
        owner.level().addFreshEntity(shield);
        owner.getData(ModAttachments.MAGIC_SHIELD).entity = shield;
        owner.addEffect(new MobEffectInstance(ModEffects.MAGIC_SHIELD, 600));
        return shield;
    }
    private static void close(GameTestHelper h, float actual, float expected, String label) {
        h.assertTrue(Math.abs(actual - expected) < 0.001, label + ": " + actual + " != " + expected);
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void hitDirectionDoesNotChangeAbsorption(GameTestHelper h) {
        var owner = target(h);
        var shield = grant(owner);
        close(h, shield.absorb(2, owner.position().add(3, 0, 0)), 0, "east hit absorbed");
        close(h, shield.hitAngle(), 90, "east visual direction");
        close(h, shield.capacity(), 10, "direction has no damage multiplier");
        close(h, shield.absorb(2, owner.position().add(0, 0, -3)), 0, "north hit absorbed");
        close(h, Math.abs(shield.hitAngle()), 180, "north visual direction");
        close(h, shield.capacity(), 8, "shared pool remains unchanged by direction");
        shield.absorb(1, null);
        close(h, shield.hitAngle(), -1000, "environmental damage has no invented direction");
        shield.refresh(5);
        close(h, shield.capacity(), 12, "refresh resets capacity");
        close(h, shield.flash(0), 0, "refresh clears hit animation");
        h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void absorbsAndOverflows(GameTestHelper h) {
        var owner = target(h);
        var shield = grant(owner);
        float health = owner.getHealth();
        owner.hurt(owner.damageSources().generic(), 8);
        close(h, owner.getHealth(), health, "first hit absorbed");
        close(h, shield.capacity(), 4, "shared capacity consumed once");
        owner.invulnerableTime = 0;
        owner.hurt(owner.damageSources().generic(), 7);
        close(h, owner.getHealth(), health - 3, "overflow reaches health");
        h.assertTrue(shield.isRemoved() && !owner.hasEffect(ModEffects.MAGIC_SHIELD), "break removes shield and buff");
        h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void resistanceAndAbsorptionOrder(GameTestHelper h) {
        var owner = target(h);
        var shield = grant(owner);
        owner.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 100, 0));
        owner.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION, 100, 0));
        close(h, owner.getAbsorptionAmount(), 4, "absorption effect grants four golden hearts");
        owner.hurt(owner.damageSources().generic(), 10);
        close(h, shield.capacity(), 4, "20 percent resistance applies before shield");
        close(h, owner.getAbsorptionAmount(), 4, "golden hearts are preserved until shield is exhausted");
        owner.removeEffect(ModEffects.MAGIC_SHIELD);
        owner.invulnerableTime = 0;
        owner.hurt(owner.damageSources().generic(), 10);
        close(h, shield.capacity(), 4, "cleansing immediately disables absorption");
        h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void refreshRecipeAndExpiry(GameTestHelper h) {
        var owner = target(h);
        var shield = grant(owner);
        shield.absorb(8);
        ModSpells.MAGIC_SHIELD.get().onCast(owner.level(), 5, owner, CastSource.SPELLBOOK, new MagicData());
        h.assertTrue(owner.getData(ModAttachments.MAGIC_SHIELD).entity == shield, "recast reuses one shield");
        close(h, shield.capacity(), MagicShieldSpell.capacity(5), "recast resets rather than adds capacity");
        h.assertTrue(GlintstoneScrollRecipes.matchesFocus(ModSpells.MAGIC_SHIELD.get(), new ItemStack(ModItems.CYAN_GLINTSTONE_SHARD.get())), "cyan focus accepted");
        h.assertTrue(!GlintstoneScrollRecipes.matchesFocus(ModSpells.MAGIC_SHIELD.get(), new ItemStack(Items.SHIELD)), "physical shield is not crafting material");
        h.assertTrue(!GlintstoneScrollRecipes.matchesFocus(ModSpells.MAGIC_SHIELD.get(), new ItemStack(ModItems.ORIGIN_GLINTSTONE.get())), "origin focus rejected");
        // Explicitly remove the longer instance so the one-tick test effect replaces it.
        owner.removeEffect(ModEffects.MAGIC_SHIELD);
        owner.addEffect(new MobEffectInstance(ModEffects.MAGIC_SHIELD, 1));
        h.runAfterDelay(3, () -> {
            h.assertTrue(shield.isRemoved(), "expired effect removes visual anchor");
            h.succeed();
        });
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void voidIsNotAbsorbed(GameTestHelper h) {
        var owner = target(h);
        var shield = grant(owner);
        float health = owner.getHealth();
        owner.hurt(owner.damageSources().fellOutOfWorld(), 4);
        close(h, shield.capacity(), 12, "void does not consume shield");
        close(h, owner.getHealth(), health - 4, "void still damages owner");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void fiveTiersAndInk(GameTestHelper h) {
        // Dedicated GameTests have no player login to trigger Iron's config synchronization.
        io.redspace.ironsspellbooks.api.config.SpellConfigManager.onDatapackSync(
                new net.neoforged.neoforge.event.OnDatapackSyncEvent(h.getLevel().getServer().getPlayerList(), null));
        var spell = ModSpells.MAGIC_SHIELD.get();
        var focus = new ItemStack(ModItems.CYAN_GLINTSTONE_SHARD.get());
        var rarities = new SpellRarity[]{SpellRarity.COMMON, SpellRarity.UNCOMMON, SpellRarity.RARE, SpellRarity.EPIC, SpellRarity.LEGENDARY};
        var inks = new ItemStack[]{new ItemStack(ItemRegistry.INK_COMMON.get()),
                new ItemStack(ItemRegistry.INK_UNCOMMON.get()), new ItemStack(ItemRegistry.INK_RARE.get()),
                new ItemStack(ItemRegistry.INK_EPIC.get()), new ItemStack(ItemRegistry.INK_LEGENDARY.get())};
        h.assertTrue(spell.getMaxLevel() == 5, "five levels: " + spell.getMaxLevel());
        h.assertTrue(spell.getSpellCooldown() == 500, "twenty-five-second base cooldown: " + spell.getSpellCooldown());
        int[] mana = {20, 25, 35, 45, 60};
        for (int i = 0; i < 5; i++) {
            int level = i + 1;
            h.assertTrue(spell.getRarity(level) == rarities[i], "explicit tier rarity");
            h.assertTrue(spell.getMinLevelForRarity(((InkItem) inks[i].getItem()).getRarity()) == level, "ink maps to correct level");
            h.assertTrue(GlintstoneScrollRecipes.matchesCrafting(spell, focus, inks[i]), "tier ink accepted");
            close(h, spell.getManaCost(level), mana[i], "tier mana cost");
            var owner = target(h);
            spell.onCast(owner.level(), level, owner, CastSource.SPELLBOOK, new MagicData());
            var shield = owner.getData(ModAttachments.MAGIC_SHIELD).entity;
            close(h, shield.capacity(), 4 + i * 2, "tier shield capacity");
            h.assertTrue(owner.getEffect(ModEffects.MAGIC_SHIELD).getDuration() == 400 + i * 100, "tier lifetime");
            h.assertTrue(owner.getEffect(ModEffects.MAGIC_SHIELD).getAmplifier() == i, "buff displays level");
        }
        h.assertTrue(!GlintstoneScrollRecipes.matchesCrafting(spell, focus, new ItemStack(Items.INK_SAC)), "ordinary ink sac cannot bypass magic ink cost");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void upgradesAndRejectsDowngrade(GameTestHelper h) {
        var spell = ModSpells.MAGIC_SHIELD.get();
        var owner = target(h);
        var data = new MagicData();
        spell.onCast(owner.level(), 1, owner, CastSource.SPELLBOOK, data);
        var shield = owner.getData(ModAttachments.MAGIC_SHIELD).entity;
        shield.absorb(2);
        spell.onCast(owner.level(), 2, owner, CastSource.SPELLBOOK, data);
        h.assertTrue(owner.getData(ModAttachments.MAGIC_SHIELD).entity == shield, "upgrade reuses entity");
        close(h, shield.capacity(), 6, "upgrade replaces rather than adds capacity");
        shield.absorb(3);
        h.assertTrue(!spell.checkPreCastConditions(owner.level(), 1, owner, data), "downgrade rejected before payment");
        spell.onCast(owner.level(), 1, owner, CastSource.SPELLBOOK, data);
        close(h, shield.capacity(), 3, "direct low-level casts cannot restore higher shield");
        h.assertTrue(shield.spellLevel() == 2, "higher tier preserved");
        spell.onCast(owner.level(), 2, owner, CastSource.SPELLBOOK, data);
        close(h, shield.capacity(), 6, "same-tier refresh resets pool");
        spell.onCast(owner.level(), 5, owner, CastSource.SPELLBOOK, data);
        close(h, shield.capacity(), 12, "legendary replaces epic");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void acquisitionTiers(GameTestHelper h) {
        io.redspace.ironsspellbooks.api.config.SpellConfigManager.onDatapackSync(
                new net.neoforged.neoforge.event.OnDatapackSyncEvent(h.getLevel().getServer().getPlayerList(), null));
        var spell = ModSpells.MAGIC_SHIELD.get();
        h.assertTrue(!spell.allowLooting(), "generic loot must not bypass tiered reward tables");
        String[] tables = {"reward_common", "reward_rare", "reward_epic"};
        int[][] bounds = {{1, 2}, {2, 3}, {4, 5}};
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, h.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1)))
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        for (int index = 0; index < tables.length; index++) {
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "chests/" + tables[index]));
            var table = h.getLevel().getServer().reloadableRegistries().getLootTable(key);
            java.util.Set<Integer> seen = new java.util.HashSet<>();
            for (int sample = 1; sample <= 300; sample++) {
                for (var stack : table.getRandomItems(params, sample * 7919L)) {
                    if (!io.redspace.ironsspellbooks.api.spells.ISpellContainer.isSpellContainer(stack)) continue;
                    var entry = io.redspace.ironsspellbooks.api.spells.ISpellContainer.get(stack).getSpellAtIndex(0);
                    if (entry.getSpell() != spell) continue;
                    int level = entry.getLevel();
                    h.assertTrue(level >= bounds[index][0] && level <= bounds[index][1], "reward tier boundary: " + tables[index] + " level " + level);
                    seen.add(level);
                }
            }
            h.assertTrue(seen.contains(bounds[index][0]) && seen.contains(bounds[index][1]), "both reward tiers are obtainable: " + tables[index]);
        }
        int sales = 0;
        for (int sample = 0; sample < 100; sample++) {
            var offers = com.eldenring.spells.entity.astrologer.AstrologerTrades.createOffers(net.minecraft.util.RandomSource.create(sample * 7919L), false);
            for (var offer : offers) {
                var stack = offer.getResult();
                if (!io.redspace.ironsspellbooks.api.spells.ISpellContainer.isSpellContainer(stack)) continue;
                var entry = io.redspace.ironsspellbooks.api.spells.ISpellContainer.get(stack).getSpellAtIndex(0);
                if (entry.getSpell() != spell) continue;
                sales++;
                h.assertTrue(entry.getLevel() == 1 && offer.getMaxUses() == 1, "merchant only sells limited common shield");
                h.assertTrue(offer.getCostA().getCount() >= 8 && offer.getCostA().getCount() <= 12, "common shield emerald price");
            }
        }
        h.assertTrue(sales > 0 && sales < 100, "merchant shield is available but not guaranteed");
        h.succeed();
    }
}

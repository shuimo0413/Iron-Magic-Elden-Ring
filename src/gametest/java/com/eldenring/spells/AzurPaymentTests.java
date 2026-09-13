package com.eldenring.spells;

import com.eldenring.spells.item.AzurCastCostData;
import com.eldenring.spells.item.AzurManaCostPolicy;
import com.eldenring.spells.item.AzurStaffBalance;
import com.eldenring.spells.registry.ModItems;
import com.eldenring.spells.registry.ModSpells;
import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;
import java.util.function.Consumer;

/** Test-only spell bodies count effects while executing the real, transformed castSpell lifecycle. */
@GameTestHolder(EldenRingSpellsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AzurPaymentTests {
    private AzurPaymentTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void initialAffordabilityUsesMainhandOnly(GameTestHelper helper) {
        Fixture f = fixture(helper, CastType.LONG);
        int price = configuredPrice();
        f.data.setMana(price - 1);
        helper.assertFalse(f.spell.canBeCastedBy(1, CastSource.SPELLBOOK, f.data, f.player).isSuccess(),
                "initial cast rejects less than the configured full price");
        f.data.setMana(price);
        helper.assertTrue(f.spell.canBeCastedBy(1, CastSource.SPELLBOOK, f.data, f.player).isSuccess(),
                "initial cast accepts exact price");
        f.player.setItemSlot(EquipmentSlot.OFFHAND, f.player.getMainHandItem().copy());
        f.player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
        f.data.setMana(10);
        helper.assertTrue(f.spell.canBeCastedBy(1, CastSource.SPELLBOOK, f.data, f.player).isSuccess(),
                "offhand Azur does not surcharge");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void castStartSnapshotSurvivesSwitchingAway(GameTestHelper helper) {
        Fixture f = fixture(helper, CastType.LONG);
        start(f, CastSource.SPELLBOOK);
        helper.assertTrue(((AzurCastCostData) f.data).eldenRingSpells$getAzurMultiplier()
                == AzurStaffBalance.manaCostMultiplier(), "start multiplier captured");
        f.player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
        f.data.setMana(100);
        finalPulse(f, CastSource.SPELLBOOK);
        helper.assertTrue(f.spell.casts == 1 && f.data.getMana() == 100 - configuredPrice(),
                "switching away retains the price of the accelerated cast");
        helper.assertTrue(((AzurCastCostData) f.data).eldenRingSpells$getAzurMultiplier() == 1.0D,
                "completion clears the snapshot");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void postedEventDiscountControlsFinalPayment(GameTestHelper helper) {
        Fixture f = fixture(helper, CastType.LONG);
        start(f, CastSource.SPELLBOOK);
        f.data.setMana(5);
        int[] eventCount = {0};
        listen(SpellOnCastEvent.class, event -> {
            if (event.getEntity() == f.player) {
                eventCount[0]++;
                helper.assertTrue(event.getOriginalManaCost() == configuredPrice(),
                        "event receives the Azur-adjusted original cost");
                event.setManaCost(5);
            }
        }, () -> finalPulse(f, CastSource.SPELLBOOK));
        helper.assertTrue(eventCount[0] == 1, "event is posted exactly once");
        helper.assertTrue(f.spell.casts == 1 && f.data.getMana() == 0, "discounted final payment succeeds");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void postedEventIncreaseCannotPartiallyPay(GameTestHelper helper) {
        Fixture f = fixture(helper, CastType.LONG);
        start(f, CastSource.SPELLBOOK);
        f.data.setMana(configuredPrice());
        listen(SpellOnCastEvent.class, event -> {
            if (event.getEntity() == f.player) {
                event.setManaCost(configuredPrice() + 10);
            }
        }, () -> finalPulse(f, CastSource.SPELLBOOK));
        helper.assertTrue(f.spell.casts == 0, "unaffordable event-increased spell has no effect");
        helper.assertTrue(f.data.getMana() == configuredPrice(), "failed payment leaves mana intact");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void unpaidFinalChannelPulseRetainsCooldown(GameTestHelper helper) {
        Fixture f = fixture(helper, CastType.CONTINUOUS);
        start(f, CastSource.SPELLBOOK);
        f.data.setMana(100);
        f.spell.castSpell(f.player.level(), 1, f.player, CastSource.SPELLBOOK, false);
        helper.assertTrue(f.spell.casts == 1, "an earlier channel pulse actually executed");
        f.data.setMana(0);
        int[] cooldownEvents = {0};
        listen(SpellCooldownAddedEvent.Pre.class, event -> {
            if (event.getEntity() == f.player) {
                cooldownEvents[0]++;
            }
        }, () -> finalPulse(f, CastSource.SPELLBOOK));
        helper.assertTrue(f.spell.casts == 1, "unpaid last pulse is not executed");
        helper.assertTrue(f.data.getMana() == 0 && !f.data.isCasting(), "channel ends without partial payment");
        helper.assertTrue(f.data.getPlayerCooldowns().isOnCooldown(f.spell), "channel cannot restart without cooldown");
        helper.assertTrue(cooldownEvents[0] == 1 && f.spell.completions == 1,
                "one cooldown event and one native completion");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void unpaidChannelRespectsCooldownCancellation(GameTestHelper helper) {
        Fixture f = fixture(helper, CastType.CONTINUOUS);
        start(f, CastSource.SPELLBOOK);
        f.data.setMana(0);
        int[] cooldownEvents = {0};
        listen(SpellCooldownAddedEvent.Pre.class, event -> {
            if (event.getEntity() == f.player) {
                cooldownEvents[0]++;
                event.setCanceled(true);
            }
        }, () -> finalPulse(f, CastSource.SPELLBOOK));
        helper.assertTrue(cooldownEvents[0] == 1, "native cancellable cooldown event was posted");
        helper.assertFalse(f.data.getPlayerCooldowns().isOnCooldown(f.spell), "cooldown event cancellation honored");
        helper.assertTrue(f.spell.casts == 0 && f.spell.completions == 1, "cancellation grants no unpaid pulse");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void nativeFreeSourcesRecastsAndCreativeStayExempt(GameTestHelper helper) {
        Fixture scroll = fixture(helper, CastType.LONG);
        start(scroll, CastSource.SCROLL);
        scroll.data.setMana(0);
        finalPulse(scroll, CastSource.SCROLL);
        helper.assertTrue(scroll.spell.casts == 1 && scroll.data.getMana() == 0, "scroll is mana-free");

        Fixture recast = fixture(helper, CastType.LONG);
        start(recast, CastSource.SPELLBOOK);
        recast.data.getPlayerRecasts().forceAddRecast(recast(recast));
        recast.data.setMana(0);
        listen(SpellOnCastEvent.class, event -> {
            if (event.getEntity() == recast.player) {
                event.setManaCost(100);
            }
        }, () -> finalPulse(recast, CastSource.SPELLBOOK));
        helper.assertTrue(recast.spell.casts == 1 && recast.data.getMana() == 0,
                "preexisting recast stays free even after an event raises cost");

        Fixture creative = fixture(helper, CastType.LONG);
        creative.player.creative = true;
        start(creative, CastSource.SPELLBOOK);
        creative.data.setMana(0);
        finalPulse(creative, CastSource.SPELLBOOK);
        helper.assertTrue(creative.spell.casts == (ServerConfigs.CREATIVE_MANA_COST.get() ? 0 : 1),
                "creative mana setting controls payment exemption");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void eventAddedRecastDoesNotRewritePaymentSnapshot(GameTestHelper helper) {
        Fixture f = fixture(helper, CastType.LONG);
        start(f, CastSource.SPELLBOOK);
        f.data.setMana(configuredPrice());
        listen(SpellOnCastEvent.class, event -> {
            if (event.getEntity() == f.player) {
                f.data.getPlayerRecasts().forceAddRecast(recast(f));
                event.setManaCost(configuredPrice() + 10);
            }
        }, () -> finalPulse(f, CastSource.SPELLBOOK));
        helper.assertTrue(f.spell.casts == 0 && f.data.getMana() == configuredPrice(),
                "native pre-event recast snapshot still requires complete payment");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void preCastEventCancellationStillPreventsCast(GameTestHelper helper) {
        Fixture f = fixture(helper, CastType.LONG);
        f.data.setMana(100);
        boolean[] started = {true};
        listen(SpellPreCastEvent.class, event -> {
            if (event.getEntity() == f.player) {
                event.setCanceled(true);
            }
        }, () -> started[0] = f.spell.attemptInitiateCast(f.player.getMainHandItem(), 1, f.player.level(),
                f.player, CastSource.SPELLBOOK, true, "mainhand"));
        helper.assertFalse(started[0], "pre-cast cancellation rejects initiation");
        helper.assertTrue(!f.data.isCasting() && f.spell.casts == 0 && f.data.getMana() == 100,
                "canceled attempt neither starts nor spends mana");
        helper.succeed();
    }

    private static int configuredPrice() {
        return AzurManaCostPolicy.apply(10, AzurStaffBalance.manaCostMultiplier(), true);
    }

    private static Fixture fixture(GameTestHelper helper, CastType type) {
        TestPlayer player = new TestPlayer(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.AZUR_GLINTSTONE_STAFF.get()));
        MagicData data = MagicData.getPlayerMagicData(player);
        data.setServerPlayer(player);
        // FakePlayer skips the native login path that creates synchronized casting state.
        data.setSyncedData(new SyncedSpellData(player));
        return new Fixture(player, data, new ProbeSpell(type));
    }

    private static void start(Fixture f, CastSource source) {
        f.data.initiateCast(f.spell, 1, 20, source, "mainhand");
    }

    private static RecastInstance recast(Fixture f) {
        return new RecastInstance(f.spell.getSpellId(), 1, 3, 200, CastSource.SPELLBOOK, null);
    }

    private static void finalPulse(Fixture f, CastSource source) {
        // This is the exact native manager sequence for its last continuous/long pulse.
        f.spell.castSpell(f.player.level(), 1, f.player, source, true);
        f.spell.onServerCastComplete(f.player.level(), 1, f.player, f.data, false);
    }

    private static <T extends Event> void listen(Class<T> type, Consumer<T> listener, Runnable action) {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, type, listener);
        try {
            action.run();
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
    }

    private record Fixture(TestPlayer player, MagicData data, ProbeSpell spell) {
    }

    private static final class TestPlayer extends FakePlayer {
        private boolean creative;

        private TestPlayer(GameTestHelper helper) {
            super(helper.getLevel(), new GameProfile(UUID.randomUUID(), "azur-payment-test"));
        }

        @Override
        public boolean isCreative() {
            return creative;
        }
    }

    private static final class ProbeSpell extends AbstractSpell {
        private final CastType type;
        private int casts;
        private int completions;

        private ProbeSpell(CastType type) {
            this.type = type;
            this.castTime = 20;
        }

        @Override
        public ResourceLocation getSpellResource() {
            return ModSpells.GLINTSTONE_PEBBLE.get().getSpellResource();
        }

        @Override
        public DefaultConfig getDefaultConfig() {
            return ModSpells.GLINTSTONE_PEBBLE.get().getDefaultConfig();
        }

        @Override
        public CastType getCastType() {
            return type;
        }

        @Override
        public SchoolType getSchoolType() {
            return SchoolRegistry.EVOCATION.get();
        }

        @Override
        public int getManaCost(int level) {
            return 10;
        }

        @Override
        public int getSpellCooldown() {
            return 40;
        }

        @Override
        public boolean requiresLearning() {
            return false;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource source, MagicData data) {
            casts++;
        }

        @Override
        public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData data,
                                         boolean canceled) {
            completions++;
            super.onServerCastComplete(level, spellLevel, entity, data, canceled);
        }
    }
}

package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.spell.CometSpell;
import com.eldenring.spells.spell.GlintstoneCometSpell;
import com.eldenring.spells.spell.GlintstonePebbleSpell;
import com.eldenring.spells.spell.GlintstoneStarsSpell;
import com.eldenring.spells.spell.GreatGlintstoneShardSpell;
import com.eldenring.spells.spell.SpiralShardSpell;
import com.eldenring.spells.spell.StarShowerSpell;
import com.eldenring.spells.spell.StarsOfRuinSpell;
import com.eldenring.spells.spell.SwiftGlintstoneShardSpell;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, EldenRingSpellsMod.MOD_ID);

    public static final Supplier<AbstractSpell> GLINTSTONE_PEBBLE =
            registerSpell(new GlintstonePebbleSpell());

    public static final Supplier<AbstractSpell> SWIFT_GLINTSTONE_SHARD =
            registerSpell(new SwiftGlintstoneShardSpell());

    public static final Supplier<AbstractSpell> GREAT_GLINTSTONE_SHARD =
            registerSpell(new GreatGlintstoneShardSpell());

    public static final Supplier<AbstractSpell> GLINTSTONE_COMET =
            registerSpell(new GlintstoneCometSpell());

    public static final Supplier<AbstractSpell> GLINTSTONE_STARS =
            registerSpell(new GlintstoneStarsSpell());

    public static final Supplier<AbstractSpell> STAR_SHOWER =
            registerSpell(new StarShowerSpell());

    public static final Supplier<AbstractSpell> STARS_OF_RUIN =
            registerSpell(new StarsOfRuinSpell());

    public static final Supplier<AbstractSpell> COMET =
            registerSpell(new CometSpell());

    public static final Supplier<AbstractSpell> SPIRAL_SHARD =
            registerSpell(new SpiralShardSpell());

    private ModSpells() {
    }

    private static Supplier<AbstractSpell> registerSpell(AbstractSpell spell) {
        return SPELLS.register(spell.getSpellName(), () -> spell);
    }

    public static void register(IEventBus modEventBus) {
        SPELLS.register(modEventBus);
    }
}

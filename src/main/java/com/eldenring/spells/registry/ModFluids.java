package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.fluid.NoopFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * 本模组流体。目前仅起源药剂液，供铁魔法炼药锅 brew / empty / fill。
 */
public final class ModFluids {
    private static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, EldenRingSpellsMod.MOD_ID);
    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, EldenRingSpellsMod.MOD_ID);

    /** 起源药剂流体类型（罐内青色液）。 */
    public static final DeferredHolder<FluidType, FluidType> ORIGIN_POTION_TYPE =
            FLUID_TYPES.register("origin_potion", () -> new FluidType(FluidType.Properties.create()));

    /** 起源药剂流体本体（无世界方块）。 */
    public static final DeferredHolder<Fluid, NoopFluid> ORIGIN_POTION =
            registerNoop("origin_potion", ORIGIN_POTION_TYPE::value);

    private ModFluids() {
    }

    private static DeferredHolder<Fluid, NoopFluid> registerNoop(String name, Supplier<FluidType> fluidType) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, name);
        DeferredHolder<Fluid, NoopFluid> holder = DeferredHolder.create(Registries.FLUID, id);
        BaseFlowingFluid.Properties properties =
                new BaseFlowingFluid.Properties(fluidType, holder::value, holder::value)
                        .bucket(() -> Items.AIR);
        FLUIDS.register(name, () -> new NoopFluid(properties));
        return holder;
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
    }
}

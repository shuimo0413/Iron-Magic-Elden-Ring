package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.fluid.NoopFluid;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 本模组流体。目前仅起源药剂液，供铁魔法炼药锅 brew / empty / fill。
 * <p>
 * Forge 1.20.1 没有 {@code BaseFlowingFluid}，改用 {@link ForgeFlowingFluid}；
 * 客户端染色也没有 {@code RegisterClientExtensionsEvent}，改为在
 * {@link FluidType#initializeClient(Consumer)} 内注册。
 */
public final class ModFluids {

    /**
     * 起源药剂罐内液体染色：ARGB，青蓝 #2FADA2。
     * 与水 still/flow 贴图相乘，呈现「发光青蓝药液」观感；改淡会偏向透明水，改深会接近墨绿。
     */
    private static final int ORIGIN_POTION_TINT_COLOR = 0xFF2FADA2;

    /**
     * 原版水静帧 / 流动贴图。本分支统一用 {@code new ResourceLocation(ns, path)}
     *（与双分支写法一致；mapped 环境里工厂方法虽在但 constructor 仅 deprecation 警告）。
     * 必须用 {@code new ResourceLocation(namespace, path)} 构造。
     * <p>
     * 必须显式覆写：Forge 1.20.1 的 {@code IClientFluidTypeExtensions#getStillTexture/getFlowingTexture}
     * 默认返回 {@code null}（不会自动回退原版水贴图），不覆写会导致液体缺少模型贴图。
     */
    private static final ResourceLocation WATER_STILL_TEXTURE =
            new ResourceLocation("minecraft", "block/water_still");
    private static final ResourceLocation WATER_FLOW_TEXTURE =
            new ResourceLocation("minecraft", "block/water_flow");

    private static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, EldenRingSpellsMod.MOD_ID);
    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, EldenRingSpellsMod.MOD_ID);

    /**
     * 起源药剂流体类型（罐内青色液）。
     * <p>
     * 覆写 {@code initializeClient} 只影响客户端渲染；方法体里引用的
     * {@link IClientFluidTypeExtensions} 匿名实现只会在客户端真正建纹理时加载，服务端不触碰。
     */
    public static final RegistryObject<FluidType> ORIGIN_POTION_TYPE =
            FLUID_TYPES.register("origin_potion", () -> new FluidType(FluidType.Properties.create()) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override
                        public int getTintColor() {
                            return ORIGIN_POTION_TINT_COLOR;
                        }

                        @Override
                        public ResourceLocation getStillTexture() {
                            return WATER_STILL_TEXTURE;
                        }

                        @Override
                        public ResourceLocation getFlowingTexture() {
                            return WATER_FLOW_TEXTURE;
                        }
                    });
                }
            });

    /** 起源药剂流体本体（无世界方块）。 */
    public static final RegistryObject<NoopFluid> ORIGIN_POTION =
            registerNoop("origin_potion", ORIGIN_POTION_TYPE::get);

    private ModFluids() {
    }

    /**
     * 注册一个无方块流体，并返回它的惰性句柄。
     * <p>
     * {@link ForgeFlowingFluid.Properties} 需要「流体类型 / 流动态 / 静止态」三个 Supplier，
     * 而液体此刻尚未真正 register 完成，因此先用 {@link RegistryObject#create} 建惰性句柄，
     * 注册完成后 {@code get()} 才能解析——这是 Forge 下自引用流体属性的标准写法。
     * <p>
     * 注意：Forge 的 {@link RegistryObject} 只有 {@code get()}（实现 {@link java.util.function.Supplier}），
     * 没有 NeoForge {@code DeferredHolder} 的 {@code value()}，方法引用必须写 {@code ::get}。
     */
    private static RegistryObject<NoopFluid> registerNoop(String name, Supplier<FluidType> fluidType) {
        ResourceLocation id = new ResourceLocation(EldenRingSpellsMod.MOD_ID, name);
        RegistryObject<NoopFluid> holder =
                RegistryObject.create(id, Registries.FLUID, EldenRingSpellsMod.MOD_ID);
        ForgeFlowingFluid.Properties properties =
                new ForgeFlowingFluid.Properties(fluidType, holder::get, holder::get)
                        .bucket(() -> Items.AIR);
        FLUIDS.register(name, () -> new NoopFluid(properties));
        return holder;
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
    }
}

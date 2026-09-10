package com.eldenring.spells;

import com.eldenring.spells.client.CarianGreatswordClientHold;
import com.eldenring.spells.client.CarianPiercerClientHold;
import com.eldenring.spells.client.CarianSlicerClientHold;
import com.eldenring.spells.client.ClientEntityRenderers;
import com.eldenring.spells.client.ClientItemModels;
import com.eldenring.spells.client.ClientParticleProviders;
import com.eldenring.spells.registry.ModBlocks;
import com.eldenring.spells.registry.ModDecorBlocks;
import com.eldenring.spells.registry.ModFluids;
import com.eldenring.spells.registry.ModItems;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import io.redspace.ironsspellbooks.render.ClientStaffItemExtensions;
import io.redspace.ironsspellbooks.render.SpellBookCurioRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

/**
 * 客户端入口：粒子 / 实体渲染 / 卷轴模型 / 魔法书 Curios 渲染。
 */
@Mod(value = EldenRingSpellsMod.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID, value = Dist.CLIENT)
public class EldenRingSpellsClient {
    public EldenRingSpellsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        EldenRingSpellsMod.LOGGER.info(
                "Elden Ring Spells client ready. Player={}",
                Minecraft.getInstance().getUser().getName()
        );
        // 十字面片水晶必须走 cutout，否则透明像素会糊成黑块
        event.enqueueWork(() -> {
            for (ModBlocks.ColorSet set : ModBlocks.BY_COLOR.values()) {
                ItemBlockRenderTypes.setRenderLayer(set.cluster.get(), RenderType.cutout());
            }
            for (ModDecorBlocks.CandelabraSet set : ModDecorBlocks.CANDELABRAS_BY_COLOR.values()) {
                ItemBlockRenderTypes.setRenderLayer(set.candelabra.get(), RenderType.cutout());
            }
            // 星星法典 / 起源秘典：复用铁魔法 SpellBookCurioRenderer，腰侧显示立体书模型
            CuriosRendererRegistry.register(ModItems.STAR_CODEX.get(), SpellBookCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.ORIGIN_CODEX.get(), SpellBookCurioRenderer::new);
            // 迅剑 / 大剑专用层：无 MirrorModifier / 准星跟臂，避免右→左被翻成左→右。
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                    CarianSlicerClientHold.CARIAN_SLICER_ANIMATION_LAYER,
                    60,
                    player -> new ModifierLayer<>()
            );
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                    CarianGreatswordClientHold.CARIAN_GREATSWORD_ANIMATION_LAYER,
                    60,
                    player -> new ModifierLayer<>()
            );
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                    CarianPiercerClientHold.CARIAN_PIERCER_ANIMATION_LAYER,
                    60,
                    player -> new ModifierLayer<>()
            );
        });
    }

    /**
     * 观星杖 / 亚兹勒的辉石杖复用铁魔法法杖握持姿势（抬臂），否则会像普通物品一样僵硬下垂。
     * 起源药剂流体：水贴图 + 青色染色，供炼药锅罐内显示。
     */
    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new ClientStaffItemExtensions(), ModItems.ASTROLOGER_STAFF.get());
        event.registerItem(new ClientStaffItemExtensions(), ModItems.AZUR_GLINTSTONE_STAFF.get());

        // 0xAARRGGBB：不透明青（对齐碎片 mid #2FADA2）
        final int originPotionTintArgb = 0xFF2FADA2;
        final ResourceLocation waterStill = ResourceLocation.withDefaultNamespace("block/water_still");
        final ResourceLocation waterFlow = ResourceLocation.withDefaultNamespace("block/water_flow");
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public int getTintColor() {
                return originPotionTintArgb;
            }

            @Override
            public ResourceLocation getStillTexture() {
                return waterStill;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return waterFlow;
            }
        }, ModFluids.ORIGIN_POTION_TYPE.get());
    }

    @SubscribeEvent
    static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        ClientParticleProviders.register(event);
    }

    @SubscribeEvent
    static void registerScrollModels(ModelEvent.RegisterAdditional event) {
        ClientItemModels.register(event);
    }

    @SubscribeEvent
    static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        ClientEntityRenderers.registerLayerDefinitions(event);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ClientEntityRenderers.registerRenderers(event);
    }

    @SubscribeEvent
    static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        ClientEntityRenderers.addPlayerLayers(event);
    }
}

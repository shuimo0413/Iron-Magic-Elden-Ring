package com.eldenring.spells;

import com.eldenring.spells.client.CarianGreatswordClientHold;
import com.eldenring.spells.client.CarianPiercerClientHold;
import com.eldenring.spells.client.CarianSlicerClientHold;
import com.eldenring.spells.client.ClientEntityRenderers;
import com.eldenring.spells.client.ClientItemModels;
import com.eldenring.spells.client.ClientParticleProviders;
import com.eldenring.spells.registry.ModBlocks;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 客户端入口：只转发粒子 / 实体渲染 / 卷轴模型注册。
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

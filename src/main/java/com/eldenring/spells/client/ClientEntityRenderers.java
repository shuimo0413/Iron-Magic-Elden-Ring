package com.eldenring.spells.client;

import com.eldenring.spells.client.render.CometAzurJetRenderer;
import com.eldenring.spells.client.render.FoundingRainDropRenderer;
import com.eldenring.spells.client.render.FoundingRainNebulaRenderer;
import com.eldenring.spells.client.render.StarlightRenderer;
import com.eldenring.spells.client.render.TerraMagicaZoneRenderer;
import com.eldenring.spells.client.render.carian.CarianGreatswordHandLayer;
import com.eldenring.spells.client.render.carian.CarianPiercerHandLayer;
import com.eldenring.spells.client.render.carian.CarianSlicerHandLayer;
import com.eldenring.spells.client.render.carian.MagicGlintbladeModels;
import com.eldenring.spells.client.render.carian.MagicGlintbladeRenderer;
import com.eldenring.spells.client.render.glintstone.GlintstoneArcRenderer;
import com.eldenring.spells.client.render.glintstone.GlintstoneCometModels;
import com.eldenring.spells.client.render.glintstone.GlintstoneProjectileRenderer;
import com.eldenring.spells.client.render.glintstone.SpiralShardRenderer;
import com.eldenring.spells.client.render.haima.HaimaCannonModels;
import com.eldenring.spells.client.render.haima.HaimaCannonRenderer;
import com.eldenring.spells.client.render.haima.HaimaGavelModels;
import com.eldenring.spells.client.render.haima.HaimaGavelRenderer;
import com.eldenring.spells.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 实体 Renderer 与模型层注册。由 {@code EldenRingSpellsClient} 转发。
 */
public final class ClientEntityRenderers {

    private ClientEntityRenderers() {
    }

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
                GlintstoneCometModels.COMET_HEAD_LAYER,
                GlintstoneCometModels::createCometHeadLayer
        );
        event.registerLayerDefinition(
                GlintstoneCometModels.SPIKED_COMET_HEAD_LAYER,
                GlintstoneCometModels::createSpikedCometHeadLayer
        );
        event.registerLayerDefinition(
                HaimaGavelModels.GAVEL_LAYER,
                HaimaGavelModels::createGavelLayer
        );
        event.registerLayerDefinition(
                HaimaCannonModels.CANNONBALL_LAYER,
                HaimaCannonModels::createCannonballLayer
        );
        event.registerLayerDefinition(
                MagicGlintbladeModels.GLINTBLADE_LAYER,
                MagicGlintbladeModels::createGlintbladeLayer
        );
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GLINTSTONE_PEBBLE.get(), GlintstoneProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.SWIFT_GLINTSTONE_SHARD.get(), GlintstoneProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.GLINTSTONE_ARC.get(), GlintstoneArcRenderer::new);
        event.registerEntityRenderer(ModEntities.CRYSTAL_BARRAGE_SHARD.get(), GlintstoneProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.CRYSTAL_BURST_SHARD.get(), GlintstoneProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.GREAT_GLINTSTONE_SHARD.get(), GlintstoneProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.GLINTSTONE_COMET.get(), GlintstoneProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.GLINTSTONE_STAR.get(), GlintstoneProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.STAR_SHOWER.get(), GlintstoneProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.STARS_OF_RUIN.get(), GlintstoneProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.GLINTSTONE_STAR_VOLLEY.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.FOUNDING_RAIN_OF_STARS.get(), FoundingRainNebulaRenderer::new);
        event.registerEntityRenderer(ModEntities.FOUNDING_RAIN_DROP.get(), FoundingRainDropRenderer::new);
        event.registerEntityRenderer(ModEntities.COMET.get(), GlintstoneProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.SPIRAL_SHARD.get(), SpiralShardRenderer::new);
        event.registerEntityRenderer(ModEntities.STARLIGHT.get(), StarlightRenderer::new);
        event.registerEntityRenderer(ModEntities.TERRA_MAGICA_ZONE.get(), TerraMagicaZoneRenderer::new);
        event.registerEntityRenderer(ModEntities.COMET_AZUR_JET.get(), CometAzurJetRenderer::new);
        event.registerEntityRenderer(ModEntities.GAVEL_OF_HAIMA.get(), HaimaGavelRenderer::new);
        event.registerEntityRenderer(ModEntities.CARIAN_SLICER.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.CARIAN_GREATSWORD.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.CARIAN_PIERCER.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.CANNON_OF_HAIMA.get(), HaimaCannonRenderer::new);
        event.registerEntityRenderer(ModEntities.MAGIC_GLINTBLADE.get(), MagicGlintbladeRenderer::new);
        event.registerEntityRenderer(ModEntities.PHALANX_GLINTBLADE.get(), MagicGlintbladeRenderer::new);
    }

    /**
     * 宽/细手臂都挂迅剑层、大剑层和贯刺层。三层都继承 {@code PlayerItemInHandLayer}，
     * PlayerAnimator 第一人称 pass 才不会把它滤掉。握点各自独立。
     */
    public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skinModel : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(skinModel);
            if (renderer instanceof PlayerRenderer playerRenderer) {
                playerRenderer.addLayer(new CarianSlicerHandLayer(
                        playerRenderer,
                        event.getContext().getItemInHandRenderer()
                ));
                playerRenderer.addLayer(new CarianGreatswordHandLayer(
                        playerRenderer,
                        event.getContext().getItemInHandRenderer()
                ));
                playerRenderer.addLayer(new CarianPiercerHandLayer(
                        playerRenderer,
                        event.getContext().getItemInHandRenderer()
                ));
            }
        }
    }
}

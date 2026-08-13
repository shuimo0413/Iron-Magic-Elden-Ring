package com.eldenring.spells;

import com.eldenring.spells.client.render.glintstone.GlintstoneCometModels;
import com.eldenring.spells.client.render.glintstone.GlintstoneProjectileRenderer;
import com.eldenring.spells.client.render.glintstone.SpiralShardRenderer;
import com.eldenring.spells.particle.glintstone.GlintstoneFlareParticle;
import com.eldenring.spells.particle.glintstone.GlintstoneGlowParticle;
import com.eldenring.spells.particle.glintstone.GlintstoneMistParticle;
import com.eldenring.spells.particle.glintstone.GlintstoneMoteParticle;
import com.eldenring.spells.particle.glintstone.GlintstoneShardParticle;
import com.eldenring.spells.particle.glintstone.GlintstoneSparkParticle;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@Mod(value = EldenRingSpellsMod.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID, value = Dist.CLIENT)
public class EldenRingSpellsClient {
    public EldenRingSpellsClient() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        EldenRingSpellsMod.LOGGER.info(
                "Elden Ring Spells client ready. Player={}",
                Minecraft.getInstance().getUser().getName()
        );
    }

    @SubscribeEvent
    static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.GLINTSTONE_SPARK.get(), GlintstoneSparkParticle.Provider::new);
        event.registerSpriteSet(ModParticles.GLINTSTONE_GLOW.get(), GlintstoneGlowParticle.Provider::new);
        event.registerSpriteSet(ModParticles.GLINTSTONE_SHARD.get(), GlintstoneShardParticle.Provider::new);
        event.registerSpriteSet(ModParticles.GLINTSTONE_MOTE.get(), GlintstoneMoteParticle.Provider::new);
        event.registerSpriteSet(ModParticles.GLINTSTONE_FLARE.get(), GlintstoneFlareParticle.Provider::new);
        event.registerSpriteSet(ModParticles.GLINTSTONE_MIST.get(), GlintstoneMistParticle.Provider::new);
    }

    @SubscribeEvent
    static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
                GlintstoneCometModels.COMET_HEAD_LAYER,
                GlintstoneCometModels::createCometHeadLayer
        );
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GLINTSTONE_PEBBLE.get(), GlintstoneProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.SWIFT_GLINTSTONE_SHARD.get(), GlintstoneProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.GREAT_GLINTSTONE_SHARD.get(), GlintstoneProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.GLINTSTONE_COMET.get(), GlintstoneProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.GLINTSTONE_STAR.get(), GlintstoneProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.STAR_SHOWER.get(), GlintstoneProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.STARS_OF_RUIN.get(), GlintstoneProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.GLINTSTONE_STAR_VOLLEY.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.COMET.get(), GlintstoneProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.SPIRAL_SHARD.get(), SpiralShardRenderer::new);
    }
}

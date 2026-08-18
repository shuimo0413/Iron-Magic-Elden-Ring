package com.eldenring.spells;

import com.eldenring.spells.client.render.CometAzurJetRenderer;
import com.eldenring.spells.client.render.FoundingRainDropRenderer;
import com.eldenring.spells.client.render.FoundingRainNebulaRenderer;
import com.eldenring.spells.client.render.TerraMagicaZoneRenderer;
import com.eldenring.spells.client.render.glintstone.GlintstoneCometModels;
import com.eldenring.spells.client.render.glintstone.GlintstoneProjectileRenderer;
import com.eldenring.spells.client.render.glintstone.SpiralShardRenderer;
import com.eldenring.spells.particle.cometazur.CometAzurInboundParticle;
import com.eldenring.spells.particle.cometazur.CometAzurJetEmitterParticle;
import com.eldenring.spells.particle.cometazur.CometAzurShockwaveAccentParticle;
import com.eldenring.spells.particle.cometazur.CometAzurShockwaveDiscParticle;
import com.eldenring.spells.particle.cometazur.CometAzurVortexParticle;
import com.eldenring.spells.particle.glintstone.AcademyGlintstoneSigilParticle;
import com.eldenring.spells.particle.glintstone.GlintstoneFlareParticle;
import com.eldenring.spells.particle.glintstone.GlintstoneGlowParticle;
import com.eldenring.spells.particle.glintstone.GlintstoneMistParticle;
import com.eldenring.spells.particle.glintstone.GlintstoneMoteParticle;
import com.eldenring.spells.particle.glintstone.GlintstoneShardParticle;
import com.eldenring.spells.particle.glintstone.GlintstoneSparkParticle;
import com.eldenring.spells.particle.foundingrain.NebulaCloudParticle;
import com.eldenring.spells.particle.foundingrain.OverheadNebulaAccentParticle;
import com.eldenring.spells.particle.foundingrain.StarAscentParticle;
import com.eldenring.spells.particle.foundingrain.StarRiverRippleParticle;
import com.eldenring.spells.particle.starriver.StarRiverParticle;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModParticles;
import com.eldenring.spells.registry.ModSpells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
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
        event.registerSpriteSet(ModParticles.ACADEMY_GLINTSTONE_SIGIL.get(), AcademyGlintstoneSigilParticle.Provider::new);
        event.registerSpriteSet(ModParticles.STAR_RIVER_MIST.get(), sprites -> new StarRiverParticle.Provider(sprites, StarRiverParticle.Kind.MIST));
        event.registerSpriteSet(ModParticles.STAR_RIVER_GLOW.get(), sprites -> new StarRiverParticle.Provider(sprites, StarRiverParticle.Kind.GLOW));
        event.registerSpriteSet(ModParticles.VOID_MOTE.get(), sprites -> new StarRiverParticle.Provider(sprites, StarRiverParticle.Kind.MOTE));
        event.registerSpriteSet(ModParticles.VOID_CORE.get(), sprites -> new StarRiverParticle.Provider(sprites, StarRiverParticle.Kind.CORE));
        event.registerSpriteSet(ModParticles.NEBULA_SPIRAL.get(), sprites -> new StarRiverParticle.Provider(sprites, StarRiverParticle.Kind.SPIRAL));
        event.registerSpriteSet(ModParticles.VIOLET_SHARD.get(), sprites -> new StarRiverParticle.Provider(sprites, StarRiverParticle.Kind.SHARD));
        event.registerSpriteSet(ModParticles.ABYSS_FLARE.get(), sprites -> new StarRiverParticle.Provider(sprites, StarRiverParticle.Kind.FLARE));
        event.registerSpriteSet(ModParticles.STAR_STREAK.get(), sprites -> new StarRiverParticle.Provider(sprites, StarRiverParticle.Kind.STREAK));
        event.registerSpriteSet(ModParticles.STAR_CLUSTER.get(), sprites -> new StarRiverParticle.Provider(sprites, StarRiverParticle.Kind.CLUSTER));
        event.registerSpriteSet(ModParticles.ECLIPSE_RING.get(), sprites -> new StarRiverParticle.Provider(sprites, StarRiverParticle.Kind.RING));
        event.registerSpriteSet(ModParticles.TWIN_DUST.get(), sprites -> new StarRiverParticle.Provider(sprites, StarRiverParticle.Kind.DUST));
        event.registerSpriteSet(ModParticles.NOVA_STAR.get(), sprites -> new StarRiverParticle.Provider(sprites, StarRiverParticle.Kind.NOVA));
        event.registerSpriteSet(ModParticles.DARK_FILAMENT.get(), sprites -> new StarRiverParticle.Provider(sprites, StarRiverParticle.Kind.FILAMENT));
        event.registerSpriteSet(ModParticles.PULSE_RING.get(), sprites -> new StarRiverParticle.Provider(sprites, StarRiverParticle.Kind.PULSE));
        event.registerSpriteSet(ModParticles.CRESCENT_WAKE.get(), sprites -> new StarRiverParticle.Provider(sprites, StarRiverParticle.Kind.CRESCENT));
        event.registerSpriteSet(ModParticles.BINARY_STAR.get(), sprites -> new StarRiverParticle.Provider(sprites, StarRiverParticle.Kind.BINARY));
        event.registerSpriteSet(ModParticles.STAR_ASCENT_MOTE.get(), StarAscentParticle.Provider::new);
        event.registerSpriteSet(ModParticles.STAR_ASCENT_TRAIL.get(), sprites -> new StarRiverParticle.Provider(sprites, StarRiverParticle.Kind.ASCENT_TRAIL));
        event.registerSpriteSet(ModParticles.NEBULA_CLOUD_PUFF.get(), sprites -> new NebulaCloudParticle.Provider(sprites, NebulaCloudParticle.Kind.PUFF));
        event.registerSpriteSet(ModParticles.NEBULA_CLOUD_HAZE.get(), sprites -> new NebulaCloudParticle.Provider(sprites, NebulaCloudParticle.Kind.HAZE));
        event.registerSpriteSet(ModParticles.NEBULA_CLOUD_CUMULUS.get(), sprites -> new NebulaCloudParticle.Provider(sprites, NebulaCloudParticle.Kind.CUMULUS));
        event.registerSpriteSet(ModParticles.NEBULA_CLOUD_CIRRUS.get(), sprites -> new NebulaCloudParticle.Provider(sprites, NebulaCloudParticle.Kind.CIRRUS));
        event.registerSpriteSet(ModParticles.NEBULA_CLOUD_CORE.get(), sprites -> new NebulaCloudParticle.Provider(sprites, NebulaCloudParticle.Kind.CORE));
        event.registerSpriteSet(ModParticles.NEBULA_CLOUD_STARDUST.get(), sprites -> new NebulaCloudParticle.Provider(sprites, NebulaCloudParticle.Kind.STARDUST));
        event.registerSpriteSet(ModParticles.NEBULA_CLOUD_RIFT.get(), sprites -> new NebulaCloudParticle.Provider(sprites, NebulaCloudParticle.Kind.RIFT));
        event.registerSpriteSet(ModParticles.NEBULA_CLOUD_WISP.get(), sprites -> new NebulaCloudParticle.Provider(sprites, NebulaCloudParticle.Kind.WISP));
        event.registerSpriteSet(ModParticles.NEBULA_CLOUD_STRATUS.get(), sprites -> new NebulaCloudParticle.Provider(sprites, NebulaCloudParticle.Kind.STRATUS));
        event.registerSpriteSet(ModParticles.NEBULA_CLOUD_TWIN.get(), sprites -> new NebulaCloudParticle.Provider(sprites, NebulaCloudParticle.Kind.TWIN));
        event.registerSpriteSet(ModParticles.NEBULA_VEIL.get(), sprites -> new NebulaCloudParticle.Provider(sprites, NebulaCloudParticle.Kind.VEIL));
        event.registerSpriteSet(ModParticles.NEBULA_BLOOM.get(), sprites -> new NebulaCloudParticle.Provider(sprites, NebulaCloudParticle.Kind.BLOOM));
        event.registerSpriteSet(ModParticles.NEBULA_VEIL_WISP.get(), sprites -> new NebulaCloudParticle.Provider(sprites, NebulaCloudParticle.Kind.VEIL_WISP));
        event.registerSpriteSet(ModParticles.OVERHEAD_NEBULA_ACCENT.get(), OverheadNebulaAccentParticle.Provider::new);
        event.registerSpriteSet(ModParticles.STAR_RIVER_RIPPLE.get(), StarRiverRippleParticle.Provider::new);
        event.registerSpriteSet(ModParticles.STAR_RIVER_SPRAY.get(), sprites -> new StarRiverParticle.Provider(sprites, StarRiverParticle.Kind.SPRAY));
        event.registerSpriteSet(ModParticles.COMET_AZUR_SHRINK.get(), CometAzurVortexParticle.Provider::new);
        event.registerSpriteSet(
                ModParticles.COMET_AZUR_MOTE.get(),
                sprites -> new CometAzurInboundParticle.Provider(sprites, CometAzurInboundParticle.Kind.MOTE)
        );
        event.registerSpriteSet(
                ModParticles.COMET_AZUR_IMPACT.get(),
                sprites -> new CometAzurInboundParticle.Provider(sprites, CometAzurInboundParticle.Kind.IMPACT)
        );
        event.registerSpriteSet(
                ModParticles.COMET_AZUR_HEAD.get(),
                sprites -> new CometAzurInboundParticle.Provider(sprites, CometAzurInboundParticle.Kind.HEAD)
        );
        event.registerSpriteSet(
                ModParticles.COMET_AZUR_DUST.get(),
                sprites -> new CometAzurInboundParticle.Provider(sprites, CometAzurInboundParticle.Kind.DUST)
        );
        event.registerSpriteSet(ModParticles.COMET_AZUR_SHOCKWAVE_RING.get(), CometAzurShockwaveDiscParticle.Provider::new);
        event.registerSpriteSet(ModParticles.COMET_AZUR_SHOCKWAVE_ACCENT.get(), CometAzurShockwaveAccentParticle.Provider::new);
        event.registerSpriteSet(ModParticles.COMET_AZUR_JET_SURROUND.get(), CometAzurJetEmitterParticle.Provider::new);
    }

    /**
     * 铁魔法 {@code ScrollModel} 用 standalone 模型查卷轴外观。
     * 把每道辉石咒的 {@code item/<spell>_scroll} 登记进去，通用卷轴才能显示 Wiki 图标。
     */
    @SubscribeEvent
    static void registerScrollModels(ModelEvent.RegisterAdditional event) {
        for (var spellHolder : ModSpells.SPELLS.getEntries()) {
            event.register(ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            EldenRingSpellsMod.MOD_ID,
                            "item/" + spellHolder.getId().getPath() + "_scroll"
                    )
            ));
        }
    }

    @SubscribeEvent
    static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
                GlintstoneCometModels.COMET_HEAD_LAYER,
                GlintstoneCometModels::createCometHeadLayer
        );
        event.registerLayerDefinition(
                GlintstoneCometModels.SPIKED_COMET_HEAD_LAYER,
                GlintstoneCometModels::createSpikedCometHeadLayer
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
        event.registerEntityRenderer(ModEntities.FOUNDING_RAIN_OF_STARS.get(), FoundingRainNebulaRenderer::new);
        event.registerEntityRenderer(ModEntities.FOUNDING_RAIN_DROP.get(), FoundingRainDropRenderer::new);
        event.registerEntityRenderer(ModEntities.COMET.get(), GlintstoneProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.SPIRAL_SHARD.get(), SpiralShardRenderer::new);
        event.registerEntityRenderer(ModEntities.TERRA_MAGICA_ZONE.get(), TerraMagicaZoneRenderer::new);
        event.registerEntityRenderer(ModEntities.COMET_AZUR_JET.get(), CometAzurJetRenderer::new);
    }
}

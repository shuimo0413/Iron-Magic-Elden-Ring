package com.eldenring.spells.client;

import com.eldenring.spells.particle.carian.CarianParticle;
import com.eldenring.spells.particle.carian.CarianSlashParticle;
import com.eldenring.spells.particle.cometazur.CometAzurInboundParticle;
import com.eldenring.spells.particle.cometazur.CometAzurJetEmitterParticle;
import com.eldenring.spells.particle.cometazur.CometAzurShockwaveAccentParticle;
import com.eldenring.spells.particle.cometazur.CometAzurShockwaveDiscParticle;
import com.eldenring.spells.particle.cometazur.CometAzurVortexParticle;
import com.eldenring.spells.particle.foundingrain.NebulaCloudParticle;
import com.eldenring.spells.particle.foundingrain.OverheadNebulaAccentParticle;
import com.eldenring.spells.particle.foundingrain.StarAscentParticle;
import com.eldenring.spells.particle.foundingrain.StarRiverRippleParticle;
import com.eldenring.spells.particle.glintstone.GlintstoneFlareParticle;
import com.eldenring.spells.particle.glintstone.GlintstoneGlowParticle;
import com.eldenring.spells.particle.glintstone.GlintstoneMistParticle;
import com.eldenring.spells.particle.glintstone.GlintstoneMoteParticle;
import com.eldenring.spells.particle.glintstone.GlintstoneShardParticle;
import com.eldenring.spells.particle.glintstone.GlintstoneSparkParticle;
import com.eldenring.spells.particle.starriver.StarRiverParticle;
import com.eldenring.spells.registry.ModParticles;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * 客户端粒子 Provider 注册。由 {@code EldenRingSpellsClient} 转发。
 */
public final class ClientParticleProviders {

    private ClientParticleProviders() {
    }

    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.GLINTSTONE_SPARK.get(), GlintstoneSparkParticle.Provider::new);
        event.registerSpriteSet(ModParticles.GLINTSTONE_GLOW.get(), GlintstoneGlowParticle.Provider::new);
        event.registerSpriteSet(ModParticles.GLINTSTONE_SHARD.get(), GlintstoneShardParticle.Provider::new);
        event.registerSpriteSet(ModParticles.GLINTSTONE_MOTE.get(), GlintstoneMoteParticle.Provider::new);
        event.registerSpriteSet(ModParticles.GLINTSTONE_FLARE.get(), GlintstoneFlareParticle.Provider::new);
        event.registerSpriteSet(ModParticles.GLINTSTONE_MIST.get(), GlintstoneMistParticle.Provider::new);
        event.registerSpriteSet(ModParticles.CARIAN_SLASH.get(), CarianSlashParticle.Provider::new);
        event.registerSpriteSet(ModParticles.CARIAN_SPARK.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.SPARK));
        event.registerSpriteSet(ModParticles.CARIAN_GLOW.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.GLOW));
        event.registerSpriteSet(ModParticles.CARIAN_SHARD.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.SHARD));
        event.registerSpriteSet(ModParticles.CARIAN_MOTE.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.MOTE));
        event.registerSpriteSet(ModParticles.CARIAN_FLARE.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.FLARE));
        event.registerSpriteSet(ModParticles.CARIAN_MIST.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.MIST));
        event.registerSpriteSet(ModParticles.CARIAN_CRESCENT.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.CRESCENT));
        event.registerSpriteSet(ModParticles.CARIAN_ARC.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.ARC));
        event.registerSpriteSet(ModParticles.CARIAN_NEEDLE.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.NEEDLE));
        event.registerSpriteSet(ModParticles.CARIAN_DUST.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.DUST));
        event.registerSpriteSet(ModParticles.CARIAN_FILAMENT.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.FILAMENT));
        event.registerSpriteSet(ModParticles.CARIAN_RING.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.RING));
        event.registerSpriteSet(ModParticles.CARIAN_CORE.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.CORE));
        event.registerSpriteSet(ModParticles.CARIAN_SPIRAL.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.SPIRAL));
        event.registerSpriteSet(ModParticles.CARIAN_STREAK.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.STREAK));
        event.registerSpriteSet(ModParticles.CARIAN_CLUSTER.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.CLUSTER));
        event.registerSpriteSet(ModParticles.CARIAN_NOVA.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.NOVA));
        event.registerSpriteSet(ModParticles.CARIAN_HALO.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.HALO));
        event.registerSpriteSet(ModParticles.CARIAN_CROSS.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.CROSS));
        event.registerSpriteSet(ModParticles.CARIAN_WISP.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.WISP));
        event.registerSpriteSet(ModParticles.CARIAN_VEIL.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.VEIL));
        event.registerSpriteSet(ModParticles.CARIAN_BLOOM.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.BLOOM));
        event.registerSpriteSet(ModParticles.CARIAN_RIPPLE.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.RIPPLE));
        event.registerSpriteSet(ModParticles.CARIAN_SIGIL.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.SIGIL));
        event.registerSpriteSet(ModParticles.CARIAN_GLINT.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.GLINT));
        event.registerSpriteSet(ModParticles.CARIAN_CRYSTAL.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.CRYSTAL));
        event.registerSpriteSet(ModParticles.CARIAN_LENS.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.LENS));
        event.registerSpriteSet(ModParticles.CARIAN_VORTEX.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.VORTEX));
        event.registerSpriteSet(ModParticles.CARIAN_COMET.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.COMET));
        event.registerSpriteSet(ModParticles.CARIAN_TWIN.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.TWIN));
        event.registerSpriteSet(ModParticles.CARIAN_ECLIPSE.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.ECLIPSE));
        event.registerSpriteSet(ModParticles.CARIAN_BINARY.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.BINARY));
        event.registerSpriteSet(ModParticles.CARIAN_SPRAY.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.SPRAY));
        event.registerSpriteSet(ModParticles.CARIAN_WAKE.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.WAKE));
        event.registerSpriteSet(ModParticles.CARIAN_THORN.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.THORN));
        event.registerSpriteSet(ModParticles.CARIAN_RUNE.get(), sprites -> new CarianParticle.Provider(sprites, CarianParticle.Kind.RUNE));
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
}

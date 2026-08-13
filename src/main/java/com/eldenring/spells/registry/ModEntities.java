package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.CometProjectile;
import com.eldenring.spells.entity.GlintstoneCometProjectile;
import com.eldenring.spells.entity.GlintstonePebbleProjectile;
import com.eldenring.spells.entity.GlintstoneStarProjectile;
import com.eldenring.spells.entity.GlintstoneStarVolleyEntity;
import com.eldenring.spells.entity.GreatGlintstoneShardProjectile;
import com.eldenring.spells.entity.SpiralShardProjectile;
import com.eldenring.spells.entity.StarShowerProjectile;
import com.eldenring.spells.entity.StarsOfRuinProjectile;
import com.eldenring.spells.entity.SwiftGlintstoneShardProjectile;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, EldenRingSpellsMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<GlintstonePebbleProjectile>> GLINTSTONE_PEBBLE =
            ENTITIES.register("glintstone_pebble", () ->
                    EntityType.Builder.<GlintstonePebbleProjectile>of(GlintstonePebbleProjectile::new, MobCategory.MISC)
                            .sized(0.4f, 0.4f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(id("glintstone_pebble"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<SwiftGlintstoneShardProjectile>> SWIFT_GLINTSTONE_SHARD =
            ENTITIES.register("swift_glintstone_shard", () ->
                    EntityType.Builder.<SwiftGlintstoneShardProjectile>of(SwiftGlintstoneShardProjectile::new, MobCategory.MISC)
                            .sized(0.32f, 0.32f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(id("swift_glintstone_shard"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<GreatGlintstoneShardProjectile>> GREAT_GLINTSTONE_SHARD =
            ENTITIES.register("great_glintstone_shard", () ->
                    EntityType.Builder.<GreatGlintstoneShardProjectile>of(GreatGlintstoneShardProjectile::new, MobCategory.MISC)
                            .sized(0.85f, 0.85f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(id("great_glintstone_shard"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<GlintstoneCometProjectile>> GLINTSTONE_COMET =
            ENTITIES.register("glintstone_comet", () ->
                    EntityType.Builder.<GlintstoneCometProjectile>of(GlintstoneCometProjectile::new, MobCategory.MISC)
                            .sized(0.95f, 0.95f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(id("glintstone_comet"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<GlintstoneStarProjectile>> GLINTSTONE_STAR =
            ENTITIES.register("glintstone_star", () ->
                    EntityType.Builder.<GlintstoneStarProjectile>of(GlintstoneStarProjectile::new, MobCategory.MISC)
                            .sized(0.34f, 0.34f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(id("glintstone_star"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<StarShowerProjectile>> STAR_SHOWER =
            ENTITIES.register("star_shower", () ->
                    EntityType.Builder.<StarShowerProjectile>of(StarShowerProjectile::new, MobCategory.MISC)
                            .sized(0.32f, 0.32f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(id("star_shower"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<StarsOfRuinProjectile>> STARS_OF_RUIN =
            ENTITIES.register("stars_of_ruin", () ->
                    EntityType.Builder.<StarsOfRuinProjectile>of(StarsOfRuinProjectile::new, MobCategory.MISC)
                            .sized(0.36f, 0.36f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(id("stars_of_ruin"))
            );

    /**
     * 辉石连发控制器：不可见，只按 tick 依次生成流星，避免 TickTask 把延迟发挤进同一帧。
     */
    public static final DeferredHolder<EntityType<?>, EntityType<GlintstoneStarVolleyEntity>> GLINTSTONE_STAR_VOLLEY =
            ENTITIES.register("glintstone_star_volley", () ->
                    EntityType.Builder.<GlintstoneStarVolleyEntity>of(GlintstoneStarVolleyEntity::new, MobCategory.MISC)
                            .sized(0.1f, 0.1f)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build(id("glintstone_star_volley"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<CometProjectile>> COMET =
            ENTITIES.register("comet", () ->
                    EntityType.Builder.<CometProjectile>of(CometProjectile::new, MobCategory.MISC)
                            .sized(1.1f, 1.1f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(id("comet"))
            );

    /**
     * 旋飞魔砾：实体为双螺旋中心轴；两颗彗星在渲染/命中时按欧拉相位展开。
     * 碰撞箱略大于半径，便于客户端追踪与调试。
     */
    public static final DeferredHolder<EntityType<?>, EntityType<SpiralShardProjectile>> SPIRAL_SHARD =
            ENTITIES.register("spiral_shard", () ->
                    EntityType.Builder.<SpiralShardProjectile>of(SpiralShardProjectile::new, MobCategory.MISC)
                            .sized(0.55f, 0.55f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(id("spiral_shard"))
            );

    private ModEntities() {
    }

    private static String id(String path) {
        return ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, path).toString();
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }
}

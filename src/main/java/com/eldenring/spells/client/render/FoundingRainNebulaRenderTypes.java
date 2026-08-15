package com.eldenring.spells.client.render;

import com.eldenring.spells.EldenRingSpellsMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * 创星雨星云面片：独立贴图 + 最近邻（像素化）+ 不写深度。
 * <p>
 * 关掉 blur，32×32 贴图放大后才是 MC 颗粒，而不是油画渐变。
 */
public final class FoundingRainNebulaRenderTypes {

    public static final ResourceLocation SOFT_BLOB_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EldenRingSpellsMod.MOD_ID,
            "textures/entity/founding_rain/nebula_soft.png"
    );

    public static final ResourceLocation FILAMENT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EldenRingSpellsMod.MOD_ID,
            "textures/entity/founding_rain/nebula_filament.png"
    );

    /** 深紫/深蓝气团：半透明，能把白天天空染暗。 */
    public static final RenderType BODY = create(
            "elden_ring_spells_nebula_body",
            SOFT_BLOB_TEXTURE,
            RenderStateShard.TRANSLUCENT_TRANSPARENCY
    );

    /** 亮紫/青丝：加法，叠在气团上发亮。 */
    public static final RenderType FILAMENT = create(
            "elden_ring_spells_nebula_filament",
            FILAMENT_TEXTURE,
            RenderStateShard.LIGHTNING_TRANSPARENCY
    );

    private FoundingRainNebulaRenderTypes() {
    }

    private static RenderType create(
            String name,
            ResourceLocation texture,
            RenderStateShard.TransparencyStateShard transparency
    ) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(transparency)
                .setCullState(RenderStateShard.NO_CULL)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .createCompositeState(false);
        return RenderType.create(
                name,
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                state
        );
    }
}

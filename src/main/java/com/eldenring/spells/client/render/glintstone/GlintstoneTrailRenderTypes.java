package com.eldenring.spells.client.render.glintstone;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * 辉石弹道光轨 RenderType。
 * <p>
 * 外层半透明保证白天天空上仍有体积；内层加法只加亮、不把尾迹染成塑料片。
 */
public final class GlintstoneTrailRenderTypes {

    /** 外层 / 细丝：标准自发光半透明。 */
    public static final RenderType TRANSLUCENT = create(
            "elden_ring_spells_glintstone_trail",
            GlintstoneCometModels.TRAIL_BEAM_TEXTURE,
            RenderStateShard.TRANSLUCENT_TRANSPARENCY
    );

    /** 内层光芯：加法，彗星 / 帚星用来做出亮核而不堆粒子。 */
    public static final RenderType ADDITIVE_CORE = create(
            "elden_ring_spells_glintstone_trail_core",
            GlintstoneCometModels.TRAIL_BEAM_TEXTURE,
            RenderStateShard.LIGHTNING_TRANSPARENCY
    );

    private GlintstoneTrailRenderTypes() {
    }

    private static RenderType create(
            String name,
            net.minecraft.resources.ResourceLocation texture,
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
                24576,
                false,
                true,
                state
        );
    }
}

package com.eldenring.spells.client.render;

import com.eldenring.spells.client.render.glintstone.GlintstoneCometModels;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * 彗星亚兹勒喷流网格：圆柱管壁 + 口部圆球。
 * <p>
 * 不用辉石拖尾那种朝相机的扁带，否则侧面一定是十字/薄片。
 * 不写深度，半透明叠层才不会把后面的细丝裁掉。
 */
public final class CometAzurJetRenderTypes {

    /** 管壁 / 圆球实体：标准半透明，能在白天天空上看出体积。 */
    public static final RenderType CYLINDER = create(
            "elden_ring_spells_comet_azur_cylinder",
            GlintstoneCometModels.TRAIL_BEAM_TEXTURE,
            RenderStateShard.TRANSLUCENT_TRANSPARENCY
    );

    /** 口部圆球光晕：加法，叠在圆球上发亮。 */
    public static final RenderType ORIGIN_GLOW = create(
            "elden_ring_spells_comet_azur_origin_glow",
            GlintstoneCometModels.COMET_GLOW_TEXTURE,
            RenderStateShard.LIGHTNING_TRANSPARENCY
    );

    /** 亮芯管：加法，圆柱中心一条更亮的细管。 */
    public static final RenderType CORE = create(
            "elden_ring_spells_comet_azur_core",
            GlintstoneCometModels.TRAIL_BEAM_TEXTURE,
            RenderStateShard.LIGHTNING_TRANSPARENCY
    );

    private CometAzurJetRenderTypes() {
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

package com.eldenring.spells.client.render;

import com.eldenring.spells.client.render.glintstone.GlintstoneCometModels;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * 彗星亚兹勒喷流网格：圆柱管壁 + 口部圆球。
 * <p>
 * 不用辉石拖尾那种朝相机的扁带，否则侧面一定是十字/薄片。
 * 不写深度，半透明叠层才不会把后面的细丝裁掉。
 */
public final class CometAzurJetRenderTypes {

    /**
     * 1.20.1 的 {@code TRANSLUCENT_TRANSPARENCY} 等是 {@link RenderStateShard} 的 protected 常量，
     * 跨包无法引用。这里按原版定义重建同语义实例：
     * 设置阶段开混合并 {@code blendFuncSeparate(源 alpha, 1-源 alpha, 1, 1-源 alpha)}，
     * 清除阶段关混合并恢复默认混合函数。混合语义与 1.21.1 分支完全一致。
     */
    private static final RenderStateShard.TransparencyStateShard TRANSLUCENT_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard(
                    "iss_elden_ring_comet_azur_translucent_transparency",
                    () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFuncSeparate(
                                GlStateManager.SourceFactor.SRC_ALPHA,
                                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                                GlStateManager.SourceFactor.ONE,
                                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
                        );
                    },
                    () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }
            );

    /** 加法混合：源颜色 × 源 alpha + 目标颜色 × 1，用于口部光晕与亮芯管。 */
    private static final RenderStateShard.TransparencyStateShard LIGHTNING_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard(
                    "iss_elden_ring_comet_azur_lightning_transparency",
                    () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(
                                GlStateManager.SourceFactor.SRC_ALPHA,
                                GlStateManager.DestFactor.ONE
                        );
                    },
                    () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }
            );

    /** 只写颜色、不写深度：半透明叠层才不会把后面的细丝裁掉。 */
    private static final RenderStateShard.WriteMaskStateShard COLOR_WRITE =
            new RenderStateShard.WriteMaskStateShard(true, false);

    /** 关背面剔除：管壁与圆球从内侧看也要可见。 */
    private static final RenderStateShard.CullStateShard NO_CULL =
            new RenderStateShard.CullStateShard(false);

    /** 采样 lightmap（顶点光写 FULL_BRIGHT）。 */
    private static final RenderStateShard.LightmapStateShard LIGHTMAP =
            new RenderStateShard.LightmapStateShard(true);

    /** 采样 overlay（顶点写 NO_OVERLAY）。 */
    private static final RenderStateShard.OverlayStateShard OVERLAY =
            new RenderStateShard.OverlayStateShard(true);

    /** 自发光实体着色器，等效 1.21.1 的 {@code RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER}。 */
    private static final RenderStateShard.ShaderStateShard ENTITY_TRANSLUCENT_EMISSIVE_SHADER =
            new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityTranslucentEmissiveShader);

    /** 管壁 / 圆球实体：标准半透明，能在白天天空上看出体积。 */
    public static final RenderType CYLINDER = create(
            "iss_elden_ring_comet_azur_cylinder",
            GlintstoneCometModels.TRAIL_BEAM_TEXTURE,
            TRANSLUCENT_TRANSPARENCY
    );

    /** 口部圆球光晕：加法，叠在圆球上发亮。 */
    public static final RenderType ORIGIN_GLOW = create(
            "iss_elden_ring_comet_azur_origin_glow",
            GlintstoneCometModels.COMET_GLOW_TEXTURE,
            LIGHTNING_TRANSPARENCY
    );

    /** 亮芯管：加法，圆柱中心一条更亮的细管。 */
    public static final RenderType CORE = create(
            "iss_elden_ring_comet_azur_core",
            GlintstoneCometModels.TRAIL_BEAM_TEXTURE,
            LIGHTNING_TRANSPARENCY
    );

    private CometAzurJetRenderTypes() {
    }

    private static RenderType create(
            String name,
            net.minecraft.resources.ResourceLocation texture,
            RenderStateShard.TransparencyStateShard transparency
    ) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(transparency)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
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

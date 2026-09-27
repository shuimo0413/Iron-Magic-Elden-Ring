package com.eldenring.spells.client.render.glintstone;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * 辉石弹道光轨 RenderType。
 * <p>
 * 外层半透明保证白天天空上仍有体积；内层加法只加亮、不把尾迹染成塑料片。
 */
public final class GlintstoneTrailRenderTypes {

    /**
     * 外层 / 细丝：标准自发光半透明。
     * <p>
     * 1.20.1 的 {@code RenderStateShard.TRANSLUCENT_TRANSPARENCY} 等是 protected 常量，跨包不可引用，
     * 因此这里按原版 {@code static {}} 的定义重建同语义实例：
     * 设置阶段开混合并 {@code blendFuncSeparate(源 alpha, 1-源 alpha, 1, 1-源 alpha)}，
     * 清除阶段关混合并恢复默认混合函数。混合语义与 1.21.1 分支一致。
     */
    private static final RenderStateShard.TransparencyStateShard TRANSLUCENT_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard(
                    "iss_elden_ring_glintstone_trail_translucent_transparency",
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

    /** 内层光芯：加法混合（源颜色 × 源 alpha + 目标颜色 × 1）。 */
    private static final RenderStateShard.TransparencyStateShard LIGHTNING_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard(
                    "iss_elden_ring_glintstone_trail_lightning_transparency",
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

    /**
     * 自发光半透明着色器。
     * <p>
     * 1.21 的 {@code RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER} 在 1.20.1 也是 protected；
     * 原版就是 {@code new ShaderStateShard(GameRenderer::getRendertypeEntityTranslucentEmissiveShader)}，
     * 该 getter 在 GameRenderer 里是 public，可直接建。
     */
    private static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER =
            new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityTranslucentEmissiveShader);

    /** 关背面剔除：光轨从侧面 / 内侧看都要可见。 */
    private static final RenderStateShard.CullStateShard NO_CULL =
            new RenderStateShard.CullStateShard(false);

    /** 采样 lightmap（顶点光写 FULL_BRIGHT）。 */
    private static final RenderStateShard.LightmapStateShard LIGHTMAP =
            new RenderStateShard.LightmapStateShard(true);

    /** 采样 overlay（顶点写 NO_OVERLAY）。 */
    private static final RenderStateShard.OverlayStateShard OVERLAY =
            new RenderStateShard.OverlayStateShard(true);

    /** 只写颜色、不写深度：半透明叠层才不会把后面的光轨裁掉。 */
    private static final RenderStateShard.WriteMaskStateShard COLOR_WRITE =
            new RenderStateShard.WriteMaskStateShard(true, false);

    public static final RenderType TRANSLUCENT = create(
            "iss_elden_ring_glintstone_trail",
            GlintstoneCometModels.TRAIL_BEAM_TEXTURE,
            TRANSLUCENT_TRANSPARENCY
    );

    public static final RenderType ADDITIVE_CORE = create(
            "iss_elden_ring_glintstone_trail_core",
            GlintstoneCometModels.TRAIL_BEAM_TEXTURE,
            LIGHTNING_TRANSPARENCY
    );

    private GlintstoneTrailRenderTypes() {
    }

    private static RenderType create(
            String name,
            net.minecraft.resources.ResourceLocation texture,
            RenderStateShard.TransparencyStateShard transparency
    ) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
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

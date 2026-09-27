package com.eldenring.spells.client.render;

import com.eldenring.spells.EldenRingSpellsMod;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * 创星雨星云面片：独立贴图 + 最近邻（像素化）+ 不写深度。
 * <p>
 * 关掉 blur，32×32 贴图放大后才是 MC 颗粒，而不是油画渐变。
 */
public final class FoundingRainNebulaRenderTypes {

    /**
     * 1.20.1 的 {@code TRANSLUCENT_TRANSPARENCY} 等是 {@link RenderStateShard} 的 protected 常量，
     * 跨包无法引用。这里按原版定义重建同语义实例：
     * 设置阶段开混合并 {@code blendFuncSeparate(源 alpha, 1-源 alpha, 1, 1-源 alpha)}，
     * 清除阶段关混合并恢复默认混合函数。混合语义与 1.21.1 分支完全一致。
     */
    private static final RenderStateShard.TransparencyStateShard TRANSLUCENT_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard(
                    "iss_elden_ring_nebula_translucent_transparency",
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

    /** 加法混合：源颜色 × 源 alpha + 目标颜色 × 1，用于亮紫/青丝叠亮。 */
    private static final RenderStateShard.TransparencyStateShard LIGHTNING_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard(
                    "iss_elden_ring_nebula_lightning_transparency",
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

    /** 只写颜色、不写深度：面片叠层才不会把后面的丝裁掉。 */
    private static final RenderStateShard.WriteMaskStateShard COLOR_WRITE =
            new RenderStateShard.WriteMaskStateShard(true, false);

    /** 关背面剔除：面片正反两面都要能看见。 */
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

    public static final ResourceLocation SOFT_BLOB_TEXTURE = new ResourceLocation(
            EldenRingSpellsMod.MOD_ID,
            "textures/entity/founding_rain/nebula_soft.png"
    );

    public static final ResourceLocation FILAMENT_TEXTURE = new ResourceLocation(
            EldenRingSpellsMod.MOD_ID,
            "textures/entity/founding_rain/nebula_filament.png"
    );

    /** 深紫/深蓝气团：半透明，能把白天天空染暗。 */
    public static final RenderType BODY = create(
            "iss_elden_ring_nebula_body",
            SOFT_BLOB_TEXTURE,
            TRANSLUCENT_TRANSPARENCY
    );

    /** 亮紫/青丝：加法，叠在气团上发亮。 */
    public static final RenderType FILAMENT = create(
            "iss_elden_ring_nebula_filament",
            FILAMENT_TEXTURE,
            LIGHTNING_TRANSPARENCY
    );

    private FoundingRainNebulaRenderTypes() {
    }

    private static RenderType create(
            String name,
            ResourceLocation texture,
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
                1536,
                false,
                true,
                state
        );
    }
}

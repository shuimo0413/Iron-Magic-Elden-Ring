package com.eldenring.spells.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;

/**
 * 创星雨星云用的粒子混合。
 * <p>
 * 气团走半透明（给天空染上深紫/深蓝），闪星走加法（只加白光）。
 * 两种都关掉 depth mask，避免原版半透明写深度后叠成硬卡片。
 * <p>
 * 1.20.1 的 {@link ParticleRenderType} 是「ParticleEngine 传入共享 BufferBuilder / Tesselator」的写法，
 * 1.20.5+ 才改成 {@code begin(Tesselator, TextureManager)} 返回 BufferBuilder，所以这里按 1.20.1 实现。
 */
public final class AdditiveParticleRenderType {

    private AdditiveParticleRenderType() {
    }

    /**
     * 气团：标准 alpha 混合，深紫/深蓝能把白天天空压暗。
     */
    public static final ParticleRenderType SOFT_TRANSLUCENT = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder bufferBuilder, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
            );
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        /**
         * 本 RenderType 是最后一个用共享 Tesselator 的批次，必须在这里 flush，
         * 并把 blend / depthMask 还原，否则后续批次会继承这里的混合状态。
         */
        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }

        @Override
        public String toString() {
            return "iss_elden_ring:soft_translucent";
        }
    };

    /**
     * 白星星：加法，叠在气团上只加亮、不染黑边。
     */
    public static final ParticleRenderType ADDITIVE = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder bufferBuilder, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }

        @Override
        public String toString() {
            return "iss_elden_ring:additive";
        }
    };
}

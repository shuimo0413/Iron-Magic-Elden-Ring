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
import org.jetbrains.annotations.Nullable;

/**
 * 创星雨星云用的粒子混合。
 * <p>
 * 气团走半透明（给天空染上深紫/深蓝），闪星走加法（只加白光）。
 * 两种都关掉 depth mask，避免原版半透明写深度后叠成硬卡片。
 */
public final class AdditiveParticleRenderType {

    private AdditiveParticleRenderType() {
    }

    /**
     * 气团：标准 alpha 混合，深紫/深蓝能把白天天空压暗。
     */
    public static final ParticleRenderType SOFT_TRANSLUCENT = new ParticleRenderType() {
        @Override
        public @Nullable BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
            );
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "elden_ring_spells:soft_translucent";
        }
    };

    /**
     * 白星星：加法，叠在气团上只加亮、不染黑边。
     */
    public static final ParticleRenderType ADDITIVE = new ParticleRenderType() {
        @Override
        public @Nullable BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "elden_ring_spells:additive";
        }
    };
}

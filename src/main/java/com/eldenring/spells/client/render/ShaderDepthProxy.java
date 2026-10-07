package com.eldenring.spells.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.function.Function;

/**
 * 让自发光法术特效在延迟渲染光影下不被天空 / 半透明方块吃掉的通用工具。
 * <p>
 * <b>根因：</b>只写颜色、不写深度的几何体，在延迟渲染管线里深度仍为 1.0。光影在 deferred / composite
 * 阶段会把深度为 1 的像素当成天空重算并覆盖；冰 / 水等半透明方块在实体之后绘制，深度测试照样通过，
 * 也会盖住特效。这是管线层面的通用行为，<b>与具体光影包无关，这里也不做任何光影检测</b>。
 * <p>
 * <b>两种用法：</b>
 * <ul>
 *   <li>{@link #solidEmissive}：实体网格（晶核、炮弹、锤身、剑身）直接改用它，颜色与深度一起写。</li>
 *   <li>{@link #depthOnly}：柔光几何（光晕、光轨、月牙）先照常用只写颜色的类型画，再用本类型把
 *       <b>同一份顶点</b>重提交一次，只写深度。原版画面零变化；光影下这些像素深度 &lt; 1，被当成普通几何体打光。</li>
 * </ul>
 * 深度代理必须在同一几何的颜色层之后提交，否则共面颜色层会被自己的深度挡掉。
 */
public final class ShaderDepthProxy {

    /**
     * 深度代理只在贴图 alpha 不低于此值的像素写深度（0~1）。
     * <p>
     * 低 alpha 的柔边如果也写深度，光影会拿「淡淡一层颜色 + 空 gbuffer」去打光，天空上会出现偏暗的晕。
     * 调高 → 写深度的区域更小更保守；调低 → 光影下保留的光晕更大，但边缘更容易发暗。
     * 现有贴图中心 alpha：辉石光晕 0.71、光轨 0.8、重力核 0.99。
     */
    public static final float PROXY_TEXTURE_ALPHA_CUTOFF = 0.45f;

    /**
     * 代理顶点 alpha（0~255）。原版自发光 shader 丢弃 {@code 贴图alpha × 顶点alpha < 0.1} 的片元，
     * 因此顶点 alpha = 0.1 / {@link #PROXY_TEXTURE_ALPHA_CUTOFF} 时恰好只保留够实的像素。颜色不写，此值不影响画面。
     */
    public static final int PROXY_VERTEX_ALPHA = (int) Math.ceil(0.1f / PROXY_TEXTURE_ALPHA_CUTOFF * 255.0f);

    private static final Function<ResourceLocation, RenderType> SOLID_EMISSIVE_BY_TEXTURE = Util.memoize(
            texture -> create("iss_elden_ring_solid_emissive", texture, RenderStateShard.COLOR_DEPTH_WRITE)
    );

    private static final Function<ResourceLocation, RenderType> DEPTH_ONLY_BY_TEXTURE = Util.memoize(
            texture -> create("iss_elden_ring_depth_proxy", texture, RenderStateShard.DEPTH_WRITE)
    );

    private ShaderDepthProxy() {
    }

    /**
     * 与原版 {@code entity_translucent_emissive} 观感相同（同一 shader，光影映射到同一自发光 gbuffer），但写深度。
     * 给实心网格用。
     */
    public static RenderType solidEmissive(ResourceLocation texture) {
        return SOLID_EMISSIVE_BY_TEXTURE.apply(texture);
    }

    /**
     * 只写深度的代理类型。顶点颜色请用 {@link #proxyColor}，让 alpha 丢弃只留下贴图够实的像素。
     */
    public static RenderType depthOnly(ResourceLocation texture) {
        return DEPTH_ONLY_BY_TEXTURE.apply(texture);
    }

    /** 把任意 ARGB 的 alpha 换成 {@link #PROXY_VERTEX_ALPHA}，RGB 保留（不写颜色，仅为可读）。 */
    public static int proxyColor(int colorArgb) {
        return (PROXY_VERTEX_ALPHA << 24) | (colorArgb & 0x00FFFFFF);
    }

    /**
     * 提交一张朝向当前 PoseStack XY 平面的深度代理方片，UV 覆盖整张贴图，和常见光晕方片一致。
     * 径向对称的光晕贴图不在乎 V 是否翻转。
     *
     * @param halfExtent 方片半边长（当前 PoseStack 局部单位）
     */
    public static void putBillboard(VertexConsumer depthConsumer, Matrix4f poseMatrix, float halfExtent) {
        int color = proxyColor(0xFFFFFF);
        putVertex(depthConsumer, poseMatrix, -halfExtent, -halfExtent, 0.0f, 1.0f, color);
        putVertex(depthConsumer, poseMatrix, halfExtent, -halfExtent, 1.0f, 1.0f, color);
        putVertex(depthConsumer, poseMatrix, halfExtent, halfExtent, 1.0f, 0.0f, color);
        putVertex(depthConsumer, poseMatrix, -halfExtent, halfExtent, 0.0f, 0.0f, color);
    }

    private static void putVertex(
            VertexConsumer consumer,
            Matrix4f poseMatrix,
            float x,
            float y,
            float u,
            float v,
            int colorArgb
    ) {
        consumer.addVertex(poseMatrix, x, y, 0.0f)
                .setColor(colorArgb)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 0.0f, 1.0f);
    }

    /**
     * 透明方式保持 {@code TRANSLUCENT}：光影按透明类别分批，代理要和它对应的半透明颜色层落在同一阶段。
     */
    private static RenderType create(
            String name,
            ResourceLocation texture,
            RenderStateShard.WriteMaskStateShard writeMask
    ) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setCullState(RenderStateShard.NO_CULL)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .setWriteMaskState(writeMask)
                .createCompositeState(false);
        return RenderType.create(
                name,
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                1536,
                true,
                true,
                state
        );
    }
}

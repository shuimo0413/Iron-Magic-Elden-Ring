package com.eldenring.spells.client.render.gravity;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.MeteoriteVoidEntity;
import com.eldenring.spells.spell.fx.MeteoriteFx;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 陨石 / 艾斯提陨石起手的虚空裂缝：盘面平面内两块贴图面片，圆形轮廓由 32×32 像素贴图决定（原版式阶梯边）。
 * <ol>
 *   <li>盘面：黑核 + 亮紫边一体的 {@code disc.png}，不透明 cutout，写深度。
 *       开光影时按普通实体处理，不会像半透明粒子那样被雾 / 泛光冲淡。</li>
 *   <li>外晕：{@code halo.png} 分档像素光环，{@link RenderType#eyes} 加法发光，光影下能吃到泛光；只做氛围。</li>
 * </ol>
 * 贴图由 {@code 工具链/gen_meteorite_void_textures.py} 生成，像素半径与下面的 FRACTION 常量对应，改一边要同步另一边。
 * 盘面朝向 / 张开进度来自实体同步数据，半径与 {@link MeteoriteFx#voidRadiusBlocks} 共用，和点缀粒子对齐。
 * 面片不自转：像素网格斜着转会破坏原版的方正像素感。
 */
public class MeteoriteVoidRenderer extends EntityRenderer<MeteoriteVoidEntity> {

    private static final ResourceLocation DISC_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/meteorite_void/disc.png");
    private static final ResourceLocation HALO_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/meteorite_void/halo.png");

    private static final RenderType DISC_RENDER_TYPE = RenderType.entityCutoutNoCull(DISC_TEXTURE);
    private static final RenderType HALO_RENDER_TYPE = RenderType.eyes(HALO_TEXTURE);

    /**
     * 盘面面片半边长相对盘面半径的比例（= 贴图里亮边外沿 16 px）。
     * 调大 → 整个黑洞连亮边一起变大；贴图里亮边内沿约为本值 × 0.83。
     */
    private static final float DISC_HALF_SIZE_FRACTION = 1.06f;

    /** 外晕面片半边长相对盘面半径的比例（= 贴图边缘 16 px）。调大 → 像素辉光铺得更开、单个像素也更大。 */
    private static final float HALO_HALF_SIZE_FRACTION = 1.55f;

    /** 外晕染色（RGB，加法混合，越亮越耀眼）。贴图是灰度分档，乘上这个颜色。 */
    private static final int HALO_TINT_RGB = 0xB070FF;

    /** 外晕呼吸亮度范围（0–1 乘到颜色上）。 */
    private static final float HALO_PULSE_MIN = 0.65f;
    private static final float HALO_PULSE_MAX = 1.0f;

    /** 外晕呼吸角频率（弧度 / tick）。 */
    private static final float HALO_PULSE_RADIANS_PER_TICK = 0.3f;

    /**
     * 视锥剔除时包围盒外扩（方块）。实体碰撞箱只有 0.5 格，外晕面片角落最远约 2.9 格，
     * 不外扩的话盘心出屏整块就消失。
     */
    private static final double CULLING_INFLATE_BLOCKS = 3.0;

    /** 外晕沿法线朝落区一侧挪开的距离（方块），避免和盘面 z-fighting。 */
    private static final float LAYER_NORMAL_OFFSET_BLOCKS = 0.01f;

    public MeteoriteVoidRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(MeteoriteVoidEntity entity) {
        return DISC_TEXTURE;
    }

    @Override
    public boolean shouldRender(MeteoriteVoidEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        if (!entity.shouldRender(cameraX, cameraY, cameraZ)) {
            return false;
        }
        return frustum.isVisible(entity.getBoundingBox().inflate(CULLING_INFLATE_BLOCKS));
    }

    @Override
    public void render(
            MeteoriteVoidEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        float radiusBlocks = (float) MeteoriteFx.voidRadiusBlocks(entity.openingProgress(partialTick));
        float ageTicks = entity.tickCount + partialTick;

        Vec3 facing = entity.facingDirection();
        Vec3 discRight = MeteoriteFx.discRightAxis(facing);
        Vec3 discUp = discRight.cross(facing).normalize();
        DiscBasis basis = new DiscBasis(discRight, discUp, facing);

        Matrix4f poseMatrix = poseStack.last().pose();
        int fullBright = LightTexture.FULL_BRIGHT;

        putDiscQuad(bufferSource.getBuffer(DISC_RENDER_TYPE), poseMatrix, basis,
                radiusBlocks * DISC_HALF_SIZE_FRACTION, 0.0f, 0xFFFFFFFF, fullBright);

        float haloPulse = Mth.lerp(0.5f + 0.5f * Mth.sin(ageTicks * HALO_PULSE_RADIANS_PER_TICK), HALO_PULSE_MIN, HALO_PULSE_MAX);
        putDiscQuad(bufferSource.getBuffer(HALO_RENDER_TYPE), poseMatrix, basis,
                radiusBlocks * HALO_HALF_SIZE_FRACTION, LAYER_NORMAL_OFFSET_BLOCKS, scaleRgb(HALO_TINT_RGB, haloPulse), fullBright);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    /** 盘面平面内以实体为中心的方形面片，整张贴图铺满。 */
    private static void putDiscQuad(
            VertexConsumer consumer,
            Matrix4f poseMatrix,
            DiscBasis basis,
            float halfSizeBlocks,
            float normalOffsetBlocks,
            int packedColor,
            int packedLight
    ) {
        putDiscVertex(consumer, poseMatrix, basis, -halfSizeBlocks, -halfSizeBlocks, normalOffsetBlocks, 0.0f, 1.0f, packedColor, packedLight);
        putDiscVertex(consumer, poseMatrix, basis, halfSizeBlocks, -halfSizeBlocks, normalOffsetBlocks, 1.0f, 1.0f, packedColor, packedLight);
        putDiscVertex(consumer, poseMatrix, basis, halfSizeBlocks, halfSizeBlocks, normalOffsetBlocks, 1.0f, 0.0f, packedColor, packedLight);
        putDiscVertex(consumer, poseMatrix, basis, -halfSizeBlocks, halfSizeBlocks, normalOffsetBlocks, 0.0f, 0.0f, packedColor, packedLight);
    }

    /** RGB 按亮度缩放，alpha 固定 255（加法混合只看颜色）。 */
    private static int scaleRgb(int rgb, float brightness) {
        int red = Mth.clamp(Math.round(((rgb >> 16) & 0xFF) * brightness), 0, 255);
        int green = Mth.clamp(Math.round(((rgb >> 8) & 0xFF) * brightness), 0, 255);
        int blue = Mth.clamp(Math.round((rgb & 0xFF) * brightness), 0, 255);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    /**
     * 盘面局部坐标 (localX, localY) + 法线偏移 → 相对实体的世界偏移后写一个顶点。
     * 法线统一给世界上方：实体着色器按法线做方向光，朝上最亮，保证亮边不会被背光压暗。
     */
    private static void putDiscVertex(
            VertexConsumer consumer,
            Matrix4f poseMatrix,
            DiscBasis basis,
            float localX,
            float localY,
            float normalOffsetBlocks,
            float u,
            float v,
            int packedColor,
            int packedLight
    ) {
        float worldX = (float) (basis.right.x * localX + basis.up.x * localY + basis.normal.x * normalOffsetBlocks);
        float worldY = (float) (basis.right.y * localX + basis.up.y * localY + basis.normal.y * normalOffsetBlocks);
        float worldZ = (float) (basis.right.z * localX + basis.up.z * localY + basis.normal.z * normalOffsetBlocks);
        consumer.addVertex(poseMatrix, worldX, worldY, worldZ)
                .setColor(packedColor)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0.0f, 1.0f, 0.0f);
    }

    /** 盘面坐标系：右轴、上轴（盘面内）与法线（朝陨石落区）。 */
    private record DiscBasis(Vec3 right, Vec3 up, Vec3 normal) {
    }
}

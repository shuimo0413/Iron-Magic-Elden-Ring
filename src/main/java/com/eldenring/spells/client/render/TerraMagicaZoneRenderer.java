package com.eldenring.spells.client.render;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.TerraMagicaZoneEntity;
import com.eldenring.spells.tuning.TerraMagicaTuning;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 魔法之境：水平贴地半透明徽记四边形。
 * <p>
 * 边长 = {@code 2 * radius}，略抬高防 z-fighting；默认不自转（见 Tuning）。
 */
public class TerraMagicaZoneRenderer extends EntityRenderer<TerraMagicaZoneEntity> {

    private static final ResourceLocation SIGIL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/terra_magica/sigil.png");

    private static final RenderType SIGIL_RENDER_TYPE = RenderType.entityTranslucentEmissive(SIGIL_TEXTURE);

    public TerraMagicaZoneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            TerraMagicaZoneEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        float radiusBlocks = entity.getRadius();
        if (radiusBlocks < 0.1f) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0, TerraMagicaTuning.SIGIL_Y_OFFSET_BLOCKS, 0.0);
        if (TerraMagicaTuning.SIGIL_SPIN_DEGREES_PER_TICK != 0.0f) {
            float ageTicks = entity.tickCount + partialTick;
            poseStack.mulPose(Axis.YP.rotationDegrees(ageTicks * TerraMagicaTuning.SIGIL_SPIN_DEGREES_PER_TICK));
        }
        // 默认四边形在 XY 平面；转到 XZ（贴地）
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));

        float halfSize = radiusBlocks;
        Matrix4f poseMatrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(SIGIL_RENDER_TYPE);

        int alpha = Mth.clamp(Math.round(TerraMagicaTuning.SIGIL_OPACITY * 255.0f), 0, 255);
        int packedColor = (alpha << 24) | 0x00FFFFFF;
        int fullBright = 0xF000F0;

        putVertex(consumer, poseMatrix, -halfSize, -halfSize, 0.0f, 0.0f, 1.0f, packedColor, fullBright);
        putVertex(consumer, poseMatrix, halfSize, -halfSize, 0.0f, 1.0f, 1.0f, packedColor, fullBright);
        putVertex(consumer, poseMatrix, halfSize, halfSize, 0.0f, 1.0f, 0.0f, packedColor, fullBright);
        putVertex(consumer, poseMatrix, -halfSize, halfSize, 0.0f, 0.0f, 0.0f, packedColor, fullBright);

        // 背面再画一次，保证从下方也能看到（稀有，但洞穴/半空无缝）
        putVertex(consumer, poseMatrix, -halfSize, halfSize, 0.0f, 0.0f, 0.0f, packedColor, fullBright);
        putVertex(consumer, poseMatrix, halfSize, halfSize, 0.0f, 1.0f, 0.0f, packedColor, fullBright);
        putVertex(consumer, poseMatrix, halfSize, -halfSize, 0.0f, 1.0f, 1.0f, packedColor, fullBright);
        putVertex(consumer, poseMatrix, -halfSize, -halfSize, 0.0f, 0.0f, 1.0f, packedColor, fullBright);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void putVertex(
            VertexConsumer consumer,
            Matrix4f poseMatrix,
            float x,
            float y,
            float z,
            float u,
            float v,
            int packedColor,
            int packedLight
    ) {
        consumer.addVertex(poseMatrix, x, y, z)
                .setColor(packedColor)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0.0f, 1.0f, 0.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(TerraMagicaZoneEntity entity) {
        return SIGIL_TEXTURE;
    }
}

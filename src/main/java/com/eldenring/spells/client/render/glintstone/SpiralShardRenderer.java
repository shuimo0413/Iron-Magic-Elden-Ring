package com.eldenring.spells.client.render.glintstone;

import com.eldenring.spells.entity.SpiralShardProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * 旋飞魔砾渲染：两颗相位差 π 的彗星头，各自带连续光轨。
 */
public class SpiralShardRenderer extends EntityRenderer<SpiralShardProjectile> {

    private final ModelPart cometBodyRoot;

    public SpiralShardRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.cometBodyRoot = context.bakeLayer(GlintstoneCometModels.COMET_HEAD_LAYER);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(
            SpiralShardProjectile entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        GlintstoneCometHeadDrawer.VisualStyle visualStyle = entity.visualStyle();
        for (int cometIndex = 0; cometIndex < 2; cometIndex++) {
            Vec3 orbitOffset = entity.orbitWorldOffset(cometIndex, partialTicks);
            Vec3 cometHeadWorld = entity.orbitWorldPosition(cometIndex, partialTicks);
            Vec3 flightDirection = entity.orbitFlightDirection(cometIndex, partialTicks);
            poseStack.pushPose();
            poseStack.translate(orbitOffset.x, orbitOffset.y, orbitOffset.z);
            GlintstoneTrailRenderer.renderHistoryRibbon(
                    poseStack,
                    bufferSource,
                    cometHeadWorld,
                    cometHeadWorld,
                    Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(),
                    entity.cometTrailHistoryWorldPositions(cometIndex),
                    entity.trailStyle(),
                    visualStyle
            );
            GlintstoneCometHeadDrawer.renderAlongDirection(
                    entity,
                    partialTicks,
                    poseStack,
                    bufferSource,
                    cometBodyRoot,
                    this.entityRenderDispatcher,
                    visualStyle,
                    flightDirection
            );
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SpiralShardProjectile entity) {
        return entity.visualStyle().bodyTexture();
    }
}

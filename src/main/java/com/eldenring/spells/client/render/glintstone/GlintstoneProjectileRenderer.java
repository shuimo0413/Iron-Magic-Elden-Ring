package com.eldenring.spells.client.render.glintstone;

import com.eldenring.spells.entity.AbstractGlintstoneProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 通用辉石弹道渲染器：先画连续光轨与螺旋细丝，再画立体晶核 / 刺簇 + 光晕。
 *
 * @param <T> 具体辉石弹道类型
 */
public class GlintstoneProjectileRenderer<T extends AbstractGlintstoneProjectile> extends EntityRenderer<T> {
    private final ModelPart diamondBodyRoot;
    private final ModelPart spikedBodyRoot;

    public GlintstoneProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.diamondBodyRoot = context.bakeLayer(GlintstoneCometModels.COMET_HEAD_LAYER);
        this.spikedBodyRoot = context.bakeLayer(GlintstoneCometModels.SPIKED_COMET_HEAD_LAYER);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(
            T entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        GlintstoneCometHeadDrawer.VisualStyle visualStyle = entity.visualStyle();
        Vec3 interpolatedHeadWorld = new Vec3(
                Mth.lerp(partialTicks, entity.xo, entity.getX()),
                Mth.lerp(partialTicks, entity.yo, entity.getY()),
                Mth.lerp(partialTicks, entity.zo, entity.getZ())
        );
        Vec3 cameraWorld = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        // PoseStack 原点就是插值弹头；历史世界坐标相对此点转换后绘制真实曲线。
        GlintstoneTrailRenderer.renderHistoryRibbon(
                poseStack,
                bufferSource,
                interpolatedHeadWorld,
                interpolatedHeadWorld,
                cameraWorld,
                entity.trailHistoryWorldPositions(),
                entity.trailStyle(),
                visualStyle
        );
        GlintstoneTrailRenderer.renderHistoryHelixFilaments(
                poseStack,
                bufferSource,
                interpolatedHeadWorld,
                interpolatedHeadWorld,
                cameraWorld,
                entity.trailHistoryWorldPositions(),
                entity.trailStyle(),
                visualStyle.glowColorArgb(),
                visualStyle.spikeColorArgb(),
                entity.tickCount + partialTicks
        );
        ModelPart bodyRoot = visualStyle.usesSpikedCrystalCluster() ? spikedBodyRoot : diamondBodyRoot;
        GlintstoneCometHeadDrawer.render(
                entity,
                partialTicks,
                poseStack,
                bufferSource,
                bodyRoot,
                this.entityRenderDispatcher,
                visualStyle
        );
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return entity.visualStyle().bodyTexture();
    }
}

package com.eldenring.spells.client.render;

import com.eldenring.spells.client.render.glintstone.GlintstoneCometModels;
import com.eldenring.spells.client.render.glintstone.GlintstoneTrailRenderer;
import com.eldenring.spells.entity.FoundingRainDropEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import com.eldenring.spells.particle.foundingrain.FoundingRainFx;

/**
 * 创星雨雨针：只画白紫曲线光带，不画彗星晶核。
 * <p>
 * 路径来自客户端历史点 + 当前帧插值弹头，原理与辉石 ribbon 相同，颜色写死在 {@link FoundingRainFx}。
 */
public class FoundingRainDropRenderer extends EntityRenderer<FoundingRainDropEntity> {

    public FoundingRainDropRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(
            FoundingRainDropEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        Vec3 interpolatedHeadWorld = new Vec3(
                Mth.lerp(partialTicks, entity.xo, entity.getX()),
                Mth.lerp(partialTicks, entity.yo, entity.getY()),
                Mth.lerp(partialTicks, entity.zo, entity.getZ())
        );
        GlintstoneTrailRenderer.renderHistoryRibbon(
                poseStack,
                bufferSource,
                interpolatedHeadWorld,
                interpolatedHeadWorld,
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(),
                entity.trailHistoryWorldPositions(),
                entity.trailStyle(),
                FoundingRainFx.RAIN_DROP_GLOW_COLOR_ARGB,
                FoundingRainFx.RAIN_DROP_CORE_COLOR_ARGB
        );
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FoundingRainDropEntity entity) {
        return GlintstoneCometModels.TRAIL_BEAM_TEXTURE;
    }
}

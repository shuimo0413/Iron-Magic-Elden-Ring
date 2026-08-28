package com.eldenring.spells.client.render.haima;

import com.eldenring.spells.client.render.glintstone.GlintstoneTrailRenderer;
import com.eldenring.spells.entity.CannonOfHaimaProjectile;
import com.eldenring.spells.spell.CannonOfHaimaSpell;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 海摩炮弹客户端渲染：抛物线历史光轨 + 自发光实心球 + 相机朝向光晕。
 * <p>
 * 炮弹必须看起来是「一颗球」而不是彗星菱形；轨迹必须跟着真实重力弧，
 * 所以光轨走 {@link GlintstoneTrailRenderer} 的历史点 ribbon。
 */
public class HaimaCannonRenderer extends EntityRenderer<CannonOfHaimaProjectile> {

    private final ModelPart cannonballRoot;

    public HaimaCannonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.cannonballRoot = context.bakeLayer(HaimaCannonModels.CANNONBALL_LAYER);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(CannonOfHaimaProjectile entity) {
        return HaimaCannonModels.CANNONBALL_BODY_TEXTURE;
    }

    @Override
    public void render(
            CannonOfHaimaProjectile entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        Vec3 interpolatedFeetWorld = new Vec3(
                Mth.lerp(partialTicks, entity.xo, entity.getX()),
                Mth.lerp(partialTicks, entity.yo, entity.getY()),
                Mth.lerp(partialTicks, entity.zo, entity.getZ())
        );
        Vec3 interpolatedHeadWorld = interpolatedFeetWorld.add(0.0, entity.getBbHeight() * 0.5, 0.0);
        Vec3 cameraWorld = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        GlintstoneTrailRenderer.renderHistoryRibbon(
                poseStack,
                bufferSource,
                interpolatedFeetWorld,
                interpolatedHeadWorld,
                cameraWorld,
                entity.trailHistoryWorldPositions(),
                entity.trailStyle(),
                entity.visualStyle()
        );
        // 帚星彗尾的五条螺旋细丝；只画 ribbon 的话轨迹会扁成一条光带。
        GlintstoneTrailRenderer.renderHistoryHelixFilaments(
                poseStack,
                bufferSource,
                interpolatedFeetWorld,
                interpolatedHeadWorld,
                cameraWorld,
                entity.trailHistoryWorldPositions(),
                entity.trailStyle(),
                entity.visualStyle().glowColorArgb(),
                entity.visualStyle().spikeColorArgb(),
                entity.tickCount + partialTicks
        );

        float yawDegrees = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitchDegrees = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        float spinDegrees = (entity.tickCount + partialTicks) * CannonOfHaimaSpell.CANNONBALL_SPIN_DEGREES_PER_TICK;
        float scale = CannonOfHaimaSpell.CANNONBALL_RENDER_SCALE;

        poseStack.pushPose();
        poseStack.translate(0.0, entity.getBbHeight() * 0.5, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawDegrees));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitchDegrees));
        poseStack.mulPose(Axis.ZP.rotationDegrees(spinDegrees));
        poseStack.scale(scale, scale, scale);

        VertexConsumer bodyConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(HaimaCannonModels.CANNONBALL_BODY_TEXTURE)
        );
        cannonballRoot.getChild(HaimaCannonModels.CORE_PART).render(
                poseStack,
                bodyConsumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                CannonOfHaimaSpell.CANNONBALL_CORE_COLOR_ARGB
        );
        cannonballRoot.getChild(HaimaCannonModels.SHELL_PART).render(
                poseStack,
                bodyConsumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                CannonOfHaimaSpell.CANNONBALL_BODY_COLOR_ARGB
        );
        cannonballRoot.getChild(HaimaCannonModels.FACET_PART).render(
                poseStack,
                bodyConsumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                CannonOfHaimaSpell.CANNONBALL_FACET_COLOR_ARGB
        );
        poseStack.popPose();

        renderBillboardGlow(poseStack, bufferSource, entity, partialTicks);
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    /**
     * 两层朝向相机的光斑，让炮弹在抛物线远端仍然能被看见。
     */
    private void renderBillboardGlow(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            CannonOfHaimaProjectile entity,
            float partialTicks
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0, entity.getBbHeight() * 0.5, 0.0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        float age = entity.tickCount + partialTicks;
        float pulse = 1.55f + Mth.sin(age * 0.42f) * 0.16f;
        int glowColor = CannonOfHaimaSpell.CANNONBALL_GLOW_COLOR_ARGB;
        int red = (glowColor >> 16) & 0xFF;
        int green = (glowColor >> 8) & 0xFF;
        int blue = glowColor & 0xFF;
        int alpha = (glowColor >> 24) & 0xFF;

        drawGlowQuad(poseStack, bufferSource, pulse * 1.65f, red, green, blue, (int) (alpha * 0.38f));
        drawGlowQuad(poseStack, bufferSource, pulse, red, green, blue, alpha);
        poseStack.popPose();
    }

    private static void drawGlowQuad(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float scale,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        if (alpha <= 0) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(HaimaCannonModels.CANNONBALL_GLOW_TEXTURE)
        );
        int color = (alpha << 24) | (red << 16) | (green << 8) | blue;
        consumer.addVertex(matrix, -0.5f, -0.5f, 0.0f)
                .setColor(color)
                .setUv(0.0f, 1.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 0.0f, 1.0f);
        consumer.addVertex(matrix, 0.5f, -0.5f, 0.0f)
                .setColor(color)
                .setUv(1.0f, 1.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 0.0f, 1.0f);
        consumer.addVertex(matrix, 0.5f, 0.5f, 0.0f)
                .setColor(color)
                .setUv(1.0f, 0.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 0.0f, 1.0f);
        consumer.addVertex(matrix, -0.5f, 0.5f, 0.0f)
                .setColor(color)
                .setUv(0.0f, 0.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 0.0f, 1.0f);
        poseStack.popPose();
    }
}

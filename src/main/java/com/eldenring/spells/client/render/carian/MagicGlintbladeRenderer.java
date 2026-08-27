package com.eldenring.spells.client.render.carian;

import com.eldenring.spells.client.render.ProjectileOrientation;
import com.eldenring.spells.client.render.glintstone.GlintstoneTrailRenderer;
import com.eldenring.spells.entity.MagicGlintbladeEntity;
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
import com.eldenring.spells.spell.MagicGlintbladeSpell;

/**
 * 魔法辉剑客户端渲染：悬停时竖直漂浮，发射后刃尖对准速度并拖一条短蓝光带。
 */
public class MagicGlintbladeRenderer extends EntityRenderer<MagicGlintbladeEntity> {

    private final ModelPart swordRoot;

    public MagicGlintbladeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.swordRoot = context.bakeLayer(CarianSwordModels.SWORD_LAYER);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(MagicGlintbladeEntity entity) {
        return CarianSwordModels.SWORD_BODY_TEXTURE;
    }

    @Override
    public void render(
            MagicGlintbladeEntity entity,
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
        if (entity.hasLaunched()) {
            Vec3 cameraWorld = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            GlintstoneTrailRenderer.renderHistoryRibbon(
                    poseStack,
                    bufferSource,
                    interpolatedHeadWorld,
                    interpolatedHeadWorld,
                    cameraWorld,
                    entity.trailHistoryWorldPositions(),
                    entity.trailStyle(),
                    MagicGlintbladeSpell.TRAIL_GLOW_COLOR_ARGB,
                    MagicGlintbladeSpell.TRAIL_CORE_COLOR_ARGB
            );
        }

        poseStack.pushPose();
        if (!entity.hasLaunched()) {
            float bob = Mth.sin((entity.tickCount + partialTicks) * MagicGlintbladeSpell.HOVER_BOB_RADIANS_PER_TICK)
                    * MagicGlintbladeSpell.HOVER_BOB_AMPLITUDE_BLOCKS;
            poseStack.translate(0.0, bob, 0.0);
            poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
            // 悬停时刃尖略朝前上方，像挂在空中蓄势
            poseStack.mulPose(Axis.XP.rotationDegrees(12.0f));
        } else {
            ProjectileOrientation.alignPoseToDeltaMovement(poseStack, entity, partialTicks);
            // 模型刃沿 +Y，弹道朝向工具把 -Z 对准速度，再转 -90° 让刃尖朝前
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
        }
        poseStack.scale(
                MagicGlintbladeSpell.SWORD_RENDER_SCALE * MagicGlintbladeSpell.SWORD_RADIAL_SCALE,
                MagicGlintbladeSpell.SWORD_RENDER_SCALE,
                MagicGlintbladeSpell.SWORD_RENDER_SCALE * MagicGlintbladeSpell.SWORD_RADIAL_SCALE
        );

        VertexConsumer bodyConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(CarianSwordModels.SWORD_BODY_TEXTURE)
        );
        swordRoot.getChild(CarianSwordModels.POMMEL_PART).render(
                poseStack, bodyConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                MagicGlintbladeSpell.SWORD_BODY_COLOR_ARGB
        );
        swordRoot.getChild(CarianSwordModels.HANDLE_PART).render(
                poseStack, bodyConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                MagicGlintbladeSpell.SWORD_BODY_COLOR_ARGB
        );
        swordRoot.getChild(CarianSwordModels.GUARD_PART).render(
                poseStack, bodyConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                MagicGlintbladeSpell.SWORD_BLADE_COLOR_ARGB
        );
        swordRoot.getChild(CarianSwordModels.BLADE_PART).render(
                poseStack, bodyConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                MagicGlintbladeSpell.SWORD_BLADE_COLOR_ARGB
        );
        swordRoot.getChild(CarianSwordModels.EDGE_PART).render(
                poseStack, bodyConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                MagicGlintbladeSpell.SWORD_EDGE_COLOR_ARGB
        );
        poseStack.popPose();

        renderHoverGlow(entity, partialTicks, poseStack, bufferSource);
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    /**
     * 悬停阶段在刃中段叠一层朝向相机的柔光，发射后光斑缩小以免盖住剑身。
     */
    private void renderHoverGlow(
            MagicGlintbladeEntity entity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource
    ) {
        float pulse = 1.0f + Mth.sin((entity.tickCount + partialTicks) * 0.45f) * 0.12f;
        float scale = entity.hasLaunched() ? 0.55f * pulse : 0.95f * pulse;
        int glowColor = MagicGlintbladeSpell.SWORD_GLOW_COLOR_ARGB;
        int red = (glowColor >> 16) & 0xFF;
        int green = (glowColor >> 8) & 0xFF;
        int blue = glowColor & 0xFF;
        int alpha = (glowColor >> 24) & 0xFF;

        poseStack.pushPose();
        poseStack.translate(0.0, entity.hasLaunched() ? 0.0 : 0.45, 0.0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(scale, scale, scale);
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(CarianSwordModels.SWORD_GLOW_TEXTURE)
        );
        int color = (alpha << 24) | (red << 16) | (green << 8) | blue;
        consumer.addVertex(matrix, -0.5f, -0.5f, 0.0f)
                .setColor(color).setUv(0.0f, 1.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 0.0f, 1.0f);
        consumer.addVertex(matrix, 0.5f, -0.5f, 0.0f)
                .setColor(color).setUv(1.0f, 1.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 0.0f, 1.0f);
        consumer.addVertex(matrix, 0.5f, 0.5f, 0.0f)
                .setColor(color).setUv(1.0f, 0.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 0.0f, 1.0f);
        consumer.addVertex(matrix, -0.5f, 0.5f, 0.0f)
                .setColor(color).setUv(0.0f, 0.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 0.0f, 1.0f);
        poseStack.popPose();
    }
}

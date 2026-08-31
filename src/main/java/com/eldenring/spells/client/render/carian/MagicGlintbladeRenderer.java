package com.eldenring.spells.client.render.carian;

import com.eldenring.spells.client.render.ProjectileOrientation;
import com.eldenring.spells.client.render.glintstone.GlintstoneTrailRenderer;
import com.eldenring.spells.entity.MagicGlintbladeEntity;
import com.eldenring.spells.spell.MagicGlintbladeSpell;
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
 * 魔法辉剑客户端渲染：Blockbench 网格 + 半透明自发光。
 * 凝结时平躺、刃尖朝出手方向由小变大，射出后沿速度飞出。
 */
public class MagicGlintbladeRenderer<T extends MagicGlintbladeEntity> extends EntityRenderer<T> {

    private final ModelPart swordRoot;

    public MagicGlintbladeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.swordRoot = context.bakeLayer(MagicGlintbladeModels.GLINTBLADE_LAYER);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return MagicGlintbladeModels.GLINTBLADE_BODY_TEXTURE;
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

        float hoverAgeTicks = entity.tickCount + partialTicks;
        float appearScale = entity.hasLaunched()
                ? 1.0f
                : entity.renderHoverSwordScale(hoverAgeTicks);
        // 巨剑阵把同一把网格原地放大；出现段仍走 0–1 缓出。
        float swordScale = appearScale * entity.renderSwordVisualScale();

        poseStack.pushPose();
        if (!entity.hasLaunched()) {
            if (entity.usesOutwardHoverPose()) {
                applyOutwardHoverPose(poseStack, entity);
            } else {
                applyHoverBladePose(poseStack, entity);
            }
        } else {
            ProjectileOrientation.alignPoseToDeltaMovement(poseStack, entity, partialTicks);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
        }
        // 宽厚已经写进 Blockbench 网格，不要再径向压扁。
        poseStack.scale(
                MagicGlintbladeSpell.SWORD_RENDER_SCALE * swordScale,
                MagicGlintbladeSpell.SWORD_RENDER_SCALE * swordScale,
                MagicGlintbladeSpell.SWORD_RENDER_SCALE * swordScale
        );

        if (swordScale > 0.02f) {
            VertexConsumer bodyConsumer = bufferSource.getBuffer(
                    RenderType.entityTranslucentEmissive(MagicGlintbladeModels.GLINTBLADE_BODY_TEXTURE)
            );
            swordRoot.getChild(MagicGlintbladeModels.POMMEL_PART).render(
                    poseStack, bodyConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    MagicGlintbladeSpell.SWORD_BODY_COLOR_ARGB
            );
            swordRoot.getChild(MagicGlintbladeModels.HANDLE_PART).render(
                    poseStack, bodyConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    MagicGlintbladeSpell.SWORD_BODY_COLOR_ARGB
            );
            swordRoot.getChild(MagicGlintbladeModels.GUARD_PART).render(
                    poseStack, bodyConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    MagicGlintbladeSpell.SWORD_BLADE_COLOR_ARGB
            );
            swordRoot.getChild(MagicGlintbladeModels.BLADE_PART).render(
                    poseStack, bodyConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    MagicGlintbladeSpell.SWORD_BLADE_COLOR_ARGB
            );
            swordRoot.getChild(MagicGlintbladeModels.RIDGE_PART).render(
                    poseStack, bodyConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    MagicGlintbladeSpell.SWORD_EDGE_COLOR_ARGB
            );
            swordRoot.getChild(MagicGlintbladeModels.EDGE_PART).render(
                    poseStack, bodyConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    MagicGlintbladeSpell.SWORD_EDGE_COLOR_ARGB
            );
        }
        poseStack.popPose();

        renderHoverGlow(entity, partialTicks, swordScale, poseStack, bufferSource);
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    /**
     * 凝结姿态：刃尖沿出手瞬间视线平躺（宽面朝天、护手水平）。
     * <p>
     * 朝向只读实体上锁死的同步 yaw/pitch，不读 {@code entityYaw}。
     * 弹道基类在速度为零时会把 yRot 拧成 0，用渲染器传入的 yaw 会让剑永远指南、看起来像斜 45°。
     */
    private static void applyHoverBladePose(PoseStack poseStack, MagicGlintbladeEntity entity) {
        float yawDegrees = entity.hoverYawDegrees();
        float pitchDegrees = entity.hoverPitchDegrees();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawDegrees));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitchDegrees + 90.0f));
    }

    /**
     * 圆阵跟手：刃尖沿玩家当前视线，五把剑平行朝前；姿态与射出后同一套轴向。
     */
    private static void applyOutwardHoverPose(PoseStack poseStack, MagicGlintbladeEntity entity) {
        ProjectileOrientation.alignPoseToDirection(poseStack, entity.hoverBladeTipWorldDirection());
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
    }

    /**
     * 漩涡核柔光。纯漩涡段只留一小点；凝结时随剑变大；射出后缩小以免盖住剑身。
     */
    private void renderHoverGlow(
            T entity,
            float partialTicks,
            float swordScale,
            PoseStack poseStack,
            MultiBufferSource bufferSource
    ) {
        float pulse = 1.0f + Mth.sin((entity.tickCount + partialTicks) * 0.45f) * 0.12f;
        float scale;
        if (entity.hasLaunched()) {
            scale = 0.38f * pulse;
        } else {
            scale = (0.20f + 0.42f * swordScale) * pulse;
        }
        int glowColor = MagicGlintbladeSpell.SWORD_GLOW_COLOR_ARGB;
        int red = (glowColor >> 16) & 0xFF;
        int green = (glowColor >> 8) & 0xFF;
        int blue = glowColor & 0xFF;
        int alpha = (glowColor >> 24) & 0xFF;

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(scale, scale, scale);
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(MagicGlintbladeModels.GLINTBLADE_GLOW_TEXTURE)
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

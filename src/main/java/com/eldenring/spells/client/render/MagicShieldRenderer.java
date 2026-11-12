package com.eldenring.spells.client.render;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.MagicShieldEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** 三块盾片沿身体旋转；容量控制亮度，受击短暂闪白；第一人称保留透明度避免遮挡。 */
public final class MagicShieldRenderer extends EntityRenderer<MagicShieldEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EldenRingSpellsMod.MOD_ID, "textures/entity/magic_shield.png");
    private static final RenderType TYPE = RenderType.entityTranslucentEmissive(TEXTURE);
    private static final RenderType CRACKED = RenderType.entityTranslucentEmissive(ResourceLocation.fromNamespaceAndPath(
            EldenRingSpellsMod.MOD_ID, "textures/entity/magic_shield_cracked.png"));
    private static final RenderType FRACTURED = RenderType.entityTranslucentEmissive(ResourceLocation.fromNamespaceAndPath(
            EldenRingSpellsMod.MOD_ID, "textures/entity/magic_shield_fractured.png"));

    public MagicShieldRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public void render(MagicShieldEntity entity, float yaw, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light) {
        if (entity.isInvisible() || entity.capacity() <= 0) return;
        float age = entity.tickCount + partialTick;
        float flash = entity.flash(partialTick);
        boolean firstPerson = entity.getOwner() == Minecraft.getInstance().player
                && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        if (entity.getOwner() != null) {
            var owner = entity.getOwner();
            pose.pushPose();
            // EntityRenderer's origin interpolates the anchor; correct to the owner's current frame.
            pose.translate(Mth.lerp(partialTick, owner.xo, owner.getX()) - Mth.lerp(partialTick, entity.xo, entity.getX()),
                    Mth.lerp(partialTick, owner.yo, owner.getY()) - Mth.lerp(partialTick, entity.yo, entity.getY()),
                    Mth.lerp(partialTick, owner.zo, owner.getZ()) - Mth.lerp(partialTick, entity.zo, entity.getZ()));
        } else pose.pushPose();
        float ratio = Mth.clamp(entity.capacity() / entity.maxCapacity(), 0, 1);
        float rotation = age * 0.9f;
        int hitPane = -1;
        if (entity.hitAngle() > -999 && flash > 0) {
            float nearest = Float.MAX_VALUE;
            for (int i = 0; i < 3; i++) {
                float distance = Math.abs(Mth.wrapDegrees(rotation + i * 120 - entity.hitAngle()));
                if (distance < nearest) { nearest = distance; hitPane = i; }
            }
        }
        RenderType type = ratio <= 0.25f ? FRACTURED : ratio <= 0.5f ? CRACKED : TYPE;
        for (int i = 0; i < 3; i++) {
            float angle = rotation + i * 120;
            float hit = flash * (hitPane < 0 ? 0.45f : hitPane == i ? 1 : 0);
            float opacity = firstPerson ? 0.19f : 0.55f;
            if (firstPerson) {
                var look = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
                double radians = Math.toRadians(angle);
                float facing = (float)(Math.sin(radians) * look.x() + Math.cos(radians) * look.z());
                // Fade smoothly inside the forward view cone, including during a hit.
                opacity *= 1 - 0.9f * Mth.clamp((facing - 0.35f) / 0.5f, 0, 1);
            }
            float pulse = ratio <= 0.25f ? 0.85f + 0.15f * Mth.sin(age * 0.35f) : 1;
            float visibility = firstPerson ? opacity / 0.19f : 1;
            int alpha = Mth.clamp((int)(255 * (opacity * (0.6f + 0.4f * ratio) * pulse
                    + hit * (firstPerson ? 0.15f : 0.35f) * visibility)), 0, 255);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(angle));
            pose.translate(0, 1.05 + Math.sin(age * 0.08 + i * 2) * 0.08, 1.12);
            // Square padded texture preserves the original 250:200 winged shield proportions.
            float scale = 1.375f * (1 + hit * 0.07f);
            pose.scale(scale, scale, 1);
            Matrix4f matrix = pose.last().pose();
            VertexConsumer vertices = buffers.getBuffer(type);
            float size = 0.64f;
            int tinted = alpha << 24 | 0xFFFFFF;
            vertex(vertices, matrix, -size, -size, 0, 1, tinted);
            vertex(vertices, matrix, size, -size, 1, 1, tinted);
            vertex(vertices, matrix, size, size, 1, 0, tinted);
            vertex(vertices, matrix, -size, size, 0, 0, tinted);
            vertex(vertices, matrix, -size, size, 0, 0, tinted);
            vertex(vertices, matrix, size, size, 1, 0, tinted);
            vertex(vertices, matrix, size, -size, 1, 1, tinted);
            vertex(vertices, matrix, -size, -size, 0, 1, tinted);
            pose.popPose();
        }
        pose.popPose();
    }
    private static void vertex(VertexConsumer out, Matrix4f matrix, float x, float y, float u, float v, int color) {
        out.addVertex(matrix, x, y, 0).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(0, 0, 1);
    }
    @Override public ResourceLocation getTextureLocation(MagicShieldEntity entity) { return TEXTURE; }
}

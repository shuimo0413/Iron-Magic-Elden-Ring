package com.eldenring.spells.client.render;

import com.eldenring.spells.entity.FoundingRainOfStarsEntity;
import com.eldenring.spells.tuning.FoundingRainOfStarsTuning;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 创星雨身前星云：水平贴天的一层软光面片（对照法环创星雨，不是一团球）。
 * <p>
 * 面片躺在 XZ 上朝下看，深紫/深蓝打底，亮丝加法叠在同一层；白色小星星仍走粒子。
 */
public class FoundingRainNebulaRenderer extends EntityRenderer<FoundingRainOfStarsEntity> {

    /**
     * 本地坐标：X 沿施法者右方，Z 沿水平前方，Y 向上。单位方块。
     */
    private record Blob(
            float localRightBlocks,
            float localUpBlocks,
            float localForwardBlocks,
            float halfSizeBlocks,
            int red,
            int green,
            int blue,
            int peakAlpha,
            float spinDegreesPerTick,
            boolean filament
    ) {
    }

    /**
     * 全躺在同一层上，Y 只留极小错开。颜色对照法环：靛底 + 紫底 + 青/亮紫丝。
     */
    private static final Blob[] BLOBS = {
            new Blob(-2.8f, 0.05f, 0.35f, 3.8f, 28, 36, 92, 118, 1.4f, false),
            new Blob(-1.1f, -0.04f, -1.15f, 4.3f, 72, 28, 118, 128, -1.1f, false),
            new Blob(0.10f, 0.07f, 0.08f, 4.9f, 36, 28, 96, 140, 0.8f, false),
            new Blob(1.35f, -0.05f, 1.05f, 4.2f, 88, 32, 132, 124, -1.3f, false),
            new Blob(2.75f, 0.03f, -0.40f, 3.7f, 24, 42, 108, 112, 1.2f, false),
            new Blob(-0.55f, 0.08f, 2.10f, 3.4f, 58, 24, 108, 96, -0.7f, false),
            new Blob(0.70f, -0.06f, -2.20f, 3.5f, 32, 48, 118, 92, 0.9f, false),
            new Blob(-1.85f, 0.02f, 0.55f, 2.5f, 168, 102, 255, 78, 3.8f, true),
            new Blob(0.20f, 0.06f, 0.25f, 2.8f, 110, 190, 255, 72, -3.2f, true),
            new Blob(1.70f, -0.03f, -0.70f, 2.3f, 186, 92, 255, 70, 2.9f, true),
            new Blob(-0.30f, 0.04f, 1.55f, 2.0f, 90, 210, 255, 64, -4.1f, true),
            new Blob(0.95f, -0.05f, -1.60f, 2.2f, 150, 88, 230, 66, 3.4f, true)
    };

    public FoundingRainNebulaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(
            FoundingRainOfStarsEntity entity,
            Frustum frustum,
            double cameraX,
            double cameraY,
            double cameraZ
    ) {
        if (!entity.isOverheadCloudActive()) {
            return super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ);
        }
        Vec3 cloudCenter = entity.overheadCloudCenter();
        double pad = FoundingRainOfStarsTuning.OVERHEAD_CLOUD_RADIUS_BLOCKS + 3.0;
        double sheetPad = FoundingRainOfStarsTuning.OVERHEAD_CLOUD_SHEET_THICKNESS_BLOCKS + 1.2;
        AABB cloudBox = new AABB(
                cloudCenter.x - pad,
                cloudCenter.y - sheetPad,
                cloudCenter.z - pad,
                cloudCenter.x + pad,
                cloudCenter.y + sheetPad,
                cloudCenter.z + pad
        );
        return frustum.isVisible(cloudBox);
    }

    @Override
    public void render(
            FoundingRainOfStarsEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        if (!entity.isOverheadCloudActive()) {
            return;
        }
        float envelope = cloudFadeEnvelope(entity, partialTick);
        if (envelope <= 0.01f) {
            return;
        }

        Vec3 entityPosition = entity.getPosition(partialTick);
        Vec3 cloudCenter = entity.overheadCloudCenter();
        float yawRadians = entity.overheadCloudYawRadians();
        float cosYaw = Mth.cos(yawRadians);
        float sinYaw = Mth.sin(yawRadians);
        float ageTicks = entity.tickCount + partialTick;

        VertexConsumer bodyConsumer = bufferSource.getBuffer(FoundingRainNebulaRenderTypes.BODY);
        for (Blob blob : BLOBS) {
            if (!blob.filament) {
                renderBlob(
                        poseStack,
                        bodyConsumer,
                        entityPosition,
                        cloudCenter,
                        cosYaw,
                        sinYaw,
                        ageTicks,
                        envelope,
                        blob
                );
            }
        }

        VertexConsumer filamentConsumer = bufferSource.getBuffer(FoundingRainNebulaRenderTypes.FILAMENT);
        for (Blob blob : BLOBS) {
            if (blob.filament) {
                renderBlob(
                        poseStack,
                        filamentConsumer,
                        entityPosition,
                        cloudCenter,
                        cosYaw,
                        sinYaw,
                        ageTicks,
                        envelope,
                        blob
                );
            }
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void renderBlob(
            PoseStack poseStack,
            VertexConsumer consumer,
            Vec3 entityPosition,
            Vec3 cloudCenter,
            float cosYaw,
            float sinYaw,
            float ageTicks,
            float envelope,
            Blob blob
    ) {
        // 水平前方 = (-sin, 0, cos)，右方 = (cos, 0, sin)
        double worldX = cloudCenter.x
                + blob.localRightBlocks * cosYaw
                + blob.localForwardBlocks * -sinYaw;
        double worldY = cloudCenter.y + blob.localUpBlocks;
        double worldZ = cloudCenter.z
                + blob.localRightBlocks * sinYaw
                + blob.localForwardBlocks * cosYaw;

        poseStack.pushPose();
        poseStack.translate(
                worldX - entityPosition.x,
                worldY - entityPosition.y,
                worldZ - entityPosition.z
        );
        // 先绕世界 Y 慢旋，再把默认 XY 四边形拍到 XZ，成为贴天的一层。
        poseStack.mulPose(Axis.YP.rotationDegrees(ageTicks * blob.spinDegreesPerTick));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
        poseStack.scale(blob.halfSizeBlocks, blob.halfSizeBlocks, blob.halfSizeBlocks);

        int alpha = Mth.clamp(Math.round(blob.peakAlpha * envelope), 0, 255);
        Matrix4f poseMatrix = poseStack.last().pose();
        putBillboardQuad(consumer, poseMatrix, blob.red, blob.green, blob.blue, alpha);
        putBillboardQuadBack(consumer, poseMatrix, blob.red, blob.green, blob.blue, alpha);
        poseStack.popPose();
    }

    private static void putBillboardQuad(
            VertexConsumer consumer,
            Matrix4f poseMatrix,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        putVertex(consumer, poseMatrix, -1.0f, -1.0f, 0.0f, 1.0f, red, green, blue, alpha);
        putVertex(consumer, poseMatrix, 1.0f, -1.0f, 1.0f, 1.0f, red, green, blue, alpha);
        putVertex(consumer, poseMatrix, 1.0f, 1.0f, 1.0f, 0.0f, red, green, blue, alpha);
        putVertex(consumer, poseMatrix, -1.0f, 1.0f, 0.0f, 0.0f, red, green, blue, alpha);
    }

    /**
     * 背面再画一次：玩家从下往上看贴天的一层时，法线朝下也能看见。
     */
    private static void putBillboardQuadBack(
            VertexConsumer consumer,
            Matrix4f poseMatrix,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        putVertex(consumer, poseMatrix, -1.0f, 1.0f, 0.0f, 0.0f, red, green, blue, alpha);
        putVertex(consumer, poseMatrix, 1.0f, 1.0f, 1.0f, 0.0f, red, green, blue, alpha);
        putVertex(consumer, poseMatrix, 1.0f, -1.0f, 1.0f, 1.0f, red, green, blue, alpha);
        putVertex(consumer, poseMatrix, -1.0f, -1.0f, 0.0f, 1.0f, red, green, blue, alpha);
    }

    private static void putVertex(
            VertexConsumer consumer,
            Matrix4f poseMatrix,
            float x,
            float y,
            float u,
            float v,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        consumer.addVertex(poseMatrix, x, y, 0.0f)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0f, 1.0f, 0.0f);
    }

    /**
     * 雨云从出现到消失的透明度包络。单位：相对 {@link FoundingRainOfStarsTuning#overheadCloudSpawnTick()}。
     */
    private static float cloudFadeEnvelope(FoundingRainOfStarsEntity entity, float partialTick) {
        float cloudAgeTicks = entity.tickCount + partialTick - FoundingRainOfStarsTuning.overheadCloudSpawnTick();
        float lifetimeTicks = FoundingRainOfStarsTuning.OVERHEAD_CLOUD_LIFETIME_TICKS;
        if (cloudAgeTicks < 0.0f || cloudAgeTicks > lifetimeTicks) {
            return 0.0f;
        }
        float fadeInEnd = FoundingRainOfStarsTuning.OVERHEAD_CLOUD_FADE_IN_TICKS;
        float fadeOutStart = lifetimeTicks - FoundingRainOfStarsTuning.OVERHEAD_CLOUD_FADE_OUT_TICKS;
        if (cloudAgeTicks < fadeInEnd) {
            float fadeInProgress = Mth.clamp(cloudAgeTicks / Math.max(1.0e-4f, fadeInEnd), 0.0f, 1.0f);
            return fadeInProgress * fadeInProgress * (3.0f - 2.0f * fadeInProgress);
        }
        if (cloudAgeTicks > fadeOutStart) {
            float fadeOutProgress = Mth.clamp(
                    (cloudAgeTicks - fadeOutStart) / Math.max(1.0e-4f, lifetimeTicks - fadeOutStart),
                    0.0f,
                    1.0f
            );
            return 1.0f - fadeOutProgress * fadeOutProgress;
        }
        return 1.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(FoundingRainOfStarsEntity entity) {
        return FoundingRainNebulaRenderTypes.SOFT_BLOB_TEXTURE;
    }
}

package com.eldenring.spells.client.render.gravity;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.MeteoriteProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/**
 * 陨石：若干块随机旋转的深色岩石 / 紫晶方块挤成一团粗糙石块，外裹一层紫色重力辉光，沿飞行方向翻滚。
 * <p>
 * 思路同 {@link RockSlingRenderer}，但块头更大、紫晶点缀更多、辉光更饱和，贴近原画里的紫色陨石。
 * 碎块的偏移 / 旋转 / 大小以实体 id 为种子，每颗陨石形状固定且彼此不同。辉光贴图复用粒子 {@code gravity_glow}。
 */
public class MeteoriteRenderer extends EntityRenderer<MeteoriteProjectile> {

    private static final ResourceLocation GLOW_TEXTURE =
            new ResourceLocation(EldenRingSpellsMod.MOD_ID, "textures/particle/gravity_glow.png");

    private static final RenderType GLOW_RENDER_TYPE = RenderType.entityTranslucentEmissive(GLOW_TEXTURE);

    /**
     * 组成一颗陨石的碎块数量。调大 → 轮廓更圆更碎；调小 → 更像几块方石粘在一起。
     */
    private static final int CHUNK_COUNT = 9;

    /** 碎块边长下限（方块）。 */
    private static final float CHUNK_MIN_SIZE_BLOCKS = 0.36f;

    /** 碎块边长上限（方块）。 */
    private static final float CHUNK_MAX_SIZE_BLOCKS = 0.56f;

    /**
     * 碎块中心离陨石中心的最大偏移（方块）。调大 → 更疙瘩、更不规则；调小 → 更紧更圆。
     */
    private static final float CHUNK_MAX_OFFSET_BLOCKS = 0.22f;

    /** 构成陨石的方块种类：深板岩为底，哭泣黑曜石 / 紫水晶块点缀出原画的紫色晶面。 */
    private static final BlockState[] ROCK_BLOCK_STATES = {
            Blocks.COBBLED_DEEPSLATE.defaultBlockState(),
            Blocks.DEEPSLATE.defaultBlockState(),
            Blocks.CRYING_OBSIDIAN.defaultBlockState(),
            Blocks.AMETHYST_BLOCK.defaultBlockState(),
            Blocks.COBBLED_DEEPSLATE.defaultBlockState(),
            Blocks.CRYING_OBSIDIAN.defaultBlockState(),
    };

    /** 飞行翻滚（度/tick）。调大 → 更像被狠狠甩下来；调小 → 更沉更稳。 */
    private static final float FLIGHT_TUMBLE_DEGREES_PER_TICK = 11.0f;

    /** 陨石受光的最低方块光照等级（0–15）。辉石微光让陨石在暗处也看得清。 */
    private static final int ROCK_MIN_BLOCK_LIGHT = 10;

    /** 辉光半边长（方块）。调大 → 紫晕更散。 */
    private static final float GLOW_HALF_SIZE_BLOCKS = 0.85f;

    /** 辉光基础不透明度（0–1）。 */
    private static final float GLOW_OPACITY = 0.55f;

    /** 辉光呼吸振幅（相对不透明度）。 */
    private static final float GLOW_PULSE_AMPLITUDE = 0.20f;

    /** 辉光呼吸角频率（弧度/tick）。 */
    private static final float GLOW_PULSE_RADIANS_PER_TICK = 0.35f;

    /** 辉光绕视线自转（度/tick）。 */
    private static final float GLOW_SPIN_DEGREES_PER_TICK = 7.0f;

    private final BlockRenderDispatcher blockRenderDispatcher;

    public MeteoriteRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(MeteoriteProjectile entity) {
        return GLOW_TEXTURE;
    }

    @Override
    public void render(
            MeteoriteProjectile entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        float ageTicks = entity.tickCount + partialTick;

        poseStack.pushPose();
        poseStack.translate(0.0, entity.getBbHeight() * 0.5, 0.0);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(ageTicks * FLIGHT_TUMBLE_DEGREES_PER_TICK));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getId() * 53.0f));
        renderRockChunks(entity, poseStack, bufferSource, brightenedLight(packedLight));
        poseStack.popPose();

        renderGlow(poseStack, bufferSource, ageTicks);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void renderRockChunks(
            MeteoriteProjectile entity,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int rockLight
    ) {
        RandomSource chunkRandom = RandomSource.create(entity.getId() * 341873128712L + 132897987541L);
        for (int chunkIndex = 0; chunkIndex < CHUNK_COUNT; chunkIndex++) {
            float chunkSizeBlocks = Mth.lerp(chunkRandom.nextFloat(), CHUNK_MIN_SIZE_BLOCKS, CHUNK_MAX_SIZE_BLOCKS);
            float offsetX = (chunkRandom.nextFloat() * 2.0f - 1.0f) * CHUNK_MAX_OFFSET_BLOCKS;
            float offsetY = (chunkRandom.nextFloat() * 2.0f - 1.0f) * CHUNK_MAX_OFFSET_BLOCKS;
            float offsetZ = (chunkRandom.nextFloat() * 2.0f - 1.0f) * CHUNK_MAX_OFFSET_BLOCKS;
            float yawDegrees = chunkRandom.nextFloat() * 360.0f;
            float pitchDegrees = chunkRandom.nextFloat() * 360.0f;
            float rollDegrees = chunkRandom.nextFloat() * 360.0f;
            BlockState chunkState = ROCK_BLOCK_STATES[chunkRandom.nextInt(ROCK_BLOCK_STATES.length)];

            poseStack.pushPose();
            poseStack.translate(offsetX, offsetY, offsetZ);
            poseStack.mulPose(Axis.YP.rotationDegrees(yawDegrees));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitchDegrees));
            poseStack.mulPose(Axis.ZP.rotationDegrees(rollDegrees));
            poseStack.scale(chunkSizeBlocks, chunkSizeBlocks, chunkSizeBlocks);
            poseStack.translate(-0.5, -0.5, -0.5);
            blockRenderDispatcher.renderSingleBlock(
                    chunkState,
                    poseStack,
                    bufferSource,
                    rockLight,
                    OverlayTexture.NO_OVERLAY
            );
            poseStack.popPose();
        }
    }

    private void renderGlow(PoseStack poseStack, MultiBufferSource bufferSource, float ageTicks) {
        float pulsedOpacity = GLOW_OPACITY * (1.0f + GLOW_PULSE_AMPLITUDE * Mth.sin(ageTicks * GLOW_PULSE_RADIANS_PER_TICK));
        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(ageTicks * GLOW_SPIN_DEGREES_PER_TICK));
        putBillboardQuad(
                bufferSource.getBuffer(GLOW_RENDER_TYPE),
                poseStack.last().pose(),
                GLOW_HALF_SIZE_BLOCKS,
                packViolet(pulsedOpacity),
                LightTexture.FULL_BRIGHT
        );
        poseStack.popPose();
    }

    /** 方块光至少抬到 {@link #ROCK_MIN_BLOCK_LIGHT}，天空光不动。 */
    private static int brightenedLight(int packedLight) {
        int blockLight = Math.max(LightTexture.block(packedLight), ROCK_MIN_BLOCK_LIGHT);
        return LightTexture.pack(blockLight, LightTexture.sky(packedLight));
    }

    /** 饱和紫（比岩石球的薰衣草紫更深更艳）。 */
    private static int packViolet(float opacity) {
        int alpha = Mth.clamp(Math.round(opacity * 255.0f), 0, 255);
        return (alpha << 24) | (170 << 16) | (90 << 8) | 245;
    }

    private static void putBillboardQuad(
            VertexConsumer consumer,
            Matrix4f poseMatrix,
            float halfSizeBlocks,
            int packedColor,
            int packedLight
    ) {
        putVertex(consumer, poseMatrix, -halfSizeBlocks, -halfSizeBlocks, 0.0f, 1.0f, packedColor, packedLight);
        putVertex(consumer, poseMatrix, halfSizeBlocks, -halfSizeBlocks, 1.0f, 1.0f, packedColor, packedLight);
        putVertex(consumer, poseMatrix, halfSizeBlocks, halfSizeBlocks, 1.0f, 0.0f, packedColor, packedLight);
        putVertex(consumer, poseMatrix, -halfSizeBlocks, halfSizeBlocks, 0.0f, 0.0f, packedColor, packedLight);
    }

    private static void putVertex(
            VertexConsumer consumer,
            Matrix4f poseMatrix,
            float x,
            float y,
            float u,
            float v,
            int packedColor,
            int packedLight
    ) {
        consumer.vertex(poseMatrix, x, y, 0.0f)
                .color(packedColor)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(0.0f, 0.0f, 1.0f)
                .endVertex();
    }
}

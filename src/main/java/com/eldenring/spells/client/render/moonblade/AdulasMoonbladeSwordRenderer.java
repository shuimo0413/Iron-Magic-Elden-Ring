package com.eldenring.spells.client.render.moonblade;

import com.eldenring.spells.client.AdulasMoonbladeHand;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;

/**
 * 月光剑手持网格：从卡利亚大剑拷出。贴图与大剑同一套像素形状（只重染成冰蓝白），
 * 所以柄定位 / 刃长缩放常量与大剑相同；改这里不会动大剑。
 */
public final class AdulasMoonbladeSwordRenderer {

    /** 第三人称手持缩放。必须和 JSON {@code scale} 相同，柄才会以原大小进掌心。 */
    private static final float THIRD_PERSON_DISPLAY_SCALE = 1.70f;

    /** 生成物立方体中心 Y。原版手持原点在这里。 */
    private static final float GENERATED_MESH_CENTER_Y = 0.50f;

    /** 竖贴图柄中心的生成物 Y。与大剑同一张像素形状，禁止改这个来「加长」。 */
    private static final float HANDLE_LOCAL_Y = 0.156f;

    /** 只沿刃轴相对剑柄拉长。2.2 ≈ 把刃伸到迅剑的两倍出头，对上 7 格斩击半径。 */
    private static final float BLADE_LENGTH_SCALE = 2.20f;

    /** display 缩放之后，沿刃轴把柄送到拳眼（方块）。 */
    private static final float HANDLE_ALONG_BLADE_BLOCKS =
            (GENERATED_MESH_CENTER_Y - HANDLE_LOCAL_Y) * THIRD_PERSON_DISPLAY_SCALE;

    private AdulasMoonbladeSwordRenderer() {
    }

    /** 在当前 PoseStack（已经 translateToHand + 握点）上画出右手那把像素月光剑。 */
    public static void renderInRightHand(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            AbstractClientPlayer player
    ) {
        ItemStack swordStack = AdulasMoonbladeHand.swordStack();
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel bakedModel = itemRenderer.getModel(
                swordStack,
                player.level(),
                player,
                player.getId() + ItemDisplayContext.THIRD_PERSON_RIGHT_HAND.ordinal()
        );

        poseStack.pushPose();
        bakedModel = ClientHooks.handleCameraTransforms(
                poseStack,
                bakedModel,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                false
        );
        applyHandleIntoPalm(poseStack);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(InventoryMenu.BLOCK_ATLAS)
        );
        for (BakedModel renderPassModel : bakedModel.getRenderPasses(swordStack, true)) {
            itemRenderer.renderModelLists(
                    renderPassModel,
                    swordStack,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    vertexConsumer
            );
        }
        poseStack.popPose();
    }

    /** 第三人称右手物品姿态 + 生成物原点平移。光轨采样必须和网格走同一套。 */
    public static void applyHeldItemPose(PoseStack poseStack, AbstractClientPlayer player) {
        ItemStack swordStack = AdulasMoonbladeHand.swordStack();
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel bakedModel = itemRenderer.getModel(
                swordStack,
                player.level(),
                player,
                player.getId() + ItemDisplayContext.THIRD_PERSON_RIGHT_HAND.ordinal()
        );
        ClientHooks.handleCameraTransforms(
                poseStack,
                bakedModel,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                false
        );
        applyHandleIntoPalm(poseStack);
    }

    /**
     * 先把立方体中心对到拳眼，再沿刃轴把柄滑进掌心，最后绕柄做刃长缩放。
     * 顺序不能换，否则滑柄距离会被倍数放大，整把剑飞出掌心。
     */
    private static void applyHandleIntoPalm(PoseStack poseStack) {
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        poseStack.translate(0.0F, HANDLE_ALONG_BLADE_BLOCKS, 0.0F);
        poseStack.translate(0.0F, HANDLE_LOCAL_Y, 0.0F);
        poseStack.scale(1.0F, BLADE_LENGTH_SCALE, 1.0F);
        poseStack.translate(0.0F, -HANDLE_LOCAL_Y, 0.0F);
    }
}

package com.eldenring.spells.client.render.carian;

import com.eldenring.spells.client.CarianGreatswordHand;
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
 * 卡利亚大剑手持网格：从迅剑渲染器拷出的独立副本。
 * 贴图像素仍用迅剑 PNG，缩放 / 柄进掌心只读本类常量，改这里不会动迅剑。
 */
public final class CarianGreatswordSwordRenderer {

    /**
     * 原版 {@code item/handheld} 第三人称缩放的两倍。必须和 JSON {@code scale} 相同，
     * {@link #HANDLE_ALONG_BLADE_BLOCKS} 才是 display 之后的真实方块位移。
     */
    private static final float THIRD_PERSON_DISPLAY_SCALE = 1.70f;

    /**
     * 生成物立方体中心 Y。原版手持原点在这里，也就是拳眼里那一点。
     */
    private static final float GENERATED_MESH_CENTER_Y = 0.50f;

    /**
     * 竖贴图柄中心的生成物 Y。与 {@code 工具链/gen_carian_slicer_sword.py} 的 handle 采样一致。
     */
    private static final float HANDLE_LOCAL_Y = 0.156f;

    /**
     * display 缩放之后，沿物品 +Y（刃轴）把柄送到拳眼（方块）。
     * 正值柄进掌心、刃更往外；不要改 JSON translation 来做这件事。
     */
    private static final float HANDLE_ALONG_BLADE_BLOCKS =
            (GENERATED_MESH_CENTER_Y - HANDLE_LOCAL_Y) * THIRD_PERSON_DISPLAY_SCALE;

    private CarianGreatswordSwordRenderer() {
    }

    /**
     * 在当前 PoseStack（已经 translateToHand + 握点）上画出右手那把像素剑。
     */
    public static void renderInRightHand(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            AbstractClientPlayer player
    ) {
        ItemStack swordStack = CarianGreatswordHand.swordStack();
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

    /**
     * 第三人称右手物品姿态 + 生成物原点平移。光轨采样必须和网格走同一套，刃尖才会对上剑。
     */
    public static void applyHeldItemPose(PoseStack poseStack, AbstractClientPlayer player) {
        ItemStack swordStack = CarianGreatswordHand.swordStack();
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
     * 先把生成物立方体中心对到原版拳眼，再沿刃轴滑，让柄（而不是剑身中点）落在掌心。
     */
    private static void applyHandleIntoPalm(PoseStack poseStack) {
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        poseStack.translate(0.0F, HANDLE_ALONG_BLADE_BLOCKS, 0.0F);
    }
}

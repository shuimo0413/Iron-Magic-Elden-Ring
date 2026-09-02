package com.eldenring.spells.client.render.carian;

import com.eldenring.spells.client.CarianPiercerHand;
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
 * 卡利亚贯刺手持网格：贴图与迅剑同一把像素剑（柄的定位不能改），只在柄枢轴上把刃拉长。
 * 缩放 / 柄进掌心 / 刃长只读本类常量，改这里不会动迅剑。
 */
public final class CarianPiercerSwordRenderer {

    /**
     * 第三人称手持缩放。必须和 JSON {@code scale} 相同，也必须和迅剑一样是 1.70，
     * 柄才会以原来的大小进掌心。加长只走 {@link #BLADE_LENGTH_SCALE}。
     */
    private static final float THIRD_PERSON_DISPLAY_SCALE = 1.70f;

    /**
     * 生成物立方体中心 Y。原版手持原点在这里，也就是拳眼里那一点。
     */
    private static final float GENERATED_MESH_CENTER_Y = 0.50f;

    /**
     * 竖贴图柄中心的生成物 Y。与迅剑同一张图、同一采样，禁止改这个来「加长」。
     */
    private static final float HANDLE_LOCAL_Y = 0.156f;

    /**
     * 只沿刃轴（生成物 +Y）相对剑柄拉长。1 = 和迅剑一样长；调大 → 刃更往外、柄不动。
     * 7 格攻击半径用 2.2，大约把刃伸到原来的两倍出头。
     */
    private static final float BLADE_LENGTH_SCALE = 2.20f;

    /**
     * display 缩放之后，沿物品 +Y（刃轴）把柄送到拳眼（方块）。
     * 正值柄进掌心；不要改 JSON translation 来做这件事。
     */
    private static final float HANDLE_ALONG_BLADE_BLOCKS =
            (GENERATED_MESH_CENTER_Y - HANDLE_LOCAL_Y) * THIRD_PERSON_DISPLAY_SCALE;

    private CarianPiercerSwordRenderer() {
    }

    /**
     * 在当前 PoseStack（已经 translateToHand + 握点）上画出右手那把像素剑。
     */
    public static void renderInRightHand(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            AbstractClientPlayer player
    ) {
        ItemStack swordStack = CarianPiercerHand.swordStack();
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
        ItemStack swordStack = CarianPiercerHand.swordStack();
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
     * 握点必须和最初的大剑 / 迅剑同一套：先把立方体中心对到拳眼，再沿刃轴把柄滑进掌心。
     * 刃长缩放只能加在这两步之后，并且绕生成物 {@link #HANDLE_LOCAL_Y} 转；
     * 若先 scale 再滑柄，{@link #HANDLE_ALONG_BLADE_BLOCKS} 会被倍数放大，整把剑飞出掌心。
     */
    private static void applyHandleIntoPalm(PoseStack poseStack) {
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        poseStack.translate(0.0F, HANDLE_ALONG_BLADE_BLOCKS, 0.0F);
        poseStack.translate(0.0F, HANDLE_LOCAL_Y, 0.0F);
        poseStack.scale(1.0F, BLADE_LENGTH_SCALE, 1.0F);
        poseStack.translate(0.0F, -HANDLE_LOCAL_Y, 0.0F);
    }
}

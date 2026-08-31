package com.eldenring.spells.client.render.carian;

import com.eldenring.spells.client.CarianSlicerHand;
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
 * 卡利亚迅剑手持网格：原版生成物（像素挤成立体薄片），但走自发光 RenderType。
 * <p>
 * {@link ItemRenderer#renderStatic} 用 {@code entity_translucent_cull} / cutout，光影（Iris、
 * Complementary 等）会把它编进受光实体缓冲，顶点 {@link LightTexture#FULL_BRIGHT} 被世界光照盖掉，
 * 夜里看起来就是一块不发光的铁。原版 {@code entity_translucent_emissive} 不采 lightmap，Iris 会映射到
 * 自发光 gbuffer，和魔法辉剑 / 海摩锤同一套。
 * <p>
 * 贴图必须绑 {@link InventoryMenu#BLOCK_ATLAS}：生成物 UV 是方块图集坐标，不能绑独立 PNG，
 * 否则会裁到整张图集上的错误一角。物品旁的 {@code carian_slicer_sword_e.png} 是 Iris / OptiFine
 * 自发光后缀图，给仍走物品图集采样的光影当发射贴图。
 */
public final class CarianSlicerSwordRenderer {

    private CarianSlicerSwordRenderer() {
    }

    /**
     * 在当前 PoseStack（已经 translateToHand + 握点）上画出右手那把像素剑。
     */
    public static void renderInRightHand(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            AbstractClientPlayer player
    ) {
        ItemStack swordStack = CarianSlicerHand.swordStack();
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
        poseStack.translate(-0.5F, -0.5F, -0.5F);
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
        ItemStack swordStack = CarianSlicerHand.swordStack();
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
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }
}

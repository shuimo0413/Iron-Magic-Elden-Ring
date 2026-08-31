package com.eldenring.spells.client.render.carian;

import com.eldenring.spells.client.CarianGreatswordHand;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;

/**
 * 把卡利亚大剑画在玩家右手里。第一人称和第三人称共用这一层。
 * 从迅剑层拷出来的独立副本：握点常量只影响大剑，改这里不会动迅剑。
 * <p>
 * PlayerAnimator 第一人称 {@code THIRD_PERSON_MODEL} 仍走 {@code PlayerRenderer}，
 * 但会把渲染层滤成只剩 {@link PlayerItemInHandLayer}。所以本类必须继承它，剑才会跟斩击骨骼走。
 * <p>
 * 贴图是竖直剑（尖在上、柄在下），生成物局部 +Y 就是刃轴。握点对齐原版
 * {@code ItemInHandLayer} 右手变换（XP -90 后刃沿手臂）。
 * 柄进掌心由 {@link CarianGreatswordSwordRenderer} 沿刃轴滑动；往下按进手心和薄片躺平在本层做。
 */
public class CarianGreatswordHandLayer extends PlayerItemInHandLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /**
     * 原版 {@code ItemInHandLayer} 右手侧向偏移（方块）。正值往玩家外侧。
     */
    private static final float VANILLA_HAND_SIDE_OFFSET_BLOCKS = 1.0f / 16.0f;

    /**
     * 原版手持上移（方块）。和侧向、沿臂一起把物品槽从肩枢轴挪到掌心。
     */
    private static final float VANILLA_HAND_UP_OFFSET_BLOCKS = 0.125f;

    /**
     * 从原版槽位再往掌心按（方块）。正值往下按进手心。截图里柄还略高于掌，只补这一小段。
     */
    private static final float PALM_DROP_BLOCKS = 0.10f;

    /**
     * 原版沿手臂到掌心（方块）。{@code XP -90 / YP 180} 之后局部 +Z 朝肩，所以用负值。
     */
    private static final float VANILLA_HAND_ALONG_ARM_OFFSET_BLOCKS = -0.625f;

    /**
     * 把立着的生成物薄片绕手臂轴躺平（度）。90 = 贴图面朝上，俯视能看见整把剑。
     * 必须和迅剑一样用 +45：JSON 的 translation 在这之后才套上，改成 -45 会把整把剑甩到身体侧面。
     * 只调大剑，迅剑在 {@link CarianSlicerHandLayer}。
     */
    private static final float BLADE_FLAT_ROLL_DEGREES = 90.0f;

    /**
     * 生成物 0–1 立方体里护手中心（刃根）。竖贴图，X=中线，Y 来自
     * {@code 工具链/gen_carian_slicer_sword.py} 的 guard 采样。
     */
    private static final float BLADE_ROOT_LOCAL_X = 0.50f;
    private static final float BLADE_ROOT_LOCAL_Y = 0.250f;
    private static final float BLADE_ROOT_LOCAL_Z = 0.50f;

    /**
     * 生成物 0–1 立方体里刃尖。竖贴图尖在高 Y，同样由生成脚本打印。
     */
    private static final float BLADE_TIP_LOCAL_X = 0.50f;
    private static final float BLADE_TIP_LOCAL_Y = 0.953f;
    private static final float BLADE_TIP_LOCAL_Z = 0.50f;

    /**
     * 玩家模型原点相对脚底的下移（方块）。与 {@code LivingEntityRenderer} 的 {@code -1.501} 对齐。
     */
    private static final float PLAYER_MODEL_FEET_OFFSET_BLOCKS = -1.501f;

    public CarianGreatswordHandLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer,
            ItemInHandRenderer itemInHandRenderer
    ) {
        super(renderer, itemInHandRenderer);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (!CarianGreatswordHand.shouldShowSword(player)) {
            return;
        }
        renderSwordInRightHand(poseStack, bufferSource, player);
        recordSlashTrail(player, partialTicks);
    }

    /**
     * 握点与朝向：先 {@code translateToHand} 跟上右手骨骼，再套原版手持槽。
     * 第一人称（PlayerAnimator 第三人称模型）和第三人称都走这里。
     */
    private void renderSwordInRightHand(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            AbstractClientPlayer player
    ) {
        poseStack.pushPose();
        this.getParentModel().translateToHand(HumanoidArm.RIGHT, poseStack);
        applyVanillaRightHandItemSlot(poseStack);
        CarianGreatswordSwordRenderer.renderInRightHand(poseStack, bufferSource, player);
        poseStack.popPose();
    }

    /**
     * 重建一份没有相机的世界姿态，把刃根 / 刃尖从生成物立方体变到世界坐标。
     * 必须和 {@link #renderSwordInRightHand} 走同一套手臂 / 握点 / 物品变换，轨迹才贴剑。
     */
    private void recordSlashTrail(AbstractClientPlayer player, float partialTicks) {
        PoseStack worldPoseStack = new PoseStack();
        Vec3 interpolatedFeet = new Vec3(
                Mth.lerp(partialTicks, player.xo, player.getX()),
                Mth.lerp(partialTicks, player.yo, player.getY()),
                Mth.lerp(partialTicks, player.zo, player.getZ())
        );
        worldPoseStack.translate(interpolatedFeet.x, interpolatedFeet.y, interpolatedFeet.z);
        float entityScale = player.getScale();
        worldPoseStack.scale(entityScale, entityScale, entityScale);
        float bodyYawDegrees = Mth.rotLerp(partialTicks, player.yBodyRotO, player.yBodyRot);
        worldPoseStack.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYawDegrees));
        worldPoseStack.scale(-1.0f, -1.0f, 1.0f);
        worldPoseStack.translate(0.0f, PLAYER_MODEL_FEET_OFFSET_BLOCKS, 0.0f);
        this.getParentModel().translateToHand(HumanoidArm.RIGHT, worldPoseStack);
        applyVanillaRightHandItemSlot(worldPoseStack);
        CarianGreatswordSwordRenderer.applyHeldItemPose(worldPoseStack, player);
        Vec3 bladeRootWorld = CarianGreatswordTrail.transformModelPoint(
                worldPoseStack,
                BLADE_ROOT_LOCAL_X,
                BLADE_ROOT_LOCAL_Y,
                BLADE_ROOT_LOCAL_Z
        );
        Vec3 bladeTipWorld = CarianGreatswordTrail.transformModelPoint(
                worldPoseStack,
                BLADE_TIP_LOCAL_X,
                BLADE_TIP_LOCAL_Y,
                BLADE_TIP_LOCAL_Z
        );
        CarianGreatswordTrail.recordBladePose(player, bladeRootWorld, bladeTipWorld, partialTicks);
    }

    /**
     * 原版右手槽位，再往下按进掌心，并把薄片躺平。
     * JSON 只负责把竖刃转到手臂朝向并缩放；不要用 JSON translation 抬整把剑。
     */
    private static void applyVanillaRightHandItemSlot(PoseStack poseStack) {
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        poseStack.translate(
                VANILLA_HAND_SIDE_OFFSET_BLOCKS,
                VANILLA_HAND_UP_OFFSET_BLOCKS - PALM_DROP_BLOCKS,
                VANILLA_HAND_ALONG_ARM_OFFSET_BLOCKS
        );
        poseStack.mulPose(Axis.ZP.rotationDegrees(BLADE_FLAT_ROLL_DEGREES));
    }
}

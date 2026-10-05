package com.eldenring.spells.client.render.moonblade;

import com.eldenring.spells.client.AdulasMoonbladeHand;
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
 * 把月光剑画在玩家右手里。第一人称和第三人称共用这一层。
 * 从卡利亚大剑层拷出的独立副本；贴图像素形状相同，所以握点 / 刃根刃尖常量也相同。
 * <p>
 * PlayerAnimator 第一人称 {@code THIRD_PERSON_MODEL} 会把渲染层滤成只剩 {@link PlayerItemInHandLayer}，
 * 所以本类必须继承它，剑才会跟斩击骨骼走。
 */
public class AdulasMoonbladeHandLayer extends PlayerItemInHandLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /** 原版 {@code ItemInHandLayer} 右手侧向偏移（方块）。 */
    private static final float VANILLA_HAND_SIDE_OFFSET_BLOCKS = 1.0f / 16.0f;

    /** 原版手持上移（方块）。 */
    private static final float VANILLA_HAND_UP_OFFSET_BLOCKS = 0.125f;

    /** 从原版槽位再往掌心按（方块）。正值往下按进手心。 */
    private static final float PALM_DROP_BLOCKS = 0.10f;

    /** 原版沿手臂到掌心（方块）。{@code XP -90 / YP 180} 之后局部 +Z 朝肩，所以用负值。 */
    private static final float VANILLA_HAND_ALONG_ARM_OFFSET_BLOCKS = -0.625f;

    /** 把立着的生成物薄片绕手臂轴躺平（度）。90 = 贴图面朝上。 */
    private static final float BLADE_FLAT_ROLL_DEGREES = 90.0f;

    /** 生成物 0–1 立方体里护手中心（刃根）。 */
    private static final float BLADE_ROOT_LOCAL_X = 0.50f;
    private static final float BLADE_ROOT_LOCAL_Y = 0.250f;
    private static final float BLADE_ROOT_LOCAL_Z = 0.50f;

    /** 生成物 0–1 立方体里刃尖。 */
    private static final float BLADE_TIP_LOCAL_X = 0.50f;
    private static final float BLADE_TIP_LOCAL_Y = 0.953f;
    private static final float BLADE_TIP_LOCAL_Z = 0.50f;

    /** 玩家模型原点相对脚底的下移（方块）。与 {@code LivingEntityRenderer} 的 {@code -1.501} 对齐。 */
    private static final float PLAYER_MODEL_FEET_OFFSET_BLOCKS = -1.501f;

    public AdulasMoonbladeHandLayer(
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
        if (!AdulasMoonbladeHand.shouldShowSword(player)) {
            return;
        }
        renderSwordInRightHand(poseStack, bufferSource, player);
        recordSlashTrail(player, partialTicks);
    }

    /** 先 {@code translateToHand} 跟上右手骨骼，再套原版手持槽。 */
    private void renderSwordInRightHand(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            AbstractClientPlayer player
    ) {
        poseStack.pushPose();
        this.getParentModel().translateToHand(HumanoidArm.RIGHT, poseStack);
        applyVanillaRightHandItemSlot(poseStack);
        AdulasMoonbladeSwordRenderer.renderInRightHand(poseStack, bufferSource, player);
        poseStack.popPose();
    }

    /**
     * 重建一份没有相机的世界姿态，把刃根 / 刃尖从生成物立方体变到世界坐标，交给光轨。
     * 必须和 {@link #renderSwordInRightHand} 走同一套变换，轨迹才贴剑。
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
        AdulasMoonbladeSwordRenderer.applyHeldItemPose(worldPoseStack, player);
        Vec3 bladeRootWorld = AdulasMoonbladeTrail.transformModelPoint(
                worldPoseStack, BLADE_ROOT_LOCAL_X, BLADE_ROOT_LOCAL_Y, BLADE_ROOT_LOCAL_Z
        );
        Vec3 bladeTipWorld = AdulasMoonbladeTrail.transformModelPoint(
                worldPoseStack, BLADE_TIP_LOCAL_X, BLADE_TIP_LOCAL_Y, BLADE_TIP_LOCAL_Z
        );
        AdulasMoonbladeTrail.recordBladePose(player, bladeRootWorld, bladeTipWorld, partialTicks);
    }

    /** 原版右手槽位，再往下按进掌心，并把薄片躺平。 */
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

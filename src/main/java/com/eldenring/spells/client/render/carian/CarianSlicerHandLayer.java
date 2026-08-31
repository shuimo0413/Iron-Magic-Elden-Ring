package com.eldenring.spells.client.render.carian;

import com.eldenring.spells.client.CarianSlicerHand;
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
 * 把卡利亚迅剑画在玩家右手里。第一人称和第三人称共用这一层。
 * <p>
 * PlayerAnimator 第一人称 {@code THIRD_PERSON_MODEL} 仍走 {@code PlayerRenderer}，
 * 但会把渲染层滤成只剩 {@link PlayerItemInHandLayer}。所以本类必须继承它，剑才会跟斩击骨骼走。
 * <p>
 * 不要走 {@code ItemInHandRenderer#renderItem}：第一人称 pass 里若关掉右手物品，
 * PlayerAnimator 会把那次调用取消，法术书和迅剑一起消失。网格由
 * {@link CarianSlicerSwordRenderer} 用自发光 RenderType 画，光影下才会亮。
 */
public class CarianSlicerHandLayer extends PlayerItemInHandLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /**
     * 剑刃相对小臂的夹角（度）。绕手臂侧向轴转，柄留在掌心。
     * 0 = 贴着小臂平行；45 = 从拳眼斜出去，看起来像握住剑柄。
     * 正负对调会让刃往身前或身后倒。
     */
    private static final float GRIP_ANGLE_FROM_ARM_DEGREES = 55.0f;

    /**
     * 绕刃把生成物薄片从「立着的卡片」躺平（度）。
     * <p>
     * 生成物在物品空间是 XY 面、厚沿 Z。原版第三人称 JSON {@code Y=-90} 把它立在手里；
     * 下面 {@code XP -90 / YP 180} 之后，父空间 +Z 就是刃轴，再转 Z 才会把贴图面转到俯视能看见。
     * JSON 里拧 X=90 不行：那是物品局部欧拉，X 在 Y=-90 之后绕的是刃轴以外的方向，只会把剑挑起来，薄片仍立着。
     * 90 = 完全躺平；-90 = 翻到贴图另一面。改这个数需要重启客户端。
     */
    private static final float BLADE_FLAT_ROLL_DEGREES = 90.0f;

    /**
     * 右手物品侧向偏移（方块）。正值往玩家外侧。略小于原版 1/16，让柄进掌心而不是浮在腕侧。
     */
    private static final float HAND_SIDE_OFFSET_BLOCKS = -0.4f;

    /**
     * 沿手臂朝向的上移（方块）。调大剑更靠近小臂。
     */
    private static final float HAND_UP_OFFSET_BLOCKS = -0.5f;

    /**
     * 沿手指朝向（方块，负值=离开手掌）。调更负则剑更往外。
     */
    private static final float HAND_FORWARD_OFFSET_BLOCKS = -0.7f;

    /**
     * 生成物 0–1 立方体里护手附近（刃根）。贴图是铁剑剪影，护手大约在对角线中点。
     */
    private static final float BLADE_ROOT_LOCAL_X = 0.50f;
    private static final float BLADE_ROOT_LOCAL_Y = 0.50f;
    private static final float BLADE_ROOT_LOCAL_Z = 0.50f;

    /**
     * 生成物 0–1 立方体里刃尖。贴图左上是刃，Minecraft 生成物 Y 朝上所以尖在高 Y、低 X。
     */
    private static final float BLADE_TIP_LOCAL_X = 0.14f;
    private static final float BLADE_TIP_LOCAL_Y = 0.88f;
    private static final float BLADE_TIP_LOCAL_Z = 0.50f;

    /**
     * 玩家模型原点相对脚底的下移（方块）。与 {@code LivingEntityRenderer} 的 {@code -1.501} 对齐。
     */
    private static final float PLAYER_MODEL_FEET_OFFSET_BLOCKS = -1.501f;

    public CarianSlicerHandLayer(
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
        if (!CarianSlicerHand.shouldShowSword(player)) {
            return;
        }
        renderSwordInRightHand(poseStack, bufferSource, player);
        recordSlashTrail(player, partialTicks);
    }

    /**
     * 握点与朝向：先 {@code translateToHand} 跟上右手骨骼，再套同一套旋转 / 平移。
     * 第一人称（PlayerAnimator 第三人称模型）和第三人称都走这里。
     */
    private void renderSwordInRightHand(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            AbstractClientPlayer player
    ) {
        poseStack.pushPose();
        this.getParentModel().translateToHand(HumanoidArm.RIGHT, poseStack);
        applyGripTransforms(poseStack);
        CarianSlicerSwordRenderer.renderInRightHand(poseStack, bufferSource, player);
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
        applyGripTransforms(worldPoseStack);
        CarianSlicerSwordRenderer.applyHeldItemPose(worldPoseStack, player);
        Vec3 bladeRootWorld = CarianSlicerTrail.transformModelPoint(
                worldPoseStack,
                BLADE_ROOT_LOCAL_X,
                BLADE_ROOT_LOCAL_Y,
                BLADE_ROOT_LOCAL_Z
        );
        Vec3 bladeTipWorld = CarianSlicerTrail.transformModelPoint(
                worldPoseStack,
                BLADE_TIP_LOCAL_X,
                BLADE_TIP_LOCAL_Y,
                BLADE_TIP_LOCAL_Z
        );
        CarianSlicerTrail.recordBladePose(player, bladeRootWorld, bladeTipWorld, partialTicks);
    }

    /**
     * 手臂空间里的握点：抬离小臂、原版物品轴向、把立着的薄片躺平。
     */
    private static void applyGripTransforms(PoseStack poseStack) {
        poseStack.mulPose(Axis.XP.rotationDegrees(-GRIP_ANGLE_FROM_ARM_DEGREES));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(BLADE_FLAT_ROLL_DEGREES));
        poseStack.translate(HAND_SIDE_OFFSET_BLOCKS, HAND_UP_OFFSET_BLOCKS, HAND_FORWARD_OFFSET_BLOCKS);
    }
}

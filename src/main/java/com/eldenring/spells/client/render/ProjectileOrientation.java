package com.eldenring.spells.client.render;

import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * 弹道实体朝向工具：把 PoseStack 对齐到当前速度方向（Z 轴朝前）。
 * 后续任意「彗星头 / 箭矢 / 晶簇」渲染都可复用。
 */
public final class ProjectileOrientation {
    private ProjectileOrientation() {
    }

    /**
     * 按实体速度旋转姿态；速度过小时回退到实体自身 yaw/pitch。
     */
    public static void alignPoseToDeltaMovement(PoseStack poseStack, Entity entity, float partialTicks) {
        Vec3 deltaMovement = entity.getDeltaMovement();
        if (deltaMovement.lengthSqr() > 1.0e-6) {
            alignPoseToDirection(poseStack, deltaMovement);
        } else {
            float xRotationDegrees = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
            float yRotationDegrees = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
            poseStack.mulPose(Axis.YP.rotationDegrees(yRotationDegrees));
            poseStack.mulPose(Axis.XP.rotationDegrees(xRotationDegrees));
        }
    }

    /**
     * 按显式世界方向旋转姿态。用于旋飞魔砾等「弹头方向不同于中心实体速度」的渲染。
     * 本地 -Z 轴会对齐到传入的前进方向。
     */
    public static void alignPoseToDirection(PoseStack poseStack, Vec3 direction) {
        if (direction.lengthSqr() <= 1.0e-8) {
            return;
        }
        Vec3 normalizedDirection = direction.normalize();
        double horizontalDistance = normalizedDirection.horizontalDistance();
        float xRotationDegrees = -((float) (Mth.atan2(horizontalDistance, normalizedDirection.y)
                * (double) (180F / (float) Math.PI)) - 90.0F);
        float yRotationDegrees = -((float) (Mth.atan2(normalizedDirection.z, normalizedDirection.x)
                * (double) (180F / (float) Math.PI)) + 90.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRotationDegrees));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRotationDegrees));
    }
}

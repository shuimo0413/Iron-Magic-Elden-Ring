package com.eldenring.spells.mixin;

import com.eldenring.spells.client.CarianGreatswordHand;
import com.eldenring.spells.client.CarianSlicerHand;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 迅剑或大剑挥砍时不要再画手里的法术书 / 卷轴，否则会和像素剑叠在一起。
 */
@Mixin(ItemInHandLayer.class)
public abstract class HideHeldItemDuringCarianSlicerMixin {

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void eldenRingSpells$hideHeldItemDuringCarianSlicer(
            LivingEntity livingEntity,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            HumanoidArm arm,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo callbackInfo
    ) {
        if (CarianSlicerHand.shouldShowSword(livingEntity)
                || CarianGreatswordHand.shouldShowSword(livingEntity)) {
            callbackInfo.cancel();
        }
    }
}

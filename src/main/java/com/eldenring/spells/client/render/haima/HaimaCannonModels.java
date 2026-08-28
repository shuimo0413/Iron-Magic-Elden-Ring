package com.eldenring.spells.client.render.haima;

import com.eldenring.spells.EldenRingSpellsMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * 海摩炮弹立方体模型（单位约 1/16 方块；{@code ModelPart.render} 已换算）。
 * <p>
 * 枢轴在球心。用核心立方 + 三轴加厚 + 切角小面近似实心辉石炮弹，
 * 飞行时再绕轴慢转，避免读成彗星头菱形。
 */
public final class HaimaCannonModels {

    public static final ModelLayerLocation CANNONBALL_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "cannon_of_haima"),
            "main"
    );

    /** 最内层实心核。 */
    public static final String CORE_PART = "core";

    /** 三轴加厚，把正方体挤成更圆的炮弹。 */
    public static final String SHELL_PART = "shell";

    /** 切角小面，让轮廓有晶体棱而不是光滑铁球。 */
    public static final String FACET_PART = "facet";

    public static final ResourceLocation CANNONBALL_BODY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/haima/gavel_body.png");

    public static final ResourceLocation CANNONBALL_GLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/glintstone/comet_glow.png");

    private HaimaCannonModels() {
    }

    /**
     * 构建炮弹网格。中心在原点，直径大约 14 像素（渲染时再整体缩放）。
     */
    public static LayerDefinition createCannonballLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        root.addOrReplaceChild(
                CORE_PART,
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.5F, -4.5F, -4.5F, 9.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                SHELL_PART,
                CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(-3.4F, -5.6F, -3.4F, 6.8F, 11.2F, 6.8F, new CubeDeformation(0.0F))
                        .texOffs(28, 24)
                        .addBox(-5.6F, -3.4F, -3.4F, 11.2F, 6.8F, 6.8F, new CubeDeformation(0.0F))
                        .texOffs(0, 44)
                        .addBox(-3.4F, -3.4F, -5.6F, 6.8F, 6.8F, 11.2F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                FACET_PART,
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-2.4F, -6.1F, -2.4F, 4.8F, 1.2F, 4.8F, new CubeDeformation(0.0F))
                        .texOffs(32, 0)
                        .addBox(-2.4F, 4.9F, -2.4F, 4.8F, 1.2F, 4.8F, new CubeDeformation(0.0F))
                        .texOffs(32, 8)
                        .addBox(-6.1F, -2.4F, -2.4F, 1.2F, 4.8F, 4.8F, new CubeDeformation(0.0F))
                        .texOffs(32, 8)
                        .addBox(4.9F, -2.4F, -2.4F, 1.2F, 4.8F, 4.8F, new CubeDeformation(0.0F))
                        .texOffs(48, 0)
                        .addBox(-2.4F, -2.4F, -6.1F, 4.8F, 4.8F, 1.2F, new CubeDeformation(0.0F))
                        .texOffs(48, 0)
                        .addBox(-2.4F, -2.4F, 4.9F, 4.8F, 4.8F, 1.2F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        return LayerDefinition.create(meshDefinition, 64, 64);
    }
}

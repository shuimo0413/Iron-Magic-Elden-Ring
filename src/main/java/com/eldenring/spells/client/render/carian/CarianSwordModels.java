package com.eldenring.spells.client.render.carian;

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
 * 卡利亚辉剑 Java 立方体模型（单位约 1/16 方块；{@link net.minecraft.client.model.ModelPart#render} 已换算）。
 * <p>
 * 坐标系：枢轴在握柄中下部，+Y 为刃尖方向。
 * 仅卡利亚迅剑使用；魔法辉剑见 {@link MagicGlintbladeModels}。
 */
public final class CarianSwordModels {

    public static final ModelLayerLocation SWORD_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "carian_sword"),
            "main"
    );

    public static final String POMMEL_PART = "pommel";
    public static final String HANDLE_PART = "handle";
    public static final String GUARD_PART = "guard";
    public static final String BLADE_PART = "blade";
    public static final String EDGE_PART = "edge";

    public static final ResourceLocation SWORD_BODY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/carian/sword_body.png");

    public static final ResourceLocation SLASH_CRESCENT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/carian/slash_crescent.png");

    public static final ResourceLocation SWORD_GLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/glintstone/comet_glow.png");

    private CarianSwordModels() {
    }

    /**
     * 构建直剑网格。
     * <ul>
     *   <li>柄沿 +Y，刃尖约 y=24</li>
     *   <li>护手沿 X 拉宽，做出卡利亚直剑的十字锷</li>
     * </ul>
     */
    public static LayerDefinition createSwordLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        root.addOrReplaceChild(
                POMMEL_PART,
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.4F, -3.4F, -1.4F, 2.8F, 2.6F, 2.8F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                HANDLE_PART,
                CubeListBuilder.create()
                        .texOffs(12, 0)
                        .addBox(-0.85F, -1.0F, -0.85F, 1.7F, 7.2F, 1.7F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                GUARD_PART,
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-4.4F, 5.6F, -1.15F, 8.8F, 1.5F, 2.3F, new CubeDeformation(0.0F))
                        .texOffs(0, 20)
                        .addBox(-1.2F, 5.3F, -1.5F, 2.4F, 2.1F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        // 剑身：薄片，沿 Y 拉长
        root.addOrReplaceChild(
                BLADE_PART,
                CubeListBuilder.create()
                        .texOffs(24, 0)
                        .addBox(-0.7F, 7.2F, -0.28F, 1.4F, 15.4F, 0.56F, new CubeDeformation(0.0F))
                        .texOffs(32, 0)
                        .addBox(-0.45F, 22.4F, -0.22F, 0.9F, 2.6F, 0.44F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        // 略大一圈的刃锋，渲染时用更亮的颜色
        root.addOrReplaceChild(
                EDGE_PART,
                CubeListBuilder.create()
                        .texOffs(40, 0)
                        .addBox(-0.95F, 7.0F, -0.12F, 1.9F, 18.2F, 0.24F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        return LayerDefinition.create(meshDefinition, 64, 64);
    }
}

package com.eldenring.spells.client.render.glintstone;

import com.eldenring.spells.EldenRingSpellsMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 辉石系「彗星头」共用模型层定义。
 * <p>
 * 形状：沿飞行方向（-Z）前后收尖的菱形晶核（截面绕 Z 转 45°，读作立体菱形）。
 * 其它辉石弹道可 bake 同一 {@link #COMET_HEAD_LAYER} 或在此追加变体。
 */
public final class GlintstoneCometModels {
    public static final ModelLayerLocation COMET_HEAD_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "glintstone_comet_head"),
            "main"
    );

    public static final ResourceLocation COMET_HEAD_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/glintstone/comet_head.png");

    public static final ResourceLocation COMET_GLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/glintstone/comet_glow.png");

    /** 连续光轨白色透明度纹理；运行时由各法术的蓝绿色顶点颜色着色。 */
    public static final ResourceLocation TRAIL_BEAM_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/glintstone/trail_beam.png");

    /** 菱形截面：绕飞行轴转 45°，方块四角变成菱形尖角。 */
    private static final float DIAMOND_SECTION_ROTATION_Z = Mth.HALF_PI * 0.5f;

    private GlintstoneCometModels() {
    }

    /**
     * 单位约等于像素；渲染时再整体 scale。
     * 坐标系：-Z 为飞行前方（配合 {@link com.eldenring.spells.client.render.ProjectileOrientation}）。
     */
    public static LayerDefinition createCometHeadLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        // mid：最宽处，菱形主体
        root.addOrReplaceChild(
                "mid",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.55F, -1.55F, -1.35F, 3.1F, 3.1F, 2.7F, new CubeDeformation(0.0F)),
                PartPose.rotation(0.0F, 0.0F, DIAMOND_SECTION_ROTATION_Z)
        );
        // front / front_tip：朝 -Z 逐级收尖
        root.addOrReplaceChild(
                "front",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.95F, -0.95F, -3.15F, 1.9F, 1.9F, 1.9F, new CubeDeformation(0.0F)),
                PartPose.rotation(0.0F, 0.0F, DIAMOND_SECTION_ROTATION_Z)
        );
        root.addOrReplaceChild(
                "front_tip",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.4F, -0.4F, -4.35F, 0.8F, 0.8F, 1.3F, new CubeDeformation(0.0F)),
                PartPose.rotation(0.0F, 0.0F, DIAMOND_SECTION_ROTATION_Z)
        );
        // back / back_tip：朝 +Z 对称收尖，整体读作菱形八面体
        root.addOrReplaceChild(
                "back",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.95F, -0.95F, 1.35F, 1.9F, 1.9F, 1.9F, new CubeDeformation(0.0F)),
                PartPose.rotation(0.0F, 0.0F, DIAMOND_SECTION_ROTATION_Z)
        );
        root.addOrReplaceChild(
                "back_tip",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.4F, -0.4F, 3.15F, 0.8F, 0.8F, 1.3F, new CubeDeformation(0.0F)),
                PartPose.rotation(0.0F, 0.0F, DIAMOND_SECTION_ROTATION_Z)
        );

        return LayerDefinition.create(meshDefinition, 16, 16);
    }
}

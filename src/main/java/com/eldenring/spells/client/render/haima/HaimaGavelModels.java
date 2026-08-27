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
 * 海摩大槌 Java 立方体模型（单位约 1/16 方块；{@link ModelPart#render} 已换算，渲染器勿再除 16）。
 * <p>
 * 坐标系：枢轴在握柄中下部，+Y 为柄朝向锤头。
 * 锤头是<strong>平头横圆柱</strong>：轴沿本地 +Z（身前方向），两端为平面圆端面。
 * 第三人称从背后看时看到的是圆端面，而不是左右拉长的横杠（那种读作「平躺」）。
 * 渲染器绕 X 俯仰：0° 柄竖直，正角向前抡砸。
 */
public final class HaimaGavelModels {

    public static final ModelLayerLocation GAVEL_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "gavel_of_haima"),
            "main"
    );

    /** 握柄零件名。 */
    public static final String HANDLE_PART = "handle";

    /** 锤头圆柱主体（轴沿 Z）。 */
    public static final String HEAD_PART = "head";

    /** 绕圆柱的环箍（沿 Z 分布）。 */
    public static final String HEAD_BAND_PART = "head_band";

    /** 前后平头端面盖。 */
    public static final String HEAD_CAP_PART = "head_cap";

    public static final ResourceLocation GAVEL_BODY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/haima/gavel_body.png");

    public static final ResourceLocation GAVEL_GLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/glintstone/comet_glow.png");

    private HaimaGavelModels() {
    }

    /**
     * 构建巨锤网格。
     * <ul>
     *   <li>柄沿 +Y</li>
     *   <li>锤头：Z 向长约 14、XY 截面直径约 12 的平头圆柱（轴朝身前）</li>
     * </ul>
     */
    public static LayerDefinition createGavelLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        // 握柄：细长杆，顶接到圆柱侧面
        root.addOrReplaceChild(
                HANDLE_PART,
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, -1.5F, -1.0F, 2.0F, 16.5F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(8, 0)
                        .addBox(-1.5F, -3.0F, -1.5F, 3.0F, 2.2F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(8, 8)
                        .addBox(-1.3F, 7.0F, -1.3F, 2.6F, 1.3F, 2.6F, new CubeDeformation(0.0F))
                        .texOffs(8, 14)
                        .addBox(-1.55F, 14.2F, -1.55F, 3.1F, 1.8F, 3.1F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        // 平头圆柱：轴 = Z（前后），截面 ≈ 圆在 XY
        // 中心约 y=21；从背后看是圆端面，不再是左右横杠
        root.addOrReplaceChild(
                HEAD_PART,
                CubeListBuilder.create()
                        // 主筒：XY 12×12，Z 长 14
                        .texOffs(0, 24)
                        .addBox(-6.0F, 15.5F, -7.0F, 12.0F, 12.0F, 14.0F, new CubeDeformation(0.0F))
                        // X 向收一点加厚圆感
                        .texOffs(0, 24)
                        .addBox(-4.6F, 14.7F, -7.0F, 9.2F, 13.6F, 14.0F, new CubeDeformation(0.0F))
                        // Y 向收一点
                        .texOffs(0, 24)
                        .addBox(-6.8F, 16.7F, -7.0F, 13.6F, 9.2F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        // 环箍：垂直于 Z 轴（前后不同位置），略鼓出
        root.addOrReplaceChild(
                HEAD_BAND_PART,
                CubeListBuilder.create()
                        .texOffs(0, 48)
                        .addBox(-6.6F, 14.9F, -5.2F, 13.2F, 13.2F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 48)
                        .addBox(-6.6F, 14.9F, -1.0F, 13.2F, 13.2F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 48)
                        .addBox(-6.6F, 14.9F, 3.2F, 13.2F, 13.2F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        // 前后平头端面（±Z）
        root.addOrReplaceChild(
                HEAD_CAP_PART,
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-5.8F, 15.7F, -7.7F, 11.6F, 11.6F, 0.9F, new CubeDeformation(0.0F))
                        .texOffs(32, 0)
                        .addBox(-5.8F, 15.7F, 6.8F, 11.6F, 11.6F, 0.9F, new CubeDeformation(0.0F))
                        .texOffs(32, 22)
                        .addBox(-5.0F, 16.5F, -7.95F, 10.0F, 10.0F, 0.5F, new CubeDeformation(0.0F))
                        .texOffs(32, 22)
                        .addBox(-5.0F, 16.5F, 7.45F, 10.0F, 10.0F, 0.5F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        return LayerDefinition.create(meshDefinition, 64, 64);
    }
}

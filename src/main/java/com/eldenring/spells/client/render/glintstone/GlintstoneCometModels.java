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
 * 辉石系弹头模型层定义。
 * <p>
 * {@link #COMET_HEAD_LAYER}：沿飞行方向（-Z）前后收尖的菱形晶核。
 * {@link #SPIKED_COMET_HEAD_LAYER}：帚星专用带刺不规则晶簇（核 + 尖刺两组）。
 */
public final class GlintstoneCometModels {
    public static final ModelLayerLocation COMET_HEAD_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "glintstone_comet_head"),
            "main"
    );

    /**
     * 帚星专用：不规则晶核 + 多向尖刺。子节点名必须是 {@code core} / {@code spikes}，
     * 绘制时核更深、刺更亮。
     */
    public static final ModelLayerLocation SPIKED_COMET_HEAD_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "glintstone_spiked_comet_head"),
            "main"
    );

    /** 刺簇模型里不规则核心组的零件名。 */
    public static final String SPIKED_CLUSTER_CORE_PART = "core";

    /** 刺簇模型里尖刺组的零件名。 */
    public static final String SPIKED_CLUSTER_SPIKES_PART = "spikes";

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

    /**
     * 帚星带刺晶簇：错旋转的粗核 + 十余根长短不一的尖刺。
     * <p>
     * 坐标系与菱形彗星头相同（-Z 前方）。多数刺朝 +Z / 径向戳出，避免再读成光滑梭子。
     * 单位约等于像素，渲染时再整体 scale。
     */
    public static LayerDefinition createSpikedCometHeadLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();
        PartDefinition core = root.addOrReplaceChild(
                SPIKED_CLUSTER_CORE_PART,
                CubeListBuilder.create(),
                PartPose.ZERO
        );
        core.addOrReplaceChild(
                "chunk_a",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.70F, -1.45F, -1.55F, 3.40F, 2.90F, 3.20F, new CubeDeformation(0.0F)),
                PartPose.rotation(0.18F, 0.31F, 0.42F)
        );
        core.addOrReplaceChild(
                "chunk_b",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.15F, -1.80F, -1.25F, 2.50F, 3.50F, 2.70F, new CubeDeformation(0.0F)),
                PartPose.rotation(0.55F, -0.22F, -0.38F)
        );
        core.addOrReplaceChild(
                "chunk_c",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.05F, -1.00F, -1.85F, 2.10F, 2.05F, 3.35F, new CubeDeformation(0.0F)),
                PartPose.rotation(-0.28F, 0.62F, 0.15F)
        );

        PartDefinition spikes = root.addOrReplaceChild(
                SPIKED_CLUSTER_SPIKES_PART,
                CubeListBuilder.create(),
                PartPose.ZERO
        );
        // 默认刺沿 +Y；PartPose 转到各个方向。偏大的 X 旋转会把刺甩向 +Z（飞行后方）。
        addCrystalSpike(spikes, "spike_back_long", 1.72F, 0.18F, -0.12F, 5.90F, 0.30F, 0.85F);
        addCrystalSpike(spikes, "spike_back_a", 1.18F, 0.22F, 0.16F, 5.20F, 0.42F, 1.05F);
        addCrystalSpike(spikes, "spike_back_b", 1.32F, 1.08F, -0.10F, 4.70F, 0.36F, 1.00F);
        addCrystalSpike(spikes, "spike_back_c", 1.26F, -1.12F, 0.20F, 5.00F, 0.38F, 1.02F);
        addCrystalSpike(spikes, "spike_back_d", 1.52F, 2.08F, 0.08F, 4.15F, 0.32F, 0.92F);
        addCrystalSpike(spikes, "spike_back_e", 1.44F, -2.18F, -0.16F, 4.35F, 0.34F, 0.95F);
        addCrystalSpike(spikes, "spike_side_a", 0.58F, 0.42F, 0.82F, 3.55F, 0.40F, 1.18F);
        addCrystalSpike(spikes, "spike_side_b", 0.66F, 2.38F, -0.48F, 3.25F, 0.36F, 1.12F);
        addCrystalSpike(spikes, "spike_side_c", -0.42F, 1.18F, 0.28F, 3.70F, 0.34F, 1.08F);
        addCrystalSpike(spikes, "spike_side_d", 0.22F, -0.82F, 1.12F, 3.05F, 0.38F, 1.20F);
        addCrystalSpike(spikes, "spike_short_a", 0.92F, 3.48F, 0.58F, 2.70F, 0.30F, 1.00F);
        addCrystalSpike(spikes, "spike_front_nub", -0.72F, -1.76F, 0.24F, 2.45F, 0.32F, 0.98F);

        return LayerDefinition.create(meshDefinition, 16, 16);
    }

    /**
     * 一根从核表面伸出的细长晶刺。盒子沿 +Y，再按弧度旋转到目标朝向。
     *
     * @param lengthUnits     刺长（模型单位）；调大 → 刺更远
     * @param thicknessUnits  截面边长（模型单位）；调大 → 更粗、更像晶柱
     * @param baseOffsetUnits 根部离原点距离，避免刺插进核心正中
     */
    private static void addCrystalSpike(
            PartDefinition spikes,
            String spikeName,
            float rotationXRadians,
            float rotationYRadians,
            float rotationZRadians,
            float lengthUnits,
            float thicknessUnits,
            float baseOffsetUnits
    ) {
        float halfThickness = thicknessUnits * 0.5F;
        spikes.addOrReplaceChild(
                spikeName,
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(
                                -halfThickness,
                                baseOffsetUnits,
                                -halfThickness,
                                thicknessUnits,
                                lengthUnits,
                                thicknessUnits,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.rotation(rotationXRadians, rotationYRadians, rotationZRadians)
        );
    }
}

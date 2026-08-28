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
import net.minecraft.util.Mth;

/**
 * 魔法辉剑立方体网格。源模型是 {@code 工具链/magic_glintblade.bbmodel}（Blockbench 模组版实体）。
 * <p>
 * 坐标系与迅剑相同：枢轴在握柄中下，+Y 为刃尖。不要用 Blockbench 的 Java 导出（它会 Y 翻转并偏 24）。
 * 单位约 1/16 方块；{@link net.minecraft.client.model.ModelPart#render} 已换算。
 */
public final class MagicGlintbladeModels {

    public static final ModelLayerLocation GLINTBLADE_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "magic_glintblade"),
            "main"
    );

    public static final String POMMEL_PART = "pommel";
    public static final String HANDLE_PART = "handle";
    public static final String GUARD_PART = "guard";
    public static final String BLADE_PART = "blade";
    public static final String RIDGE_PART = "ridge";
    public static final String EDGE_PART = "edge";

    public static final ResourceLocation GLINTBLADE_BODY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/carian/glintblade_body.png");

    public static final ResourceLocation GLINTBLADE_GLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/glintstone/comet_glow.png");

    /**
     * 护手左右翼上翘角（度）。调大 → 锷尖更朝刃；调小 → 更平的十字锷。
     */
    private static final float GUARD_WING_SWEEP_DEGREES = 12.0f;

    private MagicGlintbladeModels() {
    }

    /**
     * 构建辉剑网格。贴图 64×64，UV 分区：柄 0,0 / 柄头 16,0 / 护手 32,0 / 刃 0,16 / 中脊 16,16 / 刃口 24,16。
     */
    public static LayerDefinition createGlintbladeLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        root.addOrReplaceChild(
                POMMEL_PART,
                CubeListBuilder.create()
                        .texOffs(16, 0)
                        .addBox(-1.35F, -3.55F, -1.35F, 2.7F, 2.8F, 2.7F, new CubeDeformation(0.0F))
                        .texOffs(16, 0)
                        .addBox(-1.65F, -3.25F, -1.0F, 3.3F, 2.2F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 0)
                        .addBox(-1.0F, -3.75F, -1.0F, 2.0F, 3.2F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 0)
                        .addBox(-1.0F, -3.25F, -1.65F, 2.0F, 2.2F, 3.3F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                HANDLE_PART,
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.6F, -1.0F, -0.6F, 1.2F, 7.05F, 1.2F, new CubeDeformation(0.0F))
                        .texOffs(0, 0)
                        .addBox(-0.8F, -1.0F, -0.42F, 1.6F, 7.05F, 0.84F, new CubeDeformation(0.0F))
                        .texOffs(0, 0)
                        .addBox(-0.42F, -1.0F, -0.8F, 0.84F, 7.05F, 1.6F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        PartDefinition guard = root.addOrReplaceChild(
                GUARD_PART,
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-4.2F, 6.05F, -0.55F, 8.4F, 1.1F, 1.1F, new CubeDeformation(0.0F))
                        .texOffs(32, 0)
                        .addBox(-1.15F, 5.85F, -0.75F, 2.3F, 1.55F, 1.5F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );
        float wingSweepRadians = GUARD_WING_SWEEP_DEGREES * Mth.DEG_TO_RAD;
        guard.addOrReplaceChild(
                "guard_left",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-1.7F, -0.4F, -0.45F, 2.2F, 0.8F, 0.9F, new CubeDeformation(0.0F))
                        .texOffs(32, 0)
                        .addBox(-2.05F, -0.6F, -0.55F, 1.0F, 1.2F, 1.1F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, 6.55F, 0.0F, 0.0F, 0.0F, -wingSweepRadians)
        );
        guard.addOrReplaceChild(
                "guard_right",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-0.5F, -0.4F, -0.45F, 2.2F, 0.8F, 0.9F, new CubeDeformation(0.0F))
                        .texOffs(32, 0)
                        .addBox(1.05F, -0.6F, -0.55F, 1.0F, 1.2F, 1.1F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, 6.55F, 0.0F, 0.0F, 0.0F, wingSweepRadians)
        );

        root.addOrReplaceChild(
                BLADE_PART,
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-1.25F, 7.35F, -0.3F, 2.5F, 8.85F, 0.6F, new CubeDeformation(0.0F))
                        .texOffs(0, 16)
                        .addBox(-1.15F, 15.6F, -0.28F, 2.3F, 4.8F, 0.56F, new CubeDeformation(0.0F))
                        .texOffs(0, 16)
                        .addBox(-0.75F, 19.8F, -0.22F, 1.5F, 3.0F, 0.44F, new CubeDeformation(0.0F))
                        .texOffs(0, 16)
                        .addBox(-0.32F, 22.4F, -0.14F, 0.64F, 2.2F, 0.28F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                RIDGE_PART,
                CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(-0.18F, 7.35F, -0.5F, 0.36F, 15.45F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 16)
                        .addBox(-0.1F, 22.6F, -0.3F, 0.2F, 2.2F, 0.6F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                EDGE_PART,
                CubeListBuilder.create()
                        .texOffs(24, 16)
                        .addBox(-1.4F, 7.35F, -0.1F, 2.8F, 8.85F, 0.2F, new CubeDeformation(0.0F))
                        .texOffs(24, 16)
                        .addBox(-1.28F, 15.6F, -0.09F, 2.56F, 4.8F, 0.18F, new CubeDeformation(0.0F))
                        .texOffs(24, 16)
                        .addBox(-0.88F, 19.8F, -0.08F, 1.76F, 3.0F, 0.16F, new CubeDeformation(0.0F))
                        .texOffs(24, 16)
                        .addBox(-0.4F, 22.4F, -0.06F, 0.8F, 2.3F, 0.12F, new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        return LayerDefinition.create(meshDefinition, 64, 64);
    }
}

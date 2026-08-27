package com.eldenring.spells.entity;

import com.eldenring.spells.EldenRingSpellsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 辉石彗星头外观：缩放、颜色、贴图。放在 {@code entity} 包，避免弹道实体 import {@code client.render}。
 * <p>
 * 颜色与尺寸写死在各弹道类；不进 toml。
 */
public record GlintstoneVisualStyle(
        float bodyScaleRadial,
        float bodyScaleAlongFlight,
        float glowScale,
        float glowPulseAmplitude,
        float glowSpinDegreesPerTick,
        float glowAlongFlightScale,
        int coreColorArgb,
        int glowColorArgb,
        int spikeColorArgb,
        ResourceLocation bodyTexture,
        ResourceLocation glowTexture,
        boolean usesSpikedCrystalCluster,
        float clusterSpinDegreesPerTick
) {
    /** 菱形晶核贴图。路径与客户端模型层共用，实体侧只持有 ResourceLocation。 */
    public static final ResourceLocation COMET_HEAD_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/glintstone/comet_head.png");

    /** 朝向相机的光晕贴图。 */
    public static final ResourceLocation COMET_GLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "textures/entity/glintstone/comet_glow.png");

    /** 兼容旧调用：均匀缩放时的等效整体缩放。 */
    public float bodyScale() {
        return bodyScaleRadial;
    }

    /**
     * 用 0–1 浮点色构造均匀缩放样式（流星 / 旋飞等未改剪影的弹道）。
     */
    public static GlintstoneVisualStyle fromFloatColors(
            float bodyScale,
            float glowScale,
            float glowPulseAmplitude,
            float glowSpinDegreesPerTick,
            float coreRed, float coreGreen, float coreBlue,
            float glowRed, float glowGreen, float glowBlue, float glowAlpha
    ) {
        return anisotropic(
                bodyScale,
                bodyScale,
                glowScale,
                glowPulseAmplitude,
                glowSpinDegreesPerTick,
                1.0f,
                coreRed, coreGreen, coreBlue,
                glowRed, glowGreen, glowBlue, glowAlpha
        );
    }

    /**
     * 各向异性菱形晶核（魔砾 / 迅魔砾 / 大魔砾 / 辉石彗星）。
     *
     * @param glowAlongFlightScale 1 = 球形光晕；大于 1 沿速度拉成残影
     */
    public static GlintstoneVisualStyle anisotropic(
            float bodyScaleRadial,
            float bodyScaleAlongFlight,
            float glowScale,
            float glowPulseAmplitude,
            float glowSpinDegreesPerTick,
            float glowAlongFlightScale,
            float coreRed, float coreGreen, float coreBlue,
            float glowRed, float glowGreen, float glowBlue, float glowAlpha
    ) {
        int coreColor = packRgb(coreRed, coreGreen, coreBlue, 1.0f);
        return new GlintstoneVisualStyle(
                bodyScaleRadial,
                bodyScaleAlongFlight,
                glowScale,
                glowPulseAmplitude,
                glowSpinDegreesPerTick,
                glowAlongFlightScale,
                coreColor,
                packRgb(glowRed, glowGreen, glowBlue, glowAlpha),
                coreColor,
                COMET_HEAD_TEXTURE,
                COMET_GLOW_TEXTURE,
                false,
                0.0f
        );
    }

    /**
     * 帚星带刺晶簇：核更深、刺更亮，绕飞行轴慢转。
     */
    public static GlintstoneVisualStyle spikedCluster(
            float bodyScale,
            float glowScale,
            float glowPulseAmplitude,
            float glowSpinDegreesPerTick,
            float glowAlongFlightScale,
            float clusterSpinDegreesPerTick,
            float coreRed, float coreGreen, float coreBlue,
            float spikeRed, float spikeGreen, float spikeBlue,
            float glowRed, float glowGreen, float glowBlue, float glowAlpha
    ) {
        return new GlintstoneVisualStyle(
                bodyScale,
                bodyScale,
                glowScale,
                glowPulseAmplitude,
                glowSpinDegreesPerTick,
                glowAlongFlightScale,
                packRgb(coreRed, coreGreen, coreBlue, 1.0f),
                packRgb(glowRed, glowGreen, glowBlue, glowAlpha),
                packRgb(spikeRed, spikeGreen, spikeBlue, 1.0f),
                COMET_HEAD_TEXTURE,
                COMET_GLOW_TEXTURE,
                true,
                clusterSpinDegreesPerTick
        );
    }

    private static int packRgb(float red, float green, float blue, float alpha) {
        int packedAlpha = Mth.clamp((int) (alpha * 255.0f), 0, 255);
        int packedRed = Mth.clamp((int) (red * 255.0f), 0, 255);
        int packedGreen = Mth.clamp((int) (green * 255.0f), 0, 255);
        int packedBlue = Mth.clamp((int) (blue * 255.0f), 0, 255);
        return (packedAlpha << 24) | (packedRed << 16) | (packedGreen << 8) | packedBlue;
    }
}

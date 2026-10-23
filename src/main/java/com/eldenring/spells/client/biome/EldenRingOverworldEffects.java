package com.eldenring.spells.client.biome;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * 主世界 DimensionSpecialEffects：完全沿用原版 {@link DimensionSpecialEffects.OverworldEffects}（天空、云、雾不变），
 * 只重写 NeoForge 扩展 {@code adjustLightmapColors}，在魔法之湖里把光照贴图往冷灰蓝提亮。
 * <p>
 * 只改客户端光照贴图颜色：服务端光照、刷怪判定、F3 的 sky/block 光照数值都不变。
 * 由 {@code EldenRingSpellsClient} 在 {@code RegisterDimensionSpecialEffectsEvent} 里注册到
 * {@code minecraft:overworld}，覆盖原版实例；若其它模组也覆盖同一键，后注册者生效。
 */
public final class EldenRingOverworldEffects extends DimensionSpecialEffects.OverworldEffects {

    /**
     * 完全处在湖区、天空光满格时，暗部颜色朝目标色靠拢的比例 [0, 1]。
     * 调大 → 夜晚更亮、画面更「灰」更低对比；调小 → 更接近原版夜色。
     */
    private static final float MAX_LIGHTMAP_LIFT = 0.35f;

    /**
     * 天空光为 0 的像素（洞穴、室内深处）保留的提亮权重 [0, 1]。
     * 天空光越高权重越接近 1；调大 → 湖底洞穴也明显变亮。
     */
    private static final float CAVE_LIFT_WEIGHT = 0.3f;

    /** 提亮目标色（线性 RGB [0, 1]）：清冷灰蓝的漫射天光。 */
    private static final float COLD_AMBIENT_TARGET_RED = 0.78f;
    private static final float COLD_AMBIENT_TARGET_GREEN = 0.84f;
    private static final float COLD_AMBIENT_TARGET_BLUE = 0.95f;

    /** 光照贴图天空光轴的最大下标（16×16 贴图，0–15）。 */
    private static final float LIGHTMAP_MAX_INDEX = 15f;

    @Override
    public void adjustLightmapColors(
            ClientLevel level,
            float partialTicks,
            float skyDarken,
            float blockLightRedFlicker,
            float skyLight,
            int pixelX,
            int pixelY,
            Vector3f colors
    ) {
        float magicLakeBlend = MagicLakeAmbientLight.getBlend(partialTicks);
        if (magicLakeBlend <= 0f) {
            return;
        }
        float skyLightWeight = CAVE_LIFT_WEIGHT + (1f - CAVE_LIFT_WEIGHT) * (pixelY / LIGHTMAP_MAX_INDEX);
        float liftAmount = MAX_LIGHTMAP_LIFT * magicLakeBlend * skyLightWeight;

        // 只抬不压：已比目标亮的通道（白天、火把旁）保持原值，所以暗部抬得多、亮部几乎不变
        colors.set(
                liftChannel(colors.x(), COLD_AMBIENT_TARGET_RED, liftAmount),
                liftChannel(colors.y(), COLD_AMBIENT_TARGET_GREEN, liftAmount),
                liftChannel(colors.z(), COLD_AMBIENT_TARGET_BLUE, liftAmount)
        );
    }

    private static float liftChannel(float originalChannel, float targetChannel, float liftAmount) {
        float liftedChannel = Mth.lerp(liftAmount, originalChannel, targetChannel);
        return Mth.clamp(Math.max(originalChannel, liftedChannel), 0f, 1f);
    }
}

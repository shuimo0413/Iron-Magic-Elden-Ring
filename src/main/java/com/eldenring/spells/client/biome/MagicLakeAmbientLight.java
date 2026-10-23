package com.eldenring.spells.client.biome;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModBiomes;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 魔法之湖环境光混合系数（0 = 不在湖区，1 = 完全在湖区），供 {@link EldenRingOverworldEffects} 提亮光照贴图。
 * <p>
 * 光照贴图是全局的一张 16×16 纹理，没法逐方块区分群系，所以按「相机附近处于魔法之湖的比例」整体混合：
 * 每 tick 在相机位置及周围 4 个点采样群系，命中比例作为目标值，再指数逼近，
 * 进出湖区约 1–2 秒渐变，不会在群系边界一步跳亮。纯客户端视觉，不影响逻辑光照。
 */
@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID, value = Dist.CLIENT)
public final class MagicLakeAmbientLight {

    /**
     * 周围采样点离相机的水平距离（方块）。调大 → 靠近湖区边缘时就开始渐亮、过渡带更宽；
     * 调小 → 更贴近相机脚下的真实群系。
     */
    private static final int SAMPLE_OFFSET_BLOCKS = 12;

    /**
     * 每 tick 向目标值逼近的比例 (0, 1]。0.08 ≈ 1.8 秒达到 95%。调大 → 切换更快；调小 → 更柔和。
     */
    private static final float BLEND_APPROACH_PER_TICK = 0.08f;

    /** 低于此值直接归零，避免离开湖区后长时间残留极小的提亮、每帧白算。 */
    private static final float BLEND_ZERO_EPSILON = 0.002f;

    /** 采样点：相机正下方 + 东南西北四个偏移。 */
    private static final int[][] SAMPLE_OFFSETS_XZ = {
            {0, 0},
            {SAMPLE_OFFSET_BLOCKS, 0},
            {-SAMPLE_OFFSET_BLOCKS, 0},
            {0, SAMPLE_OFFSET_BLOCKS},
            {0, -SAMPLE_OFFSET_BLOCKS}
    };

    private static float previousTickBlend;
    private static float currentTickBlend;

    private MagicLakeAmbientLight() {
    }

    /**
     * 当前帧的混合系数，按 partialTicks 在上一 tick 与本 tick 之间插值。
     */
    public static float getBlend(float partialTicks) {
        return Mth.lerp(partialTicks, previousTickBlend, currentTickBlend);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        previousTickBlend = currentTickBlend;

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            previousTickBlend = 0f;
            currentTickBlend = 0f;
            return;
        }

        float targetBlend = sampleMagicLakeFraction(level, minecraft.gameRenderer.getMainCamera());
        currentTickBlend += (targetBlend - currentTickBlend) * BLEND_APPROACH_PER_TICK;
        if (targetBlend == 0f && currentTickBlend < BLEND_ZERO_EPSILON) {
            currentTickBlend = 0f;
        }
    }

    /**
     * 采样点中位于魔法之湖的比例 [0, 1]。相机尚未初始化时退回玩家位置。
     */
    private static float sampleMagicLakeFraction(ClientLevel level, Camera camera) {
        BlockPos centerPos;
        if (camera.isInitialized()) {
            centerPos = camera.getBlockPosition();
        } else if (Minecraft.getInstance().player != null) {
            centerPos = Minecraft.getInstance().player.blockPosition();
        } else {
            return 0f;
        }

        BlockPos.MutableBlockPos samplePos = new BlockPos.MutableBlockPos();
        int magicLakeSampleCount = 0;
        for (int[] offsetXZ : SAMPLE_OFFSETS_XZ) {
            samplePos.set(centerPos.getX() + offsetXZ[0], centerPos.getY(), centerPos.getZ() + offsetXZ[1]);
            if (level.getBiome(samplePos).is(ModBiomes.MAGIC_LAKE)) {
                magicLakeSampleCount++;
            }
        }
        return magicLakeSampleCount / (float) SAMPLE_OFFSETS_XZ.length;
    }
}

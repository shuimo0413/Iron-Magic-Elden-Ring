package com.eldenring.spells.client.render.carian;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.client.render.glintstone.GlintstoneTrailRenderTypes;
import com.eldenring.spells.client.render.glintstone.GlintstoneTrailRenderer;
import com.eldenring.spells.entity.GlintstoneTrailStyle;
import com.eldenring.spells.spell.fx.CarianSlicerFx;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 卡利亚贯刺挥砍光轨：从迅剑光轨拷出的独立副本。
 * 采样发生在 {@link CarianPiercerHandLayer}。星星粒子仍走 {@link CarianSlicerFx}。
 */
@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID, value = Dist.CLIENT)
public final class CarianPiercerTrail {

    /**
     * 刃尖路径光带。长度 / 半宽单位方块；比迅剑更长更宽，跟上 7 格大剑。
     */
    private static final GlintstoneTrailStyle TIP_TRAIL_STYLE = new GlintstoneTrailStyle(
            4.8,
            0.18f,
            0.030f,
            0.0f,
            0.0f,
            28,
            GlintstoneTrailStyle.HelixStyle.NONE,
            true,
            false
    );

    /** 光轨外辉。卡利亚宝蓝，略亮于魔法辉剑飞行尾。 */
    private static final int TRAIL_GLOW_COLOR_ARGB = 0xCC3A78F0;

    /** 光轨亮核。 */
    private static final int TRAIL_CORE_COLOR_ARGB = 0xF0E8FFFF;

    /** 刀光扫面（刃根→刃尖四边形）颜色。 */
    private static final int SWEEP_COLOR_ARGB = 0xBB5A90FF;

    /**
     * 相邻采样点刃尖位移超过此值（方块）视为换刀 / 传送，清空旧弧，避免穿过身体拉线。
     */
    private static final double SLASH_RESET_DISTANCE_BLOCKS = 4.2;

    /**
     * 停手后还继续画几 tick，让最后一刀的弧淡完。
     */
    private static final int TRAIL_LINGER_TICKS = 3;

    /** 刀光扫面最多保留的刃位姿。调大 → 弧更长更密。 */
    private static final int MAX_SWEEP_SAMPLE_COUNT = 22;

    /**
     * 沿大剑刃刷细闪的点数。比迅剑默认 3 更密，才能铺满加长的刃。
     */
    private static final int BLADE_STAR_SAMPLE_COUNT = 6;

    private static final Map<UUID, PlayerSlashTrail> TRAILS_BY_PLAYER = new HashMap<>();

    private CarianPiercerTrail() {
    }

    /**
     * 本帧剑已经跟着手画完：记下刃根 / 刃尖，供下一 tick 刷星星、本帧画 ribbon。
     */
    public static void recordBladePose(
            AbstractClientPlayer player,
            Vec3 bladeRootWorld,
            Vec3 bladeTipWorld,
            float partialTick
    ) {
        Level level = player.level();
        if (!level.isClientSide) {
            return;
        }
        PlayerSlashTrail trail = TRAILS_BY_PLAYER.computeIfAbsent(player.getUUID(), unused -> new PlayerSlashTrail());
        trail.record(player, bladeRootWorld, bladeTipWorld, level, partialTick);
    }

    /**
     * 粒子放在客户端 tick 刷，避免在实体渲染中途往粒子引擎塞东西。
     */
    @SubscribeEvent
    public static void spawnPendingSlashParticles(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            TRAILS_BY_PLAYER.clear();
            return;
        }
        if (TRAILS_BY_PLAYER.isEmpty()) {
            return;
        }
        pruneExpiredTrails(minecraft.level.getGameTime());
        for (PlayerSlashTrail trail : TRAILS_BY_PLAYER.values()) {
            trail.spawnPendingParticles(minecraft.level);
        }
    }

    /**
     * 实体画完后再画光轨：相机相对世界，不跟玩家身体 PoseStack，弧不会拧。
     */
    @SubscribeEvent
    public static void renderSlashTrails(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        if (TRAILS_BY_PLAYER.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Vec3 cameraWorld = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-cameraWorld.x, -cameraWorld.y, -cameraWorld.z);
        for (PlayerSlashTrail trail : TRAILS_BY_PLAYER.values()) {
            trail.render(poseStack, bufferSource, cameraWorld);
        }
        poseStack.popPose();
        bufferSource.endBatch();
    }

    private static void pruneExpiredTrails(long gameTimeTicks) {
        Iterator<Map.Entry<UUID, PlayerSlashTrail>> iterator = TRAILS_BY_PLAYER.entrySet().iterator();
        while (iterator.hasNext()) {
            PlayerSlashTrail trail = iterator.next().getValue();
            if (gameTimeTicks - trail.lastRecordGameTimeTicks > TRAIL_LINGER_TICKS) {
                iterator.remove();
            }
        }
    }

    /**
     * 单个玩家当前挥砍留下的刃位姿历史：刃尖 ribbon + 扫面 + 星星。
     */
    private static final class PlayerSlashTrail {

        private final ArrayDeque<BladeSample> bladeSamples = new ArrayDeque<>();
        private Vec3 lastTipWorld;
        private Vec3 lastTickTipWorld;
        private Vec3 pendingParticleRootWorld;
        private Vec3 pendingParticleTipWorld;
        private Vec3 pendingParticleTipTravel;
        private boolean hasPendingParticles;
        private long lastRecordGameTimeTicks = Long.MIN_VALUE;
        private long lastParticleGameTimeTicks = Long.MIN_VALUE;
        private float lastPartialTick = Float.NaN;

        private void record(
                AbstractClientPlayer player,
                Vec3 bladeRootWorld,
                Vec3 bladeTipWorld,
                Level level,
                float partialTick
        ) {
            long gameTimeTicks = level.getGameTime();
            if (gameTimeTicks == lastRecordGameTimeTicks && Mth.equal(partialTick, lastPartialTick)) {
                return;
            }

            if (shouldResetForNewSlash(bladeTipWorld)) {
                bladeSamples.clear();
                lastTipWorld = null;
                lastTickTipWorld = null;
            }

            bladeSamples.addLast(new BladeSample(bladeRootWorld, bladeTipWorld));
            while (bladeSamples.size() > MAX_SWEEP_SAMPLE_COUNT) {
                bladeSamples.removeFirst();
            }

            queueSlashParticles(bladeRootWorld, bladeTipWorld, gameTimeTicks);

            lastTipWorld = bladeTipWorld;
            lastRecordGameTimeTicks = gameTimeTicks;
            lastPartialTick = partialTick;
        }

        private boolean shouldResetForNewSlash(Vec3 bladeTipWorld) {
            return lastTipWorld != null
                    && lastTipWorld.distanceTo(bladeTipWorld) > SLASH_RESET_DISTANCE_BLOCKS;
        }

        private void queueSlashParticles(
                Vec3 bladeRootWorld,
                Vec3 bladeTipWorld,
                long gameTimeTicks
        ) {
            if (gameTimeTicks == lastParticleGameTimeTicks) {
                return;
            }
            pendingParticleTipTravel = lastTickTipWorld == null
                    ? Vec3.ZERO
                    : bladeTipWorld.subtract(lastTickTipWorld);
            pendingParticleRootWorld = bladeRootWorld;
            pendingParticleTipWorld = bladeTipWorld;
            hasPendingParticles = true;
            lastTickTipWorld = bladeTipWorld;
            lastParticleGameTimeTicks = gameTimeTicks;
        }

        private void spawnPendingParticles(Level level) {
            if (!hasPendingParticles || pendingParticleRootWorld == null || pendingParticleTipWorld == null) {
                return;
            }
            CarianSlicerFx.spawnAlongSlash(
                    level,
                    pendingParticleRootWorld,
                    pendingParticleTipWorld,
                    pendingParticleTipTravel == null ? Vec3.ZERO : pendingParticleTipTravel,
                    BLADE_STAR_SAMPLE_COUNT
            );
            hasPendingParticles = false;
        }

        private void render(
                PoseStack poseStack,
                MultiBufferSource bufferSource,
                Vec3 cameraWorld
        ) {
            if (bladeSamples.size() < 2) {
                return;
            }
            List<Vec3> tipHistory = new ArrayList<>(bladeSamples.size());
            for (BladeSample sample : bladeSamples) {
                tipHistory.add(sample.tipWorld);
            }
            Vec3 latestTip = tipHistory.get(tipHistory.size() - 1);
            List<Vec3> tipHistoryWithoutHead = tipHistory.subList(0, tipHistory.size() - 1);
            GlintstoneTrailRenderer.renderHistoryRibbon(
                    poseStack,
                    bufferSource,
                    Vec3.ZERO,
                    latestTip,
                    cameraWorld,
                    tipHistoryWithoutHead,
                    TIP_TRAIL_STYLE,
                    TRAIL_GLOW_COLOR_ARGB,
                    TRAIL_CORE_COLOR_ARGB
            );
            renderSweepRibbon(poseStack, bufferSource);
        }

        /**
         * 相邻刃位姿连成四边形：刃根→刃尖扫过的月牙面。
         */
        private void renderSweepRibbon(PoseStack poseStack, MultiBufferSource bufferSource) {
            if (bladeSamples.size() < 2) {
                return;
            }
            VertexConsumer consumer = bufferSource.getBuffer(GlintstoneTrailRenderTypes.TRANSLUCENT);
            Matrix4f poseMatrix = poseStack.last().pose();
            int red = (SWEEP_COLOR_ARGB >> 16) & 0xFF;
            int green = (SWEEP_COLOR_ARGB >> 8) & 0xFF;
            int blue = SWEEP_COLOR_ARGB & 0xFF;
            int alpha = (SWEEP_COLOR_ARGB >>> 24) & 0xFF;
            BladeSample[] samples = bladeSamples.toArray(BladeSample[]::new);
            for (int sampleIndex = 0; sampleIndex < samples.length - 1; sampleIndex++) {
                BladeSample older = samples[sampleIndex];
                BladeSample newer = samples[sampleIndex + 1];
                float olderAlphaScale = (sampleIndex + 1f) / samples.length;
                float newerAlphaScale = (sampleIndex + 2f) / samples.length;
                putSweepVertex(consumer, poseMatrix, older.rootWorld, red, green, blue, (int) (alpha * olderAlphaScale * 0.55f));
                putSweepVertex(consumer, poseMatrix, older.tipWorld, red, green, blue, (int) (alpha * olderAlphaScale));
                putSweepVertex(consumer, poseMatrix, newer.tipWorld, red, green, blue, (int) (alpha * newerAlphaScale));
                putSweepVertex(consumer, poseMatrix, newer.rootWorld, red, green, blue, (int) (alpha * newerAlphaScale * 0.55f));
            }
        }

        private static void putSweepVertex(
                VertexConsumer consumer,
                Matrix4f poseMatrix,
                Vec3 worldPosition,
                int red,
                int green,
                int blue,
                int alpha
        ) {
            consumer.addVertex(poseMatrix, (float) worldPosition.x, (float) worldPosition.y, (float) worldPosition.z)
                    .setColor(red, green, blue, Mth.clamp(alpha, 0, 255))
                    .setUv(0.5f, 0.5f)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(0.0f, 1.0f, 0.0f);
        }
    }

    /**
     * 一帧刃根 / 刃尖世界坐标。
     */
    private record BladeSample(Vec3 rootWorld, Vec3 tipWorld) {
    }

    /**
     * 生成物 0–1 立方体里的一点变到当前 PoseStack 的世界坐标。调用方必须先铺好无相机的世界姿态。
     */
    static Vec3 transformModelPoint(PoseStack poseStack, float localX, float localY, float localZ) {
        Vector3f transformed = new Vector3f(localX, localY, localZ);
        poseStack.last().pose().transformPosition(transformed);
        return new Vec3(transformed.x, transformed.y, transformed.z);
    }
}

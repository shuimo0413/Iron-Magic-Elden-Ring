package com.eldenring.spells.client.biome;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.client.render.magiclake.MagicLakeWispStreamRenderer;
import com.eldenring.spells.registry.ModBiomes;
import com.eldenring.spells.registry.ModParticles;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 魔法之湖迷幻光带：湖区上空缓慢流淌的宽幅幽蓝烟带（参考湖之利耶尼亚 / 魔法学院前的魔力流），十几秒后淡出。
 * <p>
 * 纯客户端视觉，不生成实体、不走网络。每 tick 在相机周围、落在魔法之湖群系内的位置补充少量光带，
 * 每条光带记录最近若干 tick 的位置（拖尾约 20–30 格），渲染时交给 {@link MagicLakeWispStreamRenderer}
 * 画成几股缠绕的宽烟丝 + 外层淡雾。两种运动：大弧线游走（大多数）和大半径螺旋上升（少数）。
 * 头部和身上偶尔掉落 {@code magic_lake_mote} 光点。
 * <p>
 * 亮度乘 {@link MagicLakeAmbientLight#getBlend}：离开湖区时随环境光一起平滑淡出。昼夜亮度一致。
 * 视频设置粒子为「最少」时不生成。
 */
@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID, value = Dist.CLIENT)
public final class MagicLakeWisps {

    /** 同时存在的光带上限。每条都很大，调大 → 画面会被蓝雾铺满。 */
    private static final int MAX_ACTIVE_WISPS = 7;
    /** 未满上限时每 tick 尝试补一条的概率。0.04 ≈ 平均 1.25 秒补一条。 */
    private static final float SPAWN_CHANCE_PER_TICK = 0.04f;
    /** 每次补充最多尝试几个随机点（落在别的群系 / 未加载区块的点会被丢弃）。 */
    private static final int SPAWN_POSITION_ATTEMPTS = 4;
    /** 出生点离相机的最小水平距离（方块）。光带很宽，太近会糊在脸上。 */
    private static final double SPAWN_DISTANCE_MIN_BLOCKS = 10.0;
    /** 出生点离相机的最大水平距离（方块）。调大 → 远处也有光带，但近处更稀。 */
    private static final double SPAWN_DISTANCE_MAX_BLOCKS = 56.0;
    /** 出生高度：地表（水面 / 地面 / 树冠）以上最少几格（方块）。 */
    private static final double SPAWN_HEIGHT_ABOVE_SURFACE_MIN_BLOCKS = 1.5;
    /** 出生高度额外随机（方块）。 */
    private static final double SPAWN_HEIGHT_ABOVE_SURFACE_RANDOM_BLOCKS = 5.0;
    /** 头部离相机超过此距离（方块）直接移除。 */
    private static final double DESPAWN_DISTANCE_BLOCKS = 96.0;

    /** 基础寿命（tick）。 */
    private static final int LIFETIME_BASE_TICKS = 260;
    /** 额外随机寿命（tick）。260–420 tick ≈ 13–21 秒。 */
    private static final int LIFETIME_RANDOM_TICKS = 160;
    /** 出生淡入时长（tick）。 */
    private static final int FADE_IN_TICKS = 50;
    /** 消亡淡出时长（tick）。 */
    private static final int FADE_OUT_TICKS = 70;
    /** 记录的历史点数（每 tick 一个）。× 速度 ≈ 拖尾长度：240 × 0.2 ≈ 48 格。调大 → 光带更长。 */
    private static final int HISTORY_POINT_COUNT = 240;

    /** 体型系数最小值，乘到宽度和缠绕半径上。 */
    private static final float SIZE_SCALE_MIN = 0.8f;
    /** 体型系数额外随机。0.8–1.3：有粗有细，不会一模一样。 */
    private static final float SIZE_SCALE_RANDOM = 0.5f;

    /** 游走最小速度（方块 / tick）。 */
    private static final double SPEED_MIN_BLOCKS_PER_TICK = 0.16;
    /** 游走额外随机速度（方块 / tick）。调大 → 拖尾更长、流得更快。 */
    private static final double SPEED_RANDOM_BLOCKS_PER_TICK = 0.08;
    /** 转向角速度振幅（弧度 / tick）。调大 → 弧线弯得更厉害。 */
    private static final double WANDER_TURN_AMPLITUDE_RADIANS_PER_TICK = 0.016;
    /** 转向变化频率（弧度 / tick）。0.018 ≈ 17 秒一个左右来回，形成大 S 弯。 */
    private static final double WANDER_TURN_FREQUENCY_RADIANS_PER_TICK = 0.018;
    /** 上下起伏振幅最小值（方块）。 */
    private static final double BOB_AMPLITUDE_MIN_BLOCKS = 0.6;
    /** 上下起伏振幅额外随机（方块）。 */
    private static final double BOB_AMPLITUDE_RANDOM_BLOCKS = 1.0;
    /** 上下起伏角频率（弧度 / tick）。0.03 ≈ 10 秒一个起伏，拖尾上能看到一段波浪。 */
    private static final double BOB_FREQUENCY_RADIANS_PER_TICK = 0.03;

    /** 螺旋上升型的概率（0–1）。 */
    private static final float SPIRAL_CHANCE = 0.25f;
    /** 螺旋半径最小值（方块）。拖尾约 24 格，半径 4 格左右正好绕成一个大漩涡。 */
    private static final double SPIRAL_RADIUS_MIN_BLOCKS = 3.5;
    /** 螺旋半径额外随机（方块）。 */
    private static final double SPIRAL_RADIUS_RANDOM_BLOCKS = 2.5;
    /** 螺旋上升速度（方块 / tick）。每绕一圈约升 1.5–2 格。 */
    private static final double SPIRAL_RISE_BLOCKS_PER_TICK = 0.012;
    /** 螺旋中心随游走方向漂移的速度比例（相对游走速度）。 */
    private static final double SPIRAL_CENTER_DRIFT_SCALE = 0.2;

    /** 头部每 tick 掉落一颗微光的概率。 */
    private static final float HEAD_MOTE_CHANCE_PER_TICK = 0.3f;
    /** 身上（随机历史点）每 tick 掉落一颗微光的概率。让整条烟带里都有星星点点。 */
    private static final float BODY_MOTE_CHANCE_PER_TICK = 0.25f;
    /** 掉落位置在中心线周围的随机偏移（方块）。 */
    private static final double MOTE_SPAWN_SPREAD_BLOCKS = 0.8;
    /** 掉落微光的随机初速度（方块 / tick）。 */
    private static final double MOTE_SPEED_BLOCKS_PER_TICK = 0.01;

    private static final List<Wisp> ACTIVE_WISPS = new ArrayList<>();
    private static ClientLevel trackedLevel;

    private MagicLakeWisps() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != trackedLevel) {
            ACTIVE_WISPS.clear();
            trackedLevel = level;
        }
        if (level == null || minecraft.isPaused()) {
            return;
        }

        Vec3 cameraWorld = minecraft.gameRenderer.getMainCamera().getPosition();
        RandomSource random = level.random;
        Iterator<Wisp> iterator = ACTIVE_WISPS.iterator();
        while (iterator.hasNext()) {
            Wisp wisp = iterator.next();
            wisp.tick(level, random);
            if (wisp.isExpired() || wisp.position.distanceToSqr(cameraWorld) > DESPAWN_DISTANCE_BLOCKS * DESPAWN_DISTANCE_BLOCKS) {
                iterator.remove();
            }
        }

        boolean particlesMinimal = minecraft.options.particles().get() == ParticleStatus.MINIMAL;
        if (particlesMinimal
                || MagicLakeAmbientLight.getBlend(1.0f) <= 0.0f
                || ACTIVE_WISPS.size() >= MAX_ACTIVE_WISPS
                || random.nextFloat() >= SPAWN_CHANCE_PER_TICK) {
            return;
        }
        Wisp spawnedWisp = trySpawnWisp(level, random, cameraWorld);
        if (spawnedWisp != null) {
            ACTIVE_WISPS.add(spawnedWisp);
        }
    }

    /**
     * 实体画完后画光带，与其它法术光轨同一时机，光影兼容性一致。
     * PoseStack 原点即相机，顶点在 double 里减相机坐标，远离世界原点也不抖。
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || ACTIVE_WISPS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float ambientBlend = MagicLakeAmbientLight.getBlend(partialTick);
        if (ambientBlend <= 0.0f) {
            return;
        }

        Vec3 cameraWorld = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        for (Wisp wisp : ACTIVE_WISPS) {
            wisp.render(poseStack, bufferSource, cameraWorld, partialTick, ambientBlend);
        }
        bufferSource.endBatch();
    }

    /**
     * 在相机周围随机找一个落在魔法之湖、区块已加载的点，出生在地表以上一点。
     */
    private static Wisp trySpawnWisp(ClientLevel level, RandomSource random, Vec3 cameraWorld) {
        BlockPos.MutableBlockPos candidatePos = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < SPAWN_POSITION_ATTEMPTS; attempt++) {
            double angleRadians = random.nextDouble() * Math.PI * 2.0;
            double distanceBlocks = Mth.lerp(random.nextDouble(), SPAWN_DISTANCE_MIN_BLOCKS, SPAWN_DISTANCE_MAX_BLOCKS);
            int candidateX = Mth.floor(cameraWorld.x + Math.cos(angleRadians) * distanceBlocks);
            int candidateZ = Mth.floor(cameraWorld.z + Math.sin(angleRadians) * distanceBlocks);
            candidatePos.set(candidateX, Mth.floor(cameraWorld.y), candidateZ);
            if (!level.hasChunkAt(candidatePos)) {
                continue;
            }
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, candidateX, candidateZ);
            candidatePos.setY(surfaceY);
            if (!level.getBiome(candidatePos).is(ModBiomes.MAGIC_LAKE)) {
                continue;
            }
            double spawnY = surfaceY
                    + SPAWN_HEIGHT_ABOVE_SURFACE_MIN_BLOCKS
                    + random.nextDouble() * SPAWN_HEIGHT_ABOVE_SURFACE_RANDOM_BLOCKS;
            return new Wisp(new Vec3(candidateX + random.nextDouble(), spawnY, candidateZ + random.nextDouble()), random);
        }
        return null;
    }

    /**
     * 一条光带：当前位置 + 最近若干 tick 的历史点 + 运动参数。
     */
    private static final class Wisp {
        private final ArrayDeque<Vec3> historyPositions = new ArrayDeque<>();
        private final int lifetimeTicks;
        private final double speedBlocksPerTick;
        private final double wanderPhaseRadians;
        private final double bobAmplitudeBlocks;
        private final double bobPhaseRadians;
        private final boolean spiral;
        private final double spiralRadiusBlocks;
        private final double spiralAngularSpeedRadiansPerTick;
        private final float sizeScale;

        private Vec3 position;
        private Vec3 previousPosition;
        private double headingRadians;
        private double baseY;
        private double spiralCenterX;
        private double spiralCenterZ;
        private double spiralAngleRadians;
        private int ageTicks;

        private Wisp(Vec3 spawnPosition, RandomSource random) {
            this.position = spawnPosition;
            this.previousPosition = spawnPosition;
            this.lifetimeTicks = LIFETIME_BASE_TICKS + random.nextInt(LIFETIME_RANDOM_TICKS + 1);
            this.speedBlocksPerTick = SPEED_MIN_BLOCKS_PER_TICK + random.nextDouble() * SPEED_RANDOM_BLOCKS_PER_TICK;
            this.wanderPhaseRadians = random.nextDouble() * Math.PI * 2.0;
            this.bobAmplitudeBlocks = BOB_AMPLITUDE_MIN_BLOCKS + random.nextDouble() * BOB_AMPLITUDE_RANDOM_BLOCKS;
            this.bobPhaseRadians = random.nextDouble() * Math.PI * 2.0;
            this.headingRadians = random.nextDouble() * Math.PI * 2.0;
            this.baseY = spawnPosition.y;
            this.spiral = random.nextFloat() < SPIRAL_CHANCE;
            this.spiralRadiusBlocks = SPIRAL_RADIUS_MIN_BLOCKS + random.nextDouble() * SPIRAL_RADIUS_RANDOM_BLOCKS;
            double spiralDirection = random.nextBoolean() ? 1.0 : -1.0;
            // 角速度 = 线速度 / 半径：螺旋型和游走型沿路径的速度一致，拖尾长度也一致。
            this.spiralAngularSpeedRadiansPerTick = spiralDirection * speedBlocksPerTick / spiralRadiusBlocks;
            this.sizeScale = SIZE_SCALE_MIN + random.nextFloat() * SIZE_SCALE_RANDOM;
            this.spiralAngleRadians = random.nextDouble() * Math.PI * 2.0;
            this.spiralCenterX = spawnPosition.x - Math.cos(spiralAngleRadians) * spiralRadiusBlocks;
            this.spiralCenterZ = spawnPosition.z - Math.sin(spiralAngleRadians) * spiralRadiusBlocks;
        }

        private void tick(ClientLevel level, RandomSource random) {
            ageTicks++;
            previousPosition = position;
            historyPositions.addLast(position);
            while (historyPositions.size() > HISTORY_POINT_COUNT) {
                historyPositions.removeFirst();
            }

            double bobOffsetBlocks = bobAmplitudeBlocks * Math.sin(ageTicks * BOB_FREQUENCY_RADIANS_PER_TICK + bobPhaseRadians);
            if (spiral) {
                spiralAngleRadians += spiralAngularSpeedRadiansPerTick;
                spiralCenterX += Math.cos(headingRadians) * speedBlocksPerTick * SPIRAL_CENTER_DRIFT_SCALE;
                spiralCenterZ += Math.sin(headingRadians) * speedBlocksPerTick * SPIRAL_CENTER_DRIFT_SCALE;
                baseY += SPIRAL_RISE_BLOCKS_PER_TICK;
                position = new Vec3(
                        spiralCenterX + Math.cos(spiralAngleRadians) * spiralRadiusBlocks,
                        baseY + bobOffsetBlocks,
                        spiralCenterZ + Math.sin(spiralAngleRadians) * spiralRadiusBlocks
                );
            } else {
                headingRadians += WANDER_TURN_AMPLITUDE_RADIANS_PER_TICK
                        * Math.sin(ageTicks * WANDER_TURN_FREQUENCY_RADIANS_PER_TICK + wanderPhaseRadians);
                position = new Vec3(
                        position.x + Math.cos(headingRadians) * speedBlocksPerTick,
                        baseY + bobOffsetBlocks,
                        position.z + Math.sin(headingRadians) * speedBlocksPerTick
                );
            }

            float currentFade = lifeFade(0.0f);
            if (random.nextFloat() < HEAD_MOTE_CHANCE_PER_TICK * currentFade) {
                dropMote(level, random, position);
            }
            if (historyPositions.size() > 2 && random.nextFloat() < BODY_MOTE_CHANCE_PER_TICK * currentFade) {
                int historyIndex = random.nextInt(historyPositions.size());
                Iterator<Vec3> historyIterator = historyPositions.iterator();
                Vec3 bodyPosition = historyIterator.next();
                for (int skipIndex = 0; skipIndex < historyIndex; skipIndex++) {
                    bodyPosition = historyIterator.next();
                }
                dropMote(level, random, bodyPosition);
            }
        }

        private static void dropMote(ClientLevel level, RandomSource random, Vec3 centerPosition) {
            level.addParticle(
                    ModParticles.MAGIC_LAKE_MOTE.get(),
                    centerPosition.x + (random.nextDouble() * 2.0 - 1.0) * MOTE_SPAWN_SPREAD_BLOCKS,
                    centerPosition.y + (random.nextDouble() * 2.0 - 1.0) * MOTE_SPAWN_SPREAD_BLOCKS,
                    centerPosition.z + (random.nextDouble() * 2.0 - 1.0) * MOTE_SPAWN_SPREAD_BLOCKS,
                    (random.nextDouble() * 2.0 - 1.0) * MOTE_SPEED_BLOCKS_PER_TICK,
                    (random.nextDouble() * 2.0 - 1.0) * MOTE_SPEED_BLOCKS_PER_TICK,
                    (random.nextDouble() * 2.0 - 1.0) * MOTE_SPEED_BLOCKS_PER_TICK
            );
        }

        private boolean isExpired() {
            return ageTicks >= lifetimeTicks;
        }

        /** 出生淡入 × 消亡淡出（0–1）。 */
        private float lifeFade(float partialTick) {
            float smoothAge = ageTicks + partialTick;
            float fadeIn = Mth.clamp(smoothAge / FADE_IN_TICKS, 0.0f, 1.0f);
            float fadeOut = Mth.clamp((lifetimeTicks - smoothAge) / FADE_OUT_TICKS, 0.0f, 1.0f);
            return fadeIn * fadeOut;
        }

        private void render(
                PoseStack poseStack,
                MultiBufferSource bufferSource,
                Vec3 cameraWorld,
                float partialTick,
                float ambientBlend
        ) {
            if (historyPositions.size() < 2) {
                return;
            }
            float brightness = lifeFade(partialTick) * ambientBlend;
            if (brightness <= 0.01f) {
                return;
            }
            List<Vec3> pathWorldPositions = new ArrayList<>(historyPositions.size() + 1);
            pathWorldPositions.addAll(historyPositions);
            pathWorldPositions.add(previousPosition.lerp(position, partialTick));
            MagicLakeWispStreamRenderer.renderStream(
                    poseStack,
                    bufferSource,
                    pathWorldPositions,
                    cameraWorld,
                    ageTicks + partialTick,
                    sizeScale,
                    brightness
            );
        }
    }
}

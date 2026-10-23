package com.eldenring.spells.particle.magiclake;

import com.eldenring.spells.client.render.AdditiveParticleRenderType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

/**
 * 魔法之湖飘浮微光：淡幽蓝的小光点，贴着湖面缓慢上浮、左右摇摆、轻轻闪烁。
 * <p>
 * 两个来源：群系 JSON 的 {@code effects.particle}（原版在玩家周围随机刷，铺满整个群系），
 * 以及 {@code MagicLakeWisps} 光带头部偶尔掉落的光点。贴图复用 {@code frost_mote} 三帧。
 * 加法混合 + 全亮度，昼夜一致；白天因为背景亮会不明显，这是预期的。
 */
public class MagicLakeMoteParticle extends TextureSheetParticle {

    /** 基础寿命（tick）。调大 → 光点停留更久、场上同时存在的数量更多。 */
    private static final int LIFETIME_BASE_TICKS = 60;
    /** 额外随机寿命上限（tick）。 */
    private static final int LIFETIME_RANDOM_TICKS = 50;
    /** 最小四边形边长（方块）。 */
    private static final float QUAD_SIZE_MIN_BLOCKS = 0.035f;
    /** 额外随机边长（方块）。调大 → 光点更显眼。 */
    private static final float QUAD_SIZE_RANDOM_BLOCKS = 0.045f;
    /** 最小上浮速度（方块 / tick）。 */
    private static final double RISE_SPEED_MIN_BLOCKS_PER_TICK = 0.003;
    /** 额外随机上浮速度（方块 / tick）。调大 → 飘得更快、更像火星而不是尘埃。 */
    private static final double RISE_SPEED_RANDOM_BLOCKS_PER_TICK = 0.006;
    /** 出生时随机水平漂移速度上限（方块 / tick）。 */
    private static final double DRIFT_SPEED_MAX_BLOCKS_PER_TICK = 0.006;
    /** 左右摇摆的速度振幅（方块 / tick）。调大 → 摆得更明显。 */
    private static final double SWAY_SPEED_AMPLITUDE_BLOCKS_PER_TICK = 0.004;
    /** 摇摆角频率（弧度 / tick）。0.09 ≈ 3.5 秒一个来回。 */
    private static final float SWAY_FREQUENCY_RADIANS_PER_TICK = 0.09f;
    /** 闪烁角频率（弧度 / tick）。 */
    private static final float TWINKLE_FREQUENCY_RADIANS_PER_TICK = 0.35f;
    /** 每帧贴图停留的 tick 数（三帧循环）。 */
    private static final int TWINKLE_FRAME_TICKS = 5;
    /** 贴图帧数（frost_mote_0..2）。 */
    private static final int SPRITE_FRAME_COUNT = 3;
    /** 最大不透明度（0–1）。加法混合下即最大亮度，调小 → 更「淡淡的」。 */
    private static final float MAX_ALPHA = 0.85f;

    /** 淡幽蓝着色（0–1）。贴图本身是冰蓝冷白，这里再往幽蓝拉。 */
    private static final float GHOST_BLUE_RED = 0.62f;
    private static final float GHOST_BLUE_GREEN = 0.80f;
    private static final float GHOST_BLUE_BLUE = 1.00f;

    /**
     * 只在地表以上这么高（方块）以内刷。原版环境粒子在玩家周围 ±32 格立方体里随机取点，
     * 不限制的话大半会飘在高空，湖面反而稀疏。
     */
    private static final int MAX_HEIGHT_ABOVE_SURFACE_BLOCKS = 8;
    /** 天空光低于此值（0–15）视为洞穴 / 地下，不刷。树冠下仍有 13 左右，不受影响。 */
    private static final int MIN_SKY_LIGHT = 10;

    private final SpriteSet sprites;
    private final double baseXSpeed;
    private final double baseZSpeed;
    private final float swayPhaseRadians;
    private final float twinklePhaseRadians;

    protected MagicLakeMoteParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SpriteSet sprites
    ) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.baseXSpeed = xSpeed + (random.nextDouble() * 2.0 - 1.0) * DRIFT_SPEED_MAX_BLOCKS_PER_TICK;
        this.baseZSpeed = zSpeed + (random.nextDouble() * 2.0 - 1.0) * DRIFT_SPEED_MAX_BLOCKS_PER_TICK;
        this.xd = baseXSpeed;
        this.yd = ySpeed + RISE_SPEED_MIN_BLOCKS_PER_TICK + random.nextDouble() * RISE_SPEED_RANDOM_BLOCKS_PER_TICK;
        this.zd = baseZSpeed;
        this.lifetime = LIFETIME_BASE_TICKS + random.nextInt(LIFETIME_RANDOM_TICKS + 1);
        this.quadSize = QUAD_SIZE_MIN_BLOCKS + random.nextFloat() * QUAD_SIZE_RANDOM_BLOCKS;
        this.gravity = 0.0f;
        this.friction = 1.0f;
        this.hasPhysics = false;
        this.swayPhaseRadians = random.nextFloat() * Mth.TWO_PI;
        this.twinklePhaseRadians = random.nextFloat() * Mth.TWO_PI;
        float tintJitter = 0.92f + random.nextFloat() * 0.08f;
        this.rCol = GHOST_BLUE_RED * tintJitter;
        this.gCol = GHOST_BLUE_GREEN * tintJitter;
        this.bCol = GHOST_BLUE_BLUE;
        this.alpha = 0.0f;
        updateSpriteFrame();
    }

    @Override
    public void tick() {
        float swayAngle = age * SWAY_FREQUENCY_RADIANS_PER_TICK + swayPhaseRadians;
        this.xd = baseXSpeed + Mth.cos(swayAngle) * SWAY_SPEED_AMPLITUDE_BLOCKS_PER_TICK;
        this.zd = baseZSpeed + Mth.sin(swayAngle) * SWAY_SPEED_AMPLITUDE_BLOCKS_PER_TICK;
        super.tick();
        updateSpriteFrame();

        float lifeProgress = (float) age / (float) lifetime;
        float lifeEnvelope = Mth.sin(Mth.clamp(lifeProgress, 0.0f, 1.0f) * Mth.PI);
        float twinkle = 0.75f + 0.25f * Mth.sin(age * TWINKLE_FREQUENCY_RADIANS_PER_TICK + twinklePhaseRadians);
        this.alpha = MAX_ALPHA * lifeEnvelope * twinkle;
    }

    private void updateSpriteFrame() {
        int frameIndex = (age / TWINKLE_FRAME_TICKS) % SPRITE_FRAME_COUNT;
        setSprite(sprites.get(frameIndex, SPRITE_FRAME_COUNT - 1));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return AdditiveParticleRenderType.ADDITIVE;
    }

    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    /**
     * 生成前过滤：水里、洞穴里、离地表太高的位置直接不生成（返回 null，原版粒子引擎会跳过）。
     */
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed
        ) {
            BlockPos spawnPos = BlockPos.containing(x, y, z);
            if (!level.getFluidState(spawnPos).isEmpty()) {
                return null;
            }
            if (level.getBrightness(LightLayer.SKY, spawnPos) < MIN_SKY_LIGHT) {
                return null;
            }
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, spawnPos.getX(), spawnPos.getZ());
            if (spawnPos.getY() > surfaceY + MAX_HEIGHT_ABOVE_SURFACE_BLOCKS) {
                return null;
            }
            return new MagicLakeMoteParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}

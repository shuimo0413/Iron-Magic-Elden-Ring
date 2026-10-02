package com.eldenring.spells.spell.fx;

import com.eldenring.spells.particle.gravity.GravityFx;
import com.eldenring.spells.registry.ModParticles;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * 陨石特效：虚空黑洞、陨石拖尾、落地爆裂。重力紫粒子走 {@link GravityFx} 同一套贴图，另加原版石屑。
 * <p>
 * 黑洞 / 拖尾只在客户端刷（由实体 tick 调用，跟着实体走，不占服务端粒子包）；
 * 落地爆裂 / 黑洞坍缩 / 射程尽头碎裂只在服务端刷（经 {@link MagicManager} 同步到附近客户端）。
 */
public final class MeteoriteFx {

    // -------------------------------------------------------------------------
    // 黑洞
    // -------------------------------------------------------------------------

    /**
     * 完全张开时黑洞盘面半径（方块）。调大 → 黑洞更大更压迫；需大于陨石出生盘面半径（见 MeteoriteCasting）。
     */
    private static final double VOID_RADIUS_BLOCKS = 1.3;

    /** 刚出现时的半径比例（相对完全张开）。 */
    private static final double VOID_OPENING_START_RADIUS_FRACTION = 0.12;

    /** 黑核填充区域相对盘面半径的比例。调大 → 黑面更满；调小 → 紫边更宽。 */
    private static final double VOID_CORE_FILL_RADIUS_FRACTION = 0.72;

    /** 完全张开时每 tick 刷的黑核粒子数。核粒子寿命约 16–24 tick，叠起来才能读成一整块黑面。 */
    private static final int VOID_CORE_PARTICLES_PER_TICK = 7;

    /** 黑核粒子尺寸倍率（相对重力黑核基准约 0.22–0.32 格）。 */
    private static final float VOID_CORE_PARTICLE_SIZE_SCALE = 2.4f;

    /** 每 tick 沿盘边刷的旋转紫光数。调大 → 边缘光环更亮更连贯。 */
    private static final int VOID_RIM_PARTICLES_PER_TICK = 9;

    /** 盘边紫光的切向速度（方块/tick）。调大 → 旋得更快、更像漩涡。 */
    private static final double VOID_RIM_SWIRL_SPEED_BLOCKS_PER_TICK = 0.10;

    /** 每 tick 从外圈被吸进黑洞的微粒 / 暗丝数量。 */
    private static final int VOID_INWARD_PARTICLES_PER_TICK = 4;

    /** 被吸微粒的出生半径范围（相对盘面半径）。 */
    private static final double VOID_INWARD_SPAWN_RADIUS_MIN_FRACTION = 1.35;
    private static final double VOID_INWARD_SPAWN_RADIUS_MAX_FRACTION = 1.9;

    /** 被吸微粒每 tick 走完出生距离的比例。调大 → 吸得更急。 */
    private static final double VOID_INWARD_SPEED_FRACTION_PER_TICK = 0.10;

    /** 每 tick 在盘边打出一道紫色电弧的概率（对应原画里撕裂虚空的闪电）。 */
    private static final float VOID_LIGHTNING_CHANCE_PER_TICK = 0.35f;

    /** 一道电弧由多少个火花点组成。 */
    private static final int VOID_LIGHTNING_SEGMENT_COUNT = 6;

    /** 电弧沿盘边延伸的总弧度。 */
    private static final double VOID_LIGHTNING_ARC_SPAN_RADIANS = 0.9;

    /** 电弧每个点相对盘边的最大径向抖动（相对盘面半径）。调大 → 更锯齿。 */
    private static final double VOID_LIGHTNING_RADIAL_JITTER_FRACTION = 0.25;

    /** 每 tick 刷外圈紫雾的概率。 */
    private static final float VOID_MIST_CHANCE_PER_TICK = 0.45f;

    /** 每 tick 在中心叠一层大蚀环的概率。蚀环会胀开淡出，给黑洞一圈呼吸的外晕。 */
    private static final float VOID_ECLIPSE_CHANCE_PER_TICK = 0.12f;

    /** 重力粒子各自的入口速度阻尼（见 GravityParticle.Kind），传速度时要先除掉。 */
    private static final double GLOW_VELOCITY_DAMP = 0.28;
    private static final double MOTE_VELOCITY_DAMP = 0.25;
    private static final double FILAMENT_VELOCITY_DAMP = 0.10;
    private static final double SPARK_VELOCITY_DAMP = 0.90;
    private static final double MIST_VELOCITY_DAMP = 0.28;

    /** 黑洞坍缩时的重力爆裂强度。 */
    private static final float VOID_COLLAPSE_PARTICLE_INTENSITY = 1.3f;

    // -------------------------------------------------------------------------
    // 陨石
    // -------------------------------------------------------------------------

    /** 飞行拖尾强度（相对 GravityFx 基准）。陨石比岩石球拖尾更亮更长。 */
    private static final float TRAIL_PARTICLE_INTENSITY = 0.95f;

    /** 每 tick 沿上一段飞行路径铺的亮紫光点数，连起来读成原画里那道紫色光尾。 */
    private static final int TRAIL_GLOW_STREAK_POINTS_PER_TICK = 3;

    /** 拖尾亮紫光点尺寸倍率。 */
    private static final float TRAIL_GLOW_SIZE_SCALE = 1.25f;

    /** 飞行时每 tick 掉落石屑的概率。 */
    private static final float TRAIL_DEBRIS_CHANCE = 0.25f;

    /** 落地重力爆裂强度。陨石雨很密，单颗不宜过亮。 */
    private static final float IMPACT_PARTICLE_INTENSITY = 1.0f;

    /** 落地迸出的石屑数量。 */
    private static final int IMPACT_DEBRIS_COUNT = 18;

    /** 落地激起的烟尘数量。 */
    private static final int IMPACT_DUST_COUNT = 4;

    /** 射程尽头碎裂的重力爆裂强度。 */
    private static final float FIZZLE_PARTICLE_INTENSITY = 0.4f;

    /** 射程尽头碎裂迸出的石屑数量。 */
    private static final int FIZZLE_DEBRIS_COUNT = 8;

    private static final BlockParticleOption ROCK_DEBRIS =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLED_DEEPSLATE.defaultBlockState());

    private MeteoriteFx() {
    }

    /**
     * 黑洞持续粒子：黑核填充 + 旋转紫边 + 外圈被吸微粒 + 盘边电弧 + 紫雾 + 呼吸蚀环。仅客户端。
     *
     * @param facing          盘面法线（朝向陨石落区）
     * @param openingProgress 张开进度 0–1
     * @param ageTicks        黑洞存活 tick，用于电弧相位
     */
    public static void voidAmbient(Level level, Vec3 center, Vec3 facing, float openingProgress, int ageTicks) {
        if (!level.isClientSide) {
            return;
        }
        RandomSource random = level.random;
        float easedOpening = 1.0f - (1.0f - openingProgress) * (1.0f - openingProgress);
        double radiusBlocks = VOID_RADIUS_BLOCKS * Mth.lerp(easedOpening, VOID_OPENING_START_RADIUS_FRACTION, 1.0);
        Vec3 discRight = discRightAxis(facing);
        Vec3 discUp = discRight.cross(facing).normalize();

        int coreCount = Math.max(1, Math.round(VOID_CORE_PARTICLES_PER_TICK * easedOpening));
        float coreSizeScale = VOID_CORE_PARTICLE_SIZE_SCALE * (0.45f + 0.55f * easedOpening);
        GravityFx.withParticleSizeScale(coreSizeScale, () -> {
            for (int coreIndex = 0; coreIndex < coreCount; coreIndex++) {
                Vec3 corePosition = center.add(randomDiscPoint(discRight, discUp, radiusBlocks * VOID_CORE_FILL_RADIUS_FRACTION, random));
                level.addParticle(ModParticles.GRAVITY_CORE.get(), corePosition.x, corePosition.y, corePosition.z, 0.0, 0.0, 0.0);
            }
        });

        GravityFx.withParticleSizeScale(1.1f, () -> {
            for (int rimIndex = 0; rimIndex < VOID_RIM_PARTICLES_PER_TICK; rimIndex++) {
                double angleRadians = random.nextDouble() * (Math.PI * 2.0);
                double rimRadius = radiusBlocks * (0.90 + random.nextDouble() * 0.16);
                Vec3 radial = discRight.scale(Math.cos(angleRadians)).add(discUp.scale(Math.sin(angleRadians)));
                Vec3 tangent = discRight.scale(-Math.sin(angleRadians)).add(discUp.scale(Math.cos(angleRadians)));
                Vec3 rimPosition = center.add(radial.scale(rimRadius));
                Vec3 rimVelocity = tangent.scale(VOID_RIM_SWIRL_SPEED_BLOCKS_PER_TICK)
                        .subtract(radial.scale(VOID_RIM_SWIRL_SPEED_BLOCKS_PER_TICK * 0.25))
                        .scale(1.0 / GLOW_VELOCITY_DAMP);
                level.addParticle(ModParticles.GRAVITY_GLOW.get(),
                        rimPosition.x, rimPosition.y, rimPosition.z,
                        rimVelocity.x, rimVelocity.y, rimVelocity.z);
            }
        });

        for (int inwardIndex = 0; inwardIndex < VOID_INWARD_PARTICLES_PER_TICK; inwardIndex++) {
            boolean useFilament = random.nextFloat() < 0.4f;
            ParticleOptions inwardParticle = useFilament ? ModParticles.GRAVITY_FILAMENT.get() : ModParticles.GRAVITY_MOTE.get();
            double velocityDamp = useFilament ? FILAMENT_VELOCITY_DAMP : MOTE_VELOCITY_DAMP;
            double angleRadians = random.nextDouble() * (Math.PI * 2.0);
            double spawnRadius = radiusBlocks * Mth.lerp(random.nextDouble(),
                    VOID_INWARD_SPAWN_RADIUS_MIN_FRACTION, VOID_INWARD_SPAWN_RADIUS_MAX_FRACTION);
            Vec3 radial = discRight.scale(Math.cos(angleRadians)).add(discUp.scale(Math.sin(angleRadians)));
            Vec3 depthJitter = facing.scale((random.nextDouble() * 2.0 - 1.0) * radiusBlocks * 0.3);
            Vec3 spawnPosition = center.add(radial.scale(spawnRadius)).add(depthJitter);
            Vec3 inwardVelocity = center.subtract(spawnPosition)
                    .scale(VOID_INWARD_SPEED_FRACTION_PER_TICK / velocityDamp);
            level.addParticle(inwardParticle,
                    spawnPosition.x, spawnPosition.y, spawnPosition.z,
                    inwardVelocity.x, inwardVelocity.y, inwardVelocity.z);
        }

        if (easedOpening > 0.5f && random.nextFloat() < VOID_LIGHTNING_CHANCE_PER_TICK) {
            spawnRimLightning(level, center, discRight, discUp, radiusBlocks, ageTicks, random);
        }

        if (random.nextFloat() < VOID_MIST_CHANCE_PER_TICK) {
            double angleRadians = random.nextDouble() * (Math.PI * 2.0);
            Vec3 radial = discRight.scale(Math.cos(angleRadians)).add(discUp.scale(Math.sin(angleRadians)));
            Vec3 mistPosition = center.add(radial.scale(radiusBlocks * (1.05 + random.nextDouble() * 0.35)));
            Vec3 mistVelocity = radial.scale(0.015 / MIST_VELOCITY_DAMP);
            GravityFx.withParticleSizeScale(1.7f, () -> level.addParticle(ModParticles.GRAVITY_MIST.get(),
                    mistPosition.x, mistPosition.y, mistPosition.z,
                    mistVelocity.x, mistVelocity.y, mistVelocity.z));
        }

        if (random.nextFloat() < VOID_ECLIPSE_CHANCE_PER_TICK) {
            float eclipseSizeScale = (float) (radiusBlocks * 2.2 / 0.41);
            GravityFx.withParticleSizeScale(eclipseSizeScale, () -> level.addParticle(ModParticles.GRAVITY_ECLIPSE.get(),
                    center.x, center.y, center.z, 0.0, 0.0, 0.0));
        }
    }

    /**
     * 黑洞收缩消失：中心一次重力爆裂。仅服务端。
     */
    public static void voidCollapse(Level level, Vec3 center) {
        if (level.isClientSide || level.getServer() == null) {
            return;
        }
        GravityFx.impact(level, center.x, center.y, center.z, VOID_COLLAPSE_PARTICLE_INTENSITY);
    }

    /**
     * 陨石飞行拖尾：重力紫点缀 + 沿路径的亮紫光尾 + 偶尔掉石屑。仅客户端。
     */
    public static void trail(Level level, Vec3 meteoriteCenter, Vec3 motion) {
        if (!level.isClientSide || motion.lengthSqr() < 1.0e-8) {
            return;
        }
        GravityFx.trailAccents(level, meteoriteCenter.x, meteoriteCenter.y, meteoriteCenter.z, motion, TRAIL_PARTICLE_INTENSITY);
        RandomSource random = level.random;
        GravityFx.withParticleSizeScale(TRAIL_GLOW_SIZE_SCALE, () -> {
            for (int pointIndex = 0; pointIndex < TRAIL_GLOW_STREAK_POINTS_PER_TICK; pointIndex++) {
                double backFraction = (pointIndex + random.nextDouble()) / TRAIL_GLOW_STREAK_POINTS_PER_TICK;
                Vec3 streakPosition = meteoriteCenter.subtract(motion.scale(backFraction));
                level.addParticle(ModParticles.GRAVITY_GLOW.get(),
                        streakPosition.x, streakPosition.y, streakPosition.z,
                        0.0, 0.0, 0.0);
            }
        });
        if (random.nextFloat() < TRAIL_DEBRIS_CHANCE) {
            level.addParticle(ROCK_DEBRIS,
                    meteoriteCenter.x + (random.nextDouble() - 0.5) * 0.5,
                    meteoriteCenter.y + (random.nextDouble() - 0.5) * 0.5,
                    meteoriteCenter.z + (random.nextDouble() - 0.5) * 0.5,
                    -motion.x * 0.05,
                    -motion.y * 0.05,
                    -motion.z * 0.05);
        }
    }

    /**
     * 落地爆裂：重力紫蚀环 / 雾 / 电弧 + 深板岩碎屑 + 烟尘。仅服务端。
     */
    public static void impact(Level level, Vec3 impactCenter) {
        if (level.isClientSide || level.getServer() == null) {
            return;
        }
        GravityFx.impact(level, impactCenter.x, impactCenter.y, impactCenter.z, IMPACT_PARTICLE_INTENSITY);
        MagicManager.spawnParticles(
                level, ROCK_DEBRIS,
                impactCenter.x, impactCenter.y + 0.2, impactCenter.z,
                IMPACT_DEBRIS_COUNT, 0.35, 0.25, 0.35,
                0.2, false
        );
        MagicManager.spawnParticles(
                level, ParticleTypes.POOF,
                impactCenter.x, impactCenter.y + 0.2, impactCenter.z,
                IMPACT_DUST_COUNT, 0.4, 0.15, 0.4,
                0.03, false
        );
    }

    /**
     * 飞满射程还没落地：原地崩散成碎石。仅服务端。
     */
    public static void fizzle(Level level, Vec3 meteoriteCenter) {
        if (level.isClientSide || level.getServer() == null) {
            return;
        }
        GravityFx.impact(level, meteoriteCenter.x, meteoriteCenter.y, meteoriteCenter.z, FIZZLE_PARTICLE_INTENSITY);
        MagicManager.spawnParticles(
                level, ROCK_DEBRIS,
                meteoriteCenter.x, meteoriteCenter.y, meteoriteCenter.z,
                FIZZLE_DEBRIS_COUNT, 0.2, 0.2, 0.2,
                0.08, false
        );
    }

    /**
     * 盘边一道锯齿电弧：沿盘边一小段弧铺若干火花，每点径向随机抖动，读起来像撕裂虚空的紫色闪电。
     */
    private static void spawnRimLightning(
            Level level,
            Vec3 center,
            Vec3 discRight,
            Vec3 discUp,
            double radiusBlocks,
            int ageTicks,
            RandomSource random
    ) {
        double startAngleRadians = random.nextDouble() * (Math.PI * 2.0) + ageTicks * 0.05;
        GravityFx.withParticleSizeScale(1.3f, () -> {
            for (int segmentIndex = 0; segmentIndex < VOID_LIGHTNING_SEGMENT_COUNT; segmentIndex++) {
                double segmentFraction = segmentIndex / (double) Math.max(1, VOID_LIGHTNING_SEGMENT_COUNT - 1);
                double angleRadians = startAngleRadians + segmentFraction * VOID_LIGHTNING_ARC_SPAN_RADIANS;
                double jitteredRadius = radiusBlocks * (1.0 + (random.nextDouble() * 2.0 - 1.0) * VOID_LIGHTNING_RADIAL_JITTER_FRACTION);
                Vec3 radial = discRight.scale(Math.cos(angleRadians)).add(discUp.scale(Math.sin(angleRadians)));
                Vec3 sparkPosition = center.add(radial.scale(jitteredRadius));
                Vec3 sparkVelocity = radial.scale(0.02 / SPARK_VELOCITY_DAMP);
                level.addParticle(ModParticles.GRAVITY_SPARK.get(),
                        sparkPosition.x, sparkPosition.y, sparkPosition.z,
                        sparkVelocity.x, sparkVelocity.y, sparkVelocity.z);
            }
        });
    }

    /** 盘面右轴：法线 × 世界上方；法线竖直时退回世界 X 轴。 */
    private static Vec3 discRightAxis(Vec3 facing) {
        Vec3 discRight = facing.cross(new Vec3(0.0, 1.0, 0.0));
        if (discRight.lengthSqr() < 1.0e-8) {
            return new Vec3(1.0, 0.0, 0.0);
        }
        return discRight.normalize();
    }

    /** 盘面内均匀随机一点（半径用 sqrt，避免全挤在中心）。 */
    private static Vec3 randomDiscPoint(Vec3 discRight, Vec3 discUp, double radiusBlocks, RandomSource random) {
        double pointRadius = radiusBlocks * Math.sqrt(random.nextDouble());
        double azimuthRadians = random.nextDouble() * (Math.PI * 2.0);
        return discRight.scale(Math.cos(azimuthRadians) * pointRadius)
                .add(discUp.scale(Math.sin(azimuthRadians) * pointRadius));
    }
}

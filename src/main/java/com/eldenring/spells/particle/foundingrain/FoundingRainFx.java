package com.eldenring.spells.particle.foundingrain;

import com.eldenring.spells.spell.FoundingRainOfStarsSpell;

import com.eldenring.spells.particle.foundingrain.OverheadNebulaAccentOptions.Accent;
import com.eldenring.spells.particle.glintstone.GlintstoneFx;
import com.eldenring.spells.registry.ModParticles;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.eldenring.spells.entity.GlintstoneTrailStyle;

/**
 * 创星雨粒子助手。手里星云仍走 {@code GlintstoneFx.starRiver}；
 * 身前气团改由软光面片渲染，这里负责白色小星星和雨针落地涟漪。
 */
public final class FoundingRainFx {

    /**
         * 出手瞬间铺星云的强度，传给 {@code GlintstoneFx.starRiver}。
         * 调大 → 核/雾/臂更密；调小 → 更稀薄。
         */
        public static float CAST_NEBULA_INTENSITY = 1.65f;

        /**
         * 出手后等待多少 tick 再从星云里抽光点（tick）。
         * 星云雾气寿命约 18–28 tick、核约 16–24 tick；取 10 时云已开始淡但仍看得见。
         * 调大 → 光点更晚飞出、云更残；调小 → 几乎刚出手就升空。
         */
        public static int ASCENT_LAUNCH_DELAY_TICKS = 10;

        /**
         * 光点从星云里错峰飞出的窗口（tick）。
         * 调大 → 更像陆续被抽走；调小 → 更齐爆。
         */
        public static int ASCENT_STAGGER_WINDOW_TICKS = 4;

        /**
         * 整波升空光点总数。调大 → 更密的星群；调小 → 更疏、更像图标里那十几道。
         */
        public static int ASCENT_MOTE_COUNT = 16;

        /**
         * 终点相对施法者眼睛的世界上移（方块）。
         */
        public static double ASCENT_TARGET_HEIGHT_ABOVE_EYES_BLOCKS = 4.0;

        /**
         * 终点水平散布半径（方块）。光点不汇成一个像素，而是在头顶铺一小盘星群。
         * 调大 → 更散；调小 → 更收束。
         */
        public static double ASCENT_TARGET_SCATTER_RADIUS_BLOCKS = 1.35;

        /**
         * 终点高度额外抖动半幅（方块）。实际 Y = 目标高度 ± 本值。
         */
        public static double ASCENT_TARGET_HEIGHT_JITTER_BLOCKS = 0.45;

        /**
         * 单颗光点从星云飞到终点的基础寿命（tick）。调大 → 升得更慢、拖尾更长。
         */
        public static int ASCENT_FLIGHT_DURATION_TICKS = 30;

        /**
         * 飞升寿命额外随机上限（tick）。实际寿命 = 基础 + random(0..本值)，避免同时闪灭。
         */
        public static int ASCENT_FLIGHT_DURATION_RANDOM_TICKS = 4;

        /**
         * 升空光点出生时四边形边长（方块）。调大 → 星星更大、更抢眼。
         */
        public static float ASCENT_MOTE_QUAD_SIZE_BLOCKS = 0.11f;

        /**
         * 升空光点尺寸额外随机（方块）。
         */
        public static float ASCENT_MOTE_QUAD_SIZE_RANDOM_BLOCKS = 0.05f;

        /**
         * 每隔多少 tick 在光点身后刷一颗拖尾。1 = 每 tick 都留残影。
         */
        public static int ASCENT_TRAIL_INTERVAL_TICKS = 1;

        /**
         * 拖尾粒子沿飞升方向的速度（方块/tick）。让残影略被拉长，而不是钉在原地。
         */
        public static double ASCENT_TRAIL_STRETCH_SPEED_BLOCKS_PER_TICK = 0.04;

        /**
         * 星云圆心沿水平前方的偏移（方块）。不跟视线俯仰，避免抬头时云贴脸、低头时砸进地面。
         */
        public static double OVERHEAD_CLOUD_FORWARD_OFFSET_BLOCKS = 3.0;

        /**
         * 头顶星云水平半宽（方块）。平铺在天空上的一层，不是一团球。
         */
        public static double OVERHEAD_CLOUD_RADIUS_BLOCKS = 4.0;

        /**
         * 星云沿水平前方的半长（方块）。和半宽接近，铺成扁盘而不是竖着的蛋。
         */
        public static double OVERHEAD_CLOUD_FORWARD_HALF_BLOCKS = 3.6;

        /**
         * 星云竖直厚度（方块）。只给层与层之间留一点点错开，防面片共面闪烁。
         * 调大 → 又变回球体。
         */
        public static double OVERHEAD_CLOUD_SHEET_THICKNESS_BLOCKS = 0.12;

        /**
         * 白色闪星数量。小、密，嵌在气团里。
         */
        public static int OVERHEAD_STAR_COUNT = 56;

        /**
         * 头顶星云存在时长（tick）。20 tick = 1 秒，100 = 5 秒。下雨时长与此相同。
         */
        public static int OVERHEAD_CLOUD_LIFETIME_TICKS = 100;

        /**
         * 淡入时长（tick）。光点消失的那几帧里云从透明胀出来。
         */
        public static int OVERHEAD_CLOUD_FADE_IN_TICKS = 8;

        /**
         * 淡出时长（tick）。调大 → 散得更慢。
         */
        public static int OVERHEAD_CLOUD_FADE_OUT_TICKS = 12;

        /**
         * 单颗云朵寿命额外随机上限（tick）。避免整团同一帧闪灭。
         */
        public static int OVERHEAD_CLOUD_LIFETIME_RANDOM_TICKS = 6;

        /**
         * 云朵粒子出生后的水平游走（方块/tick）。只要一点点，5 秒里挪大约一掌宽。
         */
        public static double OVERHEAD_CLOUD_DRIFT_BLOCKS_PER_TICK = 0.004;

        public static int SPELL_BASE_MANA_COST = 48;
        public static int SPELL_MANA_COST_PER_LEVEL = 8;
        public static int SPELL_BASE_SPELL_POWER = 10;
        public static int SPELL_SPELL_POWER_PER_LEVEL = 1;

        /**
         * 吟唱时长（tick）。本阶段先瞬时出手，好单独看星云→升空；以后若改蓄力再调。
         */
        public static int SPELL_CAST_TIME_TICKS = 0;

        public static double SPELL_COOLDOWN_SECONDS = 8.0;

        /** 最大等级。法环辉石咒固定 1 级。 */
        public static int SPELL_MAX_LEVEL = 1;

        /**
         * 时序实体在雨云淡尽后再活多少 tick。已生成的雨点是独立实体，不靠这段尾巴续命。
         */
        public static int PRELUDE_TAIL_TICKS = 2;

        /**
         * 雨云出现后再隔多少 tick 开始落雨。0 = 乌云一出现就下雨，两者都是 5 秒。
         */
        public static int RAIN_START_DELAY_TICKS = 0;

        /**
         * 每个服务端 tick 从雨云里抽出的雨点数。
         * 调大 → 更密、更卡；调小 → 更疏。光带不是粒子，密度主要吃实体 tick。
         */
        public static int RAIN_DROPS_PER_TICK = 8;

        /**
         * 雨点下落速度（方块/tick），传给 {@code AbstractMagicProjectile#getSpeed()}。
         * 调大 → 更像坠星、光带更短；调小 → 更像细雨、光带更完整。
         */
        public static float RAIN_DROP_FALL_SPEED_BLOCKS_PER_TICK = 1.15f;

        /**
         * 相对竖直方向的水平散布（无量纲，与 (0,-1,0) 相加后再归一化）。
         * 调大 → 雨点更斜、光带更容易看出曲线；调小 → 更接近直落。
         */
        public static double RAIN_DROP_TILT_HORIZONTAL_SPREAD = 0.16;

        /**
         * 出生点相对雨云层再往下挪的距离（方块）。让针尖从气团里钻出来，而不是浮在云顶。
         */
        public static double RAIN_DROP_SPAWN_BELOW_CLOUD_BLOCKS = 0.22;

        /**
         * 雨点从出生点算起，位移超过此距离（方块）就消失。
         * 落地/撞实体会更早销毁；本值是虚空、深坑的保险。
         */
        public static double RAIN_DROP_MAXIMUM_TRAVEL_BLOCKS = 40.0;

        /**
         * 雨点最长寿命（tick）。按最快下落也够飞完 {@link #RAIN_DROP_MAXIMUM_TRAVEL_BLOCKS}，
         * 卡住时不会永远留在世界上。
         */
        public static int RAIN_DROP_MAXIMUM_LIFETIME_TICKS = 48;

        /**
         * 雨幕对同一目标的最短结算间隔（tick）。必须 ≥ 原版受伤无敌窗口（约 10 tick），
         * 否则红闪一次会叠两下，体感骗伤。调大 → 跳数更疏、总伤更低。
         */
        public static int RAIN_ZONE_DAMAGE_INTERVAL_TICKS = 10;

        /**
         * 每次雨幕结算的伤害 = {@code getSpellPower(level, caster) * SPELL_DAMAGE_PER_SPELL_POWER}。
         * 5 秒里大约结算 10 次（间隔 10 tick），系数必须低于单发彗星。
         */
        public static float SPELL_DAMAGE_PER_SPELL_POWER = 0.5f;

        /**
         * 雨点光带：短、细、几乎不点缀粒子。长度单位方块；点数上限配合下落速度，
         * 大约保留 3 tick 真实路径，看起来是一根针而不是彗星尾巴。
         */
        public static com.eldenring.spells.entity.GlintstoneTrailStyle RAIN_DROP_TRAIL_STYLE = new com.eldenring.spells.entity.GlintstoneTrailStyle(
                3.2,
                0.038f,
                0.007f,
                0.0f,
                0.0f,
                12
        );

        /**
         * 雨点光芯 ARGB。近白、略带紫，叠在紫色外辉上才读得出「白紫雨针」。
         */
        public static int RAIN_DROP_CORE_COLOR_ARGB = 0xFFF4EEFF;

        /**
         * 雨点外辉 ARGB。饱和紫；外层还会再乘光束透明度倍率，所以这里 alpha 给满。
         */
        public static int RAIN_DROP_GLOW_COLOR_ARGB = 0xFFB060FF;

        /**
         * 落地涟漪四边形边长下限（方块）。贴地水平展开，调大 → 水圈更大。
         */
        public static float RAIN_RIPPLE_QUAD_SIZE_MIN_BLOCKS = 0.55f;

        /**
         * 落地涟漪额外随机边长（方块）。
         */
        public static float RAIN_RIPPLE_QUAD_SIZE_RANDOM_BLOCKS = 0.22f;

        /**
         * 落地涟漪寿命（tick）。三帧贴图按寿命播完；调大 → 水圈胀得更慢。
         */
        public static int RAIN_RIPPLE_LIFETIME_TICKS = 11;

        /**
         * 第一批升空光点到达顶点、开始消失的 tick。
         * 等于等待 + 基础飞升；错峰只有 4 tick，看起来就是「消失的一瞬间」。
         */
        public static int overheadCloudSpawnTick() {
            return ASCENT_LAUNCH_DELAY_TICKS + ASCENT_FLIGHT_DURATION_TICKS;
        }

        /**
         * 时序实体开始从雨云里抽雨点的 tick。
         */
        public static int rainStartTick() {
            return overheadCloudSpawnTick() + RAIN_START_DELAY_TICKS;
        }

        /**
         * 停止生成新雨点、停止雨幕伤害的 tick。与乌云同寿，整段都是 5 秒。
         */
        public static int rainEndTick() {
            return overheadCloudSpawnTick() + FoundingRainOfStarsSpell.OVERHEAD_CLOUD_LIFETIME_TICKS;
        }

        /**
         * 时序实体最长寿命（tick）= 雨云出现 + 雨云寿命 + 收尾。
         */
        public static int sequenceLifetimeTicks() {
            return overheadCloudSpawnTick()
                    + FoundingRainOfStarsSpell.OVERHEAD_CLOUD_LIFETIME_TICKS
                    + OVERHEAD_CLOUD_LIFETIME_RANDOM_TICKS
                    + PRELUDE_TAIL_TICKS;
        }

    private FoundingRainFx() {
    }

    /**
     * 星云圆心：施法者当时眼睛的水平前方 {@link FoundingRainFx#OVERHEAD_CLOUD_FORWARD_OFFSET_BLOCKS} 格，
     * 高度是眼高 + {@link FoundingRainFx#ASCENT_TARGET_HEIGHT_ABOVE_EYES_BLOCKS}。
     * 只应在出手瞬间调用一次，把结果存进时序实体。
     */
    public static Vec3 cloudCenterInFrontOf(LivingEntity caster) {
        Vec3 lookDirection = caster.getLookAngle();
        Vec3 horizontalForward = new Vec3(lookDirection.x, 0.0, lookDirection.z);
        if (horizontalForward.lengthSqr() < 1.0e-8) {
            float yawRadians = caster.getYRot() * Mth.DEG_TO_RAD;
            horizontalForward = new Vec3(-Math.sin(yawRadians), 0.0, Math.cos(yawRadians));
        } else {
            horizontalForward = horizontalForward.normalize();
        }
        return caster.getEyePosition()
                .add(horizontalForward.scale(FoundingRainFx.OVERHEAD_CLOUD_FORWARD_OFFSET_BLOCKS))
                .add(0.0, FoundingRainFx.ASCENT_TARGET_HEIGHT_ABOVE_EYES_BLOCKS, 0.0);
    }

    /**
     * 在星云带里撒白色小星星。气团本身不走粒子，避免方块边。
     *
     * @param cloudYawDegrees 施法瞬间偏航，星星沿横向条带散布
     */
    public static void spawnOverheadStars(Level level, Vec3 cloudCenter, float cloudYawDegrees) {
        float yawRadians = cloudYawDegrees * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(Math.cos(yawRadians), 0.0, Math.sin(yawRadians));
        Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0, Math.cos(yawRadians));
        int starCount = FoundingRainFx.OVERHEAD_STAR_COUNT;
        double halfWidthBlocks = FoundingRainOfStarsSpell.OVERHEAD_CLOUD_RADIUS_BLOCKS;
        double halfDepthBlocks = FoundingRainFx.OVERHEAD_CLOUD_FORWARD_HALF_BLOCKS;
        double halfHeightBlocks = FoundingRainFx.OVERHEAD_CLOUD_SHEET_THICKNESS_BLOCKS;
        for (int starIndex = 0; starIndex < starCount; starIndex++) {
            double alongRight = (level.random.nextDouble() * 2.0 - 1.0) * halfWidthBlocks;
            double alongForward = (level.random.nextDouble() * 2.0 - 1.0) * halfDepthBlocks;
            double alongUp = (level.random.nextDouble() * 2.0 - 1.0) * halfHeightBlocks;
            // 椭圆：两端更稀，中间更密。
            double radial01 = Math.sqrt(
                    (alongRight / halfWidthBlocks) * (alongRight / halfWidthBlocks)
                            + (alongForward / Math.max(0.2, halfDepthBlocks)) * (alongForward / Math.max(0.2, halfDepthBlocks))
            );
            if (radial01 > 1.0 && level.random.nextDouble() < 0.55) {
                continue;
            }
            Vec3 starPosition = cloudCenter
                    .add(right.scale(alongRight))
                    .add(forward.scale(alongForward))
                    .add(0.0, alongUp, 0.0);
            spawnAccent(level, Accent.MOTE, starPosition, drift(level));
        }
    }

    /**
     * 从右手星云体内抽样，把光点射向已经钉死的雨云圆心。
     */
    public static void starRiverAscent(
            Level level,
            LivingEntity caster,
            int moteCountThisTick,
            Vec3 gatheringCenter
    ) {
        GlintstoneFx.starRiverAscent(
                level,
                caster,
                moteCountThisTick,
                gatheringCenter,
                ASCENT_TARGET_SCATTER_RADIUS_BLOCKS,
                ASCENT_TARGET_HEIGHT_JITTER_BLOCKS
        );
    }

    /**
     * 雨针命中时的反馈。地面刷贴地涟漪 + 飞沫；只碰到实体时只喷飞沫，避免人身上长出水圈。
     *
     * @param hitGround {@code true} 时才生成水平涟漪
     */
    public static void spawnRainImpact(Level level, double impactX, double impactY, double impactZ, boolean hitGround) {
        if (hitGround) {
            spawnForced(
                    level,
                    ModParticles.STAR_RIVER_RIPPLE.get(),
                    impactX,
                    impactY + 0.04,
                    impactZ,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }
        spawnForced(
                level,
                ModParticles.STAR_RIVER_SPRAY.get(),
                impactX,
                impactY + 0.08,
                impactZ,
                1,
                0.04,
                0.10,
                0.04,
                0.08
        );
    }

    private static void spawnForced(
            Level level,
            ParticleOptions particle,
            double x,
            double y,
            double z,
            int count,
            double xSpread,
            double ySpread,
            double zSpread,
            double speed
    ) {
        MagicManager.spawnParticles(
                level,
                particle,
                x,
                y,
                z,
                count,
                xSpread,
                ySpread,
                zSpread,
                speed,
                true
        );
    }

    private static Vec3 drift(Level level) {
        double speed = FoundingRainFx.OVERHEAD_CLOUD_DRIFT_BLOCKS_PER_TICK;
        return new Vec3(
                (level.random.nextDouble() - 0.5) * 2.0 * speed,
                (level.random.nextDouble() - 0.5) * speed,
                (level.random.nextDouble() - 0.5) * 2.0 * speed
        );
    }

    private static void spawnAccent(Level level, Accent accent, Vec3 position, Vec3 velocity) {
        boolean hasDirectedVelocity = velocity.lengthSqr() > 1.0e-10;
        MagicManager.spawnParticles(
                level,
                new OverheadNebulaAccentOptions(accent),
                position.x,
                position.y,
                position.z,
                hasDirectedVelocity ? 0 : 1,
                hasDirectedVelocity ? velocity.x : 0.01,
                hasDirectedVelocity ? velocity.y : 0.01,
                hasDirectedVelocity ? velocity.z : 0.01,
                hasDirectedVelocity ? 1.0 : 0.0,
                true
        );
    }
}

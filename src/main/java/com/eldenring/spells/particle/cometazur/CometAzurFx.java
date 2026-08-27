package com.eldenring.spells.particle.cometazur;

import com.eldenring.spells.registry.ModParticles;
import com.eldenring.spells.spell.data.CometAzurCastData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 彗星亚兹勒视觉入口。
 * <p>
 * 蓄力：服务端刷两层旋转中心；主层在客户端按对数螺线铺汇聚粒子。
 * 蓄力结束：同一位置爆星辰涟漪。
 * 喷流：法术 tick 维持 {@link com.eldenring.spells.entity.CometAzurJetEntity}（圆管星河柱），
 * 并在喷流口刷墨绿星云 / 星团 / 闪星套管。
 * 只在服务端调用粒子入口；客户端再调会双端各刷一次。
 */
public final class CometAzurFx {

    public static int SPELL_MAX_LEVEL = 5;
        /**
         * 冷却（秒）。与辉石彗星同档：连续吟唱结束后很快就能再起手。
         * 原先 18 秒对点按/中途松手都太长。
         */
        public static double SPELL_COOLDOWN_SECONDS = 1.2;
        /**
         * 每次脉冲蓝耗。CONTINUOUS 约每 10 tick 扣一次，UI「每秒」大约是这个数 ×2。
         * 喷流写好后，没蓝会在下一次脉冲停吟唱。
         */
        public static int SPELL_BASE_MANA_COST = 10;
        public static int SPELL_MANA_COST_PER_LEVEL = 2;
        public static int SPELL_BASE_SPELL_POWER = 8;
        public static int SPELL_SPELL_POWER_PER_LEVEL = 2;

        /**
         * CONTINUOUS 最长按住时间（tick）。含 2 秒蓄力，剩下的是以后喷流能持续的上限。
         * 400 tick = 20 秒；蓝不够会更早停。调大 → 蓝够就能喷更久。
         */
        public static int SPELL_CAST_TIME_TICKS = 400;

        /**
         * 判定「正在下落、禁止起手」的竖直速度阈值（方块 / tick）。必须为负。
         * 比这个更朝下就不能放，避免半空按下被钉死；跳跃上升和顶点附近（速度接近 0）仍可空放。
         */
        public static double CAST_FALLING_Y_VELOCITY_THRESHOLD_BLOCKS_PER_TICK = -0.10;

        /**
         * 坠落距离超过这个值也算下落（方块）。走下半级台阶那种一点点不算。
         */
        public static float CAST_FALLING_MIN_DISTANCE_BLOCKS = 0.35f;

        /**
         * 蓄力 / 对数螺线汇聚时长（tick）。20 tick = 1 秒，40 = 2 秒。
         * 这段结束之后才进入喷流（ribbon 星河柱 + 周围粒子）。
         */
        public static int STARTUP_DURATION_TICKS = 40;

        /**
         * 蓄力漩涡相对眼睛沿朝向的前方距离（方块）。只影响汇聚盘，喷流口见 {@link #JET_BEAM_MOUTH_FORWARD_OFFSET_BLOCKS}。
         */
        public static double STARTUP_VORTEX_FORWARD_OFFSET_BLOCKS = 1.05;

        /**
         * 漩涡中心相对眼睛向下偏（方块）。正值往下，避免整团糊在准星上。
         */
        public static double STARTUP_VORTEX_DOWN_OFFSET_BLOCKS = 0.22;

        /**
         * {@code comet_azur_shrink_2} 中心层四边形边长（方块）。
         */
        public static float STARTUP_SHRINK_2_QUAD_SIZE_BLOCKS = 2.55f;

        /**
         * {@code comet_azur_shrink_1} 叠加层四边形边长（方块）。略小、反向转。
         */
        public static float STARTUP_SHRINK_1_QUAD_SIZE_BLOCKS = 2.05f;

        /**
         * shrink_2 平面旋转角速度（弧度 / tick）。正值逆时针。
         * 0.32 ≈ 2 秒转约 2 圈。
         */
        public static float STARTUP_SHRINK_2_ROLL_RADIANS_PER_TICK = 0.32f;

        /**
         * shrink_1 角速度（弧度 / tick）。负值 = 与主层反向。
         */
        public static float STARTUP_SHRINK_1_ROLL_RADIANS_PER_TICK = -0.22f;

        /**
         * 对数螺线极角上限（弧度）。{@code A} 从 0 走到这个值，对应 {@code 12π}（6 圈）。
         */
        public static float STARTUP_SPIRAL_MAX_ANGLE_RADIANS = (float) (12.0 * Math.PI);

        /**
         * {@code A = 0} 时的世界半径（方块）。公式是 {@code r = 外半径 × e^(wA)}。
         * 调大 → 螺线更散、蓄力盘更大。
         */
        public static float STARTUP_SPIRAL_OUTER_RADIUS_BLOCKS = 5.8f;

        /**
         * 对数螺线增长率 {@code w}（1/弧度）。必须为负才会往中心收：
         * {@code r = e^(wA)}，A 增大时 r 变小。
         * 每个 w 是一条松紧不同的汇聚曲线；越负收得越快、越贴中心。
         */
        public static float[] STARTUP_SPIRAL_W_PER_CURVE = {
                -0.040f,
                -0.062f,
                -0.090f,
                -0.120f,
                -0.155f
        };

        /**
         * 把同一组 w 再绕法线复制成几条臂，避免所有曲线叠在同一条角上。
         * 调成 1 就只看 w 本身的多条嵌套螺线。
         */
        public static int STARTUP_SPIRAL_ARM_COUNT = 4;

        /**
         * 每条（w, 臂）上沿 A 铺多少颗粒子。粒子在 2 秒内从各自的 A 走到 {@code 12π}，整条螺线一起收进中心。
         * 调大更密；总数 ≈ w条数 × 臂数 × 本值。
         */
        public static int STARTUP_SPIRAL_SAMPLES_PER_CURVE = 10;

        /** 闪星（mote_1 / mote_2）四边形边长（方块）。 */
        public static float STARTUP_MOTE_QUAD_SIZE_BLOCKS = 0.13f;

        /** 十字冲击星（impact）四边形边长（方块）。 */
        public static float STARTUP_IMPACT_QUAD_SIZE_BLOCKS = 0.19f;

        /** 星团头（head）四边形边长（方块）。 */
        public static float STARTUP_HEAD_QUAD_SIZE_BLOCKS = 0.26f;

        /** 星尘（dust）四边形边长（方块）。 */
        public static float STARTUP_DUST_QUAD_SIZE_BLOCKS = 0.24f;

        /**
         * 蓄力结束后的星辰涟漪时长（tick）。18 tick ≈ 0.9 秒，给多层环留展开时间。
         */
        public static int SHOCKWAVE_DURATION_TICKS = 18;

        /**
         * 主星环起始半径（方块）。
         */
        public static float SHOCKWAVE_RADIUS_START_BLOCKS = 0.45f;

        /**
         * 主星环结束半径（方块）。调大 → 冲击波更开。
         */
        public static float SHOCKWAVE_RADIUS_END_BLOCKS = 7.5f;

        /**
         * 最外嵌套环结束半径倍率（相对主环）。多层嵌套里最外一圈。
         */
        public static float SHOCKWAVE_OUTER_RADIUS_END_SCALE = 1.35f;

        /**
         * 中层嵌套环结束半径倍率。
         */
        public static float SHOCKWAVE_MID_RADIUS_END_SCALE = 1.12f;

        /**
         * 内层嵌套环结束半径倍率（比主环略小，形成里圈）。
         */
        public static float SHOCKWAVE_INNER_RADIUS_END_SCALE = 0.72f;

        /**
         * 中心绽光半径：从这么大胀到 {@link #SHOCKWAVE_CORE_RADIUS_END_BLOCKS}。
         */
        public static float SHOCKWAVE_CORE_RADIUS_START_BLOCKS = 0.28f;

        public static float SHOCKWAVE_CORE_RADIUS_END_BLOCKS = 1.35f;

        /** 中心绽光寿命（tick），比外环更短，先亮一下再让星环接手。 */
        public static int SHOCKWAVE_CORE_DURATION_TICKS = 8;

        /**
         * 主星环自旋（弧度 / tick）。轻微即可，太快会不像涟漪。
         */
        public static float SHOCKWAVE_RING_ROLL_RADIANS_PER_TICK = 0.055f;

        /**
         * 回声星环反向自旋。
         */
        public static float SHOCKWAVE_ECHO_ROLL_RADIANS_PER_TICK = -0.07f;

        /**
         * 跟着波前走的星点数。构成「一圈星」。
         */
        public static int SHOCKWAVE_RIDING_STAR_COUNT = 22;

        /**
         * 每 2 tick 在当前半径留下的星尘数。留在原地淡出，形成星辰涟漪的余波。
         */
        public static int SHOCKWAVE_RESIDUE_STARS_PER_PULSE = 8;

        public static int SHOCKWAVE_RESIDUE_INTERVAL_TICKS = 2;

        /** 余波星尘寿命（tick）。 */
        public static int SHOCKWAVE_RESIDUE_LIFETIME_TICKS = 7;

        /** 波前星点沿圆周额外转过的角速度（弧度 / tick），让星圈微微旋开。 */
        public static float SHOCKWAVE_STAR_SWIRL_RADIANS_PER_TICK = 0.040f;

        /** 星点 / 星尘四边形边长（方块）。 */
        public static float SHOCKWAVE_ACCENT_QUAD_SIZE_BLOCKS = 0.14f;

        /** 径向彗星残影数量。只在爆开瞬间沿半径甩出去。 */
        public static int SHOCKWAVE_STREAK_COUNT = 10;

        // -------------------------------------------------------------------------
        // 喷流周围粒子：星云缩小后熔进激光里，加法混合，不要半透明大烟团。
        // 贴图顺序见 particles/comet_azur_jet_surround.json，必须与 Kind 枚举一致。
        // -------------------------------------------------------------------------

        /**
         * 每隔多少 tick 在喷流口刷一圈周围粒子。1 = 每 tick 一圈；调大 → 更稀、更省。
         */
        public static int JET_SURROUND_SPAWN_INTERVAL_TICKS = 2;

        /**
         * 贴着圆柱的星云纹理颗数。haze / mist / stardust / wisp。
         * 必须小、贴轴，只给激光一层星云颗粒，不要喷烟。
         */
        public static int JET_NEBULA_PARTICLE_COUNT = 5;

        /**
         * 激光内部星系颗数。spiral / cluster / mist / dust。
         */
        public static int JET_GALAXY_PARTICLE_COUNT = 8;

        /**
         * 激光内部闪星 / 残影颗数。nova / head / mote / streak / filament。
         */
        public static int JET_SPARKLE_PARTICLE_COUNT = 10;

        /**
         * 沿喷流中段补星点的颗数。只补小星团，不补大雾。
         */
        public static int JET_FILL_PARTICLE_COUNT = 5;

        /**
         * 中段补粒子最远出生距离（方块）。必须小于 {@link #JET_PARTICLE_MAX_ALONG_BLOCKS}，留给剩余寿命往前飞。
         */
        public static float JET_FILL_MAX_ALONG_BLOCKS = 52.0f;

        /**
         * 星云出生圆半径（方块）。贴加粗后的圆柱外皮，几乎等于口半径。
         * 再大会再次喷出一圈烟。
         */
        public static float JET_NEBULA_RING_RADIUS_BLOCKS = 0.32f;

        /**
         * 星系出生圆半径（方块）。在圆柱截面里转。
         */
        public static float JET_GALAXY_RING_RADIUS_BLOCKS = 0.22f;

        /**
         * 闪星出生圆半径（方块）。贴亮芯。
         */
        public static float JET_SPARKLE_RING_RADIUS_BLOCKS = 0.14f;

        /**
         * 圆周槽位随机偏移（弧度）。避免每圈都叠在同一组齿轮齿上。
         */
        public static float JET_RING_SLOT_JITTER_RADIANS = 0.18f;

        /**
         * 出生半径相对标称值的随机比例下限。靠近 1 → 粒子更贴激光。
         */
        public static float JET_RING_RADIUS_RANDOM_MIN_SCALE = 0.88f;

        /**
         * 出生半径相对标称值的随机比例上限。
         */
        public static float JET_RING_RADIUS_RANDOM_MAX_SCALE = 1.12f;

        /**
         * 星云前向速度（方块 / tick）。必须跟激光差不多快，慢了就会拖成烟尾。
         */
        public static float JET_NEBULA_FORWARD_SPEED_BLOCKS_PER_TICK = 1.50f;

        /**
         * 星云寿命（tick）。1.50 × 40 = 60 格。
         */
        public static int JET_NEBULA_LIFETIME_TICKS = 40;

        /**
         * 星云绕轴角速度（弧度 / tick）。让星云纹理在激光里转，而不是原地漂。
         */
        public static float JET_NEBULA_HELIX_RADIANS_PER_TICK = 0.10f;

        /**
         * 星系初始前向速度（方块 / tick）。
         */
        public static float JET_GALAXY_FORWARD_SPEED_BLOCKS_PER_TICK = 1.50f;

        /**
         * 星系前向加速度（方块 / tick²）。0 = 匀速飞满 60 格，不再冲出射程。
         */
        public static float JET_GALAXY_FORWARD_ACCELERATION_BLOCKS_PER_TICK_SQUARED = 0.0f;

        /**
         * 星系绕喷流轴角速度（弧度 / tick）。读成激光里的星河臂。
         */
        public static float JET_GALAXY_HELIX_RADIANS_PER_TICK = 0.18f;

        /**
         * 星系寿命（tick）。1.50 × 40 = 60 格。
         */
        public static int JET_GALAXY_LIFETIME_TICKS = 40;

        /**
         * 闪星前向速度（方块 / tick）。
         */
        public static float JET_SPARKLE_FORWARD_SPEED_BLOCKS_PER_TICK = 1.50f;

        /**
         * 闪星寿命（tick）。1.50 × 40 = 60 格。
         */
        public static int JET_SPARKLE_LIFETIME_TICKS = 40;

        /**
         * 轨迹噪声振幅（方块）。必须很小，否则星云会飞出激光变成烟。
         */
        public static float JET_NOISE_AMPLITUDE_BLOCKS = 0.08f;

        /**
         * 噪声随时间变化的速率（1 / tick）。调大 → 抖得更碎。
         */
        public static float JET_NOISE_FREQUENCY_PER_TICK = 0.22f;

        /**
         * 闪星组噪声相对星云组的比例。1 = 一样乱；0 = 完全直线。
         */
        public static float JET_SPARKLE_NOISE_SCALE = 0.45f;

        /**
         * 中层星系噪声相对星云组的比例。
         */
        public static float JET_GALAXY_NOISE_SCALE = 0.70f;

        /**
         * 对螺旋极角的噪声振幅（弧度）。让齿距也不均匀。
         */
        public static float JET_NOISE_ANGLE_AMPLITUDE_RADIANS = 0.10f;

        /** 星云薄雾四边形边长（方块）。略大于圆柱直径，给激光一层纹理。 */
        public static float JET_HAZE_QUAD_SIZE_BLOCKS = 0.38f;

        /** 星河雾（带星点）四边形边长（方块）。 */
        public static float JET_STAR_RIVER_MIST_QUAD_SIZE_BLOCKS = 0.32f;

        /** 星尘团四边形边长（方块）。 */
        public static float JET_STARDUST_QUAD_SIZE_BLOCKS = 0.28f;

        /** 星云飘絮四边形边长（方块）。 */
        public static float JET_WISP_QUAD_SIZE_BLOCKS = 0.26f;

        /** 迷你漩涡星系四边形边长（方块）。 */
        public static float JET_SPIRAL_QUAD_SIZE_BLOCKS = 0.24f;

        /** 微星团四边形边长（方块）。 */
        public static float JET_CLUSTER_QUAD_SIZE_BLOCKS = 0.16f;

        /** 亚兹勒星雾四边形边长（方块）。 */
        public static float JET_COMET_MIST_QUAD_SIZE_BLOCKS = 0.22f;

        /** 星尘四边形边长（方块）。 */
        public static float JET_DUST_QUAD_SIZE_BLOCKS = 0.14f;

        /** 碎星团头四边形边长（方块）。 */
        public static float JET_HEAD_QUAD_SIZE_BLOCKS = 0.14f;

        /** 八芒新星四边形边长（方块）。 */
        public static float JET_NOVA_QUAD_SIZE_BLOCKS = 0.11f;

        /** 暗物质丝四边形边长（方块）。 */
        public static float JET_FILAMENT_QUAD_SIZE_BLOCKS = 0.26f;

        /** 彗星残影四边形边长（方块）。 */
        public static float JET_STREAK_QUAD_SIZE_BLOCKS = 0.20f;

        /** 十字闪星四边形边长（方块）。 */
        public static float JET_MOTE_QUAD_SIZE_BLOCKS = 0.08f;

        /**
         * 粒子贴图染色（0xRRGGBB）。全部走加法，必须够亮才能在激光里看见；
         * 太暗会变成灰烟，太白会洗掉墨绿。
         */
        public static int JET_TINT_HAZE_RGB = 0x4A9A68;
        public static int JET_TINT_STAR_RIVER_MIST_RGB = 0x5CB880;
        public static int JET_TINT_STARDUST_RGB = 0x6EC894;
        public static int JET_TINT_WISP_RGB = 0x48A070;
        public static int JET_TINT_SPIRAL_RGB = 0x7ED4A4;
        public static int JET_TINT_CLUSTER_RGB = 0xC8F0DC;
        public static int JET_TINT_COMET_MIST_RGB = 0x6EC890;
        public static int JET_TINT_DUST_RGB = 0xA8E0C0;
        public static int JET_TINT_HEAD_RGB = 0xD8F8E8;
        public static int JET_TINT_NOVA_RGB = 0xF0FFF8;
        public static int JET_TINT_FILAMENT_RGB = 0x62C090;
        public static int JET_TINT_STREAK_RGB = 0x8ED8B0;
        public static int JET_TINT_MOTE_RGB = 0xE8FFF4;

        // -------------------------------------------------------------------------
        // 喷流外围能量场：多条欧拉螺旋 + 几道直线，半径大于圆柱本体
        // -------------------------------------------------------------------------

        /**
         * 欧拉螺旋臂条数。每条臂用不同角速度，看起来是好几条拧着的能量曲线。
         */
        public static int JET_FIELD_EULER_ARM_COUNT = 6;

        /**
         * 每条欧拉臂每次脉冲刷几颗小粒子。调大更密、更连成线。
         */
        public static int JET_FIELD_EULER_PARTICLES_PER_ARM = 4;

        /**
         * 直线能量线数量。围着喷流均匀排开，沿朝向直冲。
         */
        public static int JET_FIELD_STRAIGHT_LINE_COUNT = 6;

        /**
         * 沿喷流中段补能量场的颗数。开喷瞬间外围就已经有螺旋/直线，不用等飞满 60 格。
         */
        public static int JET_FIELD_FILL_PARTICLE_COUNT = 14;

        /**
         * 欧拉能量场出生圆半径（方块）。必须大于圆柱视觉半径，粒子环绕在喷流外面。
         */
        public static float JET_FIELD_EULER_RING_RADIUS_BLOCKS = 1.38f;

        /**
         * 直线能量线出生圆半径（方块）。略小于欧拉臂，仍大于圆柱。
         */
        public static float JET_FIELD_STRAIGHT_RING_RADIUS_BLOCKS = 1.12f;

        /**
         * 各条欧拉臂的角速度（弧度 / tick）。正负交错 = 反向拧。
         * 调大 → 螺旋更密；调小 → 更像慢转套管。
         */
        public static float[] JET_FIELD_EULER_RADIANS_PER_TICK = {
                0.22f,
                0.31f,
                -0.18f,
                -0.27f,
                0.14f,
                -0.35f
        };

        /**
         * 能量场前向速度（方块 / tick）。1.50 × 40 = 60 格，和喷流射程对齐。
         */
        public static float JET_FIELD_FORWARD_SPEED_BLOCKS_PER_TICK = 1.50f;

        /**
         * 能量场寿命上限（tick）。实际寿命还会按剩余 60 格路程截断。
         */
        public static int JET_FIELD_LIFETIME_TICKS = 40;

        /** 能量场冲击星四边形边长（方块）。小颗，用来画螺旋点。 */
        public static float JET_FIELD_IMPACT_QUAD_SIZE_BLOCKS = 0.16f;

        /** 能量场 mote_1 四边形边长（方块）。 */
        public static float JET_FIELD_MOTE_1_QUAD_SIZE_BLOCKS = 0.14f;

        /** 能量场 mote_2 四边形边长（方块）。 */
        public static float JET_FIELD_MOTE_2_QUAD_SIZE_BLOCKS = 0.14f;

        /** 能量场辉石闪星四边形边长（方块）。 */
        public static float JET_FIELD_GLINT_MOTE_QUAD_SIZE_BLOCKS = 0.15f;

        /**
         * 直线能量线用的横向光带边长（方块）。略长，连起来像一道细光丝。
         */
        public static float JET_FIELD_GLOW_QUAD_SIZE_BLOCKS = 0.34f;

        /**
         * 能量场小粒子染色。贴图本身已是青绿，乘近白，别再染成灰绿。
         */
        public static int JET_TINT_FIELD_IMPACT_RGB = 0xF2FFF8;
        public static int JET_TINT_FIELD_MOTE_1_RGB = 0xEEFFF6;
        public static int JET_TINT_FIELD_MOTE_2_RGB = 0xF4FFF9;
        public static int JET_TINT_FIELD_GLINT_MOTE_RGB = 0xE8FFF4;
        public static int JET_TINT_FIELD_GLOW_RGB = 0xC8F4E0;

        // -------------------------------------------------------------------------
        // 星河喷流本体（圆球口 + 圆柱管，不是十字扁带）
        // -------------------------------------------------------------------------

        /**
         * 喷流最大射程（方块）。撞到实心方块会提前截断。周围粒子也不能飞过这个距离。
         */
        public static double JET_BEAM_MAX_RANGE_BLOCKS = 60.0;

        /**
         * 粒子沿喷流最远距离（方块）。与喷流射程相同，超出立刻消失。
         */
        public static float JET_PARTICLE_MAX_ALONG_BLOCKS = (float) JET_BEAM_MAX_RANGE_BLOCKS;

        /**
         * 圆柱沿程分段数。60 格需要足够圈才圆滑。
         */
        public static int JET_BEAM_CYLINDER_RING_COUNT = 22;

        /**
         * 圆柱圆周面数。12 已经能读成圆管；再少侧面会显棱。
         */
        public static int JET_BEAM_CYLINDER_SIDE_COUNT = 12;

        /**
         * 口部圆球纬度分段。
         */
        public static int JET_BEAM_SPHERE_STACK_COUNT = 8;

        /**
         * 口部圆球经度分段。
         */
        public static int JET_BEAM_SPHERE_SLICE_COUNT = 12;

        /**
         * 螺旋细丝中轴采样点数。曲线仍用 ribbon，本体才走圆柱网格。
         */
        public static int JET_BEAM_SAMPLE_COUNT = 42;

        /**
         * 喷流口相对眼睛沿朝向的前方距离（方块）。不跟蓄力漩涡下偏。
         * 1.5 格：圆球和圆柱从身前稍远处开始，避免贴在脸上。
         */
        public static double JET_BEAM_MOUTH_FORWARD_OFFSET_BLOCKS = 1.5;

        /**
         * 口部圆球半径（方块）。圆柱从这颗球里长出来。调大 → 起点光球更胖。
         */
        public static float JET_BEAM_ORIGIN_SPHERE_RADIUS_BLOCKS = 0.78f;

        /**
         * 口部圆形光晕半径（方块）。相机朝向的软光斑，把网格球收成圆轮廓。
         */
        public static float JET_BEAM_ORIGIN_GLOW_RADIUS_BLOCKS = 1.08f;

        /**
         * 圆柱口半径（方块）。喷流本体加粗后，能量场还要再大一圈。
         */
        public static float JET_BEAM_MOUTH_RADIUS_BLOCKS = 0.58f;

        /**
         * 圆柱远端半径（方块）。略粗于口，整根更像一条星河柱。
         */
        public static float JET_BEAM_TIP_RADIUS_BLOCKS = 0.78f;

        /**
         * 螺旋细丝在喷流口的半径倍率（相对 {@link #JET_BEAM_FILAMENT_RADIUS_BLOCKS}）。
         * 1 = 口就已经是标称半径。
         */
        public static float JET_BEAM_FILAMENT_MOUTH_RADIUS_SCALE = 1.00f;

        /**
         * 螺旋细丝在远端的半径倍率。略小于口，保留一点粗细渐变。
         */
        public static float JET_BEAM_FILAMENT_TIP_RADIUS_SCALE = 0.82f;

        /**
         * 喷流口采样 {@code trail_beam.png} 的 V。贴图 V=0 是亮头、V=1 是透明淡尾。
         * 必须靠近 0：拖尾默认把起点映射到 V=1，别人侧面看口部会空出好几格。
         */
        public static float JET_BEAM_TEXTURE_MOUTH_V = 0.04f;

        /**
         * 远端采样 V。0.50 仍在亮带里，整根柱子都实；调到 1 会在远处淡没。
         */
        public static float JET_BEAM_TEXTURE_TIP_V = 0.50f;

        /**
         * 外层墨绿雾半径倍率（相对圆柱半径）。贴着细芯，不要再鼓成一圈烟。
         */
        public static float JET_BEAM_VEIL_WIDTH_SCALE = 1.45f;

        /**
         * 中层星云半径倍率。
         */
        public static float JET_BEAM_NEBULA_WIDTH_SCALE = 1.18f;

        /**
         * 亮芯半径倍率。
         */
        public static float JET_BEAM_CORE_WIDTH_SCALE = 0.42f;

        /**
         * 螺旋细丝条数。绕中轴错相位，做出星河丝缕。
         */
        public static int JET_BEAM_FILAMENT_COUNT = 5;

        /**
         * 螺旋细丝绕轴半径（方块）。贴着圆柱外转，不要比柱子粗一圈。
         */
        public static float JET_BEAM_FILAMENT_RADIUS_BLOCKS = 0.66f;

        /**
         * 螺旋细丝半宽（方块）。细曲线，不是第二根柱。
         */
        public static float JET_BEAM_FILAMENT_HALF_WIDTH_BLOCKS = 0.08f;

        /**
         * 螺旋沿程扭率（弧度 / 方块）。略松，读成星河臂而不是电缆。
         */
        public static float JET_BEAM_FILAMENT_TWIST_RADIANS_PER_BLOCK = 0.11f;

        /**
         * 螺旋整体自旋（弧度 / tick）。让星河在持续喷射时缓缓转动。
         */
        public static float JET_BEAM_FILAMENT_SPIN_RADIANS_PER_TICK = 0.048f;

        /**
         * 中轴横向波纹振幅（方块）。轻微起伏，避免死直线。
         */
        public static float JET_BEAM_RIVER_WAVE_AMPLITUDE_BLOCKS = 0.10f;

        /**
         * 中轴波纹空间频率（1 / 方块）。
         */
        public static float JET_BEAM_RIVER_WAVE_FREQUENCY_PER_BLOCK = 0.38f;

        /**
         * 中轴波纹时间相位速率（弧度 / tick）。
         */
        public static float JET_BEAM_RIVER_WAVE_PHASE_RADIANS_PER_TICK = 0.10f;

        /**
         * 伤害结算圆柱半径（方块）。略大于视觉雾层，擦边的目标也能吃到伤害。
         * 判定用目标包围盒中心到射线的距离，所以要比柱子再宽一点。
         */
        public static float JET_BEAM_DAMAGE_RADIUS_BLOCKS = 1.28f;

        /**
         * 每隔多少 tick 结算一次射线伤害。CONTINUOUS 已按脉冲扣蓝，伤害别每 tick 满额。
         */
        public static int JET_BEAM_DAMAGE_INTERVAL_TICKS = 4;

        /**
         * 每次伤害结算 = 法术强度 × 本系数。
         */
        public static float JET_BEAM_DAMAGE_PER_SPELL_POWER = 0.55f;

        /**
         * 墨绿色星河配色（ARGB）。暗部墨洗绿、芯偏薄荷青玉，避免辉石那种亮青塑料。
         * 参考：#0C2018 / #143828 / #1C5840 / #3D9A72 / #8FD4B0
         */
        public static int JET_BEAM_VEIL_COLOR_ARGB = 0x8A0C2018;
        public static int JET_BEAM_NEBULA_COLOR_ARGB = 0xC0143828;
        public static int JET_BEAM_MID_COLOR_ARGB = 0xD01C5840;
        public static int JET_BEAM_CORE_COLOR_ARGB = 0xF05CB890;
        public static int JET_BEAM_FILAMENT_COLOR_ARGB = 0xE0287858;
        public static int JET_BEAM_FILAMENT_ALT_COLOR_ARGB = 0xC0184834;

    private CometAzurFx() {
    }

    /**
     * 在施法者视线前方钉一个 2 秒漩涡。位置和朝向按出手瞬间计算，之后不跟随玩家。
     */
    public static void spawnStartupVortex(Level level, LivingEntity caster) {
        if (level.isClientSide || caster == null) {
            return;
        }
        Vec3 vortexCenter = vortexCenterInFrontOf(caster);
        float yawDegrees = caster.getYRot();
        float pitchDegrees = caster.getXRot();
        spawnVortexLayer(
                level,
                vortexCenter,
                0,
                CometAzurFx.STARTUP_SHRINK_1_ROLL_RADIANS_PER_TICK,
                yawDegrees,
                pitchDegrees,
                false
        );
        spawnVortexLayer(
                level,
                vortexCenter,
                1,
                CometAzurFx.STARTUP_SHRINK_2_ROLL_RADIANS_PER_TICK,
                yawDegrees,
                pitchDegrees,
                true
        );
    }

    /**
     * 眼睛前方、略往下：第三人称能看见整团，第一人称不至于糊满准星。
     */
    public static Vec3 vortexCenterInFrontOf(LivingEntity caster) {
        Vec3 lookDirection = caster.getLookAngle();
        return caster.getEyePosition()
                .add(lookDirection.scale(CometAzurFx.STARTUP_VORTEX_FORWARD_OFFSET_BLOCKS))
                .subtract(0.0, CometAzurFx.STARTUP_VORTEX_DOWN_OFFSET_BLOCKS, 0.0);
    }

    /**
     * 喷流口：沿视线钉在玩家正前方，不跟漩涡下偏，出来就是一根粗柱。
     */
    public static Vec3 jetMouthInFrontOf(LivingEntity caster) {
        return caster.getEyePosition()
                .add(caster.getLookAngle().scale(CometAzurFx.JET_BEAM_MOUTH_FORWARD_OFFSET_BLOCKS));
    }

    private static void spawnVortexLayer(
            Level level,
            Vec3 vortexCenter,
            int spriteIndex,
            float rollRadiansPerTick,
            float yawDegrees,
            float pitchDegrees,
            boolean spawnSpirals
    ) {
        MagicManager.spawnParticles(
                level,
                new CometAzurVortexOptions(
                        spriteIndex,
                        rollRadiansPerTick,
                        yawDegrees,
                        pitchDegrees,
                        spawnSpirals
                ),
                vortexCenter.x,
                vortexCenter.y,
                vortexCenter.z,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                true
        );
    }

    /**
     * 蓄力结束后在漩涡原位置爆多层嵌套星辰涟漪。无伤害。
     * 波次：0 主环、1 回声、2 中心绽光、3 最外、4 中层、5 内层。
     */
    public static void spawnChargeShockwave(Level level, CometAzurCastData castData) {
        if (level.isClientSide || castData == null) {
            return;
        }
        Vec3 shockwaveCenter = castData.vortexCenter();
        float yawDegrees = castData.yawDegrees();
        float pitchDegrees = castData.pitchDegrees();
        spawnShockwaveRing(level, shockwaveCenter, yawDegrees, pitchDegrees, 0.0);
        spawnShockwaveRing(level, shockwaveCenter, yawDegrees, pitchDegrees, 1.0);
        spawnShockwaveRing(level, shockwaveCenter, yawDegrees, pitchDegrees, 2.0);
        spawnShockwaveRing(level, shockwaveCenter, yawDegrees, pitchDegrees, 3.0);
        spawnShockwaveRing(level, shockwaveCenter, yawDegrees, pitchDegrees, 4.0);
        spawnShockwaveRing(level, shockwaveCenter, yawDegrees, pitchDegrees, 5.0);
    }

    /**
     * {@code count = 0} 时 yaw / pitch / 波次原样进 xd / yd / zd。
     */
    private static void spawnShockwaveRing(
            Level level,
            Vec3 shockwaveCenter,
            float yawDegrees,
            float pitchDegrees,
            double waveIndex
    ) {
        MagicManager.spawnParticles(
                level,
                ModParticles.COMET_AZUR_SHOCKWAVE_RING.get(),
                shockwaveCenter.x,
                shockwaveCenter.y,
                shockwaveCenter.z,
                0,
                yawDegrees,
                pitchDegrees,
                waveIndex,
                1.0,
                true
        );
    }

    /**
     * 在锁定喷流口刷一圈星河套管（星云体积 + 螺旋星团 + 闪星），朝向用出手时钉死的 yaw/pitch。
     */
    public static void spawnJetSurround(Level level, CometAzurCastData castData) {
        if (level.isClientSide || castData == null) {
            return;
        }
        Vec3 jetMouth = castData.jetMouthWorld();
        MagicManager.spawnParticles(
                level,
                CometAzurJetOptions.emitter(castData.yawDegrees(), castData.pitchDegrees()),
                jetMouth.x,
                jetMouth.y,
                jetMouth.z,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                true
        );
    }
}

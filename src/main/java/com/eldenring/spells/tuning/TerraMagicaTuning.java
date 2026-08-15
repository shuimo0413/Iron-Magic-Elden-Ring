package com.eldenring.spells.tuning;

/**
 * 魔法之境（Terra Magica）全部可调数值集中在此。
 * <p>
 * 站在法阵内的施法者与队友获得全局 {@code SPELL_POWER +30%}（铁魔法原生属性），
 * 因此本模组辉石法术与铁魔法本体法术都会受益。多座法阵共用同一效果 ID，绝不叠层。
 */
public final class TerraMagicaTuning {

    private TerraMagicaTuning() {
    }

    // -------------------------------------------------------------------------
    // 法术数值（TerraMagicaSpell）
    // -------------------------------------------------------------------------

    /** 1 级基础法力消耗。 */
    public static final int SPELL_BASE_MANA_COST = 40;

    /** 每升一级额外法力消耗。 */
    public static final int SPELL_MANA_COST_PER_LEVEL = 5;

    /**
     * 1 级基础法术强度。本法定死 +30% 伤害，不参与伤害公式；
     * 仍保留字段以便铁魔法 UI / 等级曲线有合法 power 输入。
     */
    public static final int SPELL_BASE_SPELL_POWER = 1;

    /** 每升一级额外法术强度（本法定死加成，不参与伤害）。 */
    public static final int SPELL_SPELL_POWER_PER_LEVEL = 0;

    /**
     * 吟唱时间（tick）。大于 0 → {@code CastType.LONG}，给落阵一点仪式感。
     * 调大 → 更易被打断；调小 → 更接近瞬发。
     */
    public static final int SPELL_CAST_TIME_TICKS = 16;

    /** 冷却时间（秒）。 */
    public static final double SPELL_COOLDOWN_SECONDS = 25.0;

    /** 最大等级。法环辉石咒固定 1 级；持续时间只用 {@link #ZONE_BASE_DURATION_TICKS}。 */
    public static final int SPELL_MAX_LEVEL = 1;

    /**
     * 每升一级额外的法阵持续时间（tick）。
     * 总时长 = {@link #ZONE_BASE_DURATION_TICKS} + (level - 1) * 本值。
     */
    public static final int ZONE_DURATION_TICKS_PER_LEVEL = 40;

    // -------------------------------------------------------------------------
    // 法阵区域（TerraMagicaZoneEntity）
    // -------------------------------------------------------------------------

    /**
     * 法阵半径（方块）。边长视觉四边形约为 {@code 2 * radius}。
     * 调大 → 覆盖更宽、更易站进；调小 → 站桩要求更严。
     */
    public static final float ZONE_RADIUS_BLOCKS = 4.5f;

    /**
     * 1 级法阵持续时间（tick）。20 tick = 1 秒；600 = 30 秒（贴近法环原作）。
     */
    public static final int ZONE_BASE_DURATION_TICKS = 600;

    /**
     * 每隔多少 tick 扫描一次阵内友方并刷新效果。
     * 10 = 每 0.5 秒；调小更跟手但略增开销。
     */
    public static final int ZONE_REAPPLICATION_DELAY_TICKS = 10;

    /**
     * 施加到目标上的效果剩余时间（tick）。
     * 应略大于 {@link #ZONE_REAPPLICATION_DELAY_TICKS}，离开法阵后约 1 秒掉 buff。
     */
    public static final int EFFECT_REFRESH_DURATION_TICKS = 20;

    /**
     * 全局法术强度加成（乘算，{@code ADD_MULTIPLIED_TOTAL}）。
     * 0.30 → 默认 1.0 的 SPELL_POWER 变为 1.30（+30%）。全等级固定。
     */
    public static final double SPELL_POWER_BONUS_MULTIPLIED_TOTAL = 0.30D;

    /**
     * 贴地搜索：相对施法者脚底，最多向上 / 向下找地面的方块数。
     */
    public static final int ZONE_GROUND_SNAP_MAX_STEPS = 6;

    /**
     * 贴地后整体再抬高的距离（方块）。0.5 = 半格，避免徽记埋进地面。
     */
    public static final double ZONE_SPAWN_Y_OFFSET_BLOCKS = 0.3;

    // -------------------------------------------------------------------------
    // 视觉 / 粒子
    // -------------------------------------------------------------------------

    /**
     * 法阵贴图相对实体原点再抬高（方块），减轻 z-fighting。
     * 调大 → 徽记更「浮」；调小 → 更贴地但易闪烁。
     */
    public static final float SIGIL_Y_OFFSET_BLOCKS = 0.05f;

    /**
     * 徽记绕 Y 轴自转角速度（度 / tick）。
     * 0 = 静止（当前需求）；调大可恢复缓慢自转。
     */
    public static final float SIGIL_SPIN_DEGREES_PER_TICK = 0.0f;

    /**
     * 徽记整体不透明度（0–1）。贴图本身已有透明度，此值再乘一层。
     */
    public static final float SIGIL_OPACITY = 0.92f;

    /**
     * 客户端环境粒子密度基准（再乘半径做 clamp）。
     * 调大 → 整片法阵升起的辉石微粒更多。
     */
    public static final float ZONE_AMBIENT_PARTICLE_COUNT = 2.4f;

    /**
     * 粒子散布半径相对法阵半径的比例（1 = 铺满到边沿）。
     * 略小于 1 可避免粒子刚好卡在碰撞箱外缘。
     */
    public static final float ZONE_AMBIENT_FILL_RADIUS_FRACTION = 0.98f;

    /**
     * 法阵中心临时光源亮度（0–15）。
     * 使用原版 {@code Blocks.LIGHT}；仅在目标格为空气时放置，消散时若仍是我们的光则清除。
     */
    public static final int ZONE_CENTER_LIGHT_LEVEL = 12;
}

package com.eldenring.spells.entity.academy;

/**
 * 学院学者的玩法数值：默认值写在这里，运行时由 {@code iss_elden_ring-server.toml} 的
 * {@code mobs.academy_scholar} 段经 {@link com.eldenring.spells.config.EldenRingServerConfig#apply()} 覆盖。
 * <p>
 * 生命 / 移速 / 索敌范围在学者首次生成时写入属性基值，已存在的学者保留存档里的属性；
 * 施法间隔在实体构造（含读档）时交给 AI，改 toml 后新加载的学者生效。
 */
public final class AcademyScholarBalance {

    /** 最大生命值（半颗心为 1）。调大 → 更耐打。 */
    public static final double DEFAULT_MAX_HEALTH = 60.0D;

    /** 基础移动速度（原版属性单位，玩家约 0.1，僵尸 0.23）。调大 → 走位与追击更快。 */
    public static final double DEFAULT_MOVEMENT_SPEED = 0.25D;

    /** 索敌 / 追踪范围（方块）。调大 → 更远就发现玩家；调小 → 玩家能在教室外更安全地观察。 */
    public static final double DEFAULT_FOLLOW_RANGE_BLOCKS = 24.0D;

    /** 贴脸时两次施法的间隔（tick，20 tick = 1 秒）。调小 → 近身压制更凶。 */
    public static final int DEFAULT_ATTACK_INTERVAL_MIN_TICKS = 80;

    /** 施法距离边缘时两次施法的间隔（tick）。与最小值相同 → 远近都固定 4 秒一发；调小 → 远程火力更密。 */
    public static final int DEFAULT_ATTACK_INTERVAL_MAX_TICKS = 80;

    private static volatile double maxHealth = DEFAULT_MAX_HEALTH;
    private static volatile double movementSpeed = DEFAULT_MOVEMENT_SPEED;
    private static volatile double followRangeBlocks = DEFAULT_FOLLOW_RANGE_BLOCKS;
    private static volatile int attackIntervalMinTicks = DEFAULT_ATTACK_INTERVAL_MIN_TICKS;
    private static volatile int attackIntervalMaxTicks = DEFAULT_ATTACK_INTERVAL_MAX_TICKS;

    private AcademyScholarBalance() {
    }

    /**
     * 写入 toml 数值。最大间隔小于最小间隔时按最小间隔处理，避免铁魔法 AI 计算出负区间。
     */
    public static void configure(
            double configuredMaxHealth,
            double configuredMovementSpeed,
            double configuredFollowRangeBlocks,
            int configuredAttackIntervalMinTicks,
            int configuredAttackIntervalMaxTicks
    ) {
        maxHealth = configuredMaxHealth;
        movementSpeed = configuredMovementSpeed;
        followRangeBlocks = configuredFollowRangeBlocks;
        attackIntervalMinTicks = configuredAttackIntervalMinTicks;
        attackIntervalMaxTicks = Math.max(configuredAttackIntervalMinTicks, configuredAttackIntervalMaxTicks);
    }

    /** 世界配置卸载后恢复默认值，避免上一个世界的 toml 数值残留。 */
    public static void resetDefaults() {
        configure(
                DEFAULT_MAX_HEALTH,
                DEFAULT_MOVEMENT_SPEED,
                DEFAULT_FOLLOW_RANGE_BLOCKS,
                DEFAULT_ATTACK_INTERVAL_MIN_TICKS,
                DEFAULT_ATTACK_INTERVAL_MAX_TICKS
        );
    }

    public static double maxHealth() {
        return maxHealth;
    }

    public static double movementSpeed() {
        return movementSpeed;
    }

    public static double followRangeBlocks() {
        return followRangeBlocks;
    }

    public static int attackIntervalMinTicks() {
        return attackIntervalMinTicks;
    }

    public static int attackIntervalMaxTicks() {
        return attackIntervalMaxTicks;
    }
}

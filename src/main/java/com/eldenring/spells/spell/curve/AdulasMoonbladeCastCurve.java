package com.eldenring.spells.spell.curve;

/**
 * 亚杜拉的月光剑服务端时间轴，与卡利亚大剑相同：每刀 10 tick = 0.5 秒，
 * 刀与刀之间再空 10 tick = 0.5 秒。必须与 {@link com.eldenring.spells.client.AdulasMoonbladeClientHold} 片长一致。
 * <p>
 * 命中帧同时结算近身斩击并射出剑气。命中窗、收招写死；伤害数字在 Spell / toml。
 */
public final class AdulasMoonbladeCastCurve {

    /** 单刀片长（tick）。调大 → 每刀挥砍更慢；必须与客户端 Hold 的片长一致。 */
    public static final int SLASH_DURATION_TICKS = 10;

    /** 本刀播完到下一刀起手之间的空档（tick）。调大 → 连斩更疏。 */
    public static final int SLASH_RECOVERY_TICKS = 10;

    /** 一刀完整周期 = 挥砍 + 收招空档。服务端按这个取模，空档里不结算伤害、不放剑气。 */
    public static final int SLASH_CYCLE_TICKS = SLASH_DURATION_TICKS + SLASH_RECOVERY_TICKS;

    /**
     * 本刀结算斩击并射出剑气的 tick（从本周期第 0 tick 起算，必须落在挥砍段内）。
     * 调小 → 剑气出得更早；调大 → 更接近收招才飞出。
     */
    public static final int HIT_TICK = 4;

    /** 收到停止请求后，最多再撑几 tick 让本刀命中窗跑完。 */
    public static final int STOP_GRACE_TICKS = SLASH_DURATION_TICKS + 2;

    private AdulasMoonbladeCastCurve() {
    }

    /** 实体存活总 tick 落在本周期的哪一帧（0 … {@link #SLASH_CYCLE_TICKS}-1）。 */
    public static int tickIntoCurrentSlash(int entityAgeTicks) {
        int safeAge = Math.max(0, entityAgeTicks);
        return safeAge % SLASH_CYCLE_TICKS;
    }

    /** 当前这一刀是否刚到命中帧（每刀只为 true 一次）。 */
    public static boolean isHitTick(int entityAgeTicks) {
        int tickInCycle = tickIntoCurrentSlash(entityAgeTicks);
        return tickInCycle == HIT_TICK && tickInCycle < SLASH_DURATION_TICKS;
    }

    /** 第几刀（从 0 起）。用于判断松手后晚到的取消包是否已经跨进下一刀。 */
    public static int slashSequenceIndex(int entityAgeTicks) {
        return Math.max(0, entityAgeTicks) / SLASH_CYCLE_TICKS;
    }
}

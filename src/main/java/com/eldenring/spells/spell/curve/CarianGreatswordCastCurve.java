package com.eldenring.spells.spell.curve;

/**
 * 卡利亚大剑服务端时间轴。与客户端 PlayerAnimator 片长对齐：每刀 10 tick = 0.5 秒，
 * 刀与刀之间再空 10 tick = 0.5 秒，避免第一刀收招立刻接第二刀。
 * <p>
 * 命中窗、收招写死；伤害数字在 Spell / toml。
 */
public final class CarianGreatswordCastCurve {

    /**
     * 单刀片长（tick）。调大 → 每刀挥砍更慢；必须与客户端 Hold 的片长一致。
     */
    public static final int SLASH_DURATION_TICKS = 10;

    /**
     * 本刀播完到下一刀起手之间的空档（tick）。10 tick = 0.5 秒。
     * 调大 → 连斩更疏；点按只出一刀时不会走这段，松手即停。
     */
    public static final int SLASH_RECOVERY_TICKS = 10;

    /**
     * 一刀完整周期 = 挥砍 + 收招空档。服务端按这个取模，空档里不再结算伤害。
     */
    public static final int SLASH_CYCLE_TICKS = SLASH_DURATION_TICKS + SLASH_RECOVERY_TICKS;

    /**
     * 本刀结算伤害的 tick（从本周期第 0 tick 起算，必须落在挥砍段内）。
     * 约在挥砍中段；调小 → 出手更早结算，调大 → 更接近收招才打到人。
     */
    public static final int HIT_TICK = 4;

    /**
     * 收到停止请求后，最多再撑几 tick 让本刀命中窗跑完。
     * 略大于 {@link #SLASH_DURATION_TICKS}，避免 CancelCast 瞬间砍掉最后一刀伤害。
     */
    public static final int STOP_GRACE_TICKS = SLASH_DURATION_TICKS + 2;

    private CarianGreatswordCastCurve() {
    }

    /**
     * 实体存活总 tick 落在本周期的哪一帧（0 … {@link #SLASH_CYCLE_TICKS}-1）。
     * 0 … {@link #SLASH_DURATION_TICKS}-1 是挥砍，之后是收招空档。
     */
    public static int tickIntoCurrentSlash(int entityAgeTicks) {
        int safeAge = Math.max(0, entityAgeTicks);
        return safeAge % SLASH_CYCLE_TICKS;
    }

    /**
     * 当前这一刀是否刚到命中帧（每刀只为 true 一次；收招空档里恒为 false）。
     */
    public static boolean isHitTick(int entityAgeTicks) {
        int tickInCycle = tickIntoCurrentSlash(entityAgeTicks);
        return tickInCycle == HIT_TICK && tickInCycle < SLASH_DURATION_TICKS;
    }

    /**
     * 第几刀（从 0 起）。点按取消若落到下一刀前几 tick，用这个判断那一刀不是玩家要的。
     */
    public static int slashSequenceIndex(int entityAgeTicks) {
        return Math.max(0, entityAgeTicks) / SLASH_CYCLE_TICKS;
    }
}

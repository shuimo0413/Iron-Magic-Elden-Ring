package com.eldenring.spells.spell.curve;

/**
 * 卡利亚迅剑服务端时间轴。与客户端 PlayerAnimator 片长对齐：每刀 10 tick = 0.5 秒。
 * <p>
 * 命中窗、收招写死；伤害数字在 Spell / toml。
 */
public final class CarianSlicerCastCurve {

    /**
     * 单刀片长（tick）。调大 → 每刀更慢、连斩更疏；必须与客户端 Hold 的片长一致。
     */
    public static final int SLASH_DURATION_TICKS = 10;

    /**
     * 本刀结算伤害的 tick（从本刀第 0 tick 起算）。
     * 约在挥砍中段；调小 → 出手更早结算，调大 → 更接近收招才打到人。
     */
    public static final int HIT_TICK = 4;

    /**
     * 收到停止请求后，最多再撑几 tick 让本刀命中窗跑完。
     * 略大于 {@link #SLASH_DURATION_TICKS}，避免 CancelCast 瞬间砍掉最后一刀伤害。
     */
    public static final int STOP_GRACE_TICKS = SLASH_DURATION_TICKS + 2;

    private CarianSlicerCastCurve() {
    }

    /**
     * 实体存活总 tick 落在本刀的哪一帧（0 … {@link #SLASH_DURATION_TICKS}-1）。
     */
    public static int tickIntoCurrentSlash(int entityAgeTicks) {
        int safeAge = Math.max(0, entityAgeTicks);
        return safeAge % SLASH_DURATION_TICKS;
    }

    /**
     * 当前这一刀是否刚到命中帧（每刀只为 true 一次）。
     */
    public static boolean isHitTick(int entityAgeTicks) {
        return tickIntoCurrentSlash(entityAgeTicks) == HIT_TICK;
    }

    /**
     * 第几刀（从 0 起）。与客户端 {@code slashSequenceIndex} 对齐意图相同。
     */
    public static int slashSequenceIndex(int entityAgeTicks) {
        return Math.max(0, entityAgeTicks) / SLASH_DURATION_TICKS;
    }
}

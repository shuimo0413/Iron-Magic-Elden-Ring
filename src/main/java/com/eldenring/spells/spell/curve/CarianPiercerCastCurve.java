package com.eldenring.spells.spell.curve;

/**
 * 卡利亚贯刺服务端时间轴。点按只出一刺，与客户端 PlayerAnimator 片长对齐：
 * 15 tick = 0.75 秒（{@code carian_puncture}），没有连刺空档。
 * <p>
 * 命中窗落在剑身完全前伸附近（0.5 秒）；伤害数字在 Spell / toml。
 */
public final class CarianPiercerCastCurve {

    /**
     * 单刺片长（tick）。必须与 {@code carian_puncture} 的 0.75 秒一致。
     * 调大 → 这一刺更慢；没有下一刺。
     */
    public static final int SLASH_DURATION_TICKS = 15;

    /**
     * 本刺结算伤害的 tick（从实体出生第 0 tick 起算，必须落在片长内）。
     * 对应动画 0.5 秒：右臂已经捅出去（{@code -67.5°}），收招前结算。
     * 调小 → 出手更早结算，调大 → 更接近收招才打到人。
     */
    public static final int HIT_TICK = 10;

    /**
     * 收到停止请求后，最多再撑几 tick 让命中窗跑完。
     * 略大于 {@link #SLASH_DURATION_TICKS}，避免 CancelCast 瞬间砍掉伤害。
     */
    public static final int STOP_GRACE_TICKS = SLASH_DURATION_TICKS + 2;

    private CarianPiercerCastCurve() {
    }

    /**
     * 当前这一刺已过几 tick。点按只有一刺，不做周期取模。
     */
    public static int tickIntoCurrentSlash(int entityAgeTicks) {
        return Math.max(0, entityAgeTicks);
    }

    /**
     * 是否刚到命中帧（整段施法只为 true 一次）。
     */
    public static boolean isHitTick(int entityAgeTicks) {
        return tickIntoCurrentSlash(entityAgeTicks) == HIT_TICK;
    }
}

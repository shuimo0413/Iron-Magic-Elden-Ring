package com.eldenring.spells.spell.data;

import com.eldenring.spells.entity.MeteoriteVoidEntity;
import com.eldenring.spells.spell.AstelMeteoriteSpell;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 艾斯提陨石一次按住吟唱的附加状态。
 * <p>
 * 记录当前存活的虚空裂缝（每道各自计数、计时）、下一道裂缝的生成倒计时与首发状态。
 * 已经落下的陨石不绑在这里，松手不会把空中的陨石删掉。
 */
public final class AstelMeteoriteCastData implements ICastData {

    /**
     * 一道虚空裂缝：生成后固定在世界坐标，落完自己的陨石数就坍缩。
     */
    public static final class RiftSlot {

        /** 裂缝的视觉锚点实体（复用陨石黑洞）。 */
        private final MeteoriteVoidEntity voidEntity;

        /** 裂缝中心（世界坐标），生成后不再跟随施法者。 */
        private final Vec3 center;

        /** 裂缝朝向的水平偏航角（度）。陨石沿这个方向倾斜下坠，多道裂缝合起来铺成扇面。 */
        private final float facingYawDegrees;

        /** 这道裂缝还剩几颗陨石没落。 */
        private int meteoritesRemaining;

        /**
         * 距离下一次动作还要几 tick（单位：tick）。
         * 剩余陨石 &gt; 0 时动作为落一颗陨石；为 0 时动作为坍缩。
         */
        private int ticksUntilNextAction;

        public RiftSlot(
                MeteoriteVoidEntity voidEntity,
                Vec3 center,
                float facingYawDegrees,
                int meteoritesRemaining,
                int ticksUntilFirstMeteorite
        ) {
            this.voidEntity = voidEntity;
            this.center = center;
            this.facingYawDegrees = facingYawDegrees;
            this.meteoritesRemaining = meteoritesRemaining;
            this.ticksUntilNextAction = ticksUntilFirstMeteorite;
        }

        public MeteoriteVoidEntity voidEntity() {
            return voidEntity;
        }

        public Vec3 center() {
            return center;
        }

        public float facingYawDegrees() {
            return facingYawDegrees;
        }

        public int meteoritesRemaining() {
            return meteoritesRemaining;
        }

        /**
         * 倒计时走一 tick。
         *
         * @return true 本 tick 该执行下一次动作（落陨石或坍缩）
         */
        public boolean tickActionCountdown() {
            if (this.ticksUntilNextAction > 0) {
                this.ticksUntilNextAction--;
            }
            return this.ticksUntilNextAction <= 0;
        }

        /**
         * 记一颗陨石已落下，并排好下一次动作的等待时间。
         *
         * @param ticksUntilNextMeteorite 还有剩余陨石时距下一颗的间隔（tick）
         * @param ticksBeforeCollapse     落完最后一颗后到坍缩的停留时间（tick）
         */
        public void markMeteoriteSpawned(int ticksUntilNextMeteorite, int ticksBeforeCollapse) {
            this.meteoritesRemaining = Math.max(0, this.meteoritesRemaining - 1);
            this.ticksUntilNextAction = this.meteoritesRemaining > 0 ? ticksUntilNextMeteorite : ticksBeforeCollapse;
        }

        /** 收掉裂缝实体（带坍缩粒子）。重复调用安全。 */
        public void collapse() {
            if (!this.voidEntity.isRemoved()) {
                this.voidEntity.collapseAndDiscard();
            }
        }
    }

    /** 当前存活的裂缝。数量不超过 {@link AstelMeteoriteSpell#MAX_CONCURRENT_RIFTS}。 */
    private final List<RiftSlot> activeRifts = new ArrayList<>();

    /** 距离允许再开下一道裂缝还要几 tick（单位：tick）。0 表示有空位就可以开。 */
    private int ticksUntilNextRift;

    /** 本段吟唱是否已经落下第一颗；只给第一颗播飞弹射出音。 */
    private boolean firstMeteoriteSpawned;

    /** 本段吟唱是否已经撕开起手那道裂缝（它的张开时长等于起手蓄力时长）。 */
    private boolean firstRiftOpened;

    public AstelMeteoriteCastData() {
        this.ticksUntilNextRift = 0;
        this.firstMeteoriteSpawned = false;
        this.firstRiftOpened = false;
    }

    /**
     * @return true 表示这次调用应撕开起手裂缝；之后一直返回 false。
     */
    public boolean tryMarkFirstRiftOpened() {
        if (this.firstRiftOpened) {
            return false;
        }
        this.firstRiftOpened = true;
        return true;
    }

    public List<RiftSlot> activeRifts() {
        return activeRifts;
    }

    /**
     * 每个施法 tick 调一次：倒计时照走，到期且裂缝数未满时消费一次开裂缝窗口。
     *
     * @return true 本 tick 该开一道新裂缝
     */
    public boolean tryConsumeRiftSpawnWindow() {
        if (this.ticksUntilNextRift > 0) {
            this.ticksUntilNextRift--;
            return false;
        }
        if (this.activeRifts.size() >= Math.max(1, AstelMeteoriteSpell.MAX_CONCURRENT_RIFTS)) {
            return false;
        }
        this.ticksUntilNextRift = Math.max(0, AstelMeteoriteSpell.RIFT_SPAWN_INTERVAL_TICKS - 1);
        return true;
    }

    public void addRift(RiftSlot riftSlot) {
        this.activeRifts.add(riftSlot);
    }

    /**
     * @return true 表示这次调用是本段吟唱的第一颗陨石，应该播射出音。
     */
    public boolean tryMarkFirstMeteoriteSpawned() {
        if (this.firstMeteoriteSpawned) {
            return false;
        }
        this.firstMeteoriteSpawned = true;
        return true;
    }

    /** 收掉全部裂缝（带坍缩粒子）。重复调用安全。 */
    public void collapseAllRifts() {
        for (RiftSlot riftSlot : this.activeRifts) {
            riftSlot.collapse();
        }
        this.activeRifts.clear();
    }

    @Override
    public void reset() {
        this.ticksUntilNextRift = 0;
        this.firstMeteoriteSpawned = false;
        this.firstRiftOpened = false;
        collapseAllRifts();
    }
}

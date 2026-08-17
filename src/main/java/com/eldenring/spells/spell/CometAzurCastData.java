package com.eldenring.spells.spell;

import com.eldenring.spells.entity.CometAzurJetEntity;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 彗星亚兹勒一次吟唱的附加状态。
 * <p>
 * 出手瞬间钉死：脚底坐标、蓄力漩涡中心、喷流口、朝向。整段吟唱（含蓄力）不能移动 / 转视角，
 * 喷流与周围粒子都沿这套锁定朝向直线延伸，不再跟手拐弯。
 * 喷流口在玩家面前，与蓄力漩涡可以不在同一点。
 */
public final class CometAzurCastData implements ICastData {

    private final Vec3 lockedFeetPosition;
    private final Vec3 vortexCenter;
    private final Vec3 jetMouthWorld;
    private final float yawDegrees;
    private final float pitchDegrees;
    private boolean chargeShockwaveSpawned;
    @Nullable
    private CometAzurJetEntity jetEntity;

    public CometAzurCastData(
            Vec3 lockedFeetPosition,
            Vec3 vortexCenter,
            Vec3 jetMouthWorld,
            float yawDegrees,
            float pitchDegrees
    ) {
        this.lockedFeetPosition = lockedFeetPosition;
        this.vortexCenter = vortexCenter;
        this.jetMouthWorld = jetMouthWorld;
        this.yawDegrees = yawDegrees;
        this.pitchDegrees = pitchDegrees;
    }

    /** 出手时脚底世界坐标；每 tick 把施法者钉回这里。 */
    public Vec3 lockedFeetPosition() {
        return lockedFeetPosition;
    }

    /** 蓄力漩涡 / 涟漪中心（出手时算死）。 */
    public Vec3 vortexCenter() {
        return vortexCenter;
    }

    /** 星河喷流口（玩家面前，出手时算死）。 */
    public Vec3 jetMouthWorld() {
        return jetMouthWorld;
    }

    public float yawDegrees() {
        return yawDegrees;
    }

    public float pitchDegrees() {
        return pitchDegrees;
    }

    /** 锁定朝向的单位前向向量。 */
    public Vec3 lockedLookDirection() {
        return Vec3.directionFromRotation(this.pitchDegrees, this.yawDegrees);
    }

    @Nullable
    public CometAzurJetEntity jetEntity() {
        return jetEntity;
    }

    public void bindJetEntity(CometAzurJetEntity jetEntity) {
        this.jetEntity = jetEntity;
    }

    /**
     * @return true 表示这次调用应该刷冲击波；false 表示已经刷过。
     */
    public boolean tryMarkChargeShockwaveSpawned() {
        if (this.chargeShockwaveSpawned) {
            return false;
        }
        this.chargeShockwaveSpawned = true;
        return true;
    }

    @Override
    public void reset() {
        this.chargeShockwaveSpawned = false;
        if (this.jetEntity != null && !this.jetEntity.isRemoved()) {
            this.jetEntity.discard();
        }
        this.jetEntity = null;
    }
}

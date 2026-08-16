package com.eldenring.spells.spell;

import com.eldenring.spells.entity.CometAzurJetEntity;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 彗星亚兹勒一次吟唱的附加状态。
 * <p>
 * 记住蓄力漩涡钉死的位置 / 朝向、涟漪只爆一次，并持有星河喷流实体引用。
 * 吟唱结束时 {@link #reset()} 会丢弃喷流实体。
 */
public final class CometAzurCastData implements ICastData {

    private final Vec3 vortexCenter;
    private final float yawDegrees;
    private final float pitchDegrees;
    private boolean chargeShockwaveSpawned;
    @Nullable
    private CometAzurJetEntity jetEntity;

    public CometAzurCastData(Vec3 vortexCenter, float yawDegrees, float pitchDegrees) {
        this.vortexCenter = vortexCenter;
        this.yawDegrees = yawDegrees;
        this.pitchDegrees = pitchDegrees;
    }

    public Vec3 vortexCenter() {
        return vortexCenter;
    }

    public float yawDegrees() {
        return yawDegrees;
    }

    public float pitchDegrees() {
        return pitchDegrees;
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

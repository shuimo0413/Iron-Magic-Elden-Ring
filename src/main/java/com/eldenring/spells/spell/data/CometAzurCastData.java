package com.eldenring.spells.spell.data;

import com.eldenring.spells.entity.CometAzurJetEntity;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 彗星亚兹勒一次吟唱的附加状态。
 * <p>
 * 蓄力漩涡中心、喷流口与朝向会在每个施法 tick 按施法者当前位置和视线更新，
 * 喷流、周围粒子及伤害判定共用这套实时状态。
 * 喷流口在玩家面前，与蓄力漩涡可以不在同一点。
 */
public final class CometAzurCastData implements ICastData {

    private Vec3 vortexCenter;
    private Vec3 jetMouthWorld;
    private float yawDegrees;
    private float pitchDegrees;
    private boolean chargeShockwaveSpawned;
    @Nullable
    private CometAzurJetEntity jetEntity;

    public CometAzurCastData(
            Vec3 vortexCenter,
            Vec3 jetMouthWorld,
            float yawDegrees,
            float pitchDegrees
    ) {
        this.vortexCenter = vortexCenter;
        this.jetMouthWorld = jetMouthWorld;
        this.yawDegrees = yawDegrees;
        this.pitchDegrees = pitchDegrees;
    }

    /** 更新当前施法位置与朝向，供喷流实体、粒子和伤害判定共同读取。 */
    public void updateAim(Vec3 vortexCenter, Vec3 jetMouthWorld, float yawDegrees, float pitchDegrees) {
        this.vortexCenter = vortexCenter;
        this.jetMouthWorld = jetMouthWorld;
        this.yawDegrees = yawDegrees;
        this.pitchDegrees = pitchDegrees;
    }

    /** 当前蓄力漩涡 / 涟漪中心。 */
    public Vec3 vortexCenter() {
        return vortexCenter;
    }

    /** 当前星河喷流口（玩家面前）。 */
    public Vec3 jetMouthWorld() {
        return jetMouthWorld;
    }

    public float yawDegrees() {
        return yawDegrees;
    }

    public float pitchDegrees() {
        return pitchDegrees;
    }

    /** 当前朝向的单位前向向量。 */
    public Vec3 lookDirection() {
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

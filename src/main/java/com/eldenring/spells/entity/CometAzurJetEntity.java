package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
import com.eldenring.spells.spell.CometAzurCastData;
import com.eldenring.spells.tuning.CometAzurTuning;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 彗星亚兹勒星河喷流实体。
 * <p>
 * 朝向 / 喷流口在生成时钉死（施法者整段吟唱不能转身），之后只刷新射线长度与伤害。
 * 喷流口在玩家面前，出来就是接近最粗的柱体；视觉由客户端直线 ribbon 星河柱绘制。
 */
public class CometAzurJetEntity extends Projectile implements AntiMagicSusceptible {

    private static final EntityDataAccessor<Float> DATA_BEAM_LENGTH_BLOCKS =
            SynchedEntityData.defineId(CometAzurJetEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_YAW_DEGREES =
            SynchedEntityData.defineId(CometAzurJetEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH_DEGREES =
            SynchedEntityData.defineId(CometAzurJetEntity.class, EntityDataSerializers.FLOAT);

    private float damagePerHit;
    private int spellLevel = 1;
    private Vec3 lockedMouthWorld = Vec3.ZERO;

    /**
     * 服务端：若连续若干 tick 没被法术 refresh，视为吟唱已停，自行销毁。
     */
    private int ticksSinceLastRefresh;

    public CometAzurJetEntity(EntityType<? extends CometAzurJetEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvisible(true);
    }

    public CometAzurJetEntity(
            Level level,
            LivingEntity caster,
            CometAzurCastData castData,
            float damagePerHit,
            int spellLevel
    ) {
        this(ModEntities.COMET_AZUR_JET.get(), level);
        setOwner(caster);
        this.damagePerHit = damagePerHit;
        this.spellLevel = Math.max(1, spellLevel);
        lockAimFromCastData(castData);
        refreshBeamLength();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BEAM_LENGTH_BLOCKS, (float) CometAzurTuning.JET_BEAM_MAX_RANGE_BLOCKS);
        builder.define(DATA_YAW_DEGREES, 0.0f);
        builder.define(DATA_PITCH_DEGREES, 0.0f);
    }

    /**
     * 法术每 tick 调用：保活 + 更新伤害；朝向不再跟手。
     */
    public void refreshWhileCasting(float damagePerHit, int spellLevel) {
        this.ticksSinceLastRefresh = 0;
        this.damagePerHit = damagePerHit;
        this.spellLevel = Math.max(1, spellLevel);
        // 钉回锁定喷流口，防止任何位移/同步抖动。
        setPos(this.lockedMouthWorld.x, this.lockedMouthWorld.y, this.lockedMouthWorld.z);
        refreshBeamLength();
    }

    private void lockAimFromCastData(CometAzurCastData castData) {
        this.lockedMouthWorld = castData.jetMouthWorld();
        setPos(this.lockedMouthWorld.x, this.lockedMouthWorld.y, this.lockedMouthWorld.z);
        setYRot(castData.yawDegrees());
        setXRot(castData.pitchDegrees());
        this.yRotO = getYRot();
        this.xRotO = getXRot();
        this.entityData.set(DATA_YAW_DEGREES, castData.yawDegrees());
        this.entityData.set(DATA_PITCH_DEGREES, castData.pitchDegrees());
    }

    public float beamLengthBlocks() {
        return this.entityData.get(DATA_BEAM_LENGTH_BLOCKS);
    }

    public float syncedYawDegrees() {
        return this.entityData.get(DATA_YAW_DEGREES);
    }

    public float syncedPitchDegrees() {
        return this.entityData.get(DATA_PITCH_DEGREES);
    }

    private void setBeamLengthBlocks(float lengthBlocks) {
        this.entityData.set(
                DATA_BEAM_LENGTH_BLOCKS,
                Mth.clamp(lengthBlocks, 0.5f, (float) CometAzurTuning.JET_BEAM_MAX_RANGE_BLOCKS)
        );
    }

    /**
     * 沿锁定朝向射线检测实心方块，截断长度。朝向不变，只需重测遮挡。
     */
    private void refreshBeamLength() {
        Vec3 mouth = this.lockedMouthWorld;
        Vec3 lookDirection = Vec3.directionFromRotation(syncedPitchDegrees(), syncedYawDegrees());
        Vec3 farPoint = mouth.add(lookDirection.scale(CometAzurTuning.JET_BEAM_MAX_RANGE_BLOCKS));
        BlockHitResult blockHit = level().clip(new ClipContext(
                mouth,
                farPoint,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));
        double lengthBlocks = blockHit.getType() == HitResult.Type.MISS
                ? CometAzurTuning.JET_BEAM_MAX_RANGE_BLOCKS
                : mouth.distanceTo(blockHit.getLocation());
        setBeamLengthBlocks((float) lengthBlocks);
    }

    @Override
    public void tick() {
        baseTick();

        Entity owner = getOwner();
        if (!(owner instanceof LivingEntity caster) || !caster.isAlive() || caster.isRemoved()) {
            discard();
            return;
        }

        if (level().isClientSide) {
            return;
        }

        this.ticksSinceLastRefresh++;
        if (this.ticksSinceLastRefresh > 1) {
            discard();
            return;
        }

        setPos(this.lockedMouthWorld.x, this.lockedMouthWorld.y, this.lockedMouthWorld.z);
        refreshBeamLength();

        if (tickCount % CometAzurTuning.JET_BEAM_DAMAGE_INTERVAL_TICKS == 0) {
            dealBeamDamage(caster);
        }
    }

    private void dealBeamDamage(LivingEntity caster) {
        Vec3 mouth = this.lockedMouthWorld;
        Vec3 tip = mouth.add(Vec3.directionFromRotation(syncedPitchDegrees(), syncedYawDegrees())
                .scale(beamLengthBlocks()));
        double inflate = CometAzurTuning.JET_BEAM_DAMAGE_RADIUS_BLOCKS + 0.35;
        AABB searchBox = new AABB(mouth, tip).inflate(inflate);
        var damageSource = ModSpells.COMET_AZUR.get().getDamageSource(this, caster);
        float radiusBlocks = CometAzurTuning.JET_BEAM_DAMAGE_RADIUS_BLOCKS;
        double radiusSquared = radiusBlocks * radiusBlocks;

        for (LivingEntity target : level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                candidate -> candidate.isAlive()
                        && candidate != caster
                        && candidate.isPickable()
                        && !candidate.isSpectator()
        )) {
            if (distancePointToSegmentSquared(target.getBoundingBox().getCenter(), mouth, tip) > radiusSquared) {
                continue;
            }
            DamageSources.applyDamage(target, this.damagePerHit, damageSource);
        }
    }

    private static double distancePointToSegmentSquared(Vec3 point, Vec3 segmentStart, Vec3 segmentEnd) {
        Vec3 segment = segmentEnd.subtract(segmentStart);
        double segmentLengthSquared = segment.lengthSqr();
        if (segmentLengthSquared < 1.0e-8) {
            return point.distanceToSqr(segmentStart);
        }
        double projection = Mth.clamp(
                point.subtract(segmentStart).dot(segment) / segmentLengthSquared,
                0.0,
                1.0
        );
        Vec3 closestPoint = segmentStart.add(segment.scale(projection));
        return point.distanceToSqr(closestPoint);
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.damagePerHit = tag.getFloat("DamagePerHit");
        this.spellLevel = Math.max(1, tag.getInt("SpellLevel"));
        this.lockedMouthWorld = new Vec3(
                tag.getDouble("MouthX"),
                tag.getDouble("MouthY"),
                tag.getDouble("MouthZ")
        );
        if (tag.contains("Yaw")) {
            this.entityData.set(DATA_YAW_DEGREES, tag.getFloat("Yaw"));
            this.entityData.set(DATA_PITCH_DEGREES, tag.getFloat("Pitch"));
            setYRot(tag.getFloat("Yaw"));
            setXRot(tag.getFloat("Pitch"));
        }
        if (tag.contains("BeamLength")) {
            setBeamLengthBlocks(tag.getFloat("BeamLength"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("DamagePerHit", this.damagePerHit);
        tag.putInt("SpellLevel", this.spellLevel);
        tag.putDouble("MouthX", this.lockedMouthWorld.x);
        tag.putDouble("MouthY", this.lockedMouthWorld.y);
        tag.putDouble("MouthZ", this.lockedMouthWorld.z);
        tag.putFloat("Yaw", syncedYawDegrees());
        tag.putFloat("Pitch", syncedPitchDegrees());
        tag.putFloat("BeamLength", beamLengthBlocks());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = beamLengthBlocks() + 16.0;
        return distance < range * range;
    }

    public int spellLevel() {
        return this.spellLevel;
    }
}

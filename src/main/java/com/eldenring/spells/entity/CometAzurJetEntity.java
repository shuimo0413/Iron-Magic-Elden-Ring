package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.registry.ModSpells;
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
 * 自身几乎不可见：每 tick 钉在施法者面前喷流口，同步当前射线长度。
 * 视觉由客户端 ribbon 多层星河柱绘制；周围粒子仍由法术 tick 刷。
 * 伤害沿射线圆柱周期结算，不做密粒子本体。
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

    public CometAzurJetEntity(Level level, LivingEntity caster, float damagePerHit, int spellLevel) {
        this(ModEntities.COMET_AZUR_JET.get(), level);
        setOwner(caster);
        this.damagePerHit = damagePerHit;
        this.spellLevel = Math.max(1, spellLevel);
        snapToCasterMouth(caster);
        refreshBeamLength(caster);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BEAM_LENGTH_BLOCKS, (float) CometAzurTuning.JET_BEAM_MAX_RANGE_BLOCKS);
        builder.define(DATA_YAW_DEGREES, 0.0f);
        builder.define(DATA_PITCH_DEGREES, 0.0f);
    }

    /**
     * 法术每 tick 调用：证明仍在按住吟唱，并刷新位置 / 朝向 / 长度。
     */
    public void refreshFromCaster(LivingEntity caster, float damagePerHit, int spellLevel) {
        this.ticksSinceLastRefresh = 0;
        this.damagePerHit = damagePerHit;
        this.spellLevel = Math.max(1, spellLevel);
        setOwner(caster);
        snapToCasterMouth(caster);
        refreshBeamLength(caster);
    }

    public float beamLengthBlocks() {
        return this.entityData.get(DATA_BEAM_LENGTH_BLOCKS);
    }

    /** 客户端渲染用的水平朝向（度），走 EntityData，避免 Projectile 旋转包跟手不准。 */
    public float syncedYawDegrees() {
        return this.entityData.get(DATA_YAW_DEGREES);
    }

    public float syncedPitchDegrees() {
        return this.entityData.get(DATA_PITCH_DEGREES);
    }

    private void setBeamLengthBlocks(float lengthBlocks) {
        this.entityData.set(DATA_BEAM_LENGTH_BLOCKS, Mth.clamp(lengthBlocks, 0.5f, (float) CometAzurTuning.JET_BEAM_MAX_RANGE_BLOCKS));
    }

    /**
     * 喷流口：眼睛前方略下，与蓄力漩涡 / 周围粒子同一套偏移。
     */
    private void snapToCasterMouth(LivingEntity caster) {
        Vec3 lookDirection = caster.getLookAngle();
        Vec3 mouth = caster.getEyePosition()
                .add(lookDirection.scale(CometAzurTuning.STARTUP_VORTEX_FORWARD_OFFSET_BLOCKS))
                .subtract(0.0, CometAzurTuning.STARTUP_VORTEX_DOWN_OFFSET_BLOCKS, 0.0);
        setPos(mouth.x, mouth.y, mouth.z);
        setYRot(caster.getYRot());
        setXRot(caster.getXRot());
        this.yRotO = getYRot();
        this.xRotO = getXRot();
        this.entityData.set(DATA_YAW_DEGREES, caster.getYRot());
        this.entityData.set(DATA_PITCH_DEGREES, caster.getXRot());
    }

    private void refreshBeamLength(LivingEntity caster) {
        Vec3 mouth = position();
        Vec3 lookDirection = caster.getLookAngle();
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
            // 客户端用同步的 yaw/pitch/长度画；位置仍跟实体插值。
            return;
        }

        this.ticksSinceLastRefresh++;
        if (this.ticksSinceLastRefresh > 8) {
            discard();
            return;
        }

        snapToCasterMouth(caster);
        refreshBeamLength(caster);

        if (tickCount % CometAzurTuning.JET_BEAM_DAMAGE_INTERVAL_TICKS == 0) {
            dealBeamDamage(caster);
        }
    }

    /**
     * 沿嘴到尖端的圆柱扫敌。距离用点到线段距离，避免粗 AABB 误伤侧面太远的目标。
     */
    private void dealBeamDamage(LivingEntity caster) {
        Vec3 mouth = position();
        Vec3 tip = mouth.add(Vec3.directionFromRotation(getXRot(), getYRot()).scale(beamLengthBlocks()));
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
        if (tag.contains("BeamLength")) {
            setBeamLengthBlocks(tag.getFloat("BeamLength"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("DamagePerHit", this.damagePerHit);
        tag.putInt("SpellLevel", this.spellLevel);
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

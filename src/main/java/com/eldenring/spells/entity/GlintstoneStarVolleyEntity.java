package com.eldenring.spells.entity;

import com.eldenring.spells.particle.glintstone.GlintstoneFx;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.spell.GlintstoneCastHelper;
import com.eldenring.spells.tuning.GlintstoneStarsTuning;
import com.eldenring.spells.tuning.StarShowerTuning;
import com.eldenring.spells.tuning.StarsOfRuinTuning;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

    /**
     * 辉石连发齐射控制器：自身不可见、不碰撞，按 tick 在视线前方圆阵顶点顺时针依次生成流星。
     * <p>
     * 必须用实体 tick 错峰，不能用 {@code MinecraftServer#tell(TickTask)}。
     * {@code TickTask} 在服务端 {@code haveTime()} 为真时会立刻执行，导致「延迟」的几发
     * 和第一发挤在同一 tick，视觉上齐射、命中叠在一起形成骗伤。
     */
public class GlintstoneStarVolleyEntity extends Projectile {

    /**
     * 连发种类：决定数量、间隔、散布、弹道工厂与是否铺星河。
     */
    public enum VolleyKind {
        GLINTSTONE_STARS,
        STAR_SHOWER,
        STARS_OF_RUIN
    }

    private VolleyKind volleyKind = VolleyKind.GLINTSTONE_STARS;
    private int projectileCount = GlintstoneStarsTuning.PROJECTILE_COUNT;
    private int staggerTicks = GlintstoneStarsTuning.PROJECTILE_SPAWN_STAGGER_TICKS;
    private int spawnedProjectileCount;
    private float damagePerProjectile;

    public GlintstoneStarVolleyEntity(
            EntityType<? extends GlintstoneStarVolleyEntity> entityType,
            Level level
    ) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvisible(true);
    }

    public GlintstoneStarVolleyEntity(
            Level level,
            LivingEntity caster,
            VolleyKind volleyKind,
            float damagePerProjectile
    ) {
        this(ModEntities.GLINTSTONE_STAR_VOLLEY.get(), level);
        setOwner(caster);
        this.volleyKind = volleyKind;
        this.damagePerProjectile = damagePerProjectile;
        applyKindTuning(volleyKind);
        setPos(caster.getEyePosition());
    }

    private void applyKindTuning(VolleyKind kind) {
        switch (kind) {
            case STAR_SHOWER -> {
                this.projectileCount = StarShowerTuning.PROJECTILE_COUNT;
                this.staggerTicks = Math.max(1, StarShowerTuning.PROJECTILE_SPAWN_STAGGER_TICKS);
            }
            case STARS_OF_RUIN -> {
                this.projectileCount = StarsOfRuinTuning.PROJECTILE_COUNT;
                this.staggerTicks = Math.max(1, StarsOfRuinTuning.PROJECTILE_SPAWN_STAGGER_TICKS);
            }
            default -> {
                this.projectileCount = GlintstoneStarsTuning.PROJECTILE_COUNT;
                this.staggerTicks = Math.max(1, GlintstoneStarsTuning.PROJECTILE_SPAWN_STAGGER_TICKS);
            }
        }
    }

    @Override
    public void tick() {
        baseTick();

        Entity owner = getOwner();
        if (!(owner instanceof LivingEntity caster) || !caster.isAlive() || caster.isRemoved()) {
            discard();
            return;
        }

        setPos(caster.getEyePosition());

        if (level().isClientSide) {
            return;
        }

        boolean shouldSpawnThisTick = spawnedProjectileCount < projectileCount
                && (tickCount - 1) % staggerTicks == 0;
        if (shouldSpawnThisTick) {
            spawnNextProjectile(caster);
            spawnedProjectileCount++;
        }

        if (spawnedProjectileCount >= projectileCount) {
            discard();
        }
    }

    /**
     * 按当前发序在视线前方圆阵顶点生成一发，再沿「视线 + 上扬」立刻射出。
     * 顶点按顺时针等分，3 发呈三角形、6 发六边形、8 发八边形。
     */
    private void spawnNextProjectile(LivingEntity caster) {
        int projectileIndex = spawnedProjectileCount;
        Vec3 lookDirection = caster.getLookAngle();
        Vec3 lookPlaneOffset = GlintstoneCastHelper.clockwiseRegularPolygonOffset(
                lookDirection,
                projectileIndex,
                projectileCount,
                spawnCircleRadiusBlocks(),
                spawnCircleStartAngleDegrees()
        );
        Vec3 shootDirection = lookDirection
                .add(0.0, initialUpwardLift(), 0.0)
                .normalize();

        GlintstoneCastHelper.spawnAlongLook(
                level(),
                caster,
                this::createProjectile,
                spawnForwardOffsetBlocks(),
                castBurstForwardOffsetBlocks(),
                castBurstParticleIntensity(),
                damagePerProjectile,
                shootDirection,
                lookPlaneOffset,
                true
        );

        level().playSound(
                null,
                caster.getX(),
                caster.getY(),
                caster.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                0.55f,
                1.15f + projectileIndex * 0.04f
        );

        if (volleyKind == VolleyKind.STARS_OF_RUIN) {
            Vec3 riverOrigin = caster.getEyePosition().add(lookDirection.scale(0.35));
            GlintstoneFx.starRiver(
                    level(),
                    riverOrigin,
                    lookDirection,
                    StarsOfRuinTuning.CAST_BURST_PARTICLE_INTENSITY * 0.55f,
                    StarsOfRuinTuning.STAR_RIVER_LENGTH_BLOCKS * 0.55,
                    StarsOfRuinTuning.STAR_RIVER_RADIUS_BLOCKS * 0.75,
                    true
            );
        }
    }

    private AbstractGlintstoneProjectile createProjectile(Level level, LivingEntity caster) {
        return switch (volleyKind) {
            case STAR_SHOWER -> new StarShowerProjectile(level, caster);
            case STARS_OF_RUIN -> new StarsOfRuinProjectile(level, caster);
            case GLINTSTONE_STARS -> new GlintstoneStarProjectile(level, caster);
        };
    }

    private double spawnCircleRadiusBlocks() {
        return switch (volleyKind) {
            case STAR_SHOWER -> StarShowerTuning.SPAWN_CIRCLE_RADIUS_BLOCKS;
            case STARS_OF_RUIN -> StarsOfRuinTuning.SPAWN_CIRCLE_RADIUS_BLOCKS;
            case GLINTSTONE_STARS -> GlintstoneStarsTuning.SPAWN_CIRCLE_RADIUS_BLOCKS;
        };
    }

    private int spawnCircleStartAngleDegrees() {
        return switch (volleyKind) {
            case STAR_SHOWER -> StarShowerTuning.SPAWN_CIRCLE_START_ANGLE_DEGREES;
            case STARS_OF_RUIN -> StarsOfRuinTuning.SPAWN_CIRCLE_START_ANGLE_DEGREES;
            case GLINTSTONE_STARS -> GlintstoneStarsTuning.SPAWN_CIRCLE_START_ANGLE_DEGREES;
        };
    }

    private double initialUpwardLift() {
        return switch (volleyKind) {
            case STAR_SHOWER -> StarShowerTuning.PROJECTILE_INITIAL_UPWARD_LIFT;
            case STARS_OF_RUIN -> StarsOfRuinTuning.PROJECTILE_INITIAL_UPWARD_LIFT;
            case GLINTSTONE_STARS -> GlintstoneStarsTuning.PROJECTILE_INITIAL_UPWARD_LIFT;
        };
    }

    private double spawnForwardOffsetBlocks() {
        return switch (volleyKind) {
            case STAR_SHOWER -> StarShowerTuning.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS;
            case STARS_OF_RUIN -> StarsOfRuinTuning.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS;
            case GLINTSTONE_STARS -> GlintstoneStarsTuning.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS;
        };
    }

    private double castBurstForwardOffsetBlocks() {
        return switch (volleyKind) {
            case STAR_SHOWER -> StarShowerTuning.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS;
            case STARS_OF_RUIN -> StarsOfRuinTuning.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS;
            case GLINTSTONE_STARS -> GlintstoneStarsTuning.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS;
        };
    }

    private float castBurstParticleIntensity() {
        return switch (volleyKind) {
            case STAR_SHOWER -> StarShowerTuning.CAST_BURST_PARTICLE_INTENSITY;
            case STARS_OF_RUIN -> StarsOfRuinTuning.CAST_BURST_PARTICLE_INTENSITY;
            case GLINTSTONE_STARS -> GlintstoneStarsTuning.CAST_BURST_PARTICLE_INTENSITY;
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("VolleyKind", volleyKind.name());
        tag.putInt("ProjectileCount", projectileCount);
        tag.putInt("StaggerTicks", staggerTicks);
        tag.putInt("SpawnedProjectileCount", spawnedProjectileCount);
        tag.putFloat("DamagePerProjectile", damagePerProjectile);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        try {
            this.volleyKind = VolleyKind.valueOf(tag.getString("VolleyKind"));
        } catch (IllegalArgumentException ignored) {
            this.volleyKind = VolleyKind.GLINTSTONE_STARS;
        }
        applyKindTuning(this.volleyKind);
        if (tag.contains("ProjectileCount")) {
            this.projectileCount = tag.getInt("ProjectileCount");
        }
        if (tag.contains("StaggerTicks")) {
            this.staggerTicks = Math.max(1, tag.getInt("StaggerTicks"));
        }
        this.spawnedProjectileCount = tag.getInt("SpawnedProjectileCount");
        this.damagePerProjectile = tag.getFloat("DamagePerProjectile");
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}

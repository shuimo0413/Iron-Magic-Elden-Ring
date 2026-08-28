package com.eldenring.spells.entity;

import com.eldenring.spells.particle.glintstone.GlintstoneFx;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.spell.helper.GlintstoneCastHelper;
import com.eldenring.spells.spell.GlintstoneStarsSpell;
import com.eldenring.spells.spell.StarShowerSpell;
import com.eldenring.spells.spell.StarsOfRuinSpell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
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
    private int projectileCount = GlintstoneStarsSpell.PROJECTILE_COUNT;
    private int staggerTicks = GlintstoneStarsSpell.PROJECTILE_SPAWN_STAGGER_TICKS;
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
        applyVolleyKind(volleyKind);
        setPos(caster.getEyePosition());
    }

    private void applyVolleyKind(VolleyKind kind) {
        switch (kind) {
            case STAR_SHOWER -> {
                this.projectileCount = StarShowerSpell.PROJECTILE_COUNT;
                this.staggerTicks = Math.max(1, StarShowerSpell.PROJECTILE_SPAWN_STAGGER_TICKS);
            }
            case STARS_OF_RUIN -> {
                this.projectileCount = StarsOfRuinSpell.PROJECTILE_COUNT;
                this.staggerTicks = Math.max(1, StarsOfRuinSpell.PROJECTILE_SPAWN_STAGGER_TICKS);
            }
            default -> {
                this.projectileCount = GlintstoneStarsSpell.PROJECTILE_COUNT;
                this.staggerTicks = Math.max(1, GlintstoneStarsSpell.PROJECTILE_SPAWN_STAGGER_TICKS);
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
     * 按当前发序在视线前方圆阵顶点生成一发，再沿视线立刻射出。
     * 顶点按顺时针等分，3 发呈三角形、6 发六边形、12 发十二边形。
     * 上扬由各 Spell 的 {@code PROJECTILE_INITIAL_UPWARD_LIFT} 控制，当前均为 0（平射）。
     * 毁灭流星出手闪光改走星河粒子，不再叠青蓝 castBurst。
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
        double upwardLift = initialUpwardLift();
        Vec3 shootDirection = upwardLift == 0.0
                ? lookDirection
                : lookDirection.add(0.0, upwardLift, 0.0).normalize();
        boolean playCyanCastBurst = volleyKind != VolleyKind.STARS_OF_RUIN;

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
                playCyanCastBurst
        );

        if (volleyKind == VolleyKind.STARS_OF_RUIN) {
            GlintstoneFx.starRiverLaunch(
                    level(),
                    caster,
                    StarsOfRuinSpell.CAST_BURST_PARTICLE_INTENSITY * 0.55f
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
            case STAR_SHOWER -> StarShowerSpell.SPAWN_CIRCLE_RADIUS_BLOCKS;
            case STARS_OF_RUIN -> StarsOfRuinSpell.SPAWN_CIRCLE_RADIUS_BLOCKS;
            case GLINTSTONE_STARS -> GlintstoneStarsSpell.SPAWN_CIRCLE_RADIUS_BLOCKS;
        };
    }

    private int spawnCircleStartAngleDegrees() {
        return switch (volleyKind) {
            case STAR_SHOWER -> StarShowerSpell.SPAWN_CIRCLE_START_ANGLE_DEGREES;
            case STARS_OF_RUIN -> StarsOfRuinSpell.SPAWN_CIRCLE_START_ANGLE_DEGREES;
            case GLINTSTONE_STARS -> GlintstoneStarsSpell.SPAWN_CIRCLE_START_ANGLE_DEGREES;
        };
    }

    private double initialUpwardLift() {
        return switch (volleyKind) {
            case STAR_SHOWER -> StarShowerSpell.PROJECTILE_INITIAL_UPWARD_LIFT;
            case STARS_OF_RUIN -> StarsOfRuinSpell.PROJECTILE_INITIAL_UPWARD_LIFT;
            case GLINTSTONE_STARS -> GlintstoneStarsSpell.PROJECTILE_INITIAL_UPWARD_LIFT;
        };
    }

    private double spawnForwardOffsetBlocks() {
        return switch (volleyKind) {
            case STAR_SHOWER -> StarShowerSpell.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS;
            case STARS_OF_RUIN -> StarsOfRuinSpell.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS;
            case GLINTSTONE_STARS -> GlintstoneStarsSpell.PROJECTILE_SPAWN_FORWARD_OFFSET_BLOCKS;
        };
    }

    private double castBurstForwardOffsetBlocks() {
        return switch (volleyKind) {
            case STAR_SHOWER -> StarShowerSpell.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS;
            case STARS_OF_RUIN -> StarsOfRuinSpell.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS;
            case GLINTSTONE_STARS -> GlintstoneStarsSpell.SPELL_CAST_BURST_FORWARD_OFFSET_BLOCKS;
        };
    }

    private float castBurstParticleIntensity() {
        return switch (volleyKind) {
            case STAR_SHOWER -> StarShowerSpell.CAST_BURST_PARTICLE_INTENSITY;
            case STARS_OF_RUIN -> StarsOfRuinSpell.CAST_BURST_PARTICLE_INTENSITY;
            case GLINTSTONE_STARS -> GlintstoneStarsSpell.CAST_BURST_PARTICLE_INTENSITY;
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
        applyVolleyKind(this.volleyKind);
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

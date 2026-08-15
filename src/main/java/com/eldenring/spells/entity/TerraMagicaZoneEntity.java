package com.eldenring.spells.entity;

import com.eldenring.spells.particle.glintstone.GlintstoneFx;
import com.eldenring.spells.registry.ModEffects;
import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.tuning.TerraMagicaTuning;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.entity.spells.AoeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * 魔法之境地面法阵：静止圆形 AoE，周期性给阵内友方刷新唯一的 {@link ModEffects#TERRA_MAGICA}。
 * <p>
 * 默认 {@link AoeEntity#canHitEntity} 会<strong>排除</strong>主人与队友（攻击型法阵逻辑），
 * 本类反过来：只命中可接受友方 buff 的目标。效果 ID 全局唯一，多座重叠也不叠 30%。
 * <p>
 * 中心在空气格放置原版 {@link Blocks#LIGHT} 作为临时光源，消散 / 反制时清除。
 */
public class TerraMagicaZoneEntity extends AoeEntity implements AntiMagicSusceptible {

    /** 是否已成功放置中心光源（用于消散时只清自己放的光）。 */
    private boolean placedCenterLight;

    /** 中心光源方块坐标；未放置时为 null。 */
    private BlockPos centerLightBlockPos;

    public TerraMagicaZoneEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        setCircular();
        setReapplicationDelay(TerraMagicaTuning.ZONE_REAPPLICATION_DELAY_TICKS);
    }

    public TerraMagicaZoneEntity(Level level) {
        this(ModEntities.TERRA_MAGICA_ZONE.get(), level);
    }

    /**
     * 刷新（或首次施加）魔法之境效果。已有效果时只重置剩余时间，不升 amplifier。
     */
    @Override
    public void applyEffect(LivingEntity target) {
        Entity owner = getOwner();
        if (!(owner instanceof LivingEntity livingOwner)) {
            return;
        }
        // 使用 Entity 重载：LivingEntity 重载在铁魔法 3.16 已标记待删除。
        if (!Utils.shouldHealEntity((Entity) livingOwner, (Entity) target)) {
            return;
        }
        target.addEffect(new MobEffectInstance(
                ModEffects.TERRA_MAGICA,
                TerraMagicaTuning.EFFECT_REFRESH_DURATION_TICKS,
                0,
                false,
                true,
                true
        ));
    }

    /**
     * 友方 buff 圈：允许命中任意可选中的存活实体；是否真正加 buff 由 {@link #applyEffect} 再判。
     */
    @Override
    protected boolean canHitEntity(Entity target) {
        return !target.isSpectator() && target.isAlive() && target.isPickable();
    }

    @Override
    protected Vec3 getInflation() {
        // 略抬高命中盒，避免只站立在边缘时因 Y 差漏检（同 HealingAoe）。
        return new Vec3(0.0, 1.0, 0.0);
    }

    @Override
    public float getParticleCount() {
        return TerraMagicaTuning.ZONE_AMBIENT_PARTICLE_COUNT;
    }

    /**
     * 环境粒子改由 {@link GlintstoneFx#zoneAmbient} 在客户端 tick 里生成，
     * 这里返回 empty，避免 AoeEntity 默认粉尘与辉石风格冲突。
     */
    @Override
    public Optional<ParticleOptions> getParticle() {
        return Optional.empty();
    }

    @Override
    public void tick() {
        if (!level().isClientSide && !placedCenterLight) {
            tryPlaceCenterLight();
        }
        super.tick();
    }

    @Override
    public void ambientParticles() {
        if (!level().isClientSide) {
            return;
        }
        GlintstoneFx.zoneAmbient(
                level(),
                getX(),
                getY(),
                getZ(),
                getRadius(),
                TerraMagicaTuning.ZONE_AMBIENT_PARTICLE_COUNT,
                TerraMagicaTuning.ZONE_AMBIENT_FILL_RADIUS_FRACTION
        );
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        clearCenterLight();
        super.remove(reason);
    }

    /**
     * 仅在中心格为空气时放置原版光方块，避免破坏地形。
     */
    private void tryPlaceCenterLight() {
        if (level().isClientSide || placedCenterLight) {
            return;
        }
        BlockPos lightPos = BlockPos.containing(getX(), getY(), getZ());
        BlockState existingState = level().getBlockState(lightPos);
        if (!existingState.isAir()) {
            // 脚底被占时略抬一格再试，仍非空气则放弃放光。
            lightPos = lightPos.above();
            existingState = level().getBlockState(lightPos);
            if (!existingState.isAir()) {
                placedCenterLight = true;
                centerLightBlockPos = null;
                return;
            }
        }
        BlockState lightState = Blocks.LIGHT.defaultBlockState()
                .setValue(LightBlock.LEVEL, TerraMagicaTuning.ZONE_CENTER_LIGHT_LEVEL);
        level().setBlock(lightPos, lightState, 3);
        centerLightBlockPos = lightPos.immutable();
        placedCenterLight = true;
    }

    /**
     * 清除本实体放置的中心光；若格内已被玩家改成别的方块则不动。
     */
    private void clearCenterLight() {
        if (level().isClientSide || centerLightBlockPos == null) {
            return;
        }
        BlockState stateAtLight = level().getBlockState(centerLightBlockPos);
        if (stateAtLight.is(Blocks.LIGHT)) {
            level().removeBlock(centerLightBlockPos, false);
        }
        centerLightBlockPos = null;
        placedCenterLight = false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("PlacedCenterLight", placedCenterLight);
        if (centerLightBlockPos != null) {
            compound.putInt("CenterLightX", centerLightBlockPos.getX());
            compound.putInt("CenterLightY", centerLightBlockPos.getY());
            compound.putInt("CenterLightZ", centerLightBlockPos.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        placedCenterLight = compound.getBoolean("PlacedCenterLight");
        if (compound.contains("CenterLightX")) {
            centerLightBlockPos = new BlockPos(
                    compound.getInt("CenterLightX"),
                    compound.getInt("CenterLightY"),
                    compound.getInt("CenterLightZ")
            );
        }
    }
}

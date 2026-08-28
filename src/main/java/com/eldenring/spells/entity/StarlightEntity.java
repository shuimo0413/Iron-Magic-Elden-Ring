package com.eldenring.spells.entity;

import com.eldenring.spells.registry.ModEntities;
import com.eldenring.spells.spell.StarlightSpell;
import com.eldenring.spells.spell.fx.StarlightFx;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 星光头顶小星：钉在主人头顶，随人移动，到期 / 反制 / 主人消失时清除。
 * <p>
 * 照明用原版 {@link Blocks#LIGHT} 跟着星星换格（亮度默认 14 = 火把）。
 * 原版光没有颜色；青色只在模型和火星粒子上。
 */
public class StarlightEntity extends Projectile implements AntiMagicSusceptible {

    /**
     * 相对碰撞箱顶端再抬高（方块）。调大 → 星星更高、第三人称更醒目；调小 → 更贴头。
     */
    public static final double HEAD_Y_OFFSET_BLOCKS = 0.55;

    /**
     * 剩余寿命（tick），给客户端做消散淡出。服务端每 tick 回写。
     */
    private static final EntityDataAccessor<Integer> DATA_REMAINING_TICKS =
            SynchedEntityData.defineId(StarlightEntity.class, EntityDataSerializers.INT);

    /** 已存活 tick。读档后接着算，不跟 {@link #tickCount} 混用。 */
    private int ageTicks;

    /** 总寿命（tick）。生成时从法术写入，读档还原。 */
    private int lifetimeTicks = StarlightSpell.STAR_DURATION_TICKS;

    /** 光源亮度 0–15。生成时从法术写入。 */
    private int lightLevel = StarlightSpell.LIGHT_LEVEL;

    /** 当前占用的光方块坐标；未放置时为 null。 */
    private BlockPos currentLightBlockPos;

    public StarlightEntity(EntityType<? extends StarlightEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public StarlightEntity(Level level, LivingEntity caster, int lifetimeTicks, int lightLevel) {
        this(ModEntities.STARLIGHT.get(), level);
        setOwner(caster);
        this.lifetimeTicks = Math.max(1, lifetimeTicks);
        this.lightLevel = Math.clamp(lightLevel, 0, LightBlock.MAX_LEVEL);
        this.entityData.set(DATA_REMAINING_TICKS, this.lifetimeTicks);
        snapToOwnerHead(caster);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_REMAINING_TICKS, StarlightSpell.STAR_DURATION_TICKS);
    }

    /**
     * 剩余寿命（tick）。渲染淡出用。
     */
    public int remainingLifetimeTicks() {
        return this.entityData.get(DATA_REMAINING_TICKS);
    }

    /**
     * 头顶锚点：碰撞箱顶再抬 {@link #HEAD_Y_OFFSET_BLOCKS}，水平跟脚底中心，避免侧移时星星甩到身后。
     */
    public static Vec3 worldPositionAboveHead(LivingEntity owner) {
        return new Vec3(
                owner.getX(),
                owner.getBoundingBox().maxY + HEAD_Y_OFFSET_BLOCKS,
                owner.getZ()
        );
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity owner = getOwner() instanceof LivingEntity livingOwner ? livingOwner : null;
        if (!level().isClientSide) {
            if (owner == null || !owner.isAlive() || owner.level() != level()) {
                discard();
                return;
            }
            this.ageTicks++;
            int remainingTicks = this.lifetimeTicks - this.ageTicks;
            this.entityData.set(DATA_REMAINING_TICKS, remainingTicks);
            if (remainingTicks <= 0) {
                discard();
                return;
            }
        }

        if (owner != null && owner.isAlive()) {
            snapToOwnerHead(owner);
        }

        if (!level().isClientSide) {
            refreshMovingLight();
        } else {
            StarlightFx.ambientEmbers(level(), getX(), getY(), getZ());
        }
    }

    private void snapToOwnerHead(LivingEntity owner) {
        Vec3 headAnchor = worldPositionAboveHead(owner);
        setPos(headAnchor.x, headAnchor.y, headAnchor.z);
    }

    /**
     * 星星换格时搬走光方块：先清旧格，再在新格放。只动空气 / 水源 / 自己留下的光。
     */
    private void refreshMovingLight() {
        BlockPos desiredLightPos = resolveLightBlockPos();
        if (desiredLightPos.equals(this.currentLightBlockPos)) {
            return;
        }
        clearCurrentLight();
        if (tryPlaceLightAt(desiredLightPos)) {
            this.currentLightBlockPos = desiredLightPos.immutable();
        }
    }

    /**
     * 优先星星所在格；顶到方块里（矮洞穴）则退到眼睛格，再退到身子上一格。
     */
    private BlockPos resolveLightBlockPos() {
        BlockPos starBlockPos = BlockPos.containing(getX(), getY(), getZ());
        if (canHostLight(starBlockPos)) {
            return starBlockPos;
        }
        if (getOwner() instanceof LivingEntity livingOwner) {
            BlockPos eyeBlockPos = BlockPos.containing(livingOwner.getEyePosition());
            if (canHostLight(eyeBlockPos)) {
                return eyeBlockPos;
            }
            BlockPos bodyAirPos = livingOwner.blockPosition().above();
            if (canHostLight(bodyAirPos)) {
                return bodyAirPos;
            }
        }
        return starBlockPos;
    }

    private boolean canHostLight(BlockPos blockPos) {
        BlockState blockState = level().getBlockState(blockPos);
        if (blockState.isAir()) {
            return true;
        }
        if (blockState.is(Blocks.WATER) && blockState.getFluidState().isSource()) {
            return true;
        }
        return isOwnLightBlock(blockPos, blockState);
    }

    private boolean isOwnLightBlock(BlockPos blockPos, BlockState blockState) {
        return blockState.is(Blocks.LIGHT) && blockPos.equals(this.currentLightBlockPos);
    }

    private boolean tryPlaceLightAt(BlockPos blockPos) {
        BlockState existingState = level().getBlockState(blockPos);
        if (isOwnLightBlock(blockPos, existingState)) {
            return true;
        }
        if (existingState.isAir()) {
            level().setBlock(
                    blockPos,
                    Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, this.lightLevel),
                    3
            );
            return true;
        }
        if (existingState.is(Blocks.WATER) && existingState.getFluidState().isSource()) {
            level().setBlock(
                    blockPos,
                    Blocks.LIGHT.defaultBlockState()
                            .setValue(LightBlock.LEVEL, this.lightLevel)
                            .setValue(LightBlock.WATERLOGGED, true),
                    3
            );
            return true;
        }
        return false;
    }

    private void clearCurrentLight() {
        if (level().isClientSide || this.currentLightBlockPos == null) {
            return;
        }
        BlockState stateAtLight = level().getBlockState(this.currentLightBlockPos);
        if (stateAtLight.is(Blocks.LIGHT)) {
            if (stateAtLight.getValue(LightBlock.WATERLOGGED)) {
                level().setBlock(this.currentLightBlockPos, Blocks.WATER.defaultBlockState(), 3);
            } else {
                level().removeBlock(this.currentLightBlockPos, false);
            }
        }
        this.currentLightBlockPos = null;
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        clearCurrentLight();
        super.remove(reason);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putInt("AgeTicks", this.ageTicks);
        compoundTag.putInt("LifetimeTicks", this.lifetimeTicks);
        compoundTag.putInt("LightLevel", this.lightLevel);
        if (this.currentLightBlockPos != null) {
            compoundTag.putInt("LightX", this.currentLightBlockPos.getX());
            compoundTag.putInt("LightY", this.currentLightBlockPos.getY());
            compoundTag.putInt("LightZ", this.currentLightBlockPos.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.ageTicks = compoundTag.getInt("AgeTicks");
        this.lifetimeTicks = Math.max(1, compoundTag.getInt("LifetimeTicks"));
        this.lightLevel = Math.clamp(compoundTag.getInt("LightLevel"), 0, LightBlock.MAX_LEVEL);
        this.entityData.set(DATA_REMAINING_TICKS, this.lifetimeTicks - this.ageTicks);
        if (compoundTag.contains("LightX")) {
            this.currentLightBlockPos = new BlockPos(
                    compoundTag.getInt("LightX"),
                    compoundTag.getInt("LightY"),
                    compoundTag.getInt("LightZ")
            );
        }
    }
}

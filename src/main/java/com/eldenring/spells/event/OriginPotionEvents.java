package com.eldenring.spells.event;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 起源药剂死亡标记、悬浮起源辉石掉落与轻微上下浮动。
 */
@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID)
public final class OriginPotionEvents {
    /**
     * 玩家 persistent NBT：喝下起源药剂后置位，死亡掉落处理完清除。
     * 单位：布尔标记，无时长。
     */
    public static final String ORIGIN_POTION_KILL_TAG = EldenRingSpellsMod.MOD_ID + ":origin_potion_kill";

    /**
     * ItemEntity persistent NBT：标记为「起源辉石悬浮掉落」，每 tick 做无重力 bob。
     */
    public static final String FLOATING_ORIGIN_GLINTSTONE_TAG =
            EldenRingSpellsMod.MOD_ID + ":floating_origin_glintstone";

    /** 捡起延迟（tick）：避免死后立刻吸回背包。调大更难立刻捡。 */
    private static final int PICKUP_DELAY_TICKS = 40;

    /** 悬浮相对脚底的高度偏移（方块）：约胸口。调大更高。 */
    private static final double SPAWN_HEIGHT_OFFSET_BLOCKS = 1.2D;

    /** bob 振幅（方块）。调大浮动更明显。 */
    private static final double BOB_AMPLITUDE_BLOCKS = 0.04D;

    /** bob 角速度（弧度/tick）。调大浮动更快。 */
    private static final double BOB_ANGULAR_SPEED_RADIANS_PER_TICK = 0.12D;

    private OriginPotionEvents() {
    }

    /**
     * 服务端饮用起源药剂后调用：创造模式跳过；否则打标记并以 genericKill 处死。
     */
    public static void applyOriginDeath(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        if (player.getAbilities().instabuild) {
            return;
        }
        player.getPersistentData().putBoolean(ORIGIN_POTION_KILL_TAG, true);
        // genericKill 等同 /kill，带 bypasses_invulnerability，图腾无效
        player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
    }

    /**
     * 带标记死亡时，在胸口高度额外生成无重力起源辉石（库存掉落仍走原版）。
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!player.getPersistentData().getBoolean(ORIGIN_POTION_KILL_TAG)) {
            return;
        }
        player.getPersistentData().remove(ORIGIN_POTION_KILL_TAG);

        Level level = player.level();
        if (level.isClientSide) {
            return;
        }

        double spawnX = player.getX();
        double spawnY = player.getY() + SPAWN_HEIGHT_OFFSET_BLOCKS;
        double spawnZ = player.getZ();
        ItemEntity glintstoneDrop = new ItemEntity(
                level,
                spawnX,
                spawnY,
                spawnZ,
                new ItemStack(ModItems.ORIGIN_GLINTSTONE.get())
        );
        glintstoneDrop.setNoGravity(true);
        glintstoneDrop.setDeltaMovement(Vec3.ZERO);
        glintstoneDrop.setPickUpDelay(PICKUP_DELAY_TICKS);
        glintstoneDrop.getPersistentData().putBoolean(FLOATING_ORIGIN_GLINTSTONE_TAG, true);
        // 直接进世界，避免掉落列表后续处理改写运动
        level.addFreshEntity(glintstoneDrop);
    }

    /**
     * 悬浮起源辉石：每 tick 清重力位移并做轻微正弦浮动，保持可捡。
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) {
            return;
        }
        if (!itemEntity.getPersistentData().getBoolean(FLOATING_ORIGIN_GLINTSTONE_TAG)) {
            return;
        }
        if (itemEntity.level().isClientSide) {
            return;
        }
        itemEntity.setNoGravity(true);
        double bobY = Math.sin(itemEntity.tickCount * BOB_ANGULAR_SPEED_RADIANS_PER_TICK) * BOB_AMPLITUDE_BLOCKS;
        itemEntity.setDeltaMovement(0.0D, bobY * 0.15D, 0.0D);
        itemEntity.hasImpulse = true;
    }
}

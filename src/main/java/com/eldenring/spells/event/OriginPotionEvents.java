package com.eldenring.spells.event;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModItems;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/**
 * 起源药剂死亡标记、悬浮起源辉石掉落与轻微上下浮动。
 * <p>
 * Forge 1.20.1 没有 {@code EntityTickEvent}（1.21 才有），且 {@link ItemEntity} 不是
 * {@code LivingEntity}，无法用 {@code LivingEvent.LivingTickEvent} 兜底。因此改为：
 * 实体进世界时把「打了悬浮标记」的掉落登记进 {@link #TRACKED_FLOATING_DROPS}，
 * 再在服务端 {@code LevelTickEvent} 里只遍历被登记的实体做 bob。
 * 这样避免每 tick 扫描 {@code level.entitiesForRendering()} 那样的全量实体列表。
 */
@Mod.EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID)
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

    /**
     * 正在做悬浮 bob 的掉落，按维度（{@link Level}）分组。
     * 每次维度 tick 只遍历该维度里被登记的少量实体，避免全量扫描实体列表。
     * 键是服务端 Level 实例，靠 {@link LevelEvent.Unload} 清掉，防止换存档后残留旧维度引用。
     */
    private static final Map<Level, Set<ItemEntity>> TRACKED_FLOATING_DROPS = new HashMap<>();

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
     * 掉落物进世界时登记：只收服务端、带悬浮标记的 {@link ItemEntity}。
     * 除死亡当场生成外，存档重载 / 区块重新加载的实体也会走这里，所以登记逻辑放事件里而不是生成点。
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ItemEntity glintstoneDrop)) {
            return;
        }
        if (!glintstoneDrop.getPersistentData().getBoolean(FLOATING_ORIGIN_GLINTSTONE_TAG)) {
            return;
        }
        TRACKED_FLOATING_DROPS
                .computeIfAbsent(glintstoneDrop.level(), level -> new HashSet<>())
                .add(glintstoneDrop);
    }

    /**
     * 悬浮起源辉石：每 tick 清重力位移并做轻微正弦浮动，保持可捡。
     * 只处理已登记的掉落；已被捡起 / 反序列化淘汰（{@code isRemoved()}）的实体顺手剔除。
     */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        // LevelTickEvent 每 tick Start/End 各触发一次，只在 End 处理，保证一 tick 只动一次
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Level level = event.level;
        if (level.isClientSide()) {
            return;
        }
        Set<ItemEntity> trackedDrops = TRACKED_FLOATING_DROPS.get(level);
        if (trackedDrops == null || trackedDrops.isEmpty()) {
            return;
        }
        trackedDrops.removeIf(glintstoneDrop -> glintstoneDrop.isRemoved() || glintstoneDrop.level() != level);
        for (ItemEntity glintstoneDrop : trackedDrops) {
            glintstoneDrop.setNoGravity(true);
            double bobY = Math.sin(glintstoneDrop.tickCount * BOB_ANGULAR_SPEED_RADIANS_PER_TICK)
                    * BOB_AMPLITUDE_BLOCKS;
            glintstoneDrop.setDeltaMovement(0.0D, bobY * 0.15D, 0.0D);
            glintstoneDrop.hasImpulse = true;
        }
        if (trackedDrops.isEmpty()) {
            TRACKED_FLOATING_DROPS.remove(level);
        }
    }

    /**
     * 维度卸载（退出存档 / 切换服务器）时清掉该维度的追踪集合，防止静态 Map 一直持有旧 Level。
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) {
            TRACKED_FLOATING_DROPS.remove(level);
        }
    }
}

package com.eldenring.spells.item;

import net.minecraft.world.entity.player.Player;

/**
 * 卢瑟特的辉石杖的蓝耗倍率：数值来自 {@code iss_elden_ring-server.toml} 的
 * {@code equipment.lusat_staff.manaCostMultiplier}，由
 * {@link com.eldenring.spells.spell.cost.SpellManaCostCalculator} 与其他法杖 / 护符倍率相乘。
 * <p>
 * 服务端配置会随登录同步到客户端并触发同一套 {@code apply}，因此不需要像亚兹勒杖那样单独发包。
 */
public final class LusatStaffBalance {

    /**
     * 主手持杖时全学派蓝耗倍率默认值。1.50 = 蓝耗 +50%；调大 → 高法强的代价更重。
     */
    public static final double DEFAULT_MANA_COST_MULTIPLIER = 1.50D;

    private static volatile double manaCostMultiplier = DEFAULT_MANA_COST_MULTIPLIER;

    private LusatStaffBalance() {
    }

    public static double manaCostMultiplier() {
        return manaCostMultiplier;
    }

    public static void configure(double configuredManaCostMultiplier) {
        manaCostMultiplier = configuredManaCostMultiplier;
    }

    /** 世界配置卸载后恢复默认值，供主菜单 / 下次进世界前的悬浮提示使用。 */
    public static void resetDefaults() {
        manaCostMultiplier = DEFAULT_MANA_COST_MULTIPLIER;
    }

    public static boolean isMainhandLusat(Player player) {
        return player != null && player.getMainHandItem().getItem() instanceof LusatGlintstoneStaffItem;
    }

    public static double currentMultiplier(Player player) {
        return isMainhandLusat(player) ? manaCostMultiplier : 1.0D;
    }
}

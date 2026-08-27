package com.eldenring.spells.world;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.material.MapColor;

import java.util.Locale;

/**
 * 三色辉石：颜色只是数据（id 前缀、地图色），方块逻辑共享一份。
 * <p>
 * 不持有物品引用，避免 {@code world} 包依赖 {@code ModItems}。
 */
public enum GlintstoneColor implements StringRepresentable {
    CYAN("cyan", MapColor.DIAMOND),
    BLUE("blue", MapColor.COLOR_BLUE),
    PURPLE("purple", MapColor.COLOR_PURPLE);

    private final String idPrefix;
    private final MapColor mapColor;

    GlintstoneColor(String idPrefix, MapColor mapColor) {
        this.idPrefix = idPrefix;
        this.mapColor = mapColor;
    }

    /** 资源 id 前缀，如 {@code cyan}。 */
    public String idPrefix() {
        return idPrefix;
    }

    public MapColor mapColor() {
        return mapColor;
    }

    @Override
    public String getSerializedName() {
        return idPrefix;
    }

    public static GlintstoneColor byIndex(int index) {
        GlintstoneColor[] values = values();
        return values[Math.floorMod(index, values.length)];
    }

    @Override
    public String toString() {
        return idPrefix.toUpperCase(Locale.ROOT);
    }
}

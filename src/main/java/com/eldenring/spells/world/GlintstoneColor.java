package com.eldenring.spells.world;

import com.eldenring.spells.registry.ModItems;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * 三色辉石：颜色只是数据（id 前缀、地图色、掉落碎片），方块逻辑共享一份。
 */
public enum GlintstoneColor implements StringRepresentable {
    CYAN("cyan", MapColor.DIAMOND, () -> ModItems.CYAN_GLINTSTONE_SHARD),
    BLUE("blue", MapColor.COLOR_BLUE, () -> ModItems.BLUE_GLINTSTONE_SHARD),
    PURPLE("purple", MapColor.COLOR_PURPLE, () -> ModItems.PURPLE_GLINTSTONE_SHARD);

    private final String idPrefix;
    private final MapColor mapColor;
    private final Supplier<DeferredItem<Item>> shardItem;

    GlintstoneColor(String idPrefix, MapColor mapColor, Supplier<DeferredItem<Item>> shardItem) {
        this.idPrefix = idPrefix;
        this.mapColor = mapColor;
        this.shardItem = shardItem;
    }

    /** 资源 id 前缀，如 {@code cyan}。 */
    public String idPrefix() {
        return idPrefix;
    }

    public MapColor mapColor() {
        return mapColor;
    }

    /** 挖完整簇 / 熔炼矿石得到的碎片。 */
    public Item shard() {
        return shardItem.get().get();
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

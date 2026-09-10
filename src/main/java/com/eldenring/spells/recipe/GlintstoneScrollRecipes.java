package com.eldenring.spells.recipe;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModItems;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 辉石卷轴「抄写配方」：铁魔法卷轴锻造台焦点槽放入哪颗辉石，决定能抄哪些咒。
 * <p>
 * 焦点物品在取出成品时会被消耗（铁魔法原版行为），因此颜色 / 起源辉石本身就是材料成本。
 * 非本模组法术不干预，避免影响其它学派。
 * <p>
 * 数值来源：产品表「魔法配方」——青 / 蓝 / 紫碎片 + 起源辉石四类材料。
 */
public final class GlintstoneScrollRecipes {

    /**
     * 法术 ResourceLocation → 抄写所需焦点物品。
     * 使用 {@link LinkedHashMap} 保持表顺序，便于对照与调试。
     */
    private static final Map<ResourceLocation, Supplier<? extends Item>> SPELL_TO_FOCUS;

    static {
        Map<ResourceLocation, Supplier<? extends Item>> map = new LinkedHashMap<>();

        // —— 青色辉石碎片：学院弹道 / 场地 / 海摩 ——
        putCyan(map, "glintstone_pebble");
        putCyan(map, "swift_glintstone_shard");
        putCyan(map, "glintstone_arc");
        putCyan(map, "crystal_barrage");
        putCyan(map, "crystal_burst");
        putCyan(map, "great_glintstone_shard");
        putCyan(map, "glintstone_comet");
        putCyan(map, "glintstone_stars");
        putCyan(map, "star_shower");
        putCyan(map, "comet");
        putCyan(map, "spiral_shard");
        putCyan(map, "starlight");
        putCyan(map, "terra_magica");
        putCyan(map, "gavel_of_haima");
        putCyan(map, "cannon_of_haima");

        // —— 起源辉石：高阶星落 / 亚兹勒 ——
        putOrigin(map, "stars_of_ruin");
        putOrigin(map, "founding_rain_of_stars");
        putOrigin(map, "comet_azur");

        // —— 蓝色辉石碎片：卡利亚近战 / 辉剑阵 ——
        putBlue(map, "carian_slicer");
        putBlue(map, "carian_greatsword");
        putBlue(map, "carian_piercer");
        putBlue(map, "magic_glintblade");
        putBlue(map, "glintblade_phalanx");
        putBlue(map, "carian_phalanx");
        putBlue(map, "greatblade_phalanx");

        // —— 紫色辉石碎片：重力 ——
        putPurple(map, "gravity_ball");
        putPurple(map, "collapsing_stars");

        SPELL_TO_FOCUS = Collections.unmodifiableMap(map);
    }

    private GlintstoneScrollRecipes() {
    }

    /**
     * 只读配方表（法术 id → 焦点物品供应商）。
     */
    public static Map<ResourceLocation, Supplier<? extends Item>> all() {
        return SPELL_TO_FOCUS;
    }

    /**
     * 当前焦点是否允许抄写该法术。
     * <ul>
     *   <li>非本模组法术：恒为 {@code true}（不改其它学派）</li>
     *   <li>本模组法术：焦点物品必须与表中材料一致；未登记的法术不可抄</li>
     * </ul>
     *
     * @param spell      候选法术
     * @param focusStack 锻造台焦点槽物品（可为空）
     */
    public static boolean matchesFocus(AbstractSpell spell, ItemStack focusStack) {
        ResourceLocation spellId = spell.getSpellResource();
        if (!EldenRingSpellsMod.MOD_ID.equals(spellId.getNamespace())) {
            return true;
        }
        Supplier<? extends Item> requiredFocus = SPELL_TO_FOCUS.get(spellId);
        if (requiredFocus == null) {
            return false;
        }
        return !focusStack.isEmpty() && focusStack.is(requiredFocus.get());
    }

    /**
     * @return 该法术登记的焦点物品；非本模组或未登记时为 {@code null}
     */
    public static Item getRequiredFocus(AbstractSpell spell) {
        Supplier<? extends Item> requiredFocus = SPELL_TO_FOCUS.get(spell.getSpellResource());
        return requiredFocus == null ? null : requiredFocus.get();
    }

    private static void putCyan(Map<ResourceLocation, Supplier<? extends Item>> map, String spellPath) {
        map.put(spellId(spellPath), ModItems.CYAN_GLINTSTONE_SHARD);
    }

    private static void putBlue(Map<ResourceLocation, Supplier<? extends Item>> map, String spellPath) {
        map.put(spellId(spellPath), ModItems.BLUE_GLINTSTONE_SHARD);
    }

    private static void putPurple(Map<ResourceLocation, Supplier<? extends Item>> map, String spellPath) {
        map.put(spellId(spellPath), ModItems.PURPLE_GLINTSTONE_SHARD);
    }

    private static void putOrigin(Map<ResourceLocation, Supplier<? extends Item>> map, String spellPath) {
        map.put(spellId(spellPath), ModItems.ORIGIN_GLINTSTONE);
    }

    private static ResourceLocation spellId(String path) {
        return ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, path);
    }
}

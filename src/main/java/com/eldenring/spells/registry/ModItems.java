package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.item.AstrologerStaffItem;
import com.eldenring.spells.item.AzurGlintstoneStaffItem;
import com.eldenring.spells.item.OriginPotionItem;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 本模组物品注册入口。辉石法术不单独注册卷轴物品。
 * <p>
 * 创造栏只用铁魔法通用卷轴 {@code irons_spellbooks:scroll}，法术写在
 * {@link ISpellContainer} 里，因此抄写台可以直接抄。外观由客户端
 * {@code ScrollModel} mixin 切到 {@code item/<spell>_scroll}。
 * <p>
 * 三色辉石碎片与起源辉石是学派触媒（Focus）：放入卷轴锻造台焦点槽可抄对应辉石咒
 *（见 {@link com.eldenring.spells.recipe.GlintstoneScrollRecipes}；取出成品时消耗焦点）。
 * 星星法典 / 起源秘典是辉石学派魔法书，走铁魔法原生 {@link SpellBook}（Curios spellbook 槽）。
 * 观星杖 / 亚兹勒的辉石杖是铁魔法 {@link io.redspace.ironsspellbooks.item.weapons.StaffItem} 触媒（辉石强度 +10%）。
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(EldenRingSpellsMod.MOD_ID);

    /**
     * 星星法典：辉石学派魔法书。
     * <p>
     * 套用铁魔法原生 {@link SpellBook}（与烈焰书同类），10 个法术槽；
     * 装备后：辉石法术强度 +10%、最大法力 +200。
     * 物品模型走铁魔法 {@code template_spell_book_model}；客户端注册
     * {@code SpellBookCurioRenderer} 后腰侧显示立体书。
     * 须加入 {@code curios:spellbook} 物品标签才能装进魔法书槽。
     */
    public static final DeferredItem<Item> STAR_CODEX = ITEMS.register(
            "star_codex",
            () -> new SpellBook(10).withSpellbookAttributes(
                    new AttributeContainer(
                            ModAttributes.GLINTSTONE_SPELL_POWER,
                            0.10D,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    ),
                    new AttributeContainer(
                            AttributeRegistry.MAX_MANA,
                            200.0D,
                            AttributeModifier.Operation.ADD_VALUE
                    )
            )
    );

    /**
     * 起源秘典：高阶辉石学派魔法书。
     * <p>
     * 与星星法典同用铁魔法 {@link SpellBook} 模板（10 槽、立体书模型）；
     * 装备后：辉石法术强度 +30%、最大法力 +300。
     * 配方暂留空；须加入 {@code curios:spellbook} 才能装进魔法书槽。
     */
    public static final DeferredItem<Item> ORIGIN_CODEX = ITEMS.register(
            "origin_codex",
            () -> new SpellBook(10).withSpellbookAttributes(
                    new AttributeContainer(
                            ModAttributes.GLINTSTONE_SPELL_POWER,
                            0.30D,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    ),
                    new AttributeContainer(
                            AttributeRegistry.MAX_MANA,
                            300.0D,
                            AttributeModifier.Operation.ADD_VALUE
                    )
            )
    );

    /**
     * 观星杖：铁魔法 {@link io.redspace.ironsspellbooks.item.weapons.StaffItem} 触媒。
     * 手持时可右键施法（与铁魔法魔杖相同）；辉石法术强度 +10%。
     */
    public static final DeferredItem<Item> ASTROLOGER_STAFF = ITEMS.register(
            "astrologer_staff",
            AstrologerStaffItem::new
    );

    /**
     * 亚兹勒的辉石杖：铁魔法 {@link io.redspace.ironsspellbooks.item.weapons.StaffItem} 触媒。
     * 手持时可右键施法（与铁魔法魔杖相同）；辉石法术强度 +10%。
     */
    public static final DeferredItem<Item> AZUR_GLINTSTONE_STAFF = ITEMS.register(
            "azur_glintstone_staff",
            AzurGlintstoneStaffItem::new
    );

    /**
     * 青色辉石碎片。学院弹道 / 场地 / 海摩等咒的抄写材料（焦点槽，抄成消耗）。
     */
    public static final DeferredItem<Item> CYAN_GLINTSTONE_SHARD = ITEMS.register(
            "cyan_glintstone_shard",
            () -> new Item(new Item.Properties())
    );

    /**
     * 蓝色辉石碎片。卡利亚近战 / 辉剑阵等咒的抄写材料（焦点槽，抄成消耗）。
     */
    public static final DeferredItem<Item> BLUE_GLINTSTONE_SHARD = ITEMS.register(
            "blue_glintstone_shard",
            () -> new Item(new Item.Properties())
    );

    /**
     * 紫色辉石碎片。重力系咒的抄写材料（焦点槽，抄成消耗）。
     */
    public static final DeferredItem<Item> PURPLE_GLINTSTONE_SHARD = ITEMS.register(
            "purple_glintstone_shard",
            () -> new Item(new Item.Properties())
    );

    /**
     * 起源晶体：八辉石晶簇围下界之星合成；投入炼药锅与粗制药水炼成起源药剂。
     */
    public static final DeferredItem<Item> ORIGIN_CRYSTAL = ITEMS.register(
            "origin_crystal",
            () -> new Item(new Item.Properties())
    );

    /**
     * 起源辉石：喝下起源药剂死亡后在原地悬浮掉落；不可合成。
     * 亦为毁灭流星 / 创星雨 / 彗星亚兹勒的抄写材料（焦点槽，抄成消耗）。
     */
    public static final DeferredItem<Item> ORIGIN_GLINTSTONE = ITEMS.register(
            "origin_glintstone",
            () -> new Item(new Item.Properties())
    );

    /**
     * 起源药剂：炼药锅装瓶产物；饮用后代码处死并掉落起源辉石；恒带附魔光。
     * 堆叠上限 16，与铁魔法 elixir 接近。
     */
    public static final DeferredItem<Item> ORIGIN_POTION = ITEMS.register(
            "origin_potion",
            () -> new OriginPotionItem(new Item.Properties().stacksTo(16))
    );

    /**
     * 卡利亚迅剑视觉用物品：只给挥砍时 {@code ItemRenderer} 画手里那把像素剑。
     * 不进创造栏，玩家不会当武器用。
     */
    public static final DeferredItem<Item> CARIAN_SLICER_SWORD = ITEMS.register(
            "carian_slicer_sword",
            () -> new Item(new Item.Properties())
    );

    /**
     * 卡利亚大剑视觉用物品：只给挥砍时画手里那把像素剑。
     * 贴图像素与迅剑相同，模型 JSON / display 是大剑自己的，改 JSON 不会动迅剑。
     * 不进创造栏。
     */
    public static final DeferredItem<Item> CARIAN_GREATSWORD_SWORD = ITEMS.register(
            "carian_greatsword_sword",
            () -> new Item(new Item.Properties())
    );

    /**
     * 卡利亚贯刺视觉用物品：贴图从大剑拷出，模型 JSON / display 是贯刺自己的。
     * 不进创造栏。
     */
    public static final DeferredItem<Item> CARIAN_PIERCER_SWORD = ITEMS.register(
            "carian_piercer_sword",
            () -> new Item(new Item.Properties())
    );

    static {
        // BlockItem 与方块同 id；必须在 ModBlocks 已向总线注册之后再 register(ITEMS)
        for (ModBlocks.ColorSet set : ModBlocks.BY_COLOR.values()) {
            ITEMS.registerSimpleBlockItem(set.crystalBlock);
            ITEMS.registerSimpleBlockItem(set.cluster);
        }
    }

    private ModItems() {
    }

    /**
     * 生成一张铁魔法通用卷轴并写入指定法术。
     *
     * @param spell      要写入的法术
     * @param spellLevel 法术等级（从 {@link AbstractSpell#getMinLevel()} 到 {@link AbstractSpell#getMaxLevel()}）
     */
    public static ItemStack createFilledScroll(Supplier<AbstractSpell> spell, int spellLevel) {
        ItemStack stack = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell.get(), spellLevel, stack);
        return stack;
    }

    /** 创造栏图标用：1 级辉石魔砾通用卷轴。 */
    public static ItemStack createFilledScroll(int spellLevel) {
        return createFilledScroll(ModSpells.GLINTSTONE_PEBBLE, spellLevel);
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}

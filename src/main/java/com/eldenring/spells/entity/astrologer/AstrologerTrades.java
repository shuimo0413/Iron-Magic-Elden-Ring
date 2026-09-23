package com.eldenring.spells.entity.astrologer;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModItems;
import com.eldenring.spells.registry.ModSpells;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 观星者交易构造：显式白名单 + 起源防御拒绝。
 * <p>
 * 不依赖抄写配方，不读取实体实例状态。新增法术默认不售，必须人工加入白名单。
 * 例外：极限模式下允许以「1 下界之星 + 64 绿宝石」售卖起源辉石（药剂献祭无法回档）。
 */
public final class AstrologerTrades {

    /**
     * 可售普通辉石法术白名单。新增法术不会自动进入。
     */
    private static final List<Supplier<AbstractSpell>> SELLABLE_SPELLS = List.of(
            ModSpells.GLINTSTONE_PEBBLE,
            ModSpells.SWIFT_GLINTSTONE_SHARD,
            ModSpells.GLINTSTONE_ARC,
            ModSpells.CRYSTAL_BARRAGE,
            ModSpells.CRYSTAL_BURST,
            ModSpells.GREAT_GLINTSTONE_SHARD,
            ModSpells.GLINTSTONE_COMET,
            ModSpells.GLINTSTONE_STARS,
            ModSpells.STAR_SHOWER,
            ModSpells.COMET,
            ModSpells.SPIRAL_SHARD,
            ModSpells.STARLIGHT,
            ModSpells.TERRA_MAGICA,
            ModSpells.GAVEL_OF_HAIMA,
            ModSpells.CANNON_OF_HAIMA,
            ModSpells.CARIAN_SLICER,
            ModSpells.CARIAN_GREATSWORD,
            ModSpells.CARIAN_PIERCER,
            ModSpells.MAGIC_GLINTBLADE,
            ModSpells.GLINTBLADE_PHALANX,
            ModSpells.CARIAN_PHALANX,
            ModSpells.GREATBLADE_PHALANX,
            ModSpells.GRAVITY_BALL,
            ModSpells.COLLAPSING_STARS
    );

    /**
     * 起源三法术防御列表：即便误入白名单也绝不能售出。
     */
    private static final Set<ResourceLocation> ORIGIN_SPELL_DENYLIST = Set.of(
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "stars_of_ruin"),
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "founding_rain_of_stars"),
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "comet_azur")
    );

    private AstrologerTrades() {
    }

    /**
     * 极限模式下起源辉石交易：每次补货可用次数。
     * 调大 → 同一补货周期内可多买几颗；调小 → 更稀缺。
     */
    private static final int HARDCORE_ORIGIN_GLINTSTONE_MAX_USES = 1;

    /**
     * 生成一次库存。调用方负责持久化；本方法不碰实体字段。
     *
     * @param hardcoreMode 当前世界是否极限模式；为 true 时追加起源辉石双代价报价
     *                     （药剂献祭在极限下无法回档，用下界之星门槛替代）
     */
    public static MerchantOffers createOffers(RandomSource random, boolean hardcoreMode) {
        MerchantOffers offers = new MerchantOffers();

        offers.addAll(createFillerOffers(random, 2));
        offers.add(sellItem(new ItemStack(ModItems.CYAN_GLINTSTONE_SHARD.get(), 2 + random.nextInt(3)), 6, 10, 12, random));
        offers.add(sellItem(new ItemStack(ModItems.BLUE_GLINTSTONE_SHARD.get(), 2 + random.nextInt(3)), 6, 10, 12, random));
        offers.add(sellItem(new ItemStack(ModItems.PURPLE_GLINTSTONE_SHARD.get(), 2 + random.nextInt(3)), 6, 10, 12, random));

        addOptionalScrollOffer(offers, random, 0.0f, 0.35f);
        addOptionalScrollOffer(offers, random, 0.30f, 0.70f);
        addOptionalScrollOffer(offers, random, 0.65f, 1.0f);

        if (random.nextFloat() < 0.55f) {
            offers.add(sellItem(new ItemStack(ItemRegistry.INK_COMMON.get()), 4, 7, 8, random));
        }
        if (random.nextFloat() < 0.40f) {
            offers.add(sellItem(new ItemStack(ItemRegistry.INK_UNCOMMON.get()), 8, 12, 6, random));
        }
        if (random.nextFloat() < 0.25f) {
            offers.add(sellItem(new ItemStack(ItemRegistry.INK_RARE.get()), 14, 20, 4, random));
        }
        offers.add(sellItem(new ItemStack(ModItems.PRIMAL_GLINTSTONE_BLADE.get()), 32, 64, 1, random));

        if (random.nextFloat() < 0.55f) {
            offers.add(sellItem(new ItemStack(ModItems.ASTROLOGER_STAFF.get()), 28, 36, 1, random));
        }
        if (random.nextFloat() < 0.45f) {
            offers.add(sellItem(new ItemStack(ModItems.STAR_CODEX.get()), 36, 48, 1, random));
        }

        offers.removeIf(Objects::isNull);
        offers.removeIf(AstrologerTrades::violatesOriginDenylist);
        // 故意放在拒绝列表过滤之后：极限模式才允许售卖起源辉石
        if (hardcoreMode) {
            offers.add(createHardcoreOriginGlintstoneOffer());
        }
        return offers;
    }

    /**
     * 若已是极限世界但库存里还没有起源辉石报价（旧存档 / 升级前生成的商人），补一条。
     * 非极限或已有该报价时不做任何事。
     */
    public static void ensureHardcoreOriginGlintstoneOffer(MerchantOffers offers, boolean hardcoreMode) {
        if (!hardcoreMode || offers == null) {
            return;
        }
        for (MerchantOffer offer : offers) {
            if (offer != null && offer.getResult().is(ModItems.ORIGIN_GLINTSTONE.get())) {
                return;
            }
        }
        offers.add(createHardcoreOriginGlintstoneOffer());
    }

    /**
     * 极限模式专用：1 下界之星 + 64 绿宝石 → 1 起源辉石。
     * 不走 {@link #sellItem}，以免被起源物品拒绝列表拦掉。
     */
    private static MerchantOffer createHardcoreOriginGlintstoneOffer() {
        return new MerchantOffer(
                new ItemCost(Items.NETHER_STAR, 1),
                Optional.of(new ItemCost(Items.EMERALD, 64)),
                new ItemStack(ModItems.ORIGIN_GLINTSTONE.get()),
                0,
                HARDCORE_ORIGIN_GLINTSTONE_MAX_USES,
                20,
                0.05f
        );
    }

    private static List<MerchantOffer> createFillerOffers(RandomSource random, int count) {
        List<MerchantOffer> pool = new ArrayList<>();
        pool.add(buyItem(Items.PAPER, 12, 18, 1, 2, 16, random));
        pool.add(buyItem(Items.BOOK, 2, 4, 2, 4, 12, random));
        pool.add(buyItem(Items.AMETHYST_SHARD, 4, 8, 2, 4, 12, random));
        pool.add(sellItem(new ItemStack(Items.TORCH, 8 + random.nextInt(9)), 1, 3, 16, random));
        pool.add(sellItem(new ItemStack(Items.SPYGLASS), 8, 12, 4, random));
        pool.add(sellItem(new ItemStack(Items.ENDER_PEARL, 1 + random.nextInt(2)), 6, 10, 8, random));
        pool.add(sellItem(new ItemStack(Items.LANTERN, 2), 4, 7, 10, random));

        List<MerchantOffer> selected = new ArrayList<>();
        List<MerchantOffer> remaining = new ArrayList<>(pool);
        int pickCount = Math.min(count, remaining.size());
        for (int pickIndex = 0; pickIndex < pickCount; pickIndex++) {
            int chosenIndex = random.nextInt(remaining.size());
            selected.add(remaining.remove(chosenIndex));
        }
        return selected;
    }

    private static void addOptionalScrollOffer(MerchantOffers offers, RandomSource random, float minQuality, float maxQuality) {
        MerchantOffer offer = createScrollOffer(random, minQuality, maxQuality);
        if (offer != null) {
            offers.add(offer);
        }
    }

    private static MerchantOffer createScrollOffer(RandomSource random, float minQuality, float maxQuality) {
        List<AbstractSpell> candidates = new ArrayList<>();
        for (Supplier<AbstractSpell> supplier : SELLABLE_SPELLS) {
            AbstractSpell spell = supplier.get();
            if (spell == null || !spell.isEnabled()) {
                continue;
            }
            if (ORIGIN_SPELL_DENYLIST.contains(spell.getSpellResource())) {
                continue;
            }
            candidates.add(spell);
        }
        if (candidates.isEmpty()) {
            return null;
        }

        AbstractSpell spell = candidates.get(random.nextInt(candidates.size()));
        int maxLevel = Math.max(1, spell.getMaxLevel());
        int minLevel = Math.max(1, 1 + Math.round((maxLevel - 1) * minQuality));
        int cappedMaxLevel = Math.max(minLevel, Math.round(maxLevel * maxQuality));
        int spellLevel = random.nextIntBetweenInclusive(minLevel, cappedMaxLevel);

        ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, spellLevel, scroll);

        int emeraldCost = spell.getRarity(spellLevel).getValue() * 5
                + random.nextIntBetweenInclusive(4, 7)
                + spellLevel;
        return new MerchantOffer(
                new ItemCost(Items.EMERALD, emeraldCost),
                Optional.empty(),
                scroll,
                0,
                1,
                5,
                0.05f
        );
    }

    private static MerchantOffer sellItem(
            ItemStack result,
            int minEmeralds,
            int maxEmeralds,
            int maxUses,
            RandomSource random
    ) {
        if (isDeniedOriginItem(result.getItem())) {
            return null;
        }
        int cost = random.nextIntBetweenInclusive(minEmeralds, maxEmeralds);
        return new MerchantOffer(
                new ItemCost(Items.EMERALD, cost),
                Optional.empty(),
                result.copy(),
                0,
                maxUses,
                5,
                0.05f
        );
    }

    private static MerchantOffer buyItem(
            Item boughtItem,
            int minCount,
            int maxCount,
            int minEmeralds,
            int maxEmeralds,
            int maxUses,
            RandomSource random
    ) {
        int count = random.nextIntBetweenInclusive(minCount, maxCount);
        int emeraldPayout = random.nextIntBetweenInclusive(minEmeralds, maxEmeralds);
        return new MerchantOffer(
                new ItemCost(boughtItem, count),
                Optional.empty(),
                new ItemStack(Items.EMERALD, emeraldPayout),
                0,
                maxUses,
                2,
                0.05f
        );
    }

    /**
     * 防御性校验：结果或代价中出现起源法术卷轴 / 起源物品则剔除。
     */
    private static boolean violatesOriginDenylist(MerchantOffer offer) {
        if (offer == null) {
            return true;
        }
        return containsDeniedContent(offer.getResult())
                || containsDeniedContent(offer.getCostA())
                || containsDeniedContent(offer.getCostB());
    }

    private static boolean containsDeniedContent(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (isDeniedOriginItem(stack.getItem())) {
            return true;
        }
        if (!ISpellContainer.isSpellContainer(stack)) {
            return false;
        }
        ResourceLocation spellId = ISpellContainer.get(stack).getSpellAtIndex(0).getSpell().getSpellResource();
        return ORIGIN_SPELL_DENYLIST.contains(spellId);
    }

    private static boolean isDeniedOriginItem(Item item) {
        return item == ModItems.ORIGIN_CRYSTAL.get()
                || item == ModItems.ORIGIN_GLINTSTONE.get()
                || item == ModItems.ORIGIN_POTION.get()
                || item == ModItems.ORIGIN_CODEX.get()
                || item == ModItems.AZUR_GLINTSTONE_STAFF.get();
    }
}

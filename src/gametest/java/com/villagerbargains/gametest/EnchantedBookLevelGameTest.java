package com.villagerbargains.gametest;

import com.villagerbargains.config.PricingMode;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates the vanilla librarian enchanted-book trade many times with fixed seeds and checks the
 * {@code bookLevels} setting:
 * <ul>
 *   <li>MINIMUM / MAXIMUM always sell the lowest / highest level of the book that was rolled;</li>
 *   <li>NORMAL still varies the level like vanilla;</li>
 *   <li>the enchantment and the random sequence afterwards are identical in every level mode;</li>
 *   <li>the price is the vanilla price for the level that is sold, in every pricing mode;</li>
 *   <li>enchanted books in chest loot keep their vanilla level.</li>
 * </ul>
 */
public final class EnchantedBookLevelGameTest {
    private static final ResourceKey<VillagerTrade> LIBRARIAN_BOOK = ResourceKey.create(
            Registries.VILLAGER_TRADE, Identifier.withDefaultNamespace("librarian/1/emerald_and_book_enchanted_book"));
    private static final int SAMPLES = 300;
    private static final List<PricingMode> MODES = List.of(PricingMode.MINIMUM, PricingMode.NORMAL, PricingMode.MAXIMUM);

    /** What one generated offer looked like, plus the next random value drawn after it. */
    private record Sample(String book, int level, int minLevel, int maxLevel, boolean doubled, int price, long nextRandom) {}

    @GameTest
    public void maximumAlwaysSellsTheHighestLevel(GameTestHelper helper) {
        for (Sample sample : generate(helper, PricingMode.NORMAL, PricingMode.MAXIMUM)) {
            helper.assertValueEqual(sample.level(), sample.maxLevel(), "MAXIMUM level of " + sample.book());
        }
        helper.succeed();
    }

    @GameTest
    public void minimumAlwaysSellsTheLowestLevel(GameTestHelper helper) {
        for (Sample sample : generate(helper, PricingMode.NORMAL, PricingMode.MINIMUM)) {
            helper.assertValueEqual(sample.level(), sample.minLevel(), "MINIMUM level of " + sample.book());
        }
        helper.succeed();
    }

    @GameTest
    public void normalKeepsVanillaLevels(GameTestHelper helper) {
        Map<String, Set<Integer>> levelsByBook = new HashMap<>();
        for (Sample sample : generate(helper, PricingMode.NORMAL, PricingMode.NORMAL)) {
            helper.assertValueInBetween(sample.minLevel(), sample.level(), sample.maxLevel(), "NORMAL level of " + sample.book());
            if (sample.maxLevel() > 1) {
                levelsByBook.computeIfAbsent(sample.book(), book -> new HashSet<>()).add(sample.level());
            }
        }
        boolean varied = levelsByBook.values().stream().anyMatch(levels -> levels.size() > 1);
        helper.assertTrue(varied, "NORMAL should produce varied book levels, got " + levelsByBook);
        helper.succeed();
    }

    @GameTest
    public void onlyTheLevelChangesBetweenLevelModes(GameTestHelper helper) {
        List<Sample> normal = generate(helper, PricingMode.NORMAL, PricingMode.NORMAL);
        for (PricingMode levels : List.of(PricingMode.MINIMUM, PricingMode.MAXIMUM)) {
            List<Sample> pinned = generate(helper, PricingMode.NORMAL, levels);
            for (int i = 0; i < SAMPLES; i++) {
                Sample a = normal.get(i);
                Sample b = pinned.get(i);
                helper.assertValueEqual(b.book(), a.book(), levels + " book levels: enchantment for sample " + i);
                helper.assertValueEqual(b.nextRandom(), a.nextRandom(), levels + " book levels: next random value for sample " + i);
            }
        }
        helper.succeed();
    }

    @GameTest
    public void priceMatchesTheSoldLevelInEveryMode(GameTestHelper helper) {
        for (PricingMode pricing : MODES) {
            for (PricingMode levels : MODES) {
                for (Sample sample : generate(helper, pricing, levels)) {
                    int factor = sample.doubled() ? 2 : 1;
                    int cheapest = clamp((2 + 3 * sample.level()) * factor);
                    int dearest = clamp((6 + 13 * sample.level()) * factor);
                    String what = pricing + " price, " + levels + " book levels: " + sample.book() + " " + sample.level();
                    switch (pricing) {
                        case MINIMUM -> helper.assertValueEqual(sample.price(), cheapest, what);
                        case MAXIMUM -> helper.assertValueEqual(sample.price(), dearest, what);
                        default -> helper.assertValueInBetween(cheapest, sample.price(), dearest, what);
                    }
                }
            }
        }
        helper.succeed();
    }

    /**
     * Rolls the vanilla simple dungeon chest (which contains randomly enchanted books) with the
     * same seeds in NORMAL and MAXIMUM: the loot must be identical, because only trades are pinned.
     */
    @GameTest
    public void chestLootKeepsVanillaLevels(GameTestHelper helper) {
        List<List<ItemStack>> normal = rollDungeonChest(helper, PricingMode.NORMAL);
        List<List<ItemStack>> maximum = rollDungeonChest(helper, PricingMode.MAXIMUM);
        boolean belowMaximum = false;
        for (int i = 0; i < SAMPLES; i++) {
            List<ItemStack> a = normal.get(i);
            List<ItemStack> b = maximum.get(i);
            helper.assertValueEqual(b.size(), a.size(), "chest loot size for sample " + i);
            for (int j = 0; j < a.size(); j++) {
                helper.assertTrue(ItemStack.matches(a.get(j), b.get(j)),
                        "chest loot for sample " + i + " differs: " + a.get(j) + " vs " + b.get(j));
                ItemEnchantments stored = b.get(j).get(DataComponents.STORED_ENCHANTMENTS);
                if (stored != null) {
                    for (Object2IntMap.Entry<Holder<Enchantment>> entry : stored.entrySet()) {
                        belowMaximum |= entry.getIntValue() < entry.getKey().value().getMaxLevel();
                    }
                }
            }
        }
        // Without this the comparison above could pass by accident if every roll were already the maximum.
        helper.assertTrue(belowMaximum, "MAXIMUM book levels must not raise chest loot levels, but every book was at its maximum");
        helper.succeed();
    }

    private static List<Sample> generate(GameTestHelper helper, PricingMode pricing, PricingMode levels) {
        Holder<VillagerTrade> trade = librarianBook(helper);
        Villager trader = TradeTestSupport.spawnTrader(helper);
        return TradeTestSupport.withBookLevelMode(levels, () -> TradeTestSupport.withMode(pricing, () -> {
            List<Sample> samples = new ArrayList<>();
            for (int i = 0; i < SAMPLES; i++) {
                long seed = TradeTestSupport.seed(i);
                samples.add(sample(helper, trade, TradeTestSupport.tradeContext(helper, trader, seed), seed));
            }
            return samples;
        }));
    }

    private static List<List<ItemStack>> rollDungeonChest(GameTestHelper helper, PricingMode levels) {
        LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SIMPLE_DUNGEON);
        LootParams params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.CHEST);
        return TradeTestSupport.withBookLevelMode(levels, () -> {
            List<List<ItemStack>> rolls = new ArrayList<>();
            for (int i = 0; i < SAMPLES; i++) {
                rolls.add(List.copyOf(table.getRandomItems(params, TradeTestSupport.seed(i))));
            }
            return rolls;
        });
    }

    private static Holder<VillagerTrade> librarianBook(GameTestHelper helper) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE).getOrThrow(LIBRARIAN_BOOK);
    }

    private static Sample sample(GameTestHelper helper, Holder<VillagerTrade> trade, LootContext context, long seed) {
        MerchantOffer offer = trade.value().getOffer(context);
        if (offer == null) {
            throw helper.assertionException("No offer generated for seed %s", seed);
        }
        return describe(offer, context.getRandom().nextLong());
    }

    private static Sample describe(MerchantOffer offer, long nextRandom) {
        ItemStack book = offer.getResult();
        ItemEnchantments stored = book.get(DataComponents.STORED_ENCHANTMENTS);
        Object2IntMap.Entry<Holder<Enchantment>> entry = stored.entrySet().iterator().next();
        Holder<Enchantment> enchantment = entry.getKey();
        return new Sample(
                enchantment.getRegisteredName(),
                entry.getIntValue(),
                enchantment.value().getMinLevel(),
                enchantment.value().getMaxLevel(),
                enchantment.is(EnchantmentTags.DOUBLE_TRADE_PRICE),
                offer.getBaseCostA().getCount(),
                nextRandom);
    }

    /** Vanilla clamps every cost to the emerald stack size. */
    private static int clamp(int price) {
        return Math.min(price, 64);
    }
}

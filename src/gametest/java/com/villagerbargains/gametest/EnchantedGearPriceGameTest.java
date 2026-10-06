package com.villagerbargains.gametest;

import com.villagerbargains.config.PricingMode;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Generates every vanilla enchanted-gear trade ({@code price = base + levels}, where the
 * {@code enchant_with_levels} roll {@code levels} is also the enchanting power) many times with
 * fixed seeds and checks:
 * <ul>
 *   <li>MINIMUM always gives the cheapest price NORMAL produced, MAXIMUM the most expensive one;</li>
 *   <li>NORMAL still varies like vanilla;</li>
 *   <li>the item, its enchantments and the random sequence afterwards are identical in every mode.</li>
 * </ul>
 */
public final class EnchantedGearPriceGameTest {
    private static final List<String> GEAR_TRADES = List.of(
            "armorer/4/emerald_enchanted_diamond_boots",
            "armorer/4/emerald_enchanted_diamond_leggings",
            "armorer/5/emerald_enchanted_diamond_chestplate",
            "armorer/5/emerald_enchanted_diamond_helmet",
            "fisherman/3/emerald_enchanted_fishing_rod",
            "fletcher/4/emerald_enchanted_bow",
            "fletcher/5/emerald_enchanted_crossbow",
            "toolsmith/3/emerald_enchanted_iron_axe",
            "toolsmith/3/emerald_enchanted_iron_pickaxe",
            "toolsmith/3/emerald_enchanted_iron_shovel",
            "toolsmith/4/emerald_enchanted_diamond_axe",
            "toolsmith/4/emerald_enchanted_diamond_shovel",
            "toolsmith/5/emerald_enchanted_diamond_pickaxe",
            "wandering_trader/emerald_enchanted_iron_pickaxe",
            "weaponsmith/1/emerald_enchanted_iron_sword",
            "weaponsmith/4/emerald_enchanted_diamond_axe",
            "weaponsmith/5/emerald_enchanted_diamond_sword");
    private static final int SAMPLES = 200;

    /** One generated offer ({@code null} item when the trade gave none), plus the next random value. */
    private record Sample(String item, int price, long nextRandom) {
        boolean hasOffer() {
            return item != null;
        }
    }

    @GameTest
    public void minimumIsAlwaysTheCheapestVanillaPrice(GameTestHelper helper) {
        assertPinned(helper, PricingMode.MINIMUM);
        helper.succeed();
    }

    @GameTest
    public void maximumIsAlwaysTheMostExpensiveVanillaPrice(GameTestHelper helper) {
        assertPinned(helper, PricingMode.MAXIMUM);
        helper.succeed();
    }

    @GameTest
    public void onlyThePriceChangesBetweenModes(GameTestHelper helper) {
        Villager trader = TradeTestSupport.spawnTrader(helper);
        for (String trade : GEAR_TRADES) {
            List<Sample> normal = generate(helper, trader, trade, PricingMode.NORMAL);
            for (PricingMode mode : List.of(PricingMode.MINIMUM, PricingMode.MAXIMUM)) {
                List<Sample> pinned = generate(helper, trader, trade, mode);
                for (int i = 0; i < SAMPLES; i++) {
                    Sample a = normal.get(i);
                    Sample b = pinned.get(i);
                    String what = mode + " " + trade + " sample " + i;
                    helper.assertValueEqual(b.hasOffer(), a.hasOffer(), what + " has an offer");
                    if (a.hasOffer()) {
                        helper.assertValueEqual(b.item(), a.item(), what + " item and enchantments");
                    }
                    helper.assertValueEqual(b.nextRandom(), a.nextRandom(), what + " next random value");
                }
            }
        }
        helper.succeed();
    }

    /** Every {@code mode} price of a trade is the same and equals NORMAL's extreme for that trade. */
    private static void assertPinned(GameTestHelper helper, PricingMode mode) {
        Villager trader = TradeTestSupport.spawnTrader(helper);
        for (String trade : GEAR_TRADES) {
            TreeSet<Integer> normalPrices = prices(generate(helper, trader, trade, PricingMode.NORMAL));
            TreeSet<Integer> pinnedPrices = prices(generate(helper, trader, trade, mode));
            helper.assertTrue(normalPrices.size() > 5, "NORMAL " + trade + " should vary like vanilla, got " + normalPrices);
            int expected = mode == PricingMode.MINIMUM ? normalPrices.first() : normalPrices.last();
            helper.assertValueEqual(pinnedPrices, Set.of(expected), mode + " prices of " + trade);
        }
    }

    private static TreeSet<Integer> prices(List<Sample> samples) {
        TreeSet<Integer> prices = new TreeSet<>();
        for (Sample sample : samples) {
            if (sample.hasOffer()) {
                prices.add(sample.price());
            }
        }
        return prices;
    }

    private static List<Sample> generate(GameTestHelper helper, Villager trader, String tradeId, PricingMode mode) {
        Holder<VillagerTrade> trade = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.VILLAGER_TRADE)
                .getOrThrow(ResourceKey.create(Registries.VILLAGER_TRADE, Identifier.withDefaultNamespace(tradeId)));
        return TradeTestSupport.withMode(mode, () -> {
            List<Sample> samples = new ArrayList<>();
            for (int i = 0; i < SAMPLES; i++) {
                LootContext context = TradeTestSupport.tradeContext(helper, trader, TradeTestSupport.seed(i));
                MerchantOffer offer = trade.value().getOffer(context);
                long nextRandom = context.getRandom().nextLong();
                samples.add(offer == null
                        ? new Sample(null, 0, nextRandom)
                        : new Sample(describe(offer.getResult()), offer.getBaseCostA().getCount(), nextRandom));
            }
            return samples;
        });
    }

    /** The item id plus its enchantments, in order, e.g. {@code minecraft:bow[minecraft:power 3, ...]}. */
    private static String describe(ItemStack item) {
        ItemEnchantments enchantments = Objects.requireNonNullElse(
                item.get(DataComponents.ENCHANTMENTS), ItemEnchantments.EMPTY);
        List<String> parts = new ArrayList<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            parts.add(entry.getKey().getRegisteredName() + " " + entry.getIntValue());
        }
        return BuiltInRegistries.ITEM.getKey(item.getItem()) + parts;
    }
}

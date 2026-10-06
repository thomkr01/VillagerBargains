package com.villagerbargains.gametest;

import com.villagerbargains.config.PricingMode;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Regression sweep over <b>every</b> villager trade in the registry (all professions and the
 * wandering trader), for every villager type and {@value #SEEDS} fixed seeds.
 *
 * <p>Each offer is generated in {@code NORMAL}, {@code MINIMUM} and {@code MAXIMUM} from the same
 * seed, and the test proves that:
 * <ul>
 *   <li>a pricing mode never changes <i>what</i> is offered: whether there is an offer at all,
 *       the result stack (item, count, enchantments, all components), the second cost, max uses,
 *       XP, price multiplier and the next random value drawn afterwards are identical;</li>
 *   <li>the base price is ordered {@code MINIMUM <= NORMAL <= MAXIMUM};</li>
 *   <li>trades whose result is not enchanted (no enchantments, no stored enchantments) have a
 *       fixed vanilla price, so it is exactly the same in every mode.</li>
 * </ul>
 * It deliberately does not assert exact prices for enchanted gear or demand; other tests own those.
 *
 * <p><b>Skipped:</b> cartographer exploration-map trades (trade id path contains {@code "_map"}).
 * Generating them runs a structure search, which is far too slow for thousands of samples, and
 * their price is fixed anyway.
 */
public final class AllTradesSweepGameTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagerbargains");
    private static final int SEEDS = 20;
    private static final int MINIMUM_TRADES_CHECKED = 300;

    /** What one generated offer looked like, plus the next random value drawn after it. */
    private record Generated(MerchantOffer offer, long nextRandom) {}

    @GameTest
    public void everyTradeKeepsItsOfferAndOrdersPricesInEveryMode(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        List<Holder.Reference<VillagerTrade>> trades = registries.lookupOrThrow(Registries.VILLAGER_TRADE).listElements().toList();
        List<Holder.Reference<VillagerType>> villagerTypes = registries.lookupOrThrow(Registries.VILLAGER_TYPE).listElements().toList();
        Villager trader = TradeTestSupport.spawnTrader(helper);

        int tradesChecked = 0;
        int tradesSkipped = 0;
        int offersCompared = 0;
        for (Holder.Reference<VillagerTrade> trade : trades) {
            String tradeId = trade.getRegisteredName();
            if (isExplorationMap(tradeId)) {
                tradesSkipped++;
                continue;
            }
            tradesChecked++;
            List<Generated> normal = generateAll(helper, trader, trade, villagerTypes, PricingMode.NORMAL);
            List<Generated> minimum = generateAll(helper, trader, trade, villagerTypes, PricingMode.MINIMUM);
            List<Generated> maximum = generateAll(helper, trader, trade, villagerTypes, PricingMode.MAXIMUM);
            for (int sample = 0; sample < normal.size(); sample++) {
                String where = tradeId + " (villager type " + villagerTypes.get(sample / SEEDS).getRegisteredName()
                        + ", seed " + TradeTestSupport.seed(sample % SEEDS) + ")";
                assertSameOffer(helper, normal.get(sample), minimum.get(sample), "MINIMUM " + where);
                assertSameOffer(helper, normal.get(sample), maximum.get(sample), "MAXIMUM " + where);
                if (normal.get(sample).offer() != null) {
                    assertPrices(helper, normal.get(sample).offer(), minimum.get(sample).offer(), maximum.get(sample).offer(), where);
                    offersCompared++;
                }
            }
        }

        LOGGER.info("All-trades sweep: {} trades checked ({} exploration-map trades skipped) x {} villager types x {} seeds, "
                + "{} offers compared across NORMAL/MINIMUM/MAXIMUM",
                tradesChecked, tradesSkipped, villagerTypes.size(), SEEDS, offersCompared);
        helper.assertTrue(tradesChecked > MINIMUM_TRADES_CHECKED,
                "Expected more than " + MINIMUM_TRADES_CHECKED + " trades in the registry, checked " + tradesChecked);
        helper.assertTrue(offersCompared > 0, "No trade produced an offer");
        helper.succeed();
    }

    /** Exploration maps search for a structure while generating; see the class Javadoc. */
    private static boolean isExplorationMap(String tradeId) {
        return tradeId.substring(tradeId.indexOf(':') + 1).contains("_map");
    }

    /**
     * Generates {@code trade} for every villager type and seed in {@code mode}, ordered by type, then seed.
     * The mode is switched once per call because switching it saves the config file.
     */
    private static List<Generated> generateAll(GameTestHelper helper, Villager trader, Holder<VillagerTrade> trade,
                                               List<? extends Holder<VillagerType>> villagerTypes, PricingMode mode) {
        return TradeTestSupport.withMode(mode, () -> {
            List<Generated> generated = new ArrayList<>();
            for (Holder<VillagerType> villagerType : villagerTypes) {
                trader.setVillagerData(trader.getVillagerData().withType(villagerType));
                for (int i = 0; i < SEEDS; i++) {
                    LootContext context = TradeTestSupport.tradeContext(helper, trader, TradeTestSupport.seed(i));
                    MerchantOffer offer = trade.value().getOffer(context);
                    generated.add(new Generated(offer, context.getRandom().nextLong()));
                }
            }
            return generated;
        });
    }

    /** Everything except the base price must be identical to the NORMAL offer. */
    private static void assertSameOffer(GameTestHelper helper, Generated normal, Generated pinned, String where) {
        helper.assertValueEqual(pinned.offer() == null, normal.offer() == null, "offer missing/present for " + where);
        helper.assertValueEqual(pinned.nextRandom(), normal.nextRandom(), "next random value for " + where);
        if (normal.offer() == null) {
            return;
        }
        MerchantOffer a = normal.offer();
        MerchantOffer b = pinned.offer();
        helper.assertTrue(ItemStack.matches(b.getResult(), a.getResult()),
                "result differs for " + where + ": " + b.getResult() + " vs NORMAL " + a.getResult());
        helper.assertTrue(ItemStack.isSameItemSameComponents(b.getBaseCostA(), a.getBaseCostA()),
                "first cost item differs for " + where + ": " + b.getBaseCostA() + " vs NORMAL " + a.getBaseCostA());
        helper.assertTrue(ItemStack.matches(b.getCostB(), a.getCostB()),
                "second cost differs for " + where + ": " + b.getCostB() + " vs NORMAL " + a.getCostB());
        helper.assertValueEqual(b.getMaxUses(), a.getMaxUses(), "max uses for " + where);
        helper.assertValueEqual(b.getXp(), a.getXp(), "xp for " + where);
        helper.assertValueEqual(b.getPriceMultiplier(), a.getPriceMultiplier(), "price multiplier for " + where);
    }

    /** {@code MINIMUM <= NORMAL <= MAXIMUM}, and all equal when the result carries no enchantment. */
    private static void assertPrices(GameTestHelper helper, MerchantOffer normal, MerchantOffer minimum, MerchantOffer maximum, String where) {
        int normalPrice = normal.getBaseCostA().getCount();
        int minimumPrice = minimum.getBaseCostA().getCount();
        int maximumPrice = maximum.getBaseCostA().getCount();
        helper.assertValueInBetween(minimumPrice, normalPrice, maximumPrice,
                "NORMAL price between MINIMUM and MAXIMUM for " + where);
        if (!isEnchanted(normal.getResult())) {
            helper.assertValueEqual(minimumPrice, normalPrice, "MINIMUM price of unenchanted " + where);
            helper.assertValueEqual(maximumPrice, normalPrice, "MAXIMUM price of unenchanted " + where);
        }
    }

    private static boolean isEnchanted(ItemStack stack) {
        return !stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()
                || !stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
    }
}

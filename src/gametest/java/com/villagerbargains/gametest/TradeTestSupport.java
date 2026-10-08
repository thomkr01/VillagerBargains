package com.villagerbargains.gametest;

import com.villagerbargains.config.PricingMode;
import com.villagerbargains.config.VillagerBargainsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.Optional;
import java.util.function.Supplier;

/** Helpers shared by the game tests: seeded trade contexts and temporary pricing / book level modes. */
final class TradeTestSupport {
    private TradeTestSupport() {}

    /** System property that shifts every sample seed, so CI can repeat the tests on new seeds. */
    static final String SEED_OFFSET_PROPERTY = "villagerbargains.test.seedOffset";

    /**
     * Added to every seed. Read once from {@value #SEED_OFFSET_PROPERTY} (default 0, which keeps
     * the original seeds 1, 2, 3, ...). CI runs the game tests several times with different
     * offsets (e.g. {@code -Dvillagerbargains.test.seedOffset=100000}) so each run checks a
     * fresh set of trades while every run stays reproducible.
     */
    private static final long SEED_OFFSET = readSeedOffset();

    private static long readSeedOffset() {
        String value = System.getProperty(SEED_OFFSET_PROPERTY);
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            // Fail loudly: silently falling back to 0 would rerun the same seeds unnoticed.
            throw new IllegalArgumentException(
                    "-D" + SEED_OFFSET_PROPERTY + " must be a whole number, got: " + value, e);
        }
    }

    /**
     * The seed for sample {@code index}: {@code index + 1} plus the seed offset (see
     * {@link #SEED_OFFSET}). Never 0: {@code withOptionalRandomSeed(0)} means "no seed" and
     * would fall back to the world's random source, so a (negative) offset that would land
     * on 0 gives {@link Long#MIN_VALUE} instead.
     */
    static long seed(int index) {
        long seed = SEED_OFFSET + index + 1L;
        return seed != 0L ? seed : Long.MIN_VALUE;
    }

    /** A villager to act as the trader ({@code this} entity) for trade generation. */
    static Villager spawnTrader(GameTestHelper helper) {
        return helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 1, 1));
    }

    /**
     * Builds the same loot context {@code AbstractVillager#addOffersFromTradeSet} uses,
     * but with a fixed seed so runs can be compared.
     */
    static LootContext tradeContext(GameTestHelper helper, Villager trader, long seed) {
        LootParams params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, trader.position())
                .withParameter(LootContextParams.THIS_ENTITY, trader)
                .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
                .create(LootContextParamSets.VILLAGER_TRADE);
        return new LootContext.Builder(params).withOptionalRandomSeed(seed).create(Optional.empty());
    }

    /** Runs {@code action} with {@code mode} active, then restores the previous mode. */
    static <T> T withMode(PricingMode mode, Supplier<T> action) {
        PricingMode previous = VillagerBargainsConfig.pricingMode();
        VillagerBargainsConfig.setPricingMode(mode);
        try {
            return action.get();
        } finally {
            VillagerBargainsConfig.setPricingMode(previous);
        }
    }

    /** Runs {@code action} with book level mode {@code mode} active, then restores the previous one. */
    static <T> T withBookLevelMode(PricingMode mode, Supplier<T> action) {
        PricingMode previous = VillagerBargainsConfig.bookLevelMode();
        VillagerBargainsConfig.setBookLevelMode(mode);
        try {
            return action.get();
        } finally {
            VillagerBargainsConfig.setBookLevelMode(previous);
        }
    }

    /** Runs {@code action} with gear strength mode {@code mode} active, then restores the previous one. */
    static <T> T withGearStrengthMode(PricingMode mode, Supplier<T> action) {
        PricingMode previous = VillagerBargainsConfig.gearStrengthMode();
        VillagerBargainsConfig.setGearStrengthMode(mode);
        try {
            return action.get();
        } finally {
            VillagerBargainsConfig.setGearStrengthMode(previous);
        }
    }
}

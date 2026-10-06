package com.villagerbargains.config;

/**
 * How a random vanilla price roll is resolved.
 *
 * <p>Each mode only decides which value inside the vanilla range is used.
 * It never widens the range, so every price stays one vanilla could produce.
 */
public enum PricingMode {
    /** Always the cheapest price vanilla can roll. */
    MINIMUM,
    /** Untouched vanilla behaviour: the random roll is kept. */
    NORMAL,
    /** Always the most expensive price vanilla can roll. */
    MAXIMUM;

    /**
     * Picks the value for an integer roll.
     *
     * @param rolled  the value vanilla actually rolled
     * @param lowest  the lowest value vanilla could have rolled (inclusive)
     * @param highest the highest value vanilla could have rolled (inclusive)
     */
    public int pick(int rolled, int lowest, int highest) {
        if (highest <= lowest) {
            return rolled; // Degenerate range: vanilla had no choice to make.
        }
        return switch (this) {
            case MINIMUM -> lowest;
            case NORMAL -> rolled;
            case MAXIMUM -> highest;
        };
    }

    /**
     * Picks the value for a float roll in {@code [lowest, highest)}.
     * The upper bound is exclusive in vanilla, so MAXIMUM uses the largest float below it.
     */
    public float pick(float rolled, float lowest, float highestExclusive) {
        if (highestExclusive <= lowest) {
            return rolled; // Degenerate range: vanilla had no choice to make.
        }
        return switch (this) {
            case MINIMUM -> lowest;
            case NORMAL -> rolled;
            case MAXIMUM -> Math.nextDown(highestExclusive);
        };
    }
}

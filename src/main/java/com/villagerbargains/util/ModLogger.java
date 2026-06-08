package com.villagerbargains.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared logger — use ModLogger.get() from any class in the mod. */
public final class ModLogger {
    private ModLogger() {}
    private static final Logger LOGGER = LoggerFactory.getLogger("VillagerBargains");
    public static Logger get() { return LOGGER; }
}

package com.bargainvillage.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModLogger {
    private ModLogger() {}
    private static final Logger LOGGER = LoggerFactory.getLogger("BargainVillage");
    public static Logger get() {
        return LOGGER;
    }
}

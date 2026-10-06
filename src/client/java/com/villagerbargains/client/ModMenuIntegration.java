package com.villagerbargains.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu entrypoint ({@code "modmenu"} in {@code fabric.mod.json}).
 * Only loaded when Mod Menu is installed, so Mod Menu stays an optional dependency.
 */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return VillagerBargainsConfigScreen::new;
    }
}

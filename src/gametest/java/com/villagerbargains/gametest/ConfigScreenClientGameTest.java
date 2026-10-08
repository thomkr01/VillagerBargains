package com.villagerbargains.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.villagerbargains.client.VillagerBargainsConfigScreen;
import com.villagerbargains.config.PricingMode;
import com.villagerbargains.config.VillagerBargainsConfig;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Opens the Mod Menu settings screen in a real game client and uses it like a player would:
 * every text is translated, both buttons cycle MINIMUM → NORMAL → MAXIMUM and save to the config
 * file, and Done and Esc both return to the previous screen. Screenshots of the screen are written
 * to the client's {@code screenshots} folder, which CI uploads.
 */
public final class ConfigScreenClientGameTest implements FabricClientGameTest {
    private static final String[] SETTINGS = {"pricing", "bookLevels", "gearStrength"};

    @Override
    public void runTest(ClientGameTestContext context) {
        PricingMode previousPricing = VillagerBargainsConfig.pricingMode();
        PricingMode previousBookLevels = VillagerBargainsConfig.bookLevelMode();
        PricingMode previousGearStrength = VillagerBargainsConfig.gearStrengthMode();
        try {
            VillagerBargainsConfig.setPricingMode(PricingMode.MINIMUM);
            VillagerBargainsConfig.setBookLevelMode(PricingMode.NORMAL);
            VillagerBargainsConfig.setGearStrengthMode(PricingMode.NORMAL);

            assertTranslated();

            Screen screen = openScreen(context);
            context.takeScreenshot("villagerbargains-config-open");
            assertButtonsShow(context, screen, "Minimum", "Normal");

            context.clickScreenButton("villagerbargains.config.pricing");
            context.waitTick();
            assertModes(PricingMode.NORMAL, PricingMode.NORMAL);
            context.clickScreenButton("villagerbargains.config.pricing");
            context.waitTick();
            assertModes(PricingMode.MAXIMUM, PricingMode.NORMAL);

            context.clickScreenButton("villagerbargains.config.bookLevels");
            context.waitTick();
            assertModes(PricingMode.MAXIMUM, PricingMode.MAXIMUM);
            context.clickScreenButton("villagerbargains.config.bookLevels");
            context.waitTick();
            assertModes(PricingMode.MAXIMUM, PricingMode.MINIMUM);

            context.clickScreenButton("villagerbargains.config.gearStrength");
            context.waitTick();
            check(VillagerBargainsConfig.gearStrengthMode() == PricingMode.MAXIMUM,
                    "Gear strength should be MAXIMUM, is " + VillagerBargainsConfig.gearStrengthMode());

            assertButtonsShow(context, screen, "Maximum", "Minimum");
            context.takeScreenshot("villagerbargains-config-changed");
            assertSavedFile(PricingMode.MAXIMUM, PricingMode.MINIMUM);

            context.clickScreenButton("gui.done");
            context.waitForScreen(TitleScreen.class);

            openScreen(context);
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitForScreen(TitleScreen.class);
        } finally {
            VillagerBargainsConfig.setPricingMode(previousPricing);
            VillagerBargainsConfig.setBookLevelMode(previousBookLevels);
            VillagerBargainsConfig.setGearStrengthMode(previousGearStrength);
        }
    }

    private static Screen openScreen(ClientGameTestContext context) {
        context.waitForScreen(TitleScreen.class);
        Screen screen = context.computeOnClient(client -> new VillagerBargainsConfigScreen(new TitleScreen()));
        context.setScreen(() -> screen);
        context.waitForScreen(VillagerBargainsConfigScreen.class);
        context.waitTick();
        return screen;
    }

    /** Every text the screen shows, including each tooltip, has an English translation. */
    private static void assertTranslated() {
        List<String> keys = new ArrayList<>(List.of("villagerbargains.config.title", "villagerbargains.config.note"));
        for (String setting : SETTINGS) {
            keys.add("villagerbargains.config." + setting);
            for (PricingMode mode : PricingMode.values()) {
                String key = "villagerbargains." + setting + "." + mode.name().toLowerCase(Locale.ROOT);
                keys.add(key);
                keys.add(key + ".description");
            }
        }
        for (String key : keys) {
            String text = Component.translatable(key).getString();
            check(!text.equals(key) && !text.isBlank(), "No translation for " + key);
        }
    }

    /** The buttons are on screen, labelled with their setting and current value. */
    private static void assertButtonsShow(ClientGameTestContext context, Screen screen, String pricing, String bookLevels) {
        List<String> labels = context.computeOnClient(client -> {
            List<String> found = new ArrayList<>();
            for (var child : screen.children()) {
                if (child instanceof AbstractWidget widget) {
                    found.add(widget.getMessage().getString());
                }
            }
            return found;
        });
        check(labels.contains("Pricing: " + pricing), "Expected button 'Pricing: " + pricing + "', got " + labels);
        check(labels.contains("Book levels: " + bookLevels), "Expected button 'Book levels: " + bookLevels + "', got " + labels);
        check(labels.stream().anyMatch(label -> label.startsWith("Gear strength: ")), "Expected a 'Gear strength' button, got " + labels);
        for (String label : labels) {
            check(!label.contains("villagerbargains."), "Untranslated text on screen: " + label);
        }
    }

    private static void assertModes(PricingMode pricing, PricingMode bookLevels) {
        check(VillagerBargainsConfig.pricingMode() == pricing,
                "Pricing should be " + pricing + ", is " + VillagerBargainsConfig.pricingMode());
        check(VillagerBargainsConfig.bookLevelMode() == bookLevels,
                "Book levels should be " + bookLevels + ", is " + VillagerBargainsConfig.bookLevelMode());
    }

    /** The choices made on the screen are in {@code config/villagerbargains.json}. */
    private static void assertSavedFile(PricingMode pricing, PricingMode bookLevels) {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("villagerbargains.json");
        String json;
        try {
            json = Files.readString(file);
        } catch (IOException e) {
            throw new AssertionError("Could not read " + file, e);
        }
        check(json.contains("\"pricing\": \"" + pricing.name() + "\""), "Config file has wrong pricing: " + json);
        check(json.contains("\"bookLevels\": \"" + bookLevels.name() + "\""), "Config file has wrong book levels: " + json);
        check(json.contains("\"gearStrength\": \"MAXIMUM\""), "Config file has wrong gear strength: " + json);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

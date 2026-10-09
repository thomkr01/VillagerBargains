package com.villagerbargains.gametest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.villagerbargains.config.PricingMode;
import com.villagerbargains.config.VillagerBargainsConfig;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Runs {@code /bargain} as the server console and checks that every subcommand changes its
 * option and that {@code reload} re-reads the file. Each test restores the config before it
 * returns (all within one tick), so trade tests running alongside never see the changes.
 */
public final class BargainCommandsGameTest {
    @GameTest
    public void everySettingCanBeChanged(GameTestHelper helper) {
        withConfigRestored(() -> {
            for (PricingMode mode : PricingMode.values()) {
                String arg = mode.name().toLowerCase(java.util.Locale.ROOT);
                run(helper, "bargain price " + arg);
                run(helper, "bargain books " + arg);
                run(helper, "bargain gear " + arg);
                helper.assertValueEqual(VillagerBargainsConfig.pricingMode(), mode, "pricing after /bargain price " + arg);
                helper.assertValueEqual(VillagerBargainsConfig.bookLevelMode(), mode, "bookLevels after /bargain books " + arg);
                helper.assertValueEqual(VillagerBargainsConfig.gearStrengthMode(), mode, "gearStrength after /bargain gear " + arg);
            }
            // The show-only forms must run too.
            run(helper, "bargain");
            run(helper, "bargain price");
            run(helper, "bargain books");
            run(helper, "bargain gear");
        });
        helper.succeed();
    }

    @GameTest
    public void reloadReadsTheFile(GameTestHelper helper) {
        withConfigRestored(() -> {
            run(helper, "bargain price minimum");
            try {
                Files.writeString(FabricLoader.getInstance().getConfigDir().resolve("villagerbargains.json"),
                        "{\"pricing\": \"MAXIMUM\", \"bookLevels\": \"MAXIMUM\", \"gearStrength\": \"MINIMUM\"}");
            } catch (IOException e) {
                throw helper.assertionException("Could not write the config file: %s", e.getMessage());
            }
            run(helper, "bargain reload");
            helper.assertValueEqual(VillagerBargainsConfig.pricingMode(), PricingMode.MAXIMUM, "pricing after reload");
            helper.assertValueEqual(VillagerBargainsConfig.bookLevelMode(), PricingMode.MAXIMUM, "bookLevels after reload");
            helper.assertValueEqual(VillagerBargainsConfig.gearStrengthMode(), PricingMode.MINIMUM, "gearStrength after reload");
        });
        helper.succeed();
    }

    private static void run(GameTestHelper helper, String command) {
        CommandSourceStack console = helper.getLevel().getServer().createCommandSourceStack();
        CommandDispatcher<CommandSourceStack> dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        try {
            dispatcher.execute(command, console);
        } catch (CommandSyntaxException e) {
            throw helper.assertionException("/%s failed: %s", command, e.getMessage());
        }
    }

    private static void withConfigRestored(Runnable action) {
        PricingMode pricing = VillagerBargainsConfig.pricingMode();
        PricingMode books = VillagerBargainsConfig.bookLevelMode();
        PricingMode gear = VillagerBargainsConfig.gearStrengthMode();
        try {
            action.run();
        } finally {
            VillagerBargainsConfig.setPricingMode(pricing);
            VillagerBargainsConfig.setBookLevelMode(books);
            VillagerBargainsConfig.setGearStrengthMode(gear);
        }
    }
}

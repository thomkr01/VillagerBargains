package com.villagerbargains.gametest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.villagerbargains.VillagerBargains;
import com.villagerbargains.config.PricingMode;
import com.villagerbargains.config.VillagerBargainsConfig;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.permissions.PermissionSet;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Runs {@code /bargain} and checks that every subcommand changes only its own option, that
 * {@code reload} re-reads the file, and who may use the commands: operators yes, players without
 * operator no, and with a permissions mod (see {@link FakeLuckPerms}) each node decides on its own.
 * Each test restores the config before it returns (all within one tick), so trade tests running
 * alongside never see the changes.
 */
public final class BargainCommandsGameTest {
    private static final List<String> SUBCOMMANDS = List.of("reload", "price", "books", "gear");
    /** Every command form; none of them may run for a player without operator. */
    private static final List<String> EVERY_COMMAND = List.of(
            "bargain", "bargain reload",
            "bargain price", "bargain price minimum", "bargain price normal", "bargain price maximum",
            "bargain books", "bargain books minimum", "bargain books normal", "bargain books maximum",
            "bargain gear", "bargain gear minimum", "bargain gear normal", "bargain gear maximum");

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

    @GameTest
    public void eachSettingIsSeparate(GameTestHelper helper) {
        withConfigRestored(() -> {
            run(helper, "bargain price maximum");
            run(helper, "bargain books minimum");
            run(helper, "bargain gear normal");
            assertModes(helper, PricingMode.MAXIMUM, PricingMode.MINIMUM, PricingMode.NORMAL, "after price max, books min, gear normal");

            run(helper, "bargain books maximum");
            assertModes(helper, PricingMode.MAXIMUM, PricingMode.MAXIMUM, PricingMode.NORMAL, "after only /bargain books maximum");
            run(helper, "bargain gear minimum");
            assertModes(helper, PricingMode.MAXIMUM, PricingMode.MAXIMUM, PricingMode.MINIMUM, "after only /bargain gear minimum");
            run(helper, "bargain price normal");
            assertModes(helper, PricingMode.NORMAL, PricingMode.MAXIMUM, PricingMode.MINIMUM, "after only /bargain price normal");
        });
        helper.succeed();
    }

    @GameTest
    public void playersWithoutOperatorAreRefused(GameTestHelper helper) {
        CommandSourceStack op = console(helper);
        CommandSourceStack player = op.withPermission(PermissionSet.NO_PERMISSIONS);
        CommandNode<CommandSourceStack> bargain = dispatcher(helper).getRoot().getChild("bargain");
        helper.assertTrue(bargain != null, "/bargain is registered");

        helper.assertTrue(bargain.canUse(op), "an operator can see /bargain");
        helper.assertTrue(!bargain.canUse(player), "a player without operator cannot see /bargain");
        for (String name : SUBCOMMANDS) {
            helper.assertTrue(bargain.getChild(name).canUse(op), "an operator can use /bargain " + name);
            helper.assertTrue(!bargain.getChild(name).canUse(player), "a player without operator cannot use /bargain " + name);
        }
        withConfigRestored(() -> {
            VillagerBargainsConfig.setPricingMode(PricingMode.NORMAL);
            VillagerBargainsConfig.setBookLevelMode(PricingMode.NORMAL);
            VillagerBargainsConfig.setGearStrengthMode(PricingMode.NORMAL);
            for (String command : EVERY_COMMAND) {
                assertRefused(helper, player, command);
            }
            assertModes(helper, PricingMode.NORMAL, PricingMode.NORMAL, PricingMode.NORMAL, "after a player without operator tried every command");
        });
        helper.succeed();
    }

    /**
     * With a permissions mod installed, each node is granted or denied on its own, and showing
     * {@code /bargain} to a player checks every node (that is how LuckPerms learns them).
     * Runs only where fabric-permissions-api is installed ({@code ./gradlew build}).
     */
    @GameTest
    public void permissionNodesDecideWithLuckPerms(GameTestHelper helper) {
        if (!FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0")) {
            VillagerBargains.LOGGER.info("fabric-permissions-api not installed; skipping the LuckPerms node test");
            helper.succeed();
            return;
        }
        CommandSourceStack op = console(helper);
        CommandSourceStack player = op.withPermission(PermissionSet.NO_PERMISSIONS);
        CommandNode<CommandSourceStack> bargain = dispatcher(helper).getRoot().getChild("bargain");
        try {
            FakeLuckPerms.reset();
            helper.assertTrue(!bargain.canUse(player), "/bargain hidden from a player with no nodes");
            for (String name : SUBCOMMANDS) {
                helper.assertTrue(FakeLuckPerms.CHECKED.contains("villagerbargains.command." + name),
                        "villagerbargains.command." + name + " checked when deciding whether to show /bargain");
            }

            FakeLuckPerms.NODES.put("villagerbargains.command.price", true);
            FakeLuckPerms.NODES.put("villagerbargains.command.gear", false);
            withConfigRestored(() -> {
                VillagerBargainsConfig.setPricingMode(PricingMode.NORMAL);
                VillagerBargainsConfig.setBookLevelMode(PricingMode.NORMAL);
                VillagerBargainsConfig.setGearStrengthMode(PricingMode.NORMAL);
                // The player has only the price node.
                runAs(helper, player, "bargain price maximum");
                assertRefused(helper, player, "bargain books maximum");
                assertRefused(helper, player, "bargain gear maximum");
                assertRefused(helper, player, "bargain reload");
                // The operator is denied only the gear node.
                assertRefused(helper, op, "bargain gear minimum");
                runAs(helper, op, "bargain books minimum");
                assertModes(helper, PricingMode.MAXIMUM, PricingMode.MINIMUM, PricingMode.NORMAL, "after the node checks");
            });
        } finally {
            FakeLuckPerms.reset();
        }
        helper.succeed();
    }

    private static void assertModes(GameTestHelper helper, PricingMode pricing, PricingMode books, PricingMode gear, String when) {
        helper.assertValueEqual(VillagerBargainsConfig.pricingMode(), pricing, "pricing " + when);
        helper.assertValueEqual(VillagerBargainsConfig.bookLevelMode(), books, "bookLevels " + when);
        helper.assertValueEqual(VillagerBargainsConfig.gearStrengthMode(), gear, "gearStrength " + when);
    }

    private static void assertRefused(GameTestHelper helper, CommandSourceStack source, String command) {
        try {
            dispatcher(helper).execute(command, source);
        } catch (CommandSyntaxException expected) {
            return;
        }
        throw helper.assertionException("/%s ran for a source that lacks its permission", command);
    }

    private static void run(GameTestHelper helper, String command) {
        runAs(helper, console(helper), command);
    }

    private static void runAs(GameTestHelper helper, CommandSourceStack source, String command) {
        try {
            dispatcher(helper).execute(command, source);
        } catch (CommandSyntaxException e) {
            throw helper.assertionException("/%s failed: %s", command, e.getMessage());
        }
    }

    private static CommandSourceStack console(GameTestHelper helper) {
        return helper.getLevel().getServer().createCommandSourceStack();
    }

    private static CommandDispatcher<CommandSourceStack> dispatcher(GameTestHelper helper) {
        return helper.getLevel().getServer().getCommands().getDispatcher();
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

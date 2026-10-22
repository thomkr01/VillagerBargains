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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Runs {@code /bargain} and checks that every subcommand changes only its own option, that
 * {@code reload} re-reads the file, that {@code refresh} applies the demand rule to existing trades
 * without changing them, and who may use the commands: operators yes, players without
 * operator no, and with a permissions mod (see {@link FakeLuckPerms}) each node decides on its own.
 * Each test restores the config before it returns (all within one tick), so trade tests running
 * alongside never see the changes.
 */
public final class BargainCommandsGameTest {
    private static final List<String> SUBCOMMANDS = List.of("reload", "price", "books", "gear", "demand", "trader", "refresh");
    /** Every command form; none of them may run for a player without operator. */
    private static final List<String> EVERY_COMMAND = List.of(
            "bargain", "bargain reload",
            "bargain price", "bargain price minimum", "bargain price normal", "bargain price maximum",
            "bargain books", "bargain books minimum", "bargain books normal", "bargain books maximum",
            "bargain gear", "bargain gear minimum", "bargain gear normal", "bargain gear maximum",
            "bargain demand", "bargain demand vanilla", "bargain demand pinned",
            "bargain trader", "bargain trader vanilla", "bargain trader pinned",
            "bargain refresh", "bargain refresh villagers", "bargain refresh traders",
            "bargain refresh 00000000-0000-0000-0000-000000000000");

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
            run(helper, "bargain demand");
            run(helper, "bargain trader");
        });
        helper.succeed();
    }

    @GameTest
    public void demandAndTraderCanBeChanged(GameTestHelper helper) {
        withConfigRestored(() -> {
            run(helper, "bargain demand vanilla");
            helper.assertTrue(VillagerBargainsConfig.vanillaDemand(), "vanillaDemand after /bargain demand vanilla");
            helper.assertTrue(!VillagerBargainsConfig.vanillaWanderingTrader(), "/bargain demand leaves vanillaWanderingTrader alone");
            run(helper, "bargain trader vanilla");
            helper.assertTrue(VillagerBargainsConfig.vanillaWanderingTrader(), "vanillaWanderingTrader after /bargain trader vanilla");
            run(helper, "bargain demand pinned");
            helper.assertTrue(!VillagerBargainsConfig.vanillaDemand(), "vanillaDemand after /bargain demand pinned");
            helper.assertTrue(VillagerBargainsConfig.vanillaWanderingTrader(), "/bargain demand leaves vanillaWanderingTrader alone");
            run(helper, "bargain trader pinned");
            helper.assertTrue(!VillagerBargainsConfig.vanillaWanderingTrader(), "vanillaWanderingTrader after /bargain trader pinned");
        });
        helper.succeed();
    }

    /**
     * Offers made under MINIMUM have no demand. After switching to MAXIMUM, {@code refresh} must
     * pin their demand to MAXIMUM while keeping the very same trades, and leave a wandering trader
     * set to vanilla alone.
     */
    @GameTest
    public void refreshKeepsTradesAndPinsTheirDemand(GameTestHelper helper) {
        withConfigRestored(() -> {
            run(helper, "bargain demand pinned");
            run(helper, "bargain trader pinned");
            run(helper, "bargain price minimum");
            Villager farmer = TradeTestSupport.spawnTrader(helper);
            farmer.setVillagerData(farmer.getVillagerData().withLevel(3).withProfession(helper.getLevel().registryAccess()
                    .lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow(VillagerProfession.FARMER)));
            AbstractVillager trader = helper.spawn(EntityTypes.WANDERING_TRADER, new BlockPos(2, 1, 1));
            List<MerchantOffer> farmerOffers = List.copyOf(farmer.getOffers());
            List<MerchantOffer> traderOffers = List.copyOf(trader.getOffers());
            assertDemand(helper, farmer, false, "farmer before refresh");
            assertDemand(helper, trader, false, "wandering trader before refresh");

            run(helper, "bargain price maximum");
            run(helper, "bargain trader vanilla");
            run(helper, "bargain refresh");
            assertSameOffers(helper, farmer, farmerOffers);
            assertSameOffers(helper, trader, traderOffers);
            assertDemand(helper, farmer, true, "farmer after refresh");
            assertDemand(helper, trader, false, "wandering trader set to vanilla after refresh");

            run(helper, "bargain trader pinned");
            run(helper, "bargain price minimum");
            run(helper, "bargain refresh traders");
            assertDemand(helper, farmer, true, "farmer after /bargain refresh traders");
            assertDemand(helper, trader, false, "wandering trader after /bargain refresh traders");
            run(helper, "bargain price maximum");
            run(helper, "bargain refresh " + trader.getUUID());
            assertDemand(helper, trader, true, "wandering trader after /bargain refresh <its uuid>");
            run(helper, "bargain price minimum");
            run(helper, "bargain refresh villagers");
            assertDemand(helper, farmer, false, "farmer after /bargain refresh villagers");
            assertDemand(helper, trader, true, "wandering trader after /bargain refresh villagers");
            assertSameOffers(helper, farmer, farmerOffers);
            assertSameOffers(helper, trader, traderOffers);
        });
        helper.succeed();
    }

    /** {@code trader} still has exactly the offers it had (same objects, so same items and prices). */
    private static void assertSameOffers(GameTestHelper helper, AbstractVillager trader, List<MerchantOffer> before) {
        helper.assertValueEqual(trader.getOffers().size(), before.size(), "number of offers after refresh");
        for (int i = 0; i < before.size(); i++) {
            helper.assertTrue(trader.getOffers().get(i) == before.get(i), "offer " + i + " kept after refresh");
        }
    }

    /** Every offer of {@code trader} has the MAXIMUM demand pin (its max uses) or the MINIMUM one (0). */
    private static void assertDemand(GameTestHelper helper, AbstractVillager trader, boolean maximum, String when) {
        helper.assertTrue(!trader.getOffers().isEmpty(), "offers " + when);
        for (MerchantOffer offer : trader.getOffers()) {
            helper.assertValueEqual(offer.getDemand(), maximum ? offer.getMaxUses() : 0, "demand " + when);
        }
    }

    @GameTest
    public void reloadReadsTheFile(GameTestHelper helper) {
        withConfigRestored(() -> {
            run(helper, "bargain price minimum");
            try {
                Files.writeString(FabricLoader.getInstance().getConfigDir().resolve("villagerbargains.json"),
                        "{\"pricing\": \"MAXIMUM\", \"bookLevels\": \"MAXIMUM\", \"gearStrength\": \"MINIMUM\","
                                + " \"vanillaDemand\": true, \"vanillaWanderingTrader\": true}");
            } catch (IOException e) {
                throw helper.assertionException("Could not write the config file: %s", e.getMessage());
            }
            run(helper, "bargain reload");
            helper.assertValueEqual(VillagerBargainsConfig.pricingMode(), PricingMode.MAXIMUM, "pricing after reload");
            helper.assertValueEqual(VillagerBargainsConfig.bookLevelMode(), PricingMode.MAXIMUM, "bookLevels after reload");
            helper.assertValueEqual(VillagerBargainsConfig.gearStrengthMode(), PricingMode.MINIMUM, "gearStrength after reload");
            helper.assertTrue(VillagerBargainsConfig.vanillaDemand(), "vanillaDemand after reload");
            helper.assertTrue(VillagerBargainsConfig.vanillaWanderingTrader(), "vanillaWanderingTrader after reload");
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
        boolean vanillaDemand = VillagerBargainsConfig.vanillaDemand();
        boolean vanillaTrader = VillagerBargainsConfig.vanillaWanderingTrader();
        try {
            action.run();
        } finally {
            VillagerBargainsConfig.setPricingMode(pricing);
            VillagerBargainsConfig.setBookLevelMode(books);
            VillagerBargainsConfig.setGearStrengthMode(gear);
            VillagerBargainsConfig.setVanillaDemand(vanillaDemand);
            VillagerBargainsConfig.setVanillaWanderingTrader(vanillaTrader);
        }
    }
}

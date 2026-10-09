package com.villagerbargains.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.villagerbargains.config.PricingMode;
import com.villagerbargains.config.VillagerBargainsConfig;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The {@code /bargain} command: shows, sets and reloads every config option without a restart.
 *
 * <p>Each subcommand has its own permission node ({@code villagerbargains.command.<name>}).
 * With a permissions mod such as LuckPerms (through fabric-permissions-api, which LuckPerms
 * ships) those nodes decide; otherwise, and for nodes nobody set, operator level 2 is required.
 */
public final class BargainCommands {
    public static final String PERMISSION_PREFIX = "villagerbargains.command.";
    /** Every subcommand, which is also the last part of its permission node. */
    static final List<String> NODES = List.of("reload", "price", "books", "gear");
    /** Operators, the same level as {@code /gamerule}. */
    private static final PermissionLevel DEFAULT_LEVEL = PermissionLevel.byId(2);
    private static final boolean PERMISSIONS_API = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");

    private BargainCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bargain")
                .requires(BargainCommands::anyAllowed)
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("Villager Bargains: " + summary()), false);
                    return 1;
                })
                .then(Commands.literal("reload")
                        .requires(source -> allowed(source, "reload"))
                        .executes(context -> {
                            VillagerBargainsConfig.load();
                            context.getSource().sendSuccess(() -> Component.literal("Villager Bargains: reloaded config, " + summary()), true);
                            return 1;
                        }))
                .then(setting("price", "pricing", VillagerBargainsConfig::pricingMode, VillagerBargainsConfig::setPricingMode))
                .then(setting("books", "bookLevels", VillagerBargainsConfig::bookLevelMode, VillagerBargainsConfig::setBookLevelMode))
                .then(setting("gear", "gearStrength", VillagerBargainsConfig::gearStrengthMode, VillagerBargainsConfig::setGearStrengthMode)));
    }

    private static String summary() {
        return "pricing " + VillagerBargainsConfig.pricingMode() + ", bookLevels " + VillagerBargainsConfig.bookLevelMode()
                + ", gearStrength " + VillagerBargainsConfig.gearStrengthMode();
    }

    /** {@code /bargain <name>} shows the option, {@code /bargain <name> <mode>} sets and saves it. */
    private static LiteralArgumentBuilder<CommandSourceStack> setting(
            String name, String key, Supplier<PricingMode> get, Consumer<PricingMode> set) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(name)
                .requires(source -> allowed(source, name))
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("Villager Bargains: " + key + " is " + get.get()), false);
                    return 1;
                });
        for (PricingMode mode : PricingMode.values()) {
            node.then(Commands.literal(mode.name().toLowerCase(Locale.ROOT)).executes(context -> {
                set.accept(mode);
                context.getSource().sendSuccess(() -> Component.literal("Villager Bargains: " + key + " set to " + mode
                        + " (applies to trades villagers unlock from now on)"), true);
                return 1;
            }));
        }
        return node;
    }

    /**
     * Whether the source may see {@code /bargain} at all. Checks every node (no short-circuit):
     * LuckPerms learns a node the first time it is checked, and this runs for every joining
     * player, so all nodes show up in its web editor even for players who have none of them.
     */
    private static boolean anyAllowed(CommandSourceStack source) {
        boolean any = false;
        for (String node : NODES) {
            any |= allowed(source, node);
        }
        return any;
    }

    static boolean allowed(CommandSourceStack source, String node) {
        // Permissions is only touched when its mod is installed, so the class is never loaded otherwise.
        return PERMISSIONS_API
                ? Permissions.check(source, PERMISSION_PREFIX + node, DEFAULT_LEVEL)
                : source.permissions().hasPermission(new Permission.HasCommandLevel(DEFAULT_LEVEL));
    }
}

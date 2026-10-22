package com.villagerbargains.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.villagerbargains.config.PricingMode;
import com.villagerbargains.config.VillagerBargainsConfig;
import com.villagerbargains.price.TradeRefresh;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The {@code /bargain} command: shows, sets and reloads every config option without a restart,
 * and applies the current demand rule to existing trades.
 *
 * <p>Each subcommand has its own permission node ({@code villagerbargains.command.<name>}).
 * With a permissions mod such as LuckPerms (through fabric-permissions-api, which LuckPerms
 * ships) those nodes decide; otherwise, and for nodes nobody set, operator level 2 is required.
 */
public final class BargainCommands {
    public static final String PERMISSION_PREFIX = "villagerbargains.command.";
    /** Every subcommand, which is also the last part of its permission node. */
    static final List<String> NODES = List.of("reload", "price", "books", "gear", "demand", "trader", "refresh");
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
                .then(modeSetting("price", "pricing", VillagerBargainsConfig::pricingMode, VillagerBargainsConfig::setPricingMode))
                .then(modeSetting("books", "bookLevels", VillagerBargainsConfig::bookLevelMode, VillagerBargainsConfig::setBookLevelMode))
                .then(modeSetting("gear", "gearStrength", VillagerBargainsConfig::gearStrengthMode, VillagerBargainsConfig::setGearStrengthMode))
                .then(flagSetting("demand", "vanillaDemand", VillagerBargainsConfig::vanillaDemand, VillagerBargainsConfig::setVanillaDemand))
                .then(flagSetting("trader", "vanillaWanderingTrader", VillagerBargainsConfig::vanillaWanderingTrader, VillagerBargainsConfig::setVanillaWanderingTrader))
                .then(Commands.literal("refresh")
                        .requires(source -> allowed(source, "refresh"))
                        .executes(context -> refresh(context.getSource(), entity -> true))
                        .then(Commands.literal("villagers").executes(context -> refresh(context.getSource(), entity -> entity instanceof Villager)))
                        .then(Commands.literal("traders").executes(context -> refresh(context.getSource(),
                                entity -> entity.getType() == EntityTypes.WANDERING_TRADER)))
                        .then(Commands.argument("uuid", UuidArgument.uuid()).executes(context -> {
                            UUID uuid = UuidArgument.getUuid(context, "uuid");
                            return refresh(context.getSource(), entity -> entity.getUUID().equals(uuid));
                        }))));
    }

    /**
     * {@code /bargain refresh [villagers|traders|<uuid>]}: applies the current demand rule to the
     * existing offers of the loaded villagers and wandering traders that match {@code filter}.
     */
    private static int refresh(CommandSourceStack source, Predicate<Entity> filter) {
        int refreshed = 0;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof AbstractVillager trader && filter.test(entity)) {
                    TradeRefresh.refresh(trader);
                    refreshed++;
                }
            }
        }
        int count = refreshed;
        source.sendSuccess(() -> Component.literal("Villager Bargains: applied the current demand rule to the trades of "
                + count + " villagers and wandering traders"), true);
        return refreshed;
    }

    private static String summary() {
        return "pricing " + VillagerBargainsConfig.pricingMode() + ", bookLevels " + VillagerBargainsConfig.bookLevelMode()
                + ", gearStrength " + VillagerBargainsConfig.gearStrengthMode()
                + ", vanillaDemand " + VillagerBargainsConfig.vanillaDemand()
                + ", vanillaWanderingTrader " + VillagerBargainsConfig.vanillaWanderingTrader();
    }

    /** {@code /bargain <name>} shows the option, {@code /bargain <name> <choice>} sets and saves it. */
    private static LiteralArgumentBuilder<CommandSourceStack> setting(
            String name, String key, Supplier<?> get, Map<String, Runnable> choices) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(name)
                .requires(source -> allowed(source, name))
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("Villager Bargains: " + key + " is " + get.get()), false);
                    return 1;
                });
        choices.forEach((word, apply) -> node.then(Commands.literal(word).executes(context -> {
            apply.run();
            context.getSource().sendSuccess(() -> Component.literal("Villager Bargains: " + key + " set to "
                    + word.toUpperCase(Locale.ROOT) + " (applies to trades villagers unlock from now on)"), true);
            return 1;
        })));
        return node;
    }

    /** A setting with one choice per {@link PricingMode}: minimum, normal, maximum. */
    private static LiteralArgumentBuilder<CommandSourceStack> modeSetting(
            String name, String key, Supplier<PricingMode> get, Consumer<PricingMode> set) {
        Map<String, Runnable> choices = new LinkedHashMap<>();
        for (PricingMode mode : PricingMode.values()) {
            choices.put(mode.name().toLowerCase(Locale.ROOT), () -> set.accept(mode));
        }
        return setting(name, key, get, choices);
    }

    /** An on/off setting with the choices {@code vanilla} (true) and {@code pinned} (false). */
    private static LiteralArgumentBuilder<CommandSourceStack> flagSetting(
            String name, String key, BooleanSupplier get, Consumer<Boolean> set) {
        Map<String, Runnable> choices = new LinkedHashMap<>();
        choices.put("vanilla", () -> set.accept(true));
        choices.put("pinned", () -> set.accept(false));
        return setting(name, key, () -> get.getAsBoolean() ? "VANILLA" : "PINNED", choices);
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

package com.villagerbargains.trade;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * VANILLA TRADE REGISTRY  —  Minecraft 26.1.2 / 1.21.x
 * ─────────────────────────────────────────────────────────────────────────────
 * THIS IS THE ONLY FILE THAT NEEDS UPDATING WHEN MINECRAFT CHANGES TRADE RANGES.
 *
 * Normal trades:    register(map, "minecraft:<profession>/level_N/<name>", min, max)
 * Enchanted books:  registerBook(books, "minecraft:<enchantment_id>", min, max)
 *   — sellKey becomes "enchanted_book:minecraft:<enchantment_id>" internally
 *   — min/max are the emerald cost for that specific enchantment
 * ─────────────────────────────────────────────────────────────────────────────
 */
public final class VanillaTrades {
    private VanillaTrades() {}

    /** Primary registry: tradeId -> TradeDefinition (all normal trades). */
    private static final Map<String, TradeDefinition> REGISTRY;

    /**
     * Secondary registry for enchanted books: sellKey -> TradeDefinition.
     * Key format: "enchanted_book:minecraft:<enchantment_id>"
     * Looked up by the SELL item enchantment, not the buy item.
     */
    private static final Map<String, TradeDefinition> BOOK_REGISTRY;

    static {
        Map<String, TradeDefinition> map   = new LinkedHashMap<>();
        Map<String, TradeDefinition> books = new LinkedHashMap<>();

        // ── Armorer ────────────────────────────────────────────────────────────
        register(map, "minecraft:armorer/level_1/coal_buy",               16, 24);
        register(map, "minecraft:armorer/level_1/iron_leggings_sell",       7, 11);
        register(map, "minecraft:armorer/level_1/iron_boots_sell",          4,  7);
        register(map, "minecraft:armorer/level_1/iron_helmet_sell",         5,  9);
        register(map, "minecraft:armorer/level_1/iron_chestplate_sell",     9, 14);
        register(map, "minecraft:armorer/level_2/iron_ingot_buy",           7, 10);
        register(map, "minecraft:armorer/level_2/bell_sell",               36, 44);
        register(map, "minecraft:armorer/level_2/chainmail_leggings_sell",  3,  6);
        register(map, "minecraft:armorer/level_2/chainmail_boots_sell",     1,  4);
        register(map, "minecraft:armorer/level_3/lava_bucket_buy",          1,  1);
        register(map, "minecraft:armorer/level_3/chainmail_helmet_sell",    1,  4);
        register(map, "minecraft:armorer/level_3/chainmail_chestplate_sell",4,  8);
        register(map, "minecraft:armorer/level_3/shield_sell",              5,  9);
        register(map, "minecraft:armorer/level_4/diamond_buy",              2,  4);
        register(map, "minecraft:armorer/level_4/diamond_leggings_sell",   14, 23);
        register(map, "minecraft:armorer/level_4/diamond_boots_sell",       8, 14);
        register(map, "minecraft:armorer/level_5/diamond_helmet_sell",      8, 14);
        register(map, "minecraft:armorer/level_5/diamond_chestplate_sell", 16, 26);

        // ── Butcher ────────────────────────────────────────────────────────────
        register(map, "minecraft:butcher/level_1/chicken_buy",             14, 18);
        register(map, "minecraft:butcher/level_1/cooked_chicken_sell",      6,  8);
        register(map, "minecraft:butcher/level_1/porkchop_buy",             7, 10);
        register(map, "minecraft:butcher/level_2/coal_buy",                15, 20);
        register(map, "minecraft:butcher/level_2/cooked_porkchop_sell",     5,  7);
        register(map, "minecraft:butcher/level_3/mutton_buy",               7, 10);
        register(map, "minecraft:butcher/level_3/cooked_mutton_sell",       5,  7);
        register(map, "minecraft:butcher/level_4/dried_kelp_block_sell",    1,  3);
        register(map, "minecraft:butcher/level_4/sweet_berries_buy",       10, 13);
        register(map, "minecraft:butcher/level_5/rabbit_buy",               4,  6);
        register(map, "minecraft:butcher/level_5/cooked_rabbit_sell",       3,  5);

        // ── Cartographer ───────────────────────────────────────────────────────
        register(map, "minecraft:cartographer/level_1/paper_buy",          24, 36);
        register(map, "minecraft:cartographer/level_1/map_sell",            7, 11);
        register(map, "minecraft:cartographer/level_2/glass_pane_buy",     11, 17);
        register(map, "minecraft:cartographer/level_2/compass_sell",        6, 11);
        register(map, "minecraft:cartographer/level_3/item_frame_sell",     7, 11);
        register(map, "minecraft:cartographer/level_4/ocean_explorer_map_sell",  12, 20);
        register(map, "minecraft:cartographer/level_5/woodland_explorer_map_sell", 14, 22);

        // ── Cleric ─────────────────────────────────────────────────────────────
        register(map, "minecraft:cleric/level_1/rotten_flesh_buy",         36, 40);
        register(map, "minecraft:cleric/level_1/emerald_sell",              1,  1);
        register(map, "minecraft:cleric/level_2/gold_ingot_buy",            3,  4);
        register(map, "minecraft:cleric/level_2/redstone_sell",             1,  2);
        register(map, "minecraft:cleric/level_3/rabbit_foot_buy",           2,  3);
        register(map, "minecraft:cleric/level_3/lapis_lazuli_sell",         1,  2);
        register(map, "minecraft:cleric/level_4/turtle_shell_buy",          4,  6);
        register(map, "minecraft:cleric/level_4/potion_sell",               4,  6);
        register(map, "minecraft:cleric/level_5/nether_wart_buy",          22, 32);
        register(map, "minecraft:cleric/level_5/exp_bottle_sell",           3,  5);
        register(map, "minecraft:cleric/level_5/glowstone_buy",             5,  7);

        // ── Farmer ─────────────────────────────────────────────────────────────
        register(map, "minecraft:farmer/level_1/wheat_buy",                20, 26);
        register(map, "minecraft:farmer/level_1/bread_sell",                1,  1);
        register(map, "minecraft:farmer/level_1/pumpkin_buy",               6, 13);
        register(map, "minecraft:farmer/level_1/apple_sell",                1,  2);
        register(map, "minecraft:farmer/level_2/potato_buy",               26, 34);
        register(map, "minecraft:farmer/level_2/cookie_sell",               3,  3);
        register(map, "minecraft:farmer/level_3/pumpkin_pie_sell",          1,  2);
        register(map, "minecraft:farmer/level_3/melon_buy",                 4, 10);
        register(map, "minecraft:farmer/level_4/cake_sell",                 1,  1);
        register(map, "minecraft:farmer/level_4/suspicious_stew_sell",      1,  2);
        register(map, "minecraft:farmer/level_5/golden_carrot_sell",        3,  4);
        register(map, "minecraft:farmer/level_5/glistering_melon_slice_sell", 4, 5);

        // ── Fisherman ──────────────────────────────────────────────────────────
        register(map, "minecraft:fisherman/level_1/coal_buy",              10, 15);
        register(map, "minecraft:fisherman/level_1/cod_sell",               6, 10);
        register(map, "minecraft:fisherman/level_1/string_buy",            20, 22);
        register(map, "minecraft:fisherman/level_2/cooked_cod_sell",        6,  8);
        register(map, "minecraft:fisherman/level_2/salmon_buy",             6,  9);
        register(map, "minecraft:fisherman/level_3/cooked_salmon_sell",     8,  9);
        register(map, "minecraft:fisherman/level_3/tropical_fish_buy",      6,  7);
        register(map, "minecraft:fisherman/level_4/pufferfish_buy",         4,  6);
        register(map, "minecraft:fisherman/level_4/boat_sell",              3,  5);
        register(map, "minecraft:fisherman/level_5/enchanted_fishing_rod_sell", 8, 22);

        // ── Fletcher ───────────────────────────────────────────────────────────
        register(map, "minecraft:fletcher/level_1/stick_buy",              32, 64);
        register(map, "minecraft:fletcher/level_1/arrow_sell",             16, 32);
        register(map, "minecraft:fletcher/level_1/flint_buy",              26, 30);
        register(map, "minecraft:fletcher/level_2/bow_sell",                2,  3);
        register(map, "minecraft:fletcher/level_2/gravel_buy",             10, 10);
        register(map, "minecraft:fletcher/level_3/crossbow_sell",           3,  5);
        register(map, "minecraft:fletcher/level_3/string_buy",             15, 20);
        register(map, "minecraft:fletcher/level_4/arrow_sell_tier2",        5,  9);
        register(map, "minecraft:fletcher/level_4/feather_buy",            24, 30);
        register(map, "minecraft:fletcher/level_5/enchanted_bow_sell",      8, 22);
        register(map, "minecraft:fletcher/level_5/tipped_arrow_sell",       2,  5);

        // ── Leatherworker ──────────────────────────────────────────────────────
        register(map, "minecraft:leatherworker/level_1/leather_buy",        6, 10);
        register(map, "minecraft:leatherworker/level_1/leather_pants_sell", 2,  4);
        register(map, "minecraft:leatherworker/level_1/leather_boots_sell", 2,  4);
        register(map, "minecraft:leatherworker/level_2/flint_buy",          9, 12);
        register(map, "minecraft:leatherworker/level_2/leather_chestplate_sell", 3, 5);
        register(map, "minecraft:leatherworker/level_2/leather_helmet_sell",2,  4);
        register(map, "minecraft:leatherworker/level_3/rabbit_hide_buy",    9, 12);
        register(map, "minecraft:leatherworker/level_3/leather_horse_armor_sell", 6, 10);
        register(map, "minecraft:leatherworker/level_4/turtle_shell_sell",  4,  8);
        register(map, "minecraft:leatherworker/level_5/saddle_sell",        8, 10);

        // ── Librarian (non-book trades) ───────────────────────────────────────────────
        register(map, "minecraft:librarian/level_1/paper_buy",             24, 36);
        register(map, "minecraft:librarian/level_1/bookshelf_sell",         9, 12);
        register(map, "minecraft:librarian/level_2/book_buy",               8, 10);
        register(map, "minecraft:librarian/level_2/lantern_sell",           1,  2);
        register(map, "minecraft:librarian/level_3/ink_sac_buy",            5,  7);
        register(map, "minecraft:librarian/level_3/glass_sell",             4,  5);
        register(map, "minecraft:librarian/level_4/writable_book_buy",      2,  2);
        register(map, "minecraft:librarian/level_4/clock_sell",             5,  7);
        register(map, "minecraft:librarian/level_4/compass_sell",           4,  6);
        register(map, "minecraft:librarian/level_5/name_tag_sell",         20, 22);

        // ── Librarian enchanted books — per-enchantment emerald cost ──────────────────────
        // Matched by SELL enchantment, not buy item.
        // Source: Minecraft Wiki — Librarian Trades, MC 1.21
        registerBook(books, "minecraft:protection",            5, 19);
        registerBook(books, "minecraft:fire_protection",       5, 19);
        registerBook(books, "minecraft:feather_falling",       5, 19);
        registerBook(books, "minecraft:blast_protection",      5, 19);
        registerBook(books, "minecraft:projectile_protection", 5, 19);
        registerBook(books, "minecraft:respiration",           5, 19);
        registerBook(books, "minecraft:aqua_affinity",         5,  6);
        registerBook(books, "minecraft:thorns",                5, 19);
        registerBook(books, "minecraft:depth_strider",         5, 19);
        registerBook(books, "minecraft:frost_walker",          5, 19);
        registerBook(books, "minecraft:binding_curse",         5,  6);
        registerBook(books, "minecraft:sharpness",             5, 19);
        registerBook(books, "minecraft:smite",                 5, 19);
        registerBook(books, "minecraft:bane_of_arthropods",    5, 19);
        registerBook(books, "minecraft:knockback",             5, 19);
        registerBook(books, "minecraft:fire_aspect",           5, 19);
        registerBook(books, "minecraft:looting",               5, 19);
        registerBook(books, "minecraft:sweeping_edge",         5, 19);
        registerBook(books, "minecraft:efficiency",            5, 19);
        registerBook(books, "minecraft:silk_touch",            5, 19);
        registerBook(books, "minecraft:unbreaking",            5, 19);
        registerBook(books, "minecraft:fortune",               5, 19);
        registerBook(books, "minecraft:power",                 5, 19);
        registerBook(books, "minecraft:punch",                 5, 19);
        registerBook(books, "minecraft:flame",                 5,  8);
        registerBook(books, "minecraft:infinity",              5,  8);
        registerBook(books, "minecraft:luck_of_the_sea",       5, 19);
        registerBook(books, "minecraft:lure",                  5, 19);
        registerBook(books, "minecraft:loyalty",               5, 19);
        registerBook(books, "minecraft:impaling",              5, 19);
        registerBook(books, "minecraft:riptide",               5, 19);
        registerBook(books, "minecraft:channeling",            5,  8);
        registerBook(books, "minecraft:multishot",             5,  8);
        registerBook(books, "minecraft:quick_charge",          5, 19);
        registerBook(books, "minecraft:piercing",              5, 19);
        registerBook(books, "minecraft:mending",              20, 38);
        registerBook(books, "minecraft:vanishing_curse",       5,  6);
        registerBook(books, "minecraft:soul_speed",            5, 19);
        registerBook(books, "minecraft:swift_sneak",           5, 19);
        registerBook(books, "minecraft:wind_burst",            5, 19);
        registerBook(books, "minecraft:density",               5, 19);
        registerBook(books, "minecraft:breach",                5, 19);

        // ── Mason ──────────────────────────────────────────────────────────────
        register(map, "minecraft:mason/level_1/clay_buy",                  10, 12);
        register(map, "minecraft:mason/level_1/brick_sell",                 1,  2);
        register(map, "minecraft:mason/level_2/stone_buy",                 20, 26);
        register(map, "minecraft:mason/level_2/chiseled_stone_bricks_sell", 1,  2);
        register(map, "minecraft:mason/level_3/stone_brick_buy",           10, 12);
        register(map, "minecraft:mason/level_3/andesite_sell",              1,  2);
        register(map, "minecraft:mason/level_4/granite_buy",               16, 22);
        register(map, "minecraft:mason/level_4/polished_granite_sell",      1,  2);
        register(map, "minecraft:mason/level_5/quartz_buy",                12, 16);
        register(map, "minecraft:mason/level_5/quartz_pillar_sell",         1,  2);

        // ── Shepherd ───────────────────────────────────────────────────────────
        register(map, "minecraft:shepherd/level_1/wool_buy",               18, 22);
        register(map, "minecraft:shepherd/level_1/shears_sell",             2,  3);
        register(map, "minecraft:shepherd/level_2/dye_sell",                1,  2);
        register(map, "minecraft:shepherd/level_3/wool_colored_sell",       1,  2);
        register(map, "minecraft:shepherd/level_4/carpet_sell",             1,  2);
        register(map, "minecraft:shepherd/level_5/banner_sell",             3,  5);
        register(map, "minecraft:shepherd/level_5/bed_sell",                3,  5);

        // ── Toolsmith ──────────────────────────────────────────────────────────
        register(map, "minecraft:toolsmith/level_1/coal_buy",              15, 21);
        register(map, "minecraft:toolsmith/level_1/stone_axe_sell",         1,  2);
        register(map, "minecraft:toolsmith/level_1/stone_shovel_sell",      1,  2);
        register(map, "minecraft:toolsmith/level_1/stone_pickaxe_sell",     1,  2);
        register(map, "minecraft:toolsmith/level_1/stone_hoe_sell",         1,  2);
        register(map, "minecraft:toolsmith/level_2/iron_ingot_buy",         7, 10);
        register(map, "minecraft:toolsmith/level_2/bell_sell",             36, 44);
        register(map, "minecraft:toolsmith/level_2/iron_axe_sell",          3,  5);
        register(map, "minecraft:toolsmith/level_3/flint_buy",              8, 10);
        register(map, "minecraft:toolsmith/level_3/iron_shovel_sell",       4,  6);
        register(map, "minecraft:toolsmith/level_3/iron_pickaxe_sell",      5,  7);
        register(map, "minecraft:toolsmith/level_4/diamond_buy",            1,  2);
        register(map, "minecraft:toolsmith/level_4/diamond_axe_sell",       9, 12);
        register(map, "minecraft:toolsmith/level_5/diamond_shovel_sell",    5,  7);
        register(map, "minecraft:toolsmith/level_5/diamond_pickaxe_sell",  13, 17);
        register(map, "minecraft:toolsmith/level_5/diamond_hoe_sell",       4,  7);

        // ── Weaponsmith ────────────────────────────────────────────────────────
        register(map, "minecraft:weaponsmith/level_1/coal_buy",            15, 21);
        register(map, "minecraft:weaponsmith/level_1/iron_axe_sell",        3,  5);
        register(map, "minecraft:weaponsmith/level_1/iron_sword_sell",      7, 11);
        register(map, "minecraft:weaponsmith/level_2/iron_ingot_buy",       7, 10);
        register(map, "minecraft:weaponsmith/level_2/bell_sell",           36, 44);
        register(map, "minecraft:weaponsmith/level_3/flint_buy",            6, 10);
        register(map, "minecraft:weaponsmith/level_3/iron_sword_sell_tier2",9, 13);
        register(map, "minecraft:weaponsmith/level_4/diamond_buy",          2,  4);
        register(map, "minecraft:weaponsmith/level_4/diamond_axe_sell",    12, 17);
        register(map, "minecraft:weaponsmith/level_5/diamond_sword_sell",  13, 17);

        REGISTRY      = Collections.unmodifiableMap(map);
        BOOK_REGISTRY = Collections.unmodifiableMap(books);
    }

    // ── Registry helpers ───────────────────────────────────────────────────────
    private static void register(Map<String, TradeDefinition> map, String id, int min, int max) {
        map.put(id, new TradeDefinition(id, min, max));
    }

    /** Registers an enchanted-book trade keyed by sell enchantment. */
    private static void registerBook(Map<String, TradeDefinition> books, String enchId, int min, int max) {
        String sellKey = "enchanted_book:" + enchId;
        books.put(sellKey, new TradeDefinition(sellKey, min, max, sellKey));
    }

    public static TradeDefinition get(String tradeId)        { return REGISTRY.get(tradeId); }
    public static TradeDefinition getByBook(String sellKey)  { return BOOK_REGISTRY.get(sellKey); }
    public static Map<String, TradeDefinition> getAll()      { return REGISTRY; }
    public static Map<String, TradeDefinition> getAllBooks()  { return BOOK_REGISTRY; }
}

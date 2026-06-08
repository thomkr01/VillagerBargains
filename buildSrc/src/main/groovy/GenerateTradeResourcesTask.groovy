import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/**
 * Gradle task: reads godrollmod.json (or uses defaults) and writes
 * one JSON file per registered vanilla trade into the output directory.
 *
 * Wired in build.gradle:
 *   tasks.named('processResources').configure { dependsOn(generateTradeResources) }
 *
 * Modular design:
 *   - TRADE_REGISTRY : the single source of truth for vanilla min/max prices.
 *   - PriceMode      : mirrors the runtime enum — change names here if the config format changes.
 *   - buildJson()    : the only method that knows what the JSON fragment looks like.
 *
 * To update for a new Minecraft version: edit TRADE_REGISTRY below.
 */
@CompileStatic
abstract class GenerateTradeResourcesTask extends DefaultTask {

    // ── Output directory (wired to src/main/resources in build.gradle) ─────────
    @OutputDirectory
    abstract DirectoryProperty getOutputDir()

    // ── Optional path to a pre-existing godrollmod.json ────────────────────────
    @Optional
    @InputFile
    abstract Property<File> getConfigFile()

    // ── Vanilla trade registry: id → [min, max] ────────────────────────────────
    // THIS IS THE ONLY SECTION TO EDIT WHEN MINECRAFT CHANGES TRADE RANGES.
    private static final Map<String, int[]> TRADE_REGISTRY = [
        // Armorer
        'minecraft:armorer/level_1/coal_buy'              : [16, 24] as int[],
        'minecraft:armorer/level_1/iron_leggings_sell'    : [ 7, 11] as int[],
        'minecraft:armorer/level_1/iron_boots_sell'       : [ 4,  7] as int[],
        'minecraft:armorer/level_1/iron_helmet_sell'      : [ 5,  9] as int[],
        'minecraft:armorer/level_1/iron_chestplate_sell'  : [ 9, 14] as int[],
        'minecraft:armorer/level_2/iron_ingot_buy'        : [ 7, 10] as int[],
        'minecraft:armorer/level_2/bell_sell'             : [36, 44] as int[],
        'minecraft:armorer/level_2/chainmail_leggings_sell': [3,  6] as int[],
        'minecraft:armorer/level_2/chainmail_boots_sell'  : [ 1,  4] as int[],
        'minecraft:armorer/level_3/lava_bucket_buy'       : [ 1,  1] as int[],
        'minecraft:armorer/level_3/chainmail_helmet_sell' : [ 1,  4] as int[],
        'minecraft:armorer/level_3/chainmail_chestplate_sell': [4, 8] as int[],
        'minecraft:armorer/level_3/shield_sell'           : [ 5,  9] as int[],
        'minecraft:armorer/level_4/diamond_buy'           : [ 2,  4] as int[],
        'minecraft:armorer/level_4/diamond_leggings_sell' : [14, 23] as int[],
        'minecraft:armorer/level_4/diamond_boots_sell'    : [ 8, 14] as int[],
        'minecraft:armorer/level_5/diamond_helmet_sell'   : [ 8, 14] as int[],
        'minecraft:armorer/level_5/diamond_chestplate_sell': [16,26] as int[],
        // Butcher
        'minecraft:butcher/level_1/chicken_buy'           : [14, 18] as int[],
        'minecraft:butcher/level_1/cooked_chicken_sell'   : [ 6,  8] as int[],
        'minecraft:butcher/level_1/porkchop_buy'          : [ 7, 10] as int[],
        'minecraft:butcher/level_2/coal_buy'              : [15, 20] as int[],
        'minecraft:butcher/level_2/cooked_porkchop_sell'  : [ 5,  7] as int[],
        'minecraft:butcher/level_3/mutton_buy'            : [ 7, 10] as int[],
        'minecraft:butcher/level_3/cooked_mutton_sell'    : [ 5,  7] as int[],
        'minecraft:butcher/level_4/dried_kelp_block_sell' : [ 1,  3] as int[],
        'minecraft:butcher/level_4/sweet_berries_buy'     : [10, 13] as int[],
        'minecraft:butcher/level_5/rabbit_buy'            : [ 4,  6] as int[],
        'minecraft:butcher/level_5/cooked_rabbit_sell'    : [ 3,  5] as int[],
        // Cartographer
        'minecraft:cartographer/level_1/paper_buy'        : [24, 36] as int[],
        'minecraft:cartographer/level_1/map_sell'         : [ 7, 11] as int[],
        'minecraft:cartographer/level_2/glass_pane_buy'   : [11, 17] as int[],
        'minecraft:cartographer/level_2/compass_sell'     : [ 6, 11] as int[],
        'minecraft:cartographer/level_3/item_frame_sell'  : [ 7, 11] as int[],
        'minecraft:cartographer/level_4/ocean_explorer_map_sell': [12,20] as int[],
        'minecraft:cartographer/level_5/woodland_explorer_map_sell': [14,22] as int[],
        // Cleric
        'minecraft:cleric/level_1/rotten_flesh_buy'       : [36, 40] as int[],
        'minecraft:cleric/level_1/emerald_sell'           : [ 1,  1] as int[],
        'minecraft:cleric/level_2/gold_ingot_buy'         : [ 3,  4] as int[],
        'minecraft:cleric/level_2/redstone_sell'          : [ 1,  2] as int[],
        'minecraft:cleric/level_3/rabbit_foot_buy'        : [ 2,  3] as int[],
        'minecraft:cleric/level_3/lapis_lazuli_sell'      : [ 1,  2] as int[],
        'minecraft:cleric/level_4/turtle_shell_buy'       : [ 4,  6] as int[],
        'minecraft:cleric/level_4/potion_sell'            : [ 4,  6] as int[],
        'minecraft:cleric/level_5/nether_wart_buy'        : [22, 32] as int[],
        'minecraft:cleric/level_5/exp_bottle_sell'        : [ 3,  5] as int[],
        'minecraft:cleric/level_5/glowstone_buy'          : [ 5,  7] as int[],
        // Farmer
        'minecraft:farmer/level_1/wheat_buy'              : [20, 26] as int[],
        'minecraft:farmer/level_1/bread_sell'             : [ 1,  1] as int[],
        'minecraft:farmer/level_1/pumpkin_buy'            : [ 6, 13] as int[],
        'minecraft:farmer/level_1/apple_sell'             : [ 1,  2] as int[],
        'minecraft:farmer/level_2/potato_buy'             : [26, 34] as int[],
        'minecraft:farmer/level_2/cookie_sell'            : [ 3,  3] as int[],
        'minecraft:farmer/level_3/pumpkin_pie_sell'       : [ 1,  2] as int[],
        'minecraft:farmer/level_3/melon_buy'              : [ 4, 10] as int[],
        'minecraft:farmer/level_4/cake_sell'              : [ 1,  1] as int[],
        'minecraft:farmer/level_4/suspicious_stew_sell'   : [ 1,  2] as int[],
        'minecraft:farmer/level_5/golden_carrot_sell'     : [ 3,  4] as int[],
        'minecraft:farmer/level_5/glistering_melon_slice_sell': [4, 5] as int[],
        // Fisherman
        'minecraft:fisherman/level_1/coal_buy'            : [10, 15] as int[],
        'minecraft:fisherman/level_1/cod_sell'            : [ 6, 10] as int[],
        'minecraft:fisherman/level_1/string_buy'          : [20, 22] as int[],
        'minecraft:fisherman/level_2/cooked_cod_sell'     : [ 6,  8] as int[],
        'minecraft:fisherman/level_2/salmon_buy'          : [ 6,  9] as int[],
        'minecraft:fisherman/level_3/cooked_salmon_sell'  : [ 8,  9] as int[],
        'minecraft:fisherman/level_3/tropical_fish_buy'   : [ 6,  7] as int[],
        'minecraft:fisherman/level_4/pufferfish_buy'      : [ 4,  6] as int[],
        'minecraft:fisherman/level_4/boat_sell'           : [ 3,  5] as int[],
        'minecraft:fisherman/level_5/enchanted_fishing_rod_sell': [8,22] as int[],
        // Fletcher
        'minecraft:fletcher/level_1/stick_buy'            : [32, 64] as int[],
        'minecraft:fletcher/level_1/arrow_sell'           : [16, 32] as int[],
        'minecraft:fletcher/level_1/flint_buy'            : [26, 30] as int[],
        'minecraft:fletcher/level_2/bow_sell'             : [ 2,  3] as int[],
        'minecraft:fletcher/level_2/gravel_buy'           : [10, 10] as int[],
        'minecraft:fletcher/level_3/crossbow_sell'        : [ 3,  5] as int[],
        'minecraft:fletcher/level_3/string_buy'           : [15, 20] as int[],
        'minecraft:fletcher/level_4/arrow_sell_tier2'     : [ 5,  9] as int[],
        'minecraft:fletcher/level_4/feather_buy'          : [24, 30] as int[],
        'minecraft:fletcher/level_5/enchanted_bow_sell'   : [ 8, 22] as int[],
        'minecraft:fletcher/level_5/tipped_arrow_sell'    : [ 2,  5] as int[],
        // Leatherworker
        'minecraft:leatherworker/level_1/leather_buy'     : [ 6, 10] as int[],
        'minecraft:leatherworker/level_1/leather_pants_sell': [2,  4] as int[],
        'minecraft:leatherworker/level_1/leather_boots_sell': [2,  4] as int[],
        'minecraft:leatherworker/level_2/flint_buy'       : [ 9, 12] as int[],
        'minecraft:leatherworker/level_2/leather_chestplate_sell': [3, 5] as int[],
        'minecraft:leatherworker/level_2/leather_helmet_sell': [2, 4] as int[],
        'minecraft:leatherworker/level_3/rabbit_hide_buy' : [ 9, 12] as int[],
        'minecraft:leatherworker/level_3/leather_horse_armor_sell': [6,10] as int[],
        'minecraft:leatherworker/level_4/turtle_shell_sell': [4,  8] as int[],
        'minecraft:leatherworker/level_5/saddle_sell'     : [ 8, 10] as int[],
        // Librarian
        'minecraft:librarian/level_1/paper_buy'           : [24, 36] as int[],
        'minecraft:librarian/level_1/bookshelf_sell'      : [ 9, 12] as int[],
        'minecraft:librarian/level_2/book_buy'            : [ 8, 10] as int[],
        'minecraft:librarian/level_2/lantern_sell'        : [ 1,  2] as int[],
        'minecraft:librarian/level_3/ink_sac_buy'         : [ 5,  7] as int[],
        'minecraft:librarian/level_3/glass_sell'          : [ 4,  5] as int[],
        'minecraft:librarian/level_4/writable_book_buy'   : [ 2,  2] as int[],
        'minecraft:librarian/level_4/clock_sell'          : [ 5,  7] as int[],
        'minecraft:librarian/level_4/compass_sell'        : [ 4,  6] as int[],
        'minecraft:librarian/level_5/name_tag_sell'       : [20, 22] as int[],
        'minecraft:librarian/level_1/enchanted_book'      : [ 5, 64] as int[],
        'minecraft:librarian/level_2/enchanted_book'      : [ 5, 64] as int[],
        'minecraft:librarian/level_3/enchanted_book'      : [ 5, 64] as int[],
        'minecraft:librarian/level_4/enchanted_book'      : [ 5, 64] as int[],
        'minecraft:librarian/level_5/enchanted_book'      : [ 5, 64] as int[],
        // Mason
        'minecraft:mason/level_1/clay_buy'                : [10, 12] as int[],
        'minecraft:mason/level_1/brick_sell'              : [ 1,  2] as int[],
        'minecraft:mason/level_2/stone_buy'               : [20, 26] as int[],
        'minecraft:mason/level_2/chiseled_stone_bricks_sell': [1, 2] as int[],
        'minecraft:mason/level_3/stone_brick_buy'         : [10, 12] as int[],
        'minecraft:mason/level_3/andesite_sell'           : [ 1,  2] as int[],
        'minecraft:mason/level_4/granite_buy'             : [16, 22] as int[],
        'minecraft:mason/level_4/polished_granite_sell'   : [ 1,  2] as int[],
        'minecraft:mason/level_5/quartz_buy'              : [12, 16] as int[],
        'minecraft:mason/level_5/quartz_pillar_sell'      : [ 1,  2] as int[],
        // Shepherd
        'minecraft:shepherd/level_1/wool_buy'             : [18, 22] as int[],
        'minecraft:shepherd/level_1/shears_sell'          : [ 2,  3] as int[],
        'minecraft:shepherd/level_2/dye_sell'             : [ 1,  2] as int[],
        'minecraft:shepherd/level_3/wool_colored_sell'    : [ 1,  2] as int[],
        'minecraft:shepherd/level_4/carpet_sell'          : [ 1,  2] as int[],
        'minecraft:shepherd/level_5/banner_sell'          : [ 3,  5] as int[],
        'minecraft:shepherd/level_5/bed_sell'             : [ 3,  5] as int[],
        // Toolsmith
        'minecraft:toolsmith/level_1/coal_buy'            : [15, 21] as int[],
        'minecraft:toolsmith/level_1/stone_axe_sell'      : [ 1,  2] as int[],
        'minecraft:toolsmith/level_1/stone_shovel_sell'   : [ 1,  2] as int[],
        'minecraft:toolsmith/level_1/stone_pickaxe_sell'  : [ 1,  2] as int[],
        'minecraft:toolsmith/level_1/stone_hoe_sell'      : [ 1,  2] as int[],
        'minecraft:toolsmith/level_2/iron_ingot_buy'      : [ 7, 10] as int[],
        'minecraft:toolsmith/level_2/bell_sell'           : [36, 44] as int[],
        'minecraft:toolsmith/level_2/iron_axe_sell'       : [ 3,  5] as int[],
        'minecraft:toolsmith/level_3/flint_buy'           : [ 8, 10] as int[],
        'minecraft:toolsmith/level_3/iron_shovel_sell'    : [ 4,  6] as int[],
        'minecraft:toolsmith/level_3/iron_pickaxe_sell'   : [ 5,  7] as int[],
        'minecraft:toolsmith/level_4/diamond_buy'         : [ 1,  2] as int[],
        'minecraft:toolsmith/level_4/diamond_axe_sell'    : [ 9, 12] as int[],
        'minecraft:toolsmith/level_5/diamond_shovel_sell' : [ 5,  7] as int[],
        'minecraft:toolsmith/level_5/diamond_pickaxe_sell': [13, 17] as int[],
        'minecraft:toolsmith/level_5/diamond_hoe_sell'    : [ 4,  7] as int[],
        // Weaponsmith
        'minecraft:weaponsmith/level_1/coal_buy'          : [15, 21] as int[],
        'minecraft:weaponsmith/level_1/iron_axe_sell'     : [ 3,  5] as int[],
        'minecraft:weaponsmith/level_1/iron_sword_sell'   : [ 7, 11] as int[],
        'minecraft:weaponsmith/level_2/iron_ingot_buy'    : [ 7, 10] as int[],
        'minecraft:weaponsmith/level_2/bell_sell'         : [36, 44] as int[],
        'minecraft:weaponsmith/level_3/flint_buy'         : [ 6, 10] as int[],
        'minecraft:weaponsmith/level_3/iron_sword_sell_tier2': [9,13] as int[],
        'minecraft:weaponsmith/level_4/diamond_buy'       : [ 2,  4] as int[],
        'minecraft:weaponsmith/level_4/diamond_axe_sell'  : [12, 17] as int[],
        'minecraft:weaponsmith/level_5/diamond_sword_sell': [13, 17] as int[],
    ]

    // ── Price mode enum (mirrors VillagerBargainsConfig.PriceMode) ────────────
    enum PriceMode { MINIMUM, MAXIMUM, CUSTOM }

    // ── Gson instance ─────────────────────────────────────────────────────────
    private static final def GSON = new GsonBuilder().setPrettyPrinting().create()

    // ── Main task action ──────────────────────────────────────────────────────
    @TaskAction
    void generate() {
        // 1. Read config (fall back to defaults if file doesn’t exist yet)
        def cfg = readConfig()
        def globalMode        = cfg.globalPriceMode  as PriceMode
        def globalCustomPrice = cfg.globalCustomPrice as int
        Map<String, Map> perTrade = (cfg.perTradePrices ?: [:]) as Map<String, Map>

        int written = 0
        for (def entry : TRADE_REGISTRY.entrySet()) {
            String tradeId = entry.key
            int    vMin    = entry.value[0]
            int    vMax    = entry.value[1]

            // 2. Determine mode + raw price
            PriceMode mode
            int rawPrice
            if (perTrade.containsKey(tradeId)) {
                def override = perTrade[tradeId]
                mode     = (override.priceMode ?: 'MINIMUM') as PriceMode
                rawPrice = (override.customPrice ?: 1) as int
            } else {
                mode     = globalMode
                rawPrice = globalCustomPrice
            }

            // 3. Resolve final price
            int price = switch (mode) {
                case PriceMode.MINIMUM -> vMin
                case PriceMode.MAXIMUM -> vMax
                case PriceMode.CUSTOM  -> Math.max(vMin, Math.min(vMax, rawPrice))
            }

            // 4. Build JSON fragment
            String json = buildJson(price)

            // 5. Write file to output directory
            String relPath = tradeIdToPath(tradeId)
            File   outFile = outputDir.get().file(relPath).asFile
            outFile.parentFile.mkdirs()
            outFile.text = json
            written++
        }
        logger.lifecycle("[VillagerBargains] Generated {} trade override file(s).", written)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Reads villagerbargains.json from the project’s config dir, or returns defaults. */
    private Map readConfig() {
        if (configFile.isPresent() && configFile.get().exists()) {
            try {
                return GSON.fromJson(configFile.get().text, Map) as Map
            } catch (Exception e) {
                logger.warn('[VillagerBargains] Could not parse config — using defaults: {}', e.message)
            }
        }
        return [globalPriceMode: 'MINIMUM', globalCustomPrice: 1, perTradePrices: [:]]
    }

    /**
     * Builds the JSON fragment for a single trade override.
     * Uses a constant number provider so the price is never randomised.
     * Edit this method if Minecraft changes the villager_trade JSON schema.
     */
    private static String buildJson(int price) {
        def wants = new JsonObject()
        wants.addProperty('id', 'minecraft:emerald')
        def countProvider = new JsonObject()
        countProvider.addProperty('type', 'minecraft:constant')
        countProvider.addProperty('value', price)
        wants.add('count', countProvider)
        def root = new JsonObject()
        root.add('wants', wants)
        return GSON.toJson(root)
    }

    /**
     * Converts a trade ID to a resource path inside the jar.
     * "minecraft:armorer/level_1/coal_buy"
     *   → "data/minecraft/villager_trade/armorer/level_1/coal_buy.json"
     */
    private static String tradeIdToPath(String tradeId) {
        int colon = tradeId.indexOf(':')
        if (colon < 0) return "data/minecraft/villager_trade/${tradeId}.json"
        String ns   = tradeId.substring(0, colon)
        String path = tradeId.substring(colon + 1)
        return "data/${ns}/villager_trade/${path}.json"
    }
}

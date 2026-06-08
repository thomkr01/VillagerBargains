# VillagerBargains

A Fabric mod for **Minecraft 26.1.2** (Fabric Loader `0.19.3`) that guarantees villager trades
at the cheapest possible (godroll) price. Configurable globally or per-trade via a JSON file.

## Features

- **Default godroll** — all trades locked to their vanilla minimum price out of the box
- **Per-trade overrides** — configure any individual trade independently
- **Three price modes:** `MINIMUM`, `MAXIMUM`, `CUSTOM` (clamped to vanilla range)
- **Modular architecture** — updating for a new MC version means editing one file (`VanillaTrades.java`)
- **Zero disk writes** — overrides are injected as an in-memory server data pack at runtime

## Installation

1. Install [Fabric Loader `0.19.3`](https://fabricmc.net/use/) for Minecraft `26.1.2`
2. Drop the `.jar` from [Releases](../../releases) into your `mods/` folder
3. Launch Minecraft—the config file is created automatically on first run

## Configuration

Config file path: `<game_dir>/config/villagerbargains.json`

Default (godroll everything):

```json
{
  "globalPriceMode": "MINIMUM",
  "globalCustomPrice": 1,
  "perTradePrices": {}
}
```

Override a specific trade:

```json
{
  "globalPriceMode": "MINIMUM",
  "globalCustomPrice": 1,
  "perTradePrices": {
    "minecraft:librarian/level_1/enchanted_book": {
      "priceMode": "CUSTOM",
      "customPrice": 8
    },
    "minecraft:armorer/level_4/diamond_leggings_sell": {
      "priceMode": "MAXIMUM"
    }
  }
}
```

### Price modes

| Mode | Description |
|------|-------------|
| `MINIMUM` | Vanilla minimum (godroll) — **default** |
| `MAXIMUM` | Vanilla maximum |
| `CUSTOM` | Your value, clamped to `[vanillaMin, vanillaMax]` |

## Architecture

```
src/main/java/com/villagerbargains/
├── VillagerBargainsMod.java       ← Entrypoint
├── config/
│   └── VillagerBargainsConfig.java  ← JSON config loader/saver
├── trade/
│   ├── VanillaTrades.java           ← ⚠️ Only file to edit for MC version updates
│   ├── TradeDefinition.java         ← Immutable record: tradeId + min + max + clamp
│   ├── PriceResolver.java           ← Config + definition → final price
│   └── TradeJsonBuilder.java        ← Produces constant-count JSON fragment
├── resource/
│   ├── GodRollResourcePack.java     ← Builds the full map of override JSONs
│   └── InMemoryPack.java            ← Serves bytes as a PackResources at runtime
└── util/
    └── ModLogger.java               ← Shared SLF4J logger
```

## Updating for a new Minecraft version

1. Open `VanillaTrades.java` — edit, add, or remove `register(...)` entries
2. Bump `minecraft_version` (and optionally `loader_version` / `mod_version`) in `gradle.properties`
3. Open a PR from `feature/mc-X.Y.Z-update` → `develop` → CI runs → merge when green

## Development

See [CONTRIBUTING.md](CONTRIBUTING.md) for the branch workflow and file responsibilities.

```bash
./gradlew build   # compile + package
./gradlew jar     # jar only
```

## License

[MIT](LICENSE)

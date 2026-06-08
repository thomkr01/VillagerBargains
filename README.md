# VillagerBargains

A Fabric mod for **Minecraft 26.1.2** that locks all villager trade prices to either the cheapest or most expensive vanilla value — no more RNG, no more reloading.

> Fabric Loader `0.19.3` · Java 21

## What it does

Every time a villager generates trades, this mod intercepts and sets the emerald price to either the vanilla **minimum** (godroll) or vanilla **maximum** — your choice. No intermediate values, no surprises.

## Installation

1. Install [Fabric Loader `0.19.3`](https://fabricmc.net/use/) for Minecraft `26.1.2`
2. Drop the `.jar` from [Releases](../../releases) into your `mods/` folder
3. Launch — the config file is created automatically on first run

## Configuration

Config file: `<game_dir>/config/villagerbargains.json`

```json
{
  "globalPriceMode": "MINIMUM"
}
```

| Mode | Result |
|------|--------|
| `MINIMUM` | Cheapest possible price (godroll) — **default** |
| `MAXIMUM` | Most expensive possible price |

That's it. Change the value and restart (or use `/reload`).

## Price reference

Prices follow the vanilla formula. Examples at **MINIMUM**:

**Normal enchantments** — `2 + 3 × level` emeralds

| Enchantment | I | II | III | IV | V |
|---|---|---|---|---|---|
| Protection | 5 🪙 | 8 🪙 | 11 🪙 | 14 🪙 | — |
| Feather Falling | 5 🪙 | 8 🪙 | 11 🪙 | 14 🪙 | — |
| Sharpness | 5 🪙 | 8 🪙 | 11 🪙 | 14 🪙 | 17 🪙 |
| Efficiency | 5 🪙 | 8 🪙 | 11 🪙 | 14 🪙 | 17 🪙 |
| Breach | 5 🪙 | 8 🪙 | 11 🪙 | 14 🪙 | — |
| Unbreaking | 5 🪙 | 8 🪙 | 11 🪙 | — | — |

**Treasure enchantments** — `(2 + 3 × level) × 2` emeralds

| Enchantment | I | II | III |
|---|---|---|---|
| Mending | 10 🪙 | — | — |
| Frost Walker | 10 🪙 | 16 🪙 | — |
| Swift Sneak | 10 🪙 | 16 🪙 | 22 🪙 |
| Soul Speed | 10 🪙 | 16 🪙 | 22 🪙 |
| Wind Burst | 10 🪙 | 16 🪙 | 22 🪙 |
| Infinity | 10 🪙 | — | — |

## Updating for a new Minecraft version

1. Open `VanillaTrades.java` — edit, add, or remove `register(...)` entries
2. Bump `minecraft_version` in `gradle.properties`
3. Build: `./gradlew build`

## Architecture

```
src/main/java/com/villagerbargains/
├── VillagerBargainsMod.java          ← Entrypoint
├── config/
│   └── VillagerBargainsConfig.java   ← JSON config (globalPriceMode)
├── trade/
│   ├── VanillaTrades.java            ← ⚠️ Only file to edit for MC version updates
│   ├── TradeDefinition.java          ← Immutable record: tradeId + min + max
│   └── PriceResolver.java            ← Config + definition → final price
├── resource/
│   ├── GodRollResourcePack.java      ← Builds override JSON map
│   └── InMemoryPack.java             ← Serves bytes as PackResources at runtime
└── util/
    └── ModLogger.java                ← Shared SLF4J logger
```

## License

[MIT](LICENSE)

# BargainVillage

A Fabric mod for **Minecraft 26.1.2** that guarantees villager trades at the cheapest (godroll) price. Configurable per-trade via a JSON config file.

## Why this name?

**BargainVillage** is short, memorable, and directly signals a village full of great deals.

## Features

- Global or per-trade pricing rules
- Default mode is `MINIMUM` (godroll)
- `CUSTOM` prices are clamped to the base game's min/max for each trade
- Modular architecture designed for easy Minecraft version updates

## Config

Generated on first run at:

```text
<game_dir>/config/bargainvillage.json
```

Example:

```json
{
  "globalPriceMode": "MINIMUM",
  "globalCustomPrice": 1,
  "perTradePrices": {
    "minecraft:librarian/level_1/enchanted_book": {
      "priceMode": "CUSTOM",
      "customPrice": 8
    }
  }
}
```

## Architecture

- `config/` → config loading and saving
- `trade/` → vanilla bounds, clamping, price resolution, JSON building
- `resource/` → runtime in-memory resource-pack overrides
- `util/` → shared utilities

## Notes

Minecraft 26.1 villager trades are data-driven, so this mod works by overriding generated `villager_trade` JSON at runtime.

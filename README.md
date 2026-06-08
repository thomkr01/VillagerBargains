# Villager Bargains

A Fabric mod for **Minecraft 26.1.2** that locks villager trade prices to either the **minimum** or **maximum** vanilla price — no more RNG.

## Installation

1. Install [Fabric Loader `0.19.3`](https://fabricmc.net/use/) for Minecraft `26.1.2`
2. Drop the `.jar` from [Releases](../../releases) into your `mods/` folder
3. Launch — config is created automatically at `config/villagerbargains.json`

## Configuration

`config/villagerbargains.json`

```json
{
  "globalPriceMode": "MINIMUM"
}
```

Set `globalPriceMode` to either `MINIMUM` or `MAXIMUM`.

### Price Modes

| Mode | Description |
|------|-------------|
| `MINIMUM` | Cheapest possible price (godroll) — **default** |
| `MAXIMUM` | Most expensive possible price |

## Vanilla Price Reference

Example prices locked in by this mod:

| Trade | MINIMUM | MAXIMUM |
|-------|---------|----------|
| Enchanted Book (Lvl 1) | 5 emeralds | 19 emeralds |
| Enchanted Book (Lvl 2) | 8 emeralds | 32 emeralds |
| Enchanted Book (Lvl 3) | 11 emeralds | 45 emeralds |
| Enchanted Book (Lvl 4) | 14 emeralds | 58 emeralds |
| Enchanted Book (Lvl 5) | 17 emeralds | 64 emeralds |

## Versioning

Versions follow `MAJOR.MINOR.PATCH`:

| Part | When it changes |
|------|----------------|
| `MAJOR` | Complete rewrite or breaking change. Larger Minecraft updates. |
| `MINOR` | New feature added. |
| `PATCH` | Bug fix or minor tweak. |

So `1.0.1` means: first stable release, no new features, one bug fix/tweak.

## License

[MIT](LICENSE)

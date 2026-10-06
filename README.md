# Villager Bargains

A Fabric mod for **Minecraft 26.2 and 26.3**. It makes villager prices land on the
**cheapest price vanilla can roll**, and changes nothing else.

Everything stays vanilla: which trades a villager offers, which enchantments and levels
appear, stock, XP, demand, gossip/reputation discounts and Hero of the Village. The mod
only removes the luck from the one dice roll that decides the price.

## Pricing modes

| Mode | What happens |
|------|--------------|
| `MINIMUM` *(default)* | Every random price is the cheapest value vanilla can roll. |
| `NORMAL` | Untouched vanilla randomness. |
| `MAXIMUM` | Every random price is the most expensive value vanilla can roll. |

Change it in game with [Mod Menu](https://modrinth.com/mod/modmenu) (Mods → Villager
Bargains → Pricing), or in `config/villagerbargains.json`:

```json
{
  "pricing": "MINIMUM"
}
```

The mode applies to trades a villager unlocks **after** the change. Offers a villager
already has are saved in the world and keep their price, just like in vanilla.

## Which prices are random in vanilla?

Since 26.1 villager trades are data-driven, and almost all of them have a **fixed** price.
The random price rolls that exist, and what the mod does with them:

| Trade | Vanilla price | `MINIMUM` | `MAXIMUM` |
|-------|---------------|-----------|-----------|
| Enchanted book, level *L* (librarian) | `2 + random(0 … 4 + 10L) + 3L` | `2 + 3L` | `6 + 13L` |
| …if the enchantment is a treasure one (Mending, Frost Walker, …) | doubled | doubled | doubled |
| Any trade whose cost uses a `minecraft:uniform` range (Trade Rebalance experiment, data packs) | random in range | lowest | highest |

All results are still clamped to 64 by vanilla. Examples for books:

| Book | `MINIMUM` | `MAXIMUM` |
|------|-----------|-----------|
| Level 1 (Efficiency I) | 5 | 19 |
| Level 3 (Sharpness III) | 11 | 45 |
| Level 5 (Sharpness V) | 17 | 64 |
| Mending I (treasure) | 10 | 38 |

### What is deliberately *not* changed

* **Enchanted tools, weapons and armor.** Their price is `base + enchanting power`, and that
  same power decides how good the enchantments are. There is no separate price roll, so
  making them cheaper would mean making the item worse (or a vanilla-impossible bargain).
* **Reputation, demand, Hero of the Village.** These are vanilla price modifiers, not random rolls.

### Why nothing else changes

Vanilla still draws its random number; the mod only replaces the result. The random
sequence is consumed exactly as in vanilla, so every later roll (other trades, enchantments,
levels) comes out the same as it would have without the mod.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) `0.19.5` or newer for Minecraft 26.2 or 26.3.
2. Download the jar that matches your Minecraft version from [Releases](../../releases):
   * `villagerbargains-<version>+26.2.jar` for Minecraft 26.2.x
   * `villagerbargains-<version>+26.3.jar` for Minecraft 26.3.x
3. Put it in your `mods/` folder.

Fabric API is **not** required. Mod Menu is optional (only needed for the in-game screen).
On a server, only the server needs the mod; its config decides the prices.

## Building

Requires Java 25.

```bash
./gradlew build                 # builds both versions
./gradlew :26.3:build           # builds one version
./gradlew :26.3:runClient       # starts a dev client (with Mod Menu)
```

Jars end up in `versions/<minecraft>/build/libs/`.

## Project layout

```
src/main/java/com/villagerbargains/
├── VillagerBargains.java          entrypoint, loads the config
├── config/PricingMode.java        MINIMUM / NORMAL / MAXIMUM and how each picks a value
├── config/VillagerBargainsConfig  reads/writes config/villagerbargains.json
├── price/PriceRolls.java          the one place a vanilla roll becomes the configured value
├── price/TradeCostScope.java      marks "a trade cost is being calculated"
└── mixin/                         hooks shared by every Minecraft version
    ├── EnchantRandomlyFunctionMixin   enchanted book price roll
    └── TradeCostMixin                 opens TradeCostScope around trade-cost evaluation
src/client/java/…/client/          Mod Menu screen
src/test/java/                     unit tests
versions/<minecraft>/
├── gradle.properties              Minecraft, Fabric API and Mod Menu versions
└── src/main/…/mixin/version/      hooks whose vanilla code differs per version
```

## Versioning

`MAJOR.MINOR.PATCH+MINECRAFT`, e.g. `2.0.0+26.3`.

| Part | When it changes |
|------|----------------|
| `MAJOR` | Rewrite or breaking change. |
| `MINOR` | New feature. |
| `PATCH` | Bug fix or small tweak. |

## License

[MIT](LICENSE)

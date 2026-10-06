# Villager Bargains

A Fabric mod for **Minecraft 26.2 and 26.3** (one jar for both). It makes villager prices land on the
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
2. Download `villagerbargains-<version>.jar` from [Releases](../../releases). The same jar works on 26.2 and 26.3.
3. Put it in your `mods/` folder.

Fabric API is **not** required. Mod Menu is optional (only needed for the in-game screen).
On a server, only the server needs the mod; its config decides the prices.

## Building

Requires Java 25.

```bash
./gradlew build        # jar + unit tests + game tests
./gradlew runClient    # dev client (Minecraft 26.3, with Mod Menu)
```

The jar ends up in `build/libs/`. The project compiles against the newest supported
Minecraft version; CI runs the same jar on every supported version.

### How one jar supports several versions

Almost all vanilla code the mod hooks is identical in 26.2 and 26.3. The only difference
is the `minecraft:uniform` number provider class, which 26.3 split into new classes. Both
variants are in `mixin/provider/`, and `VillagerBargainsMixinPlugin` applies a mixin only
when its target class exists in the running game. The server log shows which ones were used:

```
Applying UniformIntProviderMixin to net.minecraft.…ints.UniformGenerator
Skipping LegacyUniformGeneratorMixin: net.minecraft.…number.UniformGenerator does not exist in this Minecraft version
```

## Project layout

```
src/main/java/com/villagerbargains/
├── VillagerBargains.java          entrypoint, loads the config
├── config/PricingMode.java        MINIMUM / NORMAL / MAXIMUM and how each picks a value
├── config/VillagerBargainsConfig  reads/writes config/villagerbargains.json
├── price/PriceRolls.java          the one place a vanilla roll becomes the configured value
├── price/TradeCostScope.java      marks "a trade cost is being calculated"
└── mixin/
    ├── VillagerBargainsMixinPlugin    skips hooks whose vanilla class is not in this version
    ├── EnchantRandomlyFunctionMixin   enchanted book price roll
    ├── TradeCostMixin                 opens TradeCostScope around trade-cost evaluation
    └── provider/                      uniform number provider hooks (26.2 and 26.3 variants)
src/client/java/…/client/          Mod Menu screen
src/test/java/                     unit tests (pricing maths)
src/gametest/java/                 in-game tests: real trade offers in every mode
```

## Testing

`./gradlew build` runs, for every Minecraft version:

* **Unit tests:** the pricing maths, including the vanilla book formula.
* **Game tests:** a real game server generates hundreds of librarian book offers with fixed
  seeds and checks that `MINIMUM`/`MAXIMUM` always give the vanilla extreme, that `NORMAL`
  still varies, and that the enchantment, its level and the random numbers drawn afterwards
  are identical in every mode. They also check that `uniform` rolls outside trade costs
  (loot tables, etc.) are left alone.

CI then takes the built jar and, for **each** supported Minecraft version, boots a real Fabric
dedicated server with only this mod installed and runs the game tests on that server.

## Versioning

`MAJOR.MINOR.PATCH`, e.g. `2.0.0`.

| Part | When it changes |
|------|----------------|
| `MAJOR` | Rewrite or breaking change. |
| `MINOR` | New feature. |
| `PATCH` | Bug fix or small tweak. |

## License

[MIT](LICENSE)

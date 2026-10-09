# Villager Bargains

A Fabric mod for **Minecraft 26.2 and 26.3** (one jar for both). It makes every villager and
wandering trader price land on the **cheapest price vanilla can produce** (or the most
expensive, or plain vanilla), for every profession. Optionally, it can also make villagers
always sell enchanted books at the lowest or highest level.

Everything else stays vanilla: which trades a villager offers, which enchantments appear (and
their levels, unless you turn on book levels), stock, XP, gossip/reputation discounts and Hero
of the Village. The mod only removes the luck from the price: the random price rolls and the
demand surcharge.

## Pricing modes

| Mode | What happens |
|------|--------------|
| `MINIMUM` *(default)* | Every random price is the cheapest value vanilla can roll, and demand never raises a price. |
| `NORMAL` | Untouched vanilla: random prices and the vanilla demand rule. |
| `MAXIMUM` | Every random price is the most expensive value vanilla can roll, and every trade always carries the demand surcharge of a fully sold-out trade. |

Change it in game with [Mod Menu](https://modrinth.com/mod/modmenu) (Mods → Villager
Bargains → Pricing), with [`/bargain price`](#commands), or in `config/villagerbargains.json`:

```json
{
  "pricing": "MINIMUM"
}
```

The mode applies to trades a villager unlocks **after** the change. Offers a villager
already has are saved in the world and keep their rolled price, just like in vanilla; only
their demand part follows the new mode, from the villager's next restock on.

## Book levels

An optional second setting, `bookLevels`, decides which level the enchanted books sold by
villagers get. It is `NORMAL` (vanilla) by default, so nothing changes unless you opt in.

```json
{
  "pricing": "MINIMUM",
  "bookLevels": "MAXIMUM"
}
```

| Mode | Level of a traded enchanted book |
|------|----------------------------------|
| `MINIMUM` | Always the enchantment's lowest level (usually I). |
| `NORMAL` *(default)* | Vanilla: a random level. |
| `MAXIMUM` | Always the enchantment's highest level (Sharpness V, Efficiency V, Unbreaking III; Mending stays I). |

In game: Mod Menu → Villager Bargains → Book levels, or `/bargain books`.

* Which enchantment a book gets stays vanilla; only its level is replaced. Vanilla still
  draws the level roll, so the random sequence is unchanged.
* Only trades are affected. Books from chests, fishing and other loot keep vanilla levels.
* Enchanted tools, weapons and armor are **not** affected (see [Gear strength](#gear-strength)).
* Like `pricing`, it applies to trades a villager unlocks after the change.

The book's price is computed by vanilla from the level that is sold, and `pricing` still pins
the price roll on its own (`2 + random(0 … 4 + 10L) + 3L`, doubled for treasure, at most 64):

| Book offered | `bookLevels` | `pricing: MINIMUM` | `pricing: NORMAL` | `pricing: MAXIMUM` |
|--------------|--------------|--------------------|-------------------|--------------------|
| Sharpness | `MINIMUM` → Sharpness I | 5 | 5 … 19 | 19 |
| Sharpness | `NORMAL` → Sharpness I … V | 5 … 17 | 5 … 64 | 19 … 64 |
| Sharpness | `MAXIMUM` → Sharpness V | 17 | 17 … 64 | 64 |
| Unbreaking | `MAXIMUM` → Unbreaking III | 11 | 11 … 45 | 45 |
| Mending (treasure) | any → Mending I | 10 | 10 … 38 | 38 |

## Gear strength

An optional third setting, `gearStrength`, decides how strongly the enchanted tools, weapons
and armor sold by villagers are enchanted. It is `NORMAL` (vanilla) by default.

```json
{
  "pricing": "MINIMUM",
  "bookLevels": "NORMAL",
  "gearStrength": "MAXIMUM"
}
```

| Mode | Enchanting power of traded gear |
|------|---------------------------------|
| `MINIMUM` | Always the weakest power vanilla can roll (5). |
| `NORMAL` *(default)* | Vanilla: a random power from 5 to 19. |
| `MAXIMUM` | Always the strongest power vanilla can roll (19), as if enchanted at level 19. |

In game: Mod Menu → Villager Bargains → Gear strength, or `/bargain gear`.

* Power 19 gives the best enchantments a villager can sell, but which enchantments it rolls is
  still vanilla's choice, so it is not always the same set.
* The price still follows `pricing`: with `MINIMUM` pricing and `MAXIMUM` strength, a diamond
  sword enchanted at power 19 costs 13 emeralds, the cheapest vanilla price for that trade.
* Only trades are affected. Gear from chests, mobs and the enchanting table stays vanilla.
* With `MINIMUM` or `MAXIMUM`, the item can differ from the one vanilla would have made with
  the same seed. Vanilla still draws the power roll first, but enchanting with a different
  power uses the random numbers that follow differently.
* Like the other settings, it applies to trades a villager unlocks after the change.

## Commands

`/bargain` changes the settings without restarting the server. Every change is saved to
`config/villagerbargains.json` immediately. Like a change in Mod Menu, it applies to trades
villagers unlock from then on: existing offers keep their price and level, and their demand
follows the new pricing mode from the next restock.

| Command | Permission node | What it does |
|---------|-----------------|--------------|
| `/bargain` | any of the nodes below | Shows the current `pricing`, `bookLevels` and `gearStrength`. |
| `/bargain reload` | `villagerbargains.command.reload` | Re-reads `config/villagerbargains.json` (after editing the file by hand). |
| `/bargain price` / `/bargain price <minimum\|normal\|maximum>` | `villagerbargains.command.price` | Shows / sets `pricing`. |
| `/bargain books` / `/bargain books <minimum\|normal\|maximum>` | `villagerbargains.command.books` | Shows / sets `bookLevels`. |
| `/bargain gear` / `/bargain gear <minimum\|normal\|maximum>` | `villagerbargains.command.gear` | Shows / sets `gearStrength`. |

Arguments tab-complete. Changes are announced to other operators, like vanilla admin
commands. The commands also work in single player with cheats enabled.

## Permissions (LuckPerms)

Without a permissions mod, every command needs operator (permission level 2, the same as
`/gamerule`).

With [LuckPerms](https://luckperms.net/) (Fabric) installed, each node can be granted or
denied per player or group:

```
/lp group mods permission set villagerbargains.command.price true
/lp group admin permission set villagerbargains.command.* true
```

The mod uses the fabric-permissions-api that LuckPerms ships. It is an optional integration,
so neither Fabric API nor LuckPerms is required.

The nodes show up in the LuckPerms web editor (`/lp editor`) and in `/lp` tab completion once
a player has joined the server since it started. LuckPerms learns a node the first time it is
checked, and the server checks every command node when it sends a joining player their
command list.

## Which prices are random in vanilla?

Since 26.1 villager trades are data-driven. Of the 391 vanilla trades (26.2/26.3, all
professions plus the wandering trader), about 350 have a **fixed** base price. The random
price rolls that exist, and what the mod does with them:

| Trade | Vanilla price | `MINIMUM` | `MAXIMUM` |
|-------|---------------|-----------|-----------|
| Enchanted book, level *L* (librarian, 27 trades) | `2 + random(0 … 4 + 10L) + 3L` | `2 + 3L` | `6 + 13L` |
| …if the enchantment is a treasure one (Mending, Frost Walker, …) | doubled | doubled | doubled |
| Enchanted tool, weapon or armor (armorer, toolsmith, weaponsmith, fletcher, fisherman, wandering trader; 17 trades) | `base + random(5 … 19)` | `base + 5` | `base + 19` |
| Any trade whose cost uses a `minecraft:uniform` range (Trade Rebalance experiment, data packs) | random in range | lowest | highest |

All results are still clamped to 64 by vanilla. Examples for books:

| Book | `MINIMUM` | `MAXIMUM` |
|------|-----------|-----------|
| Level 1 (Efficiency I) | 5 | 19 |
| Level 3 (Sharpness III) | 11 | 45 |
| Level 5 (Sharpness V) | 17 | 64 |
| Mending I (treasure) | 10 | 38 |

### Enchanted tools, weapons and armor

In vanilla the price of enchanted gear is `base + levels`, where `levels = random(5 … 19)` is
also the enchanting power that decides how good the enchantments are. The mod **decouples**
the two: the enchantments are rolled exactly as in vanilla (same power, same random
sequence), and only the price is pinned, no matter how strong the item turned out.

| Gear | Vanilla price | `MINIMUM` | `MAXIMUM` |
|------|---------------|-----------|-----------|
| Any enchanted gear with base price *B* | `B + 5` … `B + 19` | `B + 5` | `B + 19` |
| Enchanted diamond sword (weaponsmith, *B* = 8) | 13 … 27 | 13 | 27 |

So in `MINIMUM` a well-enchanted sword costs the same as a barely enchanted one, and in
`MAXIMUM` a weak roll costs as much as the best one. Both prices are ones vanilla can produce.

## The demand rule

Every trade also has a vanilla **demand** surcharge. The price you pay is

```
price = clamp(base + max(0, floor(base × demand × priceMultiplier)) + specialPriceDiff, 1, 64)
```

When the villager restocks, demand changes by `uses − (maxUses − uses)`: a trade you used a
lot gets more expensive, an unused one drifts back down (the surcharge is never negative).
Vanilla has no upper bound, so the surcharge can keep growing.

| Mode | Demand |
|------|--------|
| `MINIMUM` | Demand never raises a price (demand is kept at 0 or below). |
| `NORMAL` | Vanilla. |
| `MAXIMUM` | Always the surcharge of a fully sold-out trade (demand is kept at `maxUses` or above), for new offers and after every restock. |

Example: a trade with base price 10, `maxUses` 12 and price multiplier 0.05
(`floor(10 × 12 × 0.05) = 6`):

| Situation | `MINIMUM` | `NORMAL` | `MAXIMUM` |
|-----------|-----------|----------|-----------|
| Fresh trade, never used | 10 | 10 | 16 |
| After one full sell-out and a restock (demand 12) | 10 | 16 | 16 |

In `MAXIMUM`, repeated sell-outs can still push demand above `maxUses` as in vanilla.

The mod pins the demand value itself, so the price shown to a player is correct even when
only the server has the mod. A change of mode reaches existing offers at the villager's next
restock.

## Vanilla discounts still work

The mod only changes (a) the base price a trade is created with and (b) its demand value.
Discounts live in a separate field, `specialPriceDiff`, which the mod never touches. The final
price (vanilla `MerchantOffer#getCostA`) is `clamp(base + demand surcharge + specialPriceDiff, 1, 64)`;
discounts make `specialPriceDiff` negative, penalties (hurting or killing villagers) positive.

| Source | Effect (vanilla, in every mode) |
|--------|---------------------------------|
| Reputation/gossip (trading, curing a zombie villager) | Your reputation with that villager × the trade's price multiplier comes off the price. Curing gives the biggest, long-lasting boost; trading builds it up slowly. |
| Hero of the Village | An extra discount based on the trade's base price (higher effect level = bigger cut, at least 1 emerald) while you have the effect. |

So discounts stack on top of every mode: `MINIMUM` + a cured villager + Hero of the Village
is the cheapest price the game allows, and no price drops below 1 emerald. Because Hero's
discount scales with the base price, its cut is a little smaller in emeralds under `MINIMUM`,
but the final price is still the lowest.

### What is deliberately *not* changed

* **Reputation/gossip discounts and Hero of the Village** (see above).
* **Fixed prices.** Trades without a random roll (most of them, including exploration maps)
  cost the same base price in every mode; only demand differs.
* **Enchantments.** Which enchantment a book or a piece of gear gets is always vanilla. Book
  levels are vanilla too unless you set `bookLevels`, and gear enchanting power unless you set
  `gearStrength`.

### Why nothing else changes

Vanilla still draws its random number; the mod only replaces the result. The random
sequence is consumed exactly as in vanilla, so every later roll (other trades, enchantments,
levels) comes out the same as it would have without the mod.

The book level roll is an exception to "only the price": with `bookLevels` set to
`MINIMUM` or `MAXIMUM` the level of a traded book is replaced too. It works the same way:
vanilla rolls the level, the mod replaces the result, and the random sequence stays vanilla.
`gearStrength` works the same way for the enchanting power of traded gear, with the
difference noted under [Gear strength](#gear-strength).

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) `0.19.5` or newer for Minecraft 26.2 or 26.3.
2. Download `villagerbargains-<version>.jar` from [Releases](../../releases). The same jar works on 26.2 and 26.3.
3. Put it in your `mods/` folder.

Fabric API is **not** required. Mod Menu is optional (only needed for the in-game screen), and
so is LuckPerms (only needed for [per-player permissions](#permissions-luckperms)).
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
├── command/BargainCommands.java   /bargain command tree and permission checks
├── price/ThreadScope.java         marks "a trade cost / trade book is being made"
├── price/EnchantPowerRolls.java   enchanted gear: vanilla power for the item, pinned value for the price
├── price/DemandRule.java          how each mode pins a trade's demand
├── price/DemandPinnable.java      lets VillagerTradeMixin pin a new offer's demand
├── price/BookLevels.java          traded enchanted books: vanilla level roll → configured level
└── mixin/
    ├── VillagerBargainsMixinPlugin    skips hooks whose vanilla class is not in this version
    ├── EnchantRandomlyFunctionMixin   enchanted book price roll and level roll
    ├── EnchantWithLevelsFunctionMixin enchanted gear price (power roll)
    ├── TradeCostMixin                 opens ThreadScope.TRADE_COST around trade-cost evaluation
    ├── CommandsMixin                  registers /bargain (no Fabric API needed)
    ├── provider/                      uniform number provider hooks (26.2 and 26.3 variants)
    └── rules/                         demand rule hooks (villagerbargains.rules.mixins.json)
        ├── MerchantOfferMixin         pins demand on restock; implements DemandPinnable
        └── VillagerTradeMixin         pins demand when a new offer is created
src/main/resources/
├── villagerbargains.mixins.json        price-roll mixins
└── villagerbargains.rules.mixins.json  demand-rule mixins
src/client/java/…/client/          Mod Menu screen
src/test/java/                     unit tests (pricing maths, book levels, config file)
src/gametest/java/…/gametest/      in-game tests: real trade offers in every mode
├── EnchantedBookPriceGameTest     librarian books
├── EnchantedBookLevelGameTest     librarian book levels in every bookLevels mode
├── EnchantedGearPriceGameTest     enchanted tools, weapons and armor
├── DemandRuleGameTest             demand on new offers and restocks
├── AllTradesSweepGameTest         every trade of every profession + wandering trader
├── UniformTradeCostGameTest       uniform trade costs vs. other uniform rolls
└── BargainCommandsGameTest        /bargain commands change and reload the config
```

## Testing

`./gradlew build` runs, for every Minecraft version:

* **Unit tests:** the pricing maths, including the vanilla book formula; the book level
  resolver (`BookLevelsTest`); reading and writing the config, including `bookLevels`
  (`VillagerBargainsConfigTest`).
* **Game tests** on a real game server, with fixed seeds:
  * **Books:** hundreds of librarian book offers; `MINIMUM`/`MAXIMUM` always give the vanilla
    extreme, `NORMAL` still varies, and the enchantment, its level and the random numbers
    drawn afterwards are identical in every mode.
  * **Book levels:** with `bookLevels` `MINIMUM`/`MAXIMUM` every traded book has the lowest or
    highest level, `NORMAL` still varies, the enchantment and the random sequence stay vanilla,
    the price matches the level sold, and enchanted books in chest loot keep vanilla levels.
  * **Enchanted gear:** the price is `base + 5` / `base + 19`, while the enchantments match vanilla.
  * **Demand rule:** new offers and restocks (including sell-outs) in every mode, checked
    against the vanilla formula.
  * **Sweep:** every trade of every profession and the wandering trader, for every villager
    type and 20 seeds. All modes must produce the same items, enchantments and random
    sequence, and `MINIMUM ≤ NORMAL ≤ MAXIMUM` for every price. Exploration-map trades are
    skipped (their price is fixed).
  * **Uniform:** `uniform` rolls outside trade costs (loot tables, etc.) are left alone.
  * **Commands:** `/bargain` subcommands set every option, each one only its own (`price`,
    `books` and `gear` never change each other), and `reload` re-reads the file.
  * **Permissions:** an operator can use every command; a player without operator can neither
    see nor run any of them, and no setting changes. In `./gradlew build` the game tests also
    run with the permissions API LuckPerms uses: each node is granted or denied on its own,
    and showing `/bargain` checks every node, which is how LuckPerms learns them. The CI
    server runs leave that API out, so they test the plain operator check.

CI then takes the built jar and, for **each** supported Minecraft version, boots a real Fabric
dedicated server with only this mod installed and runs the full game-test suite on that
server **3 times**, each with a different seed offset (system property
`villagerbargains.test.seedOffset`), so every run checks different offers.

## Versioning

`MAJOR.MINOR.PATCH`, e.g. `2.0.0`.

| Part | When it changes |
|------|----------------|
| `MAJOR` | Rewrite or breaking change. |
| `MINOR` | New feature. |
| `PATCH` | Bug fix or small tweak. |

## License

[MIT](LICENSE)

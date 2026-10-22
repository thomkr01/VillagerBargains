# Changelog

Newest first. Each GitHub release lists only its own section; the full history lives here.

## 2.5.0

* `/bargain refresh` applies the current demand rule to the trades every loaded villager and wandering trader already has, right away instead of at the next restock. The trades themselves, their items and rolled prices stay as they are. `/bargain refresh villagers`, `/bargain refresh traders` and `/bargain refresh <uuid>` limit it to villagers, wandering traders or one entity. Permission node `villagerbargains.command.refresh` (operator by default).

## 2.4.0

* New optional `vanillaDemand` setting (off by default): the demand surcharge stays vanilla in every `pricing` mode, while random price rolls still follow `pricing`.
* New optional `vanillaWanderingTrader` setting (off by default): wandering trader offers keep vanilla prices and demand, while villagers still follow `pricing`. `bookLevels` and `gearStrength` still apply to its gear.
* Both have a button in Mod Menu → Villager Bargains, and a command: `/bargain demand <vanilla|pinned>` and `/bargain trader <vanilla|pinned>`, with permission nodes `villagerbargains.command.demand` and `villagerbargains.command.trader` (operator by default).

## 2.3.1

* New game tests prove that players without operator can neither see nor run any `/bargain` command, that `price`, `books` and `gear` are independent settings, and that LuckPerms nodes are granted or denied one by one. No behaviour change from 2.3.0.

## 2.3.0

* `/bargain` command: change every setting without a restart. `/bargain` shows the settings, `/bargain reload` re-reads `config/villagerbargains.json`, and `/bargain price|books|gear <minimum|normal|maximum>` sets one and saves it.
* Permission nodes `villagerbargains.command.reload|price|books|gear` (operator by default). With LuckPerms they can be granted per player or group and show up in its web editor. LuckPerms stays optional.

## 2.2.0

* New optional `gearStrength` setting (MINIMUM / NORMAL default / MAXIMUM) pins the enchanting power of tools, weapons and armor sold by villagers to the weakest (5) or strongest (19) value vanilla can roll. In Mod Menu → Villager Bargains → Gear strength, or `config/villagerbargains.json`. The price still follows `pricing`. Loot gear is unchanged.

## 2.1.0

* New optional `bookLevels` setting (MINIMUM / NORMAL default / MAXIMUM) pins the level of enchanted books sold by villagers, e.g. always Sharpness V. In Mod Menu → Villager Bargains → Book levels, or `config/villagerbargains.json`. Loot books and gear are unchanged.
* Mod info now lists Thomas as author and links the project homepage.
* Internal cleanup, a real game client test of the settings screen on CI, and a fix for the Java 25 `sun.misc.Unsafe` warning in CI server runs.

## 2.0.0

* Pricing modes MINIMUM (default) / NORMAL / MAXIMUM, in Mod Menu or `config/villagerbargains.json`.
* Enchanted books: MINIMUM `2 + 3×level`, MAXIMUM `6 + 13×level` (treasure enchantments doubled).
* Enchanted tools, weapons and armor: MINIMUM base + 5, MAXIMUM base + 19; enchantments stay vanilla.
* Demand: MINIMUM never raises prices after buying a trade out; MAXIMUM always charges the sold-out surcharge.
* Reputation, curing discounts and Hero of the Village work as in vanilla on top of every mode.
